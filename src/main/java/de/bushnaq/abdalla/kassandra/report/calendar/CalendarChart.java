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

package de.bushnaq.abdalla.kassandra.report.calendar;

import de.bushnaq.abdalla.kassandra.Context;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.report.AbstractChart;
import de.bushnaq.abdalla.kassandra.report.dao.BurnDownGraphicsTheme;

import java.time.LocalDateTime;


public class CalendarChart extends AbstractChart {

    public CalendarChart(Context context, LocalDateTime now, User user, String cssClass,
                         BurnDownGraphicsTheme graphicsTheme) throws Exception {
        super(createCaption(user), "", "", "", user.getName() + "." + user.getLocations().getLast().getCountry() + "." + user.getLocations().getLast().getState(), null, cssClass, graphicsTheme);
        getRenderers().add(new CalendarRenderer(context, user, now, cssClass, graphicsTheme));
        this.setChartWidth(getRenderers().get(0).chartWidth);
        this.setChartHeight(getRenderers().get(0).chartHeight + captionElement.height + footerElement.height - 1);
        captionElement.width = 100;
        footerElement.y      = getRenderers().get(0).chartHeight + captionElement.height;
    }

    private static String createCaption(User user) {
        return user.getName() + "." + user.getLocations().getLast().getState() + "." + user.getLocations().getLast().getCountry();
    }

    @Override
    protected void createReport() throws Exception {
        getRenderers().get(0).draw(graphics2D, 0, captionElement.height);
    }

}
