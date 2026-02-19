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
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import de.bushnaq.abdalla.kassandra.ai.AiFilterService;
import de.bushnaq.abdalla.kassandra.ai.mcp.AiAssistantService;
import de.bushnaq.abdalla.kassandra.ai.mcp.api.AuthenticationProvider;
import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.Version;
import de.bushnaq.abdalla.kassandra.rest.api.ProductApi;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import de.bushnaq.abdalla.kassandra.rest.api.VersionApi;
import de.bushnaq.abdalla.kassandra.security.SecurityUtils;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.component.AbstractMainGrid;
import de.bushnaq.abdalla.kassandra.ui.component.ChatAgentPanel;
import de.bushnaq.abdalla.kassandra.ui.component.ChatPanelSessionState;
import de.bushnaq.abdalla.kassandra.ui.dialog.ConfirmDialog;
import de.bushnaq.abdalla.kassandra.ui.dialog.VersionDialog;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;
import jakarta.annotation.security.PermitAll;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.Map;


@Route(value = "version-list", layout = MainLayout.class)
@PageTitle("Version List Page")
@PermitAll // When security is enabled, allow all authenticated users
public class VersionListView extends AbstractMainGrid<Version> implements AfterNavigationObserver {
    public static final  String                              CREATE_VERSION_BUTTON             = "create-version-button";
    private static final String                              PARAM_AI_PANEL                    = "aiPanel";
    public static final  String                              ROUTE                             = "version-list";
    public static final  String                              VERSION_AI_PANEL_BUTTON           = "version-ai-panel-button";
    public static final  String                              VERSION_GLOBAL_FILTER             = "version-global-filter";
    public static final  String                              VERSION_GRID                      = "version-grid";
    public static final  String                              VERSION_GRID_DELETE_BUTTON_PREFIX = "version-grid-delete-button-prefix-";
    public static final  String                              VERSION_GRID_EDIT_BUTTON_PREFIX   = "version-grid-edit-button-prefix-";
    public static final  String                              VERSION_GRID_NAME_PREFIX          = "version-grid-name-";
    public static final  String                              VERSION_LIST_PAGE_TITLE           = "version-list-page-title";
    public static final  String                              VERSION_ROW_COUNTER               = "version-row-counter";
    private final        AiAssistantService                  aiAssistantService;
    private final        AiFilterService                     aiFilterService;
    private final        Button                              aiToggleButton;
    private final        SplitLayout                         bodySplit;
    private final        ChatAgentPanel                      chatAgentPanel;
    private              boolean                             chatOpen                          = false;
    private final        Div                                 chatPane;
    private              com.vaadin.flow.component.Component headerComponent;
    private final        JsonMapper                          mapper;
    private final        AuthenticationProvider              mcpAuthProvider;
    private final        ProductApi                          productApi;
    private              Long                                productId;
    private              boolean                             restoringFromUrl                  = false;
    private final        UserApi                             userApi;
    private final        VersionApi                          versionApi;

    public VersionListView(VersionApi versionApi, ProductApi productApi, UserApi userApi, Clock clock,
                           AiFilterService aiFilterService, JsonMapper mapper,
                           AiAssistantService aiAssistantService, AuthenticationProvider mcpAuthProvider,
                           ChatPanelSessionState chatPanelSessionState) {
        super(clock);
        this.versionApi         = versionApi;
        this.productApi         = productApi;
        this.userApi            = userApi;
        this.aiFilterService    = aiFilterService;
        this.mapper             = mapper;
        this.aiAssistantService = aiAssistantService;
        this.mcpAuthProvider    = mcpAuthProvider;

        // AI toggle button — added to header later in addHeader() once we have the product
        aiToggleButton = new Button("AI");
        aiToggleButton.setId(VERSION_AI_PANEL_BUTTON);
        aiToggleButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        aiToggleButton.getElement().setAttribute("title", "AI Assistant");

        chatAgentPanel = new ChatAgentPanel(aiAssistantService, mcpAuthProvider, userApi, chatPanelSessionState);
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

        aiToggleButton.addClickListener(e -> {
            chatOpen = !chatOpen;
            applyChatPaneState(chatOpen);
            updateUrlParameters();
        });
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        Location        location        = event.getLocation();
        QueryParameters queryParameters = location.getQueryParameters();
        if (queryParameters.getParameters().containsKey("product")) {
            this.productId = Long.parseLong(queryParameters.getParameters().get("product").getFirst());
        }

        getElement().getParent().getComponent()
                .ifPresent(component -> {
                    if (component instanceof MainLayout mainLayout) {
                        mainLayout.getBreadcrumbs().clear();
                        Product product = productApi.getById(productId);
                        mainLayout.getBreadcrumbs().addItem("Products (" + product.getName() + ")", ProductListView.class);
                        Map<String, String> params = new HashMap<>();
                        params.put("product", String.valueOf(productId));
                        mainLayout.getBreadcrumbs().addItem("Versions", VersionListView.class, params);

                        // Build header now that we have the product — remove previous one if navigating back
                        if (headerComponent != null) remove(headerComponent);
                        remove(bodySplit);

                        Image avatar = new Image();
                        avatar.setWidth("32px");
                        avatar.setHeight("32px");
                        avatar.getStyle()
                                .set("border-radius", "var(--lumo-border-radius)")
                                .set("object-fit", "cover")
                                .set("display", "inline-block")
                                .set("margin-right", "12px");
                        if (product.getAvatarHash() != null && !product.getAvatarHash().isEmpty()) {
                            avatar.setSrc(product.getAvatarUrl());
                        }
                        headerComponent = createSmartHeader(
                                product.getName(), VERSION_LIST_PAGE_TITLE, avatar,
                                CREATE_VERSION_BUTTON, () -> openVersionDialog(null),
                                VERSION_ROW_COUNTER, VERSION_GLOBAL_FILTER,
                                aiFilterService, mapper, "Version");
                        add(headerComponent);
                        addHeaderButton(aiToggleButton);
                        add(bodySplit);
                    }
                });

        restoringFromUrl = true;
        boolean shouldOpen = queryParameters.getParameters().containsKey(PARAM_AI_PANEL);
        if (shouldOpen != chatOpen) {
            chatOpen = shouldOpen;
            applyChatPaneState(chatOpen);
        }
        restoringFromUrl = false;

        final String userEmail  = SecurityUtils.getUserEmail();
        User         userFromDb = null;
        if (!userEmail.equals(SecurityUtils.GUEST)) {
            try {
                userFromDb = userApi.getByEmail(userEmail).get();
            } catch (Exception ignored) {
            }
        }
        chatAgentPanel.setCurrentUser(userFromDb);

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

    private void confirmDelete(Version version) {
        String message = "Are you sure you want to delete version \"" + version.getName() + "\"?";
        ConfirmDialog dialog = new ConfirmDialog(
                "Confirm Delete",
                message,
                "Delete",
                () -> {
                    versionApi.deleteById(version.getId());
                    refreshGrid();
                    Notification.show("Version deleted", 3000, Notification.Position.BOTTOM_START);
                }
        );
        dialog.open();
    }

    /**
     * Creates a searchable text string from a Version for global filtering
     */
    private String getSearchableText(Version version) {
        if (version == null) return "";

        StringBuilder searchText = new StringBuilder();

        // Add key
        if (version.getKey() != null) {
            searchText.append(version.getKey()).append(" ");
        }

        // Add name
        if (version.getName() != null) {
            searchText.append(version.getName()).append(" ");
        }

        // Add formatted dates
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)
                .withZone(Clock.systemDefaultZone().getZone())
                .withLocale(getLocale());

        if (version.getCreated() != null) {
            searchText.append(dateTimeFormatter.format(version.getCreated())).append(" ");
        }

        if (version.getUpdated() != null) {
            searchText.append(dateTimeFormatter.format(version.getUpdated())).append(" ");
        }

        return searchText.toString().trim();
    }

    protected void initGrid(Clock clock) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).withZone(clock.getZone()).withLocale(getLocale());

        getGrid().setId(VERSION_GRID);

        // Add click listener to navigate to FeatureListView with the selected version ID
        getGrid().addItemClickListener(event -> {
            Version selectedVersion = event.getItem();
            // Create parameters map
            Map<String, String> params = new HashMap<>();
            params.put("product", String.valueOf(productId));
            params.put("version", String.valueOf(selectedVersion.getId()));
            // Navigate with query parameters
            UI.getCurrent().navigate(
                    FeatureListView.class,
                    QueryParameters.simple(params)
            );
        });

        {
            Grid.Column<Version> keyColumn = getGrid().addColumn(Version::getKey);
            VaadinUtil.addSimpleHeader(keyColumn, "Key", VaadinIcon.KEY);
        }
        {
            // Add name column with filtering and sorting
            Grid.Column<Version> nameColumn = getGrid().addColumn(new ComponentRenderer<>(version -> {
                Div div = new Div();
                div.add(version.getName());
                div.setId(VERSION_GRID_NAME_PREFIX + version.getName());
                return div;
            }));

            // Configure a custom comparator to properly sort by the name property
            nameColumn.setComparator((version1, version2) -> version1.getName().compareToIgnoreCase(version2.getName()));

            VaadinUtil.addSimpleHeader(nameColumn, "Name", VaadinIcon.TAG);
        }
        {
            Grid.Column<Version> createdColumn = getGrid().addColumn(version -> dateTimeFormatter.format(version.getCreated()));
            VaadinUtil.addSimpleHeader(createdColumn, "Created", VaadinIcon.CALENDAR);
        }
        {
            Grid.Column<Version> updatedColumn = getGrid().addColumn(version -> dateTimeFormatter.format(version.getUpdated()));
            VaadinUtil.addSimpleHeader(updatedColumn, "Updated", VaadinIcon.CALENDAR);
        }

        // Add actions column using VaadinUtil
        VaadinUtil.addActionColumn(
                getGrid(),
                VERSION_GRID_EDIT_BUTTON_PREFIX,
                VERSION_GRID_DELETE_BUTTON_PREFIX,
                Version::getName,
                this::openVersionDialog,
                this::confirmDelete
        );

    }

    private void openVersionDialog(Version version) {
        VersionDialog dialog = new VersionDialog(version, (savedVersion, versionDialog) -> {
            try {
                if (version != null) {
                    // Edit mode
                    versionApi.update(savedVersion);
                    Notification.show("Version updated", 3000, Notification.Position.BOTTOM_START);
                    versionDialog.close();
                } else {
                    // Create mode
                    savedVersion.setProductId(productId);
                    versionApi.persist(savedVersion);
                    Notification.show("Version created", 3000, Notification.Position.BOTTOM_START);
                    versionDialog.close();
                }
                refreshGrid();
            } catch (Exception e) {
                // Handle both field-specific and dialog-level errors
                VaadinUtil.handleApiException(e, "name", versionDialog::setNameFieldError, versionDialog::showDialogError);
            }
        });
        dialog.open();
    }

    private void refreshGrid() {
        getDataProvider().getItems().clear();
        getDataProvider().getItems().addAll((productId != null) ? versionApi.getAll(productId) : versionApi.getAll());
        getDataProvider().refreshAll();
    }

    private void updateUrlParameters() {
        if (restoringFromUrl) return;
        Map<String, String> params = new HashMap<>();
        if (productId != null) params.put("product", String.valueOf(productId));
        if (chatOpen) params.put(PARAM_AI_PANEL, "open");
        getUI().ifPresent(ui -> ui.navigate(VersionListView.class, QueryParameters.simple(params)));
    }
}
