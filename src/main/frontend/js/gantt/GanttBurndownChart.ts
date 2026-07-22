// gantt/gantt-burndown-chart.ts
// Combined interactive Gantt + burndown chart.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {AbstractChart} from '../AbstractChart.js';
import {Theme} from '../theme/Theme.js';
import {GanttRenderer} from './GanttRenderer.js';
import {BurndownRenderer} from './BurndownRenderer.js';
import {CalendarSize} from '../CalendarSize.js';
import {GanttChartDto} from './dto/GanttChartDto.js';
import {GanttBurndownChartDto} from './dto/GanttBurndownChartDto.js';

export class GanttBurndownChart extends AbstractChart {
    constructor(data: GanttBurndownChartDto, theme: Theme) {
        super('Gantt Burndown Chart', data.meta.copyright, data.meta.sprintName || '', '', '', 'gantt-burndown-chart', theme);
        this.addRenderer(new BurndownRenderer(data, theme));
        const ganttData: GanttChartDto = {
            tasks: data.tasks || [],
            meta: {
                firstDayX: data.meta.firstDayX,
                chartStart: data.meta.chartStart,
                chartEnd: data.meta.chartEnd,
                copyright: data.meta.copyright,
                now: data.meta.now || undefined,
                sprintEarliestStartDate: data.meta.sprintStart,
                sprintLatestFinishDate: data.meta.sprintEnd,
                sprintStatus: data.meta.sprintStatus || undefined,
                sprintName: data.meta.sprintName || undefined,
                preRun: data.meta.preRun,
                postRun: data.meta.postRun,
                theme: data.meta.theme,
                calendarSize: CalendarSize.MONTHS
            },
        };
        this.addRenderer(new GanttRenderer(ganttData, theme, data.meta.preRun || 0, data.meta.postRun || 0));
    }

    updateViewState(dayWidth: number, scrollOffset: number, containerWidth: number, containerHeight: number): void {
        this.containerWidth = containerWidth;
        this.containerHeight = containerHeight;

        const burndown = this.renderers[0] as BurndownRenderer;
        const gantt = this.renderers[1] as GanttRenderer;

        burndown.dayWidth = dayWidth;
        burndown.scrollOffset = scrollOffset;
        burndown.containerWidth = containerWidth;
        burndown.containerHeight = containerHeight;

        gantt.dayWidth = dayWidth;
        gantt.scrollOffset = scrollOffset;
        gantt.containerWidth = containerWidth;
        gantt.containerHeight = containerHeight;

        const burndownHeight = burndown.calculateChartHeight();
        const ganttCalendarH = gantt.calendarXAxes.getHeight();
        const ganttTaskAreaH = gantt.tasks.length * (gantt.getTaskHeight() + 1);
        const ganttHeight = ganttCalendarH + ganttTaskAreaH;

        this.setChartWidth(containerWidth);
        this.setChartHeight(burndownHeight + ganttHeight + this.captionElement.height + this.footerElement.height - 1);
        this.footerElement.y = burndownHeight + ganttHeight + this.captionElement.height;
    }

    override createReport(svg: SVGElement): void {
        const burndown = this.renderers[0] as BurndownRenderer;
        const gantt = this.renderers[1] as GanttRenderer;
        // svg.appendChild(SvgUtils.createRect(0, 0, this.chartWidth, this.chartHeight, {fill: '#f00000'}));
        // svg.appendChild(SvgUtils.createRect(0, this.captionElement.height, this.chartWidth, burndown.calculateChartHeight(), {fill: '#00f000'}));
        // svg.appendChild(SvgUtils.createRect(0, this.captionElement.height + burndown.calculateChartHeight(), this.chartWidth, gantt.calculateChartHeight(), {fill: '#0000f0'}));
        burndown.draw(svg, 0, this.captionElement.height);
        gantt.draw(svg, 0, this.captionElement.height + burndown.calculateChartHeight() + 1);
    }
}
