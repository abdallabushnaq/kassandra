// color-utils.js
// Shared color utility functions for theme and color conversions.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0
(function () {
    'use strict';

    /**
     * Converts a value to a hexadecimal color string.
     * @param {*} value The value to convert (null, string, or number)
     * @returns {?string} The hex color string (e.g., '#ababab') or null if conversion fails
     */
    function convertNumberToHexColor(value) {
        if (value == null) return null;
        if (typeof value === 'string') return value;
        if (typeof value === 'number') return '#' + (value >>> 0).toString(16).padStart(6, '0').slice(-6);
        return null;
    }

    /**
     * Retrieves a color from the theme object with a fallback value.
     * @param {Object} theme The theme configuration object
     * @param {string} key The theme property key to retrieve
     * @param {string} fallbackColor The fallback color to use if key is not found
     * @returns {string} The theme color or fallback color as a hex string
     */
    function getThemeColor(theme, key) {
        const fallbackColor = '#ffffff';
        return (theme && theme[key] != null) ? (convertNumberToHexColor(theme[key]) || fallbackColor) : fallbackColor;
    }

    /**
     * Converts a Java-encoded sprint color (#rrggbbaa) to an SVG rgba() string.
     * Falls back to default blue if color is missing or invalid.
     * @param {string} hexColorWithAlpha Sprint color in #rrggbbaa format
     * @returns {string} SVG rgba() color string or fallback color
     */
    function convertSprintColorToRgba(hexColorWithAlpha) {
        if (!hexColorWithAlpha) return 'rgba(31,143,255,0.31)';
        if (/^#[0-9a-fA-F]{8}$/.test(hexColorWithAlpha)) {
            const red = parseInt(hexColorWithAlpha.slice(1, 3), 16);
            const green = parseInt(hexColorWithAlpha.slice(3, 5), 16);
            const blue = parseInt(hexColorWithAlpha.slice(5, 7), 16);
            const alpha = (parseInt(hexColorWithAlpha.slice(7, 9), 16) / 255).toFixed(3);
            return 'rgba(' + red + ',' + green + ',' + blue + ',' + alpha + ')';
        }
        return hexColorWithAlpha;
    }

    // Export to global scope
    window.ColorUtils = {
        convertNumberToHexColor,
        getThemeColor,
        convertSprintColorToRgba
    };
})();

