package de.bushnaq.abdalla.kassandra.report.overview;


import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dao.WarnException;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Status;
import de.bushnaq.abdalla.kassandra.report.AbstractRenderer;
import de.bushnaq.abdalla.kassandra.report.burndown.RenderDao;
import de.bushnaq.abdalla.svg.util.ExtendedGraphics2D;
import de.bushnaq.abdalla.svg.util.ExtendedRectangle;
import de.bushnaq.abdalla.util.StringUtils;
import de.bushnaq.abdalla.util.date.DateUtil;

import java.awt.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

/**
 * draws timeline and boxes reprosenting porject timeline.
 * Can automatically create horizontal swimlanes for parallel running projectss
 *
 * @author abdalla.bushnaq
 */
public class SprintsOverviewRenderer extends AbstractRenderer {
    private static final Color CLOSING_COLOR            = new Color(0x0, 0x0, 0x0, 0x20);
    private static final Color CREATING_QUOTATION_COLOR = new Color(0xff, 0x0, 0x0, 0x50);
    private static final Color DEFAULT_COLOR            = new Color(0x0, 0x0, 0x0, 0xa0);
    private static final Color EXECUTING_COLOR          = new Color(0x1f, 0x8f, 0xff, 0x50);
    private static final int   LINE_HEIGHT              = 13;
    private static final Color TESTING_COLOR            = new Color(0x00, 0xff, 0xaf, 0x50);
    private static final Color WAITING_FOR_ORDER_COLOR  = new Color(0xff, 0x0, 0xff, 0x50);

    //    private BufferedImage burndownGreen = null;
    //    private BufferedImage burndownOff = null;
    //    private BufferedImage burndownRed = null;
    //    private BufferedImage burnupGreen = null;
    //    private BufferedImage burnupOff = null;
    //    private BufferedImage burnupRed = null;
    private       LocalDate                  firstDate;
    //    private BufferedImage ganttGreen = null;
    //    private BufferedImage ganttOff = null;
    //    private BufferedImage ganttRed = null;
//    private       Theme                      graphicsTheme;
    private       LocalDate                  lastDate;
    private       int                        numberOfLines       = 4;
    private final Map<Integer, List<Sprint>> projectScheduleList = new TreeMap<>();
    //    private BufferedImage qcdGreen = null;
    //    private BufferedImage qcdOff = null;
    //    private BufferedImage qcdRed = null;
    private       List<Sprint>               sprintList;

    public SprintsOverviewRenderer() {

    }

    public SprintsOverviewRenderer(RenderDao dao) throws Exception {
//    public SprintsOverviewRenderer(Context context, String column, String sprintName, Integer limit, LocalDateTime now, int numberOfLines, List<Sprint> sprintList, int chartWidth, int chartHeight, String cssClass, Theme graphicsTheme) throws Exception {
        super(dao);
//        super(sprintName + "-projectOverviewChart", false, 7, 14, graphicsTheme);
        //        this.context = context;
        this.numberOfLines = dao.numberOfLines;
        this.sprintList    = dao.sprintList;
//        this.graphicsTheme = dao.graphicsTheme;
        firstDate = findFirstDate();
        lastDate  = findLastDate();
        // ---first day should not be earlier than 6 month before now
        if (dao.limit != null) {
            LocalDate cutDay = DateUtil.addDay(dao.now.toLocalDate(), -(dao.limit));
            if (cutDay.isBefore(firstDate)) {
                firstDate = cutDay;
            }
        }

        //        ganttRed = ImageIO.read(getClass().getResource("image/gantt-red.png"));
        //        ganttGreen = ImageIO.read(getClass().getResource("image/gantt-green.png"));
        //        ganttOff = ImageIO.read(getClass().getResource("image/gantt-off.png"));
        //        burndownRed = ImageIO.read(getClass().getResource("image/burndown-red.png"));
        //        burndownGreen = ImageIO.read(getClass().getResource("image/burndown-green.png"));
        //        burndownOff = ImageIO.read(getClass().getResource("image/burndown-off.png"));
        //        burnupRed = ImageIO.read(getClass().getResource("image/burnup-red.png"));
        //        burnupGreen = ImageIO.read(getClass().getResource("image/burnup-green.png"));
        //        burnupOff = ImageIO.read(getClass().getResource("image/burnup-off.png"));
        //        qcdRed = ImageIO.read(getClass().getResource("image/qcd-red.png"));
        //        qcdGreen = ImageIO.read(getClass().getResource("image/qcd-green.png"));
        //        qcdOff = ImageIO.read(getClass().getResource("image/qcd-off.png"));

        //milestones to define start and end of chart
        milestones.add(dao.now.toLocalDate(), "N", "Now (current date)", Color.blue, true);
        int timeFrameMonths = 4;
        if (numberOfLines == 1) {
            timeFrameMonths = 2;
        }
        {
            LocalDate c = dao.now.toLocalDate().withDayOfMonth(1).minusMonths(timeFrameMonths);
            milestones.add(DateUtil.max(firstDate, c), "S", "Start (Start of project)", Color.blue, true);
        }
        {
            LocalDate c = dao.now.toLocalDate().withDayOfMonth(1).minusDays(1).plusMonths(timeFrameMonths + 1);
            milestones.add(DateUtil.min(lastDate, c), "E", "End (End of project)", Color.blue, true);
        }

        milestones.calculate();
        init();
    }

    <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<>(c);
        java.util.Collections.sort(list);
        return list;
    }

    @Override
    public int calculateChartHeight() {
        return calendarXAxes.getHeight() + projectScheduleList.size() * (getTaskHeight() + 2);
    }

    @Override
    protected int calculateChartWidth() {
        return calendarXAxes.dayOfWeek.getWidth() * days;
    }

    @Override
    protected void calculateDayWidth() {
        super.calculateDayWidth();
        calendarXAxes.dayOfWeek.setWidth(8);
    }

    private String createToolTip(int y1, Sprint sprint) {
        String toolTip;
        String delay = "";
        if (hasDelay(sprint)) {
            delay = "<br>(<font color=red>delay</font>)";
        }
        if (sprint.hasValidGanttChart()) {
            toolTip = String.format("<font face=arial><b>%s</b><br>(<font color=blue>%s</font>)%s ", sprint.getName(), sprint.getStatus().name(), delay);
        } else {
            toolTip = String.format("<font face=arial><b>%s</b><br>(<font color=blue>%s</font>)%s<br><font color=red>Gantt chart missing</font> ",
                    sprint.getName(), sprint.getStatus().name(), delay);
        }
        return toolTip;
    }

    @Override
    public void draw(ExtendedGraphics2D graphics2D, int x, int y) throws IOException {
        this.graphics2D = graphics2D;
        initPosition(x, y);
        drawCalendar(true);
        drawGraph(graphics2D);
        drawMilestones();
    }

    private void drawGraph(ExtendedGraphics2D graphics2D) {
        int y = 0;
        for (int laneId : asSortedList(projectScheduleList.keySet())) {
            y = laneId * (getTaskHeight() + 2);
            int y1 = diagram.y + y;
            for (Sprint sprint : projectScheduleList.get(laneId)) {
                Color     fillColor = getStatusColor(sprint.getStatus());
                LocalDate start     = null;
                LocalDate end       = null;

                start = sprint.getStart().toLocalDate();
                end   = sprint.getEnd().toLocalDate();
                if (start != null && end != null) {
                    if (end.isBefore(start)) {
                        end = null;
                        sprint.exceptions.add(new WarnException(
                                String.format("Project [%s][%s] does not have valid request dates.", sprint.getKey(), sprint.getName())));
                    } else {
                        int   dayX1 = calculateDayX(start);
                        int   dayX2 = calculateDayX(end);
                        int   width = dayX2 - dayX1 + calendarXAxes.dayOfWeek.getWidth() - 1;
                        int   keyWidth;
                        int   x1    = dayX1 - (calendarXAxes.dayOfWeek.getWidth() / 2 - 1);
                        Shape clip  = graphics2D.getClip();
                        // graphics2D.setClip(x1, y1, width, TASK_HEIGHT);
                        //                        if (numberOfLines > 1) {
                        //                            // mark that we do not have a gantt chart
                        //                            String text;
                        //                            int ix1 = x1;
                        //                            int iy1 = y1 + LINE_HEIGHT * (numberOfLines) + 1;
                        //                            int ix2 = ix1 + 16;
                        //                            int iy2 = iy1 + 16;
                        //                            if (!sprint.hasValidGanttChart()) {
                        //                                if (sprint.ganttChartExpected()) {
                        //                                    graphics2D.drawImage(ganttRed, null, x1, y1 + LINE_HEIGHT * (numberOfLines) + 1);
                        //                                    text = "Gantt chart is missing";
                        //                                } else {
                        //                                    graphics2D.drawImage(ganttOff, null, x1, y1 + LINE_HEIGHT * (numberOfLines) + 1);
                        //                                    text = "Gantt chart is missing";
                        //                                }
                        //                            } else {
                        //                                graphics2D.drawImage(ganttGreen, null, x1, y1 + LINE_HEIGHT * (numberOfLines) + 1);
                        //                                text = "Gantt chart exists";
                        //                            }
                        //                            imageMap += String.format("<area alt=\"<font face=arial><b>%s</b><br>\" shape=\"rect\" coords=\"%d,%d,%d,%d\" >\n", text,
                        //                                    Canvas.transformToMapX(ix1), Canvas.transformToMapY(iy1), Canvas.transformToMapX(ix2), Canvas.transformToMapY(iy2));
                        //                            ix1 += (16 + 8);
                        //                            ix2 = ix1 + 16;
                        //
                        //                            if (!sprint.hasValidBurnDownChart()) {
                        //                                if (sprint.ganttChartExpected()) {
                        //                                    graphics2D.drawImage(burndownRed, null, ix1, y1 + LINE_HEIGHT * (numberOfLines) + 1);
                        //                                    text = "Sprint is missing";
                        //                                } else {
                        //                                    graphics2D.drawImage(burndownOff, null, ix1, y1 + LINE_HEIGHT * (numberOfLines) + 1);
                        //                                    text = "Sprint is missing";
                        //                                }
                        //                            } else {
                        //                                graphics2D.drawImage(burndownGreen, null, ix1, y1 + LINE_HEIGHT * (numberOfLines) + 1);
                        //                                text = "Sprint exists";
                        //                            }
                        //                            imageMap += String.format("<area alt=\"<font face=arial><b>%s</b><br>\" shape=\"rect\" coords=\"%d,%d,%d,%d\" >\n", text,
                        //                                    Canvas.transformToMapX(ix1), Canvas.transformToMapY(iy1), Canvas.transformToMapX(ix2), Canvas.transformToMapY(iy2));
                        //                            ix1 += 16 + 8;
                        //                            ix2 = ix1 + 16;
                        //                            if (!sprint.hasValidCostChart()) {
                        //                                if (sprint.ganttChartExpected()) {
                        //                                    graphics2D.drawImage(burnupRed, null, ix1, y1 + LINE_HEIGHT * (numberOfLines) + 1);
                        //                                    text = "TimeTracker data missing";
                        //                                } else {
                        //                                    graphics2D.drawImage(burnupOff, null, ix1, y1 + LINE_HEIGHT * (numberOfLines) + 1);
                        //                                    text = "TimeTracker data missing";
                        //                                }
                        //                            } else {
                        //                                graphics2D.drawImage(burnupGreen, null, ix1, y1 + LINE_HEIGHT * (numberOfLines) + 1);
                        //                                text = "TimeTracker data exists";
                        //                            }
                        //                            imageMap += String.format("<area alt=\"<font face=arial><b>%s</b><br>\" shape=\"rect\" coords=\"%d,%d,%d,%d\" >\n", text,
                        //                                    Canvas.transformToMapX(ix1), Canvas.transformToMapY(iy1), Canvas.transformToMapX(ix2), Canvas.transformToMapY(iy2));
                        //                            ix1 += 16 + 8;
                        //                            ix2 = ix1 + 16;
                        //                            if (!sprint.hasValidQcd()) {
                        //                                if (sprint.ganttChartExpected()) {
                        //                                    graphics2D.drawImage(qcdRed, null, ix1, y1 + LINE_HEIGHT * (numberOfLines) + 1);
                        //                                    text = "Cost plan is missing";
                        //                                } else {
                        //                                    graphics2D.drawImage(qcdOff, null, ix1, y1 + LINE_HEIGHT * (numberOfLines) + 1);
                        //                                    text = "Cost plan is missing";
                        //                                }
                        //                            } else {
                        //                                graphics2D.drawImage(qcdGreen, null, ix1, y1 + LINE_HEIGHT * (numberOfLines) + 1);
                        //                                text = "Cost plan exists";
                        //                            }
                        //                            imageMap += String.format("<area alt=\"<font face=arial><b>%s</b><br>\" shape=\"rect\" coords=\"%d,%d,%d,%d\" >\n", text,
                        //                                    Canvas.transformToMapX(ix1), Canvas.transformToMapY(iy1), Canvas.transformToMapX(ix2), Canvas.transformToMapY(iy2));
                        //                        }
                        graphics2D.setColor(fillColor);
                        //                        graphics2D.fillRect(x1, y1, width, LINE_HEIGHT * (numberOfLines));
                        {
                            String toolTip = createToolTip(y1, sprint);
                            String link    = null;
//TODO reintroduce?
//                            if (sprint.burnDownSprintList.size() != 0) {
//                                link = sprint.burnDownSprintList.get(0).getJiraName() + "/" + sprint.getKey() + "/quality-board.html";
//                            } else {
//                                link = sprint.getJiraName() + "/" + sprint.getKey() + "/quality-board.html";
//                            }
                            Shape s = new ExtendedRectangle(x1, y1, width, LINE_HEIGHT * (numberOfLines), toolTip, link);
                            graphics2D.fill(s);
                        }
                        //                        graphics2D.setColor(Color.BLUE);
                        graphics2D.setColor(fillColor);
                        //                        graphics2D.drawRect(x1, y1, width - 1, LINE_HEIGHT * (numberOfLines) - 1);

                        // if (false)
                        {
                            // int textX = x1+2;
                            graphics2D.setFont(new Font("Arial", Font.BOLD, 12));
                            FontMetrics fm = graphics2D.getFontMetrics();
                            // draw title
                            {
                                String toolTip = createToolTip(y1, sprint);
                                String link    = null;
//TODO reintroduce?
//                                if (sprint.burnDownSprintList.size() != 0) {
//                                    link = sprint.burnDownSprintList.get(0).getJiraName() + "/" + sprint.getKey() + "/quality-board.html";
//                                } else {
//                                    link = sprint.getJiraName() + "/" + sprint.getKey() + "/quality-board.html";
//                                }
                                if (numberOfLines > 1) {
                                    graphics2D.setClip(x1, y1, width, getTaskHeight() - LINE_HEIGHT);
                                } else {
                                    graphics2D.setClip(x1, y1, width, getTaskHeight());
                                }
                                graphics2D.setColor(Color.BLACK);
                                List<String> wrap = StringUtils.wrap(sprint.getName(), fm, width);
                                limitTitle(width, fm, wrap);
                                int i = 0;
                                for (String s : wrap) {
                                    graphics2D.drawString(s, x1 + 1, y1 + (i + 1) * LINE_HEIGHT - 2, toolTip, link);
                                    i++;
                                }

                                graphics2D.setFont(new Font("Arial", Font.BOLD, 12));
                            }
                            // draw request key in the left lower corner
//TODO reintroduce?
//                            if (numberOfLines > 1) {
//                                graphics2D.setClip(x1, y1, width, getTaskHeight());
//                                {
//                                    keyWidth = fm.stringWidth(sprint.getKey());
//                                    String link = sprint.getJiraBaseUrl() + "/browse/" + sprint.getKey();
////                                    imageMap += String.format("<area shape=\"rect\" coords=\"%d,%d,%d,%d\" href=\"%s\" >\n", Canvas.transformToMapX(x1), Canvas.transformToMapY(y1 + (2) * LINE_HEIGHT), Canvas.transformToMapX(x1 + keyWidth - 1), Canvas.transformToMapY(y1 + (2 + 1) * LINE_HEIGHT), link);
//                                }
//                                {
//                                    String link = sprint.getJiraBaseUrl() + "/browse/" + sprint.getKey();
//                                    graphics2D.setColor(graphicsTheme.linkColor);
//                                    graphics2D.drawStringWithLink(sprint.getKey(), x1 + 1, y1 + (2 + 1) * LINE_HEIGHT - 2, link);
//                                }
//                                // draw status in the right lower corner
//                                {
//                                    graphics2D.setClip(x1 + keyWidth, y1, width - keyWidth, getTaskHeight());
//                                    graphics2D.setColor(Color.WHITE);
//                                    graphics2D.drawString(sprint.getRequestStatus(), x1 + width - 1 - fm.stringWidth(sprint.getRequestStatus()),
//                                            y1 + (2 + 1) * LINE_HEIGHT - 2);
//                                }
//                            }
                        }
                        graphics2D.setClip(clip);
//TODO reintroduce?
//                        if (sprint.hasValidGanttChart() || sprint.burnDownSprintList.size() != 0 || sprint.racProject != null) {
//                            String link;
//                            if (sprint.burnDownSprintList.size() != 0) {
//                                link = sprint.burnDownSprintList.get(0).getJiraName() + "/" + sprint.getKey() + "/quality-board.html";
//                            } else {
//                                link = sprint.getJiraName() + "/" + sprint.getKey() + "/quality-board.html";
//                            }
////                            imageMap += "<area alt=\"" + createToolTip(y1, sprint) + "\"" + String.format("shape=\"rect\" coords=\"%d,%d,%d,%d\" href=\"%s\" \" >\n", Canvas.transformToMapX(x1), Canvas.transformToMapY(y1), Canvas.transformToMapX(x1 + width - 1), Canvas.transformToMapY(y1 + LINE_HEIGHT * numberOfLines - 1), link);
//                        } else {
////                            imageMap += "<area alt=\"" + createToolTip(y1, sprint) + "\"" + String.format("shape=\"rect\" coords=\"%d,%d,%d,%d\" \" >\n", Canvas.transformToMapX(x1), Canvas.transformToMapY(y1), Canvas.transformToMapX(x1 + width - 1), Canvas.transformToMapY(y1 + LINE_HEIGHT * numberOfLines - 1));
//                        }
                    }
                }
            }
        }
    }

    private LocalDate findFirstDate() {
        LocalDate minDate = LocalDate.now();
        for (Sprint sprint : sprintList) {
            LocalDate currentDate = sprint.getStart().toLocalDate();
            if (currentDate != null && currentDate.isBefore(minDate)) {
                minDate = currentDate;
            }
        }
        return minDate;
    }

    private int findFirstFreeLane(Sprint o) {
        LocalDateTime oStartDate = o.getStart();
        LocalDateTime oEndDate   = o.getEnd();
        if (oStartDate != null && oEndDate != null) {
            LocalDateTime os = oStartDate;
            LocalDateTime of = oEndDate;
            // find
            for (int laneId : asSortedList(projectScheduleList.keySet())) {
                boolean overlapping = false;
                for (Sprint p : projectScheduleList.get(laneId)) {
                    LocalDateTime pStartDate = p.getStart();
                    LocalDateTime pEndDate   = p.getEnd();
                    if (pStartDate != null && pEndDate != null) {
                        // ---overlapping
                        LocalDateTime ps = pStartDate;
                        LocalDateTime pf = pEndDate;
                        overlapping = DateUtil.isOverlapping(os, of, overlapping, ps, pf);
                    }
                }
                if (!overlapping) {
                    return laneId;
                }
            }
            return projectScheduleList.size();
        }
        return -1;
    }

    private LocalDate findLastDate() {
        LocalDate maxDate = LocalDate.parse("1900-01-01");
        for (Sprint sprint : sprintList) {
            LocalDate currentDate = sprint.getEnd().toLocalDate();
            if (currentDate != null && currentDate.isAfter(maxDate)) {
                maxDate = currentDate;
            }
        }
        return maxDate;
    }

    private Color getStatusColor(Status status) {
        return switch (status) {
            case CREATED -> DEFAULT_COLOR;
            case STARTED -> EXECUTING_COLOR;
            case CLOSED -> CLOSING_COLOR;
        };
    }

    @Override
    protected int getTaskHeight() {
        if (numberOfLines > 1) {
            return LINE_HEIGHT * numberOfLines + 17;
        } else {
            return LINE_HEIGHT * numberOfLines;
        }
    }

    private boolean hasDelay(Sprint sprint) {
        switch (sprint.getStatus()) {
            case Status.STARTED:
                if (sprint.hasValidGanttChart()) {
                    if (ParameterOptions.getLocalNow().isAfter(sprint.getLatestFinishDate())) {
                        return true;
                    }
                }
//TODO reintroduce?
//                else {
//                    if (sprint.getBetaReleaseDate() != null && ParameterOptions.getLocalNow().isAfter(sprint.getBetaReleaseDate())) {
//                        return true;
//                    }
//                }
                break;
//TODO reintroduce?
//            case Status.CREATED::
//                if (sprint.getQuotationDueDate() != null && ParameterOptions.getLocalNow().isAfter(sprint.getQuotationDueDate())) {
//                    return true;
//                }
//                break;
        }
        return false;
    }

    public void init() throws Exception {
        for (Sprint sprint : sprintList) {
            if ((sprint.hasValidGanttChart() || sprint.hasValidGanttChart())) {
                if (isVisible(sprint)) {
                    int laneId = findFirstFreeLane(sprint);
                    if (laneId != -1) {
                        List<Sprint> list = projectScheduleList.get(laneId);
                        if (list == null) {
                            list = new ArrayList<>();
                            projectScheduleList.put(laneId, list);
                        }
                        list.add(sprint);
                    }
                }
            } else {
                sprint.exceptions.add(new WarnException(
                        String.format("Project [%s][%s] does not have valid request dates.", sprint.getKey(), sprint.getName())));
            }
        }
        initSize(0, 0, false);
    }

    private boolean isVisible(Sprint sprint) {
        return (sprint.getEnd() != null) && !sprint.getEnd().toLocalDate().isBefore(firstDate);
    }

    public void limitTitle(int width, FontMetrics fm, List<String> wrap) {
        if (wrap.size() > 2 || (wrap.size() > 1 && fm.stringWidth(wrap.get(1)) > width)) {
            while (wrap.size() > 2) {
                wrap.remove(2);
            }
            // introduce ... at the end of the string
            // find max string size
            String s = wrap.get(1);
            while (fm.stringWidth(s + "...") > width) {
                if (s.length() > 1) {
                    s = wrap.get(1).substring(0, s.length() - 1);
                } else {
                    //                    s = wrap.get(1).substring(0, s.length() - 2);
                    break;
                }
            }
            wrap.remove(1);
            wrap.add(s + "...");
        }
    }

}
