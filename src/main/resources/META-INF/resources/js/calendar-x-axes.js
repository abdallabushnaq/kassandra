// CalendarXAxes.js
// Virtual-canvas calendar header renderer.
// Mirrors Java: CalendarXAxes, CalendarSize.YEARS, calendarAtBottom=false
// Row order (top→bottom): year → month → [week] → [dayOfMonth → dayOfWeek] → [milestones]
// Depends on: svg-utils.js, color-utils.js, date-utils.js, theme-class.js
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0
/** @deprecated
 */
(function () {
    'use strict';

    let GraphColorUtil = window.GraphColorUtil;
    let createSvgElement = window.SvgUtils.createSvgElement;
    let createLine = window.SvgUtils.createLine;
    let createCircle = window.SvgUtils.createCircle;
    let createRect = window.SvgUtils.createRect;
    let createText = window.SvgUtils.createText;
    let createClipPath = window.SvgUtils.createClipPath;
    let intToHex = window.ColorUtils.intToHex;
    // let MS = window.DateUtils.MS;
    // let calculateDayIndex = window.DateUtils.calculateDayIndex;
    let calculateDays = window.DateUtils.calculateDays;
    let addDay = window.DateUtils.addDay;
    let maxDate = window.DateUtils.maxDate;
    let getWeekSunday = window.DateUtils.getWeekSunday;
    let getWeekOfYear = window.DateUtils.getWeekOfYear;
    // let createDateString = window.DateUtils.createDateString;
    let convertSprintColorToRgba = window.ColorUtils.convertSprintColorToRgba;

    const DAY_OF_MONTH_MIN_DAY_WIDTH = 16;
    const DAY_OF_WEEK_MIN_DAY_WIDTH = 10;
    const MONTH_MIN_DAY_WIDTH = 1;
    const WEEK_MIN_DAY_WIDTH = 2;
    // Row heights (Java CalendarElement heights: fontSize + margin = 4)
    const YEAR_H = 17;   // 13+4 → rect bh-1 = 16px visible
    const MONTH_H = 16;   // 12+4 → rect bh-1 = 15px visible
    const WEEK_H = 14;   // 10+4 → rect bh-1 = 13px visible
    const DOM_H = 14;   // 10+4  day-of-month
    const DOW_H = 14;   // 10+4  day-of-week
    const MILESTONE_H = 13;   // milestone row

    // Visibility thresholds (mirrors Java CalendarXAxes constants)
    const MIN_WEEK = 2;   // WEEK_MIN_DAY_WIDTH
    const MIN_DOW = 10;  // DAY_OF_WEEK_MIN_DAY_WIDTH  – show DOW row only
    const MIN_DOM = 16;  // DAY_OF_MONTH_MIN_DAY_WIDTH – show DOM row above DOW
    // ── CalendarSize enum (mirrors Java enum) ───────────────────────────────
    let CalendarSize = {
        YEARS: 'YEARS',
        MONTHS: 'MONTHS'
    };

    // ── CalendarElement class (mirrors Java CalendarElement) ────────────────
    /**
     * Represents a calendar row element (year, month, week, day).
     * Mirrors: de.bushnaq.abdalla.kassandra.report.dao.CalendarElement
     */
    class CalendarElement {
        /**
         * @param {Font|Object} font     Font object or specification
         * @param {Color|String} bgColor Background color
         * @param {number} width         Width in pixels (may be null)
         * @param {number} height        Height in pixels
         */
        constructor(font, bgColor, width, height) {
            this.font = font;
            this.bgColor = bgColor;
            this.width = width;
            this.height = height;
            this.y = 0;
        }

        getWidth() {
            return this.width;
        }

        setWidth(w) {
            this.width = w;
        }

        getHeight() {
            return this.height;
        }

        getY() {
            return this.y;
        }

        setY(yVal) {
            this.y = yVal;
        }

        getFont() {
            return this.font;
        }
    }

    // ── CalendarXAxes class ──────────────────────────────────────────────────
// ── CalendarMilestoneElement class ──────────────────────────────────────
    /**
     * Represents the milestone row element with flag styling.
     * Mirrors: de.bushnaq.abdalla.kassandra.report.dao.CalendarMilestoneElement
     */
    class CalendarMilestoneElement extends CalendarElement {
        /**
         * @param {Color|String} bgColor        Background color
         * @param {Color|String} flagBgColor    Flag background color
         * @param {number} width                Width of milestone marker
         * @param {number} height               Height of milestone marker
         * @param {Font|Object} font            Font for milestone text
         * @param {Font|Object} flagFont        Font for flag text
         * @param {number} flagHeight           Height of flag
         */
        constructor(bgColor, flagBgColor, width, height, font, flagFont, flagHeight) {
            super(font, bgColor, width, height);
            this.width = width;
            this.flagBgColor = flagBgColor;
            this.flagFont = flagFont;
            this.flagHeight = flagHeight;
            this.flagY = 0;
            this.y = 0;
        }

        getFlagHeight() {
            return this.flagHeight;
        }

        getFlagFont() {
            return this.flagFont;
        }
    }

// ── CalendarXAxes main class ────────────────────────────────────────
    /**
     * Renders calendar header for charts (Gantt, burndown, Sprints Overview).
     * Always uses parent object pattern:
     * - Gantt/burndown: parent has {graphics2D, diagram, days, ...} for Java rendering
     * - Sprints Overview: parent has {milestones, theme} for SVG rendering
     *
     * Mirrors: de.bushnaq.abdalla.kassandra.report.dao.CalendarXAxes
     */
    class CalendarXAxes {
        /**
         * Constructor.
         *
         * @param {Object} parent       Parent renderer object
         * @param {number} priRun       Days prior to time range
         * @param {number} postRun      Days post to time range
         */
        constructor(parent, priRun, postRun) {
            this.parent = parent;
            this.priRun = priRun;
            this.postRun = postRun;
            this.milestones = parent.milestones;
            this.theme = parent.theme;

            // Initialize calendar elements (mirror Java initialization in constructor)
            let margine = 4;
            this.year = new CalendarElement(
                {family: 'sans-serif', size: 14, weight: 'normal'},
                null,
                null,
                13 + margine
            );
            this.month = new CalendarElement(
                {family: 'sans-serif', size: 12, weight: 'normal'},
                null,
                null,
                12 + margine
            );
            this.week = new CalendarElement(
                {family: 'sans-serif', size: 10, weight: 'normal'},
                null,
                null,
                10 + margine
            );
            this.dayOfMonth = new CalendarElement(
                {family: 'sans-serif', size: 10, weight: 'bold'},
                null,
                20,
                10 + margine
            );
            this.dayOfWeek = new CalendarElement(
                {family: 'sans-serif', size: 10, weight: 'bold'},
                null,
                20,
                10 + margine
            );
            this.milestone = new CalendarMilestoneElement(
                null,
                null,
                11,
                10 + margine,
                {family: 'sans-serif', size: 10, weight: 'bold'},
                {family: 'sans-serif', size: 11, weight: 'normal'},
                13
            );

            this.calendarAtBottom = false;
            this.calendarSize = CalendarSize.YEARS;
            this.width = 0;
            this.x = 0;
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
            let height = this.year.getHeight();
            if (this.isMonthVisible())
                height += this.month.getHeight();
            if (this.isDayOfMonthVisible())
                height += this.dayOfMonth.getHeight();
            if (this.isDayOfWeekVisible())
                height += this.dayOfWeek.getHeight();
            if (this.isWeekVisible())
                height += this.week.getHeight();
            if (this.milestonesVisible())
                height += this.milestone.getHeight() + this.milestone.getHeight();

            // if (dayWidth >= MIN_WEEK) height += WEEK_H;
            // if (dayWidth >= MIN_DOM) height += DOM_H + DOW_H;
            // else if (dayWidth >= MIN_DOW) height += DOW_H;
            // if (hasMilestones) height += MILESTONE_H;
            return height;
        }


        // draw(svg, chartStart, totalDays, dayWidth, scrollOffset, viewportWidth, milestones) {
        drawCalendar(drawDays, svgGroup, viewportWidth) {
            // Gantt mode only
            if (!this.parent) return;

            let firstDay = addDay(this.milestones.firstMilestone, -this.priRun);
            let lastDay = maxDate(
                this.milestones.lastMilestone,
                addDay(this.milestones.firstMilestone, this.parent.days - 1)
            );

            let yearWasDrawn = false;
            let monthWasDrawn = false;
            let firstWeekWasDrawn = false;

            let weekDays = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];
            let months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

            // Phase 0-4 loop (mirrors Java 5-phase rendering)
            for (let phase = 0; phase < 5; phase++) {
                let currentDay = new Date(firstDay);

                while (currentDay <= lastDay) {
                    let daysX = this.calculateDayX(currentDay);
                    let startCal = new Date(currentDay);

                    // Phase 4: YEAR
                    if (CalendarSize.YEARS === this.calendarSize && phase === 4 &&
                        ((startCal.getDate() === 1 && startCal.getMonth() === 0) || !yearWasDrawn)) {
                        let end = new Date(startCal.getFullYear(), 11, 31);
                        if (end > lastDay) {
                            end = new Date(lastDay);
                        }
                        let x2 = this.calculateDayX(end) - this.dayOfWeek.getWidth() / 2;
                        this.drawTextBox(
                            daysX - (this.dayOfWeek.getWidth() / 2 - 1),
                            x2 + this.dayOfWeek.getWidth(),
                            this.year.getY(),
                            this.year.getHeight(),
                            String(startCal.getFullYear()),
                            this.parent.theme.xAxesTheme.yearTextColor,
                            this.parent.theme.xAxesTheme.yearBgColor,
                            this.parent.theme.xAxesTheme.yearBorderColor,
                            this.year.getFont(),
                            false,
                            svgGroup,
                            viewportWidth
                        );
                        yearWasDrawn = true;
                    }
                    // Phase 3: MONTH
                    else if (CalendarSize.YEARS === this.calendarSize && phase === 3 &&
                        (startCal.getDate() === 1 || !monthWasDrawn) && this.isMonthVisible()) {
                        let end = addDay(new Date(startCal.getFullYear(), startCal.getMonth() + 1, 1), -1);
                        if (end > lastDay) {
                            end = new Date(lastDay);
                        }
                        let x2 = this.calculateDayX(end) - this.dayOfWeek.getWidth() / 2;
                        let bgColor = this.parent.theme.xAxesTheme.monthBgColors[startCal.getMonth()];
                        this.drawTextBox(
                            daysX - (this.dayOfWeek.getWidth() / 2 - 1),
                            x2 + this.dayOfWeek.getWidth(),
                            this.month.getY(),
                            this.month.getHeight(),
                            months[startCal.getMonth()],
                            this.parent.theme.xAxesTheme.monthTextColor,
                            bgColor,
                            this.parent.theme.xAxesTheme.monthBorderColor,
                            this.month.getFont(),
                            false,
                            svgGroup,
                            viewportWidth
                        );
                        monthWasDrawn = true;
                    }
                    // Phase 2: WEEK
                    else if (CalendarSize.YEARS === this.calendarSize && phase === 2 &&
                        (startCal.getDay() === 1 || !firstWeekWasDrawn) && this.isWeekVisible()) {
                        let end = getWeekSunday(startCal);
                        if (end > lastDay) {
                            end = new Date(lastDay);
                        }
                        let x2 = this.calculateDayX(end) - this.dayOfWeek.getWidth() / 2;
                        let calendarWeek;
                        if (this.isDayOfWeekVisible()) {
                            calendarWeek = 'W' + getWeekOfYear(currentDay);
                        } else {
                            calendarWeek = String(currentDay.getDate());
                        }
                        this.drawTextBox(
                            daysX - (this.dayOfWeek.getWidth() / 2 - 1),
                            x2 + this.dayOfWeek.getWidth(),
                            this.week.getY(),
                            this.week.getHeight() - 1,
                            calendarWeek,
                            this.parent.theme.xAxesTheme.weekTextColor,
                            this.parent.theme.xAxesTheme.weekBgColor,
                            this.parent.theme.xAxesTheme.weekBorderColor,
                            this.week.getFont(),
                            false,
                            svgGroup,
                            viewportWidth
                        );
                        firstWeekWasDrawn = true;
                    }
                    // Phase 1: DAY OF WEEK / DAY OF MONTH
                    else if (phase === 1 && this.isDayOfWeekVisible()) {
                        // Day of Month
                        let bgColor = GraphColorUtil.getDayOfMonthBgColor(this.parent.theme, startCal);
                        let textColor = GraphColorUtil.getDayOfMonthTextColor(this.parent.theme, startCal);
                        this.drawTextBox(
                            daysX - (this.dayOfMonth.getWidth() / 2 - 1),
                            daysX - (this.dayOfMonth.getWidth() / 2 - 1) + (this.dayOfMonth.getWidth() - 1),
                            this.dayOfMonth.getY(),
                            this.dayOfMonth.getHeight(),
                            String(startCal.getDate()),
                            textColor,
                            bgColor,
                            this.parent.theme.xAxesTheme.dayOfMonthBorderColor,
                            this.dayOfMonth.getFont(),
                            true,
                            svgGroup,
                            viewportWidth
                        );

                        // Day of Week
                        let color = GraphColorUtil.getDayOfWeekBgColor(this.parent.theme, startCal);
                        let textColor2 = GraphColorUtil.getDayOfWeekTextColor(this.parent.theme, startCal);
                        let dayOfWeek = startCal.getDay();
                        let weekLetter = weekDays[startCal.getDay()];
                        this.drawTextBox(
                            daysX - (this.dayOfWeek.getWidth() / 2 - 1),
                            daysX - (this.dayOfWeek.getWidth() / 2 - 1) + (this.dayOfWeek.getWidth() - 1),
                            this.dayOfWeek.getY(),
                            this.dayOfWeek.getHeight(),
                            weekDays[startCal.getDay()],
                            textColor2,
                            color,
                            this.parent.theme.xAxesTheme.dayOfWeekBorderColor,
                            this.dayOfWeek.getFont(),
                            true,
                            svgGroup,
                            viewportWidth
                        );
                    }
                    // Phase 0: DAY BARS and MILESTONE BACKGROUND
                    else if (phase === 0) {
                        if (drawDays && this.isDayBarsVisible()) {
                            this.parent.drawDayBars(svgGroup, currentDay);
                        }
                        if (this.milestonesVisible()) {
                            let color = GraphColorUtil.getDayOfWeekBgColor(this.parent.theme, startCal);
                            let textColor2 = GraphColorUtil.getDayOfWeekTextColor(this.parent.theme, startCal);
                            this.drawTextBox(
                                daysX - (this.dayOfWeek.getWidth() / 2 - 1),
                                daysX - (this.dayOfWeek.getWidth() / 2 - 1) + (this.dayOfWeek.getWidth() - 1),
                                this.milestone.flagY,
                                this.milestone.flagHeight,
                                null,
                                textColor2,
                                color,
                                this.parent.theme.xAxesTheme.dayOfWeekBorderColor,
                                null,
                                true,
                                svgGroup,
                                viewportWidth
                            );
                        }
                    }

                    currentDay = addDay(currentDay, 1);
                }
            }
        }

        /**
         * Draw a text box (cell in calendar header).
         * Mirrors: public void drawTextBox(int x1, int x2, Integer y1, int height, String text, ...)
         * Handles both Java graphics2D (server rendering) and SVG (client rendering) modes.
         */
        drawTextBox(x1, x2, y1, height, text, textColor, backgroundColor, borderColor, font, centered, svgGroup, viewportWidth) {

            // SVG rendering mode (client-side)
            // Skip if outside viewport
            if (x1 + (x2 - x1) <= 0 || x1 >= (viewportWidth || 9999)) return;
            // if (x1 === 1)
            //     text = 'X';

            // if (text === 'T')
            //     text = 'X';

            let cellWidth = x2 - x1;
            // Draw background rectangle
            svgGroup.appendChild(createRect(x1, y1, cellWidth, height - 1, {fill: intToHex(backgroundColor)}));

            // Draw border line (right edge)
            if (borderColor && cellWidth > 1) {
                svgGroup.appendChild(createLine(x2, y1, x2, y1 + height - 1, {
                    stroke: intToHex(borderColor),
                    'stroke-width': '1'
                }));

                // svgGroup.appendChild(createSvgElement('line', {
                //     x1: x2, y1: y1,
                //     x2: x2, y2: y1 + height - 1,
                //     stroke: borderColor, 'stroke-width': '1'
                // }));
            }

            // Draw text label
            if (text) {
                //TODO reintroduce clipping
                // let clipId = 'cc' + Math.random().toString(36).substr(2, 9);
                // svgGroup.appendChild(createClipPath(clipId, x1, y1, cellWidth, height));
                let fontSize = '10';
                if (font && font.getSize) fontSize = String(font.getSize());
                let textX = centered ? x1 + cellWidth / 2 : x1 + 2;
                svgGroup.appendChild(createText(textX, y1 + height - 4, text, {
                    fill: intToHex(textColor),
                    'font-size': fontSize,
                    'font-family': 'sans-serif',
                    'text-anchor': centered ? 'middle' : 'start'/*,
                //TODO reintroduce clipping
                    'clip-path': 'url(#' + clipId + ')'*/
                }));
            }
        }


        /**
         * Calculate X position for a given date (Gantt mode only).
         * Mirrors: protected int calculateDayX(LocalDate date)
         */
        calculateDayX(date) {
            // Gantt mode only - requires milestones
            if (!this.milestones.firstMilestone) return 0;
            let firstMilestoneX = this.x + this.dayOfWeek.getWidth() / 2;
            return firstMilestoneX + (calculateDays(this.milestones.firstMilestone, date) - this.parent.scrollOffset + this.priRun) * this.dayOfWeek.getWidth();
        }

        drawMilestones(svg) {
            for (let milestone of this.milestones.getList()) {
                let x = this.calculateDayX(milestone.time);
                this.drawMilestoneShort(svg, milestone, milestone.time, x, this.parent.theme.ganttTheme.requestMilestoneColor, this.milestone.symbol,
                    !milestone.hidden, this.parent.theme.xAxesTheme.futureEventColor);// start
            }
        }

        drawMilestoneShort(svg, m, time, x, fillColor, text, visible, flagTextColor) {
            this.drawMilestone(svg, m, time, x, this.milestone.y, fillColor, text, visible, this.milestone.flagY, flagTextColor, true, true);
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
         * @param {boolean}     visible Whether to draw the box and text
         * @param {number|null} flagY        Y for the flag; null → no flag
         * @param {string}      flagTextColor Color for flag pole/text
         * @param {boolean}     drawFlag     Whether to draw a date flag
         * @param {boolean}     drawNowLine  Whether to draw a vertical now-line
         * @param {SVGElement}  parentGroup  SVG group to append to
         * @param {number}      diagramY     Top of diagram area (for now-line)
         * @param {number}      diagramHeight Height of diagram area (for now-line)
         * @param {boolean}     calendarAtBottom  Calendar position flag
         */
        drawMilestone(parentGroup, m, time, x, y, fillColor, text,
                      visible, flagY, flagTextColor,
                      drawFlag, drawNowLine) {
            const MILESTONE_WIDTH = 11;
            const MILESTONE_HEIGHT = 13;
            const FLAG_HEIGHT = 13;
            const theme = this.theme;
            const darkRed = '#8B0000';
            const milestoneTextColor = intToHex(theme.xAxesTheme && theme.xAxesTheme.milestoneTextColor, '#ffffff');

            if (text && text.charAt(0) === 'N' && drawNowLine) {
                parentGroup.appendChild(createLine(x, this.parent.diagram.y, x, this.parent.diagram.y + this.parent.diagram.height, {
                    stroke: darkRed,
                    'stroke-width': '2'
                }));
                // parentGroup.appendChild(createSvgElement('line', {
                //     x1: x, y1: this.parent.diagram.y,
                //     x2: x, y2: this.parent.diagram.y + this.parent.diagram.height,
                //     stroke: darkRed, 'stroke-width': '2'
                // }));
                let r = 3;
                parentGroup.appendChild(createCircle(x + 1,
                    this.calendarAtBottom ? this.parent.diagram.y - r / 2 : this.parent.diagram.y + this.parent.diagram.height - r, r, {
                        fill: darkRed
                    }));
                // parentGroup.appendChild(createSvgElement('circle', {
                //     cx: x + 1,
                //     cy: this.calendarAtBottom ? this.parent.diagram.y - r / 2 : this.parent.diagram.y + this.parent.diagram.height - r,
                //     r: r,
                //     fill: darkRed
                // }));
            }

            if (visible) {
                parentGroup.appendChild(createRect(
                    x - MILESTONE_WIDTH / 2, y,
                    MILESTONE_WIDTH, MILESTONE_HEIGHT - 1,
                    {fill: intToHex(fillColor)}
                ));
                let textEl = createText(x - 1, y + MILESTONE_HEIGHT / 2 + 1, text, {
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
                        stroke: intToHex(flagTextColor), 'stroke-width': '1'
                    }));
                    parentGroup.appendChild(createText(
                        x - MILESTONE_WIDTH / 2 + 2, flagY + FLAG_HEIGHT - 5,
                        this._formatDateForFlag(time),
                        {
                            fill: intToHex(flagTextColor),
                            'font-size': '11px',
                            'font-family': 'sans-serif',
                            'text-anchor': 'start'
                        }
                    ));
                }
            }
        }

        initPosition(x, y) {
            this.x = x;
            if (this.calendarAtBottom) {
                // flag
                // milestone, dayOfWeek
                // dayOfMonth
                // week
                // month
                // year

                this.milestone.flagY = y;
                this.milestone.y = this.milestone.flagY + this.milestone.flagHeight;
                this.dayOfWeek.setY(this.milestone.y);
                this.dayOfMonth.setY(this.dayOfWeek.getY() + this.dayOfWeek.getHeight());
                if (this.isDayOfWeekVisible()) {
                    if (this.isDayOfMonthVisible()) {
                        this.week.setY(this.dayOfMonth.getY() + this.dayOfMonth.getHeight());
                    } else {
                        this.week.setY(this.dayOfWeek.getY() + this.dayOfWeek.getHeight());
                    }
                } else {
                    this.week.setY(this.milestone.y + this.milestone.height);
                }
                if (this.isWeekVisible()) {
                    this.month.setY(this.week.getY() + this.week.getHeight());
                } else {
                    this.month.setY(this.dayOfWeek.getY() + this.dayOfWeek.getHeight());
                }
                this.year.setY(this.month.getY() + this.month.getHeight());
            } else {
                // year
                // month
                // week
                // dayOfMonth
                // milestone, dayOfWeek
                // flag
                if (CalendarSize.YEARS === this.calendarSize) {
                    this.year.setY(y);
                    this.month.setY(this.year.getY() + this.year.getHeight());
                    this.week.setY(this.month.getY() + this.month.getHeight());
                    this.dayOfMonth.setY(this.week.getY() + this.week.getHeight());
                    if (this.isDayOfMonthVisible()) {
                        this.dayOfWeek.setY(this.dayOfMonth.getY() + this.dayOfMonth.getHeight());
                    } else {
                        this.dayOfWeek.setY(this.week.getY() + this.week.getHeight());
                    }
                    this.milestone.y = this.dayOfWeek.getY();
                    this.milestone.flagY = this.dayOfWeek.getY() + this.milestone.height;

                } else {
                    this.year.setY(y);
                    this.month.setY(y);
                    this.week.setY(y);
                    this.dayOfMonth.setY(this.week.getY() + this.week.getHeight());
                    if (this.isDayOfMonthVisible()) {
                        this.dayOfWeek.setY(this.dayOfMonth.getY() + this.dayOfMonth.getHeight());
                    } else {
                        this.dayOfWeek.setY(this.week.getY() + this.week.getHeight());
                    }
                    this.milestone.y = this.dayOfWeek.getY();
                    this.milestone.flagY = this.dayOfWeek.getY() + this.milestone.height;
                }
            }
        }

        initSize(width, dayWidth, calendarAtBottom, calendarSize) {
            this.calendarAtBottom = calendarAtBottom;
            this.width = width;
            this.dayOfWeek.setWidth(dayWidth);
            this.dayOfMonth.setWidth(dayWidth);
            this.calendarSize = calendarSize;
        }

        isDayBarsVisible() {
            return this.dayOfWeek.getWidth() >= 4;
        }

        isDayOfMonthVisible() {
            return this.dayOfWeek.getWidth() >= DAY_OF_MONTH_MIN_DAY_WIDTH;
        }

        isDayOfWeekVisible() {
            return this.dayOfWeek.getWidth() >= DAY_OF_WEEK_MIN_DAY_WIDTH;
        }

        isMonthVisible() {
            return CalendarSize.YEARS === this.calendarSize && this.dayOfWeek.getWidth() >= MONTH_MIN_DAY_WIDTH;
        }

        isWeekVisible() {
            return CalendarSize.YEARS === this.calendarSize && this.dayOfWeek.getWidth() >= WEEK_MIN_DAY_WIDTH;
        }

        milestonesVisible() {
            return !this.milestones.empty();
        }


        /** @private Formats a date as "Sunday 19 June 2026". */
        _formatDateForTooltip(date) {
            return date.toLocaleDateString('en-US', {weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'});
        }

        /** @private Formats a date as "Jun.19". */
        _formatDateForFlag(date) {
            const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
            return months[date.getMonth()] + '.' + date.getDate();
        }


    }

    // ── Export ─────────────────────────────────────────────────────────────────

    window.CalendarXAxes = CalendarXAxes;
    window.CalendarSize = CalendarSize;
})();
