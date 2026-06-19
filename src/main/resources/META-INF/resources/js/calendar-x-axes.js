// CalendarXAxes.js
// Virtual-canvas calendar header renderer.
// Mirrors Java: CalendarXAxes, CalendarSize.YEARS, calendarAtBottom=false
// Row order (top→bottom): year → month → [week] → [dayOfMonth → dayOfWeek] → [milestones]
// Depends on: svg-utils.js, color-utils.js, date-utils.js, theme-class.js
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0
(function () {
    'use strict';

    var createSvgElement = window.SvgUtils.createSvgElement;
    var createRect       = window.SvgUtils.createRect;
    var createText       = window.SvgUtils.createText;
    var createClipPath   = window.SvgUtils.createClipPath;
    var intToHex         = window.ColorUtils.intToHex;
    var MS               = window.DateUtils.MS;
    var getUtcDayMidnight  = window.DateUtils.getUtcDayMidnight;
    var calculateDayIndex  = window.DateUtils.calculateDayIndex;

    // Row heights (Java CalendarElement heights: fontSize + margin = 4)
    var YEAR_H      = 17;   // 13+4 → rect bh-1 = 16px visible
    var MONTH_H     = 16;   // 12+4 → rect bh-1 = 15px visible
    var WEEK_H      = 14;   // 10+4 → rect bh-1 = 13px visible
    var DOM_H       = 14;   // 10+4  day-of-month
    var DOW_H       = 14;   // 10+4  day-of-week
    var MILESTONE_H = 13;   // milestone row

    // Visibility thresholds (mirrors Java CalendarXAxes constants)
    var MIN_WEEK = 2;   // WEEK_MIN_DAY_WIDTH
    var MIN_DOW  = 10;  // DAY_OF_WEEK_MIN_DAY_WIDTH  – show DOW row only
    var MIN_DOM  = 16;  // DAY_OF_MONTH_MIN_DAY_WIDTH – show DOM row above DOW

    // ── CalendarXAxes class ──────────────────────────────────────────────────

    /**
     * Renders a virtual-canvas calendar header for a chart.
     * Hierarchical rows: year → month → week (optional) → day-of-month/day-of-week (optional).
     * Visibility and styling are controlled by dayWidth thresholds and Theme class instance.
     *
     * Mirrors Java: CalendarXAxes
     */
    class CalendarXAxes {
        /**
         * @param {Theme} theme  Theme instance (provides xAxesTheme colors)
         */
        constructor(theme) {
            this.theme = theme || {};
        }

        /**
         * Calculates the total pixel height of the calendar header for a given day width.
         * Mirrors Java: CalendarXAxes.getHeight()
         *
         * @param {number}  dayWidth      Pixel width per day
         * @param {boolean} hasMilestones Whether there are milestones to render
         * @returns {number} Total header height in pixels
         */
        getHeight(dayWidth, hasMilestones) {
            var height = YEAR_H + MONTH_H;
            if (dayWidth >= MIN_WEEK) height += WEEK_H;
            if (dayWidth >= MIN_DOM)  height += DOM_H + DOW_H;
            else if (dayWidth >= MIN_DOW) height += DOW_H;
            if (hasMilestones) height += MILESTONE_H;
            return height;
        }

        /**
         * Draws the calendar header into the given SVG element.
         * Mirrors Java: CalendarXAxes.drawCalendar()
         *
         * @param {SVGSVGElement} svg            Target SVG element
         * @param {Date}          chartStart      UTC midnight of first chart day
         * @param {number}        totalDays       Total days in chart
         * @param {number}        dayWidth        Pixel width per day
         * @param {number}        scrollOffset    First visible day index (may be fractional)
         * @param {number}        viewportWidth   Container width in pixels
         * @param {Array}         [milestones]    Optional milestone array {date, letter, label}
         * @returns {number} Actual header height drawn
         */
        draw(svg, chartStart, totalDays, dayWidth, scrollOffset, viewportWidth, milestones) {
            var theme       = this.theme;
            var xAxesTheme  = theme.xAxesTheme || {};
            var headerGroup = createSvgElement('g', {'class': 'calendar-header'});
            svg.appendChild(headerGroup);

            // ── Resolve theme colors via direct property access ────────────────
            var yearBgColor              = intToHex(xAxesTheme.yearBgColor,              '#ababab');
            var yearTextColor            = intToHex(xAxesTheme.yearTextColor,            '#ffffff');
            var yearBorderColor          = intToHex(xAxesTheme.yearBorderColor,          '#ffffff');
            var monthTextColor           = intToHex(xAxesTheme.monthTextColor,           '#ffffff');
            var monthBorderColor         = intToHex(xAxesTheme.monthBorderColor,         '#ffffff');
            var weekBgColor              = intToHex(xAxesTheme.weekBgColor,              '#ababab');
            var weekTextColor            = intToHex(xAxesTheme.weekTextColor,            '#ffffff');
            var weekBorderColor          = intToHex(xAxesTheme.weekBorderColor,          '#ffffff');
            var dayOfMonthBgColor        = intToHex(xAxesTheme.dayOfMonthBgColor,        '#ababab');
            var dayOfMonthBorderColor    = intToHex(xAxesTheme.dayOfMonthBorderColor,    '#ffffff');
            var dayOfMonthTextColor      = intToHex(xAxesTheme.dayOfMonthTextColor,      '#ffffff');
            var dayOfMonthWeekendBgColor = intToHex(xAxesTheme.dayOfMonthWeekendBgColor, '#d7d7d7');
            var dayOfMonthWeekendTextColor = intToHex(xAxesTheme.dayOfMonthWeekendTextColor, '#000000');
            var dayOfWeekBgColor         = intToHex(xAxesTheme.dayOfweekBgColor,         '#ffffff');
            var dayOfWeekBorderColor     = intToHex(xAxesTheme.dayOfWeekBorderColor,     '#ffffff');
            var dayOfWeekTextColor       = intToHex(xAxesTheme.dayOfWeekTextColor,       '#000000');
            var dayOfWeekSatBgColor      = intToHex(xAxesTheme.dayOfweekSaturdayBgColor, '#d7d7d7');
            var dayOfWeekSunBgColor      = intToHex(xAxesTheme.dayOfweekSundayBgColor,   '#d7d7d7');
            var dayOfWeekWeekendTextColor = intToHex(xAxesTheme.dayOfWeekWeekendTextColor, '#000000');

            var MONTH_NAMES     = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
            var DAY_ABBREV      = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];

            var dayIndexToPixelX = function (dayIndex) {
                return (dayIndex - scrollOffset) * dayWidth;
            };

            var chartEnd       = new Date(chartStart.getTime() + (totalDays - 1) * MS);
            var currentY       = 0;
            var clipIdCounter  = 0;

            // ── drawCalendarCell helper ────────────────────────────────────────
            // Mirrors Java CalendarXAxes.drawTextBox()
            var drawCalendarCell = function (cellX, cellWidth, cellY, cellHeight, bgColor, borderColor, labelText, textColor, fontSize, centered) {
                if (cellX + cellWidth <= 0 || cellX >= viewportWidth) return;
                headerGroup.appendChild(createRect(cellX, cellY, cellWidth, cellHeight - 1, {fill: bgColor}));
                if (borderColor && cellWidth > 1) {
                    headerGroup.appendChild(createSvgElement('line', {
                        x1: cellX + cellWidth, y1: cellY,
                        x2: cellX + cellWidth, y2: cellY + cellHeight - 1,
                        stroke: borderColor, 'stroke-width': '1'
                    }));
                }
                if (!labelText) return;
                var clipId = 'cc' + (clipIdCounter++);
                headerGroup.appendChild(createClipPath(clipId, cellX, cellY, cellWidth, cellHeight));
                var textX = centered ? cellX + cellWidth / 2 : Math.max(cellX + 2, 2);
                headerGroup.appendChild(createText(textX, cellY + cellHeight - 4, labelText, {
                    fill: textColor,
                    'font-size': fontSize,
                    'font-family': 'sans-serif',
                    'text-anchor': centered ? 'middle' : 'start',
                    'clip-path': 'url(#' + clipId + ')'
                }));
            };

            // ── YEAR ROW (always visible) ──────────────────────────────────────
            for (var year = chartStart.getUTCFullYear(); year <= chartEnd.getUTCFullYear(); year++) {
                var startIdx = calculateDayIndex(new Date(Date.UTC(year, 0, 1)), chartStart);
                var endIdx   = calculateDayIndex(new Date(Date.UTC(year, 11, 31)), chartStart);
                var cellX    = dayIndexToPixelX(Math.max(0, startIdx));
                var cellW    = dayIndexToPixelX(Math.min(totalDays - 1, endIdx) + 1) - cellX;
                drawCalendarCell(cellX, cellW, currentY, YEAR_H, yearBgColor, yearBorderColor, String(year), yearTextColor, '14', false);
            }
            currentY += YEAR_H;

            // ── MONTH ROW (always visible) ─────────────────────────────────────
            var monthDate = new Date(Date.UTC(chartStart.getUTCFullYear(), chartStart.getUTCMonth(), 1));
            while (monthDate <= chartEnd) {
                var monthIndex = monthDate.getUTCMonth();
                var monthYear  = monthDate.getUTCFullYear();
                var monthEnd   = new Date(Date.UTC(monthYear, monthIndex + 1, 0));
                var si         = calculateDayIndex(monthDate, chartStart);
                var ei         = calculateDayIndex(monthEnd, chartStart);
                var cx         = dayIndexToPixelX(Math.max(0, si));
                var cw         = dayIndexToPixelX(Math.min(totalDays - 1, ei) + 1) - cx;
                // monthBgColors array: mirrors Java XAxesTheme.monthBgColors[12]
                var monthBgColors = xAxesTheme.monthBgColors;
                var monthBgColor  = (monthBgColors && monthBgColors[monthIndex] != null) ? intToHex(monthBgColors[monthIndex]) : '#187dc3';
                drawCalendarCell(cx, cw, currentY, MONTH_H, monthBgColor, monthBorderColor, MONTH_NAMES[monthIndex], monthTextColor, '12', false);
                monthDate = new Date(Date.UTC(monthYear, monthIndex + 1, 1));
            }
            currentY += MONTH_H;

            // ── WEEK ROW (visible when dayWidth >= MIN_WEEK) ───────────────────
            if (dayWidth >= MIN_WEEK) {
                var weekStart = new Date(chartStart.getTime());
                while (weekStart.getUTCDay() !== 1) weekStart.setUTCDate(weekStart.getUTCDate() - 1);
                while (weekStart <= chartEnd) {
                    var weekEnd  = new Date(weekStart.getTime());
                    weekEnd.setUTCDate(weekEnd.getUTCDate() + 6);
                    var ws = calculateDayIndex(weekStart, chartStart);
                    var we = calculateDayIndex(weekEnd, chartStart);
                    var wx = dayIndexToPixelX(Math.max(0, ws));
                    var ww = dayIndexToPixelX(Math.min(totalDays - 1, we) + 1) - wx;
                    var weekLabel = dayWidth >= MIN_DOW
                        ? String(new Date(chartStart.getTime() + Math.max(0, ws) * MS).getUTCDate())
                        : null;
                    drawCalendarCell(wx, ww, currentY, WEEK_H, weekBgColor, weekBorderColor, weekLabel, weekTextColor, '10', false);
                    weekStart.setUTCDate(weekStart.getUTCDate() + 7);
                }
                currentY += WEEK_H;
            }

            // ── DAY-OF-MONTH ROW (visible when dayWidth >= MIN_DOM) ───────────
            if (dayWidth >= MIN_DOM) {
                var firstVis = Math.max(0, Math.floor(scrollOffset) - 1);
                var lastVis  = Math.min(totalDays - 1, firstVis + Math.ceil(viewportWidth / dayWidth) + 2);
                for (var dayIdx = firstVis; dayIdx <= lastVis; dayIdx++) {
                    var dayDate = new Date(chartStart.getTime() + dayIdx * MS);
                    var dow     = dayDate.getUTCDay();
                    var isWend  = dow === 0 || dow === 6;
                    drawCalendarCell(
                        dayIndexToPixelX(dayIdx), dayWidth, currentY, DOM_H,
                        isWend ? dayOfMonthWeekendBgColor : dayOfMonthBgColor,
                        dayOfMonthBorderColor,
                        String(dayDate.getUTCDate()),
                        isWend ? dayOfMonthWeekendTextColor : dayOfMonthTextColor,
                        '10', true
                    );
                }
                currentY += DOM_H;
            }

            // ── DAY-OF-WEEK ROW (visible when dayWidth >= MIN_DOW) ────────────
            if (dayWidth >= MIN_DOW) {
                var firstVisDow = Math.max(0, Math.floor(scrollOffset) - 1);
                var lastVisDow  = Math.min(totalDays - 1, firstVisDow + Math.ceil(viewportWidth / dayWidth) + 2);
                for (var di = firstVisDow; di <= lastVisDow; di++) {
                    var dd   = new Date(chartStart.getTime() + di * MS);
                    var d    = dd.getUTCDay();
                    var iW   = d === 0 || d === 6;
                    var dBg  = d === 6 ? dayOfWeekSatBgColor : (d === 0 ? dayOfWeekSunBgColor : dayOfWeekBgColor);
                    drawCalendarCell(
                        dayIndexToPixelX(di), dayWidth, currentY, DOW_H,
                        dBg, dayOfWeekBorderColor, DAY_ABBREV[d],
                        iW ? dayOfWeekWeekendTextColor : dayOfWeekTextColor,
                        '10', true
                    );
                }
                // Note: currentY is NOT advanced here (DOW row shares the space counted by DOM)
                // It IS advanced below in the milestone row if present, or by the caller
            }

            // ── MILESTONE ROW ─────────────────────────────────────────────────
            if (milestones && milestones.length > 0) {
                var milestoneTextColor  = intToHex(xAxesTheme.milestoneTextColor, '#ffffff');
                var milestoneFillColor  = intToHex(xAxesTheme.futureEventColor,   '#0000ff');
                var milestoneFlagColor  = intToHex(xAxesTheme.milestoneFlagColor, '#ffffff');
                var requestColor        = intToHex(theme.ganttTheme && theme.ganttTheme.requestMilestoneColor, '#cc0000');

                // Map day-index → milestone for quick lookup
                var msMap = {};
                for (var mi = 0; mi < milestones.length; mi++) {
                    var ms    = milestones[mi];
                    var msDay = calculateDayIndex(new Date(ms.date), chartStart);
                    if (msDay >= 0 && msDay < totalDays) msMap[msDay] = ms;
                }

                var firstVisMs = Math.max(0, Math.floor(scrollOffset) - 1);
                var lastVisMs  = Math.min(totalDays - 1, firstVisMs + Math.ceil(viewportWidth / dayWidth) + 2);
                for (var mdi = firstVisMs; mdi <= lastVisMs; mdi++) {
                    var msObj = msMap[mdi];
                    if (msObj) {
                        var msX = dayIndexToPixelX(mdi) + dayWidth / 2 + 1;
                        this.drawMilestone(
                            msObj, new Date(msObj.date),
                            msX, currentY,
                            requestColor, msObj.letter,
                            true, null, milestoneFlagColor,
                            false, false,
                            headerGroup, 0, 0, false
                        );
                    }
                }
                currentY += MILESTONE_H;
            }

            return currentY;
        }

        /**
         * Draws a single milestone marker (rectangle + label).
         * Mirrors Java: CalendarXAxes.drawMilestone()
         *
         * @param {Object}      m            Milestone data object (may be null)
         * @param {Date}        time         Date of the milestone
         * @param {number}      x            X-center of the milestone
         * @param {number}      y            Y-top of the milestone
         * @param {string}      fillColor    Background color for milestone box
         * @param {string}      text         Label: 'S', 'E', 'N', etc.
         * @param {boolean}     drawMileston Whether to draw the box and text
         * @param {number|null} flagY        Y for the flag; null → no flag
         * @param {string}      flagTextColor Color for flag pole/text
         * @param {boolean}     drawFlag     Whether to draw a date flag
         * @param {boolean}     drawNowLine  Whether to draw a vertical now-line
         * @param {SVGElement}  parentGroup  SVG group to append to
         * @param {number}      diagramY     Top of diagram area (for now-line)
         * @param {number}      diagramHeight Height of diagram area (for now-line)
         * @param {boolean}     calendarAtBottom  Calendar position flag
         */
        drawMilestone(m, time, x, y, fillColor, text, drawMileston, flagY, flagTextColor, drawFlag, drawNowLine, parentGroup, diagramY, diagramHeight, calendarAtBottom) {
            var MILESTONE_WIDTH  = 11;
            var MILESTONE_HEIGHT = 13;
            var FLAG_HEIGHT      = 13;
            var theme            = this.theme;
            var darkRed          = '#8B0000';
            var milestoneTextColor = intToHex(theme.xAxesTheme && theme.xAxesTheme.milestoneTextColor, '#ffffff');

            if (text && text.charAt(0) === 'N' && drawNowLine) {
                parentGroup.appendChild(createSvgElement('line', {
                    x1: x, y1: diagramY,
                    x2: x, y2: diagramY + diagramHeight,
                    stroke: darkRed, 'stroke-width': '2'
                }));
                var r = 3;
                parentGroup.appendChild(createSvgElement('circle', {
                    cx: x + 1, cy: calendarAtBottom ? diagramY - r / 2 : diagramY + diagramHeight - r,
                    r: r, fill: darkRed
                }));
            }

            if (drawMileston) {
                parentGroup.appendChild(createRect(
                    x - MILESTONE_WIDTH / 2, y,
                    MILESTONE_WIDTH, MILESTONE_HEIGHT - 1,
                    {fill: fillColor}
                ));
                var textEl = createText(x - 1, y + MILESTONE_HEIGHT / 2 + 1, text, {
                    fill: milestoneTextColor,
                    'font-size': '10px',
                    'font-family': 'sans-serif',
                    'font-weight': 'bold',
                    'text-anchor': 'middle',
                    'dominant-baseline': 'middle'
                });
                if (m) {
                    textEl.appendChild(createSvgElement('title', {}, text + ' = ' + m.label + '\n' + this._formatDateForTooltip(time)));
                }
                parentGroup.appendChild(textEl);

                if (drawFlag && flagY != null) {
                    parentGroup.appendChild(createSvgElement('line', {
                        x1: x, y1: y + MILESTONE_HEIGHT,
                        x2: x, y2: y + MILESTONE_HEIGHT + 3,
                        stroke: flagTextColor, 'stroke-width': '1'
                    }));
                    parentGroup.appendChild(createText(
                        x - MILESTONE_WIDTH / 2 + 2, flagY + FLAG_HEIGHT - 5,
                        this._formatDateForFlag(time),
                        {fill: flagTextColor, 'font-size': '11px', 'font-family': 'sans-serif', 'text-anchor': 'start'}
                    ));
                }
            }
        }

        /** @private Formats a date as "Sunday 19 June 2026". */
        _formatDateForTooltip(date) {
            return date.toLocaleDateString('en-US', {weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'});
        }

        /** @private Formats a date as "Jun.19". */
        _formatDateForFlag(date) {
            var months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
            return months[date.getUTCMonth()] + '.' + date.getUTCDate();
        }
    }

    // ── Export ─────────────────────────────────────────────────────────────────

    window.CalendarXAxes = CalendarXAxes;
})();
