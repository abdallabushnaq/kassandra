// gantt/gantt-bundle.ts
// Entry point for the Gantt chart bundle.
// Exposes window.mountGanttChart for Java interop via Vaadin executeJs.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import { Theme }                                    from '../theme/theme.js';
import { calculateDayIndex }                        from '../date-utils.js';
import { GanttChart }                               from './gantt-chart-class.js';
import { GanttRenderer, GanttChartDto }             from './gantt-renderer.js';
import { DEFAULT_DW, MIN_DW, MAX_DW, ZOOM_STEP }   from './abstract-gantt-renderer.js';

// ── localStorage helpers ────────────────────────────────────────────────────

function viewStateKey(containerId: string): string {
    return 'kassandra.chart.' + containerId.replace(/-container$/, '') + '.view';
}

interface ViewState { dayWidth: number; scrollOffset: number; }

function loadViewState(containerId: string): ViewState | null {
    try {
        const raw = localStorage.getItem(viewStateKey(containerId));
        if (raw) {
            const s = JSON.parse(raw) as ViewState;
            if (typeof s.dayWidth === 'number' && typeof s.scrollOffset === 'number') return s;
        }
    } catch { /* unavailable */ }
    return null;
}

function saveViewState(containerId: string, dayWidth: number, scrollOffset: number): void {
    try { localStorage.setItem(viewStateKey(containerId), JSON.stringify({ dayWidth, scrollOffset })); }
    catch { /* quota */ }
}

// ── Chart factory ───────────────────────────────────────────────────────────

interface ChartHandle { render(): void; schedule(): void; destroy(): void; }

let currentGanttChartInstance: ChartHandle | null = null;

function createChart(
    container: HTMLElement,
    data:      GanttChartDto,
    options:   { containerId?: string } = {},
): ChartHandle {
    const containerId = options.containerId || container.id || 'chart';
    const theme       = new Theme(data.meta.theme as Record<string, unknown>);
    const chart       = new GanttChart(data, theme);
    const renderer    = chart.renderers[0] as GanttRenderer;

    let dayWidth     = DEFAULT_DW;
    let scrollOffset = 0;

    function getContainerWidth() { return Math.max(200, container.clientWidth || 800); }

    function constrainScrollOffset() {
        scrollOffset = Math.max(0, Math.min(
            Math.max(0, renderer.totalDays - getContainerWidth() / dayWidth),
            scrollOffset,
        ));
    }

    const saved = loadViewState(containerId);
    if (saved) {
        dayWidth     = Math.min(MAX_DW, Math.max(MIN_DW, saved.dayWidth));
        scrollOffset = saved.scrollOffset;
        constrainScrollOffset();
    } else {
        const todayIdx    = calculateDayIndex(renderer.currentDate!, renderer.chartStart!);
        const visibleDays = getContainerWidth() / dayWidth;
        scrollOffset = Math.max(0, Math.min(renderer.totalDays - visibleDays, todayIdx - visibleDays * 0.2));
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
            const rect    = container.getBoundingClientRect();
            const mouseX  = e.clientX != null ? e.clientX - rect.left : getContainerWidth() / 2;
            const dayUnder = scrollOffset + mouseX / dayWidth;
            const factor  = e.deltaY < 0 ? ZOOM_STEP : 1 / ZOOM_STEP;
            dayWidth      = Math.max(MIN_DW, Math.min(MAX_DW, dayWidth * factor));
            scrollOffset  = dayUnder - mouseX / dayWidth;
        }
        constrainScrollOffset();
        scheduleRender();
        scheduleSave();
    }

    let dragState: { startX: number; startOffset: number } | null = null;

    function handlePointerDown(e: PointerEvent) {
        if (e.button !== 0) return;
        dragState = { startX: e.clientX, startOffset: scrollOffset };
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
        if (dragState) { dragState = null; scheduleSave(); }
        container.style.cursor = 'grab';
    }

    let resizeObserver: ResizeObserver | null = null;
    if (typeof ResizeObserver !== 'undefined') {
        resizeObserver = new ResizeObserver(scheduleRender);
        resizeObserver.observe(container);
    }

    function cleanupChart() {
        container.removeEventListener('wheel',         handleWheelEvent  as EventListener);
        container.removeEventListener('pointerdown',   handlePointerDown as EventListener);
        container.removeEventListener('pointermove',   handlePointerMove as EventListener);
        container.removeEventListener('pointerup',     handlePointerUp);
        container.removeEventListener('pointercancel', handlePointerUp);
        resizeObserver?.disconnect();
        if (animationFrameId) cancelAnimationFrame(animationFrameId);
        if (saveTimerId)      clearTimeout(saveTimerId);
        container.innerHTML = '';
    }

    container.style.cursor = 'grab';
    container.addEventListener('wheel',         handleWheelEvent  as EventListener, { passive: false });
    container.addEventListener('pointerdown',   handlePointerDown as EventListener, { passive: false });
    container.addEventListener('pointermove',   handlePointerMove as EventListener, { passive: true  });
    container.addEventListener('pointerup',     handlePointerUp);
    container.addEventListener('pointercancel', handlePointerUp);

    redrawChart();
    return { render: redrawChart, schedule: scheduleRender, destroy: cleanupChart };
}

// ── Public mount API (called by Backlog.java via Vaadin executeJs) ───────────

function mountGanttChart(containerId: string, injectedData: GanttChartDto): void {
    const elementId        = containerId || 'gantt-chart-container';
    const containerElement = document.getElementById(elementId);
    if (!containerElement) return;

    currentGanttChartInstance?.destroy();
    currentGanttChartInstance = null;

    if (injectedData) {
        currentGanttChartInstance = createChart(containerElement, injectedData, { containerId: elementId });
    } else {
        containerElement.innerHTML = '<div style="padding:16px;color:red;font-family:sans-serif;">No Gantt chart data provided.</div>';
    }
}

// ── Expose globals for Java interop ─────────────────────────────────────────
declare global {
    interface Window {
        mountGanttChart:  typeof mountGanttChart;
        createGanttChart: typeof createChart;
    }
}
window.mountGanttChart  = mountGanttChart;
window.createGanttChart = createChart;

