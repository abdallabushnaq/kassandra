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

public class GraphicsLightTheme extends BurnDownGraphicsTheme {
    public static final Color[] KELLY_COLORS = {
            //                        Color.decode("0xFFB300"),    // Vivid Yellow
            //                        Color.decode("0x803E75"),    // Strong Purple
            //                        Color.decode("0xA6BDD7"),    // Very Light Blue
            //                        Color.decode("0xC10020"),    // Vivid Red
            //                        Color.decode("0x007D34"),    // Vivid Green
            //                        Color.decode("0x00538A"),    // Strong Blue
            //
            //                        Color.decode("0xCEA262"),    // Grayish Yellow
            //                        Color.decode("0x817066"),    // Medium Gray
            //
            //                        Color.decode("0xF6768E"),    // Strong Purplish Pink
            //                        Color.decode("0xFF7A5C"),    // Strong Yellowish Pink
            //                        Color.decode("0x53377A"),    // Strong Violet
            //                        Color.decode("0xFF8E00"),    // Vivid Orange Yellow
            //                        Color.decode("0xB32851"),    // Strong Purplish Red
            //                        Color.decode("0xF4C800"),    // Vivid Greenish Yellow
            //                        Color.decode("0x7F180D"),    // Strong Reddish Brown
            //                        Color.decode("0x93AA00"),    // Vivid Yellowish Green
            //                        Color.decode("0x593315"),    // Deep Yellowish Brown
            //                        Color.decode("0xF13A13"),    // Vivid Reddish Orange
            //                        Color.decode("0x232C16"),    // Dark Olive Green
            //                        Color.decode("0xFF6800"),    // Vivid Orange

            Color.red,//
            new Color(0, 0xff, 0),//light green
            Color.blue,//
            Color.yellow,//
            Color.cyan,//
            Color.magenta,//
            new Color(0xff, 0x7f, 0),//dark red
            new Color(0x7f, 0xff, 0),//dark green
            new Color(0, 0x7f, 0xff),//dark blue
            new Color(0, 0xa0, 0x7f),//dark cyan
            new Color(0x7f, 0, 0x7f),//dark magenta
            Color.darkGray

    };

    public GraphicsLightTheme() {

        // gray
        Color basicTextColor       = Color.WHITE;
        Color basicBackgroundColor = new Color(0x7a7a7a);
        Color basicBorderColor     = Color.WHITE;

        cssTheme             = ETheme.light;
        chartBackgroundColor = Color.white;

        dayOfMonthTextColor       = basicTextColor;
        dayOfMonthBackgroundColor = basicBackgroundColor;

        dayTextColor          = Color.BLACK;
        dayBackgroundColor    = Color.WHITE;
        dayDiagramBorderColor = new Color(0xf6f8ff);

        delayClosedEventColor = new Color(255, 184, 184);
        delayEventColor       = Color.red;
        futureEventColor      = Color.blue;

        milestoneFlagColor = chartBackgroundColor;
        milestoneTextColor = basicTextColor;


        nowEventColor        = Color.gray;
        pastEventColor       = Color.lightGray;
        ticksBackgroundColor = chartBackgroundColor;

        weekBackgroundColor = basicBackgroundColor;
        weekTextColor       = basicTextColor;

        ganttRelationColor           = new Color(0x34, 0x66, 0xed, 0x7f);
        ganttCriticalRelationColor   = new Color(0xff, 0, 0, 0x7f);
        ganttMilestoneColor          = new Color(0x4f, 0xbb, 0xc2, 0xff);
        ganttMilestoneTextColor      = new Color(0x50, 0x50, 0x50, 0xff);
        ganttStoryColor              = Color.black;//new Color(64, 64, 64, 0xa0);
        ganttStoryTextColor          = Color.darkGray;
        ganttTaskTextColor           = new Color(0x30, 0x30, 0x30, 0xff);
        ganttTaskBorderColor         = new Color(0x30, 0x30, 0x30, 0x7F);
        burnDownBorderColor          = new Color(0xff, 0xcc, 0x00, 0x77);
        ganttIdColor                 = new Color(0xff, 0xff, 0xff, 0xff);
        ganttIdErrorColor            = new Color(0xff, 0x0, 0x0, 0xff);
        ganttIdTextColor             = new Color(0xaa, 0xaa, 0xaa, 0xff);
        ganttIdTextErrorColor        = new Color(0xff, 0xff, 0xff, 0xff);
        ganttCriticalTaskBorderColor = new Color(0xff, 0x0, 0x0, 0xC0);
        ganttOutOfOfficeColor        = new Color(0xff, 0x33, 0x33, 0x33);

        int ps;//primary color
        int pg;//primary color green
        int ss;//secondary color
        int sg;//secondary color green
        int ts;//trinary color
        int tg;//trinary color green

        int   i     = 0;
        int[] alpha = {0x7f/*, 0x30*/};

        for (i = 0; i < KELLY_COLORS.length; i++) {
            burnDownColor[i] = new Color(KELLY_COLORS[i].getRed(), KELLY_COLORS[i].getGreen(), KELLY_COLORS[i].getBlue(), alpha[0]);
        }

        //gray
        //        burnDownColor[i++] = new Color(0x0, 0x0, 0x0, 0xa1);
        //        burnDownColor[i++] = new Color(0x0, 0x0, 0x0, 0x51);
        //        burnDownColor[i++] = new Color(0x0, 0x0, 0x0, 0x21);

        graphTextBackgroundColor = chartBackgroundColor;
        tickTextColor            = Color.darkGray;
        surroundingSquareColor   = new Color(0xaaaaaa);
        optimaleGuideColor       = new Color(0xa0a0a0);
        plannedGuideColor        = new Color(0x7f7f7f);
        ticksColor               = new Color(0xc9c9c9);
        inTimeColor              = COLOR_DARK_GREEN;
        watermarkColor           = new Color(0x10, 0x10, 0x10, 0x10);

        pastWorkDayRequestColor    = new Color(0x60, 0x00, 0xff, 0x40);
        pastWeekendRequestColor    = new Color(0x60, 0x60, 0x60, 0x40);
        futureWorkDayRequestColor  = new Color(0x00, 0x60, 0xff, 0x40);
        futureWeekendRequestColor  = new Color(0x60, 0x60, 0x60, 0x40);
        ganttRequestMilestoneColor = Color.RED/*new Color(0xa7, 0x00, 0x00)*/;

        linkColor = new Color(0x23, 0x6a, 0x97);

        bankHolidayColor = new Color(0xff, 0, 0, 0x80);
    }

    private int generateColors(int ps, int pg, int ss, int sg, int ts, int tg, int i, int a) {
        burnDownColor[i++] = new Color(ps, sg, ts, a);
        burnDownColor[i++] = new Color(ts, pg, ss, a);
        burnDownColor[i++] = new Color(ss, tg, ps, a);
        return i;
    }
}
