// graph-square.ts
// Represents a positioned, sized rectangle used for diagram layout.
// Mirrors Java: GraphSquare
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

export class GraphSquare {
    x:      number;
    y:      number;
    width:  number;
    height: number;

    constructor(x = 0, y = 0, width = 0, height = 0) {
        this.x      = x;
        this.y      = y;
        this.width  = width;
        this.height = height;
    }

    initPosition(x: number, y: number): void {
        this.x = x;
        this.y = y;
    }

    initSize(w: number, h: number): void {
        this.width  = w;
        this.height = h;
    }
}

