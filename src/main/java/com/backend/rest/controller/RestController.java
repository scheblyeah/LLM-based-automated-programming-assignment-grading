package com.backend.rest.controller;

import com.backend.rest.config.PromptConfig;
import com.backend.rest.config.PromptEnhancementConfig;
import com.backend.rest.dto.*;
import com.backend.rest.service.CsvGradingReportService;
import com.backend.rest.service.DockerCodeCompilerService;
import com.backend.rest.service.LLMRequestService;
import com.backend.rest.enums.UsedLLM;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@org.springframework.web.bind.annotation.RestController
public class RestController {
    @Autowired
    public RestController(DockerCodeCompilerService dockerCodeCompilerService,
                          PromptConfig promptConfig,
                          LLMRequestService llmRequestService) {
        this.dockerCodeCompilerService = dockerCodeCompilerService;
        this.promptConfig = promptConfig;
        this.llmRequestService = llmRequestService;
        setApiTokenFromVariable();
    }

    private final DockerCodeCompilerService dockerCodeCompilerService;
    private final PromptConfig promptConfig;
    private final LLMRequestService llmRequestService;

    private UsedLLM usedModel = UsedLLM.GPT_5;

    private LLMRequestConfiguration config;

    private String gradingSchemeInUse;
    private String generatedUnitTestsSourceCode;

    private List<CSVRow> csvRows = new ArrayList<>();
    private String llmRequestCost = "";
    private List<GradingResponseCsvEntry> gradingResponseCsvEntries;

    private List<GradingResponse> gradingResponses;
    private List<GeneratedTestResults> generatedTestResults;

    @GetMapping("/getGradingResults")
    public ResponseEntity<List<GradingResponse>> getGradingResults() {
        return ResponseEntity.ok(this.gradingResponses);
    }

    @GetMapping("/getTestResults")
    public ResponseEntity<List<GeneratedTestResults>> getTestResults() {
        // Return the test results stored in testResults
        return ResponseEntity.ok(this.generatedTestResults);
    }

    @GetMapping("/getGeneratedUnitTests")
    public ResponseEntity<String> getGeneratedUnitTest() {
        return ResponseEntity.ok(this.generatedUnitTestsSourceCode);
    }

    @GetMapping("/getGradingScheme")
    public ResponseEntity<String> getGradingScheme() {
        return ResponseEntity.ok(this.gradingSchemeInUse);
    }

    @GetMapping("/download-csv")
    public void downloadGradingCriteriaCSV(HttpServletResponse response) throws IOException {
        CsvGradingReportService.downloadGradingCriteriaCSV(response, gradingResponseCsvEntries);
    }

    @PostMapping("/generateGradingScheme")
    public String generateGradingScheme(@RequestBody SubmissionDTOMultiple submissionDTO) {
        setModelAndToken(submissionDTO);
        //set grading scheme
        if (submissionDTO.gradingScheme != null && !submissionDTO.gradingScheme.equals("")) {
            return submissionDTO.gradingScheme;
        } else {
            return llmRequestService.generateGradingSchemeIfNotGiven(submissionDTO.gradingScheme, submissionDTO.taskDescription, config);
        }
    }

    private void setModelAndToken(SubmissionDTOMultiple submissionDTO) {
        //setSelectedModel(submissionDTO); todo for now i ignore the frontend model and leave the hardcoded one...
        if (submissionDTO.apiToken != null && !submissionDTO.apiToken.equals("")) {
            this.config = new LLMRequestConfiguration(this.usedModel, submissionDTO.apiToken);
        } else {
            // if no apiToken was given I use my own API token system variable for the corresponding service
            setApiTokenFromVariable();
        }
    }

    private void setSelectedModel(SubmissionDTOMultiple submissionDTO) {
        if (submissionDTO.selectedModel != null && !submissionDTO.selectedModel.equals("")) {
            //this.usedModel = UsedModel.valueOf(submissionDTO.selectedModel);
            for (UsedLLM model : UsedLLM.values()) {
                if (model.getModelName().equalsIgnoreCase(submissionDTO.selectedModel)) {
                    this.usedModel = model;
                }
            }
        }
    }

    private void setApiTokenFromVariable() {
        String apiToken;
        if (this.usedModel == UsedLLM.GEMINI_FLASH || this.usedModel == UsedLLM.GEMINI_PRO) {
            apiToken = System.getenv("GOOGLE_TOKEN");
        } else {
            apiToken = System.getenv("OPENAI_TOKEN");
        }
        this.config = new LLMRequestConfiguration(usedModel, apiToken);
    }

    @PostMapping("/processMultipleOld")
    public String processMultipleTextsGPT(@RequestBody SubmissionDTOMultiple submissionDTO) throws IOException, InterruptedException {
        System.out.println("Number of files to be evaluated:" + submissionDTO.fileDTOs.length);
        // delete all responses from previous requests
        this.csvRows = new ArrayList<>();

        setModelAndToken(submissionDTO);
        this.gradingSchemeInUse = submissionDTO.gradingScheme;

        System.out.println("Using model: " + this.usedModel);

        // set grading scheme
        String gradingSchemeLocal = "";
        if (submissionDTO.gradingScheme != null && !submissionDTO.gradingScheme.equals("")) {
            gradingSchemeLocal = submissionDTO.gradingScheme;
        } else {
            gradingSchemeLocal = llmRequestService.generateGradingSchemeIfNotGiven(submissionDTO.gradingScheme, submissionDTO.taskDescription, config);
        }

        // Initialize prompt enhancement configuration
        PromptEnhancementConfig enhancementConfig = new PromptEnhancementConfig(
                submissionDTO.promptingStyle,
                submissionDTO.requestOrchestration,
                submissionDTO.verificationModel
        );

        // used to map/parse the JSON LLM responses
        ObjectMapper objectMapper = new ObjectMapper();

        // generate Junit Tests
        String junitTests = "";
        if (submissionDTO.generateExecuteTestsBool) {
            // todo make object with first entry of each list
            SubmissionDTOSingle submissionDTOfirst = new SubmissionDTOSingle(submissionDTO.taskDescription,
                    submissionDTO.fileDTOs[0].fileContent,
                    submissionDTO.humanGeneratedTestCases,
                    submissionDTO.sampleSolution,
                    submissionDTO.gradingScheme);
            String junitTestGenerationPrompt = llmRequestService.putVariablesInPrompt(promptConfig.getGenerateTests(), submissionDTOfirst, gradingSchemeLocal);
            junitTests = llmRequestService.makeChatRequestWithString(junitTestGenerationPrompt, "Thank you.", true, config);

            // Attempt to parse the response using Jackson
            try {
                LLMResponseTests llmResponseTests = objectMapper.readValue(junitTests, LLMResponseTests.class);
                junitTests = llmResponseTests.tests;
            } catch (JsonProcessingException e) {
                System.out.println("Error parsing response: " + e.getMessage());
                throw e; // TODO in the end, delete this for more usability in the frontend and return
                // return "The LLM response could not be parsed. Please submit again.";
            }
        }

        this.gradingResponseCsvEntries = new ArrayList<>();
        this.gradingResponses = new ArrayList<>();
        this.generatedTestResults = new ArrayList<>();
        // make different requests for each submission

        int submissionNr = 1;
        for (FileDTO fileDTO : submissionDTO.fileDTOs) {
            SubmissionDTOSingle submissionDTOSingle = new SubmissionDTOSingle(submissionDTO.taskDescription, fileDTO.fileContent, submissionDTO.humanGeneratedTestCases, submissionDTO.sampleSolution, submissionDTO.gradingScheme);

            System.out.println("Processing submission Nr.:" + submissionNr);
            submissionNr++;

            GradingResponse gradingResponse = llmRequestService.getGradingResponseWithEnhancements(
                    submissionDTOSingle, gradingSchemeLocal, enhancementConfig, config);

            gradingResponseCsvEntries.add(new GradingResponseCsvEntry(gradingResponse, fileDTO.fileName));
            gradingResponses.add(gradingResponse);

            if (submissionDTO.generateExecuteTestsBool) {
                GeneratedTestResults unitTests = llmRequestService.getParsedResponseGenerateTests(
                        llmRequestService.putVariablesInPrompt(promptConfig.getUserGradingResponse(), submissionDTOSingle, gradingSchemeLocal), config);
                this.generatedUnitTestsSourceCode = unitTests.getTestClassSourceCode();

                // Run tests and get results
                DockerCodeCompilerService.RunTestsResult testResult = dockerCodeCompilerService.runJUnitTests(
                        fileDTO.fileContent.replaceAll("package\\s+.*?;\\n", ""),
                        getJavaFileNameFromPath(fileDTO.fileName),
                        unitTests.getTestClassSourceCode().replaceAll("package\\s+.*?;\\n", ""),
                        getJavaFileNameFromSourceCode(unitTests.getTestClassSourceCode())
                );

                // Update GeneratedTestResults
                unitTests.setConsoleOutput(testResult.consoleOutput);
                for (UnitTest unitTest : unitTests.getUnitTests()) {
                    for (DockerCodeCompilerService.TestResult tr : testResult.testResults) {
                        String normalizedTestName = tr.testName.replace("()", "");
                        if (normalizedTestName.equals(unitTest.getTestName())) {
                            unitTest.setPassedTest(tr.passed);
                            break;
                        }
                    }
                }
                this.generatedTestResults.add(unitTests);

                System.out.println("Output of JUnit Docker: " + testResult.consoleOutput);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        String formatted = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm"));

        // Specify the file to save the JSON
        File file = new File("results" + formatted + ".json");

        // Convert object to JSON and save to file
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(file, gradingResponseCsvEntries.stream().map(entry -> entry.studentName() + ": " + entry.gradingResponse().totalAchievedPoints + "/" + entry.gradingResponse().totalMaxPoints).toList());
            System.out.println("JSON saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Finished! CSV file can now be downloaded!\n " + this.llmRequestCost;
    }

    @PostMapping("/processMultiple")
    public String processMultipleTextsGPTSync(@RequestBody SubmissionDTOMultiple submissionDTO) throws IOException, InterruptedException {
        System.out.println("Number of files to be evaluated:" + submissionDTO.fileDTOs.length);
        // delete all responses from previous requests
        this.csvRows = new ArrayList<>();

        setModelAndToken(submissionDTO);
        this.gradingSchemeInUse = submissionDTO.gradingScheme;

        System.out.println("Using model: " + this.usedModel);

        // set grading scheme
        String gradingSchemeLocal = "";
        if (submissionDTO.gradingScheme != null && !submissionDTO.gradingScheme.equals("")) {
            gradingSchemeLocal = submissionDTO.gradingScheme;
        } else {
            gradingSchemeLocal = llmRequestService.generateGradingSchemeIfNotGiven(submissionDTO.gradingScheme, submissionDTO.taskDescription, config);
        }

        // Initialize prompt enhancement configuration
        PromptEnhancementConfig enhancementConfig = new PromptEnhancementConfig(
                submissionDTO.promptingStyle,
                submissionDTO.requestOrchestration,
                submissionDTO.verificationModel
        );

        // used to map/parse the JSON LLM responses
        ObjectMapper objectMapper = new ObjectMapper();

        // generate Junit Tests
        String junitTests = "";
        if (submissionDTO.generateExecuteTestsBool) {
            // todo make object with first entry of each list
            SubmissionDTOSingle submissionDTOfirst = new SubmissionDTOSingle(submissionDTO.taskDescription,
                    submissionDTO.fileDTOs[0].fileContent,
                    submissionDTO.humanGeneratedTestCases,
                    submissionDTO.sampleSolution,
                    submissionDTO.gradingScheme);
            String junitTestGenerationPrompt = llmRequestService.putVariablesInPrompt(promptConfig.getGenerateTests(), submissionDTOfirst, gradingSchemeLocal);
            junitTests = llmRequestService.makeChatRequestWithString(junitTestGenerationPrompt, "Thank you.", true, config);

            // Attempt to parse the response using Jackson
            try {
                LLMResponseTests llmResponseTests = objectMapper.readValue(junitTests, LLMResponseTests.class);
                junitTests = llmResponseTests.tests;
            } catch (JsonProcessingException e) {
                System.out.println("Error parsing response: " + e.getMessage());
                throw e; // TODO in the end, delete this for more usability in the frontend and return
                // return "The LLM response could not be parsed. Please submit again.";
            }
        }

        this.gradingResponseCsvEntries = new ArrayList<>();
        this.gradingResponses = new ArrayList<>();
        this.generatedTestResults = new ArrayList<>();

        // make different requests for each submission (minimal concurrent version)
        int submissionNr = 1;
        List<Thread> threads = new ArrayList<>();
        for (FileDTO fileDTO : submissionDTO.fileDTOs) {
            final int currentNr = submissionNr;
            submissionNr++;

            String finalGradingSchemeLocal = gradingSchemeLocal;
            Thread t = new Thread(() -> {
                SubmissionDTOSingle submissionDTOSingle = new SubmissionDTOSingle(submissionDTO.taskDescription, fileDTO.fileContent, submissionDTO.humanGeneratedTestCases, submissionDTO.sampleSolution, submissionDTO.gradingScheme);

                System.out.println("Processing submission Nr.:" + currentNr);

                GradingResponse gradingResponse = llmRequestService.getGradingResponseWithEnhancements(
                        submissionDTOSingle, finalGradingSchemeLocal, enhancementConfig, config);

                // add grading results safely
                synchronized (this) {
                    gradingResponseCsvEntries.add(new GradingResponseCsvEntry(gradingResponse, fileDTO.fileName));
                    gradingResponses.add(gradingResponse);
                }

            });

            threads.add(t);
            t.start();
        }

        // Wait for all threads to finish
        for (Thread t : threads) {
            t.join();
        }

        LocalDateTime now = LocalDateTime.now();
        String formatted = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm"));

        // Specify the file to save the JSON
        File file = new File("results" + formatted + ".json");

        // Convert object to JSON and save to file
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(file, gradingResponseCsvEntries.stream().map(entry -> entry.studentName() + ": " + entry.gradingResponse().totalAchievedPoints + "/" + entry.gradingResponse().totalMaxPoints).toList());
            System.out.println("JSON saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "Finished! CSV file can now be downloaded!\n " + this.llmRequestCost;
    }



    public String getJavaFileNameFromPath(String filePath) {
        return filePath.substring(filePath.lastIndexOf("/") + 1);
    }


    public static String getJavaFileNameFromSourceCode(String sourceCode) {
        // Try to match a public class, interface, or enum first
        String regexPublic = "(?m)\\bpublic\\s+(class|interface|enum)\\s+(\\w+)";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regexPublic).matcher(sourceCode);

        if (matcher.find()) {
            return matcher.group(2) + ".java";
        }

        // If no public class, match the first class, interface, or enum
        String regexAny = "(?m)\\b(class|interface|enum)\\s+(\\w+)";
        matcher = java.util.regex.Pattern.compile(regexAny).matcher(sourceCode);

        if (matcher.find()) {
            return matcher.group(2) + ".java";
        }

        // If no match is found, return null or throw an exception
        return null;
    }

}
