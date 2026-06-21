// date-utils.js
// Shared date/time utility functions for chart rendering.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0
(function () {
    'use strict';

    // Milliseconds per day (24 hours)
    const MS = 86400000;

    /**
     * Normalizes a date to local midnight (00:00:00 local time).
     * @param {Date|string} date The date to normalize
     * @returns {Date} A new Date object at local midnight of the given date
     */
    function getDayMidnight(date) {
        const dateObj = (typeof date === 'string') ? new Date(date) : date;
        return new Date(dateObj.getFullYear(), dateObj.getMonth(), dateObj.getDate());
    }

    /**
     * Calculates the day index (zero-based) of a date relative to the chart start date.
     * @param {Date|string} date The date to calculate the index for
     * @param {Date} chartStart The local midnight of the chart's first day
     * @returns {number} The day index (0 = first day of chart, 1 = second day, etc.)
     */
    function calculateDayIndex(date, chartStart) {
        return Math.round((getDayMidnight(date).getTime() - chartStart.getTime()) / MS);
    }

    /**
     * Calculates the total number of days between two dates (inclusive).
     * @param {Date|string} startDate The start date
     * @param {Date|string} endDate The end date
     * @returns {number} The count of days from startDate to endDate (inclusive)
     */
    function calculateDayCount(startDate, endDate) {
        return Math.round((getDayMidnight(endDate).getTime() - getDayMidnight(startDate).getTime()) / MS) + 1;
    }

    /**
     * Calculates the number of days between two dates.
     * Mirrors: Java DateUtil.calculateDays(LocalDate startDate, LocalDate endDate)
     * @param {Date} startDate The start date
     * @param {Date} endDate The end date
     * @returns {number} The number of days from start to end
     */
    function calculateDays(startDate, endDate) {
        var start = getDayMidnight(startDate);
        var end = getDayMidnight(endDate);
        return Math.floor((end.getTime() - start.getTime()) / MS);
    }

    /**
     * Adds days to a date.
     * Mirrors: Java DateUtil.addDay(LocalDate start, int days)
     * @param {Date} date The start date
     * @param {number} days The number of days to add
     * @returns {Date} A new date with the days added
     */
    function addDay(date, days) {
        var result = new Date(date);
        result.setDate(result.getDate() + days);
        return result;
    }

    /**
     * Returns the maximum of two dates.
     * Mirrors: Java DateUtil.max(LocalDate d1, LocalDate d2)
     * @param {Date} date1 First date
     * @param {Date} date2 Second date
     * @returns {Date} The later of the two dates
     */
    function maxDate(date1, date2) {
        return date1 > date2 ? date1 : date2;
    }

    /**
     * Gets the Sunday of the week containing the given date.
     * Mirrors: Java DateUtil.getWeekSunday(LocalDate date)
     * @param {Date} date The date
     * @returns {Date} The Sunday of the week containing the date
     */
    function getWeekSunday(date) {
        var result = new Date(date);
        var day = result.getDay();
        var diff = result.getDate() - day + (day === 0 ? -6 : 1);
        result.setDate(diff + 6);
        return result;
    }

    /**
     * Gets the week number of year (Canada locale).
     * Mirrors: Java WeekFields.of(Locale.CANADA).weekOfWeekBasedYear()
     * @param {Date} date The date
     * @returns {number} The week number
     */
    function getWeekOfYear(date) {
        var d = new Date(date);
        d.setDate(d.getDate() + 4 - (d.getDay() || 7));
        var yearStart = new Date(d.getFullYear(), 0, 1);
        var weekNumber = Math.ceil((((d - yearStart) / MS) + 1) / 7);
        return weekNumber;
    }

    /**
     * Creates a date string in format "EEEE dd MMMM yyyy".
     * Mirrors: Java DateUtil.createDateString(LocalDate date, DateTimeFormatter formatter)
     * @param {Date} date The date
     * @param {string} pattern The format pattern (currently only "EEEE dd MMMM yyyy" supported)
     * @returns {string} The formatted date string
     */
    function createDateString(date, pattern) {
        var days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
        var months = ['January', 'February', 'March', 'April', 'May', 'June',
            'July', 'August', 'September', 'October', 'November', 'December'];
        return days[date.getDay()] + ' ' + date.getDate() + ' ' + months[date.getMonth()] + ' ' + date.getFullYear();
    }

    // Export to global scope
    window.DateUtils = {
        MS,
        getDayMidnight,
        calculateDayIndex,
        calculateDayCount,
        calculateDays,
        addDay,
        maxDate,
        getWeekSunday,
        getWeekOfYear,
        createDateString
    };
})();

