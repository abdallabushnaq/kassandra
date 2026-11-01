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

package de.bushnaq.abdalla.kassandra.report;

import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.report.burndown.RenderDao;
import de.bushnaq.abdalla.kassandra.report.dao.*;
import de.bushnaq.abdalla.kassandra.report.dao.theme.Theme;
import de.bushnaq.abdalla.svg.util.ExtendedGraphics2D;
import de.bushnaq.abdalla.util.date.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;


/**
 * abstract base class used by all renderer
 *
 * @author abdalla.bushnaq
 */
@Deprecated
public abstract class LegacyAbstractRenderer {
    public static final    int                 MAX_DAY_WIDTH              = 20;
    protected static final float               STANDARD_LINE_STROKE_WIDTH = 3.1f;
    protected final        Font                authorFont                 = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    protected              Users               authors                    = new Users();
    protected              Font                bankHolidayFont            = null;
    private                boolean             calendarAtBottom;
    public                 int                 chartHeight;
    public                 int                 chartWidth;
    public                 int                 days                       = 3;
    public final           GraphSquare         diagram                    = new GraphSquare();
    protected              int                 firstDayX                  = 0;
    public                 ExtendedGraphics2D  graphics2D;
    public                 LegacyCalendarXAxes legacyCalendarXAxes;
    protected final        Logger              logger                     = LoggerFactory.getLogger(this.getClass());
    public                 Milestones          milestones;
    public                 Theme               theme;

    public LegacyAbstractRenderer() {

    }

    public LegacyAbstractRenderer(RenderDao dao) throws IOException {
        this.theme       = dao.kassandraTheme;
        this.chartWidth  = dao.chartWidth;
        this.chartHeight = dao.chartHeight;
        if (dao.sprint != null) {
            milestones = new Milestones(dao.sprint.getName());
        } else {
            milestones = new Milestones(dao.name);
        }
        legacyCalendarXAxes = new LegacyCalendarXAxes(this, dao.preRun, dao.postRun);
    }

    protected int calculateChartHeight() {
        return chartHeight;
    }

    protected int calculateChartWidth() {
        return chartWidth;
    }

    protected LocalDate calculateDayFromIndex(int index) {
        LocalDate firstMilestoneDay = milestones.firstMilestone;
        return DateUtil.addDay(firstMilestoneDay, index);
    }

    protected int calculateDayIndex(LocalDate date) {
        LocalDate firstMilestoneDay = milestones.firstMilestone;
        return DateUtil.calculateDays(firstMilestoneDay, date);
    }

    protected int calculateDayIndex(LocalDateTime date) {
        return calculateDayIndex(date.toLocalDate());
    }

    protected void calculateDayWidth() {
        days = calculateMaxDays();
        legacyCalendarXAxes.dayOfWeek.setWidth((chartWidth) / days);
    }

    protected int calculateDayX(LocalDate date) {
        LocalDate firstMilestoneDay = milestones.firstMilestone;
        int       firstMilestoneX   = firstDayX + legacyCalendarXAxes.dayOfWeek.getWidth() / 2;
        return firstMilestoneX + (DateUtil.calculateDays(firstMilestoneDay, date) + legacyCalendarXAxes.getPriRun()) * legacyCalendarXAxes.dayOfWeek.getWidth();
    }

    protected int calculateMaxDays() {
        return DateUtil.calculateDays(milestones.firstMilestone, milestones.lastMilestone) + 1 + legacyCalendarXAxes.getPriRun() + legacyCalendarXAxes.getPostRun();
    }

    protected int calculateX(LocalDateTime date, LocalDateTime startTime, long secondsPerDay) {
        LocalDate firstMilestoneDay = milestones.firstMilestone;
        int       firstMilestoneX   = firstDayX + legacyCalendarXAxes.dayOfWeek.getWidth() / 2;
        // String createDateTimeString = DateUtil.createDateTimeString(date);
        //        LocalDateTime timeOfDay = LocalDateTimeUtil.getTimeOfDayInMillis(date);
        Duration workedToday = Duration.between(startTime, date);
        int      dayX        = firstMilestoneX + (DateUtil.calculateDays(firstMilestoneDay, DateUtil.toDayPrecision(date)) + legacyCalendarXAxes.getPriRun()) * legacyCalendarXAxes.dayOfWeek.getWidth();
        int      timeOfDayX  = (int) (((workedToday.getSeconds()) * legacyCalendarXAxes.dayOfWeek.getWidth()) / secondsPerDay);
        return dayX + timeOfDayX;
    }

    public abstract void draw(ExtendedGraphics2D graphics2D, int x, int y) throws Exception;

    protected void drawAuthor(int x, int y, int with, Color fillColor, String text, Color textColor, Font font) {
        graphics2D.setFont(font);
        graphics2D.setColor(fillColor);
        graphics2D.fillRect(x, y - legacyCalendarXAxes.milestone.height / 2, with + 4, legacyCalendarXAxes.milestone.height - 1);
        graphics2D.setColor(textColor);
        FontMetrics fm        = graphics2D.getFontMetrics();
        int         maxAscent = fm.getMaxAscent();
        graphics2D.drawString(text, x + 2, y + maxAscent / 2 - 2);
    }

    protected void drawAuthorLegend(int x, int y) {
        int authorLegendWidth = 20;
        for (User author : authors.getList()) {
//            String primaryAuthorName = Authors.mapToPrimaryLoginName(author.name);
//            if (primaryAuthorName == null) {
//                primaryAuthorName = author.name;
//            }
            FontMetrics metrics = graphics2D.getFontMetrics(legacyCalendarXAxes.milestone.font);
            int         adv     = metrics.stringWidth(author.getName());
            authorLegendWidth = Math.max(authorLegendWidth, adv);
        }
        int lineHeight = 14;
        int ay         = y + lineHeight * authors.getList().size();
        for (User author : authors.getList()) {
//            String primaryAuthorName = Authors.mapToPrimaryLoginName(author.name);
//            if (primaryAuthorName == null) {
//                primaryAuthorName = author.name;
//            }
            drawAuthor(x, ay, authorLegendWidth, author.getColor(), author.getName(), theme.burndownTheme.tickTextColor, legacyCalendarXAxes.milestone.font);
            ay -= lineHeight;
        }
    }

    protected void drawCalendar() {
        drawCalendar(true);
    }

    protected void drawCalendar(boolean drawDays) {
        legacyCalendarXAxes.drawCalendar(drawDays);
    }

    public void drawDayBars(LocalDate currentDay) {
        //        Calendar day = Calendar.getInstance();
        //        day.setTimeInMillis(currentDay);
        Color color = GraphColorUtil.getDayOfWeekBgColor(theme/*, bankHolidays*/, currentDay);
        int   x     = calculateDayX(currentDay);
        graphics2D.setColor(color);
        //day vertical bar
        graphics2D.fillRect(x - (legacyCalendarXAxes.dayOfWeek.getWidth() / 2 - 1), diagram.y, legacyCalendarXAxes.dayOfWeek.getWidth() - 1, diagram.height);//left |
        //draw vertical lines
        graphics2D.setColor(theme.ganttTheme.gridColor);
        //        graphics2D.setStroke(new BasicStroke(RELATION_LINE_STROKE_WIDTH));
        //        graphics2D.drawLine(x - (calendarXAxses.dayOfWeek.getWidth() / 2 - 1) + (calendarXAxses.dayOfWeek.getWidth() - 1), diagram.y,x - (calendarXAxses.dayOfWeek.getWidth() / 2 - 1) + (calendarXAxses.dayOfWeek.getWidth() - 1), diagram.y + diagram.height);
        //        graphics2D.draw(new Line2D.Double(x - (calendarXAxses.dayOfWeek.getWidth() / 2 - 1) + (calendarXAxses.dayOfWeek.getWidth() - 1), diagram.y,x - (calendarXAxses.dayOfWeek.getWidth() / 2 - 1) + (calendarXAxses.dayOfWeek.getWidth() - 1), diagram.y + diagram.height));
        graphics2D.fillRect(x - (legacyCalendarXAxes.dayOfWeek.getWidth() / 2 - 1) + (legacyCalendarXAxes.dayOfWeek.getWidth() - 1), diagram.y, 1, diagram.height);//right |
    }

    protected void drawGraphText(int x, int y, String text, Color textColor, Font font, TextAlignment aligned) {
        // flag
        graphics2D.setFont(font);
        FontMetrics fm    = graphics2D.getFontMetrics();
        int         width = fm.stringWidth(text);
        graphics2D.setColor(theme.chartTheme.graphTextBackgroundColor);
        switch (aligned) {
            case left:
                graphics2D.fillRect(x, y - 9 + 2, width, 12);
                graphics2D.setColor(textColor);
                graphics2D.drawString(text, x, y + 2);
                break;
            case right:
                graphics2D.fillRect(x - width, y - 9 + 2, width, 12);
                graphics2D.setColor(textColor);
                graphics2D.drawString(text, x - width, y + 2);
                break;
        }

    }

    protected void drawLegend(int x, int y, Color interpolationColor) {
        int lineHeight  = 14;
        int legendY     = y + lineHeight;
        int legendX1    = x;
        int legendX2    = legendX1 + 10;
        int legendTextY = legendY + 1;
        int legendTextX = legendX2 + 4;
        int milestoneX  = legendX1 + 5;
        int milestoneY  = legendY - legacyCalendarXAxes.milestone.height / 2;

        graphics2D.setStroke(new BasicStroke(STANDARD_LINE_STROKE_WIDTH, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{3}, 0));
        graphics2D.setColor(theme.chartTheme.surroundingSquareColor);
        graphics2D.drawLine(legendX1, legendY, legendX2, legendY);
        drawGraphText(legendTextX, legendTextY, "Guideline", theme.burndownTheme.tickTextColor, legacyCalendarXAxes.dayOfWeek.getFont(), TextAlignment.left);

        legendY += lineHeight;
        legendTextY += lineHeight;
        milestoneY += lineHeight;
        graphics2D.setColor(interpolationColor);
        graphics2D.drawLine(legendX1, legendY, legendX2, legendY);
        drawGraphText(legendTextX, legendTextY, "extrapolated release date", theme.burndownTheme.tickTextColor, legacyCalendarXAxes.dayOfWeek.getFont(),
                TextAlignment.left);

        legendY += lineHeight;
        legendTextY += lineHeight;
        milestoneY += lineHeight;
        graphics2D.setStroke(new BasicStroke(STANDARD_LINE_STROKE_WIDTH));
        graphics2D.setColor(theme.burndownTheme.borderColor);
        graphics2D.drawLine(legendX1, legendY, legendX2, legendY);
        drawGraphText(legendTextX, legendTextY, "Remaining work", theme.burndownTheme.tickTextColor, legacyCalendarXAxes.dayOfWeek.getFont(), TextAlignment.left);

        legendY += lineHeight;
        legendTextY += lineHeight;
        milestoneY += lineHeight;
        legacyCalendarXAxes.drawMilestone(null, null, milestoneX, milestoneY, theme.xAxesTheme.pastEventColor, "S", true, null, null, false, false);// start
        drawGraphText(legendTextX, legendTextY, "Start date (sprint)", theme.burndownTheme.tickTextColor, legacyCalendarXAxes.dayOfWeek.getFont(), TextAlignment.left);

        legendY += lineHeight;
        legendTextY += lineHeight;
        milestoneY += lineHeight;
        legacyCalendarXAxes.drawMilestone(null, null, milestoneX, milestoneY, theme.xAxesTheme.nowEventColor, "N", true, null, null, false, false);// now
        drawGraphText(legendTextX, legendTextY, "Now date", theme.burndownTheme.tickTextColor, legacyCalendarXAxes.dayOfWeek.getFont(), TextAlignment.left);

        legendY += lineHeight;
        legendTextY += lineHeight;
        milestoneY += lineHeight;
        legacyCalendarXAxes.drawMilestone(null, null, milestoneX, milestoneY, theme.burndownTheme.delayEventColor, "R", true, null, null, false, false);// release
        drawGraphText(legendTextX, legendTextY, "Release date", theme.burndownTheme.tickTextColor, legacyCalendarXAxes.dayOfWeek.getFont(), TextAlignment.left);

        legendY += lineHeight;
        legendTextY += lineHeight;
        milestoneY += lineHeight;
        legacyCalendarXAxes.drawMilestone(null, null, milestoneX, milestoneY, theme.xAxesTheme.futureEventColor, "E", true, null, null, false, false);// end
        drawGraphText(legendTextX, legendTextY, "End date (sprint)", theme.burndownTheme.tickTextColor, legacyCalendarXAxes.dayOfWeek.getFont(), TextAlignment.left);

        legendY += lineHeight;
        legendTextY += lineHeight;
        milestoneY += lineHeight;
        legacyCalendarXAxes.drawMilestone(null, null, milestoneX, milestoneY, theme.xAxesTheme.futureEventColor, "F", true, null, null, false, false);// first
        drawGraphText(legendTextX, legendTextY, "First punch-in", theme.burndownTheme.tickTextColor, legacyCalendarXAxes.dayOfWeek.getFont(), TextAlignment.left);

        legendY += lineHeight;
        legendTextY += lineHeight;
        milestoneY += lineHeight;
        legacyCalendarXAxes.drawMilestone(null, null, milestoneX, milestoneY, theme.xAxesTheme.futureEventColor, "L", true, null, null, false, false);// Last
        drawGraphText(legendTextX, legendTextY, "Last punch-out", theme.burndownTheme.tickTextColor, legacyCalendarXAxes.dayOfWeek.getFont(), TextAlignment.left);
    }

    protected void drawMilestones() {
        legacyCalendarXAxes.drawMilestones();
    }

    public int getDayWidth() {
        return legacyCalendarXAxes.dayOfWeek.getWidth();
    }

    protected int getTaskHeight() {
        return 13 + 4;
    }

    protected void initPosition(int x, int y) throws IOException {
        firstDayX = x;
        if (calendarAtBottom) {
            legacyCalendarXAxes.initPosition(x, y);
            diagram.initPosition(x, y);
            legacyCalendarXAxes.initPosition(x, diagram.y + diagram.height + 1);
        } else {
            legacyCalendarXAxes.initPosition(x, y);
            diagram.initPosition(x, legacyCalendarXAxes.year.getY() + legacyCalendarXAxes.getHeight());
//            legacyCalendarXAxes.initPosition(x, diagram.y + diagram.height + 1);
        }
    }

    protected void initSize(int x, boolean calendarAtBottom, CalendarSize calendarSize) throws IOException {
        this.calendarAtBottom            = calendarAtBottom;
        legacyCalendarXAxes.calendarSize = calendarSize;
        // this.calendarAtBottom = calendarAtBottom;
        calculateDayWidth();
        chartWidth      = calculateChartWidth();
        chartHeight     = calculateChartHeight();
        firstDayX       = x;
        bankHolidayFont = new Font(Font.SANS_SERIF, Font.PLAIN, Math.min(14, (int) (legacyCalendarXAxes.dayOfWeek.getWidth() * 1.1)));

        legacyCalendarXAxes.initSize(chartWidth, legacyCalendarXAxes.dayOfWeek.getWidth(), calendarAtBottom, calendarSize);
        diagram.initSize(chartWidth - x, chartHeight - legacyCalendarXAxes.getHeight());
        legacyCalendarXAxes.initSize(chartWidth, legacyCalendarXAxes.dayOfWeek.getWidth(), calendarAtBottom, calendarSize);
    }

    public void setDayWidth(int dayWidth) {
        legacyCalendarXAxes.dayOfWeek.setWidth(dayWidth);
        legacyCalendarXAxes.dayOfMonth.setWidth(dayWidth);
        chartWidth  = calculateChartWidth();
        chartHeight = calculateChartHeight();
    }

}
