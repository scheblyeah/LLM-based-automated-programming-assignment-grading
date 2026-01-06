package com.backend.rest.dto;

public class GradingSchemeSubtask {
    String subtaskName;
    double achievedPoints;

    double maxPoints;
    String justification;

    public GradingSchemeSubtask(String subtaskDefinition, int grade, int possibleMaximumPoints, String justification) {
        this.subtaskName = subtaskDefinition;
        this.achievedPoints = grade;
        this.justification = justification;
    }

    public String getSubtaskName() {
        return subtaskName;
    }

    public double getAchievedPoints() {
        return achievedPoints;
    }

    public String getJustification() {
        return justification;
    }

    public double getMaxPoints() {
        return maxPoints;
    }
}
