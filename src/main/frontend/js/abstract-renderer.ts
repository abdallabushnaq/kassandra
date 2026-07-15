// abstract-renderer.ts
// Base class for chart content renderers.
// Mirrors Java: AbstractRenderer
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {ColorUtils} from './color-utils.js';
import {SvgUtils} from './svg-utils.js';
import {DateUtils} from './date-utils.js';
import {CalendarXAxes} from './calendar-x-axes.js';
import {CalendarSize} from './calendar-size.js';
import {GraphSquare} from './graph-square.js';
import {GraphColorUtil} from './graph-color-util.js';
import {Theme} from './theme/theme.js';
import {Milestones} from './milestones.js';
import type {IRenderer} from './renderer-interface.js';
import {TextAlignment} from "./TextAlignment.js";
import {FontMetrics} from "./font-metrics.js";
import {FontSpec} from "./font-spec.js";

export abstract class AbstractRenderer implements IRenderer {
    protected static readonly STANDARD_LINE_STROKE_WIDTH: number = 3.1;
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
        this.chartHeight = 400;
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
        const firstMilestoneX = this.firstDayX + this.calendarXAxes.dayOfWeek.getWidth() / 2;
        return firstMilestoneX + (DateUtils.calculateDays(firstMilestoneDay, date) - this.scrollOffset + this.calendarXAxes.priRun) * (this.calendarXAxes.dayOfWeek.getWidth());
    }

    /** Override per-renderer to draw day background bars. */
    drawDayBars(g: SVGElement, currentDay: Date, _calendarH?: number): void {
        const color = GraphColorUtil.getDayOfWeekBgColor(this.theme, currentDay);
        const x = this.calculateDayX(currentDay);
        const dw = this.calendarXAxes.dayOfWeek.getWidth() ?? 0;
        g.appendChild(SvgUtils.createRect(
            x - (dw / 2), this.diagram.y, dw - 1, this.diagram.height,
            {fill: ColorUtils.intToHex(color)},
        ));
        g.appendChild(SvgUtils.createRect(
            x - (dw / 2) + (dw - 1), this.diagram.y, (dw - 1) + 1, this.diagram.height,
            {fill: ColorUtils.intToHex(this.theme.ganttTheme.gridColor)},
        ));
    }

    calculateDayWidth(): void {
        this.days = this.calculateMaxDays();
        this.calendarXAxes.dayOfWeek.setWidth(this.chartWidth / this.days);
    }

    calculateMaxDays(): number {
        return DateUtils.calculateDays(this.milestones.firstMilestone!, this.milestones.lastMilestone!)
            + 1 + this.calendarXAxes.priRun + this.calendarXAxes.postRun;
    }

    /**
     * Mirrors Java: protected void initPosition(int x, int y).
     * Positions the calendar header and the diagram (plot area) either with the calendar
     * above the diagram (default) or below it (calendarAtBottom), matching the Java layout
     * exactly so combined charts (e.g. GanttBurndownChart) can stack a burndown plot with its
     * calendar drawn at the bottom directly above the Gantt chart's own calendar.
     */
    initPosition(x: number, y: number): void {
        this.firstDayX = x;
        if (this.calendarAtBottom) {
            this.calendarXAxes.initPosition(x, y);
            this.diagram.initPosition(x, y);
            this.calendarXAxes.initPosition(x, this.diagram.y + this.diagram.height + 1);
        } else {
            this.calendarXAxes.initPosition(x, y);
            this.diagram.initPosition(x, this.calendarXAxes.year.getY() + this.calendarXAxes.getHeight());
        }
    }

    initSize(x: number, calendarAtBottom: boolean, calendarSize: CalendarSize, containerWidth: number): void {
        this.calendarAtBottom = calendarAtBottom;
        this.calendarXAxes.calendarSize = calendarSize;
        this.calculateDayWidth();
        this.chartWidth = containerWidth;
        this.chartHeight = this.calculateChartHeight();
        this.firstDayX = x;
        // if (calendarAtBottom) {
        this.calendarXAxes.initSize(this.chartWidth, this.calendarXAxes.dayOfWeek.getWidth() ?? 0, calendarAtBottom, calendarSize,);
        this.diagram.initSize(this.chartWidth - x, this.chartHeight - this.calendarXAxes.getHeight(),);
        this.calendarXAxes.initSize(this.chartWidth, this.calendarXAxes.dayOfWeek.getWidth() ?? 0, calendarAtBottom, calendarSize,);
        // } else {
        //     this.calendarXAxes.initSize(this.chartWidth, this.calendarXAxes.dayOfWeek.getWidth() ?? 0, calendarAtBottom, calendarSize,);
        //     this.diagram.initSize(this.chartWidth - x, this.chartHeight - this.calendarXAxes.getHeight(),);
        //     this.calendarXAxes.initSize(this.chartWidth, this.calendarXAxes.dayOfWeek.getWidth() ?? 0, calendarAtBottom, calendarSize,);
        // }


        // if (calendarAtBottom) {
        //     calendarXAxes.initSize(chartWidth, calendarXAxes.dayOfWeek.getWidth(), calendarAtBottom, calendarSize);
        //     diagram.initSize(chartWidth - x, chartHeight - calendarXAxes.getHeight());
        //     calendarXAxes.initSize(chartWidth, calendarXAxes.dayOfWeek.getWidth(), calendarAtBottom, calendarSize);
        // } else {
        //     calendarXAxes.initSize(chartWidth, calendarXAxes.dayOfWeek.getWidth(), calendarAtBottom, calendarSize);
        //     diagram.initSize(chartWidth - x, chartHeight - calendarXAxes.getHeight());
        //     calendarXAxes.initSize(chartWidth, calendarXAxes.dayOfWeek.getWidth(), calendarAtBottom, calendarSize);
        // }

    }

    drawGraphText(g: SVGElement, x: number, y: number, text: string, textColor: number | null, font: FontSpec, aligned: TextAlignment) {
        const fm = new FontMetrics(font);
        const textWidth = fm.stringWidth(text);
        switch (aligned) {
            case TextAlignment.left:
                g.appendChild(SvgUtils.createRect(x, y - 9 + 2, textWidth, 12, {fill: ColorUtils.intToHex(this.theme.chartTheme.graphTextBackgroundColor)}));
                // graphics2D.fillRect(x, y - 9 + 2, width, 12);
                // graphics2D.setColor(textColor);
                // graphics2D.drawString(text, x, y + 2);
                g.appendChild(SvgUtils.createText(x, y + 2, text, {
                    fill: ColorUtils.intToHex(textColor),
                    'font-size': font.size,
                    'font-family': font.family,
                    'text-anchor': 'left',
                }));
                break;
            case TextAlignment.right:
                // graphics2D.fillRect(x - width, y - 9 + 2, width, 12);
                g.appendChild(SvgUtils.createRect(x - textWidth, y - 9 + 2, textWidth, 12, {fill: ColorUtils.intToHex(textColor)}));
                // graphics2D.setColor(textColor);
                // graphics2D.drawString(text, x - width, y + 2);
                g.appendChild(SvgUtils.createText(x - textWidth, y + 2, text, {
                    fill: ColorUtils.intToHex(textColor),
                    'font-size': font.size,
                    'font-family': font.family,
                    'text-anchor': 'right',
                }));
                break;
        }

    }

    // ── Java: protected void drawLegend() / AbstractRenderer.drawLegend(x, y, interpolationColor) ──
    protected drawLegend(svg: SVGElement, x: number, y: number, interpolationColor: number | null): void {
        const lineHeight = 14;
        let legendY = y + lineHeight;
        const legendX1 = x;
        const legendX2 = legendX1 + 10;
        let legendTextY = legendY + 1;
        let legendTextX = legendX2 + 4;
        const milestoneX = legendX1 + 5;
        let milestoneY = legendY - this.calendarXAxes.milestone.height / 2;

        svg.appendChild(SvgUtils.createLine(legendX1, legendY, legendX2, legendY, {
            stroke: ColorUtils.intToHex(this.theme.chartTheme.surroundingSquareColor),
            'stroke-width': AbstractRenderer.STANDARD_LINE_STROKE_WIDTH,
            'stroke-dasharray': '3'
        }));
        this.drawGraphText(svg, legendTextX, legendTextY, "Guideline", this.theme.burndownTheme.tickTextColor, this.calendarXAxes.dayOfWeek.getFont(), TextAlignment.left);

        legendY += lineHeight;
        legendTextY += lineHeight;
        milestoneY += lineHeight;
        svg.appendChild(SvgUtils.createLine(legendX1, legendY, legendX2, legendY, {
            stroke: ColorUtils.intToHex(interpolationColor),
            'stroke-width': AbstractRenderer.STANDARD_LINE_STROKE_WIDTH
        }));
        this.drawGraphText(svg, legendTextX, legendTextY, "extrapolated release date", this.theme.burndownTheme.tickTextColor, this.calendarXAxes.dayOfWeek.getFont(), TextAlignment.left);

        legendY += lineHeight;
        legendTextY += lineHeight;
        milestoneY += lineHeight;
        svg.appendChild(SvgUtils.createLine(legendX1, legendY, legendX2, legendY, {
            stroke: ColorUtils.intToHex(this.theme.burndownTheme.borderColor),
            'stroke-width': AbstractRenderer.STANDARD_LINE_STROKE_WIDTH
        }));
        this.drawGraphText(svg, legendTextX, legendTextY, "Remaining work", this.theme.burndownTheme.tickTextColor, this.calendarXAxes.dayOfWeek.getFont(), TextAlignment.left);

        legendY += lineHeight;
        legendTextY += lineHeight;
        milestoneY += lineHeight;
        this.calendarXAxes.drawMilestone(svg, null, null, milestoneX, milestoneY, this.theme.xAxesTheme.pastEventColor, "S", true, null, null, false, false);// start
        this.drawGraphText(svg, legendTextX, legendTextY, "Start date (sprint)", this.theme.burndownTheme.tickTextColor, this.calendarXAxes.dayOfWeek.getFont(), TextAlignment.left);

        legendY += lineHeight;
        legendTextY += lineHeight;
        milestoneY += lineHeight;
        this.calendarXAxes.drawMilestone(svg, null, null, milestoneX, milestoneY, this.theme.xAxesTheme.nowEventColor, "N", true, null, null, false, false);// now
        this.drawGraphText(svg, legendTextX, legendTextY, "Now date", this.theme.burndownTheme.tickTextColor, this.calendarXAxes.dayOfWeek.getFont(), TextAlignment.left);

        legendY += lineHeight;
        legendTextY += lineHeight;
        milestoneY += lineHeight;
        this.calendarXAxes.drawMilestone(svg, null, null, milestoneX, milestoneY, this.theme.burndownTheme.delayEventColor, "R", true, null, null, false, false);// release
        this.drawGraphText(svg, legendTextX, legendTextY, "Release date", this.theme.burndownTheme.tickTextColor, this.calendarXAxes.dayOfWeek.getFont(), TextAlignment.left);

        legendY += lineHeight;
        legendTextY += lineHeight;
        milestoneY += lineHeight;
        this.calendarXAxes.drawMilestone(svg, null, null, milestoneX, milestoneY, this.theme.xAxesTheme.futureEventColor, "E", true, null, null, false, false);// end
        this.drawGraphText(svg, legendTextX, legendTextY, "End date (sprint)", this.theme.burndownTheme.tickTextColor, this.calendarXAxes.dayOfWeek.getFont(), TextAlignment.left);

        legendY += lineHeight;
        legendTextY += lineHeight;
        milestoneY += lineHeight;
        this.calendarXAxes.drawMilestone(svg, null, null, milestoneX, milestoneY, this.theme.xAxesTheme.futureEventColor, "F", true, null, null, false, false);// first
        this.drawGraphText(svg, legendTextX, legendTextY, "First punch-in", this.theme.burndownTheme.tickTextColor, this.calendarXAxes.dayOfWeek.getFont(), TextAlignment.left);

        legendY += lineHeight;
        legendTextY += lineHeight;
        milestoneY += lineHeight;
        this.calendarXAxes.drawMilestone(svg, null, null, milestoneX, milestoneY, this.theme.xAxesTheme.futureEventColor, "L", true, null, null, false, false);// Last
        this.drawGraphText(svg, legendTextX, legendTextY, "Last punch-out", this.theme.burndownTheme.tickTextColor, this.calendarXAxes.dayOfWeek.getFont(), TextAlignment.left);
    }

    protected calculateDayIndex(date: Date): number {
        const firstMilestoneDay = this.milestones.firstMilestone;
        return DateUtils.calculateDays(firstMilestoneDay, date);
    }

    protected drawCalendar(g: SVGElement, drawDays: boolean = true, viewportWidth: number) {
        this.calendarXAxes.drawCalendar(g, drawDays, viewportWidth);
    }


}

