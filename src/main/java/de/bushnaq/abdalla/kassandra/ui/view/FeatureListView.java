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
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import de.bushnaq.abdalla.kassandra.ai.AiFilterService;
import de.bushnaq.abdalla.kassandra.ai.mcp.AiAssistantService;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.dto.Feature;
import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.Version;
import de.bushnaq.abdalla.kassandra.rest.api.FeatureApi;
import de.bushnaq.abdalla.kassandra.rest.api.ProductApi;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import de.bushnaq.abdalla.kassandra.rest.api.VersionApi;
import de.bushnaq.abdalla.kassandra.security.SecurityUtils;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.component.AbstractMainGrid;
import de.bushnaq.abdalla.kassandra.ui.component.ChatAgentPanel;
import de.bushnaq.abdalla.kassandra.ui.component.ChatPanelSessionState;
import de.bushnaq.abdalla.kassandra.ui.dialog.ConfirmDialog;
import de.bushnaq.abdalla.kassandra.ui.dialog.FeatureDialog;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.Map;

@Route(value = "feature-list", layout = MainLayout.class)
@PageTitle("Feature List Page")
@PermitAll // When security is enabled, allow all authenticated users
@RolesAllowed({"USER", "ADMIN"}) // Restrict access to users with specific roles
public class FeatureListView extends AbstractMainGrid<Feature> implements AfterNavigationObserver {
    public static final  String                 CREATE_FEATURE_BUTTON_ID          = "create-feature-button";
    public static final  String                 FEATURE_AI_PANEL_BUTTON           = "feature-ai-panel-button";
    public static final  String                 FEATURE_GLOBAL_FILTER             = "feature-global-filter";
    public static final  String                 FEATURE_GRID                      = "feature-grid";
    public static final  String                 FEATURE_GRID_DELETE_BUTTON_PREFIX = "feature-grid-delete-button-prefix-";
    public static final  String                 FEATURE_GRID_EDIT_BUTTON_PREFIX   = "feature-grid-edit-button-prefix-";
    public static final  String                 FEATURE_GRID_NAME_PREFIX          = "feature-grid-name-";
    public static final  String                 FEATURE_LIST_PAGE_TITLE           = "feature-list-page-title";
    public static final  String                 FEATURE_ROW_COUNTER               = "feature-row-counter";
    private static final String                 ROUTE_KEY_PREFIX                  = "feature-list:";
    private final        Button                 aiToggleButton;
    private final        SplitLayout            bodySplit;
    private final        ChatAgentPanel         chatAgentPanel;
    private final        Div                    chatPane;
    private final        FeatureApi             featureApi;
    private final        ProductApi             productApi;
    private              Long                   productId;
    private final        ChatPanelSessionState  sessionState;
    private final        StableDiffusionService stableDiffusionService;
    private final        UserApi                userApi;
    private final        VersionApi             versionApi;
    private              Long                   versionId;

    public FeatureListView(FeatureApi featureApi, ProductApi productApi, VersionApi versionApi, UserApi userApi,
                           Clock clock, AiFilterService aiFilterService, JsonMapper mapper,
                           StableDiffusionService stableDiffusionService,
                           AiAssistantService aiAssistantService,
                           ChatPanelSessionState chatPanelSessionState) {
        super(clock);
        this.featureApi             = featureApi;
        this.productApi             = productApi;
        this.versionApi             = versionApi;
        this.userApi                = userApi;
        this.stableDiffusionService = stableDiffusionService;
        this.sessionState           = chatPanelSessionState;

        add(createSmartHeader("Features", FEATURE_LIST_PAGE_TITLE, VaadinIcon.LIGHTBULB,
                CREATE_FEATURE_BUTTON_ID, () -> openFeatureDialog(null),
                FEATURE_ROW_COUNTER, FEATURE_GLOBAL_FILTER, aiFilterService, mapper, "Feature"));

        aiToggleButton = new Button("AI");
        aiToggleButton.setId(FEATURE_AI_PANEL_BUTTON);
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
        Location        location        = event.getLocation();
        QueryParameters queryParameters = location.getQueryParameters();
        if (queryParameters.getParameters().containsKey("product")) {
            this.productId = Long.parseLong(queryParameters.getParameters().get("product").getFirst());
        }
        if (queryParameters.getParameters().containsKey("version")) {
            this.versionId = Long.parseLong(queryParameters.getParameters().get("version").getFirst());
        }

        chatAgentPanel.restoreOrStart(ROUTE_KEY_PREFIX + productId + ":" + versionId);

        //- update breadcrumbs
        getElement().getParent().getComponent()
                .ifPresent(component -> {
                    if (component instanceof MainLayout mainLayout) {
                        mainLayout.getBreadcrumbs().clear();
                        Product product = productApi.getById(productId);
                        Version version = versionApi.getById(versionId);
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
                            mainLayout.getBreadcrumbs().addItem("Features", FeatureListView.class, params);
                        }

                        // Pass navigation context to the AI panel so the LLM knows which product
                        // and version are selected and can supply the correct IDs to FeatureTools without asking.
                        chatAgentPanel.setViewContext(
                                "You are viewing the feature list of version '" + version.getName() + "' (versionId=" + versionId + ") of product '" + product.getName() + "' (productId=" + productId + "). " +
                                        "Use versionId=" + versionId + " when calling createFeature, updateFeature or any other feature tool that requires a versionId.");
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


    private void confirmDelete(Feature feature) {
        String message = "Are you sure you want to delete feature \"" + feature.getName() + "\"?";
        ConfirmDialog dialog = new ConfirmDialog(
                "Confirm Delete",
                message,
                "Delete",
                () -> {
                    featureApi.deleteById(feature.getId());
                    refreshGrid();
                    Notification.show("Feature deleted", 3000, Notification.Position.BOTTOM_START);
                }
        );
        dialog.open();
    }

    protected void initGrid(Clock clock) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).withZone(clock.getZone()).withLocale(getLocale());

        getGrid().setId(FEATURE_GRID);

        // Add click listener to navigate to SprintListView with the selected feature ID
        getGrid().addItemClickListener(event -> {
            Feature selectedFeature = event.getItem();
            // Create parameters map
            Map<String, String> params = new HashMap<>();
            params.put("product", String.valueOf(productId));
            params.put("version", String.valueOf(versionId));
            params.put("feature", String.valueOf(selectedFeature.getId()));
            // Navigate with query parameters
            UI.getCurrent().navigate(
                    SprintListView.class,
                    QueryParameters.simple(params)
            );
        });

        {
            Grid.Column<Feature> keyColumn = getGrid().addColumn(Feature::getKey);
            VaadinUtil.addSimpleHeader(keyColumn, "Key", VaadinIcon.KEY);
        }
        {
            // Add avatar image column
            Grid.Column<Feature> avatarColumn = getGrid().addColumn(new ComponentRenderer<>(feature -> {
                // Feature has a custom image - use URL-based loading
                com.vaadin.flow.component.html.Image avatar = new com.vaadin.flow.component.html.Image();
                avatar.setWidth("24px");
                avatar.setHeight("24px");
                avatar.getStyle()
                        .set("border-radius", "var(--lumo-border-radius)")
                        .set("object-fit", "cover")
                        .set("display", "block")
                        .set("margin", "0")
                        .set("padding", "0");

                // Use hash-based URL for proper caching
                avatar.setSrc(feature.getAvatarUrl());
                avatar.setAlt(feature.getName());
                return avatar;
            }));
            avatarColumn.setWidth("48px");
            avatarColumn.setFlexGrow(0);
            avatarColumn.setHeader("");
        }
        {
            // Add name column with filtering and sorting
            Grid.Column<Feature> nameColumn = getGrid().addColumn(new ComponentRenderer<>(feature -> {
                Div div = new Div();
                div.add(feature.getName());
                div.setId(FEATURE_GRID_NAME_PREFIX + feature.getName());
                return div;
            }));

            // Configure a custom comparator to properly sort by the name property
            nameColumn.setComparator((feature1, feature2) ->
                    feature1.getName().compareToIgnoreCase(feature2.getName()));

            VaadinUtil.addSimpleHeader(nameColumn, "Name", VaadinIcon.LIGHTBULB);
        }
        {
            Grid.Column<Feature> createdColumn = getGrid().addColumn(feature -> dateTimeFormatter.format(feature.getCreated()));
            VaadinUtil.addSimpleHeader(createdColumn, "Created", VaadinIcon.CALENDAR);
        }
        {
            Grid.Column<Feature> updatedColumn = getGrid().addColumn(feature -> dateTimeFormatter.format(feature.getUpdated()));
            VaadinUtil.addSimpleHeader(updatedColumn, "Updated", VaadinIcon.CALENDAR);
        }

        // Add actions column using VaadinUtil
        VaadinUtil.addActionColumn(
                getGrid(),
                FEATURE_GRID_EDIT_BUTTON_PREFIX,
                FEATURE_GRID_DELETE_BUTTON_PREFIX,
                Feature::getName,
                this::openFeatureDialog,
                this::confirmDelete
        );

    }

    private void openFeatureDialog(Feature feature) {
        FeatureDialog dialog = new FeatureDialog(feature, stableDiffusionService, featureApi, versionId);
        dialog.addOpenedChangeListener(event -> {
            if (!event.isOpened()) {
                // Dialog was closed, refresh the grid
                refreshGrid();
            }
        });
        dialog.open();
    }

    private void refreshGrid() {
        // Clear existing items
        getDataProvider().getItems().clear();

        // Fetch fresh data from API (with updated hashes)
        getDataProvider().getItems().addAll((versionId != null) ? featureApi.getAll(versionId) : featureApi.getAll());

        // Force complete refresh of the grid
        getDataProvider().refreshAll();

        // Force the grid to re-render
        getGrid().getDataProvider().refreshAll();

        // Push UI updates if in push mode
        getUI().ifPresent(ui -> ui.push());
    }
}
