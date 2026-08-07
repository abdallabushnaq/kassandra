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

import java.time.LocalDate;

/**
 * A calendar exception (non-working day range) for an assigned user's calendar.
 * Weekends are NOT included here; they are derived from the day-of-week in JS.
 * Only explicit off-day overrides (vacation, sick, trip, holiday) are sent.
 */
public class CalendarExceptionDto {
    /**
     * Start date of the exception range (inclusive).
     */
    public LocalDate from;
    /**
     * Single-letter abbreviation: V, S, T, or H.
     */
    public String    letter;
    /**
     * Display name supplied by the calendar, for example the public holiday name.
     */
    public String    name;
    /**
     * End date of the exception range (inclusive).
     */
    public LocalDate to;
    /**
     * Exception type: VACATION, SICK, TRIP, or HOLIDAY.
     */
    public String    type;
}
