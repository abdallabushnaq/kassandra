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

package de.bushnaq.abdalla.kassandra.report.gantt;

import de.bushnaq.abdalla.kassandra.Context;
import de.bushnaq.abdalla.kassandra.dto.Relation;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.TaskMode;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.report.AbstractRenderer;
import de.bushnaq.abdalla.kassandra.report.dao.*;
import de.bushnaq.abdalla.svg.util.ExtendedRectangle;
import de.bushnaq.abdalla.svg.util.RectangleWithToolTip;
import de.bushnaq.abdalla.util.ColorUtil;
import de.bushnaq.abdalla.util.GanttErrorHandler;
import de.bushnaq.abdalla.util.TaskUtil;
import de.bushnaq.abdalla.util.date.DateUtil;
import net.sf.mpxj.ProjectCalendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * abstract class used by GanttChartRenderer and TeamPlannnerRenderer
 *
 * @author abdalla.bushnaq
 */
public abstract class AbstractGanttRenderer extends AbstractRenderer {
    private static final   float                FINE_LINE_STROKE_WIDTH     = 1.0f;
    private static final   int                  LINE_HEIGHT                = 18;
    private static final   int                  RELATION_CORNER_LENGTH     = 14;
    private static final   float                RELATION_LINE_STROKE_WIDTH = 1f;
    public static final    int                  RESOURCE_NAME_TO_TASK_GAP  = 3;
    //TODO replace with sprint calendar working day
    public static final    int                  SECONDS_PER_DAY            = 85 * 6 * 60;//seconds between work start and work end, including lunch hour
    private static final   int                  TASK_BODY_BORDER           = 1;
    public static final    int                  TASK_NAME_TO_TASK_GAP      = 5 + 8;
    private final static   Font                 graphFont                  = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private final static   Font                 idErrorFont                = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    private final static   Font                 idFont                     = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    protected final static Font                 outOfOfficeFont            = new Font(Font.SANS_SERIF, Font.BOLD, 20);
    private final static   Font                 storyFont                  = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    private final static   Font                 taskInlineFont             = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private final static   Font                 taskProgressFont           = new Font(Font.SANS_SERIF, Font.PLAIN, 8);
    private final static   Font                 taskResourceLocationFont   = new Font(Font.SANS_SERIF, Font.PLAIN, 8);
    private final static   Font                 taskTickFont               = new Font(Font.SANS_SERIF, Font.PLAIN, 5);
    protected              AuthorsContribution  authorsContribution        = new AuthorsContribution();// remaining and worked per author
    protected              Context              context;
    private final          DateUtil             dateUtil                   = new DateUtil();
    protected              GanttErrorHandler    geh                        = new GanttErrorHandler();
    private final          Logger               logger                     = LoggerFactory.getLogger(this.getClass());
    private final          int                  numberOfLinesPerTask;
    protected              ResourcesUtilization resourcesUtilization       = new ResourcesUtilization();// only used for out of office
    protected              Map<Long, Integer>   taskHeight                 = new HashMap<>();

    public AbstractGanttRenderer(Context context, String sprintName/*, Map<LocalDate, String> bankHolidays*/, boolean completed/*, int chartWidth, int chartHeight*/, int numberOfLinesPerTask, int preRun, int postRun, BurnDownGraphicsTheme graphicsTheme) throws IOException {
        super(sprintName/*, bankHolidays*/, completed/*, chartWidth, chartHeight*/, preRun, postRun, graphicsTheme);
        this.context              = context;
        this.numberOfLinesPerTask = numberOfLinesPerTask;
    }

//    private int addVacation(ResourceUtilization ru, String resourceName, OffDay pce) {
//        int days = 0;

    /// /        RecurringData recurring = pce.getRecurring();
    /// /        if (recurring != null) {
    /// /            LocalDate[] dates = recurring.getDates();
    /// /            for (int i = 0; i < dates.length; i++) {
    /// /                LocalDate exceptionDate = dates[i];
    /// /                int       dayIndex      = calculateDayIndex(exceptionDate);
    /// /                if (dayIndex > -1) {
    /// /                    ru.addVacation(dayIndex, 1.0);
    /// /                    days++;
    /// /                    // logger.trace(String.format("%s is out of office on %s.", resourceName,
    /// /                    // dateUtil.createDateString(exceptionDate.getTime())));
    /// /                }
    /// /            }
    /// /        } else
//        {
//            LocalDate fromDate = pce.getFirstDay();
//            while (fromDate.isBefore(pce.getLastDay()) || fromDate.isEqual(pce.getLastDay())) {
//                //                logger.trace(String.format("%s is out of office on %s.", resourceName, dateUtil.createDateString(fromDate.getTime())));
//                int dayIndex = calculateDayIndex(fromDate);
//                if (dayIndex > -1) {
//                    ru.addVacation(dayIndex, 1.0);
//                    days++;
//                }
//                fromDate = DateUtil.addDay(fromDate, 1);
//            }
//        }
//        return days;
//    }
    private void drawConflictMarker(int y, List<Conflict> conflict) {
        if (conflict != null) {
            // only used in team planner chart
            graphics2D.setColor(Color.red);
            for (Conflict c : conflict) {
                if (c.originalConflict) {
                    int c1 = calculateX(c.range.start, c.range.start.truncatedTo(ChronoUnit.DAYS).withHour(8), SECONDS_PER_DAY) - calendarXAxes.dayOfWeek.getWidth() / 2;
                    int c2 = calculateX(c.range.end, c.range.end.truncatedTo(ChronoUnit.DAYS).withHour(8), SECONDS_PER_DAY) - calendarXAxes.dayOfWeek.getWidth() / 2;
                    graphics2D.fillRect(c1, y - getTaskHeight() / 2, c2 - c1 - 1, 2);
                    graphics2D.fillRect(c1, y + getTaskHeight() / 2 - 1, c2 - c1 - 1, 2);
                }
            }
        }
    }

    private void drawCriticalMarker(Task task, int x1, int x2, int y) {
        if (task.getCritical()) {
            graphics2D.setColor(graphicsTheme.ganttCriticalTaskBorderColor);
        } else {
            graphics2D.setColor(graphicsTheme.ganttTaskBorderColor);
        }
        ProjectCalendar pc;
        User            user = task.getAssignedUser();
        if (user != null && user.getCalendar() != null) {
            pc = user.getCalendar();
        } else {
            pc = task.getEffectiveCalendar();
        }
        int days = (int) Duration.between(task.getStart().truncatedTo(ChronoUnit.DAYS), task.getFinish().truncatedTo(ChronoUnit.DAYS)).toDays();
        for (int day = 0; day <= days; day++) {
            LocalDateTime currentDay = task.getStart().truncatedTo(ChronoUnit.DAYS).plusDays(day);
            if (pc.isWorkingDate(currentDay.toLocalDate())) {
                if (days == 0) {
                    //this is the left and right end
                    graphics2D.fillRect(x1, y - getTaskHeight() / 2 + TASK_BODY_BORDER, x2 - x1 + 1, 1);//upper -
                    graphics2D.fillRect(x1, y + getTaskHeight() / 2 - TASK_BODY_BORDER - 1, x2 - x1 + 1, 1);//lower -
                    graphics2D.fillRect(x1, y - getTaskHeight() / 2 + TASK_BODY_BORDER + 1, 1, getTaskHeight() - TASK_BODY_BORDER * 2 - 2);//start |
                    graphics2D.fillRect(x2, y - getTaskHeight() / 2 + TASK_BODY_BORDER + 1, 1, getTaskHeight() - TASK_BODY_BORDER * 2 - 2);//end |
                } else if (day == 0) {
                    //this is the left end
                    int xFinish = calculateX(currentDay.withHour(8).plusSeconds(SECONDS_PER_DAY), currentDay.withHour(8), SECONDS_PER_DAY) - calendarXAxes.dayOfWeek.getWidth() / 2;
                    graphics2D.fillRect(x1, y - getTaskHeight() / 2 + TASK_BODY_BORDER, xFinish - x1, 1);//upper -
                    graphics2D.fillRect(x1, y + getTaskHeight() / 2 - TASK_BODY_BORDER - 1, xFinish - x1, 1);//lower -
                    graphics2D.fillRect(x1, y - getTaskHeight() / 2 + TASK_BODY_BORDER + 1, 1, getTaskHeight() - TASK_BODY_BORDER * 2 - 2);//start |
                } else if (day == days) {
                    //this is the right end
                    int xStart = calculateX(currentDay.withHour(8), currentDay.withHour(8), SECONDS_PER_DAY) - calendarXAxes.dayOfWeek.getWidth() / 2;
                    graphics2D.fillRect(xStart, y - getTaskHeight() / 2 + TASK_BODY_BORDER, x2 - xStart + 1, 1);//upper -
                    graphics2D.fillRect(xStart, y + getTaskHeight() / 2 - TASK_BODY_BORDER - 1, x2 - xStart + 1, 1);//lower -
                    graphics2D.fillRect(x2, y - getTaskHeight() / 2 + TASK_BODY_BORDER + 1, 1, getTaskHeight() - TASK_BODY_BORDER * 2 - 2);//end |

                } else {
                    //this is the middle
                    int xStart = calculateX(currentDay.withHour(8), currentDay.withHour(8), SECONDS_PER_DAY) - calendarXAxes.dayOfWeek.getWidth() / 2;
                    graphics2D.fillRect(xStart, y - getTaskHeight() / 2 + TASK_BODY_BORDER, calendarXAxes.dayOfWeek.getWidth(), 1);
                    graphics2D.fillRect(xStart, y + getTaskHeight() / 2 - TASK_BODY_BORDER - 1, calendarXAxes.dayOfWeek.getWidth(), 1);
                }
            } else {
                for (int i = 0; i < calendarXAxes.dayOfWeek.getWidth() - 1; i++) {
                    int xStart = calculateX(currentDay.withHour(8), currentDay.withHour(8), SECONDS_PER_DAY) - calendarXAxes.dayOfWeek.getWidth() / 2;
                    int x      = i + xStart;
                    if (x % 4 == 0) {
                        graphics2D.fillRect(x, y - getTaskHeight() / 2 + TASK_BODY_BORDER, 2, 1);
                        graphics2D.fillRect(x, y + getTaskHeight() / 2 - TASK_BODY_BORDER - 1, 2, 1);
                    }
                }
            }
        }
    }

    private void drawId(Task task, int y) {
        int      x1        = firstDayX;
        int      x2        = x1 + calendarXAxes.dayOfWeek.getWidth();
        Color    fillColor = graphicsTheme.ganttIdColor;
        Color    textColor = graphicsTheme.ganttIdTextColor;
        MetaData md        = TaskUtil.getTaskMetaData(task);
        graphics2D.setFont(idFont);
        fillColor = graphicsTheme.ganttIdColor;
        textColor = graphicsTheme.ganttIdTextColor;
        graphics2D.setColor(fillColor);
        graphics2D.fillRect(x1 + 1, y - getTaskHeight() / 2, x2 - x1 - 1, getTaskHeight());
        {
            graphics2D.setColor(textColor);
            FontMetrics fm     = graphics2D.getFontMetrics();
            int         yShift = fm.getAscent() - fm.getHeight() / 2;

            if (md != null) {
                graphics2D.drawString(String.format("%02d", task.getId()), x1 + 4, y + yShift, md.getError(0));
            } else {
                graphics2D.drawString(String.format("%02d", task.getId()), x1 + 4, y + yShift);
            }
        }
    }

    private void drawManualMarker(Task task, int x1, int y, boolean labelInside, Color textColor) {
        if (task.getTaskMode() == TaskMode.MANUALLY_SCHEDULED) {
            graphics2D.setColor(Color.red);
            graphics2D.fillRect(x1, y - getTaskHeight() / 2, 1, getTaskHeight());
            //timestamp
//            graphics2D.setFont(graphFont);
//            graphics2D.setColor(textColor);
//            FontMetrics fm     = graphics2D.getFontMetrics();
//            int         yShift = fm.getAscent() - fm.getHeight() / 2;
//            String      label  = String.format("%s", dateUtil.createDateTimeString(task.getStart()));
//            int         width  = fm.stringWidth(label);
//            if (labelInside) {
//            } else {
//                graphics2D.drawString(label, x1 - width, y + yShift);
//            }
        }
    }

    private void drawMilestoneTask(Task task, int x1, int y, boolean labelInside, Color fillColor, Color textColor, String taskName) {
        if (task.getTaskMode() == TaskMode.MANUALLY_SCHEDULED) {
            graphics2D.setColor(Color.red);
//            graphics2D.fillRect(x1 - calendarXAxses.dayOfWeek.getWidth() / 2, y - getTaskHeight() / 2, 1, getTaskHeight());
            graphics2D.setColor(fillColor);
        } else {
            graphics2D.setColor(Color.gray);
        }
        int   milestoneWidth = getTaskHeight() / 2 - TASK_BODY_BORDER;
        int   c              = 0;
        int[] xPoints        = {x1 + c, x1 + c + milestoneWidth, x1 + c, x1 + c - milestoneWidth, x1 + c};
        int[] yPoints        = {y - milestoneWidth, y, y + milestoneWidth, y, y - milestoneWidth};
        int   nPoints        = 5;
        graphics2D.fillPolygon(xPoints, yPoints, nPoints);
        graphics2D.drawPolygon(xPoints, yPoints, nPoints);
        graphics2D.setFont(graphFont);
        graphics2D.setColor(textColor);
        FontMetrics fm     = graphics2D.getFontMetrics();
        int         yShift = fm.getAscent() - fm.getHeight() / 2;
//        graphics2D.drawString(taskName, x2 + 5 + 8, y + yShift);
        if (labelInside) {

        } else {
            LocalDateTime start          = task.getStart();
            String        dateTimeString = dateUtil.createDateTimeString(task.getStart());
            graphics2D.drawString(String.format("%s (%s)", taskName, dateTimeString), x1 + 2 + 8 + c + milestoneWidth / 2, y + yShift);
        }
    }

//    private void drawOutOfOffice(Task task, int y) {
//        //TODO reintroduce bank holidays and vacations
//
//        if (task.getAssignedUser() != null) {
//            for (ProjectCalendarException calendarException : task.getAssignedUser().getCalendar().getCalendarExceptions()) {
//                for (LocalDate currentDay = calendarException.getFromDate(); currentDay.isBefore(calendarException.getToDate()) || currentDay.isEqual(calendarException.getToDate()); currentDay = currentDay.plusDays(1)) {
////                    int    dayIndex = calculateDayIndex(calendarException.getFromDate());
//                    String name = calendarException.getName();
//                    if (name.equals(OffDayType.VACATION.name()) || name.equals(OffDayType.HOLIDAY.name()) || name.equals(OffDayType.SICK.name()) || name.equals(OffDayType.TRIP.name())) {
//                        int daysX = calculateDayX(currentDay);
//                        if (daysX > 0 && daysX < calendarXAxes.getWidth()) {
//                            graphics2D.setFont(outOfOfficeFont);
//                            graphics2D.setColor(graphicsTheme.ganttOutOfOfficeColor);
////                            graphics2D.fillRect(daysX - (calendarXAxses.dayOfWeek.getWidth() / 2 - 1), y - (calendarXAxses.dayOfWeek.getWidth() / 2) + 2, calendarXAxses.dayOfWeek.getWidth() - 1, getTaskHeight());
//                            graphics2D.setColor(Color.WHITE);
//                            FontMetrics fm        = graphics2D.getFontMetrics();
//                            String      text      = "O";
//                            int         width     = fm.stringWidth(text);
//                            int         maxAscent = fm.getMaxAscent();
////                            graphics2D.drawString(text, daysX - width / 2, y + (maxAscent + 1) / 2 - 2);
//                        }
//                    }
//                }
//            }
//        }
//
//
////        ProjectFile parentFile = task.getParentFile();
////        LocalDate   start      = parentFile.getEarliestStartDate().toLocalDate();
////        LocalDate   finish     = parentFile.getLatestFinishDate().toLocalDate();
////        String resourceName = null;
////        if (!task.getMilestone() && task.getResourceAssignments().size() == 1) {
////            for (ResourceAssignment assignment : task.getResourceAssignments()) {
////                Resource resource = assignment.getResource();
////                if (resource != null) {
////                    resourceName = resource.getName();
////                    break;
////                }
////            }
////        }
////        if (resourceName != null) {
////            ResourceUtilization ru = resourcesUtilization.get(Authors.mapToPrimaryLoginName(resourceName));
////            for (LocalDate currentDay = start; currentDay.isBefore(finish); currentDay = currentDay.plusDays(1)) {
////                int dayIndex = calculateDayIndex(currentDay);
////                if (ru != null && (ru.getVacation(dayIndex) != null || ru.getSickness(dayIndex) != null)) {
////                    int daysX = calculateDayX(currentDay);
////                    // int x1 = daysX - (calendarXAxses.dayOfWeek.width / 2 - 1);
////                    // int y1 = y - (calendarXAxses.dayOfWeek.width / 2) + 2;
////                    // graphics2D.drawImage(outOfOffice, null, x1, y1);
////                    graphics2D.setFont(outOfOfficeFont);
////                    graphics2D.setColor(graphicsTheme.ganttOutOfOfficeColor);
////                    graphics2D.fillRect(daysX - (calendarXAxses.dayOfWeek.getWidth() / 2 - 1), y - (calendarXAxses.dayOfWeek.getWidth() / 2) + 2,
////                            calendarXAxses.dayOfWeek.getWidth() - 1, getTaskHeight());
////                    graphics2D.setColor(Color.WHITE);
////                    FontMetrics fm        = graphics2D.getFontMetrics();
////                    String      text      = "O";
////                    int         width     = fm.stringWidth(text);
////                    int         maxAscent = fm.getMaxAscent();
////                    graphics2D.drawString(text, daysX - width / 2, y + (maxAscent + 1) / 2 - 2);
////                }
////            }
////        }
//    }

    /**
     * T------------| | | C
     *
     * @param sourceTask
     * @param targetTask
     * @throws Exception
     */
    private void drawRelation(Task sourceTask, int y2, Task targetTask, int y1) throws Exception {

        int signum = (int) Math.signum(y2 - y1);
        int y3;
        if (signum > 0) {
            y2 -= getTaskHeight() / 2 - TASK_BODY_BORDER;
            y3 = y2 - 5;
        } else {
            y2 += getTaskHeight() / 2 - TASK_BODY_BORDER;
            y3 = y2 + 5;
        }
        int x1 = calculateX(targetTask.getFinish(), targetTask.getFinish().truncatedTo(ChronoUnit.DAYS).withHour(8), SECONDS_PER_DAY) - calendarXAxes.dayOfWeek.getWidth() / 2;
        int x2 = RELATION_CORNER_LENGTH + calculateX(sourceTask.getStart(), sourceTask.getStart().truncatedTo(ChronoUnit.DAYS).withHour(8), SECONDS_PER_DAY) - calendarXAxes.dayOfWeek.getWidth() / 2 - RESOURCE_NAME_TO_TASK_GAP;
        graphics2D.setStroke(new BasicStroke(RELATION_LINE_STROKE_WIDTH));
        if (sourceTask.getCritical() && targetTask.getCritical()) {
            graphics2D.setColor(graphicsTheme.ganttCriticalRelationColor);

        } else {
            graphics2D.setColor(graphicsTheme.ganttRelationColor);
        }
//        graphics2D.drawLine(x1, y1, x2, y1);
        graphics2D.fillRect(x1 + 1, y1, x2 - x1, 1);// -
//        graphics2D.drawLine(x2, y1, x2, y2);
        graphics2D.fillRect(x2, y1 + 1, 1, y3 - y1);// |
        if (y2 > y1) {
            //arrow head down
            // x2-d,y2-d x2+D,y2-D
            // x2,Y2

            int   d       = 5;
            int[] xPoints = {x2 - d, x2 + d, x2};
            int[] yPoints = {y2 - d + signum, y2 - d + signum, y2 + signum};
            graphics2D.setColor(graphicsTheme.ganttRelationColor);
            graphics2D.fillPolygon(xPoints, yPoints, xPoints.length);
        } else {
            //arrow head up
            // x2,Y2
            // x2+3,y2+3 x2-3,y2+3

            int   d       = 5;
            int[] xPoints = {x2 + d, x2 - d, x2};
            int[] yPoints = {y2 + d + signum, y2 + d + signum, y2 + signum};
            graphics2D.setColor(graphicsTheme.ganttRelationColor);
            graphics2D.fillPolygon(xPoints, yPoints, xPoints.length);
        }

    }

    private void drawRibbon(Graphics2D graphics2D, int y1, int x1, int y2, int delta1, int delta2, Color ribbonColor) {
        int[] xpoints = {x1, x1 + delta1, x1 + delta1 + delta2, x1 + delta2};
        int[] ypoints = {y2, y1, y1, y2};
        graphics2D.setColor(ribbonColor);
        graphics2D.fillPolygon(xpoints, ypoints, xpoints.length);
    }

    private void drawStoryBody(Task task, int x1, int x2, int y, Color fillColor, String marker, String toolTip) {
        Color originalColor = fillColor;
        if (marker == null) {
//            drawTick(task.getStart(), x1, y, TextAlignment.left);
//            drawTick(task.getFinish(), x2, y, TextAlignment.right);
            int y1        = y + TASK_BODY_BORDER;
            int thickness = 2;
            graphics2D.fillRect(x1, y1 - getTaskHeight() / 2, x2 - x1 + 1, thickness);//upper ---
            graphics2D.fillRect(x1, y1 - getTaskHeight() / 2 + thickness, thickness, getTaskHeight() - TASK_BODY_BORDER * 2 - thickness);//left |
            graphics2D.fillRect(x1 + x2 - x1 - 1, y1 - getTaskHeight() / 2 + thickness, thickness, getTaskHeight() - TASK_BODY_BORDER * 2 - thickness);//right |
            if (x2 - x1 - 1 > 0) {
                ExtendedRectangle s = new ExtendedRectangle(x1 + 1, y1 - getTaskHeight() / 2, x2 - x1 - 1, getTaskHeight() - thickness * 2);
                s.setToolTip(toolTip);
                s.setVisible(false);
                graphics2D.fill(s);
            }
        } else {
            // int x1 = dayX1 - (calendarXAxses.dayOfWeek.width / 2 - 1);
            // int x2 = x1 + width;
            int   y1   = y - getTaskHeight() / 2 + 1;
            int   y2   = y1 + getTaskHeight() - 2;
            Shape clip = graphics2D.getClip();
            graphics2D.setClip(x1 + 1, y - getTaskHeight() / 2 + 2, x2 - x1 - 1, getTaskHeight() - 4);
            {
                // mark that we do not have a gantt chart
                int   delta1      = 25;// ribbon offset
                int   delta2      = 16;// ribbon width
                Color ribbonColor = originalColor;
                for (int x = x1 - delta2; x < x2; x += delta2) {
                    drawRibbon(graphics2D, y1, x, y2, delta1, delta2 - 1, ribbonColor);
                    if (ribbonColor == originalColor) {
                        ribbonColor = Color.white;
                    } else {
                        ribbonColor = originalColor;
                    }
                }
            }
            graphics2D.setClip(clip);
        }
    }

    public void drawTask(long gantUniqueId, Task task, boolean drawId, boolean drawRelations, boolean labelInside, boolean alien, String marker, List<Conflict> conflict, boolean drawOutOfOffice) throws Exception {
        //TODO implement this fixed
        if (GanttUtil.isValidTask(task)) {

            graphics2D.setStroke(new BasicStroke(FINE_LINE_STROKE_WIDTH));
            LocalDateTime start = task.getStart();
            LocalDateTime stop  = task.getFinish();
            int           x1    = calculateX(start, start.truncatedTo(ChronoUnit.DAYS).withHour(8), SECONDS_PER_DAY) - calendarXAxes.dayOfWeek.getWidth() / 2;
            int           x2    = calculateX(stop, stop.truncatedTo(ChronoUnit.DAYS).withHour(8), SECONDS_PER_DAY) - calendarXAxes.dayOfWeek.getWidth() / 2;
            Integer       lane  = taskHeight.get(gantUniqueId * 10000 + task.getId());
            int           y     = lane + getTaskHeight() / 2;
//            if (drawOutOfOffice) {
//                drawOutOfOffice(task, y);
//            }
            drawTask(task, x1, x2, y, labelInside, alien, marker, conflict);
            if (drawId) {
                drawId(task, y);
            }
            if (drawRelations) {
                List<Relation> predecessors = task.getPredecessors();
                if (predecessors != null && !predecessors.isEmpty()) {
                    for (Relation relation : predecessors) {
                        Task sourceTask = task;
                        Task targetTask = task.getSprint().getTaskById(relation.getPredecessorId());
                        //TODO implement this
                        if (relation.isVisible()) {
                            int y1 = taskHeight.get(gantUniqueId * 10000 + targetTask.getId()) + getTaskHeight() / 2;
                            int y2 = taskHeight.get(gantUniqueId * 10000 + sourceTask.getId()) + getTaskHeight() / 2;
                            drawRelation(sourceTask, y2, targetTask, y1);
                        }
                    }
                }
            }
        }
    }

    private void drawTask(Task task, int x1, int x2, int y, boolean labelInside, boolean alien, String marker, List<Conflict> conflict) throws Exception {
        Color  fillColor           = graphicsTheme.getAuthorColor(28);
        Color  textColor           = graphicsTheme.ganttTaskTextColor;
        Color  textInfoColor       = graphicsTheme.ganttTaskTextColor;
        String resourceName        = null;
        String resourceUtilization = null;
        Number units               = null;

        String taskName = task.getName();
        if (!task.isMilestone()) {
            if (task.getAssignedUser() != null) {
                resourceName        = task.getAssignedUser().getName();
                units               = task.getAssignedUser().getAvailabilities().getLast().getAvailability() * 100;
                resourceUtilization = String.format("%.0f%%", units);
                fillColor           = task.getAssignedUser().getColor();
            }
        }
        if (task.isMilestone() && task.getChildTasks().size() == 0) {
            // ---Milestone, but not a story (MS Project marks stories with 0 duration as milestones
            fillColor = graphicsTheme.ganttMilestoneColor;
            textColor = graphicsTheme.ganttMilestoneTextColor;
        } else if (task.getChildTasks().size() != 0) {
            // ---Story
            fillColor = graphicsTheme.ganttStoryColor;
            textColor = graphicsTheme.ganttStoryTextColor;
        } else if (resourceName != null && units != null) {
            //task
            textColor = graphicsTheme.ganttTaskTextColor;
//            int authorIndex = authorsContribution.getSortedKeyList().indexOf(resourceName);
//            if (authorIndex != -1) {
//                fillColor = authors.getNameMap().get(resourceName).getColor();
//            }
            fillColor = generateGanttColor(task.getAssignedUser().getColor());
        }
        if (task.isMilestone() && task.getChildTasks().isEmpty()) {
            // milestone
            drawMilestoneTask(task, x1, y, labelInside, fillColor, textColor, taskName);
        } else {
            if (!task.getChildTasks().isEmpty()) {
                // story
                String tooltip = generateTaskToolTip(task, x1, x2, y, marker, resourceName, resourceUtilization);
                drawStoryBody(task, x1, x2, y, fillColor, marker, tooltip);
                graphics2D.setFont(storyFont);
                graphics2D.setColor(textColor);
                FontMetrics fm     = graphics2D.getFontMetrics();
                int         yShift = fm.getAscent() - fm.getHeight() / 2;
                if (labelInside) {
                    // only used for team planner chart
                } else {
                    graphics2D.drawString(taskName, x2 + 2 + 8, y + yShift);
                }
            } else {
                //task
                {
                    String tooltip = generateTaskToolTip(task, x1, x2, y, marker, resourceName, resourceUtilization);
                    if (task.getProgress() == null) {
                        drawTaskBody(task, x1, x2, y, fillColor, alien, 0.0f, tooltip);
                    } else {
                        drawTaskBody(task, x1, x2, y, fillColor, alien, task.getProgress().doubleValue(), tooltip);
                    }
                }
                drawConflictMarker(y, conflict);
                drawCriticalMarker(task, x1, x2, y);
                drawManualMarker(task, x1, y, labelInside, textColor);

                // task text
                if (labelInside) {
                    graphics2D.setColor(textColor);
                    graphics2D.setFont(taskInlineFont);
                    FontMetrics fm     = graphics2D.getFontMetrics();
                    int         yShift = fm.getAscent() - fm.getHeight() / 2;
                    Shape       clip   = graphics2D.getClip();
                    graphics2D.setClip(x1 + 1, y - getTaskHeight() / 2 + RESOURCE_NAME_TO_TASK_GAP, x2 - x1 - 1 - 2, getTaskHeight() - 6);
                    if (marker != null) {
                        graphics2D.drawString(marker, x1 + 1, y - getTaskHeight() / 2 + (LINE_HEIGHT) / 2 + yShift);
                    }
                    graphics2D.drawString(task.getName(), x1 + 1, y - getTaskHeight() / 2 + (LINE_HEIGHT) * RESOURCE_NAME_TO_TASK_GAP / 2 + yShift + 1);
                    if (resourceName != null) {
                        int stringWidth = fm.stringWidth(resourceName);
                        graphics2D.drawString(resourceName + resourceUtilization, x1 - stringWidth, y + (LINE_HEIGHT) * 5 / 2 + yShift);
                    }
                    graphics2D.setClip(clip);
                } else {
                    // progress text
                    if (task.getProgress() != null) {
                        Color blendedColor = ColorUtil.calculateColorBlending(fillColor, Color.white);
                        if (task.getProgress().doubleValue() > 0.5) {
                            blendedColor = ColorUtil.calculateColorBlending(fillColor, blendedColor);// we are drawing
                            // two times
                        }
                        Color highestContrast = ColorUtil.heighestContrast(blendedColor);
                        graphics2D.setColor(highestContrast);
                        graphics2D.setFont(taskProgressFont);
                        String text  = String.format("%2.0f%%", task.getProgress().doubleValue() * 100);
                        int    width = graphics2D.getFontMetrics().stringWidth(text);
                        // will the process text fit into the task?
                        if (width < x2 - x1) {
                            FontMetrics fm     = graphics2D.getFontMetrics();
                            int         ascent = fm.getAscent();
                            Shape       clip   = graphics2D.getClip();
                            graphics2D.setClip(x1 + 1, y - getTaskHeight() / 2 + RESOURCE_NAME_TO_TASK_GAP, x2 - x1 - 1 - 2, getTaskHeight() - 6);
                            graphics2D.drawString(text, x1 + (x2 - x1) / 2 + 1 - width / 2, y + (ascent - 2) / 2);
                            graphics2D.setClip(clip);
                        }
                    }

                    {
                        //Task name
                        graphics2D.setColor(textColor);
                        graphics2D.setFont(graphFont);
                        FontMetrics fm     = graphics2D.getFontMetrics();
                        int         yShift = fm.getAscent() - fm.getHeight() / 2;
                        graphics2D.drawString(task.getKey() + " " + taskName, x2 + TASK_NAME_TO_TASK_GAP, y + yShift);
                    }
                    if (resourceName != null) {
                        //resource name+info
                        graphics2D.setColor(textColor);
                        graphics2D.setFont(graphFont);
                        FontMetrics fm                = graphics2D.getFontMetrics();
                        int         yShift            = fm.getAscent() - fm.getHeight() / 2;
                        int         resourceNameWidth = fm.stringWidth(resourceName);
                        int         resourceNameX     = x1 - resourceNameWidth - RESOURCE_NAME_TO_TASK_GAP;
                        {
//                            graphics2D.setColor(textInfoColor);
//                            graphics2D.setFont(taskResourceLocationFont);
//                            FontMetrics fm2   = graphics2D.getFontMetrics();
//                            int         infoY = y + getTaskHeight() / 2 - TASK_BODY_BORDER;
//                            graphics2D.drawString(resourceUtilization, resourceNameX, infoY);

//                            String location      = task.getAssignedUser().getLocations().getLast().getCountry() + "/" + task.getAssignedUser().getLocations().getLast().getState();
//                            int    locationWidth = fm2.stringWidth(location);
//                            int    locationX     = x1 - locationWidth - RESOURCE_NAME_TO_TASK_GAP;
//                            graphics2D.drawString(location, locationX, infoY);
                            String tooltip = generateTaskNameToolTop(resourceName, resourceUtilization, task.getAssignedUser().getLocations().getLast().getCountry(), task.getAssignedUser().getLocations().getLast().getState());
                            graphics2D.drawString(resourceName, resourceNameX, y + yShift, tooltip);
                        }
                    }
                }
            }
        }
    }

    private void drawTaskBody(Task task, int x1, int x2, int y, Color fillColor, boolean alien, double progress, String toolTip) {
        Color originalColor = fillColor;
        if (!alien) {
            int y1 = y - getTaskHeight() / 2 + TASK_BODY_BORDER;
            int h  = getTaskHeight() - TASK_BODY_BORDER * 2;
            if (x2 - x1 - 1 - 1 > 0) {
                //sometimes tasks are so small, that we cannot draw them.
//                drawTick(task.getStart(), x1, y, TextAlignment.left);
//                drawTick(task.getFinish(), x2, y, TextAlignment.right);
                ProjectCalendar pc;
                User            user = task.getAssignedUser();
                if (user != null && user.getCalendar() != null) {
                    pc = user.getCalendar();
                } else {
                    pc = task.getEffectiveCalendar();
                }
                int days = (int) Duration.between(task.getStart().truncatedTo(ChronoUnit.DAYS), task.getFinish().truncatedTo(ChronoUnit.DAYS)).toDays();
                for (int day = 0; day <= days; day++) {
                    LocalDateTime currentDay = task.getStart().truncatedTo(ChronoUnit.DAYS).plusDays(day);
                    Shape         s;
                    if (pc.isWorkingDate(currentDay.toLocalDate())) {
                        graphics2D.setColor(fillColor);
                        if (days == 0) {
                            //this is the left and right end
                            s = new RectangleWithToolTip(x1, y1, x2 - x1, h, toolTip);
                        } else if (day == 0) {
                            //this is the left end
                            int xFinish = calculateX(currentDay.withHour(8).plusSeconds(SECONDS_PER_DAY), currentDay.withHour(8), SECONDS_PER_DAY) - calendarXAxes.dayOfWeek.getWidth() / 2;
                            s = new RectangleWithToolTip(x1, y1, xFinish - x1, h, toolTip);
                        } else if (day == days) {
                            //this is the right end
                            int xStart = calculateX(currentDay.withHour(8), currentDay.withHour(8), SECONDS_PER_DAY) - calendarXAxes.dayOfWeek.getWidth() / 2;
                            s = new RectangleWithToolTip(xStart, y1, x2 - xStart + 1, h, toolTip);
                        } else {
                            //this is the middle
                            int xStart = calculateX(currentDay.withHour(8), currentDay.withHour(8), SECONDS_PER_DAY) - calendarXAxes.dayOfWeek.getWidth() / 2;
                            s = new RectangleWithToolTip(xStart, y1, calendarXAxes.dayOfWeek.getWidth(), h, toolTip);
                        }
                    } else {
                        graphics2D.setColor(new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), 64));//very transparent
                        int xStart = calculateX(currentDay.withHour(8), currentDay.withHour(8), SECONDS_PER_DAY) - calendarXAxes.dayOfWeek.getWidth() / 2;
                        s = new RectangleWithToolTip(xStart, y1, calendarXAxes.dayOfWeek.getWidth(), h, toolTip);
                    }
                    graphics2D.fill(s);
                }

                //progress
                if (progress > 0.0 && numberOfLinesPerTask == 1) {
//                    graphics2D.fillRect(x1 + 1, y1 + 4, (int) ((x2 - x1) * progress - 1), h - 8);
                    //- progress color must be a little bit darker than the task body.
                    Color color = org.apache.xmlgraphics.java2d.color.ColorUtil.lightenColor(task.getAssignedUser().getColor(), 0.6f);
                    graphics2D.setColor(color);
                    Shape s = new RectangleWithToolTip(x1 + 1, y1 + 2, (int) ((x2 - x1) * progress - 1), h - 4, toolTip);
                    graphics2D.fill(s);
                    if (progress < 1.0) {
                        graphics2D.setColor(Color.black);
                        graphics2D.fillRect(x1 + (int) ((x2 - x1) * progress - 1), y - getTaskHeight() / 2 + 2, 1, getTaskHeight() - 4);
                    }
                }
            }

        } else {
            int   y1   = y - getTaskHeight() / 2 + 1;
            int   y2   = y1 + getTaskHeight() - 1;
            Shape clip = graphics2D.getClip();
            graphics2D.setClip(x1, y - getTaskHeight() / 2 + 2, x2 - x1 - 1, getTaskHeight() - 4);
            {
                // mark that we do not have a gantt chart
                int   delta1      = 25;// ribbon offset
                int   delta2      = 16;// ribbon width
                Color ribbonColor = originalColor;
                for (int x = x1 - delta2; x < x2; x += delta2) {
                    drawRibbon(graphics2D, y1, x, y2, delta1, delta2 - 1, ribbonColor);
                    if (ribbonColor == originalColor) {
                        ribbonColor = Color.white;
                    } else {
                        ribbonColor = originalColor;
                    }
                }
            }
            graphics2D.setClip(clip);
        }
    }

    private void drawTick(LocalDateTime time, int x, int y, TextAlignment alignment) {
        DateTimeFormatter formatter  = DateTimeFormatter.ofPattern("HH:mm");
        String            timeString = time.format(formatter);
        graphics2D.setColor(graphicsTheme.ganttTaskTickLineColor);
        graphics2D.fillRect(x, y - getTaskHeight() / 2 + TASK_BODY_BORDER - 2, 1, 2);
        graphics2D.setFont(taskTickFont);
        graphics2D.setColor(graphicsTheme.ganttTaskTickTextColor);
        FontMetrics fm    = graphics2D.getFontMetrics();
        int         width = fm.stringWidth(timeString);
        int         testX;
        switch (alignment) {
            case left:
                testX = x - width;
                break;
            default:
                testX = x;
                break;
        }
        graphics2D.drawString(timeString, testX, y - getTaskHeight() / 2 + TASK_BODY_BORDER - 2);
    }

    private Color generateGanttColor(Color color) {
        color = org.apache.xmlgraphics.java2d.color.ColorUtil.lightenColor(color, 0.5f);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), 128);
    }

    private String generateTaskNameToolTop(String resourceName, String resourceUtilization, String country, String state) {
        String toolTip = "";

        toolTip += String.format("%s<br>", resourceName);
        toolTip += String.format("Availability <b>%s</b><br>", resourceUtilization);
        toolTip += String.format("Country <b>%s</b><br>", country);
        toolTip += String.format("State <b>%s</b><br>", state);
        return toolTip;
    }

    private String generateTaskToolTip(Task task, int x1, int x2, int y, String marker, String resourceName, String resourceUtelization) throws Exception {
        String start    = DateUtil.createDateString(task.getStart(), dateUtil.dtfymdhms);
        String finish   = DateUtil.createDateString(task.getFinish(), dateUtil.dtfymdhms);
        String progress = null;
        if (task.getProgress() != null) {
            progress = String.format("%2.0f%%", task.getProgress().doubleValue() * 100);

        }
        String duration = DateUtil.createDurationString(task.getDuration(), true, true, true);
        String toolTip  = "";
        if (marker != null) {
            toolTip += String.format("%s<br>", marker);
        } else {
        }
        if (resourceName != null) {
            String primaryResourceName = resourceName;
            toolTip += String.format("<b>%s</b><br>", task.getName(), primaryResourceName + resourceUtelization, start, finish, task.getNotes());
        }
        if (task.getChildTasks().isEmpty() || resourceName != null) {
            String primaryResourceName = resourceName;
            toolTip += String.format("<b>Resource</b> %s<br>", primaryResourceName + resourceUtelization);
        }
        toolTip += String.format("<b>Duration</b> %s<br>", duration);
        toolTip += String.format("<b>Start</b> %s<br>", start);
        toolTip += String.format("<b>Finish</b> %s<br>", finish);
        if (task.getChildTasks().isEmpty() && progress != null) {
            toolTip += String.format("<b>Progress</b> %s<br>", progress);
        }
        if (task.getNotes() != null && !task.getNotes().isEmpty()) {
            toolTip += String.format("<b>Notes</b> %s", task.getNotes());
        }
//        toolTip += String.format("\" shape=\"rect\" coords=\"%d,%d,%d,%d\"", AbstractCanvas.transformToMapX(x1 + 1), AbstractCanvas.transformToMapY(y - getTaskHeight() / 2), AbstractCanvas.transformToMapX(x2 - 1), AbstractCanvas.transformToMapY(y + getTaskHeight() / 2));
        return toolTip;
    }

    @Override
    protected int getTaskHeight() {
        return LINE_HEIGHT * numberOfLinesPerTask;
    }

//    protected void initOutOfOffice(/*GanttInformationList gattInformationList*/) {
////TODO reintroduce bank holidays and vacations
//
////        int vacations = 0;
////        for (GanttInformation ganttInformation : gattInformationList) {
////            if (context.debug.filterGantt(ganttInformation)) {
////                logger.trace("Extracting out-of-Office information from " + ganttInformation.projectFileName);
////                for (Resource resource : ganttInformation.projectFile.getResources()) {
////                    String                                                     primaryResourcename = Authors.mapToPrimaryLoginName(resource.getName());
////                    com.ricoh.sdced.projects.dashboard.dao.ResourceUtilization ru                  = resourcesUtilization.get(primaryResourcename);
////                    if (ru == null && primaryResourcename != null) {
////                        ru = new ResourceUtilization(milestones.firstMilestone, days);
////                        ru.setMaxUnit(0.7);
////                        resourcesUtilization.put(primaryResourcename, ru);
////                    }
////                    ProjectCalendar resourceCalendar = resource.getCalendar();
////                    if (resourceCalendar != null && ru != null) {
////                        for (ProjectCalendarException pce : resourceCalendar.getCalendarExceptions()) {
////                            vacations += addVacation(ru, resource.getName(), pce);
////                        }
////                    } else if (resource.getID() == 0) {
////                        // we do not expect a calendar
////                    } else {
////                        logger.trace(resource.getName() + " has no calendar");
////                    }
////                }
////            }
////        }
////        logger.info(String.format("Included %d vacation days from %d gantt charts.", vacations, gattInformationList.size()));
//
//    }

//    protected void initOutOfOffice(TimeTracker timeTracker) {
//        if (timeTracker != null) {
//            for (TimeTrackerUser user : timeTracker.timeTrackerUserList) {
//                for (TimeTrackerPunch punch : user.racPunchList) {
//                    String                                                     resourceName        = timeTracker.userIdToUserNameMap.get(punch.userId).name;
//                    String                                                     primaryResourcename = Authors.mapToPrimaryLoginName(resourceName);
//                    com.ricoh.sdced.projects.dashboard.dao.ResourceUtilization ru                  = resourcesUtilization.get(primaryResourcename);
//                    if (ru == null && primaryResourcename != null) {
//                        ru = new ResourceUtilization(milestones.firstMilestone, days);
//                        ru.setMaxUnit(0.7);
//                        resourcesUtilization.put(primaryResourcename, ru);
//                    }
//                    if (ru != null) {
//                        int startDayIndex = calculateDayIndex(punch.in);
//                        if (startDayIndex > -1) {
//                            if (punch.projectId == 1) {
//                                switch (punch.taskId) {
//                                    case 285:// vacation
//                                        ru.addVacation(startDayIndex, 1.0);
//                                        break;
//                                    case 286:// sickness
//                                        ru.addSickness(startDayIndex, 1.0);
//                                        break;
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
}
