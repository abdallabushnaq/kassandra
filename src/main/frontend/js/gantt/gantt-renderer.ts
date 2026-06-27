// gantt/gantt-renderer.ts
// Mirrors Java: GanttRenderer extends AbstractGanttRenderer
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import { intToHex }                                    from '../color-utils.js';
import { createSvgElement, createLine, createRect, createText } from '../svg-utils.js';
import { MS, getDayMidnight, calculateDayIndex, calculateDayCount } from '../date-utils.js';
import { Milestone }                                   from '../milestone.js';
import { Milestones }                                  from '../milestones.js';
import { Theme }                                       from '../theme/theme.js';
import { getCalendarException }                        from './date-helpers.js';
import { AbstractGanttRenderer, TaskDto, DEFAULT_DW } from './abstract-gantt-renderer.js';

export interface GanttChartMeta {
    chartStart:               string;
    chartEnd:                 string;
    now?:                     string;
    sprintEarliestStartDate:  string;
    sprintLatestFinishDate:   string;
    sprintStatus?:            string;
    sprintName?:              string;
    preRun?:                  number;
    postRun?:                 number;
    theme?:                   Record<string, unknown>;
}

export interface GanttChartDto {
    tasks: TaskDto[];
    meta:  GanttChartMeta;
}

export class GanttRenderer extends AbstractGanttRenderer {
    constructor(data: GanttChartDto, theme: Theme) {
        const chartStart    = getDayMidnight(new Date(data.meta.chartStart));
        const chartEnd      = getDayMidnight(new Date(data.meta.chartEnd));
        const now           = data.meta.now ? getDayMidnight(new Date(data.meta.now)) : getDayMidnight(new Date());
        const earliestStart = getDayMidnight(new Date(data.meta.sprintEarliestStartDate));
        const latestFinish  = getDayMidnight(new Date(data.meta.sprintLatestFinishDate));
        const sprintStatus  = data.meta.sprintStatus;

        const milestonesList: Milestone[] = [];
        if (sprintStatus !== 'CLOSED') {
            milestonesList.push(new Milestone(now,           'N', 'Now (current date)',       false));
        }
        milestonesList.push(    new Milestone(earliestStart, 'S', 'Start (Start of project)', false));
        milestonesList.push(    new Milestone(latestFinish,  'E', 'End (End of project)',     false));

        const milestones = new Milestones(
            milestonesList,
            milestonesList.length > 0 ? milestonesList[0].time : chartStart,
            milestonesList.length > 0 ? milestonesList[milestonesList.length - 1].time : chartStart,
        );

        super(theme, milestones, data.meta.preRun || 0, data.meta.postRun || 0);

        this.tasks       = data.tasks || [];
        this.chartStart  = chartStart;
        this.totalDays   = calculateDayCount(chartStart, chartEnd);
        this.currentDate = now;

        for (const task of this.tasks) {
            this._taskById[String(task.id)] = task;
        }
    }

    override calculateDayWidth(): void {
        this.dayWidth = DEFAULT_DW;
    }

    override drawDayBars(g: SVGElement, dayDate: Date, calendarH = 0): void {
        const dayIdx    = calculateDayIndex(dayDate, this.chartStart!);
        const dayLeft   = this.dayIndexToPixelX(dayIdx);
        const gridColor = intToHex(this.theme.ganttTheme.gridColor, '#e4e8f3');
        for (const task of this.tasks) {
            const rowY = calendarH + task.rowIndex * (this.getTaskHeight() + 1);
            g.appendChild(createRect(dayLeft, rowY - 1, this.dayWidth, 1, { fill: gridColor }));
            g.appendChild(createRect(dayLeft, rowY, 1, this.getTaskHeight(), { fill: gridColor }));
            const bgColor = this.getGanttDayStripeColor(task, dayDate);
            g.appendChild(createRect(dayLeft + 1, rowY, this.dayWidth - 1, this.getTaskHeight(), { fill: bgColor }));
            const ex = getCalendarException(dayDate, task.calendarExceptions);
            if (ex?.letter && this.dayWidth >= 14) {
                const cx     = dayLeft + this.dayWidth / 2;
                const letter = createText(cx, rowY + this.getTaskHeight() / 2, ex.letter, {
                    fill: intToHex(this.theme.ganttTheme.outOfOfficeColor, '#ffffff'),
                    'font-size': '22', 'font-family': 'sans-serif', 'font-weight': 'bold',
                    'text-anchor': 'middle', 'dominant-baseline': 'middle',
                });
                letter.appendChild(createSvgElement('title', {}, ex.type || 'Off-day'));
                g.appendChild(letter);
            }
        }
    }

    drawGanttChart(g: SVGElement): void {
        for (const task of this.tasks) {
            this.drawTask(g, 0, task, true, true, false, false, null, null, true);
        }
    }

    renderNowLine(totalHeight: number): SVGGElement {
        const g              = createSvgElement('g', { class: 'now-line' });
        const containerWidth = this.containerWidth;
        const nowIdx         = calculateDayIndex(this.currentDate!, this.chartStart!);
        const xPos           = this.dayIndexToPixelX(nowIdx) + this.dayWidth / 2;
        if (xPos < 0 || xPos > containerWidth) return g;
        g.appendChild(createLine(xPos, 0, xPos, totalHeight, { stroke: '#cc0000', 'stroke-width': '2' }));
        return g;
    }

    override draw(svg: SVGSVGElement, _x: number, y: number): void {
        const calendarH = this.calendarXAxes.getHeight(this.dayWidth, this.milestones.list.length > 0);
        const taskAreaH = this.tasks.length * (this.getTaskHeight() + 1);
        const totalH    = calendarH + taskAreaH;
        this._calendarH = y + calendarH;

        const gDayBars = createSvgElement('g', { class: 'day-bars' });
        const firstDay = Math.max(0, Math.floor(this.scrollOffset) - 1);
        const lastDay  = Math.min(this.totalDays - 1, firstDay + Math.ceil(this.containerWidth / this.dayWidth) + 2);
        for (let d = firstDay; d <= lastDay; d++) {
            const dayDate = new Date(this.chartStart!.getTime() + d * MS);
            this.drawDayBars(gDayBars, dayDate, this._calendarH);
        }
        svg.appendChild(gDayBars);

        this.calendarXAxes.initPosition(0, y);
        this.calendarXAxes.draw(
            svg, this.chartStart!, this.totalDays,
            this.dayWidth, this.scrollOffset, this.containerWidth, this.milestones,
        );

        const gTasks = createSvgElement('g', { class: 'tasks' });
        this.drawGanttChart(gTasks);
        svg.appendChild(gTasks);

        svg.appendChild(this.renderNowLine(y + totalH));
    }
}
