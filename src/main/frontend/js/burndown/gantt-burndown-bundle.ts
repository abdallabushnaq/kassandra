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

import {DateUtils} from '../DateUtils.js';
import {Theme} from '../theme/Theme.js';
import {DEFAULT_DW, MAX_DW, MIN_DW, ZOOM_STEP} from '../gantt/AbstractGanttRenderer.js';
import {BurndownRenderer} from './BurndownRenderer.js';
import {GanttRenderer} from '../gantt/GanttRenderer.js';
import {GanttBurndownChart} from './GanttBurndownChart.js';
import {GanttBurndownChartDto} from './dto/GanttBurndownChartDto.js';

// ── Visual-zoom constants ───────────────────────────────────────────────────
/** Per-notch scale factor for Ctrl+wheel / trackpad-pinch visual zoom. */
const VISUAL_ZOOM_STEP = 1.1;
const VISUAL_ZOOM_MIN = 0.1;
const VISUAL_ZOOM_MAX = 10.0;

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

interface ChartHandle {
    render(): void;

    schedule(): void;

    destroy(): void;
}

let currentGanttBurndownChartInstance: ChartHandle | null = null;

function createTooltipElement(container: HTMLElement): HTMLDivElement {
    const tooltip = document.createElement('div');
    tooltip.style.position = 'absolute';
    tooltip.style.pointerEvents = 'none';
    tooltip.style.zIndex = '1000';
    tooltip.style.maxWidth = '460px';
    tooltip.style.padding = '8px 10px';
    tooltip.style.borderRadius = '6px';
    tooltip.style.background = 'rgba(15,23,42,0.94)';
    tooltip.style.color = '#fff';
    tooltip.style.fontFamily = 'sans-serif';
    tooltip.style.fontSize = '12px';
    tooltip.style.lineHeight = '1.4';
    tooltip.style.boxShadow = '0 4px 16px rgba(0,0,0,0.25)';
    tooltip.style.display = 'none';
    tooltip.style.whiteSpace = 'normal';
    container.appendChild(tooltip);
    return tooltip;
}

function createChart(
    container: HTMLElement,
    data: GanttBurndownChartDto,
    options: { containerId?: string } = {},
): ChartHandle {
    const containerId = options.containerId || container.id || 'gantt-burndown-chart';
    const theme = new Theme(data.meta.theme as Record<string, unknown>);
    const chart = new GanttBurndownChart(data, theme);
    const burndown = chart.renderers[0] as BurndownRenderer;
    const gantt = chart.renderers[1] as GanttRenderer;

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
    /** Vertical scroll position in unscaled group pixels (0 = top). Active only when chart.verticalScrollEnabled. */
    let scrollYOffset = 0;

    function getContainerWidth() {
        return Math.max(200, container.clientWidth || 800);
    }

    function getContainerHeight() {
        return Math.max(200, container.clientHeight || 600);
    }

    function constrainScrollOffset() {
        scrollOffset = Math.max(0, Math.min(
            Math.max(0, gantt.totalDays - getContainerWidth() / (dayWidth * visualScale)),
            scrollOffset,
        ));
    }

    function constrainScrollYOffset() {
        if (!chart.verticalScrollEnabled) return;
        scrollYOffset = Math.max(0, Math.min(
            Math.max(0, chart.chartHeight - getContainerHeight() / visualScale),
            scrollYOffset,
        ));
    }

    const saved = loadViewState(containerId);
    if (saved) {
        dayWidth = Math.min(MAX_DW, Math.max(MIN_DW, saved.dayWidth));
        scrollOffset = saved.scrollOffset;
        constrainScrollOffset();
    } else {
        const referenceDate = gantt.currentDate || burndown.currentDate || new Date(data.meta.sprintStart);
        const todayIdx = DateUtils.calculateDayIndex(referenceDate, gantt.chartStart!);
        const visibleDays = getContainerWidth() / dayWidth;
        scrollOffset = Math.max(0, Math.min(gantt.totalDays - visibleDays, todayIdx - visibleDays * 0.2));
    }

    let saveTimerId: ReturnType<typeof setTimeout> | null = null;

    function scheduleSave() {
        if (saveTimerId) clearTimeout(saveTimerId);
        saveTimerId = setTimeout(() => saveViewState(containerId, dayWidth, scrollOffset), 250);
    }

    let animationFrameId: number | null = null;

    function ensureTooltip(): HTMLDivElement {
        let tooltip = container.querySelector<HTMLDivElement>('.gantt-burndown-tooltip');
        if (!tooltip) {
            tooltip = createTooltipElement(container);
            tooltip.className = 'gantt-burndown-tooltip';
        }
        return tooltip;
    }

    function redrawChart() {
        renderedScrollOffset = scrollOffset;
        container.style.position = 'relative';
        chart.updateViewState(dayWidth, scrollOffset, getContainerWidth(), getContainerHeight());
        chart.render(container);
        ensureTooltip();
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
        // Vertical: renderer always draws from Y=0, so the full scrollYOffset is always the translation.
        const scrollTyGroup = chart.verticalScrollEnabled ? -scrollYOffset : 0;
        const ty = visualPanY + visualScale * scrollTyGroup;
        chart.updateContentTransform(tx, ty, visualScale);
        // Grow/shrink the SVG to fit the scaled content; updateSvgHeight respects verticalScrollEnabled.
        chart.updateSvgHeight(chart.chartHeight * visualScale);
    }

    /**
     * Schedules a full SVG redraw 150 ms after the last translate-only scroll event.
     * This corrects viewport-culling gaps that may appear at the scroll edges during fast panning.
     */
    function scheduleLazyRedraw() {
        if (lazyRedrawTimerId) clearTimeout(lazyRedrawTimerId);
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
            constrainScrollYOffset();
            applyContentTransform();
            scheduleLazyRedraw();
        } else if (e.deltaX !== 0) {
            // Horizontal scroll — fast path: translate the group, defer the full redraw.
            scrollOffset += e.deltaX / dayWidth;
            constrainScrollOffset();
            applyContentTransform();
            scheduleLazyRedraw();
            scheduleSave();
        } else if (chart.verticalScrollEnabled) {
            // Vertical scroll when verticalScrollEnabled — pan Y, fast path.
            scrollYOffset += e.deltaY / visualScale;
            constrainScrollYOffset();
            applyContentTransform();
            scheduleLazyRedraw();
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

    let dragState: { startX: number; startOffset: number; startY: number; startYOffset: number } | null = null;

    function handlePointerDown(e: PointerEvent) {
        if (e.button !== 0)
            return;
        dragState = {startX: e.clientX, startOffset: scrollOffset, startY: e.clientY, startYOffset: scrollYOffset};
        container.setPointerCapture(e.pointerId);
        // container.style.cursor = 'grabbing';
        e.preventDefault();
    }

    function handlePointerMove(e: PointerEvent) {
        if (!dragState)
            return;
        // Divide by visualScale so dragging always pans 1:1 with screen pixels, regardless of zoom.
        scrollOffset = dragState.startOffset - (e.clientX - dragState.startX) / (dayWidth * visualScale);
        constrainScrollOffset();
        if (chart.verticalScrollEnabled) {
            scrollYOffset = dragState.startYOffset - (e.clientY - dragState.startY) / visualScale;
            constrainScrollYOffset();
        }
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

    function hideTooltip() {
        const tooltip = container.querySelector<HTMLDivElement>('.gantt-burndown-tooltip');
        if (tooltip) tooltip.style.display = 'none';
    }

    function handleMouseMove(e: MouseEvent) {
        const target = e.target instanceof Element ? e.target.closest('[data-tooltip-html]') : null;
        const tooltip = ensureTooltip();
        if (!target) {
            tooltip.style.display = 'none';
            return;
        }
        const html = target.getAttribute('data-tooltip-html');
        if (!html) {
            tooltip.style.display = 'none';
            return;
        }
        tooltip.innerHTML = html;
        tooltip.style.display = 'block';
        const rect = container.getBoundingClientRect();
        const tooltipRect = tooltip.getBoundingClientRect();
        let left = e.clientX - rect.left + 14;
        let top = e.clientY - rect.top + 14;
        if (left + tooltipRect.width > rect.width - 8) left = rect.width - tooltipRect.width - 8;
        if (top + tooltipRect.height > rect.height - 8) top = rect.height - tooltipRect.height - 8;
        tooltip.style.left = `${Math.max(8, left)}px`;
        tooltip.style.top = `${Math.max(8, top)}px`;
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
        container.removeEventListener('mousemove', handleMouseMove);
        container.removeEventListener('mouseleave', hideTooltip);
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
    container.addEventListener('mousemove', handleMouseMove);
    container.addEventListener('mouseleave', hideTooltip);

    redrawChart();
    return {render: redrawChart, schedule: scheduleRender, destroy: cleanupChart};
}

function mountGanttBurndownChart(containerId: string, injectedData: GanttBurndownChartDto): void {
    const elementId = containerId || 'gantt-burndown-chart-container';
    const containerElement = document.getElementById(elementId);
    if (!containerElement) return;

    currentGanttBurndownChartInstance?.destroy();
    currentGanttBurndownChartInstance = null;

    if (injectedData) {
        currentGanttBurndownChartInstance = createChart(containerElement, injectedData, {containerId: elementId});
    } else {
        containerElement.innerHTML = '<div style="padding:16px;color:red;font-family:sans-serif;">No Gantt burndown chart data provided.</div>';
    }
}

declare global {
    interface Window {
        mountGanttBurndownChart: typeof mountGanttBurndownChart;
        createGanttBurndownChart: typeof createChart;
    }
}

window.mountGanttBurndownChart = mountGanttBurndownChart;
window.createGanttBurndownChart = createChart;
