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

package de.bushnaq.abdalla.kassandra.ui.view;

import com.vaadin.flow.component.Svg;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.bushnaq.abdalla.kassandra.Context;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttChart;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttUtil;
import de.bushnaq.abdalla.kassandra.report.html.util.HtmlUtil;
import de.bushnaq.abdalla.kassandra.rest.api.*;
import de.bushnaq.abdalla.kassandra.ui.HtmlColor;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.util.RenderUtil;
import de.bushnaq.abdalla.util.GanttErrorHandler;
import de.bushnaq.abdalla.util.date.DateUtil;
import de.bushnaq.abdalla.util.date.ReportUtil;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

// Create a utility method for generating two-part cells


@Route(value = "sprint-quality-board", layout = MainLayout.class)
@PageTitle("Sprint Quality Board")
@PermitAll // When security is enabled, allow all authenticated users
public class SprintQualityBoard extends Main implements AfterNavigationObserver {
    public static final String            SPRINT_GRID_NAME_PREFIX = "sprint-grid-name-";
    private final       Clock             clock;
    @Autowired
    protected           Context           context;
    private final       LocalDateTime     created;
    private final       GanttErrorHandler eh                      = new GanttErrorHandler();
    private final       FeatureApi        featureApi;
    private             Long              featureId;
    private             GanttUtil         ganttUtil;
    private final       HtmlUtil          htmlUtil                = new HtmlUtil();
    final               Logger            logger                  = LoggerFactory.getLogger(this.getClass());
    private final       LocalDateTime     now;
    private final       H2                pageTitle;
    private final       ProductApi        productApi;
    private             Long              productId;
    private             Sprint            sprint;
    private final       SprintApi         sprintApi;
    private             Long              sprintId;
    private             SprintStatistics  sprintStatistics;
    private final       TaskApi           taskApi;
    private final       UserApi           userApi;
    private final       VersionApi        versionApi;
    private             Long              versionId;
    private final       WorklogApi        worklogApi;

    public SprintQualityBoard(WorklogApi worklogApi, TaskApi taskApi, SprintApi sprintApi, ProductApi productApi, VersionApi versionApi, FeatureApi featureApi, UserApi userApi, Clock clock) {
        created         = LocalDateTime.now(clock);
        this.worklogApi = worklogApi;
        this.taskApi    = taskApi;
        this.sprintApi  = sprintApi;
        this.productApi = productApi;
        this.versionApi = versionApi;
        this.featureApi = featureApi;
        this.userApi    = userApi;
        this.clock      = clock;
        this.now        = ParameterOptions.getLocalNow();

        pageTitle = new H2("Sprint Quality Board");
        pageTitle.addClassNames(
                LumoUtility.Margin.Top.MEDIUM,
                LumoUtility.Margin.Bottom.SMALL
        );
        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        this.getStyle().set("padding-left", "var(--lumo-space-m)");
        this.getStyle().set("padding-right", "var(--lumo-space-m)");

    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        //- Get query parameters
        Location        location        = event.getLocation();
        QueryParameters queryParameters = location.getQueryParameters();
        if (queryParameters.getParameters().containsKey("product")) {
            this.productId = Long.parseLong(queryParameters.getParameters().get("product").getFirst());
        }
        if (queryParameters.getParameters().containsKey("version")) {
            this.versionId = Long.parseLong(queryParameters.getParameters().get("version").getFirst());
        }
        if (queryParameters.getParameters().containsKey("feature")) {
            this.featureId = Long.parseLong(queryParameters.getParameters().get("feature").getFirst());
        }
        if (queryParameters.getParameters().containsKey("sprint")) {
            this.sprintId = Long.parseLong(queryParameters.getParameters().get("sprint").getFirst());
        }

        ganttUtil = new GanttUtil(context);
        loadData();

        pageTitle.setText(sprint.getName());
        //- update breadcrumbs
        getElement().getParent().getComponent()
                .ifPresent(component -> {
                    if (component instanceof MainLayout mainLayout) {
                        mainLayout.getBreadcrumbs().clear();
                        Product product = productApi.getById(productId);
                        mainLayout.getBreadcrumbs().addItem("Products (" + product.getName() + ")", ProductListView.class);
//                        mainLayout.getBreadcrumbs().addItem("Products", ProductListView.class);
                        {
                            Map<String, String> params = new HashMap<>();
                            params.put("product", String.valueOf(productId));
                            Version version = versionApi.getById(versionId);
                            mainLayout.getBreadcrumbs().addItem("Versions (" + version.getName() + ")", VersionListView.class, params);
//                            mainLayout.getBreadcrumbs().addItem("Versions", VersionListView.class, params);
                        }
                        {
                            Map<String, String> params = new HashMap<>();
                            params.put("product", String.valueOf(productId));
                            params.put("version", String.valueOf(versionId));
                            Feature feature = featureApi.getById(featureId);
                            mainLayout.getBreadcrumbs().addItem("Features (" + feature.getName() + ")", FeatureListView.class, params);
                            mainLayout.getBreadcrumbs().addItem("Features", FeatureListView.class, params);
                        }
                        {
                            Map<String, String> params = new HashMap<>();
                            params.put("product", String.valueOf(productId));
                            params.put("version", String.valueOf(versionId));
                            params.put("feature", String.valueOf(featureId));
                            Sprint sprint = sprintApi.getById(sprintId);
                            mainLayout.getBreadcrumbs().addItem("Sprints (" + sprint.getName() + ")", SprintListView.class, params);
//                            mainLayout.getBreadcrumbs().addItem("Sprints", SprintListView.class, params);
                        }
                        {
                            Map<String, String> params = new HashMap<>();
                            params.put("product", String.valueOf(productId));
                            params.put("version", String.valueOf(versionId));
                            params.put("feature", String.valueOf(featureId));
                            params.put("sprint", String.valueOf(sprintId));
                            mainLayout.getBreadcrumbs().addItem("Tasks", TaskListView.class, params);
                        }
                    }
                });

        // Check if sprint has start date
        if (sprint.getStart() == null) {
            // Display message in the center of the page
            Div messageContainer = new Div();
            messageContainer.getStyle()
                    .set("display", "flex")
                    .set("justify-content", "center")
                    .set("align-items", "center")
                    .set("height", "50vh")
                    .set("width", "100%");

            H3 message = new H3("No Sprint Data to Show");
            message.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)");

            messageContainer.add(message);
            add(messageContainer);
            logTime();
            return;
        } else {
//        renderBurnDownChart();
            createSprintDetailsLayout();
            createGanttChart();
        }
        logTime();
    }

    private Div createFieldDisplay(String label, String value, String status) {
        Div container = new Div();
        container.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("padding", "var(--lumo-space-s)")
                .set("background", "var(--lumo-base-color)");

        // Create a wrapper div for the value part that doesn't stretch
        Div valueWrapper = new Div();
        valueWrapper.getStyle()
                .set("display", "flex") // Use flex to allow inner content to determine size
                .set("width", "auto");  // Don't stretch to full width

        // Value part (top)
        Span valueSpan = new Span(value);
        if (label != null) {
            try {
                valueSpan.setTitle(htmlUtil.getHtmlTipSnippet(label));
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            }
        }
        // Apply status-based styling if status is provided
        if (status != null) {
            valueSpan.setClassName("cell-text " + status.toLowerCase()); // Add a class for potential CSS styling
        } else {
            valueSpan.setClassName("cell-text");
        }
        valueWrapper.add(valueSpan);
        // Name part with HTML support for special characters
        Span nameSpan = new Span();
        nameSpan.getElement().setProperty("innerHTML", label);
        nameSpan.setClassName("cell-name");
        container.add(valueWrapper, nameSpan);
        return container;
    }

    // Overload for backward compatibility
    private Div createFieldDisplay(String label, String value) {
        return createFieldDisplay(label, value, null);
    }

    private void createGanttChart() {
        try {
            long       time  = System.currentTimeMillis();
            Div        div   = new Div();
            Svg        svg   = new Svg();
            GanttChart chart = RenderUtil.generateGanttChartSvg(context, sprint, svg);
            svg.getStyle()//.set("object-fit", "contain") // Maintain aspect ratio
                    .set("margin-top", "var(--lumo-space-m)");
            svg.setClassName("qtip-shadow");
            div.setWidth(chart.getChartWidth() + "px");
            div.add(svg);
            add(div);
            logger.info("Gantt chart generated in {} ms", System.currentTimeMillis() - time);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            add(new Paragraph("Error generating gantt chart: " + e.getMessage()));
        }
    }

    private void createSprintDetailsLayout() {
        final DateTimeFormatter dtfymd = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        // Create a container with CSS Grid layout
        Div gridContainer = new Div();
        gridContainer.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(4, 1fr) 2fr repeat(2, 1fr)")  // 4 columns left, 1 middle (wider), 2 right
                .set("grid-template-rows", "auto auto auto auto")  // 3 rows
                .set("gap", "var(--lumo-space-m)")  // spacing between cells
                .set("width", "100%")
                .set("height", "auto");

        Duration estimation                      = DateUtil.add(sprint.getWorked(), sprint.getRemaining());
        String   manDelayString                  = ReportUtil.calcualteManDelayString(sprint.getStart(), now, sprint.getEnd(), sprint.getWorked(), DateUtil.add(sprint.getWorked(), sprint.getRemaining()));
        double   delayFraction                   = ReportUtil.calcualteDelayFraction(sprint.getStart(), now, sprint.getEnd(), sprint.getWorked(), DateUtil.add(sprint.getWorked(), sprint.getRemaining()));
        String   status                          = HtmlColor.calculateStatusColor(delayFraction);
        Double   extrapolatedDelayFraction       = ReportUtil.calculateExtrapolatedScheduleDelayFraction(sprint.getStart(), now, sprint.getEnd(), sprint.getWorked(), DateUtil.add(sprint.getWorked(), sprint.getRemaining()));
        String   extrapolatedStatus              = HtmlColor.calculateStatusColor(extrapolatedDelayFraction);
        String   extrapolatedScheduleDelayString = ReportUtil.calculateExtrapolatedScheduleDelayString(sprint.getStart(), now, sprint.getEnd(), sprint.getWorked(), DateUtil.add(sprint.getWorked(), sprint.getRemaining()));// Delay

        // First row
        gridContainer.add(createFieldDisplay("Sprint Name", sprint.getName()));//column 1
        gridContainer.add(createFieldDisplay("Sprint Start Date", DateUtil.createDateString(sprint.getStart(), dtfymd)));//column 2
        gridContainer.add(createFieldDisplay("Total Work Days", "" + DateUtil.calculateWorkingDaysIncluding(sprint.getStart().toLocalDate(), sprint.getEnd().toLocalDate())));//column 3
        gridContainer.add(createFieldDisplay("Remaining Work Days", "" + DateUtil.calculateWorkingDaysIncluding(now, sprint.getEnd())));//column 4
        gridContainer.add(createFieldDisplay("Current Effort Delay", String.format("%s (%.0f%%)", manDelayString, 100 * delayFraction), status));//column 6
        gridContainer.add(createFieldDisplay("&Sigma; Effort Spent", DateUtil.createDurationString(sprint.getWorked(), false, true, false)));//column 7

        // Second row
        gridContainer.add(createFieldDisplay("", ""));//column 1
        gridContainer.add(createFieldDisplay("Sprint End Date", DateUtil.createDateString(sprint.getEnd(), dtfymd)));//column 2
        gridContainer.add(createFieldDisplay("Expected Progress", String.format("%.0f%%", 100 * ReportUtil.calcualteExpectedProgress(sprint.getStart(), now, sprint.getEnd(), sprint.getWorked(), sprint.getOriginalEstimation(), estimation, sprint.getRemaining()))));//column 3
        gridContainer.add(createFieldDisplay("Progress", String.format("%.0f%%", 100 * ReportUtil.calcualteProgress(sprint.getWorked(), estimation)), status));//column 4
        gridContainer.add(createFieldDisplay("Current Schedule Delay", DateUtil.createDurationString(ReportUtil.calcualteWorkDaysMiliseconsDelay(sprint.getStart(), now, sprint.getEnd(), sprint.getWorked(), sprint.getOriginalEstimation(), estimation, sprint.getRemaining()), false, true, false), status));//column 6
        gridContainer.add(createFieldDisplay("&Sigma; Effort Estimate", DateUtil.createWorkDayDurationString(DateUtil.add(sprint.getWorked(), sprint.getRemaining()), false, true, false)));//column 7


        // Third row
        gridContainer.add(createFieldDisplay("3.1", "a"));//column 1
        if (sprint.getRemaining() == null || sprint.getRemaining().equals(Duration.ZERO)) {
            gridContainer.add(createFieldDisplay("Actual Sprint Release Date", DateUtil.createDateString(sprint.getReleaseDate(), dtfymd), status));//column 2
        } else {
            gridContainer.add(createFieldDisplay("Extrapolated Sprint Release Date", DateUtil.createDateString(sprint.getReleaseDate(), dtfymd), extrapolatedStatus));//column 2
        }
        gridContainer.add(createFieldDisplay("Optimal Efficiency", ReportUtil.createPersonDayEfficiencyString(ReportUtil.calcualteOptimaleEfficiency(sprint.getStart(), sprint.getEnd(), DateUtil.add(sprint.getWorked(), sprint.getRemaining())))));//column 3
        gridContainer.add(createFieldDisplay("Efficiency", ReportUtil.createPersonDayEfficiencyString(ReportUtil.calcualteEfficiency(sprint.getStart(), now, sprint.getEnd(), sprint.getWorked(), sprint.getRemaining())), status));//column 4
        if (extrapolatedDelayFraction != null) {
            gridContainer.add(createFieldDisplay("Extrapolated Schedule Delay", String.format("%s (%.0f%%)", extrapolatedScheduleDelayString, 100 * extrapolatedDelayFraction), extrapolatedStatus));//column 6
        } else {
            gridContainer.add(createFieldDisplay("Extrapolated Schedule Delay", "NA", extrapolatedStatus));//column 6
        }
        gridContainer.add(createFieldDisplay("&Sigma; Remaining Effort Estimate", DateUtil.createDurationString(sprint.getRemaining(), false, true, false)));//column 7


        // forth row
        gridContainer.add(createFieldDisplay("4.1", "a"));//column 1
        gridContainer.add(createFieldDisplay("4.2", "b"));//column 2
        gridContainer.add(createFieldDisplay("4.3", "a"));//column 3
        gridContainer.add(createFieldDisplay("4.4", "b"));//column 4
        gridContainer.add(createFieldDisplay("4.6", "a"));//column 6
        gridContainer.add(createFieldDisplay("4.7", "b"));//column 7

        // Create the spanning column (column 5)
        Div spanningColumn = new Div();
        spanningColumn.getStyle()
                .set("grid-column", "5 / 6")  // Column 5
                .set("grid-row", "1 / 5")     // Span all 4 rows
                .set("padding", "var(--lumo-space-m)")
                .set("background-color", "var(--lumo-base-color)");

        try {
            long time = System.currentTimeMillis();
            Svg  svg  = new Svg();
            RenderUtil.generateBurnDownChartSvg(context, sprint, svg);
            svg.getStyle().set("object-fit", "contain") // Maintain aspect ratio
                    .set("margin-top", "var(--lumo-space-m)");
            svg.setClassName("qtip-shadow");
            spanningColumn.add(svg);
            logger.info("Burndown chart generated in {} ms", System.currentTimeMillis() - time);
        } catch (Exception e) {
            spanningColumn.add(new Paragraph("Error loading burndown chart: " + e.getMessage()));
        }
        gridContainer.add(spanningColumn);

        add(gridContainer);

    }

    private ComponentRenderer<Div, Sprint> createTwoPartRenderer(
            Function<Sprint, String> valueProvider,
            Function<Sprint, String> nameProvider) {

        return new ComponentRenderer<>(item -> {
            Div container = new Div();
            container.getStyle()
                    .set("display", "flex")
                    .set("flex-direction", "column");

            // Value part (top)
            Span value = new Span(valueProvider.apply(item));
            value.getStyle()
                    .set("font-weight", "normal");

            // Name part (bottom, smaller)
            Span name = new Span(nameProvider.apply(item));
            name.getStyle()
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("color", "var(--lumo-secondary-text-color)");

            container.add(value, name);
            return container;
        });
    }

    private void loadData() {
        //- populate grid with tasks of the sprint
        long time = System.currentTimeMillis();

        // Capture the security context from the current thread
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Load in parallel with security context propagation
        CompletableFuture<Sprint> sprintFuture = CompletableFuture.supplyAsync(() -> {
            // Set security context in this thread
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            try {
                Sprint s = sprintApi.getById(sprintId);
                s.initialize();
                return s;
            } finally {
                SecurityContextHolder.clearContext();// Clear the security context after execution
            }
        });

        CompletableFuture<List<User>> usersFuture = CompletableFuture.supplyAsync(() -> {
            // Set security context in this thread
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            try {
                return userApi.getAll(sprintId);
            } finally {
                SecurityContextHolder.clearContext();// Clear the security context after execution

            }
        });

        CompletableFuture<List<Task>> tasksFuture = CompletableFuture.supplyAsync(() -> {
            // Set security context in this thread
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            try {
                return taskApi.getAll(sprintId);
            } finally {
                SecurityContextHolder.clearContext();// Clear the security context after execution
            }
        });

        CompletableFuture<List<Worklog>> worklogsFuture = CompletableFuture.supplyAsync(() -> {
            // Set security context in this thread
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            try {
                return worklogApi.getAll(sprintId);
            } finally {
                SecurityContextHolder.clearContext();// Clear the security context after execution
            }
        });

        // Wait for all futures and combine results
        try {
            sprint = sprintFuture.get();
            logger.info("sprint loaded and initialized in {} ms", System.currentTimeMillis() - time);
            time = System.currentTimeMillis();
            sprint.initUserMap(usersFuture.get());
            sprint.initTaskMap(tasksFuture.get(), worklogsFuture.get());
            logger.info("sprint user, task and worklog maps initialized in {} ms", System.currentTimeMillis() - time);
            if (sprint.getStart() != null) {
                sprint.recalculate(ParameterOptions.getLocalNow());
                sprintStatistics = new SprintStatistics(sprint, now);
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error loading sprint data", e);
            // Handle exception appropriately
        }
        ganttUtil.levelResources(eh, sprint, "", ParameterOptions.getLocalNow());

    }

    private void logTime() {
        logger.info("generated page in {}", DateUtil.create24hDurationString(Duration.between(created, LocalDateTime.now()), true, true, true, false));
    }


}
