// theme/theme.ts
// Mirrors Java: Theme base class
// Instantiated from the server-side ThemeDto JSON object.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import { BurndownTheme } from './burndown-theme.js';
import { CalendarTheme } from './calendar-theme.js';
import { ChartTheme }    from './chart-theme.js';
import { GanttTheme }    from './gantt-theme.js';
import { XAxesTheme }    from './xaxes-theme.js';

export class Theme {
    readonly themeVariance: string;
    readonly burndownTheme: BurndownTheme;
    readonly calendarTheme: CalendarTheme;
    readonly chartTheme:    ChartTheme;
    readonly ganttTheme:    GanttTheme;
    readonly xAxesTheme:    XAxesTheme;

    /**
     * @param data ThemeDto JSON received from the server (may be null/undefined
     *             for a default empty theme)
     */
    constructor(data: Record<string, unknown> = {}) {
        this.themeVariance = (data['themeVariance'] as string) || 'light';
        this.burndownTheme = new BurndownTheme((data['burndownTheme'] as Record<string, unknown>) || {});
        this.calendarTheme = new CalendarTheme((data['calendarTheme'] as Record<string, number | null>) || {});
        this.chartTheme    = new ChartTheme((data['chartTheme']    as Record<string, number | null>) || {});
        this.ganttTheme    = new GanttTheme((data['ganttTheme']    as Record<string, number | null>) || {});
        this.xAxesTheme    = new XAxesTheme((data['xAxesTheme']    as Record<string, unknown>) || {});
    }
}

