// caption-element.ts
// Renders a single-line chart title at the top of the SVG canvas.
// Mirrors Java: CaptionElement
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import { intToHex }                  from './color-utils.js';
import { createText }                from './svg-utils.js';
import { Theme }                     from './theme/theme.js';

export class CaptionElement {
    text:   string | null;
    height: number;
    width:  number;
    x:      number;
    y:      number;
    private _theme: Theme;

    /**
     * @param text          Caption text (null → height = 0, nothing drawn)
     * @param _relateCssPath CSS path prefix (not used in SVG output)
     * @param theme         Theme instance (provides chartTheme.captionTextColor)
     */
    constructor(text: string | null, _relateCssPath: string, theme: Theme) {
        this.text   = text;
        this.height = text != null ? 26 : 0;
        this.width  = 0;
        this.x      = 3;
        this.y      = 0;
        this._theme = theme;
    }

    /**
     * Draws the caption into the given SVG element.
     * Mirrors Java: CaptionElement.draw(ExtendedGraphics2D).
     */
    draw(svg: SVGElement): void {
        if (!this.text) return;
        const textColor = intToHex(this._theme.chartTheme.captionTextColor, '#2c7bf4');
        const textY     = this.y + Math.floor(this.height / 2) + 7;
        svg.appendChild(createText(this.x, textY, this.text, {
            fill:          textColor,
            'font-size':   '18',
            'font-family': 'sans-serif',
        }));
    }
}

