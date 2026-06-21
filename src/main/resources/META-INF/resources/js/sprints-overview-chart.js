// sprints-overview-chart.js
// Interactive Sprints Overview chart renderer.
// Mirrors Java: SprintsOverviewRenderer, SprintsOverviewChart
//
// Class hierarchy (matches Java):
//   AbstractRenderer  (chart-util.js)
//     └─ SprintsOverviewRenderer  (this file)
//   AbstractCanvas  (chart-util.js)
//     └─ AbstractChart  (chart-util.js)
//          └─ SprintsOverviewChart (this file)
//
// Interaction:
//   mouse wheel (vertical)   → zoom (dayWidth, keeps day under cursor)
//   trackpad swipe (horiz.)  → pan
//   click + drag             → pan
//   right-click on sprint    → context menu with navigation links
//   container resize         → automatic redraw
//
// View state (dayWidth + scrollOffset) is persisted in localStorage under
//   kassandra.chart.<chartName>.view
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0
(function () {
    'use strict';

    var createSvgElement = window.SvgUtils.createSvgElement;
    var createRect = window.SvgUtils.createRect;
    var createText = window.SvgUtils.createText;
    var createLine = window.SvgUtils.createLine;
    var createClipPath = window.SvgUtils.createClipPath;
    var intToHex = window.ColorUtils.intToHex;
    var convertSprintColorToRgba = window.ColorUtils.convertSprintColorToRgba;
    var MS = window.DateUtils.MS;
    var getDayMidnight = window.DateUtils.getDayMidnight;
    var calculateDayIndex = window.DateUtils.calculateDayIndex;
    var calculateDayCount = window.DateUtils.calculateDayCount;

    // ── Layout constants ───────────────────────────────────────────────────────
    // Mirrors Java: SprintsOverviewRenderer constants

    /** Single-line height for sprint content. Mirrors Java SprintsOverviewRenderer.LINE_HEIGHT = 13. */
    var LINE_HEIGHT = 13;
    /** Number of text lines per sprint block. Mirrors Java SprintsOverviewRenderer.numberOfLines = 3 (from dao). */
    var NUMBER_OF_LINES = 3;
    /**
     * Sprint rectangle height = LINE_HEIGHT * numberOfLines = 13 * 3 = 39.
     * Mirrors Java SprintsOverviewRenderer.getTaskHeight() when numberOfLines > 1:
     *   LINE_HEIGHT * numberOfLines = 39. The rect is drawn with this height.
     */
    var SPRINT_H = LINE_HEIGHT * NUMBER_OF_LINES;          // 39 px
    /**
     * Lane spacing = getTaskHeight() + 2.
     * Java getTaskHeight() when numberOfLines > 1: LINE_HEIGHT * numberOfLines + 17 = 56.
     */
    var TASK_H = LINE_HEIGHT * NUMBER_OF_LINES + 17;     // 56 px
    var LANE_H = TASK_H + 2;                             // 58 px

    /** Default pixel width per day. Mirrors Java SprintsOverviewRenderer.calculateDayWidth() → dayOfWeek.width = 8. */
    var DEFAULT_DW = 8;
    var MIN_DW = 1;
    var MAX_DW = 80;
    var ZOOM_STEP = 1.25;

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
        } catch (e) { /* unavailable */
        }
        return null;
    }

    function saveViewState(containerId, dayWidth, scrollOffset) {
        try {
            localStorage.setItem(viewStateKey(containerId), JSON.stringify({
                dayWidth: dayWidth,
                scrollOffset: scrollOffset
            }));
        } catch (e) { /* quota */
        }
    }

    // ── Context menu (singleton per page) ─────────────────────────────────────

    /** @type {?HTMLDivElement} */
    var contextMenuSingleton = null;

    function getOrCreateContextMenu() {
        if (contextMenuSingleton) return contextMenuSingleton;
        var menu = document.createElement('div');
        menu.style.cssText = [
            'position:fixed', 'z-index:99999',
            'background:var(--lumo-base-color,#fff)',
            'color:var(--lumo-body-text-color,#333)',
            'border:1px solid var(--lumo-contrast-20pct,#ccc)',
            'border-radius:var(--lumo-border-radius-m,4px)',
            'box-shadow:0 4px 16px rgba(0,0,0,.18)',
            'padding:4px 0', 'min-width:160px', 'display:none'
        ].join(';');
        document.body.appendChild(menu);
        contextMenuSingleton = menu;
        document.addEventListener('click', function (event) {
            if (contextMenuSingleton && !contextMenuSingleton.contains(event.target)) {
                contextMenuSingleton.style.display = 'none';
            }
        });
        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape' && contextMenuSingleton) {
                contextMenuSingleton.style.display = 'none';
            }
        });
        return menu;
    }

    function hideContextMenu() {
        if (contextMenuSingleton) contextMenuSingleton.style.display = 'none';
    }

    function showContextMenuForSprint(clientX, clientY, sprint) {
        var menu = getOrCreateContextMenu();
        menu.innerHTML = '';

        // Sprint name header
        if (sprint.name) {
            var header = document.createElement('div');
            header.style.cssText = 'padding:4px 14px;font-weight:bold;border-bottom:1px solid var(--lumo-contrast-20pct,#ccc);margin-bottom:4px';
            header.textContent = sprint.name;
            menu.appendChild(header);
        }

        // Navigation items
        var items = [
            {label: 'Backlog', href: '/ui/backlog?sprint=' + sprint.id},
            {label: 'Active Sprint', href: '/ui/active-sprint?sprint=' + sprint.id},
            {label: 'Quality Board', href: '/ui/quality-board?sprint=' + sprint.id}
        ];
        items.forEach(function (item) {
            var row = document.createElement('a');
            row.href = item.href;
            row.textContent = item.label;
            row.style.cssText = 'display:block;padding:6px 14px;text-decoration:none;color:inherit;cursor:pointer';
            row.addEventListener('mouseenter', function () {
                row.style.background = 'var(--lumo-primary-color-10pct,#e8f0fe)';
            });
            row.addEventListener('mouseleave', function () {
                row.style.background = '';
            });
            row.addEventListener('click', function (e) {
                e.stopPropagation();
                hideContextMenu();
            });
            menu.appendChild(row);
        });

        menu.style.display = 'block';
        var menuW = menu.offsetWidth || 160;
        var menuH = menu.offsetHeight || 120;
        var leftPos = (clientX + menuW + 2 > window.innerWidth) ? (clientX - menuW) : (clientX + 2);
        var topPos = (clientY + menuH + 2 > window.innerHeight) ? (clientY - menuH) : (clientY + 2);
        menu.style.left = Math.max(0, leftPos) + 'px';
        menu.style.top = Math.max(0, topPos) + 'px';
    }

    // ── SprintsOverviewRenderer ────────────────────────────────────────────────
    // Mirrors Java: SprintsOverviewRenderer extends AbstractRenderer

    class SprintsOverviewRenderer extends window.AbstractRenderer {

        constructor(data, theme, preRun, postRun) {
            // Create milestones first, BEFORE calling super()
            // Mirrors Java SprintsOverviewRenderer constructor
            let chartStart = new Date(data.meta.chartStart || Date.now());
            let chartEnd = new Date(data.meta.chartEnd || Date.now());
            let currentDate = new Date(data.meta.now);

            let milestonesList = [];

            // Milestone "N" (Now) - current date
            milestonesList.push(new window.Milestone(
                currentDate,
                'N',
                'Now (current date)',
                false
            ));

            milestonesList.push(new window.Milestone(
                getDayMidnight(chartStart),
                'S',
                'Start (Start of project)',
                false
            ));

            milestonesList.push(new window.Milestone(
                getDayMidnight(chartEnd),
                'E',
                'End (End of project)',
                false
            ));

            let milestones = new window.Milestones(
                milestonesList,
                chartStart, // firstMilestone
                chartEnd // lastMilestone
            );

            // NOW call super() with theme and milestones
            // Mirrors Java: AbstractRenderer constructor pattern
            // SprintsOverviewRenderer uses 3 (preRun) and 5 (postRun) like Java
            super(theme, milestones, preRun, postRun);

            this.lanes = data.lanes || [];
            this.chartStart = chartStart;
            this.chartEnd = chartEnd;
            this.currentDate = currentDate;
            this.totalDays = calculateDayCount(this.chartStart, this.chartEnd);

            /** Interactive scroll/zoom state – updated by SprintsOverviewChart.updateViewState() */
            this.dayWidth = DEFAULT_DW;
            this.containerWidth = 800;

            /** Hit areas for context menu – rebuilt on every draw(). */
            this.sprintHitAreas = [];
            this.initSize(0, false, window.CalendarSize.YEARS);
        }

        calculateChartWidth() {
            return this.calendarXAxes.dayOfWeek.getWidth() * this.days;
        }

        dayIndexToPixelX(dayIndex) {
            return (dayIndex - this.scrollOffset + this.preRun) * this.dayWidth;
        }

        calculateDayWidth() {
            // super.calculateDayWidth();
            // this.calendarXAxes.dayOfWeek.setWidth(8);
            return this.dayWidth;
        }

        /**
         * Computes sprite area height.
         * Mirrors Java: SprintsOverviewRenderer.calculateChartHeight() – lane portion.
         */
        calculateLaneAreaHeight() {
            return this.lanes.length * LANE_H + 8;
        }

        /**
         * Computes the full renderer content height (calendar + lanes).
         * Mirrors Java: SprintsOverviewRenderer.calculateChartHeight().
         */
        calculateChartHeight() {
            var calH = this.calendarXAxes ? this.calendarXAxes.getHeight(this.dayWidth, false) : 0;
            return calH + this.calculateLaneAreaHeight();
        }

        /**
         * Renders weekend stripes or week-boundary lines.
         * Mirrors Java: AbstractRenderer.drawDayBars() (background stripe portion for SprintsOverview).
         */
        renderWeekendStripes(baseY, baseHeight) {
            let g = createSvgElement('g', {'class': 'weekend-stripes'});
            let containerWidth = this.containerWidth;
            let xAxesTheme = this.theme.xAxesTheme;

            if (this.dayWidth >= 4) {
                let satColor = intToHex(xAxesTheme.dayOfweekSaturdayBgColor, null);
                let sunColor = intToHex(xAxesTheme.dayOfweekSundayBgColor, null);
                let firstDay = Math.max(0, Math.floor(this.scrollOffset) - 1);
                let lastDay = Math.min(this.totalDays - 1, firstDay + Math.ceil(containerWidth / this.dayWidth) + 2);
                for (let d = firstDay; d <= lastDay; d++) {
                    let dow = new Date(this.chartStart.getTime() + d * MS).getDay();
                    let xPos = this.dayIndexToPixelX(d);
                    if (xPos + this.dayWidth < 0 || xPos > containerWidth) continue;
                    let bgColor = dow === 6 ? satColor : (dow === 0 ? sunColor : null);
                    if (!bgColor) continue;
                    g.appendChild(createRect(xPos, baseY, this.dayWidth, baseHeight, {fill: bgColor}));
                }
            } else {
                // Week-boundary vertical lines
                let gridColor = intToHex(this.theme.ganttTheme.gridColor, '#e4e8f3');
                let firstD = Math.max(0, Math.floor(this.scrollOffset));
                let lastD = Math.min(this.totalDays - 1, firstD + Math.ceil(containerWidth / this.dayWidth) + 8);
                for (let dd = firstD; dd <= lastD; dd++) {
                    if (new Date(this.chartStart.getTime() + dd * MS).getDay() !== 1) continue;
                    let xp = this.dayIndexToPixelX(dd);
                    if (xp < 0 || xp > containerWidth) continue;
                    g.appendChild(createLine(xp, baseY, xp, baseY + baseHeight, {
                        stroke: gridColor,
                        'stroke-width': '1'
                    }));
                }
            }
            return g;
        }

        /**
         * Renders vertical day grid lines.
         * Mirrors Java: AbstractRenderer.drawDayBars() (grid line portion).
         */
        renderVerticalGridLines(baseY, baseHeight) {
            let g = createSvgElement('g', {'class': 'grid-lines'});
            let containerWidth = this.containerWidth;
            if (this.dayWidth < 4) return g;

            let gridColor = intToHex(this.theme.ganttTheme.gridColor, '#e4e8f3');
            let firstDay = Math.max(0, Math.floor(this.scrollOffset) - 1);
            let lastDay = Math.min(this.totalDays, firstDay + Math.ceil(containerWidth / this.dayWidth) + 2);
            for (let d = firstDay; d <= lastDay; d++) {
                let xPos = this.dayIndexToPixelX(d);
                if (xPos < 0 || xPos > containerWidth) continue;
                g.appendChild(createLine(xPos, baseY, xPos, baseY + baseHeight, {
                    stroke: gridColor,
                    'stroke-width': '1'
                }));
            }
            return g;
        }

        /**
         * Renders sprint rectangles and labels across all lanes.
         * Mirrors Java: SprintsOverviewRenderer.drawGraph()
         */
        drawGraph(svg) {
            this.sprintHitAreas = [];
            let g = createSvgElement('g', {'class': 'sprints'});
            let containerWidth = this.containerWidth;
            let self = this;

            this.lanes.forEach(function (lane, laneIndex) {
                let laneY = self.diagram.y + laneIndex * LANE_H;

                (lane.sprints || []).forEach(function (sprint) {
                    if (!sprint.start || !sprint.end) return;
                    let startIdx = calculateDayIndex(sprint.start, self.chartStart);
                    let endIdx = calculateDayIndex(sprint.end, self.chartStart);
                    let sprintX = self.dayIndexToPixelX(startIdx);
                    let sprintW = (endIdx - startIdx + 1) * self.dayWidth - 1;
                    if (sprintX + sprintW < 0 || sprintX > containerWidth) return;

                    // Register hit area for context menu
                    self.sprintHitAreas.push({sprint: sprint, x: sprintX, y: laneY, width: sprintW, height: SPRINT_H});

                    // Sprint rectangle – fill color from sprint.color (#rrggbbaa)
                    var fillColor = convertSprintColorToRgba(sprint.color);
                    var rect = createRect(sprintX, laneY, sprintW, SPRINT_H, {fill: fillColor});
                    rect.appendChild(createSvgElement('title', {}, self.buildSprintTooltip(sprint)));
                    g.appendChild(rect);

                    // Sprint name label (clipped to rect width)
                    if (sprintW > 20) {
                        var clipId = 'sp' + laneIndex + '_' + sprint.id;
                        g.appendChild(createClipPath(clipId, sprintX, laneY, sprintW, SPRINT_H));
                        // Java: drawString at x1+1, y1 + (0+1)*LINE_HEIGHT - 2 = y1 + LINE_HEIGHT - 2
                        var textY = laneY + LINE_HEIGHT - 2;
                        g.appendChild(createText(sprintX + 1, textY, sprint.name || '', {
                            fill: '#000000',
                            'font-size': '12',
                            'font-family': 'Arial,sans-serif',
                            'font-weight': 'bold',
                            'clip-path': 'url(#' + clipId + ')'
                        }));
                    }
                });
            });
            svg.appendChild(g);
        }

        /**
         * Renders the "today" vertical marker line.
         * Mirrors Java: CalendarXAxes now-line rendering for overview.
         */
        renderCurrentDateLine(chartHeight) {
            let g = createSvgElement('g', {'class': 'now-line'});
            let containerWidth = this.containerWidth;
            let nowIdx = calculateDayIndex(this.currentDate, this.chartStart);
            let xPos = this.dayIndexToPixelX(nowIdx) + this.dayWidth / 2;
            if (xPos < 0 || xPos > containerWidth) return g;
            g.appendChild(createLine(xPos, 0, xPos, chartHeight, {stroke: '#cc0000', 'stroke-width': '2'}));
            return g;
        }

        /** Builds a multi-line tooltip string for a sprint. */
        buildSprintTooltip(sprint) {
            let tooltip = sprint.name || '';
            if (sprint.key) tooltip += '\nKey: ' + sprint.key;
            if (sprint.status) tooltip += '\nStatus: ' + sprint.status;
            if (sprint.start) tooltip += '\nStart: ' + new Date(sprint.start).toLocaleDateString();
            if (sprint.end) tooltip += '\nEnd: ' + new Date(sprint.end).toLocaleDateString();
            if (sprint.delay) tooltip += '\n(DELAYED)';
            return tooltip;
        }

        initPosition(x, y) {
            this.firstDayX = x;
            if (this.calendarAtBottom) {
                this.calendarXAxes.initPosition(x, y);
                this.diagram.initPosition(x, y);
                this.calendarXAxes.initPosition(x, this.diagram.y + this.diagram.height + 1);
            } else {
                this.calendarXAxes.initPosition(x, y);
                this.diagram.initPosition(x, this.calendarXAxes.year.getY() + this.calendarXAxes.getHeight());
//            this.calendarXAxes.initPosition(x, this.diagram.y + this.diagram.height + 1);
            }
        }

        drawCalendar(drawDays, svg) {
            this.calendarXAxes.drawCalendar(drawDays, svg, this.diagram.width);
        }

        /**
         * Draws the SprintsOverview content into the SVG at (x, y).
         * Mirrors Java: SprintsOverviewRenderer.draw(ExtendedGraphics2D, int x, int y)
         */
        draw(svg, x, y) {
            // var calendarH = this.calendarXAxes.getHeight(this.dayWidth, false);
            // var lanesH = this.calculateLaneAreaHeight();
            // var totalH = calendarH + lanesH;
            //
            // // Initialize calendar positioning – mirrors Java: initPosition(firstDayX + x, y)
            // this.calendarXAxes.initPosition(0, y);
            //
            // // Render order: stripes → grid → sprints → calendar header → now-line
            // // Mirrors Java: drawCalendar() → drawGraph() → drawMilestones()
            // svg.appendChild(this.renderWeekendStripes(y + calendarH, lanesH));
            // svg.appendChild(this.renderVerticalGridLines(y + calendarH, lanesH));
            // svg.appendChild(this.renderSprintRectangles(y + calendarH));
            // // Use unified drawCalendar method like gantt-chart.js (pass milestones parameter)
            // this.calendarXAxes.draw(svg, this.chartStart, this.totalDays, this.dayWidth, this.scrollOffset, this.containerWidth, this.milestones);
            // svg.appendChild(this.renderCurrentDateLine(y + totalH));

            this.initPosition(0 + x, y);
            this.drawCalendar(true, svg);
            this.drawGraph(svg, x, y);
            this.drawMilestones(svg);
        }

    }

    // ── SprintsOverviewChart ───────────────────────────────────────────────────
    // Mirrors Java: SprintsOverviewChart extends AbstractChart

    class SprintsOverviewChart extends window.AbstractChart {
        /**
         * @param {Object} data   SprintOverviewDto JSON
         * @param {Theme}  theme  Theme instance
         */
        constructor(data, theme) {
            super('Project Overview Chart', '', '', '', 'sprints-overview-chart', theme);
            /** Mirrors Java: SprintsOverviewChart → getRenderers().add(new SprintsOverviewRenderer(dao)) */
            this.addRenderer(new SprintsOverviewRenderer(data, theme, 5, 5));
        }

        /**
         * Updates renderer scroll/zoom state and recomputes chart dimensions.
         * Called before each render frame.
         *
         * @param {number} dayWidth
         * @param {number} scrollOffset
         * @param {number} containerWidth
         */
        updateViewState(dayWidth, scrollOffset, containerWidth) {
            var renderer = this.renderers[0];
            renderer.dayWidth = dayWidth;
            renderer.calendarXAxes.dayOfWeek.width = dayWidth;
            renderer.scrollOffset = scrollOffset;
            renderer.containerWidth = containerWidth;

            var calendarH = renderer.calendarXAxes.getHeight(dayWidth, false);
            var lanesH = renderer.calculateLaneAreaHeight();
            var contentH = calendarH + lanesH;

            // Mirrors Java: SprintsOverviewChart constructor dimension setup
            this.setChartWidth(containerWidth);
            this.setChartHeight(contentH + this.captionElement.height + this.footerElement.height - 1);
            this.footerElement.y = contentH + this.captionElement.height;//TODO
            this.renderers[0].initSize(this.firstDayX, false, window.CalendarSize.YEARS);

        }

        /** Mirrors Java: SprintsOverviewChart.createReport() */
        createReport(svg) {
            this.renderers[0].draw(svg, 0, this.captionElement.height);
        }
    }

    // ── Mount / createChart function ──────────────────────────────────────────

    /** Singleton chart instance (one per page). */
    var currentChartInstance = null;

    /**
     * Creates and mounts an interactive Sprints Overview chart into a DOM container.
     *
     * @param {HTMLElement} container   Host element
     * @param {Object}      data        SprintOverviewDto JSON
     * @param {Object}      [options]
     * @param {string}      [options.containerId]
     * @returns {{ render:Function, schedule:Function, destroy:Function }}
     */
    function createChart(container, data, options) {
        options = options || {};
        var containerId = options.containerId || container.id || 'chart';

        var theme = new window.Theme(data.meta.theme);
        var chart = new SprintsOverviewChart(data, theme);
        var renderer = chart.renderers[0];

        var dayWidth = DEFAULT_DW;
        var scrollOffset = 0;

        function getContainerWidth() {
            return Math.max(200, container.clientWidth || 800);
        }

        function constrainScrollOffset() {
            scrollOffset = Math.max(0, Math.min(
                Math.max(0, renderer.totalDays - getContainerWidth() / dayWidth),
                scrollOffset
            ));
        }

        var saved = loadViewState(containerId);
        if (saved) {
            dayWidth = Math.min(MAX_DW, Math.max(MIN_DW, saved.dayWidth));
            scrollOffset = saved.scrollOffset;
            constrainScrollOffset();
        } else {
            var todayIdx = calculateDayIndex(renderer.currentDate, renderer.chartStart);
            var visibleDays = getContainerWidth() / dayWidth;
            scrollOffset = Math.max(0, Math.min(renderer.totalDays - visibleDays, todayIdx - visibleDays * 0.3));
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
                var rect = container.getBoundingClientRect();
                var mouseX = (event.clientX != null) ? (event.clientX - rect.left) : (getContainerWidth() / 2);
                var dayUnder = scrollOffset + mouseX / dayWidth;
                var factor = event.deltaY < 0 ? ZOOM_STEP : 1 / ZOOM_STEP;
                dayWidth = Math.max(MIN_DW, Math.min(MAX_DW, dayWidth * factor));
                scrollOffset = dayUnder - mouseX / dayWidth;
                // this.initSize(this.firstDayX, false, window.CalendarSize.YEARS);
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
            if (dragState) {
                dragState = null;
                scheduleSave();
            }
            container.style.cursor = 'grab';
        }

        // Right-click context menu
        function handleContextMenuRequest(event) {
            event.preventDefault();
            hideContextMenu();
            var rect = container.getBoundingClientRect();
            var mouseX = event.clientX - rect.left;
            var mouseY = event.clientY - rect.top;
            var hits = renderer.sprintHitAreas;
            for (var i = 0; i < hits.length; i++) {
                var h = hits[i];
                if (mouseX >= h.x && mouseX <= h.x + h.width && mouseY >= h.y && mouseY <= h.y + h.height) {
                    showContextMenuForSprint(event.clientX, event.clientY, h.sprint);
                    return;
                }
            }
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
            container.removeEventListener('contextmenu', handleContextMenuRequest);
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
        container.addEventListener('contextmenu', handleContextMenuRequest);

        redrawChart();
        return {render: redrawChart, schedule: scheduleRender, destroy: cleanupChart};
    }

    // ── Public mount API ───────────────────────────────────────────────────────

    /**
     * Mounts (or remounts) the Sprints Overview chart into a named container.
     * Called by SprintListView.refreshClientChart() via Vaadin executeJs.
     *
     * @param {string} containerId   DOM element ID of the chart container
     * @param {Object} injectedData  SprintOverviewDto JSON
     */
    function mountSprintsOverviewChart(containerId, injectedData) {
        var elementId = containerId || 'sprints-overview-chart-container';
        var containerElement = document.getElementById(elementId);
        if (!containerElement) return;

        if (currentChartInstance && typeof currentChartInstance.destroy === 'function') {
            currentChartInstance.destroy();
            currentChartInstance = null;
        }

        if (injectedData) {
            currentChartInstance = createChart(containerElement, injectedData, {containerId: elementId});
        } else {
            containerElement.innerHTML = '<div style="padding:16px;color:red;font-family:sans-serif;">No chart data provided.</div>';
        }
    }

    // ── Exports ────────────────────────────────────────────────────────────────

    window.mountSprintsOverviewChart = mountSprintsOverviewChart;
    window.createSprintsOverviewChart = createChart;
    window.SprintsOverviewRenderer = SprintsOverviewRenderer;
    window.SprintsOverviewChart = SprintsOverviewChart;
})();


