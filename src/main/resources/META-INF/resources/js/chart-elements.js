// chart-elements.js
// CaptionElement and FooterElement – SVG-drawing helpers.
// Mirrors Java: CaptionElement, FooterElement
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0
(function () {
    'use strict';

    var createSvgElement = window.SvgUtils.createSvgElement;
    var createRect       = window.SvgUtils.createRect;
    var createText       = window.SvgUtils.createText;
    var intToHex         = window.ColorUtils.intToHex;

    // ── CaptionElement ─────────────────────────────────────────────────────────
    // Mirrors Java: CaptionElement
    // Renders a single-line chart title at the top of the SVG canvas.

    class CaptionElement {
        /**
         * @param {string}  text   Caption text (null → height = 0, nothing drawn)
         * @param {string}  relateCssPath  CSS path prefix (not used in SVG output)
         * @param {Theme}   theme  Theme instance (provides chartTheme.captionTextColor)
         */
        constructor(text, relateCssPath, theme) {
            /** Caption text to display; null means no caption. */
            this.text   = text;
            /** Height reserved for the caption row.  Mirrors Java CaptionElement.height = 26. */
            this.height = (text != null) ? 26 : 0;
            /** Total SVG width – set by AbstractChart.setChartWidth() */
            this.width  = 0;
            /** x offset of the text – mirrors Java CaptionElement.x = 3 */
            this.x      = 3;
            /** y offset of the caption area – mirrors Java CaptionElement.y = 0 */
            this.y      = 0;
            this._theme = theme;
        }

        /**
         * Draws the caption into the given SVG element.
         * Mirrors Java: CaptionElement.draw(ExtendedGraphics2D).
         * Font: sans-serif 18px plain.
         * Y position: y + height/2 + (ascent-2)/2 – 1 ≈ y + 16 (for 18px font).
         *
         * @param {SVGSVGElement} svg  Target SVG element
         */
        draw(svg) {
            if (!this.text) return;
            var textColor = intToHex(this._theme.chartTheme.captionTextColor, '#2c7bf4');
            // Java: g2.drawString(text, x, y + height/2 + (ascent-2)/2 - 1)
            // For 18px font: ascent ≈ 15, so y + 13 + 6 - 1 = y + 18
            var textY = this.y + Math.floor(this.height / 2) + 7;
            svg.appendChild(createText(this.x, textY, this.text, {
                fill: textColor,
                'font-size': '18',
                'font-family': 'sans-serif'
            }));
        }
    }

    // ── FooterElement ──────────────────────────────────────────────────────────
    // Mirrors Java: FooterElement
    // Renders copyright text on the left and sprint key on the right.

    class FooterElement {
        /**
         * @param {string}  text   Footer left text (e.g. copyright string; null → height = 0)
         * @param {string}  key    Footer right text (e.g. sprint name)
         * @param {Theme}   theme  Theme instance (provides chartTheme.footerTextColor)
         */
        constructor(text, key, theme) {
            /** Left-side footer text (copyright). */
            this.text   = text;
            /** Right-side footer text (sprint key/name). */
            this.key    = key || '';
            /** Height reserved for footer row. Mirrors Java FooterElement.height = 14. */
            this.height = (text != null) ? 14 : 0;
            /** Total SVG width – set by AbstractChart */
            this.width  = 0;
            /** x offset of the left text – mirrors Java FooterElement.x = 3 */
            this.x      = 3;
            /** y offset of the footer row – set by AbstractChart after renderer height is known */
            this.y      = 1;
            this._theme = theme;
        }

        /**
         * Draws the footer into the given SVG element.
         * Mirrors Java: FooterElement.draw(Graphics2D).
         * Font: sans-serif 10px plain.
         *
         * @param {SVGSVGElement} svg  Target SVG element
         */
        draw(svg) {
            if (!this.text) return;
            var textColor = intToHex(this._theme.chartTheme.footerTextColor, '#2c7bf4');
            // Java: g2.drawString(text, x, y + maxAscent - 2)
            // For 10px font: maxAscent ≈ 10, so textY = y + 8
            var textY = this.y + 8;
            // Left: copyright text
            svg.appendChild(createText(this.x, textY, this.text, {
                fill: textColor,
                'font-size': '10',
                'font-family': 'sans-serif'
            }));
            // Right: key (sprint name), right-aligned
            if (this.key) {
                // Approximate right-alignment: key.length * 5 px per char
                var approxKeyWidth = this.key.length * 5;
                var keyX = Math.max(this.x + 10, this.width - approxKeyWidth - 1);
                svg.appendChild(createText(keyX, textY, this.key, {
                    fill: textColor,
                    'font-size': '10',
                    'font-family': 'sans-serif',
                    'text-anchor': 'start'
                }));
            }
        }
    }

    // ── Export to global scope ─────────────────────────────────────────────────

    window.CaptionElement = CaptionElement;
    window.FooterElement  = FooterElement;
})();

