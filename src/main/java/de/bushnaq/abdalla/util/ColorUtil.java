/*
 *
 * Copyright (C) 2025-2025 Abdalla Bushnaq
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

package de.bushnaq.abdalla.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

/**
 * Utility class providing color manipulation, conversion, and contrast-calculation helpers.
 *
 * <p>All methods are static; this class is not intended to be instantiated.</p>
 */
public class ColorUtil {
    private static final Logger logger = LoggerFactory.getLogger(ColorUtil.class);

    /**
     * Calculate the color blending on a background color taking the color's alpha channel into account
     * So,
     * a color with an alpha channel not 255 drawn over white background will appear lighter and
     * a color with an alpha channel not 255 drawn over black background will appear darker.
     * This method calculates the new blended color
     *
     * @param aColor,     the color we want to blend over the background using the alpha channel
     * @param background, the background that the color is going to be drawn on
     * @return the new blended color
     */
    public static Color calculateColorBlending(final Color aColor, Color background) {
        //        logger.info(String.format("r=%d, g=%d, b=%s, a=%d %08X", aColor.getRed(), aColor.getGreen(), aColor.getBlue(), aColor.getAlpha(), aColor.getRGB()));
        final int alpha = aColor.getAlpha();
        final int red   = (aColor.getRed() * alpha) / 255 + (background.getRed() * (255 - alpha)) / 255;
        final int green = (aColor.getGreen() * alpha) / 255 + (background.getGreen() * (255 - alpha)) / 255;
        final int blue  = (aColor.getBlue() * alpha) / 255 + (background.getBlue() * (255 - alpha)) / 255;
        Color     c     = new Color(red, green, blue);
        //        logger.info(String.format("r=%d, g=%d, b=%s, a=%d %08X", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha(), c.getRGB()));
        return c;
    }

    /**
     * Calculates the complementary color by inverting each RGB component.
     * The alpha channel is ignored and the returned color is fully opaque.
     *
     * @param color the source color
     * @return a new {@link Color} whose red, green and blue components are {@code 255 - component}
     */
    public static Color calculateComplementaryColor(Color color) {
        Color complementaryColor = new Color(255 - color.getRed(), 255 - color.getGreen(), 255 - color.getBlue());
        return complementaryColor;
    }

    /**
     * Calculates a perceived-luminance-weighted contrast value between two colors.
     * Uses the formula {@code 0.299·R + 0.587·G + 0.114·B} applied to the absolute per-channel
     * difference, giving a value in the range {@code [0, 255]}.
     *
     * @param aC1 the first color
     * @param aC2 the second color
     * @return the contrast value; higher means more perceptually distinct
     */
    public static int calculateContrast(final Color aC1, final Color aC2) {
        final int red   = Math.abs(aC1.getRed() - aC2.getRed());
        final int green = Math.abs(aC1.getGreen() - aC2.getGreen());
        final int blue  = Math.abs(aC1.getBlue() - aC2.getBlue());

        //0.299*R + 0.587*G + 0.114*B
        final int light = ((red * 299) / 1000) + ((green * 587) / 1000) + ((blue * 114) / 1000);
        return light;
    }

    /**
     * Scales each RGB component of the given color by {@code aFraction}, clamping the result to
     * the valid range {@code [0, 255]}. The alpha channel is not preserved; the returned color is
     * fully opaque.
     *
     * @param aColor    the source color
     * @param aFraction the scale factor; values &lt; 1.0 darken, values &gt; 1.0 brighten
     * @return a new {@link Color} with scaled RGB components
     */
    public static Color colorFraction(final Color aColor, final double aFraction) {
        final int _red   = Math.max(Math.min((int) (aColor.getRed() * aFraction), 255), 0);
        final int _green = Math.max(Math.min((int) (aColor.getGreen() * aFraction), 255), 0);
        final int _blue  = Math.max(Math.min((int) (aColor.getBlue() * aFraction), 255), 0);
        return new Color(_red, _green, _blue);
    }

    /**
     * Linearly interpolates (blends) two colors in RGB space.
     * The result is {@code aColor2 * aFraction + aColor1 * (1 - aFraction)}.
     * A fraction of {@code 0.0} returns {@code aColor1}; {@code 1.0} returns {@code aColor2}.
     *
     * @param aColor1   the start color
     * @param aColor2   the end color
     * @param aFraction the interpolation weight in the range {@code [0.0, 1.0]}
     * @return a new {@link Color} representing the blended result
     */
    public static Color colorMerger(final Color aColor1, final Color aColor2, final float aFraction) {
        final float[] _color1 = aColor1.getColorComponents(null);
        final float[] _color2 = aColor2.getColorComponents(null);
        for (int i = 0; i < 3; i++) {
            _color1[i] = _color2[i] * aFraction + _color1[i] * (1 - aFraction);
        }
        return new Color(_color1[0], _color1[1], _color1[2]);
    }

    /**
     * Convert java.awt.Color to hex string with # prefix.
     * Returns a default light gray (#D3D3D3) if color is null.
     *
     * @param color the color to convert
     * @return hex color string with # prefix (e.g., "#FF0000")
     */
    public static String colorToHexString(final Color color) {
        if (color == null) {
            return "#D3D3D3"; // Light Gray default
        }
        return "#" + colorToHtmlColor(color).toUpperCase();
    }

    /**
     * Converts a {@link Color} to a 6-character lowercase hex string <em>without</em> a leading
     * {@code #} character (e.g., {@code "ff0000"} for red).
     * Use {@link #colorToHexString(Color)} when the {@code #} prefix is required.
     *
     * @param aColor the color to convert
     * @return a 6-character lowercase hex string representing the RGB components
     */
    public static String colorToHtmlColor(final Color aColor) {
        int       _rgb   = aColor.getRGB();
        final int _red   = aColor.getRed();
        final int _green = aColor.getGreen();
        final int _blue  = aColor.getBlue();
        _rgb = (_red << 16) + (_green << 8) + (_blue);
        final String _buffer = Integer.toHexString(_rgb);
        if (_buffer.length() < 6) {
            String _resturnValue = "0";
            for (int i = 0; i < 6 - _buffer.length() - 1; i++) {
                _resturnValue += "0";
            }
            return _resturnValue + _buffer;
        } else {
            return _buffer;
        }
    }

    /**
     * Converts a {@link Color} to a CSS {@code rgb()} string suitable for use in JSF / CSS contexts.
     * For example, {@code Color.RED} produces {@code "rgb( 255, 0, 0)"}.
     * The alpha channel is ignored.
     *
     * @param aColor the color to convert
     * @return a CSS {@code rgb(r, g, b)} string
     */
    public static String colorToJsfColor(final Color aColor) {
        final int _red   = aColor.getRed();
        final int _green = aColor.getGreen();
        final int _blue  = aColor.getBlue();
        return "rgb( " + _red + ", " + _green + ", " + _blue + ")";
    }

    /**
     * Computes the perceptual difference between two pixels represented as {@code int[]} arrays
     * in the format {@code [R, G, B, A]}.
     *
     * <p>The comparison is performed in HSB (Hue, Saturation, Brightness) space by summing the
     * absolute differences of all three HSB components. Pixels where the <em>pattern</em> alpha
     * ({@code aPatternPixelColor[3]}) is {@code 0} (fully transparent) contribute zero difference.</p>
     *
     * @param aPatternPixelColor the reference pixel as {@code [R, G, B, A]}
     * @param aImagePixelColor   the pixel under test as {@code [R, G, B, A]}
     * @return the cumulative HSB difference; {@code 0.0f} means identical colors
     */
    public static float difference(final int[] aPatternPixelColor, final int[] aImagePixelColor) {
        float[] _patternHsb = null;
        {
            // int _red = aPatternPixelColor.getRed();
            // int _green = aPatternPixelColor.getGreen();
            // int _blue = aPatternPixelColor.getBlue();
            _patternHsb = Color.RGBtoHSB(aPatternPixelColor[0], aPatternPixelColor[1], aPatternPixelColor[2], null);
        }
        float[] _imageHsb = null;
        {
            // int _red = aImagePixelColor.getRed();
            // int _green = aImagePixelColor.getGreen();
            // int _blue = aImagePixelColor.getBlue();
            _imageHsb = Color.RGBtoHSB(aImagePixelColor[0], aImagePixelColor[1], aImagePixelColor[2], null);
        }
        float _difference = 0;
        if (aPatternPixelColor[3] != 0) {
            // ---In case not transparent
            _difference += Math.abs(_patternHsb[0] - _imageHsb[0]);
            _difference += Math.abs(_patternHsb[1] - _imageHsb[1]);
            _difference += Math.abs(_patternHsb[2] - _imageHsb[2]);
        } else {
        }
        return _difference;
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
    public static Color heighestContrast(final Color aColor) {
        final int red   = aColor.getRed();
        final int green = aColor.getGreen();
        final int blue  = aColor.getBlue();

        //0.299*R + 0.587*G + 0.114*B
        final int light = ((red * 299) / 1000) + ((green * 587) / 1000) + ((blue * 114) / 1000);
        if (light < 127) {
            return Color.white;
        } else {
            return Color.black;
        }
    }

    /**
     * Returns either {@link Color#white} or {@link Color#black}, whichever provides the higher
     * perceptual contrast against the given color when it is rendered over the specified background.
     *
     * <p>The color's own alpha channel is used to alpha-blend it over the background, and the
     * most contrasting candidate is chosen from the resulting blended color via
     * {@link #selectMostContrastColor(Color, Color[])}.</p>
     *
     * @param aColor     the foreground color (its alpha value controls the blend ratio)
     * @param background the background color that {@code aColor} is drawn on
     * @return {@link Color#white} or {@link Color#black}, whichever contrasts more with the blended result
     */
    public static Color heighestContrast(final Color aColor, Color background) {
        return heighestContrast(aColor, background, aColor.getAlpha());
    }

    /**
     * Returns either {@link Color#white} or {@link Color#black}, whichever provides the higher
     * perceptual contrast against the given color when it is rendered over the specified background
     * using the supplied alpha value (overriding the color's own alpha channel).
     *
     * <p>The supplied {@code alpha} value is used to alpha-blend {@code aColor} over
     * {@code background}, and the most contrasting candidate is chosen from the resulting blended
     * color via {@link #selectMostContrastColor(Color, Color[])}.</p>
     *
     * @param aColor     the foreground color
     * @param background the background color that {@code aColor} is drawn on
     * @param alpha      the explicit alpha value in the range {@code [0, 255]} to use for blending;
     *                   {@code 0} means fully transparent (background only), {@code 255} means fully opaque
     * @return {@link Color#white} or {@link Color#black}, whichever contrasts more with the blended result
     */
    public static Color heighestContrast(final Color aColor, Color background, int alpha) {
        final int red   = (aColor.getRed() * alpha) / 255 + (background.getRed() * (255 - alpha)) / 255;
        final int green = (aColor.getGreen() * alpha) / 255 + (background.getGreen() * (255 - alpha)) / 255;
        final int blue  = (aColor.getBlue() * alpha) / 255 + (background.getBlue() * (255 - alpha)) / 255;
        Color     c     = new Color(red, green, blue);
        Color     color = ColorUtil.selectMostContrastColor(c, new Color[]{Color.black, Color.white});
        return color;
    }

    /**
     * Converts a hex color string to a java.awt.Color.
     * Accepts formats: "#RRGGBB", "#AARRGGBB", "RRGGBB", or "AARRGGBB" (with or without leading '#').
     * If alpha is not specified, it defaults to 255 (opaque).
     *
     * @param hexString the hex color string (e.g., "#FF0000", "FF0000", "80FF0000")
     * @return the corresponding Color object
     * @throws IllegalArgumentException if the string is not a valid hex color
     */
    public static Color hexStringToColor(final String hexString) {
        if (hexString == null) {
            throw new IllegalArgumentException("hexString cannot be null");
        }
        String s = hexString.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.length() == 6) {
            // RRGGBB
            int rgb = Integer.parseInt(s, 16);
            return new Color(rgb);
        } else if (s.length() == 8) {
            // AARRGGBB
            int argb = (int) Long.parseLong(s, 16); // use long to avoid sign issues
            int alpha = (argb >> 24) & 0xFF;
            int red   = (argb >> 16) & 0xFF;
            int green = (argb >> 8) & 0xFF;
            int blue  = argb & 0xFF;
            return new Color(red, green, blue, alpha);
        } else {
            throw new IllegalArgumentException("Invalid hex color string: '" + hexString + "'");
        }
    }

    /**
     * Selects the color from the given array that provides the highest contrast against the
     * background color. Contrast is measured as the squared Euclidean distance in RGB space,
     * which emphasises large per-channel differences.
     *
     * @param backgroundColor the reference background color
     * @param colors          an array of candidate colors to evaluate
     * @return the candidate color whose squared RGB distance from {@code backgroundColor} is largest;
     *         {@code null} if {@code colors} is empty
     */
    public static Color selectMostContrastColor(Color backgroundColor, Color[] colors) {
        int   maxContrast   = 0;
        Color contrastColor = null;
        for (Color c : colors) {
            int contrast = Math.abs(backgroundColor.getRed() - c.getRed()) * Math.abs(backgroundColor.getRed() - c.getRed())
                    + Math.abs(backgroundColor.getGreen() - c.getGreen()) * Math.abs(backgroundColor.getGreen() - c.getGreen())
                    + Math.abs(backgroundColor.getBlue() - c.getBlue()) * Math.abs(backgroundColor.getBlue() - c.getBlue());
            if (contrast > maxContrast) {
                contrastColor = c;
                maxContrast   = contrast;
            }
        }
        return contrastColor;
    }

    /**
     * Returns a new {@link Color} identical to the given color but with the specified alpha value.
     *
     * @param c1    the source color (RGB components are preserved)
     * @param alpha the new alpha value in the range {@code [0, 255]};
     *              {@code 0} is fully transparent, {@code 255} is fully opaque
     * @return a new {@link Color} with the same RGB components and the new alpha
     */
    public static Color setAlpha(Color c1, int alpha) {
        return new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), alpha);
    }

    /**
     * Converts a Windows COLORREF color (stored as BGR) to a Java {@link Color} (stored as RGB)
     * by swapping the red and blue channels. The green channel is unchanged.
     *
     * @param aColor the Windows-order color
     * @return a new {@link Color} with the red and blue channels swapped
     */
    public static Color windowsToJava(final Color aColor) {
        final int _red   = aColor.getRed();
        final int _green = aColor.getGreen();
        final int _blue  = aColor.getBlue();
        return new Color(_blue, _green, _red);
    }

}
