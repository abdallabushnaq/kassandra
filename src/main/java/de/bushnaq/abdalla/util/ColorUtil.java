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

    public static Color calculateComplementaryColor(Color color) {
        Color complementaryColor = new Color(255 - color.getRed(), 255 - color.getGreen(), 255 - color.getBlue());
        return complementaryColor;
    }

    public static int calculateContrast(final Color aC1, final Color aC2) {
        final int red   = Math.abs(aC1.getRed() - aC2.getRed());
        final int green = Math.abs(aC1.getGreen() - aC2.getGreen());
        final int blue  = Math.abs(aC1.getBlue() - aC2.getBlue());

        //0.299*R + 0.587*G + 0.114*B
        final int light = ((red * 299) / 1000) + ((green * 587) / 1000) + ((blue * 114) / 1000);
        return light;
    }

    public static Color colorFraction(final Color aColor, final double aFraction) {
        final int _red   = Math.max(Math.min((int) (aColor.getRed() * aFraction), 255), 0);
        final int _green = Math.max(Math.min((int) (aColor.getGreen() * aFraction), 255), 0);
        final int _blue  = Math.max(Math.min((int) (aColor.getBlue() * aFraction), 255), 0);
        return new Color(_red, _green, _blue);
    }

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

    public static String colorToJsfColor(final Color aColor) {
        final int _red   = aColor.getRed();
        final int _green = aColor.getGreen();
        final int _blue  = aColor.getBlue();
        return "rgb( " + _red + ", " + _green + ", " + _blue + ")";
    }

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
     * Blue 255 will return a gray value of 28 Red 255 will return a gray value of 76 Green 255 will return a gray value
     * of 150 The commulative gray value is the sum of the component values
     *
     * @param aColor given color
     * @return Returns either white or black, whichever will have the highest contrast to the given color
     */
    public static Color heighestContrast(final Color aColor, Color background) {
        return heighestContrast(aColor, background, aColor.getAlpha());
    }

    public static Color heighestContrast(final Color aColor, Color background, int alpha) {
        final int red   = (aColor.getRed() * alpha) / 255 + (background.getRed() * (255 - alpha)) / 255;
        final int green = (aColor.getGreen() * alpha) / 255 + (background.getGreen() * (255 - alpha)) / 255;
        final int blue  = (aColor.getBlue() * alpha) / 255 + (background.getBlue() * (255 - alpha)) / 255;
        Color     c     = new Color(red, green, blue);
        Color     color = ColorUtil.selectMostContrastColor(c, new Color[]{Color.black, Color.white});
        return color;
    }

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

    public static Color setAlpha(Color c1, int alpha) {
        return new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), alpha);
    }

    public static Color windowsToJava(final Color aColor) {
        final int _red   = aColor.getRed();
        final int _green = aColor.getGreen();
        final int _blue  = aColor.getBlue();
        return new Color(_blue, _green, _red);
    }

}
