/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

import {ColorUtils} from './ColorUtils.js';
import {SvgUtils} from './SvgUtils.js';
import {Theme} from './theme/Theme.js';
import {FontSpec} from "./FontSpec.js";

/**
 * Renders copyright text on the left and sprint key on the right.
 * Mirrors Java: FooterElement
 *
 */
export class FooterElement {
    public width: number;
    public height: number;
    public y: number;
    private readonly text: string | null;
    private readonly key: string | null;
    private readonly x: number;
    private readonly font: FontSpec;
    private theme: Theme;

    /**
     * @param text  Footer left text (e.g. copyright string; null → height = 0)
     * @param key   Footer right text (e.g. sprint name)
     * @param theme Theme instance (provides chartTheme.footerTextColor)
     */
    constructor(text: string | null, key: string | null, theme: Theme) {
        this.text = text;
        this.key = key;
        this.font = new FontSpec(FontSpec.SANS_SERIF, 10, FontSpec.PLAIN);
        this.height = text != null ? 14 : 0;
        this.width = 0;
        this.x = 3;
        this.y = 1;
        this.theme = theme;
    }

    /**
     * Draws the footer into the given SVG element.
     * Mirrors Java: FooterElement.draw(Graphics2D).
     */
    public draw(svg: SVGElement): void {
        if (this.text || this.key) {
            const textColor = ColorUtils.intToHex(this.theme.chartTheme.footerTextColor, '#2c7bf4');
            const maxAscent = this.font.maxAscent;
            const textY = this.y + maxAscent + 1;
            if (this.text) {
                svg.appendChild(SvgUtils.createText(this.x, textY, this.text, {
                    fill: textColor,
                    'font-size': this.font.size,
                    'font-family': this.font.family,
                }));
            }

            if (this.key) {
                svg.appendChild(SvgUtils.createText(this.width - 4, textY, this.key, {
                    fill: textColor,
                    'font-size': this.font.size,
                    'font-family': this.font.family,
                    'text-anchor': 'end',
                }));
            }
        }
    }
}

