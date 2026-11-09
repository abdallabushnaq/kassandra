// theme/sprint-overview-theme.ts
// Mirrors Java: SprintOverviewTheme
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

export class SprintOverviewTheme {
    readonly createdSprintBgColor: number | null;
    readonly startedSprintBgColor: number | null;
    readonly closedSprintBgColor: number | null;
    readonly sprintTextColor: number | null;

    constructor(data: Record<string, number | null> = {}) {
        this.createdSprintBgColor = data['createdSprintBgColor'] ?? null;
        this.startedSprintBgColor = data['startedSprintBgColor'] ?? null;
        this.closedSprintBgColor = data['closedSprintBgColor'] ?? null;
        this.sprintTextColor = data['sprintTextColor'] ?? null;
    }
}
