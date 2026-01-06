package com.backend.rest.dto;

import java.util.List;

public record TestResultCsvEntry (String studentName, String testConsoleOutput, List<UnitTest> unitTests) {
    //TODO add numberOfPassedTests and getAmountOfTests

}
