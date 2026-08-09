// abstract-canvas.ts
// Manages SVG canvas dimensions, background, border, and the render pipeline.
// Mirrors Java: AbstractCanvas
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {ColorUtils} from './ColorUtils.js';
import {SvgUtils} from './SvgUtils.js';
import {Theme} from './theme/Theme.js';

export abstract class AbstractCanvas {
    theme: Theme;
    /** Visible viewport width in pixels, externally driven by container size. */
    containerWidth: number;
    chartHeight: number;
    /**
     * When {@code true}, the SVG height is fixed to {@link containerHeight} and the
     * content group can be scrolled vertically via {@code scrollYOffset} in the bundle.
     * The clip path then clips vertically at {@link containerHeight}.
     * Defaults to {@code false} (current behaviour: chart grows to fit content).
     */
    verticalScrollEnabled: boolean = false;
    protected chartWidth: number;
    protected borderWidth: number;
    /** Visible viewport height in pixels, externally driven by container size. */
    protected containerHeight: number;
    /**
     * Wrapper group around the footer, set by AbstractChart.drawFooter.
     * Translated downward by (scaledHeight − chartHeight) so the footer
     * stays at the bottom of the chart when visual zoom is active.
     */
    protected footerGroupEl: SVGGElement | null = null;
    /** Reference to the background rect so it can be resized together with the SVG on visual zoom. */
    private backgroundEl: SVGRectElement | null = null;
    /** Reference to the border rect so it can be resized together with the SVG on visual zoom. */
    private borderEl: SVGRectElement | null = null;
    /** Reference to the root SVG element so its height can be updated on visual zoom without a full rebuild. */
    private svgEl: SVGSVGElement | null = null;
    /** Reference to the content group element for fast transform-only scroll and visual-zoom updates. */
    private contentGroupEl: SVGGElement | null = null;
    private burndownClipSeq = 0;

    protected constructor(theme: Theme) {
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
        const rect = SvgUtils.createRect(0, 0, this.chartWidth, this.chartHeight, {fill: bgColor});
        this.backgroundEl = rect;
        svg.appendChild(rect);
    }

    /** Draws a 1px border around the chart. */
    drawBorder(svg: SVGElement): void {
        const borderColor = ColorUtils.intToHex(this.theme.chartTheme.chartBorderColor, '#aaaaaa');
        const rect = SvgUtils.createRect(0.5, 0.5, this.chartWidth - 1, this.chartHeight - 1, {
            fill: 'none',
            stroke: borderColor,
            'stroke-width': '1',
        });
        this.borderEl = rect;
        svg.appendChild(rect);
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
        const svg = SvgUtils.createSvg({
            width: this.chartWidth,
            height: this.chartHeight,
            style: 'display:block;user-select:none;shape-rendering:crispEdges',
        });
        this.svgEl = svg;
        //--- lets clip to the size of our container.
        const clippedSvg = this.createClipPath(svg);
        this.drawBackground(clippedSvg);
        this.drawCaption(clippedSvg);
        const scrollingGroup = SvgUtils.createGroup(0, 0, {'contentGroup': 'content'});
        this.contentGroupEl = scrollingGroup;
        clippedSvg.appendChild(scrollingGroup);
        this.createReport(scrollingGroup);
        this.drawFooter(clippedSvg);
        this.drawBorder(clippedSvg);
        container.innerHTML = '';
        container.appendChild(svg);
    }

    createClipPath(svg: SVGSVGElement): SVGElement {
        const clipId = `ChartClip-${++this.burndownClipSeq}`;
        // When vertical scroll is enabled, clip at containerHeight so content beyond the
        // viewport is hidden and the SVG element itself stays fixed at containerHeight.
        // Otherwise, use an arbitrarily large value to avoid clipping the bottom.
        const clipH = this.verticalScrollEnabled ? this.containerHeight : 10000;
        svg.appendChild(SvgUtils.createClipPath(clipId, 0, 0, this.containerWidth, clipH));
        const group = SvgUtils.createGroup({'clip-path': `url(#${clipId})`});
        svg.appendChild(group);
        return group;
    }

    /**
     * Applies a combined scroll-translate and visual-scale transform to the content group without
     * a full SVG rebuild. The transform is {@code translate(tx, ty) scale(scale)}.
     * <p>
     * Callers must pass {@code tx = visualPanX + visualScale * scrollTxGroup} so that the scroll
     * translation is applied inside the scale, preventing visual jumps when lazy full redraws fire.
     *
     * @param tx    Combined horizontal translation (visual pan + scaled scroll offset) in pixels.
     * @param ty    Vertical translation (visual pan only) in pixels.
     * @param scale Uniform visual scale factor (1.0 = no visual zoom).
     */
    updateContentTransform(tx: number, ty: number, scale: number): void {
        if (this.contentGroupEl) {
            this.contentGroupEl.setAttribute('transform', `translate(${tx}, ${ty}) scale(${scale})`);
        }
    }

    /**
     * Updates the SVG root element's height to accommodate the current visual-zoom scale.
     * Also resizes the background and border rects, which live outside the content group and
     * therefore do not receive the scale transform automatically.
     * Called after {@link updateContentTransform} so the container grows/shrinks with the content.
     * <p>
     * When {@link verticalScrollEnabled} is {@code true}, the SVG is kept at
     * {@link containerHeight} and the footer translation is left at zero (the footer
     * stays at its original render position and is visible when scrolled to the bottom).
     *
     * @param height New height in pixels (typically {@code chartHeight * visualScale}).
     */
    updateSvgHeight(height: number): void {
        const h = this.verticalScrollEnabled
            ? Math.ceil(this.containerHeight)
            : Math.ceil(height);
        this.svgEl?.setAttribute('height', String(h));
        this.backgroundEl?.setAttribute('height', String(h));
        this.borderEl?.setAttribute('height', String(h - 1));
        if (this.footerGroupEl) {
            // When vertical scroll is active, keep footer at its rendered position (delta=0).
            // Otherwise, shift it down by the extra pixels added by the visual zoom scale.
            const delta = this.verticalScrollEnabled ? 0 : (h - this.chartHeight);
            this.footerGroupEl.setAttribute('transform', `translate(0, ${delta})`);
        }
    }
}
