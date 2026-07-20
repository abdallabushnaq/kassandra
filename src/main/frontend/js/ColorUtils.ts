// color-utils.ts
// Shared color utility functions for theme and color conversions.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

export class ColorUtils {
    static WHITE: number = 0xffffff;
    static BLACK: number = 0x000000;

    /**
     * Converts a 0xAARRGGBB integer (as sent by Java with alpha encoded via {@code alpha << 24})
     * to a CSS hex color string in #rrggbbaa format.
     * Mirrors the inverse of Java ThemeDto.rgb() helper.
     *
     * @param value   0xAARRGGBB integer with alpha in bits 24–31, or null/undefined/string
     * @param fallback default color when value is null (defaults to '#ffffff')
     * @returns CSS hex color string, e.g. '#3a7bc8ff'
     */
    static intToHexWithAlpha(value: number | string | null | undefined, fallback = '#ffffff'): string {
        if (value == null) return fallback;
        if (typeof value === 'string') return value;
        if (typeof value === 'number') {
            const unsigned = value >>> 0;
            const r = (unsigned >> 16) & 0xff;
            const g = (unsigned >> 8) & 0xff;
            const b = unsigned & 0xff;
            const a = (unsigned >> 24) & 0xff;
            return '#' + [r, g, b, a].map(x => x.toString(16).padStart(2, '0')).join('');
        }
        return fallback;
    }

    /**
     * Converts a 0xRRGGBB integer (as sent by the server ThemeDto) to a CSS hex color string.
     * Mirrors the inverse of Java ThemeDto.rgb() helper.
     *
     * @param value   0xRRGGBB integer, or null/undefined/string
     * @param fallback default color when value is null (defaults to '#ffffff')
     * @returns CSS hex color string, e.g. '#3a7bc8'
     */
    static intToHex(value: number | string | null | undefined, fallback = '#ffffff'): string {
        if (value == null)
            return fallback;
        if (typeof value === 'string')
            return value;
        if (typeof value === 'number')
            return '#' + (value >>> 0).toString(16).padStart(6, '0').slice(-6);
        return fallback;
    }

    /**
     * Converts a Java-encoded sprint color (#rrggbbaa) to an SVG rgba() string.
     * Falls back to default blue if color is missing or invalid.
     *
     * @param hexColorWithAlpha Sprint color in #rrggbbaa format
     * @returns SVG rgba() color string or fallback color
     */
    static convertSprintColorToRgba(hexColorWithAlpha: string | null | undefined): string {
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
    static hexToRgbaWithAlpha(hexColorWithAlpha: string | null | undefined, alphaOverride?: number | null): string {
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
    static highestContrast(aColor: number): number {
        const red = (aColor >> 16) & 0xff;
        const green = (aColor >> 8) & 0xff;
        const blue = aColor & 0xff;
        //0.299*R + 0.587*G + 0.114*B
        const light = ((red * 299) / 1000) + ((green * 587) / 1000) + ((blue * 114) / 1000);
        if (light < 127) {
            return ColorUtils.WHITE;
        } else {
            return ColorUtils.BLACK;
        }
    }

    /**
     * Lightens a 0xRRGGBB color by blending it towards white.
     * Mirrors Java: ColorUtil.lightenColor(color, factor).
     * @param color  0xRRGGBB integer
     * @param factor 0.0 = no change, 1.0 = full white
     */
    static lightenColor(color: number, factor: number): number {
        const r = (color >> 16) & 0xff;
        const g = (color >> 8) & 0xff;
        const b = color & 0xff;
        const lr = Math.round(r + (255 - r) * factor);
        const lg = Math.round(g + (255 - g) * factor);
        const lb = Math.round(b + (255 - b) * factor);
        return (lr << 16) | (lg << 8) | lb;
    }

    /**
     * Converts a 0xRRGGBB integer + separate alpha (0–255) to an rgba() CSS string.
     * @param color  0xRRGGBB integer (or null → fallback)
     * @param alpha  0–255 alpha
     * @param fallback default when color is null
     */
    static intToRgba(color: number | null | undefined, alpha: number, fallback = 'rgba(0,0,0,0.5)'): string {
        if (color == null) return fallback;
        const r = (color >> 16) & 0xff;
        const g = (color >> 8) & 0xff;
        const b = color & 0xff;
        return `rgba(${r},${g},${b},${(alpha / 255).toFixed(3)})`;
    }

    /**
     * Returns a new {@link Color} identical to the given color but with the specified alpha value.
     *
     * @param c1    the source color (RGB components are preserved)
     * @param alpha the new alpha value in the range {@code [0, 255]};
     *              {@code 0} is fully transparent, {@code 255} is fully opaque
     * @return a new {@link Color} with the same RGB components and the new alpha
     */
    static setAlpha(c1: number, alpha: number): number {
        return (c1 & 0x00ffffff) | ((alpha & 0xff) << 24);
    }

}
