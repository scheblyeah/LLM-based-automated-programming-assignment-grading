package com.backend.rest.dto;

public class UnitTest {
    String testName;
    boolean passedTest;
    String testSourceCode;
    String consoleOutput;

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public boolean isPassedTest() {
        return passedTest;
    }

    public void setPassedTest(boolean passedTest) {
        this.passedTest = passedTest;
    }

    public String getTestSourceCode() {
        return testSourceCode;
    }

    public void setTestSourceCode(String testSourceCode) {
        this.testSourceCode = testSourceCode;
    }

    public String getConsoleOutput() {
        return consoleOutput;
    }

    public void setConsoleOutput(String consoleOutput) {
        this.consoleOutput = consoleOutput;
    }
}