/*
 *
 * Copyright (C) 2025-2025 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.ai;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test class for SprintInsightsGenerator.
 * Demonstrates how to use the AI-powered sprint analysis functionality.
 */
@Tag("AiUnitTest")
@SpringBootTest
public class SprintInsightsGeneratorTest {

    /**
     * Sample sprint statistics data for testing (reduced size matching @JsonIgnore fields)
     */
    private static final String                  SAMPLE_SPRINT_STATISTICS_DATA = """
            [
              {
                "currentEfficiency" : "85% Person",
                "currentEffortDelay" : "2h 15m",
                "currentScheduleDelay" : "1d 3h",
                "delayFraction" : 0.12,
                "effortEstimateDisplay" : "4d 3h 0m",
                "effortRemainingDisplay" : "4h 48m",
                "effortSpentDisplay" : "3d 22h 12m",
                "extrapolatedDelayFraction" : 0.08,
                "extrapolatedReleaseDate" : "2025.01.03",
                "extrapolatedScheduleDelay" : "1d 2h",
                "extrapolatedStatus" : "WARNING",
                "optimalEfficiency" : "120% Person",
                "remainingWorkDays" : 2,
                "sprintEndDate" : "2024-12-27T11:30:00",
                "sprintName" : "paris",
                "sprintStartDate" : "2024-12-06T08:00:00",
                "status" : "WARNING",
                "totalWorkDays" : 15,
                "isActualReleaseDate" : false,
                "releaseDateLabel" : "Extrapolated Sprint Release Date",
                "actualProgressDisplay" : "95%",
                "expectedProgressDisplay" : "88%",
                "releaseDateStatus" : "WARNING",
                "currentEffortDelayDisplay" : "2h 15m (12%)",
                "extrapolatedScheduleDelayDisplay" : "1d 2h (8%)"
              },
              {
                "currentEfficiency" : "110% Person",
                "currentEffortDelay" : "0m",
                "currentScheduleDelay" : "0m",
                "delayFraction" : 0.0,
                "effortEstimateDisplay" : "2d 5h 0m",
                "effortRemainingDisplay" : "0h",
                "effortSpentDisplay" : "2d 5h 0m",
                "extrapolatedDelayFraction" : 0.0,
                "extrapolatedReleaseDate" : "2024.11.15",
                "extrapolatedScheduleDelay" : "0m",
                "extrapolatedStatus" : "GOOD",
                "optimalEfficiency" : "100% Person",
                "remainingWorkDays" : 0,
                "sprintEndDate" : "2024-11-15T17:00:00",
                "sprintName" : "london",
                "sprintStartDate" : "2024-11-01T08:00:00",
                "status" : "GOOD",
                "totalWorkDays" : 10,
                "isActualReleaseDate" : true,
                "releaseDateLabel" : "Actual Sprint Release Date",
                "actualProgressDisplay" : "100%",
                "expectedProgressDisplay" : "100%",
                "releaseDateStatus" : "GOOD",
                "currentEffortDelayDisplay" : "0m (0%)",
                "extrapolatedScheduleDelayDisplay" : "0m (0%)"
              },
              {
                "currentEfficiency" : "125% Person",
                "currentEffortDelay" : "-2h 15m",
                "currentScheduleDelay" : "-1h 30m",
                "delayFraction" : -0.05,
                "effortEstimateDisplay" : "1d 2h 0m",
                "effortRemainingDisplay" : "0h",
                "effortSpentDisplay" : "1d 2h 0m",
                "extrapolatedDelayFraction" : 0.0,
                "extrapolatedReleaseDate" : "2024.10.31",
                "extrapolatedScheduleDelay" : "0m",
                "extrapolatedStatus" : "GOOD",
                "optimalEfficiency" : "90% Person",
                "remainingWorkDays" : 0,
                "sprintEndDate" : "2024-10-31T17:00:00",
                "sprintName" : "tokyo",
                "sprintStartDate" : "2024-10-25T08:00:00",
                "status" : "GOOD",
                "totalWorkDays" : 5,
                "isActualReleaseDate" : true,
                "releaseDateLabel" : "Actual Sprint Release Date",
                "actualProgressDisplay" : "100%",
                "expectedProgressDisplay" : "100%",
                "releaseDateStatus" : "GOOD",
                "currentEffortDelayDisplay" : "-2h 15m (-5%)",
                "extrapolatedScheduleDelayDisplay" : "0m (0%)"
              },
              {
                "currentEfficiency" : "0% Person",
                "currentEffortDelay" : "0m",
                "currentScheduleDelay" : "0m",
                "delayFraction" : 0.0,
                "effortEstimateDisplay" : "6d 1h 0m",
                "effortRemainingDisplay" : "6d 1h 0m",
                "effortSpentDisplay" : "0h",
                "extrapolatedDelayFraction" : null,
                "extrapolatedReleaseDate" : "2025.03.05",
                "extrapolatedScheduleDelay" : "NA",
                "extrapolatedStatus" : "GOOD",
                "optimalEfficiency" : "150% Person",
                "remainingWorkDays" : 20,
                "sprintEndDate" : "2025-02-28T17:00:00",
                "sprintName" : "newyork",
                "sprintStartDate" : "2025-02-01T08:00:00",
                "status" : "GOOD",
                "totalWorkDays" : 20,
                "isActualReleaseDate" : false,
                "releaseDateLabel" : "Extrapolated Sprint Release Date",
                "actualProgressDisplay" : "0%",
                "expectedProgressDisplay" : "0%",
                "releaseDateStatus" : "GOOD",
                "currentEffortDelayDisplay" : "0m (0%)",
                "extrapolatedScheduleDelayDisplay" : "NA"
              }
            ]
            """;
    @Autowired
    private              SprintInsightsGenerator sprintInsightsGenerator;

    @Test
    public void testEstimationAccuracyAnalysis() {
        System.out.println("\n=== Testing Estimation Accuracy Analysis ===");

        try {
            String question = "How accurate are our sprint estimations? Are we consistently over or under-estimating?";
            String analysis = sprintInsightsGenerator.generateFocusedInsights(SAMPLE_SPRINT_STATISTICS_DATA, question);

            System.out.println("Question: " + question);
            System.out.println("Analysis:");
            System.out.println(analysis);

        } catch (Exception e) {
            System.err.println("Error generating estimation analysis: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testGenerateFocusedInsights() {
        System.out.println("\n=== Testing Focused Sprint Insights ===");

        try {
            String question        = "Which sprints are at risk of missing their deadlines and why?";
            String focusedInsights = sprintInsightsGenerator.generateFocusedInsights(SAMPLE_SPRINT_STATISTICS_DATA, question);

            System.out.println("Question: " + question);
            System.out.println("Focused Insights:");
            System.out.println(focusedInsights);

            // Basic validation
            assert focusedInsights != null && !focusedInsights.trim().isEmpty() : "Focused insights should not be empty";

        } catch (Exception e) {
            System.err.println("Error generating focused insights: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testGenerateInsights() {
        System.out.println("=== Testing General Sprint Insights ===");

        try {
            String insights = sprintInsightsGenerator.generateInsights(SAMPLE_SPRINT_STATISTICS_DATA);
            System.out.println("Generated Insights:");
            System.out.println(insights);

            // Basic validation
            assert insights != null && !insights.trim().isEmpty() : "Insights should not be empty";

        } catch (Exception e) {
            System.err.println("Error generating insights: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testGenerateQuickSummary() {
        System.out.println("\n=== Testing Quick Sprint Summary ===");

        try {
            String summary = sprintInsightsGenerator.generateQuickSummary(SAMPLE_SPRINT_STATISTICS_DATA);

            System.out.println("Quick Summary:");
            System.out.println(summary);

            // Basic validation
            assert summary != null && !summary.trim().isEmpty() : "Summary should not be empty";

        } catch (Exception e) {
            System.err.println("Error generating quick summary: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testWorkloadPatternsAnalysis() {
        System.out.println("\n=== Testing Workload Patterns Analysis ===");

        try {
            String question = "What patterns can you identify in our sprint workload and team velocity?";
            String analysis = sprintInsightsGenerator.generateFocusedInsights(SAMPLE_SPRINT_STATISTICS_DATA, question);

            System.out.println("Question: " + question);
            System.out.println("Analysis:");
            System.out.println(analysis);

        } catch (Exception e) {
            System.err.println("Error generating workload analysis: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
