// chart-util.js
// AbstractCanvas, AbstractChart, AbstractRenderer – base classes for all charts.
// Mirrors Java: AbstractCanvas, AbstractChart, AbstractRenderer
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0
(function () {
    'use strict';

    var createSvgElement = window.SvgUtils.createSvgElement;
    var createRect       = window.SvgUtils.createRect;
    var createLine       = window.SvgUtils.createLine;
    var intToHex         = window.ColorUtils.intToHex;

    // ── AbstractCanvas ─────────────────────────────────────────────────────────
    // Mirrors Java: AbstractCanvas
    // Manages SVG canvas dimensions, background, border, and the render pipeline.

    class AbstractCanvas {
        /**
         * @param {Theme} theme  Theme instance
         */
        constructor(theme) {
            /** Total SVG width (including border). Mirrors Java AbstractCanvas.chartWidth. */
            this.chartWidth  = 0;
            /** Total SVG height (including border). Mirrors Java AbstractCanvas.chartHeight. */
            this.chartHeight = 0;
            /** Border thickness in pixels. Mirrors Java AbstractCanvas.borderWidth = 1. */
            this.borderWidth = 1;
            this.theme       = theme;
        }

        /**
         * Sets chartWidth, adding borderWidth.
         * Mirrors Java: AbstractCanvas.setChartWidth(int chartWidth).
         */
        setChartWidth(chartWidth) {
            this.chartWidth = chartWidth + this.borderWidth;
        }

        /**
         * Sets chartHeight, adding borderWidth.
         * Mirrors Java: AbstractCanvas.setChartHeight(int chartHeight).
         */
        setChartHeight(chartHeight) {
            this.chartHeight = chartHeight + this.borderWidth;
        }

        /**
         * Fills the entire SVG with the theme background color.
         * Mirrors Java: AbstractCanvas.drawBackground().
         *
         * @param {SVGSVGElement} svg
         */
        drawBackground(svg) {
            var bgColor = intToHex(this.theme.chartTheme.backgroundColor, '#ffffff');
            svg.appendChild(createRect(0, 0, this.chartWidth, this.chartHeight, {fill: bgColor}));
        }

        /**
         * Draws a 1px border around the chart.
         * Mirrors Java: AbstractCanvas.drawBorder(ExtendedGraphics2D g2).
         *
         * @param {SVGSVGElement} svg
         */
        drawBorder(svg) {
            var borderColor = intToHex(this.theme.chartTheme.chartBorderColor, '#aaaaaa');
            svg.appendChild(createRect(0, 0, this.chartWidth - 1, this.chartHeight - 1, {
                fill: 'none',
                stroke: borderColor,
                'stroke-width': '1'
            }));
        }

        /** Abstract – implemented by AbstractChart to draw the caption. */
        drawCaption(svg) { /* to be overridden */ }

        /** Abstract – implemented by AbstractChart to draw the footer. */
        drawFooter(svg) { /* to be overridden */ }

        /** Abstract – implemented by concrete charts (GanttChart, SprintsOverviewChart). */
        createReport(svg) { /* to be overridden */ }

        /**
         * Renders the complete chart into an SVG and appends it to the container.
         * Render order: background → caption → report → footer → border.
         * Mirrors Java: AbstractCanvas.render(…).
         *
         * @param {HTMLElement} container  Host DOM element
         */
        render(container) {
            var svg = createSvgElement('svg', {
                width: this.chartWidth,
                height: this.chartHeight,
                style: 'display:block;user-select:none;shape-rendering:crispEdges'
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

    // ── AbstractChart ──────────────────────────────────────────────────────────
    // Mirrors Java: AbstractChart extends AbstractCanvas
    // Adds caption, footer, and a list of renderers.

    class AbstractChart extends AbstractCanvas {
        /**
         * @param {string}  caption         Chart title text
         * @param {string}  projectRequestKey  Sprint key shown in footer (may be '')
         * @param {string}  relateCssPath   CSS path (for caption link; not used in SVG)
         * @param {string}  column          Grid column (not used in SVG rendering)
         * @param {string}  imageName       File name hint (not used in interactive rendering)
         * @param {Theme}   theme           Theme instance
         */
        constructor(caption, projectRequestKey, relateCssPath, column, imageName, theme) {
            super(theme);
            /**
             * Caption element drawn at the top of the chart.
             * Mirrors Java: AbstractChart.captionElement.
             */
            this.captionElement = new window.CaptionElement(caption, relateCssPath, theme);
            /**
             * Footer element drawn at the bottom of the chart.
             * Mirrors Java: AbstractChart.footerElement.
             */
            this.footerElement = new window.FooterElement(
                'Copyright \u00A9 ' + new Date().getFullYear(),
                projectRequestKey || '',
                theme
            );
            /**
             * List of renderers.  Mirrors Java: AbstractChart.renderers (List<AbstractRenderer>).
             */
            this.renderers = [];
        }

        /**
         * Adds a renderer to the list.
         * Mirrors Java: AbstractChart uses getRenderers().add(renderer).
         *
         * @param {AbstractRenderer} renderer
         */
        addRenderer(renderer) {
            this.renderers.push(renderer);
        }

        /**
         * Sets the chart width and propagates it to caption and footer.
         * Mirrors Java: AbstractChart.setChartWidth(int chartWidth).
         *
         * @param {number} chartWidth
         */
        setChartWidth(chartWidth) {
            super.setChartWidth(chartWidth);
            if (this.captionElement) this.captionElement.width = this.chartWidth;
            if (this.footerElement)  this.footerElement.width  = this.chartWidth;
        }

        /** Delegates to captionElement.draw(svg). */
        drawCaption(svg) {
            if (this.captionElement) this.captionElement.draw(svg);
        }

        /** Delegates to footerElement.draw(svg). */
        drawFooter(svg) {
            if (this.footerElement) this.footerElement.draw(svg);
        }
    }

    // ── AbstractRenderer ───────────────────────────────────────────────────────
    // Mirrors Java: AbstractRenderer
    // Base class for chart content renderers.

    class AbstractRenderer {
        constructor() {
            /** Computed chart content width (without caption/footer). */
            this.chartWidth  = 0;
            /** Computed chart content height (without caption/footer). */
            this.chartHeight = 0;
            /** CalendarXAxes instance for rendering the time header. */
            this.calendarXAxes = null;
            /** Theme instance. */
            this.theme = null;
        }

        /**
         * Abstract: compute chart content height.
         * Mirrors Java: AbstractRenderer.calculateChartHeight().
         *
         * @returns {number}
         */
        calculateChartHeight() { return this.chartHeight; }

        /**
         * Abstract: compute chart content width.
         * Mirrors Java: AbstractRenderer.calculateChartWidth().
         *
         * @returns {number}
         */
        calculateChartWidth() { return this.chartWidth; }

        /**
         * Abstract: draw chart content into the SVG.
         * Mirrors Java: AbstractRenderer.draw(ExtendedGraphics2D, int x, int y).
         *
         * @param {SVGSVGElement} svg
         * @param {number}        x    X offset (always 0 for full-width charts)
         * @param {number}        y    Y offset = captionElement.height
         */
        draw(svg, x, y) { /* to be overridden */ }
    }

    // ── Export to global scope ─────────────────────────────────────────────────

    window.AbstractCanvas   = AbstractCanvas;
    window.AbstractChart    = AbstractChart;
    window.AbstractRenderer = AbstractRenderer;
})();

