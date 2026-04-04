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

import java.time.DayOfWeek;

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

    /**
     * Returns the working-time schedule for the given day of the week.
     *
     * @param dayOfWeek the day to look up
     * @return the {@link WorkDaySchedule} for that day; never {@code null}
     */
    public WorkDaySchedule getScheduleForDay(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> monday;
            case TUESDAY -> tuesday;
            case WEDNESDAY -> wednesday;
            case THURSDAY -> thursday;
            case FRIDAY -> friday;
            case SATURDAY -> saturday;
            case SUNDAY -> sunday;
        };
    }

    /**
     * Computes the net working minutes in a single working day based on this work-week definition.
     * Uses the first working day found as the representative (all working days are expected to share the
     * same hours). Returns 450 (7.5 h) as a fallback when no working day is defined.
     *
     * @return net working minutes per day
     */
    public int computeMinutesPerDay() {
        for (DayOfWeek day : DayOfWeek.values()) {
            WorkDaySchedule schedule = getScheduleForDay(day);
            if (schedule != null && schedule.isWorkingDay()) {
                return computeMinutesForSchedule(schedule);
            }
        }
        return (int) (7.5 * 60); // fallback
    }

    /**
     * Computes the total net working minutes across all working days in this work-week definition.
     *
     * @return net working minutes per week
     */
    public int computeMinutesPerWeek() {
        int total = 0;
        for (DayOfWeek day : DayOfWeek.values()) {
            WorkDaySchedule schedule = getScheduleForDay(day);
            if (schedule != null && schedule.isWorkingDay()) {
                total += computeMinutesForSchedule(schedule);
            }
        }
        return total == 0 ? 5 * (int) (7.5 * 60) : total; // fallback
    }

    /**
     * Computes the net working minutes for a single day schedule, excluding the lunch break when present.
     *
     * @param schedule the day schedule to evaluate
     * @return net working minutes
     */
    private int computeMinutesForSchedule(WorkDaySchedule schedule) {
        if (schedule.getWorkStart() == null || schedule.getWorkEnd() == null) {
            return (int) (7.5 * 60);
        }
        int total = (int) java.time.Duration.between(schedule.getWorkStart(), schedule.getWorkEnd()).toMinutes();
        if (schedule.getLunchStart() != null && schedule.getLunchEnd() != null) {
            total -= (int) java.time.Duration.between(schedule.getLunchStart(), schedule.getLunchEnd()).toMinutes();
        }
        return total;
    }
}

