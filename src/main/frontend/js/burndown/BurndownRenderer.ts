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

// ── Constants (mirrors Java BurnDownRenderer static fields) ─────────────────
const ONE_WEEK = 7;
const ONE_WORK_MONTH = 20 * 75 * 60 * 6;
const ONE_WORK_WEEK = 5 * 75 * 60 * 6;
const SECONDS_PER_WORKING_DAY = 75 * 6 * 60;
/** Mirrors Java: public static final int Y_AXIS_WIDTH = 50; */
export const Y_AXIS_WIDTH = 50;
const STANDARD_LINE_STROKE_WIDTH = 3.1;

/**
 * Fixed plot (diagram) height in pixels. Mirrors Java's {@code dao.chartHeight}, which is
 * computed externally by the caller (e.g. RenderDao) for the SVG/PNG export path. For the
 * interactive browser chart we pick a constant plot height instead.
 */
const PLOT_HEIGHT = 240;

let burndownClipSeq = 0;

/**
 * Standard agile burn down chart, based on logged work in jira tickets of a
 * specific sprint.
 *
 * Mirrors Java: {@code BurnDownRenderer extends AbstractRenderer}.
 */
export class BurndownRenderer extends AbstractRenderer {
    readonly data: GanttBurndownChartDto;

    // ── fields mirroring BurnDownRenderer.java ──────────────────────────────
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
    private readonly eBestWork: number;
    private readonly eWorstWork: number | null;
    private extrapolationColor!: number | null;
    private readonly maxActualWorked: number;
    private readonly maxWorked!: number;
    private readonly sprintClosed: boolean;
    private yAxis!: GraphSquare;
    private readonly calendarSize: CalendarSize;

    constructor(chart: AbstractChart, data: GanttBurndownChartDto) {
        const meta = data.meta;
        // Java: super(dao) — AbstractRenderer(RenderDao) sets theme/chartWidth/chartHeight/milestones/
        // calendarXAxes. milestones starts empty here; createMilestones() (called below from
        // processingInit) populates it, mirroring BurnDownRenderer(RenderDao) -> processingInit(dao)
        // -> createMilestones(...).
        super(chart/*, new Milestones([])*/, meta.preRun || 0, meta.postRun || 0);

        this.data = data;
        // Java: init(dao) -> initSize(dao.firstDayX, true, dao.calendarSize) — BurnDownRenderer
        // always draws its own calendar at the BOTTOM of its plot area, so that it sits directly
        // above the Gantt chart's own (top) calendar when the two are stacked in GanttBurndownChart.
        this.calendarAtBottom = true;

        this.chartStart = DateUtils.getDayMidnight(new Date(meta.chartStart));
        // this.totalDays = this.calculateMaxDays();// meta.totalDays || DateUtils.calculateDayCount(this.chartStart, DateUtils.getDayMidnight(new Date(meta.chartEnd)));
        this.days = this.calculateMaxDays();
        this.currentDate = meta.now ? DateUtils.getDayMidnight(new Date(meta.now)) : null;
        this.dayWidth = 20;

        this.sprintClosed = meta.sprintClosed;
        this.ganttWorkWithoutBufferPerDayAccumulated = data.ganttGuideWithoutBuffer || null;
        this.ganttWorkWithBufferPerDayAccumulated = data.ganttGuideWithBuffer || null;
        // was the estimation in the gantt chart actually bigger than the current estimation?
        this.maxActualWorked = meta.maxWorkedSeconds;
        this.maxWorked = meta.maxWorkedSeconds;
        if (this.ganttWorkWithoutBufferPerDayAccumulated) {
            this.maxWorked = Math.max(this.maxWorked, this.ganttWorkWithoutBufferPerDayAccumulated[0]);
        }
        if (this.ganttWorkWithBufferPerDayAccumulated) {
            this.maxWorked = Math.max(this.maxWorked, this.ganttWorkWithBufferPerDayAccumulated[0]);
        }
        // this.maxWorked = meta.maxWorkedSeconds;
        this.eBestWork = meta.estimatedBestWorkSeconds;
        this.eWorstWork = null; // dao.estimatedWorstWork has no DTO equivalent yet
        this.calendarSize = data.meta.calendarSize;

        // Java: processingInit(dao) -> createMilestones(...) -> init(dao)
        this.processingInit(meta);
    }

    // ── AbstractRenderer overrides ───────────────────────────────────────────

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
    //     const calendarH = this.calendarXAxes.getHeight(this.dayWidth, this.milestones.list.length > 0);
    //     return calendarH + PLOT_HEIGHT;
    // }

    // ── Java: public void draw(ExtendedGraphics2D graphics2D, int x, int y) ──
    override draw(svg: SVGElement, x: number, y: number): void {
        // Java: init(dao) -> initSize(...) is called once during construction; here we redo the
        // size computation on every draw() because containerWidth/dayWidth can change between
        // renders (interactive zoom/pan/resize), unlike the static Java export.
        this.calculateDayWidth();
        this.chartWidth = this.calculateChartWidth();
        this.chartHeight = this.calculateChartHeight();
        // this.diagram.initSize(this.containerWidth, PLOT_HEIGHT);

        this.initPosition(this.firstDayX + x, y);
        this.yAxis = new GraphSquare(2, this.diagram.y, Y_AXIS_WIDTH, this.diagram.height);

        this.drawCalendar(svg, true, this.containerWidth);
        this.drawMilestones(svg);
        this.createBurnDownChart(svg);
    }

    // ── Java: protected void createMilestones(LocalDateTime start, LocalDateTime now, LocalDateTime end,

    // ── Java: public void init(RenderDao dao) ──
    public init(): void {
        // Java: initSize(dao.firstDayX, true, dao.calendarSize) — sizing happens per-draw() in this
        // interactive port instead (see draw()), since containerWidth/dayWidth can change on
        // zoom/pan/resize, unlike the static Java export.
        this.initSize(this.data.meta.firstDayX, true, this.calendarSize, this.containerWidth);
        const rMilestone = this.milestones.get('R');
        const eMilestone = this.milestones.get('E');
        if (rMilestone && eMilestone && rMilestone.time.getTime() > eMilestone.time.getTime()) {
            this.extrapolationColor = this.theme.burndownTheme.delayEventColor;
        } else {
            this.extrapolationColor = this.theme.burndownTheme.inTimeColor;
        }
        // Java: calculateAuthorContribution(...), the per-day usersWorkPerDayAccumulated construction,
        // and the final milestones.add("L", ..., hidden=true) refinement are all pre-computed
        // server-side in GanttBurndownChartService and delivered via data.authors / data.ganttGuide*.
    }

    // ── Java: private void processingInit(RenderDao dao) ──
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
    }

    /**
     * Converts a Java-semantics dayIndex (relative to milestones.firstMilestone, as used throughout
     * BurnDownRenderer.java) into an index into this.data's server-computed per-day arrays, which are
     * pre-computed relative to chartStart (= firstMilestone - priRun, guaranteed by createMilestones()
     * above). This is the one explicit adaptation needed because the DTO arrays are built server-side
     * before the client's Milestones are known.
     */
    private toArrayIndex(dayIndex: number): number {
        return dayIndex + this.calendarXAxes.priRun;
    }

    // ── Java: protected int calculateAuthorGraphHeight(Duration authorDelta, Duration authorEstimated, Duration sumEstimated) ──
    private calculateAuthorGraphHeight(authorDeltaSeconds: number, authorEstimatedSeconds: number, sumEstimatedSeconds: number): number {
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

    // ── Java: protected boolean isPlannedBurnDownGuideAvailable() ──
    private isPlannedBurnDownGuideAvailable(): boolean {
        return this.ganttWorkWithoutBufferPerDayAccumulated != null;
    }

    // ── Java: private boolean isAbandonedProject() ──
    private isAbandonedProject(): boolean {
        return this.sprintClosed;
    }

    // ── Java: private Color generateBurnDownColor(Color color) ──
    // Kept for parity with Java; this port receives the already-lightened/alpha'd author.color
    // computed server-side in GanttBurndownChartService (same ColorUtil.lightenColor(0.75)+alpha 128

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

        this.drawAuthorLegend(svg);
        this.drawLegend(svg, this.diagram.width - 130, this.diagram.y, this.extrapolationColor);


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
                this.drawOptimalBurnDownGuide(svg, firstDayX, this.eBestWork);
            }

            if (this.eWorstWork != null) {
                this.drawOptimalBurnDownGuide(svg, firstDayX, this.eWorstWork);
            }

            // release extrapolation (Velocity)
            const releaseMilestone = this.milestones.get('R');
            if (releaseMilestone) {
                const x1 = startX - this.dayWidth / 2 + 1;
                const y1 = this.diagram.y + this.diagram.height - this.calculateGraphHeight(this.maxActualWorked);
                const x = firstDayX + (DateUtils.calculateDays(firstDay, releaseMilestone.time) - this.scrollOffset + this.calendarXAxes.priRun) * this.dayWidth + this.dayWidth / 2;
                svg.appendChild(SvgUtils.createLine(x1, y1, x, this.diagram.y + this.diagram.height, {
                    stroke: ColorUtils.intToHex(this.extrapolationColor),
                    'stroke-width': STANDARD_LINE_STROKE_WIDTH,
                    'stroke-dasharray': '3'
                }));
            }
        }
        // ---Maybe this is a test case ticket? (nothing to draw)
    }

    // ── Java: protected void drawAuthorLegend() / drawAuthorLegend(x, y) ──
    private drawAuthorLegend(svg: SVGElement): void {
        const authors = this.data.authors;
        const fontSize = 10;
        // Estimate the widest author name (mirrors FontMetrics.stringWidth loop in Java).
        let authorLegendWidth = 20;
        for (const author of authors) {
            const w = (author.userName?.length || 0) * (fontSize * 0.6);
            authorLegendWidth = Math.max(authorLegendWidth, w);
        }
        const x = this.diagram.width - 130 - authorLegendWidth - 5;
        const lineHeight = 14;
        let ay = this.diagram.y + lineHeight * authors.length;
        for (const author of authors) {
            this.drawAuthor(svg, x, ay, authorLegendWidth, author.color, author.userName || 'Unknown');
            ay -= lineHeight;
        }
    }

    // ── Java: protected void drawAuthor(int x, int y, int with, Color fillColor, String text, ...) ──
    private drawAuthor(svg: SVGElement, x: number, y: number, width: number, fillColor: number, text: string): void {
        const textColor = ColorUtils.intToHex(this.theme.burndownTheme.tickTextColor, '#334155');
        svg.appendChild(SvgUtils.createRect(x, y - 6, width + 4, 12, {fill: ColorUtils.intToHex(fillColor, '#1f8fff')}));
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

        const nMilestone = this.milestones.get('N');
        const nowTime = nMilestone ? nMilestone.time : (this.currentDate ?? this.milestones.lastMilestone!);
        const maxLastOrNow = nowTime.getTime() > this.milestones.lastMilestone!.getTime() ? nowTime : this.milestones.lastMilestone!;
        const minLastOrNow = nowTime.getTime() < this.milestones.lastMilestone!.getTime() ? nowTime : this.milestones.lastMilestone!;

        // graphHeight[dayIndex] = the accumulated stacked band height of all authors drawn so far
        const days = DateUtils.calculateDays(firstDay, maxLastOrNow) + 1;
        const graphHeight = new Array<number>(days + 3).fill(0);

        let authorIndex = 0;
        for (const author of authors) {
            const authorEstimatedWork = author.totalWorkedSeconds + author.totalRemainingSeconds;
            if (authorEstimatedWork === 0) {
                authorIndex++;
                continue;
            }

            let yesterdayX = 0, yesterdayY = 0, yesterdayY2 = 0;
            let lastX = 0, lastY = 0, lastY2 = 0;
            const nowDayIndex = DateUtils.calculateDays(firstDay, minLastOrNow);
            const maxDayIndex = nowDayIndex;

            for (let dayIndex = 0; dayIndex <= maxDayIndex + 2; dayIndex++) {
                const x = firstDayX + (dayIndex - this.scrollOffset + this.calendarXAxes.priRun) * this.calendarXAxes.dayOfWeek.getWidth();
                let currentDayAuthorGraphHeight = 0;
                let tooltip: string | null = null;
                let y = 0;
                if (dayIndex <= maxDayIndex + 2) {
                    // const arrIndex = this.toArrayIndex(dayIndex);
                    currentDayAuthorGraphHeight = this.calculateAuthorGraphHeight(author.accumulatedWorkPerDay[dayIndex], authorEstimatedWork, this.maxWorked);
                    y = this.diagram.y + this.diagram.height - graphHeight[dayIndex] - currentDayAuthorGraphHeight;
                    if (dayIndex > 0) {
                        const tIdx = dayIndex - 1;
                        tooltip = (tIdx >= 0 && tIdx < author.tooltipPerDay.length) ? author.tooltipPerDay[tIdx] : null;
                    }
                }

                if (x !== lastX) {
                    // ---a new day started, so we can draw the polygon of last day
                    if (yesterdayX !== 0 && lastX !== 0) {
                        const DayFromDayIndex = this.calendarFromDayIndex(dayIndex - 1);
                        this.drawPolygon(group, yesterdayX, yesterdayY, yesterdayY2, lastX, lastY, lastY2,
                            authorIndex === authors.length - 1, DateUtils.isWorkDay(DayFromDayIndex),
                            ColorUtils.intToHex(this.theme.burndownTheme.borderColor, '#334155'),
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
    }

    private generateBurnDownColor(color: number): string {
        color = ColorUtils.lightenColor(color, 0.75);
        return ColorUtils.intToHex(ColorUtils.setAlpha(color, 128));
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
        if (tooltip) {
            polygon.setAttribute('data-tooltip-html', tooltip);
            polygon.setAttribute('pointer-events', 'all');
        }
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
        const color = ColorUtils.intToHex(this.theme.burndownTheme.optimaleGuideColor, '#a855f7');

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
            svg.appendChild(SvgUtils.createLine(lastX, lastY, daysX, y, {
                stroke: color, 'stroke-width': STANDARD_LINE_STROKE_WIDTH, 'stroke-dasharray': '3',
            }));
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
        const color = ColorUtils.intToHex(this.theme.burndownTheme.plannedGuideColor, '#3b82f6');

        // Java: guide.getSize() — the guide's own (firstMilestone-relative) length.
        // const guideSize = guide.length - this.calendarXAxes.priRun;
        for (let i = 0; i < guide.length; i++) {
            const r = guide[i];
            const x = firstDayX + (i - this.scrollOffset + this.calendarXAxes.priRun) * this.calendarXAxes.dayOfWeek.getWidth() - this.calendarXAxes.dayOfWeek.getWidth() / 2 + 1;
            const y = this.diagram.y + this.diagram.height - this.calculateGraphHeight(r);
            if (i !== 0) {
                svg.appendChild(SvgUtils.createLine(lastX, lastY, x, y, {
                    stroke: color, 'stroke-width': STANDARD_LINE_STROKE_WIDTH, 'stroke-dasharray': '3',
                }));
            }
            lastX = x;
            lastY = y;
        }
    }

    // ── Java: private void drawWatermark(int x, int y, String watermark, Color watermarkColor) ──
    private drawWatermark(svg: SVGElement, x: number, y: number, watermark: string, watermarkColor: number | null): void {
        svg.appendChild(SvgUtils.createText(x, y, watermark, {
            fill: ColorUtils.intToHex(watermarkColor, '#787878'),
            'font-size': '64px', 'font-family': 'Arial, sans-serif', 'font-weight': 'bold',
        }));
    }

    // ── Java: private void drawYAxes(int startX, Duration estimatedWork) ──
    private drawYAxes(svg: SVGElement, startX: number, estimatedWork: number): void {
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
        const tickColor = ColorUtils.intToHex(this.theme.burndownTheme.ticksColor, '#94a3b8');
        const gridColor = ColorUtils.intToHex(this.theme.ganttTheme.gridColor, '#e4e8f3');
        const textColor = ColorUtils.intToHex(this.theme.burndownTheme.tickTextColor, '#334155');
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
                    this.diagram.width,
                    markY,
                    {
                        stroke: gridColor,
                        'stroke-width': '1',
                        'vector-effect': 'non-scaling-stroke'
                    }));
                const label = `${Math.round(timeMark / mark)}${markUnit}`;
                svg.appendChild(SvgUtils.createText(
                    this.yAxis.x + this.yAxis.width - 5 - this.scrollOffset * this.dayWidth,
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
}
