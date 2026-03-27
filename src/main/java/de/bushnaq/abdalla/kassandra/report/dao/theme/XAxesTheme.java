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
    public Color   XAxesDayOfMonthBgColor          = new Color(0xababab);
    public Color   XAxesDayOfMonthBorderColor      = Color.WHITE;
    public Color   XAxesDayOfMonthTextColor        = Color.WHITE;
    public Color   XAxesDayOfMonthWeekendBgColor   = new Color(247, 247, 247);
    public Color   XAxesDayOfMonthWeekendTextColor = Color.BLACK;
    //-------------------------- Day of Week
    public Color   XAxesDayOfWeekBorderColor       = Color.WHITE;
    public Color   XAxesDayOfWeekTextColor         = Color.BLACK;
    public Color   XAxesDayOfweekBgColor           = Color.WHITE;
    public Color   XAxesDayOfweekWeekendBgColor    = new Color(247, 247, 247);
    //-------------------------- Month
    public Color[] XAxesMonthBgColors              = new Color[12];
    public Color   XAxesMonthBorderColor           = Color.WHITE;
    public Color   XAxesMonthTextColor             = Color.WHITE;
    //-------------------------- Week
    public Color   XAxesWeekBgColor                = new Color(0xababab);
    public Color   XAxesWeekBoderColor             = Color.WHITE;
    public Color   XAxesWeekTextColor              = Color.WHITE;
    //-------------------------- Year
    public Color   XAxesYearBackgroundColor        = new Color(0xababab);
    public Color   XAxesYearBoderColor             = Color.white;
    public Color   XAxesYearTextColor              = Color.WHITE;

    public XAxesTheme() {
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