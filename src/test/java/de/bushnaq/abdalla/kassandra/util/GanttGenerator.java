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
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.TaskStatus;
import de.bushnaq.abdalla.kassandra.dto.Worklog;
import de.bushnaq.abdalla.kassandra.report.burndown.BurnDownChart;
import de.bushnaq.abdalla.kassandra.report.burndown.RenderDao;
import de.bushnaq.abdalla.kassandra.report.dao.ETheme;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttChart;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttUtil;
import de.bushnaq.abdalla.kassandra.report.overview.SprintsOverviewChart;
import de.bushnaq.abdalla.profiler.Profiler;
import de.bushnaq.abdalla.profiler.SampleType;
import de.bushnaq.abdalla.util.GanttErrorHandler;
import de.bushnaq.abdalla.util.Util;
import de.bushnaq.abdalla.util.date.DateUtil;
import net.sf.mpxj.ProjectFile;
import org.junit.jupiter.api.TestInfo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GanttGenerator extends MPXJGenerator {

    protected final Random random    = new Random();
    /**
     * Theme used for Gantt chart generation in tests.
     * Defaults to {@link ETheme#dark} to preserve pre-existing test reference images.
     * Tests that need to switch theme (e.g. {@code CriticalTest}) set this field directly.
     */
    public          ETheme testTheme = ETheme.dark;

    private RenderDao createRenderDao(Context context, Sprint sprint, String column, LocalDateTime now, int chartWidth, int chartHeight, String link) {
        RenderDao dao = new RenderDao();
        dao.context            = context;
        dao.column             = column;
        dao.sprintName         = column + "-burn-down";
        dao.link               = link;
        dao.start              = sprint.getStart();
        dao.now                = now;
        dao.end                = sprint.getEnd();
        dao.release            = sprint.getReleaseDate();
        dao.chartWidth         = chartWidth;
        dao.chartHeight        = chartHeight;
        dao.sprint             = sprint;
        dao.estimatedBestWork  = DateUtil.add(sprint.getWorked(), sprint.getRemaining());
        dao.estimatedWorstWork = null;
        dao.maxWorked          = DateUtil.add(sprint.getWorked(), sprint.getRemaining());
        dao.remaining          = sprint.getRemaining();
        dao.worklog            = sprint.getWorklogs();
        dao.worklogRemaining   = sprint.getWorklogRemaining();
        dao.cssClass           = "scheduleWithMargin";
        dao.kassandraTheme     = context.parameters.getActiveGraphicsTheme();
        return dao;
    }

    public void generateBurndownChart(TestInfo testInfo, Long sprintId, String testFolder) throws Exception {
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
        RenderDao     dao         = createRenderDao(context, sprint, sprintName, ParameterOptions.getLocalNow(), 0, 36 * 10,  /*urlPrefix +*/ "sprint-" + sprint.getId() + "/sprint.html");
        BurnDownChart chart       = new BurnDownChart("/", dao);
        String        description = testInfo.getDisplayName().replace("_", "-");
        chart.render(Util.generateCopyrightString(ParameterOptions.getLocalNow()), description, testFolder);
    }

    public void generateGanttChart(TestInfo testInfo, long sprintId, String testFolder) throws Exception {
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
        GanttChart chart = new GanttChart(context, "", "/", "Gantt Chart", sprintName, exceptions, ParameterOptions.getLocalNow(), false, sprint/*, 1887, 1000*/, "scheduleWithMargin", context.parameters.getActiveGraphicsTheme());
//        String     description = testCaseInfo.getDisplayName().replace("_", "-");
        String description = TestInfoUtil.getTestMethodName(testInfo);
        chart.render(Util.generateCopyrightString(ParameterOptions.getLocalNow()), description, testFolder);
    }

    public void generateOverviewChart(TestInfo testInfo, Long sprintId, String testFolder) throws Exception {
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
     * Generates worklogs for the tasks in the sprint simulating a team of people working.
     *
     * @param sprint
     * @param now
     */
    public void generateWorklogs(long sprintId, LocalDateTime now) {
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
                    if (task.isTask()) {
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

//                                        task.addTimeSpent(savedWorklog.getTimeSpent());
//                                        task.setRemainingEstimate(timeRemaining);
//                                        task.recalculate();


                                        task.addTimeSpent(w);
                                        task.removeRemainingEstimate(w);
                                        task.recalculate();
                                        task.setTaskStatus(TaskStatus.IN_PROGRESS);
                                    } else {
                                        task.setTaskStatus(TaskStatus.DONE);
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

//        if (projectFile == null) {
//            try (Profiler pc = new Profiler(SampleType.FILE)) {
//                storeExpectedResult(testInfo, sprint);
//                storeResult(testInfo, sprint);
//            }
//        }
    }


//    protected GanttContext initializeInstances() throws Exception {
//        GanttContext gc = new GanttContext();
//        gc.allUsers    = new ArrayList<>(users);
//        gc.allProducts = new ArrayList<>(products);
//        gc.allVersions = new ArrayList<>(versions);
//        gc.allFeatures = new ArrayList<>(features);
//        gc.allSprints  = new ArrayList<>(sprints);
//        gc.allTasks    = new ArrayList<>(tasks);
//        gc.allWorklogs = new ArrayList<>(worklogs);
//        gc.initialize();
//
//        return gc;
//    }

}
