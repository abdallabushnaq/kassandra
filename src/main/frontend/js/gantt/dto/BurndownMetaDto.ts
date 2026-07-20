// gantt/dto/burndown-meta-dto.ts
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {CalendarSize} from "../../CalendarSize.js";

/**
 * DTO representing metadata for a burndown chart.
 */
export interface BurndownMetaDto {
    firstDayX: number;
    chartStart: string;
    chartEnd: string;
    sprintStart: string;
    sprintEnd: string;
    now?: string | null;
    firstWorklogDate?: string | null;
    lastWorklogDate?: string | null;
    releaseDate?: string | null;
    maxWorkedSeconds: number;
    estimatedBestWorkSeconds: number;
    sprintName?: string | null;
    sprintStatus?: string | null;
    sprintClosed: boolean;
    preRun: number;
    postRun: number;
    totalDays: number;
    theme?: Record<string, unknown>;
    calendarSize: CalendarSize;
}
