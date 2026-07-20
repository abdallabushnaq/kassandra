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

package de.bushnaq.abdalla.kassandra.rest.dto.theme;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.bushnaq.abdalla.kassandra.report.dao.theme.Theme;

import java.awt.*;

/**
 * DTO that mirrors the server-side {@link Theme} class hierarchy for JSON transport.
 * <p>
 * Instead of a flat key→value map, the theme is sent as a nested object that
 * mirrors the Java {@code Theme → ChartTheme / GanttTheme / XAxesTheme / ...} structure.
 * Color values are transmitted as 0xRRGGBB integers (no alpha).
 * Integer fields (e.g. taskTransparency) are transmitted as plain integers.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ThemeDto {

    public BurndownThemeDto burndownTheme = new BurndownThemeDto();
    public CalendarThemeDto calendarTheme = new CalendarThemeDto();
    public ChartThemeDto    chartTheme    = new ChartThemeDto();
    public GanttThemeDto    ganttTheme    = new GanttThemeDto();
    /**
     * "light" or "dark" – mirrors {@code Theme.themeVariance.name()}
     */
    public String           themeVariance;
    public XAxesThemeDto    xAxesTheme    = new XAxesThemeDto();

    // ── Sub-theme DTOs ─────────────────────────────────────────────────────────

    // ── Factory ────────────────────────────────────────────────────────────────

    /**
     * Converts a server-side {@link Theme} instance into a {@link ThemeDto}
     * suitable for JSON serialisation.
     *
     * @param theme the theme to convert (must not be null)
     * @return populated DTO
     */
    public static ThemeDto fromTheme(Theme theme) {
        ThemeDto dto = new ThemeDto();
        dto.themeVariance = theme.themeVariance.name();

        // ── ChartTheme ─────────────────────────────────────────────────────────
        dto.chartTheme.backgroundColor          = rgb(theme.chartTheme.backgroundColor);
        dto.chartTheme.captionTextColor         = rgb(theme.chartTheme.captionTextColor);
        dto.chartTheme.chartBorderColor         = rgb(theme.chartTheme.chartBorderColor);
        dto.chartTheme.dayOfweekSaturdayBgColor = rgb(theme.chartTheme.dayOfweekSaturdayBgColor);
        dto.chartTheme.dayOfweekSundayBgColor   = rgb(theme.chartTheme.dayOfweekSundayBgColor);
        dto.chartTheme.footerTextColor          = rgb(theme.chartTheme.footerTextColor);
        dto.chartTheme.graphTextBackgroundColor = rgb(theme.chartTheme.graphTextBackgroundColor);
        dto.chartTheme.surroundingSquareColor   = rgb(theme.chartTheme.surroundingSquareColor);

        // ── GanttTheme ─────────────────────────────────────────────────────────
        dto.ganttTheme.criticalRelationColor   = rgb(theme.ganttTheme.criticalRelationColor);
        dto.ganttTheme.criticalTaskBorderColor = rgb(theme.ganttTheme.criticalTaskBorderColor);
        dto.ganttTheme.gridColor               = rgb(theme.ganttTheme.gridColor);
        dto.ganttTheme.holidayBgColor          = rgb(theme.ganttTheme.holidayBgColor);
        dto.ganttTheme.idBgColor               = rgb(theme.ganttTheme.idBgColor);
        dto.ganttTheme.idTextColor             = rgb(theme.ganttTheme.idTextColor);
        dto.ganttTheme.milestoneBgColor        = rgb(theme.ganttTheme.milestoneBgColor);
        dto.ganttTheme.milestoneTextColor      = rgb(theme.ganttTheme.milestoneTextColor);
        dto.ganttTheme.outOfOfficeColor        = rgb(theme.ganttTheme.outOfOfficeColor);
        dto.ganttTheme.relationColor           = rgb(theme.ganttTheme.relationColor);
        dto.ganttTheme.requestMilestoneColor   = rgb(theme.ganttTheme.requestMilestoneColor);
        dto.ganttTheme.sickBgColor             = rgb(theme.ganttTheme.sickBgColor);
        dto.ganttTheme.storyColor              = rgb(theme.ganttTheme.storyColor);
        dto.ganttTheme.storyTextColor          = rgb(theme.ganttTheme.storyTextColor);
        dto.ganttTheme.taskBorderColor         = rgb(theme.ganttTheme.taskBorderColor);
        dto.ganttTheme.taskTextColor           = rgb(theme.ganttTheme.taskTextColor);
        dto.ganttTheme.taskTickLineColor       = rgb(theme.ganttTheme.taskTickLineColor);
        dto.ganttTheme.taskTickTextColor       = rgb(theme.ganttTheme.taskTickTextColor);
        dto.ganttTheme.taskTransparency        = theme.ganttTheme.taskTransparency;
        dto.ganttTheme.taskWeekEndTransparency = theme.ganttTheme.taskWeekEndTransparency;
        dto.ganttTheme.tripBgColor             = rgb(theme.ganttTheme.tripBgColor);
        dto.ganttTheme.vacationBgColor         = rgb(theme.ganttTheme.vacationBgColor);

        // ── XAxesTheme ─────────────────────────────────────────────────────────
        dto.xAxesTheme.dayOfMonthBgColor          = rgb(theme.xAxesTheme.dayOfMonthBgColor);
        dto.xAxesTheme.dayOfMonthBorderColor      = rgb(theme.xAxesTheme.dayOfMonthBorderColor);
        dto.xAxesTheme.dayOfMonthTextColor        = rgb(theme.xAxesTheme.dayOfMonthTextColor);
        dto.xAxesTheme.dayOfMonthWeekendBgColor   = rgb(theme.xAxesTheme.dayOfMonthWeekendBgColor);
        dto.xAxesTheme.dayOfMonthWeekendTextColor = rgb(theme.xAxesTheme.dayOfMonthWeekendTextColor);
        dto.xAxesTheme.dayOfWeekBorderColor       = rgb(theme.xAxesTheme.dayOfWeekBorderColor);
        dto.xAxesTheme.dayOfWeekTextColor         = rgb(theme.xAxesTheme.dayOfWeekTextColor);
        dto.xAxesTheme.dayOfWeekWeekendTextColor  = rgb(theme.xAxesTheme.dayOfWeekWeekendTextColor);
        dto.xAxesTheme.dayOfweekBgColor           = rgb(theme.xAxesTheme.dayOfweekBgColor);
        dto.xAxesTheme.dayOfweekSaturdayBgColor   = rgb(theme.xAxesTheme.dayOfweekSaturdayBgColor);
        dto.xAxesTheme.dayOfweekSundayBgColor     = rgb(theme.xAxesTheme.dayOfweekSundayBgColor);
        dto.xAxesTheme.futureEventColor           = rgb(theme.xAxesTheme.futureEventColor);
        dto.xAxesTheme.milestoneFlagColor         = rgb(theme.xAxesTheme.milestoneFlagColor);
        dto.xAxesTheme.milestoneTextColor         = rgb(theme.xAxesTheme.milestoneTextColor);
        if (theme.xAxesTheme.monthBgColors != null) {
            for (int i = 0; i < theme.xAxesTheme.monthBgColors.length && i < 12; i++) {
                dto.xAxesTheme.monthBgColors[i] = rgb(theme.xAxesTheme.monthBgColors[i]);
            }
        }
        dto.xAxesTheme.monthBorderColor = rgb(theme.xAxesTheme.monthBorderColor);
        dto.xAxesTheme.monthTextColor   = rgb(theme.xAxesTheme.monthTextColor);
        dto.xAxesTheme.nowEventColor    = rgb(theme.xAxesTheme.nowEventColor);
        dto.xAxesTheme.pastEventColor   = rgb(theme.xAxesTheme.pastEventColor);
        dto.xAxesTheme.weekBgColor      = rgb(theme.xAxesTheme.weekBgColor);
        dto.xAxesTheme.weekBorderColor  = rgb(theme.xAxesTheme.weekBorderColor);
        dto.xAxesTheme.weekTextColor    = rgb(theme.xAxesTheme.weekTextColor);
        dto.xAxesTheme.yearBgColor      = rgb(theme.xAxesTheme.yearBgColor);
        dto.xAxesTheme.yearBorderColor  = rgb(theme.xAxesTheme.yearBorderColor);
        dto.xAxesTheme.yearTextColor    = rgb(theme.xAxesTheme.yearTextColor);

        // ── CalendarTheme ──────────────────────────────────────────────────────
        dto.calendarTheme.fillingDayTextColor = rgb(theme.calendarTheme.fillingDayTextColor);
        dto.calendarTheme.holidayBgColor      = rgb(theme.calendarTheme.holidayBgColor);
        dto.calendarTheme.holidayTextColor    = rgb(theme.calendarTheme.holidayTextColor);
        dto.calendarTheme.monthNameColor      = rgb(theme.calendarTheme.monthNameColor);
        dto.calendarTheme.normalDayTextColor  = rgb(theme.calendarTheme.normalDayTextColor);
        dto.calendarTheme.sickBgColor         = rgb(theme.calendarTheme.sickBgColor);
        dto.calendarTheme.sickTextColor       = rgb(theme.calendarTheme.sickTextColor);
        dto.calendarTheme.todayBgColor        = rgb(theme.calendarTheme.todayBgColor);
        dto.calendarTheme.todayTextColor      = rgb(theme.calendarTheme.todayTextColor);
        dto.calendarTheme.tripBgColor         = rgb(theme.calendarTheme.tripBgColor);
        dto.calendarTheme.tripTextColor       = rgb(theme.calendarTheme.tripTextColor);
        dto.calendarTheme.vacationBgColor     = rgb(theme.calendarTheme.vacationBgColor);
        dto.calendarTheme.vacationTextColor   = rgb(theme.calendarTheme.vacationTextColor);
        dto.calendarTheme.weekDayTextColor    = rgb(theme.calendarTheme.weekDayTextColor);
        dto.calendarTheme.weekendBgColor      = rgb(theme.calendarTheme.weekendBgColor);
        dto.calendarTheme.weekendTextColor    = rgb(theme.calendarTheme.weekendTextColor);
        dto.calendarTheme.yearTextColor       = rgb(theme.calendarTheme.yearTextColor);

        // ── BurndownTheme ──────────────────────────────────────────────────────
        dto.burndownTheme.borderColor        = rgb(theme.burndownTheme.borderColor);
        dto.burndownTheme.delayEventColor    = rgb(theme.burndownTheme.delayEventColor);
        dto.burndownTheme.inTimeColor        = rgb(theme.burndownTheme.inTimeColor);
        dto.burndownTheme.optimaleGuideColor = rgb(theme.burndownTheme.optimaleGuideColor);
        dto.burndownTheme.plannedGuideColor  = rgb(theme.burndownTheme.plannedGuideColor);
        dto.burndownTheme.tickTextColor      = rgb(theme.burndownTheme.tickTextColor);
        dto.burndownTheme.ticksColor         = rgb(theme.burndownTheme.ticksColor);
        dto.burndownTheme.watermarkColor     = rgb(theme.burndownTheme.watermarkColor);
        if (theme.burndownTheme.burnDownColor != null) {
            for (int i = 0; i < theme.burndownTheme.burnDownColor.length && i < 12; i++) {
                dto.burndownTheme.burnDownColor[i] = rgb(theme.burndownTheme.burnDownColor[i]);
            }
        }

        return dto;
    }

    /**
     * Converts a {@link Color} to a 0xRRGGBB integer (alpha discarded).
     */
    private static Integer rgb(Color c) {
        if (c == null) return null;
        return ((c.getRed() & 0xff) << 16) | ((c.getGreen() & 0xff) << 8) | (c.getBlue() & 0xff);
    }
}

