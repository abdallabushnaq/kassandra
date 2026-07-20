// gantt/dto/gantt-burndown-chart-dto.ts
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {AuthorSeriesDto} from './AuthorSeriesDto.js';
import {BurndownMetaDto} from './BurndownMetaDto.js';
import {TaskDto} from './TaskDto.js';

/**
 * DTO representing a complete Gantt burndown chart.
 */
export interface GanttBurndownChartDto {
    meta: BurndownMetaDto;
    authors: AuthorSeriesDto[];
    ganttGuideWithoutBuffer?: number[] | null;
    ganttGuideWithBuffer?: number[] | null;
    tasks: TaskDto[];
}
