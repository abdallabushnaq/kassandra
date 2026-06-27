// theme/gantt-theme.ts
// Mirrors Java: GanttTheme
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

export class GanttTheme {
    readonly criticalRelationColor:   number | null;
    readonly criticalTaskBorderColor: number | null;
    readonly gridColor:               number | null;
    readonly holidayBgColor:          number | null;
    readonly idBgColor:               number | null;
    readonly idTextColor:             number | null;
    readonly milestoneBgColor:        number | null;
    readonly milestoneTextColor:      number | null;
    readonly outOfOfficeColor:        number | null;
    readonly relationColor:           number | null;
    readonly requestMilestoneColor:   number | null;
    readonly sickBgColor:             number | null;
    readonly storyColor:              number | null;
    readonly storyTextColor:          number | null;
    readonly taskBorderColor:         number | null;
    readonly taskTextColor:           number | null;
    readonly taskTickLineColor:       number | null;
    readonly taskTickTextColor:       number | null;
    /** 0–255; alpha for normal task segments. Mirrors Java GanttTheme.taskTransparency. */
    readonly taskTransparency:        number;
    /** 0–255; alpha for non-working-day segments. Mirrors Java GanttTheme.taskWeekEndTransparency. */
    readonly taskWeekEndTransparency: number;
    readonly tripBgColor:             number | null;
    readonly vacationBgColor:         number | null;

    constructor(data: Record<string, number | null> = {}) {
        this.criticalRelationColor   = data['criticalRelationColor']   ?? null;
        this.criticalTaskBorderColor = data['criticalTaskBorderColor'] ?? null;
        this.gridColor               = data['gridColor']               ?? null;
        this.holidayBgColor          = data['holidayBgColor']          ?? null;
        this.idBgColor               = data['idBgColor']               ?? null;
        this.idTextColor             = data['idTextColor']             ?? null;
        this.milestoneBgColor        = data['milestoneBgColor']        ?? null;
        this.milestoneTextColor      = data['milestoneTextColor']      ?? null;
        this.outOfOfficeColor        = data['outOfOfficeColor']        ?? null;
        this.relationColor           = data['relationColor']           ?? null;
        this.requestMilestoneColor   = data['requestMilestoneColor']   ?? null;
        this.sickBgColor             = data['sickBgColor']             ?? null;
        this.storyColor              = data['storyColor']              ?? null;
        this.storyTextColor          = data['storyTextColor']          ?? null;
        this.taskBorderColor         = data['taskBorderColor']         ?? null;
        this.taskTextColor           = data['taskTextColor']           ?? null;
        this.taskTickLineColor       = data['taskTickLineColor']       ?? null;
        this.taskTickTextColor       = data['taskTickTextColor']       ?? null;
        this.taskTransparency        = data['taskTransparency']        ?? 128;
        this.taskWeekEndTransparency = data['taskWeekEndTransparency'] ?? 64;
        this.tripBgColor             = data['tripBgColor']             ?? null;
        this.vacationBgColor         = data['vacationBgColor']         ?? null;
    }
}

