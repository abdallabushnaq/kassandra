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
public class LightTheme extends Theme {
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

    public LightTheme(StableDiffusionConfig stableDiffusionConfig) {
        super(ETheme.light);
        Color basicTextColor = Color.WHITE;
        Color baseBgColor    = ColorUtil.hexStringToColor(stableDiffusionConfig.getAvatarLightBackgroundColor());


        //---------------------------------------------------------------------
        //-- ChartTheme
        //---------------------------------------------------------------------
        chartTheme.backgroundColor          = baseBgColor;
        chartTheme.graphTextBackgroundColor = chartTheme.backgroundColor;
        chartTheme.surroundingSquareColor   = new Color(0xaaaaaa);
        ganttTheme.requestMilestoneColor    = Color.RED/*new Color(0xa7, 0x00, 0x00)*/;
        //---------------------------------------------------------------------


        //---------------------------------------------------------------------
        //-- CalendarTheme
        //---------------------------------------------------------------------
        calendarTheme.fillingDayTextColor = new Color(0xe2dbdb); // very Light gray for filling days before and after the days we are interested in
        calendarTheme.holidayBgColor      = new Color(183, 216, 240);  // Light blue
        calendarTheme.holidayTextColor    = new Color(123, 180, 200);  // Light blue
        calendarTheme.monthNameColor      = new Color(0xff6336);  // Red for month names
        calendarTheme.normalDayTextColor  = new Color(0x323232);  // almost black
        calendarTheme.sickBgColor         = new Color(0xfff2e8); // Light red
        calendarTheme.sickTextColor       = new Color(0xff6d5b); // Light red
        calendarTheme.todayBgColor        = new Color(0xff3a30);  // Red circle for today
        calendarTheme.todayTextColor      = Color.white;  // White text for today
        calendarTheme.tripBgColor         = new Color(183, 183, 183);// new Color(0xfffcea);  // Light yellow
        calendarTheme.tripTextColor       = new Color(64, 64, 64);//new Color(0xff931e);  // Light yellow
        calendarTheme.vacationBgColor     = new Color(183, 240, 216);  // Light green
        calendarTheme.vacationTextColor   = new Color(123, 200, 180);  // Light green
        calendarTheme.weekDayTextColor    = new Color(180, 180, 180);  // Light gray for weekends
        calendarTheme.weekendBgColor      = Color.WHITE;
        calendarTheme.weekendTextColor    = new Color(180, 180, 180);  // Light gray for weekends
        calendarTheme.yearTextColor       = new Color(80, 80, 80);  // Dark gray for year display
        //---------------------------------------------------------------------

        //---------------------------------------------------------------------
        //-- XAxesTheme
        //---------------------------------------------------------------------
        //-------------------------- Day of Month
        xAxesTheme.dayOfMonthBgColor          = new Color(0xababab);
        xAxesTheme.dayOfMonthBorderColor      = Color.WHITE;
        xAxesTheme.dayOfMonthTextColor        = Color.WHITE;
        xAxesTheme.dayOfMonthWeekendBgColor   = new Color(247, 247, 247);
        xAxesTheme.dayOfMonthWeekendTextColor = Color.BLACK;
        //------------------------- Day of Week
        xAxesTheme.dayOfweekBgColor          = Color.WHITE;
        xAxesTheme.dayOfWeekBorderColor      = Color.WHITE;
        xAxesTheme.dayOfWeekTextColor        = Color.BLACK;
        xAxesTheme.dayOfweekWeekendBgColor   = new Color(247, 247, 247);
        xAxesTheme.dayOfWeekWeekendTextColor = Color.white;
        //------------------------- Month
        xAxesTheme.monthBorderColor = Color.WHITE;
        xAxesTheme.monthTextColor   = Color.WHITE;
        //------------------------- Week
        xAxesTheme.weekBgColor    = new Color(0xababab);
        xAxesTheme.weekBoderColor = Color.WHITE;
        xAxesTheme.weekTextColor  = Color.WHITE;
        //------------------------- Year
        xAxesTheme.yearBgColor    = new Color(0xababab);
        xAxesTheme.yearBoderColor = Color.white;
        xAxesTheme.yearTextColor  = Color.WHITE;
        //-------------------------
        xAxesTheme.futureEventColor   = Color.blue;
        xAxesTheme.milestoneFlagColor = chartTheme.backgroundColor;
        xAxesTheme.milestoneTextColor = basicTextColor;
        xAxesTheme.nowEventColor      = Color.gray;
        xAxesTheme.pastEventColor     = Color.lightGray;
        //---------------------------------------------------------------------

        //---------------------------------------------------------------------
        //-- GanttTheme
        //---------------------------------------------------------------------
        ganttTheme.relationColor         = new Color(0x34, 0x66, 0xed, 0x7f);
        ganttTheme.criticalRelationColor = new Color(0xff, 0, 0, 0x7f);
        ganttTheme.milestoneColor        = new Color(0x4f, 0xbb, 0xc2, 0xff);
        ganttTheme.milestoneTextColor    = new Color(0x50, 0x50, 0x50, 0xff);
        ganttTheme.storyColor            = Color.black;//new Color(64, 64, 64, 0xa0);
        ganttTheme.storyTextColor        = Color.darkGray;
        ganttTheme.taskTextColor         = new Color(0x30, 0x30, 0x30, 0xff);
        ganttTheme.taskBorderColor       = new Color(0x30, 0x30, 0x30, 0x7F);
        ganttTheme.idColor               = new Color(0xff, 0xff, 0xff, 0xff);
//        ganttTheme.ganttIdErrorColor            = new Color(0xff, 0x0, 0x0, 0xff);
        ganttTheme.idTextColor = new Color(0xaa, 0xaa, 0xaa, 0xff);
//        ganttTheme.ganttIdTextErrorColor        = new Color(0xff, 0xff, 0xff, 0xff);
        ganttTheme.criticalTaskBorderColor = new Color(0xff, 0x0, 0x0, 0xC0);
        ganttTheme.gridColor               = new Color(0xe5f2ff, false);
        //--DayStripeColors
        ganttTheme.outOfOfficeColor  = new Color(0xff, 0xff, 0xff, 0xff);
        ganttTheme.sickBgColor       = new Color(0xffe6e6);
        ganttTheme.tripBgColor       = new Color(0xffe6e6);
        ganttTheme.vacationBgColor   = new Color(0xffe6e6);
        ganttTheme.taskTickLineColor = new Color(183, 216, 240);
        ganttTheme.taskTickTextColor = new Color(0, 0, 0, 127);
        //---------------------------------------------------------------------


        //---------------------------------------------------------------------
        //-- BurndownTheme
        //---------------------------------------------------------------------
        burndownTheme.delayEventColor = Color.red;
        int   i     = 0;
        int[] alpha = {0x7f/*, 0x30*/};

        for (i = 0; i < KELLY_COLORS.length; i++) {
            burndownTheme.burnDownColor[i] = new Color(KELLY_COLORS[i].getRed(), KELLY_COLORS[i].getGreen(), KELLY_COLORS[i].getBlue(), alpha[0]);
        }
        burndownTheme.tickTextColor      = Color.darkGray;
        burndownTheme.borderColor        = new Color(0xff, 0xcc, 0x00, 0x77);
        burndownTheme.optimaleGuideColor = new Color(0xa0a0a0);
        burndownTheme.plannedGuideColor  = new Color(0x7f7f7f);
        burndownTheme.ticksColor         = new Color(0xc9c9c9);
        burndownTheme.inTimeColor        = ColorConstants.COLOR_DARK_GREEN;
        burndownTheme.watermarkColor     = new Color(0x10, 0x10, 0x10, 0x10);
        //---------------------------------------------------------------------


        // gray
//        Color basicBackgroundColor = new Color(0x7a7a7a);
//        Color basicBorderColor     = Color.WHITE;


//        dayOfMonthTextColor       = basicTextColor;
//        dayOfMonthBackgroundColor = basicBackgroundColor;

//        dayTextColor          = Color.BLACK;
//        dayBackgroundColor    = Color.WHITE;
//        dayDiagramBorderColor = new Color(0xf6f8ff);

//        delayClosedEventColor = new Color(255, 184, 184);
//        ticksBackgroundColor = chartBackgroundColor;

//        weekBackgroundColor = basicBackgroundColor;
//        weekTextColor       = basicTextColor;


//        int ps;//primary color
//        int pg;//primary color green
//        int ss;//secondary color
//        int sg;//secondary color green
//        int ts;//trinary color
//        int tg;//trinary color green


        //gray
        //        burnDownColor[i++] = new Color(0x0, 0x0, 0x0, 0xa1);
        //        burnDownColor[i++] = new Color(0x0, 0x0, 0x0, 0x51);
        //        burnDownColor[i++] = new Color(0x0, 0x0, 0x0, 0x21);


//        pastWorkDayRequestColor               = new Color(0x60, 0x00, 0xff, 0x40);
//        pastWeekendRequestColor               = new Color(0x60, 0x60, 0x60, 0x40);
//        futureWorkDayRequestColor             = new Color(0x00, 0x60, 0xff, 0x40);
//        futureWeekendRequestColor             = new Color(0x60, 0x60, 0x60, 0x40);

//        linkColor = new Color(0x23, 0x6a, 0x97);

//        bankHolidayColor = new Color(0xff, 0, 0, 0x80);
    }

//    private int generateColors(int ps, int pg, int ss, int sg, int ts, int tg, int i, int a) {
//        burndownTheme.burnDownColor[i++] = new Color(ps, sg, ts, a);
//        burndownTheme.burnDownColor[i++] = new Color(ts, pg, ss, a);
//        burndownTheme.burnDownColor[i++] = new Color(ss, tg, ps, a);
//        return i;
//    }
}
