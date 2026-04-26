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
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.Lumo;
import de.bushnaq.abdalla.kassandra.ai.filter.AiFilterService;
import de.bushnaq.abdalla.kassandra.ai.lmstudio.LmStudioService;
import de.bushnaq.abdalla.kassandra.ai.mcp.AiAssistantService;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.AvatarService;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.config.DefaultEntitiesInitializer;
import de.bushnaq.abdalla.kassandra.config.KassandraProperties;
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
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.stream.Collectors;

@Route(value = "feature-list", layout = MainLayout.class)
@PageTitle("Feature List Page")
@Menu(order = 3, icon = "vaadin:puzzle-piece", title = "Features")
@PermitAll // When security is enabled, allow all authenticated users
@RolesAllowed({"USER", "ADMIN"}) // Restrict access to users with specific roles
@Slf4j
public class FeatureListView extends AbstractMainGrid<Feature> implements AfterNavigationObserver {
    public static final  String                       CREATE_FEATURE_BUTTON_ID          = "create-feature-button";
    public static final  String                       FEATURE_AI_PANEL_BUTTON           = "feature-ai-panel-button";
    public static final  String                       FEATURE_GLOBAL_FILTER             = "feature-global-filter";
    public static final  String                       FEATURE_GRID                      = "feature-grid";
    public static final  String                       FEATURE_GRID_DELETE_BUTTON_PREFIX = "feature-grid-delete-button-prefix-";
    public static final  String                       FEATURE_GRID_EDIT_BUTTON_PREFIX   = "feature-grid-edit-button-prefix-";
    public static final  String                       FEATURE_GRID_NAME_PREFIX          = "feature-grid-name-";
    public static final  String                       FEATURE_LIST_PAGE_TITLE           = "feature-list-page-title";
    public static final  String                       FEATURE_ROW_COUNTER               = "feature-row-counter";
    private static final String                       ROUTE_KEY_PREFIX                  = "feature-list:";
    public static final  String                       VERSION_SELECTOR                  = "version-selector";
    private final        Button                       aiToggleButton;
    private              List<Feature>                allFeatures                       = new ArrayList<>();
    private final        AvatarService                avatarService;
    private final        SplitLayout                  bodySplit;
    private final        ChatAgentPanel               chatAgentPanel;
    private final        Div                          chatPane;
    private final        FeatureApi                   featureApi;
    private              boolean                      isRestoringFromUrl                = false;
    private final        ProductApi                   productApi;
    private              UUID                         productId;
    private final        Map<UUID, Product>           productMap                        = new HashMap<>();
    private              String                       requestedVersionIds;
    private              Set<Version>                 selectedVersions                  = new HashSet<>();
    private final        ChatPanelSessionState        sessionState;
    private final        StableDiffusionService       stableDiffusionService;
    private final        UserApi                      userApi;
    private final        VersionApi                   versionApi;
    private              UUID                         versionId;
    private final        Map<UUID, Version>           versionMap                        = new HashMap<>();
    private final        MultiSelectComboBox<Version> versionSelector;

    public FeatureListView(FeatureApi featureApi, ProductApi productApi, VersionApi versionApi, UserApi userApi,
                           Clock clock, AiFilterService aiFilterService, JsonMapper mapper,
                           AvatarService avatarService,
                           StableDiffusionService stableDiffusionService,
                           AiAssistantService aiAssistantService,
                           ChatPanelSessionState chatPanelSessionState,
                           KassandraProperties kassandraProperties,
                           LmStudioService lmStudioService) {
        super(clock);
        this.featureApi             = featureApi;
        this.productApi             = productApi;
        this.versionApi             = versionApi;
        this.userApi                = userApi;
        this.avatarService          = avatarService;
        this.stableDiffusionService = stableDiffusionService;
        this.sessionState           = chatPanelSessionState;

        add(createSmartHeader("Features", FEATURE_LIST_PAGE_TITLE, (Image) null,
                CREATE_FEATURE_BUTTON_ID, () -> openFeatureDialog(null),
                FEATURE_ROW_COUNTER, FEATURE_GLOBAL_FILTER, aiFilterService, mapper, "Feature"));

        versionSelector = new MultiSelectComboBox<>();
        versionSelector.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        versionSelector.setId(VERSION_SELECTOR);
        // Version names are only unique within one product — prefix with product name.
        versionSelector.setItemLabelGenerator(v -> {
            Product p = productMap.get(v.getProductId());
            return p != null ? p.getName() + " / " + v.getName() : v.getName();
        });
        versionSelector.setPlaceholder("All versions");
        versionSelector.setClearButtonVisible(true);
        versionSelector.setWidth("280px");
        versionSelector.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                selectedVersions = new HashSet<>(e.getValue());
                updateHeaderForSelection();
                updateUrlParameters();
                applyFeatureFilter();
            }
        });
        // Insert before the smart-filter so the version selector appears on the far left
        getLastHeaderRightLayout().addComponentAtIndex(0, versionSelector);

        aiToggleButton = new Button("AI");
        aiToggleButton.setId(FEATURE_AI_PANEL_BUTTON);
        aiToggleButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        aiToggleButton.getElement().setAttribute("title", "AI Assistant");
        addHeaderButton(aiToggleButton);

        chatAgentPanel = new ChatAgentPanel(aiAssistantService, userApi, chatPanelSessionState, kassandraProperties, lmStudioService);
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
        log.info("=== afterNavigation called {} parameters", queryParameters.getParameters().size());
        if (queryParameters.getParameters().containsKey("product")) {
            this.productId = UUID.fromString(queryParameters.getParameters().get("product").getFirst());
        }
        if (queryParameters.getParameters().containsKey("version")) {
            this.versionId = UUID.fromString(queryParameters.getParameters().get("version").getFirst());
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
        if (productId == null || versionId == null) {
            log.warn("No products/versions available; FeatureListView will show empty state");
        }
        // Capture requested versions from URL for initial ComboBox preselection.
        // Cleared here so each navigation cycle starts fresh.
        requestedVersionIds = null;
        if (queryParameters.getParameters().containsKey("versions")) {
            requestedVersionIds = queryParameters.getParameters().get("versions").getFirst();
        }

        chatAgentPanel.restoreOrStart(ROUTE_KEY_PREFIX + productId + ":" + versionId);

        //- update breadcrumbs
        getElement().getParent().getComponent()
                .ifPresent(component -> {
                    if (component instanceof MainLayout mainLayout) {
                        if (productId == null || versionId == null) {
                            log.warn("No products/versions available; skipping FeatureListView breadcrumb setup");
                            return;
                        }
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

    /**
     * Applies a data-provider filter so the grid shows only features whose version is
     * selected in the MultiSelectComboBox.  When the selection is empty the filter is
     * cleared and all features are shown.
     */
    private void applyFeatureFilter() {
        if (!selectedVersions.isEmpty()) {
            Set<UUID> versionIds = selectedVersions.stream().map(Version::getId).collect(Collectors.toSet());
            getDataProvider().setFilter(f -> versionIds.contains(f.getVersionId()));
        } else {
            getDataProvider().setFilter(null);
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
            Feature             selectedFeature = event.getItem();
            Map<String, String> params          = new HashMap<>();
            params.put("product", String.valueOf(productId));
            params.put("version", String.valueOf(versionId));
            params.put("feature", String.valueOf(selectedFeature.getId()));
            UI.getCurrent().navigate(SprintListView.class, QueryParameters.simple(params));
        });

        {
            Grid.Column<Feature> keyColumn = getGrid().addColumn(Feature::getKey);
            VaadinUtil.addSimpleHeader(keyColumn, "Key", VaadinIcon.KEY);
        }
        {
            // product
            Grid.Column<Feature> productColumn = getGrid().addColumn(feature -> {
                Version v = versionMap.get(feature.getVersionId());
                if (v == null) return "";
                Product p = productMap.get(v.getProductId());
                return p != null ? p.getName() : "";
            });
            VaadinUtil.addSimpleHeader(productColumn, "Product", VaadinIcon.PACKAGE);
        }
        {
            // version
            Grid.Column<Feature> versionColumn = getGrid().addColumn(feature -> {
                Version v = versionMap.get(feature.getVersionId());
                return v != null ? v.getName() : "";
            });
            VaadinUtil.addSimpleHeader(versionColumn, "Version", VaadinIcon.COMPILE);
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

                // Use hash-based URL for proper caching; theme-aware
                boolean isDark = UI.getCurrent().getElement().getThemeList().contains(Lumo.DARK);
                avatar.setSrc(feature.getAvatarUrl(isDark));
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
        FeatureDialog dialog = new FeatureDialog(feature, avatarService, stableDiffusionService, featureApi, versionId);
        dialog.addOpenedChangeListener(event -> {
            if (!event.isOpened()) {
                // Dialog was closed, refresh the grid
                refreshGrid();
            }
        });
        dialog.open();
    }

    private void refreshGrid() {
        // Load all features and rebuild lookup maps
        List<Feature> freshFeatures = featureApi.getAll();
        allFeatures = freshFeatures;

        versionMap.clear();
        versionApi.getAll().forEach(v -> versionMap.put(v.getId(), v));

        productMap.clear();
        productApi.getAll().forEach(p -> productMap.put(p.getId(), p));

        // Replace data-provider items
        getDataProvider().getItems().clear();
        getDataProvider().getItems().addAll(freshFeatures);

        // Update ComboBox items while preserving / establishing selection
        if (versionSelector != null) {
            Set<Version> currentSelection = versionSelector.getValue();
            isRestoringFromUrl = true;
            versionSelector.setItems(new ArrayList<>(versionMap.values()));
            if (!currentSelection.isEmpty()) {
                // Preserve the previously selected versions (match by ID)
                Set<UUID> selectedIds = currentSelection.stream().map(Version::getId).collect(Collectors.toSet());
                Set<Version> toRestore = versionMap.values().stream()
                        .filter(v -> selectedIds.contains(v.getId()))
                        .collect(Collectors.toSet());
                if (!toRestore.isEmpty()) {
                    selectedVersions = toRestore;
                    versionSelector.setValue(toRestore);
                }
            } else if (requestedVersionIds != null) {
                // URL contained a 'versions' key — it is authoritative.
                // Non-empty → restore those IDs; empty string → user explicitly cleared, leave empty.
                if (!requestedVersionIds.isEmpty()) {
                    Set<Version> toSelect = new HashSet<>();
                    for (String idStr : requestedVersionIds.split(",")) {
                        try {
                            UUID id = UUID.fromString(idStr.trim());
                            Optional.ofNullable(versionMap.get(id)).ifPresent(toSelect::add);
                        } catch (NumberFormatException e) {
                            log.warn("Invalid version ID in URL: {}", idStr);
                        }
                    }
                    if (!toSelect.isEmpty()) {
                        selectedVersions = toSelect;
                        versionSelector.setValue(toSelect);
                    }
                }
                // else: empty string → leave selector empty (applyFeatureFilter will clear the filter)
            } else if (versionId != null) {
                // Default: preselect the navigation-context version
                Optional.ofNullable(versionMap.get(versionId)).ifPresent(v -> {
                    selectedVersions = new HashSet<>(Set.of(v));
                    versionSelector.setValue(selectedVersions);
                });
            } else if (!versionMap.isEmpty()) {
                // No context ID given → select all versions
                selectedVersions = new HashSet<>(versionMap.values());
                versionSelector.setValue(selectedVersions);
            }
            isRestoringFromUrl = false;
        }

        // Apply ComboBox-driven filter
        applyFeatureFilter();

        // Force complete refresh of the grid
        getDataProvider().refreshAll();
        getGrid().getDataProvider().refreshAll();
        getUI().ifPresent(ui -> ui.push());

        // Sync header title to the current version selection
        updateHeaderForSelection();
    }

    /**
     * Updates the header title to reflect the current version-selector state.
     * <ul>
     *   <li>Exactly one version selected → show that version's name.</li>
     *   <li>Zero or multiple versions selected → show "Features".</li>
     * </ul>
     * Versions have no custom avatars, so no icon is shown in either case.
     */
    private void updateHeaderForSelection() {
        if (getHeaderPageTitle() == null) {
            return;
        }
        if (selectedVersions.size() == 1) {
            getHeaderPageTitle().setText(selectedVersions.iterator().next().getName());
        } else {
            getHeaderPageTitle().setText("Features");
        }
    }

    /**
     * Pushes the current {@code product}, {@code version}, and selected {@code versions}
     * into the URL so that the filter state survives browser refresh and can be
     * bookmarked.  No-op while restoring state from a URL to prevent feedback loops.
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
        // Always write the key — empty string signals "user intentionally cleared".
        String versionIds = selectedVersions.stream()
                .map(v -> String.valueOf(v.getId()))
                .collect(Collectors.joining(","));
        params.put("versions", versionIds);
        getUI().ifPresent(ui -> ui.navigate(FeatureListView.class, QueryParameters.simple(params)));
    }
}
