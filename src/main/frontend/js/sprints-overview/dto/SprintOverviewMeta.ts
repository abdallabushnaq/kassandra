// sprints-overview/dto/sprint-overview-sprintOverviewMetaDto.ts
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {SprintOverviewZoomSettings} from './SprintOverviewZoomSettings.js';
import type {CalendarAxesZoomSettings} from '../../CalendarAxesZoomSettings.js';

/**
 * DTO representing metadata for the sprints overview.
 */
export interface SprintOverviewMeta {
    chartStart: string;
    chartEnd: string;
    now: string;
    copyright: string;
    laneCount: number;
    zoomSettings: SprintOverviewZoomSettings;
    calendarAxesZoom: CalendarAxesZoomSettings;
    xAxesTheme?: Record<string, unknown>;
    theme?: Record<string, unknown>;
}
