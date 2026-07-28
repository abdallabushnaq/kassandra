// abstract-canvas.ts
// Manages SVG canvas dimensions, background, border, and the render pipeline.
// Mirrors Java: AbstractCanvas
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {ColorUtils} from './ColorUtils.js';
import {SvgUtils} from './SvgUtils.js';
import {Theme} from './theme/Theme.js';

export abstract class AbstractCanvas {
    static burndownClipSeq = 0;
    chartWidth: number;
    chartHeight: number;
    borderWidth: number;
    theme: Theme;
    /** Visible viewport width in pixels, externally driven by container size. */
    containerWidth: number;
    /** Visible viewport height in pixels, externally driven by container size. */
    containerHeight: number;
    /** Reference to the scrolling group element for fast translate-only scroll updates. */
    protected scrollingGroupEl: SVGGElement | null = null;

    constructor(theme: Theme) {
        this.chartWidth = 0;
        this.chartHeight = 0;
        this.borderWidth = 1;
        this.theme = theme;
        this.containerWidth = 800;
        this.containerHeight = 600;
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
    drawBackground(svg: SVGElement): void {
        const bgColor = ColorUtils.intToHex(this.theme.chartTheme.backgroundColor, '#fffff0');
        svg.appendChild(SvgUtils.createRect(0, 0, this.chartWidth, this.chartHeight, {fill: bgColor}));
    }

    /** Draws a 1px border around the chart. */
    drawBorder(svg: SVGElement): void {
        const borderColor = ColorUtils.intToHex(this.theme.chartTheme.chartBorderColor, '#aaaaaa');
        svg.appendChild(SvgUtils.createRect(0.5, 0.5, this.chartWidth - 1, this.chartHeight - 1, {
            fill: 'none',
            stroke: borderColor,
            'stroke-width': '1',
        }));
    }

    /** Abstract – implemented by AbstractChart to draw the caption. */
    drawCaption(_svg: SVGElement): void { /* to be overridden */
    }

    /** Abstract – implemented by AbstractChart to draw the footer. */
    drawFooter(_svg: SVGElement): void { /* to be overridden */
    }

    /** Abstract – implemented by concrete charts. */
    createReport(_svg: SVGElement): void { /* to be overridden */
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
        //--- lets clip to the size of our container.
        const clippedSvg = this.createClipPath(svg);
        this.drawBackground(clippedSvg);
        this.drawCaption(clippedSvg);
        const scrollingGroup = SvgUtils.createGroup(0, 0, {'scrollingGroup': 'scroll'});
        this.scrollingGroupEl = scrollingGroup;
        clippedSvg.appendChild(scrollingGroup);
        this.createReport(scrollingGroup);
        this.drawFooter(clippedSvg);
        this.drawBorder(clippedSvg);
        container.innerHTML = '';
        container.appendChild(svg);
    }

    createClipPath(svg: SVGSVGElement): SVGElement {
        const clipId = `ChartClip-${++AbstractCanvas.burndownClipSeq}`;
        svg.appendChild(SvgUtils.createClipPath(clipId, 0, 0, this.containerWidth, this.containerHeight));
        const group = SvgUtils.createSvgElement('g', {'clip-path': `url(#${clipId})`});
        svg.appendChild(group);
        return group;
    }

    /**
     * Updates the horizontal translation of the scrolling group without a full SVG rebuild.
     * Used for fast scroll-only updates; a lazy full redraw should follow to correct culling gaps.
     *
     * @param tx Horizontal translation in pixels (negative = scroll right / forward in time).
     */
    updateScrollTranslate(tx: number): void {
        if (this.scrollingGroupEl) {
            this.scrollingGroupEl.setAttribute('transform', `translate(${tx}, 0)`);
        }
    }
}

