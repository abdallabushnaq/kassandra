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
import de.bushnaq.abdalla.kassandra.report.dao.BurnDownGuide;
import de.bushnaq.abdalla.kassandra.report.dao.ETheme;
import de.bushnaq.abdalla.kassandra.report.dao.Milestones;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttUtil;
import de.bushnaq.abdalla.kassandra.service.GanttBurndownChartService;
import de.bushnaq.abdalla.kassandra.util.AbstractGanttTestUtil;
import de.bushnaq.abdalla.kassandra.util.BurnDownUtil;
import de.bushnaq.abdalla.kassandra.util.PersistingEntityGenerator;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import de.bushnaq.abdalla.util.date.DateUtil;
import de.bushnaq.abdalla.util.TaskUtil;
import de.bushnaq.abdalla.util.GanttErrorHandler;
import net.sf.mpxj.ProjectCalendar;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.awt.Color;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    @Autowired
    private             GanttBurndownChartService service;
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

    /**
     * Keeps the scheduled guide unchanged by sick days discovered after leveling.
     *
     * @throws Exception if guide calculation fails
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void plannedGuideIgnoresSickDaysAndCountsAfternoonStarts() throws Exception {
        LocalDateTime start         = LocalDateTime.parse("2026-03-02T13:00:00");
        LocalDateTime finish        = LocalDateTime.parse("2026-03-05T10:00:00");
        Sprint        plannedSprint = new Sprint();
        plannedSprint.setName("Planned");
        plannedSprint.initialize();
        User user = new User();
        user.setName("Planner");
        user.addAvailability(new Availability(0.5f, start.toLocalDate()));
        plannedSprint.initUserMap(List.of(user));
        Task task = new Task();
        task.setName("Scheduled task");
        task.setSprint(plannedSprint);
        task.setResourceId(user.getId());
        task.setStart(start);
        task.setFinish(finish);
        task.setMinEstimate(Duration.ofMinutes(390));
        plannedSprint.addTask(task);

        user.getCalendar().addCalendarException(LocalDate.parse("2026-03-03")).setName(OffDayType.SICK.name());
        user.getCalendar().addCalendarException(LocalDate.parse("2026-03-04")).setName(OffDayType.VACATION.name());
        Milestones milestones = new Milestones(plannedSprint.getName());
        milestones.add(start.toLocalDate(), "S", "Start", Color.BLUE);
        milestones.add(finish.toLocalDate(), "E", "End", Color.BLUE);
        milestones.calculate();
        service.milestones = milestones;

        BurnDownGuide guide = new BurnDownGuide(start, 3);
        service.initGanttGuide(plannedSprint, finish, guide, false);

        assertEquals(Duration.ofMinutes(105), guide.getWork(0));
        assertEquals(Duration.ofMinutes(225), guide.getWork(1));
        assertEquals(Duration.ZERO, guide.getWork(2));
        assertEquals(Duration.ofMinutes(60), guide.getWork(3));
        assertEquals(Duration.ofMinutes(390), guide.get(0));
        assertEquals(Duration.ZERO, guide.get(4));
    }

    /**
     * The chart's plan ignores sickness, but both leveling options retain vacations, holidays and work weeks.
     *
     * @throws Exception if guide calculation fails
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void sicknessFreeLevelingKeepsTheGuideAtTheMinimumEstimate() throws Exception {
        LocalDateTime start         = LocalDateTime.parse("2026-03-02T08:00:00");
        GanttUtil     gantt         = new GanttUtil();
        for (boolean sprintHasOwner : List.of(true, false)) {
            Sprint planned = createSicknessSprint(start, sprintHasOwner);
            int calendarCount = planned.getProjectFile().getCalendars().size();
            gantt.levelResources(new GanttErrorHandler(), planned, "", start, false);
            assertEquals(calendarCount, planned.getProjectFile().getCalendars().size());
            Task plannedTask = planned.getTasks().getFirst();
            assertEquals(LocalDateTime.parse("2026-03-06T16:30:00"), plannedTask.getFinish());

            Milestones milestones = new Milestones(planned.getName());
            milestones.add(start.toLocalDate(), "S", "Start", Color.BLUE);
            milestones.add(plannedTask.getFinish().toLocalDate(), "E", "End", Color.BLUE);
            milestones.calculate();
            service.milestones = milestones;
            BurnDownGuide guide = new BurnDownGuide(start, 5);
            service.initGanttGuide(planned, plannedTask.getFinish(), guide, false);
            assertTrue(Math.abs(guide.get(0).minus(plannedTask.getMinEstimate()).toSeconds()) <= 60,
                    "The optimum guide must start at the minimum estimate: guide=" + guide.get(0) + ", estimate=" + plannedTask.getMinEstimate());

            User plannedUser = planned.getUser(plannedTask.getResourceId());
            assertEquals(LocalDateTime.parse("2026-03-06T08:00:00"),
                    plannedTask.getEffectiveCalendar().getNextWorkStart(LocalDateTime.parse("2026-03-03T08:00:00")));
            ProjectCalendar calendar = plannedUser.createCalendarWithoutSickDays(planned);
            try {
                assertEquals(LocalDateTime.parse("2026-03-10T08:00:00"),
                        calendar.getNextWorkStart(LocalDateTime.parse("2026-03-09T08:00:00")));
            } finally {
                planned.getProjectFile().getCalendars().remove(calendar);
            }

            Sprint replanned = createSicknessSprint(start, sprintHasOwner);
            gantt.levelResources(new GanttErrorHandler(), replanned, "", start, true);
            assertEquals(LocalDateTime.parse("2026-03-10T16:30:00"), replanned.getTasks().getFirst().getFinish());
            Sprint legacy = createSicknessSprint(start, sprintHasOwner);
            gantt.levelResources(new GanttErrorHandler(), legacy, "", start);
            assertEquals(replanned.getTasks().getFirst().getFinish(), legacy.getTasks().getFirst().getFinish());
        }
    }

    private Sprint createSicknessSprint(LocalDateTime start, boolean sprintHasOwner) {
        Sprint sprint = new Sprint();
        sprint.setName("Sickness scheduling");
        sprint.setStart(start);
        sprint.initialize();
        sprint.getCalendar().addCalendarException(LocalDate.parse("2026-03-09")).setName("Holiday");
        User user = new User();
        user.setName("Planner");
        user.addAvailability(new Availability(1f, start.toLocalDate()));
        WorkWeek week = new WorkWeek();
        for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.FRIDAY)) {
            WorkDaySchedule schedule = week.getScheduleForDay(day);
            schedule.setWorkingDay(true);
            schedule.setWorkStart(LocalTime.of(8, 0));
            schedule.setWorkEnd(LocalTime.of(16, 30));
            schedule.setLunchStart(LocalTime.NOON);
            schedule.setLunchEnd(LocalTime.of(13, 0));
        }
        user.addUserWorkWeek(new UserWorkWeek(week, start.toLocalDate()));
        user.getOffDays().add(new OffDay(LocalDate.parse("2026-03-03"), LocalDate.parse("2026-03-03"), OffDayType.SICK));
        user.getOffDays().add(new OffDay(LocalDate.parse("2026-03-05"), LocalDate.parse("2026-03-05"), OffDayType.VACATION));
        if (sprintHasOwner)
            sprint.setUserId(user.getId());
        sprint.initUserMap(List.of(user));
        Task task = new Task();
        task.setName("Scheduled task");
        task.setSprint(sprint);
        task.setResourceId(user.getId());
        task.setMinEstimate(Duration.ofMinutes(3 * 450));
        sprint.addTask(task);
        return sprint;
    }

    /**
     * Accounts for lunch before applying availability and preserves partial finish days.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void partialDayWorkRespectsLunchAndScheduledFinish() {
        LocalDate day           = LocalDate.parse("2026-08-19");
        Sprint    plannedSprint = new Sprint();
        plannedSprint.setStart(day.atTime(8, 0));
        plannedSprint.initialize();
        User user = new User();
        user.setName("Planner");
        user.addAvailability(new Availability(0.9f, day));
        plannedSprint.initUserMap(List.of(user));
        Task task = new Task();
        task.setName("Partial task");
        task.setSprint(plannedSprint);
        task.setResourceId(user.getId());
        task.setMinEstimate(Duration.ofHours(4));
        task.setRemainingEstimate(Duration.ofHours(4));
        task.setStart(day.atTime(9, 6, 42));
        task.setFinish(day.atTime(14, 33, 24));

        assertEquals(Duration.ofSeconds(14401), TaskUtil.getTaskWorkPerDay(plannedSprint, day, task, false));

        task.setStart(day.atTime(8, 0));
        task.setFinish(day.plusDays(1).atTime(10, 0));
        task.setRemainingEstimate(Duration.ofSeconds(6480));
        assertEquals(Duration.ofSeconds(6480), TaskUtil.getTaskWorkPerDay(plannedSprint, day.plusDays(1), task, false));
        task.setRemainingEstimate(Duration.ofHours(3));
        assertEquals(Duration.ofSeconds(24300), TaskUtil.getTaskWorkPerDay(plannedSprint, day.plusDays(1), task, false));
    }

    /**
     * Limits all tasks assigned to one user to one working day and skips sick days.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void worklogsRespectSharedAvailabilityAndSickDays() {
        LocalDate day           = LocalDate.parse("2026-03-02");
        Sprint    plannedSprint = new Sprint();
        plannedSprint.setName("Capacity");
        plannedSprint.setStart(day.atTime(8, 0));
        plannedSprint.initialize();
        User user = new User();
        user.setName("Planner");
        user.addAvailability(new Availability(1f, day));
        plannedSprint.initUserMap(List.of(user));
        user.getCalendar().addCalendarException(day.plusDays(1)).setName(OffDayType.SICK.name());
        for (int i = 0; i < 2; i++) {
            Task task = new Task();
            task.setName("Task " + i);
            task.setSprint(plannedSprint);
            task.setResourceId(user.getId());
            task.setStart(day.atTime(8, 0));
            task.setFinish(day.plusDays(3).atTime(16, 30));
            task.setMinEstimate(Duration.ofHours(8));
            task.setRemainingEstimate(Duration.ofHours(8));
            plannedSprint.addTask(task);
        }

        List<Worklog> generated = new ArrayList<>();
        PersistingEntityGenerator generator = new PersistingEntityGenerator() {
            @Override
            protected Worklog addWorklogToBuffer(Task task, User author, LocalDateTime start, Duration spent, String comment) {
                Worklog worklog = new Worklog();
                worklog.setStart(start);
                worklog.setTimeSpent(spent);
                worklog.setTimeRemainingEstimate(task.getRemainingEstimate().minus(spent));
                task.addWorklog(worklog);
                generated.add(worklog);
                return worklog;
            }

            @Override
            protected void flushWorklogBuffer(Sprint sprint) {
            }

            @Override
            public void persistTasksAndSprint(Sprint sprint) {
            }
        };
        BurnDownUtil.generateWorklogs(generator, plannedSprint, 0f, day.plusDays(5).atTime(8, 0));

        assertEquals(Duration.ofHours(7).plusMinutes(30), generated.stream()
                .filter(worklog -> worklog.getStart().toLocalDate().equals(day))
                .map(Worklog::getTimeSpent).reduce(Duration.ZERO, Duration::plus));
        assertEquals(Duration.ofHours(7).plusMinutes(30), generated.stream()
                .filter(worklog -> worklog.getStart().toLocalDate().equals(day.plusDays(2)))
                .map(Worklog::getTimeSpent).reduce(Duration.ZERO, Duration::plus));
        assertEquals(Duration.ZERO, generated.stream()
                .filter(worklog -> worklog.getStart().toLocalDate().equals(day.plusDays(1)))
                .map(Worklog::getTimeSpent).reduce(Duration.ZERO, Duration::plus));
        assertEquals(Duration.ofHours(16), generated.stream().map(Worklog::getTimeSpent).reduce(Duration.ZERO, Duration::plus));
        assertEquals(Duration.ofMinutes(30), generated.stream()
                .filter(worklog -> worklog.getStart().toLocalDate().equals(day.plusDays(2)))
                .filter(worklog -> worklog.getTimeSpent().compareTo(Duration.ofHours(1)) < 0)
                .map(Worklog::getTimeSpent).reduce(Duration.ZERO, Duration::plus));
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
