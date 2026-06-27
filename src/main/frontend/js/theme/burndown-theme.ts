// theme/burndown-theme.ts
// Mirrors Java: BurndownTheme (including burnDownColor[12] array)
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

export class BurndownTheme {
    readonly borderColor:        number | null;
    /** Array of 12 author colors. Mirrors Java BurndownTheme.burnDownColor[]. */
    readonly burnDownColor:      (number | null)[];
    readonly delayEventColor:    number | null;
    readonly inTimeColor:        number | null;
    readonly optimaleGuideColor: number | null;
    readonly plannedGuideColor:  number | null;
    readonly tickTextColor:      number | null;
    readonly ticksColor:         number | null;
    readonly watermarkColor:     number | null;

    constructor(data: Record<string, unknown> = {}) {
        this.borderColor        = (data['borderColor']        as number | null) ?? null;
        this.burnDownColor      = Array.isArray(data['burnDownColor'])
            ? (data['burnDownColor'] as (number | null)[]).slice()
            : new Array<number | null>(12).fill(null);
        this.delayEventColor    = (data['delayEventColor']    as number | null) ?? null;
        this.inTimeColor        = (data['inTimeColor']        as number | null) ?? null;
        this.optimaleGuideColor = (data['optimaleGuideColor'] as number | null) ?? null;
        this.plannedGuideColor  = (data['plannedGuideColor']  as number | null) ?? null;
        this.tickTextColor      = (data['tickTextColor']      as number | null) ?? null;
        this.ticksColor         = (data['ticksColor']         as number | null) ?? null;
        this.watermarkColor     = (data['watermarkColor']     as number | null) ?? null;
    }

    /** Mirrors Java: BurndownTheme.getAuthorColor(int index) */
    getAuthorColor(index: number): number | null {
        const i = index < 0 ? -index : index;
        return this.burnDownColor[i % 12] ?? null;
    }
}

