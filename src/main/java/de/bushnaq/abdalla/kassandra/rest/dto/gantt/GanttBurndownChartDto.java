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
 * DTO for the interactive client-side combined Gantt + Burndown chart rendered by
 * gantt-burndown-bundle.js.
 * <p>
 * The chart has two vertically stacked sections sharing the same day axis:
 * <ul>
 *   <li>Top: burndown chart (stacked area chart of remaining work per author per day)</li>
 *   <li>Bottom: Gantt chart (task bars, same scroll/zoom)</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GanttBurndownChartDto {

    /**
     * Per-author accumulated work series for the stacked burndown bars.
     * Sorted by author name for consistent legend ordering.
     */
    public List<AuthorSeriesDto> authors = new ArrayList<>();
    /**
     * Gantt-derived planned burn-down guide (with buffer — all leaf tasks).
     * Same indexing as {@link #ganttGuideWithoutBuffer}.
     */
    public List<Long>            ganttGuideWithBuffer;
    /**
     * Gantt-derived planned burn-down guide (without buffer — only tasks that
     * have impact on cost).
     * Element {@code d} is remaining work in seconds at the start of day {@code d}
     * (day 0 = {@code burndownMetaDto.chartStart}).
     * {@code null} when no Gantt task data is available.
     */
    public List<Long>            ganttGuideWithoutBuffer;
    /**
     * Burndown chart metadata.
     */
    public BurndownMetaDto       meta    = new BurndownMetaDto();
    /**
     * Gantt task rows for the bottom section (reuses {@link TaskDto}).
     */
    public List<TaskDto>         tasks   = new ArrayList<>();

    // ── Nested classes ──────────────────────────────────────────────────────

}
