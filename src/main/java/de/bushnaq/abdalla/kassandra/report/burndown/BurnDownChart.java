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

package de.bushnaq.abdalla.kassandra.report.burndown;


import de.bushnaq.abdalla.kassandra.report.AbstractChart;

public class BurnDownChart extends AbstractChart {

    public BurnDownChart(String relativeCssPath, RenderDao dao) throws Exception {
        super("Work Burn Down Chart", dao.sprint.getName(), relativeCssPath, dao.column, dao.sprintName, dao.link, dao.cssClass, dao.graphicsTheme);
        getRenderers().add(new BurnDownRenderer(dao));
        this.setChartWidth(getRenderers().getFirst().chartWidth);
        this.setChartHeight(getRenderers().getFirst().chartHeight + captionElement.height + footerElement.height - 1);
        captionElement.width = dao.chartWidth;
        footerElement.y      = getRenderers().getFirst().chartHeight + captionElement.height;
    }

    @Override
    protected void createReport() throws Exception {
        getRenderers().getFirst().draw(graphics2D, 0, captionElement.height);
    }

}
