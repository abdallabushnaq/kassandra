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

import de.bushnaq.abdalla.kassandra.report.AbstractRenderer;
import de.bushnaq.abdalla.util.Util;
import de.bushnaq.abdalla.util.date.DateUtil;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.Locale;

public class CalendarXAxes {
    public static final  int                      DAY_OF_MONTH_MIN_DAY_WIDTH = 16;
    private static final int                      DAY_OF_WEEK_MIN_DAY_WIDTH  = 10;
    private static final int                      MONTH_MIN_DAY_WIDTH        = 1;
    private static final int                      WEEK_MIN_DAY_WIDTH         = 2;
    @Getter
    private              boolean                  calendarAtBottom;
    public               CalendarElement          dayOfMonth;
    public               CalendarElement          dayOfWeek;
    private final        DateTimeFormatter        imageMapSdf                = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Util.locale);
    private final        Logger                   logger                     = LoggerFactory.getLogger(this.getClass());
    public               CalendarMilestoneElement milestone;
    private final        Milestones               milestones;
    public               CalendarElement          month;
    private final        AbstractRenderer         parent;
    @Getter
    private final        int                      postRun;
    @Getter
    private final        int                      priRun;
    private final        DateTimeFormatter        sdf                        = DateTimeFormatter.ofPattern("d", Util.locale);
    private final        DateTimeFormatter        sdfMMMdd                   = DateTimeFormatter.ofPattern("MMM.d", Util.locale);
    public               CalendarElement          week;
    @Getter
    private              int                      width                      = 0;
    @Getter
    private              int                      x;
    public               CalendarElement          year;

    /**
     * @param parent
     * @param priRun,  how many days to draw prior to the time range we are
     *                 interested in
     * @param postRun, how many days to draw post to the time range we are
     *                 interested in
     */
    public CalendarXAxes(AbstractRenderer parent, int priRun, int postRun) {
        this.parent     = parent;
        this.priRun     = priRun;
        this.postRun    = postRun;
        this.milestones = parent.milestones;
        int margine = 4;
        year       = new CalendarElement(new Font(Font.SANS_SERIF, Font.PLAIN, 14), null, null, 13 + margine);
        month      = new CalendarElement(new Font(Font.SANS_SERIF, Font.PLAIN, 12), null, null, 12 + margine);
        week       = new CalendarElement(new Font(Font.SANS_SERIF, Font.PLAIN, 10), null, null, 10 + margine);
        dayOfMonth = new CalendarElement(new Font(Font.SANS_SERIF, Font.BOLD, 10), null, 20, 10 + margine);
        dayOfWeek  = new CalendarElement(new Font(Font.SANS_SERIF, Font.BOLD, 10), null, 20, 10 + margine);
        milestone  = new CalendarMilestoneElement(null, null, 11, 10 + margine, new Font(Font.SANS_SERIF, Font.BOLD, 10), null, 13, new Font(Font.SANS_SERIF, Font.PLAIN, 11));
    }

    protected int calculateDayX(LocalDate date) {
        int firstMilestoneX = x + dayOfWeek.getWidth() / 2;
        return firstMilestoneX + (DateUtil.calculateDays(milestones.firstMilestone, date) + priRun) * dayOfWeek.getWidth();
    }

    public void drawCalendar(boolean drawDays) {
        LocalDate firstDay = DateUtil.addDay(milestones.firstMilestone, -priRun);
        //TODO why are we ignoring postRun days?
        LocalDate lastDay           = DateUtil.max(milestones.lastMilestone, DateUtil.addDay(milestones.firstMilestone, parent.days - 1));
        boolean   yearWasDrawn      = false;
        boolean   monthWasDrawn     = false;
        boolean   firstWeekWasDrawn = false;

        // logger.trace(String.format("%s first=%s, last=%s, day.width=%d",
        // father.getImageName(), DateUtil.createDateString(firstDay, sdtmf),
        // DateUtil.createDateString(lastDay, sdtmf), dayOfWeek.width));
        for (int phase = 0; phase < 5; phase++) {
            for (LocalDate currentDay = firstDay; currentDay.isBefore(lastDay) || currentDay.isEqual(lastDay); currentDay = currentDay.plusDays(1)) {
                // if (phase == 4)
                // logger.trace(String.format("first=%s, last=%s, current=%s",
                // Util.createDateString(firstDay, sdtmf), Util.createDateString(lastDay,
                // sdtmf), Util.createDateString(currentDay, sdtmf)));
                int       daysX    = calculateDayX(currentDay);
                String[]  weekDays = {"M", "T", "W", "T", "F", "S", "S"};
                String[]  months   = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                LocalDate startCal = currentDay;

                if (phase == 4 && ((startCal.getDayOfMonth() == 1 && startCal.getMonth() == Month.JANUARY) || !yearWasDrawn)) {
                    // --year
                    {
                        LocalDate end = startCal.withMonth(12).withDayOfMonth(31);// new years eve
                        //                        end.set(Calendar.MONTH, 11); // 11 = december
                        //                        end.set(Calendar.DAY_OF_MONTH, 31); // new years eve
                        if (end.isAfter(lastDay)) {
                            end = lastDay;
                        }
                        int x2 = calculateDayX(end) - dayOfWeek.getWidth() / 2;
                        drawTextBox(daysX - (dayOfWeek.getWidth() / 2 - 1), x2 + dayOfWeek.getWidth(), year.getY(), year.getHeight(),
                                String.valueOf(startCal.getYear()), parent.graphicsTheme.XAxesYearTextColor, parent.graphicsTheme.XAxesYearBackgroundColor,
                                parent.graphicsTheme.XAxesYearBoderColor, year.getFont(), false);
                        yearWasDrawn = true;
                    }
                } else if (phase == 3 && (startCal.getDayOfMonth() == 1 || !monthWasDrawn) && isMonthVisible()) {
                    // --month
                    {
                        LocalDate end = startCal.plusMonths(1).withDayOfMonth(1).minusDays(1);
                        if (end.isAfter(lastDay)) {
                            end = lastDay;
                        }
                        int   x2              = calculateDayX(end) - dayOfWeek.getWidth() / 2;
                        Color backgroundColor = parent.graphicsTheme.XAxesMonthBgColors[startCal.getMonth().getValue() - 1];
                        drawTextBox(daysX - (dayOfWeek.getWidth() / 2 - 1), x2 + dayOfWeek.getWidth(), month.getY(), month.getHeight(),
                                months[startCal.getMonth().getValue() - 1], parent.graphicsTheme.XAxesMonthTextColor, backgroundColor,
                                parent.graphicsTheme.XAxesMonthBorderColor, month.getFont(), false);
                        monthWasDrawn = true;
                    }
                } else if (phase == 2 && (startCal.getDayOfWeek() == DayOfWeek.MONDAY || !firstWeekWasDrawn) && isWeekVisible()) {
                    // --week of the year
                    {
                        LocalDate end = DateUtil.getWeekSunday(startCal);
                        if (end.isAfter(lastDay)) {
                            end = lastDay;
                        }
                        int    x2 = calculateDayX(end) - dayOfWeek.getWidth() / 2;
                        String calendarWeek;
                        if (isDayOfWeekVisible()) {
                            TemporalField woy = WeekFields.of(Locale.CANADA).weekOfWeekBasedYear();
                            calendarWeek = "W" + currentDay.get(woy);
                        } else {
                            calendarWeek = DateUtil.createDateString(currentDay, sdf);
                        }
                        drawTextBox(daysX - (dayOfWeek.getWidth() / 2 - 1), x2 + dayOfWeek.getWidth(), week.getY(), week.getHeight() - 1, calendarWeek,
                                parent.graphicsTheme.XAxesWeekTextColor, parent.graphicsTheme.XAxesWeekBgColor, parent.graphicsTheme.XAxesWeekBoderColor,
                                week.getFont(), false);
                        firstWeekWasDrawn = true;
                    }
                } else if (phase == 1 && isDayOfWeekVisible()) {
                    // --day of Month ( 1, 2, ..., 31)
                    {
                        Color bgColor   = GraphColorUtil.getDayOfMonthBgColor(parent.graphicsTheme, startCal);
                        Color textColor = GraphColorUtil.getDayOfMonthTextColor(parent.graphicsTheme, startCal);
                        drawTextBox(daysX - (dayOfMonth.getWidth() / 2 - 1), daysX - (dayOfMonth.getWidth() / 2 - 1) + (dayOfMonth.getWidth() - 1),
                                dayOfMonth.getY(), dayOfMonth.getHeight(), "" + startCal.getDayOfMonth(), textColor, bgColor,
                                parent.graphicsTheme.XAxesDayOfMonthBorderColor, dayOfMonth.getFont(), true);
//                        if (startCal.getDayOfWeek() == DayOfWeek.SATURDAY || startCal.getDayOfWeek() == DayOfWeek.SUNDAY) {
//                            drawTextBox(daysX - (dayOfMonth.getWidth() / 2 - 1), daysX - (dayOfMonth.getWidth() / 2 - 1) + (dayOfMonth.getWidth() - 1),
//                                    dayOfMonth.getY(), dayOfMonth.getHeight(), "" + startCal.getDayOfMonth(), textColor, bgColor,
//                                    parent.graphicsTheme.XAxesDayOfMonthBorderColor, dayOfMonth.getFont(), true);
//                        } else {
//                            drawTextBox(daysX - (dayOfMonth.getWidth() / 2 - 1), daysX - (dayOfMonth.getWidth() / 2 - 1) + (dayOfMonth.getWidth() - 1),
//                                    dayOfMonth.getY(), dayOfMonth.getHeight(), "" + startCal.getDayOfMonth(), textColor, bgColor,
//                                    parent.graphicsTheme.XAxesDayOfMonthBorderColor, dayOfMonth.getFont(), true);
//                        }
                    }

                    // --day of week (M, T, W, T, F, S, S)
                    {
                        Color color = GraphColorUtil.getDayOfWeekBgColor(parent.graphicsTheme, startCal);
                        drawTextBox(daysX - (dayOfWeek.getWidth() / 2 - 1), daysX - (dayOfWeek.getWidth() / 2 - 1) + (dayOfWeek.getWidth() - 1),
                                dayOfWeek.getY(), dayOfWeek.getHeight(), weekDays[startCal.getDayOfWeek().getValue() - 1], parent.graphicsTheme.XAxesDayOfWeekTextColor,
                                color, parent.graphicsTheme.XAxesDayOfWeekBorderColor, dayOfWeek.getFont(), true);
                    }
                } else if (phase == 0) {
                    {
                        if (drawDays && isDayBarsVisible()) {
                            parent.drawDayBars(currentDay);
                        }
                    }
                    if (milestonesVisible()) {
                        Color color = GraphColorUtil.getDayOfWeekBgColor(parent.graphicsTheme, startCal);
                        parent.graphics2D.setColor(color);
                        drawTextBox(daysX - (dayOfWeek.getWidth() / 2 - 1), daysX - (dayOfWeek.getWidth() / 2 - 1) + (dayOfWeek.getWidth() - 1),
                                milestone.flagY, milestone.flagHeight, null, parent.graphicsTheme.XAxesDayOfWeekTextColor,
                                color, parent.graphicsTheme.XAxesDayOfWeekBorderColor, null, true);
                    }
                }
            }
        }
    }

    public void drawMilestone(Milestone m, LocalDate time, int x, Color fillColor, String text, boolean drawMilestone, Color flagTextColor) {
        drawMilestone(m, time, x, milestone.y, fillColor, text, drawMilestone, milestone.flagY, flagTextColor, true, true);
    }

    public void drawMilestone(Milestone m, LocalDate time, int x, int y, Color fillColor, String text, boolean drawMilestone, Integer flagY,
                              Color flagTextColor, boolean drawFlag, boolean drawNowLine) {
//        String imageMap = "";
        int centerX = x;
        int centerY = y;
        if (text.startsWith("N")) {
            // now line
            if (drawNowLine) {
                parent.graphics2D.setColor(GraphicsTheme.COLOR_DARK_RED);
                parent.graphics2D.fillRect(x, parent.diagram.y, 2, parent.diagram.height);
                int d = Math.max(dayOfWeek.getWidth() / 3, 6);
                if (calendarAtBottom) {
                    parent.graphics2D.fillOval(x + 1 - d / 2, parent.diagram.y - d / 2, d, d);
                } else {
                    parent.graphics2D.fillOval(x + 1 - d / 2, parent.diagram.y + parent.diagram.height - d, d, d);
                }
            }
        }
        if (drawMilestone) {
            // draw milestone
            parent.graphics2D.setFont(milestone.font);
            parent.graphics2D.setColor(fillColor);
            parent.graphics2D.fillRect(centerX - milestone.width / 2, centerY, milestone.width, milestone.height - 1);
            //            if (m != null) {
            //                String lable = DateUtil.createDateString(time, imageMapSdf);
            //                String toolTip = String.format("<b>%s</b> = %s<br>%s.<br>", text, m.name, lable);
            //                Shape s = new TitledRectangle(centerX - milestone.width / 2, centerY, milestone.width, milestone.height - 1, toolTip);
            //                father.graphics2D.fill(s);
            //            } else {
            //                Shape s = new Rectangle(centerX - milestone.width / 2, centerY, milestone.width, milestone.height - 1);
            //                father.graphics2D.fill(s);
            //            }
            parent.graphics2D.setColor(parent.graphicsTheme.milestoneTextColor);
            FontMetrics fm        = parent.graphics2D.getFontMetrics();
            int         maxAscent = fm.getMaxAscent();
            {
                if (m != null) {
                    String lable   = DateUtil.createDateString(time, imageMapSdf);
                    String toolTip = String.format("<b>%s</b> = %s<br>%s.<br>", text, m.name, lable);
                    parent.graphics2D.drawString(text, x - milestone.width / 2 + 2, centerY + milestone.height / 2 + maxAscent / 2 - 2, toolTip);
                } else {
                    parent.graphics2D.drawString(text, x - milestone.width / 2 + 2, centerY + milestone.height / 2 + maxAscent / 2 - 2);
                }
            }
            if (m != null) {
                // imageMap
//                String lable = DateUtil.createDateString(time, imageMapSdf);
//                imageMap += "<area alt=\"<font face=arial>";
//                imageMap += String.format("<b>%s</b> = %s<br>%s.<br>", text, m.name, lable);
//                imageMap += String.format("\" shape=\"rect\" coords=\"%d,%d,%d,%d\"", AbstractCanvas.transformToMapX(centerX - milestone.width / 2),
//                        AbstractCanvas.transformToMapY(centerY), AbstractCanvas.transformToMapX(centerX + milestone.width / 2),
//                        AbstractCanvas.transformToMapY(centerY + milestone.height - 1));
//                imageMap += " >\n";
            }
        }
        if (drawMilestone && drawFlag) {
            parent.graphics2D.setFont(milestone.flagFont);
            FontMetrics fm    = parent.graphics2D.getFontMetrics();
            String      lable = DateUtil.createDateString(time, sdfMMMdd);
            int         width = fm.stringWidth(lable);
            // flag background
            parent.graphics2D.setColor(parent.graphicsTheme.milestoneFlagColor);
            parent.graphics2D.fillRect(x - milestone.width / 2, flagY, width, milestone.flagHeight);
            if (calendarAtBottom) {
                // flag pole
                parent.graphics2D.setColor(flagTextColor);
                parent.graphics2D.fillRect(centerX, milestone.flagY + milestone.flagHeight - 4, 1, 3);
                // flag
                parent.graphics2D.drawString(lable, x - milestone.width / 2, milestone.flagY + milestone.flagHeight - 5);
            } else {
                // flag pole
                parent.graphics2D.setColor(flagTextColor);
                parent.graphics2D.fillRect(centerX, centerY + milestone.height, 1, 3);
                // flag
                parent.graphics2D.drawString(lable, x - milestone.width / 2, milestone.flagY + milestone.flagHeight - 1);
            }
        }
    }

    public void drawMilestones() {
        for (Milestone milestone : milestones.getList()) {
            int x = calculateDayX(milestone.time);
            drawMilestone(milestone, milestone.time, x, parent.graphicsTheme.ganttRequestMilestoneColor, milestone.symbol, !milestone.hidden,
                    parent.graphicsTheme.futureEventColor);// start
        }
    }

    public void drawTextBox(int x1, int x2, Integer y1, int height, String text, Color textColor, Color backgroundColor, Color borderColor, Font font,
                            boolean centered) {
        {
            parent.graphics2D.setColor(backgroundColor);
            parent.graphics2D.fillRect(x1, y1, x2 - x1, height - 1);
        }
        {
            parent.graphics2D.setColor(borderColor);
            parent.graphics2D.fillRect(x1 - 1, y1 - 1, x2 - x1 + 1, 1);
            parent.graphics2D.fillRect(x2, y1, 1, height - 1);
        }
        if (text != null) {
            parent.graphics2D.setFont(font);
            FontMetrics fm    = parent.graphics2D.getFontMetrics();
            int         width = fm.stringWidth(text);
            parent.graphics2D.setColor(textColor);
            int maxAscent = fm.getMaxAscent();
            if (centered) {
                parent.graphics2D.drawString(text, x1 + (x2 - x1) / 2 - width / 2, y1 + height / 2 + (maxAscent + 1) / 2 - 2);
            } else {
                parent.graphics2D.drawString(text, x1 + 1 + 1, y1 + height / 2 + (maxAscent + 1) / 2 - 2);
            }
        }
    }

    public int getHeight() {
        int height = year.getHeight();
        if (isMonthVisible()) {
            height += month.getHeight();
        }
        if (isWeekVisible()) {
            height += week.getHeight();
        }
        if (isDayOfWeekVisible()) {
            height += dayOfWeek.getHeight();
            height += dayOfMonth.getHeight();
        } else if (milestonesVisible()) {
            height += milestone.height;
        }
        if (milestonesVisible()) {
            height += milestone.flagHeight;
        }
        return height;
    }

    public int getY() {
        return year.getY();
    }

    public void initPosition(int x, int y) {
        this.x = x;
        if (calendarAtBottom) {
            // flag
            // milestone, dayOfWeek
            // dayOfMonth
            // week
            // month
            // year

            milestone.flagY = y;
            milestone.y     = milestone.flagY + milestone.flagHeight;
            dayOfWeek.setY(milestone.y);
            dayOfMonth.setY(dayOfWeek.getY() + dayOfWeek.getHeight());
            if (isDayOfWeekVisible()) {
                if (isDayOfMonthVisible()) {
                    week.setY(dayOfMonth.getY() + dayOfMonth.getHeight());
                } else {
                    week.setY(dayOfWeek.getY() + dayOfWeek.getHeight());
                }
            } else {
                week.setY(milestone.y + milestone.height);
            }
            if (isWeekVisible()) {
                month.setY(week.getY() + week.getHeight());
            } else {
                month.setY(dayOfWeek.getY() + dayOfWeek.getHeight());
            }
            year.setY(month.getY() + month.getHeight());
        } else {
            // year
            // month
            // week
            // dayOfMonth
            // milestone, dayOfWeek
            // flag
            year.setY(y);
            month.setY(year.getY() + year.getHeight());
            week.setY(month.getY() + month.getHeight());
            dayOfMonth.setY(week.getY() + week.getHeight());
            if (isDayOfMonthVisible()) {
                dayOfWeek.setY(dayOfMonth.getY() + dayOfMonth.getHeight());
            } else {
                dayOfWeek.setY(week.getY() + week.getHeight());
            }
            milestone.y     = dayOfWeek.getY();
            milestone.flagY = dayOfWeek.getY() + milestone.height;
        }
    }

    public void initSize(int width, int dayWidth, boolean calendarAtBottom) {
        this.calendarAtBottom = calendarAtBottom;
        this.width            = width;
        dayOfWeek.setWidth(dayWidth);
        dayOfMonth.setWidth(dayWidth);
    }

    private boolean isDayBarsVisible() {
        return dayOfWeek.getWidth() >= 4;
    }

    private boolean isDayOfMonthVisible() {
        return dayOfWeek.getWidth() >= DAY_OF_MONTH_MIN_DAY_WIDTH;
    }

    private boolean isDayOfWeekVisible() {
        return dayOfWeek.getWidth() >= DAY_OF_WEEK_MIN_DAY_WIDTH;
    }

    private boolean isMonthVisible() {
        return dayOfWeek.getWidth() >= MONTH_MIN_DAY_WIDTH;
    }

    private boolean isWeekVisible() {
        return dayOfWeek.getWidth() >= WEEK_MIN_DAY_WIDTH;
    }

    private boolean milestonesVisible() {
        return !milestones.empty();
    }

}
