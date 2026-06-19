// theme-class.js
// Theme class hierarchy – mirrors Java:
//   Theme → ChartTheme / GanttTheme / XAxesTheme / CalendarTheme / BurndownTheme
//
// Theme data is received from the server as a ThemeDto (nested JSON object).
// Each sub-theme class wraps its portion of the server data and provides
// typed property access identical to the Java class field names.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0
(function () {
    'use strict';

    // ── ChartTheme ─────────────────────────────────────────────────────────────
    // Mirrors Java: ChartTheme

    class ChartTheme {
        constructor(data) {
            data = data || {};
            this.backgroundColor          = data.backgroundColor          != null ? data.backgroundColor          : null;
            this.captionTextColor         = data.captionTextColor         != null ? data.captionTextColor         : null;
            this.chartBorderColor         = data.chartBorderColor         != null ? data.chartBorderColor         : null;
            this.dayOfweekSaturdayBgColor = data.dayOfweekSaturdayBgColor != null ? data.dayOfweekSaturdayBgColor : null;
            this.dayOfweekSundayBgColor   = data.dayOfweekSundayBgColor   != null ? data.dayOfweekSundayBgColor   : null;
            this.footerTextColor          = data.footerTextColor          != null ? data.footerTextColor          : null;
            this.graphTextBackgroundColor = data.graphTextBackgroundColor != null ? data.graphTextBackgroundColor : null;
            this.surroundingSquareColor   = data.surroundingSquareColor   != null ? data.surroundingSquareColor   : null;
        }
    }

    // ── GanttTheme ─────────────────────────────────────────────────────────────
    // Mirrors Java: GanttTheme

    class GanttTheme {
        constructor(data) {
            data = data || {};
            this.criticalRelationColor   = data.criticalRelationColor   != null ? data.criticalRelationColor   : null;
            this.criticalTaskBorderColor = data.criticalTaskBorderColor != null ? data.criticalTaskBorderColor : null;
            this.gridColor               = data.gridColor               != null ? data.gridColor               : null;
            this.holidayBgColor          = data.holidayBgColor          != null ? data.holidayBgColor          : null;
            this.idBgColor               = data.idBgColor               != null ? data.idBgColor               : null;
            this.idTextColor             = data.idTextColor             != null ? data.idTextColor             : null;
            this.milestoneBgColor        = data.milestoneBgColor        != null ? data.milestoneBgColor        : null;
            this.milestoneTextColor      = data.milestoneTextColor      != null ? data.milestoneTextColor      : null;
            this.outOfOfficeColor        = data.outOfOfficeColor        != null ? data.outOfOfficeColor        : null;
            this.relationColor           = data.relationColor           != null ? data.relationColor           : null;
            this.requestMilestoneColor   = data.requestMilestoneColor   != null ? data.requestMilestoneColor   : null;
            this.sickBgColor             = data.sickBgColor             != null ? data.sickBgColor             : null;
            this.storyColor              = data.storyColor              != null ? data.storyColor              : null;
            this.storyTextColor          = data.storyTextColor          != null ? data.storyTextColor          : null;
            this.taskBorderColor         = data.taskBorderColor         != null ? data.taskBorderColor         : null;
            this.taskTextColor           = data.taskTextColor           != null ? data.taskTextColor           : null;
            this.taskTickLineColor       = data.taskTickLineColor       != null ? data.taskTickLineColor       : null;
            this.taskTickTextColor       = data.taskTickTextColor       != null ? data.taskTickTextColor       : null;
            /** 0–255; alpha for normal task segments – mirrors Java GanttTheme.taskTransparency */
            this.taskTransparency        = data.taskTransparency        != null ? data.taskTransparency        : 128;
            /** 0–255; alpha for non-working-day segments – mirrors Java GanttTheme.taskWeekEndTransparency */
            this.taskWeekEndTransparency = data.taskWeekEndTransparency != null ? data.taskWeekEndTransparency : 64;
            this.tripBgColor             = data.tripBgColor             != null ? data.tripBgColor             : null;
            this.vacationBgColor         = data.vacationBgColor         != null ? data.vacationBgColor         : null;
        }
    }

    // ── XAxesTheme ─────────────────────────────────────────────────────────────
    // Mirrors Java: XAxesTheme (including monthBgColors[12] array)

    class XAxesTheme {
        constructor(data) {
            data = data || {};
            // Day of Month
            this.dayOfMonthBgColor          = data.dayOfMonthBgColor          != null ? data.dayOfMonthBgColor          : null;
            this.dayOfMonthBorderColor      = data.dayOfMonthBorderColor      != null ? data.dayOfMonthBorderColor      : null;
            this.dayOfMonthTextColor        = data.dayOfMonthTextColor        != null ? data.dayOfMonthTextColor        : null;
            this.dayOfMonthWeekendBgColor   = data.dayOfMonthWeekendBgColor   != null ? data.dayOfMonthWeekendBgColor   : null;
            this.dayOfMonthWeekendTextColor = data.dayOfMonthWeekendTextColor != null ? data.dayOfMonthWeekendTextColor : null;
            // Day of Week
            this.dayOfWeekBorderColor       = data.dayOfWeekBorderColor       != null ? data.dayOfWeekBorderColor       : null;
            this.dayOfWeekTextColor         = data.dayOfWeekTextColor         != null ? data.dayOfWeekTextColor         : null;
            this.dayOfWeekWeekendTextColor  = data.dayOfWeekWeekendTextColor  != null ? data.dayOfWeekWeekendTextColor  : null;
            this.dayOfweekBgColor           = data.dayOfweekBgColor           != null ? data.dayOfweekBgColor           : null;
            this.dayOfweekSaturdayBgColor   = data.dayOfweekSaturdayBgColor   != null ? data.dayOfweekSaturdayBgColor   : null;
            this.dayOfweekSundayBgColor     = data.dayOfweekSundayBgColor     != null ? data.dayOfweekSundayBgColor     : null;
            // Events / Milestones
            this.futureEventColor           = data.futureEventColor           != null ? data.futureEventColor           : null;
            this.milestoneFlagColor         = data.milestoneFlagColor         != null ? data.milestoneFlagColor         : null;
            this.milestoneTextColor         = data.milestoneTextColor         != null ? data.milestoneTextColor         : null;
            // Month – array of 12 colors (Jan–Dec), mirrors Java XAxesTheme.monthBgColors
            this.monthBgColors              = Array.isArray(data.monthBgColors) ? data.monthBgColors.slice() : new Array(12).fill(null);
            this.monthBorderColor           = data.monthBorderColor           != null ? data.monthBorderColor           : null;
            this.monthTextColor             = data.monthTextColor             != null ? data.monthTextColor             : null;
            // Now / Past
            this.nowEventColor              = data.nowEventColor              != null ? data.nowEventColor              : null;
            this.pastEventColor             = data.pastEventColor             != null ? data.pastEventColor             : null;
            // Week
            this.weekBgColor                = data.weekBgColor                != null ? data.weekBgColor                : null;
            this.weekBorderColor            = data.weekBorderColor            != null ? data.weekBorderColor            : null;
            this.weekTextColor              = data.weekTextColor              != null ? data.weekTextColor              : null;
            // Year
            this.yearBgColor                = data.yearBgColor                != null ? data.yearBgColor                : null;
            this.yearBorderColor            = data.yearBorderColor            != null ? data.yearBorderColor            : null;
            this.yearTextColor              = data.yearTextColor              != null ? data.yearTextColor              : null;
        }
    }

    // ── CalendarTheme ──────────────────────────────────────────────────────────
    // Mirrors Java: CalendarTheme

    class CalendarTheme {
        constructor(data) {
            data = data || {};
            this.fillingDayTextColor = data.fillingDayTextColor != null ? data.fillingDayTextColor : null;
            this.holidayBgColor      = data.holidayBgColor      != null ? data.holidayBgColor      : null;
            this.holidayTextColor    = data.holidayTextColor    != null ? data.holidayTextColor    : null;
            this.monthNameColor      = data.monthNameColor      != null ? data.monthNameColor      : null;
            this.normalDayTextColor  = data.normalDayTextColor  != null ? data.normalDayTextColor  : null;
            this.sickBgColor         = data.sickBgColor         != null ? data.sickBgColor         : null;
            this.sickTextColor       = data.sickTextColor       != null ? data.sickTextColor       : null;
            this.todayBgColor        = data.todayBgColor        != null ? data.todayBgColor        : null;
            this.todayTextColor      = data.todayTextColor      != null ? data.todayTextColor      : null;
            this.tripBgColor         = data.tripBgColor         != null ? data.tripBgColor         : null;
            this.tripTextColor       = data.tripTextColor       != null ? data.tripTextColor       : null;
            this.vacationBgColor     = data.vacationBgColor     != null ? data.vacationBgColor     : null;
            this.vacationTextColor   = data.vacationTextColor   != null ? data.vacationTextColor   : null;
            this.weekDayTextColor    = data.weekDayTextColor    != null ? data.weekDayTextColor    : null;
            this.weekendBgColor      = data.weekendBgColor      != null ? data.weekendBgColor      : null;
            this.weekendTextColor    = data.weekendTextColor    != null ? data.weekendTextColor    : null;
            this.yearTextColor       = data.yearTextColor       != null ? data.yearTextColor       : null;
        }
    }

    // ── BurndownTheme ──────────────────────────────────────────────────────────
    // Mirrors Java: BurndownTheme (including burnDownColor[12] array)

    class BurndownTheme {
        constructor(data) {
            data = data || {};
            this.borderColor        = data.borderColor        != null ? data.borderColor        : null;
            /** Array of 12 author colors – mirrors Java BurndownTheme.burnDownColor[] */
            this.burnDownColor      = Array.isArray(data.burnDownColor) ? data.burnDownColor.slice() : new Array(12).fill(null);
            this.delayEventColor    = data.delayEventColor    != null ? data.delayEventColor    : null;
            this.inTimeColor        = data.inTimeColor        != null ? data.inTimeColor        : null;
            this.optimaleGuideColor = data.optimaleGuideColor != null ? data.optimaleGuideColor : null;
            this.plannedGuideColor  = data.plannedGuideColor  != null ? data.plannedGuideColor  : null;
            this.tickTextColor      = data.tickTextColor      != null ? data.tickTextColor      : null;
            this.ticksColor         = data.ticksColor         != null ? data.ticksColor         : null;
            this.watermarkColor     = data.watermarkColor     != null ? data.watermarkColor     : null;
        }

        /** Mirrors Java: BurndownTheme.getAuthorColor(int index) */
        getAuthorColor(index) {
            if (index < 0) index = -index;
            return this.burnDownColor[index % 12];
        }
    }

    // ── Theme ──────────────────────────────────────────────────────────────────
    // Mirrors Java: Theme base class
    // Instantiated from the server-side ThemeDto JSON object.

    class Theme {
        /**
         * @param {Object} data  ThemeDto JSON received from the server (may be null/undefined
         *                       for a default empty theme)
         */
        constructor(data) {
            data = data || {};
            this.themeVariance   = data.themeVariance || 'light';
            this.burndownTheme   = new BurndownTheme(data.burndownTheme);
            this.calendarTheme   = new CalendarTheme(data.calendarTheme);
            this.chartTheme      = new ChartTheme(data.chartTheme);
            this.ganttTheme      = new GanttTheme(data.ganttTheme);
            this.xAxesTheme      = new XAxesTheme(data.xAxesTheme);
        }
    }

    // ── Export to global scope ─────────────────────────────────────────────────

    window.ChartTheme    = ChartTheme;
    window.GanttTheme    = GanttTheme;
    window.XAxesTheme    = XAxesTheme;
    window.CalendarTheme = CalendarTheme;
    window.BurndownTheme = BurndownTheme;
    window.Theme         = Theme;
})();

