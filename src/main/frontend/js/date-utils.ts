// date-utils.ts
// Shared date/time utility functions for chart rendering.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

export class DateUtils {
    /** Milliseconds per day (24 hours). */
    static readonly MS = 86_400_000;

    /**
     * Normalizes a date to local midnight (00:00:00 local time).
     */
    static getDayMidnight(date: Date | string): Date {
        const d = typeof date === 'string' ? new Date(date) : date;
        return new Date(d.getFullYear(), d.getMonth(), d.getDate());
    }

    /**
     * Calculates the day index (zero-based) of a date relative to a chart start date.
     */
    static calculateDayIndex(date: Date | string, chartStart: Date): number {
        return Math.round((DateUtils.getDayMidnight(date).getTime() - chartStart.getTime()) / DateUtils.MS);
    }

    /**
     * Calculates the total number of days between two dates (inclusive).
     */
    static calculateDayCount(startDate: Date | string, endDate: Date | string): number {
        return Math.round(
            (DateUtils.getDayMidnight(endDate).getTime() - DateUtils.getDayMidnight(startDate).getTime()) / DateUtils.MS
        ) + 1;
    }

    /**
     * Calculates the number of days between two dates.
     * Mirrors: Java DateUtil.calculateDays(LocalDate startDate, LocalDate endDate)
     */
    static calculateDays(startDate: Date | string, endDate: Date | string): number {
        return Math.floor(
            (DateUtils.getDayMidnight(endDate).getTime() - DateUtils.getDayMidnight(startDate).getTime()) / DateUtils.MS
        );
    }

    /**
     * Adds days to a date.
     * Mirrors: Java DateUtil.addDay(LocalDate start, int days)
     */
    static addDay(date: Date, days: number): Date {
        const result = new Date(date);
        result.setDate(result.getDate() + days);
        return result;
    }

    /**
     * Returns the maximum of two dates.
     * Mirrors: Java DateUtil.max(LocalDate d1, LocalDate d2)
     */
    static maxDate(date1: Date, date2: Date): Date {
        return date1 > date2 ? date1 : date2;
    }

    /**
     * Gets the Sunday of the week containing the given date.
     * Mirrors: Java DateUtil.getWeekSunday(LocalDate date)
     */
    static getWeekSunday(date: Date): Date {
        const result = new Date(date);
        const day = result.getDay();
        const diff = result.getDate() - day + (day === 0 ? -6 : 1);
        result.setDate(diff + 6);
        return result;
    }

    /**
     * Gets the ISO week number of the year (Canada locale).
     * Mirrors: Java WeekFields.of(Locale.CANADA).weekOfWeekBasedYear()
     */
    static getWeekOfYear(date: Date): number {
        const d = new Date(date);
        d.setDate(d.getDate() + 4 - (d.getDay() || 7));
        const yearStart = new Date(d.getFullYear(), 0, 1);
        return Math.ceil((((d.getTime() - yearStart.getTime()) / DateUtils.MS) + 1) / 7);
    }

    /**
     * Counts working days (Mon–Fri) from start to end inclusive.
     * Mirrors Java: DateUtil.calculateWorkingDaysIncluding(LocalDate start, LocalDate end).
     */
    static calculateWorkingDaysIncluding(start: Date, end: Date): number {
        let count = 0;
        let current = DateUtils.getDayMidnight(start);
        const endDay = DateUtils.getDayMidnight(end);
        while (current.getTime() <= endDay.getTime()) {
            const dow = current.getDay();
            if (dow !== 0 && dow !== 6) count++;
            current = new Date(current.getTime() + DateUtils.MS);
        }
        return count;
    }

    /**
     * Creates a date string in format "EEEE dd MMMM yyyy".
     * Mirrors: Java DateUtil.createDateString(LocalDate date, DateTimeFormatter formatter)
     */
    static createDateString(date: Date): string {
        const days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
        const months = [
            'January', 'February', 'March', 'April', 'May', 'June',
            'July', 'August', 'September', 'October', 'November', 'December',
        ];
        return `${days[date.getDay()]} ${date.getDate()} ${months[date.getMonth()]} ${date.getFullYear()}`;
    }

    //TODO "use user calendar"
    static isWorkDay(c: Date): boolean {
        return (c.getDay() != 6 && c.getDay() != 0);
    }
}
