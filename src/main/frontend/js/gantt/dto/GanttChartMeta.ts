// gantt/dto/gantt-chart-sprintOverviewMetaDto.ts
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {CalendarSize} from "../../CalendarSize.js";

/**
 * DTO representing metadata for a Gantt chart.
 */
export interface GanttChartMeta {
    firstDayX: number;
    chartStart: string;
    chartEnd: string;
    copyright: string;
    now?: string;
    sprintStart: string;
    sprintEnd: string;
    sprintStatus?: string;
    sprintName?: string;
    preRun?: number;
    postRun?: number;
    calendarSize: CalendarSize;
    theme?: Record<string, unknown>;
}
