/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

import {AbstractRenderer} from '../AbstractRenderer.js';
import {ColorUtils} from '../ColorUtils.js';
import {DateUtils} from '../DateUtils.js';
import {GraphSquare} from '../GraphSquare.js';
import {SvgUtils} from '../SvgUtils.js';
import {CalendarSize} from "../CalendarSize.js";
import {GanttBurndownChartDto} from './dto/GanttBurndownChartDto.js';
import {BurndownMetaDto} from './dto/BurndownMetaDto.js';
import {AbstractChart} from '../AbstractChart.js';

// ── Constants (mirrors Java LegacyBurnDownRenderer static fields) ─────────────────
const ONE_WEEK = 7;
const ONE_WORK_MONTH = 20 * 75 * 60 * 6;
const ONE_WORK_WEEK = 5 * 75 * 60 * 6;
const SECONDS_PER_WORKING_DAY = 75 * 6 * 60;
/** Mirrors Java: public static final int Y_AXIS_WIDTH = 50; */
export const Y_AXIS_WIDTH = 50;
const STANDARD_LINE_STROKE_WIDTH = 3;
const REPLAY_SLIDER_HEIGHT = 18;

/**
 * Fixed plot (diagram) height in pixels. Mirrors Java's {@code dao.chartHeight}, which is
 * computed externally by the caller (e.g. LegacyRenderDao) for the SVG/PNG export path. For the
 * interactive browser chart we pick a constant plot height instead.
 */
const PLOT_HEIGHT = 240;

let burndownClipSeq = 0;

export interface ActualInspectionPoint {
    date: Date;
    seconds: number;
    authorName: string;
    authorSeconds: number;
    x: number;
    y: number;
}

/**
 * Standard agile burn down chart, based on logged work in jira tickets of a
 * specific sprint.
 *
 * Mirrors Java: {@code LegacyBurnDownRenderer extends LegacyAbstractRenderer}.
 */
export class BurndownRenderer extends AbstractRenderer {
    readonly data: GanttBurndownChartDto;
    actualInspectionPoints: readonly ActualInspectionPoint[] = [];
    inspectionLayer: SVGGElement | null = null;
    private accumulatedWorkPerDay: number[][] = [];
    private replayRemainingWorkPerDay: number[][] = [];
    private replayDayIndex = 0;
    private replayInitialized = false;
    private replayEndDate: Date | null = null;
    private replayTotalEstimate = 0;
    private readonly originalReleaseDate: Date | null;

    // ── fields mirroring LegacyBurnDownRenderer.java ──────────────────────────────
    /**
     * Anchor date for {@code this.data}'s pre-computed per-day arrays (server-computed as
     * sprintStart - preRun in GanttBurndownChartService). Java has no equivalent field and always
     * indexes relative to {@code milestones.firstMilestone} directly; see the note in
     * {@link #createMilestones} and {@link #toArrayIndex} for why/how this is reconciled.
     */
    chartStart: Date;
    // totalDays: number;
    /** LocalDateTime now (the "N" milestone date, or null when hidden). */
    currentDate: Date | null;
    /** Day pixel width, externally driven by zoom (see gantt-burndown-bundle.ts). */
    dayWidth: number;
    /** max work per day, were every day has the amount of work planned at that day and all days before that */
    ganttWorkWithBufferPerDayAccumulated: number[] | null = null;
    /** min work per day, were every day has the amount of work planned at that day and all days before that */
    ganttWorkWithoutBufferPerDayAccumulated: number[] | null = null;
    private readonly eWorstWork: number | null;
    private extrapolationColor!: number | null;
    private maxWorked = 0;
    private readonly sprintClosed: boolean;
    private readonly calendarSize: CalendarSize;

    constructor(chart: AbstractChart, data: GanttBurndownChartDto) {
        const meta = data.meta;
        // Java: super(dao) — LegacyAbstractRenderer(LegacyRenderDao) sets theme/chartWidth/chartHeight/milestones/
        // legacyCalendarXAxes. milestones starts empty here; createMilestones() (called below from
        // processingInit) populates it, mirroring LegacyBurnDownRenderer(LegacyRenderDao) -> processingInit(dao)
        // -> createMilestones(...).
        super(chart/*, new Milestones([])*/, meta.preRun || 0, meta.postRun || 0, meta.calendarAxesZoom);

        this.data = data;
        this.originalReleaseDate = meta.releaseDate ? DateUtils.getDayMidnight(meta.releaseDate) : null;
        // Java: init(dao) -> initSize(dao.firstDayX, true, dao.calendarSize) — LegacyBurnDownRenderer
        // always draws its own calendar at the BOTTOM of its plot area, so that it sits directly
        // above the Gantt chart's own (top) calendar when the two are stacked in LegacyGanttBurndownChart.
        this.calendarAtBottom = true;

        this.chartStart = DateUtils.getDayMidnight(new Date(meta.chartStart));
        // this.totalDays = this.calculateMaxDays();// meta.totalDays || DateUtils.calculateDayCount(this.chartStart, DateUtils.getDayMidnight(new Date(meta.chartEnd)));
        this.days = this.calculateMaxDays();
        this.currentDate = meta.now ? DateUtils.getDayMidnight(new Date(meta.now)) : null;
        this.dayWidth = 20;

        this.sprintClosed = meta.sprintClosed;
        this.ganttWorkWithoutBufferPerDayAccumulated = data.ganttGuideWithoutBuffer || null;
        this.ganttWorkWithBufferPerDayAccumulated = data.ganttGuideWithBuffer || null;
        this.eWorstWork = null; // dao.estimatedWorstWork has no DTO equivalent yet
        this.calendarSize = data.meta.calendarSize;

        // Java: processingInit(dao) -> createMilestones(...) -> init(dao)
        this.processingInit(meta);
    }

    // ── LegacyAbstractRenderer overrides ───────────────────────────────────────────

    /**
     * Mirrors Java: calculateChartWidth().
     * The interactive chart's width is driven by the scrollable container instead of a fixed
     * days*dayWidth computation.
     */
    override calculateChartWidth(): number {
        return this.containerWidth;
    }

    /**
     * Mirrors Java: calculateDayWidth().
     * Day width is externally controlled by zoom (see gantt-burndown-bundle.ts) instead of being
     * derived from a fixed chartWidth.
     */
    override calculateDayWidth(): void {
        this.days = this.calculateMaxDays();
        this.calendarXAxes.dayOfWeek.setWidth(this.dayWidth);
        this.calendarXAxes.dayOfMonth.setWidth(this.dayWidth);
    }

    // override calculateChartHeight(): number {
    //     const calendarH = this.legacyCalendarXAxes.getHeight(this.dayWidth, this.milestones.list.length > 0);
    //     return calendarH + PLOT_HEIGHT;
    // }

    // ── Java: public void draw(ExtendedGraphics2D graphics2D, int x, int y) ──
    override draw(svg: SVGElement, x: number, y: number): void {
        this.actualInspectionPoints = [];
        this.inspectionLayer = null;
        // Java: init(dao) -> initSize(...) is called once during construction; here we redo the
        // size computation on every draw() because containerWidth/dayWidth can change between
        // renders (interactive zoom/pan/resize), unlike the static Java export.
        this.calculateDayWidth();
        this.chartWidth = this.calculateChartWidth();
        this.chartHeight = this.calculateChartHeight();
        // this.diagram.initSize(this.containerWidth, PLOT_HEIGHT);

        this.initPosition(this.firstDayX + x, y);
        this.drawCalendar(svg, true, this.containerWidth);
        this.drawMilestones(svg);
        this.createBurnDownChart(svg);
        this.drawReplaySlider(svg);
    }

    // ── Java: protected void createMilestones(LocalDateTime start, LocalDateTime now, LocalDateTime end,

    // ── Java: public void init(LegacyRenderDao dao) ──
    public init(): void {
        this.calculateDayWidth();
        this.calendarXAxes.additionalBottomHeight = this.calendarXAxes.isDayOfWeekVisible() ? REPLAY_SLIDER_HEIGHT : 0;
        // Java: initSize(dao.firstDayX, true, dao.calendarSize) — sizing happens per-draw() in this
        // interactive port instead (see draw()), since containerWidth/dayWidth can change on
        // zoom/pan/resize, unlike the static Java export.
        this.initSize(this.data.meta.firstDayX, true, this.calendarSize, this.containerWidth);
        const replayEndDayIndex = this.getReplayEndDayIndex();
        if (!this.replayInitialized) {
            const initialDate = this.currentDate ?? this.milestones.lastMilestone!;
            this.replayDayIndex = Math.min(
                replayEndDayIndex,
                Math.max(0, DateUtils.calculateDays(this.milestones.firstMilestone!, initialDate)),
            );
            this.replayInitialized = true;
        } else {
            this.replayDayIndex = Math.min(this.replayDayIndex, replayEndDayIndex);
        }
        this.calculateAccumulatedWorkPerDay();
        this.updateReplayForecast();
        // GanttBurndownChartService provides daily work and remaining-estimate snapshots; this renderer
        // accumulates work and uses estimate snapshots through the selected replay day.
    }

    // ── Java: private void processingInit(LegacyRenderDao dao) ──
    private processingInit(meta: BurndownMetaDto): void {
        this.createMilestonesFromMeta(meta);
        this.init();
    }

    private createMilestonesFromMeta(meta: BurndownMetaDto) {
        const start = DateUtils.getDayMidnight(new Date(meta.sprintStart));
        const end = DateUtils.getDayMidnight(new Date(meta.sprintEnd));
        let now: Date | null = null;
        if (!this.isHideNow(now, end, meta.sprintClosed)) {
            now = meta.now ? DateUtils.getDayMidnight(new Date(meta.now)) : null;
        }
        const firstWorklog = meta.firstWorklogDate ? DateUtils.getDayMidnight(new Date(meta.firstWorklogDate)) : null;
        const lastWorklog = meta.lastWorklogDate ? DateUtils.getDayMidnight(new Date(meta.lastWorklogDate)) : null;
        const release = meta.releaseDate ? DateUtils.getDayMidnight(new Date(meta.releaseDate)) : null;
        this.createMilestones(start, now, end, firstWorklog, lastWorklog, release);
        this.replayEndDate = new Date(this.milestones.lastMilestone);
    }

    /**
     * Converts a Java-semantics dayIndex (relative to milestones.firstMilestone, as used throughout
     * LegacyBurnDownRenderer.java) into an index into this.data's server-computed per-day arrays, which are
     * pre-computed relative to chartStart (= firstMilestone - priRun, guaranteed by createMilestones()
     * above). This is the one explicit adaptation needed because the DTO arrays are built server-side
     * before the client's Milestones are known.
     */
    private toArrayIndex(dayIndex: number): number {
        return dayIndex + this.calendarXAxes.priRun;
    }

    // ── Java: protected int calculateAuthorGraphHeight(Duration authorDelta, Duration authorEstimated, Duration sumEstimated) ──
    private calculateAuthorGraphHeight(authorDeltaSeconds: number, authorEstimatedSeconds: number, sumEstimatedSeconds: number): number {
        if (authorEstimatedSeconds <= 0 || sumEstimatedSeconds <= 0)
            return 0;
        const maxAuthorGraphHeight = (this.diagram.height * authorEstimatedSeconds) / sumEstimatedSeconds;
        return ((authorEstimatedSeconds - authorDeltaSeconds) * maxAuthorGraphHeight) / authorEstimatedSeconds;
    }

    // ── Java: protected int calculateGraphHeight(Duration height) ──
    private calculateGraphHeight(seconds: number): number {
        return (seconds * this.diagram.height) / this.maxWorked;
    }

    // ── Java: private LocalDate calendarFromDayIndex(int dayIndex) ──
    private calendarFromDayIndex(dayIndex: number): Date {
        return DateUtils.addDay(this.milestones.firstMilestone!, dayIndex);
    }

    /**
     * Selects the replay day and recalculates the accumulated work and release forecast.
     *
     * @param dayIndex Zero-based day index relative to the first chart day
     * @return Whether the selected day changed
     */
    public setReplayDayIndex(dayIndex: number): boolean {
        const replayDayIndex = Math.max(0, Math.min(this.getReplayEndDayIndex(), Math.round(dayIndex)));
        if (replayDayIndex === this.replayDayIndex)
            return false;
        this.replayDayIndex = replayDayIndex;
        this.calculateAccumulatedWorkPerDay();
        this.updateReplayForecast();
        return true;
    }

    private getReplayEndDayIndex(): number {
        const lastDay = this.replayEndDate ?? this.milestones.lastMilestone!;
        const latestAvailableDay = this.currentDate && this.currentDate.getTime() < lastDay.getTime()
            ? this.currentDate
            : lastDay;
        return Math.max(0, DateUtils.calculateDays(this.milestones.firstMilestone!, latestAvailableDay));
    }

    private updateReplayForecast(): void {
        const releaseDate = this.calculateReplayReleaseDate();
        let releaseMilestone = this.milestones.get('R');
        if (releaseDate) {
            if (!releaseMilestone) {
                this.milestones.add(releaseDate, 'R', 'Release (Estimated release date)');
                releaseMilestone = this.milestones.get('R');
            }
            if (releaseMilestone) {
                releaseMilestone.time = releaseDate;
                releaseMilestone.hidden = false;
            }
        } else if (releaseMilestone) {
            releaseMilestone.time = this.originalReleaseDate ?? this.replayEndDate ?? releaseMilestone.time;
            releaseMilestone.hidden = true;
        }
        this.milestones.calculate();
        this.days = this.calculateMaxDays();

        const endMilestone = this.milestones.get('E');
        this.extrapolationColor = releaseMilestone && !releaseMilestone.hidden && endMilestone
            && releaseMilestone.time.getTime() > endMilestone.time.getTime()
            ? this.theme.burndownTheme.delayEventColor
            : this.theme.burndownTheme.inTimeColor;
    }

    private calculateReplayReleaseDate(): Date | null {
        const startDate = this.milestones.get('S')?.time;
        if (!startDate)
            return null;

        const replayDate = DateUtils.addDay(this.milestones.firstMilestone!, this.replayDayIndex);
        const workingDaysSpent = DateUtils.calculateWorkingDaysIncluding(startDate, replayDate);
        const replayArrayIndex = this.replayDayIndex + 1;
        let workedSeconds = 0;
        for (const authorWork of this.accumulatedWorkPerDay) {
            const authorWorkedSeconds = authorWork[replayArrayIndex];
            if (!Number.isFinite(authorWorkedSeconds))
                return null;
            workedSeconds += authorWorkedSeconds;
        }
        if (workedSeconds <= 0)
            return null;

        if (!Number.isFinite(this.replayTotalEstimate) || this.replayTotalEstimate <= 0)
            return null;
        const expectedWorkingDays = Math.ceil(this.replayTotalEstimate * workingDaysSpent / workedSeconds);
        if (expectedWorkingDays > 300)
            return null;

        let releaseDate = DateUtils.getDayMidnight(startDate);
        if (expectedWorkingDays > 2) {
            let workingDaysIndex = 1;
            while (workingDaysIndex < expectedWorkingDays) {
                if (DateUtils.isWorkDay(releaseDate))
                    workingDaysIndex++;
                releaseDate = DateUtils.addDay(releaseDate, 1);
            }
            while (!DateUtils.isWorkDay(releaseDate))
                releaseDate = DateUtils.addDay(releaseDate, 1);
        }
        return releaseDate;
    }

    private calculateAccumulatedWorkPerDay(): void {
        const lastDayIndex = this.replayDayIndex + 1;
        this.accumulatedWorkPerDay = this.data.authors.map(author => {
            const accumulated: number[] = new Array(author.workPerDay.length);
            accumulated[0] = 0;
            let last = 0;
            let hasCompleteHistory = true;
            for (let i = 1; i < author.workPerDay.length; i++) {
                if (i <= lastDayIndex) {
                    const dailyWork = author.workPerDay[i];
                    if (hasCompleteHistory && Number.isFinite(dailyWork)) {
                        last += dailyWork;
                        accumulated[i] = last;
                    } else {
                        accumulated[i] = Number.NaN;
                        hasCompleteHistory = false;
                    }
                } else {
                    // Fill in empty parts of the days list with the last known total.
                    accumulated[i] = last;
                }
            }
            return accumulated;
        });
        this.replayRemainingWorkPerDay = this.data.authors.map(author => {
            const remaining: number[] = new Array(author.workPerDay.length);
            let last = Number.NaN;
            let hasCompleteHistory = true;
            for (let i = 0; i < author.workPerDay.length; i++) {
                if (i <= lastDayIndex) {
                    const estimate = author.remainingWorkPerDay[i];
                    if (hasCompleteHistory && Number.isFinite(estimate)) {
                        last = estimate;
                        remaining[i] = estimate;
                    } else {
                        remaining[i] = Number.NaN;
                        hasCompleteHistory = false;
                    }
                } else {
                    remaining[i] = last;
                }
            }
            return remaining;
        });

        let replayTotalEstimate = 0;
        let hasCompleteReplayEstimate = true;
        let lastCompleteReplayEstimate = 0;
        for (let dayIndex = 0; dayIndex <= lastDayIndex; dayIndex++) {
            let dayEstimate = 0;
            let hasCompleteDayEstimate = true;
            for (let authorIndex = 0; authorIndex < this.data.authors.length; authorIndex++) {
                const worked = this.accumulatedWorkPerDay[authorIndex][dayIndex];
                const remaining = this.replayRemainingWorkPerDay[authorIndex][dayIndex];
                if (!Number.isFinite(worked) || !Number.isFinite(remaining)) {
                    hasCompleteDayEstimate = false;
                    break;
                }
                dayEstimate += worked + remaining;
            }
            if (hasCompleteDayEstimate) {
                lastCompleteReplayEstimate = dayEstimate;
                if (dayIndex === lastDayIndex)
                    replayTotalEstimate = dayEstimate;
            } else if (dayIndex === lastDayIndex) {
                hasCompleteReplayEstimate = false;
            }
        }
        this.replayTotalEstimate = hasCompleteReplayEstimate ? replayTotalEstimate : Number.NaN;
        this.maxWorked = hasCompleteReplayEstimate ? replayTotalEstimate : lastCompleteReplayEstimate;
    }

    private drawReplaySlider(svg: SVGElement): void {
        if (!this.calendarXAxes.isDayOfWeekVisible())
            return;

        const startDay = this.milestones.firstMilestone!;
        const maxDayIndex = this.getReplayEndDayIndex();
        const dayWidth = this.calendarXAxes.dayOfWeek.getWidth();
        const firstDayX = this.diagram.x + dayWidth / 2;
        const startX = firstDayX + (this.calendarXAxes.priRun - this.scrollOffset) * dayWidth;
        const endX = startX + maxDayIndex * dayWidth;
        const selectedX = startX + this.replayDayIndex * dayWidth;
        const y = this.calendarXAxes.year.getY() + this.calendarXAxes.year.getHeight() + REPLAY_SLIDER_HEIGHT / 2;
        const slider = SvgUtils.createGroup({
            class: 'burndown-replay-slider',
            'data-replay-slider': 'true',
            'data-min-x': String(startX),
            'data-max-x': String(endX),
            'data-min-day-index': '0',
            'data-max-day-index': String(maxDayIndex),
            role: 'slider',
            tabindex: '0',
            focusable: 'true',
            'aria-label': 'Replay burndown chart',
            'aria-valuemin': '0',
            'aria-valuemax': String(maxDayIndex),
            'aria-valuenow': String(this.replayDayIndex),
            'aria-valuetext': DateUtils.addDay(startDay, this.replayDayIndex).toLocaleDateString(),
            style: 'cursor: ew-resize; touch-action: none;',
        });
        svg.appendChild(slider);

        const trackColor = ColorUtils.intToHex(this.theme.ganttTheme.gridColor);
        const progressColor = ColorUtils.intToHex(this.theme.burndownTheme.borderColor);
        slider.appendChild(SvgUtils.createLine(startX, y, endX, y, {
            stroke: trackColor, 'stroke-width': '4', 'stroke-linecap': 'round',
            'pointer-events': 'stroke',
        }));
        slider.appendChild(SvgUtils.createLine(startX, y, selectedX, y, {
            stroke: progressColor, 'stroke-width': '4', 'stroke-linecap': 'round',
            'pointer-events': 'none',
        }));
        slider.appendChild(SvgUtils.createCircle(selectedX, y, 6, {
            fill: progressColor, stroke: trackColor, 'stroke-width': '2',
            'pointer-events': 'all',
        }));
    }

    // ── Java: protected boolean isPlannedBurnDownGuideAvailable() ──
    private isPlannedBurnDownGuideAvailable(): boolean {
        return this.ganttWorkWithoutBufferPerDayAccumulated != null;
    }

    // ── Java: private boolean isAbandonedProject() ──
    private isAbandonedProject(): boolean {
        return this.sprintClosed;
    }

    // ── Java: private Color generateBurnDownColor(Color color) ──
    // Kept for parity with Java; author.color is precomputed server-side to match the Gantt task fill color.

    // ── Java: protected boolean isHideNow(LocalDateTime now, LocalDateTime end, boolean completed) ──
    private isHideNow(now: Date | null, end: Date | null, completed: boolean): boolean {
        if (completed || this.isAbandonedProject()) {
            // We do not want to keep drawing the graph further and further to include the
            // current date, if it is closed.
            return end != null && now != null && now.getTime() > DateUtils.addDay(end, ONE_WEEK).getTime();
        }
        return false;
    }

    // ── Java: protected void createBurnDownChart() ──
    private createBurnDownChart(svg: SVGElement): void {
        const firstDay = this.milestones.firstMilestone!;
        const firstDayX = this.diagram.x + this.calendarXAxes.dayOfWeek.getWidth() / 2;
        const startMilestone = this.milestones.get('S')!;
        const startX = firstDayX + (DateUtils.calculateDays(firstDay, startMilestone.time) - this.scrollOffset + this.calendarXAxes.priRun) * this.dayWidth;
        const chartEndX = this.getChartEndX();
        const legendX = chartEndX - 130;

        if (this.maxWorked && this.maxWorked !== 0) {
            // y axis markings
            this.drawYAxes(svg, startX, this.maxWorked);

            if (this.data.authors.length !== 0) {
                this.drawBurnDown(svg, firstDay, firstDayX);
            }

            if (this.sprintClosed) {
                this.drawWatermark(svg, startX - this.dayWidth / 2 + 10,
                    this.chartHeight - this.calendarXAxes.getHeight() - this.calendarXAxes.year.getHeight() - 10,
                    'CLOSED', this.theme.burndownTheme.watermarkColor);
            } else if (this.isAbandonedProject()) {
                this.drawWatermark(svg, startX - this.dayWidth / 2 + 10,
                    this.chartHeight - this.calendarXAxes.getHeight() - this.calendarXAxes.year.getHeight() - 10,
                    'ABANDONED', this.theme.burndownTheme.watermarkColor);
            }
            // Optimal burn down rate

            if (this.isPlannedBurnDownGuideAvailable()) {
                this.drawPlannedBurnDownGuide(svg, firstDayX, this.ganttWorkWithoutBufferPerDayAccumulated!);
                this.drawPlannedBurnDownGuide(svg, firstDayX, this.ganttWorkWithBufferPerDayAccumulated!);
            } else {
                // ---linear guide
                if (Number.isFinite(this.replayTotalEstimate))
                    this.drawOptimalBurnDownGuide(svg, firstDayX, this.replayTotalEstimate);
            }

            if (this.eWorstWork != null) {
                this.drawOptimalBurnDownGuide(svg, firstDayX, this.eWorstWork);
            }

            // release extrapolation (Velocity)
            const releaseMilestone = this.milestones.get('R');
            if (releaseMilestone && !releaseMilestone.hidden) {
                const x1 = startX - this.dayWidth / 2 + 1;
                const y1 = this.diagram.y + this.diagram.height - this.calculateGraphHeight(this.replayTotalEstimate);
                const x = firstDayX + (DateUtils.calculateDays(firstDay, releaseMilestone.time) - this.scrollOffset + this.calendarXAxes.priRun) * this.dayWidth + this.dayWidth / 2;
                this.drawDottedGuideLine(svg, x1, y1, x, this.diagram.y + this.diagram.height, ColorUtils.intToHex(this.extrapolationColor));
                // svg.appendChild(SvgUtils.createLine(x1, y1, x, this.diagram.y + this.diagram.height, {
                //     stroke: ColorUtils.intToHex(this.extrapolationColor),
                //     'stroke-width': STANDARD_LINE_STROKE_WIDTH,
                //     'stroke-dasharray': '3'
                // }));
            }
        }

        this.drawAuthorLegend(svg, legendX);
        this.drawLegend(svg, legendX, this.diagram.y, this.extrapolationColor);

        this.inspectionLayer = SvgUtils.createGroup({class: 'burndown-inspection-layer', 'pointer-events': 'none'});
        svg.appendChild(this.inspectionLayer);

        // ---Maybe this is a test case ticket? (nothing to draw)
    }

    // ── Java: protected void drawAuthorLegend() / drawAuthorLegend(x, y) ──
    private drawAuthorLegend(svg: SVGElement, legendX: number): void {
        const authors = this.data.authors;
        const fontSize = 10;
        // Estimate the widest author name (mirrors FontMetrics.stringWidth loop in Java).
        let authorLegendWidth = 20;
        for (const author of authors) {
            const w = (author.userName?.length || 0) * (fontSize * 0.6);
            authorLegendWidth = Math.max(authorLegendWidth, w);
        }
        const x = legendX - authorLegendWidth - 5;
        const lineHeight = 14;
        let ay = this.diagram.y + lineHeight * authors.length;
        for (const author of authors) {
            this.drawAuthor(svg, x, ay, authorLegendWidth, author.color, author.userName || 'Unknown');
            ay -= lineHeight;
        }
    }

    // ── Java: protected void drawAuthor(int x, int y, int with, Color fillColor, String text, ...) ──
    private drawAuthor(svg: SVGElement, x: number, y: number, width: number, fillColor: number, text: string): void {
        const textColor = ColorUtils.intToHex(this.theme.burndownTheme.tickTextColor);
        svg.appendChild(SvgUtils.createRect(x, y - 6, width + 4, 12, {fill: ColorUtils.intToHex(fillColor)}));
        svg.appendChild(SvgUtils.createText(x + 2, y + 3, text, {
            fill: textColor, 'font-size': '10px', 'font-family': 'sans-serif', 'font-weight': 'bold',
        }));
    }


    // ── Java: private void drawBurnDown(LocalDate firstDay, int firstDayX, Duration estimatedWork) ──
    private drawBurnDown(svg: SVGElement, firstDay: Date, firstDayX: number): void {
        // Java: if (context.parameters.detailed) { ... } — detailed is always true in practice (see
        // ParameterOptions.detailed: "should always be true, otherwise resources will not be shown
        // in burn down chart"), so the non-detailed (simple border-only) branch is not ported here.
        const group = SvgUtils.createGroup();

        const authors = this.data.authors; // Java: usersTotalContribution.getSortedKeyList() (pre-sorted server-side)

        const nowTime = this.calendarFromDayIndex(this.replayDayIndex);
        const maxLastOrNow = nowTime.getTime() > this.milestones.lastMilestone!.getTime() ? nowTime : this.milestones.lastMilestone!;
        const minLastOrNow = nowTime.getTime() < this.milestones.lastMilestone!.getTime() ? nowTime : this.milestones.lastMilestone!;

        // graphHeight[dayIndex] = the accumulated stacked band height of all authors drawn so far
        const days = DateUtils.calculateDays(firstDay, maxLastOrNow) + 1;
        const graphHeight = new Array<number>(days + 3).fill(0);
        const remaining = new Array<number>(days + 3).fill(0);
        const valid = new Array<boolean>(days + 3).fill(true);
        const reportingDay = this.calendarFromDayIndex(this.replayDayIndex);
        const lastDayIndex = Math.min(DateUtils.calculateDays(firstDay, minLastOrNow),
            DateUtils.calculateDays(firstDay, reportingDay));
        const authorPoints = new Map<number, { authorName: string; authorSeconds: number; y: number }[]>();
        let renderedAuthors = 0;

        let authorIndex = 0;
        for (const author of authors) {
            const accumulatedWorkPerDay = this.accumulatedWorkPerDay[authorIndex];
            const remainingWorkPerDay = this.replayRemainingWorkPerDay[authorIndex];
            const maxDayIndex = DateUtils.calculateDays(firstDay, minLastOrNow);
            let hasVisibleEstimate = false;
            for (let dayIndex = 0; dayIndex <= maxDayIndex; dayIndex++) {
                if (Number.isFinite(accumulatedWorkPerDay[dayIndex])
                    && Number.isFinite(remainingWorkPerDay[dayIndex])
                    && accumulatedWorkPerDay[dayIndex] + remainingWorkPerDay[dayIndex] > 0) {
                    hasVisibleEstimate = true;
                    break;
                }
            }
            if (!hasVisibleEstimate) {
                authorIndex++;
                continue;
            }
            renderedAuthors++;

            let yesterdayX = 0, yesterdayY = 0, yesterdayY2 = 0;
            let lastX = 0, lastY = 0, lastY2 = 0;

            for (let dayIndex = 0; dayIndex <= maxDayIndex + 2; dayIndex++) {
                const x = firstDayX + (dayIndex - this.scrollOffset + this.calendarXAxes.priRun) * this.calendarXAxes.dayOfWeek.getWidth();
                let currentDayAuthorGraphHeight = 0;
                let tooltip: string | null = null;
                let y = 0;
                if (dayIndex <= maxDayIndex + 2) {
                    const worked = accumulatedWorkPerDay[dayIndex];
                    const remainingEstimate = remainingWorkPerDay[dayIndex];
                    if (Number.isFinite(worked) && Number.isFinite(remainingEstimate)) {
                        const estimatedWork = worked + remainingEstimate;
                        currentDayAuthorGraphHeight = this.calculateAuthorGraphHeight(worked, estimatedWork, this.maxWorked);
                        remaining[dayIndex] += remainingEstimate;
                    } else {
                        valid[dayIndex] = false;
                    }
                    y = this.diagram.y + this.diagram.height - graphHeight[dayIndex] - currentDayAuthorGraphHeight;
                    const pointX = x - this.dayWidth / 2;
                    if (dayIndex >= 1 && dayIndex <= lastDayIndex + 1
                        && pointX >= this.diagram.x && pointX <= this.containerWidth
                        && Number.isFinite(worked) && Number.isFinite(remainingEstimate) && Number.isFinite(y)) {
                        const points = authorPoints.get(dayIndex) ?? [];
                        points.push({
                            authorName: author.userName || 'Unknown',
                            authorSeconds: remainingEstimate,
                            y,
                        });
                        authorPoints.set(dayIndex, points);
                    }
                    if (dayIndex > 0) {
                        const tIdx = dayIndex - 1;
                        tooltip = (tIdx >= 0 && tIdx < author.tooltipPerDay.length) ? author.tooltipPerDay[tIdx] : null;
                    }
                }

                if (x !== lastX) {
                    // ---a new day started, so we can draw the polygon of last day
                    if (yesterdayX !== 0 && lastX !== 0) {
                        const DayFromDayIndex = this.calendarFromDayIndex(dayIndex - 1);
                        this.drawPolygon(group, yesterdayX, yesterdayY, yesterdayY2, lastX - 1, lastY, lastY2,
                            authorIndex === authors.length - 1, DateUtils.isWorkDay(DayFromDayIndex),
                            ColorUtils.intToHex(this.theme.burndownTheme.borderColor),
                            this.generateBurnDownColor(author.color), tooltip, author.userName || '');
                    }
                    yesterdayX = lastX;
                    yesterdayY = lastY;
                    yesterdayY2 = lastY2;
                }
                lastX = x;
                lastY = y;
                if (dayIndex <= maxDayIndex + 2) {
                    lastY2 = this.diagram.y + this.diagram.height - graphHeight[dayIndex];
                    graphHeight[dayIndex] += currentDayAuthorGraphHeight;
                }
            }
            authorIndex++;
        }

        svg.appendChild(group);
        if (renderedAuthors) {
            const points: ActualInspectionPoint[] = [];
            for (const [index, samples] of authorPoints) {
                if (!valid[index] || !Number.isFinite(graphHeight[index]))
                    continue;
                const x = firstDayX + (index - this.scrollOffset + this.calendarXAxes.priRun) * this.dayWidth - this.dayWidth / 2;
                for (const sample of samples) {
                    points.push({
                        date: DateUtils.addDay(firstDay, index - 1),
                        seconds: remaining[index],
                        authorName: sample.authorName,
                        authorSeconds: sample.authorSeconds,
                        x,
                        y: sample.y,
                    });
                }
            }
            this.actualInspectionPoints = points;
        }
    }

    private generateBurnDownColor(color: number): string {
        // color = ColorUtils.lightenColor(color, 0.75);
        // return ColorUtils.intToHex(ColorUtils.setAlpha(color, 128));
        return ColorUtils.intToHex(color);
    }

    /**
     * x1,y1-----------------x2,y2
     *   |                      |
     *   |                      |
     * x1,y3-----------------x2,y4
     *
     * Mirrors Java: private void drawPolygon(int x1, int y1, int y3, int x2, int y2, int y4,
     * boolean drawBorder, boolean weekday, Color borderColor, Color color, List transactions, String authorName)
     */
    private drawPolygon(
        svg: SVGElement, x1: number, y1: number, y3: number, x2: number, y2: number, y4: number,
        drawBorder: boolean, _weekday: boolean, borderColor: string, color: string,
        tooltip: string | null, _authorName: string,
    ): void {
        const dw = this.dayWidth;
        let points = [`${x1 - dw / 2},${y1}`];
        if (dw > 3) {
            points.push(...[
                `${x2 - dw / 2 + 1 - 1},${y2}`,
                `${x2 - dw / 2 + 1 - 1},${y4}`,
            ]);
        } else {
            points.push(...[
                `${x2 - dw / 2 + 1},${y2}`,
                `${x2 - dw / 2 + 1},${y4}`,
            ]);
        }
        points.push(...[`${x1 - dw / 2},${y3}`]);
        const polygon = SvgUtils.createPolygon({
            points: points.join(' '),
            fill: color,
            stroke: 'none'
        });
        svg.appendChild(polygon);
        if (drawBorder) {
            svg.appendChild(SvgUtils.createLine(x1 - dw / 2, y1, x2 - 1 - dw / 2, y2, {
                stroke: borderColor, 'stroke-width': STANDARD_LINE_STROKE_WIDTH,
            }));
        }
    }

    // ── Java: protected void drawOptimalBurnDownGuide(int firstDayX, Duration estimatedWork) ──
    private drawOptimalBurnDownGuide(svg: SVGElement, firstDayX: number, estimatedWork: number): void {
        const firstDay = this.milestones.get('S')!.time;
        const lastDay = this.milestones.get('E')!.time;
        const workingDays = DateUtils.calculateWorkingDaysIncluding(firstDay, lastDay);
        const workPerWorkingDay = estimatedWork / workingDays;
        const color = ColorUtils.intToHex(this.theme.burndownTheme.optimaleGuideColor);

        let workingdays = 0;
        let lastX = firstDayX + (DateUtils.calculateDays(firstDay, firstDay) - this.scrollOffset + this.calendarXAxes.priRun) * this.calendarXAxes.dayOfWeek.getWidth() - this.calendarXAxes.dayOfWeek.getWidth() / 2 + 1;
        let lastY = this.diagram.y + this.diagram.height - this.calculateGraphHeight(estimatedWork);

        for (let currentDay = new Date(firstDay); currentDay.getTime() <= lastDay.getTime(); currentDay = DateUtils.addDay(currentDay, 1)) {
            const daysX = firstDayX + (DateUtils.calculateDays(firstDay, currentDay) - this.scrollOffset + this.calendarXAxes.priRun) * this.calendarXAxes.dayOfWeek.getWidth() + this.calendarXAxes.dayOfWeek.getWidth() / 2 - 1;
            // const daysX = this.calculateX(currentDay, firstDay, SECONDS_PER_WORKING_DAY)
            const dow = currentDay.getDay();
            if (dow !== 6 && dow !== 0) workingdays++;
            const work = estimatedWork - workingdays * workPerWorkingDay;
            const y = this.diagram.y + this.diagram.height - this.calculateGraphHeight(work);
            this.drawDottedGuideLine(svg, lastX, lastY, daysX, y, color);
            lastX = daysX;
            lastY = y;
        }
    }

    /**
     * Draw the burn down guide taken from the gantt chart.
     * Mirrors Java: private void drawPlannedBurnDownGuide(int firstDayX, BurnDownGuide guide)
     */
    private drawPlannedBurnDownGuide(svg: SVGElement, firstDayX: number, guide: number[]): void {
        let lastX = 0;
        let lastY: number = 0;
        if (this.isPlannedBurnDownGuideAvailable()) {
            lastY = this.diagram.y + this.diagram.height - this.calculateGraphHeight(guide[0])
        } else {
            lastY = this.diagram.y + this.diagram.height - this.calculateGraphHeight(this.maxWorked);
        }
        const color = ColorUtils.intToHex(this.theme.burndownTheme.plannedGuideColor);

        // Java: guide.getSize() — the guide's own (firstMilestone-relative) length.
        // const guideSize = guide.length - this.legacyCalendarXAxes.priRun;
        for (let i = 0; i < guide.length; i++) {
            const r = guide[i];
            const x = firstDayX + (i - this.scrollOffset + this.calendarXAxes.priRun) * this.calendarXAxes.dayOfWeek.getWidth() - this.calendarXAxes.dayOfWeek.getWidth() / 2 + 1;
            const y = this.diagram.y + this.diagram.height - this.calculateGraphHeight(r);
            if (i !== 0) {
                this.drawDottedGuideLine(svg, lastX, lastY, x, y, color);
            }
            lastX = x;
            lastY = y;
        }
    }

    private drawDottedGuideLine(svg: SVGElement, x1: number, y1: number, x2: number, y2: number, color: string): void {
        svg.appendChild(SvgUtils.createLine(x1, y1, x2, y2, {
            stroke: '#ffffff', 'stroke-width': STANDARD_LINE_STROKE_WIDTH - 1,
        }));
        svg.appendChild(SvgUtils.createLine(x1, y1, x2, y2, {
            stroke: color, 'stroke-width': STANDARD_LINE_STROKE_WIDTH, 'stroke-dasharray': '3',
        }));
    }

    // ── Java: private void drawWatermark(int x, int y, String watermark, Color watermarkColor) ──
    private drawWatermark(svg: SVGElement, x: number, y: number, watermark: string, watermarkColor: number | null): void {
        svg.appendChild(SvgUtils.createText(x, y, watermark, {
            fill: ColorUtils.intToHex(watermarkColor),
            'font-size': '64px', 'font-family': 'Arial, sans-serif', 'font-weight': 'bold',
        }));
    }

    // ── Java: private void drawYAxes(int startX, Duration estimatedWork) ──
    private drawYAxes(svg: SVGElement, startX: number, estimatedWork: number): void {
        const yAxis = new GraphSquare(
            startX - this.dayWidth / 2 - Y_AXIS_WIDTH,
            this.diagram.y,
            Y_AXIS_WIDTH,
            this.diagram.height,
        );
        let mark = Math.floor(estimatedWork / 5);
        let markUnit: string;
        if (mark > ONE_WORK_MONTH) {
            mark = ONE_WORK_MONTH;
            markUnit = 'pm';
        } else if (mark > ONE_WORK_WEEK) {
            mark = ONE_WORK_WEEK;
            markUnit = 'pw';
        } else {
            mark = SECONDS_PER_WORKING_DAY;
            markUnit = 'pd';
        }
        const plotBottom = this.diagram.y + this.diagram.height;
        const tickColor = ColorUtils.intToHex(this.theme.burndownTheme.ticksColor);
        const gridColor = ColorUtils.intToHex(this.theme.ganttTheme.gridColor);
        const textColor = ColorUtils.intToHex(this.theme.burndownTheme.tickTextColor);
        let lastMarkY = 99999;
        for (let timeMark = 0; timeMark < estimatedWork; timeMark += mark) {
            const markY = plotBottom - 1 - this.calculateGraphHeight(timeMark);
            if (lastMarkY - markY > 12) {
                svg.appendChild(SvgUtils.createLine(
                    startX - 4 - this.dayWidth / 2,
                    markY,
                    startX - 4 - this.dayWidth / 2 + 4,
                    markY,
                    {
                        stroke: tickColor,
                        'stroke-width': '1',
                        'vector-effect': 'non-scaling-stroke'
                    }));
                svg.appendChild(SvgUtils.createLine(
                    startX - 1,
                    markY,
                    this.getChartEndX(),
                    markY,
                    {
                        stroke: gridColor,
                        'stroke-width': '1',
                        'vector-effect': 'non-scaling-stroke'
                    }));
                const label = `${Math.round(timeMark / mark)}${markUnit}`;
                svg.appendChild(SvgUtils.createText(
                    yAxis.x + yAxis.width - 5,
                    markY,
                    label,
                    {
                        fill: textColor, 'font-size': '11px', 'font-family': 'sans-serif',
                        'text-anchor': 'end', 'dominant-baseline': 'middle',
                    }));
                lastMarkY = markY;
            }
        }
    }

    private getChartEndX(): number {
        return this.diagram.x + (this.days - this.scrollOffset) * this.dayWidth;
    }
}
