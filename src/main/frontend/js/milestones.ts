// milestones.ts
// Container for a list of milestones.
// Mirrors Java: de.bushnaq.abdalla.kassandra.report.dao.Milestones
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import { Milestone } from './milestone.js';

export class Milestones {
    list:            Milestone[];
    map:             Record<string, Milestone>;
    firstMilestone:  Date | null;
    lastMilestone:   Date | null;

    /**
     * @param milestonesList Optional initial list of milestones
     * @param firstDate      Optional explicit first milestone date
     * @param lastDate       Optional explicit last milestone date
     */
    constructor(milestonesList: Milestone[] = [], firstDate: Date | null = null, lastDate: Date | null = null) {
        this.list = milestonesList.slice();
        this.map  = {};

        for (const m of this.list) {
            if (m?.symbol) this.map[m.symbol] = m;
        }

        this.firstMilestone = firstDate ?? (this.list.length > 0 ? this.list[0].time : null);
        this.lastMilestone  = lastDate  ?? (this.list.length > 0 ? this.list[this.list.length - 1].time : null);
    }

    /**
     * Adds a new milestone.
     * Mirrors Java: add(LocalDate time, String symbol, String name, Color color, boolean hidden)
     */
    add(time: Date | null | undefined, symbol: string, name: string, hidden = false): void {
        if (time != null) {
            const m = new Milestone(time, symbol, name, false);
            m.hidden = hidden;
            this.list.push(m);
            this.map[symbol] = m;
        }
    }

    /**
     * Adds a milestone object directly.
     * Mirrors Java: add(Milestone m)
     */
    addMilestone(m: Milestone): void {
        if (m) {
            this.list.push(m);
            if (m.symbol) this.map[m.symbol] = m;
        }
    }

    /**
     * Sorts milestones and updates first/last dates.
     * Mirrors Java: calculate()
     */
    calculate(): void {
        this.list.sort((a, b) => a.compareTo(b));
        if (this.list.length > 0) {
            this.firstMilestone = this.list[0].time;
            this.lastMilestone  = this.list[this.list.length - 1].time;
        }
    }

    /**
     * Returns the list of milestones.
     * Mirrors Java: getList()
     */
    getList(): Milestone[] {
        return this.list;
    }

    /**
     * Gets a milestone by symbol.
     * Mirrors Java: get(String name)
     */
    get(symbol: string): Milestone | null {
        return this.map[symbol] ?? null;
    }

    /**
     * Clears all milestones.
     * Mirrors Java: clear()
     */
    clear(): void {
        this.list = [];
        this.map  = {};
    }

    /**
     * Checks if all milestones are hidden.
     * Mirrors Java: empty()
     */
    empty(): boolean {
        for (const m of this.list) {
            if (!m.hidden) return false;
        }
        return true;
    }

    /**
     * Removes a milestone by symbol.
     * Mirrors Java: remove(String name)
     */
    remove(symbol: string): void {
        const idx = this.list.findIndex(m => m.symbol === symbol);
        if (idx !== -1) {
            this.list.splice(idx, 1);
            delete this.map[symbol];
        }
    }
}

