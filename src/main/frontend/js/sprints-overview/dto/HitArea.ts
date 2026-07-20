// sprints-overview/dto/hit-area.ts
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {SprintDto} from './SprintDto.js';

/**
 * DTO representing a clickable hit area for a sprint.
 */
export interface HitArea {
    sprint: SprintDto;
    x: number;
    y: number;
    width: number;
    height: number;
}
