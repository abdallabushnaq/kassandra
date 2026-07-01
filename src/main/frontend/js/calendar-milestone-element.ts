// calendar-milestone-element.ts
// Represents the milestone row element with flag styling.
// Mirrors Java: de.bushnaq.abdalla.kassandra.report.dao.CalendarMilestoneElement
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {CalendarElement, FontSpec} from './calendar-element.js';

export class CalendarMilestoneElement extends CalendarElement {
    flagBgColor: unknown;
    flagFont: FontSpec | null;
    flagHeight: number;
    flagY: number;

    constructor(
        bgColor: unknown,
        flagBgColor: unknown,
        width: number,
        height: number,
        font: FontSpec,
        flagFont: FontSpec,
        flagHeight: number,
    ) {
        super(font, bgColor, width, height);
        this.flagBgColor = flagBgColor;
        this.flagFont = flagFont;
        this.flagHeight = flagHeight;
        this.flagY = 0;
    }

    getFlagHeight(): number {
        return this.flagHeight;
    }

    getFlagFont(): FontSpec | null {
        return this.flagFont;
    }
}

