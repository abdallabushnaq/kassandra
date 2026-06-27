// date-utils.ts
// Shared date/time utility functions for chart rendering.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

/** Milliseconds per day (24 hours). */
export const MS = 86_400_000;

/**
 * Normalizes a date to local midnight (00:00:00 local time).
 */
export function getDayMidnight(date: Date | string): Date {
    const d = typeof date === 'string' ? new Date(date) : date;
    return new Date(d.getFullYear(), d.getMonth(), d.getDate());
}

/**
 * Calculates the day index (zero-based) of a date relative to a chart start date.
 */
export function calculateDayIndex(date: Date | string, chartStart: Date): number {
    return Math.round((getDayMidnight(date).getTime() - chartStart.getTime()) / MS);
}

/**
 * Calculates the total number of days between two dates (inclusive).
 */
export function calculateDayCount(startDate: Date | string, endDate: Date | string): number {
    return Math.round(
        (getDayMidnight(endDate).getTime() - getDayMidnight(startDate).getTime()) / MS
    ) + 1;
}

/**
 * Calculates the number of days between two dates.
 * Mirrors: Java DateUtil.calculateDays(LocalDate startDate, LocalDate endDate)
 */
export function calculateDays(startDate: Date | string, endDate: Date | string): number {
    return Math.floor(
        (getDayMidnight(endDate).getTime() - getDayMidnight(startDate).getTime()) / MS
    );
}

/**
 * Adds days to a date.
 * Mirrors: Java DateUtil.addDay(LocalDate start, int days)
 */
export function addDay(date: Date, days: number): Date {
    const result = new Date(date);
    result.setDate(result.getDate() + days);
    return result;
}

/**
 * Returns the maximum of two dates.
 * Mirrors: Java DateUtil.max(LocalDate d1, LocalDate d2)
 */
export function maxDate(date1: Date, date2: Date): Date {
    return date1 > date2 ? date1 : date2;
}

/**
 * Gets the Sunday of the week containing the given date.
 * Mirrors: Java DateUtil.getWeekSunday(LocalDate date)
 */
export function getWeekSunday(date: Date): Date {
    const result = new Date(date);
    const day  = result.getDay();
    const diff = result.getDate() - day + (day === 0 ? -6 : 1);
    result.setDate(diff + 6);
    return result;
}

/**
 * Gets the ISO week number of the year (Canada locale).
 * Mirrors: Java WeekFields.of(Locale.CANADA).weekOfWeekBasedYear()
 */
export function getWeekOfYear(date: Date): number {
    const d = new Date(date);
    d.setDate(d.getDate() + 4 - (d.getDay() || 7));
    const yearStart = new Date(d.getFullYear(), 0, 1);
    return Math.ceil((((d.getTime() - yearStart.getTime()) / MS) + 1) / 7);
}

/**
 * Creates a date string in format "EEEE dd MMMM yyyy".
 * Mirrors: Java DateUtil.createDateString(LocalDate date, DateTimeFormatter formatter)
 */
export function createDateString(date: Date): string {
    const days   = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
    const months = [
        'January', 'February', 'March', 'April', 'May', 'June',
        'July', 'August', 'September', 'October', 'November', 'December',
    ];
    return `${days[date.getDay()]} ${date.getDate()} ${months[date.getMonth()]} ${date.getFullYear()}`;
}

