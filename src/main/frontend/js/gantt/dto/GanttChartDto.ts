// gantt/dto/gantt-chart-dto.ts
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {GanttChartMeta} from './GanttChartMeta.js';
import {TaskDto} from './TaskDto.js';

/**
 * DTO representing a complete Gantt chart with tasks and metadata.
 */
export interface GanttChartDto {
    tasks: TaskDto[];
    meta: GanttChartMeta;
}
