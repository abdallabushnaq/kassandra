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

import de.bushnaq.abdalla.kassandra.dto.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("UnitTest")
class SimulatorUtilTest {
    @TempDir
    private Path outputDirectory;

    private static Sprint sprint(String name, User user, Task task) {
        Sprint sprint = new Sprint();
        sprint.setId(UUID.randomUUID());
        sprint.setName(name);
        sprint.setStart(LocalDateTime.of(2026, 3, 9, 8, 0));
        sprint.setEnd(LocalDateTime.of(2026, 3, 13, 17, 0));
        sprint.setUserMap(Map.of(user.getId(), user));
        sprint.setTasks(List.of(task));
        task.setSprint(sprint);
        return sprint;
    }

    private static Task task(User user, Worklog worklog) {
        Task task = new Task();
        task.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        task.setResourceId(user.getId());
        task.setWorklogs(worklog == null ? List.of() : List.of(worklog));
        return task;
    }

    private static User user(String name, List<OffDay> offDays) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName(name);
        user.setOffDays(offDays);
        return user;
    }

    private static Worklog worklog(LocalDateTime start) {
        Worklog worklog = new Worklog();
        worklog.setStart(start);
        worklog.setTimeSpent(Duration.ofHours(2));
        worklog.setTimeRemainingEstimate(Duration.ofHours(1));
        return worklog;
    }

    @Test
    void writesUserWorklogComparisonForOverlappingSickLeave() throws IOException {
        User user = user("Alex Example", List.of(new OffDay(LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 11), OffDayType.SICK)));
        User    referenceUser   = user("Alex Example", List.of());
        Task    referenceTask   = task(referenceUser, worklog(LocalDateTime.of(2026, 3, 9, 8, 0)));
        Task    sickLeaveTask   = task(user, null);
        Sprint  referenceSprint = sprint("Sprint One", referenceUser, referenceTask);
        Sprint  sickLeaveSprint = sprint("Sprint One", user, sickLeaveTask);
        Worklog replayWorklog   = worklog(LocalDateTime.of(2026, 3, 10, 8, 0));
        String  taskKey         = sickLeaveTask.getKey();

        assertTrue(SimulatorUtil.hasSickLeaveDuringSprint(user, sickLeaveSprint));
        assertFalse(SimulatorUtil.hasSickLeaveDuringSprint(referenceUser, referenceSprint));
        SimulatorUtil.writeUserWorklogComparison(2, user, referenceSprint, sickLeaveSprint,
                Map.of(taskKey, List.of(replayWorklog)), outputDirectory);

        Path   outputFile = outputDirectory.resolve("case-2-Sprint_One-Alex_Example-worklogs.csv");
        String csv        = Files.readString(outputFile);
        assertTrue(csv.contains("\"reference\",\"Sprint One\",\"Alex Example\",\"" + taskKey + "\""));
        assertTrue(csv.contains("\"simulation\",\"Sprint One\",\"Alex Example\",\"" + taskKey + "\""));
    }

    @Test
    void sanitizesFileNames() {
        assertEquals("Sprint_One_2026", SimulatorUtil.safeFileName("Sprint One/2026"));
    }

    @Test
    void movesInitialDatabaseSnapshotIntoTemporaryDirectory() throws IOException {
        Path exportDirectory = outputDirectory.resolve("exports");
        Path snapshotDirectory = outputDirectory.resolve("retained");
        H2DatabaseStateManager databaseStateManager = new H2DatabaseStateManager() {
            @Override
            public String exportDatabaseSnapshot(String snapshotName) {
                Path exportedSnapshot = exportDirectory.resolve(snapshotName + ".zip");
                try {
                    Files.createDirectories(exportDirectory);
                    Files.writeString(exportedSnapshot, "snapshot");
                    return exportedSnapshot.toString();
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            }
        };

        Path snapshot = SimulatorUtil.createInitialDatabaseSnapshot(databaseStateManager, snapshotDirectory,
                "baseline-");

        assertTrue(snapshot.startsWith(snapshotDirectory));
        assertEquals("snapshot", Files.readString(snapshot));
        assertFalse(Files.exists(exportDirectory.resolve(snapshot.getFileName())));
    }
}
