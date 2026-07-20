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

package de.bushnaq.abdalla.kassandra.rest.dto.overview;

import de.bushnaq.abdalla.kassandra.rest.dto.theme.ThemeDto;

import java.time.LocalDateTime;

public class SprintOverviewMetaDto {
    public LocalDateTime chartEnd;
    public LocalDateTime chartStart;
    public String        copyright;
    public Integer       laneCount;
    public LocalDateTime now;
    /**
     * Theme colors as a nested class-structured object mirroring Java's {@code Theme} hierarchy.
     * Mirrors Java: {@code Theme → ChartTheme / GanttTheme / XAxesTheme / ...}
     */
    public ThemeDto      theme   = new ThemeDto();
    public String        version = "v2";
}
