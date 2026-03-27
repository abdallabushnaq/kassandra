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

import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionConfig;
import de.bushnaq.abdalla.kassandra.report.dao.ETheme;
import de.bushnaq.abdalla.util.ColorUtil;
import org.springframework.stereotype.Component;

import java.awt.*;

@Component
public class DarkTheme extends KassandraTheme {
    public static final Color[] KELLY_COLORS = {

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

    public DarkTheme(StableDiffusionConfig stableDiffusionConfig) {

        // gray
        Color basicTextColor = Color.WHITE;
//        Color basicBackgroundColor = new Color(0x7a7a7a);
//        Color basicBorderColor     = Color.WHITE;

        cssTheme             = ETheme.light;
        chartBackgroundColor = ColorUtil.hexStringToColor(stableDiffusionConfig.getAvatarDarkBackgroundColor());

//        dayOfMonthTextColor       = basicTextColor;
//        dayOfMonthBackgroundColor = basicBackgroundColor;

//        dayTextColor          = Color.BLACK;
//        dayBackgroundColor    = Color.WHITE;
//        dayDiagramBorderColor = new Color(0xf6f8ff);

//        delayClosedEventColor = new Color(255, 184, 184);
        burndownTheme.delayEventColor = Color.red;
        xAxesTheme.futureEventColor   = Color.blue;

        xAxesTheme.milestoneFlagColor = chartBackgroundColor;
        xAxesTheme.milestoneTextColor = basicTextColor;


        xAxesTheme.nowEventColor  = Color.gray;
        xAxesTheme.pastEventColor = Color.lightGray;
//        ticksBackgroundColor = chartBackgroundColor;

//        weekBackgroundColor = basicBackgroundColor;
//        weekTextColor       = basicTextColor;

        ganttTheme.ganttRelationColor           = new Color(0x34, 0x66, 0xed, 0x7f);
        ganttTheme.ganttCriticalRelationColor   = new Color(0xff, 0, 0, 0x7f);
        ganttTheme.ganttMilestoneColor          = new Color(0x4f, 0xbb, 0xc2, 0xff);
        ganttTheme.ganttMilestoneTextColor      = new Color(0x50, 0x50, 0x50, 0xff);
        ganttTheme.ganttStoryColor              = Color.black;//new Color(64, 64, 64, 0xa0);
        ganttTheme.ganttStoryTextColor          = Color.darkGray;
        ganttTheme.ganttTaskTextColor           = new Color(0x30, 0x30, 0x30, 0xff);
        ganttTheme.ganttTaskBorderColor         = new Color(0x30, 0x30, 0x30, 0x7F);
        burndownTheme.burnDownBorderColor       = new Color(0xff, 0xcc, 0x00, 0x77);
        ganttTheme.ganttIdColor                 = new Color(0xff, 0xff, 0xff, 0xff);
        ganttTheme.ganttIdErrorColor            = new Color(0xff, 0x0, 0x0, 0xff);
        ganttTheme.ganttIdTextColor             = new Color(0xaa, 0xaa, 0xaa, 0xff);
        ganttTheme.ganttIdTextErrorColor        = new Color(0xff, 0xff, 0xff, 0xff);
        ganttTheme.ganttCriticalTaskBorderColor = new Color(0xff, 0x0, 0x0, 0xC0);
        ganttTheme.ganttOutOfOfficeColor        = new Color(0xff, 0x33, 0x33, 0x33);

        int ps;//primary color
        int pg;//primary color green
        int ss;//secondary color
        int sg;//secondary color green
        int ts;//trinary color
        int tg;//trinary color green

        int   i     = 0;
        int[] alpha = {0x7f/*, 0x30*/};

        for (i = 0; i < KELLY_COLORS.length; i++) {
            burndownTheme.burnDownColor[i] = new Color(KELLY_COLORS[i].getRed(), KELLY_COLORS[i].getGreen(), KELLY_COLORS[i].getBlue(), alpha[0]);
        }

        //gray
        //        burnDownColor[i++] = new Color(0x0, 0x0, 0x0, 0xa1);
        //        burnDownColor[i++] = new Color(0x0, 0x0, 0x0, 0x51);
        //        burnDownColor[i++] = new Color(0x0, 0x0, 0x0, 0x21);

        graphTextBackgroundColor         = chartBackgroundColor;
        burndownTheme.tickTextColor      = Color.darkGray;
        surroundingSquareColor           = new Color(0xaaaaaa);
        burndownTheme.optimaleGuideColor = new Color(0xa0a0a0);
        burndownTheme.plannedGuideColor  = new Color(0x7f7f7f);
        burndownTheme.ticksColor         = new Color(0xc9c9c9);
        burndownTheme.inTimeColor        = ColorConstants.COLOR_DARK_GREEN;
        burndownTheme.watermarkColor     = new Color(0x10, 0x10, 0x10, 0x10);

//        pastWorkDayRequestColor               = new Color(0x60, 0x00, 0xff, 0x40);
//        pastWeekendRequestColor               = new Color(0x60, 0x60, 0x60, 0x40);
//        futureWorkDayRequestColor             = new Color(0x00, 0x60, 0xff, 0x40);
//        futureWeekendRequestColor             = new Color(0x60, 0x60, 0x60, 0x40);
        ganttTheme.ganttRequestMilestoneColor = Color.RED/*new Color(0xa7, 0x00, 0x00)*/;

//        linkColor = new Color(0x23, 0x6a, 0x97);

//        bankHolidayColor = new Color(0xff, 0, 0, 0x80);
    }

    private int generateColors(int ps, int pg, int ss, int sg, int ts, int tg, int i, int a) {
        burndownTheme.burnDownColor[i++] = new Color(ps, sg, ts, a);
        burndownTheme.burnDownColor[i++] = new Color(ts, pg, ss, a);
        burndownTheme.burnDownColor[i++] = new Color(ss, tg, ps, a);
        return i;
    }
}
