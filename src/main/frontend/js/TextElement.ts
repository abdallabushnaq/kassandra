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
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

import {FontSpec} from './FontSpec.js';
import {SvgUtils} from './SvgUtils.js';
import {Theme} from './theme/Theme.js';

/** Renders optional text at the left and right corners of a chart area. */
export abstract class TextElement {
    public width: number;
    public height: number;
    public x: number;
    public y: number;

    protected constructor(
        public readonly leftText: string | null,
        public readonly rightText: string | null,
        protected readonly theme: Theme,
        protected readonly font: FontSpec,
        height: number,
        x: number,
        y: number,
    ) {
        this.height = leftText !== null || rightText !== null ? height : 0;
        this.width = 0;
        this.x = x;
        this.y = y;
    }

    /** Draws the left and right text into the given SVG element. */
    public draw(svg: SVGElement): void {
        if (!this.leftText && !this.rightText)
            return;

        if (this.leftText)
            this.drawLines(svg, this.leftText, false);
        if (this.rightText)
            this.drawLines(svg, this.rightText, true);
    }

    /** Returns the font used for text in the requested corner. */
    protected getFont(rightAligned: boolean): FontSpec {
        return this.font;
    }

    /** Splits text into the SVG lines rendered in the requested corner. */
    protected getTextLines(text: string, _rightAligned: boolean): string[] {
        return [text];
    }

    /** Returns the text color in SVG notation. */
    protected abstract getTextColor(): string;

    /** Returns the SVG baseline for a text line in the requested corner. */
    protected abstract getTextY(rightAligned: boolean, lineIndex: number): number;

    /** Draws one text line at the supplied position. */
    protected drawText(
        svg: SVGElement,
        text: string,
        x: number,
        y: number,
        rightAligned: boolean,
        font: FontSpec = this.getFont(rightAligned),
    ): void {
        const attributes = {
            fill: this.getTextColor(),
            'font-size': font.size,
            'font-family': font.family,
            ...(rightAligned ? {'text-anchor': 'end'} : {}),
        };
        svg.appendChild(SvgUtils.createText(x, y, text, attributes));
    }

    private drawLines(svg: SVGElement, text: string, rightAligned: boolean): void {
        for (const [lineIndex, line] of this.getTextLines(text, rightAligned).entries())
            this.drawText(
                svg,
                line,
                rightAligned ? this.width - 4 : this.x,
                this.getTextY(rightAligned, lineIndex),
                rightAligned,
            );
    }
}
