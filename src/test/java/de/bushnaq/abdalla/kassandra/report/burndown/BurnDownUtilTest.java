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

package de.bushnaq.abdalla.kassandra.report.burndown;

import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.report.dao.ETheme;
import de.bushnaq.abdalla.kassandra.util.AbstractGanttTestUtil;
import de.bushnaq.abdalla.kassandra.util.BurnDownUtil;
import de.bushnaq.abdalla.kassandra.util.PersistingEntityGenerator;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import de.bushnaq.abdalla.util.date.DateUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("UnitTest")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.MethodName.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class BurnDownUtilTest extends AbstractGanttTestUtil {
    public static final float                     WORK_PER_DAY = 7.5f;
    @Autowired
    protected           PersistingEntityGenerator peg;
    Sprint        sprint;
    Task          startMilestone;
    LocalDateTime startTime;

    private void addSequentialWorklogs(LocalDateTime start, UUID taskId, Duration work, int count, List<Worklog> expectedWorklogs) {
        for (int i = 0; i < count; i++) {
            Worklog wl = new Worklog();
            wl.setStart(start.plusDays(i));
            wl.setTaskId(taskId);
            wl.setTimeSpent(work);
            expectedWorklogs.add(wl);
        }
    }

    private void assertWorklogsEquals(List<Worklog> expectedWorklogs) {
        for (int i = 0; i < sprint.getWorklogs().size(); i++) {
            Worklog actualWorklog   = sprint.getWorklogs().get(i);
            Worklog expectedWorklog = expectedWorklogs.get(i);
            assertEquals(expectedWorklog.getStart(), actualWorklog.getStart(), "Worklog start time mismatch at index " + i);
            assertEquals(expectedWorklog.getTaskId(), actualWorklog.getTaskId(), "Worklog task ID mismatch at index " + i);
            assertEquals(expectedWorklog.getTimeSpent(), actualWorklog.getTimeSpent(), "Worklog time spent mismatch at index " + i);
        }
    }

    @BeforeEach
    protected void beforeEach(TestInfo testInfo) {
        super.beforeEach(testInfo);
        peg.init();
    }

    /**
     * one task 5d starting Sunday
     *
     * @param testInfo junit test information
     * @throws Exception
     */
    @Test
    @Order(1)
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void burndownTask01_startOnSunday(TestInfo testInfo) throws Exception {
        init(testInfo, 1, "2025-01-05T08:00:00");//start on sunday

        //create tasks
        User resource1 = getUser(0);
        Task task1     = peg.addParentTask("[1] Parent Task", sprint, null, startMilestone);
        Task task2     = peg.addTask("[2] Child Task", "5d", "7d", resource1, sprint, task1, null);
        initSprint(testInfo, sprint);

        //test
        {
            float availability = resource1.getAvailabilities().getFirst().getAvailability();
            assertEquals(startTime.plusDays(1), startMilestone.getStart(), "Sprint should not start on a Sunday");
            assertEquals(startTime.plusDays(12).withHour(16).withMinute(30), sprint.getEnd(), "Sprint must end after 10 working days, as the availability of the user is 50%.");
            List<Worklog> expectedWorklogs = new ArrayList<Worklog>();
            addSequentialWorklogs(startMilestone.getStart(), task2.getId(), calculateWorkFraction("7h 30m", availability), 5, expectedWorklogs);
            addSequentialWorklogs(startMilestone.getStart().plusDays(7), task2.getId(), calculateWorkFraction("7h 30m", availability), 5, expectedWorklogs);
            assertEquals(expectedWorklogs.size(), sprint.getWorklogs().size(), "expectedWorklogs size mismatch");
            assertWorklogsEquals(expectedWorklogs);
        }
    }

    /**
     * one task 5d starting Monday
     *
     * @param testInfo junit test information
     * @throws Exception
     */
    @Test
    @Order(2)
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void burndownTask02_startOnMonday(TestInfo testInfo) throws Exception {
        init(testInfo, 2, "2025-01-06T08:00:00");//start on monday

        //create sprint
        User resource1 = getUser(0);
        Task task1     = peg.addParentTask("[1] Parent Task", sprint, null, startMilestone);
        Task task2     = peg.addTask("[2] Child Task", "5d", "7d", resource1, sprint, task1, null);

        initSprint(testInfo, sprint);

        //test
        {
            float availability = resource1.getAvailabilities().getFirst().getAvailability();
            assertEquals(startTime, startMilestone.getStart(), "Sprint should not start on a Sunday.");
            assertEquals(startTime.plusDays(11).withHour(16).withMinute(30), sprint.getEnd(), "Sprint must end after 10 working days, as the availability of the user is 50%.");
            List<Worklog> expectedWorklogs = new ArrayList<Worklog>();
            addSequentialWorklogs(startMilestone.getStart(), task2.getId(), calculateWorkFraction("7h 30m", availability), 5, expectedWorklogs);
            addSequentialWorklogs(startMilestone.getStart().plusDays(7), task2.getId(), calculateWorkFraction("7h 30m", availability), 5, expectedWorklogs);
            assertEquals(expectedWorklogs.size(), sprint.getWorklogs().size(), "expectedWorklogs size mismatch");
            assertWorklogsEquals(expectedWorklogs);
        }
    }

    /**
     * one task 5d starting Monday with year eve in between
     *
     * @param testInfo junit test information
     * @throws Exception
     */
    @Test
    @Order(3)
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void burndownTask03_withHoliday(TestInfo testInfo) throws Exception {
        init(testInfo, 2, "2024-12-30T08:00:00");//start before new eve

        //create sprint
        User resource1 = getUser(0);
        Task task1     = peg.addParentTask("[1] Parent Task", sprint, null, startMilestone);
        Task task2     = peg.addTask("[2] Child Task", "5d", "7d", resource1, sprint, task1, null);

        initSprint(testInfo, sprint);

        //test
        {
            float availability = resource1.getAvailabilities().getFirst().getAvailability();
            assertEquals(startTime, startMilestone.getStart(), "Sprint should not start on a Sunday.");
            assertEquals(startTime.plusDays(14).withHour(16).withMinute(30), sprint.getEnd(), "Sprint must end after 10 working days, as the availability of the user is 50%.");
            List<Worklog> expectedWorklogs = new ArrayList<Worklog>();
            addSequentialWorklogs(startMilestone.getStart(), task2.getId(), calculateWorkFraction("7h 30m", availability), 2, expectedWorklogs);//2 days before eve
            addSequentialWorklogs(startMilestone.getStart().plusDays(3), task2.getId(), calculateWorkFraction("7h 30m", availability), 2, expectedWorklogs);//2 days after eve
            addSequentialWorklogs(startMilestone.getStart().plusDays(7), task2.getId(), calculateWorkFraction("7h 30m", availability), 5, expectedWorklogs);//one week
            addSequentialWorklogs(startMilestone.getStart().plusDays(14), task2.getId(), calculateWorkFraction("7h 30m", availability), 1, expectedWorklogs);//one day
            assertEquals(expectedWorklogs.size(), sprint.getWorklogs().size(), "expectedWorklogs size mismatch");
            assertWorklogsEquals(expectedWorklogs);
        }
    }

    private Duration calculateWorkFraction(String workAsString, float availability) {
        Duration work = DateUtil.parseWorkDayDurationString(workAsString);
        return Duration.ofSeconds((long) (work.getSeconds() * availability));
    }

    private User getUser(int userIndex) {
        return peg.getUsers().stream().sorted(Comparator.comparing(User::getName)).toList().get(userIndex);
    }

    private void init(TestInfo testInfo, int testCaseIndex, String startTime) throws Exception {
        this.startTime = LocalDateTime.parse(startTime);
        context.parameters.setTheme(ETheme.light);
        TestInfoUtil.setTestCaseIndex(testInfo, testCaseIndex);
        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + testCaseIndex);
        TestInfoUtil.setTestStart(testInfo, startTime);//sunday
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + testCaseIndex);
        generateOneProduct(testInfo);
        ParameterOptions.setNow(OffsetDateTime.parse("2025-02-03T08:00:00+01:00"));//make sure now is after the sprint end so that all work can be burned down
        initializeInstances();
        sprint         = peg.eg.getSprints().get(1);
        startMilestone = peg.addTask(sprint, null, "Start", LocalDateTime.parse(startTime), null, Duration.ZERO, null, null, TaskMode.MANUALLY_SCHEDULED, true);
    }

    private void initSprint(TestInfo testInfo, Sprint sprint) throws Exception {
        sprint.initialize();
        sprint.initUserMap(peg.userApi.getAll(sprint.getId()));
        sprint.initTaskMap(peg.taskApi.getAll(sprint.getId()), peg.worklogApi.getAll(sprint.getId()));
        levelResourcesAndPersist(testInfo, sprint, null);
        {
            for (Sprint s : peg.getSprints()) {
                peg.taskApi.updateBatch(sprint.getTasks(), s.getId());
                peg.sprintApi.update(s);
            }
        }
        peg.generateRandomSickDays(peg.getUsers().stream().toList());
        float delay = 0f;
        BurnDownUtil.generateWorklogs(peg, sprint, delay, ParameterOptions.getLocalNow());
        sprint.initTaskMap(peg.taskApi.getAll(sprint.getId()), peg.worklogApi.getAll(sprint.getId()));
        generateGanttChart(testInfo, sprint.getId(), null);
        generateBurndownChart(testInfo, sprint.getId());
//        printTables();
    }
}
