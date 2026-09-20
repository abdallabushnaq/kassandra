// chart-render-state.ts
// Shared chart readiness marker for browser automation.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

export const CHART_RENDERED_ATTRIBUTE = 'data-chart-rendered';

/** Clears the readiness marker before the chart container is rebuilt. */
export function markChartRendering(container: HTMLElement): void {
    container.removeAttribute(CHART_RENDERED_ATTRIBUTE);
}

/** Marks the container after the chart SVG has been rendered. */
export function markChartRendered(container: HTMLElement, chartIds: readonly string[]): void {
    container.setAttribute(CHART_RENDERED_ATTRIBUTE, chartIds.join(' '));
}
