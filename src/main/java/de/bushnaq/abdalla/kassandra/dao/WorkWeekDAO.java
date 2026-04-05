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

package de.bushnaq.abdalla.kassandra.dao;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

/**
 * Entity representing a named, globally-defined work week.
 * Each instance defines whether each of the seven days of the week is a working day
 * and, for working days, the work and lunch time ranges.
 * Work weeks are assigned to users via {@link UserWorkWeekDAO}.
 */
@Entity
@Table(name = "work_weeks")
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@BatchSize(size = 10)
public class WorkWeekDAO extends AbstractTimeAwareDAO {

    /** Unique human-readable name (e.g. "5x8", "Arabic 5x8"). */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Transient field: the number of {@link UserWorkWeekDAO} entries that reference this work week.
     * Not persisted; populated by the controller before serialisation.
     */
    @jakarta.persistence.Transient
    private int userCount;

    /** Optional description of the work week. */
    @Column(length = 500)
    private String description;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // --- per-day schedules stored as embedded columns ---

    /** Monday schedule. */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "workingDay",  column = @Column(name = "mon_working_day", nullable = false)),
        @AttributeOverride(name = "workStart",   column = @Column(name = "mon_work_start")),
        @AttributeOverride(name = "workEnd",     column = @Column(name = "mon_work_end")),
        @AttributeOverride(name = "lunchStart",  column = @Column(name = "mon_lunch_start")),
        @AttributeOverride(name = "lunchEnd",    column = @Column(name = "mon_lunch_end"))
    })
    private WorkDayScheduleDAO monday = new WorkDayScheduleDAO();

    /** Tuesday schedule. */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "workingDay",  column = @Column(name = "tue_working_day", nullable = false)),
        @AttributeOverride(name = "workStart",   column = @Column(name = "tue_work_start")),
        @AttributeOverride(name = "workEnd",     column = @Column(name = "tue_work_end")),
        @AttributeOverride(name = "lunchStart",  column = @Column(name = "tue_lunch_start")),
        @AttributeOverride(name = "lunchEnd",    column = @Column(name = "tue_lunch_end"))
    })
    private WorkDayScheduleDAO tuesday = new WorkDayScheduleDAO();

    /** Wednesday schedule. */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "workingDay",  column = @Column(name = "wed_working_day", nullable = false)),
        @AttributeOverride(name = "workStart",   column = @Column(name = "wed_work_start")),
        @AttributeOverride(name = "workEnd",     column = @Column(name = "wed_work_end")),
        @AttributeOverride(name = "lunchStart",  column = @Column(name = "wed_lunch_start")),
        @AttributeOverride(name = "lunchEnd",    column = @Column(name = "wed_lunch_end"))
    })
    private WorkDayScheduleDAO wednesday = new WorkDayScheduleDAO();

    /** Thursday schedule. */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "workingDay",  column = @Column(name = "thu_working_day", nullable = false)),
        @AttributeOverride(name = "workStart",   column = @Column(name = "thu_work_start")),
        @AttributeOverride(name = "workEnd",     column = @Column(name = "thu_work_end")),
        @AttributeOverride(name = "lunchStart",  column = @Column(name = "thu_lunch_start")),
        @AttributeOverride(name = "lunchEnd",    column = @Column(name = "thu_lunch_end"))
    })
    private WorkDayScheduleDAO thursday = new WorkDayScheduleDAO();

    /** Friday schedule. */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "workingDay",  column = @Column(name = "fri_working_day", nullable = false)),
        @AttributeOverride(name = "workStart",   column = @Column(name = "fri_work_start")),
        @AttributeOverride(name = "workEnd",     column = @Column(name = "fri_work_end")),
        @AttributeOverride(name = "lunchStart",  column = @Column(name = "fri_lunch_start")),
        @AttributeOverride(name = "lunchEnd",    column = @Column(name = "fri_lunch_end"))
    })
    private WorkDayScheduleDAO friday = new WorkDayScheduleDAO();

    /** Saturday schedule. */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "workingDay",  column = @Column(name = "sat_working_day", nullable = false)),
        @AttributeOverride(name = "workStart",   column = @Column(name = "sat_work_start")),
        @AttributeOverride(name = "workEnd",     column = @Column(name = "sat_work_end")),
        @AttributeOverride(name = "lunchStart",  column = @Column(name = "sat_lunch_start")),
        @AttributeOverride(name = "lunchEnd",    column = @Column(name = "sat_lunch_end"))
    })
    private WorkDayScheduleDAO saturday = new WorkDayScheduleDAO();

    /** Sunday schedule. */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "workingDay",  column = @Column(name = "sun_working_day", nullable = false)),
        @AttributeOverride(name = "workStart",   column = @Column(name = "sun_work_start")),
        @AttributeOverride(name = "workEnd",     column = @Column(name = "sun_work_end")),
        @AttributeOverride(name = "lunchStart",  column = @Column(name = "sun_lunch_start")),
        @AttributeOverride(name = "lunchEnd",    column = @Column(name = "sun_lunch_end"))
    })
    private WorkDayScheduleDAO sunday = new WorkDayScheduleDAO();
}

