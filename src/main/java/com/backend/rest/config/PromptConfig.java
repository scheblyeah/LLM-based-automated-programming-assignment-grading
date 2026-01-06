package com.backend.rest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "prompts")
public class PromptConfig {

    private String systemGeneral;
    private String systemGrades;
    private String systemFeedback;
    private String systemResponseFormatOnlyGrades;
    private String systemResponseFormatOnlyFeedback;
    private String systemResponseFormatGradesAndFeedback;
    private String description;
    private String submission;
    private String testCases;
    private String sampleSolution;
    private String gradingSchemeGiven;
    private String gradingSchemeToBeGenerated;
    private String generateTests;
    private String systemGradingResponse;
    private String userGradingResponse;

    private String chainOfThought;
    private String oneShot;
    private String fewShot;

    public String getChainOfThought() {
        return chainOfThought;
    }

    public void setChainOfThought(String chainOfThought) {
        this.chainOfThought = chainOfThought;
    }

    public String getOneShot() {
        return oneShot;
    }

    public void setOneShot(String oneShot) {
        this.oneShot = oneShot;
    }

    public String getFewShot() {
        return fewShot;
    }

    public void setFewShot(String fewShot) {
        this.fewShot = fewShot;
    }

    // Getters and Setters

    public String getSystemGeneral() {
        return systemGeneral;
    }

    public void setSystemGeneral(String systemGeneral) {
        this.systemGeneral = systemGeneral;
    }

    public String getSystemGrades() {
        return systemGrades;
    }

    public void setSystemGrades(String systemGrades) {
        this.systemGrades = systemGrades;
    }

    public String getSystemFeedback() {
        return systemFeedback;
    }

    public void setSystemFeedback(String systemFeedback) {
        this.systemFeedback = systemFeedback;
    }

    public String getSystemResponseFormatOnlyGrades() {
        return systemResponseFormatOnlyGrades;
    }

    public void setSystemResponseFormatOnlyGrades(String systemResponseFormatOnlyGrades) {
        this.systemResponseFormatOnlyGrades = systemResponseFormatOnlyGrades;
    }

    public String getSystemResponseFormatOnlyFeedback() {
        return systemResponseFormatOnlyFeedback;
    }

    public void setSystemResponseFormatOnlyFeedback(String systemResponseFormatOnlyFeedback) {
        this.systemResponseFormatOnlyFeedback = systemResponseFormatOnlyFeedback;
    }

    public String getSystemResponseFormatGradesAndFeedback() {
        return systemResponseFormatGradesAndFeedback;
    }

    public void setSystemResponseFormatGradesAndFeedback(String systemResponseFormatGradesAndFeedback) {
        this.systemResponseFormatGradesAndFeedback = systemResponseFormatGradesAndFeedback;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubmission() {
        return submission;
    }

    public void setSubmission(String submission) {
        this.submission = submission;
    }

    public String getTestCases() {
        return testCases;
    }

    public void setTestCases(String testCases) {
        this.testCases = testCases;
    }

    public String getSampleSolution() {
        return sampleSolution;
    }

    public void setSampleSolution(String sampleSolution) {
        this.sampleSolution = sampleSolution;
    }

    public String getGradingSchemeGiven() {
        return gradingSchemeGiven;
    }

    public void setGradingSchemeGiven(String gradingSchemeGiven) {
        this.gradingSchemeGiven = gradingSchemeGiven;
    }

    public String getGradingSchemeToBeGenerated() {
        return gradingSchemeToBeGenerated;
    }

    public void setGradingSchemeToBeGenerated(String gradingSchemeToBeGenerated) {
        this.gradingSchemeToBeGenerated = gradingSchemeToBeGenerated;
    }

    public String getGenerateTests() {
        return generateTests;
    }

    public void setGenerateTests(String generateTests) {
        this.generateTests = generateTests;
    }

    public String getSystemGradingResponse() {
        return systemGradingResponse;
    }

    public void setSystemGradingResponse(String systemGradingResponse) {
        this.systemGradingResponse = systemGradingResponse;
    }

    public String getUserGradingResponse() {
        return userGradingResponse;
    }

    public void setUserGradingResponse(String userGradingResponse) {
        this.userGradingResponse = userGradingResponse;
    }
}