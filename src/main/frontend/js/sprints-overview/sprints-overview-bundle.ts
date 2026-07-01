// sprints-overview/sprints-overview-bundle.ts
// Entry point for the Sprints Overview chart bundle.
// Exposes window.mountSprintsOverviewChart for Java interop via Vaadin executeJs.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {Theme} from '../theme/theme.js';
import {calculateDayIndex} from '../date-utils.js';
import {SprintsOverviewChart} from './sprints-overview-chart.js';
import {
    DEFAULT_DW,
    HitArea,
    MAX_DW,
    MIN_DW,
    SprintOverviewDto,
    SprintsOverviewRenderer,
    ZOOM_STEP
} from './sprints-overview-renderer.js';
import {hideContextMenu, showContextMenuForSprint} from './context-menu.js';

// ── localStorage helpers ────────────────────────────────────────────────────

function viewStateKey(containerId: string): string {
    return 'kassandra.chart.' + containerId.replace(/-container$/, '') + '.view';
}

interface ViewState {
    dayWidth: number;
    scrollOffset: number;
}

function loadViewState(containerId: string): ViewState | null {
    try {
        const raw = localStorage.getItem(viewStateKey(containerId));
        if (raw) {
            const s = JSON.parse(raw) as ViewState;
            if (typeof s.dayWidth === 'number' && typeof s.scrollOffset === 'number') return s;
        }
    } catch { /* unavailable */
    }
    return null;
}

function saveViewState(containerId: string, dayWidth: number, scrollOffset: number): void {
    try {
        localStorage.setItem(viewStateKey(containerId), JSON.stringify({dayWidth, scrollOffset}));
    } catch { /* quota */
    }
}

// ── Chart factory ───────────────────────────────────────────────────────────

interface ChartHandle {
    render(): void;

    schedule(): void;

    destroy(): void;
}

let currentChartInstance: ChartHandle | null = null;

function createChart(
    container: HTMLElement,
    data: SprintOverviewDto,
    options: { containerId?: string } = {},
): ChartHandle {
    const containerId = options.containerId || container.id || 'chart';
    const theme = new Theme(data.meta.theme as Record<string, unknown>);
    const chart = new SprintsOverviewChart(data, theme);
    const renderer = chart.renderers[0] as SprintsOverviewRenderer;

    let dayWidth = DEFAULT_DW;
    let scrollOffset = 0;

    function getContainerWidth() {
        return Math.max(200, container.clientWidth || 800);
    }

    function constrainScrollOffset() {
        scrollOffset = Math.max(0, Math.min(
            Math.max(0, renderer.totalDays - getContainerWidth() / dayWidth),
            scrollOffset,
        ));
    }

    const saved = loadViewState(containerId);
    if (saved) {
        dayWidth = Math.min(MAX_DW, Math.max(MIN_DW, saved.dayWidth));
        scrollOffset = saved.scrollOffset;
        constrainScrollOffset();
    } else {
        const todayIdx = calculateDayIndex(renderer.currentDate, renderer.chartStart);
        const visibleDays = getContainerWidth() / dayWidth;
        scrollOffset = Math.max(0, Math.min(renderer.totalDays - visibleDays, todayIdx - visibleDays * 0.3));
    }

    let saveTimerId: ReturnType<typeof setTimeout> | null = null;

    function scheduleSave() {
        if (saveTimerId) clearTimeout(saveTimerId);
        saveTimerId = setTimeout(() => saveViewState(containerId, dayWidth, scrollOffset), 250);
    }

    let animationFrameId: number | null = null;

    function redrawChart() {
        chart.updateViewState(dayWidth, scrollOffset, getContainerWidth());
        chart.render(container);
    }

    function scheduleRender() {
        if (animationFrameId) cancelAnimationFrame(animationFrameId);
        animationFrameId = requestAnimationFrame(redrawChart);
    }

    function handleWheelEvent(e: WheelEvent) {
        e.preventDefault();
        if (e.deltaX !== 0) {
            scrollOffset += e.deltaX / dayWidth;
        } else {
            const rect = container.getBoundingClientRect();
            const mouseX = e.clientX != null ? e.clientX - rect.left : getContainerWidth() / 2;
            const dayUnder = scrollOffset + mouseX / dayWidth;
            const factor = e.deltaY < 0 ? ZOOM_STEP : 1 / ZOOM_STEP;
            dayWidth = Math.max(MIN_DW, Math.min(MAX_DW, dayWidth * factor));
            scrollOffset = dayUnder - mouseX / dayWidth;
        }
        constrainScrollOffset();
        scheduleRender();
        scheduleSave();
    }

    let dragState: { startX: number; startOffset: number } | null = null;

    function handlePointerDown(e: PointerEvent) {
        if (e.button !== 0) return;
        dragState = {startX: e.clientX, startOffset: scrollOffset};
        container.setPointerCapture(e.pointerId);
        container.style.cursor = 'grabbing';
        e.preventDefault();
    }

    function handlePointerMove(e: PointerEvent) {
        if (!dragState) return;
        scrollOffset = dragState.startOffset - (e.clientX - dragState.startX) / dayWidth;
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

    function handleContextMenuRequest(e: MouseEvent) {
        e.preventDefault();
        hideContextMenu();
        const rect = container.getBoundingClientRect();
        const mouseX = e.clientX - rect.left;
        const mouseY = e.clientY - rect.top;
        for (const h of renderer.sprintHitAreas as HitArea[]) {
            if (mouseX >= h.x && mouseX <= h.x + h.width && mouseY >= h.y && mouseY <= h.y + h.height) {
                showContextMenuForSprint(e.clientX, e.clientY, h.sprint);
                return;
            }
        }
    }

    let resizeObserver: ResizeObserver | null = null;
    if (typeof ResizeObserver !== 'undefined') {
        resizeObserver = new ResizeObserver(scheduleRender);
        resizeObserver.observe(container);
    }

    function cleanupChart() {
        container.removeEventListener('wheel', handleWheelEvent as EventListener);
        container.removeEventListener('pointerdown', handlePointerDown as EventListener);
        container.removeEventListener('pointermove', handlePointerMove as EventListener);
        container.removeEventListener('pointerup', handlePointerUp);
        container.removeEventListener('pointercancel', handlePointerUp);
        container.removeEventListener('contextmenu', handleContextMenuRequest as EventListener);
        resizeObserver?.disconnect();
        if (animationFrameId) cancelAnimationFrame(animationFrameId);
        if (saveTimerId) clearTimeout(saveTimerId);
        container.innerHTML = '';
    }

    container.style.cursor = 'grab';
    container.addEventListener('wheel', handleWheelEvent as EventListener, {passive: false});
    container.addEventListener('pointerdown', handlePointerDown as EventListener, {passive: false});
    container.addEventListener('pointermove', handlePointerMove as EventListener, {passive: true});
    container.addEventListener('pointerup', handlePointerUp);
    container.addEventListener('pointercancel', handlePointerUp);
    container.addEventListener('contextmenu', handleContextMenuRequest as EventListener);

    redrawChart();
    return {render: redrawChart, schedule: scheduleRender, destroy: cleanupChart};
}

// ── Public mount API (called by SprintListView via Vaadin executeJs) ────────

function mountSprintsOverviewChart(containerId: string, injectedData: SprintOverviewDto): void {
    const elementId = containerId || 'sprints-overview-chart-container';
    const containerElement = document.getElementById(elementId);
    if (!containerElement) return;

    currentChartInstance?.destroy();
    currentChartInstance = null;

    if (injectedData) {
        currentChartInstance = createChart(containerElement, injectedData, {containerId: elementId});
    } else {
        containerElement.innerHTML = '<div style="padding:16px;color:red;font-family:sans-serif;">No chart data provided.</div>';
    }
}

// ── Expose global for Java interop ──────────────────────────────────────────
declare global {
    interface Window {
        mountSprintsOverviewChart: typeof mountSprintsOverviewChart;
        createSprintsOverviewChart: typeof createChart;
    }
}
window.mountSprintsOverviewChart = mountSprintsOverviewChart;
window.createSprintsOverviewChart = createChart;

