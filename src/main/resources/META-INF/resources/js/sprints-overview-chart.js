// sprints-overview-v3.js
// Virtual-canvas Sprints Overview chart.
// Depends on: svg-utils.js, color-utils.js, date-utils.js, CalendarXAxes.js, theme-color-constants.js
//
// Interaction:
//   plain wheel      → zoom (change dayWidth, keep day-under-cursor stable)
//   horizontal swipe → pan (trackpad)
//   right-click      → context menu with Backlog / Active Sprint / Quality Board links
//
// View state (dayWidth + scrollOffset) is persisted in localStorage using the key
//   kassandra.chart.<chartName>.view
// so each chart instance keeps its own position across theme toggles and page reloads.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0
(function () {
    'use strict';

    // Destructure utilities from global scope
    const {createSvgElement, createRect, createText, createLine, createClipPath} = window.SvgUtils;
    const {getThemeColor, convertSprintColorToRgba} = window.ColorUtils;
    const {MS, getUtcDayMidnight, calculateDayIndex, calculateDayCount} = window.DateUtils;
    const colorKeys = window.ThemeColorKeys;

    // ── Java SprintsOverviewRenderer constants (numberOfLines = 3) ────────────
    const LINE_HEIGHT = 13;
    const SPRINT_H = LINE_HEIGHT * 3;        // 39 px – height of sprint rect
    const TASK_H = LINE_HEIGHT * 3 + 17;   // 56 px – getTaskHeight()
    const LANE_H = TASK_H + 2;             // 58 px – lane spacing

    // ── Threshold below which per-day stripes become week-boundary lines ──────
    const MIN_DAY_BARS = 4;   // Java: isDayBarsVisible() = dayWidth >= 4

    // ── Zoom / scroll config ──────────────────────────────────────────────────
    const DEFAULT_DW = 8;     // matches Java CalendarXAxes.calculateDayWidth() → 8
    const MIN_DW = 1;
    const MAX_DW = 80;
    const ZOOM_STEP = 1.25;


    // ── localStorage view-state helpers ───────────────────────────────────────
    // Key format: kassandra.chart.<chartName>.view
    // where <chartName> is derived from the container ID by stripping a trailing "-container".
    // Example: "sprints-overview-v3-container" → "kassandra.chart.sprints-overview-v3.view"

    /**
     * Generates the localStorage key for storing chart view state (zoom and scroll position).
     * @param {string} containerId The DOM element ID of the chart container
     * @returns {string} The localStorage key for this chart instance
     */
    function generateViewStateKey(containerId) {
        const chartName = (containerId || 'chart').replace(/-container$/, '');
        return 'kassandra.chart.' + chartName + '.view';
    }

    /**
     * Loads the saved view state (dayWidth and scrollOffset) from localStorage.
     * @param {string} containerId The DOM element ID of the chart container
     * @returns {?Object} Object with dayWidth and scrollOffset properties, or null if not saved
     */
    function loadChartViewState(containerId) {
        try {
            const rawData = localStorage.getItem(generateViewStateKey(containerId));
            if (rawData) {
                const state = JSON.parse(rawData);
                if (typeof state.dayWidth === 'number' && typeof state.scrollOffset === 'number') return state;
            }
        } catch (error) { /* storage may be unavailable */
        }
        return null;
    }

    /**
     * Saves the current view state (dayWidth and scrollOffset) to localStorage.
     * Silently fails if storage is unavailable or quota exceeded.
     * @param {string} containerId The DOM element ID of the chart container
     * @param {number} dayWidth Current pixel width per day
     * @param {number} scrollOffset Current scroll position (first visible day index)
     */
    function saveChartViewState(containerId, dayWidth, scrollOffset) {
        try {
            localStorage.setItem(generateViewStateKey(containerId), JSON.stringify({dayWidth, scrollOffset}));
        } catch (error) { /* quota exceeded or restricted – silently ignore */
        }
    }

    // ── Context menu (one singleton per page) ─────────────────────────────────

    /**
     * Global context menu DOM element (singleton, created on first use).
     * @type {?HTMLDivElement}
     */
    var singletoneContextMenu = null;

    /**
     * Initializes the context menu DOM element if not already created.
     * Creates a single floating menu element on first call; subsequent calls return the existing element.
     * The menu includes click-away and keyboard dismissal.
     * @returns {HTMLDivElement} The context menu DOM element
     */
    function initializeContextMenuSingleton() {
        if (singletoneContextMenu) return singletoneContextMenu;
        const menuElement = document.createElement('div');
        menuElement.style.cssText = [
            'position:fixed',
            'z-index:99999',
            'background:var(--lumo-base-color,#fff)',
            'color:var(--lumo-body-text-color,#333)',
            'border:1px solid var(--lumo-contrast-20pct,#ccc)',
            'border-radius:var(--lumo-border-radius-m,4px)',
            'box-shadow:0 4px 16px rgba(0,0,0,.18)',
            'padding:4px 0',
            'font-family:var(--lumo-font-family,Arial,sans-serif)',
            'font-size:13px',
            'min-width:168px',
            'display:none',
            'user-select:none',
        ].join(';');
        document.body.appendChild(menuElement);
        singletoneContextMenu = menuElement;

        // Dismiss on any click outside the menu
        document.addEventListener('click', function (event) {
            if (!singletoneContextMenu.contains(event.target)) hideContextMenuSingleton();
        });

        // Dismiss on scroll
        document.addEventListener('scroll', hideContextMenuSingleton, true);

        // Dismiss on Escape key
        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape') hideContextMenuSingleton();
        });

        return menuElement;
    }

    /**
     * Hides the context menu by setting display to 'none'.
     */
    function hideContextMenuSingleton() {
        if (singletoneContextMenu) singletoneContextMenu.style.display = 'none';
    }

    /**
     * Displays the context menu at the specified screen position with sprint-specific options.
     * Menu includes navigation links to Backlog, Active Sprint, and Quality Board views.
     * @param {number} clientX Screen X coordinate where the menu should appear
     * @param {number} clientY Screen Y coordinate where the menu should appear
     * @param {Object} sprint Sprint object with id, name, and navigation properties
     */
    function showContextMenuForSprint(clientX, clientY, sprint) {
        const menuElement = initializeContextMenuSingleton();
        menuElement.innerHTML = '';

        const menuItems = [
            {label: '\u{1F4CB} Backlog', url: 'backlog?sprint=' + sprint.id},
            {label: '\u25B6 Active Sprint', url: 'active-sprints?sprints=' + sprint.id},
            {label: '\u{1F4CB} Quality Board', url: 'quality-board?sprint=' + sprint.id},
        ];

        // Optional header showing sprint name
        const headerElement = document.createElement('div');
        headerElement.textContent = sprint.name || 'Sprint';
        headerElement.style.cssText = [
            'padding:4px 14px 4px 14px',
            'font-weight:bold',
            'font-size:12px',
            'color:var(--lumo-secondary-text-color,#666)',
            'border-bottom:1px solid var(--lumo-contrast-10pct,#eee)',
            'margin-bottom:4px',
            'white-space:nowrap',
            'overflow:hidden',
            'text-overflow:ellipsis',
            'max-width:220px',
        ].join(';');
        menuElement.appendChild(headerElement);

        // Add menu items
        menuItems.forEach(function (item) {
            const rowElement = document.createElement('div');
            rowElement.textContent = item.label;
            rowElement.style.cssText = [
                'padding:6px 16px',
                'cursor:pointer',
                'white-space:nowrap',
                'transition:background 0.1s',
            ].join(';');

            rowElement.addEventListener('mouseenter', function () {
                rowElement.style.background = 'var(--lumo-primary-color-10pct,#e8f0fe)';
            });

            rowElement.addEventListener('mouseleave', function () {
                rowElement.style.background = '';
            });

            rowElement.addEventListener('click', function (event) {
                event.stopPropagation();
                hideContextMenuSingleton();
                window.location.href = item.url;
            });

            menuElement.appendChild(rowElement);
        });

        // Position: prefer right-and-below cursor; flip if it would overflow viewport
        menuElement.style.display = 'block';
        const menuWidth = menuElement.offsetWidth || 168;
        const menuHeight = menuElement.offsetHeight || 120;
        const viewportWidth = window.innerWidth;
        const viewportHeight = window.innerHeight;
        const leftPosition = (clientX + menuWidth + 2 > viewportWidth) ? (clientX - menuWidth) : (clientX + 2);
        const topPosition = (clientY + menuHeight + 2 > viewportHeight) ? (clientY - menuHeight) : (clientY + 2);
        menuElement.style.left = Math.max(0, leftPosition) + 'px';
        menuElement.style.top = Math.max(0, topPosition) + 'px';
    }

    // ── Chart factory ─────────────────────────────────────────────────────────

    /**
     * Creates and renders a sprints overview interactive chart.
     * The chart displays project sprints across a timeline with interactive zoom and pan controls.
     * View state (dayWidth and scrollOffset) is automatically persisted to localStorage and restored
     * on subsequent instantiations.
     *
     * Interaction:
     * - Mouse wheel (vertical) → zoom in/out (maintains day under cursor)
     * - Trackpad swipe (horizontal) → pan left/right
     * - Click and drag → pan left/right
     * - Right-click on sprint → context menu with navigation links
     * - Container resize → automatic redraw
     *
     * @param {HTMLElement} container DOM container element where the chart will be rendered
     * @param {Object} data Chart data object with meta (configuration) and lanes (sprint rows)
     * @param {Object} options Optional configuration object
     * @param {string} options.containerId Unique identifier for this chart instance (used for localStorage)
     * @param {number} options.initialDayWidth Initial pixel width per day
     * @returns {Object} Control API with render, schedule, and destroy methods
     */
    function createChart(container, data, options) {
        options = options || {};
        const containerId = options.containerId || container.id || 'chart';
        const metadata = data.meta || {};
        const lanes = data.lanes || [];
        const theme = metadata.xAxesTheme || {};
        const chartStart = getUtcDayMidnight(new Date(metadata.chartStart || Date.now()));
        const chartEnd = getUtcDayMidnight(new Date(metadata.chartEnd || Date.now()));
        const currentDate = metadata.now ? getUtcDayMidnight(new Date(metadata.now)) : getUtcDayMidnight(new Date());
        const totalDays = calculateDayCount(chartStart, chartEnd);

        // CalendarXAxes class from CalendarXAxes.js
        const calendar = new window.CalendarXAxes(theme);

        let dayWidth = Math.min(MAX_DW, Math.max(MIN_DW, options.initialDayWidth || DEFAULT_DW));
        let scrollOffset = 0;  // First visible day index (fractional, zero-based)

        // Hit areas for context menu – rebuilt on every render
        let sprintHitAreas = [];  // [{sprint, x, y, width, height}]

        // Helper: Convert day index to viewport pixel X coordinate
        const dayIndexToPixelX = (dayIndex) => (dayIndex - scrollOffset) * dayWidth;

        // Helper: Get current container width
        const getContainerWidth = () => Math.max(200, container.clientWidth || 800);

        // Helper: Get calendar header height
        const getCalendarHeight = () => calendar.getHeight(dayWidth);

        // ── Restore or initialize scroll/zoom position ────────────────────────
        /**
         * Initializes scroll position and zoom level from saved state or defaults.
         * If saved state exists, restores dayWidth and scrollOffset.
         * Otherwise, initializes with "today" at 30% from the left edge.
         */
        function initializeScroll() {
            const savedState = loadChartViewState(containerId);
            if (savedState) {
                dayWidth = Math.min(MAX_DW, Math.max(MIN_DW, savedState.dayWidth));
                scrollOffset = savedState.scrollOffset;
                constrainScrollOffset();
            } else {
                // Default: put "today" at 30% from the left
                const todayIndex = calculateDayIndex(currentDate, chartStart);
                const visibleDays = getContainerWidth() / dayWidth;
                scrollOffset = Math.max(0, Math.min(totalDays - visibleDays, todayIndex - visibleDays * 0.3));
            }
        }

        // ── Debounced save after interactions ──────────────────────────────────
        let saveTimerId = null;

        /**
         * Schedules a deferred chart state save (debounced 250ms).
         */
        function scheduleSave() {
            if (saveTimerId) clearTimeout(saveTimerId);
            saveTimerId = setTimeout(function () {
                saveChartViewState(containerId, dayWidth, scrollOffset);
            }, 250);
        }

        // ── Render individual chart layers ─────────────────────────────────────

        /**
         * Renders weekend stripes and/or week boundary lines below sprints.
         * At high dayWidth (>= MIN_DAY_BARS), draws per-day colored stripes for weekends.
         * At low dayWidth (< MIN_DAY_BARS), draws thin lines at week boundaries (Mondays).
         * @param {number} baseY Y position of the stripes layer
         * @param {number} baseHeight Height of the stripes layer
         * @returns {SVGGElement} Group element containing stripes
         */
        function renderWeekendStripes(baseY, baseHeight) {
            const stripesGroup = createSvgElement('g', {'class': 'stripes'});
            const containerWidth = getContainerWidth();
            const saturdayBgColor = getThemeColor(theme, colorKeys.XAXES_DAY_OF_WEEK_SATURDAY_BG_COLOR);
            const sundayBgColor = getThemeColor(theme, colorKeys.XAXES_DAY_OF_WEEK_SUNDAY_BG_COLOR);

            if (dayWidth >= MIN_DAY_BARS) {
                // Per-day colored stripes (weekends colored, weekdays transparent)
                const firstVisibleDay = Math.max(0, Math.floor(scrollOffset) - 1);
                const lastVisibleDay = Math.min(totalDays - 1, firstVisibleDay + Math.ceil(containerWidth / dayWidth) + 2);
                for (let dayIdx = firstVisibleDay; dayIdx <= lastVisibleDay; dayIdx++) {
                    const dayOfWeek = new Date(chartStart.getTime() + dayIdx * MS).getUTCDay();
                    const bgColor = dayOfWeek === 6 ? saturdayBgColor : (dayOfWeek === 0 ? sundayBgColor : null);
                    if (!bgColor) continue;
                    const xPos = dayIndexToPixelX(dayIdx);
                    if (xPos + dayWidth < 0 || xPos > containerWidth) continue;
                    stripesGroup.appendChild(createRect(xPos, baseY, dayWidth, baseHeight, {fill: bgColor}));
                }
            } else {
                // Week-boundary lines only (one Monday line per week)
                const gridColor = getThemeColor(theme, colorKeys.GANTT_GRID_COLOR);
                const firstVisibleDay = Math.max(0, Math.floor(scrollOffset));
                const lastVisibleDay = Math.min(totalDays - 1, firstVisibleDay + Math.ceil(containerWidth / dayWidth) + 8);
                for (let dayIdx = firstVisibleDay; dayIdx <= lastVisibleDay; dayIdx++) {
                    if (new Date(chartStart.getTime() + dayIdx * MS).getUTCDay() !== 1) continue;
                    const xPos = dayIndexToPixelX(dayIdx);
                    if (xPos < 0 || xPos > containerWidth) continue;
                    stripesGroup.appendChild(createLine(xPos, baseY, xPos, baseY + baseHeight, {
                        stroke: gridColor,
                        'stroke-width': '1'
                    }));
                }
            }
            return stripesGroup;
        }

        /**
         * Renders vertical grid lines for each day (when dayWidth >= MIN_DAY_BARS).
         * @param {number} baseY Y position of the grid layer
         * @param {number} baseHeight Height of the grid layer
         * @returns {SVGGElement} Group element containing grid lines
         */
        function renderVerticalGridLines(baseY, baseHeight) {
            const gridGroup = createSvgElement('g', {'class': 'grid'});
            if (dayWidth < MIN_DAY_BARS) return gridGroup;

            const gridColor = getThemeColor(theme, colorKeys.GANTT_GRID_COLOR);
            const containerWidth = getContainerWidth();
            const firstVisibleDay = Math.max(0, Math.floor(scrollOffset) - 1);
            const lastVisibleDay = Math.min(totalDays, firstVisibleDay + Math.ceil(containerWidth / dayWidth) + 2);

            for (let dayIdx = firstVisibleDay; dayIdx <= lastVisibleDay; dayIdx++) {
                const xPos = dayIndexToPixelX(dayIdx);
                if (xPos < 0 || xPos > containerWidth) continue;
                gridGroup.appendChild(createLine(xPos, baseY, xPos, baseY + baseHeight, {
                    stroke: gridColor,
                    'stroke-width': '1'
                }));
            }
            return gridGroup;
        }

        /**
         * Renders sprint rectangles and labels across all lanes.
         * Builds hit areas for context menu interaction.
         * @param {number} baseY Y position where sprints start (below calendar header)
         * @returns {SVGGElement} Group element containing all sprint rectangles
         */
        function renderSprintRectangles(baseY) {
            sprintHitAreas = [];  // Rebuild on every render
            const sprintsGroup = createSvgElement('g', {'class': 'sprints'});
            const containerWidth = getContainerWidth();

            lanes.forEach((lane, laneIndex) => {
                const laneY = baseY + laneIndex * LANE_H;
                (lane.sprints || []).forEach((sprint) => {
                    if (!sprint.start || !sprint.end) return;

                    const startIndex = calculateDayIndex(sprint.start, chartStart);
                    const endIndex = calculateDayIndex(sprint.end, chartStart);

                    // Sprint rect: starts at left edge of start-day, ends 1px before right edge of end-day
                    const sprintX = dayIndexToPixelX(startIndex);
                    const sprintWidth = (endIndex - startIndex + 1) * dayWidth - 1;

                    if (sprintX + sprintWidth < 0 || sprintX > containerWidth) return;

                    // Register hit area for context menu
                    sprintHitAreas.push({
                        sprint,
                        x: sprintX,
                        y: laneY,
                        width: Math.max(0, sprintWidth),
                        height: SPRINT_H
                    });

                    // Draw sprint rectangle
                    const rectElement = createRect(sprintX, laneY, sprintWidth, SPRINT_H, {
                        fill: convertSprintColorToRgba(sprint.color)
                    });
                    rectElement.appendChild(createSvgElement('title', {}, buildSprintTooltip(sprint)));
                    sprintsGroup.appendChild(rectElement);

                    // Draw sprint name label (if wide enough)
                    if (sprintWidth > 20) {
                        const clipPathId = 'sp' + laneIndex + '_' + sprint.id;
                        sprintsGroup.appendChild(createClipPath(clipPathId, sprintX, laneY, sprintWidth, SPRINT_H));
                        // Java: drawString at x1+1, y1+(0+1)*LINE_HEIGHT-2 = y1+11
                        sprintsGroup.appendChild(createText(sprintX + 1, laneY + LINE_HEIGHT - 2, sprint.name || '', {
                            fill: '#000000',
                            'font-size': '12',
                            'font-family': 'Arial,sans-serif',
                            'font-weight': 'bold',
                            'clip-path': 'url(#' + clipPathId + ')'
                        }));
                    }
                });
            });

            return sprintsGroup;
        }

        /**
         * Constructs a multi-line tooltip text for a sprint.
         * Includes key, status, start/end dates, and delay flag if applicable.
         * @param {Object} sprint Sprint object with name, key, status, start, end, delay properties
         * @returns {string} Multi-line tooltip text
         */
        function buildSprintTooltip(sprint) {
            let tooltip = sprint.name || '';
            if (sprint.key) tooltip += '\nKey: ' + sprint.key;
            if (sprint.status) tooltip += '\nStatus: ' + sprint.status;
            if (sprint.start) tooltip += '\nStart: ' + new Date(sprint.start).toLocaleDateString();
            if (sprint.end) tooltip += '\nEnd:   ' + new Date(sprint.end).toLocaleDateString();
            if (sprint.delay) tooltip += '\n(DELAYED)';
            return tooltip;
        }

        /**
         * Renders the "today" marker line (2px dark red line at current date).
         * @param {number} chartHeight Total chart height
         * @returns {SVGGElement} Group element containing the now line
         */
        function renderCurrentDateLine(chartHeight) {
            const nowLineGroup = createSvgElement('g', {'class': 'now-line'});
            const containerWidth = getContainerWidth();

            // Java: center of day cell = dayLeft + dayWidth/2
            const xPos = dayIndexToPixelX(calculateDayIndex(currentDate, chartStart)) + dayWidth / 2;
            if (xPos < 0 || xPos > containerWidth) return nowLineGroup;

            nowLineGroup.appendChild(createLine(xPos, 0, xPos, chartHeight, {
                stroke: '#cc0000',
                'stroke-width': '2'
            }));
            return nowLineGroup;
        }

        // ── Full chart redraw ──────────────────────────────────────────────────

        let animationFrameId = null;

        /**
         * Redraw the entire chart (internal render loop).
         * Clears the container and recreates all SVG elements: stripes, grid, sprints, calendar, now-line.
         * Called by schedule() via requestAnimationFrame.
         */
        function redrawChart() {
            const containerWidth = getContainerWidth();
            const calendarHeight = getCalendarHeight();
            const sprintsBaseHeight = lanes.length * LANE_H + 8;
            const totalHeight = calendarHeight + sprintsBaseHeight;

            const svg = createSvgElement('svg', {
                width: containerWidth,
                height: totalHeight,
                style: 'display:block;user-select:none;shape-rendering: crispEdges'
            });

            // Draw order (bottom to top): stripes → grid → sprints → calendar header → now-line
            svg.appendChild(renderWeekendStripes(calendarHeight, sprintsBaseHeight));
            svg.appendChild(renderVerticalGridLines(calendarHeight, sprintsBaseHeight));
            svg.appendChild(renderSprintRectangles(calendarHeight));
            calendar.draw(svg, chartStart, totalDays, dayWidth, scrollOffset, containerWidth);
            svg.appendChild(renderCurrentDateLine(totalHeight));

            container.innerHTML = '';
            container.appendChild(svg);
        }

        /**
         * Schedules a redraw on the next animation frame.
         */
        function scheduleRender() {
            if (animationFrameId) cancelAnimationFrame(animationFrameId);
            animationFrameId = requestAnimationFrame(redrawChart);
        }

        /**
         * Constrains scrollOffset to valid range: [0, totalDays - visibleDays]
         */
        function constrainScrollOffset() {
            scrollOffset = Math.max(0, Math.min(Math.max(0, totalDays - getContainerWidth() / dayWidth), scrollOffset));
        }

        // ── Mouse-wheel and trackpad interaction ───────────────────────────────

        /**
         * Handles mouse wheel events for zoom and trackpad horizontal swipe for pan.
         * - Vertical wheel wheel scroll → zoom in/out (maintains day under cursor)
         * - Horizontal trackpad swipe → pan left/right
         * @param {WheelEvent} event The wheel event
         */
        function handleWheelEvent(event) {
            event.preventDefault();
            if (event.deltaX !== 0) {
                // Horizontal trackpad swipe → pan
                scrollOffset += event.deltaX / dayWidth;
            } else {
                // Vertical wheel → zoom about the cursor position
                const containerRect = container.getBoundingClientRect();
                const mouseX = (event.clientX != null) ? (event.clientX - containerRect.left) : (getContainerWidth() / 2);
                const dayUnderCursor = scrollOffset + mouseX / dayWidth;
                const zoomFactor = event.deltaY < 0 ? ZOOM_STEP : 1 / ZOOM_STEP;
                dayWidth = Math.max(MIN_DW, Math.min(MAX_DW, dayWidth * zoomFactor));
                scrollOffset = dayUnderCursor - mouseX / dayWidth;
            }
            constrainScrollOffset();
            scheduleRender();
            scheduleSave();
        }

        // ── Click-and-drag panning ──────────────────────────────────────────────

        let dragState = null;  // {startX, startOffset} when dragging

        /**
         * Handles pointer down (mouse button pressed or touch start).
         * Initiates drag-to-pan if left mouse button.
         * @param {PointerEvent} event The pointer down event
         */
        function handlePointerDown(event) {
            if (event.button !== 0) return;
            dragState = {startX: event.clientX, startOffset: scrollOffset};
            container.setPointerCapture(event.pointerId);
            container.style.cursor = 'grabbing';
            event.preventDefault();
        }

        /**
         * Handles pointer move (mouse or touch motion).
         * Updates scroll position during drag-to-pan.
         * @param {PointerEvent} event The pointer move event
         */
        function handlePointerMove(event) {
            if (!dragState) return;
            const deltaX = event.clientX - dragState.startX;
            scrollOffset = dragState.startOffset - deltaX / dayWidth;
            constrainScrollOffset();
            scheduleRender();
        }

        /**
         * Handles pointer up (mouse button released or touch end).
         * Ends drag-to-pan and saves view state.
         */
        function handlePointerUp() {
            if (dragState) {
                dragState = null;
                scheduleSave();
            }
            container.style.cursor = 'grab';
        }

        // ── Context menu (right-click on sprint) ────────────────────────────────

        /**
         * Handles right-click events on the chart.
         * If right-click is over a sprint, displays context menu.
         * @param {MouseEvent} event The context menu event
         */
        function handleContextMenuRequest(event) {
            event.preventDefault();
            hideContextMenuSingleton();

            const containerRect = container.getBoundingClientRect();
            const mouseX = event.clientX - containerRect.left;
            const mouseY = event.clientY - containerRect.top;

            // Check hit areas
            for (let i = 0; i < sprintHitAreas.length; i++) {
                const hitArea = sprintHitAreas[i];
                if (mouseX >= hitArea.x && mouseX <= hitArea.x + hitArea.width &&
                    mouseY >= hitArea.y && mouseY <= hitArea.y + hitArea.height) {
                    showContextMenuForSprint(event.clientX, event.clientY, hitArea.sprint);
                    return;
                }
            }
        }

        // ── ResizeObserver: redraw when container resizes ───────────────────────
        let resizeObserver = null;
        if (typeof ResizeObserver !== 'undefined') {
            resizeObserver = new ResizeObserver(scheduleRender);
            resizeObserver.observe(container);
        }

        // ── Cleanup and initialization ─────────────────────────────────────────

        /**
         * Cleans up all event listeners, observers, and timers.
         * Called when destroying the chart instance.
         */
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

        // ── Initialization ─────────────────────────────────────────────────────
        container.style.cursor = 'grab';
        container.addEventListener('wheel', handleWheelEvent, {passive: false});
        container.addEventListener('pointerdown', handlePointerDown, {passive: false});
        container.addEventListener('pointermove', handlePointerMove, {passive: true});
        container.addEventListener('pointerup', handlePointerUp);
        container.addEventListener('pointercancel', handlePointerUp);
        container.addEventListener('contextmenu', handleContextMenuRequest);

        initializeScroll();
        redrawChart();

        // Return control API
        return {
            render: redrawChart,
            schedule: scheduleRender,
            destroy: cleanupChart
        };
    }

    // ── Public mount API ──────────────────────────────────────────────────────

    /**
     * Global chart instance (one per page).
     * Allows cleanup and re-initialization on theme changes or navigation.
     * @type {?Object}
     */
    var currentChartInstance = null;

    /**
     * Mounts or remounts the sprints overview chart to a DOM container.
     * Called by Vaadin on component initialization, theme changes, and navigation.
     * Automatically cleans up any previous instance before creating a new one.
     *
     * Data is always supplied as the second argument by SprintListView.refreshClientChart(),
     * which serializes the SprintOverviewDto server-side. The browser never fetches
     * /api/overview/sprints as that endpoint requires authentication.
     *
     * @param {string} containerId The DOM element ID of the chart container
     * @param {Object} injectedData Chart data object (supplied by server via Vaadin)
     *                              with meta and lanes properties
     */
    function mountSprintsOverviewChart(containerId, injectedData) {
        const elementId = containerId || 'sprints-overview-chart-container';
        const containerElement = document.getElementById(elementId);
        if (!containerElement) return;  // Not in DOM yet; Vaadin will call us again via executeJs

        // Cleanup previous instance
        if (currentChartInstance && typeof currentChartInstance.destroy === 'function') {
            currentChartInstance.destroy();
            currentChartInstance = null;
        }

        // Create new instance if data provided
        if (injectedData) {
            currentChartInstance = createChart(containerElement, injectedData, {containerId: elementId});
        } else {
            containerElement.innerHTML = '<div style="padding:16px;color:red;font-family:sans-serif;">No chart data provided.</div>';
        }
    }

    // Export public API
    window.mountSprintsOverviewChart = mountSprintsOverviewChart;
    window.createSprintsOverviewChart = createChart;  // Used by test page
})();
