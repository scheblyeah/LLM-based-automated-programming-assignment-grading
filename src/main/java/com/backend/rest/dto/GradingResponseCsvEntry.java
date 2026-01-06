package com.backend.rest.dto;

public record GradingResponseCsvEntry(GradingResponse gradingResponse, String studentName) {
    //TODO add getAchivedPoints and getMaximumPoints
}
