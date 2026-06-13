/*
 * Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0
 * Theme Color Key Constants
 *
 * This file defines constants for all theme color keys used in sprints-overview-chart.js
 * and calendar-x-axes.js. These constants correspond to the server-side Theme.toMap() output
 * which uses the format "themeName.fieldName" for nested theme objects.
 *
 * Color values are transmitted as 0xRRGGBB integers (no alpha) from the Java server.
 */
(function () {
    'use strict';

    // Export theme color constants
    window.ThemeColorKeys = {
        // ── GanttTheme ──────────────────────────────────────────────────────────
        GANTT_GRID_COLOR: 'ganttTheme.gridColor',
        GANTT_CRITICAL_RELATION_COLOR: 'ganttTheme.criticalRelationColor',
        GANTT_CRITICAL_TASK_BORDER_COLOR: 'ganttTheme.criticalTaskBorderColor',
        GANTT_HOLIDAY_BG_COLOR: 'ganttTheme.holidayBgColor',
        GANTT_ID_BG_COLOR: 'ganttTheme.idBgColor',
        GANTT_ID_TEXT_COLOR: 'ganttTheme.idTextColor',
        GANTT_MILESTONE_BG_COLOR: 'ganttTheme.milestoneBgColor',
        GANTT_MILESTONE_TEXT_COLOR: 'ganttTheme.milestoneTextColor',
        GANTT_OUT_OF_OFFICE_COLOR: 'ganttTheme.outOfOfficeColor',
        GANTT_RELATION_COLOR: 'ganttTheme.relationColor',
        GANTT_REQUEST_MILESTONE_COLOR: 'ganttTheme.requestMilestoneColor',
        GANTT_SICK_BG_COLOR: 'ganttTheme.sickBgColor',
        GANTT_STORY_COLOR: 'ganttTheme.storyColor',
        GANTT_STORY_TEXT_COLOR: 'ganttTheme.storyTextColor',
        GANTT_TASK_BORDER_COLOR: 'ganttTheme.taskBorderColor',
        GANTT_TASK_TEXT_COLOR: 'ganttTheme.taskTextColor',
        GANTT_TASK_TICK_LINE_COLOR: 'ganttTheme.taskTickLineColor',
        GANTT_TASK_TICK_TEXT_COLOR: 'ganttTheme.taskTickTextColor',
        GANTT_TASK_TRANSPARENCY: 'ganttTheme.taskTransparency',
        GANTT_TASK_WEEK_END_TRANSPARENCY: 'ganttTheme.taskWeekEndTransparency',
        GANTT_TRIP_BG_COLOR: 'ganttTheme.tripBgColor',
        GANTT_VACATION_BG_COLOR: 'ganttTheme.vacationBgColor',

        // ── XAxesTheme ──────────────────────────────────────────────────────────
        XAXES_DAY_OF_MONTH_BG_COLOR: 'xAxesTheme.dayOfMonthBgColor',
        XAXES_DAY_OF_MONTH_BORDER_COLOR: 'xAxesTheme.dayOfMonthBorderColor',
        XAXES_DAY_OF_MONTH_TEXT_COLOR: 'xAxesTheme.dayOfMonthTextColor',
        XAXES_DAY_OF_MONTH_WEEKEND_BG_COLOR: 'xAxesTheme.dayOfMonthWeekendBgColor',
        XAXES_DAY_OF_MONTH_WEEKEND_TEXT_COLOR: 'xAxesTheme.dayOfMonthWeekendTextColor',

        XAXES_DAY_OF_WEEK_BORDER_COLOR: 'xAxesTheme.dayOfWeekBorderColor',
        XAXES_DAY_OF_WEEK_TEXT_COLOR: 'xAxesTheme.dayOfWeekTextColor',
        XAXES_DAY_OF_WEEK_WEEKEND_TEXT_COLOR: 'xAxesTheme.dayOfWeekWeekendTextColor',
        XAXES_DAY_OF_WEEK_BG_COLOR: 'xAxesTheme.dayOfweekBgColor',
        XAXES_DAY_OF_WEEK_SATURDAY_BG_COLOR: 'xAxesTheme.dayOfweekSaturdayBgColor',
        XAXES_DAY_OF_WEEK_SUNDAY_BG_COLOR: 'xAxesTheme.dayOfweekSundayBgColor',

        XAXES_FUTURE_EVENT_COLOR: 'xAxesTheme.futureEventColor',
        XAXES_MILESTONE_FLAG_COLOR: 'xAxesTheme.milestoneFlagColor',
        XAXES_MILESTONE_TEXT_COLOR: 'xAxesTheme.milestoneTextColor',

        XAXES_MONTH_BORDER_COLOR: 'xAxesTheme.monthBorderColor',
        XAXES_MONTH_TEXT_COLOR: 'xAxesTheme.monthTextColor',

        XAXES_MONTH_BG_COLORS_PREFIX: 'xAxesTheme.monthBgColors.',

        XAXES_NOW_EVENT_COLOR: 'xAxesTheme.nowEventColor',
        XAXES_PAST_EVENT_COLOR: 'xAxesTheme.pastEventColor',

        XAXES_WEEK_BG_COLOR: 'xAxesTheme.weekBgColor',
        XAXES_WEEK_BORDER_COLOR: 'xAxesTheme.weekBorderColor',
        XAXES_WEEK_TEXT_COLOR: 'xAxesTheme.weekTextColor',

        XAXES_YEAR_BG_COLOR: 'xAxesTheme.yearBgColor',
        XAXES_YEAR_BORDER_COLOR: 'xAxesTheme.yearBorderColor',
        XAXES_YEAR_TEXT_COLOR: 'xAxesTheme.yearTextColor',

        // ── CalendarTheme ───────────────────────────────────────────────────────
        CALENDAR_FILLING_DAY_TEXT_COLOR: 'calendarTheme.fillingDayTextColor',
        CALENDAR_HOLIDAY_BG_COLOR: 'calendarTheme.holidayBgColor',
        CALENDAR_HOLIDAY_TEXT_COLOR: 'calendarTheme.holidayTextColor',
        CALENDAR_MONTH_NAME_COLOR: 'calendarTheme.monthNameColor',
        CALENDAR_NORMAL_DAY_TEXT_COLOR: 'calendarTheme.normalDayTextColor',
        CALENDAR_SICK_BG_COLOR: 'calendarTheme.sickBgColor',
        CALENDAR_SICK_TEXT_COLOR: 'calendarTheme.sickTextColor',
        CALENDAR_TODAY_BG_COLOR: 'calendarTheme.todayBgColor',
        CALENDAR_TODAY_TEXT_COLOR: 'calendarTheme.todayTextColor',
        CALENDAR_TRIP_BG_COLOR: 'calendarTheme.tripBgColor',
        CALENDAR_TRIP_TEXT_COLOR: 'calendarTheme.tripTextColor',
        CALENDAR_VACATION_BG_COLOR: 'calendarTheme.vacationBgColor',
        CALENDAR_VACATION_TEXT_COLOR: 'calendarTheme.vacationTextColor',
        CALENDAR_WEEK_DAY_TEXT_COLOR: 'calendarTheme.weekDayTextColor',
        CALENDAR_WEEKEND_BG_COLOR: 'calendarTheme.weekendBgColor',
        CALENDAR_WEEKEND_TEXT_COLOR: 'calendarTheme.weekendTextColor',
        CALENDAR_YEAR_TEXT_COLOR: 'calendarTheme.yearTextColor',

        // ── ChartTheme ──────────────────────────────────────────────────────────
        CHART_BACKGROUND_COLOR: 'chartTheme.backgroundColor',
        CHART_CAPTION_TEXT_COLOR: 'chartTheme.captionTextColor',
        CHART_BORDER_COLOR: 'chartTheme.chartBorderColor',
        CHART_DAY_OF_WEEK_SATURDAY_BG_COLOR: 'chartTheme.dayOfweekSaturdayBgColor',
        CHART_DAY_OF_WEEK_SUNDAY_BG_COLOR: 'chartTheme.dayOfweekSundayBgColor',
        CHART_FOOTER_TEXT_COLOR: 'chartTheme.footerTextColor',
        CHART_GRAPH_TEXT_BACKGROUND_COLOR: 'chartTheme.graphTextBackgroundColor',
        CHART_SURROUNDING_SQUARE_COLOR: 'chartTheme.surroundingSquareColor',

        // ── BurndownTheme ───────────────────────────────────────────────────────
        BURNDOWN_BORDER_COLOR: 'burndownTheme.borderColor',
        BURNDOWN_DELAY_EVENT_COLOR: 'burndownTheme.delayEventColor',
        BURNDOWN_IN_TIME_COLOR: 'burndownTheme.inTimeColor',
        BURNDOWN_OPTIMALE_GUIDE_COLOR: 'burndownTheme.optimaleGuideColor',
        BURNDOWN_PLANNED_GUIDE_COLOR: 'burndownTheme.plannedGuideColor',
        BURNDOWN_TICK_TEXT_COLOR: 'burndownTheme.tickTextColor',
        BURNDOWN_TICKS_COLOR: 'burndownTheme.ticksColor',
        BURNDOWN_WATERMARK_COLOR: 'burndownTheme.watermarkColor',

        BURNDOWN_BURN_DOWN_COLOR_PREFIX: 'burndownTheme.burnDownColor',
    };
})();

