// abstract-canvas.ts
// Manages SVG canvas dimensions, background, border, and the render pipeline.
// Mirrors Java: AbstractCanvas
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {intToHex} from './color-utils.js';
import {SvgUtils} from './svg-utils.js';
import {Theme} from './theme/theme.js';

export abstract class AbstractCanvas {
    chartWidth: number;
    chartHeight: number;
    borderWidth: number;
    theme: Theme;

    constructor(theme: Theme) {
        this.chartWidth = 0;
        this.chartHeight = 0;
        this.borderWidth = 1;
        this.theme = theme;
    }

    /** Sets chartWidth, this is the width the browser has reserved for us in total */
    setChartWidth(chartWidth: number): void {
        this.chartWidth = chartWidth;
    }

    /** Sets chartHeight, adding borderWidth. Mirrors Java: AbstractCanvas.setChartHeight(int). */
    setChartHeight(chartHeight: number): void {
        this.chartHeight = chartHeight + this.borderWidth * 2;
    }

    /** Fills the entire SVG with the theme background color. */
    drawBackground(svg: SVGGElement): void {
        const bgColor = intToHex(this.theme.chartTheme.backgroundColor, '#fffff0');
        svg.appendChild(SvgUtils.createRect(0, 0, this.chartWidth, this.chartHeight, {fill: bgColor}));
    }

    /** Draws a 1px border around the chart. */
    drawBorder(svg: SVGGElement): void {
        const borderColor = intToHex(this.theme.chartTheme.chartBorderColor, '#aaaaaa');
        svg.appendChild(SvgUtils.createRect(0.5, 0.5, this.chartWidth - 1, this.chartHeight - 1, {
            fill: 'none',
            stroke: borderColor,
            'stroke-width': '1',
        }));
    }

    /** Abstract – implemented by AbstractChart to draw the caption. */
    drawCaption(_svg: SVGGElement): void { /* to be overridden */
    }

    /** Abstract – implemented by AbstractChart to draw the footer. */
    drawFooter(_svg: SVGGElement): void { /* to be overridden */
    }

    /** Abstract – implemented by concrete charts. */
    createReport(_svg: SVGGElement): void { /* to be overridden */
    }

    /**
     * Renders the complete chart into an SVG and appends it to the container.
     * Order: background → caption → report → footer → border.
     * Mirrors Java: AbstractCanvas.render(…).
     */
    render(container: HTMLElement): void {
        const svg = SvgUtils.createSvgElement('svg', {
            width: this.chartWidth,
            height: this.chartHeight,
            style: 'display:block;user-select:none;shape-rendering:crispEdges',
        });
        this.drawBackground(svg);
        this.drawCaption(svg);
        this.createReport(svg);
        this.drawFooter(svg);
        this.drawBorder(svg);
        container.innerHTML = '';
        container.appendChild(svg);
    }
}

