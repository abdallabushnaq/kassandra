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
import {Theme} from './theme/Theme.js';
import {FontSpec} from './FontSpec.js';
import {TextElement} from './TextElement.js';

/**
 * Renders copyright text on the left and sprint key on the right.
 * Mirrors Java: FooterElement
 *
 */
export class FooterElement extends TextElement {

    /**
     * @param leftText  Footer text at the left corner (e.g. copyright string)
     * @param rightText Footer text at the right corner (e.g. sprint name)
     * @param theme Theme instance (provides chartTheme.footerTextColor)
     */
    constructor(leftText: string | null, rightText: string | null, theme: Theme) {
        super(leftText, rightText, theme, new FontSpec(FontSpec.SANS_SERIF, 10, FontSpec.PLAIN), 14, 3, 1);
    }

    protected getTextColor(): string {
        return ColorUtils.intToHex(this.theme.chartTheme.footerTextColor, '#2c7bf4');
    }

    protected getTextY(_rightAligned: boolean, _lineIndex: number): number {
        return this.y + this.font.maxAscent + 1;
    }
}
