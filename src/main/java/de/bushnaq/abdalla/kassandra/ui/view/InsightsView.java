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
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.bushnaq.abdalla.kassandra.Context;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.ai.SprintInsightsGenerator;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.Worklog;
import de.bushnaq.abdalla.kassandra.rest.api.*;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Route(value = "insights", layout = MainLayout.class)
@PageTitle("Insights Page")
@Menu(order = 0, icon = "vaadin:lightbulb", title = "Insights")
@PermitAll // When security is enabled, allow all authenticated users
public class InsightsView extends Main implements AfterNavigationObserver {

    @Autowired
    protected Context context;
    private   Button  generateInsightsButton;
    private   Button  generateQuickSummaryButton;
    private   Div     insightsContent;
    String jsonString = "";
    private       ProgressBar             loadingIndicator;
    final         Logger                  logger = LoggerFactory.getLogger(this.getClass());
    private       VerticalLayout          mainLayout;
    private final LocalDateTime           now;
    private final H2                      pageTitle;
    private       TextField               questionField;
    private final SprintApi               sprintApi;
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
//        this.productApi = productApi;
        this.versionApi = versionApi;
//        this.featureApi = featureApi;
        this.userApi = userApi;
        this.now     = ParameterOptions.getLocalNow();

        pageTitle = new H2("Sprint Insights");
        pageTitle.addClassNames(
                LumoUtility.Margin.Top.MEDIUM,
                LumoUtility.Margin.Bottom.SMALL
        );

        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        this.getStyle().set("padding-left", "var(--lumo-space-m)");
        this.getStyle().set("padding-right", "var(--lumo-space-m)");

        initializeUI();
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        getElement().getParent().getComponent()
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
                        "The system analyzes sprint performance, timeline adherence, estimation accuracy, and provides actionable recommendations."
        );
        description.addClassNames(LumoUtility.TextColor.SECONDARY);

        // Question input field
        questionField = new TextField("Ask a specific question (optional)");
        questionField.setPlaceholder("e.g., Which sprints are at risk of missing deadlines?");
        questionField.setWidthFull();
        questionField.setHelperText("Leave empty for comprehensive analysis of all aspects");

        // Buttons layout
        HorizontalLayout buttonsLayout = new HorizontalLayout();
        buttonsLayout.setSpacing(true);

        generateInsightsButton = new Button("Generate Comprehensive Insights", VaadinIcon.LIGHTBULB.create());
        generateInsightsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        generateInsightsButton.addClickListener(e -> generateInsights());

        generateQuickSummaryButton = new Button("Quick Summary", VaadinIcon.CLIPBOARD_TEXT.create());
        generateQuickSummaryButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        generateQuickSummaryButton.addClickListener(e -> generateQuickSummary());

        buttonsLayout.add(generateInsightsButton, generateQuickSummaryButton);

        // Loading indicator
        loadingIndicator = new ProgressBar();
        loadingIndicator.setIndeterminate(true);
        loadingIndicator.setVisible(false);
        loadingIndicator.setWidthFull();

        controlsSection.add(description, questionField, buttonsLayout, loadingIndicator);
        mainLayout.add(controlsSection);
    }

    private void createInsightsSection() {
        VerticalLayout insightsContainer = new VerticalLayout();
        insightsContainer.setSpacing(true);
        insightsContainer.setPadding(false);

        H3 insightsTitle = new H3("AI-Generated Insights");
        insightsTitle.addClassNames(LumoUtility.Margin.Top.LARGE);

        insightsContent = new Div();
        insightsContent.addClassNames(
                LumoUtility.Background.CONTRAST_5,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.MEDIUM
        );
        insightsContent.setWidthFull();
        insightsContent.getStyle().set("min-height", "200px");

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

        // Format the insights as HTML for better readability
        String[] sections = insights.split("\n\n");

        for (String section : sections) {
            if (section.trim().isEmpty()) continue;

            if (section.startsWith("##")) {
                // Main heading
                H3 heading = new H3(section.substring(2).trim());
                heading.addClassNames(LumoUtility.TextColor.PRIMARY);
                insightsContent.add(heading);
            } else if (section.startsWith("###")) {
                // Sub heading
                H4 subHeading = new H4(section.substring(3).trim());
                subHeading.addClassNames(LumoUtility.TextColor.SECONDARY);
                insightsContent.add(subHeading);
            } else if (section.startsWith("- ")) {
                // List items
                Div listContainer = new Div();
                listContainer.addClassNames(LumoUtility.Margin.Left.MEDIUM);

                String[] lines = section.split("\n");
                for (String line : lines) {
                    if (line.trim().startsWith("- ")) {
                        Paragraph listItem = new Paragraph("• " + line.substring(2).trim());
                        listItem.addClassNames(LumoUtility.Margin.Vertical.NONE);
                        listContainer.add(listItem);
                    }
                }
                insightsContent.add(listContainer);
            } else {
                // Regular paragraph
                Paragraph paragraph = new Paragraph(section.trim());
                paragraph.addClassNames(LumoUtility.Margin.Bottom.SMALL);
                insightsContent.add(paragraph);
            }
        }

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

    private void generateInsights() {
        if (sprintStatistics.isEmpty()) {
            Notification.show("No sprint data available. Please ensure sprints are loaded.", 3000, Notification.Position.MIDDLE);
            return;
        }

        setLoadingState(true);

        CompletableFuture<String> insightsFuture = CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting comprehensive insights generation...");
                String question = questionField.getValue();
                String result;
                if (question != null && !question.trim().isEmpty()) {
                    result = sprintInsightsGenerator.generateFocusedInsights(jsonString, question.trim());
                    logger.info("Focused insights generation completed successfully. Result length: {}", result != null ? result.length() : 0);
                } else {
//todo re-introduce

//                    result = sprintInsightsGenerator.generateInsights(jsonString);
//                    logger.info("Comprehensive insights generation completed successfully. Result length: {}", result != null ? result.length() : 0);
                    result = null;
                }
                return result;
            } catch (Exception e) {
                logger.error("Error generating insights", e);
                return "Error generating insights: " + e.getMessage();
            }
        });

        // Wait for the future to complete and then update UI in main thread
        try {
            String insights = insightsFuture.get();
            logger.info("Insights received in main thread, updating UI...");
            logger.info("Insights content preview: {}", insights != null ? insights.substring(0, Math.min(100, insights.length())) : "null");

            displayInsights(insights);
            setLoadingState(false);

            logger.info("UI update completed in main thread");
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error waiting for insights completion", e);
            setLoadingState(false);
            Notification.show("Error generating insights: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
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

        setLoadingState(true);

        CompletableFuture<String> summaryFuture = CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting quick summary generation...");
                String result = sprintInsightsGenerator.generateQuickSummary(jsonString);
                logger.info("Quick summary generation completed successfully. Result length: {}", result != null ? result.length() : 0);
                return result;
            } catch (Exception e) {
                logger.error("Error generating quick summary", e);
                return "Error generating quick summary: " + e.getMessage();
            }
        });

        // Wait for the future to complete and then update UI in main thread
        try {
            String summary = summaryFuture.get();
            logger.info("Summary received in main thread, updating UI...");
            logger.info("Summary content preview: {}", summary != null ? summary.substring(0, Math.min(100, summary.length())) : "null");

            displayInsights(summary);
            setLoadingState(false);

            logger.info("UI update completed in main thread");
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error waiting for quick summary completion", e);
            setLoadingState(false);
            Notification.show("Error generating quick summary: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void initializeUI() {
        mainLayout = new VerticalLayout();
        mainLayout.setSizeFull();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        add(pageTitle);
        add(mainLayout);

        createControlsSection();
        createInsightsSection();
    }

    private void loadData() {
        // Capture the security context from the current thread
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        List<Sprint> sprintIds = sprintApi.getAll();

        for (Sprint sprint : sprintIds) {
            sprintStatistics.add(new SprintStatistics(loadSprintData(authentication, sprint.getId()), now));
        }
    }

    private Sprint loadSprintData(Authentication authentication, Long sprintId) {
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
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error loading sprint data", e);
        }
        return sprint;
    }

    private void setLoadingState(boolean loading) {
        loadingIndicator.setVisible(loading);
        generateInsightsButton.setEnabled(!loading);
        generateQuickSummaryButton.setEnabled(!loading);

        if (loading) {
            generateInsightsButton.setText("Generating Insights...");
        } else {
            generateInsightsButton.setText("Generate Comprehensive Insights");
        }
    }
}
