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

package de.bushnaq.abdalla.kassandra.report;

import de.bushnaq.abdalla.kassandra.report.dao.CaptionElement;
import de.bushnaq.abdalla.kassandra.report.dao.FooterElement;
import de.bushnaq.abdalla.kassandra.report.dao.GraphicsTheme;
import de.bushnaq.abdalla.svg.util.ExtendedGraphics2D;
import de.bushnaq.abdalla.util.Util;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractChart extends AbstractCanvas {
    public        CaptionElement         captionElement;
    public        FooterElement          footerElement;
    //    private final String                 mapName;
    private final List<AbstractRenderer> renderers = new ArrayList<>();

    public AbstractChart(String caption, String projectRequestKey, String relateCssPath, String column, String imageName/*, String mapName*/, String link, String cssClass, GraphicsTheme graphicsTheme) throws IOException {
        super(column, imageName/*, mapName*/, link, cssClass, graphicsTheme);
        captionElement = new CaptionElement(caption, relateCssPath);
        footerElement  = new FooterElement(Util.generateCopyrightString(LocalDateTime.now()), projectRequestKey);
//        this.mapName   = mapName;
    }

    @Override
    protected void drawBackground() {
        graphics2D.setColor(graphicsTheme.chartBackgroundColor);
        graphics2D.fillRect(0, captionElement.height, getChartWidth() - 1, getChartHeight() - captionElement.height - 1);
    }

    @Override
    protected void drawCaption(ExtendedGraphics2D graphics2d2) {
        captionElement.draw(graphics2d2);
    }

    @Override
    protected void drawFooter(ExtendedGraphics2D graphics2d2) {
        footerElement.draw(graphics2d2);
    }

    public List<AbstractRenderer> getRenderers() {
        return renderers;
    }

    @Override
    public void setChartWidth(int chartWidth) {
        super.setChartWidth(chartWidth);
        if (captionElement != null) {
            captionElement.width = chartWidth;
        }
        if (footerElement != null) {
            footerElement.width = chartWidth;
        }
    }

}
