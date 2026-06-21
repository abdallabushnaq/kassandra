// milestone.js
// Milestone and Milestones classes for chart rendering.
// Mirrors Java: de.bushnaq.abdalla.kassandra.report.dao.Milestone and Milestones
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0
(function () {
    'use strict';

    /**
     * Represents a single milestone on a timeline.
     * Mirrors Java: Milestone
     *
     * @param {Date}    time      Milestone date
     * @param {string}  symbol    Short symbol (e.g., "N", "S", "E")
     * @param {string}  name      Descriptive name
     * @param {boolean} [nowLine] Optional: whether this is the "now" line
     */
    class Milestone {
        constructor(time, symbol, name, nowLine = false) {
            this.time    = time;       // Date object
            this.symbol  = symbol;     // String
            this.name    = name;       // String
            this.nowLine = nowLine;    // boolean
            this.hidden  = false;      // boolean (set by add())
        }

        /**
         * Comparison for sorting.
         * Mirrors Java: compareTo()
         */
        compareTo(other) {
            if (this.time < other.time) return -1;
            if (this.time > other.time) return 1;
            return 0;
        }
    }

    /**
     * Container for a list of milestones.
     * Mirrors Java: Milestones
     *
     * @param {Array<Milestone>} [milestonesList] Optional initial list
     * @param {Date}             [firstDate]      Optional first milestone date
     * @param {Date}             [lastDate]       Optional last milestone date
     */
    class Milestones {
        constructor(milestonesList = [], firstDate = null, lastDate = null) {
            this.list = milestonesList || [];
            this.map  = {};

            // Build map from symbol -> milestone
            this.list.forEach(m => {
                if (m && m.symbol) {
                    this.map[m.symbol] = m;
                }
            });

            this.firstMilestone = firstDate || (this.list.length > 0 ? this.list[0].time : null);
            this.lastMilestone  = lastDate  || (this.list.length > 0 ? this.list[this.list.length - 1].time : null);
        }

        /**
         * Adds a new milestone.
         * Mirrors Java: add(LocalDate time, String symbol, String name, Color color, boolean hidden)
         */
        add(time, symbol, name, hidden = false) {
            if (time !== null && time !== undefined) {
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
        addMilestone(m) {
            if (m) {
                this.list.push(m);
                if (m.symbol) this.map[m.symbol] = m;
            }
        }

        /**
         * Sorts milestones and sets first/last.
         * Mirrors Java: calculate()
         */
        calculate() {
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
        getList() {
            return this.list;
        }

        /**
         * Gets a milestone by symbol.
         * Mirrors Java: get(String name)
         */
        get(symbol) {
            return this.map[symbol] || null;
        }

        /**
         * Clears all milestones.
         * Mirrors Java: clear()
         */
        clear() {
            this.list = [];
            this.map  = {};
        }

        /**
         * Checks if all milestones are hidden.
         * Mirrors Java: empty()
         */
        empty() {
            for (let m of this.list) {
                if (!m.hidden) return false;
            }
            return true;
        }

        /**
         * Removes a milestone by symbol.
         * Mirrors Java: remove(String name)
         */
        remove(symbol) {
            for (let i = 0; i < this.list.length; i++) {
                if (this.list[i].symbol === symbol) {
                    this.list.splice(i, 1);
                    delete this.map[symbol];
                    return;
                }
            }
        }
    }

    // ── Exports ────────────────────────────────────────────────────────────────

    window.Milestone  = Milestone;
    window.Milestones = Milestones;

})();

