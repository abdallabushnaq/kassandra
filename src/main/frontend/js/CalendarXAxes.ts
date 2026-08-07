// calendar-x-axes.ts
// Virtual-canvas calendar header renderer.
// Mirrors Java: CalendarXAxes
// Row order (top→bottom): year → month → [week] → [dayOfMonth → dayOfWeek] → [milestones]
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {ColorUtils} from './ColorUtils.js';
import {SvgUtils} from './SvgUtils.js';
import {DateUtils} from './DateUtils.js';
import {GraphColorUtil} from './GraphColorUtil.js';
import {CalendarElement} from './CalendarElement.js';
import {FontSpec} from "./FontSpec.js";
import {CalendarMilestoneElement} from './CalendarMilestoneElement.js';
import {CalendarSize} from './CalendarSize.js';
import type {IRenderer} from './IRenderer.js';

const DAY_OF_MONTH_MIN_DAY_WIDTH = 16;
const DAY_OF_WEEK_MIN_DAY_WIDTH = 10;
const MONTH_MIN_DAY_WIDTH = 1;
const WEEK_MIN_DAY_WIDTH = 2;

export class CalendarXAxes {
    parent: IRenderer;
    priRun: number;
    postRun: number;
    year: CalendarElement;
    month: CalendarElement;
    week: CalendarElement;
    dayOfMonth: CalendarElement;
    dayOfWeek: CalendarElement;
    milestone: CalendarMilestoneElement;
    calendarAtBottom: boolean;
    calendarSize: CalendarSize;
    width: number;
    x: number;

    constructor(parent: IRenderer, priRun: number, postRun: number) {
        const d0 = new Date(2025, 9, 24, 0, 0, 0, 0);
        const d1 = new Date(2025, 9, 25, 0, 0, 0, 0);
        const d2 = new Date(2025, 9, 26, 0, 0, 0, 0);
        const d3 = new Date(2025, 9, 27, 0, 0, 0, 0);
        const ds1 = DateUtils.calculateDays(d0, d1);
        const ds2 = DateUtils.calculateDays(d1, d2);
        const ds3 = DateUtils.calculateDays(d2, d3);


        this.parent = parent;
        this.priRun = priRun;
        this.postRun = postRun;

        const margin = 4;
        this.year = new CalendarElement(new FontSpec('sans-serif', 14, 'normal'), null, null, 13 + margin);
        this.month = new CalendarElement(new FontSpec('sans-serif', 12, 'normal'), null, null, 12 + margin);
        this.week = new CalendarElement(new FontSpec('sans-serif', 10, 'normal'), null, null, 10 + margin);
        this.dayOfMonth = new CalendarElement(new FontSpec('sans-serif', 10, 'bold'), null, 20, 10 + margin);
        this.dayOfWeek = new CalendarElement(new FontSpec('sans-serif', 10, 'bold'), null, 20, 10 + margin);
        this.milestone = new CalendarMilestoneElement(
            null, null, 11, 10 + margin,
            new FontSpec('sans-serif', 10, 'bold'),
            new FontSpec('sans-serif', 11, 'normal'),
            13,
        );

        this.calendarAtBottom = false;
        this.calendarSize = CalendarSize.YEARS;
        this.width = 0;
        this.x = 0;
    }

    /**
     * Calculates total pixel height of the calendar header for a given day width.
     * Mirrors Java: CalendarXAxes.getHeight()
     */
    // getHeight(): number {
    //     let height = this.year.getHeight();
    //     if (this.isMonthVisible()) height += this.month.getHeight();
    //     if (this.isDayOfMonthVisible()) height += this.dayOfMonth.getHeight();
    //     if (this.isDayOfWeekVisible()) height += this.dayOfWeek.getHeight();
    //     if (this.isWeekVisible()) height += this.week.getHeight();
    //     if (this.milestonesVisible()) height += this.milestone.getHeight() + this.milestone.getHeight();
    //     return height;
    // }
    getHeight(): number {
        let height = 0;
        if (this.isYearVisible()) {
            height += this.year.getHeight();
        }
        if (this.isMonthVisible()) {
            height += this.month.getHeight();
        }
        if (this.isWeekVisible()) {
            height += this.week.getHeight();
        }
        if (this.isDayOfWeekVisible()) {
            height += this.dayOfWeek.getHeight();
            height += this.dayOfMonth.getHeight();
        } else if (this.milestonesVisible()) {
            height += this.milestone.getHeight();
        }
        if (this.milestonesVisible()) {
            height += this.milestone.getFlagHeight();
        }
        return height;
    }

    drawCalendar(svgGroup: SVGElement, drawDays: boolean, viewportWidth: number): void {
        // if (!this.parent)
        //     return;

        const firstDay = DateUtils.addDay(this.parent.milestones.firstMilestone!, -this.priRun);
        const l = DateUtils.addDay(this.parent.milestones.firstMilestone!, this.parent.days - 1);
        const lastDay = DateUtils.maxDate(this.parent.milestones.lastMilestone!, DateUtils.addDay(this.parent.milestones.firstMilestone!, this.parent.days - 1));

        let yearWasDrawn = false;
        let monthWasDrawn = false;
        let firstWeekWasDrawn = false;

        const weekDays = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];
        const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

        for (let phase = 0; phase < 5; phase++) {
            let currentDay = new Date(firstDay);

            while (currentDay <= lastDay) {
                const daysX = this.calculateDayX(currentDay);
                const startCal = new Date(currentDay);

                // Phase 4: YEAR
                if (CalendarSize.YEARS === this.calendarSize && phase === 4 &&
                    ((startCal.getDate() === 1 && startCal.getMonth() === 0) || !yearWasDrawn)) {
                    const end = new Date(startCal.getFullYear(), 11, 31);
                    if (end > lastDay) end.setTime(lastDay.getTime());
                    const x2 = this.calculateDayX(end) - (this.dayOfWeek.getWidth() ?? 0) / 2;
                    this.drawTextBox(
                        daysX - ((this.dayOfWeek.getWidth() ?? 0) / 2),
                        x2 + (this.dayOfWeek.getWidth() ?? 0) - 1,//keep 1px left for border
                        this.year.getY(), this.year.getHeight(),
                        String(startCal.getFullYear()),
                        this.parent.theme.xAxesTheme.yearTextColor,
                        this.parent.theme.xAxesTheme.yearBgColor,
                        this.parent.theme.xAxesTheme.yearBorderColor,
                        this.year.getFont(), false, svgGroup, viewportWidth,
                    );
                    yearWasDrawn = true;
                }
                // Phase 3: MONTH
                else if (CalendarSize.YEARS === this.calendarSize && phase === 3 &&
                    (startCal.getDate() === 1 || !monthWasDrawn) && this.isMonthVisible()) {
                    const end = DateUtils.addDay(new Date(startCal.getFullYear(), startCal.getMonth() + 1, 1), -1);
                    if (end > lastDay) end.setTime(lastDay.getTime());
                    const x2 = this.calculateDayX(end) - (this.dayOfWeek.getWidth() ?? 0) / 2;
                    const bgColor = this.parent.theme.xAxesTheme.monthBgColors[startCal.getMonth()];
                    this.drawTextBox(
                        daysX - ((this.dayOfWeek.getWidth() ?? 0) / 2),
                        x2 + (this.dayOfWeek.getWidth() ?? 0) - 1,//keep 1px left for border
                        this.month.getY(), this.month.getHeight(),
                        months[startCal.getMonth()],
                        this.parent.theme.xAxesTheme.monthTextColor,
                        bgColor,
                        this.parent.theme.xAxesTheme.monthBorderColor,
                        this.month.getFont(), false, svgGroup, viewportWidth,
                    );
                    monthWasDrawn = true;
                }
                // Phase 2: WEEK
                else if (CalendarSize.YEARS === this.calendarSize && phase === 2 &&
                    (startCal.getDay() === 1 || !firstWeekWasDrawn) && this.isWeekVisible()) {
                    const end = DateUtils.getWeekSunday(startCal);
                    if (end > lastDay) end.setTime(lastDay.getTime());
                    const x2 = this.calculateDayX(end) - (this.dayOfWeek.getWidth() ?? 0) / 2;
                    const calendarWeek = this.isDayOfWeekVisible()
                        ? 'W' + DateUtils.getWeekOfYear(currentDay)
                        : String(currentDay.getDate());
                    this.drawTextBox(
                        daysX - ((this.dayOfWeek.getWidth() ?? 0) / 2),
                        x2 + (this.dayOfWeek.getWidth() ?? 0) - 1,//keep 1px left for border
                        this.week.getY(), this.week.getHeight(),
                        calendarWeek,
                        this.parent.theme.xAxesTheme.weekTextColor,
                        this.parent.theme.xAxesTheme.weekBgColor,
                        this.parent.theme.xAxesTheme.weekBorderColor,
                        this.week.getFont(), false, svgGroup, viewportWidth,
                    );
                    firstWeekWasDrawn = true;
                }
                // Phase 1: DAY OF MONTH / DAY OF WEEK
                else if (phase === 1) {
                    if (this.isDayOfMonthVisible()) {
                        const domBgColor = GraphColorUtil.getDayOfMonthBgColor(this.parent.theme, startCal);
                        const domTextColor = GraphColorUtil.getDayOfMonthTextColor(this.parent.theme, startCal);
                        const dw = this.dayOfMonth.getWidth() ?? 0;
                        this.drawTextBox(
                            daysX - (dw / 2), daysX - (dw / 2) + (dw - 1),//keep 1px left for border
                            this.dayOfMonth.getY(), this.dayOfMonth.getHeight(),
                            String(startCal.getDate()),
                            domTextColor, domBgColor,
                            this.parent.theme.xAxesTheme.dayOfMonthBorderColor,
                            this.dayOfMonth.getFont(), true, svgGroup, viewportWidth,
                        );
                    }
                    if (this.isDayOfWeekVisible()) {
                        const dowColor = GraphColorUtil.getDayOfWeekBgColor(this.parent.theme, startCal);
                        const dowTextColor = GraphColorUtil.getDayOfWeekTextColor(this.parent.theme, startCal);
                        const dowW = this.dayOfWeek.getWidth() ?? 0;
                        this.drawTextBox(
                            daysX - (dowW / 2), daysX - (dowW / 2) + (dowW - 1),//keep 1px left for border
                            this.dayOfWeek.getY(), this.dayOfWeek.getHeight(),
                            weekDays[startCal.getDay()],
                            dowTextColor, dowColor,
                            this.parent.theme.xAxesTheme.dayOfWeekBorderColor,
                            this.dayOfWeek.getFont(), true, svgGroup, viewportWidth,
                        );
                    }
                }
                // Phase 0: DAY BARS and MILESTONE BACKGROUND
                else if (phase === 0) {
                    if (drawDays && this.isDayBarsVisible()) {
                        this.parent.drawDayBars(svgGroup, currentDay);
                    }
                    if (this.milestonesVisible()) {
                        const color = GraphColorUtil.getDayOfWeekBgColor(this.parent.theme, startCal);
                        const textColor2 = GraphColorUtil.getDayOfWeekTextColor(this.parent.theme, startCal);
                        const dowW = this.dayOfWeek.getWidth() ?? 0;
                        this.drawTextBox(
                            daysX - (dowW / 2), daysX - (dowW / 2) + (dowW - 1),//keep 1px left for border
                            this.milestone.flagY, this.milestone.flagHeight,
                            null, textColor2, color,
                            this.parent.theme.xAxesTheme.dayOfWeekBorderColor,
                            null, true, svgGroup, viewportWidth,
                        );
                    }
                }

                currentDay = DateUtils.addDay(currentDay, 1);
            }
        }
    }

    /**
     * Compatibility wrapper called by GanttRenderer.draw().
     * Mirrors the 7-argument signature used there.
     */
    // draw(
    //     svgGroup: SVGElement,
    //     _chartStart: Date,
    //     _totalDays: number,
    //     dayWidth: number,
    //     _scrollOffset: number,
    //     viewportWidth: number,
    //     _milestones: Milestones,
    // ): void {
    //     this.initSize(viewportWidth, dayWidth, this.calendarAtBottom, this.calendarSize);
    //     this.drawCalendar(svgGroup, false, viewportWidth);
    //     this.drawMilestones(svgGroup);
    // }

    drawTextBox(
        x1: number, x2: number,
        y1: number, cellHeight: number,
        text: string | null,
        textColor: number | null,
        backgroundColor: number | null,
        borderColor: number | null,
        font: FontSpec | null,
        centered: boolean,
        svgGroup: SVGElement,
        viewportWidth: number,
    ): void {
        if (SvgUtils.isClipped(x1, x2, viewportWidth))
            return;
        const cellWidth = x2 - x1 + 1;
        const group = svgGroup.appendChild(SvgUtils.createGroup(x1, y1));
        group.appendChild(SvgUtils.createRect(0, 0, cellWidth - 1, cellHeight - 1, {fill: ColorUtils.intToHex(backgroundColor)}));//leave 1px for border right and bottom

        if (borderColor && cellWidth > 1) {
            group.appendChild(SvgUtils.createLine(cellWidth - 1 + 0.5, 0, cellWidth - 1 + 0.5, cellHeight - 1, {
                stroke: ColorUtils.intToHex(borderColor),
                'stroke-width': '1',
            }));
        }

        if (text && font) {
            const fontSize = font && 'size' in font ? String(font.size) : '10';
            const maxAscent = font.maxAscent;
            const textX = centered ? (cellWidth - 1) / 2 : 2;
            group.appendChild(SvgUtils.createText(textX, (cellHeight - 1) / 2 + maxAscent / 2, text, {
                fill: ColorUtils.intToHex(textColor),
                'font-size': fontSize,
                'font-family': 'sans-serif',
                'text-anchor': centered ? 'middle' : 'start',
                'font-weight': font.weight,
            }));
        }
    }

    /** Calculate X position for a given date. Mirrors Java: protected int calculateDayX(LocalDate date). */
    calculateDayX(date: Date): number {
        if (!this.parent.milestones.firstMilestone)
            return 0;
        const firstMilestoneX = this.x + (this.dayOfWeek.getWidth() ?? 0) / 2;
        return firstMilestoneX
            + (DateUtils.calculateDays(this.parent.milestones.firstMilestone, date) - this.parent.scrollOffset + this.priRun)
            * (this.dayOfWeek.getWidth() ?? 0);
    }

    drawMilestones(svg: SVGElement): void {
        for (const milestone of this.parent.milestones.getList()) {
            const x = this.calculateDayX(milestone.time);
            this.drawMilestoneShort(
                svg, milestone, milestone.time, x,
                this.parent.theme.ganttTheme.requestMilestoneColor,
                milestone.symbol,
                !milestone.hidden,
                this.parent.theme.xAxesTheme.futureEventColor,
            );
        }
    }

    drawMilestoneShort(
        svg: SVGElement,
        m: import('./Milestone').Milestone | null,
        time: Date, x: number,
        fillColor: number | null, text: string,
        visible: boolean, flagTextColor: number | null,
    ): void {
        this.drawMilestone(svg, m, time, x, this.milestone.y, fillColor, text, visible,
            this.milestone.flagY, flagTextColor, true, true);
    }

    drawMilestone(
        parentGroup: SVGElement,
        m: import('./Milestone').Milestone | null,
        time: Date | null, x: number, y: number,
        fillColor: number | null, text: string,
        visible: boolean,
        flagY: number | null, flagTextColor: number | null,
        drawFlag: boolean, drawNowLine: boolean,
    ): void {
        const FLAG_HEIGHT = 13;
        const theme = this.parent.theme;
        const darkRed = '#cc4a31';
        const milestoneTextColor = ColorUtils.intToHex(theme.xAxesTheme?.milestoneTextColor, '#ffffff');

        if (text?.charAt(0) === 'N' && drawNowLine) {
            // now line
            parentGroup.appendChild(SvgUtils.createLine(
                x, this.parent.diagram.y,
                x, this.parent.diagram.y + this.parent.diagram.height,
                {stroke: darkRed, 'stroke-width': '2'},
            ));
            if (this.dayOfWeek.width) {
                const r = Math.max(this.dayOfWeek.width / 3, 6) / 2;
                parentGroup.appendChild(SvgUtils.createCircle(x,
                    this.calendarAtBottom
                        ? this.parent.diagram.y - r / 2
                        : this.parent.diagram.y + this.parent.diagram.height - r,
                    r, {fill: darkRed, 'shape-rendering': 'auto'},
                ));
            }
        }

        if (visible) {
            parentGroup.appendChild(SvgUtils.createRect(
                x - this.milestone.width / 2, y,
                this.milestone.width, this.milestone.height - 1,
                {fill: ColorUtils.intToHex(fillColor)},
            ));
            const textEl = SvgUtils.createText(x - 1, y + this.milestone.height / 2 + 1, text, {
                fill: milestoneTextColor,
                'font-size': '10px',
                'font-family': 'sans-serif',
                'font-weight': 'bold',
                'text-anchor': 'middle',
                'dominant-baseline': 'middle',
            });
            if (m) {
                textEl.appendChild(
                    SvgUtils.createSvgElement('title', {}, `${text} = ${m.name}\n${this._formatDateForTooltip(time)}`),
                );
            }
            parentGroup.appendChild(textEl);

            if (drawFlag && flagY != null) {
                parentGroup.appendChild(SvgUtils.createSvgElement('line', {
                    x1: x, y1: y + this.milestone.height,
                    x2: x, y2: y + this.milestone.height + 3,
                    stroke: ColorUtils.intToHex(flagTextColor), 'stroke-width': '1',
                }));
                parentGroup.appendChild(SvgUtils.createText(
                    x - this.milestone.width / 2 + 2, flagY + FLAG_HEIGHT - 5,
                    this._formatDateForFlag(time),
                    {
                        fill: ColorUtils.intToHex(flagTextColor),
                        'font-size': '11px',
                        'font-family': 'sans-serif',
                        'text-anchor': 'start'
                    },
                ));
            }
        }
    }

    initPosition(x: number, y: number): void {
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
                this.week.setY(this.isDayOfMonthVisible()
                    ? this.dayOfMonth.getY() + this.dayOfMonth.getHeight()
                    : this.dayOfWeek.getY() + this.dayOfWeek.getHeight());
            } else {
                this.week.setY(this.milestone.y + this.milestone.height);
            }
            this.month.setY(this.isWeekVisible()
                ? this.week.getY() + this.week.getHeight()
                : this.dayOfWeek.getY() + this.dayOfWeek.getHeight());
            this.year.setY(this.month.getY() + this.month.getHeight());
        } else {
            if (CalendarSize.YEARS === this.calendarSize) {
                this.year.setY(y);
                this.month.setY(this.year.getY() + this.year.getHeight());
                this.week.setY(this.month.getY() + this.month.getHeight());
                this.dayOfMonth.setY(this.week.getY() + this.week.getHeight());
                this.dayOfWeek.setY(this.isDayOfMonthVisible()
                    ? this.dayOfMonth.getY() + this.dayOfMonth.getHeight()
                    : this.week.getY() + this.week.getHeight());
                this.milestone.y = this.dayOfWeek.getY();
                this.milestone.flagY = this.dayOfWeek.getY() + this.milestone.height;
            } else {
                //year and month are not visible
                this.year.setY(y);
                this.month.setY(y);
                this.week.setY(y);
                if (this.isWeekVisible())
                    this.dayOfMonth.setY(this.week.getY() + this.week.getHeight());
                else
                    this.dayOfMonth.setY(this.week.getY());
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

    initSize(width: number, dayWidth: number, calendarAtBottom: boolean, calendarSize: CalendarSize): void {
        this.calendarAtBottom = calendarAtBottom;
        this.width = width;
        this.dayOfWeek.setWidth(dayWidth);
        this.dayOfMonth.setWidth(dayWidth);
        this.calendarSize = calendarSize;
    }

    isDayBarsVisible(): boolean {
        return (this.dayOfWeek.getWidth() ?? 0) >= 4;
    }

    isDayOfMonthVisible(): boolean {
        return (this.dayOfWeek.getWidth() ?? 0) >= DAY_OF_MONTH_MIN_DAY_WIDTH;
    }

    isDayOfWeekVisible(): boolean {
        return (this.dayOfWeek.getWidth() ?? 0) >= DAY_OF_WEEK_MIN_DAY_WIDTH;
    }

    isMonthVisible(): boolean {
        return CalendarSize.YEARS === this.calendarSize && (this.dayOfWeek.getWidth() ?? 0) >= MONTH_MIN_DAY_WIDTH;
    }

    isYearVisible(): boolean {
        return CalendarSize.YEARS === this.calendarSize;
    }

    isWeekVisible(): boolean {
        return CalendarSize.YEARS === this.calendarSize && (this.dayOfWeek.getWidth() ?? 0) >= WEEK_MIN_DAY_WIDTH;
    }

    milestonesVisible(): boolean {
        return !this.parent.milestones.empty();
    }

    private _formatDateForTooltip(date: Date | null): string {
        return date ? date.toLocaleDateString('en-US', {
            weekday: 'long',
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        }) : '';
    }

    private _formatDateForFlag(date: Date | null): string {
        const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        return date ? `${months[date.getMonth()]}.${date.getDate()}` : '';
    }
}
