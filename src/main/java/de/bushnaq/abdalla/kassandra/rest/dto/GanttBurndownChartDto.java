/*
 * Copyright (C) 2025-2026 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0
 */

package de.bushnaq.abdalla.kassandra.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.bushnaq.abdalla.kassandra.report.dao.CalendarSize;

import java.time.LocalDateTime;
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
    public List<AuthorSeriesDto>       authors      = new ArrayList<>();
    /**
     * Burndown chart metadata.
     */
    public BurndownMeta                burndownMeta = new BurndownMeta();
    /**
     * Gantt-derived planned burn-down guide (with buffer — all leaf tasks).
     * Same indexing as {@link #ganttGuideWithoutBuffer}.
     */
    public List<Long>                  ganttGuideWithBuffer;
    /**
     * Gantt-derived planned burn-down guide (without buffer — only tasks that
     * have impact on cost).
     * Element {@code d} is remaining work in seconds at the start of day {@code d}
     * (day 0 = {@code burndownMeta.chartStart}).
     * {@code null} when no Gantt task data is available.
     */
    public List<Long>                  ganttGuideWithoutBuffer;
    /**
     * Gantt task rows for the bottom section (reuses {@link GanttChartDto.TaskDto}).
     */
    public List<GanttChartDto.TaskDto> tasks        = new ArrayList<>();

    // ── Nested classes ──────────────────────────────────────────────────────

    /**
     * Per-author accumulated work series for the stacked burndown polygon.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AuthorSeriesDto {

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

    /**
     * Burndown chart metadata: date window, milestone dates, Y-axis parameters, theme.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BurndownMeta {

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
}
