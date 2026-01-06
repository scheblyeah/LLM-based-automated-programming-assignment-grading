package com.backend.rest.dto;

public class SubmissionDTOSingle {
    public String taskDescription;
    public String studentSubmission;
    public String humanGeneratedTestCases;
    public String sampleSolution;
    public String gradingScheme;

    public SubmissionDTOSingle(String taskDescription, String studentSubmission, String humanGeneratedTestCases, String sampleSolution, String gradingScheme) {
        this.taskDescription = taskDescription;
        this.studentSubmission = studentSubmission;
        this.humanGeneratedTestCases = humanGeneratedTestCases;
        this.sampleSolution = sampleSolution;
        this.gradingScheme = gradingScheme;
    }
}
