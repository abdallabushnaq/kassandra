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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Creates and reads the per-sprint sick-day log written alongside generated database snapshots.
 */
public class SnapshotLogGenerator {
    private static final String SPRINT_PREFIX    = "Sprint: ";
    private static final String SICK_DAYS_MARKER = " | Sick days: [";
    private static final String RECORD_SUFFIX    = "]";

    private final Path logFile;

    /**
     * Creates a generator for the given snapshot log file.
     *
     * @param logFile path of the log file to create and access
     */
    public SnapshotLogGenerator(Path logFile) {
        this.logFile = Objects.requireNonNull(logFile);
    }

    /**
     * Creates the log file's parent directories and truncates any previous log contents.
     *
     * @throws IOException if the file or its parent directories cannot be created
     */
    public void create() throws IOException {
        Files.createDirectories(logFile.toAbsolutePath().getParent());
        Files.writeString(logFile, "", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Appends one record containing the sick days selected for a sprint.
     *
     * @param sprintName sprint name associated with the record
     * @param sickDays selected sick days, or an empty list when the sprint has none
     * @throws IOException if the record cannot be written
     */
    public void appendSickDays(String sprintName, List<LocalDate> sickDays) throws IOException {
        Objects.requireNonNull(sprintName);
        Objects.requireNonNull(sickDays);
        String dates = String.join(", ", sickDays.stream().map(LocalDate::toString).toList());
        String record = SPRINT_PREFIX + sprintName + SICK_DAYS_MARKER + dates + RECORD_SUFFIX + System.lineSeparator();
        Files.writeString(logFile, record, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * Appends one record indicating that a sprint has no sick days.
     *
     * @param sprintName sprint name associated with the empty sick-day list
     * @throws IOException if the record cannot be written
     */
    public void appendEmptySickDayList(String sprintName) throws IOException {
        appendSickDays(sprintName, List.of());
    }

    /**
     * Reads the sick days recorded for the requested sprint.
     *
     * @param sprintName exact sprint name to find
     * @return the recorded sick days, in their logged order
     * @throws IOException if the log cannot be read
     * @throws IllegalArgumentException if no record exists for the requested sprint
     * @throws IllegalStateException if more than one record exists for the requested sprint
     */
    public List<LocalDate> getSickDays(String sprintName) throws IOException {
        Objects.requireNonNull(sprintName);
        String recordPrefix = SPRINT_PREFIX + sprintName + SICK_DAYS_MARKER;
        List<LocalDate> matchingSickDays = null;
        for (String line : Files.readAllLines(logFile, StandardCharsets.UTF_8)) {
            if (!line.startsWith(recordPrefix) || !line.endsWith(RECORD_SUFFIX)) {
                continue;
            }
            if (matchingSickDays != null) {
                throw new IllegalStateException("Multiple sick-day records found for sprint '" + sprintName + "'.");
            }
            String dates = line.substring(recordPrefix.length(), line.length() - RECORD_SUFFIX.length());
            matchingSickDays = new ArrayList<>();
            if (!dates.isBlank()) {
                for (String date : dates.split(",\\s*")) {
                    matchingSickDays.add(LocalDate.parse(date));
                }
            }
        }
        if (matchingSickDays == null) {
            throw new IllegalArgumentException("No sick-day record found for sprint '" + sprintName + "'.");
        }
        return List.copyOf(matchingSickDays);
    }
}
