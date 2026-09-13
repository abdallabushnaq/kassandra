// gantt/gantt-bundle.ts
// Entry point for the Gantt chart bundle.
// Exposes window.mountGanttChart for Java interop via Vaadin executeJs.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {DateUtils} from '../DateUtils.js';
import {mountDetachableChart} from '../DetachableChartHost.js';
import {ChartHandle, InteractiveTimelineChart} from '../InteractiveTimelineChart.js';
import {Theme} from '../theme/Theme.js';
import {DEFAULT_DW, MAX_DW, MIN_DW, ZOOM_STEP} from './AbstractGanttRenderer.js';
import {GanttChart} from './GanttChart.js';
import {GanttChartDto} from './dto/GanttChartDto.js';
import {GanttRenderer} from './GanttRenderer.js';

export function createGanttChart(
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
        getContainerHeight: () => {
            const naturalHeight = Number.isFinite(chart.chartHeight) ? chart.chartHeight : 0;
            return Math.max(200, naturalHeight || container.clientHeight || 600);
        },
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

function mountGanttChart(containerId: string, injectedData: GanttChartDto, title: string = 'Gantt chart'): void {
    const elementId = containerId || 'gantt-chart-container';
    const containerElement = document.getElementById(elementId);
    if (!containerElement)
        return;

    if (injectedData) {
        mountDetachableChart(
            {
                bundleUrl: '/js/generated/gantt/gantt-bundle.js',
                containerId: elementId,
                factoryExportName: 'createGanttChart',
                factory: createGanttChart,
                title: 'Gantt chart',
            },
            containerElement,
            injectedData,
            title,
        );
    } else {
        containerElement.innerHTML = '<div style="padding:16px;color:red;font-family:sans-serif;">No Gantt chart data provided.</div>';
    }
}

// ── Expose globals for Java interop ─────────────────────────────────────────

declare global {
    interface Window {
        mountGanttChart: typeof mountGanttChart;
        createGanttChart: typeof createGanttChart;
    }
}
window.mountGanttChart = mountGanttChart;
window.createGanttChart = createGanttChart;
