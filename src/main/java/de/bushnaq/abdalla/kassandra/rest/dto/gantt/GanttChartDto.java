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
 * DTO for the interactive client-side Gantt chart rendered by gantt-chart.js.
 * <p>
 * Contains all data needed to render the chart client-side including:
 * task scheduling information, colors, calendar exceptions (non-working days),
 * dependency relations, and theme colors.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GanttChartDto {

    public GanttMetaDto  meta  = new GanttMetaDto();
    public List<TaskDto> tasks = new ArrayList<>();

}

