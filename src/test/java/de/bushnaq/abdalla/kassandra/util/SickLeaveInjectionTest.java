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
import de.bushnaq.abdalla.kassandra.dto.OffDay;
import de.bushnaq.abdalla.kassandra.dto.OffDayType;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.Worklog;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("UnitTest")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SickLeaveInjectionTest extends AbstractGanttTestUtil {
    @Autowired
    private OffDayGenerator offDayGenerator;

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    void injectedSicknessTargetsCriticalWorkAndDelaysWorklogs(@TempDir Path tempDir) throws Exception {
        ParameterOptions.setNow(OffsetDateTime.parse("2026-02-01T08:00:00+01:00"));
        LocalDate start = LocalDate.of(2026, 1, 12);
        User criticalUser = peg.addUser("Critical Developer", "kristen.hubbell@kassandra.org", "USER", "de", "nw",
                start.minusYears(1), Color.BLUE, 1f, null);
        User otherUser = peg.addUser("Other Developer", "claudine.fick@kassandra.org", "USER", "de", "nw",
                start.minusYears(1), Color.RED, 1f, null);
        Sprint sprint = new Sprint();
        sprint.setName("Critical sprint");
        sprint.setStart(start.atTime(8, 0));
        sprint.setEnd(start.plusDays(4).atTime(16, 30));
        sprint.initialize();
        sprint.initUserMap(List.of(criticalUser, otherUser));
        Task critical = task(sprint, criticalUser, start, 5, Duration.ofMinutes(5 * 450), true);
        task(sprint, otherUser, start, 1, Duration.ofMinutes(450), true).setRemainingEstimate(Duration.ZERO);
        task(sprint, otherUser, start, 5, Duration.ofHours(100), false).setRemainingEstimate(Duration.ZERO);

        User copy = peg.userApi.getById(criticalUser.getId());
        Sprint otherSprint = new Sprint();
        otherSprint.initialize();
        otherSprint.initUserMap(List.of(copy));

        Path sickDayLog = tempDir.resolve("SickLeaveInjectionTest-sick-days.log");
        SnapshotLogGenerator snapshotLogGenerator = new SnapshotLogGenerator(sickDayLog);
        snapshotLogGenerator.create();
        offDayGenerator.injectSickDays(sprint, List.of(sprint, otherSprint), peg.getUsers(), snapshotLogGenerator);

        List<LocalDate> expected = List.of(start.plusDays(1), start.plusDays(2), start.plusDays(3));
        assertEquals(expected, snapshotLogGenerator.getSickDays("Critical sprint"));
        assertEquals("Sprint: Critical sprint | Sick days: " + expected + System.lineSeparator(), Files.readString(sickDayLog));
        assertEquals(expected, criticalUser.getOffDays().stream()
                .filter(offDay -> offDay.getType() == OffDayType.SICK)
                .map(OffDay::getFirstDay).sorted().toList());
        assertEquals(expected, peg.userApi.getById(criticalUser.getId()).getOffDays().stream()
                .filter(offDay -> offDay.getType() == OffDayType.SICK)
                .map(OffDay::getFirstDay).sorted().toList());
        assertTrue(otherUser.getOffDays().stream().noneMatch(offDay -> offDay.getType() == OffDayType.SICK));
        for (LocalDate day : expected) {
            assertFalse(critical.getEffectiveCalendar().isWorkingDate(day));
            assertFalse(copy.getCalendar().isWorkingDate(day));
        }

        List<Worklog> generated = new ArrayList<>();
        PersistingEntityGenerator generator = new PersistingEntityGenerator() {
            @Override
            protected Worklog addWorklogToBuffer(Task task, User author, LocalDateTime timestamp, Duration spent, String comment) {
                Worklog worklog = new Worklog();
                worklog.setStart(timestamp);
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
        BurnDownUtil.generateWorklogs(generator, sprint, 0f, start.plusWeeks(2).atTime(8, 0));
        assertTrue(generated.stream().noneMatch(worklog -> expected.contains(worklog.getStart().toLocalDate())));
        assertEquals(Duration.ZERO, critical.getRemainingEstimate());
        assertTrue(generated.getLast().getStart().isAfter(sprint.getEnd()));
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    void injectedSicknessSkipsExistingAbsencesAndUnassignedCriticalTasks(@TempDir Path tempDir) throws Exception {
        ParameterOptions.setNow(OffsetDateTime.parse("2026-02-01T08:00:00+01:00"));
        LocalDate start = LocalDate.of(2026, 1, 12);
        User user = peg.addUser("Critical Developer", "kristen.hubbell@kassandra.org", "USER", "de", "nw",
                start.minusYears(1), Color.BLUE, 1f, null);
        Sprint sprint = new Sprint();
        sprint.setName("Absences");
        sprint.initialize();
        sprint.initUserMap(List.of(user));
        peg.addOffDay(user, start.plusDays(2), start.plusDays(2), OffDayType.VACATION);
        task(sprint, user, start, 5, Duration.ofMinutes(5 * 450), true);

        Path sickDayLog = tempDir.resolve("SickLeaveInjectionTest-sick-days.log");
        SnapshotLogGenerator snapshotLogGenerator = new SnapshotLogGenerator(sickDayLog);
        snapshotLogGenerator.create();
        offDayGenerator.injectSickDays(sprint, List.of(sprint), peg.getUsers(), snapshotLogGenerator);

        List<LocalDate> expectedSickDays = List.of(start.plusDays(1), start.plusDays(3));
        assertEquals(expectedSickDays, snapshotLogGenerator.getSickDays("Absences"));
        assertEquals(expectedSickDays, user.getOffDays().stream()
                .filter(offDay -> offDay.getType() == OffDayType.SICK)
                .map(OffDay::getFirstDay).sorted().toList());
        assertEquals(3, peg.userApi.getById(user.getId()).getOffDays().size());

        Sprint emptySprint = new Sprint();
        emptySprint.setName("No critical work");
        offDayGenerator.injectSickDays(emptySprint, List.of(emptySprint), peg.getUsers(), snapshotLogGenerator);
        assertEquals(List.of(), snapshotLogGenerator.getSickDays("No critical work"));
        assertThrows(IllegalArgumentException.class, () -> snapshotLogGenerator.getSickDays("Unknown sprint"));
        assertEquals(3, user.getOffDays().size());
    }

    @Test
    void sickSprintProbabilityIsValidatedAndIncludedInSnapshotSettings() {
        RandomCase defaultCase = RandomCase.builder().build();
        RandomCase disabledCase = RandomCase.builder().sickSprintProbability(0f).build();
        assertEquals(0.5f, defaultCase.getSickSprintProbability());
        assertFalse(defaultCase.toString().equals(disabledCase.toString()));
        assertThrows(IllegalArgumentException.class, () -> RandomCase.builder().sickSprintProbability(-0.1f));
        assertThrows(IllegalArgumentException.class, () -> RandomCase.builder().sickSprintProbability(1.1f));
        assertThrows(IllegalArgumentException.class, () -> RandomCase.builder().sickSprintProbability(Float.NaN));
    }

    private Task task(Sprint sprint, User user, LocalDate start, int calendarDays, Duration work, boolean critical) {
        Task task = new Task();
        task.setName("Task");
        task.setSprint(sprint);
        task.setResourceId(user.getId());
        task.setStart(start.atTime(8, 0));
        task.setFinish(start.plusDays(calendarDays - 1).atTime(16, 30));
        task.setMinEstimate(work);
        task.setRemainingEstimate(work);
        task.setCritical(critical);
        sprint.addTask(task);
        return task;
    }
}
