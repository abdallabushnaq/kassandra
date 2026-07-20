// sprints-overview/dto/sprint-overview-dto.ts
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {LaneDto} from './lane-dto.js';
import {SprintOverviewMeta} from './sprint-overview-meta.js';

/**
 * DTO representing the complete sprints overview data.
 */
export interface SprintOverviewDto {
    lanes: LaneDto[];
    meta: SprintOverviewMeta;
}
