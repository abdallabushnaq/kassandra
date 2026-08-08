// caption-element.ts
// Renders a single-line chart title at the top of the SVG canvas.
// Mirrors Java: CaptionElement
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {ColorUtils} from './ColorUtils.js';
import {FontSpec} from './FontSpec.js';
import {Theme} from './theme/Theme.js';
import {TextElement} from './TextElement.js';

export class CaptionElement extends TextElement {
    private static readonly HELP_TEXT_WIDTH = 300;

    private readonly helpFont = new FontSpec('sans-serif', 10, FontSpec.PLAIN);

    /**
     * @param leftText      Caption text at the left corner
     * @param rightText     Caption text at the right corner
     * @param _relateCssPath CSS path prefix (not used in SVG output)
     * @param theme         Theme instance (provides chartTheme.captionTextColor)
     */
    constructor(leftText: string | null, rightText: string | null, _relateCssPath: string, theme: Theme) {
        super(leftText, rightText, theme, new FontSpec('sans-serif', 18, FontSpec.PLAIN), 26, 3, 0);
    }

    override draw(svg: SVGElement): void {
        if (this.leftText)
            this.drawText(svg, this.leftText, this.x, this.getTextY(false, 0), false);
        if (!this.rightText)
            return;

        const rightTextX = this.width - 4;
        const leftTextX = Math.max(this.x, rightTextX - CaptionElement.HELP_TEXT_WIDTH);
        for (const [lineIndex, line] of this.rightText.split('|').entries()) {
            const [leftText, rightText] = line.split(';', 2).map((part) => part.trim());
            const textY = this.getTextY(true, lineIndex);
            if (leftText)
                this.drawText(svg, leftText, leftTextX, textY, false, this.helpFont);
            if (rightText)
                this.drawText(svg, rightText, rightTextX, textY, true);
        }
    }

    protected getTextColor(): string {
        return ColorUtils.intToHex(this.theme.chartTheme.captionTextColor, '#2c7bf4');
    }

    protected getFont(rightAligned: boolean): FontSpec {
        return rightAligned ? this.helpFont : this.font;
    }

    protected getTextY(rightAligned: boolean, lineIndex: number): number {
        if (rightAligned)
            return this.y + 10 + lineIndex * 11;
        return this.y + Math.floor(this.height / 2) + 7;
    }
}
