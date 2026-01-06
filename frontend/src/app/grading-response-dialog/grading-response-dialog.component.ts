import { Component, Inject } from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import { HttpClient } from '@angular/common/http';
import {MatSnackBar} from "@angular/material/snack-bar";
import {BackendService} from "../backend.service";

export interface GradingResponse { // Consolidated GradingResponse interface
  studentName: string;
  exerciseName: string;
  totalMaxPoints: number;
  totalAchievedPoints: number;
  gradingSchemeSubtasks: GradingSchemeSubtask[];
  showDetails?: boolean; // Keep frontend-specific UI state if needed
}

export interface GradingSchemeSubtask { // Consolidated GradingCriteria interface
  subtaskName: string; // Use criterionName consistently
  maxPoints: number;
  achievedPoints: number;
  justification?: string; // Justification is optional in frontend display, but might be present from backend
}

export interface GeneratedTestResults { // Consolidated GeneratedTestResults interface
  studentName: string;
  consoleOutput: string;
  unitTests: UnitTest[];
  testClassSourceCode: string;
  consoleOutputFull: string;
}

export interface UnitTest { // Keep the existing UnitTest interface
  testName: string;
  passedTest: boolean;
  testSourceCode: string;
  consoleOutput:string;
}

@Component({
  selector: 'app-grading-response-dialog',
  templateUrl: './grading-response-dialog.component.html',
  styleUrls: ['./grading-response-dialog.component.css']
})
export class GradingResponseDialogComponent {
  gradingSchemeContent: string = '';
  generatedUnitTests: string = '';

  constructor(@Inject(MAT_DIALOG_DATA) public data: { gradingResponses: GradingResponse[], testResults: GeneratedTestResults[] },
              private http: HttpClient, public dialogRef: MatDialogRef<GradingResponseDialogComponent>,
              private snackBar: MatSnackBar,  // Inject MatSnackBar
              private backendService: BackendService // Inject BackendService
  ) {
    this.data.gradingResponses = data.gradingResponses || [];
    this.data.gradingResponses.forEach(student => (student.showDetails = false));
    this.data.testResults = data.testResults || [];
  }

  ngOnInit(): void { // Fetch grading scheme on initialization
    this.fetchGradingScheme();
    this.fetchGeneratedUnitTests();
  }

  fetchGradingScheme(): void {
    this.backendService.getGradingScheme().subscribe(
      (scheme) => {
        this.gradingSchemeContent = scheme;
      },
      (error) => {
        console.error('Error fetching grading scheme:', error);
        this.snackBar.open('Error fetching grading scheme.', 'Close', { duration: 3000 });
      }
    );
  }

  fetchGeneratedUnitTests(): void {
    this.backendService.getGeneratedTests().subscribe(
      (testsString) => {
        this.generatedUnitTests = testsString;
      },
      (error) => {
        console.error('Error fetching generated Unit Tests:', error);
        this.snackBar.open('Error fetching generated Unit Tests.', 'Close', { duration: 3000 });
      }
    );
  }



  downloadCSV() {
    this.backendService.downloadCSV().subscribe((blob) => { // Use backendService
      const a = document.createElement('a');
      const objectUrl = URL.createObjectURL(blob);
      a.href = objectUrl;
      a.download = 'grading_criteria.csv'; // Use more descriptive filename
      a.click();
      URL.revokeObjectURL(objectUrl);
    }, error => {
      console.error("Error downloading CSV:", error);
      // Handle download error (e.g., show snackbar)
      this.snackBar.open('Error downloading CSV file.', 'Close', { duration: 3000 });
    });
  }

  getBackgroundStyle(student: GradingResponse) {
    const percentage = (student.totalAchievedPoints / student.totalMaxPoints) * 100;

    if (percentage <= 50) {
      return { backgroundColor: 'red', color: 'black' };
    } else if (percentage <= 80) {
      return { backgroundColor: 'orange', color: 'black' };
    } else {
      return { backgroundColor: 'green', color: 'black' };
    }
  }

  getBackgroundStyleGradingCriteria(test: GradingSchemeSubtask) {
    if (test.maxPoints < 0) {
      // Penalty case — no penalty received is good!
      if (test.achievedPoints === 0) {
        return { backgroundColor: 'green', color: 'black' }; // no penalty
      } else {
        return { backgroundColor: 'red', color: 'black' }; // received penalty
      }
    }

    const percentage = (test.achievedPoints / test.maxPoints) * 100;

    if (percentage < 50) {
      return { backgroundColor: 'red', color: 'black' };
    } else if (percentage < 80) {
      return { backgroundColor: 'orange', color: 'black' };
    } else {
      return { backgroundColor: 'green', color: 'black' };
    }
  }

  getBackgroundStyleTestResults(tests: GeneratedTestResults) {
    const failedTests = tests.unitTests.filter(unitTest => !unitTest.passedTest);

    if (failedTests.length > 1) {
      return { backgroundColor: 'red', color: 'black' };
    } else if (failedTests.length == 1) {
      return { backgroundColor: 'orange', color: 'black' };
    } else {
      return { backgroundColor: 'green', color: 'black' };
    }
  }

  getBackgroundStyleUnitTest(test: UnitTest) {
    if (test.passedTest) {
      return { backgroundColor: 'green', color: 'black' };
    } else {
      return { backgroundColor: 'red', color: 'black' };
    }
  }

  toggleDetails(student: GradingResponse) {
    student.showDetails = !student.showDetails;
  }

}
