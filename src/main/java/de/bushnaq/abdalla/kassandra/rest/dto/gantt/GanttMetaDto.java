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

import de.bushnaq.abdalla.kassandra.report.dao.CalendarSize;
import de.bushnaq.abdalla.kassandra.rest.dto.theme.ThemeDto;

import java.time.LocalDateTime;

/**
 * Chart metadata: date range, sprint info, and theme colors.
 */
public class GanttMetaDto {
    public CalendarSize  calendarSize;
    /**
     * Last visible day of the chart (UTC midnight).
     */
    public LocalDateTime chartEnd;
    /**
     * First visible day of the chart (UTC midnight).
     */
    public LocalDateTime chartStart;
    public String        copyright;
    public int           firstDayX;
    /**
     * Current date/time (for the "now" marker line).
     */
    public LocalDateTime now;
    /**
     * Number of extra days rendered after the latest task finish.
     * Mirrors {@code RenderDao.postRun}; used to leave room for task-name labels
     * that extend beyond the right edge of the bar.
     */
    public int           postRun;
    /**
     * Number of extra days rendered before the earliest task start.
     * Mirrors {@code RenderDao.preRun}; used to leave room for user-name labels.
     */
    public int           preRun;
    /**
     * Latest finish date of any task in the sprint (used for milestone "E").
     */
    public LocalDateTime sprintEnd;
    /**
     * Name of the sprint being rendered.
     */
    public String        sprintName;
    /**
     * Earliest start date of any task in the sprint (used for milestone "S").
     */
    public LocalDateTime sprintStart;
    /**
     * Sprint status: ACTIVE, CLOSED, or PLANNING (used to determine if "N" milestone should be shown).
     */
    public String        sprintStatus;
    /**
     * Theme colors as a nested class-structured object mirroring Java's {@code Theme} hierarchy.
     * Each sub-theme (chartTheme, ganttTheme, xAxesTheme, ...) contains typed color fields
     * as 0xRRGGBB integers. Mirrors Java: {@code Theme → ChartTheme / GanttTheme / ...}
     */
    public ThemeDto      theme = new ThemeDto();
}
