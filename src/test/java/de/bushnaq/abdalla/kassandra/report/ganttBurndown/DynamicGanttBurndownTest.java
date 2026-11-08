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

package de.bushnaq.abdalla.kassandra.report.ganttBurndown;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionConfig;
import de.bushnaq.abdalla.kassandra.config.KassandraProperties;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.report.dao.ETheme;
import de.bushnaq.abdalla.kassandra.report.dao.theme.DarkTheme;
import de.bushnaq.abdalla.kassandra.report.dao.theme.LightTheme;
import de.bushnaq.abdalla.kassandra.report.gantt.AbstractGanttTester;
import de.bushnaq.abdalla.kassandra.rest.dto.gantt.AuthorSeriesDto;
import de.bushnaq.abdalla.kassandra.rest.dto.gantt.GanttBurndownChartDto;
import de.bushnaq.abdalla.kassandra.rest.dto.gantt.TaskDto;
import de.bushnaq.abdalla.kassandra.service.GanttBurndownChartService;
import de.bushnaq.abdalla.kassandra.service.GanttChartService;
import de.bushnaq.abdalla.kassandra.util.GanttGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * loading mpp files once with all start/finish/duration information into a reference sprint, then
 * loading mpp files once without start/finish/duration information into a sprint.
 * then leveling the sprint resources
 * then comparing both sprints.
 * <p>
 * This ensures that leveling  resources matches what ms project does, and that the generated gantt chart matches what ms project generates.
 */
@Tag("UnitTest")
@Slf4j
public class DynamicGanttBurndownTest extends AbstractGanttTester {
    private final GanttBurndownChartService service    = createChartService();
    private final String                    testFolder = "references/DynamicGanttBurndownTest";

    @BeforeEach
    public void beforeEach() {
        Logger logger = (Logger) LoggerFactory.getLogger("de.bushnaq");
        logger.setLevel(Level.TRACE);
        ParameterOptions.setNow(OffsetDateTime.parse("2025-06-05T08:00:00+01:00"));
    }

    private static GanttBurndownChartService createChartService() {
        StableDiffusionConfig config       = new StableDiffusionConfig();
        LightTheme            lightTheme   = new LightTheme(config);
        DarkTheme             darkTheme    = new DarkTheme(config);
        GanttChartService     ganttService = new GanttChartService(lightTheme, darkTheme, new KassandraProperties());
        return new GanttBurndownChartService(lightTheme, darkTheme, ganttService);
    }

    private void generateGanttBurndownChart(TestInfo testInfo, GanttGenerator g, Sprint sprint) throws Exception {
        g.testTheme = ETheme.light;
        g.generateGanttBurndownChart(testInfo, sprint.getId(), testFolder, 1.0);
        g.testTheme = ETheme.dark;
        g.generateGanttBurndownChart(testInfo, sprint.getId(), testFolder, 1.0);
    }

    private void generateGanttChart(TestInfo testInfo, GanttGenerator g, Sprint sprint) throws Exception {
        g.testTheme = ETheme.light;
        g.generateGanttChart(testInfo, sprint.getId(), testFolder, 1.0);
        g.testTheme = ETheme.dark;
        g.generateGanttChart(testInfo, sprint.getId(), testFolder, 1.0);
    }

    @Test
    public void test(TestInfo testInfo) throws Exception {
        GanttGenerator g         = new GanttGenerator(service);
        User           resource1 = g.addUser("resource1", 0.3f);
        User           resource2 = g.addUser("resource2", 0.7f);
        Sprint         sprint    = g.addSprint();

        Task task1 = g.addParentTask("[1] Parent Task", sprint, null, null);
        Task task2 = g.addTask("[2] Child Task ", "5d", null, resource1, sprint, task1, null);
        Task task3 = g.addTask("[3] Child Task ", "5d", null, resource2, sprint, task1, task2);

        g.initializeSprint(sprint);
        g.levelResources(testInfo, sprint, null);

        Task startMilestone = g.addTask(sprint, null, "Start", LocalDateTime.parse("2025-05-05T08:00:00"), null, Duration.ZERO, null, null, TaskMode.MANUALLY_SCHEDULED, true);
        task1.getPredecessors().add(new Relation(startMilestone.getId(), true));

        g.createDeliveryBufferTask(sprint, Duration.ZERO);
        g.levelResources(testInfo, sprint, null);


        generateGanttChart(testInfo, g, sprint);

        assertEquals(task1.getStart(), LocalDateTime.parse("2025-05-05T08:00:00"));
    }

    @Test
    public void testDeliveryBuffer1(TestInfo testInfo) throws Exception {
        testTheme = ETheme.light;
        GanttGenerator g         = new GanttGenerator(service);
        User           resource1 = g.addUser("resource1", 0.3f);
        User           resource2 = g.addUser("resource2", 0.7f);
        Sprint         sprint    = g.addSprint();

        Task task1          = g.addParentTask("[1] Parent Task", sprint, null, null);
        Task task2          = g.addTask("[2] Child Task ", "5d", "6d", resource1, sprint, task1, null);
        Task task3          = g.addTask("[3] Child Task ", "5d", "7d", resource2, sprint, task1, task2);
        Task startMilestone = g.addTask(sprint, null, "Start", LocalDateTime.parse("2025-05-05T08:00:00"), null, Duration.ZERO, null, null, TaskMode.MANUALLY_SCHEDULED, true);
        task1.getPredecessors().add(new Relation(startMilestone.getId(), true));

        g.createDeliveryBufferTask(sprint, Duration.ZERO);
        g.initializeSprint(sprint);
        g.testTheme = testTheme;

        g.levelResources(testInfo, sprint, null);
        g.generateWorklogs(sprint.getId(), ParameterOptions.getLocalNow());
        for (Task task : sprint.getTasks()) {
            Duration work = Duration.ZERO;
            for (Worklog worklog : task.getWorklogs()) {
                work = work.plus(worklog.getTimeSpent());
            }
            log.info("Task {} {} has worklogs with total duration {}", task.getName(), task.getMinEstimate(), work);
        }

        GanttBurndownChartDto chart             = service.build(sprint, ParameterOptions.getLocalNow(), false);
        int                   alignedTaskColors = 0;
        for (AuthorSeriesDto author : chart.authors) {
            TaskDto assignedTask = chart.tasks.stream()
                    .filter(task -> author.userName.equals(task.assignedUserName))
                    .findFirst()
                    .orElse(null);
            if (assignedTask != null) {
                Color authorColor = new Color((int) author.color, true);
                assertEquals(String.format("#%02x%02x%02x%02x", authorColor.getRed(), authorColor.getGreen(),
                        authorColor.getBlue(), authorColor.getAlpha()), assignedTask.fillColor);
                alignedTaskColors++;
            }
        }
        assertTrue(alignedTaskColors > 0, "No Burndown author matched an assigned Gantt task");

        generateGanttBurndownChart(testInfo, g, sprint);
        assertEquals(task1.getStart(), LocalDateTime.parse("2025-05-05T08:00:00"));
    }
}
