// gantt/dto/author-series-dto.ts
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

/**
 * DTO representing a series of work data for a single author in a burndown chart.
 */
export interface AuthorSeriesDto {
    userName?: string | null;
    color: number;
    totalWorkedSeconds: number;
    totalRemainingSeconds: number;
    accumulatedWorkPerDay: number[];
    tooltipPerDay: (string | null)[];
}
