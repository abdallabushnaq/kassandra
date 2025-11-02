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
import de.bushnaq.abdalla.kassandra.report.burndown.BurnDownChart;
import de.bushnaq.abdalla.kassandra.report.burndown.RenderDao;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttChart;
import de.bushnaq.abdalla.util.Util;
import de.bushnaq.abdalla.util.date.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RenderUtil {
    public static final String BURNDOWN_CHART = "burndown-chart";
    public static final String GANTT_CHART    = "gantt-chart";
    final static        Logger logger         = LoggerFactory.getLogger(RenderUtil.class);

    private static RenderDao createRenderDao(Context context, Sprint sprint, String column, LocalDateTime now, int chartWidth, int chartHeight, String link) {
        RenderDao dao = new RenderDao();
        dao.context            = context;
        dao.column             = column;
        dao.sprintName         = column + "-burn-down";
        dao.link               = link;
        dao.start              = sprint.getStart();
        dao.now                = now;
        dao.end                = sprint.getEnd();
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
        dao.graphicsTheme      = context.parameters.graphicsTheme;
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
    public static void generateBurnDownChartSvg(Context context, Sprint sprint, Svg svg) throws Exception {
        List<Throwable> exceptions = new ArrayList<>();
        RenderDao       dao        = createRenderDao(context, sprint, "burn-down", ParameterOptions.getLocalNow(), 640, 400, "sprint-" + sprint.getId() + "/sprint.html");
        BurnDownChart   chart      = new BurnDownChart("/", dao);
        RenderUtil.renderSvg(chart, svg);
        svg.setId(BURNDOWN_CHART);
    }

    /**
     * Generates a Gantt chart SVG for the given sprint and updates the provided Svg component.
     *
     * @param context the application context
     * @param sprint  the sprint for which to generate the Gantt chart
     * @param svg     the Svg component to update with the Gantt chart
     * @throws Exception if an error occurs during Gantt chart generation
     */
    public static GanttChart generateGanttChartSvg(Context context, Sprint sprint, Svg svg) throws Exception {
        List<Throwable> exceptions = new ArrayList<>();
        GanttChart      chart      = new GanttChart(context, "", "/", "Gantt Chart", sprint.getName() + "-gant-chart", exceptions, ParameterOptions.getLocalNow(), false, sprint/*, 1887, 1000*/, "scheduleWithMargin", context.parameters.graphicsTheme);
        RenderUtil.renderSvg(chart, svg);
        svg.setId(GANTT_CHART);
        return chart;
    }

    /**
     * Renders a BurnDownChart to a ByteArrayOutputStream.
     *
     * @param chart the BurnDownChart to render
     * @return ByteArrayOutputStream containing the rendered BurnDownChart
     */
    private static ByteArrayOutputStream render(BurnDownChart chart) {
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
     * @param chart the GanttChart to render
     * @return ByteArrayOutputStream containing the rendered Gantt chart
     */
    private static ByteArrayOutputStream render(GanttChart chart) {
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
     * Renders a BurnDownChart to a Svg component.
     *
     * @param chart the BurnDownChart to render
     * @param svg   the Svg component to update or create
     */
    private static void renderSvg(BurnDownChart chart, Svg svg) {
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
     * @param chart the GanttChart to render
     * @param svg   the Svg component to update
     */
    private static void renderSvg(GanttChart chart, Svg svg) {
        try (ByteArrayOutputStream outputStream = render(chart)) {
            String svgString = outputStream.toString(StandardCharsets.UTF_8);
            // Update existing Svg with new content
            svg.setSvg(svgString);
        } catch (Exception e) {
            logger.error("Error creating gantt chart", e);
        }
    }

}
