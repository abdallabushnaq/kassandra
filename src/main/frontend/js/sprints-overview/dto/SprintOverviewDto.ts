// sprints-overview/dto/sprint-overview-dto.ts
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {LaneDto} from './LaneDto.js';
import {SprintOverviewMeta} from './SprintOverviewMeta.js';

/**
 * DTO representing the complete sprints overview data.
 */
export interface SprintOverviewDto {
    lanes: LaneDto[];
    meta: SprintOverviewMeta;
}
