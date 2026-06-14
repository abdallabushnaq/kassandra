// CalendarXAxes.js
// Virtual-canvas calendar header renderer.
// Mirrors Java: CalendarXAxes, CalendarSize.YEARS, calendarAtBottom=false
// Row order (top→bottom): year → month → [week] → [dayOfMonth → dayOfWeek]
// Depends on: svg-utils.js, color-utils.js, date-utils.js, theme-color-constants.js
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0
(function () {
    'use strict';

    // Destructure utilities from global scope
    const {createSvgElement, createRect, createText, createClipPath} = window.SvgUtils;
    const {getThemeColor} = window.ColorUtils;
    const {MS, getUtcDayMidnight, calculateDayIndex} = window.DateUtils;
    const colorKeys = window.ThemeColorKeys;

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


    // ── CalendarXAxes class ──────────────────────────────────────────────────

    /**
     * Renders a virtual-canvas calendar header for a Gantt chart.
     * Hierarchical rows: year → month → week (optional) → day-of-month/day-of-week (optional).
     * Visibility and styling are controlled by dayWidth thresholds and theme colors.
     */
    class CalendarXAxes {
        /**
         * Initializes the calendar renderer with an optional theme.
         * @param {Object} theme Optional theme configuration object with color overrides
         */
        constructor(theme) {
            this.theme = theme || {};
        }

        /**
         * Calculates the total pixel height of the calendar header for a given day width.
         * Row visibility is determined by dayWidth thresholds:
         * - Year and Month rows are always visible.
         * - Week row appears when dayWidth >= MIN_WEEK (2px).
         * - Day-of-Month and Day-of-Week rows appear when dayWidth >= MIN_DOM (16px).
         * - Day-of-Week row alone appears when dayWidth >= MIN_DOW (10px).
         *
         * @param {number} dayWidth Pixel width per day (may be fractional)
         * @returns {number} Total header height in pixels
         */
        getHeight(dayWidth) {
            let height = YEAR_H + MONTH_H;
            if (dayWidth >= MIN_WEEK) height += WEEK_H;
            if (dayWidth >= MIN_DOM) height += DOM_H + DOW_H;  // both DOM and DOW rows
            else if (dayWidth >= MIN_DOW) height += DOW_H;          // DOW row only, no DOM
            return height;
        }

        /**
         * Draws the calendar header into the given SVG element.
         * Renders year, month, week (optional), day-of-month (optional), and day-of-week (optional) rows.
         * Each row is clipped to the visible viewport and includes text labels.
         * Text is centered or left-aligned within cells based on the row type.
         *
         * @param {SVGSVGElement} svg Target SVG element to append the header group to
         * @param {Date} chartStart UTC midnight of the first day of the chart
         * @param {number} totalDays Total number of days in the chart
         * @param {number} dayWidth Pixel width per day (may be fractional)
         * @param {number} scrollOffset First visible day index (zero-based, may be fractional)
         * @param {number} viewportWidth Container width in pixels
         * @returns {number} Total header height drawn (same as getHeight(dayWidth))
         */
        draw(svg, chartStart, totalDays, dayWidth, scrollOffset, viewportWidth) {
            const theme = this.theme;
            const headerGroup = createSvgElement('g', {'class': 'calendar-header'}, undefined);
            svg.appendChild(headerGroup);

            // ── Resolve theme colors (Java LightTheme defaults as fallback) ───
            const yearBgColor = getThemeColor(theme, colorKeys.XAXES_YEAR_BG_COLOR);
            const yearTextColor = getThemeColor(theme, colorKeys.XAXES_YEAR_TEXT_COLOR);
            const yearBorderColor = getThemeColor(theme, colorKeys.XAXES_YEAR_BORDER_COLOR);
            const monthTextColor = getThemeColor(theme, colorKeys.XAXES_MONTH_TEXT_COLOR);
            const monthBorderColor = getThemeColor(theme, colorKeys.XAXES_MONTH_BORDER_COLOR);
            const weekBgColor = getThemeColor(theme, colorKeys.XAXES_WEEK_BG_COLOR);
            const weekTextColor = getThemeColor(theme, colorKeys.XAXES_WEEK_TEXT_COLOR);
            const weekBorderColor = getThemeColor(theme, colorKeys.XAXES_WEEK_BORDER_COLOR);
            const dayOfMonthBgColor = getThemeColor(theme, colorKeys.XAXES_DAY_OF_MONTH_BG_COLOR);
            const dayOfMonthBorderColor = getThemeColor(theme, colorKeys.XAXES_DAY_OF_MONTH_BORDER_COLOR);
            const dayOfMonthTextColor = getThemeColor(theme, colorKeys.XAXES_DAY_OF_MONTH_TEXT_COLOR);
            const dayOfMonthWeekendBgColor = getThemeColor(theme, colorKeys.XAXES_DAY_OF_MONTH_WEEKEND_BG_COLOR);
            const dayOfMonthWeekendTextColor = getThemeColor(theme, colorKeys.XAXES_DAY_OF_MONTH_WEEKEND_TEXT_COLOR);
            const dayOfWeekBgColor = getThemeColor(theme, colorKeys.XAXES_DAY_OF_WEEK_BG_COLOR);
            const dayOfWeekBorderColor = getThemeColor(theme, colorKeys.XAXES_DAY_OF_WEEK_BORDER_COLOR);
            const dayOfWeekTextColor = getThemeColor(theme, colorKeys.XAXES_DAY_OF_WEEK_TEXT_COLOR);
            const dayOfWeekSaturdayBgColor = getThemeColor(theme, colorKeys.XAXES_DAY_OF_WEEK_SATURDAY_BG_COLOR);
            const dayOfWeekSundayBgColor = getThemeColor(theme, colorKeys.XAXES_DAY_OF_WEEK_SUNDAY_BG_COLOR);
            const dayOfWeekWeekendTextColor = getThemeColor(theme, colorKeys.XAXES_DAY_OF_WEEK_WEEKEND_TEXT_COLOR);

            // Java XAxesTheme.monthBgColors defaults (from XAxesTheme constructor)
            // const MONTH_BACKGROUNDS = [
            //     '#187dc3', '#24aeef', '#279e68', '#62b742', '#acc231', '#f9b71b',
            //     '#f1751d', '#e54629', '#e71657', '#ad3483', '#654198', '#0855a3'
            // ];
            const MONTH_NAMES = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
            const DAY_ABBREVIATIONS = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];

            // Helper: Convert day index to pixel x-position within viewport
            const dayIndexToPixelX = (dayIndex) => (dayIndex - scrollOffset) * dayWidth;

            const chartEnd = new Date(chartStart.getTime() + (totalDays - 1) * MS);

            let currentY = 0;
            let clipPathIdCounter = 0;  // Unique clip-path ID generator

            /**
             * Draws a calendar cell (text box with background and border).
             * Mirrors Java CalendarXAxes.drawTextBox().
             * - Fill rect height = bh-1; y cursor advances by bh → 1px gap between rows
             * - No stroke on fill rect (stroke would eat into visible fill area)
             * - Right-border: explicit 1px vertical line at x=cellX+cellWidth
             * - Text alignment: left-aligned at cellX+2  (if not centered)
             *                   or centered horizontally (if centered)
             *
             * @param {number} cellX X position of cell left edge
             * @param {number} cellWidth Width of cell
             * @param {number} cellY Y position of cell top edge
             * @param {number} cellHeight Height of cell
             * @param {string} bgColor Background fill color
             * @param {string} borderColor Right-border color
             * @param {string} labelText Text label (omitted if empty/null)
             * @param {string} textColor Text color
             * @param {string} fontSize Font size (CSS value)
             * @param {boolean} centered If true, center text horizontally; else left-align
             */
            const drawCalendarCell = (cellX, cellWidth, cellY, cellHeight, bgColor, borderColor, labelText, textColor, fontSize, centered) => {
                // Skip cells entirely outside the viewport
                if (cellX + cellWidth <= 0 || cellX >= viewportWidth) return;

                // Draw background fill (no stroke to avoid clipping)
                headerGroup.appendChild(createRect(cellX, cellY, cellWidth, cellHeight - 1, {fill: bgColor}));

                // Right-side vertical separator (1px line)
                if (borderColor && cellWidth > 1) {
                    headerGroup.appendChild(createSvgElement('line', {
                        x1: cellX + cellWidth, y1: cellY, x2: cellX + cellWidth, y2: cellY + cellHeight - 1,
                        stroke: borderColor, 'stroke-width': '1'
                    }, undefined));
                }

                if (!labelText) return;

                // Create clip path to prevent text overflow
                const clipPathId = 'cc' + (clipPathIdCounter++);
                headerGroup.appendChild(createClipPath(clipPathId, cellX, cellY, cellWidth, cellHeight));

                // Calculate text x position
                const textX = centered ? cellX + cellWidth / 2 : Math.max(cellX + 2, 2);

                // Java: y = y1 + height/2 + (maxAscent+1)/2 - 2  ≈  y1 + height - 4
                headerGroup.appendChild(createText(textX, cellY + cellHeight - 4, labelText, {
                    fill: textColor,
                    'font-size': fontSize,
                    'font-family': 'sans-serif',
                    'text-anchor': centered ? 'middle' : 'start',
                    'clip-path': 'url(#' + clipPathId + ')'
                }));
            };

            // ── YEAR ROW (always visible) ─────────────────────────────────────
            for (let year = chartStart.getFullYear(); year <= chartEnd.getFullYear(); year++) {
                const startIndex = calculateDayIndex(new Date(year, 0, 1), chartStart);
                const endIndex = calculateDayIndex(new Date(year, 11, 31), chartStart);
                const cellX = dayIndexToPixelX(Math.max(0, startIndex));
                const cellWidth = dayIndexToPixelX(Math.min(totalDays - 1, endIndex) + 1) - cellX;
                drawCalendarCell(cellX, cellWidth, currentY, YEAR_H, yearBgColor, yearBorderColor, String(year), yearTextColor, '14', false);
            }
            currentY += YEAR_H;

            // ── MONTH ROW (always visible) ────────────────────────────────────
            let monthDate = new Date(Date.UTC(chartStart.getFullYear(), chartStart.getMonth(), 1));
            while (monthDate <= chartEnd) {
                const monthIndex = monthDate.getUTCMonth();
                const year = monthDate.getUTCFullYear();
                const monthEnd = new Date(Date.UTC(year, monthIndex + 1, 0));
                const startIndex = calculateDayIndex(monthDate, chartStart);
                const endIndex = calculateDayIndex(monthEnd, chartStart);
                const cellX = dayIndexToPixelX(Math.max(0, startIndex));
                const cellWidth = dayIndexToPixelX(Math.min(totalDays - 1, endIndex) + 1) - cellX;
                const monthBgColor = getThemeColor(theme, colorKeys.XAXES_MONTH_BG_COLORS_PREFIX + monthIndex);
                drawCalendarCell(cellX, cellWidth, currentY, MONTH_H, monthBgColor, monthBorderColor, MONTH_NAMES[monthIndex], monthTextColor, '12', false);
                monthDate = new Date(Date.UTC(year, monthIndex + 1, 1));
            }
            currentY += MONTH_H;

            // ── WEEK ROW ─────────────────────────────────────────────────────
            // Visible when dayWidth >= MIN_WEEK. Each week spans Monday to Sunday.
            if (dayWidth >= MIN_WEEK) {
                let weekStart = new Date(chartStart);
                // Back up to the start of the week (Monday)
                while (weekStart.getUTCDay() !== 1) weekStart.setUTCDate(weekStart.getUTCDate() - 1);

                while (weekStart <= chartEnd) {
                    const weekEnd = new Date(weekStart);
                    weekEnd.setUTCDate(weekEnd.getUTCDate() + 6);
                    const startIndex = calculateDayIndex(weekStart, chartStart);
                    const endIndex = calculateDayIndex(weekEnd, chartStart);
                    const cellX = dayIndexToPixelX(Math.max(0, startIndex));
                    const cellWidth = dayIndexToPixelX(Math.min(totalDays - 1, endIndex) + 1) - cellX;

                    // Label: show first visible day of week (day-of-month) when dayWidth >= MIN_DOW
                    const weekLabel = dayWidth >= MIN_DOW
                        ? String(new Date(chartStart.getTime() + Math.max(0, startIndex) * MS).getUTCDate())
                        : null;
                    drawCalendarCell(cellX, cellWidth, currentY, WEEK_H, weekBgColor, weekBorderColor, weekLabel, weekTextColor, '10', false);
                    weekStart.setUTCDate(weekStart.getUTCDate() + 7);
                }
                currentY += WEEK_H;
            }

            // ── DAY-OF-MONTH row (visible when dayWidth >= MIN_DOM = 16) ─────
            // Shows calendar day numbers with weekend highlighting.
            if (dayWidth >= MIN_DOM) {
                const firstVisibleDay = Math.max(0, Math.floor(scrollOffset) - 1);
                const lastVisibleDay = Math.min(totalDays - 1, firstVisibleDay + Math.ceil(viewportWidth / dayWidth) + 2);
                for (let dayIdx = firstVisibleDay; dayIdx <= lastVisibleDay; dayIdx++) {
                    const dayDate = new Date(chartStart.getTime() + dayIdx * MS);
                    const dayOfWeek = dayDate.getUTCDay();
                    const isWeekend = dayOfWeek === 0 || dayOfWeek === 6;
                    drawCalendarCell(
                        dayIndexToPixelX(dayIdx), dayWidth, currentY, DOM_H,
                        isWeekend ? dayOfMonthWeekendBgColor : dayOfMonthBgColor,
                        dayOfMonthBorderColor,
                        String(dayDate.getUTCDate()),
                        isWeekend ? dayOfMonthWeekendTextColor : dayOfMonthTextColor,
                        '10', true
                    );
                }
                currentY += DOM_H;
            }

            // ── DAY-OF-WEEK row (visible when dayWidth >= MIN_DOW = 10) ──────
            // Shows day abbreviations (S, M, T, W, T, F, S) with weekend highlighting.
            if (dayWidth >= MIN_DOW) {
                const firstVisibleDay = Math.max(0, Math.floor(scrollOffset) - 1);
                const lastVisibleDay = Math.min(totalDays - 1, firstVisibleDay + Math.ceil(viewportWidth / dayWidth) + 2);
                for (let dayIdx = firstVisibleDay; dayIdx <= lastVisibleDay; dayIdx++) {
                    const dayDate = new Date(chartStart.getTime() + dayIdx * MS);
                    const dayOfWeek = dayDate.getUTCDay();
                    const isWeekend = dayOfWeek === 0 || dayOfWeek === 6;
                    const dayOfWeekBg = dayOfWeek === 6 ? dayOfWeekSaturdayBgColor : (dayOfWeek === 0 ? dayOfWeekSundayBgColor : dayOfWeekBgColor);
                    drawCalendarCell(
                        dayIndexToPixelX(dayIdx), dayWidth, currentY, DOW_H,
                        dayOfWeekBg,
                        dayOfWeekBorderColor,
                        DAY_ABBREVIATIONS[dayOfWeek],
                        isWeekend ? dayOfWeekWeekendTextColor : dayOfWeekTextColor,
                        '10', true
                    );
                }
                currentY += DOW_H;
            }

            return currentY; // Actual header height drawn
        }
    }

    // Export
    window.CalendarXAxes = CalendarXAxes;
})();
