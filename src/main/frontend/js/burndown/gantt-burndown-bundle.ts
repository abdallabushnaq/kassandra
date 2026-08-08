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
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

import {DateUtils} from '../DateUtils.js';
import {ChartHandle, InteractiveTimelineChart} from '../InteractiveTimelineChart.js';
import {Theme} from '../theme/Theme.js';
import {DEFAULT_DW, MAX_DW, MIN_DW, ZOOM_STEP} from '../gantt/AbstractGanttRenderer.js';
import {GanttRenderer} from '../gantt/GanttRenderer.js';
import {BurndownRenderer} from './BurndownRenderer.js';
import {GanttBurndownChart} from './GanttBurndownChart.js';
import {GanttBurndownChartDto} from './dto/GanttBurndownChartDto.js';

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

    function ensureTooltip(): HTMLDivElement {
        let tooltip = container.querySelector<HTMLDivElement>('.gantt-burndown-tooltip');
        if (!tooltip) {
            tooltip = createTooltipElement(container);
            tooltip.className = 'gantt-burndown-tooltip';
        }
        return tooltip;
    }

    function hideTooltip(): void {
        const tooltip = container.querySelector<HTMLDivElement>('.gantt-burndown-tooltip');
        if (tooltip)
            tooltip.style.display = 'none';
    }

    function handleMouseMove(event: MouseEvent): void {
        const target = event.target instanceof Element ? event.target.closest('[data-tooltip-html]') : null;
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
        let left = event.clientX - rect.left + 14;
        let top = event.clientY - rect.top + 14;
        if (left + tooltipRect.width > rect.width - 8)
            left = rect.width - tooltipRect.width - 8;
        if (top + tooltipRect.height > rect.height - 8)
            top = rect.height - tooltipRect.height - 8;
        tooltip.style.left = `${Math.max(8, left)}px`;
        tooltip.style.top = `${Math.max(8, top)}px`;
    }

    const interactiveChart = new InteractiveTimelineChart({
        container,
        containerId,
        chart,
        renderer: gantt,
        defaultDayWidth: DEFAULT_DW,
        minDayWidth: MIN_DW,
        maxDayWidth: MAX_DW,
        dayWidthZoomStep: ZOOM_STEP,
        beforeRender: () => {
            container.style.position = 'relative';
        },
        afterRender: ensureTooltip,
        initialScrollOffset: (dayWidth, containerWidth) => {
            const referenceDate = gantt.currentDate || burndown.currentDate || new Date(data.meta.sprintStart);
            const todayIdx = DateUtils.calculateDayIndex(referenceDate, gantt.chartStart!);
            const visibleDays = containerWidth / dayWidth;
            return Math.max(0, Math.min(gantt.days - visibleDays, todayIdx - visibleDays * 0.2));
        },
    });
    interactiveChart.addEventListener('mousemove', handleMouseMove as EventListener);
    interactiveChart.addEventListener('mouseleave', hideTooltip as EventListener);
    interactiveChart.render();
    return interactiveChart;
}

function mountGanttBurndownChart(containerId: string, injectedData: GanttBurndownChartDto): void {
    const elementId = containerId || 'gantt-burndown-chart-container';
    const containerElement = document.getElementById(elementId);
    if (!containerElement)
        return;

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
