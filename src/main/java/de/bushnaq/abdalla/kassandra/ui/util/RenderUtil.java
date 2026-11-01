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

package de.bushnaq.abdalla.kassandra.ui.util;

import com.vaadin.flow.component.Svg;
import de.bushnaq.abdalla.kassandra.Context;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.report.GanttBurndown.LegacyGanttBurndownChartLegacy;
import de.bushnaq.abdalla.kassandra.report.burndown.LegacyBurnDownChartLegacy;
import de.bushnaq.abdalla.kassandra.report.burndown.RenderDao;
import de.bushnaq.abdalla.kassandra.report.dao.CalendarSize;
import de.bushnaq.abdalla.kassandra.report.gantt.LegacyGanttChartLegacy;
import de.bushnaq.abdalla.kassandra.report.overview.LegacySprintsOverviewChartLegacy;
import de.bushnaq.abdalla.kassandra.service.GanttBurndownChartService;
import de.bushnaq.abdalla.util.Util;
import de.bushnaq.abdalla.util.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static de.bushnaq.abdalla.kassandra.report.burndown.LegacyBurnDownRendererLegacy.Y_AXIS_WIDTH;

@Slf4j
@Service
public class RenderUtil {
    public static final String                    BURNDOWN_CHART         = "burndown-chart";
    public static final String                    GANTT_BURNDOWN_CHART   = "gantt-burndown-chart";
    public static final String                    GANTT_CHART            = "gantt-chart";
    public static final String                    SPRINTS_OVERVIEW_CHART = "sprints-overview-chart";
    final static        Logger                    logger                 = LoggerFactory.getLogger(RenderUtil.class);
    @Autowired
    private             GanttBurndownChartService service;

    public static RenderDao createBurndownRenderDao(Context context, Sprint sprint, String column, LocalDateTime now, int chartWidth, int chartHeight, String link, int firstDayX) {
        RenderDao dao = new RenderDao();
        dao.context = context;
        dao.name    = column;
//        dao.sprintName         = column + "-burn-down";
        dao.link               = link;
        dao.start              = sprint.getStart();//TODO replace with sprint.getStart()
        dao.now                = now;
        dao.end                = sprint.getEnd();//TODO replace with sprint.getEnd()
        dao.release            = sprint.getReleaseDate();
        dao.chartWidth         = chartWidth;
        dao.chartHeight        = chartHeight;
        dao.sprint             = sprint;
        dao.estimatedBestWork  = DateUtil.add(sprint.getWorked(), sprint.getRemaining());
        dao.estimatedWorstWork = null;
        dao.maxWorked          = DateUtil.add(sprint.getWorked(), sprint.getRemaining());
        dao.remaining          = sprint.getRemaining();
        dao.worklog            = sprint.getWorklogs();
        dao.worklogRemaining   = sprint.getWorklogRemaining();
        dao.cssClass           = "scheduleWithMargin";
        dao.kassandraTheme     = context.parameters.getActiveGraphicsTheme();
        dao.firstDayX          = firstDayX;
        return dao;
    }

    public static RenderDao createGanttRenderDao(Context context, Sprint sprint, String name, LocalDateTime now, int chartWidth, int chartHeight, String link, int firstDayX, CalendarSize calendarSize) {
        RenderDao dao = new RenderDao();
        dao.context = context;
        dao.name    = name;
//        dao.sprintName         = name + "-burn-down";
        dao.link               = link;
        dao.start              = sprint.getStart();//TODO replace with sprint.getStart()
        dao.now                = now;
        dao.end                = sprint.getEnd();//TODO replace with sprint.getEnd()
        dao.release            = sprint.getReleaseDate();
        dao.chartWidth         = chartWidth;
        dao.chartHeight        = chartHeight;
        dao.sprint             = sprint;
        dao.estimatedBestWork  = DateUtil.add(sprint.getWorked(), sprint.getRemaining());
        dao.estimatedWorstWork = null;
        dao.maxWorked          = DateUtil.add(sprint.getWorked(), sprint.getRemaining());
        dao.remaining          = sprint.getRemaining();
        dao.worklog            = sprint.getWorklogs();
        dao.worklogRemaining   = sprint.getWorklogRemaining();
        dao.cssClass           = "scheduleWithMargin";
        dao.preRun             = 14;
        dao.postRun            = 14;
        dao.kassandraTheme     = context.parameters.getActiveGraphicsTheme();
        dao.firstDayX          = firstDayX;
        dao.calendarSize       = calendarSize;
        return dao;
    }

    public static RenderDao createOverviewRenderDao(Context context, List<Sprint> sprintList, String name, LocalDateTime now, int numberOfLines, int chartWidth, int chartHeight, int firstDayX) {
        RenderDao dao = new RenderDao();
        dao.context = context;
        dao.name    = name;
//        dao.sprintName = column + "-burn-down";
//        dao.link = link;
//        dao.start              = sprint.getStart();
        dao.now = now;
//        dao.end                = sprint.getEnd();
//        dao.release            = sprint.getReleaseDate();
        dao.chartWidth  = chartWidth;
        dao.chartHeight = chartHeight;
//        dao.sprint             = sprint;
//        dao.estimatedBestWork  = DateUtil.add(sprint.getWorked(), sprint.getRemaining());
        dao.estimatedWorstWork = null;
//        dao.maxWorked          = DateUtil.add(sprint.getWorked(), sprint.getRemaining());
//        dao.remaining          = sprint.getRemaining();
//        dao.worklog            = sprint.getWorklogs();
//        dao.worklogRemaining   = sprint.getWorklogRemaining();
        dao.cssClass       = "scheduleWithMargin";
        dao.kassandraTheme = context.parameters.getActiveGraphicsTheme();
        dao.sprintList     = sprintList;
        dao.numberOfLines  = numberOfLines;
        dao.firstDayX      = firstDayX;
        return dao;
    }

    /**
     * Generates a BurnDown chart SVG for the given sprint and updates the provided Svg component.
     *
     * @param context the application context
     * @param sprint  the sprint for which to generate the BurnDown chart
     * @param svg     the Svg component to update with the BurnDown chart
     * @throws Exception if an error occurs during BurnDown chart generation
     */
    public void generateBurnDownChartSvg(Context context, Sprint sprint, Svg svg) throws Exception {
        List<Throwable>           exceptions = new ArrayList<>();
        RenderDao                 dao        = createBurndownRenderDao(context, sprint, "burn-down", ParameterOptions.getLocalNow(), 640, 400, "sprint-" + sprint.getId() + "/sprint.html", Y_AXIS_WIDTH);
        LegacyBurnDownChartLegacy chart      = new LegacyBurnDownChartLegacy(service, "/", dao);
        RenderUtil.renderSvg(chart, svg);
        svg.setId(BURNDOWN_CHART);
    }

    public LegacyGanttBurndownChartLegacy generateGanttBurnChartSvg(Context context, Sprint sprint, Svg svg) throws Exception {
        List<Throwable>                exceptions  = new ArrayList<>();
        RenderDao                      burndownDao = createBurndownRenderDao(context, sprint, "gant", ParameterOptions.getLocalNow(), 640, 400, "sprint-" + sprint.getId() + "/sprint.html", Y_AXIS_WIDTH);
        RenderDao                      ganttDao    = createGanttRenderDao(context, sprint, "gant", ParameterOptions.getLocalNow(), 640, 400, "sprint-" + sprint.getId() + "/sprint.html", Y_AXIS_WIDTH, CalendarSize.DAYS);
        LegacyGanttBurndownChartLegacy chart       = new LegacyGanttBurndownChartLegacy(service, "/", burndownDao, ganttDao);
//        LegacyGanttBurndownChartLegacy chart      = new LegacyGanttBurndownChartLegacy(context, "", "/", "Gantt Chart", sprint.getName() + "-gant-chart", exceptions, ParameterOptions.getLocalNow(), false, sprint, "scheduleWithMargin", context.parameters.getActiveGraphicsTheme());
        RenderUtil.renderSvg(chart, svg);
        svg.setId(GANTT_BURNDOWN_CHART);
        return chart;
    }

    /**
     * Generates a Gantt chart SVG for the given sprint and updates the provided Svg component.
     *
     * @param context the application context
     * @param sprint  the sprint for which to generate the Gantt chart
     * @param svg     the Svg component to update with the Gantt chart
     * @throws Exception if an error occurs during Gantt chart generation
     */
    public static LegacyGanttChartLegacy generateGanttChartSvg(Context context, Sprint sprint, Svg svg) throws Exception {
        List<Throwable>        exceptions = new ArrayList<>();
        RenderDao              dao        = createGanttRenderDao(context, sprint, "gant", ParameterOptions.getLocalNow(), 640, 400, "sprint-" + sprint.getId() + "/sprint.html", 0, CalendarSize.YEARS);
        LegacyGanttChartLegacy chart      = new LegacyGanttChartLegacy("/", dao);
//        LegacyGanttChartLegacy      chart      = new LegacyGanttChartLegacy(context, "", "/", "Gantt Chart", sprint.getName() + "-gant-chart", exceptions, ParameterOptions.getLocalNow(), false, sprint, "scheduleWithMargin", context.parameters.getActiveGraphicsTheme());
        RenderUtil.renderSvg(chart, svg);
        svg.setId(GANTT_CHART);
        return chart;
    }

    /**
     * Generates a LegacySprintsOverviewChartLegacy SVG for the provided sprint list and updates the given Svg component.
     * Only sprints with non-null start and end dates are included; sprints without dates are silently skipped.
     *
     * @param context the application context (must have its theme synchronised before calling from a background thread)
     * @param sprints the list of sprints to display; must contain at least one sprint with valid start/end dates
     * @param svg     the Svg component to populate with the rendered chart
     * @throws Exception if an error occurs during chart generation
     */
    public static void generateSprintsOverviewChartSvg(Context context, List<Sprint> sprints, Svg svg) throws Exception {
        LegacySprintsOverviewChartLegacy chart = new LegacySprintsOverviewChartLegacy(
                context, "", "/", "sprints-overview", null,
                ParameterOptions.getLocalNow(), sprints,
                1887, 1000, "scheduleWithMargin", context.parameters.getActiveGraphicsTheme());
        renderSvg(chart, svg);
        svg.setId(SPRINTS_OVERVIEW_CHART);
    }

    /**
     * Renders a LegacyBurnDownChartLegacy to a ByteArrayOutputStream.
     *
     * @param chart the LegacyBurnDownChartLegacy to render
     * @return ByteArrayOutputStream containing the rendered LegacyBurnDownChartLegacy
     */
    private static ByteArrayOutputStream render(LegacyBurnDownChartLegacy chart) {
        ByteArrayOutputStream o = new ByteArrayOutputStream(64 * 1024); //begin size 64 KB
        try {
            chart.render(Util.generateCopyrightString(ParameterOptions.getLocalNow()), o);
            return o;
        } catch (Exception e) {
            try {
                o.close(); // Close the stream in case of error
            } catch (Exception closeException) {
                logger.warn("Failed to close output stream", closeException);
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Renders a Gantt chart to a ByteArrayOutputStream.
     *
     * @param chart the LegacyGanttChartLegacy to render
     * @return ByteArrayOutputStream containing the rendered Gantt chart
     */
    private static ByteArrayOutputStream render(LegacyGanttChartLegacy chart) {
        ByteArrayOutputStream o = new ByteArrayOutputStream(64 * 1024); //begin size 64 KB
        try {
            chart.render(Util.generateCopyrightString(ParameterOptions.getLocalNow()), o);
            return o;
        } catch (Exception e) {
            try {
                o.close(); // Close the stream in case of error
            } catch (Exception closeException) {
                logger.warn("Failed to close output stream", closeException);
            }
            throw new RuntimeException(e);
        }
    }

    private static ByteArrayOutputStream render(LegacyGanttBurndownChartLegacy chart) {
        ByteArrayOutputStream o = new ByteArrayOutputStream(64 * 1024); //begin size 64 KB
        try {
            chart.render(Util.generateCopyrightString(ParameterOptions.getLocalNow()), o);
            return o;
        } catch (Exception e) {
            try {
                o.close(); // Close the stream in case of error
            } catch (Exception closeException) {
                logger.warn("Failed to close output stream", closeException);
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Renders a LegacySprintsOverviewChartLegacy to a ByteArrayOutputStream.
     *
     * @param chart the LegacySprintsOverviewChartLegacy to render
     * @return ByteArrayOutputStream containing the rendered chart
     */
    private static ByteArrayOutputStream render(LegacySprintsOverviewChartLegacy chart) {
        ByteArrayOutputStream o = new ByteArrayOutputStream(64 * 1024);
        try {
            chart.render(Util.generateCopyrightString(ParameterOptions.getLocalNow()), o);
            return o;
        } catch (Exception e) {
            try {
                o.close();
            } catch (Exception closeException) {
                logger.warn("Failed to close output stream", closeException);
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Renders a LegacyBurnDownChartLegacy to a Svg component.
     *
     * @param chart the LegacyBurnDownChartLegacy to render
     * @param svg   the Svg component to update or create
     */
    private static void renderSvg(LegacyBurnDownChartLegacy chart, Svg svg) {
        try (ByteArrayOutputStream outputStream = render(chart)) {
            String svgString = outputStream.toString(StandardCharsets.UTF_8);
            // Update existing Svg with new content
            svg.setSvg(svgString);
        } catch (Exception e) {
            logger.error("Error creating Burn-Down chart", e);
        }
    }

    /**
     * Renders a Gantt chart to a Svg component.
     *
     * @param chart the LegacyGanttChartLegacy to render
     * @param svg   the Svg component to update
     */
    private static void renderSvg(LegacyGanttChartLegacy chart, Svg svg) {
        try (ByteArrayOutputStream outputStream = render(chart)) {
            String svgString = outputStream.toString(StandardCharsets.UTF_8);
            // Update existing Svg with new content
            svg.setSvg(svgString);
        } catch (Exception e) {
            logger.error("Error creating gantt chart", e);
        }
    }

    private static void renderSvg(LegacyGanttBurndownChartLegacy chart, Svg svg) {
        log.info("Rendering gantt chart {}.", chart.footerElement.getKey());
        try (ByteArrayOutputStream outputStream = render(chart)) {
            String svgString = outputStream.toString(StandardCharsets.UTF_8);
            // Update existing Svg with new content
            svg.setSvg(svgString);
        } catch (Exception e) {
            logger.error("Error creating gantt chart", e);
        }
    }

    /**
     * Renders a LegacySprintsOverviewChartLegacy to a Svg component.
     *
     * @param chart the LegacySprintsOverviewChartLegacy to render
     * @param svg   the Svg component to update
     */
    private static void renderSvg(LegacySprintsOverviewChartLegacy chart, Svg svg) {
        try (ByteArrayOutputStream outputStream = render(chart)) {
            String svgString = outputStream.toString(StandardCharsets.UTF_8);
            svg.setSvg(svgString);
        } catch (Exception e) {
            logger.error("Error creating sprints overview chart", e);
        }
    }

}
