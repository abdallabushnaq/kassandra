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

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.bushnaq.abdalla.kassandra.Context;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.config.DefaultEntitiesInitializer;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.report.GanttBurndown.GanttBurndownChart;
import de.bushnaq.abdalla.kassandra.report.burndown.RenderDao;
import de.bushnaq.abdalla.kassandra.report.dao.CalendarSize;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttChart;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttUtil;
import de.bushnaq.abdalla.kassandra.report.html.util.HtmlUtil;
import de.bushnaq.abdalla.kassandra.rest.api.*;
import de.bushnaq.abdalla.kassandra.service.SprintExportService;
import de.bushnaq.abdalla.kassandra.ui.HtmlColor;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.component.ThemeChangedEvent;
import de.bushnaq.abdalla.kassandra.ui.util.RenderUtil;
import de.bushnaq.abdalla.util.GanttErrorHandler;
import de.bushnaq.abdalla.util.Util;
import de.bushnaq.abdalla.util.date.DateUtil;
import de.bushnaq.abdalla.util.date.ReportUtil;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

// Create a utility method for generating two-part cells


@Route(value = "quality-board", layout = MainLayout.class)
@PageTitle("Quality Board")
@Menu(order = 8, icon = "vaadin:chart-3d", title = "Quality Board")
@PermitAll // When security is enabled, allow all authenticated users
@Slf4j
public class QualityBoard extends Main implements AfterNavigationObserver {
    public static final String                  MENU_ITEM_ID            = "/quality-board";
    public static final String                  SPRINT_GRID_NAME_PREFIX = "sprint-grid-name-";
    public static final String                  SPRINT_SELECTOR_ID      = "sprint-selector";
    /**
     * All non-Backlog sprints belonging to the current feature, used to populate the sprint selector.
     */
    private             List<Sprint>            allFeatureSprints       = new ArrayList<>();
    /**
     * Container for the burndown SVG (the spanning column in the stats grid).
     */
    private             Div                     burnDownContainer;
    //    /**
//     * In-flight async burndown generation; cancelled before a new run starts.
//     */
//    private             CompletableFuture<Void> burndownGenerationFuture;
    private final       Clock                   clock;
    @Autowired
    protected           Context                 context;
    private final       LocalDateTime           created;
    private final       GanttErrorHandler       eh                      = new GanttErrorHandler();
    private final       FeatureApi              featureApi;
    private             UUID                    featureId;
    /**
     * Container for the Gantt chart SVG.
     */
    private             Div                     ganttBurndownChartContainer;    /**
     * In-flight async Gantt generation; cancelled before a new run starts.
     */
    private             CompletableFuture<Void> ganttGenerationFuture;
    private             GanttUtil               ganttUtil;
    /**
     * Persistent header layout (sprint selector + page title) — survives content reloads.
     */
    private final       HorizontalLayout        header;
    /**
     * Avatar image shown next to the page title — updated to reflect the selected sprint.
     */
    private final       Image                   headerAvatar;
    private final       HtmlUtil                htmlUtil                = new HtmlUtil();
    /**
     * Guard flag: prevents {@link #updateUrlParameters()} from firing during programmatic selector restores.
     */
    private             boolean                 isRestoringFromUrl      = false;
    final               Logger                  logger                  = LoggerFactory.getLogger(this.getClass());
    private final       LocalDateTime           now;
    private final       H2                      pageTitle;
    private final       ProductApi              productApi;
    private             UUID                    productId;
    private             Sprint                  sprint;
    private final       SprintApi               sprintApi;
    private             UUID                    sprintId;
    /**
     * Sprint selector ComboBox — lets the user switch sprints without leaving the page.
     */
    private final       ComboBox<Sprint>        sprintSelector;
    private             SprintStatistics        sprintStatistics;
    @Autowired
    private             SprintExportService     sprintExportService;
    private final       TaskApi                 taskApi;
    /**
     * Registration for the {@link ThemeChangedEvent} listener; removed in {@link #onDetach}.
     */
    private             Registration            themeChangedRegistration;
    private final       UserApi                 userApi;
    private final       VersionApi              versionApi;
    private             UUID                    versionId;
    private final       WorklogApi              worklogApi;

    public QualityBoard(WorklogApi worklogApi, TaskApi taskApi, SprintApi sprintApi, ProductApi productApi, VersionApi versionApi, FeatureApi featureApi, UserApi userApi, Clock clock) {
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
        pageTitle.addClassNames(LumoUtility.Margin.NONE);

        headerAvatar = new Image();
        headerAvatar.setWidth("32px");
        headerAvatar.setHeight("32px");
        headerAvatar.getStyle()
                .set("border-radius", "var(--lumo-border-radius)")
                .set("object-fit", "cover")
                .set("display", "inline-block")
                .set("margin-right", "12px");
        headerAvatar.setVisible(false);

        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        this.getStyle().set("padding-left", "var(--lumo-space-xs)");
        this.getStyle().set("padding-right", "var(--lumo-space-xs)");

        // Sprint selector — persists across content reloads triggered by sprint switching
        sprintSelector = new ComboBox<>();
        sprintSelector.setId(SPRINT_SELECTOR_ID);
        sprintSelector.addThemeVariants(ComboBoxVariant.LUMO_SMALL);
        sprintSelector.setItemLabelGenerator(Sprint::getName);
        sprintSelector.setPlaceholder("Select sprint");
        sprintSelector.setWidth("250px");
        sprintSelector.addValueChangeListener(e -> {
            if (e.isFromClient() && e.getValue() != null) {
                sprintId = e.getValue().getId();
                reloadContent();
                updateUrlParameters();
            }
        });

        // Persistent header: avatar + page title on the left, sprint selector next to it
        header = new HorizontalLayout(headerAvatar, pageTitle, sprintSelector);
        header.setAlignItems(FlexComponent.Alignment.END);
        header.setWidthFull();
        header.getStyle().set("padding", "var(--lumo-space-xs)");
        header.setSpacing(true);
        header.setId("page-header");
        add(header);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        //- Get query parameters
        Location        location        = event.getLocation();
        QueryParameters queryParameters = location.getQueryParameters();
        log.info("=== afterNavigation called {} parameters", queryParameters.getParameters().size());
        if (queryParameters.getParameters().containsKey("product")) {
            this.productId = UUID.fromString(queryParameters.getParameters().get("product").getFirst());
        }
        if (queryParameters.getParameters().containsKey("version")) {
            this.versionId = UUID.fromString(queryParameters.getParameters().get("version").getFirst());
        }
        if (queryParameters.getParameters().containsKey("feature")) {
            this.featureId = UUID.fromString(queryParameters.getParameters().get("feature").getFirst());
        }
        if (queryParameters.getParameters().containsKey("sprint")) {
            this.sprintId = UUID.fromString(queryParameters.getParameters().get("sprint").getFirst());
        }
        // Resolve defaults when navigated directly from the menu (no URL params)
        if (productId == null) {
            productId = productApi.getAll().stream()
                    .filter(p -> !DefaultEntitiesInitializer.DEFAULT_NAME.equals(p.getName()))
                    .map(Product::getId)
                    .findFirst()
                    .orElse(null);
        }
        if (versionId == null && productId != null) {
            final UUID pid = productId;
            versionId = versionApi.getAll().stream()
                    .filter(v -> pid.equals(v.getProductId()))
                    .map(Version::getId)
                    .findFirst()
                    .orElse(null);
        }
        if (featureId == null && versionId != null) {
            final UUID vid = versionId;
            featureId = featureApi.getAll().stream()
                    .filter(f -> vid.equals(f.getVersionId()))
                    .map(Feature::getId)
                    .findFirst()
                    .orElse(null);
        }
        if (sprintId == null && featureId != null) {
            final UUID fid = featureId;
            sprintId = sprintApi.getAll().stream()
                    .filter(s -> fid.equals(s.getFeatureId())
                            && !DefaultEntitiesInitializer.BACKLOG_SPRINT_NAME.equals(s.getName()))
                    .map(Sprint::getId)
                    .findFirst()
                    .orElse(null);
        }
        if (sprintId == null) {
            logger.warn("No sprint found to display in QualityBoard");
        }

        populateSprintSelector();
        reloadContent();
        updateUrlParameters();
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

    /**
     * Creates the Gantt/burndown chart container and the download toolbar that appears below it.
     * The toolbar contains a JSON export anchor and an MSPDI XML export anchor; both use
     * {@link StreamResource} so the file is generated lazily on the first browser download
     * request rather than up front.
     */
    private void createGanttBurndownChart() {
        ganttBurndownChartContainer = new Div();
        add(ganttBurndownChartContainer);
        add(createDownloadToolbar());
    }

    /**
     * Builds the download toolbar placed below the Gantt/burndown chart.
     * Contains a "Download JSON" and a "Download XML" anchor button, each backed by a
     * {@link StreamResource} that generates the export bytes on demand.
     *
     * @return a {@link HorizontalLayout} containing the two download anchors
     */
    private HorizontalLayout createDownloadToolbar() {
        Sprint sprintSnapshot = sprint; // capture before async use

        // JSON anchor
        StreamResource jsonResource = new StreamResource(
                sprintSnapshot.getName() + ".json",
                () -> {
                    try {
                        return new ByteArrayInputStream(sprintExportService.exportToJson(sprintSnapshot));
                    } catch (Exception e) {
                        log.error("Error generating JSON export for sprint {}", sprintSnapshot.getName(), e);
                        return new ByteArrayInputStream(new byte[0]);
                    }
                });
        jsonResource.setContentType("application/json");
        Anchor jsonAnchor = new Anchor(jsonResource, "");
        jsonAnchor.getElement().setAttribute("download", true);
        Button jsonButton = new Button("JSON", new Icon(VaadinIcon.DOWNLOAD));
        jsonButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        jsonButton.getElement().setAttribute("title", "Download sprint data as JSON");
        jsonAnchor.add(jsonButton);

        // MSPDI XML anchor
        StreamResource xmlResource = new StreamResource(
                sprintSnapshot.getName() + ".xml",
                () -> {
                    try {
                        return new ByteArrayInputStream(sprintExportService.exportToMspdiXml(sprintSnapshot));
                    } catch (Exception e) {
                        log.error("Error generating XML export for sprint {}", sprintSnapshot.getName(), e);
                        return new ByteArrayInputStream(new byte[0]);
                    }
                });
        xmlResource.setContentType("application/xml");
        Anchor xmlAnchor = new Anchor(xmlResource, "");
        xmlAnchor.getElement().setAttribute("download", true);
        Button xmlButton = new Button("XML", new Icon(VaadinIcon.DOWNLOAD));
        xmlButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        xmlButton.getElement().setAttribute("title", "Download sprint data as Microsoft Project XML (MSPDI)");
        xmlAnchor.add(xmlButton);

        HorizontalLayout toolbar = new HorizontalLayout(jsonAnchor, xmlAnchor);
        toolbar.getStyle()
                .set("margin-top", "var(--lumo-space-s)")
                .set("padding-left", "var(--lumo-space-xs)");
        toolbar.setSpacing(true);
        return toolbar;
    }

    private void createSprintDetailsLayout() {
        final DateTimeFormatter dtfymd = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        // Create a container with CSS Grid layout
        Div gridContainer = new Div();
        gridContainer.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(4, 1fr) repeat(2, 1fr)")  // 4 columns left, 2 right
                .set("grid-template-rows", "auto auto auto auto")  // 4 rows
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
        gridContainer.add(createFieldDisplay("Feature", sprint.getFeature().getName()));//column 1
        gridContainer.add(createFieldDisplay("Sprint End Date", DateUtil.createDateString(sprint.getEnd(), dtfymd)));//column 2
        gridContainer.add(createFieldDisplay("Expected Progress", String.format("%.0f%%", 100 * ReportUtil.calcualteExpectedProgress(sprint.getStart(), now, sprint.getEnd(), sprint.getWorked(), sprint.getOriginalEstimation(), estimation, sprint.getRemaining()))));//column 3
        gridContainer.add(createFieldDisplay("Progress", String.format("%.0f%%", 100 * ReportUtil.calcualteProgress(sprint.getWorked(), estimation)), status));//column 4
        gridContainer.add(createFieldDisplay("Current Schedule Delay", DateUtil.createDurationString(ReportUtil.calcualteWorkDaysMiliseconsDelay(sprint.getStart(), now, sprint.getEnd(), sprint.getWorked(), sprint.getOriginalEstimation(), estimation, sprint.getRemaining()), false, true, false), status));//column 6
        gridContainer.add(createFieldDisplay("&Sigma; Effort Estimate", DateUtil.createWorkDayDurationString(DateUtil.add(sprint.getWorked(), sprint.getRemaining()), false, true, false)));//column 7


        // Third row
        gridContainer.add(createFieldDisplay("Version", sprint.getFeature().getVersion().getName()));//column 1
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

    /**
     * Generates the Gantt chart SVG asynchronously, then updates {@link #ganttBurndownChartContainer}
     * on the UI thread via {@link UI#access(com.vaadin.flow.server.Command)}.
     * Cancels any in-flight previous generation before starting a new one.
     */
    private void generateGanttBurndownChartAsync() {
        if (sprint == null) {
            return;
        }
        if (ganttGenerationFuture != null && !ganttGenerationFuture.isDone()) {
            ganttGenerationFuture.cancel(true);
        }
        ganttBurndownChartContainer.removeAll();

        UI             ui             = UI.getCurrent();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Sprint         sprintSnapshot = sprint;
        log.info("created sprint snapshot for chart generation");

        ganttGenerationFuture = CompletableFuture.supplyAsync(() -> {
            SecurityContext ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(authentication);
            SecurityContextHolder.setContext(ctx);
            try {
                Svg                svg   = new Svg();
                GanttBurndownChart chart = RenderUtil.generateGanttBurnChartSvg(context, sprintSnapshot, svg);
                return new Object[]{svg, chart};
            } catch (Exception e) {
                throw new RuntimeException("Error generating Gantt chart", e);
            } finally {
                SecurityContextHolder.clearContext();
            }
        }).thenAccept(result -> {
            Svg                svg   = (Svg) result[0];
            GanttBurndownChart chart = (GanttBurndownChart) result[1];
            ui.access(() -> {
                ganttBurndownChartContainer.removeAll();
                svg.getStyle().set("margin-top", "var(--lumo-space-m)");
                svg.setClassName("qtip-shadow");
                ganttBurndownChartContainer.setWidth(chart.getChartWidth() + "px");
                ganttBurndownChartContainer.add(svg);
                ui.push();
            });
        }).exceptionally(ex -> {
            logger.error("Error generating Gantt chart", ex);
            ui.access(() -> {
                ganttBurndownChartContainer.removeAll();
                ganttBurndownChartContainer.add(new Paragraph("Error generating gantt chart: " + ex.getMessage()));
                ui.push();
            });
            return null;
        });
    }

    private void loadData() {
        if (sprintId == null) {
            sprint = null;
            return;
        }
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
            sprint.setFeature(featureApi.getById(sprint.getFeatureId()));
            sprint.getFeature().setVersion(versionApi.getById(sprint.getFeature().getVersionId()));
            sprint.getFeature().getVersion().setProduct(productApi.getById(sprint.getFeature().getVersion().getProductId()));
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
        {
            try {
                RenderDao  dao   = RenderUtil.createGanttRenderDao(context, sprint, sprint.getName(), ParameterOptions.getLocalNow(), 640, 400, "sprint-" + sprint.getId() + "/sprint.html", 0, CalendarSize.YEARS);
                GanttChart chart = new GanttChart("/", dao);
                chart.render(Util.generateCopyrightString(ParameterOptions.getLocalNow()), sprint.getName(), "references/debug");
            } catch (Exception e) {
                logger.error("Error generating gantt chart", e);

            }

        }

    }

    private void logTime() {
        logger.info("generated page in {}", DateUtil.create24hDurationString(Duration.between(created, LocalDateTime.now()), true, true, true, false));
    }

    /**
     * Subscribes to {@link ThemeChangedEvent} so the Gantt and burndown charts are re-generated
     * in the new theme whenever the user toggles the theme.
     *
     * @param attachEvent the attach event
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        themeChangedRegistration = ComponentUtil.addListener(attachEvent.getUI(), ThemeChangedEvent.class, e -> refreshCharts());
    }

    /**
     * Removes the {@link ThemeChangedEvent} subscription to prevent memory leaks.
     *
     * @param detachEvent the detach event
     */
    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (themeChangedRegistration != null) {
            themeChangedRegistration.remove();
            themeChangedRegistration = null;
        }
        super.onDetach(detachEvent);
    }

    /**
     * Populates the sprint selector with all non-Backlog sprints that belong to the current
     * feature, then selects the sprint identified by {@link #sprintId}.
     * The {@link #isRestoringFromUrl} guard prevents the value-change listener from echoing
     * a spurious {@link #updateUrlParameters()} call during programmatic selection.
     */
    private void populateSprintSelector() {
        if (sprintSelector == null) {
            return;
        }
        try {
            if (featureId != null) {
                allFeatureSprints = sprintApi.getAll().stream()
                        .filter(s -> !"Backlog".equals(s.getName()))
                        .sorted(Comparator.comparing(Sprint::getStart, Comparator.nullsLast(Comparator.naturalOrder())))
                        .collect(Collectors.toCollection(ArrayList::new));
            } else {
                allFeatureSprints = new ArrayList<>();
            }
            isRestoringFromUrl = true;
            sprintSelector.setItems(allFeatureSprints);
            if (sprintId != null) {
                allFeatureSprints.stream()
                        .filter(s -> s.getId().equals(sprintId))
                        .findFirst()
                        .ifPresent(sprintSelector::setValue);
            }
        } catch (Exception e) {
            logger.error("Error loading sprints for selector", e);
        } finally {
            isRestoringFromUrl = false;
        }
    }

    /**
     * Re-generates both SVG charts using the current theme.
     * Syncs the theme on the UI thread first, then launches two independent async tasks.
     * No-op when sprint data has not been loaded yet.
     */
    private void refreshCharts() {
        if (sprint == null) {
            return;
        }
        context.syncTheme();
//        generateBurnDownChartAsync();
        generateGanttBurndownChartAsync();
    }

    /**
     * (Re-)builds the content area for the currently selected sprint.
     * Removes any children added by a previous cycle (keeping the persistent
     * {@link #header}), then loads data and recreates the detail grid and charts.
     * Called both from {@link #afterNavigation} on first load and from the sprint
     * selector listener when the user picks a different sprint.
     */
    private void reloadContent() {
        // Remove children from a previous render cycle, keeping the persistent header.
        getChildren()
                .filter(c -> c != header)
                .collect(Collectors.toList())
                .forEach(this::remove);

        ganttUtil = new GanttUtil();
        loadData();

        updateHeaderForSelection();

        if (sprint == null) {
            return;
        }


        //- update breadcrumbs
        getElement().getParent().getComponent()
                .ifPresent(component -> {
                    if (component instanceof MainLayout mainLayout) {
                        mainLayout.getBreadcrumbs().clear();
//                        Product product = productApi.getById(productId);
                        mainLayout.getBreadcrumbs().addItem("Products (" + sprint.getFeature().getVersion().getProduct().getName() + ")", ProductListView.class);
                        {
                            Map<String, String> params = new HashMap<>();
                            params.put("product", String.valueOf(productId));
//                            Version version = versionApi.getById(versionId);
                            mainLayout.getBreadcrumbs().addItem("Versions (" + sprint.getFeature().getVersion().getName() + ")", VersionListView.class, params);
                        }
                        {
                            Map<String, String> params = new HashMap<>();
                            params.put("product", String.valueOf(productId));
                            params.put("version", String.valueOf(versionId));
//                            Feature feature = featureApi.getById(featureId);
                            mainLayout.getBreadcrumbs().addItem("Features (" + sprint.getFeature().getName() + ")", FeatureListView.class, params);
                        }
                        {
                            Map<String, String> params = new HashMap<>();
                            params.put("product", String.valueOf(productId));
                            params.put("version", String.valueOf(versionId));
                            params.put("feature", String.valueOf(featureId));
//                            Sprint s = sprintApi.getById(sprintId);
                            mainLayout.getBreadcrumbs().addItem("Sprints (" + sprint.getName() + ")", SprintListView.class, params);
                        }
                        {
                            Map<String, String> params = new HashMap<>();
                            params.put("product", String.valueOf(productId));
                            params.put("version", String.valueOf(versionId));
                            params.put("feature", String.valueOf(featureId));
                            params.put("sprint", String.valueOf(sprintId));
                            mainLayout.getBreadcrumbs().addItem("Quality Board", Backlog.class, params);
                        }
                    }
                });

        // Check if sprint has start date
        if (sprint.getStart() == null) {
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
        } else {
            createSprintDetailsLayout();
            createGanttBurndownChart();
            refreshCharts();
        }
    }

    /**
     * Updates the page title text and avatar to reflect the currently selected sprint.
     * <ul>
     *   <li>Sprint loaded → show that sprint's avatar and name.</li>
     *   <li>No sprint loaded → hide avatar and show "Sprint Quality Board".</li>
     * </ul>
     */
    private void updateHeaderForSelection() {
        if (sprint != null) {
            pageTitle.setText(sprint.getName());
            if (sprint.getLightAvatarHash() != null && !sprint.getLightAvatarHash().isEmpty()) {
                boolean isDark = UI.getCurrent().getElement().getThemeList().contains(Lumo.DARK);
                headerAvatar.setSrc(sprint.getAvatarUrl(isDark));
                headerAvatar.setVisible(true);
            } else {
                headerAvatar.setVisible(false);
            }
        } else {
            pageTitle.setText("Sprint Quality Board");
            headerAvatar.setVisible(false);
        }
    }

    /**
     * Pushes the current {@code product}, {@code version}, {@code feature}, and {@code sprint}
     * into the browser URL so that pressing F5 restores the view to the same sprint.
     * Uses {@link com.vaadin.flow.component.page.History#replaceState} to update the address bar
     * without triggering a full Vaadin navigation cycle.  No-op while restoring state from a URL
     * to prevent feedback loops.
     */
    private void updateUrlParameters() {
        if (isRestoringFromUrl) {
            return;
        }
        List<String> parts = new ArrayList<>();
        if (productId != null) {
            parts.add("product=" + productId);
        }
        if (versionId != null) {
            parts.add("version=" + versionId);
        }
        if (featureId != null) {
            parts.add("feature=" + featureId);
        }
        if (sprintId != null) {
            parts.add("sprint=" + sprintId);
        }
        String url = "quality-board" + (parts.isEmpty() ? "" : "?" + String.join("&", parts));
        getUI().ifPresent(ui -> ui.getPage().getHistory().replaceState(null, url));
    }

}
