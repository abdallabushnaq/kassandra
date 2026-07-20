// sprints-overview/dto/sprint-dto.ts
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

/**
 * DTO representing a sprint in the sprints overview.
 */
export interface SprintDto {
    id: number | string;
    key?: string;
    name?: string;
    start: Date | null;
    end: Date | null;
    status?: string;
    color?: string;
    hasGantt?: boolean;
    delay?: boolean;
}
