// calendar-x-axes.ts
// Virtual-canvas calendar header renderer.
// Mirrors Java: CalendarXAxes
// Row order (top→bottom): year → month → [week] → [dayOfMonth → dayOfWeek] → [milestones]
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import { intToHex, convertSprintColorToRgba }                       from './color-utils.js';
import { createSvgElement, createLine, createCircle, createRect, createText } from './svg-utils.js';
import { calculateDays, addDay, maxDate, getWeekSunday, getWeekOfYear }       from './date-utils.js';
import { GraphColorUtil }                                             from './graph-color-util.js';
import { CalendarElement, FontSpec }                                  from './calendar-element.js';
import { CalendarMilestoneElement }                                   from './calendar-milestone-element.js';
import { CalendarSize }                                               from './calendar-size.js';
import type { IRenderer }                                             from './renderer-interface.js';
import type { Milestones }                                            from './milestones.js';
import type { Theme }                                                 from './theme/theme.js';

const DAY_OF_MONTH_MIN_DAY_WIDTH = 16;
const DAY_OF_WEEK_MIN_DAY_WIDTH  = 10;
const MONTH_MIN_DAY_WIDTH        = 1;
const WEEK_MIN_DAY_WIDTH         = 2;

export class CalendarXAxes {
    parent:       IRenderer;
    priRun:       number;
    postRun:      number;
    milestones:   Milestones;
    theme:        Theme;
    year:         CalendarElement;
    month:        CalendarElement;
    week:         CalendarElement;
    dayOfMonth:   CalendarElement;
    dayOfWeek:    CalendarElement;
    milestone:    CalendarMilestoneElement;
    calendarAtBottom: boolean;
    calendarSize: CalendarSize;
    width:        number;
    x:            number;

    constructor(parent: IRenderer, priRun: number, postRun: number) {
        this.parent     = parent;
        this.priRun     = priRun;
        this.postRun    = postRun;
        this.milestones = parent.milestones;
        this.theme      = parent.theme;

        const margin = 4;
        this.year = new CalendarElement(
            { family: 'sans-serif', size: 14, weight: 'normal' }, null, null, 13 + margin);
        this.month = new CalendarElement(
            { family: 'sans-serif', size: 12, weight: 'normal' }, null, null, 12 + margin);
        this.week = new CalendarElement(
            { family: 'sans-serif', size: 10, weight: 'normal' }, null, null, 10 + margin);
        this.dayOfMonth = new CalendarElement(
            { family: 'sans-serif', size: 10, weight: 'bold' }, null, 20, 10 + margin);
        this.dayOfWeek = new CalendarElement(
            { family: 'sans-serif', size: 10, weight: 'bold' }, null, 20, 10 + margin);
        this.milestone = new CalendarMilestoneElement(
            null, null, 11, 10 + margin,
            { family: 'sans-serif', size: 10, weight: 'bold' },
            { family: 'sans-serif', size: 11, weight: 'normal' },
            13,
        );

        this.calendarAtBottom = false;
        this.calendarSize     = CalendarSize.YEARS;
        this.width            = 0;
        this.x                = 0;
    }

    /**
     * Calculates total pixel height of the calendar header for a given day width.
     * Mirrors Java: CalendarXAxes.getHeight()
     */
    getHeight(_dayWidth: number, _hasMilestones: boolean): number {
        let height = this.year.getHeight();
        if (this.isMonthVisible())      height += this.month.getHeight();
        if (this.isDayOfMonthVisible()) height += this.dayOfMonth.getHeight();
        if (this.isDayOfWeekVisible())  height += this.dayOfWeek.getHeight();
        if (this.isWeekVisible())       height += this.week.getHeight();
        if (this.milestonesVisible())   height += this.milestone.getHeight() + this.milestone.getHeight();
        return height;
    }

    drawCalendar(drawDays: boolean, svgGroup: SVGElement, viewportWidth: number): void {
        if (!this.parent) return;

        const firstDay = addDay(this.milestones.firstMilestone!, -this.priRun);
        const lastDay  = maxDate(
            this.milestones.lastMilestone!,
            addDay(this.milestones.firstMilestone!, this.parent.days - 1),
        );

        let yearWasDrawn      = false;
        let monthWasDrawn     = false;
        let firstWeekWasDrawn = false;

        const weekDays = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];
        const months   = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

        for (let phase = 0; phase < 5; phase++) {
            let currentDay = new Date(firstDay);

            while (currentDay <= lastDay) {
                const daysX    = this.calculateDayX(currentDay);
                const startCal = new Date(currentDay);

                // Phase 4: YEAR
                if (CalendarSize.YEARS === this.calendarSize && phase === 4 &&
                    ((startCal.getDate() === 1 && startCal.getMonth() === 0) || !yearWasDrawn)) {
                    const end = new Date(startCal.getFullYear(), 11, 31);
                    if (end > lastDay) end.setTime(lastDay.getTime());
                    const x2 = this.calculateDayX(end) - (this.dayOfWeek.getWidth() ?? 0) / 2;
                    this.drawTextBox(
                        daysX - ((this.dayOfWeek.getWidth() ?? 0) / 2 - 1),
                        x2 + (this.dayOfWeek.getWidth() ?? 0),
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
                    const end = addDay(new Date(startCal.getFullYear(), startCal.getMonth() + 1, 1), -1);
                    if (end > lastDay) end.setTime(lastDay.getTime());
                    const x2      = this.calculateDayX(end) - (this.dayOfWeek.getWidth() ?? 0) / 2;
                    const bgColor = this.parent.theme.xAxesTheme.monthBgColors[startCal.getMonth()];
                    this.drawTextBox(
                        daysX - ((this.dayOfWeek.getWidth() ?? 0) / 2 - 1),
                        x2 + (this.dayOfWeek.getWidth() ?? 0),
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
                    const end = getWeekSunday(startCal);
                    if (end > lastDay) end.setTime(lastDay.getTime());
                    const x2           = this.calculateDayX(end) - (this.dayOfWeek.getWidth() ?? 0) / 2;
                    const calendarWeek = this.isDayOfWeekVisible()
                        ? 'W' + getWeekOfYear(currentDay)
                        : String(currentDay.getDate());
                    this.drawTextBox(
                        daysX - ((this.dayOfWeek.getWidth() ?? 0) / 2 - 1),
                        x2 + (this.dayOfWeek.getWidth() ?? 0),
                        this.week.getY(), this.week.getHeight() - 1,
                        calendarWeek,
                        this.parent.theme.xAxesTheme.weekTextColor,
                        this.parent.theme.xAxesTheme.weekBgColor,
                        this.parent.theme.xAxesTheme.weekBorderColor,
                        this.week.getFont(), false, svgGroup, viewportWidth,
                    );
                    firstWeekWasDrawn = true;
                }
                // Phase 1: DAY OF MONTH / DAY OF WEEK
                else if (phase === 1 && this.isDayOfWeekVisible()) {
                    const domBgColor   = GraphColorUtil.getDayOfMonthBgColor(this.parent.theme, startCal);
                    const domTextColor = GraphColorUtil.getDayOfMonthTextColor(this.parent.theme, startCal);
                    const dw           = this.dayOfMonth.getWidth() ?? 0;
                    this.drawTextBox(
                        daysX - (dw / 2 - 1), daysX - (dw / 2 - 1) + (dw - 1),
                        this.dayOfMonth.getY(), this.dayOfMonth.getHeight(),
                        String(startCal.getDate()),
                        domTextColor, domBgColor,
                        this.parent.theme.xAxesTheme.dayOfMonthBorderColor,
                        this.dayOfMonth.getFont(), true, svgGroup, viewportWidth,
                    );

                    const dowColor     = GraphColorUtil.getDayOfWeekBgColor(this.parent.theme, startCal);
                    const dowTextColor = GraphColorUtil.getDayOfWeekTextColor(this.parent.theme, startCal);
                    const dowW         = this.dayOfWeek.getWidth() ?? 0;
                    this.drawTextBox(
                        daysX - (dowW / 2 - 1), daysX - (dowW / 2 - 1) + (dowW - 1),
                        this.dayOfWeek.getY(), this.dayOfWeek.getHeight(),
                        weekDays[startCal.getDay()],
                        dowTextColor, dowColor,
                        this.parent.theme.xAxesTheme.dayOfWeekBorderColor,
                        this.dayOfWeek.getFont(), true, svgGroup, viewportWidth,
                    );
                }
                // Phase 0: DAY BARS and MILESTONE BACKGROUND
                else if (phase === 0) {
                    if (drawDays && this.isDayBarsVisible()) {
                        this.parent.drawDayBars(svgGroup, currentDay);
                    }
                    if (this.milestonesVisible()) {
                        const color      = GraphColorUtil.getDayOfWeekBgColor(this.parent.theme, startCal);
                        const textColor2 = GraphColorUtil.getDayOfWeekTextColor(this.parent.theme, startCal);
                        const dowW       = this.dayOfWeek.getWidth() ?? 0;
                        this.drawTextBox(
                            daysX - (dowW / 2 - 1), daysX - (dowW / 2 - 1) + (dowW - 1),
                            this.milestone.flagY, this.milestone.flagHeight,
                            null, textColor2, color,
                            this.parent.theme.xAxesTheme.dayOfWeekBorderColor,
                            null, true, svgGroup, viewportWidth,
                        );
                    }
                }

                currentDay = addDay(currentDay, 1);
            }
        }
    }

    /**
     * Compatibility wrapper called by GanttRenderer.draw().
     * Mirrors the 7-argument signature used there.
     */
    draw(
        svgGroup:      SVGElement,
        _chartStart:   Date,
        _totalDays:    number,
        dayWidth:      number,
        _scrollOffset: number,
        viewportWidth: number,
        _milestones:   Milestones,
    ): void {
        this.initSize(viewportWidth, dayWidth, this.calendarAtBottom, this.calendarSize);
        this.drawCalendar(false, svgGroup, viewportWidth);
        this.drawMilestones(svgGroup);
    }

    drawTextBox(
        x1: number, x2: number,
        y1: number, height: number,
        text: string | null,
        textColor: number | null | undefined,
        backgroundColor: number | null | undefined,
        borderColor: number | null | undefined,
        font: FontSpec | null | undefined,
        centered: boolean,
        svgGroup: SVGElement,
        viewportWidth: number,
    ): void {
        if (x1 + (x2 - x1) <= 0 || x1 >= (viewportWidth || 9999)) return;
        const cellWidth = x2 - x1;
        svgGroup.appendChild(createRect(x1, y1, cellWidth, height - 1, { fill: intToHex(backgroundColor) }));

        if (borderColor && cellWidth > 1) {
            svgGroup.appendChild(createLine(x2, y1, x2, y1 + height - 1, {
                stroke:         intToHex(borderColor),
                'stroke-width': '1',
            }));
        }

        if (text) {
            const fontSize = font && 'size' in font ? String(font.size) : '10';
            const textX    = centered ? x1 + cellWidth / 2 : x1 + 2;
            svgGroup.appendChild(createText(textX, y1 + height - 4, text, {
                fill:          intToHex(textColor),
                'font-size':   fontSize,
                'font-family': 'sans-serif',
                'text-anchor': centered ? 'middle' : 'start',
            }));
        }
    }

    /** Calculate X position for a given date. Mirrors Java: protected int calculateDayX(LocalDate date). */
    calculateDayX(date: Date): number {
        if (!this.milestones.firstMilestone) return 0;
        const firstMilestoneX = this.x + (this.dayOfWeek.getWidth() ?? 0) / 2;
        return firstMilestoneX
            + (calculateDays(this.milestones.firstMilestone, date) - this.parent.scrollOffset + this.priRun)
            * (this.dayOfWeek.getWidth() ?? 0);
    }

    drawMilestones(svg: SVGElement): void {
        for (const milestone of this.milestones.getList()) {
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
        svg: SVGElement, m: import('./milestone').Milestone | null,
        time: Date, x: number,
        fillColor: number | null, text: string,
        visible: boolean, flagTextColor: number | null,
    ): void {
        this.drawMilestone(svg, m, time, x, this.milestone.y, fillColor, text, visible,
            this.milestone.flagY, flagTextColor, true, true);
    }

    drawMilestone(
        parentGroup: SVGElement,
        m: import('./milestone').Milestone | null,
        time: Date, x: number, y: number,
        fillColor: number | null, text: string,
        visible: boolean,
        flagY: number | null, flagTextColor: number | null,
        drawFlag: boolean, drawNowLine: boolean,
    ): void {
        const MILESTONE_WIDTH  = 11;
        const MILESTONE_HEIGHT = 13;
        const FLAG_HEIGHT      = 13;
        const theme            = this.theme;
        const darkRed          = '#8B0000';
        const milestoneTextColor = intToHex(theme.xAxesTheme?.milestoneTextColor, '#ffffff');

        if (text?.charAt(0) === 'N' && drawNowLine) {
            parentGroup.appendChild(createLine(
                x, this.parent.diagram.y,
                x, this.parent.diagram.y + this.parent.diagram.height,
                { stroke: darkRed, 'stroke-width': '2' },
            ));
            const r = 3;
            parentGroup.appendChild(createCircle(x + 1,
                this.calendarAtBottom
                    ? this.parent.diagram.y - r / 2
                    : this.parent.diagram.y + this.parent.diagram.height - r,
                r, { fill: darkRed },
            ));
        }

        if (visible) {
            parentGroup.appendChild(createRect(
                x - MILESTONE_WIDTH / 2, y,
                MILESTONE_WIDTH, MILESTONE_HEIGHT - 1,
                { fill: intToHex(fillColor) },
            ));
            const textEl = createText(x - 1, y + MILESTONE_HEIGHT / 2 + 1, text, {
                fill:                milestoneTextColor,
                'font-size':         '10px',
                'font-family':       'sans-serif',
                'font-weight':       'bold',
                'text-anchor':       'middle',
                'dominant-baseline': 'middle',
            });
            if (m) {
                textEl.appendChild(
                    createSvgElement('title', {}, `${text} = ${m.name}\n${this._formatDateForTooltip(time)}`),
                );
            }
            parentGroup.appendChild(textEl);

            if (drawFlag && flagY != null) {
                parentGroup.appendChild(createSvgElement('line', {
                    x1: x, y1: y + MILESTONE_HEIGHT,
                    x2: x, y2: y + MILESTONE_HEIGHT + 3,
                    stroke: intToHex(flagTextColor), 'stroke-width': '1',
                }));
                parentGroup.appendChild(createText(
                    x - MILESTONE_WIDTH / 2 + 2, flagY + FLAG_HEIGHT - 5,
                    this._formatDateForFlag(time),
                    { fill: intToHex(flagTextColor), 'font-size': '11px', 'font-family': 'sans-serif', 'text-anchor': 'start' },
                ));
            }
        }
    }

    initPosition(x: number, y: number): void {
        this.x = x;
        if (this.calendarAtBottom) {
            this.milestone.flagY = y;
            this.milestone.y     = this.milestone.flagY + this.milestone.flagHeight;
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
                this.milestone.y     = this.dayOfWeek.getY();
                this.milestone.flagY = this.dayOfWeek.getY() + this.milestone.height;
            } else {
                this.year.setY(y);
                this.month.setY(y);
                this.week.setY(y);
                this.dayOfMonth.setY(this.week.getY() + this.week.getHeight());
                this.dayOfWeek.setY(this.isDayOfMonthVisible()
                    ? this.dayOfMonth.getY() + this.dayOfMonth.getHeight()
                    : this.week.getY() + this.week.getHeight());
                this.milestone.y     = this.dayOfWeek.getY();
                this.milestone.flagY = this.dayOfWeek.getY() + this.milestone.height;
            }
        }
    }

    initSize(width: number, dayWidth: number, calendarAtBottom: boolean, calendarSize: CalendarSize): void {
        this.calendarAtBottom = calendarAtBottom;
        this.width            = width;
        this.dayOfWeek.setWidth(dayWidth);
        this.dayOfMonth.setWidth(dayWidth);
        this.calendarSize     = calendarSize;
    }

    isDayBarsVisible():    boolean { return (this.dayOfWeek.getWidth() ?? 0) >= 4; }
    isDayOfMonthVisible(): boolean { return (this.dayOfWeek.getWidth() ?? 0) >= DAY_OF_MONTH_MIN_DAY_WIDTH; }
    isDayOfWeekVisible():  boolean { return (this.dayOfWeek.getWidth() ?? 0) >= DAY_OF_WEEK_MIN_DAY_WIDTH; }
    isMonthVisible():      boolean { return CalendarSize.YEARS === this.calendarSize && (this.dayOfWeek.getWidth() ?? 0) >= MONTH_MIN_DAY_WIDTH; }
    isWeekVisible():       boolean { return CalendarSize.YEARS === this.calendarSize && (this.dayOfWeek.getWidth() ?? 0) >= WEEK_MIN_DAY_WIDTH; }
    milestonesVisible():   boolean { return !this.milestones.empty(); }

    private _formatDateForTooltip(date: Date): string {
        return date.toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
    }

    private _formatDateForFlag(date: Date): string {
        const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        return `${months[date.getMonth()]}.${date.getDate()}`;
    }
}

