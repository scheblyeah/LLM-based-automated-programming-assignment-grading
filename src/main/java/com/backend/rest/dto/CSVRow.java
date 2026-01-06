package com.backend.rest.dto;

import java.util.Optional;

public class CSVRow {
    public String filename;
    public Optional<Integer> grade;
    public Optional<String> gradingbreakdown;
    public Optional<String> feedback;
    public Optional<Boolean> compiles;
    public Optional<Boolean> generatedTestsSucceed;

    public CSVRow(String filename, Integer grade, String gradingbreakdown, String feedback, Boolean compiles, Boolean generatedTestsSucceed) {
        this.filename = filename;
        this.grade = Optional.ofNullable(grade);
        this.gradingbreakdown = Optional.ofNullable(gradingbreakdown);
        this.feedback = Optional.ofNullable(feedback);
        this.compiles = Optional.ofNullable(compiles);
        this.generatedTestsSucceed = Optional.ofNullable(generatedTestsSucceed);
    }

}
