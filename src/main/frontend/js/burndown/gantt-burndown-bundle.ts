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
        },
        initialScrollOffset: (dayWidth, containerWidth) => {
            const referenceDate = gantt.currentDate || burndown.currentDate || new Date(data.meta.sprintStart);
            const todayIdx = DateUtils.calculateDayIndex(referenceDate, gantt.chartStart!);
            const visibleDays = containerWidth / dayWidth;
            return Math.max(0, Math.min(gantt.days - visibleDays, todayIdx - visibleDays * 0.2));
        },
    });
    let replayPointerId: number | null = null;
    const replayDayIndexFromPointer = (event: PointerEvent): number | null => {
        const slider = viewport.querySelector<SVGGElement>('[data-replay-slider]');
        const screenMatrix = slider?.getScreenCTM();
        const svg = slider?.ownerSVGElement;
        if (!slider || !screenMatrix || !svg)
            return null;

        const point = svg.createSVGPoint();
        point.x = event.clientX;
        point.y = event.clientY;
        const localX = point.matrixTransform(screenMatrix.inverse()).x;
        const minX = Number(slider.getAttribute('data-min-x'));
        const maxX = Number(slider.getAttribute('data-max-x'));
        const minDayIndex = Number(slider.getAttribute('data-min-day-index'));
        const maxDayIndex = Number(slider.getAttribute('data-max-day-index'));
        const ratio = maxX === minX ? 0 : Math.max(0, Math.min(1, (localX - minX) / (maxX - minX)));
        return minDayIndex + Math.round(ratio * (maxDayIndex - minDayIndex));
    };
    const updateReplayFromPointer = (event: PointerEvent): void => {
        const dayIndex = replayDayIndexFromPointer(event);
        if (dayIndex !== null && burndown.setReplayDayIndex(dayIndex))
            interactiveChart.schedule();
    };
    interactiveChart.addEventListener('pointerdown', (event => {
        const target = event.target instanceof Element ? event.target.closest('[data-replay-slider]') : null;
        if (!target)
            return;
        event.preventDefault();
        event.stopPropagation();
        replayPointerId = event.pointerId;
        viewport.setPointerCapture(event.pointerId);
        updateReplayFromPointer(event);
    }) as EventListener);
    interactiveChart.addEventListener('pointermove', (event => {
        const pointerEvent = event as PointerEvent;
        if (replayPointerId === pointerEvent.pointerId)
            updateReplayFromPointer(pointerEvent);
    }) as EventListener);
    const finishReplayPointer = (event: Event): void => {
        const pointerEvent = event as PointerEvent;
        if (replayPointerId !== pointerEvent.pointerId)
            return;
        replayPointerId = null;
        if (viewport.hasPointerCapture(pointerEvent.pointerId))
            viewport.releasePointerCapture(pointerEvent.pointerId);
    };
    interactiveChart.addEventListener('pointerup', finishReplayPointer);
    interactiveChart.addEventListener('pointercancel', finishReplayPointer);
    interactiveChart.addEventListener('lostpointercapture', finishReplayPointer);
    interactiveChart.addEventListener('keydown', (event => {
        const keyboardEvent = event as KeyboardEvent;
        const target = keyboardEvent.target instanceof Element
            ? keyboardEvent.target.closest<SVGGElement>('[data-replay-slider]')
            : null;
        if (!target)
            return;

        const currentDayIndex = Number(target.getAttribute('aria-valuenow'));
        const minDayIndex = Number(target.getAttribute('aria-valuemin'));
        const maxDayIndex = Number(target.getAttribute('aria-valuemax'));
        let nextDayIndex: number | null = null;
        switch (keyboardEvent.key) {
            case 'ArrowLeft':
            case 'ArrowDown':
                nextDayIndex = currentDayIndex - 1;
                break;
            case 'ArrowRight':
            case 'ArrowUp':
                nextDayIndex = currentDayIndex + 1;
                break;
            case 'Home':
                nextDayIndex = minDayIndex;
                break;
            case 'End':
                nextDayIndex = maxDayIndex;
                break;
        }
        if (nextDayIndex !== null) {
            keyboardEvent.preventDefault();
            if (burndown.setReplayDayIndex(nextDayIndex))
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
