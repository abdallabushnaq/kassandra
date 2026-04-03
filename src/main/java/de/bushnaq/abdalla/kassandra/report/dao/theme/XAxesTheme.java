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

import java.awt.*;

public class XAxesTheme {
    //-------------------------- Day of Month
    public Color   dayOfMonthBgColor;
    public Color   dayOfMonthBorderColor;
    public Color   dayOfMonthTextColor;
    public Color   dayOfMonthWeekendBgColor;
    public Color   dayOfMonthWeekendTextColor;
    //-------------------------- Day of Week
    public Color   dayOfWeekBorderColor;
    public Color   dayOfWeekTextColor;
    public Color   dayOfWeekWeekendTextColor;
    public Color   dayOfweekBgColor;
    public Color   dayOfweekSaturdayBgColor;
    public Color   dayOfweekSundayBgColor;
    //-------------------------------
    public Color   futureEventColor;
    public Color   milestoneFlagColor;
    public Color   milestoneTextColor;
    //-------------------------- Month
    public Color[] monthBgColors = new Color[12];
    public Color   monthBorderColor;
    public Color   monthTextColor;
    //---------------------------------
    public Color   nowEventColor;
    public Color   pastEventColor;
    //-------------------------- Week
    public Color   weekBgColor;
    public Color   weekBoderColor;
    public Color   weekTextColor;
    //-------------------------- Year
    public Color   yearBgColor;
    public Color   yearBoderColor;
    public Color   yearTextColor;

    public XAxesTheme() {
        int ma = 0xff;
        monthBgColors[0]  = new Color(0x18, 0x7d, 0xc3, ma);
        monthBgColors[1]  = new Color(0x24, 0xae, 0xef, ma);
        monthBgColors[2]  = new Color(0x27, 0x9e, 0x68, ma);
        monthBgColors[3]  = new Color(0x62, 0xb7, 0x42, ma);
        monthBgColors[4]  = new Color(0xac, 0xc2, 0x31, ma);
        monthBgColors[5]  = new Color(0xf9, 0xb7, 0x1b, ma);
        monthBgColors[6]  = new Color(0xf1, 0x75, 0x1d, ma);
        monthBgColors[7]  = new Color(0xe5, 0x46, 0x29, ma);
        monthBgColors[8]  = new Color(0xe7, 0x16, 0x57, ma);
        monthBgColors[9]  = new Color(0xad, 0x34, 0x83, ma);
        monthBgColors[10] = new Color(0x65, 0x41, 0x98, ma);
        monthBgColors[11] = new Color(0x08, 0x55, 0xa3, ma);
    }
}