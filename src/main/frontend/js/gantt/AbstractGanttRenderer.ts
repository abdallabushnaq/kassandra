// gantt/abstract-gantt-renderer.ts
// Base class for Gantt renderers. Mirrors Java: LegacyLegacyAbstractGanttRenderer extends LegacyAbstractRenderer
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {ColorUtils} from '../ColorUtils.js';
import {SvgUtils} from '../SvgUtils.js';
import {DateUtils} from '../DateUtils.js';
import {AbstractRenderer} from '../AbstractRenderer.js';
import {getCalendarException, isWorkingDay} from './date-helpers.js';
import {TaskDto} from './dto/TaskDto.js';
import {CalendarException} from './dto/CalendarException.js';
import {GanttZoomSettings} from './dto/GanttZoomSettings.js';
import {FontMetrics} from "../FontMetrics.js";
import {FontSpec} from "../FontSpec.js";
import {AbstractChart} from '../AbstractChart.js';
import {GraphColorUtil} from "../GraphColorUtil.js";

// ── Constants (mirrors Java LegacyLegacyAbstractGanttRenderer field declarations) ────────
export const LINE_HEIGHT = 18;
const RELATION_CORNER_LENGTH = 14;
const RELATION_STROKE_WIDTH = 2;
const RESOURCE_NAME_TO_TASK_GAP = 3;
export const SECONDS_PER_DAY = 85 * 6 * 60; // 30600
const TASK_BODY_MARGIN = 3;
// const TASK_PROGRESS_HEIGHT = 1;
// const TASK_PROGRESS_BOTTOM_GAP = 1;
const TASK_NAME_TO_TASK_GAP = 5 + 8;        // 13
const NONE_WORKING_DAY_FONT_SIZE = 22;

export const DEFAULT_DW = 20;
export const MIN_DW = 2;
export const MAX_DW = 80;
export const ZOOM_STEP = 1.25;

export abstract class AbstractGanttRenderer extends AbstractRenderer {
    private static readonly taskProgressFont: FontSpec = new FontSpec(FontSpec.SANS_SERIF, 8, FontSpec.PLAIN);
    private static readonly graphFont: FontSpec = new FontSpec(FontSpec.SANS_SERIF, 12, FontSpec.PLAIN);
    dayWidth: number;
    chartStart: Date | null;
    // totalDays: number;
    currentDate: Date | null;
    tasks: TaskDto[];
    _calendarH: number;
    _taskById: Record<string, TaskDto>;
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
    protected calendarExceptionsById: Map<string, CalendarException[]>;

    // dayIndexToPixelX(dayIndex: number): number {
    //     return (dayIndex - this.scrollOffset) * this.dayWidth;
    // }

    protected constructor(chart: AbstractChart/*, milestones: Milestones*/, preRun: number, postRun: number,
                          protected readonly ganttZoom: GanttZoomSettings) {
        super(chart/*, milestones*/, preRun, postRun);
        this.dayWidth = DEFAULT_DW;
        this.chartStart = null;
        // this.totalDays = 0;
        this.currentDate = null;
        this.tasks = [];
        this._calendarH = 0;
        this._taskById = {};
        this.calendarExceptionsById = new Map();
    }

    getTaskHeight(): number {
        return LINE_HEIGHT;
    }

    override calculateChartHeight(): number {
        const calH = this.calendarXAxes
            ? this.calendarXAxes.getHeight()
            : 0;
        return calH + this.tasks.length * (this.getTaskHeight() + 1);
    }

    getGanttDayStripeColor(task: TaskDto, dayDate: Date): string {
        const dow = dayDate.getDay();
        if (dow === 6 || dow === 0 || isWorkingDay(dayDate, this.getCalendarExceptions(task))) {
            return ColorUtils.intToHex(GraphColorUtil.getDayOfWeekBgColor(this.theme, dayDate));
        }
        const exception = getCalendarException(dayDate, this.getCalendarExceptions(task));
        if (exception) {
            const t = exception.type;
            if (t === 'VACATION') return ColorUtils.intToHex(this.theme.ganttTheme.vacationBgColor, '#a0c8ff');
            if (t === 'TRIP') return ColorUtils.intToHex(this.theme.ganttTheme.tripBgColor, '#c8a0ff');
            if (t === 'SICK') return ColorUtils.intToHex(this.theme.ganttTheme.sickBgColor, '#ffa0a0');
            return ColorUtils.intToHex(this.theme.ganttTheme.holidayBgColor, '#ffd0a0');
        }
        return ColorUtils.intToHex(this.theme.xAxesTheme.dayOfMonthWeekendBgColor, '#d7d7d7');
    }

    drawConflictMarker(_g: SVGElement, _y: number, _conflict: unknown): void { /* team planner only */
    }

    // override drawDayBars(g: SVGElement, dayDate: Date, calendarH = 0): void {
    //     const dayIdx = calculateDayIndex(dayDate, this.chartStart!);
    //     const dayLeft = this.dayIndexToPixelX(dayIdx);
    //     const gridColor = ColorUtils.intToHex(this.theme.ganttTheme.gridColor, '#e4e8f3');
    //     for (const task of this.tasks) {
    //         const rowY = calendarH + task.rowIndex * (this.getTaskHeight() + 1);
    //         g.appendChild(SvgUtils.createRect(dayLeft, rowY - 1, this.dayWidth, 1, {fill: gridColor}));
    //         g.appendChild(SvgUtils.createRect(dayLeft, rowY, 1, LINE_HEIGHT, {fill: gridColor}));
    //         const bgColor = this.getGanttDayStripeColor(task, dayDate);
    //         g.appendChild(SvgUtils.createRect(dayLeft + 1, rowY, this.dayWidth - 1, LINE_HEIGHT, {fill: bgColor}));
    //         const exception = getCalendarException(dayDate, this.getCalendarExceptions(task));
    //         if (exception?.letter && this.dayWidth >= 14) {
    //             const cx = dayLeft + this.dayWidth / 2;
    //             const letter = SvgUtils.createText(cx, rowY + LINE_HEIGHT / 2, exception.letter, {
    //                 fill: ColorUtils.intToHex(this.theme.ganttTheme.outOfOfficeColor, '#ffffff'),
    //                 'font-size': String(NONE_WORKING_DAY_FONT_SIZE),
    //                 'font-family': 'sans-serif', 'font-weight': 'bold',
    //                 'text-anchor': 'middle', 'dominant-baseline': 'middle',
    //             });
    //             letter.appendChild(SvgUtils.createTitle(exception.type || 'Off-day'));
    //             g.appendChild(letter);
    //         }
    //     }
    // }

    // getDayOfWeekStripBgColor(dayDate: Date): string {
    //     const dow = dayDate.getDay();
    //     if (dow === 6) return ColorUtils.intToHex(this.theme.chartTheme.dayOfweekSaturdayBgColor, '#d7d7d7');
    //     if (dow === 0) return ColorUtils.intToHex(this.theme.chartTheme.dayOfweekSundayBgColor, '#d7d7d7');
    //     return ColorUtils.intToHex(this.theme.xAxesTheme.dayOfweekBgColor, '#ffffff');
    // }

    drawTaskBorder(g: SVGElement, task: TaskDto, x1: number, x2: number, y: number): void {
        if (SvgUtils.isClipped(x1, x2, this.chart.containerWidth))
            return;
        const borderColor = task.critical
            ? ColorUtils.intToHex(this.theme.ganttTheme.criticalTaskBorderColor, '#ff0000')
            : ColorUtils.intToHex(this.theme.ganttTheme.taskBorderColor, '#888888');

        // Calculate days between start and finish (both truncated to day precision)
        const startTruncated = DateUtils.getDayMidnight(task.start!);
        const finishTruncated = DateUtils.getDayMidnight(task.finish!);
        const days = Math.floor((finishTruncated.getTime() - startTruncated.getTime()) / DateUtils.MS);
        const th = this.getTaskHeight();
        const top = y - th / 2 + TASK_BODY_MARGIN;
        const bottom = y + th / 2 - TASK_BODY_MARGIN;
        const solidLineAttributes = {
            class: 'task-border',
            stroke: borderColor,
            'stroke-width': '1',
            'vector-effect': 'non-scaling-stroke',
        };
        const dottedLineAttributes = {
            ...solidLineAttributes,
            'stroke-dasharray': '2 2',
        };

        if (!this.showDetail(this.ganttZoom.renderTaskNonWorkingDaysAtDayWidth)) {
            g.appendChild(SvgUtils.createLine(x1, top, x2, top, solidLineAttributes));
            g.appendChild(SvgUtils.createLine(x1, bottom, x2, bottom, solidLineAttributes));
            g.appendChild(SvgUtils.createLine(x1, top, x1, bottom, solidLineAttributes));
            g.appendChild(SvgUtils.createLine(x2, top, x2, bottom, solidLineAttributes));
            return;
        }

        for (let day = 0; day <= days; day++) {
            const currentDay = new Date(startTruncated.getTime() + day * DateUtils.MS);
            const working = isWorkingDay(currentDay, this.getCalendarExceptions(task));

            if (working) {
                if (days === 0) {
                    // This is the left and right end
                    g.appendChild(SvgUtils.createLine(x1, top, x2, top, solidLineAttributes));
                    g.appendChild(SvgUtils.createLine(x1, bottom, x2, bottom, solidLineAttributes));
                    g.appendChild(SvgUtils.createLine(x1, top, x1, bottom, solidLineAttributes));
                    g.appendChild(SvgUtils.createLine(x2, top, x2, bottom, solidLineAttributes));
                } else if (day === 0) {
                    // This is the left end
                    const currentDayAt8 = DateUtils.withTime(currentDay, 8, 0)!;
                    const nextDayAt8 = new Date(currentDayAt8.getTime() + SECONDS_PER_DAY * 1000);
                    const xFinish = this.calculateX(nextDayAt8, currentDayAt8, SECONDS_PER_DAY) - this.calendarXAxes.dayOfWeek.getWidth() / 2;
                    g.appendChild(SvgUtils.createLine(x1, top, xFinish, top, solidLineAttributes));
                    g.appendChild(SvgUtils.createLine(x1, bottom, xFinish, bottom, solidLineAttributes));
                    g.appendChild(SvgUtils.createLine(x1, top, x1, bottom, solidLineAttributes));
                } else if (day === days) {
                    // This is the right end
                    const currentDayAt8 = DateUtils.withTime(currentDay, 8, 0)!;
                    const xStart = this.calculateX(currentDayAt8, currentDayAt8, SECONDS_PER_DAY) - this.calendarXAxes.dayOfWeek.getWidth() / 2;
                    g.appendChild(SvgUtils.createLine(xStart, top, x2, top, solidLineAttributes));
                    g.appendChild(SvgUtils.createLine(xStart, bottom, x2, bottom, solidLineAttributes));
                    g.appendChild(SvgUtils.createLine(x2, top, x2, bottom, solidLineAttributes));
                } else {
                    // This is the middle
                    const currentDayAt8 = DateUtils.withTime(currentDay, 8, 0)!;
                    const xStart = this.calculateX(currentDayAt8, currentDayAt8, SECONDS_PER_DAY) - this.calendarXAxes.dayOfWeek.getWidth() / 2;
                    const xFinish = xStart + this.calendarXAxes.dayOfWeek.getWidth();
                    g.appendChild(SvgUtils.createLine(xStart, top, xFinish, top, solidLineAttributes));
                    g.appendChild(SvgUtils.createLine(xStart, bottom, xFinish, bottom, solidLineAttributes));
                }
            } else {
                // Non-working day (weekend) - draw dashed border
                const currentDayAt8 = DateUtils.withTime(currentDay, 8, 0)!;
                const xStart = this.calculateX(currentDayAt8, currentDayAt8, SECONDS_PER_DAY) - this.calendarXAxes.dayOfWeek.getWidth() / 2;
                const xFinish = xStart + this.calendarXAxes.dayOfWeek.getWidth();
                g.appendChild(SvgUtils.createLine(xStart, top, xFinish, top, dottedLineAttributes));
                g.appendChild(SvgUtils.createLine(xStart, bottom, xFinish, bottom, dottedLineAttributes));
            }
        }
    }

    drawId(g: SVGElement, task: TaskDto, y: number): void {
        const x1 = this.firstDayX - this.scrollOffset * this.dayWidth;
        const x2 = x1 + this.calendarXAxes.dayOfWeek.getWidth();
        // const x1 = this.dayIndexToPixelX(0);
        // const x2 = x1 + this.dayWidth;
        const fillColor = ColorUtils.intToHex(this.theme.ganttTheme.idBgColor, '#cccccc');
        const textColor = ColorUtils.intToHex(this.theme.ganttTheme.idTextColor, '#000000');
        g.appendChild(SvgUtils.createRect(x1 + 1, y - this.getTaskHeight() / 2, x2 - x1 - 1, this.getTaskHeight(), {fill: fillColor}));
        const keyText = SvgUtils.createText(x1 + 4, y, task.key || '', {
            fill: textColor, 'font-size': '12', 'font-family': 'sans-serif', 'dominant-baseline': 'middle',
        });
        if (task.name)
            keyText.appendChild(SvgUtils.createTitle(task.name));
        g.appendChild(keyText);
    }

    drawManualMarker(g: SVGElement, task: TaskDto, x1: number, y: number, _labelInside: boolean): void {
        if (task.manuallyScheduled) {
            g.appendChild(SvgUtils.createRect(x1, y - this.getTaskHeight() / 2, 1, this.getTaskHeight(), {fill: '#ff0000'}));
        }
    }

    drawMilestoneTask(g: SVGElement, task: TaskDto, x1: number, y: number, _labelInside: boolean, taskName: string): void {
        const mW = this.getTaskHeight() / 2 - TASK_BODY_MARGIN;
        if (SvgUtils.isClipped(x1 - mW, x1 + mW, this.chart.containerWidth))
            return;
        const fillColor = task.fillColor ? ColorUtils.convertSprintColorToRgba(task.fillColor) : '#808080';
        const borderColor = task.borderColor || '#888888';
        const points = [
            `${x1},${y - mW}`, `${x1 + mW},${y}`,
            `${x1},${y + mW}`, `${x1 - mW},${y}`,
            `${x1},${y - mW}`,
        ].join(' ');
        const poly = SvgUtils.createPolygon({
            class: 'milestone-body',
            points,
            fill: fillColor,
            stroke: this.showDetail(this.ganttZoom.renderMilestoneBorderAtDayWidth) ? borderColor : 'none',
            'stroke-width': '1',
            'vector-effect': 'non-scaling-stroke'
        });
        poly.appendChild(SvgUtils.createTitle(this.generateTaskToolTip(task)));
        g.appendChild(poly);
        const textColor = task.textColor || ColorUtils.intToHex(this.theme.ganttTheme.taskTextColor, '#303030');
        const labelX = x1 + mW / 2 + 10;
        const showDate = this.showDetail(this.ganttZoom.renderMilestoneStartDateAtDayWidth);
        const showTime = this.showDetail(this.ganttZoom.renderMilestoneStartTimeAtDayWidth);
        let dateStr = '';
        if (task.start && showDate && showTime)
            dateStr = DateUtils.toLocalYMDHMString(task.start, this.dateTimeFormat);
        else if (task.start && showDate)
            dateStr = new Intl.DateTimeFormat('en-DE', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit'
            }).format(task.start);
        else if (task.start && showTime)
            dateStr = new Intl.DateTimeFormat('en-DE', {
                hour: '2-digit',
                minute: '2-digit',
                hour12: false
            }).format(task.start);
        const label = dateStr ? `${taskName} (${dateStr})` : taskName;
        const lbl = SvgUtils.createText(labelX, y, label, {
            class: 'milestone-label',
            fill: textColor, 'font-size': '12', 'font-family': 'sans-serif', 'dominant-baseline': 'middle',
        });
        lbl.appendChild(SvgUtils.createTitle(this.generateTaskToolTip(task)));
        g.appendChild(lbl);
    }

    drawRelation(g: SVGElement, sourceTask: TaskDto, y2: number, targetTask: TaskDto, y1: number): void {
        const th = this.getTaskHeight();
        const milestoneHalfWidth = th / 2 - TASK_BODY_MARGIN;
        const x1 = this.calculateX(targetTask.finish!, DateUtils.withTime(targetTask.finish, 8, 0), SECONDS_PER_DAY)
            - this.calendarXAxes.dayOfWeek.getWidth() / 2 + (targetTask.milestone ? milestoneHalfWidth : 0);
        const x2 = this.calculateX(sourceTask.start!, DateUtils.withTime(sourceTask.start, 8, 0), SECONDS_PER_DAY)
            - this.calendarXAxes.dayOfWeek.getWidth() / 2 - (sourceTask.milestone ? milestoneHalfWidth : 0);
        const xExit = Math.max(x1, x2) + RELATION_CORNER_LENGTH;
        const xEntry = x2 - RELATION_CORNER_LENGTH;
        const rowDirection = y2 > y1 ? 1 : -1;
        const yGap = y2 - rowDirection * (th + 1) / 2;
        const exitDirection = Math.sign(yGap - y1);
        const entryDirection = Math.sign(y2 - yGap);
        const radius = Math.min(4, Math.abs(yGap - y1) / 2, Math.abs(y2 - yGap) / 2, (xExit - xEntry) / 2);
        if (xExit <= 0 || Math.min(x1, xEntry) >= this.chart.containerWidth)
            return;
        const arrowColor = (sourceTask.critical && targetTask.critical)
            ? ColorUtils.intToHex(this.theme.ganttTheme.criticalRelationColor, '#ff0000')
            : ColorUtils.intToHex(this.theme.ganttTheme.relationColor, '#3466ed');
        const arrowLength = 5;
        const path = [
            `M ${x1} ${y1}`,
            `H ${xExit - radius}`,
            `Q ${xExit} ${y1} ${xExit} ${y1 + exitDirection * radius}`,
            `V ${yGap - exitDirection * radius}`,
            `Q ${xExit} ${yGap} ${xExit - radius} ${yGap}`,
            `H ${xEntry + radius}`,
            `Q ${xEntry} ${yGap} ${xEntry} ${yGap + entryDirection * radius}`,
            `V ${y2 - entryDirection * radius}`,
            `Q ${xEntry} ${y2} ${xEntry + radius} ${y2}`,
            `H ${x2 - arrowLength}`,
        ].join(' ');
        g.appendChild(SvgUtils.createPath({
            d: path, fill: 'none', stroke: arrowColor, 'stroke-width': RELATION_STROKE_WIDTH,
        }));
        g.appendChild(SvgUtils.createPolygon({
            points: `${x2 - arrowLength},${y2 - 3} ${x2 - arrowLength},${y2 + 3} ${x2},${y2}`,
            fill: arrowColor,
        }));
    }

    drawTaskRelations(g: SVGElement, task: TaskDto): void {
        if (!task.start || !task.finish || !this.showDetail(this.ganttZoom.renderDependenciesAtDayWidth) || !task.predecessors?.length)
            return;

        const y = this._calendarH + task.rowIndex * (this.getTaskHeight() + 1) + this.getTaskHeight() / 2;
        for (const rel of task.predecessors) {
            if (!rel.visible) continue;
            const targetTask = this._taskById[String(rel.predecessorId)];
            if (!targetTask?.finish) continue;
            const y1 = this._calendarH + targetTask.rowIndex * (this.getTaskHeight() + 1) + this.getTaskHeight() / 2;
            const relation = SvgUtils.createGroup({class: 'task-dependency'});
            this.drawRelation(relation, task, y, targetTask, y1);
            g.appendChild(relation);
        }
    }

    drawRibbon(g: SVGElement, y1: number, x1: number, y2: number, delta1: number, delta2: number, ribbonColor: string): void {
        const points = `${x1},${y2} ${x1 + delta1},${y1} ${x1 + delta1 + delta2},${y1} ${x1 + delta2},${y2}`;
        g.appendChild(SvgUtils.createPolygon({points, fill: ribbonColor}));
    }

    drawStoryBody(g: SVGElement, task: TaskDto, x1: number, x2: number, y: number, marker: string | null): void {
        if (SvgUtils.isClipped(x1, x2, this.chart.containerWidth))
            return;
        const fillColor = task.fillColor
            ? ColorUtils.convertSprintColorToRgba(task.fillColor)
            : ColorUtils.intToHex(this.theme.ganttTheme.storyColor, '#444444');
        const tooltip = this.generateTaskToolTip(task);
        const th = this.getTaskHeight();

        if (marker == null) {
            const y1 = y + TASK_BODY_MARGIN;
            const thickness = 2;
            g.appendChild(SvgUtils.createRect(x1, y1 - th / 2, x2 - x1 + 1, thickness, {fill: fillColor}));
            g.appendChild(SvgUtils.createRect(x1, y1 - th / 2 + thickness, thickness, th - TASK_BODY_MARGIN * 2 - thickness, {fill: fillColor}));
            g.appendChild(SvgUtils.createRect(x2 - 1, y1 - th / 2 + thickness, thickness, th - TASK_BODY_MARGIN * 2 - thickness, {fill: fillColor}));
            if (x2 - x1 - 1 > 0) {
                const tooltipRect = SvgUtils.createRect(x1 + 1, y1 - th / 2, x2 - x1 - 1, th - thickness * 2, {
                    fill: 'none',
                    'pointer-events': 'all'
                });
                tooltipRect.appendChild(SvgUtils.createTitle(tooltip));
                g.appendChild(tooltipRect);
            }
        } else {
            const stY1 = y - th / 2 + 1;
            const stY2 = stY1 + th - 1;
            const clipId = 'sr-' + String(task.id).replace(/-/g, '');
            g.appendChild(SvgUtils.createClipPath(clipId, x1 + 1, y - th / 2 + 2, x2 - x1 - 1, th - 4));
            const grp = SvgUtils.createGroup({'clip-path': `url(#${clipId})`});
            let cur = fillColor;
            for (let rx = x1 - 16; rx < x2; rx += 16) {
                this.drawRibbon(grp, stY1, rx, stY2, 25, 15, cur);
                cur = (cur === fillColor) ? '#ffffff' : fillColor;
            }
            g.appendChild(grp);
        }
    }

    drawTask(
        g: SVGElement,
        _gantUniqueId: number,
        task: TaskDto,
        doDrawId: boolean,
        labelInside: boolean,
        alien: boolean,
        marker: string | null,
        conflict: unknown,
        _drawOutOfOffice: boolean,
    ): void {
        if (!task.start || !task.finish) return;
        const x1 = this.calculateX(task.start, DateUtils.withTime(task.start, 8, 0)!, SECONDS_PER_DAY) - this.calendarXAxes.dayOfWeek.getWidth() / 2;
        const x2 = this.calculateX(task.finish, DateUtils.withTime(task.finish, 8, 0)!, SECONDS_PER_DAY) - this.calendarXAxes.dayOfWeek.getWidth() / 2;
        const y = this._calendarH + task.rowIndex * (this.getTaskHeight() + 1) + this.getTaskHeight() / 2;

        this.drawTaskInner(g, task, x1, x2, y, labelInside, alien, marker, conflict);
        if (doDrawId) this.drawId(g, task, y);
    }

    drawTaskInner(
        g: SVGElement,
        task: TaskDto,
        x1: number, x2: number, y: number,
        _labelInside: boolean,
        alien: boolean,
        marker: string | null,
        conflict: unknown,
    ): void {
        const textColor = task.textColor || ColorUtils.intToHex(this.theme.ganttTheme.taskTextColor, '#303030');
        const taskName = task.name || '';
        const th = this.getTaskHeight();

        if (task.milestone && !task.story) {
            this.drawMilestoneTask(g, task, x1, y, false, taskName);
        } else if (task.story) {
            this.drawStoryBody(g, task, x1, x2, y, marker);
            const storyLabelX = x2 + 10;
            if (storyLabelX < this.containerWidth + 40) {
                const storyLabel = SvgUtils.createText(storyLabelX, y, taskName, {
                    fill: textColor, 'font-size': '12', 'font-family': 'sans-serif',
                    'font-weight': 'bold', 'dominant-baseline': 'middle',
                });
                storyLabel.appendChild(SvgUtils.createTitle(this.generateTaskToolTip(task)));
                g.appendChild(storyLabel);
            }
        } else {
            const progress = task.progress || 0;
            this.drawTaskBody(g, task, x1, x2, y, alien, progress);
            this.drawConflictMarker(g, y, conflict);
            if (this.showDetail(this.ganttZoom.renderTaskBorderAtDayWidth))
                this.drawTaskBorder(g, task, x1, x2, y);
            this.drawManualMarker(g, task, x1, y, false);
            if (this.showDetail(this.ganttZoom.renderTaskProgressTextAtDayWidth))
                this.drawProgress(g, task, x1, x2, y, progress);

            const labelRight = x2 + TASK_NAME_TO_TASK_GAP;
            if (labelRight < this.containerWidth + 40) {
                const clipId3 = 'tn-' + String(task.id).replace(/-/g, '');
                const clipW3 = Math.max(0, this.containerWidth - labelRight);
                if (clipW3 > 8) {
                    g.appendChild(SvgUtils.createClipPath(clipId3, labelRight, y - th, clipW3, th * 2));
                    const taskLabel = (task.key && this.showDetail(this.ganttZoom.renderTaskKeyAtDayWidth) ? task.key + ' ' : '') + taskName;
                    const nameLabel = SvgUtils.createText(labelRight, y, taskLabel, {
                        fill: textColor, 'font-size': '12', 'font-family': 'sans-serif',
                        'dominant-baseline': 'middle', 'clip-path': `url(#${clipId3})`,
                    });
                    nameLabel.appendChild(SvgUtils.createTitle(this.generateTaskToolTip(task)));
                    g.appendChild(nameLabel);
                }
            }
            this.drawUserName(g, task, x1, y, textColor);
        }
    }

    drawProgress(g: SVGElement, task: TaskDto, x1: number, x2: number, y: number, progress: number) {
        if (SvgUtils.isClipped(x1, x2, this.chart.containerWidth))
            return;
        const fillColor = this.theme.burndownTheme.getAuthorColor(28);
        const th = this.getTaskHeight();
        if (progress > 0) {
            //draw progress % if it fits inside the task
            let blendedColor = ColorUtils.calculateColorBlending(fillColor, ColorUtils.WHITE);
            if (progress > 0.5) {
                blendedColor = ColorUtils.calculateColorBlending(fillColor, blendedColor);// we are drawing two times
            }
            const highestContrast = ColorUtils.highestContrast(blendedColor);
            const barWidth = x2 - x1;
            const text = `${Math.round(progress * 100)}%`;
            const fm = new FontMetrics(AbstractGanttRenderer.taskProgressFont);
            const textWidth = fm.stringWidth(text);
            if (textWidth < barWidth) {
                const clipId2 = 'pt-' + String(task.id).replace(/-/g, '');
                g.appendChild(SvgUtils.createClipPath(clipId2, x1 + 1, y - th / 2 + RESOURCE_NAME_TO_TASK_GAP, x2 - x1 - 3, th - 6));
                g.appendChild(SvgUtils.createText(x1 + barWidth / 2, y, text, {
                    class: 'task-progress-text',
                    fill: highestContrast,
                    'font-size': AbstractGanttRenderer.taskProgressFont.size,
                    'font-family': AbstractGanttRenderer.taskProgressFont.family,
                    'text-anchor': 'middle',
                    'dominant-baseline': 'alphabetic',
                    dy: '0.35em',
                    'clip-path': `url(#${clipId2})`,
                }));
            }
        }
    }

    drawTaskBody(g: SVGElement, task: TaskDto, x1: number, x2: number, y: number, alien: boolean, progress: number): void {
        if (SvgUtils.isClipped(x1, x2, this.chart.containerWidth))
            return;
        const fillColor = task.fillColor;
        const tooltip = this.generateTaskToolTip(task);
        const th = this.getTaskHeight();

        if (!alien) {
            const y1 = y - th / 2 + TASK_BODY_MARGIN;
            const h = th - TASK_BODY_MARGIN * 2;
            if (x2 > x1) {
                if (!this.showDetail(this.ganttZoom.renderTaskNonWorkingDaysAtDayWidth)) {
                    const rect = SvgUtils.createRect(x1, y1, x2 - x1, h, {
                        class: 'task-body',
                        fill: ColorUtils.convertSprintColorToRgba(fillColor),
                    });
                    rect.appendChild(SvgUtils.createTitle(tooltip));
                    g.appendChild(rect);
                } else {
                    // Calculate days between start and finish (both truncated to day precision)
                    const startTruncated = DateUtils.getDayMidnight(task.start!);
                    const finishTruncated = DateUtils.getDayMidnight(task.finish!);
                    const days = Math.floor((finishTruncated.getTime() - startTruncated.getTime()) / DateUtils.MS);

                    for (let day = 0; day <= days; day++) {
                        const currentDay = new Date(startTruncated.getTime() + day * DateUtils.MS);
                        let segX: number, segW: number;

                        if (isWorkingDay(currentDay, this.getCalendarExceptions(task))) {
                            const fill = ColorUtils.convertSprintColorToRgba(fillColor);
                            if (days === 0) {
                                // This is the left and right end
                                segX = x1;
                                segW = x2 - x1;
                            } else if (day === 0) {
                                // This is the left end
                                const currentDayAt8 = DateUtils.withTime(currentDay, 8, 0)!;
                                const nextDayAt8 = new Date(currentDayAt8.getTime() + SECONDS_PER_DAY * 1000);
                                const xFinish = this.calculateX(nextDayAt8, currentDayAt8, SECONDS_PER_DAY) - this.calendarXAxes.dayOfWeek.getWidth() / 2;
                                segX = x1;
                                segW = xFinish - x1;
                            } else if (day === days) {
                                // This is the right end
                                const currentDayAt8 = DateUtils.withTime(currentDay, 8, 0)!;
                                const xStart = this.calculateX(currentDayAt8, currentDayAt8, SECONDS_PER_DAY) - this.calendarXAxes.dayOfWeek.getWidth() / 2;
                                segX = xStart;
                                segW = x2 - xStart;
                            } else {
                                // This is the middle
                                const currentDayAt8 = DateUtils.withTime(currentDay, 8, 0)!;
                                const xStart = this.calculateX(currentDayAt8, currentDayAt8, SECONDS_PER_DAY) - this.calendarXAxes.dayOfWeek.getWidth() / 2;
                                segX = xStart;
                                segW = this.calendarXAxes.dayOfWeek.getWidth();
                            }
                            const rect = SvgUtils.createRect(segX, y1, segW, h, {class: 'task-body', fill});
                            rect.appendChild(SvgUtils.createTitle(tooltip));
                            g.appendChild(rect);
                        } else {
                            // Weekend/non-working day
                            // const weekendFill = ColorUtils.hexToRgbaWithAlpha(fillColor, this.theme.ganttTheme.taskWeekEndTransparency);
                            // const currentDayAt8 = DateUtils.withTime(currentDay, 8, 0)!;
                            // const xStart = this.calculateX(currentDayAt8, currentDayAt8, SECONDS_PER_DAY) - this.calendarXAxes.dayOfWeek.getWidth() / 2;
                            // const rectW = SvgUtils.createRect(xStart, y1, this.calendarXAxes.dayOfWeek.getWidth(), h, {
                            //     class: 'task-body',
                            //     fill: weekendFill
                            // });
                            // rectW.appendChild(SvgUtils.createTitle(tooltip));
                            // g.appendChild(rectW);
                        }
                    }
                }

                // Progress bar
                // if (progress > 0.0 && this.showDetail(this.ganttZoom.renderTaskProgressBarAtDayWidth)) {
                //     const progressFill = ColorUtils.intToHex(this.theme.ganttTheme.progressColor, '#0000ff');
                //     const progressW = (x2 - x1) * progress;
                //     if (progressW > 0) {
                //         const pRect = SvgUtils.createRect(x1, y1 + h - TASK_PROGRESS_BOTTOM_GAP - TASK_PROGRESS_HEIGHT, progressW, TASK_PROGRESS_HEIGHT, {
                //             class: 'task-progress-bar',
                //             fill: progressFill
                //         });
                //         pRect.appendChild(SvgUtils.createTitle(tooltip));
                //         g.appendChild(pRect);
                //         // if (progress < 1.0) {
                //         //     g.appendChild(SvgUtils.createRect(x1 + progressW, y - th / 2 + 2, 1, th - 4, {fill: '#000000'}));
                //         // }
                //     }
                // }
            }
        } else {
            const aY1 = y - th / 2 + 1;
            const aY2 = aY1 + th - 1;
            const clipId = 'ta-' + String(task.id).replace(/-/g, '');
            g.appendChild(SvgUtils.createClipPath(clipId, x1, y - th / 2 + 2, x2 - x1 - 1, th - 4));
            const grp = SvgUtils.createGroup({'clip-path': `url(#${clipId})`});
            let cur = fillColor ? ColorUtils.convertSprintColorToRgba(fillColor) : '#aaaaaa';
            for (let ax = x1 - 16; ax < x2; ax += 16) {
                this.drawRibbon(grp, aY1, ax, aY2, 25, 15, cur);
                cur = (cur === (fillColor ? ColorUtils.convertSprintColorToRgba(fillColor) : '#aaaaaa')) ? '#ffffff' : (fillColor ? ColorUtils.convertSprintColorToRgba(fillColor) : '#aaaaaa');
            }
            g.appendChild(grp);
        }
    }

    drawTick(_g: SVGElement, _time: string, _x: number, _y: number, _alignment: string): void { /* commented out in Java */
    }

    generateTaskNameToolTip(resourceName: string | null | undefined, resourceUtilization: string | null | undefined, country: string | null | undefined, state: string | null | undefined): string {
        let tip = resourceName || '';
        if (resourceUtilization) tip += `\nAvailability ${resourceUtilization}`;
        if (country) tip += `\nCountry ${country}`;
        if (state) tip += `\nState ${state}`;
        return tip;
    }

    generateTaskToolTip(task: TaskDto): string {
        let s = task.name || '';
        if (task.key)
            s += `\nKey: ${task.key}`;
        if (task.start)
            s += `\nStart: ${DateUtils.toLocalYMDHMString(task.start, this.dateTimeFormat)}`;
        if (task.finish)
            s += `\nFinish: ${DateUtils.toLocalYMDHMString(task.finish, this.dateTimeFormat)}`;
        if (task.assignedUserName)
            s += `\nResource: ${task.assignedUserName}`;
        if (task.assignedUserAvailability)
            s += `\nAvailability: ${task.assignedUserAvailability}`;
        if (task.progress && task.progress > 0)
            s += `\nProgress: ${Math.round(task.progress * 100)}%`;
        if (task.notes)
            s += `\nNotes: ${task.notes}`;
        return s;
    }

    protected getCalendarExceptions(task: TaskDto): CalendarException[] | undefined {
        return task.calendarId ? this.calendarExceptionsById.get(String(task.calendarId)) : undefined;
    }

    private showDetail(threshold: number): boolean {
        return this.dayWidth * this.visualScale >= threshold;
    }

    private drawUserName(g: SVGElement, task: TaskDto, x1: number, y: number, textColor: string) {
        if (task.assignedUserName) {
            const resourceName = task.assignedUserName;
            const fm = new FontMetrics(AbstractGanttRenderer.graphFont);
            const textWidth = fm.stringWidth(resourceName);
            if (SvgUtils.isClipped(x1, x1 + textWidth, this.chart.containerWidth))
                return;
            const th = this.getTaskHeight();
            const rnX = x1 - textWidth - RESOURCE_NAME_TO_TASK_GAP;
            if (rnX > -100) {
                const clipId4 = 'rn-' + String(task.id).replace(/-/g, '');
                const clipW4 = Math.min(120, x1 > 0 ? x1 : 0);
                if (clipW4 > 8) {
                    g.appendChild(SvgUtils.createClipPath(clipId4, Math.max(0, rnX), y - th, clipW4, th * 2));
                    const rLabel = SvgUtils.createText(rnX + textWidth, y, resourceName, {
                        fill: textColor, 'font-size': '12', 'font-family': 'sans-serif',
                        'text-anchor': 'end', 'dominant-baseline': 'middle',
                        'clip-path': `url(#${clipId4})`,
                    });
                    rLabel.appendChild(SvgUtils.createTitle(
                        this.generateTaskNameToolTip(resourceName, task.assignedUserAvailability, task.assignedUserCountry, task.assignedUserState)));
                    g.appendChild(rLabel);
                }
            }
        }
    }
}
