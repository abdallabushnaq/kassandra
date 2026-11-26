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
import {CHART_RENDERED_THEME_ATTRIBUTE} from '../ChartRenderState.js';
import {mountDetachableChart} from '../DetachableChartHost.js';
import {ChartHandle, InteractiveTimelineChart} from '../InteractiveTimelineChart.js';
import {Theme} from '../theme/Theme.js';
import {DEFAULT_DW, MAX_DW, MIN_DW, ZOOM_STEP} from '../gantt/AbstractGanttRenderer.js';
import {GanttRenderer} from '../gantt/GanttRenderer.js';
import {BurndownRenderer} from './BurndownRenderer.js';
import {GanttBurndownChart} from './GanttBurndownChart.js';
import {GanttBurndownChartDto} from './dto/GanttBurndownChartDto.js';
import {GanttTaskInteraction} from '../gantt/GanttTaskInteraction.js';
import {BurndownPointInteraction} from './BurndownPointInteraction.js';

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

export function createGanttBurndownChart(
    container: HTMLElement,
    data: GanttBurndownChartDto,
    options: { containerId?: string; initialVisualScale?: number } = {},
): ChartHandle {
    const containerId = options.containerId || container.id || 'gantt-burndown-chart';
    const theme = new Theme(data.meta.theme as Record<string, unknown>);
    const chart = new GanttBurndownChart(data, theme);
    const burndown = chart.renderers[0] as BurndownRenderer;
    const gantt = chart.renderers[1] as GanttRenderer;
    const inspection = new GanttTaskInteraction(container, gantt, theme);
    const viewport = inspection.viewport;
    const points = new BurndownPointInteraction(viewport, burndown);
    let replayPointer: number | null = null;
    let restoreReplayFocus = false;

    function ensureTooltip(): HTMLDivElement {
        let tooltip = viewport.querySelector<HTMLDivElement>('.gantt-burndown-tooltip');
        if (!tooltip) {
            tooltip = createTooltipElement(viewport);
            tooltip.className = 'gantt-burndown-tooltip';
        }
        return tooltip;
    }

    function hideTooltip(): void {
        const tooltip = viewport.querySelector<HTMLDivElement>('.gantt-burndown-tooltip');
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
        const rect = viewport.getBoundingClientRect();
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
        container: viewport,
        containerId,
        chart,
        renderer: gantt,
        defaultDayWidth: DEFAULT_DW,
        minDayWidth: MIN_DW,
        maxDayWidth: MAX_DW,
        dayWidthZoomStep: ZOOM_STEP,
        initialVisualScale: options.initialVisualScale,
        onActivate: target => inspection.activate(target),
        afterRender: () => {
            ensureTooltip();
            inspection.afterRender();
            points.afterRender();
            if (restoreReplayFocus) {
                viewport.querySelector<SVGElement>('[data-replay-slider]')?.focus();
                restoreReplayFocus = false;
            }
        },
        initialScrollOffset: (dayWidth, containerWidth) => {
            const referenceDate = gantt.currentDate || burndown.currentDate || new Date(data.meta.sprintStart);
            const todayIdx = DateUtils.calculateDayIndex(referenceDate, gantt.chartStart!);
            const visibleDays = containerWidth / dayWidth;
            return Math.max(0, Math.min(gantt.days - visibleDays, todayIdx - visibleDays * 0.2));
        },
    });
    const updateReplayFromPointer = (event: PointerEvent): void => {
        const slider = viewport.querySelector<SVGGElement>('[data-replay-slider]');
        const matrix = slider?.getScreenCTM();
        if (!slider || !matrix)
            return;
        const position = new DOMPoint(event.clientX, event.clientY).matrixTransform(matrix.inverse());
        const startX = Number(slider.getAttribute('data-start-x'));
        const minX = Number(slider.getAttribute('data-min-x'));
        const dayWidth = Number(slider.getAttribute('data-day-width'));
        const dayIndex = position.x < startX && minX > startX - dayWidth
            ? (position.x <= (minX + startX) / 2 ? -1 : 0)
            : (position.x - startX) / dayWidth;
        if (burndown.setReplayDayIndex(dayIndex))
            interactiveChart.schedule();
    };
    interactiveChart.addEventListener('pointerdown', (event => {
        const pointer = event as PointerEvent;
        if (pointer.button !== 0 || !(pointer.target instanceof Element)
            || !pointer.target.closest('[data-replay-slider]'))
            return;
        replayPointer = pointer.pointerId;
        viewport.setPointerCapture(pointer.pointerId);
        updateReplayFromPointer(pointer);
        pointer.preventDefault();
    }) as EventListener);
    interactiveChart.addEventListener('pointermove', (event => {
        const pointer = event as PointerEvent;
        if (pointer.pointerId === replayPointer)
            updateReplayFromPointer(pointer);
    }) as EventListener);
    const finishReplayPointer = (event: Event): void => {
        const pointer = event as PointerEvent;
        if (pointer.pointerId !== replayPointer)
            return;
        replayPointer = null;
        if (viewport.hasPointerCapture(pointer.pointerId))
            viewport.releasePointerCapture(pointer.pointerId);
    };
    interactiveChart.addEventListener('pointerup', finishReplayPointer);
    interactiveChart.addEventListener('pointercancel', finishReplayPointer);
    interactiveChart.addEventListener('lostpointercapture', finishReplayPointer);
    interactiveChart.addEventListener('keydown', (event => {
        const key = event as KeyboardEvent;
        if (!(key.target instanceof Element) || !key.target.closest('[data-replay-slider]'))
            return;
        const slider = key.target.closest('[data-replay-slider]')!;
        const current = Number(slider.getAttribute('aria-valuenow'));
        let next: number;
        switch (key.key) {
            case 'ArrowLeft': case 'ArrowDown': next = current - 1; break;
            case 'ArrowRight': case 'ArrowUp': next = current + 1; break;
            case 'Home': next = Number(slider.getAttribute('aria-valuemin')); break;
            case 'End': next = Number(slider.getAttribute('aria-valuemax')); break;
            default: return;
        }
        key.preventDefault();
        if (burndown.setReplayDayIndex(next)) {
            restoreReplayFocus = true;
            interactiveChart.schedule();
        }
    }) as EventListener);
    interactiveChart.addEventListener('mousemove', handleMouseMove as EventListener);
    interactiveChart.addEventListener('mouseleave', hideTooltip as EventListener);
    interactiveChart.addEventListener('pointermove', (event => points.handlePointerMove(event as PointerEvent)) as EventListener);
    interactiveChart.addEventListener('pointerleave', () => points.clear());
    interactiveChart.addEventListener('pointerdown', () => points.clear());
    interactiveChart.addEventListener('pointercancel', () => points.clear());
    interactiveChart.addEventListener('wheel', () => points.clear());
    const handle = inspection.attach({
        render: () => interactiveChart.render(),
        schedule: () => interactiveChart.schedule(),
        destroy: () => {
            points.destroy();
            interactiveChart.destroy();
        },
    });
    handle.render();
    container.setAttribute(CHART_RENDERED_THEME_ATTRIBUTE, theme.themeVariance);
    return handle;
}

function mountGanttBurndownChart(
    containerId: string,
    injectedData: GanttBurndownChartDto,
    title: string = 'Gantt and burndown chart',
    initialVisualScale?: number,
): void {
    const elementId = containerId || 'gantt-burndown-chart-container';
    const containerElement = document.getElementById(elementId);
    if (!containerElement)
        return;

    if (injectedData) {
        mountDetachableChart(
            {
                bundleUrl: '/js/generated/burndown/gantt-burndown-bundle.js',
                containerId: elementId,
                chartIds: [GanttBurndownChart.BURNDOWN_CHART_ID, GanttBurndownChart.CHART_ID],
                factoryExportName: 'createGanttBurndownChart',
                factory: createGanttBurndownChart,
                title: 'Gantt and burndown chart',
            },
            containerElement,
            injectedData,
            title,
            initialVisualScale,
        );
    } else {
        containerElement.innerHTML = '<div style="padding:16px;color:red;font-family:sans-serif;">No Gantt burndown chart data provided.</div>';
    }
}

declare global {
    interface Window {
        mountGanttBurndownChart: typeof mountGanttBurndownChart;
        createGanttBurndownChart: typeof createGanttBurndownChart;
    }
}
window.mountGanttBurndownChart = mountGanttBurndownChart;
window.createGanttBurndownChart = createGanttBurndownChart;
