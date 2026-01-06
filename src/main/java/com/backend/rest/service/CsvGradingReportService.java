package com.backend.rest.service;

import com.backend.rest.dto.GradingSchemeSubtask;
import com.backend.rest.dto.GradingResponseCsvEntry;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class CsvGradingReportService {

    public static void downloadGradingCriteriaCSV(HttpServletResponse response, List<GradingResponseCsvEntry> gradingResponseCsvEntries) throws IOException {
        // Set response headers
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"grading_criteria.csv\"");

        PrintWriter writer = response.getWriter();

        // Ensure there are entries to process
        if (!gradingResponseCsvEntries.isEmpty()) {
            // Get all unique subtasks (assume all GradingResponses have the same subtasks structure)
            List<GradingSchemeSubtask> subtasks = gradingResponseCsvEntries.get(0).gradingResponse().getGradingSchemeSubtasks();

            // Calculate the total possible points
            double totalPossiblePoints = subtasks.stream()
                    .mapToDouble(GradingSchemeSubtask::getMaxPoints)
                    .sum();

            // Generate header row
            StringBuilder headerRow = new StringBuilder();
            headerRow.append("\"Grading Criteria Subtask Description\",\"Total Points Per Subtask\"");

            // Append student names and justification columns to the header
            for (GradingResponseCsvEntry entry : gradingResponseCsvEntries) {
                headerRow.append(",\"").append(entry.studentName()).append("\",\"Justification:\" ");
                headerRow.append(",\"").append(entry.studentName()).append("\",\"Justification\""); // <--- Changed line

            }
            writer.println(headerRow);

            // Prepare an array to track total points per student
            int[] studentTotalPoints = new int[gradingResponseCsvEntries.size()];

            // Generate rows for each subtask
            for (GradingSchemeSubtask subtask : subtasks) {
                StringBuilder subtaskRow = new StringBuilder();

                // Subtask description and total points
                subtaskRow.append("\"").append(subtask.getSubtaskName().replace("\"", "'")).append("\"");
                subtaskRow.append(",").append(subtask.getMaxPoints());

                // Append each student's achieved points and justification for this subtask
                for (int i = 0; i < gradingResponseCsvEntries.size(); i++) {
                    GradingResponseCsvEntry entry = gradingResponseCsvEntries.get(i);
                    List<GradingSchemeSubtask> studentSubtasks = entry.gradingResponse().getGradingSchemeSubtasks();

                    GradingSchemeSubtask studentSubtask = studentSubtasks.stream()
                            .filter(s -> s.getSubtaskName().equals(subtask.getSubtaskName()))
                            .findFirst()
                            .orElse(null);

                    if (studentSubtask != null) {
                        // Achieved points for this subtask
                        subtaskRow.append(",").append(studentSubtask.getAchievedPoints());

                        // Justification for this subtask
                        subtaskRow.append(",\"").append(studentSubtask.getJustification().replace("\"", "'")).append("\"");

                        // Add points to the student's total
                        studentTotalPoints[i] += studentSubtask.getAchievedPoints();
                    } else {
                        // Empty fields for student if subtask not found
                        subtaskRow.append(",").append(","); // No points, no justification
                    }
                }

                writer.println(subtaskRow);
            }

            // Add the total points row at the end
            StringBuilder totalPointsRow = new StringBuilder();
            totalPointsRow.append("\"Total Points of Exercise:\",\"").append(totalPossiblePoints).append("\"");

            // Append each student's total points with a column gap for the justification
            for (int i = 0; i < gradingResponseCsvEntries.size(); i++) { // <--- Changed line
                totalPointsRow.append(",").append(studentTotalPoints[i]).append(","); // <--- Changed line
            }

            writer.println(totalPointsRow);
        } else {
            writer.println("No data available.");
        }

        writer.flush();
    }
}
