// gantt/gantt-bundle.ts
// Entry point for the Gantt chart bundle.
// Exposes window.mountGanttChart for Java interop via Vaadin executeJs.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {DateUtils} from '../DateUtils.js';
import {ChartHandle, InteractiveTimelineChart} from '../InteractiveTimelineChart.js';
import {Theme} from '../theme/Theme.js';
import {DEFAULT_DW, MAX_DW, MIN_DW, ZOOM_STEP} from './AbstractGanttRenderer.js';
import {GanttChart} from './GanttChart.js';
import {GanttChartDto} from './dto/GanttChartDto.js';
import {GanttRenderer} from './GanttRenderer.js';

let currentGanttChartInstance: ChartHandle | null = null;

function createChart(
    container: HTMLElement,
    data: GanttChartDto,
    options: { containerId?: string } = {},
): ChartHandle {
    const containerId = options.containerId || container.id || 'chart';
    const theme = new Theme(data.meta.theme as Record<string, unknown>);
    const chart = new GanttChart(data, theme);
    const renderer = chart.renderers[0] as GanttRenderer;
    const interactiveChart = new InteractiveTimelineChart({
        container,
        containerId,
        chart,
        renderer,
        defaultDayWidth: DEFAULT_DW,
        minDayWidth: MIN_DW,
        maxDayWidth: MAX_DW,
        dayWidthZoomStep: ZOOM_STEP,
        getContainerHeight: () => Math.max(200, Math.min(chart.chartHeight, container.clientHeight || 600)),
        initialScrollOffset: (dayWidth, containerWidth) => {
            const todayIdx = DateUtils.calculateDayIndex(renderer.currentDate!, renderer.chartStart!);
            const visibleDays = containerWidth / dayWidth;
            return Math.max(0, Math.min(renderer.days - visibleDays, todayIdx - visibleDays * 0.2));
        },
    });
    interactiveChart.render();
    return interactiveChart;
}

// ── Public mount API (called by Backlog.java via Vaadin executeJs) ───────────

function mountGanttChart(containerId: string, injectedData: GanttChartDto): void {
    const elementId = containerId || 'gantt-chart-container';
    const containerElement = document.getElementById(elementId);
    if (!containerElement)
        return;

    currentGanttChartInstance?.destroy();
    currentGanttChartInstance = null;

    if (injectedData) {
        currentGanttChartInstance = createChart(containerElement, injectedData, {containerId: elementId});
    } else {
        containerElement.innerHTML = '<div style="padding:16px;color:red;font-family:sans-serif;">No Gantt chart data provided.</div>';
    }
}

// ── Expose globals for Java interop ─────────────────────────────────────────

declare global {
    interface Window {
        mountGanttChart: typeof mountGanttChart;
        createGanttChart: typeof createChart;
    }
}
window.mountGanttChart = mountGanttChart;
window.createGanttChart = createChart;
