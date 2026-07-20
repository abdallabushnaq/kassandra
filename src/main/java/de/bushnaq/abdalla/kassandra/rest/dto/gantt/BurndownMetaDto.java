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
import de.bushnaq.abdalla.kassandra.report.dao.CalendarSize;
import de.bushnaq.abdalla.kassandra.rest.dto.theme.ThemeDto;

import java.time.LocalDateTime;

/**
 * Burndown chart metadata: date window, milestone dates, Y-axis parameters, theme.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BurndownMetaDto {

    public CalendarSize  calendarSize;
    /**
     * Chart window end: {@code sprintEnd + postRun} days.
     */
    public LocalDateTime chartEnd;
    /**
     * Chart window start: {@code sprintStart − preRun} days.
     * Day 0 for both the burndown and Gantt renderers.
     */
    public LocalDateTime chartStart;
    public String        copyright;
    /**
     * Optimal guide: best-case estimated work in seconds.
     * Currently the same as {@link #maxWorkedSeconds}.
     */
    public long          estimatedBestWorkSeconds;
    public int           firstDayX;
    /**
     * F milestone: date of the first worklog entry. {@code null} if no worklogs.
     */
    public LocalDateTime firstWorklogDate;
    /**
     * L milestone: last-day-with-value + 1. {@code null} if no worklogs.
     * Mirrors Java: {@code milestones.add(calendarFromDayIndex(lastDayIndexWithValue + 1), "L", ...)}.
     */
    public LocalDateTime lastWorklogDate;
    /**
     * Y-axis maximum in seconds: total estimated work (worked + remaining).
     * Mirrors Java: {@code DateUtil.add(sprint.getWorked(), sprint.getRemaining())}.
     */
    public long          maxWorkedSeconds;
    /**
     * N milestone: current date/time. {@code null} when hidden (sprint closed
     * and now is more than 7 days after sprintEnd).
     */
    public LocalDateTime now;
    /**
     * Days shown after {@code sprintEnd} in the chart window.
     */
    public int           postRun;
    /**
     * Days shown before {@code sprintStart} in the chart window.
     */
    public int           preRun;
    /**
     * R milestone: projected release date. {@code null} if unavailable.
     */
    public LocalDateTime releaseDate;
    /**
     * {@code true} if the sprint is closed (shows CLOSED watermark).
     */
    public boolean       sprintClosed;
    /**
     * E milestone: sprint planned end date (= {@code sprint.getEnd()}).
     */
    public LocalDateTime sprintEnd;
    /**
     * Sprint name shown in the caption.
     */
    public String        sprintName;
    /**
     * S milestone: sprint planned start date (= {@code sprint.getStart()}).
     */
    public LocalDateTime sprintStart;
    /**
     * Sprint status: ACTIVE, CLOSED, PLANNING, etc.
     */
    public String        sprintStatus;
    /**
     * Theme colors, serialized as a nested {@link ThemeDto} object.
     */
    public ThemeDto      theme;
    /**
     * Total chart days from {@code chartStart} to {@code chartEnd} inclusive.
     */
    public int           totalDays;
}
