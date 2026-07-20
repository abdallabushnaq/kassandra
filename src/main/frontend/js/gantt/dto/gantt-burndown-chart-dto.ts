// gantt/dto/gantt-burndown-chart-dto.ts
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {AuthorSeriesDto} from './author-series-dto.js';
import {BurndownMetaDto} from './burndown-meta-dto.js';
import {TaskDto} from './task-dto.js';

/**
 * DTO representing a complete Gantt burndown chart.
 */
export interface GanttBurndownChartDto {
    burndownMeta: BurndownMetaDto;
    authors: AuthorSeriesDto[];
    ganttGuideWithoutBuffer?: number[] | null;
    ganttGuideWithBuffer?: number[] | null;
    tasks: TaskDto[];
}
