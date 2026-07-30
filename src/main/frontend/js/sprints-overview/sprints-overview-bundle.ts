// sprints-overview/sprints-overview-bundle.ts
// Entry point for the Sprints Overview chart bundle.
// Exposes window.mountSprintsOverviewChart for Java interop via Vaadin executeJs.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {Theme} from '../theme/Theme.js';
import {DateUtils} from '../DateUtils.js';
import {SprintsOverviewChart} from './SprintsOverviewChart.js';
import {DEFAULT_DW, MAX_DW, MIN_DW, SprintsOverviewRenderer, ZOOM_STEP} from './SprintsOverviewRenderer.js';
import {HitArea} from './dto/HitArea.js';
import {SprintOverviewDto} from './dto/SprintOverviewDto.js';
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

// ── Visual-zoom constants ───────────────────────────────────────────────────
/** Per-notch scale factor for Ctrl+wheel / trackpad-pinch visual zoom. */
const VISUAL_ZOOM_STEP = 1.1;
const VISUAL_ZOOM_MIN = 0.1;
const VISUAL_ZOOM_MAX = 10.0;

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
    /** scrollOffset value that was in effect when the last full SVG render completed. */
    let renderedScrollOffset = 0;
    /** Timer for debounced lazy full-redraw after translate-only scroll events. */
    let lazyRedrawTimerId: ReturnType<typeof setTimeout> | null = null;
    /** Visual-zoom state — purely presentational, does not affect the data model. */
    let visualScale = 1.0;
    let visualPanX = 0;
    let visualPanY = 0;

    function getContainerWidth() {
        return Math.max(200, container.clientWidth || 800);
    }

    function getContainerHeight() {
        return Math.max(200, container.clientHeight || 600);
    }

    function constrainScrollOffset() {
        scrollOffset = Math.max(0, Math.min(
            Math.max(0, renderer.totalDays - getContainerWidth() / (dayWidth * visualScale)),
            scrollOffset,
        ));
    }

    const saved = loadViewState(containerId);
    if (saved) {
        dayWidth = Math.min(MAX_DW, Math.max(MIN_DW, saved.dayWidth));
        scrollOffset = saved.scrollOffset;
        constrainScrollOffset();
    } else {
        const todayIdx = DateUtils.calculateDayIndex(renderer.currentDate, renderer.chartStart);
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
        renderedScrollOffset = scrollOffset;
        chart.updateViewState(dayWidth, scrollOffset, getContainerWidth(), getContainerHeight());
        chart.render(container);
        applyContentTransform(); // re-apply visual scale after the SVG is rebuilt
    }

    /**
     * Applies the combined content transform to the group element without rebuilding the SVG.
     *
     * The scroll translation is placed *inside* the scale so that the lazy full-redraw never
     * produces a visual jump:  tx = vpX + scale × scrollTxGroup.
     */
    function applyContentTransform() {
        const scrollTxGroup = -(scrollOffset - renderedScrollOffset) * dayWidth;
        const tx = visualPanX + visualScale * scrollTxGroup;
        chart.updateContentTransform(tx, visualPanY, visualScale);
        // Grow/shrink the SVG to fit the scaled content; no vertical clipping.
        chart.updateSvgHeight(chart.chartHeight * visualScale);
    }

    /**
     * Schedules a full SVG redraw 150 ms after the last translate-only scroll event.
     * This corrects viewport-culling gaps that may appear at the scroll edges during fast panning.
     */
    function scheduleLazyRedraw() {
        if (lazyRedrawTimerId)
            clearTimeout(lazyRedrawTimerId);
        lazyRedrawTimerId = setTimeout(() => {
            lazyRedrawTimerId = null;
            scheduleRender();
        }, 150);
    }

    function scheduleRender() {
        if (animationFrameId)
            cancelAnimationFrame(animationFrameId);
        animationFrameId = requestAnimationFrame(redrawChart);
    }

    function handleWheelEvent(e: WheelEvent) {
        e.preventDefault();
        if (e.ctrlKey) {
            // Ctrl+wheel on desktop, or trackpad pinch (fires as ctrlKey=true WheelEvent).
            // Fast path: update the group transform only; lazy redraw corrects culling.
            // Vertical zoom is top-anchored (visualPanY stays 0): the chart grows/shrinks downward.
            const rect = container.getBoundingClientRect();
            const cx = e.clientX - rect.left;
            const delta = e.deltaY < 0 ? VISUAL_ZOOM_STEP : 1 / VISUAL_ZOOM_STEP;
            const newScale = Math.max(VISUAL_ZOOM_MIN, Math.min(VISUAL_ZOOM_MAX, visualScale * delta));
            const d = newScale / visualScale; // actual factor after clamping
            const newVisualPanX = cx * (1 - d) + visualPanX * d;
            visualScale = newScale;
            // Absorb the pan offset into scrollOffset so that visualPanX stays 0.
            // screen_x(D) = visualPanX + visualScale*dayWidth*(D - scrollOffset)
            // Setting visualPanX=0: scrollOffset_new = scrollOffset - newVisualPanX/(scale*dayWidth)
            scrollOffset -= newVisualPanX / (visualScale * dayWidth);
            visualPanX = 0;
            constrainScrollOffset();
            applyContentTransform();
            scheduleLazyRedraw();
        } else if (e.deltaX !== 0) {
            // Horizontal scroll — fast path: translate the group, defer the full redraw.
            scrollOffset += e.deltaX / dayWidth;
            constrainScrollOffset();
            applyContentTransform();
            scheduleLazyRedraw();
            scheduleSave();
        } else {
            // Vertical scroll = dayWidth zoom — always requires a full redraw.
            // Reset visual zoom so pixel positions remain coherent after dayWidth changes.
            const rect = container.getBoundingClientRect();
            const mouseX = e.clientX != null ? e.clientX - rect.left : getContainerWidth() / 2;
            const dayUnder = scrollOffset + mouseX / dayWidth;
            const factor = e.deltaY < 0 ? ZOOM_STEP : 1 / ZOOM_STEP;
            dayWidth = Math.max(MIN_DW, Math.min(MAX_DW, dayWidth * factor));
            scrollOffset = dayUnder - mouseX / dayWidth;
            constrainScrollOffset();
            visualScale = 1.0;
            visualPanX = 0;
            visualPanY = 0;
            scheduleRender();
            scheduleSave();
        }
    }

    let dragState: { startX: number; startOffset: number } | null = null;

    function handlePointerDown(e: PointerEvent) {
        if (e.button !== 0) return;
        dragState = {startX: e.clientX, startOffset: scrollOffset};
        container.setPointerCapture(e.pointerId);
        // container.style.cursor = 'grabbing';
        e.preventDefault();
    }

    function handlePointerMove(e: PointerEvent) {
        if (!dragState) return;
        // Divide by visualScale so dragging always pans 1:1 with screen pixels, regardless of zoom.
        scrollOffset = dragState.startOffset - (e.clientX - dragState.startX) / (dayWidth * visualScale);
        constrainScrollOffset();
        applyContentTransform();
        scheduleLazyRedraw();
    }

    function handlePointerUp() {
        if (dragState) {
            dragState = null;
            scheduleSave();
        }
        // container.style.cursor = 'grab';
    }

    function handleContextMenuRequest(e: MouseEvent) {
        e.preventDefault();
        hideContextMenu();
        const rect = container.getBoundingClientRect();
        const mouseX = e.clientX - rect.left;
        const mouseY = e.clientY - rect.top;
        // Hit areas are recorded in group space (at renderedScrollOffset, before visual transform).
        // Convert screen coords to group space to match them correctly.
        const scrollTxGroup = -(scrollOffset - renderedScrollOffset) * dayWidth;
        const tx = visualPanX + visualScale * scrollTxGroup;
        const groupX = (mouseX - tx) / visualScale;
        const groupY = (mouseY - visualPanY) / visualScale;
        for (const h of renderer.sprintHitAreas as HitArea[]) {
            if (groupX >= h.x && groupX <= h.x + h.width && groupY >= h.y && groupY <= h.y + h.height) {
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
        if (lazyRedrawTimerId) clearTimeout(lazyRedrawTimerId);
        container.innerHTML = '';
    }

    // container.style.cursor = 'grab';
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

