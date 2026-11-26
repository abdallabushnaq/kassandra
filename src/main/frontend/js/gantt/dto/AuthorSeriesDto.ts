// gantt/dto/author-series-dto.ts
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

/**
 * DTO representing daily work and remaining-estimate data for one burndown author.
 */
export interface AuthorSeriesDto {
    userName?: string | null;
    color: number;
    totalWorkedSeconds: number;
    totalRemainingSeconds: number;
    workPerDay: number[];
    remainingWorkPerDay: number[];
    tooltipPerDay: (string | null)[];
}
