// gantt-chart.js
// Client-side interactive Gantt chart renderer for Kassandra.
// Depends on: svg-utils.js, color-utils.js, date-utils.js, calendar-x-axes.js, theme-color-constants.js
//
// Interaction:
//   mouse wheel      → zoom (change dayWidth, keep day-under-cursor stable)
//   click + drag     → pan left/right
//
// View state (dayWidth + scrollOffset) is persisted in localStorage using the key
//   kassandra.chart.<chartName>.view
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0
(function () {
    'use strict';

    // Destructure utilities from global scope
    const {createSvgElement, createRect, createText, createLine, createClipPath} = window.SvgUtils;
    const {getThemeColor, convertSprintColorToRgba} = window.ColorUtils;
    const {MS, getUtcDayMidnight, calculateDayIndex, calculateDayCount} = window.DateUtils;
    const colorKeys = window.ThemeColorKeys;

    // ── Layout constants (mirror Java AbstractGanttRenderer / GanttRenderer) ─
    const TASK_H = 18;         // Java: LINE_HEIGHT=18, numberOfLinesPerTask=1
    const ROW_H = TASK_H + 1; // 1px gap between rows
    const TASK_BODY_BORDER = 1;
    const RESOURCE_NAME_TO_TASK_GAP = 8;  // Java: RESOURCE_NAME_TO_TASK_GAP=3 + TASK_NAME_TO_TASK_GAP=13
    const TASK_NAME_TO_TASK_GAP = 13;
    const RELATION_CORNER_LENGTH = 14;
    const MILESTONE_HALF = TASK_H / 2 - TASK_BODY_BORDER; // diamond half-size

    // ── Zoom / scroll config ──────────────────────────────────────────────────
    const DEFAULT_DW = 20;     // matches Java CalendarXAxes.dayOfWeek.width = 20
    const MIN_DW = 2;
    const MAX_DW = 80;
    const ZOOM_STEP = 1.25;

    // ── Day-bars visibility threshold ─────────────────────────────────────────
    const MIN_DAY_BARS = 4;   // draw per-day stripes only when dayWidth >= this

    // ── localStorage helpers ─────────────────────────────────────────────────

    function generateViewStateKey(containerId) {
        const chartName = (containerId || 'chart').replace(/-container$/, '');
        return 'kassandra.chart.' + chartName + '.view';
    }

    function loadChartViewState(containerId) {
        try {
            const raw = localStorage.getItem(generateViewStateKey(containerId));
            if (raw) {
                const state = JSON.parse(raw);
                if (typeof state.dayWidth === 'number' && typeof state.scrollOffset === 'number') return state;
            }
        } catch (_) { /* storage unavailable */
        }
        return null;
    }

    function saveChartViewState(containerId, dayWidth, scrollOffset) {
        try {
            localStorage.setItem(generateViewStateKey(containerId), JSON.stringify({dayWidth, scrollOffset}));
        } catch (_) { /* quota exceeded */
        }
    }

    // ── Calendar exception helpers ────────────────────────────────────────────

    /**
     * Parses a YYYY-MM-DD string to a UTC midnight Date.
     * @param {string} dateStr
     * @returns {Date}
     */
    function parseLocalDate(dateStr) {
        if (!dateStr) return null;
        // LocalDate arrives as "YYYY-MM-DD" (no time component)
        const parts = dateStr.split('-');
        return new Date(Date.UTC(+parts[0], +parts[1] - 1, +parts[2]));
    }

    /**
     * Tests whether a UTC midnight Date is covered by any of the task's calendar exceptions.
     * Weekends are NOT passed here; use getUTCDay() === 0 || 6 for those.
     * @param {Date}   date
     * @param {Array}  exceptions  Array of {from, to, type, letter}
     * @returns {Object|null}  The first matching exception, or null
     */
    function getCalendarException(date, exceptions) {
        if (!exceptions || !exceptions.length) return null;
        for (let i = 0; i < exceptions.length; i++) {
            const ex = exceptions[i];
            const from = parseLocalDate(ex.from);
            const to = parseLocalDate(ex.to);
            if (from && to && date >= from && date <= to) return ex;
        }
        return null;
    }

    /**
     * Returns true when the given UTC midnight Date is a working day for a task.
     * A day is non-working when:
     *   - it falls on Saturday (6) or Sunday (0), OR
     *   - it is covered by one of the task's calendar exceptions.
     *
     * @param {Date}   date
     * @param {Array}  exceptions
     * @returns {boolean}
     */
    function isWorkingDay(date, exceptions) {
        const dow = date.getUTCDay();
        if (dow === 0 || dow === 6) return false;
        return getCalendarException(date, exceptions) === null;
    }

    // ── Colour helpers ────────────────────────────────────────────────────────

    /**
     * Converts an 8-digit #rrggbbaa hex string to an SVG rgba() string.
     * Falls back to the 6-digit (#rrggbb) path if no alpha digits are present.
     * Delegates to the shared ColorUtils helper for sprint-style colours.
     *
     * @param {string} hex  e.g. "#3a7bc8b0"
     * @returns {string}    e.g. "rgba(58,123,200,0.690)"
     */
    function hexToRgba(hex) {
        return convertSprintColorToRgba(hex);
    }

    /**
     * Converts an 8-digit #rrggbbaa hex string to an rgba() with a different alpha.
     * Used to override the task fill alpha for non-working-day segments.
     *
     * @param {string} hex       #rrggbbaa source string
     * @param {number} newAlpha  replacement alpha 0–255
     * @returns {string}
     */
    function hexToRgbaWithAlpha(hex, newAlpha) {
        if (!hex || hex.length < 7) return 'rgba(0,0,0,0.3)';
        const r = parseInt(hex.slice(1, 3), 16);
        const g = parseInt(hex.slice(3, 5), 16);
        const b = parseInt(hex.slice(5, 7), 16);
        return 'rgba(' + r + ',' + g + ',' + b + ',' + (newAlpha / 255).toFixed(3) + ')';
    }

    // ── Exception strip background colours ────────────────────────────────────

    /**
     * Returns the background colour (as an rgba/hex string) for a non-working day cell
     * in a task row, based on the exception type and theme.
     *
     * @param {Object} theme        Flat theme colour map (ThemeColorKeys values)
     * @param {Object|null} exception  Calendar exception object (null for plain weekends)
     * @param {Date}   date         Current day
     * @returns {string}  CSS colour
     */
    function getDayStripeBgColor(theme, exception, date) {
        const dow = date.getUTCDay();
        if (dow === 6) return getThemeColor(theme, colorKeys.CHART_DAY_OF_WEEK_SATURDAY_BG_COLOR);
        if (dow === 0) return getThemeColor(theme, colorKeys.CHART_DAY_OF_WEEK_SUNDAY_BG_COLOR);
        if (exception) {
            const t = exception.type;
            if (t === 'VACATION') return getThemeColor(theme, colorKeys.GANTT_VACATION_BG_COLOR);
            if (t === 'TRIP') return getThemeColor(theme, colorKeys.GANTT_TRIP_BG_COLOR);
            if (t === 'SICK') return getThemeColor(theme, colorKeys.GANTT_SICK_BG_COLOR);
            return getThemeColor(theme, colorKeys.GANTT_HOLIDAY_BG_COLOR);
        }
        // weekday, no exception → transparent (no stripe)
        return null;
    }

    // ── Chart factory ─────────────────────────────────────────────────────────

    /**
     * Creates and mounts an interactive Gantt chart into a DOM container.
     *
     * @param {HTMLElement} container   Host element (the chart fills its width)
     * @param {Object}      data        GanttChartDto serialised as JSON
     * @param {Object}      [options]
     * @param {string}      [options.containerId]   For localStorage key
     * @param {number}      [options.initialDayWidth]
     * @returns {{ render, schedule, destroy }}
     */
    function createChart(container, data, options) {
        options = options || {};
        const containerId = options.containerId || container.id || 'chart';

        const metadata = data.meta || {};
        const tasks = data.tasks || [];
        const theme = metadata.theme || {};

        const chartStart = getUtcDayMidnight(new Date(metadata.chartStart || Date.now()));
        const chartEnd = getUtcDayMidnight(new Date(metadata.chartEnd || Date.now()));
        const currentDate = metadata.now ? getUtcDayMidnight(new Date(metadata.now)) : getUtcDayMidnight(new Date());
        const totalDays = calculateDayCount(chartStart, chartEnd);

        // Build an id→rowIndex map for relation rendering
        const taskRowMap = {};
        tasks.forEach(function (t) {
            taskRowMap[String(t.id)] = t.rowIndex;
        });

        // CalendarXAxes header renderer
        const calendar = new window.CalendarXAxes(theme);

        let dayWidth = Math.min(MAX_DW, Math.max(MIN_DW, options.initialDayWidth || DEFAULT_DW));
        let scrollOffset = 0;

        // Helper: day index → viewport pixel X (left edge of day column)
        const dayIndexToPixelX = (idx) => (idx - scrollOffset) * dayWidth;
        const getContainerWidth = () => Math.max(200, container.clientWidth || 800);
        const getCalendarHeight = () => calendar.getHeight(dayWidth);

        // ── Scroll / zoom initialisation ──────────────────────────────────

        function initializeScroll() {
            const saved = loadChartViewState(containerId);
            if (saved) {
                dayWidth = Math.min(MAX_DW, Math.max(MIN_DW, saved.dayWidth));
                scrollOffset = saved.scrollOffset;
                constrainScrollOffset();
            } else {
                // Default: put "today" at 20% from the left
                const todayIdx = calculateDayIndex(currentDate, chartStart);
                const visibleDays = getContainerWidth() / dayWidth;
                scrollOffset = Math.max(0, Math.min(totalDays - visibleDays, todayIdx - visibleDays * 0.2));
            }
        }

        // ── Debounced save ────────────────────────────────────────────────
        let saveTimerId = null;

        function scheduleSave() {
            if (saveTimerId) clearTimeout(saveTimerId);
            saveTimerId = setTimeout(function () {
                saveChartViewState(containerId, dayWidth, scrollOffset);
            }, 250);
        }

        // ── Render layers ─────────────────────────────────────────────────

        /**
         * Renders per-row day-stripe background colours.
         * Each task row gets coloured cells for weekends and off-days.
         * Falls back to week-boundary lines when dayWidth < MIN_DAY_BARS.
         *
         * @param {number} calendarH  Y position where task rows start
         * @param {number} totalH     Height of the task area
         * @returns {SVGGElement}
         */
        function renderDayStripes(calendarH, totalH) {
            const g = createSvgElement('g', {'class': 'day-stripes'});
            const containerWidth = getContainerWidth();

            if (dayWidth < MIN_DAY_BARS) {
                // Draw week-boundary lines only
                const gridColor = getThemeColor(theme, colorKeys.GANTT_GRID_COLOR);
                const firstDay = Math.max(0, Math.floor(scrollOffset));
                const lastDay = Math.min(totalDays - 1, firstDay + Math.ceil(containerWidth / dayWidth) + 8);
                for (let d = firstDay; d <= lastDay; d++) {
                    if (new Date(chartStart.getTime() + d * MS).getUTCDay() !== 1) continue;
                    const xPos = dayIndexToPixelX(d);
                    if (xPos < 0 || xPos > containerWidth) continue;
                    g.appendChild(createLine(xPos, calendarH, xPos, calendarH + totalH, {
                        stroke: gridColor, 'stroke-width': '1'
                    }));
                }
                return g;
            }

            // Per-day stripes for each task row
            const firstDay = Math.max(0, Math.floor(scrollOffset) - 1);
            const lastDay = Math.min(totalDays - 1, firstDay + Math.ceil(containerWidth / dayWidth) + 2);

            tasks.forEach(function (task) {
                const rowY = calendarH + task.rowIndex * ROW_H;
                for (let d = firstDay; d <= lastDay; d++) {
                    const xPos = dayIndexToPixelX(d);
                    if (xPos + dayWidth < 0 || xPos > containerWidth) continue;

                    const dayDate = new Date(chartStart.getTime() + d * MS);
                    const dow = dayDate.getUTCDay();
                    const exception = (dow !== 0 && dow !== 6)
                        ? getCalendarException(dayDate, task.calendarExceptions)
                        : null;
                    const bgColor = getDayStripeBgColor(theme, exception, dayDate);
                    if (!bgColor) continue; // weekday, no exception → transparent

                    g.appendChild(createRect(xPos, rowY, dayWidth, TASK_H, {fill: bgColor}));

                    // Draw off-day letter (V/T/S/H) for user off-days
                    if (exception && exception.letter && dayWidth >= 14) {
                        const letterEl = createText(
                            xPos + dayWidth / 2, rowY + TASK_H / 2 + 4,
                            exception.letter, {
                                fill: getThemeColor(theme, colorKeys.GANTT_OUT_OF_OFFICE_COLOR),
                                'font-size': '10',
                                'font-family': 'sans-serif',
                                'font-weight': 'bold',
                                'text-anchor': 'middle'
                            }
                        );
                        g.appendChild(letterEl);
                    }
                }
            });

            return g;
        }

        /**
         * Renders vertical day grid lines across the task area.
         *
         * @param {number} calendarH  Y start of task area
         * @param {number} totalH     Height of task area
         * @returns {SVGGElement}
         */
        function renderGridLines(calendarH, totalH) {
            const g = createSvgElement('g', {'class': 'grid-lines'});
            if (dayWidth < MIN_DAY_BARS) return g;

            const gridColor = getThemeColor(theme, colorKeys.GANTT_GRID_COLOR);
            const containerWidth = getContainerWidth();
            const firstDay = Math.max(0, Math.floor(scrollOffset) - 1);
            const lastDay = Math.min(totalDays, firstDay + Math.ceil(containerWidth / dayWidth) + 2);

            for (let d = firstDay; d <= lastDay; d++) {
                const xPos = dayIndexToPixelX(d);
                if (xPos < 0 || xPos > containerWidth) continue;
                g.appendChild(createLine(xPos, calendarH, xPos, calendarH + totalH, {
                    stroke: gridColor, 'stroke-width': '1'
                }));
            }
            return g;
        }

        /**
         * Renders all task bars, milestone diamonds, story brackets, progress overlays,
         * and text labels (task name to the right, resource name to the left).
         *
         * @param {number} calendarH  Y offset where the first task row starts
         * @returns {SVGGElement}
         */
        function renderTasks(calendarH) {
            const g = createSvgElement('g', {'class': 'tasks'});
            const containerWidth = getContainerWidth();

            tasks.forEach(function (task) {
                if (!task.start || !task.finish) return;

                const startDayIdx = calculateDayIndex(task.start, chartStart);
                const finishDayIdx = calculateDayIndex(task.finish, chartStart);

                const x1 = dayIndexToPixelX(startDayIdx);
                const x2 = dayIndexToPixelX(finishDayIdx + 1);  // exclusive right edge

                // Cull rows entirely outside the viewport
                if (x2 < 0 || x1 > containerWidth) return;

                const rowY = calendarH + task.rowIndex * ROW_H;
                const bodyY = rowY + TASK_BODY_BORDER;
                const bodyH = TASK_H - TASK_BODY_BORDER * 2;
                const midY = rowY + TASK_H / 2;  // vertical centre of the row

                if (task.milestone) {
                    // ── Diamond milestone ─────────────────────────────────
                    renderMilestone(g, task, x1, midY);
                } else if (task.story) {
                    // ── Story bracket ─────────────────────────────────────
                    renderStory(g, task, x1, x2, rowY, bodyY, bodyH, containerWidth);
                } else {
                    // ── Regular task bar (per-day segments) ───────────────
                    renderTaskBar(g, task, startDayIdx, finishDayIdx, x1, x2, bodyY, bodyH, containerWidth);
                    renderProgress(g, task, x1, x2, bodyY, bodyH);
                    renderCriticalBorder(g, task, startDayIdx, finishDayIdx, x1, x2, bodyY, bodyH);
                }

                // ── Labels ────────────────────────────────────────────────
                if (!task.milestone) {
                    renderTaskLabels(g, task, x1, x2, midY, containerWidth);
                }
            });

            return g;
        }

        /**
         * Draws a milestone as a filled diamond centred at (x1+dayWidth/2, midY).
         */
        function renderMilestone(g, task, x1, midY) {
            const cx = x1 + dayWidth / 2;  // centre of the start day column
            const hw = MILESTONE_HALF;
            const points = [
                cx + ',' + (midY - hw),
                (cx + hw) + ',' + midY,
                cx + ',' + (midY + hw),
                (cx - hw) + ',' + midY
            ].join(' ');
            const poly = createSvgElement('polygon', {
                points: points,
                fill: hexToRgba(task.fillColor),
                stroke: task.borderColor || '#888888',
                'stroke-width': '1'
            });
            poly.appendChild(createSvgElement('title', {}, buildTaskTooltip(task)));
            g.appendChild(poly);
        }

        /**
         * Draws a story bar: thick top and side borders with semi-transparent fill.
         */
        function renderStory(g, task, x1, x2, rowY, bodyY, bodyH, containerWidth) {
            const w = x2 - x1;
            if (w <= 0) return;
            const fillColor = hexToRgba(task.fillColor);

            // Background fill
            const bg = createRect(x1, bodyY, w, bodyH, {fill: fillColor, opacity: '0.6'});
            bg.appendChild(createSvgElement('title', {}, buildTaskTooltip(task)));
            g.appendChild(bg);

            // Top border (2px thick)
            g.appendChild(createRect(x1, rowY, w, 2, {fill: task.borderColor || '#444444'}));
            // Left border
            g.appendChild(createRect(x1, rowY, 2, bodyH, {fill: task.borderColor || '#444444'}));
            // Right border
            g.appendChild(createRect(x2 - 2, rowY, 2, bodyH, {fill: task.borderColor || '#444444'}));
        }

        /**
         * Draws a regular task bar with per-day segments coloured by working / non-working status.
         */
        function renderTaskBar(g, task, startDayIdx, finishDayIdx, x1, x2, bodyY, bodyH, containerWidth) {
            const weekEndAlpha = getTransparency(task);

            for (let d = startDayIdx; d <= finishDayIdx; d++) {
                const segX = dayIndexToPixelX(d);
                if (segX + dayWidth < 0 || segX > containerWidth) continue;

                const dayDate = new Date(chartStart.getTime() + d * MS);
                const working = isWorkingDay(dayDate, task.calendarExceptions);

                let segFill;
                if (working) {
                    segFill = hexToRgba(task.fillColor);
                } else {
                    // Non-working segment: same colour but taskWeekEndTransparency alpha
                    segFill = hexToRgbaWithAlpha(task.fillColor, weekEndAlpha);
                }

                const rect = createRect(segX, bodyY, dayWidth, bodyH, {fill: segFill});
                rect.appendChild(createSvgElement('title', {}, buildTaskTooltip(task)));
                g.appendChild(rect);
            }
        }

        /**
         * Extracts the taskWeekEndTransparency value from the theme (0–255).
         */
        function getTransparency(task) {
            const rawKey = 'ganttTheme.taskWeekEndTransparency';
            const v = theme[rawKey];
            return (v != null && typeof v === 'number') ? v : 40;
        }

        /**
         * Draws the progress overlay bar inside the task bar.
         */
        function renderProgress(g, task, x1, x2, bodyY, bodyH) {
            if (!task.progress || task.progress <= 0) return;
            const progressW = Math.round((x2 - x1) * task.progress);
            if (progressW <= 1) return;

            const progressColor = task.progressColor ? hexToRgba(task.progressColor) : 'rgba(255,255,255,0.4)';
            g.appendChild(createRect(x1 + 1, bodyY + 2, progressW - 1, bodyH - 4, {fill: progressColor}));

            // Progress end marker (1px vertical line)
            if (task.progress < 1.0) {
                g.appendChild(createRect(x1 + progressW - 1, bodyY + 2, 1, bodyH - 4, {fill: '#000000'}));
            }
        }

        /**
         * Draws the task border (solid for working days, dotted for non-working days).
         * Mirrors Java AbstractGanttRenderer.drawCriticalMarker().
         */
        function renderCriticalBorder(g, task, startDayIdx, finishDayIdx, x1, x2, bodyY, bodyH) {
            const borderColor = task.borderColor || '#888888';
            const topY = bodyY;
            const bottomY = bodyY + bodyH - 1;
            const days = finishDayIdx - startDayIdx;

            for (let d = startDayIdx; d <= finishDayIdx; d++) {
                const dayDate = new Date(chartStart.getTime() + d * MS);
                const working = isWorkingDay(dayDate, task.calendarExceptions);
                const dayLeft = dayIndexToPixelX(d);
                const dayRight = dayLeft + dayWidth;
                const isFirst = (d === startDayIdx);
                const isLast = (d === finishDayIdx);

                if (working) {
                    // Solid horizontal top/bottom borders for this day segment
                    g.appendChild(createRect(dayLeft, topY, dayWidth, 1, {fill: borderColor}));
                    g.appendChild(createRect(dayLeft, bottomY, dayWidth, 1, {fill: borderColor}));
                    if (isFirst) {
                        g.appendChild(createRect(dayLeft, topY + 1, 1, bodyH - 2, {fill: borderColor}));
                    }
                    if (isLast) {
                        g.appendChild(createRect(dayRight - 1, topY + 1, 1, bodyH - 2, {fill: borderColor}));
                    }
                } else {
                    // Dotted border for non-working day segments
                    for (let px = dayLeft; px < dayRight; px += 4) {
                        g.appendChild(createRect(px, topY, 2, 1, {fill: borderColor}));
                        g.appendChild(createRect(px, bottomY, 2, 1, {fill: borderColor}));
                    }
                }
            }
        }

        /**
         * Renders task name (to the right) and resource name (to the left) labels.
         */
        function renderTaskLabels(g, task, x1, x2, midY, containerWidth) {
            const textY = midY + 4;  // approximate baseline for 12px font centred on midY
            const textFill = task.textColor || '#000000';

            // Resource name to the LEFT of the task bar
            if (task.assignedUserName && x1 > 0) {
                const clipId = 'gc-left-' + String(task.id).replace(/-/g, '');
                const labelX = x1 - RESOURCE_NAME_TO_TASK_GAP;
                // Clip to prevent overlap with earlier tasks – keep max 120px wide
                const clipW = Math.min(120, x1);
                if (clipW > 8) {
                    const clipX = labelX - clipW;
                    g.appendChild(createClipPath(clipId, Math.max(0, clipX), midY - TASK_H, clipW, TASK_H * 2));
                    g.appendChild(createText(labelX, textY, task.assignedUserName, {
                        fill: textFill,
                        'font-size': '12',
                        'font-family': 'sans-serif',
                        'text-anchor': 'end',
                        'clip-path': 'url(#' + clipId + ')'
                    }));
                }
            }

            // Task key + name to the RIGHT of the task bar
            const labelRight = x2 + TASK_NAME_TO_TASK_GAP;
            if (labelRight < containerWidth + 40) {  // allow slight overflow
                const clipId = 'gc-right-' + String(task.id).replace(/-/g, '');
                const clipW = Math.max(0, containerWidth - labelRight);
                if (clipW > 8) {
                    g.appendChild(createClipPath(clipId, labelRight, midY - TASK_H, clipW, TASK_H * 2));
                    const label = (task.key ? task.key + ' ' : '') + (task.name || '');
                    g.appendChild(createText(labelRight, textY, label, {
                        fill: textFill,
                        'font-size': '12',
                        'font-family': 'sans-serif',
                        'font-weight': task.story ? 'bold' : 'normal',
                        'clip-path': 'url(#' + clipId + ')'
                    }));
                }
            }
        }

        /**
         * Renders finish-to-start dependency arrows between tasks.
         * Arrow: horizontal line from predecessor.finish → vertical → arrowhead at successor.start.
         *
         * @param {number} calendarH  Y offset where task rows start
         * @returns {SVGGElement}
         */
        function renderRelations(calendarH) {
            const g = createSvgElement('g', {'class': 'relations'});
            const containerWidth = getContainerWidth();
            const relColor = getThemeColor(theme, colorKeys.GANTT_RELATION_COLOR);
            const critColor = getThemeColor(theme, colorKeys.GANTT_CRITICAL_RELATION_COLOR);

            // Build id→task map for quick lookup
            const taskById = {};
            tasks.forEach(function (t) {
                taskById[String(t.id)] = t;
            });

            tasks.forEach(function (sourceTask) {
                if (!sourceTask.predecessors || !sourceTask.predecessors.length) return;
                if (!sourceTask.start) return;

                const sourceRow = calendarH + sourceTask.rowIndex * ROW_H + TASK_H / 2;
                const sourceX1 = dayIndexToPixelX(calculateDayIndex(sourceTask.start, chartStart));

                sourceTask.predecessors.forEach(function (rel) {
                    if (!rel.visible) return;
                    const targetTask = taskById[String(rel.predecessorId)];
                    if (!targetTask || !targetTask.finish) return;

                    const targetRow = calendarH + targetTask.rowIndex * ROW_H + TASK_H / 2;
                    const targetFinX = dayIndexToPixelX(calculateDayIndex(targetTask.finish, chartStart) + 1);

                    // Arrow colour: both critical → critColor, else relColor
                    const arrowColor = (sourceTask.critical && targetTask.critical) ? critColor : relColor;

                    // x1 = predecessor finish edge
                    const ax1 = targetFinX;
                    // x2 = RELATION_CORNER_LENGTH past successor start
                    const ax2 = sourceX1 + RELATION_CORNER_LENGTH;

                    // Skip if arrow is entirely off-screen
                    if (Math.max(ax1, ax2) < 0 || Math.min(ax1, ax2) > containerWidth) return;

                    const y1 = targetRow;  // predecessor row centre
                    const y2 = sourceRow;  // successor row centre
                    const signum = (y2 > y1) ? 1 : -1;
                    const yEnd = (y2 > y1)
                        ? y2 - TASK_H / 2 + TASK_BODY_BORDER
                        : y2 + TASK_H / 2 - TASK_BODY_BORDER;
                    const yMid = (y2 > y1) ? yEnd - 5 : yEnd + 5;

                    // Horizontal segment from predecessor finish to turn point
                    g.appendChild(createLine(ax1, y1, ax2, y1, {stroke: arrowColor, 'stroke-width': '1'}));
                    // Vertical segment from turn point to near successor
                    g.appendChild(createLine(ax2, y1, ax2, yMid, {stroke: arrowColor, 'stroke-width': '1'}));

                    // Arrowhead (triangle pointing toward successor)
                    const d = 5;
                    let points;
                    if (y2 > y1) {
                        points = [(ax2 - d) + ',' + (yEnd - d + signum),
                            (ax2 + d) + ',' + (yEnd - d + signum),
                            ax2 + ',' + (yEnd + signum)].join(' ');
                    } else {
                        points = [(ax2 + d) + ',' + (yEnd + d + signum),
                            (ax2 - d) + ',' + (yEnd + d + signum),
                            ax2 + ',' + (yEnd + signum)].join(' ');
                    }
                    g.appendChild(createSvgElement('polygon', {
                        points: points, fill: arrowColor
                    }));
                });
            });

            return g;
        }

        /**
         * Renders the "now" vertical line (2px red).
         *
         * @param {number} totalHeight  Full SVG height
         * @returns {SVGGElement}
         */
        function renderNowLine(totalHeight) {
            const g = createSvgElement('g', {'class': 'now-line'});
            const containerWidth = getContainerWidth();
            const nowIdx = calculateDayIndex(currentDate, chartStart);
            const xPos = dayIndexToPixelX(nowIdx) + dayWidth / 2;
            if (xPos < 0 || xPos > containerWidth) return g;
            g.appendChild(createLine(xPos, 0, xPos, totalHeight, {
                stroke: '#cc0000', 'stroke-width': '2'
            }));
            return g;
        }

        // ── Tooltip builder ───────────────────────────────────────────────

        function buildTaskTooltip(task) {
            let s = task.name || '';
            if (task.key) s += '\nKey: ' + task.key;
            if (task.start) s += '\nStart: ' + new Date(task.start).toLocaleDateString();
            if (task.finish) s += '\nFinish: ' + new Date(task.finish).toLocaleDateString();
            if (task.assignedUserName) s += '\nResource: ' + task.assignedUserName;
            if (task.progress > 0) s += '\nProgress: ' + Math.round(task.progress * 100) + '%';
            return s;
        }

        // ── Full redraw ───────────────────────────────────────────────────

        let animationFrameId = null;

        /**
         * Redraws the entire chart SVG.
         * Render order (bottom → top): day stripes → grid lines → tasks → relations → calendar header → now line
         */
        function redrawChart() {
            const containerWidth = getContainerWidth();
            const calendarH = getCalendarHeight();
            const taskAreaH = tasks.length * ROW_H + 8;
            const totalHeight = calendarH + taskAreaH;

            const svg = createSvgElement('svg', {
                width: containerWidth,
                height: totalHeight,
                style: 'display:block;user-select:none;shape-rendering:crispEdges'
            });

            svg.appendChild(renderDayStripes(calendarH, taskAreaH));
            svg.appendChild(renderGridLines(calendarH, taskAreaH));
            svg.appendChild(renderTasks(calendarH));
            svg.appendChild(renderRelations(calendarH));
            calendar.draw(svg, chartStart, totalDays, dayWidth, scrollOffset, containerWidth);
            svg.appendChild(renderNowLine(totalHeight));

            container.innerHTML = '';
            container.appendChild(svg);
        }

        function scheduleRender() {
            if (animationFrameId) cancelAnimationFrame(animationFrameId);
            animationFrameId = requestAnimationFrame(redrawChart);
        }

        function constrainScrollOffset() {
            scrollOffset = Math.max(0, Math.min(
                Math.max(0, totalDays - getContainerWidth() / dayWidth),
                scrollOffset
            ));
        }

        // ── Wheel zoom / trackpad pan ─────────────────────────────────────

        function handleWheelEvent(event) {
            event.preventDefault();
            if (event.deltaX !== 0) {
                scrollOffset += event.deltaX / dayWidth;
            } else {
                const rect = container.getBoundingClientRect();
                const mouseX = (event.clientX != null) ? (event.clientX - rect.left) : (getContainerWidth() / 2);
                const dayUnderCursor = scrollOffset + mouseX / dayWidth;
                const factor = event.deltaY < 0 ? ZOOM_STEP : 1 / ZOOM_STEP;
                dayWidth = Math.max(MIN_DW, Math.min(MAX_DW, dayWidth * factor));
                scrollOffset = dayUnderCursor - mouseX / dayWidth;
            }
            constrainScrollOffset();
            scheduleRender();
            scheduleSave();
        }

        // ── Click-and-drag pan ────────────────────────────────────────────

        let dragState = null;

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
            if (dragState) {
                dragState = null;
                scheduleSave();
            }
            container.style.cursor = 'grab';
        }

        // ── ResizeObserver ────────────────────────────────────────────────
        let resizeObserver = null;
        if (typeof ResizeObserver !== 'undefined') {
            resizeObserver = new ResizeObserver(scheduleRender);
            resizeObserver.observe(container);
        }

        // ── Cleanup ───────────────────────────────────────────────────────

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

        // ── Attach listeners and initial render ───────────────────────────
        container.style.cursor = 'grab';
        container.addEventListener('wheel', handleWheelEvent, {passive: false});
        container.addEventListener('pointerdown', handlePointerDown, {passive: false});
        container.addEventListener('pointermove', handlePointerMove, {passive: true});
        container.addEventListener('pointerup', handlePointerUp);
        container.addEventListener('pointercancel', handlePointerUp);

        initializeScroll();
        redrawChart();

        return {render: redrawChart, schedule: scheduleRender, destroy: cleanupChart};
    }

    // ── Public mount API ──────────────────────────────────────────────────────

    /** Singleton chart instance (one Gantt chart per page). */
    var currentGanttChartInstance = null;

    /**
     * Mounts (or remounts) the Gantt chart into the named container.
     * Called by QualityBoard.refreshGanttChart() via Vaadin executeJs.
     *
     * @param {string} containerId   DOM element ID of the chart container
     * @param {Object} injectedData  GanttChartDto deserialised from JSON
     */
    function mountGanttChart(containerId, injectedData) {
        const elementId = containerId || 'gantt-chart-container';
        const containerElement = document.getElementById(elementId);
        if (!containerElement) return;

        // Destroy previous instance
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

    // Export public API
    window.mountGanttChart = mountGanttChart;
    window.createGanttChart = createChart;    // For test pages
})();

