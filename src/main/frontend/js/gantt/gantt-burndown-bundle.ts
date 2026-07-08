// gantt/gantt-burndown-bundle.ts
// Entry point for the interactive combined Gantt + burndown chart bundle.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {DateUtils} from '../date-utils.js';
import {Theme} from '../theme/theme.js';
import {DEFAULT_DW, MAX_DW, MIN_DW, ZOOM_STEP} from './abstract-gantt-renderer.js';
import {BurndownRenderer, GanttBurndownChartDto} from './burndown-renderer.js';
import {GanttRenderer} from './gantt-renderer.js';
import {GanttBurndownChart} from './gantt-burndown-chart.js';

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
    const theme = new Theme(data.burndownMeta.theme as Record<string, unknown>);
    const chart = new GanttBurndownChart(data, theme);
    const burndown = chart.renderers[0] as BurndownRenderer;
    const gantt = chart.renderers[1] as GanttRenderer;

    let dayWidth = DEFAULT_DW;
    let scrollOffset = 0;

    function getContainerWidth() {
        return Math.max(200, container.clientWidth || 800);
    }

    function constrainScrollOffset() {
        scrollOffset = Math.max(0, Math.min(
            Math.max(0, gantt.totalDays - getContainerWidth() / dayWidth),
            scrollOffset,
        ));
    }

    const saved = loadViewState(containerId);
    if (saved) {
        dayWidth = Math.min(MAX_DW, Math.max(MIN_DW, saved.dayWidth));
        scrollOffset = saved.scrollOffset;
        constrainScrollOffset();
    } else {
        const referenceDate = gantt.currentDate || burndown.currentDate || new Date(data.burndownMeta.sprintStart);
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
        container.style.position = 'relative';
        chart.updateViewState(dayWidth, scrollOffset, getContainerWidth());
        chart.render(container);
        ensureTooltip();
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
        container.innerHTML = '';
    }

    container.style.cursor = 'grab';
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
