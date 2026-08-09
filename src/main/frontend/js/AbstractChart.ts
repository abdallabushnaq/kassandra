// abstract-chart.ts
// Adds caption, footer, and a list of renderers to AbstractCanvas.
// Mirrors Java: AbstractChart extends AbstractCanvas
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {AbstractCanvas} from './AbstractCanvas.js';
import {AbstractRenderer} from './AbstractRenderer.js';
import {CaptionElement} from './CaptionElement.js';
import {FooterElement} from './FooterElement.js';
import {SvgUtils} from './SvgUtils.js';
import {Theme} from './theme/Theme.js';


export abstract class AbstractChart extends AbstractCanvas {
    protected static readonly TIMELINE_CONTROLS_HINT =
        'Shift + mouse wheel: day width; Alt + mouse wheel: zoom|Left mouse button: pan';

    renderers: AbstractRenderer[];
    protected captionElement: CaptionElement;
    protected footerElement: FooterElement;

    /**
     * @param captionLeftText   Chart title text shown at the left corner
     * @param captionRightText  Chart title text shown at the right corner
     * @param footerLeftText    Footer text shown at the left corner
     * @param footerRightText   Footer text shown at the right corner
     * @param relateCssPath     CSS path (for caption link; not used in SVG)
     * @param _column           Grid column (not used in SVG rendering)
     * @param _imageName        File name hint (not used in interactive rendering)
     * @param theme             Theme instance
     */
    protected constructor(
        captionLeftText: string | null,
        captionRightText: string | null,
        footerLeftText: string | null,
        footerRightText: string | null,
        relateCssPath: string,
        _column: string,
        _imageName: string,
        theme: Theme,
    ) {
        super(theme);
        this.captionElement = new CaptionElement(captionLeftText, captionRightText, relateCssPath, theme);
        this.footerElement = new FooterElement(footerLeftText, footerRightText, theme);
        this.renderers = [];
        // this.firstDayX = 0;
    }

    /** Sets the chart width and propagates it to caption and footer. */
    override setChartWidth(chartWidth: number): void {
        super.setChartWidth(chartWidth);
        if (this.captionElement)
            this.captionElement.width = this.chartWidth;
        if (this.footerElement)
            this.footerElement.width = this.chartWidth;
    }

    override drawCaption(svg: SVGElement): void {
        this.captionElement?.draw(svg);
    }

    /** Updates the timeline metrics shown with the caption interaction hints. */
    public updateTimelineCaption(dayWidth: number, visualScale: number): void {
        this.captionElement.updateTimelineMetrics(dayWidth, visualScale);
    }

    override drawFooter(svg: SVGElement): void {
        // Wrap the footer elements in a <g> so updateSvgHeight can translate the whole
        // footer downward when visual zoom increases the chart height.
        const group = SvgUtils.createGroup();
        this.footerGroupEl = group;
        svg.appendChild(group);
        this.footerElement?.draw(group);
    }

    /** Adds a renderer to the list. */
    protected addRenderer(renderer: AbstractRenderer): void {
        this.renderers.push(renderer);
    }


}
