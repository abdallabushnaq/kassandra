// color-utils.ts
// Shared color utility functions for theme and color conversions.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

/**
 * Converts a 0xRRGGBB integer (as sent by the server ThemeDto) to a CSS hex color string.
 * Mirrors the inverse of Java ThemeDto.rgb() helper.
 *
 * @param value   0xRRGGBB integer, or null/undefined/string
 * @param fallback default color when value is null (defaults to '#ffffff')
 * @returns CSS hex color string, e.g. '#3a7bc8'
 */
export function intToHex(value: number | string | null | undefined, fallback = '#ffffff'): string {
    if (value == null) return fallback;
    if (typeof value === 'string') return value;
    if (typeof value === 'number') return '#' + (value >>> 0).toString(16).padStart(6, '0').slice(-6);
    return fallback;
}

/**
 * Converts a Java-encoded sprint color (#rrggbbaa) to an SVG rgba() string.
 * Falls back to default blue if color is missing or invalid.
 *
 * @param hexColorWithAlpha Sprint color in #rrggbbaa format
 * @returns SVG rgba() color string or fallback color
 */
export function convertSprintColorToRgba(hexColorWithAlpha: string | null | undefined): string {
    if (!hexColorWithAlpha) return 'rgba(31,143,255,0.31)';
    if (/^#[0-9a-fA-F]{8}$/.test(hexColorWithAlpha)) {
        const red = parseInt(hexColorWithAlpha.slice(1, 3), 16);
        const green = parseInt(hexColorWithAlpha.slice(3, 5), 16);
        const blue = parseInt(hexColorWithAlpha.slice(5, 7), 16);
        const alpha = (parseInt(hexColorWithAlpha.slice(7, 9), 16) / 255).toFixed(3);
        return `rgba(${red},${green},${blue},${alpha})`;
    }
    return hexColorWithAlpha;
}

/**
 * Converts an 8-digit #rrggbbaa hex color string to rgba() with a specific alpha override.
 * Used to render task segments with a different transparency (e.g. non-working-day segments).
 *
 * @param hexColorWithAlpha Color in #rrggbbaa format
 * @param alphaOverride     New alpha value 0–255 (defaults to 64)
 * @returns rgba() color string
 */
export function hexToRgbaWithAlpha(hexColorWithAlpha: string | null | undefined, alphaOverride?: number | null): string {
    if (!hexColorWithAlpha) return 'rgba(31,143,255,0.25)';
    if (/^#[0-9a-fA-F]{8}$/.test(hexColorWithAlpha)) {
        const red = parseInt(hexColorWithAlpha.slice(1, 3), 16);
        const green = parseInt(hexColorWithAlpha.slice(3, 5), 16);
        const blue = parseInt(hexColorWithAlpha.slice(5, 7), 16);
        const alpha = ((alphaOverride != null ? alphaOverride : 64) / 255).toFixed(3);
        return `rgba(${red},${green},${blue},${alpha})`;
    }
    return hexColorWithAlpha;
}

export class ColorUtil {
    static WHITE: number = 0xffffff;
    static BLACK: number = 0x000000;

    static calculateColorBlending(aColor: number, background: number): number {
        //        logger.info(String.format("r=%d, g=%d, b=%s, a=%d %08X", aColor.getRed(), aColor.getGreen(), aColor.getBlue(), aColor.getAlpha(), aColor.getRGB()));
        const alpha = (aColor >> 24) & 0xff;
        const red = ((aColor >> 16) & 0xff * alpha) / 255 + ((background >> 16) & 0xff * (255 - alpha)) / 255;
        const green = ((aColor >> 8) & 0xff * alpha) / 255 + ((background >> 8) & 0xff * (255 - alpha)) / 255;
        const blue = (aColor & 0xff * alpha) / 255 + (background & 0xff * (255 - alpha)) / 255;
        const c = (red << 16) | (green << 8) | blue;
        //        logger.info(String.format("r=%d, g=%d, b=%s, a=%d %08X", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha(), c.getRGB()));
        return c;
    }

    /**
     * Returns either {@link Color#white} or {@link Color#black}, whichever provides the higher
     * perceptual contrast against the given fully-opaque color.
     * Uses the luminance formula {@code 0.299·R + 0.587·G + 0.114·B}; colors with a luminance
     * below 127 are considered dark, so white is returned, and vice versa.
     *
     * @param aColor the background color (alpha channel is ignored)
     * @return {@link Color#white} for dark backgrounds, {@link Color#black} for light backgrounds
     */
    static heighestContrast(aColor: number): number {
        const red = (aColor >> 16) & 0xff;
        const green = (aColor >> 8) & 0xff;
        const blue = aColor & 0xff;
        //0.299*R + 0.587*G + 0.114*B
        const light = ((red * 299) / 1000) + ((green * 587) / 1000) + ((blue * 114) / 1000);
        if (light < 127) {
            return ColorUtil.WHITE;
        } else {
            return ColorUtil.BLACK;
        }
    }

}

