// abstract-chart.ts
// Adds caption, footer, and a list of renderers to AbstractCanvas.
// Mirrors Java: AbstractChart extends AbstractCanvas
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {AbstractCanvas} from './AbstractCanvas.js';
import {AbstractRenderer} from './AbstractRenderer.js';
import {CaptionElement} from './CaptionElement.js';
import {FooterElement} from './FooterElement.js';
import {Theme} from './theme/Theme.js';

export abstract class AbstractChart extends AbstractCanvas {
    captionElement: CaptionElement;
    footerElement: FooterElement;
    renderers: AbstractRenderer[];
    firstDayX: number;

    /**
     * @param caption           Chart title text
     * @param copyright         Copyright text shown in footer
     * @param projectRequestKey Sprint key shown in footer (may be '')
     * @param relateCssPath     CSS path (for caption link; not used in SVG)
     * @param _column           Grid column (not used in SVG rendering)
     * @param _imageName        File name hint (not used in interactive rendering)
     * @param theme             Theme instance
     */
    constructor(
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
        this.footerElement = new FooterElement(copyright, projectRequestKey || '', theme,);
        this.renderers = [];
        this.firstDayX = 0;
    }

    /** Adds a renderer to the list. */
    addRenderer(renderer: AbstractRenderer): void {
        this.renderers.push(renderer);
    }

    /** Sets the chart width and propagates it to caption and footer. */
    override setChartWidth(chartWidth: number): void {
        super.setChartWidth(chartWidth);
        if (this.captionElement) this.captionElement.width = this.chartWidth;
        if (this.footerElement) this.footerElement.width = this.chartWidth;
    }

    override drawCaption(svg: SVGSVGElement): void {
        this.captionElement?.draw(svg);
    }

    override drawFooter(svg: SVGSVGElement): void {
        this.footerElement?.draw(svg);
    }

}

