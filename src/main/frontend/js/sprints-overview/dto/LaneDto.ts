// sprints-overview/dto/lane-dto.ts
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {SprintDto} from './SprintDto.js';

/**
 * DTO representing a lane containing sprints.
 */
export interface LaneDto {
    laneId: number;
    sprints: SprintDto[];
}
