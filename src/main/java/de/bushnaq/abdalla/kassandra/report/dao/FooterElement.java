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

package de.bushnaq.abdalla.kassandra.report.dao;

import java.awt.*;

public class FooterElement {
    private final Color  backgroundColor = Color.white;
    private final Color  color           = new Color(0x2c, 0x7b, 0xf4);
    public        int    height          = 14;
    private final String imageMap        = "";
    private final String projectRequestKey;
    private final Font   signFont        = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
    //    private Graphics2D graphics2D;
    String text;
    public int width;
    public int x = 3;
    public int y = 1;

    public FooterElement(String text, String projectRequestKey/*, int chartWidth, int chartHeight*/) {
        this.text              = text;
        this.projectRequestKey = projectRequestKey;
//        this.width             = chartWidth;
//        y                      = chartHeight;
        if (text == null) {
            height = 0;
        }
    }

    public void draw(Graphics2D g2) {

        if (text != null) {
            g2.setColor(backgroundColor);
            g2.fillRect(0, y, width, height);
            g2.setColor(color);
            g2.setFont(signFont);
            FontMetrics fm        = g2.getFontMetrics();
            int         maxAscent = fm.getMaxAscent();
            g2.setColor(Color.darkGray);
            g2.drawString(text, x, y + maxAscent - 2);
            int textWidth = g2.getFontMetrics().stringWidth(projectRequestKey);
            g2.drawString(projectRequestKey, width - textWidth - 1, y + maxAscent - 2);
        }
    }

    public String getImageMap() {
        return imageMap;
    }

}