// sprints-overview/sprints-overview-renderer.ts
// Renders sprint lanes on a virtual timeline canvas.
// Mirrors Java: SprintsOverviewRenderer extends AbstractRenderer
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {ColorUtils} from '../ColorUtils.js';
import {SvgUtils} from '../SvgUtils.js';
import {DateUtils} from '../DateUtils.js';
import {AbstractRenderer} from '../AbstractRenderer.js';
import {CalendarSize} from '../CalendarSize.js';
import {Milestone} from '../Milestone.js';
import {Milestones} from '../Milestones.js';
import {SprintDto} from './dto/SprintDto.js';
import {LaneDto} from './dto/LaneDto.js';
import {SprintOverviewDto} from './dto/SprintOverviewDto.js';
import {HitArea} from './dto/HitArea.js';
import {AbstractChart} from '../AbstractChart.js';

// ── Layout constants (mirrors Java SprintsOverviewRenderer) ──────────────
const LINE_HEIGHT = 13;
const NUMBER_OF_LINES = 3;
const SPRINT_H = LINE_HEIGHT * NUMBER_OF_LINES;        // 39 px
const TASK_H = LINE_HEIGHT * NUMBER_OF_LINES + 17;   // 56 px
const LANE_H = TASK_H + 2;                           // 58 px

export const DEFAULT_DW = 8;
export const MIN_DW = 1;
export const MAX_DW = 80;
export const ZOOM_STEP = 1.25;

/**
 * convert string date representative to Date
 * @param sprint
 */
function convertSprintDates(sprint: SprintDto): SprintDto {
    return {
        ...sprint,
        start: sprint.start ? new Date(sprint.start as any) : null,
        end: sprint.end ? new Date(sprint.end as any) : null,
    };
}

export class SprintsOverviewRenderer extends AbstractRenderer {
    lanes: LaneDto[];
    chartStart: Date;
    chartEnd: Date;
    currentDate: Date;
    // totalDays: number;
    dayWidth: number;
    sprintHitAreas: HitArea[];
    // 24-hour format
    readonly options24: Intl.DateTimeFormatOptions = {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
    };
    dateTimeFormat: Intl.DateTimeFormat = new Intl.DateTimeFormat('en-DE', this.options24);

    constructor(chart: AbstractChart, data: SprintOverviewDto, preRun: number, postRun: number) {
        const chartStart = new Date(data.meta.chartStart || Date.now());
        const chartEnd = new Date(data.meta.chartEnd || Date.now());
        const currentDate = new Date(data.meta.now);


        super(chart, preRun, postRun);
        const milestonesList: Milestone[] = [
            new Milestone(currentDate, 'N', 'Now (current date)', false),
            new Milestone(DateUtils.getDayMidnight(chartStart), 'S', 'Start (Start of project)', false),
            new Milestone(DateUtils.getDayMidnight(chartEnd), 'E', 'End (End of project)', false),
        ];
        this.milestones = new Milestones(milestonesList, chartStart, chartEnd);
        this.milestones.calculate();


        this.lanes = (data.lanes || []).map(lane => ({
            ...lane,
            sprints: lane.sprints.map(convertSprintDates),
        }));
        this.chartStart = chartStart;
        this.chartEnd = chartEnd;
        this.currentDate = currentDate;
        this.days = this.calculateMaxDays();
        // this.totalDays = DateUtils.calculateDayCount(chartStart, chartEnd);
        this.dayWidth = DEFAULT_DW;
        this.sprintHitAreas = [];
        this.init();
    }

    public init() {
        this.initSize(0, false, CalendarSize.YEARS, this.chartWidth);
    }

    override calculateChartWidth(): number {
        return (this.calendarXAxes.dayOfWeek.getWidth() ?? 0) * this.days;
    }

    // dayIndexToPixelX(dayIndex: number): number {
    //     return this.firstDayX + (dayIndex - this.scrollOffset + this.preRun) * this.dayWidth;
    // }

    override calculateDayWidth(): void {
        this.days = this.calculateMaxDays();
        this.calendarXAxes.dayOfWeek.setWidth(this.dayWidth);
        this.calendarXAxes.dayOfMonth.setWidth(this.dayWidth);
    }

    // override calculateDayWidth(): void {
    //     // no-op: day width is controlled by scroll/zoom state
    // }

    // renderWeekendStripes(baseY: number, baseHeight: number): SVGGElement {
    //     const g = SvgUtils.createGroup({class: 'weekend-stripes'});
    //     const containerWidth = this.containerWidth;
    //     const xAxesTheme = this.theme.xAxesTheme;
    //
    //     if (this.dayWidth >= 4) {
    //         const satColor = intToHex(xAxesTheme.dayOfweekSaturdayBgColor, '');
    //         const sunColor = intToHex(xAxesTheme.dayOfweekSundayBgColor, '');
    //         const firstDay = Math.max(0, Math.floor(this.scrollOffset) - 1);
    //         const lastDay = Math.min(this.totalDays - 1, firstDay + Math.ceil(containerWidth / this.dayWidth) + 2);
    //         for (let d = firstDay; d <= lastDay; d++) {
    //             const dow = new Date(this.chartStart.getTime() + d * MS).getDay();
    //             const xPos = this.dayIndexToPixelX(d);
    //             if (xPos + this.dayWidth < 0 || xPos > containerWidth) continue;
    //             const bgColor = dow === 6 ? satColor : dow === 0 ? sunColor : null;
    //             if (!bgColor) continue;
    //             g.appendChild(SvgUtils.createRect(xPos, baseY, this.dayWidth, baseHeight, {fill: bgColor}));
    //         }
    //     } else {
    //         const gridColor = intToHex(this.theme.ganttTheme.gridColor, '#e4e8f3');
    //         const firstD = Math.max(0, Math.floor(this.scrollOffset));
    //         const lastD = Math.min(this.totalDays - 1, firstD + Math.ceil(containerWidth / this.dayWidth) + 8);
    //         for (let dd = firstD; dd <= lastD; dd++) {
    //             if (new Date(this.chartStart.getTime() + dd * MS).getDay() !== 1) continue;
    //             const xp = this.dayIndexToPixelX(dd);
    //             if (xp < 0 || xp > containerWidth) continue;
    //             g.appendChild(SvgUtils.createLine(xp, baseY, xp, baseY + baseHeight, {
    //                 stroke: gridColor, 'stroke-width': '1',
    //             }));
    //         }
    //     }
    //     return g;
    // }

    // renderVerticalGridLines(baseY: number, baseHeight: number): SVGGElement {
    //     const g = SvgUtils.createGroup({class: 'grid-lines'});
    //     if (this.dayWidth < 4) return g;
    //     const containerWidth = this.containerWidth;
    //     const gridColor = intToHex(this.theme.ganttTheme.gridColor, '#e4e8f3');
    //     const firstDay = Math.max(0, Math.floor(this.scrollOffset) - 1);
    //     const lastDay = Math.min(this.totalDays, firstDay + Math.ceil(containerWidth / this.dayWidth) + 2);
    //     for (let d = firstDay; d <= lastDay; d++) {
    //         const xPos = this.dayIndexToPixelX(d);
    //         if (xPos < 0 || xPos > containerWidth) continue;
    //         g.appendChild(SvgUtils.createLine(xPos, baseY, xPos, baseY + baseHeight, {
    //             stroke: gridColor, 'stroke-width': '1',
    //         }));
    //     }
    //     return g;
    // }

    calculateLaneAreaHeight(): number {
        return this.lanes.length * LANE_H + 8;
    }

    override calculateChartHeight(): number {
        const calH = this.calendarXAxes ? this.calendarXAxes.getHeight() : 0;
        return calH + this.calculateLaneAreaHeight();
    }

    drawGraph(svg: SVGElement): void {
        this.sprintHitAreas = [];
        const g = SvgUtils.createGroup({class: 'sprints'});
        const containerWidth = this.containerWidth;

        this.lanes.forEach((lane, laneIndex) => {
            const laneY = this.diagram.y + laneIndex * LANE_H;

            (lane.sprints || []).forEach((sprint) => {
                if (!sprint.start || !sprint.end) return;
                // const startIdx = this.calculateDayIndex(sprint.start);
                // const endIdx = this.calculateDayIndex(sprint.end);
                const sprintX = this.calculateDayX(sprint.start);
                const sprintW = DateUtils.calculateDayCount(sprint.start, sprint.end) * this.dayWidth - 1;
                if (sprintX + sprintW < 0 || sprintX > containerWidth) return;

                this.sprintHitAreas.push({sprint, x: sprintX, y: laneY, width: sprintW, height: SPRINT_H});

                const fillColor = ColorUtils.convertSprintColorToRgba(sprint.color);
                const rect = SvgUtils.createRect(sprintX, laneY, sprintW, SPRINT_H, {fill: fillColor});
                rect.appendChild(SvgUtils.createTitle(this.buildSprintTooltip(sprint)));
                g.appendChild(rect);

                if (sprintW > 20) {
                    const clipId = `sp${laneIndex}_${sprint.id}`;
                    g.appendChild(SvgUtils.createClipPath(clipId, sprintX, laneY, sprintW, SPRINT_H));
                    const textY = laneY + LINE_HEIGHT - 2;
                    g.appendChild(SvgUtils.createText(sprintX + 1, textY, sprint.name || '', {
                        fill: '#000000',
                        'font-size': '12',
                        'font-family': 'Arial,sans-serif',
                        'font-weight': 'bold',
                    }));
                }
            });
        });
        svg.appendChild(g);
    }

    // renderCurrentDateLine(chartHeight: number): SVGGElement {
    //     const g = SvgUtils.createGroup({class: 'now-line'});
    //     const containerWidth = this.containerWidth;
    //     const nowIdx = calculateDayIndex(this.currentDate, this.chartStart);
    //     const xPos = this.dayIndexToPixelX(nowIdx) + this.dayWidth / 2;
    //     if (xPos < 0 || xPos > containerWidth) return g;
    //     g.appendChild(SvgUtils.createLine(xPos, 0, xPos, chartHeight, {stroke: '#cc0000', 'stroke-width': '2'}));
    //     return g;
    // }

    buildSprintTooltip(sprint: SprintDto): string {
        let tooltip = sprint.name || '';
        if (sprint.key) tooltip += `\nKey: ${sprint.key}`;
        if (sprint.status) tooltip += `\nStatus: ${sprint.status}`;
        if (sprint.start) tooltip += `\nStart: ${DateUtils.toLocalYMDHMString(sprint.start, this.dateTimeFormat)}`;
        if (sprint.end) tooltip += `\nEnd: ${DateUtils.toLocalYMDHMString(sprint.end, this.dateTimeFormat)}`;
        if (sprint.delay) tooltip += '\n(DELAYED)';
        return tooltip;
    }

    initPosition(x: number, y: number): void {
        this.firstDayX = x;
        this.calendarXAxes.initPosition(x, y);
        this.diagram.initPosition(x, this.calendarXAxes.year.getY() + this.calendarXAxes.getHeight());
    }

    drawCalendar(svg: SVGElement, drawDays: boolean, viewportWidth: number = this.diagram.width): void {
        this.calendarXAxes.drawCalendar(svg, drawDays, viewportWidth);
    }

    override draw(svg: SVGElement, x: number, y: number): void {
        this.initPosition(x, y);
        this.drawCalendar(svg, true);
        this.drawGraph(svg);
        this.drawMilestones(svg);
    }
}
