// color-utils.js
// Shared color utility functions for theme and color conversions.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0
(function () {
    'use strict';

    /**
     * Converts a 0xRRGGBB integer (as sent by the server ThemeDto) to a CSS hex color string.
     * Mirrors the inverse of Java ThemeDto.rgb() helper.
     *
     * @param {number|null} value  0xRRGGBB integer, or null/undefined
     * @param {string} [fallback]  default color when value is null (defaults to '#ffffff')
     * @returns {string} CSS hex color string, e.g. '#3a7bc8'
     */
    function intToHex(value, fallback) {
        if (fallback === undefined) fallback = '#ffffff';
        if (value == null) return fallback;
        if (typeof value === 'string') return value;
        if (typeof value === 'number') return '#' + (value >>> 0).toString(16).padStart(6, '0').slice(-6);
        return fallback;
    }

    /**
     * Converts a Java-encoded sprint color (#rrggbbaa) to an SVG rgba() string.
     * Falls back to default blue if color is missing or invalid.
     *
     * @param {string} hexColorWithAlpha Sprint color in #rrggbbaa format
     * @returns {string} SVG rgba() color string or fallback color
     */
    function convertSprintColorToRgba(hexColorWithAlpha) {
        if (!hexColorWithAlpha) return 'rgba(31,143,255,0.31)';
        if (/^#[0-9a-fA-F]{8}$/.test(hexColorWithAlpha)) {
            var red   = parseInt(hexColorWithAlpha.slice(1, 3), 16);
            var green = parseInt(hexColorWithAlpha.slice(3, 5), 16);
            var blue  = parseInt(hexColorWithAlpha.slice(5, 7), 16);
            var alpha = (parseInt(hexColorWithAlpha.slice(7, 9), 16) / 255).toFixed(3);
            return 'rgba(' + red + ',' + green + ',' + blue + ',' + alpha + ')';
        }
        return hexColorWithAlpha;
    }

    /**
     * Converts an 8-digit #rrggbbaa hex color string to rgba() with a specific alpha override.
     * Used to render task segments with a different transparency (e.g. non-working-day segments).
     *
     * @param {string} hexColorWithAlpha  Color in #rrggbbaa format
     * @param {number} alphaOverride      New alpha value 0-255
     * @returns {string} rgba() color string
     */
    function hexToRgbaWithAlpha(hexColorWithAlpha, alphaOverride) {
        if (!hexColorWithAlpha) return 'rgba(31,143,255,0.25)';
        if (/^#[0-9a-fA-F]{8}$/.test(hexColorWithAlpha)) {
            var red   = parseInt(hexColorWithAlpha.slice(1, 3), 16);
            var green = parseInt(hexColorWithAlpha.slice(3, 5), 16);
            var blue  = parseInt(hexColorWithAlpha.slice(5, 7), 16);
            var alpha = ((alphaOverride != null ? alphaOverride : 64) / 255).toFixed(3);
            return 'rgba(' + red + ',' + green + ',' + blue + ',' + alpha + ')';
        }
        return hexColorWithAlpha;
    }

    // Export to global scope
    window.ColorUtils = {
        intToHex,
        convertSprintColorToRgba,
        hexToRgbaWithAlpha
    };
})();
