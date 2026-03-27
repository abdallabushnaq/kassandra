/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
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

package de.bushnaq.abdalla.kassandra.report.dao.theme;

import de.bushnaq.abdalla.kassandra.report.dao.ETheme;

import java.awt.*;

public class GraphicsTheme {
    public final XAxesTheme    XAxesTheme             = new XAxesTheme();
    public final CalendarTheme calendarTheme          = new CalendarTheme();
    //-------------------------------------------------------
    public       Color         chartBackgroundColor;
    public       Color         chartBorderColor       = new Color(0xaaaaaa);
    public       ETheme        cssTheme               = ETheme.light;
    //-------------------------- Gantt
    public       Color         ganttGridColor         = new Color(0xe5f2ff, false);
    public       Color         ganttHolidayBgColor    = new Color(0xffe6e6);
    public       Color         ganttSickBgColor       = new Color(0xffe6e6);
    public       Color         ganttTaskTickLineColor = new Color(183, 216, 240);
    public       Color         ganttTaskTickTextColor = new Color(0, 0, 0, 127);
    public       Color         ganttTripBgColor       = new Color(0xffe6e6);
    public       Color         ganttVacationBgColor   = new Color(0xffe6e6);
    //-------------------------------------------------------
//    public              Color   holidayColor                    = new Color(183, 216, 240);  // Light blue;
//    public              Color   sickColor                       = new Color(0xfff2e8); // Light red;
//    public              Color   tripColor                       = new Color(183, 183, 183);//new Color(0xfffcea);  // Light yellow;
//    public              Color   vacationColor                   = new Color(183, 240, 216);  // Light green;

    public GraphicsTheme() {
    }

}
