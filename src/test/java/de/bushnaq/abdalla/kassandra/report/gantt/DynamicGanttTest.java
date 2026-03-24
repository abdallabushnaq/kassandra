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

package de.bushnaq.abdalla.kassandra.report.gantt;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.util.GanttGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
public class DynamicGanttTest extends AbstractGanttTester {
    private final String testFolder = "references/dynamic-gantt";

    @BeforeEach
    public void beforeEach() {
        Logger logger = (Logger) LoggerFactory.getLogger("de.bushnaq");
        logger.setLevel(Level.TRACE);
        ParameterOptions.setNow(OffsetDateTime.parse("2025-08-05T08:00:00+01:00"));
    }

    @Test
    public void test(TestInfo testInfo) throws Exception {
        GanttGenerator g         = new GanttGenerator();
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

        g.levelResources(testInfo, sprint, null);

        g.generateGanttChart(testInfo, sprint.getId(), testFolder);

        assertEquals(task1.getStart(), LocalDateTime.parse("2025-05-05T08:00:00"));
    }

}
