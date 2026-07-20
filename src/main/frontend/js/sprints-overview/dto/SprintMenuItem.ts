// sprints-overview/dto/sprint-menu-item.ts
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

/**
 * DTO representing a sprint menu item for context menus.
 */
export interface SprintMenuItem {
    id: number | string;
    name?: string;
    key?: string;
    status?: string;
}
