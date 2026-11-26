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
import java.util.UUID;

/**
 * Per-author daily work series for the stacked burndown polygon.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthorSeriesDto {

    /**
     * User ID used to associate task estimates with the assigned author.
     */
    public UUID userId;
    /**
     * Work in seconds, one element per chart day.
     * Element {@code d} contains work on day {@code d - 1}; element 0 is always 0.
     * Length = {@code totalDays + 3}.
     */
    public List<Long>   workPerDay = new ArrayList<>();
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
     * Accumulates the daily work series for server-side consumers.
     *
     * @return cumulative work in seconds for each day slot
     */
    public List<Long> toAccumulatedWorkPerDay() {
        List<Long> accumulated = new ArrayList<>(workPerDay.size());
        long total = 0;
        for (Long work : workPerDay) {
            if (work == null)
                throw new IllegalStateException("Daily work series contains a missing value");
            total += work;
            accumulated.add(total);
        }
        return accumulated;
    }
}
