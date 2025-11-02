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

package de.bushnaq.abdalla.kassandra.report.gantt;

import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.TaskMode;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.util.AbstractGanttTestUtil;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@TestMethodOrder(MethodOrderer.MethodName.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class GanttTest extends AbstractGanttTestUtil {

    /**
     * test dependency between two tasks
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void gantt_01(TestInfo testInfo) throws Exception {
        int testCaseIndex = 1;
        TestInfoUtil.setTestCaseIndex(testInfo, testCaseIndex);
        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + testCaseIndex);
        TestInfoUtil.setTestStart(testInfo, "2024-12-15T08:00:00");
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + testCaseIndex);
        generateOneProduct(testInfo);
        addRandomUser(0, 0.3f);
        addRandomUser(1, 0.7f);
        initializeInstances();

        //create tasks
        Sprint savedSprint    = expectedSprints.getFirst();
        Sprint sprint         = sprintApi.getById(savedSprint.getId());
        User   resource1      = expectedUsers.stream().toList().getFirst();
        User   resource2      = expectedUsers.stream().toList().get(1);
        Task   startMilestone = addTask(sprint, null, "Start", LocalDateTime.parse(TestInfoUtil.getTestStart(testInfo)), null, Duration.ZERO, null, null, TaskMode.MANUALLY_SCHEDULED, true);
        Task   task1          = addParentTask("[1] Parent Task", sprint, null, startMilestone);
        Task   task2          = addTask("[2] Child Task", "5d", null, resource1, sprint, task1, null);
        Task   task3          = addTask("[3] Child Task", "5d", null, resource2, sprint, task1, task2);
//        TestInfoUtil.setTestCaseIndex(testInfo, 1);
        sprint.initialize();
        sprint.initUserMap(userApi.getAll(sprint.getId()));
        sprint.initTaskMap(taskApi.getAll(sprint.getId()), worklogApi.getAll(sprint.getId()));
        levelResources(testInfo, sprint, null);
        generateWorklogs(sprint, ParameterOptions.getLocalNow());
        generateGanttChart(testInfo, sprint.getId(), null);
        generateBurndownChart(testInfo, sprint.getId());
    }

    /**
     * dependency between two parent tasks
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void gantt_02(TestInfo testInfo) throws Exception {
        int testCaseIndex = 2;
        TestInfoUtil.setTestCaseIndex(testInfo, testCaseIndex);
        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + testCaseIndex);
        TestInfoUtil.setTestStart(testInfo, "2024-12-15T08:00:00");
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + testCaseIndex);
        generateOneProduct(testInfo);
        addRandomUser(2, 0.5f);
        addRandomUser(3, 0.7f);
        initializeInstances();

        //create tasks
        Sprint sprint         = expectedSprints.getFirst();
        User   resource1      = expectedUsers.stream().toList().getFirst();
        User   resource2      = expectedUsers.stream().toList().get(1);
        Task   startMilestone = addTask(sprint, null, "Start", LocalDateTime.parse(TestInfoUtil.getTestStart(testInfo)), Duration.ZERO, null, null, null, TaskMode.MANUALLY_SCHEDULED, true);
        Task   task1          = addParentTask("[1] Parent Task", sprint, null, startMilestone);
        Task   task2          = addTask("[2] Child Task ", "5d", null, resource1, sprint, task1, null);
        Task   task3          = addTask("[3] Child Task ", "5d", null, resource2, sprint, task1, task2);

        Task task4 = addParentTask("[4] Parent Task", sprint, null, task1);
        Task task5 = addTask("[5] Child Task ", "5d", null, resource1, sprint, task4, null);
        Task task6 = addTask("[6] Child Task ", "5d", null, resource2, sprint, task4, task5);

//        TestInfoUtil.setTestCaseIndex(testInfo, 2);
        sprint.initialize();
        sprint.initUserMap(userApi.getAll(sprint.getId()));
        sprint.initTaskMap(taskApi.getAll(sprint.getId()), worklogApi.getAll(sprint.getId()));
        levelResources(testInfo, sprint, null);
        generateWorklogs(sprint, ParameterOptions.getLocalNow());
        generateGanttChart(testInfo, sprint.getId(), null);
        generateBurndownChart(testInfo, sprint.getId());
    }

    /**
     * two tasks with different resources and no dependency
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void gantt_03(TestInfo testInfo) throws Exception {
        int testCaseIndex = 3;
        TestInfoUtil.setTestCaseIndex(testInfo, testCaseIndex);
        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + testCaseIndex);
        TestInfoUtil.setTestStart(testInfo, "2024-12-15T08:00:00");
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + testCaseIndex);
        generateOneProduct(testInfo);

        addRandomUser(0, 0.5f);
        addRandomUser(4, 0.7f);
        initializeInstances();

        //create tasks
        Sprint sprint         = expectedSprints.getFirst();
        User   resource1      = expectedUsers.stream().toList().getFirst();
        User   resource2      = expectedUsers.stream().toList().get(1);
        Task   startMilestone = addTask(sprint, null, "Start", LocalDateTime.parse(TestInfoUtil.getTestStart(testInfo)), Duration.ZERO, null, null, null, TaskMode.MANUALLY_SCHEDULED, true);
        Task   task1          = addParentTask("[1] Parent Task", sprint, null, startMilestone);
        Task   task2          = addTask("[2] Child Task ", "5d", null, resource1, sprint, task1, null);
        Task   task3          = addTask("[3] Child Task ", "5d", null, resource2, sprint, task1, task2);

        Task task4 = addParentTask("[4] Parent Task", sprint, null, task1);
        Task task5 = addTask("[5] Child Task ", "5d", null, resource1, sprint, task4, null);
        Task task6 = addTask("[6] Child Task ", "5d", null, resource2, sprint, task4, task5);

        Task task7 = addParentTask("[7] Parent Task", sprint, null, task4);
        Task task8 = addTask("[8] Child Task ", "5d", null, resource1, sprint, task7, null);
        Task task9 = addTask("[9] Child Task ", "5d", null, resource2, sprint, task7, null);

//        TestInfoUtil.setTestCaseIndex(testInfo, 3);
        sprint.initialize();
        sprint.initUserMap(userApi.getAll(sprint.getId()));
        sprint.initTaskMap(taskApi.getAll(sprint.getId()), worklogApi.getAll(sprint.getId()));
        levelResources(testInfo, sprint, null);
        generateWorklogs(sprint, ParameterOptions.getLocalNow());
        generateGanttChart(testInfo, sprint.getId(), null);
        generateBurndownChart(testInfo, sprint.getId());
    }

}
