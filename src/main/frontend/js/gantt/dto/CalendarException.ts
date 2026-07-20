// gantt/dto/calendar-exception.ts
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

/**
 * DTO representing a calendar exception (e.g., holiday, vacation).
 */
export interface CalendarException {
    from: string;
    to: string;
    type?: string;
    name?: string;
    letter?: string;
}
