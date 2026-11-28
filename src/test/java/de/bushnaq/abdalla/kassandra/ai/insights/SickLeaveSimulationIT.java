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

import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.Worklog;
import de.bushnaq.abdalla.kassandra.service.GanttBurndownChartService;
import de.bushnaq.abdalla.kassandra.service.SickLeaveImpactContributor;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.util.*;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static de.bushnaq.abdalla.kassandra.util.SimulatorUtil.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
public class SickLeaveSimulationIT extends AbstractKeycloakUiTestUtil {
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
        Path worklogOutputDirectory = Path.of("test-results", "sick-leave-impact");
        for (String sprintName : sickLeaveEvaluations.keySet().stream().sorted().toList()) {
            SprintEvaluation                       referenceEvaluation     = referenceEvaluations.get(sprintName);
            SprintEvaluation                       sickLeaveEvaluation     = sickLeaveEvaluations.get(sprintName);
            SickLeaveWorklogSimulator.ReplayResult replay                  = replayResults.get(sprintName);
            Map<String, List<Worklog>>             replayWorklogsByTaskKey = new LinkedHashMap<>();
            for (Map.Entry<String, Task> entry : sickLeaveEvaluation.tasksByKey().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()).toList()) {
                SickLeaveWorklogSimulator.TaskReplay taskReplay = replay.tasksById().get(entry.getValue().getId());
                if (taskReplay == null) {
                    throw new IllegalStateException("Replay did not return task '" + entry.getKey()
                            + "' in sprint '" + sprintName + "'.");
                }
                List<Worklog> replayWorklogs = taskReplay.worklogs().stream().map(replayWorklog -> {
                    Worklog worklog = new Worklog();
                    worklog.setStart(replayWorklog.start());
                    worklog.setTimeSpent(replayWorklog.timeSpent());
                    worklog.setTimeRemainingEstimate(replayWorklog.remainingEstimate());
                    return worklog;
                }).toList();
                replayWorklogsByTaskKey.put(entry.getKey(), replayWorklogs);
            }

            for (User user : sickLeaveEvaluation.sprint().getUserMap().values().stream()
                    .sorted(Comparator.comparing(User::getName)).toList()) {
                if (SimulatorUtil.hasSickLeaveDuringSprint(user, sickLeaveEvaluation.sprint())) {
                    SimulatorUtil.writeUserWorklogComparison(randomCase.getTestCaseIndex(), user,
                            referenceEvaluation.sprint(), sickLeaveEvaluation.sprint(), replayWorklogsByTaskKey,
                            worklogOutputDirectory);
                }
            }
        }

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

    private Map<String, SprintEvaluation> evaluateSprints(RandomCase randomCase) throws Exception {
        SnapshotLogGenerator snapshotLogGenerator = new SnapshotLogGenerator(
                Path.of("test-database-snapshots", randomCase.generateName() + ".log"));
        Map<String, SprintEvaluation> evaluations = new HashMap<>();
        List<Sprint> sprints = SimulatorUtil.loadSprints(SecurityContextHolder.getContext().getAuthentication(),
                peg.sprintApi, peg.userApi, peg.taskApi, peg.worklogApi);
        for (Sprint sprint : sprints) {
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

    private String progressPercent(Float progress) {
        return progress == null ? null : String.format(Locale.ROOT, "%.0f%%", progress * 100);
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
    public void testSickLeaveImpactAgainstZeroUnderestimation(RandomCase randomCase, TestInfo testInfo) throws Exception {
        String testMethodName = testInfo.getTestMethod().orElseThrow().getName() + "-sick-leave-impact-" + randomCase.getTestCaseIndex();
        TestInfoUtil.setTestMethod(testInfo, testMethodName);
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testMethodName);

        // Generate the reference first without sick leave, keeping the original seed and all other data settings.
        RandomCase reference = randomCase.withProbabilities(builder -> builder.sickSprintProbability(0f).worklogDelayProbability(0f));

        // Save the clean post-Keycloak fixture so each scenario starts without the other scenario's data.
        Path initialDatabaseSnapshot = SimulatorUtil.createInitialDatabaseSnapshot(databaseStateManager, temporaryDirectory, "sick-leave-impact-baseline-");
        SimulatorUtil.restoreInitialDatabaseSnapshot(databaseStateManager, entityManager, initialDatabaseSnapshot);

        // Keep the actual sickness-free worklogs in memory as the expected replay result.
        generateProductsIfNeeded(testInfo, reference, true);
        Map<String, SprintEvaluation> referenceEvaluations = evaluateSprints(reference);
        String                        caseName             = caseName(reference);
        generateScenarioCharts("0-percent-sick-leave", referenceEvaluations.values().stream().map(SprintEvaluation::sprint).toList(), ganttBurndownChartService, Path.of("test-results", "sick-leave-impact"), CHART_VISUAL_SCALE);

        // Restore the clean database before generating the sick-leave scenario.
        SimulatorUtil.restoreInitialDatabaseSnapshot(databaseStateManager, entityManager, initialDatabaseSnapshot);
        peg.eg.init();
        peg.nameGenerator = new NameGenerator();
        peg.getOffDays().clear();

        // Generate the same seeded data with sick leave on every eligible sprint.
        RandomCase sickLeaveCase = randomCase.withProbabilities(builder -> builder.sickSprintProbability(1f).worklogDelayProbability(0f));
        generateProductsIfNeeded(testInfo, sickLeaveCase, true);
        Map<String, SprintEvaluation> sickLeaveEvaluations = evaluateSprints(sickLeaveCase);
        assertEquals(referenceEvaluations.keySet(), sickLeaveEvaluations.keySet(), "Both snapshots should contain the same sprints");
        generateScenarioCharts("100-percent-sick-leave", sickLeaveEvaluations.values().stream().map(SprintEvaluation::sprint).toList(), ganttBurndownChartService, Path.of("test-results", "sick-leave-impact"), CHART_VISUAL_SCALE);

        // Replay sick-snapshot worklogs with sickness removed and emit paired diagnostics before asserting equality.
        compareReplayWithReference(sickLeaveCase, referenceEvaluations, sickLeaveEvaluations);
    }

}
