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
    renderers: AbstractRenderer[];
    protected captionElement: CaptionElement;
    protected footerElement: FooterElement;

    // private firstDayX: number;

    /**
     * @param caption           Chart title text
     * @param copyright         Copyright text shown in footer
     * @param projectRequestKey Sprint key shown in footer (may be '')
     * @param relateCssPath     CSS path (for caption link; not used in SVG)
     * @param _column           Grid column (not used in SVG rendering)
     * @param _imageName        File name hint (not used in interactive rendering)
     * @param theme             Theme instance
     */
    protected constructor(
        caption: string | null,
        copyright: string,
        projectRequestKey: string,
        relateCssPath: string,
        _column: string,
        _imageName: string,
        theme: Theme,
    ) {
        super(theme);
        this.captionElement = new CaptionElement(caption, relateCssPath, theme);
        this.footerElement = new FooterElement(copyright, projectRequestKey, theme);
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

    override drawFooter(svg: SVGElement): void {
        // Wrap the footer elements in a <g> so updateSvgHeight can translate the whole
        // footer downward when visual zoom increases the chart height.
        const group = SvgUtils.createSvgElement('g', {});
        this.footerGroupEl = group as SVGGElement;
        svg.appendChild(group);
        this.footerElement?.draw(group);
    }

    /** Adds a renderer to the list. */
    protected addRenderer(renderer: AbstractRenderer): void {
        this.renderers.push(renderer);
    }


}

