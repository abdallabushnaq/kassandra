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

package de.bushnaq.abdalla.svg.util;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

@Tag("UnitTest")
public class TestSVGGen {

    private static final int WIDTH  = 16;
    private static final int WIDTH2 = 8;

    @Test
    public void addDayTest(TestInfo testInfo) throws FileNotFoundException, UnsupportedEncodingException, SVGGraphics2DIOException {
        // Get a DOMImplementation.
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();

        // Create an instance of org.w3c.dom.Document.
        String   svgNS    = "http://www.w3.org/2000/svg";
        Document document = domImpl.createDocument(svgNS, "svg", null);

        // Create an instance of the SVG Generator.
        ExtendedSvgGraphics2D svgGraphics = new ExtendedSvgGraphics2D(document);
        // Ask the test to render into the SVG Graphics2D implementation.
        TestSVGGen test = new TestSVGGen();
        test.paint(svgGraphics);
        // Finally, stream out SVG to the standard output using
        // UTF-8 encoding.
        boolean          useCSS = true; // we want to use CSS style attributes
        FileOutputStream o      = new FileOutputStream("target/" + testInfo.getDisplayName() + ".svg");
        Writer           out    = new OutputStreamWriter(o, StandardCharsets.UTF_8);
        svgGraphics.stream(out, useCSS);
    }

    public void paint(Graphics2D g2d) {
        Font font = new Font("Arial", Font.BOLD, 10);
        for (int iy = 0; iy < 10; iy++) {
            for (int ix = 0; ix < 10; ix++) {
                int    x    = ix * WIDTH + WIDTH;
                int    y    = iy * WIDTH + WIDTH;
                String text = String.format("%d%d", ix, iy);
                {
                    {
                        g2d.setStroke(new BasicStroke(0.1f));
                        g2d.setPaint(Color.lightGray);
                        Shape s = new RectangleWithToolTip(x - WIDTH2, y - WIDTH2, WIDTH - 1, WIDTH - 1, text);
                        g2d.fill(s);
                    }

                    {
                        g2d.setFont(font);
                        g2d.setColor(Color.black);
                        FontMetrics fm        = g2d.getFontMetrics();
                        int         maxAscent = fm.getMaxAscent();
                        int         width     = fm.stringWidth(text);
                        g2d.drawString(text, x - width / 2, y + maxAscent / 2);
                    }
                    //                    {
                    //                        //                    g2d.setColor(new Color(0, 0, 0, 0));
                    //                        Shape s = new InvisibleTitledRectangle(x - WIDTH2, y - WIDTH2, WIDTH - 1, WIDTH - 1, text);
                    //                        g2d.fill(s);
                    //                    }
                }
                x = ix * WIDTH + WIDTH + 10 * WIDTH;
                //                {
                //                    {
                //                        g2d.setStroke(new BasicStroke(0.1f));
                //                        g2d.setPaint(Color.lightGray);
                //                        Shape s = new Rectangle(x - WIDTH2, y - WIDTH2, WIDTH - 1, WIDTH - 1);
                //                        g2d.fill(s);
                //                    }
                //
                //                    {
                //                        g2d.setFont(font);
                //                        g2d.setColor(Color.black);
                //                        FontMetrics fm = g2d.getFontMetrics();
                //                        int maxAscent = fm.getMaxAscent();
                //                        int width = fm.stringWidth(text);
                //                        g2d.drawString(text, x - width / 2, y + maxAscent / 2);
                //                    }
                //                    {
                //                        //                    g2d.setColor(new Color(0, 0, 0, 0));
                //                        Shape s = new TitledRectangle(x - WIDTH2, y - WIDTH2, WIDTH - 1, WIDTH - 1, text);
                //                        g2d.fill(s);
                //                    }
                //                }
            }
        }
    }
}
