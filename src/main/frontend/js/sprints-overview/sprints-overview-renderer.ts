// sprints-overview/sprints-overview-renderer.ts
// Renders sprint lanes on a virtual timeline canvas.
// Mirrors Java: SprintsOverviewRenderer extends AbstractRenderer
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {convertSprintColorToRgba, intToHex} from '../color-utils.js';
import {createClipPath, createLine, createRect, createSvgElement, createText} from '../svg-utils.js';
import {calculateDayCount, calculateDayIndex, getDayMidnight, MS} from '../date-utils.js';
import {AbstractRenderer} from '../abstract-renderer.js';
import {CalendarSize} from '../calendar-size.js';
import {Milestone} from '../milestone.js';
import {Milestones} from '../milestones.js';
import {Theme} from '../theme/theme.js';

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

export interface SprintDto {
    id: number | string;
    key?: string;
    name?: string;
    start?: string;
    end?: string;
    status?: string;
    color?: string;
    hasGantt?: boolean;
    delay?: boolean;
}

export interface LaneDto {
    laneId: number;
    sprints: SprintDto[];
}

export interface SprintOverviewMeta {
    chartStart: string;
    chartEnd: string;
    now: string;
    laneCount: number;
    xAxesTheme?: Record<string, unknown>;
    theme?: Record<string, unknown>;
}

export interface SprintOverviewDto {
    lanes: LaneDto[];
    meta: SprintOverviewMeta;
}

export interface HitArea {
    sprint: SprintDto;
    x: number;
    y: number;
    width: number;
    height: number;
}

export class SprintsOverviewRenderer extends AbstractRenderer {
    lanes: LaneDto[];
    chartStart: Date;
    chartEnd: Date;
    currentDate: Date;
    totalDays: number;
    dayWidth: number;
    containerWidth: number;
    sprintHitAreas: HitArea[];

    constructor(data: SprintOverviewDto, theme: Theme, preRun: number, postRun: number) {
        const chartStart = new Date(data.meta.chartStart || Date.now());
        const chartEnd = new Date(data.meta.chartEnd || Date.now());
        const currentDate = new Date(data.meta.now);

        const milestonesList: Milestone[] = [
            new Milestone(currentDate, 'N', 'Now (current date)', false),
            new Milestone(getDayMidnight(chartStart), 'S', 'Start (Start of project)', false),
            new Milestone(getDayMidnight(chartEnd), 'E', 'End (End of project)', false),
        ];
        const milestones = new Milestones(milestonesList, chartStart, chartEnd);

        super(theme, milestones, preRun, postRun);

        this.lanes = data.lanes || [];
        this.chartStart = chartStart;
        this.chartEnd = chartEnd;
        this.currentDate = currentDate;
        this.totalDays = calculateDayCount(chartStart, chartEnd);
        this.dayWidth = DEFAULT_DW;
        this.containerWidth = 800;
        this.sprintHitAreas = [];

        this.initSize(0, false, CalendarSize.YEARS, this.chartWidth);
    }

    override calculateChartWidth(): number {
        return (this.calendarXAxes.dayOfWeek.getWidth() ?? 0) * this.days;
    }

    dayIndexToPixelX(dayIndex: number): number {
        return (dayIndex - this.scrollOffset + this.preRun) * this.dayWidth;
    }

    override calculateDayWidth(): void {
        // no-op: day width is controlled by scroll/zoom state
    }

    calculateLaneAreaHeight(): number {
        return this.lanes.length * LANE_H + 8;
    }

    override calculateChartHeight(): number {
        const calH = this.calendarXAxes ? this.calendarXAxes.getHeight(this.dayWidth, false) : 0;
        return calH + this.calculateLaneAreaHeight();
    }

    renderWeekendStripes(baseY: number, baseHeight: number): SVGGElement {
        const g = createSvgElement('g', {class: 'weekend-stripes'});
        const containerWidth = this.containerWidth;
        const xAxesTheme = this.theme.xAxesTheme;

        if (this.dayWidth >= 4) {
            const satColor = intToHex(xAxesTheme.dayOfweekSaturdayBgColor, '');
            const sunColor = intToHex(xAxesTheme.dayOfweekSundayBgColor, '');
            const firstDay = Math.max(0, Math.floor(this.scrollOffset) - 1);
            const lastDay = Math.min(this.totalDays - 1, firstDay + Math.ceil(containerWidth / this.dayWidth) + 2);
            for (let d = firstDay; d <= lastDay; d++) {
                const dow = new Date(this.chartStart.getTime() + d * MS).getDay();
                const xPos = this.dayIndexToPixelX(d);
                if (xPos + this.dayWidth < 0 || xPos > containerWidth) continue;
                const bgColor = dow === 6 ? satColor : dow === 0 ? sunColor : null;
                if (!bgColor) continue;
                g.appendChild(createRect(xPos, baseY, this.dayWidth, baseHeight, {fill: bgColor}));
            }
        } else {
            const gridColor = intToHex(this.theme.ganttTheme.gridColor, '#e4e8f3');
            const firstD = Math.max(0, Math.floor(this.scrollOffset));
            const lastD = Math.min(this.totalDays - 1, firstD + Math.ceil(containerWidth / this.dayWidth) + 8);
            for (let dd = firstD; dd <= lastD; dd++) {
                if (new Date(this.chartStart.getTime() + dd * MS).getDay() !== 1) continue;
                const xp = this.dayIndexToPixelX(dd);
                if (xp < 0 || xp > containerWidth) continue;
                g.appendChild(createLine(xp, baseY, xp, baseY + baseHeight, {
                    stroke: gridColor, 'stroke-width': '1',
                }));
            }
        }
        return g;
    }

    renderVerticalGridLines(baseY: number, baseHeight: number): SVGGElement {
        const g = createSvgElement('g', {class: 'grid-lines'});
        if (this.dayWidth < 4) return g;
        const containerWidth = this.containerWidth;
        const gridColor = intToHex(this.theme.ganttTheme.gridColor, '#e4e8f3');
        const firstDay = Math.max(0, Math.floor(this.scrollOffset) - 1);
        const lastDay = Math.min(this.totalDays, firstDay + Math.ceil(containerWidth / this.dayWidth) + 2);
        for (let d = firstDay; d <= lastDay; d++) {
            const xPos = this.dayIndexToPixelX(d);
            if (xPos < 0 || xPos > containerWidth) continue;
            g.appendChild(createLine(xPos, baseY, xPos, baseY + baseHeight, {
                stroke: gridColor, 'stroke-width': '1',
            }));
        }
        return g;
    }

    drawGraph(svg: SVGElement): void {
        this.sprintHitAreas = [];
        const g = createSvgElement('g', {class: 'sprints'});
        const containerWidth = this.containerWidth;

        this.lanes.forEach((lane, laneIndex) => {
            const laneY = this.diagram.y + laneIndex * LANE_H;

            (lane.sprints || []).forEach((sprint) => {
                if (!sprint.start || !sprint.end) return;
                const startIdx = calculateDayIndex(sprint.start, this.chartStart);
                const endIdx = calculateDayIndex(sprint.end, this.chartStart);
                const sprintX = this.dayIndexToPixelX(startIdx);
                const sprintW = (endIdx - startIdx + 1) * this.dayWidth - 1;
                if (sprintX + sprintW < 0 || sprintX > containerWidth) return;

                this.sprintHitAreas.push({sprint, x: sprintX, y: laneY, width: sprintW, height: SPRINT_H});

                const fillColor = convertSprintColorToRgba(sprint.color);
                const rect = createRect(sprintX, laneY, sprintW, SPRINT_H, {fill: fillColor});
                rect.appendChild(createSvgElement('title', {}, this.buildSprintTooltip(sprint)));
                g.appendChild(rect);

                if (sprintW > 20) {
                    const clipId = `sp${laneIndex}_${sprint.id}`;
                    g.appendChild(createClipPath(clipId, sprintX, laneY, sprintW, SPRINT_H));
                    const textY = laneY + LINE_HEIGHT - 2;
                    g.appendChild(createText(sprintX + 1, textY, sprint.name || '', {
                        fill: '#000000',
                        'font-size': '12',
                        'font-family': 'Arial,sans-serif',
                        'font-weight': 'bold',
                        'clip-path': `url(#${clipId})`,
                    }));
                }
            });
        });
        svg.appendChild(g);
    }

    renderCurrentDateLine(chartHeight: number): SVGGElement {
        const g = createSvgElement('g', {class: 'now-line'});
        const containerWidth = this.containerWidth;
        const nowIdx = calculateDayIndex(this.currentDate, this.chartStart);
        const xPos = this.dayIndexToPixelX(nowIdx) + this.dayWidth / 2;
        if (xPos < 0 || xPos > containerWidth) return g;
        g.appendChild(createLine(xPos, 0, xPos, chartHeight, {stroke: '#cc0000', 'stroke-width': '2'}));
        return g;
    }

    buildSprintTooltip(sprint: SprintDto): string {
        let tooltip = sprint.name || '';
        if (sprint.key) tooltip += `\nKey: ${sprint.key}`;
        if (sprint.status) tooltip += `\nStatus: ${sprint.status}`;
        if (sprint.start) tooltip += `\nStart: ${new Date(sprint.start).toLocaleDateString()}`;
        if (sprint.end) tooltip += `\nEnd: ${new Date(sprint.end).toLocaleDateString()}`;
        if (sprint.delay) tooltip += '\n(DELAYED)';
        return tooltip;
    }

    initPosition(x: number, y: number): void {
        this.firstDayX = x;
        this.calendarXAxes.initPosition(x, y);
        this.diagram.initPosition(x, this.calendarXAxes.year.getY() + this.calendarXAxes.getHeight(this.dayWidth, false));
    }

    drawCalendar(drawDays: boolean, svg: SVGElement): void {
        this.calendarXAxes.drawCalendar(drawDays, svg, this.diagram.width);
    }

    override draw(svg: SVGSVGElement, x: number, y: number): void {
        this.initPosition(x, y);
        this.drawCalendar(true, svg);
        this.drawGraph(svg);
        this.drawMilestones(svg);
    }
}

