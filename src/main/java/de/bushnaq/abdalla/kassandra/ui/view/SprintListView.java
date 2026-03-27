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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.theme.lumo.Lumo;
import de.bushnaq.abdalla.kassandra.ai.filter.AiFilterService;
import de.bushnaq.abdalla.kassandra.ai.mcp.AiAssistantService;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.config.DefaultEntitiesInitializer;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.rest.api.*;
import de.bushnaq.abdalla.kassandra.security.SecurityUtils;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.component.AbstractMainGrid;
import de.bushnaq.abdalla.kassandra.ui.component.ChatAgentPanel;
import de.bushnaq.abdalla.kassandra.ui.component.ChatPanelSessionState;
import de.bushnaq.abdalla.kassandra.ui.dialog.ConfirmDialog;
import de.bushnaq.abdalla.kassandra.ui.dialog.SprintDialog;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;
import de.bushnaq.abdalla.util.date.DateUtil;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.stream.Collectors;

@Route(value = "sprint-list", layout = MainLayout.class)
@PageTitle("Sprint List Page")
//@Menu(order = 1, icon = "vaadin:factory", title = "project List")
@PermitAll // When security is enabled, allow all authenticated users
@Slf4j
public class SprintListView extends AbstractMainGrid<Sprint> implements AfterNavigationObserver {
    public static final  String                       CREATE_SPRINT_BUTTON             = "create-sprint-button";
    public static final  String                       FEATURE_SELECTOR                 = "feature-selector";
    private static final String                       PARAM_AI_PANEL                   = "aiPanel";
    private static final String                       ROUTE_KEY_PREFIX                 = "sprint-list:";
    public static final  String                       SPRINT_AI_PANEL_BUTTON           = "sprint-ai-panel-button";
    public static final  String                       SPRINT_GLOBAL_FILTER             = "sprint-global-filter";
    public static final  String                       SPRINT_GRID                      = "sprint-grid";
    public static final  String                       SPRINT_GRID_CONFIG_BUTTON_PREFIX = "sprint-grid-config-button-prefix-";
    public static final  String                       SPRINT_GRID_DELETE_BUTTON_PREFIX = "sprint-grid-delete-button-prefix-";
    public static final  String                       SPRINT_GRID_EDIT_BUTTON_PREFIX   = "sprint-grid-edit-button-prefix-";
    public static final  String                       SPRINT_GRID_NAME_PREFIX          = "sprint-grid-name-";
    public static final  String                       SPRINT_LIST_PAGE_TITLE           = "sprint-list-page-title";
    public static final  String                       SPRINT_ROW_COUNTER               = "sprint-row-counter";
    private final        Button                       aiToggleButton;
    private              List<Sprint>                 allSprints                       = new ArrayList<>();
    private final        SplitLayout                  bodySplit;
    private final        ChatAgentPanel               chatAgentPanel;
    private final        Div                          chatPane;
    private final        Clock                        clock;
    private final        FeatureApi                   featureApi;
    private              Long                         featureId;
    private final        Map<Long, Feature>           featureMap                       = new HashMap<>();
    private final        MultiSelectComboBox<Feature> featureSelector;
    private              boolean                      isRestoringFromUrl               = false;
    private final        ProductApi                   productApi;
    private              Long                         productId;
    private final        Map<Long, Product>           productMap                       = new HashMap<>();
    private              String                       requestedFeatureIds;
    private              Set<Feature>                 selectedFeatures                 = new HashSet<>();
    private final        ChatPanelSessionState        sessionState;
    private final        SprintApi                    sprintApi;
    private final        StableDiffusionService       stableDiffusionService;
    private final        UserApi                      userApi;
    private final        VersionApi                   versionApi;
    private              Long                         versionId;
    private final        Map<Long, Version>           versionMap                       = new HashMap<>();

    public SprintListView(SprintApi sprintApi, ProductApi productApi, VersionApi versionApi, FeatureApi featureApi,
                          UserApi userApi, Clock clock, AiFilterService aiFilterService, JsonMapper mapper,
                          StableDiffusionService stableDiffusionService,
                          AiAssistantService aiAssistantService,
                          ChatPanelSessionState chatPanelSessionState) {
        super(clock);
        this.sprintApi              = sprintApi;
        this.productApi             = productApi;
        this.versionApi             = versionApi;
        this.featureApi             = featureApi;
        this.userApi                = userApi;
        this.clock                  = clock;
        this.stableDiffusionService = stableDiffusionService;
        this.sessionState           = chatPanelSessionState;

        add(createSmartHeader("Sprints", SPRINT_LIST_PAGE_TITLE, VaadinIcon.EXIT,
                CREATE_SPRINT_BUTTON, () -> openSprintDialog(null),
                SPRINT_ROW_COUNTER, SPRINT_GLOBAL_FILTER, aiFilterService, mapper, "Sprint"));

        featureSelector = new MultiSelectComboBox<>();
        featureSelector.setId(FEATURE_SELECTOR);
        // Feature names are only unique within one version,
        // and version names within one product — prefix with "Product / Version"
        // to guarantee uniqueness across the entire server.
        featureSelector.setItemLabelGenerator(f -> {
            Version v = versionMap.get(f.getVersionId());
            Product p = v != null ? productMap.get(v.getProductId()) : null;
            String prefix = (p != null ? p.getName() + " / " : "")
                    + (v != null ? v.getName() + " / " : "");
            return prefix + f.getName();
        });
        featureSelector.setPlaceholder("All features");
        featureSelector.setClearButtonVisible(true);
        featureSelector.setWidth("380px");
        featureSelector.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                selectedFeatures = new HashSet<>(e.getValue());
                updateUrlParameters();
                applyFeatureFilter();
            }
        });
        // Insert before the smart-filter so the feature selector appears on the far left
        getLastHeaderRightLayout().addComponentAtIndex(0, featureSelector);

        aiToggleButton = new Button("AI");
        aiToggleButton.setId(SPRINT_AI_PANEL_BUTTON);
        aiToggleButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        aiToggleButton.getElement().setAttribute("title", "AI Assistant");
        addHeaderButton(aiToggleButton);

        chatAgentPanel = new ChatAgentPanel(aiAssistantService, userApi, chatPanelSessionState);
        chatAgentPanel.setSizeFull();

        chatPane = new Div(chatAgentPanel);
        chatPane.getStyle()
                .set("height", "100%").set("overflow", "hidden")
                .set("transition", "width 0.3s ease, opacity 0.3s ease")
                .set("width", "0").set("opacity", "0").set("min-width", "0");

        bodySplit = new SplitLayout(getGridPanelWrapper(), chatPane);
        bodySplit.setOrientation(SplitLayout.Orientation.HORIZONTAL);
        bodySplit.setSizeFull();
        bodySplit.setSplitterPosition(100);
        add(bodySplit);

        aiToggleButton.addClickListener(e -> {
            sessionState.setPanelOpen(!sessionState.isPanelOpen());
            applyChatPaneState(sessionState.isPanelOpen());
        });
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        log.info("=== afterNavigation called ===");
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
        // Capture requested features from URL for initial ComboBox preselection.
        // Cleared here so each navigation cycle starts fresh.
        requestedFeatureIds = null;
        if (queryParameters.getParameters().containsKey("features")) {
            requestedFeatureIds = queryParameters.getParameters().get("features").getFirst();
        }

        chatAgentPanel.restoreOrStart(ROUTE_KEY_PREFIX + productId + ":" + versionId + ":" + featureId);

        //- update breadcrumbs
        getElement().getParent().getComponent()
                .ifPresent(component -> {
                    if (component instanceof MainLayout mainLayout) {
                        mainLayout.getBreadcrumbs().clear();
                        Product product = productApi.getById(productId);
                        Version version = versionApi.getById(versionId);
                        Feature feature = featureApi.getById(featureId);
                        mainLayout.getBreadcrumbs().addItem("Products (" + product.getName() + ")", ProductListView.class);
                        {
                            Map<String, String> params = new HashMap<>();
                            params.put("product", String.valueOf(productId));
                            mainLayout.getBreadcrumbs().addItem("Versions (" + version.getName() + ")", VersionListView.class, params);
                        }
                        {
                            Map<String, String> params = new HashMap<>();
                            params.put("product", String.valueOf(productId));
                            params.put("version", String.valueOf(versionId));
                            mainLayout.getBreadcrumbs().addItem("Features (" + feature.getName() + ")", FeatureListView.class, params);
                        }
                        {
                            Map<String, String> params = new HashMap<>();
                            params.put("product", String.valueOf(productId));
                            params.put("version", String.valueOf(versionId));
                            params.put("feature", String.valueOf(featureId));
                            mainLayout.getBreadcrumbs().addItem("Sprints", SprintListView.class, params);
                        }

                        // Pass navigation context to the AI panel so the LLM knows which product,
                        // version, and feature are selected and can supply the correct IDs to SprintTools without asking.
                        chatAgentPanel.setViewContext(
                                "You are viewing the sprint list of feature '" + feature.getName() + "' (featureId=" + featureId + ") of version '" + version.getName() + "' (versionId=" + versionId + ") of product '" + product.getName() + "' (productId=" + productId + "). " +
                                        "Use featureId=" + featureId + " when calling createSprint or any other sprint tool that requires a featureId.");
                    }
                });

        applyChatPaneState(sessionState.isPanelOpen());

        final String userEmail  = SecurityUtils.getUserEmail();
        User         userFromDb = null;
        if (!userEmail.equals(SecurityUtils.GUEST)) {
            try {
                userFromDb = userApi.getByEmail(userEmail).get();
            } catch (Exception ignored) {
            }
        }
        chatAgentPanel.setCurrentUser(userFromDb);
        chatAgentPanel.setOnAiReply(this::refreshGrid);

        refreshGrid();
    }

    private void applyChatPaneState(boolean open) {
        if (open) {
            chatPane.getStyle().set("width", "35%").set("opacity", "1").set("min-width", "280px");
            bodySplit.setSplitterPosition(65);
            aiToggleButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        } else {
            chatPane.getStyle().set("width", "0").set("opacity", "0").set("min-width", "0");
            bodySplit.setSplitterPosition(100);
            aiToggleButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }
    }

    /**
     * Applies a data-provider filter so the grid shows only sprints whose feature is
     * selected in the MultiSelectComboBox.  When the selection is empty the filter is
     * cleared and all sprints are shown.
     */
    private void applyFeatureFilter() {
        if (!selectedFeatures.isEmpty()) {
            Set<Long> ids = selectedFeatures.stream().map(Feature::getId).collect(Collectors.toSet());
            getDataProvider().setFilter(sprint -> ids.contains(sprint.getFeatureId()));
        } else {
            getDataProvider().setFilter(null);
        }
    }

    private void confirmDelete(Sprint sprint) {
        String message = "Are you sure you want to delete sprint \"" + sprint.getName() + "\"?";
        ConfirmDialog dialog = new ConfirmDialog(
                "Confirm Delete",
                message,
                "Delete",
                () -> {
                    sprintApi.deleteById(sprint.getId());
                    refreshGrid();
                    Notification.show("Sprint deleted", 3000, Notification.Position.BOTTOM_START);
                }
        );
        dialog.open();
    }

    protected void initGrid(Clock clock) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).withZone(clock.getZone()).withLocale(getLocale());

        getGrid().setId(SPRINT_GRID);

        // Add click listener to navigate to SprintQualityBoard with the selected sprint ID
        getGrid().addItemClickListener(event -> {
            Sprint selectedSprint = event.getItem();
            // Create parameters map
            Map<String, String> params = new HashMap<>();
            params.put("product", String.valueOf(productId));
            params.put("version", String.valueOf(versionId));
            params.put("feature", String.valueOf(featureId));
            params.put("sprint", String.valueOf(selectedSprint.getId()));
            // Navigate with query parameters
            UI.getCurrent().navigate(
                    SprintQualityBoard.class,
                    QueryParameters.simple(params)
            );
        });

        {
            Grid.Column<Sprint> keyColumn = getGrid().addColumn(Sprint::getKey);
            VaadinUtil.addSimpleHeader(keyColumn, "Key", VaadinIcon.KEY);
        }

        {
            //product
            Grid.Column<Sprint> productColumn = getGrid().addColumn(sprint -> {
                Feature f = featureMap.get(sprint.getFeatureId());
                if (f == null) return "";
                Version v = versionMap.get(f.getVersionId());
                if (v == null) return "";
                Product p = productMap.get(v.getProductId());
                return p != null ? p.getName() : "";
            });
            VaadinUtil.addSimpleHeader(productColumn, "Product", VaadinIcon.PACKAGE);
        }

        {
            //version
            Grid.Column<Sprint> versionColumn = getGrid().addColumn(sprint -> {
                Feature f = featureMap.get(sprint.getFeatureId());
                if (f == null) return "";
                Version v = versionMap.get(f.getVersionId());
                return v != null ? v.getName() : "";
            });
            VaadinUtil.addSimpleHeader(versionColumn, "Version", VaadinIcon.COMPILE);
        }

        {
            //feature
            Grid.Column<Sprint> featureColumn = getGrid().addColumn(sprint -> {
                Feature f = featureMap.get(sprint.getFeatureId());
                return f != null ? f.getName() : "";
            });
            VaadinUtil.addSimpleHeader(featureColumn, "Feature", VaadinIcon.LIGHTBULB);
        }

        {
            // Add avatar image column
            Grid.Column<Sprint> avatarColumn = getGrid().addColumn(new ComponentRenderer<>(sprint -> {
                // Sprint has a custom image - use URL-based loading
                com.vaadin.flow.component.html.Image avatar = new com.vaadin.flow.component.html.Image();
                avatar.setWidth("24px");
                avatar.setHeight("24px");
                avatar.getStyle()
                        .set("border-radius", "var(--lumo-border-radius)")
                        .set("object-fit", "cover")
                        .set("display", "block")
                        .set("margin", "0")
                        .set("padding", "0");

                // Use hash-based URL for proper caching; theme-aware
                boolean isDark = UI.getCurrent().getElement().getThemeList().contains(Lumo.DARK);
                avatar.setSrc(sprint.getAvatarUrl(isDark));
                avatar.setAlt(sprint.getName());
                return avatar;
            }));
            avatarColumn.setWidth("48px");
            avatarColumn.setFlexGrow(0);
            avatarColumn.setHeader("");
        }
        {
            // Add name column with filtering and sorting
            Grid.Column<Sprint> nameColumn = getGrid().addColumn(new ComponentRenderer<>(sprint -> {
                Div div = new Div();
                div.add(sprint.getName());
                div.setId(SPRINT_GRID_NAME_PREFIX + sprint.getName());
                return div;
            }));

            // Configure a custom comparator to properly sort by the name property
            nameColumn.setComparator((sprint1, sprint2) ->
                    sprint1.getName().compareToIgnoreCase(sprint2.getName()));

            VaadinUtil.addSimpleHeader(nameColumn, "Name", VaadinIcon.EXIT);
        }

        {
            Grid.Column<Sprint> startColumn = getGrid().addColumn(sprint -> sprint.getStart() != null ? dateTimeFormatter.format(sprint.getStart()) : "");
            VaadinUtil.addSimpleHeader(startColumn, "Start", VaadinIcon.CALENDAR);
        }

        {
            Grid.Column<Sprint> endColumn = getGrid().addColumn(sprint -> sprint.getEnd() != null ? dateTimeFormatter.format(sprint.getEnd()) : "");
            VaadinUtil.addSimpleHeader(endColumn, "End", VaadinIcon.CALENDAR);
        }

        {
            Grid.Column<Sprint> statusColumn = getGrid().addColumn(sprint -> sprint.getStatus().name());
            VaadinUtil.addSimpleHeader(statusColumn, "Status", VaadinIcon.FLAG);
        }

        {
            Grid.Column<Sprint> originalEstimationColumn = getGrid().addColumn(sprint ->
                    sprint.getOriginalEstimation() != null ?
                            DateUtil.createDurationString(sprint.getOriginalEstimation(), false, true, true) : "");
            VaadinUtil.addSimpleHeader(originalEstimationColumn, "Original Estimation", VaadinIcon.CLOCK);
        }

        {
            Grid.Column<Sprint> workedColumn = getGrid().addColumn(sprint ->
                    sprint.getWorked() != null ?
                            DateUtil.createDurationString(sprint.getWorked(), false, true, true) : "");
            VaadinUtil.addSimpleHeader(workedColumn, "Worked", VaadinIcon.TIMER);
        }

        {
            Grid.Column<Sprint> remainingColumn = getGrid().addColumn(sprint ->
                    sprint.getRemaining() != null ?
                            DateUtil.createDurationString(sprint.getRemaining(), false, true, true) : "");
            VaadinUtil.addSimpleHeader(remainingColumn, "Remaining", VaadinIcon.HOURGLASS);
        }

        // Add actions column using VaadinUtil with an additional config button
        getGrid().addColumn(new ComponentRenderer<>(sprint -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setAlignItems(FlexComponent.Alignment.CENTER);
            layout.setSpacing(true);

            Button editButton = new Button(new Icon(VaadinIcon.EDIT));
            editButton.setId(SPRINT_GRID_EDIT_BUTTON_PREFIX + sprint.getName());
            editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            editButton.addClickListener(e -> openSprintDialog(sprint));
//            editButton.getElement().setAttribute("title", "Edit");

            Button configButton = new Button(new Icon(VaadinIcon.COG));
            configButton.setId(SPRINT_GRID_CONFIG_BUTTON_PREFIX + sprint.getName());
            configButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            configButton.addClickListener(e -> {
                Map<String, String> params = new HashMap<>();
                params.put("product", String.valueOf(productId));
                params.put("version", String.valueOf(versionId));
                params.put("feature", String.valueOf(featureId));
                params.put("sprint", String.valueOf(sprint.getId()));
                UI.getCurrent().navigate(
                        Backlog.class,
                        QueryParameters.simple(params)
                );
            });
//            configButton.getElement().setAttribute("title", "Tasks Configuration");

            Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
            deleteButton.setId(SPRINT_GRID_DELETE_BUTTON_PREFIX + sprint.getName());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(e -> confirmDelete(sprint));
            // Hide delete button for Backlog sprint
            deleteButton.setVisible(!DefaultEntitiesInitializer.BACKLOG_SPRINT_NAME.equals(sprint.getName()));
//            deleteButton.getElement().setAttribute("title", "Delete");

            layout.add(editButton, configButton, deleteButton);
            return layout;
        })).setWidth("160px").setFlexGrow(0);

    }

    private void openSprintDialog(Sprint sprint) {
        SprintDialog dialog = new SprintDialog(sprint, stableDiffusionService, sprintApi, featureId);
        dialog.addOpenedChangeListener(event -> {
            if (!event.isOpened()) {
                // Dialog was closed, refresh the grid
                refreshGrid();
            }
        });
        dialog.open();
    }

    /**
     * Reloads sprints from the API, refreshes the feature ComboBox item list while
     * preserving (or initially setting) the current selection, then re-applies
     * the grid filter.
     * <p>
     * Selection priority on the first call after navigation:
     * <ol>
     *   <li>Preserve existing ComboBox selection (on grid refresh).</li>
     *   <li>Restore from {@code features} URL parameter (comma-separated feature IDs).</li>
     *   <li>Preselect the navigation-context feature when {@code featureId} is set.</li>
     *   <li>Select all features when no context is given.</li>
     * </ol>
     */
    private void refreshGrid() {
        // Reload sprints and rebuild all lookup maps
        List<Sprint> freshSprints = sprintApi.getAll();
        allSprints = freshSprints;

        List<Feature> freshFeatures = featureApi.getAll();
        featureMap.clear();
        freshFeatures.forEach(f -> featureMap.put(f.getId(), f));

        versionMap.clear();
        versionApi.getAll().forEach(v -> versionMap.put(v.getId(), v));

        productMap.clear();
        productApi.getAll().forEach(p -> productMap.put(p.getId(), p));

        // Replace data-provider items
        getDataProvider().getItems().clear();
        getDataProvider().getItems().addAll(freshSprints);

        // Update ComboBox items while preserving / establishing selection
        if (featureSelector != null) {
            Set<Feature> currentSelection = featureSelector.getValue();
            isRestoringFromUrl = true;
            featureSelector.setItems(freshFeatures);
            if (!currentSelection.isEmpty()) {
                // Preserve the previously selected features (match by ID)
                Set<Long> selectedIds = currentSelection.stream().map(Feature::getId).collect(Collectors.toSet());
                Set<Feature> toRestore = freshFeatures.stream()
                        .filter(f -> selectedIds.contains(f.getId()))
                        .collect(Collectors.toSet());
                if (!toRestore.isEmpty()) {
                    selectedFeatures = toRestore;
                    featureSelector.setValue(toRestore);
                }
            } else if (requestedFeatureIds != null) {
                // URL contained a 'features' key — it is authoritative.
                // Non-empty → restore those IDs; empty string → user explicitly cleared, leave empty.
                if (!requestedFeatureIds.isEmpty()) {
                    Set<Feature> toSelect = new HashSet<>();
                    for (String idStr : requestedFeatureIds.split(",")) {
                        try {
                            Long id = Long.parseLong(idStr.trim());
                            freshFeatures.stream().filter(f -> f.getId().equals(id)).findFirst().ifPresent(toSelect::add);
                        } catch (NumberFormatException e) {
                            log.warn("Invalid feature ID in URL: {}", idStr);
                        }
                    }
                    if (!toSelect.isEmpty()) {
                        selectedFeatures = toSelect;
                        featureSelector.setValue(toSelect);
                    }
                }
                // else: empty string → leave selector empty (applyFeatureFilter will clear the filter)
            } else if (featureId != null) {
                // Default: preselect the navigation-context feature
                freshFeatures.stream()
                        .filter(f -> featureId.equals(f.getId()))
                        .findFirst()
                        .ifPresent(f -> {
                            selectedFeatures = new HashSet<>(Set.of(f));
                            featureSelector.setValue(selectedFeatures);
                        });
            } else if (!freshFeatures.isEmpty()) {
                // No context ID given → select all features
                selectedFeatures = new HashSet<>(freshFeatures);
                featureSelector.setValue(selectedFeatures);
            }
            isRestoringFromUrl = false;
        }

        // Apply ComboBox-driven filter
        applyFeatureFilter();

        // Force complete refresh of the grid
        getDataProvider().refreshAll();
        getGrid().getDataProvider().refreshAll();
        getUI().ifPresent(ui -> ui.push());
    }

    /**
     * Pushes the current {@code product}, {@code version}, {@code feature}, and
     * selected {@code sprint} into the URL so that the filter state survives
     * browser refresh and can be bookmarked.  No-op while restoring state from a
     * URL to prevent feedback loops.
     */
    private void updateUrlParameters() {
        if (isRestoringFromUrl) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        if (productId != null) {
            params.put("product", String.valueOf(productId));
        }
        if (versionId != null) {
            params.put("version", String.valueOf(versionId));
        }
        if (featureId != null) {
            params.put("feature", String.valueOf(featureId));
        }
        // Always write the key — empty string signals "user intentionally cleared".
        String featureIds = selectedFeatures.stream()
                .map(f -> String.valueOf(f.getId()))
                .collect(Collectors.joining(","));
        params.put("features", featureIds);
        getUI().ifPresent(ui -> ui.navigate(SprintListView.class, QueryParameters.simple(params)));
    }
}
