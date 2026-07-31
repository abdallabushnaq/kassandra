// gantt/gantt-chart-class.ts
// Mirrors Java: GanttChart extends AbstractChart
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {AbstractChart} from '../AbstractChart.js';
import {Theme} from '../theme/Theme.js';
import {GanttRenderer} from './GanttRenderer.js';
import {GanttChartDto} from './dto/GanttChartDto.js';

export class GanttChart extends AbstractChart {
    constructor(data: GanttChartDto, theme: Theme) {
        super('Gantt Chart', data.meta.copyright, data.meta.sprintName || '', '', '', 'gantt-chart', theme);
        this.addRenderer(new GanttRenderer(this, data, 5, 5));
    }

    updateViewState(dayWidth: number, scrollOffset: number, containerWidth: number, containerHeight: number): void {
        this.containerWidth = containerWidth;
        this.containerHeight = containerHeight;
        this.setChartWidth(containerWidth);

        const renderer = this.renderers[0] as GanttRenderer;
        renderer.dayWidth = dayWidth;
        renderer.calculateDayWidth();//TODO handle day width on AbstractRenderer level
        renderer.scrollOffset = scrollOffset;
        renderer.containerWidth = containerWidth;
        renderer.containerHeight = containerHeight;
        renderer.init();

        const calendarH = renderer.calendarXAxes.getHeight();
        const taskAreaH = renderer.tasks.length * (renderer.getTaskHeight() + 1);
        const contentH = calendarH + taskAreaH;

        this.setChartHeight(contentH + this.captionElement.height + this.footerElement.height - 1);
        this.footerElement.y = contentH + this.captionElement.height;
    }

    override createReport(svg: SVGElement): void {
        (this.renderers[0] as GanttRenderer).draw(svg, 0, this.captionElement.height);
    }
}

