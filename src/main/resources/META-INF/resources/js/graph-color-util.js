/*
 * GraphColorUtil.js - Utility class for color calculations
 *
 * Line-by-line port from Java GraphColorUtil.
 * Static utility methods for determining calendar/chart colors based on date and theme.
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0
 */
(function () {
    'use strict';

    /**
     * Utility class for color determination based on dates and theme.
     * Mirrors: de.bushnaq.abdalla.kassandra.report.dao.GraphColorUtil
     */
    class GraphColorUtil {

        /**
         * Get background color for day-of-month cell.
         * Mirrors: public static Color getDayOfMonthBgColor(Theme theme, LocalDate startCal)
         */
        static getDayOfMonthBgColor(theme, date) {
            var dow = date.getDay();
            // Saturday (6) or Sunday (0)
            if (dow === 6 || dow === 0) {
                return theme.xAxesTheme.dayOfMonthWeekendBgColor;
            }
            // Monday-Friday
            return theme.xAxesTheme.dayOfMonthBgColor;
        }

        /**
         * Get text color for day-of-month cell.
         * Mirrors: public static Color getDayOfMonthTextColor(Theme theme, LocalDate startCal)
         */
        static getDayOfMonthTextColor(theme, date) {
            var dow = date.getDay();
            // Saturday (6) or Sunday (0)
            if (dow === 6 || dow === 0) {
                return theme.xAxesTheme.dayOfMonthWeekendTextColor;
            }
            // Monday-Friday
            return theme.xAxesTheme.dayOfMonthTextColor;
        }

        /**
         * Get background color for day-of-week cell.
         * Mirrors: public static Color getDayOfWeekBgColor(Theme theme, LocalDate startCal)
         */
        static getDayOfWeekBgColor(theme, date) {
            var dow = date.getDay();
            switch (dow) {
                case 1: // Monday
                case 2: // Tuesday
                case 3: // Wednesday
                case 4: // Thursday
                case 5: // Friday
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
        static getDayOfWeekStripBgColor(theme, date) {
            var dow = date.getDay();
            switch (dow) {
                case 1: // Monday
                case 2: // Tuesday
                case 3: // Wednesday
                case 4: // Thursday
                case 5: // Friday
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
        static getDayOfWeekTextColor(theme, date) {
            var dow = date.getDay();
            // Saturday (6) or Sunday (0)
            if (dow === 6 || dow === 0) {
                return theme.xAxesTheme.dayOfWeekWeekendTextColor;
            }
            // Monday-Friday
            return theme.xAxesTheme.dayOfWeekTextColor;
        }

        /**
         * Get background color for Gantt chart day stripe.
         * Mirrors: public static Color getGanttDayStripeColor(Theme theme, ProjectCalendar pc, LocalDate currentDate)
         *
         * Note: Simplified version without ProjectCalendar functionality (may need adjustment per usage)
         */
        static getGanttDayStripeColor(theme, projectCalendar, currentDate) {
            var dow = currentDate.getDay();

            // For now, return strip color for weekend/weekday
            if (dow === 6 || dow === 0) {
                return this.getDayOfWeekStripBgColor(theme, currentDate);
            }

            // For working dates (simplified - no ProjectCalendar exception handling)
            return this.getDayOfWeekStripBgColor(theme, currentDate);
        }

        /**
         * Get off-day letter (V=vacation, T=trip, S=sick, H=holiday).
         * Mirrors: public static String getOffDayLetter(ProjectCalendarException exception)
         *
         * Note: Simplified version without ProjectCalendarException
         */
        static getOffDayLetter(exception) {
            if (exception != null) {
                if (exception.name === 'VACATION') {
                    return 'V';
                } else if (exception.name === 'TRIP') {
                    return 'T';
                } else if (exception.name === 'SICK') {
                    return 'S';
                } else {
                    return 'H';
                }
            }
            return null;
        }
    }

    // ── Export to global namespace ──────────────────────────────────────────
    window.GraphColorUtil = GraphColorUtil;


})();

