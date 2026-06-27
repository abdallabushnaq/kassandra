// sprints-overview/sprints-overview-chart.ts
// Mirrors Java: SprintsOverviewChart extends AbstractChart
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {AbstractChart} from '../abstract-chart.js';
import {CalendarSize} from '../calendar-size.js';
import {Theme} from '../theme/theme.js';
import {SprintOverviewDto, SprintsOverviewRenderer} from './sprints-overview-renderer.js';

export class SprintsOverviewChart extends AbstractChart {
    constructor(data: SprintOverviewDto, theme: Theme) {
        super('Project Overview Chart', '', '', '', 'sprints-overview-chart', theme);
        this.addRenderer(new SprintsOverviewRenderer(data, theme, 5, 5));
    }

    /**
     * Updates renderer scroll/zoom state and recomputes chart dimensions.
     * Called before each render frame.
     */
    updateViewState(dayWidth: number, scrollOffset: number, containerWidth: number): void {
        const renderer = this.renderers[0] as SprintsOverviewRenderer;
        renderer.dayWidth = dayWidth;
        renderer.calendarXAxes.dayOfWeek.width = dayWidth;
        renderer.scrollOffset = scrollOffset;
        renderer.containerWidth = containerWidth;

        const calendarH = renderer.calendarXAxes.getHeight(dayWidth, false);
        const lanesH = renderer.calculateLaneAreaHeight();
        const contentH = calendarH + lanesH;

        this.setChartWidth(containerWidth);
        this.setChartHeight(contentH + this.captionElement.height + this.footerElement.height - 1);
        this.footerElement.y = contentH + this.captionElement.height;
        renderer.initSize(this.renderers[0].firstDayX, false, CalendarSize.YEARS, containerWidth);
    }

    override createReport(svg: SVGSVGElement): void {
        (this.renderers[0] as SprintsOverviewRenderer).draw(svg, 0, this.captionElement.height);
    }
}

