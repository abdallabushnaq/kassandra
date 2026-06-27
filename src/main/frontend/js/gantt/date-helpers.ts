// gantt/date-helpers.ts
// Local date/time parsing helpers used by the Gantt renderer.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

export function parseLocalDate(dateStr: string | null | undefined): Date | null {
    if (!dateStr) return null;
    const p = dateStr.split('-');
    return new Date(+p[0], +p[1] - 1, +p[2]);
}

/**
 * Parses an ISO LocalDateTime string (no timezone suffix) to a JS Date.
 * Strings like "2026-03-01T08:00:00" are treated as local time.
 */
export function parseLocalDateTime(str: string | Date | null | undefined): Date | null {
    if (!str) return null;
    if (str instanceof Date) return str;
    return new Date(str);
}

/**
 * Returns "8:00 AM on same day" datetime string for a given LocalDateTime string.
 * Mirrors Java: date.truncatedTo(ChronoUnit.DAYS).withHour(8)
 */
export function getDayAt8AM(datetimeStr: string | null | undefined): string | null {
    if (!datetimeStr) return null;
    return datetimeStr.split('T')[0] + 'T08:00:00';
}

export interface CalendarException {
    from:    string;
    to:      string;
    type?:   string;
    name?:   string;
    letter?: string;
}

export function getCalendarException(date: Date, exceptions: CalendarException[] | null | undefined): CalendarException | null {
    if (!exceptions?.length) return null;
    for (const ex of exceptions) {
        const from = parseLocalDate(ex.from);
        const to   = parseLocalDate(ex.to);
        if (from && to && date >= from && date <= to) return ex;
    }
    return null;
}

/**
 * Mirrors Java: ProjectCalendar.isWorkingDate(LocalDate).
 * Returns true for Mon–Fri weekdays with no calendar exception.
 */
export function isWorkingDay(date: Date, exceptions: CalendarException[] | null | undefined): boolean {
    const dow = date.getDay();
    if (dow === 0 || dow === 6) return false;
    return getCalendarException(date, exceptions) === null;
}

