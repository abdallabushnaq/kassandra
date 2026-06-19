// gantt-chart.js
// Interactive Gantt chart renderer.
// Mirrors Java: AbstractGanttRenderer, GanttRenderer, GanttChart
//
// Class hierarchy (matches Java):
//   AbstractRenderer  (chart-util.js)
//     └─ AbstractGanttRenderer  (this file)
//          └─ GanttRenderer     (this file)
//   AbstractCanvas  (chart-util.js)
//     └─ AbstractChart  (chart-util.js)
//          └─ GanttChart (this file)
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0
(function () {
    'use strict';

    var createSvgElement         = window.SvgUtils.createSvgElement;
    var createRect               = window.SvgUtils.createRect;
    var createText               = window.SvgUtils.createText;
    var createLine               = window.SvgUtils.createLine;
    var createClipPath           = window.SvgUtils.createClipPath;
    var intToHex                 = window.ColorUtils.intToHex;
    var convertSprintColorToRgba = window.ColorUtils.convertSprintColorToRgba;
    var hexToRgbaWithAlpha       = window.ColorUtils.hexToRgbaWithAlpha;
    var MS                       = window.DateUtils.MS;
    var getUtcDayMidnight        = window.DateUtils.getUtcDayMidnight;
    var calculateDayIndex        = window.DateUtils.calculateDayIndex;
    var calculateDayCount        = window.DateUtils.calculateDayCount;

    // ── Constants ──────────────────────────────────────────────────────────────
    // Mirrors Java: AbstractGanttRenderer field declarations

    var FINE_LINE_STROKE_WIDTH    = 1.0;       // AbstractGanttRenderer.FINE_LINE_STROKE_WIDTH
    var LINE_HEIGHT               = 18;        // AbstractGanttRenderer.LINE_HEIGHT
    var RELATION_CORNER_LENGTH    = 14;        // AbstractGanttRenderer.RELATION_CORNER_LENGTH
    var RELATION_LINE_STROKE_WIDTH = 1;        // AbstractGanttRenderer.RELATION_LINE_STROKE_WIDTH
    var RESOURCE_NAME_TO_TASK_GAP = 3;         // AbstractGanttRenderer.RESOURCE_NAME_TO_TASK_GAP
    /** seconds between work start and work end, including lunch hour */
    var SECONDS_PER_DAY           = 85 * 6 * 60; // AbstractGanttRenderer.SECONDS_PER_DAY = 30600
    var TASK_BODY_BORDER          = 1;         // AbstractGanttRenderer.TASK_BODY_BORDER
    var TASK_NAME_TO_TASK_GAP     = 5 + 8;    // AbstractGanttRenderer.TASK_NAME_TO_TASK_GAP = 13

    // Mirrors Java GanttRenderer field declarations
    var GANTT_TASK_POST_SPACE     = 0;         // GanttRenderer.GANTT_TASK_POST_SPACE
    var GANTT_TASK_PRI_SPACE      = 0;         // GanttRenderer.GANTT_TASK_PRI_SPACE
    // NoneWorkingDayFont = new Font(Font.SANS_SERIF, Font.BOLD, 22)
    var NONE_WORKING_DAY_FONT_SIZE = 22;       // GanttRenderer.NoneWorkingDayFont

    // Interactive chart constants (not in Java — needed for scrollable UI)
    var DEFAULT_DW = 20; // matches GanttRenderer.calculateDayWidth → dayOfWeek.setWidth(20)
    var MIN_DW     = 2;
    var MAX_DW     = 80;
    var ZOOM_STEP  = 1.25;

    // ── localStorage helpers ───────────────────────────────────────────────────

    function viewStateKey(containerId) {
        var chartName = (containerId || 'chart').replace(/-container$/, '');
        return 'kassandra.chart.' + chartName + '.view';
    }

    function loadViewState(containerId) {
        try {
            var raw = localStorage.getItem(viewStateKey(containerId));
            if (raw) {
                var s = JSON.parse(raw);
                if (typeof s.dayWidth === 'number' && typeof s.scrollOffset === 'number') return s;
            }
        } catch (e) { /* unavailable */ }
        return null;
    }

    function saveViewState(containerId, dayWidth, scrollOffset) {
        try {
            localStorage.setItem(viewStateKey(containerId), JSON.stringify({dayWidth: dayWidth, scrollOffset: scrollOffset}));
        } catch (e) { /* quota */ }
    }

    // ── Date / time helpers ────────────────────────────────────────────────────

    function parseLocalDate(dateStr) {
        if (!dateStr) return null;
        var p = dateStr.split('-');
        return new Date(Date.UTC(+p[0], +p[1] - 1, +p[2]));
    }

    /**
     * Parses an ISO LocalDateTime string (no timezone suffix) to a JS Date.
     * Mirrors Java: task.getStart() / task.getFinish() (LocalDateTime).
     * Strings like "2026-03-01T08:00:00" are treated as local time by JS; since we
     * use this consistently for all datetimes the relative differences are correct.
     */
    function parseLocalDateTime(str) {
        if (!str) return null;
        if (str instanceof Date) return str;
        return new Date(str);
    }

    /**
     * Returns the "8:00 AM on same day" datetime string for a given LocalDateTime string.
     * Mirrors Java: date.truncatedTo(ChronoUnit.DAYS).withHour(8)
     * e.g. "2026-03-01T10:30:00" → "2026-03-01T08:00:00"
     */
    function getDayAt8AM(datetimeStr) {
        if (!datetimeStr) return null;
        return datetimeStr.split('T')[0] + 'T08:00:00';
    }

    // ── Calendar exception helpers ─────────────────────────────────────────────

    function getCalendarException(date, exceptions) {
        if (!exceptions || !exceptions.length) return null;
        for (var i = 0; i < exceptions.length; i++) {
            var ex   = exceptions[i];
            var from = parseLocalDate(ex.from);
            var to   = parseLocalDate(ex.to);
            if (from && to && date >= from && date <= to) return ex;
        }
        return null;
    }

    /**
     * Mirrors Java: ProjectCalendar.isWorkingDate(LocalDate).
     * Returns true for Mon-Fri weekdays with no calendar exception.
     */
    function isWorkingDay(date, exceptions) {
        var dow = date.getUTCDay();
        if (dow === 0 || dow === 6) return false;
        return getCalendarException(date, exceptions) === null;
    }

    // ── AbstractGanttRenderer ──────────────────────────────────────────────────
    // Mirrors Java: AbstractGanttRenderer extends AbstractRenderer

    class AbstractGanttRenderer extends window.AbstractRenderer {
        constructor() {
            super();
            this.dayWidth        = DEFAULT_DW;
            this.scrollOffset    = 0;
            this.containerWidth  = 800;
            this.chartStart      = null;  // UTC midnight of chart first day
            this.totalDays       = 0;
            this.currentDate     = null;
            this.tasks           = [];
            this.milestones      = [];
            /** Absolute pixel Y of the top of the task area; set before drawGanttChart. */
            this._calendarH      = 0;
        }

        /** Converts a 0-based day index (from chartStart) to a viewport pixel X. */
        dayIndexToPixelX(dayIndex) {
            return (dayIndex - this.scrollOffset) * this.dayWidth;
        }

        /**
         * Mirrors Java: AbstractRenderer.calculateX(LocalDateTime date, LocalDateTime startTime, long secondsPerDay)
         *
         * Java formula:
         *   firstMilestoneX = firstDayX + dayWidth/2
         *   dayX = firstMilestoneX + (calculateDays(firstMilestone, toDayPrecision(date)) + priRun) * dayWidth
         *   timeOfDayX = (int)((workedToday.getSeconds() * dayWidth) / secondsPerDay)
         *   return dayX + timeOfDayX
         *   → caller does: x = calculateX(...) - dayWidth/2
         *
         * JS simplification (firstDayX=0, priRun baked into chartStart, subtract dayWidth/2 and scrollOffset):
         *   return dayIndexToPixelX(dayIndex) + timeOfDayX
         *
         * @param {string} datetimeStr   ISO LocalDateTime string, e.g. "2026-03-01T10:30:00"
         * @param {string} startTimeStr  ISO LocalDateTime for working-day start, e.g. "2026-03-01T08:00:00"
         * @param {number} secondsPerDay Working seconds per day (SECONDS_PER_DAY = 30600)
         * @returns {number} Viewport pixel X
         */
        calculateX(datetimeStr, startTimeStr, secondsPerDay) {
            var date      = parseLocalDateTime(datetimeStr);
            var startTime = parseLocalDateTime(startTimeStr);
            // DateUtil.calculateDays(firstMilestone, toDayPrecision(date)) + priRun
            // = calculateDayIndex(date, chartStart)  [chartStart = firstMilestone - preRun]
            var dayIndex = calculateDayIndex(datetimeStr, this.chartStart);
            // Duration.between(startTime, date).getSeconds()
            var workedSeconds = (date.getTime() - startTime.getTime()) / 1000;
            // (int)((workedSeconds * dayWidth) / secondsPerDay)
            var timeOfDayX = Math.floor(workedSeconds * this.dayWidth / secondsPerDay);
            return this.dayIndexToPixelX(dayIndex) + timeOfDayX;
        }

        // Mirrors Java: AbstractGanttRenderer.getTaskHeight()
        getTaskHeight() {
            return LINE_HEIGHT; // LINE_HEIGHT * numberOfLinesPerTask (= 18 * 1)
        }

        // Mirrors Java: GanttRenderer.calculateChartHeight()
        calculateChartHeight() {
            var calH = this.calendarXAxes
                ? this.calendarXAxes.getHeight(this.dayWidth, this.milestones.length > 0)
                : 0;
            // calendarXAxes.getHeight() + GANTT_TASK_PRI_SPACE + tasks * (taskHeight+1) + GANTT_TASK_POST_SPACE
            return calH + GANTT_TASK_PRI_SPACE + this.tasks.length * (this.getTaskHeight() + 1) + GANTT_TASK_POST_SPACE;
        }

        // ── Color helpers ──────────────────────────────────────────────────────

        /**
         * Mirrors Java: GraphColorUtil.getDayOfWeekStripBgColor(theme, date)
         *   weekday   → xAxesTheme.dayOfweekBgColor
         *   Saturday  → chartTheme.dayOfweekSaturdayBgColor
         *   Sunday    → chartTheme.dayOfweekSundayBgColor
         */
        getDayOfWeekStripBgColor(dayDate) {
            var dow = dayDate.getUTCDay();
            if (dow === 6) return intToHex(this.theme.chartTheme.dayOfweekSaturdayBgColor, '#d7d7d7');
            if (dow === 0) return intToHex(this.theme.chartTheme.dayOfweekSundayBgColor,   '#d7d7d7');
            return intToHex(this.theme.xAxesTheme.dayOfweekBgColor, '#ffffff');
        }

        /**
         * Mirrors Java: GraphColorUtil.getGanttDayStripeColor(theme, pc, currentDate)
         *   Saturday / Sunday / working weekday → getDayOfWeekStripBgColor
         *   non-working weekday (exception)     → exception-type color
         */
        getGanttDayStripeColor(task, dayDate) {
            var dow = dayDate.getUTCDay();
            if (dow === 6 || dow === 0 || isWorkingDay(dayDate, task.calendarExceptions)) {
                return this.getDayOfWeekStripBgColor(dayDate);
            }
            var exception = getCalendarException(dayDate, task.calendarExceptions);
            if (exception) {
                var t = exception.type;
                if (t === 'VACATION') return intToHex(this.theme.ganttTheme.vacationBgColor, '#a0c8ff');
                if (t === 'TRIP')     return intToHex(this.theme.ganttTheme.tripBgColor,     '#c8a0ff');
                if (t === 'SICK')     return intToHex(this.theme.ganttTheme.sickBgColor,     '#ffa0a0');
                return intToHex(this.theme.ganttTheme.holidayBgColor, '#ffd0a0');
            }
            return intToHex(this.theme.xAxesTheme.dayOfMonthWeekendBgColor, '#d7d7d7');
        }

        // ── drawDayBars (base) ─────────────────────────────────────────────────

        /**
         * Mirrors Java: AbstractRenderer.drawDayBars(LocalDate currentDay)
         * Base implementation: draws plain weekday/weekend background for each task row.
         * GanttRenderer overrides this.
         */
        drawDayBars(g, dayDate, calendarH) {
            var dayIdx    = calculateDayIndex(dayDate, this.chartStart);
            var dayLeft   = this.dayIndexToPixelX(dayIdx);
            var gridColor = intToHex(this.theme.ganttTheme.gridColor, '#e4e8f3');
            var self      = this;

            for (var ti = 0; ti < this.tasks.length; ti++) {
                var task = this.tasks[ti];
                var rowY = calendarH + task.rowIndex * (self.getTaskHeight() + 1);

                // ── 1. Grid ───────────────────────────────────────────────────
                // Mirrors Java:
                //   fillRect(x1-1, y1-1, dayWidth, 1)  → top --   (x1 = dayLeft+1, so x1-1 = dayLeft)
                //   fillRect(x1-1, y1,   1, taskHeight) → left |
                g.appendChild(createRect(dayLeft,     rowY - 1, self.dayWidth, 1,          {fill: gridColor})); // top --
                g.appendChild(createRect(dayLeft,     rowY,     1,             LINE_HEIGHT, {fill: gridColor})); // left |

                // ── 2. Background ─────────────────────────────────────────────
                // Mirrors Java: getGanttDayStripeColor → fill rect at (x1, y1, dayWidth-1, taskHeight)
                var bgColor = self.getGanttDayStripeColor(task, dayDate);
                g.appendChild(createRect(dayLeft + 1, rowY, self.dayWidth - 1, LINE_HEIGHT, {fill: bgColor}));

                // ── 3. Exception letter ───────────────────────────────────────
                // Mirrors Java: NoneWorkingDayFont (22pt bold), drawString(letter, x-xShift, y+yShift)
                var exception = getCalendarException(dayDate, task.calendarExceptions);
                if (exception && exception.letter && self.dayWidth >= 14) {
                    var cx     = dayLeft + self.dayWidth / 2;
                    var letter = createText(cx, rowY + LINE_HEIGHT / 2, exception.letter, {
                        fill: intToHex(self.theme.ganttTheme.outOfOfficeColor, '#ffffff'),
                        'font-size': String(NONE_WORKING_DAY_FONT_SIZE),
                        'font-family': 'sans-serif',
                        'font-weight': 'bold',
                        'text-anchor': 'middle',
                        'dominant-baseline': 'middle'
                    });
                    letter.appendChild(createSvgElement('title', {}, exception.type || 'Off-day'));
                    g.appendChild(letter);
                }
            }
        }

        // ── Task drawing methods ───────────────────────────────────────────────

        /**
         * Mirrors Java: AbstractGanttRenderer.drawConflictMarker(int y, List<Conflict> conflict)
         * Only used in team planner chart. In GanttRenderer conflict is always null.
         */
        drawConflictMarker(g, y, conflict) {
            if (conflict != null) {
                // only used in team planner chart
                // graphics2D.setColor(Color.red);
                // for (Conflict c : conflict) { if (c.originalConflict) { fillRect(...) } }
            }
        }

        /**
         * Mirrors Java: AbstractGanttRenderer.drawCriticalMarker(Task task, int x1, int x2, int y)
         * Draws the task outline per day:
         *   working day  → solid top/bottom; first/last day also get a vertical side too
         *   non-working  → dotted top/bottom (every 4th absolute pixel, 2px wide)
         *
         * Note: xStart/xFinish per day are always at whole-day pixel boundaries because
         *   calculateX(day@8AM, day@8AM, SECONDS_PER_DAY) = dayIndexToPixelX(d) + 0  → left edge
         *   calculateX(day@8AM+SECONDS_PER_DAY, day@8AM, SECONDS_PER_DAY) = dayIndexToPixelX(d) + dayWidth
         */
        drawCriticalMarker(g, task, x1, x2, y) {
            if (task.critical) {
                var borderColor = intToHex(this.theme.ganttTheme.criticalTaskBorderColor, '#ff0000');
            } else {
                var borderColor = intToHex(this.theme.ganttTheme.taskBorderColor, '#888888');
            }
            // ProjectCalendar pc — in JS: use task.calendarExceptions
            // int days = Duration.between(start.truncatedTo(DAYS), finish.truncatedTo(DAYS)).toDays()
            var startDayIdx  = calculateDayIndex(task.start,  this.chartStart);
            var finishDayIdx = calculateDayIndex(task.finish, this.chartStart);
            var days         = finishDayIdx - startDayIdx;

            for (var day = 0; day <= days; day++) {
                // LocalDateTime currentDay = start.truncatedTo(DAYS).plusDays(day)
                var dayIdx      = startDayIdx + day;
                var dayDate     = new Date(this.chartStart.getTime() + dayIdx * MS);
                // pc.isWorkingDate(currentDay.toLocalDate())
                var working     = isWorkingDay(dayDate, task.calendarExceptions);
                // xStart = calculateX(currentDay@8AM, currentDay@8AM, SECONDS_PER_DAY) - dayWidth/2
                //        = dayIndexToPixelX(dayIdx)
                var xStart      = this.dayIndexToPixelX(dayIdx);
                // xFinish = calculateX(currentDay@8AM+SECONDS_PER_DAY, currentDay@8AM, SECONDS_PER_DAY) - dayWidth/2
                //         = dayIndexToPixelX(dayIdx) + dayWidth
                var xFinish     = xStart + this.dayWidth;

                if (working) {
                    if (days === 0) {
                        // this is the left and right end
                        g.appendChild(createRect(x1, y - this.getTaskHeight()/2 + TASK_BODY_BORDER, x2 - x1 + 1, 1, {fill: borderColor})); //upper -
                        g.appendChild(createRect(x1, y + this.getTaskHeight()/2 - TASK_BODY_BORDER - 1, x2 - x1 + 1, 1, {fill: borderColor})); //lower -
                        g.appendChild(createRect(x1, y - this.getTaskHeight()/2 + TASK_BODY_BORDER + 1, 1, this.getTaskHeight() - TASK_BODY_BORDER*2 - 2, {fill: borderColor})); //start |
                        g.appendChild(createRect(x2, y - this.getTaskHeight()/2 + TASK_BODY_BORDER + 1, 1, this.getTaskHeight() - TASK_BODY_BORDER*2 - 2, {fill: borderColor})); //end |
                    } else if (day === 0) {
                        // this is the left end
                        g.appendChild(createRect(x1, y - this.getTaskHeight()/2 + TASK_BODY_BORDER, xFinish - x1, 1, {fill: borderColor})); //upper -
                        g.appendChild(createRect(x1, y + this.getTaskHeight()/2 - TASK_BODY_BORDER - 1, xFinish - x1, 1, {fill: borderColor})); //lower -
                        g.appendChild(createRect(x1, y - this.getTaskHeight()/2 + TASK_BODY_BORDER + 1, 1, this.getTaskHeight() - TASK_BODY_BORDER*2 - 2, {fill: borderColor})); //start |
                    } else if (day === days) {
                        // this is the right end
                        g.appendChild(createRect(xStart, y - this.getTaskHeight()/2 + TASK_BODY_BORDER, x2 - xStart + 1, 1, {fill: borderColor})); //upper -
                        g.appendChild(createRect(xStart, y + this.getTaskHeight()/2 - TASK_BODY_BORDER - 1, x2 - xStart + 1, 1, {fill: borderColor})); //lower -
                        g.appendChild(createRect(x2,     y - this.getTaskHeight()/2 + TASK_BODY_BORDER + 1, 1, this.getTaskHeight() - TASK_BODY_BORDER*2 - 2, {fill: borderColor})); //end |
                    } else {
                        // this is the middle
                        g.appendChild(createRect(xStart, y - this.getTaskHeight()/2 + TASK_BODY_BORDER, this.dayWidth, 1, {fill: borderColor}));
                        g.appendChild(createRect(xStart, y + this.getTaskHeight()/2 - TASK_BODY_BORDER - 1, this.dayWidth, 1, {fill: borderColor}));
                    }
                } else {
                    // non-working day: dotted border
                    // for (int i = 0; i < dayWidth-1; i++) { int x = i + xStart; if (x % 4 == 0) ... }
                    for (var i = 0; i < this.dayWidth - 1; i++) {
                        // Use absolute chart pixel (dayIdx * dayWidth + i) to match Java's x%4 pattern
                        var px = xStart + i;
                        if ((dayIdx * this.dayWidth + i) % 4 === 0) {
                            g.appendChild(createRect(px, y - this.getTaskHeight()/2 + TASK_BODY_BORDER, 2, 1, {fill: borderColor}));
                            g.appendChild(createRect(px, y + this.getTaskHeight()/2 - TASK_BODY_BORDER - 1, 2, 1, {fill: borderColor}));
                        }
                    }
                }
            }
        }

        /**
         * Mirrors Java: AbstractGanttRenderer.drawId(Task task, int y)
         * Draws a colored box with the task key at firstDayX (= chart coordinate 0).
         * In Java: x1 = firstDayX = 0; x2 = x1 + dayWidth.
         */
        drawId(g, task, y) {
            // int x1 = firstDayX  →  in JS: dayIndexToPixelX(0) = (0 - scrollOffset) * dayWidth
            var x1         = this.dayIndexToPixelX(0);
            var x2         = x1 + this.dayWidth;
            var fillColor  = intToHex(this.theme.ganttTheme.idBgColor,   '#cccccc');
            var textColor  = intToHex(this.theme.ganttTheme.idTextColor,  '#000000');
            // graphics2D.setFont(idFont)  →  Font(SANS_SERIF, PLAIN, 12)
            // graphics2D.fillRect(x1+1, y - taskHeight/2, x2-x1-1, taskHeight)
            g.appendChild(createRect(x1 + 1, y - this.getTaskHeight()/2, x2 - x1 - 1, this.getTaskHeight(), {fill: fillColor}));
            // graphics2D.drawString(task.getKey(), x1+4, y+yShift)
            // MetaData md = TaskUtil.getTaskMetaData(task) — not available in JS, treat as null
            var midY = y; // y is already midY
            var keyText = createText(x1 + 4, midY, task.key || '', {
                fill: textColor,
                'font-size': '12',
                'font-family': 'sans-serif',
                'dominant-baseline': 'middle'
            });
            if (task.name) keyText.appendChild(createSvgElement('title', {}, task.name));
            g.appendChild(keyText);
        }

        /**
         * Mirrors Java: AbstractGanttRenderer.drawManualMarker(Task task, int x1, int y, boolean labelInside, Color textColor)
         * Draws a 1px red vertical bar at the left edge of a manually-scheduled task.
         * Java: graphics2D.setColor(Color.red); fillRect(x1, y - getTaskHeight()/2, 1, getTaskHeight())
         */
        drawManualMarker(g, task, x1, y, labelInside) {
            if (task.manuallyScheduled) {
                // graphics2D.setColor(Color.red);
                // graphics2D.fillRect(x1, y - getTaskHeight()/2, 1, getTaskHeight());
                g.appendChild(createRect(x1, y - this.getTaskHeight()/2, 1, this.getTaskHeight(), {fill: '#ff0000'}));
                // Note: timestamp drawing is commented out in Java source
            }
        }

        /**
         * Mirrors Java: AbstractGanttRenderer.drawMilestoneTask(Task task, int x1, int y, boolean labelInside, Color fillColor, Color textColor, String taskName)
         * Java: int milestoneWidth = getTaskHeight()/2 - TASK_BODY_BORDER  (= 8)
         *       int c = 0
         *       xPoints = {x1+c, x1+c+mW, x1+c, x1+c-mW, x1+c}  → diamond centered at x1
         *       yPoints = {y-mW, y, y+mW, y, y-mW}
         */
        drawMilestoneTask(g, task, x1, y, labelInside, taskName) {
            var milestoneWidth = this.getTaskHeight() / 2 - TASK_BODY_BORDER; // = 8
            var c              = 0;
            // Java: if (task.getTaskMode() == MANUALLY_SCHEDULED) setColor(fillColor) else setColor(Color.gray)
            // In JS, fillColor pre-computed in DTO
            var fillColor   = task.fillColor ? convertSprintColorToRgba(task.fillColor) : '#808080';
            var borderColor = task.borderColor || '#888888';
            var points = [
                (x1 + c)                   + ',' + (y - milestoneWidth),
                (x1 + c + milestoneWidth)  + ',' + y,
                (x1 + c)                   + ',' + (y + milestoneWidth),
                (x1 + c - milestoneWidth)  + ',' + y,
                (x1 + c)                   + ',' + (y - milestoneWidth)
            ].join(' ');
            // graphics2D.fillPolygon(xPoints, yPoints, nPoints)
            // graphics2D.drawPolygon(xPoints, yPoints, nPoints)
            var poly = createSvgElement('polygon', {
                points: points,
                fill: fillColor,
                stroke: borderColor,
                'stroke-width': '1'
            });
            poly.appendChild(createSvgElement('title', {}, this.generateTaskToolTip(task)));
            g.appendChild(poly);
            // graphics2D.setFont(graphFont)  →  Font(SANS_SERIF, PLAIN, 12)
            // graphics2D.setColor(textColor)
            var textColor = task.textColor || intToHex(this.theme.ganttTheme.taskTextColor, '#303030');
            // FontMetrics yShift = ascent - height/2  →  use dominant-baseline:middle
            if (labelInside) {
                // only used for team planner chart — not drawn in GanttRenderer
            } else {
                // graphics2D.drawString(String.format("%s (%s)", taskName, dateTimeString),
                //     x1 + 2 + 8 + c + milestoneWidth/2, y + yShift)
                var labelX = x1 + c + milestoneWidth / 2 + 10; // = x1 + 2 + 8 + c + mW/2
                var dateStr = task.start ? new Date(task.start).toLocaleDateString() : '';
                var label   = (taskName || '') + ' (' + dateStr + ')';
                var lbl = createText(labelX, y, label, {
                    fill: textColor,
                    'font-size': '12',
                    'font-family': 'sans-serif',
                    'dominant-baseline': 'middle'
                });
                lbl.appendChild(createSvgElement('title', {}, this.generateTaskToolTip(task)));
                g.appendChild(lbl);
            }
        }

        /**
         * Mirrors Java: AbstractGanttRenderer.drawRelation(Task sourceTask, int y2, Task targetTask, int y1)
         *
         * T------------| | | C
         *
         * Java:
         *   int x1 = calculateX(targetTask.getFinish(), ...) - dayWidth/2
         *   int x2 = RELATION_CORNER_LENGTH + calculateX(sourceTask.getStart(), ...) - dayWidth/2 - RESOURCE_NAME_TO_TASK_GAP
         */
        drawRelation(g, sourceTask, y2, targetTask, y1) {
            var signum = (int) => (int > 0 ? 1 : int < 0 ? -1 : 0);
            var sign   = signum(y2 - y1);
            var yEnd, yMid;
            if (sign > 0) {
                y2  -= this.getTaskHeight() / 2 - TASK_BODY_BORDER;
                yEnd = y2;
                yMid = y2 - 5;
            } else {
                y2  += this.getTaskHeight() / 2 - TASK_BODY_BORDER;
                yEnd = y2;
                yMid = y2 + 5;
            }
            // int x1 = calculateX(targetTask.getFinish(), ...) - dayWidth/2
            var ax1 = this.calculateX(targetTask.finish, getDayAt8AM(targetTask.finish), SECONDS_PER_DAY);
            // int x2 = RELATION_CORNER_LENGTH + calculateX(sourceTask.getStart(), ...) - dayWidth/2 - RESOURCE_NAME_TO_TASK_GAP
            var ax2 = RELATION_CORNER_LENGTH
                    + this.calculateX(sourceTask.start, getDayAt8AM(sourceTask.start), SECONDS_PER_DAY)
                    - RESOURCE_NAME_TO_TASK_GAP;

            var arrowColor = (sourceTask.critical && targetTask.critical)
                ? intToHex(this.theme.ganttTheme.criticalRelationColor, '#ff0000')
                : intToHex(this.theme.ganttTheme.relationColor, '#3466ed');

            // graphics2D.setStroke(new BasicStroke(RELATION_LINE_STROKE_WIDTH))
            // graphics2D.fillRect(ax1+1, y1, ax2-ax1, 1)  → horizontal line
            g.appendChild(createRect(ax1 + 1, y1, ax2 - ax1, 1, {fill: arrowColor}));
            // graphics2D.fillRect(ax2, y1+1, 1, yMid-y1)  → vertical line
            g.appendChild(createRect(ax2, y1 + 1, 1, yMid - y1, {fill: arrowColor}));

            var d = 5;
            var pts;
            if (y2 > y1) {
                // arrow head down: {ax2-d, yEnd-d+sign}, {ax2+d, yEnd-d+sign}, {ax2, yEnd+sign}
                pts = [(ax2 - d) + ',' + (yEnd - d + sign),
                       (ax2 + d) + ',' + (yEnd - d + sign),
                       ax2       + ',' + (yEnd     + sign)].join(' ');
            } else {
                // arrow head up: {ax2+d, yEnd+d+sign}, {ax2-d, yEnd+d+sign}, {ax2, yEnd+sign}
                pts = [(ax2 + d) + ',' + (yEnd + d + sign),
                       (ax2 - d) + ',' + (yEnd + d + sign),
                       ax2       + ',' + (yEnd     + sign)].join(' ');
            }
            // graphics2D.setColor(theme.ganttTheme.relationColor)
            g.appendChild(createSvgElement('polygon', {points: pts, fill: arrowColor}));
        }

        /**
         * Mirrors Java: AbstractGanttRenderer.drawRibbon(Graphics2D, int y1, int x1, int y2, int delta1, int delta2, Color ribbonColor)
         * Draws a parallelogram (diagonal stripe) used for alien/marker task bodies.
         * Java: xpoints = {x1, x1+delta1, x1+delta1+delta2, x1+delta2}
         *       ypoints = {y2,        y1,              y1,       y2}
         */
        drawRibbon(g, y1, x1, y2, delta1, delta2, ribbonColor) {
            var points = [
                x1                      + ',' + y2,
                (x1 + delta1)           + ',' + y1,
                (x1 + delta1 + delta2)  + ',' + y1,
                (x1 + delta2)           + ',' + y2
            ].join(' ');
            // graphics2D.setColor(ribbonColor); graphics2D.fillPolygon(xpoints, ypoints, xpoints.length)
            g.appendChild(createSvgElement('polygon', {points: points, fill: ribbonColor}));
        }

        /**
         * Mirrors Java: AbstractGanttRenderer.drawStoryBody(Task task, int x1, int x2, int y, Color fillColor, String marker, String toolTip)
         * marker == null → normal inverted-U bracket
         * marker != null → ribbon/diagonal stripe pattern (alien story)
         */
        drawStoryBody(g, task, x1, x2, y, marker) {
            var fillColor = task.fillColor ? convertSprintColorToRgba(task.fillColor)
                                           : intToHex(this.theme.ganttTheme.storyColor, '#444444');
            var tooltip = this.generateTaskToolTip(task);
            // graphics2D.setColor(fillColor)
            if (marker == null) {
                // drawTick(task.getStart(), x1, y, TextAlignment.left)   ← commented out in Java
                // drawTick(task.getFinish(), x2, y, TextAlignment.right) ← commented out in Java
                var y1        = y + TASK_BODY_BORDER;
                var thickness = 2;
                // graphics2D.fillRect(x1, y1 - taskHeight/2, x2-x1+1, thickness) //upper ---
                g.appendChild(createRect(x1, y1 - this.getTaskHeight()/2, x2 - x1 + 1, thickness, {fill: fillColor}));
                // graphics2D.fillRect(x1, y1 - taskHeight/2 + thickness, thickness, taskHeight - BORDER*2 - thickness) //left |
                g.appendChild(createRect(x1, y1 - this.getTaskHeight()/2 + thickness, thickness, this.getTaskHeight() - TASK_BODY_BORDER*2 - thickness, {fill: fillColor}));
                // graphics2D.fillRect(x1+x2-x1-1, ...) = fillRect(x2-1, ...) //right |
                g.appendChild(createRect(x2 - 1, y1 - this.getTaskHeight()/2 + thickness, thickness, this.getTaskHeight() - TASK_BODY_BORDER*2 - thickness, {fill: fillColor}));
                // invisible tooltip shape
                if (x2 - x1 - 1 > 0) {
                    var tooltipRect = createRect(x1 + 1, y1 - this.getTaskHeight()/2, x2 - x1 - 1, this.getTaskHeight() - thickness * 2, {fill: 'none', 'pointer-events': 'all'});
                    tooltipRect.appendChild(createSvgElement('title', {}, tooltip));
                    g.appendChild(tooltipRect);
                }
            } else {
                // marker != null → ribbon pattern (alien story, used in team planner)
                var stY1  = y - this.getTaskHeight()/2 + 1;
                var stY2  = stY1 + this.getTaskHeight() - 2;
                // graphics2D.setClip(x1+1, y-taskHeight/2+2, x2-x1-1, taskHeight-4)
                var clipId = 'sr-' + String(task.id).replace(/-/g, '');
                g.appendChild(createClipPath(clipId, x1 + 1, y - this.getTaskHeight()/2 + 2, x2 - x1 - 1, this.getTaskHeight() - 4));
                var ribbonGroup = createSvgElement('g', {'clip-path': 'url(#' + clipId + ')'});
                // int delta1 = 25; int delta2 = 16; Color ribbonColor = fillColor
                var delta1      = 25;
                var delta2      = 16;
                var ribbonColor = fillColor;
                for (var rx = x1 - delta2; rx < x2; rx += delta2) {
                    this.drawRibbon(ribbonGroup, stY1, rx, stY2, delta1, delta2 - 1, ribbonColor);
                    ribbonColor = (ribbonColor === fillColor) ? '#ffffff' : fillColor;
                }
                g.appendChild(ribbonGroup);
            }
        }

        /**
         * Mirrors Java: AbstractGanttRenderer.drawTask(long gantUniqueId, Task task, boolean drawId, ...)
         * Public entry point. Computes x1/x2 using calculateX (intra-day time offset),
         * retrieves y from the taskHeight map (= _calendarH + rowIndex * (taskHeight+1) + taskHeight/2),
         * then calls the private drawTask overload.
         */
        drawTask(g, gantUniqueId, task, doDrawId, drawRelations, labelInside, alien, marker, conflict, drawOutOfOffice) {
            // if (GanttUtil.isValidTask(task)) — tasks in DTO are already filtered to valid ones
            if (!task.start || !task.finish) return;

            // graphics2D.setStroke(new BasicStroke(FINE_LINE_STROKE_WIDTH))
            var start = task.start;
            var stop  = task.finish;
            // int x1 = calculateX(start, start.truncatedTo(DAYS).withHour(8), SECONDS_PER_DAY) - dayWidth/2
            var x1    = this.calculateX(start, getDayAt8AM(start), SECONDS_PER_DAY);
            // int x2 = calculateX(stop, stop.truncatedTo(DAYS).withHour(8), SECONDS_PER_DAY) - dayWidth/2
            var x2    = this.calculateX(stop, getDayAt8AM(stop), SECONDS_PER_DAY);
            // Integer lane = taskHeight.get(gantUniqueId + "-" + task.getId())
            // int y = lane + getTaskHeight() / 2
            var y     = this._calendarH + task.rowIndex * (this.getTaskHeight() + 1) + GANTT_TASK_PRI_SPACE + this.getTaskHeight() / 2;

            // drawOutOfOffice handling: drawOutOfOffice(task, y) — commented out in Java

            // private drawTask overload
            this._drawTask(g, task, x1, x2, y, labelInside, alien, marker, conflict);

            if (doDrawId) {
                this.drawId(g, task, y);
            }
            if (drawRelations) {
                // draw relations for this task's predecessors
                var self     = this;
                var taskById = this._taskById || {};
                if (task.predecessors && task.predecessors.length) {
                    task.predecessors.forEach(function (rel) {
                        var targetTask = taskById[String(rel.predecessorId)];
                        if (!targetTask) return;
                        // if (relation.isVisible())
                        if (rel.visible) {
                            // int y1 = taskHeight.get(...targetTask) + taskHeight/2
                            var y1 = self._calendarH + targetTask.rowIndex * (self.getTaskHeight() + 1) + GANTT_TASK_PRI_SPACE + self.getTaskHeight() / 2;
                            // int y2 = taskHeight.get(...sourceTask) + taskHeight/2
                            var y2 = y;
                            self.drawRelation(g, task, y2, targetTask, y1);
                        }
                    });
                }
            }
        }

        /**
         * Mirrors Java: AbstractGanttRenderer.drawTask(Task task, int x1, int x2, int y, boolean labelInside, boolean alien, String marker, List<Conflict> conflict)
         * Private dispatcher: determines task type, calls the appropriate body-drawing method,
         * then draws labels.
         * Colors are pre-computed in DTO by GanttChartService (mirrors AbstractGanttRenderer.drawTask color logic).
         */
        _drawTask(g, task, x1, x2, y, labelInside, alien, marker, conflict) {
            // Color fillColor / textColor / resourceName / units — pre-computed in DTO
            var fillColor   = task.fillColor;
            var textColor   = task.textColor   || intToHex(this.theme.ganttTheme.taskTextColor, '#303030');
            var taskName    = task.name        || '';
            var resourceName = task.assignedUserName || null;
            var resourceUtilization = task.assignedUserAvailability || null;

            if (task.milestone && !task.story) {
                // ---Milestone, but not a story
                this.drawMilestoneTask(g, task, x1, y, labelInside, taskName);
            } else {
                if (task.story) {
                    // ---Story (has children)
                    this.drawStoryBody(g, task, x1, x2, y, marker);
                    // graphics2D.setFont(storyFont)  →  Font(SANS_SERIF, BOLD, 12)
                    // graphics2D.setColor(textColor)
                    // FontMetrics yShift = ascent - height/2
                    if (labelInside) {
                        // only used for team planner chart
                    } else {
                        // graphics2D.drawString(taskName, x2 + 2 + 8, y + yShift)
                        var storyLabelX = x2 + 2 + 8;
                        if (storyLabelX < this.containerWidth + 40) {
                            var storyLabel = createText(storyLabelX, y, taskName, {
                                fill: textColor,
                                'font-size': '12',
                                'font-family': 'sans-serif',
                                'font-weight': 'bold',
                                'dominant-baseline': 'middle'
                            });
                            storyLabel.appendChild(createSvgElement('title', {}, this.generateTaskToolTip(task)));
                            g.appendChild(storyLabel);
                        }
                    }
                } else {
                    // ---Regular task
                    var progress = task.progress || 0.0;
                    this.drawTaskBody(g, task, x1, x2, y, alien, progress);
                    this.drawConflictMarker(g, y, conflict);
                    this.drawCriticalMarker(g, task, x1, x2, y);
                    this.drawManualMarker(g, task, x1, y, labelInside);

                    // task text
                    if (labelInside) {
                        // team planner chart — not drawn here
                    } else {
                        // progress text (8pt, centered inside bar)
                        if (progress > 0) {
                            // graphics2D.setFont(taskProgressFont)  →  8pt
                            var text       = Math.round(progress * 100) + '%';
                            var barWidth   = x2 - x1;
                            // Approximate: 5px per char for 8pt font
                            var textWidth  = text.length * 5;
                            // if (width < x2 - x1)
                            if (textWidth < barWidth) {
                                // graphics2D.setClip(x1+1, y-taskHeight/2+GAP, x2-x1-3, taskHeight-6)
                                var clipId2 = 'pt-' + String(task.id).replace(/-/g, '');
                                g.appendChild(createClipPath(clipId2, x1 + 1, y - this.getTaskHeight()/2 + RESOURCE_NAME_TO_TASK_GAP, x2 - x1 - 3, this.getTaskHeight() - 6));
                                // graphics2D.drawString(text, x1 + (x2-x1)/2 + 1 - width/2, y + (ascent-2)/2)
                                var progressText = createText(x1 + barWidth/2, y, text, {
                                    fill: '#ffffff',
                                    stroke: '#000000',
                                    'stroke-width': '0.4',
                                    'font-size': '8',
                                    'font-family': 'sans-serif',
                                    'text-anchor': 'middle',
                                    'dominant-baseline': 'middle',
                                    'clip-path': 'url(#' + clipId2 + ')'
                                });
                                g.appendChild(progressText);
                            }
                        }

                        // Task name: graphics2D.drawString(key + " " + taskName, x2 + TASK_NAME_TO_TASK_GAP, y + yShift)
                        {
                            var labelRight = x2 + TASK_NAME_TO_TASK_GAP;
                            if (labelRight < this.containerWidth + 40) {
                                var clipId3 = 'tn-' + String(task.id).replace(/-/g, '');
                                var clipW3  = Math.max(0, this.containerWidth - labelRight);
                                if (clipW3 > 8) {
                                    g.appendChild(createClipPath(clipId3, labelRight, y - this.getTaskHeight(), clipW3, this.getTaskHeight() * 2));
                                    var nameLabel = createText(labelRight, y, (task.key ? task.key + ' ' : '') + taskName, {
                                        fill: textColor,
                                        'font-size': '12',
                                        'font-family': 'sans-serif',
                                        'dominant-baseline': 'middle',
                                        'clip-path': 'url(#' + clipId3 + ')'
                                    });
                                    nameLabel.appendChild(createSvgElement('title', {}, this.generateTaskToolTip(task)));
                                    g.appendChild(nameLabel);
                                }
                            }
                        }

                        // Resource name: graphics2D.drawString(resourceName, resourceNameX, y + yShift)
                        if (resourceName != null) {
                            // int resourceNameWidth = fm.stringWidth(resourceName)  → approx 7px/char
                            var resourceNameWidth = resourceName.length * 7;
                            // int resourceNameX = x1 - resourceNameWidth - RESOURCE_NAME_TO_TASK_GAP
                            var resourceNameX = x1 - resourceNameWidth - RESOURCE_NAME_TO_TASK_GAP;
                            if (resourceNameX > -100) {
                                var clipId4 = 'rn-' + String(task.id).replace(/-/g, '');
                                var clipW4  = Math.min(120, x1 > 0 ? x1 : 0);
                                if (clipW4 > 8) {
                                    g.appendChild(createClipPath(clipId4, Math.max(0, resourceNameX), y - this.getTaskHeight(), clipW4, this.getTaskHeight() * 2));
                                    var rLabel = createText(resourceNameX + resourceNameWidth, y, resourceName, {
                                        fill: textColor,
                                        'font-size': '12',
                                        'font-family': 'sans-serif',
                                        'text-anchor': 'end',
                                        'dominant-baseline': 'middle',
                                        'clip-path': 'url(#' + clipId4 + ')'
                                    });
                                    rLabel.appendChild(createSvgElement('title', {}, this.generateTaskNameToolTip(resourceName, resourceUtilization, task.assignedUserCountry, task.assignedUserState)));
                                    g.appendChild(rLabel);
                                }
                            }
                        }
                    }
                }
            }
        }

        /**
         * Mirrors Java: AbstractGanttRenderer.drawTaskBody(Task task, int x1, int x2, int y, Color fillColor, boolean alien, double progress, String toolTip)
         * Draws filled task body day by day.
         * Working days → full fillColor opacity.
         * Non-working days → fillColor with taskWeekEndTransparency alpha.
         * After body segments draws the progress bar.
         * alien=true → ribbon diagonal stripe pattern instead of solid fill.
         */
        drawTaskBody(g, task, x1, x2, y, alien, progress) {
            var fillColor    = task.fillColor;
            var originalColor = fillColor;
            var tooltip      = this.generateTaskToolTip(task);

            if (!alien) {
                // int y1 = y - taskHeight/2 + TASK_BODY_BORDER
                var y1 = y - this.getTaskHeight()/2 + TASK_BODY_BORDER;
                // int h  = taskHeight - TASK_BODY_BORDER*2
                var h  = this.getTaskHeight() - TASK_BODY_BORDER * 2;
                // if (x2 - x1 - 1 - 1 > 0)  → skip very thin tasks
                if (x2 - x1 - 2 > 0) {
                    // ProjectCalendar pc — in JS: use task.calendarExceptions
                    // int days = Duration.between(start.truncated(DAYS), finish.truncated(DAYS)).toDays()
                    var startDayIdx  = calculateDayIndex(task.start,  this.chartStart);
                    var finishDayIdx = calculateDayIndex(task.finish, this.chartStart);
                    var days         = finishDayIdx - startDayIdx;

                    for (var day = 0; day <= days; day++) {
                        // LocalDateTime currentDay = start.truncated(DAYS).plusDays(day)
                        var dayIdx  = startDayIdx + day;
                        var dayDate = new Date(this.chartStart.getTime() + dayIdx * MS);
                        // Shape s;
                        var segX, segW;

                        if (isWorkingDay(dayDate, task.calendarExceptions)) {
                            // graphics2D.setColor(fillColor)
                            var fill = convertSprintColorToRgba(fillColor);
                            if (days === 0) {
                                // this is the left and right end
                                // s = new RectangleWithToolTip(x1, y1, x2-x1, h, toolTip)
                                segX = x1; segW = x2 - x1;
                            } else if (day === 0) {
                                // this is the left end
                                // xFinish = calculateX(currentDay@8AM+SECONDS_PER_DAY, ...) - dayWidth/2
                                //         = dayIndexToPixelX(dayIdx) + dayWidth
                                var xFinish = this.dayIndexToPixelX(dayIdx) + this.dayWidth;
                                // s = new RectangleWithToolTip(x1, y1, xFinish-x1, h, toolTip)
                                segX = x1; segW = xFinish - x1;
                            } else if (day === days) {
                                // this is the right end
                                // xStart = calculateX(currentDay@8AM, ...) - dayWidth/2
                                //        = dayIndexToPixelX(dayIdx)
                                var xStart2 = this.dayIndexToPixelX(dayIdx);
                                // s = new RectangleWithToolTip(xStart, y1, x2-xStart+1, h, toolTip)
                                segX = xStart2; segW = x2 - xStart2 + 1;
                            } else {
                                // this is the middle
                                // xStart = dayIndexToPixelX(dayIdx)
                                var xStart3 = this.dayIndexToPixelX(dayIdx);
                                // s = new RectangleWithToolTip(xStart, y1, dayWidth, h, toolTip)
                                segX = xStart3; segW = this.dayWidth;
                            }
                            var rect = createRect(segX, y1, segW, h, {fill: fill});
                            rect.appendChild(createSvgElement('title', {}, tooltip));
                            g.appendChild(rect);
                        } else {
                            // non-working day: fillColor with taskWeekEndTransparency
                            // graphics2D.setColor(new Color(r, g, b, taskWeekEndTransparency))
                            var weekendFill = hexToRgbaWithAlpha(fillColor, this.theme.ganttTheme.taskWeekEndTransparency);
                            var xStart4 = this.dayIndexToPixelX(dayIdx);
                            // s = new RectangleWithToolTip(xStart, y1, dayWidth, h, toolTip)
                            var rectW = createRect(xStart4, y1, this.dayWidth, h, {fill: weekendFill});
                            rectW.appendChild(createSvgElement('title', {}, tooltip));
                            g.appendChild(rectW);
                        }
                    } // end for day

                    // progress bar
                    // if (progress > 0.0 && numberOfLinesPerTask == 1)
                    if (progress > 0.0) {
                        // Color color = lightenColor(userColor, 0.6f) — pre-computed in DTO as progressColor
                        var progressFill = task.progressColor
                            ? convertSprintColorToRgba(task.progressColor)
                            : hexToRgbaWithAlpha(fillColor, 200);
                        // graphics2D.fillRect(x1+1, y1+2, (int)((x2-x1)*progress-1), h-4)
                        var progressW = Math.floor((x2 - x1) * progress - 1);
                        if (progressW > 0) {
                            var pRect = createRect(x1 + 1, y1 + 2, progressW, h - 4, {fill: progressFill});
                            pRect.appendChild(createSvgElement('title', {}, tooltip));
                            g.appendChild(pRect);
                            // if (progress < 1.0) fillRect(x1+(x2-x1)*progress-1, y-h/2+2, 1, h-4)
                            if (progress < 1.0) {
                                g.appendChild(createRect(x1 + progressW, y - this.getTaskHeight()/2 + 2, 1, this.getTaskHeight() - 4, {fill: '#000000'}));
                            }
                        }
                    }
                }
            } else {
                // alien=true → ribbon diagonal stripe pattern
                var aY1   = y - this.getTaskHeight()/2 + 1;
                var aY2   = aY1 + this.getTaskHeight() - 1;
                // graphics2D.setClip(x1, y-taskHeight/2+2, x2-x1-1, taskHeight-4)
                var clipId5 = 'ta-' + String(task.id).replace(/-/g, '');
                g.appendChild(createClipPath(clipId5, x1, y - this.getTaskHeight()/2 + 2, x2 - x1 - 1, this.getTaskHeight() - 4));
                var alienGroup = createSvgElement('g', {'clip-path': 'url(#' + clipId5 + ')'});
                // int delta1 = 25; int delta2 = 16; Color ribbonColor = originalColor
                var adelta1      = 25;
                var adelta2      = 16;
                var aRibbonColor = originalColor ? convertSprintColorToRgba(originalColor) : '#aaaaaa';
                var aWhite       = '#ffffff';
                var aCurrent     = aRibbonColor;
                for (var ax = x1 - adelta2; ax < x2; ax += adelta2) {
                    this.drawRibbon(alienGroup, aY1, ax, aY2, adelta1, adelta2 - 1, aCurrent);
                    aCurrent = (aCurrent === aRibbonColor) ? aWhite : aRibbonColor;
                }
                g.appendChild(alienGroup);
            }
        }

        /**
         * Mirrors Java: AbstractGanttRenderer.drawTick(LocalDateTime time, int x, int y, TextAlignment alignment)
         * Draws a tick mark with a time label at a task start/end edge.
         * NOTE: All callers in Java have this commented out — stub only.
         */
        drawTick(g, time, x, y, alignment) {
            // drawTick(task.getStart(), x1, y, TextAlignment.left)  ← commented out in Java
            // drawTick(task.getFinish(), x2, y, TextAlignment.right) ← commented out in Java
        }

        /**
         * Mirrors Java: AbstractGanttRenderer.generateTaskNameToolTop(String resourceName, ...)
         * Returns plain-text tooltip for a resource name label.
         */
        generateTaskNameToolTip(resourceName, resourceUtilization, country, state) {
            var tip = (resourceName || '');
            if (resourceUtilization) tip += '\nAvailability ' + resourceUtilization;
            if (country)             tip += '\nCountry '      + country;
            if (state)               tip += '\nState '        + state;
            return tip;
        }

        /**
         * Mirrors Java: AbstractGanttRenderer.generateTaskToolTip(Task task, ...)
         * Returns plain-text tooltip for a task body.
         */
        generateTaskToolTip(task) {
            var s = task.name || '';
            if (task.key)              s += '\nKey: '      + task.key;
            if (task.start)            s += '\nStart: '    + new Date(task.start).toLocaleDateString();
            if (task.finish)           s += '\nFinish: '   + new Date(task.finish).toLocaleDateString();
            if (task.assignedUserName) s += '\nResource: ' + task.assignedUserName;
            if (task.assignedUserAvailability) s += '\nAvailability: ' + task.assignedUserAvailability;
            if (task.progress > 0)     s += '\nProgress: ' + Math.round(task.progress * 100) + '%';
            return s;
        }
    }

    // ── GanttRenderer ─────────────────────────────────────────────────────────
    // Mirrors Java: GanttRenderer extends AbstractGanttRenderer

    class GanttRenderer extends AbstractGanttRenderer {
        /**
         * @param {Object} data   GanttChartDto JSON
         * @param {Theme}  theme  Theme instance
         */
        constructor(data, theme) {
            super();
            this.theme         = theme;
            this.tasks         = data.tasks      || [];
            this.milestones    = data.milestones || [];
            this.chartStart    = getUtcDayMidnight(new Date(data.meta.chartStart));
            this.totalDays     = calculateDayCount(
                getUtcDayMidnight(new Date(data.meta.chartStart)),
                getUtcDayMidnight(new Date(data.meta.chartEnd))
            );
            this.currentDate   = data.meta.now
                ? getUtcDayMidnight(new Date(data.meta.now))
                : getUtcDayMidnight(new Date());
            this.calendarXAxes = new window.CalendarXAxes(theme);

            // Build taskById map for relation lookup in drawTask (drawRelations=true)
            // Mirrors Java: task.getSprint().getTaskById(relation.getPredecessorId())
            this._taskById = {};
            for (var ti = 0; ti < this.tasks.length; ti++) {
                this._taskById[String(this.tasks[ti].id)] = this.tasks[ti];
            }
        }

        /**
         * Mirrors Java: GanttRenderer.calculateDayWidth()
         * super.calculateDayWidth() then sets dayWidth = 20.
         */
        calculateDayWidth() {
            // super.calculateDayWidth() — sets dayWidth based on chartWidth/days
            // calendarXAxes.dayOfWeek.setWidth(20)
            this.dayWidth = DEFAULT_DW; // = 20
        }

        /**
         * Overrides AbstractRenderer.drawDayBars.
         * Mirrors Java: GanttRenderer.drawDayBars(LocalDate currentDay)
         *
         * For each task row in this calendar column:
         *   1. Grid:       top horizontal line + left vertical line  (ganttTheme.gridColor)
         *   2. Background: getGanttDayStripeColor (per task's calendar)
         *   3. Letter:     exception letter in NoneWorkingDayFont (22pt bold) when dayWidth ≥ 14
         *
         * Java:
         *   int x  = calculateDayX(currentDay)               → center of day column
         *   int x1 = x - (dayWidth/2 - 1)                    → one pixel from left edge
         *   fillRect(x1-1, y1-1, dayWidth, 1)                → top --   at left edge
         *   fillRect(x1-1, y1,   1,       taskHeight)         → left |  at left edge
         *   fill(new Rectangle(x1, y1, dayWidth-1, taskHeight)) → background
         */
        drawDayBars(g, dayDate, calendarH) {
            var dayIdx    = calculateDayIndex(dayDate, this.chartStart);
            // calculateDayX(currentDay) = dayWidth/2 + dayIdx*dayWidth  (in Java coords)
            // → in viewport: dayIndexToPixelX(dayIdx) + dayWidth/2
            // → x1 = x - (dayWidth/2 - 1) = dayIndexToPixelX(dayIdx) + dayWidth/2 - dayWidth/2 + 1 = dayIndexToPixelX(dayIdx) + 1
            var dayLeft   = this.dayIndexToPixelX(dayIdx); // left edge of column
            // x1-1 = dayLeft  (from Java: fillRect(x1-1, ...) where x1 = dayLeft+1)
            var gridColor = intToHex(this.theme.ganttTheme.gridColor, '#e4e8f3');
            var self      = this;

            for (var ti = 0; ti < this.tasks.length; ti++) {
                var task = this.tasks[ti];
                var rowY = calendarH + task.rowIndex * (self.getTaskHeight() + 1) + GANTT_TASK_PRI_SPACE;

                // grid: top -- and left |
                // fillRect(x1-1, y1-1, dayWidth, 1)  → at dayLeft, rowY-1, width=dayWidth, height=1
                g.appendChild(createRect(dayLeft,     rowY - 1, self.dayWidth, 1,               {fill: gridColor})); // top --
                // fillRect(x1-1, y1, 1, taskHeight)  → at dayLeft, rowY, width=1, height=taskHeight
                g.appendChild(createRect(dayLeft,     rowY,     1,            self.getTaskHeight(), {fill: gridColor})); // left |

                // background: fill(Rectangle(x1, y1, dayWidth-1, taskHeight))
                // x1 = dayLeft+1, width = dayWidth-1
                var bgColor = self.getGanttDayStripeColor(task, dayDate);
                g.appendChild(createRect(dayLeft + 1, rowY, self.dayWidth - 1, self.getTaskHeight(), {fill: bgColor}));

                // exception letter (NoneWorkingDayFont: Font(SANS_SERIF, BOLD, 22))
                // graphics2D.setFont(NoneWorkingDayFont)
                // int xShift = fm.stringWidth(letter) / 2
                // graphics2D.drawString(letter, x - xShift, y + yShift, tooltip)
                var exception = getCalendarException(dayDate, task.calendarExceptions);
                if (exception && exception.letter && self.dayWidth >= 14) {
                    // x = calculateDayX(currentDay) in Java → center of column in viewport
                    var cx     = dayLeft + self.dayWidth / 2;
                    var letter = createText(cx, rowY + self.getTaskHeight() / 2, exception.letter, {
                        fill: intToHex(self.theme.ganttTheme.outOfOfficeColor, '#ffffff'),
                        'font-size': String(NONE_WORKING_DAY_FONT_SIZE),
                        'font-family': 'sans-serif',
                        'font-weight': 'bold',
                        'text-anchor': 'middle',
                        'dominant-baseline': 'middle'
                    });
                    letter.appendChild(createSvgElement('title', {}, exception.type || 'Off-day'));
                    g.appendChild(letter);
                }
            }
        }

        /**
         * Mirrors Java: GanttRenderer.drawGanttChart() and drawGanttChart(int yOffset)
         * Iterates all (valid) tasks and calls drawTask for each.
         * Java: drawTask(0, task, drawId=true, drawRelations=true, labelInside=false, alien=false, marker=null, conflict=null, drawOutOfOffice=true)
         */
        drawGanttChart(g) {
            // for (Task task : sprint.getTasks()) { if (GanttUtil.isValidTask(task)) { ... } }
            // Tasks in DTO are already filtered to valid ones
            for (var ti = 0; ti < this.tasks.length; ti++) {
                // drawTask(0, task, true, true, false, false, null, null, true)
                this.drawTask(g, 0, this.tasks[ti], true, true, false, false, null, null, true);
            }
        }

        /**
         * Mirrors Java: GanttRenderer.draw(ExtendedGraphics2D graphics2D, int x, int y)
         *
         * Java:
         *   initPosition(firstDayX + x, y)
         *   calculateTaskHeightMap(y + calendarXAxes.getHeight())
         *   drawCalendar()    → calendar header rows + drawDayBars per day
         *   drawMilestones()  → milestone row (handled by calendarXAxes.draw)
         *   drawGanttChart()  → task bodies, borders, labels, relations
         */
        draw(svg, x, y) {
            // calendarXAxes.getHeight() → pixel height of calendar header rows
            var calendarH = this.calendarXAxes.getHeight(this.dayWidth, this.milestones.length > 0);
            // calculateChartHeight() = calendarH + GANTT_TASK_PRI_SPACE + tasks*(taskHeight+1) + GANTT_TASK_POST_SPACE
            var taskAreaH = GANTT_TASK_PRI_SPACE + this.tasks.length * (this.getTaskHeight() + 1) + GANTT_TASK_POST_SPACE;
            var totalH    = calendarH + taskAreaH;
            // calculateTaskHeightMap(y + calendarXAxes.getHeight())
            // In JS: stored as this._calendarH for use in drawTask
            this._calendarH = y + calendarH;

            // ── drawCalendar() ──────────────────────────────────────────────
            // CalendarXAxes.drawCalendar() → draws header rows then calls renderer.drawDayBars(day) for each day
            var gDayBars = createSvgElement('g', {'class': 'day-bars'});
            var firstDay = Math.max(0, Math.floor(this.scrollOffset) - 1);
            var lastDay  = Math.min(this.totalDays - 1, firstDay + Math.ceil(this.containerWidth / this.dayWidth) + 2);
            for (var d = firstDay; d <= lastDay; d++) {
                var dayDate = new Date(this.chartStart.getTime() + d * MS);
                this.drawDayBars(gDayBars, dayDate, this._calendarH);
            }
            svg.appendChild(gDayBars);

            // Calendar header rows (year, month, week, dom, dow)
            // drawMilestones() is handled by passing milestones to calendarXAxes.draw
            this.calendarXAxes.draw(
                svg, this.chartStart, this.totalDays,
                this.dayWidth, this.scrollOffset,
                this.containerWidth, this.milestones
            );

            // ── drawGanttChart() ────────────────────────────────────────────
            var gTasks = createSvgElement('g', {'class': 'tasks'});
            this.drawGanttChart(gTasks);
            svg.appendChild(gTasks);

            // Now-line (mirrors Java milestone "N" vertical red line)
            svg.appendChild(this.renderNowLine(y + totalH));
        }

        /** Draws a vertical red "now" line at the current date. */
        renderNowLine(totalHeight) {
            var g              = createSvgElement('g', {'class': 'now-line'});
            var containerWidth = this.containerWidth;
            var nowIdx         = calculateDayIndex(this.currentDate, this.chartStart);
            var xPos           = this.dayIndexToPixelX(nowIdx) + this.dayWidth / 2;
            if (xPos < 0 || xPos > containerWidth) return g;
            g.appendChild(createLine(xPos, 0, xPos, totalHeight, {stroke: '#cc0000', 'stroke-width': '2'}));
            return g;
        }
    }

    // ── GanttChart ─────────────────────────────────────────────────────────────
    // Mirrors Java: GanttChart extends AbstractChart

    class GanttChart extends window.AbstractChart {
        /**
         * @param {Object} data   GanttChartDto JSON
         * @param {Theme}  theme  Theme instance
         */
        constructor(data, theme) {
            super('Gantt Chart', data.meta.sprintName || '', '', '', 'gantt-chart', theme);
            /** Mirrors Java: GanttChart → getRenderers().add(new GanttRenderer(dao)) */
            this.addRenderer(new GanttRenderer(data, theme));
        }

        /**
         * Updates renderer scroll/zoom state and recomputes chart dimensions.
         * Called before each render frame.
         */
        updateViewState(dayWidth, scrollOffset, containerWidth) {
            var renderer            = this.renderers[0];
            renderer.dayWidth       = dayWidth;
            renderer.scrollOffset   = scrollOffset;
            renderer.containerWidth = containerWidth;

            var calendarH = renderer.calendarXAxes.getHeight(dayWidth, renderer.milestones.length > 0);
            var taskAreaH = GANTT_TASK_PRI_SPACE + renderer.tasks.length * (renderer.getTaskHeight() + 1) + GANTT_TASK_POST_SPACE;
            var contentH  = calendarH + taskAreaH;

            this.setChartWidth(containerWidth);
            this.setChartHeight(contentH + this.captionElement.height + this.footerElement.height - 1);
            this.footerElement.y = contentH + this.captionElement.height;
        }

        /** Mirrors Java: GanttChart.createReport() */
        createReport(svg) {
            this.renderers[0].draw(svg, 0, this.captionElement.height);
        }
    }

    // ── Mount / createChart function ──────────────────────────────────────────

    var currentGanttChartInstance = null;

    function createChart(container, data, options) {
        options         = options || {};
        var containerId = options.containerId || container.id || 'chart';

        var theme    = new window.Theme(data.meta.theme);
        var chart    = new GanttChart(data, theme);
        var renderer = chart.renderers[0];

        var dayWidth     = DEFAULT_DW;
        var scrollOffset = 0;

        function getContainerWidth() { return Math.max(200, container.clientWidth || 800); }

        function constrainScrollOffset() {
            scrollOffset = Math.max(0, Math.min(
                Math.max(0, renderer.totalDays - getContainerWidth() / dayWidth),
                scrollOffset
            ));
        }

        var saved = loadViewState(containerId);
        if (saved) {
            dayWidth     = Math.min(MAX_DW, Math.max(MIN_DW, saved.dayWidth));
            scrollOffset = saved.scrollOffset;
            constrainScrollOffset();
        } else {
            var todayIdx    = calculateDayIndex(renderer.currentDate, renderer.chartStart);
            var visibleDays = getContainerWidth() / dayWidth;
            scrollOffset    = Math.max(0, Math.min(renderer.totalDays - visibleDays, todayIdx - visibleDays * 0.2));
        }

        var saveTimerId = null;
        function scheduleSave() {
            if (saveTimerId) clearTimeout(saveTimerId);
            saveTimerId = setTimeout(function () {
                saveViewState(containerId, dayWidth, scrollOffset);
            }, 250);
        }

        var animationFrameId = null;

        function redrawChart() {
            var containerWidth = getContainerWidth();
            chart.updateViewState(dayWidth, scrollOffset, containerWidth);
            chart.render(container);
        }

        function scheduleRender() {
            if (animationFrameId) cancelAnimationFrame(animationFrameId);
            animationFrameId = requestAnimationFrame(redrawChart);
        }

        function handleWheelEvent(event) {
            event.preventDefault();
            if (event.deltaX !== 0) {
                scrollOffset += event.deltaX / dayWidth;
            } else {
                var rect     = container.getBoundingClientRect();
                var mouseX   = (event.clientX != null) ? (event.clientX - rect.left) : (getContainerWidth() / 2);
                var dayUnder = scrollOffset + mouseX / dayWidth;
                var factor   = event.deltaY < 0 ? ZOOM_STEP : 1 / ZOOM_STEP;
                dayWidth     = Math.max(MIN_DW, Math.min(MAX_DW, dayWidth * factor));
                scrollOffset = dayUnder - mouseX / dayWidth;
            }
            constrainScrollOffset();
            scheduleRender();
            scheduleSave();
        }

        var dragState = null;
        function handlePointerDown(event) {
            if (event.button !== 0) return;
            dragState = {startX: event.clientX, startOffset: scrollOffset};
            container.setPointerCapture(event.pointerId);
            container.style.cursor = 'grabbing';
            event.preventDefault();
        }
        function handlePointerMove(event) {
            if (!dragState) return;
            scrollOffset = dragState.startOffset - (event.clientX - dragState.startX) / dayWidth;
            constrainScrollOffset();
            scheduleRender();
        }
        function handlePointerUp() {
            if (dragState) { dragState = null; scheduleSave(); }
            container.style.cursor = 'grab';
        }

        var resizeObserver = null;
        if (typeof ResizeObserver !== 'undefined') {
            resizeObserver = new ResizeObserver(scheduleRender);
            resizeObserver.observe(container);
        }

        function cleanupChart() {
            container.removeEventListener('wheel', handleWheelEvent);
            container.removeEventListener('pointerdown', handlePointerDown);
            container.removeEventListener('pointermove', handlePointerMove);
            container.removeEventListener('pointerup', handlePointerUp);
            container.removeEventListener('pointercancel', handlePointerUp);
            if (resizeObserver) resizeObserver.disconnect();
            if (animationFrameId) cancelAnimationFrame(animationFrameId);
            if (saveTimerId) clearTimeout(saveTimerId);
            container.innerHTML = '';
        }

        container.style.cursor = 'grab';
        container.addEventListener('wheel', handleWheelEvent, {passive: false});
        container.addEventListener('pointerdown', handlePointerDown, {passive: false});
        container.addEventListener('pointermove', handlePointerMove, {passive: true});
        container.addEventListener('pointerup', handlePointerUp);
        container.addEventListener('pointercancel', handlePointerUp);

        redrawChart();
        return {render: redrawChart, schedule: scheduleRender, destroy: cleanupChart};
    }

    // ── Public mount API ───────────────────────────────────────────────────────

    function mountGanttChart(containerId, injectedData) {
        var elementId        = containerId || 'gantt-chart-container';
        var containerElement = document.getElementById(elementId);
        if (!containerElement) return;

        if (currentGanttChartInstance && typeof currentGanttChartInstance.destroy === 'function') {
            currentGanttChartInstance.destroy();
            currentGanttChartInstance = null;
        }

        if (injectedData) {
            currentGanttChartInstance = createChart(containerElement, injectedData, {containerId: elementId});
        } else {
            containerElement.innerHTML = '<div style="padding:16px;color:red;font-family:sans-serif;">No Gantt chart data provided.</div>';
        }
    }

    // ── Exports ────────────────────────────────────────────────────────────────

    window.mountGanttChart       = mountGanttChart;
    window.createGanttChart      = createChart;
    window.GanttRenderer         = GanttRenderer;
    window.GanttChart            = GanttChart;
    window.AbstractGanttRenderer = AbstractGanttRenderer;
})();
