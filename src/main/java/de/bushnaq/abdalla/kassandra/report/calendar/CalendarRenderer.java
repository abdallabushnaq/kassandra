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

package de.bushnaq.abdalla.kassandra.report.calendar;

import de.bushnaq.abdalla.kassandra.Context;
import de.bushnaq.abdalla.kassandra.dto.OffDayType;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.report.AbstractRenderer;
import de.bushnaq.abdalla.kassandra.report.dao.BurnDownGraphicsTheme;
import de.bushnaq.abdalla.svg.util.ExtendedGraphics2D;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectCalendarException;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * renders a gantt chart using ms project mpp file as base
 * Will make out of office time visible
 *
 * @author abdalla.bushnaq
 */
public class CalendarRenderer extends AbstractRenderer {
    private final int                   DAY_SIZE              = 24;
    private final int                   HEADER_HEIGHT         = 25;
    private final int                   LEGEND_HEIGHT         = 24 * 2;
    private final int                   MONTHS_PER_COL        = 3;
    private final int                   MONTHS_PER_ROW        = 4;
    private final int                   MONTH_GAP             = 20;
    private final int                   MONTH_HEIGHT          = 180;
    private final int                   MONTH_WIDTH           = 200;
    private final int                   WEEKDAY_HEADER_HEIGHT = 20;
    private final int                   YEAR_HEIGHT           = 20;
    private final int                   cellHeight            = 16;
    private final int                   cellWidth             = 16;
    //    private       ExtendedGraphics2D    graphics2D;
    private final BurnDownGraphicsTheme graphicsTheme;
    private       int                   holidays;
    private final LocalDateTime         now;
    private       int                   sickDays;
    private       int                   tripDays;
    private final User                  user;
    private       int                   vacationDays          = 0;

    public CalendarRenderer(Context context, User user, LocalDateTime now, String cssClass, BurnDownGraphicsTheme graphicsTheme) throws Exception {
        this.user          = user;
        this.now           = now;
        this.graphicsTheme = graphicsTheme;
        chartWidth         = MONTH_WIDTH * MONTHS_PER_ROW + MONTH_GAP * (MONTHS_PER_ROW - 1);
        chartHeight        = YEAR_HEIGHT + HEADER_HEIGHT + WEEKDAY_HEADER_HEIGHT + MONTH_HEIGHT * MONTHS_PER_COL + MONTH_GAP * (MONTHS_PER_COL - 1) + LEGEND_HEIGHT;
    }

    protected int calculateChartHeight() {
        return cellHeight * 12;
    }

    protected int calculateChartWidth() {
        return cellWidth * 31;
    }

    @Override
    public void draw(ExtendedGraphics2D graphics2D, int x, int y) throws Exception {
        drawCalendar(graphics2D, 2025, now.toLocalDate(), x, y);
    }

    void drawCalendar(Graphics2D graphics2D, int year, LocalDate today, int x, int y) throws IOException, FontFormatException {
        int holidays = 0;

        // Save original font and color
        Font  originalFont  = graphics2D.getFont();
        Color originalColor = graphics2D.getColor();

        // Fonts
        Font smallFont = new Font(Font.SANS_SERIF, Font.PLAIN, 6);

        int yearTextHeight = drawYear(graphics2D, year, x, y);

        // Calculate offset for calendar to position below the year text
        int yearOffset = yearTextHeight + 20; // Add some padding

        // For each month
        for (int monthIndex = 0; monthIndex < 12; monthIndex++) {
            int row = monthIndex / MONTHS_PER_ROW;
            int col = monthIndex % MONTHS_PER_ROW;

            int monthStartX = x + col * (MONTH_WIDTH + MONTH_GAP);
            int monthStartY = y + yearOffset + row * (MONTH_HEIGHT + MONTH_GAP); // Apply the year offset

            // Create LocalDate for first day of month
            LocalDate firstOfMonth = LocalDate.of(year, monthIndex + 1, 1);
            String    monthName    = firstOfMonth.getMonth().toString();

            drawMonthName(graphics2D, monthStartX, monthStartY, monthName);

            drawWeekDays(graphics2D, monthStartX, monthStartY, firstOfMonth);

            // Determine first day of week for this month (0 = Sunday, 6 = Saturday)
            int firstDayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7;
            if (firstDayOfWeek == 0) firstDayOfWeek = 7;  // Convert Sunday from 0 to 7
            firstDayOfWeek--; // Adjust to 0-based index

            // Last day of month
            int lastDay = firstOfMonth.lengthOfMonth();

            // Calculate starting Y position for the first week, adding adequate space after weekday headers
            int daysStartY = monthStartY + HEADER_HEIGHT + WEEKDAY_HEADER_HEIGHT + 10; // Added 10px extra spacing

            // Draw days from previous month to fill the gaps at the beginning
            if (firstDayOfWeek > 0) {
                // Get the previous month
                LocalDate prevMonth        = firstOfMonth.minusMonths(1);
                int       prevMonthLastDay = prevMonth.lengthOfMonth();

                // Draw the days from the previous month
                for (int i = 0; i < firstDayOfWeek; i++) {
                    int prevDay    = prevMonthLastDay - firstDayOfWeek + i + 1;
                    int dayX       = monthStartX + 10 + (i * DAY_SIZE);
                    int dayY       = daysStartY;
                    int dayCenterX = dayX + (DAY_SIZE / 2);
                    int dayCenterY = dayY + (DAY_SIZE / 2) - 6;

                    // Draw with filling text color
                    graphics2D.setColor(graphicsTheme.calendarFillingDayTextColor);
                    drawDayOfMonth(graphics2D, dayCenterX, dayCenterY, prevDay);
                }
            }

            for (int day = 1; day <= lastDay; day++) {
                LocalDate currentDate = LocalDate.of(year, monthIndex + 1, day);
                int       dayOfWeek   = (firstDayOfWeek + day - 1) % 7;
                int       weekNum     = (firstDayOfWeek + day - 1) / 7;
                int       dayX        = monthStartX + 10 + (dayOfWeek * DAY_SIZE);
                int       dayY        = daysStartY + (weekNum * DAY_SIZE);
                // Calculate center position for the day cell
                int dayCenterX = dayX + (DAY_SIZE / 2);
                int dayCenterY = dayY + (DAY_SIZE / 2) - 6; // Adjusted to center the text better

                ProjectCalendar pc        = user.getCalendar();
                boolean         isWeekend = !pc.isWorkingDay(currentDate.getDayOfWeek());
                // Check if today
                boolean isToday = currentDate.equals(today);

                // Check off days
                Color bgColor   = null;
                Color textColor = graphicsTheme.calendarNormalDayTextColor;

                if (isWeekend) {
                    bgColor   = graphicsTheme.calendarWeekendBgColor;
                    textColor = graphicsTheme.calendarWeekendTextColor;
                } else {
                    ProjectCalendarException exception = pc.getException(currentDate);
                    if (exception != null) {
                        String name = exception.getName();
                        if (name.equals(OffDayType.VACATION.name())) {
                            bgColor   = graphicsTheme.calendarVacationBgColor;
                            textColor = graphicsTheme.calendarVacationTextColor;
                            vacationDays++;
                        } else if (name.equals(OffDayType.SICK.name())) {
                            bgColor   = graphicsTheme.calendarSickBgColor;
                            textColor = graphicsTheme.calendarSickTextColor;
                            sickDays++;
                        } else if (name.equals(OffDayType.TRIP.name())) {
                            bgColor   = graphicsTheme.calendarTripBgColor;
                            textColor = graphicsTheme.calendarTripTextColor;
                            tripDays++;
                        } else {
                            bgColor   = graphicsTheme.calendarHolidayBgColor;
                            textColor = graphicsTheme.calendarHolidayTextColor;
                            this.holidays++;
                        }
                    }
                }

                if (bgColor != null) {
                    graphics2D.setColor(bgColor);
                    graphics2D.fillRect(dayCenterX - (DAY_SIZE / 2) + 1, dayCenterY - (DAY_SIZE / 2) + 1, DAY_SIZE - 1, DAY_SIZE - 1);
                }

                graphics2D.setColor(textColor);
                //today can be any day, even weekend, holiday, vacation or sock day.
                if (isToday) {
                    drawToday(graphics2D, dayCenterX, dayCenterY);
                }
                drawDayOfMonth(graphics2D, dayCenterX, dayCenterY, day);
            }

            // Draw days from next month to fill the gaps at the end
            int lastDayOfWeek = (firstDayOfWeek + lastDay - 1) % 7;
            if (lastDayOfWeek < 6) { // If the month doesn't end on a Sunday
                int daysToAdd = 6 - lastDayOfWeek;

                for (int i = 1; i <= daysToAdd; i++) {
                    int weekNum    = (firstDayOfWeek + lastDay - 1 + i) / 7;
                    int dayOfWeek  = (firstDayOfWeek + lastDay - 1 + i) % 7;
                    int dayX       = monthStartX + 10 + (dayOfWeek * DAY_SIZE);
                    int dayY       = daysStartY + (weekNum * DAY_SIZE);
                    int dayCenterX = dayX + (DAY_SIZE / 2);
                    int dayCenterY = dayY + (DAY_SIZE / 2) - 6;

                    // Draw with filling text color
                    graphics2D.setColor(graphicsTheme.calendarFillingDayTextColor);
                    drawDayOfMonth(graphics2D, dayCenterX, dayCenterY, i);
                }
            }
        }
        int legendStartY = y + (6 * DAY_SIZE) + yearOffset + 2 * (MONTH_HEIGHT + MONTH_GAP) + HEADER_HEIGHT + WEEKDAY_HEADER_HEIGHT + 10; // Added 10px extra spacing
        drawLegend(graphics2D, x + 15, legendStartY);

        // Restore original settings
        graphics2D.setFont(originalFont);
        graphics2D.setColor(originalColor);
    }

    private static void drawDayOfMonth(Graphics2D graphics2D, int dayCenterX, int dayCenterY, int day) {
        // Draw day number
        String      dayStr    = String.valueOf(day);
        FontMetrics metrics   = graphics2D.getFontMetrics();
        int         textWidth = metrics.stringWidth(dayStr);
        Font        dayFont   = new Font(Font.SANS_SERIF, Font.BOLD, 10);
        graphics2D.setFont(dayFont);
        graphics2D.drawString(dayStr, dayCenterX - (textWidth / 2), dayCenterY + (metrics.getAscent() / 2));
    }

    // Helper method to draw example day numbers in legend
    private void drawExampleDayNumber(Graphics2D graphics2D, int x, int y, int squareSize, String dayNumber) {
        Font originalFont = graphics2D.getFont();
        // Use the same font style as in the actual calendar
        Font dayFont = new Font(Font.SANS_SERIF, Font.BOLD, 10);
        graphics2D.setFont(dayFont);

        FontMetrics fm        = graphics2D.getFontMetrics();
        int         textWidth = fm.stringWidth(dayNumber);
        int         yShift    = fm.getAscent() - fm.getHeight() / 2;

        // Center the day number in the square
        int centerX = x + squareSize / 2;
        int centerY = y + squareSize / 2;

        graphics2D.drawString(dayNumber, centerX - (textWidth / 2), centerY + yShift);

        // Restore original font
        graphics2D.setFont(originalFont);
    }

    private void drawLegend(Graphics2D graphics2D, int x, int y) {
        // Save original font and color
        Font  originalFont  = graphics2D.getFont();
        Color originalColor = graphics2D.getColor();

        Font legendFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        graphics2D.setFont(legendFont);

        int itemWidth  = 100;
        int itemHeight = 20;
        int squareSize = 16;
        int gap        = 10;

        // Create legend items
        List<LegendItem> legendItems = Arrays.asList(
                new LegendItem(graphicsTheme.calendarVacationBgColor, graphicsTheme.calendarVacationTextColor, "Vacation", "" + vacationDays),
                new LegendItem(graphicsTheme.calendarSickBgColor, graphicsTheme.calendarSickTextColor, "Sick Leave", "" + sickDays),
                new LegendItem(graphicsTheme.calendarHolidayBgColor, graphicsTheme.calendarHolidayTextColor, "Holiday", "" + holidays),
                new LegendItem(graphicsTheme.calendarTripBgColor, graphicsTheme.calendarTripTextColor, "Business Trip", "" + tripDays)
        );

        // Draw each legend item
        for (int i = 0; i < legendItems.size(); i++) {
            LegendItem item  = legendItems.get(i);
            int        itemX = x + (itemWidth * i);

            // Draw the colored box
            graphics2D.setColor(item.bgColor);
            graphics2D.fillRect(itemX, y, squareSize, squareSize);

            // Draw example day number
            graphics2D.setColor(item.textColor);
            drawExampleDayNumber(graphics2D, itemX, y, squareSize, item.exampleNumber);

            // Draw the label
            FontMetrics fm     = graphics2D.getFontMetrics();
            int         yShift = fm.getAscent() - fm.getHeight() / 2;
            graphics2D.drawString(item.label, itemX + squareSize + 5, y + squareSize / 2 + yShift);
        }

        // Restore original settings
        graphics2D.setFont(originalFont);
        graphics2D.setColor(originalColor);
    }

    private void drawMonthName(Graphics2D graphics2D, int monthStartX, int monthStartY, String monthName) {
        // Draw month name
        Font monthFont = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
        graphics2D.setFont(monthFont);
        graphics2D.setColor(graphicsTheme.calendarMonthNameColor);
        FontMetrics monthMetrics = graphics2D.getFontMetrics();
        int         monthAscent  = monthMetrics.getAscent();
        graphics2D.drawString(monthName, monthStartX + 10 + DAY_SIZE / 2 - 5, monthStartY + monthAscent);
    }

    private void drawToday(Graphics2D graphics2D, int dayCenterX, int dayCenterY) {
        graphics2D.setColor(graphicsTheme.calendarTodayBgColor);
        graphics2D.fillOval(dayCenterX - (DAY_SIZE / 2) + 1, dayCenterY - (DAY_SIZE / 2) + 1, DAY_SIZE - 1, DAY_SIZE - 1);
        graphics2D.setColor(graphicsTheme.calendarTodayTextColor);
    }

    private void drawWeekDays(Graphics2D graphics2D, int monthStartX, int monthStartY, LocalDate firstOfMonth) {
        // Draw weekday headers
        // Day of week labels
        String[] weekdays    = {"M", "T", "W", "T", "F", "S", "S"};
        Font     weekdayFont = new Font(Font.SANS_SERIF, Font.BOLD, 10);
        graphics2D.setFont(weekdayFont);
        FontMetrics weekdayMetrics = graphics2D.getFontMetrics();
        int         weekdayAscent  = weekdayMetrics.getAscent();
        for (int i = 0; i < 7; i++) {
            int weekdayX = monthStartX + 10 + (i * DAY_SIZE);
            // Center the weekday text vertically in the weekday header area
            int weekdayY = monthStartY + HEADER_HEIGHT + (WEEKDAY_HEADER_HEIGHT / 2) + (weekdayAscent / 2);
            // Get the calendar day type from the user's calendar
            ProjectCalendar pc      = user.getCalendar();
            LocalDate       dayDate = firstOfMonth.withDayOfMonth(1).plusDays(i);
            graphics2D.setColor(graphicsTheme.calendarWeekDayTextColor);
            graphics2D.drawString(weekdays[i], weekdayX + DAY_SIZE / 2 - 5, weekdayY);
        }
    }

    private int drawYear(Graphics2D graphics2D, int year, int x, int y) throws IOException, FontFormatException {
        Font yearFont = new Font(Font.SANS_SERIF, Font.PLAIN, 24);
//        Font yearFont = loadFont("Inter[opsz,wght].ttf", 24f);
        graphics2D.setFont(yearFont);
        graphics2D.setColor(graphicsTheme.calendarYearTextColor);
        String yearText = String.valueOf(year);

        // Calculate year text dimensions
        FontMetrics yearMetrics    = graphics2D.getFontMetrics(yearFont);
        int         yearTextHeight = yearMetrics.getHeight();
        int         yearTextWidth  = yearMetrics.stringWidth(yearText);

        // Center the year text
        graphics2D.drawString(yearText, x + 5, y + yearMetrics.getAscent());
        return yearTextHeight;
    }

    Font loadFont(String fontName, float size) {
        try (FileInputStream fontStream = new FileInputStream("resources/fonts/ofl/inter/" + fontName)) {
            Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
            // Use FontVariation API for Java 17+
            Map map = Map.of(TextAttribute.WEIGHT, TextAttribute.WEIGHT_LIGHT);
            return font.deriveFont(map).deriveFont(size);

        } catch (FontFormatException | IOException e) {
            logger.error(e.getMessage(), e);
            return new Font(Font.SANS_SERIF, Font.PLAIN, 24);
        }
    }

    private static class LegendItem {
        private final Color  bgColor;
        private final String exampleNumber;
        private final String label;
        private final Color  textColor;

        public LegendItem(Color bgColor, Color textColor, String label, String exampleNumber) {
            this.bgColor       = bgColor;
            this.textColor     = textColor;
            this.label         = label;
            this.exampleNumber = exampleNumber;
        }
    }
}

