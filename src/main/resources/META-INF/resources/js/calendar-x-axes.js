// CalendarXAxes.js
// Virtual-canvas calendar header renderer.
// Mirrors Java: CalendarXAxes, CalendarSize.YEARS, calendarAtBottom=false
// Row order (top→bottom): year → month → [week] → [dayOfMonth → dayOfWeek]
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0
(function () {
    'use strict';

    // Row heights (Java CalendarElement heights: fontSize + margine=4)
    const YEAR_H = 17;   // 13+4 → rect bh-1 = 16px visible
    const MONTH_H = 16;   // 12+4 → rect bh-1 = 15px visible
    const WEEK_H = 14;   // 10+4 → rect bh-1 = 13px visible
    const DOM_H = 14;   // 10+4  day-of-month
    const DOW_H = 14;   // 10+4  day-of-week

    // Visibility thresholds (Java CalendarXAxes constants)
    const MIN_WEEK = 2;   // WEEK_MIN_DAY_WIDTH
    const MIN_DOW = 10;  // DAY_OF_WEEK_MIN_DAY_WIDTH  – show DOW row only
    const MIN_DOM = 16;  // DAY_OF_MONTH_MIN_DAY_WIDTH – show DOM row above DOW

    const MS = 86400000;
    const SVG_NS = 'http://www.w3.org/2000/svg';

    // ── SVG helpers ──────────────────────────────────────────────────────────
    function svgEl(tag, attrs, textContent) {
        const e = document.createElementNS(SVG_NS, tag);
        if (attrs) {
            for (const k of Object.keys(attrs)) {
                e.setAttribute(k, String(attrs[k]));
            }
        }
        if (textContent != null) e.textContent = textContent;
        return e;
    }

    function r(x, y, w, h, a) {
        // crispEdges on all rects so calendar row borders are pixel-sharp (no AA)
        // return svgEl('rect', Object.assign({x, y, width: Math.max(0, w), height: Math.max(0, h), 'shape-rendering': 'crispEdges'}, a));
        return svgEl('rect', Object.assign({x, y, width: Math.max(0, w), height: Math.max(0, h)}, a));
    }

    function t(x, y, content, a) {
        return svgEl('text', Object.assign({x, y}, a), content);
    }

    function mkClip(id, x, y, w, h) {
        const defs = svgEl('defs');
        const cp = svgEl('clipPath', {id});
        cp.appendChild(r(x, y, w, h, {}));
        defs.appendChild(cp);
        return defs;
    }

    // ── Color helpers ────────────────────────────────────────────────────────
    function numToHex(v) {
        if (v == null) return null;
        if (typeof v === 'string') return v;
        if (typeof v === 'number') return '#' + (v >>> 0).toString(16).padStart(6, '0').slice(-6);
        return null;
    }

    function tc(theme, key, fallback) {
        return (theme && theme[key] != null) ? (numToHex(theme[key]) || fallback) : fallback;
    }

    function utcDay(d) {
        const dt = (typeof d === 'string') ? new Date(d) : d;
        return new Date(Date.UTC(dt.getFullYear(), dt.getMonth(), dt.getDate()));
    }

    function dayIdxOf(date, chartStart) {
        return Math.round((utcDay(date).getTime() - chartStart.getTime()) / MS);
    }

    // ── CalendarXAxes class ──────────────────────────────────────────────────
    class CalendarXAxes {
        constructor(theme) {
            this.theme = theme || {};
        }

        /** Pixel height of the calendar header for the given day width. */
        getHeight(dayWidth) {
            let h = YEAR_H + MONTH_H;
            if (dayWidth >= MIN_WEEK) h += WEEK_H;
            if (dayWidth >= MIN_DOM) h += DOM_H + DOW_H;  // both DOM and DOW rows
            else if (dayWidth >= MIN_DOW) h += DOW_H;          // DOW row only, no DOM
            return h;
        }

        /**
         * Append the calendar header <g> element to the given SVG.
         *
         * @param {SVGSVGElement} svg        Target SVG element
         * @param {Date}         chartStart  UTC midnight of first chart day
         * @param {number}       totalDays   Total days in chart
         * @param {number}       dayWidth    Pixels per day (may be fractional)
         * @param {number}       scroll      First visible day index (fractional)
         * @param {number}       W           Container width in pixels
         * @returns {number} Actual header height drawn (same as getHeight(dayWidth))
         */
        draw(svg, chartStart, totalDays, dayWidth, scroll, W) {
            const theme = this.theme;
            const g = svgEl('g', {'class': 'calendar-header'});
            svg.appendChild(g);

            // ── Resolve theme colors (Java LightTheme defaults as fallback) ───
            const yBg = tc(theme, 'yearBgColor', '#ababab');
            const yFg = tc(theme, 'yearTextColor', '#ffffff');
            const yBd = tc(theme, 'yearBoderColor', '#ffffff');
            const mFg = tc(theme, 'monthTextColor', '#ffffff');
            const mBd = tc(theme, 'monthBorderColor', '#ffffff');
            const wBg = tc(theme, 'weekBgColor', '#ababab');
            const wFg = tc(theme, 'weekTextColor', '#ffffff');
            const wBd = tc(theme, 'weekBoderColor', '#ffffff');
            const dmBg = tc(theme, 'dayOfMonthBgColor', '#ababab');
            const dmBd = tc(theme, 'dayOfMonthBorderColor', '#ffffff');
            const dmFg = tc(theme, 'dayOfMonthTextColor', '#ffffff');
            const dmWB = tc(theme, 'dayOfMonthWeekendBgColor', '#d7d7d7');
            const dmWF = tc(theme, 'dayOfMonthWeekendTextColor', '#000000');
            const dwBg = tc(theme, 'dayOfweekBgColor', '#ffffff');
            const dwBd = tc(theme, 'dayOfWeekBorderColor', '#ffffff');
            const dwFg = tc(theme, 'dayOfWeekTextColor', '#000000');
            const dwSa = tc(theme, 'dayOfweekSaturdayBgColor', '#d7d7d7');
            const dwSu = tc(theme, 'dayOfweekSundayBgColor', '#d7d7d7');
            const dwWF = tc(theme, 'dayOfWeekWeekendTextColor', '#000000');

            // Java XAxesTheme.monthBgColors defaults (from XAxesTheme constructor)
            const MONTH_BG = [
                '#187dc3', '#24aeef', '#279e68', '#62b742', '#acc231', '#f9b71b',
                '#f1751d', '#e54629', '#e71657', '#ad3483', '#654198', '#0855a3'
            ];
            const MONTH_NAMES = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
            const DAY_NAMES = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];

            // X position of the left edge of day [di] in the current viewport
            const dX = di => (di - scroll) * dayWidth;

            const chartEnd = new Date(chartStart.getTime() + (totalDays - 1) * MS);

            let y = 0;
            let ci = 0;  // unique clip-path ID counter

            /**
             * Mirrors Java CalendarXAxes.drawTextBox().
             * Fill rect height = bh-1, y cursor advances by bh → 1px gap between rows.
             * No stroke on fill rect (stroke was eating 1px from the visible fill area).
             * Right-border: explicit 1px vertical line at x=bx+bw.
             * centered=false → text at bx+2 (left-aligned, Java: x1+1+1)
             * centered=true  → text centered horizontally
             */
            const box = (bx, bw, by, bh, bg, bd, label, fg, fontSize, centered) => {
                if (bx + bw <= 0 || bx >= W) return;
                // Fill only (no stroke) – avoids the stroke eating into the visible area
                g.appendChild(r(bx, by, bw, bh - 1, {fill: bg}));
                // Right-side vertical separator (Java: fillRect(x2, y1, 1, h-1))
                if (bd && bw > 1) {
                    g.appendChild(svgEl('line', {
                        x1: bx + bw, y1: by, x2: bx + bw, y2: by + bh - 1,
                        stroke: bd, 'stroke-width': '1'
                    }));
                }
                if (!label) return;
                const cid = 'cc' + (ci++);
                g.appendChild(mkClip(cid, bx, by, bw, bh));
                const tx = centered ? bx + bw / 2 : Math.max(bx + 2, 2);
                // Java: y = y1 + height/2 + (maxAscent+1)/2 - 2  ≈  y1 + height - 4
                g.appendChild(t(tx, by + bh - 4, label, {
                    fill: fg,
                    'font-size': fontSize,
                    'font-family': 'sans-serif',
                    'text-anchor': centered ? 'middle' : 'start',
                    'clip-path': 'url(#' + cid + ')'
                }));
            };

            // ── YEAR ROW (always visible) ─────────────────────────────────────
            // No full-width background rect: individual year blocks start at x=0 (clamped)
            // and cover the full viewport. The 1px gap at y=YEAR_H-1 stays transparent.
            for (let yr = chartStart.getFullYear(); yr <= chartEnd.getFullYear(); yr++) {
                const si = dayIdxOf(new Date(yr, 0, 1), chartStart);
                const ei = dayIdxOf(new Date(yr, 11, 31), chartStart);
                const bx = dX(Math.max(0, si));
                const bw = dX(Math.min(totalDays - 1, ei) + 1) - bx;
                box(bx, bw, y, YEAR_H, yBg, yBd, String(yr), yFg, '11', false);
            }
            y += YEAR_H;

            // ── MONTH ROW (always visible) ────────────────────────────────────
            let md = new Date(Date.UTC(chartStart.getFullYear(), chartStart.getMonth(), 1));
            while (md <= chartEnd) {
                const mo = md.getUTCMonth();
                const yr2 = md.getUTCFullYear();
                const me = new Date(Date.UTC(yr2, mo + 1, 0));
                const si = dayIdxOf(md, chartStart);
                const ei = dayIdxOf(me, chartStart);
                const bx = dX(Math.max(0, si));
                const bw = dX(Math.min(totalDays - 1, ei) + 1) - bx;
                const bg = tc(theme, 'monthBgColors.' + mo, MONTH_BG[mo]);
                box(bx, bw, y, MONTH_H, bg, mBd, MONTH_NAMES[mo], mFg, '11', false);
                md = new Date(Date.UTC(yr2, mo + 1, 1));
            }
            y += MONTH_H;

            // ── WEEK ROW ─────────────────────────────────────────────────────
            if (dayWidth >= MIN_WEEK) {
                // No full-width background rect: week blocks cover the full width.
                let ws = new Date(chartStart);
                while (ws.getUTCDay() !== 1) ws.setUTCDate(ws.getUTCDate() - 1);
                while (ws <= chartEnd) {
                    const we = new Date(ws);
                    we.setUTCDate(we.getUTCDate() + 6);
                    const si = dayIdxOf(ws, chartStart);
                    const ei = dayIdxOf(we, chartStart);
                    const bx = dX(Math.max(0, si));
                    const bw = dX(Math.min(totalDays - 1, ei) + 1) - bx;
                    // Label: day-of-month of first visible day in the week
                    const label = dayWidth >= MIN_DOW
                        ? String(new Date(chartStart.getTime() + Math.max(0, si) * MS).getUTCDate())
                        : null;
                    box(bx, bw, y, WEEK_H, wBg, wBd, label, wFg, '9', false);
                    ws.setUTCDate(ws.getUTCDate() + 7);
                }
                y += WEEK_H;
            }

            // ── DAY-OF-MONTH row (visible when dayWidth >= MIN_DOM = 16) ─────
            if (dayWidth >= MIN_DOM) {
                const fv = Math.max(0, Math.floor(scroll) - 1);
                const lv = Math.min(totalDays - 1, fv + Math.ceil(W / dayWidth) + 2);
                for (let i = fv; i <= lv; i++) {
                    const dt = new Date(chartStart.getTime() + i * MS);
                    const dw = dt.getUTCDay();
                    const wk = dw === 0 || dw === 6;
                    box(dX(i), dayWidth, y, DOM_H, wk ? dmWB : dmBg, dmBd, String(dt.getUTCDate()), wk ? dmWF : dmFg, '9', true);
                }
                y += DOM_H;
            }

            // ── DAY-OF-WEEK row (visible when dayWidth >= MIN_DOW = 10) ──────
            if (dayWidth >= MIN_DOW) {
                const fv = Math.max(0, Math.floor(scroll) - 1);
                const lv = Math.min(totalDays - 1, fv + Math.ceil(W / dayWidth) + 2);
                for (let i = fv; i <= lv; i++) {
                    const dt = new Date(chartStart.getTime() + i * MS);
                    const dw = dt.getUTCDay();
                    const wk = dw === 0 || dw === 6;
                    const dwB = dw === 6 ? dwSa : (dw === 0 ? dwSu : dwBg);
                    box(dX(i), dayWidth, y, DOW_H, dwB, dwBd, DAY_NAMES[dw], wk ? dwWF : dwFg, '9', true);
                }
                y += DOW_H;
            }

            return y; // actual height used
        }
    }

    // Export
    window.CalendarXAxes = CalendarXAxes;
})();
/*
 * Copyright (C) 2025-2026 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0
 */
