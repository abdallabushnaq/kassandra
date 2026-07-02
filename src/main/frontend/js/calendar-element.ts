// calendar-element.ts
// Represents a single calendar row element (year, month, week, day).
// Mirrors Java: de.bushnaq.abdalla.kassandra.report.dao.CalendarElement
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0


import {FontSpec} from "./font-spec.js";

export class CalendarElement {
    font: FontSpec;
    bgColor: unknown;
    width: number | null;
    height: number;
    y: number;

    // metrics: FontMetricsData | null;

    constructor(font: FontSpec, bgColor: unknown, width: number | null, height: number) {
        this.font = font;
        this.bgColor = bgColor;
        this.width = width;
        this.height = height;
        this.y = 0;
    }

    getWidth(): number | null {
        return this.width;
    }

    setWidth(w: number): void {
        this.width = w;
    }

    getHeight(): number {
        return this.height;
    }

    getY(): number {
        return this.y;
    }

    setY(y: number): void {
        this.y = y;
    }

    getFont(): FontSpec {
        return this.font;
    }

}
