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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/**
 * DTO representing the working-time schedule for a single day of the week.
 */
@Getter
@Setter
@NoArgsConstructor
public class WorkDaySchedule {

    /** Whether this day is a working day. */
    private boolean   workingDay  = false;

    /** Start of the working period (e.g. 08:00). */
    private LocalTime workStart;

    /** End of the working period (e.g. 17:00). */
    private LocalTime workEnd;

    /** Start of the lunch break (e.g. 12:00). */
    private LocalTime lunchStart;

    /** End of the lunch break (e.g. 13:00). */
    private LocalTime lunchEnd;

    /**
     * Convenience constructor for a full working day.
     *
     * @param workStart  start of working hours
     * @param workEnd    end of working hours
     * @param lunchStart start of lunch break
     * @param lunchEnd   end of lunch break
     */
    public WorkDaySchedule(LocalTime workStart, LocalTime workEnd, LocalTime lunchStart, LocalTime lunchEnd) {
        this.workingDay  = true;
        this.workStart   = workStart;
        this.workEnd     = workEnd;
        this.lunchStart  = lunchStart;
        this.lunchEnd    = lunchEnd;
    }
}

