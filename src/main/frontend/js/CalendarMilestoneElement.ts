// calendar-milestone-element.ts
// Represents the milestone row element with flag styling.
// Mirrors Java: de.bushnaq.abdalla.kassandra.report.dao.CalendarMilestoneElement
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {FontSpec} from './FontSpec.js';

export class CalendarMilestoneElement {
    font: FontSpec;
    bgColor: unknown;
    width: number;
    height: number;
    flagBgColor: unknown;
    flagFont: FontSpec;
    flagHeight: number;
    flagY: number;
    y: number;

    constructor(
        bgColor: unknown,
        flagBgColor: unknown,
        width: number,
        height: number,
        font: FontSpec,
        flagFont: FontSpec,
        flagHeight: number,
    ) {
        this.font = font;
        this.bgColor = bgColor;
        this.width = width;
        this.height = height;
        this.flagBgColor = flagBgColor;
        this.flagFont = flagFont;
        this.flagHeight = flagHeight;
        this.flagY = 0;
        this.y = 0;
    }

    getFlagHeight(): number {
        return this.flagHeight;
    }

    getFlagFont(): FontSpec | null {
        return this.flagFont;
    }

    getWidth(): number {
        return this.width;
    }

    setWidth(w: number): void {
        this.width = w;
    }

    getHeight(): number {
        return this.height;
    }

}

