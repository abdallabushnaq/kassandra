// gantt/abstract-gantt-renderer.ts
// Base class for Gantt renderers. Mirrors Java: AbstractGanttRenderer extends AbstractRenderer
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {ColorUtils} from '../ColorUtils.js';
import {SvgUtils} from '../SvgUtils.js';
import {DateUtils} from '../DateUtils.js';
import {AbstractRenderer} from '../AbstractRenderer.js';
import {getCalendarException, isWorkingDay} from './date-helpers.js';
import {TaskDto} from './dto/TaskDto.js';
import {CalendarException} from './dto/CalendarException.js';
import {FontMetrics} from "../FontMetrics.js";
import {FontSpec} from "../FontSpec.js";
import {AbstractChart} from '../AbstractChart.js';
import {GraphColorUtil} from "../GraphColorUtil.js";

// ── Constants (mirrors Java AbstractGanttRenderer field declarations) ────────
const FINE_LINE_STROKE_WIDTH = 1.0;
export const LINE_HEIGHT = 18;
const RELATION_CORNER_LENGTH = 14;
const RESOURCE_NAME_TO_TASK_GAP = 3;
export const SECONDS_PER_DAY = 85 * 6 * 60; // 30600
const TASK_BODY_BORDER = 1;
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

    protected constructor(chart: AbstractChart/*, milestones: Milestones*/, preRun: number, postRun: number) {
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
    //             letter.appendChild(SvgUtils.createSvgElement('title', {}, exception.type || 'Off-day'));
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

    drawConflictMarker(_g: SVGElement, _y: number, _conflict: unknown): void { /* team planner only */
    }

    drawCriticalMarker(g: SVGElement, task: TaskDto, x1: number, x2: number, y: number): void {
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

        for (let day = 0; day <= days; day++) {
            const currentDay = new Date(startTruncated.getTime() + day * DateUtils.MS);
            const working = isWorkingDay(currentDay, this.getCalendarExceptions(task));

            if (working) {
                if (days === 0) {
                    // This is the left and right end
                    g.appendChild(SvgUtils.createRect(x1, y - th / 2 + TASK_BODY_BORDER, x2 - x1 + 1, 1, {fill: borderColor}));
                    g.appendChild(SvgUtils.createRect(x1, y + th / 2 - TASK_BODY_BORDER - 1, x2 - x1 + 1, 1, {fill: borderColor}));
                    g.appendChild(SvgUtils.createRect(x1, y - th / 2 + TASK_BODY_BORDER + 1, 1, th - TASK_BODY_BORDER * 2 - 2, {fill: borderColor}));
                    g.appendChild(SvgUtils.createRect(x2, y - th / 2 + TASK_BODY_BORDER + 1, 1, th - TASK_BODY_BORDER * 2 - 2, {fill: borderColor}));
                } else if (day === 0) {
                    // This is the left end
                    const currentDayAt8 = DateUtils.withTime(currentDay, 8, 0)!;
                    const nextDayAt8 = new Date(currentDayAt8.getTime() + SECONDS_PER_DAY * 1000);
                    const xFinish = this.calculateX(nextDayAt8, currentDayAt8, SECONDS_PER_DAY) - this.calendarXAxes.dayOfWeek.getWidth() / 2;
                    g.appendChild(SvgUtils.createRect(x1, y - th / 2 + TASK_BODY_BORDER, xFinish - x1, 1, {fill: borderColor}));
                    g.appendChild(SvgUtils.createRect(x1, y + th / 2 - TASK_BODY_BORDER - 1, xFinish - x1, 1, {fill: borderColor}));
                    g.appendChild(SvgUtils.createRect(x1, y - th / 2 + TASK_BODY_BORDER + 1, 1, th - TASK_BODY_BORDER * 2 - 2, {fill: borderColor}));
                } else if (day === days) {
                    // This is the right end
                    const currentDayAt8 = DateUtils.withTime(currentDay, 8, 0)!;
                    const xStart = this.calculateX(currentDayAt8, currentDayAt8, SECONDS_PER_DAY) - this.calendarXAxes.dayOfWeek.getWidth() / 2;
                    g.appendChild(SvgUtils.createRect(xStart, y - th / 2 + TASK_BODY_BORDER, x2 - xStart + 1, 1, {fill: borderColor}));
                    g.appendChild(SvgUtils.createRect(xStart, y + th / 2 - TASK_BODY_BORDER - 1, x2 - xStart + 1, 1, {fill: borderColor}));
                    g.appendChild(SvgUtils.createRect(x2, y - th / 2 + TASK_BODY_BORDER + 1, 1, th - TASK_BODY_BORDER * 2 - 2, {fill: borderColor}));
                } else {
                    // This is the middle
                    const currentDayAt8 = DateUtils.withTime(currentDay, 8, 0)!;
                    const xStart = this.calculateX(currentDayAt8, currentDayAt8, SECONDS_PER_DAY) - this.calendarXAxes.dayOfWeek.getWidth() / 2;
                    g.appendChild(SvgUtils.createRect(xStart, y - th / 2 + TASK_BODY_BORDER, this.calendarXAxes.dayOfWeek.getWidth(), 1, {fill: borderColor}));
                    g.appendChild(SvgUtils.createRect(xStart, y + th / 2 - TASK_BODY_BORDER - 1, this.calendarXAxes.dayOfWeek.getWidth(), 1, {fill: borderColor}));
                }
            } else {
                // Non-working day (weekend) - draw dashed border
                const currentDayAt8 = DateUtils.withTime(currentDay, 8, 0)!;
                const xStart = this.calculateX(currentDayAt8, currentDayAt8, SECONDS_PER_DAY) - this.calendarXAxes.dayOfWeek.getWidth() / 2;
                for (let i = 0; i < this.calendarXAxes.dayOfWeek.getWidth() - 1; i++) {
                    const x = i + xStart;
                    if (x % 4 === 0) {
                        g.appendChild(SvgUtils.createRect(x, y - th / 2 + TASK_BODY_BORDER, 2, 1, {fill: borderColor}));
                        g.appendChild(SvgUtils.createRect(x, y + th / 2 - TASK_BODY_BORDER - 1, 2, 1, {fill: borderColor}));
                    }
                }
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
            keyText.appendChild(SvgUtils.createSvgElement('title', {}, task.name));
        g.appendChild(keyText);
    }

    drawManualMarker(g: SVGElement, task: TaskDto, x1: number, y: number, _labelInside: boolean): void {
        if (task.manuallyScheduled) {
            g.appendChild(SvgUtils.createRect(x1, y - this.getTaskHeight() / 2, 1, this.getTaskHeight(), {fill: '#ff0000'}));
        }
    }

    drawMilestoneTask(g: SVGElement, task: TaskDto, x1: number, y: number, _labelInside: boolean, taskName: string): void {
        const mW = this.getTaskHeight() / 2 - TASK_BODY_BORDER;
        if (SvgUtils.isClipped(x1 - mW, x1 + mW, this.chart.containerWidth))
            return;
        const fillColor = task.fillColor ? ColorUtils.convertSprintColorToRgba(task.fillColor) : '#808080';
        const borderColor = task.borderColor || '#888888';
        const points = [
            `${x1},${y - mW}`, `${x1 + mW},${y}`,
            `${x1},${y + mW}`, `${x1 - mW},${y}`,
            `${x1},${y - mW}`,
        ].join(' ');
        const poly = SvgUtils.createSvgElement('polygon', {
            points,
            fill: fillColor,
            stroke: borderColor,
            'stroke-width': '1'
        });
        poly.appendChild(SvgUtils.createSvgElement('title', {}, this.generateTaskToolTip(task)));
        g.appendChild(poly);
        const textColor = task.textColor || ColorUtils.intToHex(this.theme.ganttTheme.taskTextColor, '#303030');
        const labelX = x1 + mW / 2 + 10;
        const dateStr = task.start ? DateUtils.toLocalYMDHMString(task.start, this.dateTimeFormat) : '';
        const label = `${taskName || ''} (${dateStr})`;
        const lbl = SvgUtils.createText(labelX, y, label, {
            fill: textColor, 'font-size': '12', 'font-family': 'sans-serif', 'dominant-baseline': 'middle',
        });
        lbl.appendChild(SvgUtils.createSvgElement('title', {}, this.generateTaskToolTip(task)));
        g.appendChild(lbl);
    }

    drawRelation(g: SVGElement, sourceTask: TaskDto, y2: number, targetTask: TaskDto, y1: number): void {
        const signum = (n: number) => (n > 0 ? 1 : n < 0 ? -1 : 0);
        const sign = signum(y2 - y1);
        let yEnd: number, yMid: number;
        const th = this.getTaskHeight();
        if (sign > 0) {
            y2 -= th / 2 - TASK_BODY_BORDER;
            yEnd = y2;
            yMid = y2 - 5;
        } else {
            y2 += th / 2 - TASK_BODY_BORDER;
            yEnd = y2;
            yMid = y2 + 5;
        }
        const x1 = this.calculateX(targetTask.finish!, DateUtils.withTime(targetTask.finish, 8, 0), SECONDS_PER_DAY) - this.calendarXAxes.dayOfWeek.getWidth() / 2;
        const x2 = RELATION_CORNER_LENGTH + this.calculateX(sourceTask.start!, DateUtils.withTime(sourceTask.start, 8, 0), SECONDS_PER_DAY) - this.calendarXAxes.dayOfWeek.getWidth() / 2 - RESOURCE_NAME_TO_TASK_GAP;
        if (SvgUtils.isClipped(x1, x2, this.chart.containerWidth))
            return;
        const arrowColor = (sourceTask.critical && targetTask.critical)
            ? ColorUtils.intToHex(this.theme.ganttTheme.criticalRelationColor, '#ff0000')
            : ColorUtils.intToHex(this.theme.ganttTheme.relationColor, '#3466ed');
        g.appendChild(SvgUtils.createRect(x1 + 1, y1, x2 - x1, 1, {fill: arrowColor}));
        g.appendChild(SvgUtils.createRect(x2, y1 + 1, 1, yMid - y1, {fill: arrowColor}));
        const d = 5;
        const pts = y2 > y1
            ? `${x2 - d},${yEnd - d + sign} ${x2 + d},${yEnd - d + sign} ${x2},${yEnd + sign}`
            : `${x2 + d},${yEnd + d + sign} ${x2 - d},${yEnd + d + sign} ${x2},${yEnd + sign}`;
        g.appendChild(SvgUtils.createSvgElement('polygon', {points: pts, fill: arrowColor}));
    }

    drawRibbon(g: SVGElement, y1: number, x1: number, y2: number, delta1: number, delta2: number, ribbonColor: string): void {
        const points = `${x1},${y2} ${x1 + delta1},${y1} ${x1 + delta1 + delta2},${y1} ${x1 + delta2},${y2}`;
        g.appendChild(SvgUtils.createSvgElement('polygon', {points, fill: ribbonColor}));
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
            const y1 = y + TASK_BODY_BORDER;
            const thickness = 2;
            g.appendChild(SvgUtils.createRect(x1, y1 - th / 2, x2 - x1 + 1, thickness, {fill: fillColor}));
            g.appendChild(SvgUtils.createRect(x1, y1 - th / 2 + thickness, thickness, th - TASK_BODY_BORDER * 2 - thickness, {fill: fillColor}));
            g.appendChild(SvgUtils.createRect(x2 - 1, y1 - th / 2 + thickness, thickness, th - TASK_BODY_BORDER * 2 - thickness, {fill: fillColor}));
            if (x2 - x1 - 1 > 0) {
                const tooltipRect = SvgUtils.createRect(x1 + 1, y1 - th / 2, x2 - x1 - 1, th - thickness * 2, {
                    fill: 'none',
                    'pointer-events': 'all'
                });
                tooltipRect.appendChild(SvgUtils.createSvgElement('title', {}, tooltip));
                g.appendChild(tooltipRect);
            }
        } else {
            const stY1 = y - th / 2 + 1;
            const stY2 = stY1 + th - 1;
            const clipId = 'sr-' + String(task.id).replace(/-/g, '');
            g.appendChild(SvgUtils.createClipPath(clipId, x1 + 1, y - th / 2 + 2, x2 - x1 - 1, th - 4));
            const grp = SvgUtils.createSvgElement('g', {'clip-path': `url(#${clipId})`});
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
        drawRelations: boolean,
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
        if (drawRelations && task.predecessors?.length) {
            for (const rel of task.predecessors) {
                if (!rel.visible) continue;
                const targetTask = this._taskById[String(rel.predecessorId)];
                if (!targetTask) continue;
                const y1 = this._calendarH + targetTask.rowIndex * (this.getTaskHeight() + 1) + this.getTaskHeight() / 2;
                this.drawRelation(g, task, y, targetTask, y1);
            }
        }
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
                storyLabel.appendChild(SvgUtils.createSvgElement('title', {}, this.generateTaskToolTip(task)));
                g.appendChild(storyLabel);
            }
        } else {
            const progress = task.progress || 0;
            this.drawTaskBody(g, task, x1, x2, y, alien, progress);
            this.drawConflictMarker(g, y, conflict);
            this.drawCriticalMarker(g, task, x1, x2, y);
            this.drawManualMarker(g, task, x1, y, false);
            this.drawProgress(g, task, x1, x2, y, progress);

            const labelRight = x2 + TASK_NAME_TO_TASK_GAP;
            if (labelRight < this.containerWidth + 40) {
                const clipId3 = 'tn-' + String(task.id).replace(/-/g, '');
                const clipW3 = Math.max(0, this.containerWidth - labelRight);
                if (clipW3 > 8) {
                    g.appendChild(SvgUtils.createClipPath(clipId3, labelRight, y - th, clipW3, th * 2));
                    const nameLabel = SvgUtils.createText(labelRight, y, (task.key ? task.key + ' ' : '') + taskName, {
                        fill: textColor, 'font-size': '12', 'font-family': 'sans-serif',
                        'dominant-baseline': 'middle', 'clip-path': `url(#${clipId3})`,
                    });
                    nameLabel.appendChild(SvgUtils.createSvgElement('title', {}, this.generateTaskToolTip(task)));
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
            //draw progress if it fits inside the task
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
                    fill: highestContrast,
                    'font-size': AbstractGanttRenderer.taskProgressFont.size,
                    'font-family': AbstractGanttRenderer.taskProgressFont.family,
                    'text-anchor': 'middle',
                    'dominant-baseline': 'middle',
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
            const y1 = y - th / 2 + TASK_BODY_BORDER;
            const h = th - TASK_BODY_BORDER * 2;
            if (x2 - x1 - 1 - 1 > 0) {
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
                            segW = x2 - xStart + 1;
                        } else {
                            // This is the middle
                            const currentDayAt8 = DateUtils.withTime(currentDay, 8, 0)!;
                            const xStart = this.calculateX(currentDayAt8, currentDayAt8, SECONDS_PER_DAY) - this.calendarXAxes.dayOfWeek.getWidth() / 2;
                            segX = xStart;
                            segW = this.calendarXAxes.dayOfWeek.getWidth();
                        }
                        const rect = SvgUtils.createRect(segX, y1, segW, h, {fill});
                        rect.appendChild(SvgUtils.createSvgElement('title', {}, tooltip));
                        g.appendChild(rect);
                    } else {
                        // Weekend/non-working day
                        const weekendFill = ColorUtils.hexToRgbaWithAlpha(fillColor, this.theme.ganttTheme.taskWeekEndTransparency);
                        const currentDayAt8 = DateUtils.withTime(currentDay, 8, 0)!;
                        const xStart = this.calculateX(currentDayAt8, currentDayAt8, SECONDS_PER_DAY) - this.calendarXAxes.dayOfWeek.getWidth() / 2;
                        const rectW = SvgUtils.createRect(xStart, y1, this.calendarXAxes.dayOfWeek.getWidth(), h, {fill: weekendFill});
                        rectW.appendChild(SvgUtils.createSvgElement('title', {}, tooltip));
                        g.appendChild(rectW);
                    }
                }

                // Progress bar
                if (progress > 0.0) {
                    const progressFill = task.progressColor
                        ? ColorUtils.convertSprintColorToRgba(task.progressColor)
                        : ColorUtils.hexToRgbaWithAlpha(fillColor, 200);
                    const progressW = Math.floor((x2 - x1) * progress - 1);
                    if (progressW > 0) {
                        const pRect = SvgUtils.createRect(x1 + 1, y1 + 2, progressW, h - 4, {fill: progressFill});
                        pRect.appendChild(SvgUtils.createSvgElement('title', {}, tooltip));
                        g.appendChild(pRect);
                        if (progress < 1.0) {
                            g.appendChild(SvgUtils.createRect(x1 + progressW, y - th / 2 + 2, 1, th - 4, {fill: '#000000'}));
                        }
                    }
                }
            }
        } else {
            const aY1 = y - th / 2 + 1;
            const aY2 = aY1 + th - 1;
            const clipId = 'ta-' + String(task.id).replace(/-/g, '');
            g.appendChild(SvgUtils.createClipPath(clipId, x1, y - th / 2 + 2, x2 - x1 - 1, th - 4));
            const grp = SvgUtils.createSvgElement('g', {'clip-path': `url(#${clipId})`});
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
        return s;
    }

    protected getCalendarExceptions(task: TaskDto): CalendarException[] | undefined {
        return task.calendarId ? this.calendarExceptionsById.get(String(task.calendarId)) : undefined;
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
                    rLabel.appendChild(SvgUtils.createSvgElement('title', {},
                        this.generateTaskNameToolTip(resourceName, task.assignedUserAvailability, task.assignedUserCountry, task.assignedUserState)));
                    g.appendChild(rLabel);
                }
            }
        }
    }
}
