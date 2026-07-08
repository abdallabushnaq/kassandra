// gantt/gantt-renderer.ts
// Mirrors Java: GanttRenderer extends AbstractGanttRenderer
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {ColorUtils} from '../color-utils.js';
import {SvgUtils} from '../svg-utils.js';
import {DateUtils} from '../date-utils.js';
import {Milestone} from '../milestone.js';
import {Milestones} from '../milestones.js';
import {Theme} from '../theme/theme.js';
import {getCalendarException} from './date-helpers.js';
import {AbstractGanttRenderer, DEFAULT_DW, TaskDto} from './abstract-gantt-renderer.js';
import {CalendarSize} from "Frontend/js/calendar-size";

export interface GanttChartMeta {
    chartStart: string;
    chartEnd: string;
    now?: string;
    sprintEarliestStartDate: string;
    sprintLatestFinishDate: string;
    sprintStatus?: string;
    sprintName?: string;
    preRun?: number;
    postRun?: number;
    calendarSize: CalendarSize;
    theme?: Record<string, unknown>;
}

export interface GanttChartDto {
    tasks: TaskDto[];
    meta: GanttChartMeta;
}


export class GanttRenderer extends AbstractGanttRenderer {
    static GANTT_TASK_POST_SPACE: number = 0;
    static GANTT_TASK_PRI_SPACE: number = 0;
    calendarSize: CalendarSize;

    constructor(data: GanttChartDto, theme: Theme, preRun: number, postRun: number) {
        const chartStart = DateUtils.getDayMidnight(new Date(data.meta.chartStart));
        const chartEnd = DateUtils.getDayMidnight(new Date(data.meta.chartEnd));
        const now = data.meta.now ? DateUtils.getDayMidnight(new Date(data.meta.now)) : DateUtils.getDayMidnight(new Date());
        const earliestStart = DateUtils.getDayMidnight(new Date(data.meta.sprintEarliestStartDate));
        const latestFinish = DateUtils.getDayMidnight(new Date(data.meta.sprintLatestFinishDate));
        const sprintStatus = data.meta.sprintStatus;

        const milestonesList: Milestone[] = [];
        if (sprintStatus !== 'CLOSED') {
            milestonesList.push(new Milestone(now, 'N', 'Now (current date)', false));
        }
        milestonesList.push(new Milestone(earliestStart, 'S', 'Start (Start of project)', false));
        milestonesList.push(new Milestone(latestFinish, 'E', 'End (End of project)', false));

        const milestones = new Milestones(
            milestonesList,
            milestonesList.length > 0 ? milestonesList[0].time : chartStart,
            milestonesList.length > 0 ? milestonesList[milestonesList.length - 1].time : chartStart,
        );

        super(theme, milestones, data.meta.preRun || 0, data.meta.postRun || 0);

        this.tasks = data.tasks || [];
        this.chartStart = chartStart;
        this.totalDays = DateUtils.calculateDayCount(chartStart, chartEnd);
        this.currentDate = now;
        this.calendarSize = data.meta.calendarSize;

        for (const task of this.tasks) {
            this._taskById[String(task.id)] = task;
        }
    }

    override calculateDayWidth(): void {
        this.dayWidth = DEFAULT_DW;
    }


    calculateNumberOfTasks(tasks: TaskDto[]): number {
        let size = 0;
        for (const task of tasks) {
            // if (this.isValidTask(task)) {
            size++;
            // }
        }
        return size;
    }

    override calculateChartHeight(): number {
        return this.calendarXAxes.getHeight() + GanttRenderer.GANTT_TASK_PRI_SPACE + this.calculateNumberOfTasks(this.tasks) * (this.getTaskHeight() + 1) + GanttRenderer.GANTT_TASK_POST_SPACE;
    }

    override drawDayBars(g: SVGElement, dayDate: Date, calendarH = 0): void {
        const dayIdx = DateUtils.calculateDayIndex(dayDate, this.chartStart!);
        const dayLeft = this.dayIndexToPixelX(dayIdx);
        const gridColor = ColorUtils.intToHex(this.theme.ganttTheme.gridColor, '#e4e8f3');
        for (const task of this.tasks) {
            const rowY = calendarH + task.rowIndex * (this.getTaskHeight() + 1);
            //grid
            g.appendChild(SvgUtils.createRect(dayLeft, rowY - 1, this.dayWidth, 1, {fill: gridColor}));
            g.appendChild(SvgUtils.createRect(dayLeft, rowY, 1, this.getTaskHeight(), {fill: gridColor}));
            const bgColor = this.getGanttDayStripeColor(task, dayDate);
            //background
            g.appendChild(SvgUtils.createRect(dayLeft + 1, rowY, this.dayWidth - 1, this.getTaskHeight(), {fill: bgColor}));
            const ex = getCalendarException(dayDate, task.calendarExceptions);
            if (ex?.letter && this.dayWidth >= 14) {
                const cx = dayLeft + this.dayWidth / 2;
                const letter = SvgUtils.createText(cx, rowY + this.getTaskHeight() / 2, ex.letter, {
                    fill: ColorUtils.intToHex(this.theme.ganttTheme.outOfOfficeColor, '#ffffff'),
                    'font-size': '22', 'font-family': 'sans-serif', 'font-weight': 'bold',
                    'text-anchor': 'middle', 'dominant-baseline': 'middle',
                });
                letter.appendChild(SvgUtils.createSvgElement('title', {}, ex.type || 'Off-day'));
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
        const g = SvgUtils.createSvgElement('g', {class: 'now-line'});
        const containerWidth = this.containerWidth;
        const nowIdx = DateUtils.calculateDayIndex(this.currentDate!, this.chartStart!);
        const xPos = this.dayIndexToPixelX(nowIdx) + this.dayWidth / 2;
        if (xPos < 0 || xPos > containerWidth) return g;
        g.appendChild(SvgUtils.createLine(xPos, 0, xPos, totalHeight, {stroke: '#cc0000', 'stroke-width': '2'}));
        return g;
    }

    override draw(svg: SVGSVGElement, _x: number, y: number): void {
        const calendarH = this.calendarXAxes.getHeight();
        const taskAreaH = this.tasks.length * (this.getTaskHeight() + 1);
        const totalH = calendarH + taskAreaH;
        this._calendarH = y + calendarH;

        const gDayBars = SvgUtils.createSvgElement('g', {class: 'day-bars'});
        const firstDay = Math.max(0, Math.floor(this.scrollOffset) - 1);
        const lastDay = Math.min(this.totalDays - 1, firstDay + Math.ceil(this.containerWidth / this.dayWidth) + 2);
        for (let d = firstDay; d <= lastDay; d++) {
            const dayDate = new Date(this.chartStart!.getTime() + d * DateUtils.MS);
            this.drawDayBars(gDayBars, dayDate, this._calendarH);
        }
        svg.appendChild(gDayBars);

        this.calendarXAxes.initPosition(0, y);
        // this.calendarXAxes.draw(
        //     svg, this.chartStart!, this.totalDays,
        //     this.dayWidth, this.scrollOffset, this.containerWidth, this.milestones,
        // );
        this.calendarXAxes.initSize(this.containerWidth, this.dayWidth, this.calendarAtBottom, this.calendarSize);
        this.calendarXAxes.drawCalendar(svg, false, this.containerWidth);
        this.calendarXAxes.drawMilestones(svg);

        const gTasks = SvgUtils.createSvgElement('g', {class: 'tasks'});
        this.drawGanttChart(gTasks);
        svg.appendChild(gTasks);

        svg.appendChild(this.renderNowLine(y + totalH));
    }
}

