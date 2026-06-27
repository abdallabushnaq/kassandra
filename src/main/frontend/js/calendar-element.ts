// calendar-element.ts
// Represents a single calendar row element (year, month, week, day).
// Mirrors Java: de.bushnaq.abdalla.kassandra.report.dao.CalendarElement
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

export interface FontSpec {
    family: string;
    size:   number;
    weight: string;
}

export class CalendarElement {
    font:    FontSpec | null;
    bgColor: unknown;
    width:   number | null;
    height:  number;
    y:       number;

    constructor(font: FontSpec | null, bgColor: unknown, width: number | null, height: number) {
        this.font    = font;
        this.bgColor = bgColor;
        this.width   = width;
        this.height  = height;
        this.y       = 0;
    }

    getWidth():  number | null { return this.width; }
    setWidth(w: number): void  { this.width = w; }
    getHeight(): number        { return this.height; }
    getY():      number        { return this.y; }
    setY(y: number): void      { this.y = y; }
    getFont():   FontSpec | null { return this.font; }
}

