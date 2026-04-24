/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
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

package de.bushnaq.abdalla.kassandra.ai.insights;

import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.config.DefaultEntitiesInitializer;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.Worklog;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.view.SprintStatistics;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Test class for SprintInsightsGenerator.
 * Demonstrates how to use the AI-powered sprint analysis functionality.
 */
@Tag("AiUnitTest")
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=${test.server.port:0}",
                "spring.profiles.active=test",
                "spring.security.basic.enabled=false"// Disable basic authentication for these tests
        }
)
@AutoConfigureMockMvc
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class SprintInsightsGeneratorTest extends AbstractKeycloakUiTestUtil {
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
    //    @Test
//    public void testGenerateQuickSummary() {
//        System.out.println("\n=== Testing Quick Sprint Summary ===");
//
//        try {
//            String summary = sprintInsightsGenerator.generateQuickSummary(SAMPLE_SPRINT_STATISTICS_DATA);
//
//            System.out.println("Quick Summary:");
//            System.out.println(summary);
//
//            // Basic validation
//            assert summary != null && !summary.trim().isEmpty() : "Summary should not be empty";
//
//        } catch (Exception e) {
//            System.err.println("Error generating quick summary: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

    private String generateJson(SprintStatistics sprintStatistics) {
        // Create a separate ObjectMapper that respects @JsonIgnore annotations for AI insights
        JsonMapper jsonMapper = new JsonMapper();
        //todo do we need this?
//        jsonMapper.registerModule(new JavaTimeModule());
//        jsonMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String jsonString = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sprintStatistics);
//        log.info("Generated JSON for {} sprints", sprintStatistics.size());
        return jsonString;
    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                new RandomCase(3, OffsetDateTime.parse("2025-08-11T08:00:00+01:00"), LocalDate.parse("2025-08-04"), Duration.ofDays(10), 2, 2, 2, 2, 2, 2, 1, 5, 5, 8, 8, 6, 7)//
        };
        return Arrays.stream(randomCases).toList();
    }

    private List<SprintStatistics> loadData() {
        List<SprintStatistics> sprintStatistics = new ArrayList<>();
        // Capture the security context from the current thread
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        List<Sprint> sprintIds = sprintApi.getAll();

        for (Sprint sprint : sprintIds) {
            if (!sprint.getName().equals(DefaultEntitiesInitializer.BACKLOG_SPRINT_NAME)) {
                sprintStatistics.add(new SprintStatistics(loadSprintData(authentication, sprint.getId()), ParameterOptions.getLocalNow()));
            }
        }
        return sprintStatistics;
    }

    private Sprint loadSprintData(Authentication authentication, UUID sprintId) {
        Sprint sprint = null;
        long   time   = System.currentTimeMillis();
        // Load in parallel with security context propagation
        CompletableFuture<Sprint> sprintFuture = CompletableFuture.supplyAsync(() -> {
            // Set security context in this thread
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            try {
                Sprint s = sprintApi.getById(sprintId);
                s.initialize();
                return s;
            } finally {
                SecurityContextHolder.clearContext();// Clear the security context after execution
            }
        });

        CompletableFuture<List<User>> usersFuture = CompletableFuture.supplyAsync(() -> {
            // Set security context in this thread
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            try {
                return userApi.getAll(sprintId);
            } finally {
                SecurityContextHolder.clearContext();// Clear the security context after execution
            }
        });

        CompletableFuture<List<Task>> tasksFuture = CompletableFuture.supplyAsync(() -> {
            // Set security context in this thread
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            try {
                return taskApi.getAll(sprintId);
            } finally {
                SecurityContextHolder.clearContext();// Clear the security context after execution
            }
        });

        CompletableFuture<List<Worklog>> worklogsFuture = CompletableFuture.supplyAsync(() -> {
            // Set security context in this thread
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            try {
                return worklogApi.getAll(sprintId);
            } finally {
                SecurityContextHolder.clearContext();// Clear the security context after execution
            }
        });

        // Wait for all futures and combine results
        try {
            sprint = sprintFuture.get();
            log.info("sprint loaded and initialized in {} ms", System.currentTimeMillis() - time);
            time = System.currentTimeMillis();
            sprint.initUserMap(usersFuture.get());
            sprint.initTaskMap(tasksFuture.get(), worklogsFuture.get());
            log.info("sprint user, task and worklog maps initialized in {} ms", System.currentTimeMillis() - time);
            sprint.recalculate(ParameterOptions.getLocalNow());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error loading sprint data", e);
        }
        return sprint;
    }

//    @Test
//    public void testEstimationAccuracyAnalysis() {
//        System.out.println("\n=== Testing Estimation Accuracy Analysis ===");
//
//        try {
//            String question = "How accurate are our sprint estimations? Are we consistently over or under-estimating?";
//            String analysis = sprintInsightsGenerator.generateFocusedInsights(SAMPLE_SPRINT_STATISTICS_DATA, question);
//
//            System.out.println("Question: " + question);
//            System.out.println("Analysis:");
//            System.out.println(analysis);
//
//        } catch (Exception e) {
//            System.err.println("Error generating estimation analysis: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

//    @Test
//    public void testGenerateFocusedInsights() {
//        System.out.println("\n=== Testing Focused Sprint Insights ===");
//
//        try {
//            String question        = "Which sprints are at risk of missing their deadlines and why?";
//            String focusedInsights = sprintInsightsGenerator.generateFocusedInsights(SAMPLE_SPRINT_STATISTICS_DATA, question);
//
//            System.out.println("Question: " + question);
//            System.out.println("Focused Insights:");
//            System.out.println(focusedInsights);
//
//            // Basic validation
//            assert focusedInsights != null && !focusedInsights.trim().isEmpty() : "Focused insights should not be empty";
//
//        } catch (Exception e) {
//            System.err.println("Error generating focused insights: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

//    @Test
//    public void testGenerateInsights() {
//        System.out.println("=== Testing General Sprint Insights ===");
//
//        try {
//            String insights = sprintInsightsGenerator.generateInsights(SAMPLE_SPRINT_STATISTICS_DATA);
//            System.out.println("Generated Insights:");
//            System.out.println(insights);
//
//            // Basic validation
//            assert insights != null && !insights.trim().isEmpty() : "Insights should not be empty";
//
//        } catch (Exception e) {
//            System.err.println("Error generating insights: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testGenerateInsights(RandomCase randomCase, TestInfo testInfo) throws Exception {
        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        generateProductsIfNeeded(testInfo, randomCase);
        try {
            List<SprintStatistics> sprintStatistics = loadData();
            for (SprintStatistics sprintStatistic : sprintStatistics) {
                String result = sprintInsightsGenerator.generateInsights(generateJson(sprintStatistic));
                assert result != null && !result.trim().isEmpty() : "Summary should not be empty";
                break;
            }


//            System.out.println("Quick Summary:");
//            System.out.println(result);

            // Basic validation

        } catch (Exception e) {
            System.err.println("Error generating quick summary: " + e.getMessage());
            e.printStackTrace();
        }
    }

//    @Test
//    public void testWorkloadPatternsAnalysis() {
//        System.out.println("\n=== Testing Workload Patterns Analysis ===");
//
//        try {
//            String question = "What patterns can you identify in our sprint workload and team velocity?";
//            String analysis = sprintInsightsGenerator.generateFocusedInsights(SAMPLE_SPRINT_STATISTICS_DATA, question);
//
//            System.out.println("Question: " + question);
//            System.out.println("Analysis:");
//            System.out.println(analysis);
//
//        } catch (Exception e) {
//            System.err.println("Error generating workload analysis: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
}
