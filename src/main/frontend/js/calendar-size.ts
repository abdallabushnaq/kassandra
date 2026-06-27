// calendar-size.ts
// CalendarSize enum – mirrors Java enum CalendarSize.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

export const CalendarSize = {
    YEARS:  'YEARS',
    MONTHS: 'MONTHS',
} as const;

export type CalendarSize = typeof CalendarSize[keyof typeof CalendarSize];

