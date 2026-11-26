// caption-element.ts
// Renders a single-line chart title at the top of the SVG canvas.
// Mirrors Java: CaptionElement
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {ColorUtils} from './ColorUtils.js';
import {FontMetrics} from './FontMetrics.js';
import {FontSpec} from './FontSpec.js';
import {SvgUtils} from './SvgUtils.js';
import {Theme} from './theme/Theme.js';
import {TextElement} from './TextElement.js';
import {ZoomThreshold} from './ZoomThreshold.js';

export class CaptionElement extends TextElement {
    private static readonly HELP_TEXT_WIDTH = 350;
    private static readonly ZOOM_METER_WIDTH = 256;
    private static readonly ZOOM_METER_HEIGHT = 14;
    private static readonly ZOOM_THRESHOLD_HIT_WIDTH = 6;

    private readonly helpFont = new FontSpec('sans-serif', 10, FontSpec.PLAIN);
    private readonly captionFontMetrics: FontMetrics;
    private timelineMetrics: {
        dayWidth: number;
        visualScale: number;
        minEffectiveDayWidth: number;
        maxEffectiveDayWidth: number;
    } | null = null;
    private zoomThresholds: ZoomThreshold[] = [];

    /**
     * @param leftText      Caption text at the left corner
     * @param rightText     Caption text at the right corner
     * @param _relateCssPath CSS path prefix (not used in SVG output)
     * @param theme         Theme instance (provides chartTheme.captionTextColor)
     */
    constructor(leftText: string | null, rightText: string | null, _relateCssPath: string, theme: Theme) {
        super(leftText, rightText, theme, new FontSpec('sans-serif', 18, FontSpec.PLAIN), 26, 3, 0);
        this.captionFontMetrics = new FontMetrics(this.font);
    }

    /** Updates the metrics shown with the timeline interaction hints. */
    public updateTimelineMetrics(
        dayWidth: number,
        visualScale: number,
        minEffectiveDayWidth: number,
        maxEffectiveDayWidth: number,
        zoomThresholds: ZoomThreshold[],
    ): void {
        this.timelineMetrics = {dayWidth, visualScale, minEffectiveDayWidth, maxEffectiveDayWidth};
        this.zoomThresholds = zoomThresholds;
    }

    override draw(svg: SVGElement): void {
        if (this.leftText)
            this.drawText(svg, this.leftText, this.x, this.getTextY(false, 0), false);
        this.drawZoomMeter(svg);
        if (!this.rightText)
            return;

        const rightTextX = this.width - 4;
        const leftTextX = Math.max(this.x, rightTextX - CaptionElement.HELP_TEXT_WIDTH);
        for (const [lineIndex, line] of this.rightText.split('|').entries()) {
            let [leftText, rightText] = line.split(';', 2).map((part) => part.trim());
            if (lineIndex === 0 && this.timelineMetrics) {
                const effectiveDayWidth = this.timelineMetrics.dayWidth * this.timelineMetrics.visualScale;
                leftText = `${leftText} (${Number(effectiveDayWidth.toFixed(1))}px)`;
                rightText = `${rightText} (${Math.round(this.timelineMetrics.visualScale * 100)}%)`;
            }
            const textY = this.getTextY(true, lineIndex);
            if (leftText)
                this.drawText(svg, leftText, leftTextX, textY, false, this.helpFont);
            if (rightText)
                this.drawText(svg, rightText, rightTextX, textY, true);
        }
    }

    protected getTextColor(): string {
        return ColorUtils.intToHex(this.theme.chartTheme.captionTextColor);
    }

    protected getFont(rightAligned: boolean): FontSpec {
        return rightAligned ? this.helpFont : this.font;
    }

    protected getTextY(rightAligned: boolean, lineIndex: number): number {
        if (rightAligned)
            return this.y + 10 + lineIndex * 11;
        return this.y + Math.floor(this.height / 2) + 7;
    }

    private drawZoomMeter(svg: SVGElement): void {
        const metrics = this.timelineMetrics;
        if (!metrics)
            return;

        const minEffectiveDayWidth = metrics.minEffectiveDayWidth;
        const maxEffectiveDayWidth = metrics.maxEffectiveDayWidth;
        const range = maxEffectiveDayWidth - minEffectiveDayWidth;
        if (!Number.isFinite(range) || range <= 0)
            throw new RangeError('The zoom meter requires a finite, positive effective day-width range');

        const leftTextEnd = this.x + (this.leftText ? this.captionFontMetrics.stringWidth(this.leftText) : 0) + 8;
        const rightTextStart = this.rightText ? this.width - 4 - CaptionElement.HELP_TEXT_WIDTH : this.width - 4;
        const availableWidth = rightTextStart - leftTextEnd;
        if (availableWidth < CaptionElement.ZOOM_METER_WIDTH)
            return;

        const meterX = leftTextEnd + (availableWidth - CaptionElement.ZOOM_METER_WIDTH) / 2;
        const meterY = this.y + (this.height - CaptionElement.ZOOM_METER_HEIGHT) / 2 - 2;
        const isDarkTheme = this.theme.themeVariance.toLowerCase() === 'dark';
        const trackBorderColor = isDarkTheme ? '#64748b' : '#94a3b8';
        const thresholdColor = isDarkTheme ? '#cbd5e1' : '#475569';
        const currentZoomColor = isDarkTheme ? '#fb7185' : '#e11d48';
        const meter = SvgUtils.createGroup({'class': 'zoom-meter'});
        const track = SvgUtils.createRect(meterX, meterY, CaptionElement.ZOOM_METER_WIDTH, CaptionElement.ZOOM_METER_HEIGHT, {
            'class': 'zoom-meter-track',
            fill: ColorUtils.intToHex(this.theme.chartTheme.backgroundColor),
            stroke: trackBorderColor,
            'stroke-width': '1',
            'data-min-effective-day-width': minEffectiveDayWidth,
            'data-max-effective-day-width': maxEffectiveDayWidth,
        });
        track.appendChild(SvgUtils.createTitle(
            `Zoom range: ${this.formatDayWidth(minEffectiveDayWidth)} to ${this.formatDayWidth(maxEffectiveDayWidth)} effective px/day (minimum at left, maximum at right)`,
        ));
        meter.appendChild(track);

        const thresholdsByWidth = new Map<number, string[]>();
        for (const threshold of this.zoomThresholds) {
            const descriptions = thresholdsByWidth.get(threshold.effectiveDayWidth) || [];
            descriptions.push(threshold.description);
            thresholdsByWidth.set(threshold.effectiveDayWidth, descriptions);
        }

        for (const [effectiveDayWidth, descriptions] of thresholdsByWidth) {
            const x = this.getZoomMeterX(meterX, effectiveDayWidth, minEffectiveDayWidth, range);
            const tooltip = descriptions.join('\n');
            const line = SvgUtils.createLine(x, meterY + 1, x, meterY + CaptionElement.ZOOM_METER_HEIGHT - 1, {
                'class': 'zoom-meter-threshold',
                stroke: thresholdColor,
                'stroke-width': '1.5',
                'data-effective-day-width': effectiveDayWidth,
            });
            line.appendChild(SvgUtils.createTitle(tooltip));
            meter.appendChild(line);

            const hitArea = SvgUtils.createRect(
                x - CaptionElement.ZOOM_THRESHOLD_HIT_WIDTH / 2,
                meterY,
                CaptionElement.ZOOM_THRESHOLD_HIT_WIDTH,
                CaptionElement.ZOOM_METER_HEIGHT,
                {
                    'class': 'zoom-meter-threshold-hit-area',
                    fill: 'transparent',
                    stroke: 'none',
                    'pointer-events': 'all',
                    cursor: 'help',
                    'data-effective-day-width': effectiveDayWidth,
                },
            );
            hitArea.appendChild(SvgUtils.createTitle(tooltip));
            meter.appendChild(hitArea);
        }

        const currentEffectiveDayWidth = metrics.dayWidth * metrics.visualScale;
        const currentX = this.getZoomMeterX(meterX, currentEffectiveDayWidth, minEffectiveDayWidth, range);
        const currentLine = SvgUtils.createLine(
            currentX,
            meterY + 1,
            currentX,
            meterY + CaptionElement.ZOOM_METER_HEIGHT - 1,
            {
                'class': 'zoom-meter-current',
                stroke: currentZoomColor,
                'stroke-width': '2',
                'data-effective-day-width': currentEffectiveDayWidth,
            },
        );
        currentLine.appendChild(SvgUtils.createTitle(
            `Current zoom: ${this.formatDayWidth(currentEffectiveDayWidth)} effective px/day`,
        ));
        meter.appendChild(currentLine);
        svg.appendChild(meter);
    }

    private getZoomMeterX(meterX: number, value: number, minEffectiveDayWidth: number, range: number): number {
        const position = Math.max(0, Math.min(1, (value - minEffectiveDayWidth) / range));
        return meterX + position * CaptionElement.ZOOM_METER_WIDTH;
    }

    private formatDayWidth(dayWidth: number): string {
        return String(Number(dayWidth.toFixed(2)));
    }
}
