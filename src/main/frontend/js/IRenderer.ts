// renderer-interface.ts
// IRenderer interface used by CalendarXAxes to reference its parent renderer
// without creating a circular module dependency.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import type {Milestones} from './Milestones.js';
import type {GraphSquare} from './GraphSquare.js';
import type {Theme} from './theme/Theme.js';

export interface IRenderer {
    theme: Theme;
    milestones: Milestones;
    scrollOffset: number;
    days: number;
    firstDayX: number;
    diagram: GraphSquare;

    /** Called once per visible day column during calendar rendering. */
    drawDayBars(g: SVGElement, dayDate: Date): void;
}

