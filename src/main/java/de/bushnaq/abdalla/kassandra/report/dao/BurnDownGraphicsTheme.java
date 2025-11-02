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

public class BurnDownGraphicsTheme extends ScheduleGraphicsTheme {
    public static final int     MAX_AUTHOR_COLOR = 12/*(9-4) * 3*//*+ 3*//*+1*/;
    public              Color   bankHolidayColor;
    public              Color   burnDownBorderColor;
    protected           Color[] burnDownColor    = new Color[MAX_AUTHOR_COLOR];
    public              Color   ganttRequestMilestoneColor;
    public              Color   graphTextBackgroundColor;
    public              Color   optimaleGuideColor;
    public              Color   plannedGuideColor;
    public              Color   surroundingSquareColor;
    public              Color   tickTextColor;
    public              Color   ticksColor;
    public              Color   watermarkColor;

    public Color getAuthorColor(int index) {
        if (index < 0) {
            index = -index;
        }
        int colorIndex = index % MAX_AUTHOR_COLOR;
        return burnDownColor[colorIndex];
    }

//    public Color getHeatColor(int index) {
//        if (index < 0) {
//            index = -index;
//        }
//        int colorIndex = index % MAX_HEAT_COLOR;
//        return heatColor[colorIndex];
//    }

}
