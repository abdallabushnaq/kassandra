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

package de.bushnaq.abdalla.kassandra.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.bushnaq.abdalla.kassandra.report.dao.CalendarSize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DTO for the interactive client-side Gantt chart rendered by gantt-chart.js.
 * <p>
 * Contains all data needed to render the chart client-side including:
 * task scheduling information, colors, calendar exceptions (non-working days),
 * dependency relations, and theme colors.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GanttChartDto {

    public Meta          meta  = new Meta();
    public List<TaskDto> tasks = new ArrayList<>();

    /**
     * A calendar exception (non-working day range) for a task's assigned user.
     * Weekends are NOT included here; they are derived from the day-of-week in JS.
     * Only explicit off-day overrides (vacation, sick, trip, holiday) are sent.
     */
    public static class CalendarExceptionDto {
        /**
         * Start date of the exception range (inclusive).
         */
        public LocalDate from;
        /**
         * Single-letter abbreviation: V, S, T, or H.
         */
        public String    letter;
        /**
         * End date of the exception range (inclusive).
         */
        public LocalDate to;
        /**
         * Exception type: VACATION, SICK, TRIP, or HOLIDAY.
         */
        public String    type;
    }

    /**
     * Chart metadata: date range, sprint info, and theme colors.
     */
    public static class Meta {
        public CalendarSize  calendarSize;
        /**
         * Last visible day of the chart (UTC midnight).
         */
        public LocalDateTime chartEnd;
        /**
         * First visible day of the chart (UTC midnight).
         */
        public LocalDateTime chartStart;
        public int           firstDayX;
        /**
         * Current date/time (for the "now" marker line).
         */
        public LocalDateTime now;
        /**
         * Number of extra days rendered after the latest task finish.
         * Mirrors {@code RenderDao.postRun}; used to leave room for task-name labels
         * that extend beyond the right edge of the bar.
         */
        public int           postRun;
        /**
         * Number of extra days rendered before the earliest task start.
         * Mirrors {@code RenderDao.preRun}; used to leave room for user-name labels.
         */
        public int           preRun;
        /**
         * Earliest start date of any task in the sprint (used for milestone "S").
         */
        public LocalDateTime sprintEarliestStartDate;
        /**
         * Latest finish date of any task in the sprint (used for milestone "E").
         */
        public LocalDateTime sprintLatestFinishDate;
        /**
         * Name of the sprint being rendered.
         */
        public String        sprintName;
        /**
         * Sprint status: ACTIVE, CLOSED, or PLANNING (used to determine if "N" milestone should be shown).
         */
        public String        sprintStatus;
        /**
         * Theme colors as a nested class-structured object mirroring Java's {@code Theme} hierarchy.
         * Each sub-theme (chartTheme, ganttTheme, xAxesTheme, ...) contains typed color fields
         * as 0xRRGGBB integers. Mirrors Java: {@code Theme → ChartTheme / GanttTheme / ...}
         */
        public ThemeDto      theme = new ThemeDto();
    }

    /**
     * A finish-to-start dependency relation pointing to the predecessor task.
     */
    public static class RelationDto {
        /**
         * ID of the predecessor (finish) task.
         */
        public UUID    predecessorId;
        /**
         * Whether this relation should be rendered as an arrow.
         */
        public boolean visible;
    }

    /**
     * All rendering data for a single Gantt task row.
     */
    public static class TaskDto {
        /**
         * User availability percentage (e.g., "100%" or "50%").
         */
        public String                     assignedUserAvailability;
        /**
         * User's country (for tooltip).
         */
        public String                     assignedUserCountry;
        /**
         * Name of the assigned resource, shown to the left of the task bar.
         */
        public String                     assignedUserName;
        /**
         * User's state/region (for tooltip).
         */
        public String                     assignedUserState;
        /**
         * Task border color in #rrggbb format (critical vs. normal).
         */
        public String                     borderColor;
        /**
         * Non-working day ranges for the assigned user (weekdays only).
         * Weekends are excluded because the JS derives them from the day-of-week.
         */
        public List<CalendarExceptionDto> calendarExceptions = new ArrayList<>();
        /**
         * True if this task is on the critical path.
         */
        public boolean                    critical;
        /**
         * Task body fill color in #rrggbbaa format (8 hex digits).
         * Alpha encodes the taskTransparency (0=fully transparent, ff=opaque).
         */
        public String                     fillColor;
        /**
         * Scheduled finish date/time.
         */
        public LocalDateTime              finish;
        /**
         * Unique task identifier.
         */
        public UUID                       id;
        /**
         * Short key label shown in the chart (e.g. "T-42").
         */
        public String                     key;
        /**
         * True if manually scheduled.
         */
        public boolean                    manuallyScheduled;
        /**
         * True if this is a milestone (zero-duration, no children).
         */
        public boolean                    milestone;
        /**
         * Full task name.
         */
        public String                     name;
        /**
         * Finish-to-start predecessor relations for drawing dependency arrows.
         */
        public List<RelationDto>          predecessors       = new ArrayList<>();
        /**
         * Completion fraction 0–1.
         */
        public double                     progress;
        /**
         * Progress bar fill color in #rrggbbaa format.
         * A slightly lighter version of the user's base color.
         */
        public String                     progressColor;
        /**
         * Zero-based row index used to compute the Y position in JS.
         */
        public int                        rowIndex;
        /**
         * Scheduled start date/time.
         */
        public LocalDateTime              start;
        /**
         * True if this is a story (has child tasks).
         */
        public boolean                    story;
        /**
         * Task text label color in #rrggbb format.
         */
        public String                     textColor;
    }
}

