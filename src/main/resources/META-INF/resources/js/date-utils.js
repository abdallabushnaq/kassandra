// date-utils.js
// Shared date/time utility functions for chart rendering.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0
(function () {
    'use strict';

    // Milliseconds per day (24 hours)
    const MS = 86400000;

    /**
     * Normalizes a date to UTC midnight (00:00:00 UTC).
     * @param {Date|string} date The date to normalize
     * @returns {Date} A new Date object at UTC midnight of the given date
     */
    function getUtcDayMidnight(date) {
        const dateObj = (typeof date === 'string') ? new Date(date) : date;
        return new Date(Date.UTC(dateObj.getFullYear(), dateObj.getMonth(), dateObj.getDate()));
    }

    /**
     * Calculates the day index (zero-based) of a date relative to the chart start date.
     * @param {Date|string} date The date to calculate the index for
     * @param {Date} chartStart The UTC midnight of the chart's first day
     * @returns {number} The day index (0 = first day of chart, 1 = second day, etc.)
     */
    function calculateDayIndex(date, chartStart) {
        return Math.round((getUtcDayMidnight(date).getTime() - chartStart.getTime()) / MS);
    }

    /**
     * Calculates the total number of days between two dates (inclusive).
     * @param {Date|string} startDate The start date
     * @param {Date|string} endDate The end date
     * @returns {number} The count of days from startDate to endDate (inclusive)
     */
    function calculateDayCount(startDate, endDate) {
        return Math.round((getUtcDayMidnight(endDate).getTime() - getUtcDayMidnight(startDate).getTime()) / MS) + 1;
    }

    // Export to global scope
    window.DateUtils = {
        MS,
        getUtcDayMidnight,
        calculateDayIndex,
        calculateDayCount
    };
})();
/*
 * Copyright (C) 2025-2026 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0
 */

