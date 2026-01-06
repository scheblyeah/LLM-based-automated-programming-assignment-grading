package com.backend.rest.dto;

import java.util.List;

public class GeneratedTestResults {
    List<UnitTest> unitTests;
    String testClassSourceCode;
    String consoleOutputFull;
    public String studentName;

    public GeneratedTestResults(List<UnitTest> generatedTests, String testClassSourceCode, String consoleOutput, String studentName) {
    this.unitTests = generatedTests;
    this.testClassSourceCode = testClassSourceCode;
    this.consoleOutputFull = consoleOutput;
    }

    public List<UnitTest> getUnitTests() {
        return unitTests;
    }

    public String getTestClassSourceCode() {
        return testClassSourceCode;
    }

    public String getConsoleOutput() {
        return consoleOutputFull;
    }

    public void setConsoleOutput(String consoleOutput) {
        this.consoleOutputFull = consoleOutput;
    }

    public void setTestClassSourceCode(String testClassSourceCode) {
        this.testClassSourceCode = testClassSourceCode;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
}