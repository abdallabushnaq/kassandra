// sprints-overview/sprints-overview-bundle.ts
// Entry point for the Sprints Overview chart bundle.
// Exposes window.mountSprintsOverviewChart for Java interop via Vaadin executeJs.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {DateUtils} from '../DateUtils.js';
import {ChartHandle, InteractiveTimelineChart} from '../InteractiveTimelineChart.js';
import {Theme} from '../theme/Theme.js';
import {hideContextMenu, showContextMenuForSprint} from './context-menu.js';
import {HitArea} from './dto/HitArea.js';
import {SprintOverviewDto} from './dto/SprintOverviewDto.js';
import {SprintsOverviewChart} from './SprintsOverviewChart.js';
import {DEFAULT_DW, MAX_DW, MIN_DW, SprintsOverviewRenderer, ZOOM_STEP} from './SprintsOverviewRenderer.js';

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
    const interactiveChart = new InteractiveTimelineChart({
        container,
        containerId,
        chart,
        renderer,
        defaultDayWidth: DEFAULT_DW,
        minDayWidth: MIN_DW,
        maxDayWidth: MAX_DW,
        dayWidthZoomStep: ZOOM_STEP,
        initialScrollOffset: (dayWidth, containerWidth) => {
            const todayIdx = DateUtils.calculateDayIndex(renderer.currentDate, renderer.chartStart);
            const visibleDays = containerWidth / dayWidth;
            return Math.max(0, Math.min(renderer.days - visibleDays, todayIdx - visibleDays * 0.3));
        },
    });

    function handleContextMenuRequest(event: MouseEvent): void {
        event.preventDefault();
        hideContextMenu();
        const {x, y} = interactiveChart.toContentCoordinates(event.clientX, event.clientY);
        for (const hitArea of renderer.sprintHitAreas as HitArea[]) {
            if (x >= hitArea.x && x <= hitArea.x + hitArea.width && y >= hitArea.y && y <= hitArea.y + hitArea.height) {
                showContextMenuForSprint(event.clientX, event.clientY, hitArea.sprint);
                return;
            }
        }
    }

    interactiveChart.addEventListener('contextmenu', handleContextMenuRequest as EventListener);
    interactiveChart.render();
    return interactiveChart;
}

// ── Public mount API (called by SprintListView via Vaadin executeJs) ────────

function mountSprintsOverviewChart(containerId: string, injectedData: SprintOverviewDto): void {
    const elementId = containerId || 'sprints-overview-chart-container';
    const containerElement = document.getElementById(elementId);
    if (!containerElement)
        return;

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
