// gantt/gantt-renderer.ts
// Mirrors Java: GanttRenderer extends AbstractGanttRenderer
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {ColorUtils} from '../ColorUtils.js';
import {SvgUtils} from '../SvgUtils.js';
import {DateUtils} from '../DateUtils.js';
import {getCalendarException} from './date-helpers.js';
import {AbstractGanttRenderer} from './AbstractGanttRenderer.js';
import {CalendarSize} from "../CalendarSize.js";
import {TaskDto} from './dto/TaskDto.js';
import {GanttChartDto} from './dto/GanttChartDto.js';
import {AbstractChart} from '../AbstractChart.js';
import {GanttChartMeta} from "Frontend/src/main/frontend/js/gantt/dto";

/**
 * convert string date representative to Date
 * @param task
 */
function convertTaskDates(task: TaskDto): TaskDto {
    return {
        ...task,
        start: task.start ? new Date(task.start as any) : null,
        finish: task.finish ? new Date(task.finish as any) : null,
    };
}

export class GanttRenderer extends AbstractGanttRenderer {
    static GANTT_TASK_POST_SPACE: number = 0;
    static GANTT_TASK_PRI_SPACE: number = 0;
    calendarSize: CalendarSize;
    data: GanttChartDto;

    constructor(chart: AbstractChart, data: GanttChartDto, preRun: number, postRun: number) {
        const chartStart = DateUtils.getDayMidnight(new Date(data.meta.chartStart));
        const chartEnd = DateUtils.getDayMidnight(new Date(data.meta.chartEnd));
        const now = data.meta.now ? DateUtils.getDayMidnight(new Date(data.meta.now)) : DateUtils.getDayMidnight(new Date());
        const sprintStart = DateUtils.getDayMidnight(new Date(data.meta.sprintStart));
        const sprintEnd = DateUtils.getDayMidnight(new Date(data.meta.sprintEnd));
        // const sprintStatus = data.meta.sprintStatus;

        // const milestonesList: Milestone[] = [];
        // if (data.meta.sprintStatus !== 'CLOSED') {
        //     if (now.getTime() <= DateUtils.addDay(sprintEnd, 7).getTime()) {
        //         milestonesList.push(new Milestone(now, 'N', 'Now (current date)', false));
        //     }
        // }
        // milestonesList.push(new Milestone(sprintStart, 'S', 'Start (Start of project)', false));
        // milestonesList.push(new Milestone(sprintEnd, 'E', 'End (End of project)', false));
        //
        // const milestones = new Milestones(
        //     milestonesList,
        //     milestonesList.length > 0 ? milestonesList[0].time : chartStart,
        //     milestonesList.length > 0 ? milestonesList[milestonesList.length - 1].time : chartStart,
        // );

        super(chart/*, milestones*/, data.meta.preRun || 0, data.meta.postRun || 0);
        this.createMilestonesFromMeta(data.meta);

        this.data = data;
        this.tasks = (data.tasks || []).map(convertTaskDates);//convert all dates from string to Date
        this.chartStart = chartStart;
        this.days = this.calculateMaxDays();//DateUtils.calculateDayCount(chartStart, chartEnd);
        this.currentDate = now;
        this.calendarSize = data.meta.calendarSize;

        for (const task of this.tasks) {
            this._taskById[String(task.id)] = task;
        }
        this.init();
    }

    public init() {
        this.initSize(this.data.meta.firstDayX, false, this.calendarSize, this.containerWidth);
    }

    // }
    override calculateDayWidth(): void {
        this.days = this.calculateMaxDays();
        this.calendarXAxes.dayOfWeek.setWidth(this.dayWidth);
        this.calendarXAxes.dayOfMonth.setWidth(this.dayWidth);
    }

// override calculateDayWidth(): void {
    //     super.calculateDayWidth();
    //     this.dayWidth = DEFAULT_DW;

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

    override drawDayBars(g: SVGElement, currentDay: Date): void {
        const x = this.calculateDayX(currentDay);
        const x1 = x - (this.calendarXAxes.dayOfWeek.getWidth() / 2 - 1);
        if (SvgUtils.isClipped(x1 - 1, x1 - 1 + this.dayWidth, this.chart.containerWidth))
            return;
        const gridColor = ColorUtils.intToHex(this.theme.ganttTheme.gridColor, '#e4e8f3');
        for (const task of this.tasks) {
            const y1 = this._calendarH + task.rowIndex * (this.getTaskHeight() + 1) /*+ this.getTaskHeight() / 2*/;

            //grid
            g.appendChild(SvgUtils.createRect(x1 - 1, y1 - 1, this.dayWidth, 1, {fill: gridColor}));//top --
            g.appendChild(SvgUtils.createRect(x1 - 1, y1, 1, this.getTaskHeight(), {fill: gridColor}));//left |
            const bgColor = this.getGanttDayStripeColor(task, currentDay);
            //background
            g.appendChild(SvgUtils.createRect(x1 + 1, y1, this.dayWidth - 1, this.getTaskHeight(), {fill: bgColor}));
            const ex = getCalendarException(currentDay, task.calendarExceptions);
            if (ex?.letter && this.dayWidth >= 14) {
                const cx = x1 + this.dayWidth / 2;
                const letter = SvgUtils.createText(cx, y1 + this.getTaskHeight() / 2, ex.letter, {
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

//     @Override
//     public void drawDayBars(LocalDate currentDay) {
//     int ganttUniqueId = 0;
//     for (Task task : sprint.getTasks()) {
//     ProjectCalendar pc   = task.getEffectiveCalendar();
//     Integer         lane = taskHeight.get(ganttUniqueId + "-" + task.getId());
//     int             y    = lane + getTaskHeight() / 2;
//     int             x    = calculateDayX(currentDay);
//     int             y1   = y - getTaskHeight() / 2;
//     int             x1   = x - (calendarXAxes.dayOfWeek.getWidth() / 2 - 1);
// {
//     //grid
//     graphics2D.setColor(theme.ganttTheme.gridColor);
//     graphics2D.fillRect(x1 - 1, y1 - 1, calendarXAxes.dayOfWeek.getWidth(), 1);//top --
//     graphics2D.fillRect(x1 - 1, y1, 1, getTaskHeight());//left |
// }
// {
//     //background
//     graphics2D.setColor(GraphColorUtil.getGanttDayStripeColor(theme, pc, currentDay));
//     Shape s = new Rectangle(x1, y1, calendarXAxes.dayOfWeek.getWidth() - 1, getTaskHeight());
//
//     ProjectCalendarException exception = GraphColorUtil.getException(theme, pc, currentDay);
//     if (exception != null) {
//         String letter = GraphColorUtil.getOffDayLetter(exception);
//         if (letter != null) {
//             graphics2D.fill(s);
//             graphics2D.setColor(theme.ganttTheme.outOfOfficeColor);
//             graphics2D.setFont(NoneWorkingDayFont);
//             FontMetrics fm      = graphics2D.getFontMetrics();
//             int         yShift  = fm.getAscent() - fm.getHeight() / 2 - 1;
//             int         xShift  = fm.stringWidth(letter) / 2;
//             String      tooltip = createOffDayToolTip(exception);
//             graphics2D.drawString(letter, x - xShift, y + yShift, tooltip);
//         }
//     } else {
//         graphics2D.fill(s);
//     }
// }
// }
// }

    override draw(svg: SVGElement, x: number, y: number): void {
        const calendarH = this.calendarXAxes.getHeight();
        const taskAreaH = this.tasks.length * (this.getTaskHeight() + 1);
        const totalH = calendarH + taskAreaH;
        this._calendarH = y + calendarH;

        // const gDayBars = SvgUtils.createSvgElement('g', {class: 'day-bars'});
        const firstDay = Math.max(0, Math.floor(this.scrollOffset) - 1);
        const lastDay = Math.min(this.days - 1, firstDay + Math.ceil(this.containerWidth / this.dayWidth) + 2);
        // for (let d = firstDay; d <= lastDay; d++) {
        //     const dayDate = new Date(this.chartStart!.getTime() + d * DateUtils.MS);
        //     this.drawDayBars(gDayBars, dayDate, this._calendarH);
        // }
        // svg.appendChild(gDayBars);

        this.calendarXAxes.initPosition(this.firstDayX + x, y);
        this.calendarXAxes.initSize(this.containerWidth, this.dayWidth, this.calendarAtBottom, this.calendarSize);
        this.calendarXAxes.drawCalendar(svg, true, this.containerWidth);
        this.calendarXAxes.drawMilestones(svg);

        const gTasks = SvgUtils.createSvgElement('g', {class: 'tasks'});
        this.drawGanttChart(gTasks);
        svg.appendChild(gTasks);

        // svg.appendChild(this.renderNowLine(y + totalH));
    }

    // renderNowLine(totalHeight: number): SVGGElement {
    //     const g = SvgUtils.createSvgElement('g', {class: 'now-line'});
    //     const containerWidth = this.containerWidth;
    //     const nowIdx = this.calculateDayIndex(this.currentDate);
    //     const xPos = this.dayIndexToPixelX(nowIdx) + this.dayWidth / 2;
    //     if (xPos < 0 || xPos > containerWidth) return g;
    //     g.appendChild(SvgUtils.createLine(xPos, 0, xPos, totalHeight, {stroke: '#cc0000', 'stroke-width': '2'}));
    //     return g;
    // }

    private createMilestonesFromMeta(meta: GanttChartMeta) {
        const start = DateUtils.getDayMidnight(new Date(meta.sprintStart));
        const end = DateUtils.getDayMidnight(new Date(meta.sprintEnd));
        let now: Date | null = null;
        // if (!this.isHideNow(now, end, meta.sprintClosed))
        {
            now = meta.now ? DateUtils.getDayMidnight(new Date(meta.now)) : null;
        }
        // const firstWorklog = meta.firstWorklogDate ? DateUtils.getDayMidnight(new Date(meta.firstWorklogDate)) : null;
        // const lastWorklog = meta.lastWorklogDate ? DateUtils.getDayMidnight(new Date(meta.lastWorklogDate)) : null;
        // const release = meta.releaseDate ? DateUtils.getDayMidnight(new Date(meta.releaseDate)) : null;
        this.createMilestones(start, now, end, null, null, null);
    }
}

