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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-author daily work and remaining-estimate series for the stacked burndown polygon.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthorSeriesDto {

    /**
     * Work logged in seconds, one element per chart day.
     * Element {@code d} = total seconds worked by this author on day {@code d − 1}. Element 0 is always 0.
     * Length = {@code totalDays + 3}.
     * The browser accumulates these daily values so the chart can replay any date.
     */
    public List<Long>   workPerDay = new ArrayList<>();
    /**
     * Remaining work attributed to this author in seconds, one element per chart day.
     * Element {@code d} is the remaining estimate at the end of day {@code d - 1}; element 0 is the
     * estimate immediately before the first chart day. Worklog snapshots update the task estimate
     * from their logged day onward.
     */
    public List<Long>   remainingWorkPerDay = new ArrayList<>();
    /**
     * Bar fill color in {@code #rrggbbaa} format.
     * Matches the associated Gantt task fill color: the user color lightened by 50% with the theme's task transparency.
     */
    public long         color;
    /**
     * Tooltip HTML for each day-slot (parallel to {@link #workPerDay}).
     * Element {@code d} = HTML for work logged <em>on</em> day {@code d − 1}.
     * {@code null}/empty at slots where no work was logged.
     */
    public List<String> tooltipPerDay         = new ArrayList<>();
    /**
     * Total remaining work attributed to this author in seconds.
     */
    public long         totalRemainingSeconds;
    /**
     * Total work logged by this author in seconds.
     */
    public long         totalWorkedSeconds;
    /**
     * Author display name (for legend and tooltips).
     */
    public String       userName;

    /**
     * Converts the daily work series to cumulative work values for server-side exports and analysis.
     *
     * @return cumulative work in seconds, preserving the original series indexing
     */
    public List<Long> toAccumulatedWorkPerDay() {
        List<Long> accumulated = new ArrayList<>(workPerDay.size());
        long total = 0;
        boolean hasCompleteHistory = true;
        for (Long work : workPerDay) {
            if (work == null || !hasCompleteHistory) {
                accumulated.add(null);
                hasCompleteHistory = false;
            } else {
                total += work;
                accumulated.add(total);
            }
        }
        return accumulated;
    }
}
