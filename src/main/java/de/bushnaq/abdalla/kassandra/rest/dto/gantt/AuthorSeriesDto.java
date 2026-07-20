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
 * Per-author accumulated work series for the stacked burndown polygon.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthorSeriesDto {

    /**
     * Cumulative work in seconds, one element per chart day.
     * Element {@code d} = total seconds worked by this author from day 0 up to and
     * including end of day {@code d − 1}. Element 0 is always 0.
     * Length = {@code totalDays + 3}.
     * Mirrors Java: {@code usersWorkPerDayAccumulated.get(user)[d].duration.getSeconds()}.
     */
    public List<Long>   accumulatedWorkPerDay = new ArrayList<>();
    /**
     * Bar fill color in {@code #rrggbbaa} format.
     * Mirrors Java: {@code generateBurnDownColor(user.getColor())} →
     * lighten user color 75% towards white, alpha = 128.
     */
    public long         color;
    /**
     * Tooltip HTML for each day-slot (parallel to {@link #accumulatedWorkPerDay}).
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
}
