// graph-color-util.ts
// Utility class for color calculations based on dates and theme.
// Line-by-line port from Java GraphColorUtil.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import { Theme } from './theme/theme.js';

export class GraphColorUtil {

    /**
     * Get background color for day-of-month cell.
     * Mirrors: public static Color getDayOfMonthBgColor(Theme theme, LocalDate startCal)
     */
    static getDayOfMonthBgColor(theme: Theme, date: Date): number | null {
        const dow = date.getDay();
        return (dow === 6 || dow === 0)
            ? theme.xAxesTheme.dayOfMonthWeekendBgColor
            : theme.xAxesTheme.dayOfMonthBgColor;
    }

    /**
     * Get text color for day-of-month cell.
     * Mirrors: public static Color getDayOfMonthTextColor(Theme theme, LocalDate startCal)
     */
    static getDayOfMonthTextColor(theme: Theme, date: Date): number | null {
        const dow = date.getDay();
        return (dow === 6 || dow === 0)
            ? theme.xAxesTheme.dayOfMonthWeekendTextColor
            : theme.xAxesTheme.dayOfMonthTextColor;
    }

    /**
     * Get background color for day-of-week cell.
     * Mirrors: public static Color getDayOfWeekBgColor(Theme theme, LocalDate startCal)
     */
    static getDayOfWeekBgColor(theme: Theme, date: Date): number | null {
        switch (date.getDay()) {
            case 1: case 2: case 3: case 4: case 5: // Mon–Fri
                return theme.xAxesTheme.dayOfweekBgColor;
            case 6: // Saturday
                return theme.xAxesTheme.dayOfweekSaturdayBgColor;
            case 0: // Sunday
                return theme.xAxesTheme.dayOfweekSundayBgColor;
            default:
                return null;
        }
    }

    /**
     * Get background color for day-of-week strip in chart.
     * Mirrors: public static Color getDayOfWeekStripBgColor(Theme theme, LocalDate startCal)
     */
    static getDayOfWeekStripBgColor(theme: Theme, date: Date): number | null {
        switch (date.getDay()) {
            case 1: case 2: case 3: case 4: case 5: // Mon–Fri
                return theme.xAxesTheme.dayOfweekBgColor;
            case 6: // Saturday
                return theme.chartTheme.dayOfweekSaturdayBgColor;
            case 0: // Sunday
                return theme.chartTheme.dayOfweekSundayBgColor;
            default:
                return null;
        }
    }

    /**
     * Get text color for day-of-week cell.
     * Mirrors: public static Color getDayOfWeekTextColor(Theme theme, LocalDate startCal)
     */
    static getDayOfWeekTextColor(theme: Theme, date: Date): number | null {
        const dow = date.getDay();
        return (dow === 6 || dow === 0)
            ? theme.xAxesTheme.dayOfWeekWeekendTextColor
            : theme.xAxesTheme.dayOfWeekTextColor;
    }

    /**
     * Get background color for Gantt chart day stripe.
     * Mirrors: public static Color getGanttDayStripeColor(Theme theme, ProjectCalendar pc, LocalDate currentDate)
     */
    static getGanttDayStripeColor(theme: Theme, _projectCalendar: unknown, currentDate: Date): number | null {
        return this.getDayOfWeekStripBgColor(theme, currentDate);
    }

    /**
     * Get off-day letter (V=vacation, T=trip, S=sick, H=holiday).
     * Mirrors: public static String getOffDayLetter(ProjectCalendarException exception)
     */
    static getOffDayLetter(exception: { name?: string } | null | undefined): string | null {
        if (exception != null) {
            if (exception.name === 'VACATION') return 'V';
            if (exception.name === 'TRIP')     return 'T';
            if (exception.name === 'SICK')     return 'S';
            return 'H';
        }
        return null;
    }
}

