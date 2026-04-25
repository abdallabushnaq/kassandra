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

package de.bushnaq.abdalla.kassandra.report.GanttBurndown;

import de.bushnaq.abdalla.kassandra.report.AbstractChart;
import de.bushnaq.abdalla.kassandra.report.burndown.BurnDownRenderer;
import de.bushnaq.abdalla.kassandra.report.burndown.RenderDao;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttRenderer;


public class GanttBurndownChart extends AbstractChart {
    private final BurnDownRenderer bdr;
    private final GanttRenderer    gr;

    public GanttBurndownChart(String relativeCssPath, RenderDao burndownDao, RenderDao ganttDao) throws Exception {
        super("Gantt Burndown Chart", ganttDao.sprint.getName(), relativeCssPath, ganttDao.name, ganttDao.name, null, ganttDao.cssClass, ganttDao.kassandraTheme);
        burndownDao.preRun  = Math.max(burndownDao.preRun, ganttDao.preRun);
        burndownDao.postRun = Math.max(burndownDao.postRun, ganttDao.postRun);
        bdr                 = new BurnDownRenderer(burndownDao);
        gr                  = new GanttRenderer(ganttDao);
        getRenderers().add(bdr);
        getRenderers().add(gr);
        bdr.setDayWidth(gr.getDayWidth());
        this.setChartWidth(Math.max(bdr.chartWidth, gr.chartWidth));
//        this.setChartWidth(getRenderers().getFirst().chartWidth);
        this.setChartHeight(bdr.chartHeight + gr.chartHeight + captionElement.height + footerElement.height - 1);
        captionElement.width = getChartWidth();
        footerElement.y      = captionElement.height + getRenderers().get(0).chartHeight + getRenderers().get(1).chartHeight;
    }

    @Override
    protected void createReport() throws Exception {
        bdr.draw(graphics2D, 0, captionElement.height);
        gr.draw(graphics2D, 0, captionElement.height + bdr.chartHeight);
    }

}
