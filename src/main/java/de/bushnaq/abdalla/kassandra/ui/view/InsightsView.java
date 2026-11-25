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

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.bushnaq.abdalla.kassandra.Context;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.ai.insights.SprintInsightsGenerator;
import de.bushnaq.abdalla.kassandra.config.DefaultEntitiesInitializer;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.rest.api.*;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.component.SmartHeader;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Route(value = "insights", layout = MainLayout.class)
@PageTitle("Insights Page")
@Menu(order = 0, icon = "vaadin:lightbulb", title = "Insights")
@PermitAll // When security is enabled, allow all authenticated users
public class InsightsView extends Main implements AfterNavigationObserver {

    @Autowired
    protected Context context;
    private   Button  generateInsightsButton;
    private   Button  generateQuickSummaryButton;
    private   MenuItem sprintSummaryMenu;
    private   Disposable insightsSubscription;
    private   long        insightsRequestId;
    private final SmartHeader             header;
    private   Div     insightsContent;
    String jsonString = "";
    private       ProgressBar             loadingIndicator;
    final         Logger                  logger = LoggerFactory.getLogger(this.getClass());
    private       VerticalLayout          mainLayout;
    private final LocalDateTime           now;
    private       TextField               questionField;
    private final SprintApi               sprintApi;
    private final ProductApi              productApi;
    private final FeatureApi              featureApi;
    // AI Insights Components
    @Autowired
    private       SprintInsightsGenerator sprintInsightsGenerator;
    List<SprintStatistics> sprintStatistics = new ArrayList<>();
    private final TaskApi    taskApi;
    private final UserApi    userApi;
    private final VersionApi versionApi;
    private final WorklogApi worklogApi;

    public InsightsView(WorklogApi worklogApi, TaskApi taskApi, SprintApi sprintApi, ProductApi productApi, VersionApi versionApi, FeatureApi featureApi, UserApi userApi, Clock clock) {
        this.worklogApi = worklogApi;
        this.taskApi    = taskApi;
        this.sprintApi  = sprintApi;
        this.productApi = productApi;
        this.versionApi = versionApi;
        this.featureApi = featureApi;
        this.userApi = userApi;
        this.now     = ParameterOptions.getLocalNow();

        header = new SmartHeader("Sprint Insights", null, VaadinIcon.LIGHTBULB.create());

        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        this.getStyle().set("padding-left", "var(--lumo-space-m)");
        this.getStyle().set("padding-right", "var(--lumo-space-m)");

        initializeUI();
        addDetachListener(event -> {
            insightsRequestId++;
            if (insightsSubscription != null) {
                insightsSubscription.dispose();
                insightsSubscription = null;
                setLoadingState(false);
            }
        });
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        MainLayout.findParent(this)
                .ifPresent(component -> {
                    if (component instanceof MainLayout layout) {
                        layout.getBreadcrumbs().clear();
                        layout.getBreadcrumbs().addItem("Insights", InsightsView.class);
                    }
                });
        loadData();
        generateJson();
    }

    private void createControlsSection() {
        VerticalLayout controlsSection = new VerticalLayout();
        controlsSection.setSpacing(true);
        controlsSection.setPadding(false);

        // Description
        Paragraph description = new Paragraph(
                "Generate AI-powered insights from your sprint data. " +
                        "The system analyzes sprint performance, timeline adherence, estimation accuracy, and provides actionable recommendations. " +
                        "Sprint Summary is available when a started sprint has logged work."
        );
        description.addClassNames(LumoUtility.TextColor.SECONDARY);

        // Question input field
        questionField = new TextField("Ask a specific question (optional)");
        questionField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        questionField.setPlaceholder("e.g., Which sprints are at risk of missing deadlines?");
        questionField.setWidthFull();
        questionField.setHelperText("Leave empty for comprehensive analysis of all aspects");

        generateInsightsButton = new Button("Generate Comprehensive Insights", VaadinIcon.LIGHTBULB.create());
        generateInsightsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        generateInsightsButton.addClickListener(e -> generateInsights());

        generateQuickSummaryButton = new Button("Quick Summary", VaadinIcon.CLIPBOARD_TEXT.create());
        generateQuickSummaryButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        generateQuickSummaryButton.addClickListener(e -> generateQuickSummary());

        MenuBar sprintMenuBar = new MenuBar();
        sprintSummaryMenu = sprintMenuBar.addItem("Sprint Summary");
        sprintSummaryMenu.setEnabled(false);
        sprintSummaryMenu.setTooltipText("Select a started sprint with logged work");
        header.addActions(generateInsightsButton, generateQuickSummaryButton, sprintMenuBar);

        // Loading indicator
        loadingIndicator = new ProgressBar();
        loadingIndicator.setIndeterminate(true);
        loadingIndicator.setVisible(false);
        loadingIndicator.setWidthFull();

        controlsSection.add(description, questionField, loadingIndicator);
        mainLayout.add(controlsSection);
    }

    private void createInsightsSection() {
        VerticalLayout insightsContainer = new VerticalLayout();
        insightsContainer.setSpacing(true);
        insightsContainer.setPadding(false);
        insightsContainer.setWidthFull();
        insightsContainer.getStyle().set("flex", "1 1 0").set("min-height", "0");

        H3 insightsTitle = new H3("AI-Generated Insights");
        insightsTitle.addClassNames(LumoUtility.Margin.Top.LARGE);

        insightsContent = new Div();
        insightsContent.addClassNames(
                LumoUtility.Background.CONTRAST_5,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.MEDIUM
        );
        insightsContent.setWidthFull();
        insightsContent.getStyle().set("min-height", "0").set("flex", "1 1 0")
                .set("overflow-y", "auto").set("overflow-x", "auto");

        // Initial content
        Paragraph placeholder = new Paragraph("Click 'Generate Comprehensive Insights' or 'Quick Summary' to analyze your sprint data with AI.");
        placeholder.addClassNames(LumoUtility.TextColor.SECONDARY);
        insightsContent.add(placeholder);

        insightsContainer.add(insightsTitle, insightsContent);
        mainLayout.add(insightsContainer);
    }

    private void displayInsights(String insights) {
        insightsContent.removeAll();

        if (insights == null || insights.trim().isEmpty()) {
            Paragraph errorMsg = new Paragraph("No insights generated. Please try again.");
            errorMsg.addClassNames(LumoUtility.TextColor.ERROR);
            insightsContent.add(errorMsg);
            return;
        }

        Markdown markdown = new Markdown(unwrapMarkdownReport(insights));
        markdown.setLineBreaks(true);
        insightsContent.add(markdown);

        // Add timestamp
        LocalDateTime now       = LocalDateTime.now();
        Paragraph     timestamp = new Paragraph("Generated on: " + now);
        timestamp.addClassNames(
                LumoUtility.TextColor.TERTIARY,
                LumoUtility.FontSize.SMALL,
                LumoUtility.Margin.Top.MEDIUM
        );
        insightsContent.add(timestamp);
    }

    private String unwrapMarkdownReport(String insights) {
        Matcher opening = Pattern.compile("\\A[ \\t]*```(?:markdown|md)[ \\t]*\\r?\\n", Pattern.CASE_INSENSITIVE).matcher(insights);
        if (!opening.find()) {
            return insights;
        }

        String report = insights.substring(opening.end());
        Matcher fences = Pattern.compile("(?m)^ {0,3}(`{3,}|~{3,})([^\\r\\n]*)").matcher(report);
        String innerFence = null;
        while (fences.find()) {
            String marker = fences.group(1);
            String suffix = fences.group(2);
            if (innerFence == null) {
                if (marker.equals("```") && suffix.isBlank() && report.substring(fences.end()).isBlank()) {
                    return report.substring(0, fences.start());
                }
                if (marker.charAt(0) != '`' || !suffix.contains("`")) {
                    innerFence = marker;
                }
            } else if (marker.charAt(0) == innerFence.charAt(0)
                    && marker.length() >= innerFence.length() && suffix.isBlank()) {
                innerFence = null;
            }
        }
        return report;
    }

    private void generateInsights() {
        if (sprintStatistics.isEmpty()) {
            Notification.show("No sprint data available. Please ensure sprints are loaded.", 3000, Notification.Position.MIDDLE);
            return;
        }
//todo re-introduce

//                    result = sprintInsightsGenerator.generateInsights(jsonString);
//                    logger.info("Comprehensive insights generation completed successfully. Result length: {}", result != null ? result.length() : 0);
        String question = questionField.getValue();
        streamInsights(sprintInsightsGenerator.streamInsights(jsonString, question));
    }

    private void generateJson() {
        // Create a separate ObjectMapper that respects @JsonIgnore annotations for AI insights
        JsonMapper jsonMapper = new JsonMapper();
        //todo do we need this?
//        jsonMapper.registerModule(new JavaTimeModule());
//        jsonMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        jsonString = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sprintStatistics);
        logger.info("Generated JSON for {} sprints", sprintStatistics.size());
    }

    private void generateQuickSummary() {
        if (sprintStatistics.isEmpty()) {
            Notification.show("No sprint data available. Please ensure sprints are loaded.", 3000, Notification.Position.MIDDLE);
            return;
        }

        streamInsights(sprintInsightsGenerator.streamInsights(jsonString,
                "Provide only a brief summary with key metrics: total sprints, status distribution, and top 2 most important insights. Keep it under 200 words."));
    }

    private void generateSprintSummary(Sprint sprint, SprintStatistics statistics, String label) {
        String sprintJson = createSprintSummaryJson(sprint, statistics, label);
        streamInsights(sprintInsightsGenerator.streamSprintSummary(sprintJson));
    }

    private String createSprintSummaryJson(Sprint sprint, SprintStatistics statistics, String label) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("context", label);
        data.put("statistics", statistics);
        data.put("tasks", sprint.getTasks().stream().map(task -> {
            Map<String, Object> facts = new LinkedHashMap<>();
            facts.put("name", task.getName());
            facts.put("deliveryBuffer", task.getName() != null && task.isDeliveryBufferTask());
            facts.put("status", task.getTaskStatus());
            facts.put("timeSpentSeconds", task.getTimeSpent() == null ? null : task.getTimeSpent().getSeconds());
            facts.put("remainingEstimateSeconds", task.getRemainingEstimate() == null ? null : task.getRemainingEstimate().getSeconds());
            facts.put("maximumEstimateSeconds", task.getMaxEstimate() == null ? null : task.getMaxEstimate().getSeconds());
            facts.put("start", task.getStart());
            facts.put("finish", task.getFinish());
            return facts;
        }).toList());
        return new JsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(data);
    }

    private void streamInsights(Flux<String> response) {
        getUI().ifPresent(ui -> {
            long requestId = ++insightsRequestId;
            setLoadingState(true);
            insightsContent.removeAll();
            Markdown markdown = new Markdown("");
            markdown.setLineBreaks(true);
            insightsContent.add(markdown);
            StringBuilder accumulated = new StringBuilder();
            insightsSubscription = response.subscribeOn(Schedulers.boundedElastic()).subscribe(chunk -> {
                accumulated.append(chunk);
                String text = accumulated.toString();
                ui.access(() -> {
                    if (!isAttached() || requestId != insightsRequestId) return;
                    markdown.setContent(unwrapMarkdownReport(text));
                    insightsContent.getElement().executeJs("requestAnimationFrame(() => { this.scrollTop = this.scrollHeight; });");
                    ui.push();
                });
            }, error -> {
                logger.error("Error streaming insights", error);
                ui.access(() -> {
                    if (!isAttached() || requestId != insightsRequestId) return;
                    Paragraph errorMessage = new Paragraph("Error generating insights: " + error.getMessage());
                    errorMessage.addClassNames(LumoUtility.TextColor.ERROR);
                    insightsContent.add(errorMessage);
                    setLoadingState(false);
                    insightsSubscription = null;
                    ui.push();
                });
            }, () -> {
                String result = accumulated.toString();
                ui.access(() -> {
                    if (!isAttached() || requestId != insightsRequestId) return;
                    displayInsights(result);
                    setLoadingState(false);
                    insightsSubscription = null;
                    insightsContent.getElement().executeJs("requestAnimationFrame(() => { this.scrollTop = this.scrollHeight; });");
                    ui.push();
                });
            });
        });
    }

    private void initializeUI() {
        mainLayout = new VerticalLayout();
        mainLayout.setSizeFull();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);
        mainLayout.getStyle().set("flex", "1 1 0").set("min-height", "0");

        add(header);
        add(mainLayout);

        createControlsSection();
        createInsightsSection();
    }

    private void loadData() {
        // Capture the security context from the current thread
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        List<Sprint> sprintIds = sprintApi.getAll();
        Map<UUID, Feature> features = new HashMap<>();
        featureApi.getAll().forEach(feature -> features.put(feature.getId(), feature));
        Map<UUID, Version> versions = new HashMap<>();
        versionApi.getAll().forEach(version -> versions.put(version.getId(), version));
        Map<UUID, Product> products = new HashMap<>();
        productApi.getAll().forEach(product -> products.put(product.getId(), product));
        List<SprintSelection> eligible = new ArrayList<>();
        sprintStatistics.clear();
        sprintSummaryMenu.getSubMenu().removeAll();

        for (Sprint sprint : sprintIds) {
            if (!sprint.getName().equals(DefaultEntitiesInitializer.BACKLOG_SPRINT_NAME)) {
                Sprint loaded = loadSprintData(authentication, sprint.getId());
                SprintStatistics statistics = new SprintStatistics(loaded, now);
                sprintStatistics.add(statistics);
                if (isEligibleForSummary(loaded)) {
                    Feature feature = features.get(loaded.getFeatureId());
                    Version version = feature == null ? null : versions.get(feature.getVersionId());
                    Product product = version == null ? null : products.get(version.getProductId());
                    String label = (product == null ? "" : product.getName() + " / ")
                            + (version == null ? "" : version.getName() + " / ")
                            + (feature == null ? "" : feature.getName() + " / ") + loaded.getName();
                    eligible.add(new SprintSelection(loaded, statistics, label));
                }
            }
        }
        eligible.sort(Comparator.comparing(SprintSelection::label, String.CASE_INSENSITIVE_ORDER));
        eligible.forEach(item -> sprintSummaryMenu.getSubMenu().addItem(item.label(),
                event -> generateSprintSummary(item.sprint(), item.statistics(), item.label())));
        sprintSummaryMenu.setEnabled(!eligible.isEmpty());
    }

    private boolean isEligibleForSummary(Sprint sprint) {
        return sprint.getStatus() == Status.STARTED && sprint.getWorklogs().stream()
                .anyMatch(worklog -> worklog.getTimeSpent() != null && !worklog.getTimeSpent().isZero()
                        && !worklog.getTimeSpent().isNegative());
    }

    private record SprintSelection(Sprint sprint, SprintStatistics statistics, String label) {
    }

    private Sprint loadSprintData(Authentication authentication, UUID sprintId) {
        Sprint sprint = null;
        long   time   = System.currentTimeMillis();
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
            sprint.recalculate(ParameterOptions.getLocalNow());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while loading sprint " + sprintId, e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to load sprint " + sprintId, e.getCause());
        }
        return sprint;
    }

    private void setLoadingState(boolean loading) {
        loadingIndicator.setVisible(loading);
        generateInsightsButton.setEnabled(!loading);
        generateQuickSummaryButton.setEnabled(!loading);
        sprintSummaryMenu.setEnabled(!loading && !sprintSummaryMenu.getSubMenu().getItems().isEmpty());

        if (loading) {
            generateInsightsButton.setText("Generating Insights...");
        } else {
            generateInsightsButton.setText("Generate Comprehensive Insights");
        }
    }
}
