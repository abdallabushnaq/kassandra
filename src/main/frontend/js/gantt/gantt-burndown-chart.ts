// gantt/gantt-burndown-chart.ts
// Combined interactive Gantt + burndown chart.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {AbstractChart} from '../abstract-chart.js';
import {Theme} from '../theme/theme.js';
import {GanttChartDto, GanttRenderer} from './gantt-renderer.js';
import {BurndownRenderer, GanttBurndownChartDto} from './burndown-renderer.js';
import {CalendarSize} from '../calendar-size.js';

export class GanttBurndownChart extends AbstractChart {
    constructor(data: GanttBurndownChartDto, theme: Theme) {
        super('Gantt Burndown Chart', data.burndownMeta.sprintName || '', '', '', 'gantt-burndown-chart', theme);
        this.addRenderer(new BurndownRenderer(data, theme));
        const ganttData: GanttChartDto = {
            tasks: data.tasks || [],
            meta: {
                firstDayX: data.burndownMeta.firstDayX,
                chartStart: data.burndownMeta.chartStart,
                chartEnd: data.burndownMeta.chartEnd,
                now: data.burndownMeta.now || undefined,
                sprintEarliestStartDate: data.burndownMeta.sprintStart,
                sprintLatestFinishDate: data.burndownMeta.sprintEnd,
                sprintStatus: data.burndownMeta.sprintStatus || undefined,
                sprintName: data.burndownMeta.sprintName || undefined,
                preRun: data.burndownMeta.preRun,
                postRun: data.burndownMeta.postRun,
                theme: data.burndownMeta.theme,
                calendarSize: CalendarSize.MONTHS
            },
        };
        this.addRenderer(new GanttRenderer(ganttData, theme, data.burndownMeta.preRun || 0, data.burndownMeta.postRun || 0));
    }

    updateViewState(dayWidth: number, scrollOffset: number, containerWidth: number): void {
        const burndown = this.renderers[0] as BurndownRenderer;
        const gantt = this.renderers[1] as GanttRenderer;

        burndown.dayWidth = dayWidth;
        burndown.scrollOffset = scrollOffset;
        burndown.containerWidth = containerWidth;

        gantt.dayWidth = dayWidth;
        gantt.scrollOffset = scrollOffset;
        gantt.containerWidth = containerWidth;

        const burndownHeight = burndown.calculateChartHeight();
        const ganttCalendarH = gantt.calendarXAxes.getHeight();
        const ganttTaskAreaH = gantt.tasks.length * (gantt.getTaskHeight() + 1);
        const ganttHeight = ganttCalendarH + ganttTaskAreaH;

        this.setChartWidth(containerWidth);
        this.setChartHeight(burndownHeight + ganttHeight + this.captionElement.height + this.footerElement.height - 1);
        this.footerElement.y = burndownHeight + ganttHeight + this.captionElement.height;
    }

    override createReport(svg: SVGSVGElement): void {
        const burndown = this.renderers[0] as BurndownRenderer;
        const gantt = this.renderers[1] as GanttRenderer;
        // svg.appendChild(SvgUtils.createRect(0, 0, this.chartWidth, this.chartHeight, {fill: '#f00000'}));
        // svg.appendChild(SvgUtils.createRect(0, this.captionElement.height, this.chartWidth, burndown.calculateChartHeight(), {fill: '#00f000'}));
        // svg.appendChild(SvgUtils.createRect(0, this.captionElement.height + burndown.calculateChartHeight(), this.chartWidth, gantt.calculateChartHeight(), {fill: '#0000f0'}));
        burndown.draw(svg, 0, this.captionElement.height);
        gantt.draw(svg, 0, this.captionElement.height + burndown.calculateChartHeight() + 1);
    }
}
