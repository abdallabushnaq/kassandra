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

package de.bushnaq.abdalla.kassandra.util;

import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.config.DefaultEntitiesInitializer;
import de.bushnaq.abdalla.kassandra.dto.OffDayType;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.Worklog;
import de.bushnaq.abdalla.kassandra.rest.api.SprintApi;
import de.bushnaq.abdalla.kassandra.rest.api.TaskApi;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import de.bushnaq.abdalla.kassandra.rest.api.WorklogApi;
import de.bushnaq.abdalla.kassandra.service.GanttBurndownChartService;
import de.bushnaq.abdalla.util.date.DateUtil;
import de.bushnaq.abdalla.util.date.ReportUtil;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

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
import java.util.function.Supplier;

@Slf4j
public final class SimulatorUtil {
    private SimulatorUtil() {
    }

    /**
     * Exports a database snapshot and moves it into the supplied temporary directory.
     *
     * @param databaseStateManager database snapshot exporter
     * @param temporaryDirectory   directory for the retained snapshot
     * @param snapshotNamePrefix   prefix used to identify the exported snapshot
     * @return the path to the retained snapshot
     * @throws IOException if the snapshot cannot be moved into the temporary directory
     */
    public static Path createInitialDatabaseSnapshot(H2DatabaseStateManager databaseStateManager,
                                                     Path temporaryDirectory, String snapshotNamePrefix)
            throws IOException {
        Files.createDirectories(temporaryDirectory);
        String snapshotName = snapshotNamePrefix + UUID.randomUUID();
        Path exported = Path.of(databaseStateManager.exportDatabaseSnapshot(snapshotName));
        Path temporary = temporaryDirectory.resolve(exported.getFileName());
        return Files.move(exported, temporary, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Restores a database snapshot after clearing the persistence context.
     *
     * @param databaseStateManager database snapshot importer
     * @param entityManager         persistence context to clear before and after import
     * @param snapshot              snapshot file to import
     * @throws IllegalStateException if the snapshot cannot be restored
     */
    public static void restoreInitialDatabaseSnapshot(H2DatabaseStateManager databaseStateManager,
                                                       EntityManager entityManager, Path snapshot) {
        entityManager.clear();
        if (!databaseStateManager.importDatabaseSnapshot(snapshot.toString())) {
            throw new IllegalStateException("Could not restore database snapshot from: " + snapshot);
        }
        entityManager.clear();
    }

    /**
     * Appends a CSV row with every value escaped and quoted.
     *
     * @param csv    destination buffer
     * @param values values to append as columns
     */
    public static void appendCsvRow(StringBuilder csv, Object... values) {
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

    /**
     * Appends a summary row containing the sprint's current effort and schedule delays.
     *
     * @param csv    destination buffer
     * @param sprint sprint to summarize
     * @param now    timestamp at which to calculate the delays
     */
    public static void appendSprintSummaryRow(StringBuilder csv, Sprint sprint, LocalDateTime now) {
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

    /**
     * Returns the first non-null worklog date for a task.
     *
     * @param task task whose worklogs are inspected
     * @return the first date, or {@code null} if the task has no dated worklogs
     */
    public static LocalDate firstWorklogDate(Task task) {
        return task.getWorklogs().stream()
                .map(Worklog::getStart)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    /**
     * Generates one chart for each sprint in name order.
     *
     * @param scenarioFolder  scenario label appended to each chart filename
     * @param sprints         sprints to render
     * @param service         chart service used by the generator
     * @param outputDirectory directory for generated chart files
     * @param visualScale     chart rendering scale
     * @throws Exception if the output directory cannot be created or chart generation fails
     */
    public static void generateScenarioCharts(String scenarioFolder, Collection<Sprint> sprints,
                                              GanttBurndownChartService service, Path outputDirectory,
                                              double visualScale) throws Exception {
        Files.createDirectories(outputDirectory);
        GanttGenerator chartGenerator = new GanttGenerator(service);
        for (Sprint sprint : sprints.stream().sorted(Comparator.comparing(Sprint::getName)).toList()) {
            String chartName = safeFileName(sprint.getName()) + "-" + scenarioFolder;
            chartGenerator.generateGanttBurndownChart(sprint, outputDirectory.toString(), chartName,
                    visualScale, false);
        }
    }

    /**
     * Returns the last non-null worklog date for a task.
     *
     * @param task task whose worklogs are inspected
     * @return the last date, or {@code null} if the task has no dated worklogs
     */
    public static LocalDate lastWorklogDate(Task task) {
        return task.getWorklogs().stream()
                .map(Worklog::getStart)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    /**
     * Checks whether a user has a sick leave day overlapping a sprint's scheduled date range.
     *
     * @param user   user whose off days are checked
     * @param sprint sprint whose date range is checked
     * @return {@code true} if a sick leave overlaps the sprint
     */
    public static boolean hasSickLeaveDuringSprint(User user, Sprint sprint) {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(sprint, "sprint");
        if (sprint.getStart() == null || sprint.getEnd() == null || user.getOffDays() == null) {
            return false;
        }

        LocalDate sprintStart = sprint.getStart().toLocalDate();
        LocalDate sprintEnd = sprint.getEnd().toLocalDate();
        return user.getOffDays().stream()
                .filter(Objects::nonNull)
                .filter(offDay -> offDay.getType() == OffDayType.SICK)
                .filter(offDay -> offDay.getFirstDay() != null && offDay.getLastDay() != null)
                .anyMatch(offDay -> !offDay.getLastDay().isBefore(sprintStart)
                        && !offDay.getFirstDay().isAfter(sprintEnd));
    }

    /**
     * Writes a reference-versus-replay worklog CSV for one user in one sprint.
     *
     * @param caseIndex                    randomized scenario index
     * @param user                         user whose worklogs are written
     * @param referenceSprint              sickness-free reference sprint
     * @param sickLeaveSprint              sprint generated with sick leave
     * @param replayWorklogsByTaskKey      sickness-free replay worklogs keyed by task key
     * @param outputDirectory              destination directory
     * @throws IOException if the output directory or CSV file cannot be written
     * @throws IllegalStateException if replay data is missing for a matching task
     */
    public static void writeUserWorklogComparison(int caseIndex, User user, Sprint referenceSprint,
                                                  Sprint sickLeaveSprint,
                                                  Map<String, List<Worklog>> replayWorklogsByTaskKey,
                                                  Path outputDirectory) throws IOException {
        String userName = user.getName();
        String sprintName = sickLeaveSprint.getName();
        StringBuilder csv = new StringBuilder();
        appendCsvRow(csv, "version", "sprint-id", "user", "task-id", "worklog-start", "time-spent",
                "remaining-estimate");

        referenceSprint.getTasks().stream()
                .filter(task -> isAssignedTo(task, userName))
                .sorted(Comparator.comparing(Task::getKey))
                .forEach(task -> task.getWorklogs().stream()
                        .sorted(Comparator.comparing(Worklog::getStart))
                        .forEach(worklog -> appendCsvRow(csv, "reference", sprintName, userName, task.getKey(),
                                worklog.getStart(), worklog.getTimeSpent(), worklog.getTimeRemainingEstimate())));

        for (Task task : sickLeaveSprint.getTasks().stream()
                .filter(candidate -> isAssignedTo(candidate, userName))
                .sorted(Comparator.comparing(Task::getKey)).toList()) {
            List<Worklog> replayWorklogs = replayWorklogsByTaskKey.get(task.getKey());
            if (replayWorklogs == null) {
                throw new IllegalStateException("Replay did not return task '" + task.getKey() + "' in sprint '"
                        + sprintName + "'.");
            }
            replayWorklogs.stream()
                    .sorted(Comparator.comparing(Worklog::getStart))
                    .forEach(worklog -> appendCsvRow(csv, "simulation", sprintName, userName, task.getKey(),
                            worklog.getStart(), worklog.getTimeSpent(), worklog.getTimeRemainingEstimate()));
        }

        Files.createDirectories(outputDirectory);
        Path outputFile = outputDirectory.resolve("case-" + caseIndex + "-" + safeFileName(sprintName) + "-"
                + safeFileName(userName) + "-worklogs.csv");
        Files.writeString(outputFile, csv, StandardCharsets.UTF_8);
        log.info("Wrote {} reference/simulation worklogs for sprint '{}' to {}", userName, sprintName,
                outputFile.toAbsolutePath());
    }

    /**
     * Loads and initializes all data required to work with a sprint.
     *
     * @param authentication authentication propagated to asynchronous API requests
     * @param sprintId       ID of the sprint to load
     * @param sprintApi      API used to load the sprint
     * @param userApi        API used to load sprint users
     * @param taskApi        API used to load sprint tasks
     * @param worklogApi     API used to load sprint worklogs
     * @return the initialized sprint
     * @throws InterruptedException if loading is interrupted
     * @throws ExecutionException   if an asynchronous API request fails
     */
    public static Sprint loadSprintData(Authentication authentication, UUID sprintId, SprintApi sprintApi,
                                        UserApi userApi, TaskApi taskApi, WorklogApi worklogApi)
            throws InterruptedException, ExecutionException {
        long time = System.currentTimeMillis();
        CompletableFuture<Sprint> sprintFuture = supplyWithAuthentication(authentication, () -> {
            Sprint sprint = sprintApi.getById(sprintId);
            sprint.initialize();
            return sprint;
        });
        CompletableFuture<List<User>> usersFuture = supplyWithAuthentication(authentication,
                () -> userApi.getAll(sprintId));
        CompletableFuture<List<Task>> tasksFuture = supplyWithAuthentication(authentication,
                () -> taskApi.getAll(sprintId));
        CompletableFuture<List<Worklog>> worklogsFuture = supplyWithAuthentication(authentication,
                () -> worklogApi.getAll(sprintId));

        Sprint sprint = sprintFuture.get();
        log.info("sprint loaded and initialized in {} ms", System.currentTimeMillis() - time);
        time = System.currentTimeMillis();
        sprint.initUserMap(usersFuture.get());
        sprint.initTaskMap(tasksFuture.get(), worklogsFuture.get());
        log.info("sprint user, task and worklog maps initialized in {} ms", System.currentTimeMillis() - time);
        sprint.recalculate(ParameterOptions.getLocalNow());
        return sprint;
    }

    /**
     * Loads every sprint except the backlog and initializes each sprint's task and user data.
     *
     * @param authentication authentication propagated to asynchronous API requests
     * @param sprintApi      API used to list sprints
     * @param userApi        API used to load sprint users
     * @param taskApi        API used to load sprint tasks
     * @param worklogApi     API used to load sprint worklogs
     * @return the loaded, initialized sprints
     * @throws InterruptedException if loading is interrupted
     * @throws ExecutionException   if an asynchronous API request fails
     */
    public static List<Sprint> loadSprints(Authentication authentication, SprintApi sprintApi, UserApi userApi,
                                           TaskApi taskApi, WorklogApi worklogApi)
            throws InterruptedException, ExecutionException {
        List<Sprint> sprints = new ArrayList<>();
        for (Sprint sprint : sprintApi.getAll()) {
            if (!sprint.getName().equals(DefaultEntitiesInitializer.BACKLOG_SPRINT_NAME)) {
                sprints.add(loadSprintData(authentication, sprint.getId(), sprintApi, userApi, taskApi, worklogApi));
            }
        }
        return sprints;
    }

    /**
     * Replaces characters that are unsafe or unsuitable in filenames with underscores.
     *
     * @param name filename value to sanitize
     * @return a filename-safe value
     */
    public static String safeFileName(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static boolean isAssignedTo(Task task, String userName) {
        return task.getAssignedUser() != null && Objects.equals(task.getAssignedUser().getName(), userName);
    }

    private static <T> CompletableFuture<T> supplyWithAuthentication(Authentication authentication,
                                                                     Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            try {
                return supplier.get();
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
    }
}
