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

import de.bushnaq.abdalla.kassandra.Context;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.Relation;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.Worklog;
import de.bushnaq.abdalla.kassandra.report.GanttBurndown.GanttBurndownChart;
import de.bushnaq.abdalla.kassandra.report.burndown.BurnDownChart;
import de.bushnaq.abdalla.kassandra.report.burndown.RenderDao;
import de.bushnaq.abdalla.kassandra.report.dao.CalendarSize;
import de.bushnaq.abdalla.kassandra.report.dao.ETheme;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttChart;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttUtil;
import de.bushnaq.abdalla.kassandra.report.overview.SprintsOverviewChart;
import de.bushnaq.abdalla.kassandra.ui.util.RenderUtil;
import de.bushnaq.abdalla.profiler.Profiler;
import de.bushnaq.abdalla.profiler.SampleType;
import de.bushnaq.abdalla.util.GanttErrorHandler;
import de.bushnaq.abdalla.util.Util;
import de.bushnaq.abdalla.util.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import net.sf.mpxj.ProjectFile;
import org.junit.jupiter.api.TestInfo;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static de.bushnaq.abdalla.kassandra.report.burndown.BurnDownRenderer.Y_AXIS_WIDTH;

@Slf4j
public class GanttGenerator extends MPXJGenerator {

    protected final Random random    = new Random();
    /**
     * Theme used for Gantt chart generation in tests.
     * Defaults to {@link ETheme#dark} to preserve pre-existing test reference images.
     * Tests that need to switch theme (e.g. {@code CriticalTest}) set this field directly.
     */
    public          ETheme testTheme = ETheme.dark;

    /**
     * Returns {@code true} when all predecessors of the given task—including predecessors
     * declared on any ancestor task in the parent hierarchy—have a zero
     * {@code remainingEstimate} (i.e. are done).
     * <p>
     * Predecessors that cannot be resolved within the sprint (e.g. cross-sprint relations)
     * are treated as done so they do not block execution.
     * </p>
     *
     * @param task   the task to check
     * @param sprint the sprint that owns the task
     * @return {@code true} if no unfinished predecessors exist; {@code false} otherwise
     */
    private boolean areAllPredecessorsDone(Task task, Sprint sprint) {
        Task cursor = task;
        while (cursor != null) {
            for (Relation relation : cursor.getPredecessors()) {
                Task predecessor = sprint.getTaskById(relation.getPredecessorId());
                if (predecessor != null && !predecessor.getRemainingEstimate().isZero()) {
                    return false;
                }
            }
            cursor = cursor.getParentTask();
        }
        return true;
    }

    public void generateBurndownChart(TestInfo testInfo, UUID sprintId, String testFolder) throws Exception {
        Sprint sprint = sprints.stream().filter(s -> s.getId() == sprintId).findFirst().orElseThrow(() -> new IllegalArgumentException("Sprint with id " + sprintId + " not found"));
        sprint.initialize();
        sprint.initUserMap(users);
        sprint.initTaskMap(tasks, worklogs);
        sprint.recalculate(ParameterOptions.getLocalNow());
//        Context       context     = new Context(new KassandraParameterOptions(new LightTheme(null), new DarkTheme(null)));
        Context context = new Context(null);
        context.parameters.setTheme(testTheme);
        String sprintName;
        if (testInfo.getDisplayName().indexOf('=') != -1) {
            sprintName = testInfo.getDisplayName().substring(testInfo.getDisplayName().indexOf('"') + 1, testInfo.getDisplayName().lastIndexOf('"')) + "-" + context.parameters.getActiveGraphicsTheme().themeVariance.name();
        } else {
            sprintName = testInfo.getDisplayName() + "-" + context.parameters.getActiveGraphicsTheme().themeVariance.name();
        }
        RenderDao     dao         = RenderUtil.createBurndownRenderDao(context, sprint, sprintName, ParameterOptions.getLocalNow(), 0, 36 * 10,  /*urlPrefix +*/ "sprint-" + sprint.getId() + "/sprint.html", Y_AXIS_WIDTH);
        BurnDownChart chart       = new BurnDownChart("/", dao);
        String        description = testInfo.getDisplayName().replace("_", "-");
        chart.render(Util.generateCopyrightString(ParameterOptions.getLocalNow()), description, testFolder);
    }

    public void generateGanttBurndownChart(TestInfo testInfo, UUID sprintId, String testFolder) throws Exception {
        Sprint sprint = sprints.stream().filter(s -> s.getId() == sprintId).findFirst().orElseThrow(() -> new IllegalArgumentException("Sprint with id " + sprintId + " not found"));
        sprint.initialize();
        sprint.initUserMap(users);
        sprint.initTaskMap(tasks, worklogs);
        sprint.recalculate(ParameterOptions.getLocalNow());
        Context context = new Context(null);
        context.parameters.setTheme(testTheme);
        String sprintName;
        if (testInfo.getDisplayName().indexOf('=') != -1) {
            sprintName = testInfo.getDisplayName().substring(testInfo.getDisplayName().indexOf('"') + 1, testInfo.getDisplayName().lastIndexOf('"')) + "-" + context.parameters.getActiveGraphicsTheme().themeVariance.name() + "-gant-burndown-chart";
        } else {
            sprintName = testInfo.getDisplayName() + "-" + context.parameters.getActiveGraphicsTheme().themeVariance.name() + "-gant-chart";
        }
        RenderDao          burndownDao = RenderUtil.createBurndownRenderDao(context, sprint, "gannt-burn-down", ParameterOptions.getLocalNow(), 640, 400, "sprint-" + sprint.getId() + "/sprint.html", Y_AXIS_WIDTH);
        RenderDao          ganttDao    = RenderUtil.createGanttRenderDao(context, sprint, "gannt-burn-down", ParameterOptions.getLocalNow(), 640, 400, "sprint-" + sprint.getId() + "/sprint.html", Y_AXIS_WIDTH, CalendarSize.DAYS);
        GanttBurndownChart chart       = new GanttBurndownChart("/", burndownDao, ganttDao);
//        GanttBurndownChart chart = new GanttBurndownChart(context, "", "/", "Gantt Burndown Chart", sprintName, exceptions, ParameterOptions.getLocalNow(), false, sprint/*, 1887, 1000*/, "scheduleWithMargin", context.parameters.getActiveGraphicsTheme());
//        String     description = testCaseInfo.getDisplayName().replace("_", "-");
        String description = TestInfoUtil.getTestMethodName(testInfo);
        chart.render(Util.generateCopyrightString(ParameterOptions.getLocalNow()), description, testFolder);
    }

    public void generateGanttChart(TestInfo testInfo, UUID sprintId, String testFolder) throws Exception {
        Sprint sprint = sprints.stream().filter(s -> s.getId() == sprintId).findFirst().orElseThrow(() -> new IllegalArgumentException("Sprint with id " + sprintId + " not found"));
        sprint.initialize();
        sprint.initUserMap(users);
        sprint.initTaskMap(tasks, worklogs);
        sprint.recalculate(ParameterOptions.getLocalNow());
        Context context = new Context(null);
        context.parameters.setTheme(testTheme);
        String sprintName;
        if (testInfo.getDisplayName().indexOf('=') != -1) {
            sprintName = testInfo.getDisplayName().substring(testInfo.getDisplayName().indexOf('"') + 1, testInfo.getDisplayName().lastIndexOf('"')) + "-" + context.parameters.getActiveGraphicsTheme().themeVariance.name() + "-gant-chart";
        } else {
            sprintName = testInfo.getDisplayName() + "-" + context.parameters.getActiveGraphicsTheme().themeVariance.name() + "-gant-chart";
        }
        RenderDao  dao   = RenderUtil.createGanttRenderDao(context, sprint, sprintName, ParameterOptions.getLocalNow(), 640, 400, "sprint-" + sprint.getId() + "/sprint.html", 0, CalendarSize.YEARS);
        GanttChart chart = new GanttChart("/", dao);
//        GanttChart chart = new GanttChart(context, "", "/", "Gantt Chart", sprintName, exceptions, ParameterOptions.getLocalNow(), false, sprint/*, 1887, 1000*/, "scheduleWithMargin", context.parameters.getActiveGraphicsTheme());
//        String     description = testCaseInfo.getDisplayName().replace("_", "-");
        String description = TestInfoUtil.getTestMethodName(testInfo);
        chart.render(Util.generateCopyrightString(ParameterOptions.getLocalNow()), description, testFolder);
    }

    public void generateOverviewChart(TestInfo testInfo, UUID sprintId, String testFolder) throws Exception {
        Sprint sprint = sprints.stream().filter(s -> s.getId() == sprintId).findFirst().orElseThrow(() -> new IllegalArgumentException("Sprint with id " + sprintId + " not found"));
        sprint.initialize();
        sprint.initUserMap(users);
        sprint.initTaskMap(tasks, worklogs);
        sprint.recalculate(ParameterOptions.getLocalNow());

        List<Sprint> sprintList = new ArrayList<>();
        sprintList.add(sprint);

        Context context = new Context(null);
        context.parameters.setTheme(testTheme);
        String sprintName;
        if (testInfo.getDisplayName().indexOf('=') != -1) {
            sprintName = testInfo.getDisplayName().substring(testInfo.getDisplayName().indexOf('"') + 1, testInfo.getDisplayName().lastIndexOf('"')) + "-" + context.parameters.getActiveGraphicsTheme().themeVariance.name() + "-overview-chart";
        } else {
            sprintName = testInfo.getDisplayName() + "-" + context.parameters.getActiveGraphicsTheme().themeVariance.name() + "-overview-chart";
        }
        SprintsOverviewChart chart       = new SprintsOverviewChart(context, "", "/", sprintName, null, ParameterOptions.getLocalNow(), sprintList, 1887, 1000, "scheduleWithMargin", context.parameters.getActiveGraphicsTheme());
        String               description = TestInfoUtil.getTestMethodName(testInfo);
        chart.render(Util.generateCopyrightString(ParameterOptions.getLocalNow()), description, testFolder);
    }

    /**
     * Generates worklogs for the tasks in the sprint, simulating a team working day-by-day.
     * <p>
     * <b>Estimate inflation (delay &gt; 0):</b> before the day-loop starts, each leaf task's
     * {@code remainingEstimate} is inflated in-memory to a value sampled from
     * {@code [maxEstimate, maxEstimate * (1 + delay)]}, so that delayed sprints exceed the
     * worst-case estimate.  Tasks in sprints with {@code delay == 0} keep their original
     * {@code minEstimate}.
     * </p>
     * <p>
     * <b>Dependency gate:</b> work is only logged for a task when all of its predecessors—
     * including predecessors declared on any ancestor task in the hierarchy—have a zero
     * {@code remainingEstimate} (i.e. are effectively done).
     * </p>
     *
     * @param sprint the sprint to generate worklogs for
     * @param delay  over-run factor (0 = no over-run; 0.3 = up to 30 % above {@code maxEstimate})
     * @param now    the simulated current date/time; work is only logged for days before this date
     */
    @Transactional
    public void generateWorklogs(Sprint sprint, float delay, LocalDateTime now) {
        try (Profiler pc = new Profiler(SampleType.CPU)) {

            final long SECONDS_PER_WORKING_DAY = 75 * 6 * 60;

            // Step 1: pre-inflate remaining estimates when this sprint is delayed.
            if (delay > 0f) {
                for (Task task : sprint.getTasks()) {
                    if (task.isTask() && task.isImpactOnCost()) {
                        Duration maxEstimate = task.getMaxEstimate();
                        if (maxEstimate != null && !maxEstimate.isZero()) {
                            long     extraSeconds = (long) (delay * maxEstimate.getSeconds() * random.nextFloat());
                            Duration inflated     = maxEstimate.plusSeconds(extraSeconds);
                            log.debug("Inflating task '{}': minEstimate={}, inflated remainingEstimate={} (delay={})", task.getName(), task.getMinEstimate(), inflated, delay);
                            task.setRemainingEstimate(inflated);
                        }
                    }
                }
            }
            printTasks(sprint);
            Duration rest = Duration.ofSeconds(1);
            // Step 2: iterate over the days of the sprint
            for (LocalDate day = sprint.getStart().toLocalDate(); !rest.equals(Duration.ZERO) && now.toLocalDate().isAfter(day); day = day.plusDays(1)) {
                LocalDateTime startOfDay = day.atStartOfDay().plusHours(8);
                rest = Duration.ZERO;
                // iterate over all tasks
                for (Task task : sprint.getTasks()) {
                    if (task.isTask() && task.isImpactOnCost()) {
                        Number availability = task.getAvailability();
                        if (!day.isBefore(task.getStart().toLocalDate())) {
                            // Day is on or after task start
                            if (task.getEffectiveCalendar().isWorkingDate(day)) {
                                // is a working day for this user
                                if (task.getStart().isBefore(startOfDay) || task.getStart().isEqual(startOfDay)) {
                                    if (!task.getRemainingEstimate().isZero()) {
                                        if (areAllPredecessorsDone(task, sprint)) {
                                            // we have the whole day
                                            double performance = 1f;//daily performance is usually 100% of the resource availability
                                            if (random.nextFloat() < 0.2f) {
                                                //in rare cases, performance can be much worse or better than usual, e.g. due to unexpected problems or overtime
                                                double minPerformance = 0.5f;//minimum performance of a resource (underwork)
                                                double maxPerformance = 1.2f;//maximum performance of a resource (overwork)
                                                performance = minPerformance + random.nextFloat() * (maxPerformance - minPerformance);
                                            }
                                            Duration maxWork = Duration.ofSeconds((long) ((performance * availability.doubleValue() * SECONDS_PER_WORKING_DAY)));
                                            Duration w       = maxWork;
                                            Duration delta   = task.getRemainingEstimate().minus(w);
                                            if (delta.isZero() || delta.isPositive()) {
                                            } else {
                                                w = task.getRemainingEstimate();
                                            }
                                            Worklog worklog = addWorklog(task, task.getAssignedUser(), DateUtil.localDateTimeToOffsetDateTime(day.atStartOfDay()), w, task.getName());
                                            task.calculateStatus();
                                        } else {
                                            log.debug("Task '{}' blocked on {} – predecessor not yet done", task.getName(), day);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    rest = rest.plus(task.getRemainingEstimate()); // accumulate the rest
                }
            }
            printTasks(sprint);
            sprint.recalculate(ParameterOptions.getLocalNow());
        }
//        flushWorklogBuffer(sprint);
//        persistTasksAndSprint(sprint);
    }

    /**
     * Generates worklogs for the tasks in the sprint simulating a team of people working.
     *
     * @param sprint
     * @param now
     */
    public void generateWorklogs(UUID sprintId, LocalDateTime now) {
        Sprint sprint = sprints.stream().filter(s -> s.getId() == sprintId).findFirst().orElseThrow(() -> new IllegalArgumentException("Sprint with id " + sprintId + " not found"));
        try (Profiler pc = new Profiler(SampleType.CPU)) {

            final long         SECONDS_PER_WORKING_DAY = 75 * 6 * 60;
            final long         SECONDS_PER_HOUR        = 60 * 60;
            long               oneDay                  = 75 * SECONDS_PER_HOUR / 10;
            java.time.Duration rest                    = java.time.Duration.ofSeconds(1);
            //- iterate over the days of the sprint
            for (LocalDate day = sprint.getStart().toLocalDate(); !rest.equals(java.time.Duration.ZERO) && now.toLocalDate().isAfter(day); day = day.plusDays(1)) {
                LocalDateTime startOfDay     = day.atStartOfDay().plusHours(8);
                LocalDateTime endOfDay       = day.atStartOfDay().plusHours(16).plusMinutes(30);
                LocalDateTime lunchStartTime = DateUtil.calculateLunchStartTime(day.atStartOfDay());
                LocalDateTime lunchStopTime  = DateUtil.calculateLunchStopTime(day.atStartOfDay());
                rest = java.time.Duration.ZERO;
                //iterate over all tasks
                for (de.bushnaq.abdalla.kassandra.dto.Task task : sprint.getTasks()) {
                    if (task.isTask() && task.isImpactOnCost()) {
                        Number availability = task.getAvailability();
                        if (!day.isBefore(task.getStart().toLocalDate())) {
                            // Day is after task start
                            if (task.getEffectiveCalendar().isWorkingDate(day)) {
                                //is a working day for this user
                                if (task.getStart().isBefore(startOfDay) || task.getStart().isEqual(startOfDay)) {
                                    if (!task.getRemainingEstimate().isZero()) {
                                        // we have the whole day
                                        double             minPerformance = 0.6f;
                                        double             fraction       = minPerformance + random.nextFloat() * (1 - minPerformance) * 1.2;
                                        java.time.Duration maxWork        = java.time.Duration.ofSeconds((long) ((fraction * availability.doubleValue() * SECONDS_PER_WORKING_DAY)));
                                        java.time.Duration w              = maxWork;
                                        java.time.Duration delta          = task.getRemainingEstimate().minus(w);
                                        if (delta.isZero() || delta.isPositive()) {
                                        } else {
                                            w = task.getRemainingEstimate();
                                        }
                                        Worklog worklog = addWorklog(task, task.getAssignedUser(), DateUtil.localDateTimeToOffsetDateTime(day.atStartOfDay()), w, task.getName());

                                        task.calculateStatus();
                                    }
                                }
                            }
                        }
                    }
                    rest = rest.plus(task.getRemainingEstimate());//accumulate the rest
                }
            }
        }
    }

    public void initializeSprint(Sprint sprint) {
        sprint.initialize();
        sprint.initUserMap(users);
        sprint.initTaskMap(tasks, worklogs);
    }

    public void levelResources(TestInfo testInfo, Sprint sprint, ProjectFile projectFile) throws Exception {
//        initializeInstances();
        GanttUtil         ganttUtil = new GanttUtil();
        GanttErrorHandler eh        = new GanttErrorHandler();
        ganttUtil.levelResources(eh, sprint, "", ParameterOptions.getLocalNow());
    }

    private void printTasks(Sprint sprint) {
        log.info("---------------------------");
        for (Task task : sprint.getTasks()) {
            if (!task.isStory() && !task.isDeliveryBufferTask() && !task.isMilestone())
                log.info("Task '{}': minEstimate={}, maxEstimate={}, spent={}, remainingEstimate={}", task.getName(), task.getMinEstimate(), task.getMaxEstimate(), task.getTimeSpent(), task.getRemainingEstimate());
        }
        log.info("---------------------------");
    }


}
