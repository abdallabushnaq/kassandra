// footer-element.ts
// Renders copyright text on the left and sprint key on the right.
// Mirrors Java: FooterElement
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {intToHex} from './color-utils.js';
import {createText} from './svg-utils.js';
import {Theme} from './theme/theme.js';

export class FooterElement {
    text: string | null;
    key: string;
    height: number;
    width: number;
    x: number;
    y: number;
    private _theme: Theme;

    /**
     * @param text  Footer left text (e.g. copyright string; null → height = 0)
     * @param key   Footer right text (e.g. sprint name)
     * @param theme Theme instance (provides chartTheme.footerTextColor)
     */
    constructor(text: string | null, key: string, theme: Theme) {
        this.text = text;
        this.key = key || '';
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
        const textColor = intToHex(this._theme.chartTheme.footerTextColor, '#2c7bf4');
        const textY = this.y + 8;

        svg.appendChild(createText(this.x, textY, this.text + "na so was 2", {
            fill: textColor,
            'font-size': '10',
            'font-family': 'sans-serif',
        }));

        if (this.key) {
            const approxKeyWidth = this.key.length * 5;
            const keyX = Math.max(this.x + 10, this.width - approxKeyWidth - 1);
            svg.appendChild(createText(keyX, textY, this.key, {
                fill: textColor,
                'font-size': '10',
                'font-family': 'sans-serif',
                'text-anchor': 'start',
            }));
        }
    }
}

