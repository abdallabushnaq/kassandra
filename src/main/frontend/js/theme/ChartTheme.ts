// theme/chart-theme.ts
// Mirrors Java: ChartTheme
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

export class ChartTheme {
    readonly backgroundColor:          number | null;
    readonly captionTextColor:         number | null;
    readonly chartBorderColor:         number | null;
    readonly dayOfweekSaturdayBgColor: number | null;
    readonly dayOfweekSundayBgColor:   number | null;
    readonly footerTextColor:          number | null;
    readonly graphTextBackgroundColor: number | null;
    readonly surroundingSquareColor:   number | null;

    constructor(data: Record<string, number | null> = {}) {
        this.backgroundColor          = data['backgroundColor']          ?? null;
        this.captionTextColor         = data['captionTextColor']         ?? null;
        this.chartBorderColor         = data['chartBorderColor']         ?? null;
        this.dayOfweekSaturdayBgColor = data['dayOfweekSaturdayBgColor'] ?? null;
        this.dayOfweekSundayBgColor   = data['dayOfweekSundayBgColor']   ?? null;
        this.footerTextColor          = data['footerTextColor']          ?? null;
        this.graphTextBackgroundColor = data['graphTextBackgroundColor'] ?? null;
        this.surroundingSquareColor   = data['surroundingSquareColor']   ?? null;
    }
}

