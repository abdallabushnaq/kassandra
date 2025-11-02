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

import de.bushnaq.abdalla.svg.util.ExtendedGraphics2D;

import java.awt.*;

public class CaptionElement {
    private final Color  backgroundColor = Color.white;
    private final Color  color           = new Color(0x2c, 0x7b, 0xf4);
    public        Font   font            = new Font(Font.SANS_SERIF, Font.PLAIN, 18);
    public        int    height          = 26;
    //    private       String imageMap        = "";
    private final String relateCssPath;
    private final String text;
    public        int    width;
    public        int    x               = 3;
    public        int    y               = 0;

    public CaptionElement(String text, String relateCssPath/*, int chartWidth, int chartHeight*/) {
        this.text          = text;
        this.relateCssPath = relateCssPath;
//        this.width         = chartWidth;
        if (text == null) {
            height = 0;
        }
    }

    public void draw(ExtendedGraphics2D graphics2D) {

        if (text != null) {
//            graphics2D.setColor(backgroundColor);
//            graphics2D.fillRect(0, y, width, height);
            graphics2D.setColor(color);
            graphics2D.setFont(font);
            FontMetrics fm           = graphics2D.getFontMetrics();
            int         ascent       = fm.getAscent();
            int         captionWidth = fm.stringWidth(text);
            //            father.graphics2D.fillRect(x, y, width, height);
            String link = relateCssPath + text + ".png";
            graphics2D.drawStringWithLink(text, x, y + height / 2 + (ascent - 2) / 2 - 1, link);
            link = replaceSpaceWithDash(link);
//            imageMap += String.format("<area shape=\"rect\" coords=\"%d,%d,%d,%d\" href=\"%s\" >\n", AbstractCanvas.transformToMapX(x), AbstractCanvas.transformToMapY(y),
//                    AbstractCanvas.transformToMapX(x + captionWidth), AbstractCanvas.transformToMapY(y + height), link);
        }
    }

//    public String getImageMap() {
//        return imageMap;
//    }

    private String replaceSpaceWithDash(String link) {
        return link.replace(' ', '-').toLowerCase();
    }

}