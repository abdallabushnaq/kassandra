// abstract-renderer.ts
// Base class for chart content renderers.
// Mirrors Java: AbstractRenderer
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {intToHex} from './color-utils.js';
import {createRect} from './svg-utils.js';
import {calculateDays} from './date-utils.js';
import {CalendarXAxes} from './calendar-x-axes.js';
import {CalendarSize} from './calendar-size.js';
import {GraphSquare} from './graph-square.js';
import {GraphColorUtil} from './graph-color-util.js';
import {Theme} from './theme/theme.js';
import {Milestones} from './milestones.js';
import type {IRenderer} from './renderer-interface.js';

export abstract class AbstractRenderer implements IRenderer {
    chartWidth: number;
    chartHeight: number;
    theme: Theme;
    milestones: Milestones;
    calendarXAxes: CalendarXAxes;
    days: number;
    firstDayX: number;
    scrollOffset: number;
    preRun: number;
    postRun: number;
    diagram: GraphSquare;
    calendarAtBottom: boolean;

    constructor(theme: Theme, milestones: Milestones, preRun: number, postRun: number) {
        this.chartWidth = 0;
        this.chartHeight = 0;
        this.theme = theme;
        this.milestones = milestones;
        this.days = 3;
        this.firstDayX = 0;
        this.scrollOffset = 0;
        this.preRun = preRun;
        this.postRun = postRun;
        this.calendarAtBottom = false;
        this.diagram = new GraphSquare();
        this.calendarXAxes = new CalendarXAxes(this, preRun, postRun);
    }

    calculateChartHeight(): number {
        return this.chartHeight;
    }

    calculateChartWidth(): number {
        return this.chartWidth;
    }

    /** Abstract – draw chart content into the SVG. */
    draw(_svg: SVGSVGElement, _x: number, _y: number): void { /* to be overridden */
    }

    drawMilestones(svg: SVGElement): void {
        this.calendarXAxes.drawMilestones(svg);
    }

    calculateDayX(date: Date): number {
        const firstMilestoneDay = this.milestones.firstMilestone!;
        const firstMilestoneX = this.firstDayX + (this.calendarXAxes.dayOfWeek.getWidth() ?? 0) / 2;
        return firstMilestoneX
            + (calculateDays(firstMilestoneDay, date) - this.scrollOffset + this.calendarXAxes.priRun)
            * (this.calendarXAxes.dayOfWeek.getWidth() ?? 0);
    }

    /** Override per-renderer to draw day background bars. */
    drawDayBars(g: SVGElement, currentDay: Date, _calendarH?: number): void {
        const color = GraphColorUtil.getDayOfWeekBgColor(this.theme, currentDay);
        const x = this.calculateDayX(currentDay);
        const dw = this.calendarXAxes.dayOfWeek.getWidth() ?? 0;
        g.appendChild(createRect(
            x - (dw / 2 - 1), this.diagram.y, dw - 1, this.diagram.height,
            {fill: intToHex(color)},
        ));
        g.appendChild(createRect(
            x - (dw / 2 - 1) + (dw - 1), this.diagram.y, (dw - 1) + 1, this.diagram.height,
            {fill: intToHex(this.theme.ganttTheme.gridColor)},
        ));
    }

    calculateDayWidth(): void {
        this.days = this.calculateMaxDays();
        this.calendarXAxes.dayOfWeek.setWidth(this.chartWidth / this.days);
    }

    calculateMaxDays(): number {
        return calculateDays(this.milestones.firstMilestone!, this.milestones.lastMilestone!)
            + 1 + this.calendarXAxes.priRun + this.calendarXAxes.postRun;
    }

    initSize(x: number, calendarAtBottom: boolean, calendarSize: CalendarSize, containerWidth: number): void {
        this.calendarAtBottom = calendarAtBottom;
        this.calendarXAxes.calendarSize = calendarSize;
        this.calculateDayWidth();
        this.chartWidth = containerWidth;
        this.chartHeight = this.calculateChartHeight();
        this.firstDayX = x;
        this.calendarXAxes.initSize(
            this.chartWidth,
            this.calendarXAxes.dayOfWeek.getWidth() ?? 0,
            calendarAtBottom,
            calendarSize,
        );
        this.diagram.initSize(
            this.chartWidth - x,
            this.chartHeight - this.calendarXAxes.getHeight(this.calendarXAxes.dayOfWeek.getWidth() ?? 0, false),
        );
    }
}

