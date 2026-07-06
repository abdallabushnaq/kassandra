// footer-element.ts
// Renders copyright text on the left and sprint key on the right.
// Mirrors Java: FooterElement
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {ColorUtils} from './color-utils.js';
import {SvgUtils} from './svg-utils.js';
import {Theme} from './theme/theme.js';
import {FontSpec} from "./font-spec.js";

export class FooterElement {
    text: string | null;
    key: string;
    height: number;
    width: number;
    x: number;
    y: number;
    private readonly font: FontSpec;
    private _theme: Theme;

    /**
     * @param text  Footer left text (e.g. copyright string; null → height = 0)
     * @param key   Footer right text (e.g. sprint name)
     * @param theme Theme instance (provides chartTheme.footerTextColor)
     */
    constructor(text: string | null, key: string, theme: Theme) {
        this.text = text;
        this.key = key || '';
        this.font = new FontSpec(FontSpec.SANS_SERIF, 10, FontSpec.PLAIN);
        this.height = text != null ? 14 : 0;
        this.width = 0;
        this.x = 3;
        this.y = 1;
        this._theme = theme;
    }

    /**
     * Draws the footer into the given SVG element.
     * Mirrors Java: FooterElement.draw(Graphics2D).
     */
    draw(svg: SVGElement): void {
        if (!this.text) return;
        const textColor = ColorUtils.intToHex(this._theme.chartTheme.footerTextColor, '#2c7bf4');
        const fontSize = this.font && 'size' in this.font ? String(this.font.size) : '10';
        const maxAscent = this.font.maxAscent;
        const textY = this.y + maxAscent + 1;

        svg.appendChild(SvgUtils.createText(this.x, textY, this.text, {
            fill: textColor,
            'font-size': fontSize,
            'font-family': 'sans-serif',
        }));

        if (this.key) {
            const approxKeyWidth = this.key.length * 5;
            const keyX = Math.max(this.x + 10, this.width - approxKeyWidth - 1);
            svg.appendChild(SvgUtils.createText(keyX, textY, this.key, {
                fill: textColor,
                'font-size': fontSize,
                'font-family': 'sans-serif',
                'text-anchor': 'start',
            }));
        }
    }
}

