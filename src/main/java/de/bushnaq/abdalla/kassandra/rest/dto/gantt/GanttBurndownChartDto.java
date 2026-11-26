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

package de.bushnaq.abdalla.kassandra.rest.dto.gantt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DTO for the interactive client-side combined Gantt + Burndown chart rendered by
 * gantt-burndown-bundle.js.
 * <p>
 * The chart has two vertically stacked sections sharing the same day axis:
 * <ul>
 *   <li>Top: burndown chart (stacked area chart of remaining work per author per day)</li>
 *   <li>Bottom: Gantt chart (task bars, same scroll/zoom)</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GanttBurndownChartDto {

    /**
     * Per-author daily work series for the stacked burndown bars.
     * Sorted by author name for consistent legend ordering.
     */
    public List<AuthorSeriesDto> authors = new ArrayList<>();
    /**
     * Unique assigned-user calendars referenced by the Gantt task rows.
     */
    public List<UserCalendarDto> calendars = new ArrayList<>();
    /**
     * Gantt-derived planned burn-down guide (with buffer — all leaf tasks).
     * Same indexing as {@link #ganttGuideWithoutBuffer}.
     */
    public List<Long>            ganttGuideWithBuffer;
    /**
     * Gantt-derived planned burn-down guide (without buffer — only tasks that
     * have impact on cost).
     * Element {@code d} is remaining work in seconds at the start of day {@code d}
     * (day 0 = {@link #seriesStart}).
     * {@code null} when no Gantt task data is available.
     */
    public List<Long>            ganttGuideWithoutBuffer;
    /**
     * Burndown chart metadata.
     */
    public BurndownMetaDto       meta    = new BurndownMetaDto();
    /**
     * Task estimates and dated worklogs used for replay.
     */
    public List<ReplayTask> replayTasks = new ArrayList<>();
    /**
     * Server-side date of slot zero in the actual and guide arrays, excluding chart padding.
     */
    @JsonIgnore
    public LocalDate seriesStart;
    /**
     * Server-side sprint scope for pairing chart and task evidence.
     */
    @JsonIgnore
    public UUID sprintId;
    /**
     * Gantt task rows for the bottom section (reuses {@link TaskDto}).
     */
    public List<TaskDto>         tasks   = new ArrayList<>();

    /**
     * The initial estimate and worklogs of a task.
     */
    public static class ReplayTask {
        /** The task ID, used to match the replay snapshot to its Gantt row. */
        public UUID taskId;
        /** The user assigned to the task, or null if unassigned. */
        public UUID userId;
        /** Whether the task contributes to sprint effort. */
        public boolean impactOnCost;
        /** Initial task estimate in seconds. */
        public long initialEstimateSeconds;
        /** Worklogs sorted by start time. */
        public List<ReplayWorklog> worklogs = new ArrayList<>();
    }

    /**
     * A dated worklog and its remaining estimate snapshot.
     */
    public static class ReplayWorklog {
        /** The author who logged the work. */
        public UUID authorId;
        /** Worklog start time. */
        public LocalDateTime start;
        /** Time spent in seconds. */
        public long timeSpentSeconds;
        /** Remaining estimate in seconds. */
        public Long remainingEstimateSeconds;
    }

}
