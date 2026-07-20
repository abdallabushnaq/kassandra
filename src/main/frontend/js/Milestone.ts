// milestone.ts
// Represents a single milestone on a timeline.
// Mirrors Java: de.bushnaq.abdalla.kassandra.report.dao.Milestone
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

export class Milestone {
    time:    Date;
    symbol:  string;
    name:    string;
    nowLine: boolean;
    hidden:  boolean;

    /**
     * @param time    Milestone date
     * @param symbol  Short symbol (e.g., "N", "S", "E")
     * @param name    Descriptive name
     * @param nowLine Whether this is the "now" line (default false)
     */
    constructor(time: Date, symbol: string, name: string, nowLine = false) {
        this.time    = time;
        this.symbol  = symbol;
        this.name    = name;
        this.nowLine = nowLine;
        this.hidden  = false;
    }

    /**
     * Comparison for sorting.
     * Mirrors Java: compareTo()
     */
    compareTo(other: Milestone): number {
        if (this.time < other.time) return -1;
        if (this.time > other.time) return 1;
        return 0;
    }
}

