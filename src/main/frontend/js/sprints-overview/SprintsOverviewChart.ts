// sprints-overview/sprints-overview-chart.ts
// Mirrors Java: SprintsOverviewChart extends AbstractChart
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {AbstractChart} from '../AbstractChart.js';
import {CalendarSize} from '../CalendarSize.js';
import {Theme} from '../theme/Theme.js';
import {SprintsOverviewRenderer} from './SprintsOverviewRenderer.js';
import {SprintOverviewDto} from './dto/SprintOverviewDto.js';

export class SprintsOverviewChart extends AbstractChart {
    constructor(data: SprintOverviewDto, theme: Theme) {
        super('Project Overview Chart', data.meta.copyright, '', '', '', 'sprints-overview-chart', theme);
        this.addRenderer(new SprintsOverviewRenderer(data, theme, 5, 5));
    }

    /**
     * Updates renderer scroll/zoom state and recomputes chart dimensions.
     * Called before each render frame.
     */
    updateViewState(dayWidth: number, scrollOffset: number, containerWidth: number, containerHeight: number): void {
        this.containerWidth = containerWidth;
        this.containerHeight = containerHeight;
        this.setChartWidth(containerWidth);

        const renderer = this.renderers[0] as SprintsOverviewRenderer;
        renderer.dayWidth = dayWidth;
        renderer.calculateDayWidth();//TODO handle day width on AbstractRenderer level
        renderer.calendarXAxes.dayOfWeek.width = dayWidth;
        renderer.scrollOffset = scrollOffset;
        renderer.containerWidth = containerWidth;
        renderer.containerHeight = containerHeight;
        renderer.init();

        const calendarH = renderer.calendarXAxes.getHeight();
        const lanesH = renderer.calculateLaneAreaHeight();
        const contentH = calendarH + lanesH;

        this.setChartHeight(contentH + this.captionElement.height + this.footerElement.height - 1);
        this.footerElement.y = contentH + this.captionElement.height;
        renderer.initSize(this.renderers[0].firstDayX, false, CalendarSize.YEARS, containerWidth - 2 * this.borderWidth);
    }

    override createReport(svg: SVGElement): void {
        (this.renderers[0] as SprintsOverviewRenderer).draw(svg, this.borderWidth, this.captionElement.height + this.borderWidth);
    }
}

