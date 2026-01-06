import {Component} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import JSZip from 'jszip';
import {MatSnackBar} from '@angular/material/snack-bar';
import {
  GradingSchemeSubtask,
  GradingResponse,
  GradingResponseDialogComponent, GeneratedTestResults, UnitTest
} from "./grading-response-dialog/grading-response-dialog.component";
import { MatDialog } from '@angular/material/dialog';
import { BackendService } from './backend.service';
import {firstValueFrom} from "rxjs";


@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'frontend';
  taskDescription = '';
  gradingScheme = '';
  sampleSolution = '';
  humanGeneratedTestCases = '';
  zipFileNames = 'The found <b>.java</b> files of your <b>.zip</b> will be displayed here.';
  llmResponse = '';
  csvContent: string = '';
  selectedModelType: string = 'gpt-4o-mini';
  apiToken = ''; // for the LLM request
  folderName = '';  // Keyword to filter paths (NOT used for bucketing)
  foundFiles: string[] = [];  // Array to store found files
  fileDTO: Array<{ fileName: string, fileContent: string }> = [];
  zipFile: File | null = null;  // Uploaded ZIP file
  javaZipFileNumberOfCharacters: number = 0;
  gradingSchemeCsvFileNumberOfCharacters: number = 0;
  generatedGradingSchemeResponse = '';
  useProvidedGradingScheme = false;

  generateGradingBool = true;
  generateExecuteTestsBool = false;

  gradingResponsesCache: GradingResponse[] | null = null; // Cache for Grading Responses
  testResultsCache: GeneratedTestResults[] | null = null;  // Cache for Test Results

  isSubmitButtonDisabled = false;     // Control Submit button state
  isShowResultsButtonDisabled = true; // Control "Show Results" button state
  isLoadingResults = false;

  helpTextTaskDescription = "Enter a clear and concise description of the programming task or exercise that the student submissions are addressing.  This description will be used to generate a relevant grading scheme and evaluation feedback.";
  helpTextGradingSchemeText = "Provide a grading scheme as plain text. This will be used to guide the evaluation of student submissions. If left empty and 'Generate Grading Scheme' is checked, a grading scheme will be automatically generated.";
  helpTextGradingSchemeCSV = "Upload a CSV file containing a structured grading scheme.  The system will parse this CSV to understand the grading criteria.  This option overrides the 'Grading Scheme (Text)' input if both are provided.";
  helpTextSampleSolutionText = "Provide a sample solution to the exercise as plain text. This sample solution helps the system understand the expected solution and can improve grading and test generation accuracy.";
  helpTextSampleSolutionCSV = "Upload a CSV file containing the sample solution.  Similar to the grading scheme CSV, this allows for a structured sample solution.  This overrides 'Sample Solution (Text)' if both are provided.";
  helpTextTestCases = "Enter specific test cases that you want to be manually included in the evaluation.  These test cases will be combined with any automatically generated tests (if test generation is enabled).";
  helpTextModelType = "Select the Large Language Model (LLM) you want to use for generating grading schemes, evaluating submissions, and/or generating tests. Different models have different strengths, pricing, and capabilities.";
  helpTextApiToken = "Enter your API token for the selected Large Language Model. This token is necessary to authenticate your requests to the LLM service.  You will need to obtain an API token from the respective LLM provider (e.g., OpenAI, Google Gemini).";
  helpTextFolderName = "Type a keyword to filter the files within your ZIP (e.g., a folder or exercise label like 'e01'). This only filters the list; bucketing by student picks the path segment with the most uniqueness after the common tail.";
  helpTextZipFile = "Upload a ZIP file containing the student's Java code submissions. The tool will automatically extract and process all .java files found within the ZIP. Ensure that the ZIP file is properly formatted.";
  helpTextFoundFiles = "These are the .java files that were found in your uploaded .zip after applying the filter (if any)."

  constructor(private http: HttpClient, private snackBar: MatSnackBar, private dialog: MatDialog, private backendService: BackendService) {}

  // Handle ZIP file selection
  onZipFileSelected(event: any): void {
    this.zipFile = event.target.files[0];
    this.returnRelevantFileNames().then(() => {
      if (this.foundFiles.length > 0) {
        this.zipFileNames = `Found ${this.foundFiles.length} files: \n` + this.foundFiles.join('\n');
      } else {
        this.zipFileNames = 'No matching files found. Please upload a <b>.zip</b> file containing files of the selected file type.<br><br>Alternatively, the code submission can be submitted as text.';
      }
    });
    this.llmResponse = '';
    this.countCharactersInZip(this.zipFile).then(charCount => {
      this.gradingSchemeCsvFileNumberOfCharacters = charCount;
    }).catch(error => {
      console.error('Failed to count characters in ZIP:', error);
      this.gradingSchemeCsvFileNumberOfCharacters = 0; // Fallback value
    });
  }

  onModelChange(event: any): void {
    this.selectedModelType = event.target.value;
  }

  // Handle folder name (keyword) change
  onFolderNameChange(event: any): void {
    this.folderName = event.target.value;
    if (this.zipFile) {
      this.onZipFileSelected({target: {files: [this.zipFile]}});
    }
  }

  // --- Helpers for path handling ---

  public norm(p: string): string {
    return p.replace(/\\/g, '/');
  }

  /** Return directory segments only (exclude filename). */
  private dirSegments(p: string): string[] {
    const parts = this.norm(p).split('/').filter(Boolean);
    return parts.slice(0, Math.max(0, parts.length - 1));
  }

  /** Count how many trailing directory segments are identical across all filtered paths. */
  private commonDirSuffixLength(paths: string[]): number {
    if (!paths.length) return 0;
    const dirs = paths.map(p => this.dirSegments(p));
    const minLen = Math.min(...dirs.map(d => d.length));
    let suffix = 0;
    for (let off = 0; off < minLen; off++) {
      const ref = (dirs[0][dirs[0].length - 1 - off] || '').toLowerCase();
      const allSame = dirs.every(d => ((d[d.length - 1 - off] || '').toLowerCase() === ref));
      if (!allSame) break;
      suffix++;
    }
    return suffix; // e.g. 2 when all end with /test1/e01
  }

  /** Pick the folder index (from end) that yields the most unique values across paths. */
  private pickBestStudentOffsetFromEnd(paths: string[], commonSuffix: number): number {
    if (!paths.length) return 0;
    const dirsList = paths.map(p => this.dirSegments(p));

    // Max possible offset (k) from the end (0 = last dir, then 1, 2, ...)
    const maxK = Math.max(...dirsList.map(d => d.length - 1 - commonSuffix));

    let bestK = 0;
    let bestUnique = -1;

    for (let k = 0; k <= maxK; k++) {
      const values = dirsList.map(d => {
        const idx = d.length - 1 - commonSuffix - k;
        return idx >= 0 ? d[idx] : '';
      });
      const uniq = new Set(values.filter(v => v !== '')).size;

      // Prefer the k with the most unique buckets; tie-breaker: choose the smaller k (deeper/closer to the tail)
      if (uniq > bestUnique) {
        bestUnique = uniq;
        bestK = k;
      }
    }

    return bestK;
  }

  // --- UI: list files that match filter ---

  // crawl the .zip file for relevant files
  async returnRelevantFileNames(): Promise<void> {
    if (!this.zipFile) {
      this.foundFiles = [];
      return;
    }

    const zip = new JSZip();
    const content = await zip.loadAsync(this.zipFile);

    const entries = (Object.values(content.files) as any[])
      .filter((f: any) => !f.dir)
      .map((f: any) => f.name)
      .filter((name: string) => name.endsWith('.java'))
      .filter((name: string) => {
        if (!this.folderName) return true; // no filter → keep all .java
        return this.norm(name).toLowerCase().includes(this.norm(this.folderName).toLowerCase()); // keyword filter only
      })
      .sort((a: string, b: string) => a.localeCompare(b, 'en', { numeric: true, sensitivity: 'base' }));

    this.foundFiles = entries;
  }

  // --- Build payload for backend ---

  // search and safe the relevant file names and file contents
  async prepareFilesForBackend(): Promise<void> {
    if (!this.zipFile) return;

    this.fileDTO = [];
    const zip = new JSZip();
    const content = await zip.loadAsync(this.zipFile);

    // 1) Filter: only .java, and (if provided) only paths containing the keyword.
    const entries = (Object.values(content.files) as any[])
      .filter((f: any) => !f.dir)
      .map((f: any) => f.name)
      .filter((name: string) => name.endsWith('.java'))
      .filter((name: string) => {
        if (!this.folderName) return true;
        return this.norm(name).toLowerCase().includes(this.norm(this.folderName).toLowerCase());
      })
      .sort((a: string, b: string) => a.localeCompare(b, 'en', { numeric: true, sensitivity: 'base' }));

    if (!entries.length) {
      this.fileDTO = [];
      return;
    }

    // 2) Find the common trailing dirs (e.g., e01, maybe test1 as well)
    const commonSuffix = this.commonDirSuffixLength(entries);
    console.log('Common trailing dir segments across all entries:', commonSuffix);

    // 3) Choose the folder level (from the end) that maximizes uniqueness across entries
    const k = this.pickBestStudentOffsetFromEnd(entries, commonSuffix);
    console.log('Chosen offset-from-end (0=last dir) for student bucket:', k);

    // 4) Bucket using that folder level
    const buckets: Record<string, string[]> = {};
    const nameByKey: Record<string, string> = {}; // <-- map to final fileName (path before common tail)
    const reads: Promise<void>[] = [];

    for (const path of entries) {
      const dirs = this.dirSegments(path);
      const idx = dirs.length - 1 - commonSuffix - k;
      const studentKey = idx >= 0 ? (dirs[idx] || 'root') : 'root';

      // base path before the common tail, e.g. everything up to (but excluding) /test1/e01
      const basePath = dirs.slice(0, Math.max(0, dirs.length - commonSuffix)).join('/');
      if (!nameByKey[studentKey]) nameByKey[studentKey] = basePath || studentKey;

      const zipEntry = (content.files as any)[path] as any;
      reads.push(
        zipEntry.async('text').then((text: string) => {
          (buckets[studentKey] ||= []).push(text);
        })
      );
    }

    await Promise.all(reads);

    // 5) Deterministic order of students; build DTOs (concatenate all files per student)
    const studentKeys = Object.keys(buckets)
      .filter(k2 => buckets[k2].length > 0)
      .sort((a, b) => a.localeCompare(b, 'en', { numeric: true, sensitivity: 'base' }));

    this.fileDTO = studentKeys.map(k2 => ({
      fileName: nameByKey[k2] || k2,          // use the folder path before the common tail
      fileContent: buckets[k2].join('\n\n'),
    }));

    console.log('Student mapping:', studentKeys.map((k2, i) => ({ index: i + 1, studentKey: k2, name: nameByKey[k2] })));
  }

  // --- CSV & UI helpers ---

  // Method to handle CSV file selection
  onFileSelected(event: any): void {
    const file: File = event.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.readAsText(file);
      reader.onload = () => {
        const csvString = reader.result as string;
        this.csvContent = this.parseCsv(csvString);
        this.gradingSchemeCsvFileNumberOfCharacters = this.csvContent.length;
      };
    }
  }

  // Method to handle CSV file selection
  onFileSelectedSampleSolution(event: any): void {
    const file: File = event.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.readAsText(file);
      reader.onload = () => {
        const sampleSolutionString = reader.result as string;
        this.sampleSolution = this.parseCsv(sampleSolutionString);
      };
    }
  }

  // Method to parse the CSV file content
  parseCsv(csvString: string): string {
    const lines = csvString.split('\n');
    let result = '';
    lines.forEach(line => {
      result += line.replace(/;/g, ' ').trim() + '\n';  // You can adjust this as per your needs
    });
    return result.trim();  // Removing trailing newline
  }

  openGradingResultsDialog(): void {
    if (this.gradingResponsesCache || this.testResultsCache) {
      this.dialog.open(GradingResponseDialogComponent, {
        data: { gradingResponses: this.gradingResponsesCache, testResults: this.testResultsCache },
        width: '80%',
        height: '80%',
      });
    } else {
      this.snackBar.open('Failed to fetch grading/test results.', 'Close', { duration: 3000 });
    }
    this.isSubmitButtonDisabled = false;
  }

  onSubmitMultiple(): void {
    console.log("ON SUBMIT MULTIPLE IS NOW!!!!!!!!")
    const gradingSchemeToSend = this.csvContent || this.gradingScheme;
    this.llmResponse = '';

    this.isSubmitButtonDisabled = true;
    this.isShowResultsButtonDisabled = true;
    this.isLoadingResults = true;
    this.gradingResponsesCache = null;
    this.testResultsCache = null;

    // Determine if a grading scheme is provided by the user
    this.useProvidedGradingScheme = !!(this.csvContent || this.gradingScheme);

    this.prepareFilesForBackend().then(() => {
      if (this.fileDTO.length === 0) {
        this.snackBar.open('Please upload or paste a student submission!', 'Close', {
          duration: 2000,
        });
        this.isSubmitButtonDisabled = false;
        this.isLoadingResults = false;
        return;
      }

      const submissionData = {
        taskDescription: this.taskDescription,
        fileDTOs: this.fileDTO,
        gradingScheme: gradingSchemeToSend,
        humanGeneratedTestCases: this.humanGeneratedTestCases,
        sampleSolution: this.sampleSolution,
        selectedModel: this.selectedModelType,
        apiToken: this.apiToken,
        generateGradingBool: this.generateGradingBool,
        generateExecuteTestsBool: this.generateExecuteTestsBool,
        useProvidedGradingScheme: this.useProvidedGradingScheme
      };

      const processSubmissions = () => {
        this.backendService.processMultiple(submissionData).subscribe(data => {
          this.llmResponse = data;
          this.fetchAndCacheResults().then(() => {
            this.isShowResultsButtonDisabled = false;
            this.isLoadingResults = false;
          });
        }, error => {
          console.error('Error processing submissions:', error);
          this.snackBar.open('Error processing submissions. Please check console for details.', 'Close', {duration: 3000});
          this.isSubmitButtonDisabled = false;
          this.isShowResultsButtonDisabled = true;
          this.isLoadingResults = false;
        });
      };

      if (this.generateGradingBool && !this.useProvidedGradingScheme) {
        this.backendService.generateGradingScheme(submissionData).subscribe(generatedGradingSchemeResponse => {
          this.generatedGradingSchemeResponse = generatedGradingSchemeResponse;
          submissionData.gradingScheme = generatedGradingSchemeResponse;
          processSubmissions();
        }, error => {
          console.error('Error generating grading scheme:', error);
          this.snackBar.open('Error generating grading scheme. Please check console for details.', 'Close', {duration: 3000});
          this.isSubmitButtonDisabled = false;
          this.isShowResultsButtonDisabled = true;
          this.isLoadingResults = false;
        });
      } else {
        processSubmissions();
      }
    });
  }

  async countCharactersInZip(file: File | null): Promise<number> {
    if (file == null) {
      return 0;
    }
    const zip = new JSZip();
    let totalCharCount = 0;

    try {
      const zipContent = await new Promise<ArrayBuffer>((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = (e) => {
          if (e.target) {
            resolve(e.target.result as ArrayBuffer);
          } else {
            reject(new Error('File reading failed: target is null'));
          }
        };
        reader.onerror = () => reject(new Error('File reading failed'));
        reader.readAsArrayBuffer(file);
      });

      const unzipped = await zip.loadAsync(zipContent);

      for (const filename of Object.keys(unzipped.files)) {
        if (filename.endsWith('.java') && filename.includes(this.folderName)) {
          const fileContent = await (unzipped.files as any)[filename].async('text');
          totalCharCount += fileContent.length;
          console.log(`${filename}: ${fileContent.length} characters`);
        }
      }
      console.log(`Total number of characters in .java files: ${totalCharCount}`);
    } catch (error) {
      console.error('Error processing ZIP file:', error);
      throw error;
    }

    return totalCharCount;
  }

  // Function to calculate the approximate total input characters
  getApproximateInputCharacters(): number {
    let totalChars = 0;
    totalChars += this.taskDescription.length;
    if(this.gradingSchemeCsvFileNumberOfCharacters == null){
      totalChars += this.gradingSchemeCsvFileNumberOfCharacters;
    } else {
      totalChars += this.gradingScheme.length;
    }
    totalChars += this.sampleSolution.length;
    totalChars += this.humanGeneratedTestCases.length;

    totalChars += this.gradingSchemeCsvFileNumberOfCharacters;
    return totalChars;
  }

  // Function to calculate the approximate token count
  getApproximateTokenCount(): number {
    return Math.round(this.getApproximateInputCharacters() / 4);
  }

  async fetchAndCacheResults(): Promise<void> {
    try {
      const [gradingResponsesBackend, testResultsBackend] = await Promise.all([
        firstValueFrom(this.backendService.getGradingResults()),
        firstValueFrom(this.backendService.getTestResults())
      ]);

      let gradingResponsesArray: GradingResponse[] = [];

      if (gradingResponsesBackend && Array.isArray(gradingResponsesBackend)) {
        gradingResponsesArray = gradingResponsesBackend as GradingResponse[];
      } else if (gradingResponsesBackend) {
        console.warn('Grading Results Backend did not return an array, attempting to treat it as single element array.');
        gradingResponsesArray = [gradingResponsesBackend] as GradingResponse[];
      } else {
        console.error('Grading Results Backend returned no data or invalid data.');
        gradingResponsesArray = [];
      }

      const gradingResponses: GradingResponse[] = gradingResponsesArray.map(entry => ({
        ...entry,
        showDetails: false
      }));

      const testResultsCast = testResultsBackend as GeneratedTestResults[];

      this.gradingResponsesCache = gradingResponses;
      this.testResultsCache = testResultsCast;

    } catch (error) {
      console.error('Error fetching results:', error);
      this.snackBar.open('Error fetching grading/test results. Check console.', 'Close', { duration: 3000 });
      this.isShowResultsButtonDisabled = true;
      this.isLoadingResults = false;
      throw error;
    }
  }
}
