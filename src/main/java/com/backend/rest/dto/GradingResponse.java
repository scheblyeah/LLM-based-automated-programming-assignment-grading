package com.backend.rest.dto;

import java.util.List;

public class GradingResponse {

    public String studentName;
    public String exerciseName;
    public double totalMaxPoints;
    public double totalAchievedPoints;
    List<GradingSchemeSubtask> gradingSchemeSubtasks;

    public GradingResponse() {
    }

    public GradingResponse(List<GradingSchemeSubtask> gradingSchemeSubtasks, String studentName, String exerciseName) { // Updated constructor
        this.gradingSchemeSubtasks = gradingSchemeSubtasks;
        this.studentName = studentName;
        this.exerciseName = exerciseName; // Initialize exerciseName

        this.totalMaxPoints = gradingSchemeSubtasks.stream() // Calculate total max points
                .mapToDouble(GradingSchemeSubtask::getMaxPoints)
                .sum();
        this.totalAchievedPoints = gradingSchemeSubtasks.stream() // Calculate total achieved points
                .mapToDouble(GradingSchemeSubtask::getAchievedPoints)
                .sum();
    }

    public void calcTotalAchievedAndTotalMaxPoints(){
        this.totalMaxPoints = gradingSchemeSubtasks.stream() // Calculate total max points only for positive nr of points
                .mapToDouble(GradingSchemeSubtask::getMaxPoints)
                .filter(totalMaxPoints -> totalMaxPoints > 0)
                .sum();
        this.totalAchievedPoints = gradingSchemeSubtasks.stream() // Calculate total achieved points
                .mapToDouble(GradingSchemeSubtask::getAchievedPoints)
                .sum();
    }

    public List<GradingSchemeSubtask> getGradingSchemeSubtasks() {
        return gradingSchemeSubtasks;
    }

    public void setGradingSchemeSubtasks(List<GradingSchemeSubtask> gradingSchemeSubtasks) {
        this.gradingSchemeSubtasks = gradingSchemeSubtasks;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public double getTotalMaxPoints() {
        return totalMaxPoints;
    }

    public double getTotalAchievedPoints() {
        return totalAchievedPoints;
    }
}