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

package de.bushnaq.abdalla.kassandra.dto;

import lombok.*;

/**
 * DTO representing a globally-defined work week.
 * Each instance describes the working schedule for all seven days of the week.
 * Work weeks are assigned to users via {@link UserWorkWeek}.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
public class WorkWeek extends AbstractTimeAware implements Comparable<WorkWeek> {

    /** Unique human-readable name (e.g. "5x8", "Arabic 5x8"). */
    private String          name;

    /** Optional description. */
    private String          description;

    /** Primary key. */
    private Long            id;

    /** Monday schedule. */
    private WorkDaySchedule monday    = new WorkDaySchedule();

    /** Tuesday schedule. */
    private WorkDaySchedule tuesday   = new WorkDaySchedule();

    /** Wednesday schedule. */
    private WorkDaySchedule wednesday = new WorkDaySchedule();

    /** Thursday schedule. */
    private WorkDaySchedule thursday  = new WorkDaySchedule();

    /** Friday schedule. */
    private WorkDaySchedule friday    = new WorkDaySchedule();

    /** Saturday schedule. */
    private WorkDaySchedule saturday  = new WorkDaySchedule();

    /** Sunday schedule. */
    private WorkDaySchedule sunday    = new WorkDaySchedule();

    @Override
    public int compareTo(WorkWeek other) {
        if (this.id == null || other.id == null) return 0;
        return this.id.compareTo(other.id);
    }

    /**
     * Returns a summary string listing the abbreviations of working days.
     * Used as display text in the grid.
     *
     * @return comma-separated abbreviations of working days, e.g. "Mon, Tue, Wed, Thu, Fri"
     */
    public String getWorkingDaysSummary() {
        StringBuilder sb = new StringBuilder();
        addDay(sb, monday,    "Mon");
        addDay(sb, tuesday,   "Tue");
        addDay(sb, wednesday, "Wed");
        addDay(sb, thursday,  "Thu");
        addDay(sb, friday,    "Fri");
        addDay(sb, saturday,  "Sat");
        addDay(sb, sunday,    "Sun");
        return sb.toString();
    }

    private void addDay(StringBuilder sb, WorkDaySchedule schedule, String label) {
        if (schedule != null && schedule.isWorkingDay()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(label);
        }
    }
}

