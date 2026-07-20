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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * All rendering data for a single Gantt task row.
 */
public class TaskDto {
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
