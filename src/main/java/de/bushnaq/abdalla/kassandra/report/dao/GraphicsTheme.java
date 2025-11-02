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

public class GraphicsTheme {
    public static final Color   COLOR_DARK_GREEN                = new Color(0x095c09);
    public static final Color   COLOR_DARK_RED                  = new Color(0xcc4a31);
    public static final Color   COLOR_GOLD                      = new Color(0xffcc00);
    public static final Color   COLOR_MAGENTA                   = new Color(0xe5127d);
    //-------------------------- Day of Month
    public              Color   XAxesDayOfMonthBgColor          = new Color(0xababab);
    public              Color   XAxesDayOfMonthBorderColor      = Color.WHITE;
    public              Color   XAxesDayOfMonthTextColor        = Color.WHITE;
    public              Color   XAxesDayOfMonthWeekendBgColor   = new Color(247, 247, 247);
    public              Color   XAxesDayOfMonthWeekendTextColor = Color.BLACK;
    //-------------------------- Day of Week
    public              Color   XAxesDayOfWeekBorderColor       = Color.WHITE;
    public              Color   XAxesDayOfWeekTextColor         = Color.BLACK;
    public              Color   XAxesDayOfweekBgColor           = Color.WHITE;
    public              Color   XAxesDayOfweekWeekendBgColor    = new Color(247, 247, 247);
    //-------------------------- Month
    public              Color[] XAxesMonthBgColors              = new Color[12];
    public              Color   XAxesMonthBorderColor           = Color.WHITE;
    public              Color   XAxesMonthTextColor             = Color.WHITE;
    //-------------------------- Week
    public              Color   XAxesWeekBgColor                = new Color(0xababab);
    public              Color   XAxesWeekBoderColor             = Color.WHITE;
    public              Color   XAxesWeekTextColor              = Color.WHITE;
    //-------------------------- Year
    public              Color   XAxesYearBackgroundColor        = new Color(0xababab);
    public              Color   XAxesYearBoderColor             = Color.white;
    public              Color   XAxesYearTextColor              = Color.WHITE;
    //-------------------------- Calendar Chart
    public              Color   calendarFillingDayTextColor     = new Color(0xe2dbdb); // very Light gray for filling days before and after the days we are interested in
    public              Color   calendarHolidayBgColor          = new Color(183, 216, 240);  // Light blue
    public              Color   calendarHolidayTextColor        = new Color(123, 180, 200);  // Light blue
    public              Color   calendarMonthNameColor          = new Color(0xff6336);  // Red for month names
    public              Color   calendarNormalDayTextColor      = new Color(0x323232);  // almost black
    public              Color   calendarSickBgColor             = new Color(0xfff2e8); // Light red
    public              Color   calendarSickTextColor           = new Color(0xff6d5b); // Light red
    public              Color   calendarTodayBgColor            = new Color(0xff3a30);  // Red circle for today
    public              Color   calendarTodayTextColor          = Color.white;  // White text for today
    public              Color   calendarTripBgColor             = new Color(183, 183, 183);// new Color(0xfffcea);  // Light yellow
    public              Color   calendarTripTextColor           = new Color(64, 64, 64);//new Color(0xff931e);  // Light yellow
    public              Color   calendarVacationBgColor         = new Color(183, 240, 216);  // Light green
    public              Color   calendarVacationTextColor       = new Color(123, 200, 180);  // Light green
    public              Color   calendarWeekDayTextColor        = new Color(180, 180, 180);  // Light gray for weekends
    public              Color   calendarWeekendBgColor          = Color.WHITE;
    public              Color   calendarWeekendTextColor        = new Color(180, 180, 180);  // Light gray for weekends
    public              Color   calendarYearTextColor           = new Color(80, 80, 80);  // Dark gray for year display
    //-------------------------------------------------------
    public              Color   chartBackgroundColor;
    public              Color   chartBorderColor                = new Color(0xaaaaaa);
    public              ETheme  cssTheme                        = ETheme.light;
    //-------------------------- Gantt
    public              Color   ganttGridColor                  = new Color(0xe5f2ff, false);
    public              Color   ganttHolidayBgColor             = new Color(0xffe6e6);
    public              Color   ganttSickBgColor                = new Color(0xffe6e6);
    public              Color   ganttTaskTickLineColor          = new Color(183, 216, 240);
    public              Color   ganttTaskTickTextColor          = new Color(0, 0, 0, 127);
    public              Color   ganttTripBgColor                = new Color(0xffe6e6);
    public              Color   ganttVacationBgColor            = new Color(0xffe6e6);
    //-------------------------------------------------------
//    public              Color   holidayColor                    = new Color(183, 216, 240);  // Light blue;
//    public              Color   sickColor                       = new Color(0xfff2e8); // Light red;
//    public              Color   tripColor                       = new Color(183, 183, 183);//new Color(0xfffcea);  // Light yellow;
//    public              Color   vacationColor                   = new Color(183, 240, 216);  // Light green;

    public GraphicsTheme() {
        int ma = 0xff;
        XAxesMonthBgColors[0]  = new Color(0x18, 0x7d, 0xc3, ma);
        XAxesMonthBgColors[1]  = new Color(0x24, 0xae, 0xef, ma);
        XAxesMonthBgColors[2]  = new Color(0x27, 0x9e, 0x68, ma);
        XAxesMonthBgColors[3]  = new Color(0x62, 0xb7, 0x42, ma);
        XAxesMonthBgColors[4]  = new Color(0xac, 0xc2, 0x31, ma);
        XAxesMonthBgColors[5]  = new Color(0xf9, 0xb7, 0x1b, ma);
        XAxesMonthBgColors[6]  = new Color(0xf1, 0x75, 0x1d, ma);
        XAxesMonthBgColors[7]  = new Color(0xe5, 0x46, 0x29, ma);
        XAxesMonthBgColors[8]  = new Color(0xe7, 0x16, 0x57, ma);
        XAxesMonthBgColors[9]  = new Color(0xad, 0x34, 0x83, ma);
        XAxesMonthBgColors[10] = new Color(0x65, 0x41, 0x98, ma);
        XAxesMonthBgColors[11] = new Color(0x08, 0x55, 0xa3, ma);
    }
}
