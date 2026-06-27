// gantt/abstract-gantt-renderer.ts
// Base class for Gantt renderers. Mirrors Java: AbstractGanttRenderer extends AbstractRenderer
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import { intToHex, convertSprintColorToRgba, hexToRgbaWithAlpha } from '../color-utils.js';
import { createSvgElement, createRect, createText, createLine, createClipPath } from '../svg-utils.js';
import { MS, calculateDayIndex }                                    from '../date-utils.js';
import { AbstractRenderer }                                         from '../abstract-renderer.js';
import { Theme }                                                    from '../theme/theme.js';
import { Milestones }                                               from '../milestones.js';
import {
    parseLocalDateTime, getDayAt8AM, getCalendarException,
    isWorkingDay, CalendarException,
} from './date-helpers.js';

// ── Constants (mirrors Java AbstractGanttRenderer field declarations) ────────
const FINE_LINE_STROKE_WIDTH      = 1.0;
export const LINE_HEIGHT          = 18;
const RELATION_CORNER_LENGTH      = 14;
const RESOURCE_NAME_TO_TASK_GAP   = 3;
export const SECONDS_PER_DAY      = 85 * 6 * 60; // 30600
const TASK_BODY_BORDER            = 1;
const TASK_NAME_TO_TASK_GAP       = 5 + 8;        // 13
const NONE_WORKING_DAY_FONT_SIZE  = 22;

export const DEFAULT_DW  = 20;
export const MIN_DW      = 2;
export const MAX_DW      = 80;
export const ZOOM_STEP   = 1.25;

// ── Task DTO ─────────────────────────────────────────────────────────────────
export interface TaskDto {
    id:                      number | string;
    key?:                    string;
    name?:                   string;
    start?:                  string;
    finish?:                 string;
    rowIndex:                number;
    fillColor?:              string;
    textColor?:              string;
    borderColor?:            string;
    progressColor?:          string;
    progress?:               number;
    milestone?:              boolean;
    story?:                  boolean;
    critical?:               boolean;
    manuallyScheduled?:      boolean;
    assignedUserName?:       string | null;
    assignedUserAvailability?:string | null;
    assignedUserCountry?:    string | null;
    assignedUserState?:      string | null;
    calendarExceptions?:     CalendarException[];
    predecessors?:           { predecessorId: number | string; visible?: boolean }[];
}

export abstract class AbstractGanttRenderer extends AbstractRenderer {
    dayWidth:      number;
    scrollOffset:  number;
    containerWidth:number;
    chartStart:    Date | null;
    totalDays:     number;
    currentDate:   Date | null;
    tasks:         TaskDto[];
    _calendarH:    number;
    _taskById:     Record<string, TaskDto>;

    constructor(theme: Theme, milestones: Milestones, preRun: number, postRun: number) {
        super(theme, milestones, preRun, postRun);
        this.dayWidth       = DEFAULT_DW;
        this.scrollOffset   = 0;
        this.containerWidth = 800;
        this.chartStart     = null;
        this.totalDays      = 0;
        this.currentDate    = null;
        this.tasks          = [];
        this._calendarH     = 0;
        this._taskById      = {};
    }

    dayIndexToPixelX(dayIndex: number): number {
        return (dayIndex - this.scrollOffset) * this.dayWidth;
    }

    calculateX(datetimeStr: string, startTimeStr: string | null, secondsPerDay: number): number {
        const date         = parseLocalDateTime(datetimeStr)!;
        const startTime    = parseLocalDateTime(startTimeStr)!;
        const dayIndex     = calculateDayIndex(datetimeStr, this.chartStart!);
        const workedSeconds = (date.getTime() - startTime.getTime()) / 1000;
        const timeOfDayX   = Math.floor(workedSeconds * this.dayWidth / secondsPerDay);
        return this.dayIndexToPixelX(dayIndex) + timeOfDayX;
    }

    getTaskHeight(): number { return LINE_HEIGHT; }

    override calculateChartHeight(): number {
        const calH = this.calendarXAxes
            ? this.calendarXAxes.getHeight(this.dayWidth, this.milestones.list.length > 0)
            : 0;
        return calH + this.tasks.length * (this.getTaskHeight() + 1);
    }

    getDayOfWeekStripBgColor(dayDate: Date): string {
        const dow = dayDate.getDay();
        if (dow === 6) return intToHex(this.theme.chartTheme.dayOfweekSaturdayBgColor, '#d7d7d7');
        if (dow === 0) return intToHex(this.theme.chartTheme.dayOfweekSundayBgColor,   '#d7d7d7');
        return intToHex(this.theme.xAxesTheme.dayOfweekBgColor, '#ffffff');
    }

    getGanttDayStripeColor(task: TaskDto, dayDate: Date): string {
        const dow = dayDate.getDay();
        if (dow === 6 || dow === 0 || isWorkingDay(dayDate, task.calendarExceptions)) {
            return this.getDayOfWeekStripBgColor(dayDate);
        }
        const exception = getCalendarException(dayDate, task.calendarExceptions);
        if (exception) {
            const t = exception.type;
            if (t === 'VACATION') return intToHex(this.theme.ganttTheme.vacationBgColor, '#a0c8ff');
            if (t === 'TRIP')     return intToHex(this.theme.ganttTheme.tripBgColor,     '#c8a0ff');
            if (t === 'SICK')     return intToHex(this.theme.ganttTheme.sickBgColor,     '#ffa0a0');
            return intToHex(this.theme.ganttTheme.holidayBgColor, '#ffd0a0');
        }
        return intToHex(this.theme.xAxesTheme.dayOfMonthWeekendBgColor, '#d7d7d7');
    }

    override drawDayBars(g: SVGElement, dayDate: Date, calendarH = 0): void {
        const dayIdx    = calculateDayIndex(dayDate, this.chartStart!);
        const dayLeft   = this.dayIndexToPixelX(dayIdx);
        const gridColor = intToHex(this.theme.ganttTheme.gridColor, '#e4e8f3');
        for (const task of this.tasks) {
            const rowY = calendarH + task.rowIndex * (this.getTaskHeight() + 1);
            g.appendChild(createRect(dayLeft, rowY - 1, this.dayWidth, 1, { fill: gridColor }));
            g.appendChild(createRect(dayLeft, rowY, 1, LINE_HEIGHT, { fill: gridColor }));
            const bgColor = this.getGanttDayStripeColor(task, dayDate);
            g.appendChild(createRect(dayLeft + 1, rowY, this.dayWidth - 1, LINE_HEIGHT, { fill: bgColor }));
            const exception = getCalendarException(dayDate, task.calendarExceptions);
            if (exception?.letter && this.dayWidth >= 14) {
                const cx = dayLeft + this.dayWidth / 2;
                const letter = createText(cx, rowY + LINE_HEIGHT / 2, exception.letter, {
                    fill: intToHex(this.theme.ganttTheme.outOfOfficeColor, '#ffffff'),
                    'font-size': String(NONE_WORKING_DAY_FONT_SIZE),
                    'font-family': 'sans-serif', 'font-weight': 'bold',
                    'text-anchor': 'middle', 'dominant-baseline': 'middle',
                });
                letter.appendChild(createSvgElement('title', {}, exception.type || 'Off-day'));
                g.appendChild(letter);
            }
        }
    }

    drawConflictMarker(_g: SVGElement, _y: number, _conflict: unknown): void { /* team planner only */ }

    drawCriticalMarker(g: SVGElement, task: TaskDto, x1: number, x2: number, y: number): void {
        const borderColor = task.critical
            ? intToHex(this.theme.ganttTheme.criticalTaskBorderColor, '#ff0000')
            : intToHex(this.theme.ganttTheme.taskBorderColor, '#888888');

        const startDayIdx  = calculateDayIndex(task.start!, this.chartStart!);
        const finishDayIdx = calculateDayIndex(task.finish!, this.chartStart!);
        const days         = finishDayIdx - startDayIdx;

        for (let day = 0; day <= days; day++) {
            const dayIdx   = startDayIdx + day;
            const dayDate  = new Date(this.chartStart!.getTime() + dayIdx * MS);
            const working  = isWorkingDay(dayDate, task.calendarExceptions);
            const xStart   = this.dayIndexToPixelX(dayIdx);
            const xFinish  = xStart + this.dayWidth;

            if (working) {
                const th = this.getTaskHeight();
                if (days === 0) {
                    g.appendChild(createRect(x1, y - th/2 + TASK_BODY_BORDER, x2-x1+1, 1, { fill: borderColor }));
                    g.appendChild(createRect(x1, y + th/2 - TASK_BODY_BORDER - 1, x2-x1+1, 1, { fill: borderColor }));
                    g.appendChild(createRect(x1, y - th/2 + TASK_BODY_BORDER + 1, 1, th-TASK_BODY_BORDER*2-2, { fill: borderColor }));
                    g.appendChild(createRect(x2, y - th/2 + TASK_BODY_BORDER + 1, 1, th-TASK_BODY_BORDER*2-2, { fill: borderColor }));
                } else if (day === 0) {
                    g.appendChild(createRect(x1, y-th/2+TASK_BODY_BORDER, xFinish-x1, 1, { fill: borderColor }));
                    g.appendChild(createRect(x1, y+th/2-TASK_BODY_BORDER-1, xFinish-x1, 1, { fill: borderColor }));
                    g.appendChild(createRect(x1, y-th/2+TASK_BODY_BORDER+1, 1, th-TASK_BODY_BORDER*2-2, { fill: borderColor }));
                } else if (day === days) {
                    g.appendChild(createRect(xStart, y-th/2+TASK_BODY_BORDER, x2-xStart+1, 1, { fill: borderColor }));
                    g.appendChild(createRect(xStart, y+th/2-TASK_BODY_BORDER-1, x2-xStart+1, 1, { fill: borderColor }));
                    g.appendChild(createRect(x2, y-th/2+TASK_BODY_BORDER+1, 1, th-TASK_BODY_BORDER*2-2, { fill: borderColor }));
                } else {
                    g.appendChild(createRect(xStart, y-th/2+TASK_BODY_BORDER, this.dayWidth, 1, { fill: borderColor }));
                    g.appendChild(createRect(xStart, y+th/2-TASK_BODY_BORDER-1, this.dayWidth, 1, { fill: borderColor }));
                }
            } else {
                for (let i = 0; i < this.dayWidth - 1; i++) {
                    if ((dayIdx * this.dayWidth + i) % 4 === 0) {
                        const px = xStart + i;
                        const th = this.getTaskHeight();
                        g.appendChild(createRect(px, y-th/2+TASK_BODY_BORDER, 2, 1, { fill: borderColor }));
                        g.appendChild(createRect(px, y+th/2-TASK_BODY_BORDER-1, 2, 1, { fill: borderColor }));
                    }
                }
            }
        }
    }

    drawId(g: SVGElement, task: TaskDto, y: number): void {
        const x1        = this.dayIndexToPixelX(0);
        const x2        = x1 + this.dayWidth;
        const fillColor = intToHex(this.theme.ganttTheme.idBgColor, '#cccccc');
        const textColor = intToHex(this.theme.ganttTheme.idTextColor, '#000000');
        g.appendChild(createRect(x1+1, y-this.getTaskHeight()/2, x2-x1-1, this.getTaskHeight(), { fill: fillColor }));
        const keyText = createText(x1+4, y, task.key || '', {
            fill: textColor, 'font-size': '12', 'font-family': 'sans-serif', 'dominant-baseline': 'middle',
        });
        if (task.name) keyText.appendChild(createSvgElement('title', {}, task.name));
        g.appendChild(keyText);
    }

    drawManualMarker(g: SVGElement, task: TaskDto, x1: number, y: number, _labelInside: boolean): void {
        if (task.manuallyScheduled) {
            g.appendChild(createRect(x1, y - this.getTaskHeight()/2, 1, this.getTaskHeight(), { fill: '#ff0000' }));
        }
    }

    drawMilestoneTask(g: SVGElement, task: TaskDto, x1: number, y: number, _labelInside: boolean, taskName: string): void {
        const mW          = this.getTaskHeight() / 2 - TASK_BODY_BORDER;
        const fillColor   = task.fillColor ? convertSprintColorToRgba(task.fillColor) : '#808080';
        const borderColor = task.borderColor || '#888888';
        const points      = [
            `${x1},${y - mW}`, `${x1 + mW},${y}`,
            `${x1},${y + mW}`, `${x1 - mW},${y}`,
            `${x1},${y - mW}`,
        ].join(' ');
        const poly = createSvgElement('polygon', { points, fill: fillColor, stroke: borderColor, 'stroke-width': '1' });
        poly.appendChild(createSvgElement('title', {}, this.generateTaskToolTip(task)));
        g.appendChild(poly);
        const textColor  = task.textColor || intToHex(this.theme.ganttTheme.taskTextColor, '#303030');
        const labelX     = x1 + mW / 2 + 10;
        const dateStr    = task.start ? new Date(task.start).toLocaleDateString() : '';
        const label      = `${taskName || ''} (${dateStr})`;
        const lbl        = createText(labelX, y, label, {
            fill: textColor, 'font-size': '12', 'font-family': 'sans-serif', 'dominant-baseline': 'middle',
        });
        lbl.appendChild(createSvgElement('title', {}, this.generateTaskToolTip(task)));
        g.appendChild(lbl);
    }

    drawRelation(g: SVGElement, sourceTask: TaskDto, y2: number, targetTask: TaskDto, y1: number): void {
        const signum = (n: number) => (n > 0 ? 1 : n < 0 ? -1 : 0);
        const sign   = signum(y2 - y1);
        let yEnd: number, yMid: number;
        const th = this.getTaskHeight();
        if (sign > 0) { y2 -= th/2 - TASK_BODY_BORDER; yEnd = y2; yMid = y2 - 5; }
        else          { y2 += th/2 - TASK_BODY_BORDER; yEnd = y2; yMid = y2 + 5; }
        const ax1 = this.calculateX(targetTask.finish!, getDayAt8AM(targetTask.finish), SECONDS_PER_DAY);
        const ax2 = RELATION_CORNER_LENGTH
            + this.calculateX(sourceTask.start!, getDayAt8AM(sourceTask.start), SECONDS_PER_DAY)
            - RESOURCE_NAME_TO_TASK_GAP;
        const arrowColor = (sourceTask.critical && targetTask.critical)
            ? intToHex(this.theme.ganttTheme.criticalRelationColor, '#ff0000')
            : intToHex(this.theme.ganttTheme.relationColor, '#3466ed');
        g.appendChild(createRect(ax1+1, y1, ax2-ax1, 1, { fill: arrowColor }));
        g.appendChild(createRect(ax2, y1+1, 1, yMid-y1, { fill: arrowColor }));
        const d   = 5;
        const pts = y2 > y1
            ? `${ax2-d},${yEnd-d+sign} ${ax2+d},${yEnd-d+sign} ${ax2},${yEnd+sign}`
            : `${ax2+d},${yEnd+d+sign} ${ax2-d},${yEnd+d+sign} ${ax2},${yEnd+sign}`;
        g.appendChild(createSvgElement('polygon', { points: pts, fill: arrowColor }));
    }

    drawRibbon(g: SVGElement, y1: number, x1: number, y2: number, delta1: number, delta2: number, ribbonColor: string): void {
        const points = `${x1},${y2} ${x1+delta1},${y1} ${x1+delta1+delta2},${y1} ${x1+delta2},${y2}`;
        g.appendChild(createSvgElement('polygon', { points, fill: ribbonColor }));
    }

    drawStoryBody(g: SVGElement, task: TaskDto, x1: number, x2: number, y: number, marker: string | null): void {
        const fillColor = task.fillColor
            ? convertSprintColorToRgba(task.fillColor)
            : intToHex(this.theme.ganttTheme.storyColor, '#444444');
        const tooltip   = this.generateTaskToolTip(task);
        const th        = this.getTaskHeight();

        if (marker == null) {
            const y1        = y + TASK_BODY_BORDER;
            const thickness = 2;
            g.appendChild(createRect(x1, y1-th/2, x2-x1+1, thickness, { fill: fillColor }));
            g.appendChild(createRect(x1, y1-th/2+thickness, thickness, th-TASK_BODY_BORDER*2-thickness, { fill: fillColor }));
            g.appendChild(createRect(x2-1, y1-th/2+thickness, thickness, th-TASK_BODY_BORDER*2-thickness, { fill: fillColor }));
            if (x2-x1-1 > 0) {
                const tooltipRect = createRect(x1+1, y1-th/2, x2-x1-1, th-thickness*2, { fill: 'none', 'pointer-events': 'all' });
                tooltipRect.appendChild(createSvgElement('title', {}, tooltip));
                g.appendChild(tooltipRect);
            }
        } else {
            const stY1    = y - th/2 + 1;
            const stY2    = stY1 + th - 1;
            const clipId  = 'sr-' + String(task.id).replace(/-/g, '');
            g.appendChild(createClipPath(clipId, x1+1, y-th/2+2, x2-x1-1, th-4));
            const grp     = createSvgElement('g', { 'clip-path': `url(#${clipId})` });
            let cur       = fillColor;
            for (let rx = x1 - 16; rx < x2; rx += 16) {
                this.drawRibbon(grp, stY1, rx, stY2, 25, 15, cur);
                cur = (cur === fillColor) ? '#ffffff' : fillColor;
            }
            g.appendChild(grp);
        }
    }

    drawTask(
        g:               SVGElement,
        _gantUniqueId:   number,
        task:            TaskDto,
        doDrawId:        boolean,
        drawRelations:   boolean,
        labelInside:     boolean,
        alien:           boolean,
        marker:          string | null,
        conflict:        unknown,
        _drawOutOfOffice: boolean,
    ): void {
        if (!task.start || !task.finish) return;
        const x1 = this.calculateX(task.start,  getDayAt8AM(task.start)!,  SECONDS_PER_DAY);
        const x2 = this.calculateX(task.finish, getDayAt8AM(task.finish)!, SECONDS_PER_DAY);
        const y  = this._calendarH + task.rowIndex * (this.getTaskHeight() + 1) + this.getTaskHeight() / 2;

        this._drawTask(g, task, x1, x2, y, labelInside, alien, marker, conflict);
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

    _drawTask(
        g:           SVGElement,
        task:        TaskDto,
        x1:          number, x2: number, y: number,
        _labelInside: boolean,
        alien:       boolean,
        marker:      string | null,
        conflict:    unknown,
    ): void {
        const textColor = task.textColor || intToHex(this.theme.ganttTheme.taskTextColor, '#303030');
        const taskName  = task.name || '';
        const th        = this.getTaskHeight();

        if (task.milestone && !task.story) {
            this.drawMilestoneTask(g, task, x1, y, false, taskName);
        } else if (task.story) {
            this.drawStoryBody(g, task, x1, x2, y, marker);
            const storyLabelX = x2 + 10;
            if (storyLabelX < this.containerWidth + 40) {
                const storyLabel = createText(storyLabelX, y, taskName, {
                    fill: textColor, 'font-size': '12', 'font-family': 'sans-serif',
                    'font-weight': 'bold', 'dominant-baseline': 'middle',
                });
                storyLabel.appendChild(createSvgElement('title', {}, this.generateTaskToolTip(task)));
                g.appendChild(storyLabel);
            }
        } else {
            const progress = task.progress || 0;
            this.drawTaskBody(g, task, x1, x2, y, alien, progress);
            this.drawConflictMarker(g, y, conflict);
            this.drawCriticalMarker(g, task, x1, x2, y);
            this.drawManualMarker(g, task, x1, y, false);

            if (progress > 0) {
                const barWidth  = x2 - x1;
                const text      = `${Math.round(progress * 100)}%`;
                const textWidth = text.length * 5;
                if (textWidth < barWidth) {
                    const clipId2 = 'pt-' + String(task.id).replace(/-/g, '');
                    g.appendChild(createClipPath(clipId2, x1+1, y-th/2+RESOURCE_NAME_TO_TASK_GAP, x2-x1-3, th-6));
                    g.appendChild(createText(x1 + barWidth/2, y, text, {
                        fill: '#ffffff', stroke: '#000000', 'stroke-width': '0.4',
                        'font-size': '8', 'font-family': 'sans-serif',
                        'text-anchor': 'middle', 'dominant-baseline': 'middle',
                        'clip-path': `url(#${clipId2})`,
                    }));
                }
            }

            const labelRight = x2 + TASK_NAME_TO_TASK_GAP;
            if (labelRight < this.containerWidth + 40) {
                const clipId3 = 'tn-' + String(task.id).replace(/-/g, '');
                const clipW3  = Math.max(0, this.containerWidth - labelRight);
                if (clipW3 > 8) {
                    g.appendChild(createClipPath(clipId3, labelRight, y-th, clipW3, th*2));
                    const nameLabel = createText(labelRight, y, (task.key ? task.key + ' ' : '') + taskName, {
                        fill: textColor, 'font-size': '12', 'font-family': 'sans-serif',
                        'dominant-baseline': 'middle', 'clip-path': `url(#${clipId3})`,
                    });
                    nameLabel.appendChild(createSvgElement('title', {}, this.generateTaskToolTip(task)));
                    g.appendChild(nameLabel);
                }
            }

            if (task.assignedUserName) {
                const rn      = task.assignedUserName;
                const rnWidth = rn.length * 7;
                const rnX     = x1 - rnWidth - RESOURCE_NAME_TO_TASK_GAP;
                if (rnX > -100) {
                    const clipId4 = 'rn-' + String(task.id).replace(/-/g, '');
                    const clipW4  = Math.min(120, x1 > 0 ? x1 : 0);
                    if (clipW4 > 8) {
                        g.appendChild(createClipPath(clipId4, Math.max(0, rnX), y-th, clipW4, th*2));
                        const rLabel = createText(rnX + rnWidth, y, rn, {
                            fill: textColor, 'font-size': '12', 'font-family': 'sans-serif',
                            'text-anchor': 'end', 'dominant-baseline': 'middle',
                            'clip-path': `url(#${clipId4})`,
                        });
                        rLabel.appendChild(createSvgElement('title', {},
                            this.generateTaskNameToolTip(rn, task.assignedUserAvailability, task.assignedUserCountry, task.assignedUserState)));
                        g.appendChild(rLabel);
                    }
                }
            }
        }
    }

    drawTaskBody(g: SVGElement, task: TaskDto, x1: number, x2: number, y: number, alien: boolean, progress: number): void {
        const fillColor    = task.fillColor;
        const tooltip      = this.generateTaskToolTip(task);
        const th           = this.getTaskHeight();

        if (!alien) {
            const y1 = y - th/2 + TASK_BODY_BORDER;
            const h  = th - TASK_BODY_BORDER * 2;
            if (x2 - x1 - 2 > 0) {
                const startDayIdx  = calculateDayIndex(task.start!, this.chartStart!);
                const finishDayIdx = calculateDayIndex(task.finish!, this.chartStart!);
                const days         = finishDayIdx - startDayIdx;

                for (let day = 0; day <= days; day++) {
                    const dayIdx   = startDayIdx + day;
                    const dayDate  = new Date(this.chartStart!.getTime() + dayIdx * MS);
                    let segX: number, segW: number;

                    if (isWorkingDay(dayDate, task.calendarExceptions)) {
                        const fill = convertSprintColorToRgba(fillColor);
                        if (days === 0) { segX = x1; segW = x2 - x1; }
                        else if (day === 0) { segX = x1; segW = this.dayIndexToPixelX(dayIdx) + this.dayWidth - x1; }
                        else if (day === days) { segX = this.dayIndexToPixelX(dayIdx); segW = x2 - segX + 1; }
                        else { segX = this.dayIndexToPixelX(dayIdx); segW = this.dayWidth; }
                        const rect = createRect(segX, y1, segW, h, { fill });
                        rect.appendChild(createSvgElement('title', {}, tooltip));
                        g.appendChild(rect);
                    } else {
                        const weekendFill = hexToRgbaWithAlpha(fillColor, this.theme.ganttTheme.taskWeekEndTransparency);
                        const xStart4     = this.dayIndexToPixelX(dayIdx);
                        const rectW       = createRect(xStart4, y1, this.dayWidth, h, { fill: weekendFill });
                        rectW.appendChild(createSvgElement('title', {}, tooltip));
                        g.appendChild(rectW);
                    }
                }

                if (progress > 0) {
                    const progressFill = task.progressColor
                        ? convertSprintColorToRgba(task.progressColor)
                        : hexToRgbaWithAlpha(fillColor, 200);
                    const progressW = Math.floor((x2 - x1) * progress - 1);
                    if (progressW > 0) {
                        const pRect = createRect(x1+1, y1+2, progressW, h-4, { fill: progressFill });
                        pRect.appendChild(createSvgElement('title', {}, tooltip));
                        g.appendChild(pRect);
                        if (progress < 1.0) {
                            g.appendChild(createRect(x1 + progressW, y - th/2 + 2, 1, th - 4, { fill: '#000000' }));
                        }
                    }
                }
            }
        } else {
            const aY1    = y - th/2 + 1;
            const aY2    = aY1 + th - 1;
            const clipId = 'ta-' + String(task.id).replace(/-/g, '');
            g.appendChild(createClipPath(clipId, x1, y-th/2+2, x2-x1-1, th-4));
            const grp   = createSvgElement('g', { 'clip-path': `url(#${clipId})` });
            let cur     = fillColor ? convertSprintColorToRgba(fillColor) : '#aaaaaa';
            for (let ax = x1 - 16; ax < x2; ax += 16) {
                this.drawRibbon(grp, aY1, ax, aY2, 25, 15, cur);
                cur = (cur === (fillColor ? convertSprintColorToRgba(fillColor) : '#aaaaaa')) ? '#ffffff' : (fillColor ? convertSprintColorToRgba(fillColor) : '#aaaaaa');
            }
            g.appendChild(grp);
        }
    }

    drawTick(_g: SVGElement, _time: string, _x: number, _y: number, _alignment: string): void { /* commented out in Java */ }

    generateTaskNameToolTip(resourceName: string | null | undefined, resourceUtilization: string | null | undefined, country: string | null | undefined, state: string | null | undefined): string {
        let tip = resourceName || '';
        if (resourceUtilization) tip += `\nAvailability ${resourceUtilization}`;
        if (country)             tip += `\nCountry ${country}`;
        if (state)               tip += `\nState ${state}`;
        return tip;
    }

    generateTaskToolTip(task: TaskDto): string {
        let s = task.name || '';
        if (task.key)                      s += `\nKey: ${task.key}`;
        if (task.start)                    s += `\nStart: ${new Date(task.start).toLocaleDateString()}`;
        if (task.finish)                   s += `\nFinish: ${new Date(task.finish).toLocaleDateString()}`;
        if (task.assignedUserName)         s += `\nResource: ${task.assignedUserName}`;
        if (task.assignedUserAvailability) s += `\nAvailability: ${task.assignedUserAvailability}`;
        if (task.progress && task.progress > 0) s += `\nProgress: ${Math.round(task.progress * 100)}%`;
        return s;
    }
}

