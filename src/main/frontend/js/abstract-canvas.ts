// abstract-canvas.ts
// Manages SVG canvas dimensions, background, border, and the render pipeline.
// Mirrors Java: AbstractCanvas
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {intToHex} from './color-utils.js';
import {createRect, createSvgElement} from './svg-utils.js';
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

    /** Sets chartWidth, adding borderWidth. Mirrors Java: AbstractCanvas.setChartWidth(int). */
    setChartWidth(chartWidth: number): void {
        this.chartWidth = chartWidth + this.borderWidth * 2;
    }

    /** Sets chartHeight, adding borderWidth. Mirrors Java: AbstractCanvas.setChartHeight(int). */
    setChartHeight(chartHeight: number): void {
        this.chartHeight = chartHeight + this.borderWidth * 2;
    }

    /** Fills the entire SVG with the theme background color. */
    drawBackground(svg: SVGSVGElement): void {
        const bgColor = intToHex(this.theme.chartTheme.backgroundColor, '#ffffff');
        svg.appendChild(createRect(0, 0, this.chartWidth, this.chartHeight, {fill: bgColor}));
    }

    /** Draws a 1px border around the chart. */
    drawBorder(svg: SVGSVGElement): void {
        const borderColor = intToHex(this.theme.chartTheme.chartBorderColor, '#aaaaaa');
        svg.appendChild(createRect(0, 0, this.chartWidth - 1, this.chartHeight - 1, {
            fill: 'none',
            stroke: borderColor,
            'stroke-width': '1',
        }));
    }

    /** Abstract – implemented by AbstractChart to draw the caption. */
    drawCaption(_svg: SVGSVGElement): void { /* to be overridden */
    }

    /** Abstract – implemented by AbstractChart to draw the footer. */
    drawFooter(_svg: SVGSVGElement): void { /* to be overridden */
    }

    /** Abstract – implemented by concrete charts. */
    createReport(_svg: SVGSVGElement): void { /* to be overridden */
    }

    /**
     * Renders the complete chart into an SVG and appends it to the container.
     * Order: background → caption → report → footer → border.
     * Mirrors Java: AbstractCanvas.render(…).
     */
    render(container: HTMLElement): void {
        const svg = createSvgElement('svg', {
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

