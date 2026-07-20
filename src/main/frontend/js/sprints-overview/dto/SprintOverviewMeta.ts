// sprints-overview/dto/sprint-overview-sprintOverviewMetaDto.ts
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

/**
 * DTO representing metadata for the sprints overview.
 */
export interface SprintOverviewMeta {
    chartStart: string;
    chartEnd: string;
    now: string;
    copyright: string;
    laneCount: number;
    xAxesTheme?: Record<string, unknown>;
    theme?: Record<string, unknown>;
}
