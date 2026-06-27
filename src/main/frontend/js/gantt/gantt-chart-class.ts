// gantt/gantt-chart-class.ts
// Mirrors Java: GanttChart extends AbstractChart
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import { AbstractChart }             from '../abstract-chart.js';
import { Theme }                     from '../theme/theme.js';
import { GanttRenderer, GanttChartDto } from './gantt-renderer.js';

export class GanttChart extends AbstractChart {
    constructor(data: GanttChartDto, theme: Theme) {
        super('Gantt Chart', data.meta.sprintName || '', '', '', 'gantt-chart', theme);
        this.addRenderer(new GanttRenderer(data, theme));
    }

    updateViewState(dayWidth: number, scrollOffset: number, containerWidth: number): void {
        const renderer = this.renderers[0] as GanttRenderer;
        renderer.dayWidth       = dayWidth;
        renderer.scrollOffset   = scrollOffset;
        renderer.containerWidth = containerWidth;

        const calendarH = renderer.calendarXAxes.getHeight(dayWidth, renderer.milestones.list.length > 0);
        const taskAreaH = renderer.tasks.length * (renderer.getTaskHeight() + 1);
        const contentH  = calendarH + taskAreaH;

        this.setChartWidth(containerWidth);
        this.setChartHeight(contentH + this.captionElement.height + this.footerElement.height - 1);
        this.footerElement.y = contentH + this.captionElement.height;
    }

    override createReport(svg: SVGSVGElement): void {
        (this.renderers[0] as GanttRenderer).draw(svg, 0, this.captionElement.height);
    }
}

