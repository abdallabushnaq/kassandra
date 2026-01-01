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

package de.bushnaq.abdalla.kassandra.report.burndown;

import de.bushnaq.abdalla.kassandra.Context;
import de.bushnaq.abdalla.kassandra.dao.WarnException;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.Worklog;
import de.bushnaq.abdalla.kassandra.report.AbstractRenderer;
import de.bushnaq.abdalla.kassandra.report.dao.*;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttUtil;
import de.bushnaq.abdalla.svg.util.ExtendedGraphics2D;
import de.bushnaq.abdalla.svg.util.ExtendedPolygon;
import de.bushnaq.abdalla.util.ErrorException;
import de.bushnaq.abdalla.util.date.DateUtil;
import org.apache.xmlgraphics.java2d.color.ColorUtil;

import java.awt.*;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Standard agile burn down chart, based on logged work in jira tickets of a
 * specific sprint.
 *
 * @author abdalla.bushnaq
 */
public class BurnDownRenderer extends AbstractRenderer {

    private static final String                 ERROR_106_AGNTT_START_DATE_NOT_MACTHING_SPRINT_START_DATE = "Error #106: Gantt start date %s does not match sprint start date %s. Ignoring Gantt chart guide information";
    private static final int                    ONE_WEEK                                                  = 7;
    private static final long                   ONE_WORK_MONTH                                            = 20L * 75L * 60L * 6L;
    private static final long                   ONE_WORK_WEEK                                             = 5L * 75L * 60L * 6L;
    private static final long                   SECONDS_PER_HOUR                                          = 60 * 60;
    private static final long                   SECONDS_PER_WORKING_DAY                                   = 75 * 6 * 60;
    private static final String                 WORK_OUTSIDE_ALLOWED_TIME_BOUNDARIES_OCCURRED             = "Work outside allowed time boundaries occurred";
    private static final int                    Y_AXIS_WIDTH                                              = 50;
    private static final float                  fine_LINE_STROKE_WIDTH                                    = 1f;
    private static final DateTimeFormatter      sdfymd                                                    = DateTimeFormatter.ofPattern("yyyy.MMM.dd");
    private              Context                context;
    private              Duration               eBestWork;
    private              Duration               eWorstWork;
    private              Color                  extrapolationColor;
    public               BurnDownGuide          ganttWorkWithBufferPerDayAccumulated                      = null;// max work per day, were every day has the amount of work planned at that day and all days before that
    public               BurnDownGuide          ganttWorkWithoutBufferPerDayAccumulated                   = null;// min work per day, were every day has the amount of work planned at that day and all days before that
    protected            BurnDownGraphicsTheme  graphicsTheme;
    protected            LocalDateTime          lastWorklog;
    private final        Duration               maxActualWorked;
    private              Duration               maxWorked;
    private              int                    numberOfWorkExceptions                                    = 0;
    private final        Sprint                 sprint;
    private              boolean                sprintClosed;
    protected            AuthorsContribution    usersTotalContribution                                    = new AuthorsContribution();
    private final        Map<User, DayWork[]>   usersWorkPerDayAccumulated                                = new HashMap<>();// work per author and day, were every day has the amount of work done at all previous days, first day has 0 work done
    private              List<Worklog>          worklog;
    private              List<WorklogRemaining> worklogRemaining;
    private              GraphSquare            yAxis;

    public BurnDownRenderer(RenderDao dao) throws Exception {
        super(dao);
        this.sprint = dao.sprint;
        processingInit(dao);
        // populateGuide(context, start, end, request);
        initGanttGuide(context, dao.start, dao.end);
        maxActualWorked = dao.maxWorked;
        if (isPlannedBurnDownGuideAvailable()) {
            // was the estimation in the gantt chart actually bigger than the current estimation?
            this.maxWorked = DateUtil.max(maxWorked, ganttWorkWithoutBufferPerDayAccumulated.get(0));
            this.maxWorked = DateUtil.max(maxWorked, ganttWorkWithBufferPerDayAccumulated.get(0));
        }
        if (numberOfWorkExceptions != 0) {
            sprint.exceptions.add(new WarnException(String.format("%s %d times.", WORK_OUTSIDE_ALLOWED_TIME_BOUNDARIES_OCCURRED, numberOfWorkExceptions)));
        }
    }

    public void calculateAuthorContribution(List<Worklog> worklogList, List<WorklogRemaining> worklogRemaining,
                                            AuthorsContribution authorsContribution) {
        if (worklogRemaining != null) {
            for (WorklogRemaining work : worklogRemaining) {
                AuthorContribution ac = authorsContribution.get(work.getAuthor());
                if (ac == null) {
                    ac = new AuthorContribution();
                    authorsContribution.put(work.getAuthor(), ac);
                }
                ac.addRemaining(work.getRemaining());
            }
        }
        if (worklogList != null) {
            for (Worklog work : worklogList) {
                AuthorContribution ac = authorsContribution.get(sprint.getuser(work.getAuthorId()));
                if (ac == null) {
                    ac = new AuthorContribution();
                    authorsContribution.put(sprint.getuser(work.getAuthorId()), ac);
                }
                ac.addWorked(work.getTimeSpent());
            }
        }
    }

    protected int calculateAuthorGraphHeight(Duration authorDelta, Duration authorEstimated, Duration sumEstimated) {
        long maxAuthorGraphHeight = (diagram.height * authorEstimated.getSeconds()) / sumEstimated.getSeconds();
        return (int) ((authorEstimated.minus(authorDelta).getSeconds() * maxAuthorGraphHeight) / (authorEstimated.getSeconds()));
    }

    @Override
    protected int calculateChartWidth() {
        //        logger.info(String.format("DEBUG chartType=%s, dayWidth=%d, days=%d, chartWidth=%d", getClass().getSimpleName(), calendarXAxses.dayOfWeek.getWidth(), days, calendarXAxses.dayOfWeek.getWidth() * days));
        return Y_AXIS_WIDTH + calendarXAxes.dayOfWeek.getWidth() * days + calendarXAxes.getPriRun() + calendarXAxes.getPostRun();
    }

    @Override
    protected void calculateDayWidth() {
        super.calculateDayWidth();
        if (chartWidth == 0) {
            //big chart with fixed day width
            calendarXAxes.dayOfWeek.setWidth(MAX_DAY_WIDTH);
        } else {
            calendarXAxes.dayOfWeek.setWidth((chartWidth - Y_AXIS_WIDTH) / days);
            calendarXAxes.dayOfWeek.setWidth(Math.min(calendarXAxes.dayOfWeek.getWidth(), MAX_DAY_WIDTH));
            calendarXAxes.dayOfWeek.setWidth(Math.max(calendarXAxes.dayOfWeek.getWidth(), 1));
            days = (chartWidth - Y_AXIS_WIDTH) / calendarXAxes.dayOfWeek.getWidth();
        }
    }

    protected int calculateGraphHight(Duration hight) {
        return (int) ((hight.getSeconds() * (diagram.height)) / (maxWorked.getSeconds()));
    }

    private void calculateWorkPerDay(Context context, Task task, BurnDownGuide guide) throws Exception {
        LocalDateTime start = task.getStart();
        LocalDateTime stop  = task.getFinish();
        if (!task.isMilestone() && task.getResourceId() != null) {
            if (GanttUtil.isValidTask(task) && !task.isMilestone() && task.getChildTasks().isEmpty()) {
                if (stop.isBefore(start) || stop.isEqual(start)) {
                    sprint.exceptions.add(new ErrorException(String.format("Task %s finish time has to be after start time. Ignoring task.", task.getName())));
                } else {
                    //                    Duration duration0 = task.getDuration();
//                    for (User assignment : task.getAssignedUser())
                    {
//                        Resource resource =  assignment.getResource();

//                        Number availability = assignment.getUnits();
                        Number availability = task.getAssignedUser().getAvailabilities().getLast().getAvailability();
                        if (/*resource != null &&*/ availability != null && context.debug.filterResource(task.getAssignedUser())) {
                            int  startDayIndex = calculateDayIndex(start);
                            int  stopDayIndex  = calculateDayIndex(stop);
                            long oneDay        = 75 * SECONDS_PER_HOUR / 10;
                            //                            long oneHour = SECONDS_PER_HOUR;
                            if (stopDayIndex == startDayIndex) {
                                // ---We assume that a task is worked on every day from 8:00 - 12:00, 13:00 - 16:30 (7.5h, with lunch time at 12:00)
                                // start time might not be in the morning at exactly 8:00
                                // stop time might not be in the afternoon 15:30
                                LocalDateTime lunchStartTime = DateUtil.calculateLunchStartTime(start);
                                LocalDateTime lunchStopTime  = DateUtil.calculateLunchStopTime(start);
                                if (start.isBefore(lunchStartTime) || start.isEqual(lunchStartTime)) {
                                    // if(start <= 12:00)
                                    if (stop.isBefore(lunchStartTime) || stop.isEqual(lunchStartTime)) {
                                        // if(stop <= 12:00)
                                        // (stop - start)/7.5
                                        double   fraction = (double) Duration.between(start, stop).getSeconds() / oneDay;
                                        Duration work     = Duration.ofSeconds((long) ((fraction * availability.doubleValue() * SECONDS_PER_WORKING_DAY)));
                                        guide.add(startDayIndex, work);
                                    } else if (stop.isAfter(lunchStopTime) || stop.isEqual(lunchStopTime)) {
                                        // if(stop >= 13:00)
                                        // ( stop - start -1h )/7.5
                                        double   fraction = (double) Duration.between(start, stop).minusHours(1).getSeconds() / oneDay;
                                        Duration work     = Duration.ofSeconds((long) ((fraction * availability.doubleValue() * SECONDS_PER_WORKING_DAY)));
                                        guide.add(startDayIndex, work);
                                    } else {
                                        throw new Exception(String.format("Task %s stop within lunch time", task.getName()));
                                    }
                                } else if (start.isAfter(lunchStopTime) || start.isEqual(lunchStopTime)) {
                                    // if(start >= 13:00 && stop >= 13:00)
                                    // (stop - Start))/7.5
                                    double   fraction = ((double) Duration.between(start, stop).getSeconds()) / oneDay;
                                    Duration work     = Duration.ofSeconds((long) ((fraction * availability.doubleValue() * SECONDS_PER_WORKING_DAY)));
                                    guide.add(startDayIndex, work);
                                } else {
                                    throw new Exception(String.format("Task %s stop before start", task.getName()));
                                }

                            } else {
                                // start
                                {
                                    // ---We assume that a task is worked on every day from 8:00 - 12:00, 13:00 -  16:30 (7.5h, with lunch time at 12:00)
                                    // start time might not be in the morning at exactly 8:00
                                    LocalDateTime lunchStartTime = DateUtil.calculateLunchStartTime(start);
                                    LocalDateTime lunchStopTime  = DateUtil.calculateLunchStopTime(start);
                                    if (start.isBefore(lunchStartTime) || start.isEqual(lunchStartTime)) {
                                        double   fraction = ((double) Duration.between(start.toLocalTime(), LocalTime.of(16, 30)).minusHours(1).getSeconds()) / oneDay;
                                        Duration work     = Duration.ofSeconds((long) ((fraction * availability.doubleValue() * SECONDS_PER_WORKING_DAY)));
                                        guide.add(startDayIndex, work);
                                    } else if (start.isAfter(lunchStopTime) || start.isEqual(lunchStopTime)) {
                                        double   fraction = ((double) Duration.between(start.toLocalTime(), LocalTime.of(16, 30)).minusHours(1).getSeconds()) / oneDay;
                                        Duration work     = Duration.ofSeconds((long) ((fraction * availability.doubleValue() * SECONDS_PER_WORKING_DAY)));
                                        guide.add(startDayIndex, work);
                                    } else {
                                        throw new Exception(String.format("Task %s start within lunch time", task.getName()));
                                    }
                                }
                                // end
                                {
                                    // ---We assume that a task is worked on every day from 8:00 - 12:00, 13:00 - 16:30 (7.5h, with lunch time at 12:00)
                                    // last day, stop time might not be in the afternoon 16:30
                                    LocalDateTime lunchStartTime = DateUtil.calculateLunchStartTime(stop);
                                    LocalDateTime lunchStopTime  = DateUtil.calculateLunchStopTime(stop);
                                    if (stop.isBefore(lunchStartTime) || stop.isEqual(lunchStartTime)) {
                                        // if(stop <= 12:00)
                                        // (stop - 8:00)/7.5
                                        double   fraction = ((double) Duration.between(LocalTime.of(8, 0), stop.toLocalTime()).getSeconds()) / oneDay;
                                        Duration work     = Duration.ofSeconds((long) ((fraction * availability.doubleValue() * SECONDS_PER_WORKING_DAY)));
                                        guide.add(stopDayIndex, work);
                                    } else if (stop.isAfter(lunchStopTime) || stop.isEqual(lunchStopTime)) {
                                        // if(stop >= 13:00)
                                        // (stop - 8:00 -1h)/7.5
                                        double   fraction = ((double) Duration.between(LocalTime.of(8, 0), stop.toLocalTime()).minusHours(1).getSeconds()) / oneDay;
                                        Duration work     = Duration.ofSeconds((long) ((fraction * availability.doubleValue() * SECONDS_PER_WORKING_DAY)));
                                        guide.add(stopDayIndex, work);
                                    } else {
                                        throw new Exception(String.format("Task %s stop within lunch time", task.getName()));
                                    }
                                }
                            }
                            if (startDayIndex + 1 < stopDayIndex) {
                                for (int index = startDayIndex + 1; index < stopDayIndex; index++) {
                                    LocalDate today = calculateDayFromIndex(index);
                                    if (task.getEffectiveCalendar().isWorkingDate(today))
//                                    if (isResourceWorkingDay(context, task.getAssignedUser(), today))
                                    {
                                        Duration work = Duration.ofSeconds((long) (availability.doubleValue() * SECONDS_PER_WORKING_DAY));
                                        guide.add(index, work);
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    private LocalDate calendarFromDayIndex(int dayIndex) {
        return milestones.firstMilestone.plusDays(dayIndex);
    }

    protected void createBurnDownChart() {
        graphics2D.setStroke(new BasicStroke(STANDARD_LINE_STROKE_WIDTH));

        LocalDate firstDay  = milestones.firstMilestone /*- (long) (60 * ONE_DAY_MILLIS / dayWidth)*/;
        int       firstDayX = diagram.x + calendarXAxes.dayOfWeek.getWidth() / 2;
        int       startX    = firstDayX + DateUtil.calculateDays(firstDay, milestones.get("S").time) * calendarXAxes.dayOfWeek.getWidth();

        drawAuthorLegend();
        drawLegend();
        if (maxWorked != null && !maxWorked.isZero()) {
            // y axis markings
            drawYAxes(startX, maxWorked);
            if (worklog != null && worklog.size() != 0) {
                drawBurnDown(firstDay, firstDayX, maxWorked);
            }
            if (sprintClosed) {
                drawWatermark(startX - calendarXAxes.dayOfWeek.getWidth() / 2 + 10,
                        chartHeight - calendarXAxes.getHeight() - calendarXAxes.year.getHeight() - 10, "CLOSED", graphicsTheme.watermarkColor);
            } else if (isAbandonedProject()) {
                drawWatermark(startX - calendarXAxes.dayOfWeek.getWidth() / 2 + 10,
                        chartHeight - calendarXAxes.getHeight() - calendarXAxes.year.getHeight() - 10, "ABANDONED", graphicsTheme.watermarkColor);
            }
            // Optimal burn down rate

            if (isPlannedBurnDownGuideAvailable()) {
                drawPlannedBurnDownGuide(firstDayX, ganttWorkWithoutBufferPerDayAccumulated);
                drawPlannedBurnDownGuide(firstDayX, ganttWorkWithBufferPerDayAccumulated);
            } else {
                // ---liniar guide
                drawOptimalBurnDownGuide(firstDayX, eBestWork);
            }

            if (eWorstWork != null) {
                drawOptimalBurnDownGuide(firstDayX, eWorstWork);
            }

            // release extrapolation (Velocity)
            if (milestones.get("R") != null) {
                graphics2D.setColor(extrapolationColor);
                int x1 = startX - calendarXAxes.dayOfWeek.getWidth() / 2 + 1;
                int y1 = diagram.y + diagram.height - calculateGraphHight(maxActualWorked);
                {
                    int x = firstDayX + DateUtil.calculateDays(firstDay, milestones.get("R").time) * calendarXAxes.dayOfWeek.getWidth();
                    graphics2D.drawLine(x1, y1, x, diagram.y + diagram.height);
                }
            }
        } else {
            // ---Maybe this is a test case ticket?
        }

    }

    protected void createMilestones(LocalDateTime start, LocalDateTime now, LocalDateTime end, LocalDateTime firstWorklog, LocalDateTime lastWorklog,
                                    LocalDateTime release, boolean completed) {
        if (start != null) {
            milestones.add(start.toLocalDate(), "S", "Start (Start of project)", Color.blue);
        }
        milestones.add(now.toLocalDate(), "N", "Now (current date)", Color.blue);
        if (end != null) {
            milestones.add(end.toLocalDate(), "E", "End (End of project)", Color.blue);
        }
        if (release != null) {
            milestones.add(release.toLocalDate(), "R", "Release (Estimated release date)", Color.blue);
        }
        if (isHideNow(now, end, completed)) {
            milestones.remove("N");
        }
        if (firstWorklog != null && (start == null || !firstWorklog.toLocalDate().isEqual(start.toLocalDate()))) {
            milestones.add(firstWorklog.toLocalDate(), "F", "First punch-in", Color.blue);
        }
        if (lastWorklog != null && (end == null || !lastWorklog.toLocalDate().isEqual(end.toLocalDate()))) {
            milestones.add(lastWorklog.toLocalDate(), "L", "Last punch-out", Color.blue);
        }
        milestones.calculate();
        // milestones.print();//debugging code
    }

    @Override
    public void draw(ExtendedGraphics2D graphics2D, int x, int y) throws IOException {
        this.graphics2D = graphics2D;
        initPosition(Y_AXIS_WIDTH + x, y);
        drawCalendar(true);
        drawMilestones();
        createBurnDownChart();
    }

    protected void drawAuthorLegend() {
        int authorLegendWidth = 20;
        for (User author : authors.getList()) {
            FontMetrics metrics = graphics2D.getFontMetrics(calendarXAxes.milestone.font);
            // get the advance of my text in this font
            // and render context
            int adv = metrics.stringWidth(author.getName());
            authorLegendWidth = Math.max(authorLegendWidth, adv);
        }
        drawAuthorLegend(chartWidth - 130 - authorLegendWidth - 5, diagram.y);
    }

    private void drawBorder(int x, int y, int lastX, int lastY) {
        graphics2D.setColor(graphicsTheme.burnDownBorderColor);
        graphics2D.drawLine(lastX - calendarXAxes.dayOfWeek.getWidth() / 2, lastY, x - calendarXAxes.dayOfWeek.getWidth() / 2, y);
    }

    private void drawBurnDown(LocalDate firstDay, int firstDayX, Duration estimatedWork) {
        // burn down graph
        if (context.parameters.detailed) {
            Integer[] graphHeight;
            {
                int days = DateUtil.calculateDays(milestones.firstMilestone, DateUtil.max(milestones.lastMilestone, milestones.get("N").time)) + 1;
                graphHeight = new Integer[days + 3];
                for (int i = 0; i < days + 3; i++) {
                    graphHeight[i] = 0;
                }
            }
            int authorIndex = 0;
            // int sum = 0;
            for (User user : usersTotalContribution.getSortedKeyList()) {
                AuthorContribution ac                  = usersTotalContribution.get(user);
                Duration           authorEstimatedWork = ac.worked.plus(ac.remaining);
                if (!authorEstimatedWork.isZero()) {
                    // (worked + remaining));
                    int yesterdayX  = 0;
                    int yesterdayY  = 0;
                    int yesterdayY2 = 0;
                    int lastX       = 0;
                    int lastY       = 0;
                    int lastY2      = 0;
                    int x           = 0;
                    int y           = 0;
                    int nowDayIndex = (DateUtil.calculateDays(milestones.firstMilestone, DateUtil.min(milestones.lastMilestone, milestones.get("N").time)));
                    {
                        int maxDayIndex = nowDayIndex;// Math.max(authorWorkPerDay.get(user).length, nowDayIndex);
                        // logger.info(DateUtil.createDateString(calculateDayFromIndex(maxDayIndex), sdfMMMdd));
                        for (int dayIndex = 0; dayIndex <= maxDayIndex + 2; dayIndex++) {
                            x = firstDayX + dayIndex * calendarXAxes.dayOfWeek.getWidth();
                            int                currentDayAuthorGraphHeight = 0;
                            List<List<String>> transactions                = null;
                            if (dayIndex <= maxDayIndex + 2) {
                                currentDayAuthorGraphHeight = calculateAuthorGraphHeight(usersWorkPerDayAccumulated.get(user)[dayIndex].duration,
                                        authorEstimatedWork, maxWorked);
                                y                           = diagram.y + diagram.height - graphHeight[dayIndex] - currentDayAuthorGraphHeight;
                                if (dayIndex > 0) {
                                    transactions = usersWorkPerDayAccumulated.get(user)[dayIndex - 1].transactions;
                                }
                            }
                            if (x != lastX) {
                                // ---a new day started, so we can draw the polygon of last day
                                if (yesterdayX != 0 && lastX != 0) {
//                                    User      author               = authors.getIdMap().get(user.getId());
                                    LocalDate calendarFromDayIndex = calendarFromDayIndex(dayIndex - 2);
                                    drawPolygon(yesterdayX, yesterdayY, yesterdayY2, lastX, lastY, lastY2,
                                            authorIndex == usersTotalContribution.getSortedKeyList().size() - 1, DateUtil.isWorkDay(calendarFromDayIndex),
                                            graphicsTheme.burnDownBorderColor, generateBurnDownColor(user.getColor()), transactions, user.getName());
                                }
                                yesterdayX  = lastX;
                                yesterdayY  = lastY;
                                yesterdayY2 = lastY2;
                            }
                            lastX = x;
                            lastY = y;
                            if (dayIndex <= maxDayIndex + 2) {
                                                         lastY2 = diagram.y + diagram.height - graphHeight[dayIndex];
                                graphHeight[dayIndex] += currentDayAuthorGraphHeight;
                            }
                        }
                    }
                }
                authorIndex++;
            }
        }
        // draw border
        if (!context.parameters.detailed) {
            graphics2D.setStroke(new BasicStroke(STANDARD_LINE_STROKE_WIDTH));
            int      lastX      = 0;
            int      lastY      = 0;
            Duration worked     = Duration.ZERO;
            int      yesterdayX = 0;
            int      yesterdayY = 0;
            for (Worklog work : worklog) {
                if (work.getStart().toLocalDate().isBefore(milestones.get("N").time)) {
                    worked = worked.plus(work.getTimeSpent());
                    int x = firstDayX + DateUtil.calculateDays(firstDay, DateUtil.toDayPrecision(work.getStart())) * calendarXAxes.dayOfWeek.getWidth();
                    if (x < firstDayX) {
                        x = firstDayX;
                    }
                    int y = diagram.y + diagram.height - calculateGraphHight(estimatedWork.minus(worked));
                    if (x != lastX) {
                        // ---a new day started, so we can draw the polygon of last day
                        if (yesterdayX != 0 && lastX != 0) {
                            drawBorder(lastX, lastY, yesterdayX, yesterdayY);
                        }
                        yesterdayX = lastX;
                        yesterdayY = lastY;
                    }
                    lastX = x;
                    lastY = y;
                }
            }
            // ---Draw last polygon
            drawBorder(lastX, lastY, yesterdayX, yesterdayY);
            // to now
            if (worklog.size() != 0) {
                int x = firstDayX + DateUtil.calculateDays(firstDay, milestones.get("N").time) * calendarXAxes.dayOfWeek.getWidth();
                if (x != lastX) {
                    drawBorder(lastX, lastY, x, lastY);
                }
            }
        }
    }

    protected void drawLegend() {
        drawLegend(chartWidth - 130, diagram.y, extrapolationColor);
    }

    protected void drawOptimalBurnDownGuide(int firstDayX, Duration estimatedWork) {
        LocalDate firstDay          = milestones.get("S").time;
        LocalDate lastDay           = milestones.get("E").time;
        int       workingDays       = DateUtil.calculateWorkingDaysIncluding(firstDay, lastDay);
        Duration  workPerWorkingDay = Duration.ofSeconds((long) (((double) estimatedWork.getSeconds()) / workingDays));
        Duration  work;
        graphics2D.setStroke(new BasicStroke(STANDARD_LINE_STROKE_WIDTH, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{3}, 0));
        graphics2D.setColor(graphicsTheme.optimaleGuideColor);
        int workingdays = 0;
        int y;
        int lastX       = firstDayX + calendarXAxes.dayOfWeek.getWidth() * DateUtil.calculateDays(firstDay, firstDay) - calendarXAxes.dayOfWeek.getWidth() / 2 + 1;
        int lastY       = diagram.y + diagram.height - calculateGraphHight(estimatedWork);
        for (LocalDate currentDay = firstDay; currentDay.isBefore(lastDay) || currentDay.isEqual(lastDay); currentDay = currentDay.plusDays(1)) {
            int daysX = firstDayX + calendarXAxes.dayOfWeek.getWidth() * DateUtil.calculateDays(firstDay, currentDay) + calendarXAxes.dayOfWeek.getWidth() / 2
                    - 1;
            if (currentDay.getDayOfWeek() == DayOfWeek.SATURDAY || currentDay.getDayOfWeek() == DayOfWeek.SUNDAY) {

            } else {
                workingdays++;
            }
            work = Duration.ofSeconds(estimatedWork.getSeconds() - workingdays * workPerWorkingDay.getSeconds());
            y    = diagram.y + diagram.height - calculateGraphHight(work);
            graphics2D.drawLine(lastX, lastY, daysX, y);
            lastX = daysX;
            lastY = y;
        }
    }

    /**
     * Draw the burn down guide taken from the gantt chart
     *
     * @param firstDayX
     * @param guide
     */
    private void drawPlannedBurnDownGuide(int firstDayX, BurnDownGuide guide) {
        int lastX = 0;
        int lastY = 0;
        if (isPlannedBurnDownGuideAvailable()) {
            lastY = diagram.y + diagram.height - calculateGraphHight(guide.get(0));
        } else {
            lastY = diagram.y + diagram.height - calculateGraphHight(maxWorked);
        }
        graphics2D.setStroke(new BasicStroke(STANDARD_LINE_STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{3}, 0));
        graphics2D.setStroke(new BasicStroke(STANDARD_LINE_STROKE_WIDTH, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{3}, 0));
        graphics2D.setColor(graphicsTheme.plannedGuideColor);
        for (int i = 0; i < guide.getSize(); i++) {
            Duration r = guide.get(i);
            int      x = firstDayX + i * calendarXAxes.dayOfWeek.getWidth() - calendarXAxes.dayOfWeek.getWidth() / 2 + 1;
            int      y = diagram.y + diagram.height - calculateGraphHight(r);
            if (i != 0) {
                graphics2D.drawLine(lastX, lastY, x, y);
            }
            lastX = x;
            lastY = y;
        }
    }

    /**
     * x1,y1-----------------x2,y1 | | | | | x2,y2 | | | |
     * x1,y3-----------------x2,y4
     *
     * @param x2
     * @param y2
     * @param x1
     * @param y1
     * @param y4
     * @param color
     * @param transactions
     * @param authorName
     */
    private void drawPolygon(int x1, int y1, int y3, int x2, int y2, int y4, boolean drawBorder, boolean weekday, Color borderColor, Color color,
                             List<List<String>> transactions, String authorName) {
        {
            ExtendedPolygon p = new ExtendedPolygon();
            p.setToolTip(DayWork.transactionsToTooltips(transactions, authorName));
            p.addPoint(x1 - calendarXAxes.dayOfWeek.getWidth() / 2, y1);
            if (calendarXAxes.dayOfWeek.getWidth() > 3) {
                p.addPoint(x2 - calendarXAxes.dayOfWeek.getWidth() / 2 + 1 - 1, y2);
                p.addPoint(x2 - calendarXAxes.dayOfWeek.getWidth() / 2 + 1 - 1, y4);
            } else {
                p.addPoint(x2 - calendarXAxes.dayOfWeek.getWidth() / 2 + 1, y2);
                p.addPoint(x2 - calendarXAxes.dayOfWeek.getWidth() / 2 + 1, y4);
            }
            p.addPoint(x1 - calendarXAxes.dayOfWeek.getWidth() / 2, y3);
            graphics2D.setColor(color);
            //            graphics2D.fillPolygon(p);
            graphics2D.fill(p);
        }
        if (drawBorder) {
            graphics2D.setStroke(new BasicStroke(STANDARD_LINE_STROKE_WIDTH));
            drawBorder(x1, y1, x2 - 1, y2);
        }
    }

    private void drawWatermark(int x, int y, String watermark, Color watermarkColor) {
        final Font font = new Font("Arial", Font.BOLD, 64);
        graphics2D.setFont(font);
        graphics2D.setColor(watermarkColor);
        graphics2D.drawString(watermark, x, y);
    }

    private void drawYAxes(int startX, Duration estimatedWork) {
        {
            graphics2D.setStroke(new BasicStroke(fine_LINE_STROKE_WIDTH));
            Duration mark     = Duration.ofSeconds(estimatedWork.getSeconds() / 5);
            String   markUnit = null;
            if (mark.getSeconds() > ONE_WORK_MONTH) {
                mark     = Duration.ofSeconds(ONE_WORK_MONTH);
                markUnit = "pm";
            } else if (mark.getSeconds() > ONE_WORK_WEEK) {
                mark     = Duration.ofSeconds(ONE_WORK_WEEK);
                markUnit = "pw";
            } else {
                mark     = Duration.ofSeconds(SECONDS_PER_WORKING_DAY);
                markUnit = "pd";
            }
            int lastMarkY = 99999;
//            Color hLineColor = ColorUtil.setAlpha(graphicsTheme.ganttGridColor, 32);
            for (Duration timeMark = Duration.ZERO; timeMark.getSeconds() < estimatedWork.getSeconds(); timeMark = timeMark.plus(mark)) {
                int markY = diagram.y + diagram.height - 1 - calculateGraphHight(timeMark);
                if (lastMarkY - markY > 12) {
                    graphics2D.setColor(graphicsTheme.ticksColor);
                    graphics2D.fillRect(startX - 4 - calendarXAxes.dayOfWeek.getWidth() / 2, markY, 4, 1);
                    graphics2D.setColor(graphicsTheme.ganttGridColor);
                    graphics2D.fillRect(startX - 1, markY, diagram.width - startX, 1);//grid --

                    drawGraphText(yAxis.x + yAxis.width - 5, markY, timeMark.getSeconds() / mark.getSeconds() + markUnit, graphicsTheme.tickTextColor,
                            yAxis.lableFont, TextAlignment.right);
                    lastMarkY = markY;
                }
            }
        }
    }

    private Color generateBurnDownColor(Color color) {
        color = ColorUtil.lightenColor(color, 0.75f);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), 128);
    }

    public void init() throws IOException {
        initSize(Y_AXIS_WIDTH, 0, true);
        if (milestones.get("R") != null && milestones.get("R").time.isAfter(milestones.get("E").time)) {
            extrapolationColor = graphicsTheme.delayEventColor;
        } else {
            extrapolationColor = graphicsTheme.inTimeColor;
        }

        calculateAuthorContribution(worklog, worklogRemaining, usersTotalContribution);

        Map<User, Duration> authorWorkSum = new HashMap<>();// total work done by any author
        for (User user : usersTotalContribution.getSortedKeyList()) {
            if (authorWorkSum.get(user) == null) {
                authorWorkSum.put(user, Duration.ZERO);
            }
        }
        int days = DateUtil.calculateDays(milestones.firstMilestone, milestones.lastMilestone) + 1;
        if (worklog != null) {
            int lastDayIndexWithValue = 0;// the last day any author has data
            for (Worklog work : worklog) {
                Duration  aws     = authorWorkSum.get(sprint.getuser(work.getAuthorId()));
                DayWork[] dayWork = usersWorkPerDayAccumulated.get(sprint.getuser(work.getAuthorId()));
                if (dayWork == null) {
                    // create a list of work per day for authors that have worked
                    dayWork    = new DayWork[days + 3];// first value is 0
                    dayWork[0] = new DayWork();
                    usersWorkPerDayAccumulated.put(sprint.getuser(work.getAuthorId()), dayWork);
                }
                aws = aws.plus(work.getTimeSpent());
                authorWorkSum.put(sprint.getuser(work.getAuthorId()), aws);
                int day = (DateUtil.calculateDays(milestones.firstMilestone, DateUtil.toDayPrecision(work.getStart())));
                if (day < 0) {
                    // ignore any data before first day of sprint
                    day = 0;
                }
                if (day < days) {
                    {
                        if (dayWork[day + 1] != null) {
                            dayWork[day + 1].setDuration(aws);
                            dayWork[day + 1].add(sprint, work);
                        } else {
                            dayWork[day + 1] = new DayWork(aws);// every day has the amount of work done at that day and all days before that
                            dayWork[day + 1].add(sprint, work);
                        }
                        if (!aws.isZero()) {
                            lastDayIndexWithValue = Math.max(day, lastDayIndexWithValue);
                        }
                    }
                } else {
                    numberOfWorkExceptions++;
                    logger.error(WORK_OUTSIDE_ALLOWED_TIME_BOUNDARIES_OCCURRED);
                }

            }
            milestones.add(calendarFromDayIndex(lastDayIndexWithValue + 1), "L", "last value + 1", Color.red, true);
        }
        milestones.calculate();
        int nowDayIndex = (DateUtil.calculateDays(milestones.firstMilestone, DateUtil.min(milestones.lastMilestone, milestones.get("N").time)));
        for (User user : usersTotalContribution.getSortedKeyList()) {
            Duration  last = Duration.ZERO;
            DayWork[] aw   = usersWorkPerDayAccumulated.get(user);
            if (aw == null) {
                // create a list of work per day for authors that have no work done yet and thus
                // where missed in the above use case involving worklog
                aw    = new DayWork[days + 3];
                aw[0] = new DayWork();
                usersWorkPerDayAccumulated.put(user, aw);
            }
            // fill in empty parts of the days list
            for (int i = 1; i < usersWorkPerDayAccumulated.get(user).length; i++) {
                if (i <= nowDayIndex + 2) {
                    if (usersWorkPerDayAccumulated.get(user)[i] == null
                            || last.toSeconds() > usersWorkPerDayAccumulated.get(user)[i].duration.toSeconds()) {
                        usersWorkPerDayAccumulated.get(user)[i] = new DayWork(last);
                    }
                    last = usersWorkPerDayAccumulated.get(user)[i].duration;
                } else {
                    // ignore anything after today (only makes sense in a test scenario, where we
                    // simulate a now time in the past)
                    if (usersWorkPerDayAccumulated.get(user)[i] == null) {
                        usersWorkPerDayAccumulated.get(user)[i] = new DayWork(Duration.ZERO);
                    }
                }
            }
        }

        if (usersTotalContribution.size() != 0) {
            for (User user : usersTotalContribution.getSortedKeyList()) {
                authors.add(user);
            }
        }
//        authors.calculateColors(graphicsTheme, true);
    }

    /**
     * Initialize BurnDownGuide object from gantt chart of this project to visualize
     * planned burn down rate
     *
     * @param context
     * @param sprintStartDate
     * @param sprintEndDate
     * @throws Exception
     */
    private void initGanttGuide(Context context, LocalDateTime sprintStartDate, LocalDateTime sprintEndDate)
            throws Exception {
        int startDayIndex = calculateDayIndex(sprintStartDate);
        int stopDayIndex  = calculateDayIndex(sprintEndDate);

        ganttWorkWithoutBufferPerDayAccumulated = new BurnDownGuide(context, sprintStartDate, startDayIndex, stopDayIndex);
        ganttWorkWithBufferPerDayAccumulated    = new BurnDownGuide(context, sprintStartDate, startDayIndex, stopDayIndex);
        for (Task task : sprint.getTasks()) {
            if (!task.isMilestone() && task.getChildTasks().isEmpty()) {
                //only include tasks that have an impact on the cost, as otherwise they are also not included in the sprint (e.g. delivery buffers)
                if (task.isImpactOnCost()) {
                    calculateWorkPerDay(context, task, ganttWorkWithoutBufferPerDayAccumulated);
                } else {
                    Duration d = task.getMinEstimate();
//                        if (task.getResourceAssignments().size() > 0)
                    {
//                            net.sf.mpxj.Duration d = task.getResourceAssignments().get(0).getWork();
                        logger.info(String.format("%s has %s effort, but no impact on cost", task.getName(), DateUtil.create24hDurationString(d, true, true, false)));
                    }
                }
                calculateWorkPerDay(context, task, ganttWorkWithBufferPerDayAccumulated);
            }
        }
        ganttWorkWithoutBufferPerDayAccumulated.convertToAccumulatedValues();
        ganttWorkWithBufferPerDayAccumulated.convertToAccumulatedValues();
    }

    private boolean isAbandonedProject() {
        //if the request ticket is closed, but the sprint is still open
        //        + " and status = 'Closed' or status = 'Cancelled' or status = 'Waiting for Quotation Request' or status = 'Rejected' or status = 'On Hold'";

//        return (sprintClosed != 1) && (request.getRequestStatus().equals(AbstractIssue.REQUEST_STATUS_CLOSED)
//                || request.getRequestStatus().equals(AbstractIssue.REQUEST_STATUS_CANCELED)
//                || request.getRequestStatus().equals(AbstractIssue.REQUEST_STATUS_ON_HOLD)
//                || request.getRequestStatus().equals(AbstractIssue.REQUEST_STATUS_REJECTED)
//                || request.getRequestStatus().equals(AbstractIssue.REQUEST_STATUS_WAITING_FOR_QUOTATION_REQUEST));
        return sprint.isClosed();
    }

    protected boolean isHideNow(LocalDateTime now, LocalDateTime end, boolean completed) {
        if (completed || isAbandonedProject()) {
            // We do not want to keep drawing the graph further and further to include the
            // current date, if it is closed.
            return end != null && now.isAfter(DateUtil.addDay(end, ONE_WEEK));
        }
        return false;
    }

    protected boolean isPlannedBurnDownGuideAvailable() {
        return ganttWorkWithoutBufferPerDayAccumulated != null;
    }

//    private boolean isResourceWorkingDay(Context context, User resource, LocalDate day) {
//        if (day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY) {
//            // ---ignore Saturdays and Sundays
//            return false;
//        }
//        if (resource != null) {
//            ProjectCalendar resourceCalendar = resource.getCalendar();
//            // ---ignore bank holidays
//            return (resourceCalendar == null || resourceCalendar.getException(day) == null) /*&& (context.bankHolidays.get(day) == null)*/;
//        }
//        return true;
//    }

    private void processingInit(RenderDao dao) throws IOException {
        this.context          = dao.context;
        this.worklog          = dao.worklog;
        this.lastWorklog      = dao.lastWorklog;
        this.worklogRemaining = dao.worklogRemaining;
        this.eBestWork        = dao.estimatedBestWork;
        this.eWorstWork       = dao.estimatedWorstWork;
        this.maxWorked        = dao.maxWorked;
        this.sprintClosed     = dao.sprint.isClosed();
        this.graphicsTheme    = dao.graphicsTheme;
        createMilestones(dao.start, dao.now, dao.end, dao.firstWorklog, dao.lastWorklog, dao.sprint.getReleaseDate(), dao.sprint.isClosed());

        init();
        yAxis = new GraphSquare(2, diagram.y, Y_AXIS_WIDTH, diagram.height, new Font("Arial", Font.PLAIN, 12), new Font("Arial", Font.PLAIN, 12));
    }

}
