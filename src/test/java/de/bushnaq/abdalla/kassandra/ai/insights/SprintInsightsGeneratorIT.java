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
import de.bushnaq.abdalla.kassandra.service.GanttBurndownChartService;
import de.bushnaq.abdalla.kassandra.service.SickLeaveImpactContributor;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.view.SprintStatistics;
import de.bushnaq.abdalla.kassandra.util.*;
import de.bushnaq.abdalla.util.date.DateUtil;
import de.bushnaq.abdalla.util.date.ReportUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
public class SprintInsightsGeneratorIT extends AbstractKeycloakUiTestUtil {
    private static final double                     CHART_VISUAL_SCALE = 2.0;
    @Autowired
    private              H2DatabaseStateManager     databaseStateManager;
    @Autowired
    private              GanttBurndownChartService  ganttBurndownChartService;
    @Autowired
    private              SickLeaveImpactContributor sickLeaveImpactContributor;
    @Autowired
    private              SprintInsightsGenerator    sprintInsightsGenerator;
    @TempDir
    private              Path                       temporaryDirectory;

    private static void appendCsvRow(StringBuilder csv, Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append('"');
            if (values[i] != null) {
                csv.append(values[i].toString().replace("\"", "\"\""));
            }
            csv.append('"');
        }
        csv.append(System.lineSeparator());
    }

    private void appendSprintSummaryRow(StringBuilder csv, Sprint sprint, LocalDateTime now) {
        Duration estimation = DateUtil.add(sprint.getWorked(), sprint.getRemaining());
        String currentEffortDelay = ReportUtil.calcualteManDelayString(sprint.getStart(), now, sprint.getEnd(),
                sprint.getWorked(), estimation);
        String currentScheduleDelay = DateUtil.createDurationString(
                ReportUtil.calculateWorkDaysMillisecondsDelay(sprint.getStart(), now, sprint.getEnd(),
                        sprint.getWorked(), sprint.getOriginalEstimation(), estimation, sprint.getRemaining()),
                false, true, false);
        appendCsvRow(csv, sprint.getName(), "NA", sprint.getStart(), sprint.getEnd(), "",
                currentEffortDelay, currentScheduleDelay);
    }

    private String caseName(RandomCase randomCase) {
        return randomCase.generateName().replace(".snapshot", "") + "-case-" + randomCase.getTestCaseIndex();
    }

    private void compareReplayWithReference(RandomCase randomCase, Map<String, SprintEvaluation> referenceEvaluations,
                                            Map<String, SprintEvaluation> sickLeaveEvaluations) throws Exception {
        Path outputDirectory = Path.of("test-results", "sick-leave-impact");
        Files.createDirectories(outputDirectory);
        String                                              caseName             = caseName(randomCase);
        Path                                                referenceOutputFile  = outputDirectory.resolve(caseName + "-reference.csv");
        Path                                                simulationOutputFile = outputDirectory.resolve(caseName + "-simulation.csv");
        Path                                                chartOutputDirectory = outputDirectory;
        StringBuilder                                       referenceCsv         = new StringBuilder();
        StringBuilder                                       simulationCsv        = new StringBuilder();
        Map<String, SickLeaveWorklogSimulator.ReplayResult> replayResults        = new LinkedHashMap<>();
        appendCsvRow(referenceCsv, "sprint-id", "task-id", "start", "end", "progress",
                "current-effort-delay", "current-schedule-delay");
        appendCsvRow(simulationCsv, "sprint-id", "task-id", "start", "end", "progress",
                "current-effort-delay", "current-schedule-delay");

        for (SprintEvaluation sickEvaluation : sickLeaveEvaluations.values().stream()
                .sorted(Comparator.comparing(evaluation -> evaluation.sprint().getName())).toList()) {
            String           sprintName          = sickEvaluation.sprint().getName();
            SprintEvaluation referenceEvaluation = referenceEvaluations.get(sprintName);
            assertEquals(referenceEvaluation.tasksByKey().keySet(), sickEvaluation.tasksByKey().keySet(),
                    "Task plans should match for sprint " + sprintName);

            SickLeaveWorklogSimulator.ReplayResult replay = SickLeaveWorklogSimulator.replayWithoutSickLeave(
                    sickEvaluation.sprint(), randomCase.getNow().toLocalDateTime());
            replayResults.put(sprintName, replay);

            for (Map.Entry<String, Task> entry : sickEvaluation.tasksByKey().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()).toList()) {
                String                               taskKey       = entry.getKey();
                Task                                 sickTask      = entry.getValue();
                Task                                 referenceTask = referenceEvaluation.tasksByKey().get(taskKey);
                SickLeaveWorklogSimulator.TaskReplay taskReplay    = replay.tasksById().get(sickTask.getId());
                if (taskReplay == null) {
                    throw new IllegalStateException("Replay did not return task '" + taskKey + "' in sprint '" + sprintName + "'.");
                }

                // Stable names/keys pair the independently generated snapshots despite their different UUIDs.
                appendCsvRow(referenceCsv, sprintName, taskKey, firstWorklogDate(referenceTask),
                        lastWorklogDate(referenceTask), progressPercent(referenceTask.getProgress()), "", "");
                appendCsvRow(simulationCsv, sprintName, taskKey, taskReplay.firstWorklogDate(),
                        taskReplay.lastWorklogDate(), progressPercent(taskReplay.progress()), "", "");
            }

            appendSprintSummaryRow(referenceCsv, referenceEvaluation.sprint(), randomCase.getNow().toLocalDateTime());
            try (SickLeaveWorklogSimulator.AppliedReplay ignored = SickLeaveWorklogSimulator.applyToSprint(
                    sickEvaluation.sprint(), replay, randomCase.getNow().toLocalDateTime())) {
                appendSprintSummaryRow(simulationCsv, sickEvaluation.sprint(), randomCase.getNow().toLocalDateTime());
            }
        }

        // Write the two same-schema files for direct row-by-row comparison.
        Files.writeString(referenceOutputFile, referenceCsv, StandardCharsets.UTF_8);
        Files.writeString(simulationOutputFile, simulationCsv, StandardCharsets.UTF_8);
        log.info("Wrote sickness-free reference to {} and replay to {}", referenceOutputFile.toAbsolutePath(),
                simulationOutputFile.toAbsolutePath());
        writeKristenLondonWorklogComparison(randomCase, referenceEvaluations, sickLeaveEvaluations, replayResults);

        Files.createDirectories(chartOutputDirectory);
        GanttGenerator chartGenerator = new GanttGenerator(ganttBurndownChartService);
        for (Map.Entry<String, SickLeaveWorklogSimulator.ReplayResult> entry : replayResults.entrySet()) {
            SprintEvaluation evaluation = sickLeaveEvaluations.get(entry.getKey());
            try (SickLeaveWorklogSimulator.AppliedReplay ignored = SickLeaveWorklogSimulator.applyToSprint(
                    evaluation.sprint(), entry.getValue(), randomCase.getNow().toLocalDateTime())) {
                String chartName = safeFileName(entry.getKey()) + "-simulation";
                chartGenerator.generateGanttBurndownChart(evaluation.sprint(), chartOutputDirectory.toString(),
                        chartName, CHART_VISUAL_SCALE, false);
            }
        }
    }

    private Path createInitialDatabaseSnapshot() throws IOException {
        String snapshotName = "sick-leave-impact-baseline-" + UUID.randomUUID();
        Path   exported     = Path.of(databaseStateManager.exportDatabaseSnapshot(snapshotName));
        Path   temporary    = temporaryDirectory.resolve(exported.getFileName());
        return Files.move(exported, temporary, StandardCopyOption.REPLACE_EXISTING);
    }

    private Map<String, SprintEvaluation> evaluateSprints(RandomCase randomCase) throws Exception {
        SnapshotLogGenerator snapshotLogGenerator = new SnapshotLogGenerator(
                Path.of("test-database-snapshots", randomCase.generateName() + ".log"));
        Map<String, SprintEvaluation> evaluations = new HashMap<>();
        for (Sprint sprint : loadSprints()) {
            if (sprint.getStart() == null || sprint.getStart().isAfter(randomCase.getNow().toLocalDateTime())) {
                continue;
            }
            SickLeaveImpactContributor.SprintImpact impact = sickLeaveImpactContributor.evaluate(sprint);
            assertEquals(snapshotLogGenerator.getSickDays(sprint.getName()), impact.sickDays(),
                    "Detected sick leave should match the snapshot log for sprint " + sprint.getName());
            Map<String, Task> tasksByKey = new HashMap<>();
            for (Task task : sprint.getTasks()) {
                tasksByKey.put(task.getKey(), task);
            }
            evaluations.put(sprint.getName(), new SprintEvaluation(sprint, impact, tasksByKey));
        }
        return evaluations;
    }

//    @ParameterizedTest
//    @MethodSource("de.bushnaq.abdalla.kassandra.ui.RandomCases#listRandomCases")
//    @WithMockUser(username = "admin-user", roles = "ADMIN")
//    public void testGenerateInsights(RandomCase randomCase, TestInfo testInfo) throws Exception {
//        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
//        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
//        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
//        generateProductsIfNeeded(testInfo, randomCase);
//        try {
//            List<SprintStatistics> sprintStatistics = loadData();
//            for (SprintStatistics sprintStatistic : sprintStatistics) {
//                String result = sprintInsightsGenerator.generateInsights(generateJson(sprintStatistic));
//                assert result != null && !result.trim().isEmpty() : "Summary should not be empty";
//                break;
//            }
//        } catch (Exception e) {
//            System.err.println("Error generating quick summary: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

    private LocalDate firstWorklogDate(Task task) {
        return task.getWorklogs().stream()
                .map(Worklog::getStart)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .min(LocalDate::compareTo)
                .orElse(null);
    }

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

    private void generateScenarioCharts(String scenarioFolder, Map<String, SprintEvaluation> evaluations)
            throws Exception {
        Path chartOutputDirectory = Path.of("test-results", "sick-leave-impact");
        Files.createDirectories(chartOutputDirectory);
        GanttGenerator chartGenerator = new GanttGenerator(ganttBurndownChartService);
        for (SprintEvaluation evaluation : evaluations.values().stream()
                .sorted(Comparator.comparing(item -> item.sprint().getName())).toList()) {
            String chartName = safeFileName(evaluation.sprint().getName()) + "-" + scenarioFolder;
            chartGenerator.generateGanttBurndownChart(evaluation.sprint(), chartOutputDirectory.toString(),
                    chartName, CHART_VISUAL_SCALE, false);
        }
    }

    private boolean isKristenHubbell(Task task) {
        return task.getAssignedUser() != null && "Kristen Hubbell".equals(task.getAssignedUser().getName());
    }

    private LocalDate lastWorklogDate(Task task) {
        return task.getWorklogs().stream()
                .map(Worklog::getStart)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    private List<SprintStatistics> loadData() {
        List<SprintStatistics> sprintStatistics = new ArrayList<>();
        for (Sprint sprint : loadSprints()) {
            sprintStatistics.add(new SprintStatistics(sprint, ParameterOptions.getLocalNow()));
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
                Sprint s = peg.sprintApi.getById(sprintId);
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
                return peg.userApi.getAll(sprintId);
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
                return peg.taskApi.getAll(sprintId);
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
                return peg.worklogApi.getAll(sprintId);
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

    private List<Sprint> loadSprints() {
        List<Sprint> sprints = new ArrayList<>();
        // Capture the security context from the current thread
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        List<Sprint> sprintIds = peg.sprintApi.getAll();

        for (Sprint sprint : sprintIds) {
            if (!sprint.getName().equals(DefaultEntitiesInitializer.BACKLOG_SPRINT_NAME)) {
                sprints.add(loadSprintData(authentication, sprint.getId()));
            }
        }
        return sprints;
    }

    private String progressPercent(Float progress) {
        return progress == null ? null : String.format(Locale.ROOT, "%.0f%%", progress * 100);
    }

    private void restoreInitialDatabaseSnapshot(Path snapshot) {
        entityManager.clear();
        assertTrue(databaseStateManager.importDatabaseSnapshot(snapshot.toString()),
                "Could not restore the clean sick-leave test database");
        entityManager.clear();
    }

    private String safeFileName(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /**
     * Verifies detected critical-path absences and propagated delays against the same seeded no-sickness snapshot.
     *
     * @param randomCase synthetic sprint configuration used for both snapshots
     * @param testInfo   current test metadata used by the fixture generator
     * @throws Exception if snapshot creation or data loading fails
     */
    @ParameterizedTest
    @MethodSource("de.bushnaq.abdalla.kassandra.ui.RandomCases#listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testSickLeaveImpact(RandomCase randomCase, TestInfo testInfo) throws Exception {
        String testMethodName = testInfo.getTestMethod().orElseThrow().getName() + "-sick-leave-impact-" + randomCase.getTestCaseIndex();
        TestInfoUtil.setTestMethod(testInfo, testMethodName);
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testMethodName);

        // Generate the reference first without sick leave, keeping the original seed and all other data settings.
        RandomCase reference = withProbabilities(randomCase, 0f, 0f);

        // Save the clean post-Keycloak fixture so each scenario starts without the other scenario's data.
        Path initialDatabaseSnapshot = createInitialDatabaseSnapshot();
        restoreInitialDatabaseSnapshot(initialDatabaseSnapshot);

        // Keep the actual sickness-free worklogs in memory as the expected replay result.
        generateProductsIfNeeded(testInfo, reference, true);
        Map<String, SprintEvaluation> referenceEvaluations = evaluateSprints(reference);
        String                        caseName             = caseName(reference);
        generateScenarioCharts("0-percent-sick-leave", referenceEvaluations);

        // Restore the clean database before generating the sick-leave scenario.
        restoreInitialDatabaseSnapshot(initialDatabaseSnapshot);
        peg.eg.init();
        peg.nameGenerator = new NameGenerator();
        peg.getOffDays().clear();

        // Generate the same seeded data with sick leave on every eligible sprint.
        RandomCase sickLeaveCase = withProbabilities(randomCase, 1f, 0);
        generateProductsIfNeeded(testInfo, sickLeaveCase, true);
        Map<String, SprintEvaluation> sickLeaveEvaluations = evaluateSprints(sickLeaveCase);
        assertEquals(referenceEvaluations.keySet(), sickLeaveEvaluations.keySet(),
                "Both snapshots should contain the same sprints");
        generateScenarioCharts("100-percent-sick-leave", sickLeaveEvaluations);

        // Replay sick-snapshot worklogs with sickness removed and emit paired diagnostics before asserting equality.
        compareReplayWithReference(sickLeaveCase, referenceEvaluations, sickLeaveEvaluations);
    }

    private RandomCase withProbabilities(RandomCase randomCase, float sickSprintProbability,
                                         float worklogDelayProbability) {
        return RandomCase.builder()
                .testCaseIndex(randomCase.getTestCaseIndex())
                .now(randomCase.getNow())
                .startDate(randomCase.getStartDate())
                .numberOfProducts(randomCase.products().min(), randomCase.products().max())
                .numberOfVersions(randomCase.versions().min(), randomCase.versions().max())
                .numberOfFeatures(randomCase.features().min(), randomCase.features().max())
                .numberOfSprints(randomCase.sprints().min(), randomCase.sprints().max())
                .taskDurationPerDay(randomCase.taskDurationDays().min(), randomCase.taskDurationDays().max())
                .numberOfStories(randomCase.stories().min(), randomCase.stories().max())
                .numberOfUsers(randomCase.users().min(), randomCase.users().max())
                .numberOfUsersPerSprint(randomCase.usersPerSprint().min(), randomCase.usersPerSprint().max())
                .numberOfTasks(randomCase.tasks().min(), randomCase.tasks().max())
                .seed(randomCase.getSeed())
                .sickSprintProbability(sickSprintProbability)
                .worklogDelayProbability(worklogDelayProbability)
                .build();
    }

    private void writeKristenLondonWorklogComparison(RandomCase randomCase,
                                                     Map<String, SprintEvaluation> referenceEvaluations,
                                                     Map<String, SprintEvaluation> sickLeaveEvaluations,
                                                     Map<String, SickLeaveWorklogSimulator.ReplayResult> replayResults)
            throws IOException {
        SprintEvaluation                       referenceLondon = referenceEvaluations.get("London");
        SprintEvaluation                       sickLeaveLondon = sickLeaveEvaluations.get("London");
        SickLeaveWorklogSimulator.ReplayResult replay          = replayResults.get("London");
        if (referenceLondon == null || sickLeaveLondon == null || replay == null) {
            log.info("Skipping Kristen Hubbell worklog comparison: a started London sprint is not available.");
            return;
        }

        StringBuilder csv = new StringBuilder();
        appendCsvRow(csv, "version", "sprint-id", "user", "task-id", "worklog-start", "time-spent",
                "remaining-estimate");
        for (Map.Entry<String, Task> entry : referenceLondon.tasksByKey().entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).toList()) {
            Task task = entry.getValue();
            if (isKristenHubbell(task)) {
                task.getWorklogs().stream().sorted(Comparator.comparing(Worklog::getStart))
                        .forEach(worklog -> appendCsvRow(csv, "reference", "London", task.getAssignedUser().getName(),
                                entry.getKey(), worklog.getStart(), worklog.getTimeSpent(),
                                worklog.getTimeRemainingEstimate()));
            }
        }
        for (Map.Entry<String, Task> entry : sickLeaveLondon.tasksByKey().entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).toList()) {
            Task task = entry.getValue();
            if (isKristenHubbell(task)) {
                SickLeaveWorklogSimulator.TaskReplay taskReplay = replay.tasksById().get(task.getId());
                if (taskReplay == null) {
                    throw new IllegalStateException("Replay did not return task '" + entry.getKey() + "' in London.");
                }
                for (SickLeaveWorklogSimulator.ReplayWorklog worklog : taskReplay.worklogs()) {
                    appendCsvRow(csv, "simulation", "London", task.getAssignedUser().getName(), entry.getKey(),
                            worklog.start(), worklog.timeSpent(), worklog.remainingEstimate());
                }
            }
        }

        Path outputFile = Path.of("test-results", "sick-leave-impact",
                "case-" + randomCase.getTestCaseIndex() + "-London-Kristen-Hubbell-worklogs.csv");
        Files.writeString(outputFile, csv, StandardCharsets.UTF_8);
        log.info("Wrote Kristen Hubbell's London reference/simulation worklogs to {}", outputFile.toAbsolutePath());
    }

    private record SprintEvaluation(Sprint sprint, SickLeaveImpactContributor.SprintImpact impact,
                                    Map<String, Task> tasksByKey) {
    }

}
