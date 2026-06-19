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
    const MILESTONE_H = 13;  // 10+4  milestone row
    const FLAG_HEIGHT = 13;
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
         * - Milestone row is always visible if milestones are present.
         *
         * @param {number} dayWidth Pixel width per day (may be fractional)
         * @param {boolean} hasMilestones Whether there are project milestones to render
         * @returns {number} Total header height in pixels
         */
        getHeight(dayWidth, hasMilestones) {
            let height = YEAR_H + MONTH_H;
            if (dayWidth >= MIN_WEEK) height += WEEK_H;
            if (dayWidth >= MIN_DOM) height += DOM_H + DOW_H;  // both DOM and DOW rows
            else if (dayWidth >= MIN_DOW) height += DOW_H;          // DOW row only, no DOM
            if (hasMilestones) height += MILESTONE_H;  // milestone row always added if milestones present
            return height;
        }

        /**
         * Draws the calendar header into the given SVG element.
         * Renders year, month, week (optional), day-of-month (optional), day-of-week (optional),
         * and milestone (optional) rows.
         * Each row is clipped to the visible viewport and includes text labels.
         * Text is centered or left-aligned within cells based on the row type.
         *
         * @param {SVGSVGElement} svg Target SVG element to append the header group to
         * @param {Date} chartStart UTC midnight of the first day of the chart
         * @param {number} totalDays Total number of days in the chart
         * @param {number} dayWidth Pixel width per day (may be fractional)
         * @param {number} scrollOffset First visible day index (zero-based, may be fractional)
         * @param {number} viewportWidth Container width in pixels
         * @param {Array} [milestones] Optional array of {date, letter, label} project milestones
         * @returns {number} Total header height drawn (same as getHeight(dayWidth, hasMilestones))
         */
        draw(svg, chartStart, totalDays, dayWidth, scrollOffset, viewportWidth, milestones) {
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
                // currentY += DOW_H;
            }

            // ── MILESTONE row (visible if milestones are present) ──────────
            // Shows project milestones: S (start), E (end), N (now) in a dedicated row
            if (milestones && milestones.length > 0) {
                const milestoneTextColor = getThemeColor(theme, colorKeys.XAXES_MILESTONE_TEXT_COLOR);
                const milestoneBgColor = getThemeColor(theme, colorKeys.GANTT_REQUEST_MILESTONE_COLOR);
                const milestoneFlagColor = getThemeColor(theme, colorKeys.XAXES_MILESTONE_FLAG_COLOR);
                const milestoneFlagTextColor = getThemeColor(theme, colorKeys.XAXES_FUTURE_EVENT_COLOR);

                // Build a map of day index to milestone letter for quick lookup
                const milestonesByDayIdx = {};
                for (let i = 0; i < milestones.length; i++) {
                    const milestone = milestones[i];
                    // Parse milestone date and convert to day index
                    const milestoneDate = new Date(milestone.date);
                    const dayIdx = calculateDayIndex(milestoneDate, chartStart);
                    if (dayIdx >= 0 && dayIdx < totalDays) {
                        milestonesByDayIdx[dayIdx] = milestone;
                    }
                }

                // Draw milestone row for visible days
                const firstVisibleDay = Math.max(0, Math.floor(scrollOffset) - 1);
                const lastVisibleDay = Math.min(totalDays - 1, firstVisibleDay + Math.ceil(viewportWidth / dayWidth) + 2);
                for (let dayIdx = firstVisibleDay; dayIdx <= lastVisibleDay; dayIdx++) {
                    const milestone = milestonesByDayIdx[dayIdx];
                    const milestoneLabel = milestone ? milestone.letter : '';  // S, E, N or empty

                    if (milestone) {
                        const milestoneX = dayIndexToPixelX(dayIdx) + dayWidth / 2 + 1;
                        this.drawMilestone(
                            milestone,
                            new Date(milestone.date),
                            milestoneX,
                            currentY,
                            milestoneBgColor,
                            milestoneLabel,
                            true,  // drawMilestone
                            currentY + DOW_H,  // flagY (no flag in calendar row)
                            milestoneFlagTextColor,
                            true, // drawFlag
                            true, // drawNowLine
                            headerGroup,
                            0,     // diagramY (not used in calendar row)
                            0,     // diagramHeight (not used in calendar row)
                            true  // calendarAtBottom (not used in calendar row)
                        );
                    }
                }
                currentY += MILESTONE_H;
            }

            return currentY; // Actual header height drawn
        }

        /**
         * Draws a single milestone on the chart.
         * Mirrors Java CalendarXAxes.drawMilestone().
         * Handles regular milestones, now lines, and optional flags with date labels.
         *
         * @param {Object} m The milestone object (may be null for "now" lines)
         * @param {Date} time The date of the milestone
         * @param {number} x The x-position (center) of the milestone
         * @param {number} y The y-position of the milestone rectangle
         * @param {string} fillColor Background color for milestone box (e.g., #RRGGBB)
         * @param {string} text The milestone label/symbol (e.g., "S", "E", "N")
         * @param {boolean} drawMilestone Whether to draw the milestone box and text
         * @param {number|null} flagY The y-position of the flag (top-left); null means no flag
         * @param {string} flagTextColor Color for flag pole and text
         * @param {boolean} drawFlag Whether to draw the date flag below the milestone
         * @param {boolean} drawNowLine Whether to draw the "now" vertical line (for "N" text)
         * @param {SVGElement} parentGroup The SVG group element to append drawn elements to
         * @param {number} diagramY Top of the diagram area (for now line)
         * @param {number} diagramHeight Height of the diagram area (for now line)
         * @param {boolean} calendarAtBottom Whether calendar is at bottom (affects now line circle position)
         */
        drawMilestone(m, time, x, y, fillColor, text, drawMilestone, flagY, flagTextColor, drawFlag, drawNowLine, parentGroup, diagramY, diagramHeight, calendarAtBottom) {
            const centerX = x;
            const centerY = y;
            const MILESTONE_WIDTH = 11;
            const MILESTONE_HEIGHT = 13;
            const FLAG_HEIGHT = 13;

            // Use theme colors (mirroring Java CalendarXAxes.drawMilestone)
            const darkRed = '#8B0000';  // ColorConstants.COLOR_DARK_RED
            const milestoneTextColor = getThemeColor(this.theme, colorKeys.XAXES_MILESTONE_TEXT_COLOR);
            const milestoneFlagColor = getThemeColor(this.theme, colorKeys.XAXES_MILESTONE_FLAG_COLOR);

            // Handle "now" line (text starts with "N")
            if (text && text.startsWith('N')) {
                if (drawNowLine) {
                    // Draw vertical line
                    parentGroup.appendChild(createSvgElement('line', {
                        x1: x, y1: diagramY,
                        x2: x, y2: diagramY + diagramHeight,
                        stroke: darkRed,
                        'stroke-width': '2'
                    }, undefined));

                    // Draw circle at top or bottom
                    const circleRadius = Math.max(Math.floor(10 / 3), 3); // Math.max(dayWidth/3, 6) adapted
                    if (calendarAtBottom) {
                        parentGroup.appendChild(createSvgElement('circle', {
                            cx: x + 1,
                            cy: diagramY - circleRadius / 2,
                            r: circleRadius,
                            fill: darkRed
                        }, undefined));
                    } else {
                        parentGroup.appendChild(createSvgElement('circle', {
                            cx: x + 1,
                            cy: diagramY + diagramHeight - circleRadius,
                            r: circleRadius,
                            fill: darkRed
                        }, undefined));
                    }
                }
            }

            // Draw milestone box and text
            if (drawMilestone) {
                // Draw milestone rectangle
                parentGroup.appendChild(createRect(
                    centerX - MILESTONE_WIDTH / 2,
                    centerY,
                    MILESTONE_WIDTH,
                    MILESTONE_HEIGHT - 1,
                    {fill: fillColor}
                ));

                // Draw milestone text
                const textX = centerX - 1;
                const textY = centerY + MILESTONE_HEIGHT / 2 + 1;
                const textElement = createText(textX, textY, text, {
                    fill: milestoneTextColor,
                    'font-size': '10px',
                    'font-family': 'sans-serif',
                    'font-weight': 'bold',
                    'text-anchor': 'middle',
                    'dominant-baseline': 'middle'
                });
                parentGroup.appendChild(textElement);

                // Add tooltip if milestone object is present
                if (m) {
                    const titleEl = createSvgElement('title', {},
                        text + ' = ' + m.name + '\n' + this._formatDateForTooltip(time));
                    textElement.appendChild(titleEl);
                }

                // Draw flag (date label) if requested
                if (drawFlag && flagY !== null) {
                    const flagLabel = this._formatDateForFlag(time);
                    const flagWidth = Math.max(flagLabel.length * 5, 30); // Rough estimate: ~5px per char

                    // Flag background rectangle
                    // parentGroup.appendChild(createRect(
                    //     x - MILESTONE_WIDTH / 2,
                    //     flagY,
                    //     flagWidth,
                    //     FLAG_HEIGHT,
                    //     {fill: milestoneFlagColor}
                    // ));

                    // Flag pole (vertical line from milestone to flag)
                    if (calendarAtBottom) {
                        parentGroup.appendChild(createSvgElement('line', {
                            x1: centerX,
                            y1: flagY + FLAG_HEIGHT - 4,
                            x2: centerX,
                            y2: flagY + FLAG_HEIGHT - 1,
                            stroke: flagTextColor,
                            'stroke-width': '1'
                        }, undefined));
                    } else {
                        parentGroup.appendChild(createSvgElement('line', {
                            x1: centerX,
                            y1: centerY + MILESTONE_HEIGHT,
                            x2: centerX,
                            y2: centerY + MILESTONE_HEIGHT + 3,
                            stroke: flagTextColor,
                            'stroke-width': '1'
                        }, undefined));
                    }

                    // Flag text (date)
                    const flagTextElement = createText(
                        x - MILESTONE_WIDTH / 2 + 2,
                        flagY + FLAG_HEIGHT - 5,
                        flagLabel,
                        {
                            fill: flagTextColor,
                            'font-size': '11px',
                            'font-family': 'sans-serif',
                            'text-anchor': 'start'
                        }
                    );
                    parentGroup.appendChild(flagTextElement);
                }
            }
        }

        /**
         * Formats a date for tooltip display (e.g., "Sunday 19 June 2026").
         * @param {Date} date
         * @returns {string}
         */
        _formatDateForTooltip(date) {
            const options = {weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'};
            return date.toLocaleDateString('en-US', options);
        }

        /**
         * Formats a date for flag label display (e.g., "Jun.19").
         * @param {Date} date
         * @returns {string}
         */
        _formatDateForFlag(date) {
            const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
            const month = monthNames[date.getUTCMonth()];
            const day = date.getUTCDate();
            return `${month}.${day}`;
        }
    }

    // Export
    window.CalendarXAxes = CalendarXAxes;
})();
