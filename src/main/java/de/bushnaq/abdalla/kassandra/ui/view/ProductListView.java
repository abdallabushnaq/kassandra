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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import de.bushnaq.abdalla.kassandra.ai.AiFilterService;
import de.bushnaq.abdalla.kassandra.ai.mcp.AiAssistantService;
import de.bushnaq.abdalla.kassandra.ai.mcp.api.AuthenticationProvider;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.config.DefaultEntitiesInitializer;
import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.dto.ProductAclEntry;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.rest.api.ProductAclApi;
import de.bushnaq.abdalla.kassandra.rest.api.ProductApi;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import de.bushnaq.abdalla.kassandra.rest.api.UserGroupApi;
import de.bushnaq.abdalla.kassandra.security.SecurityUtils;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.component.AbstractMainGrid;
import de.bushnaq.abdalla.kassandra.ui.component.ChatAgentPanel;
import de.bushnaq.abdalla.kassandra.ui.component.ChatPanelSessionState;
import de.bushnaq.abdalla.kassandra.ui.dialog.ConfirmDialog;
import de.bushnaq.abdalla.kassandra.ui.dialog.ProductDialog;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Route(value = "product-list", layout = MainLayout.class)
@PageTitle("Product List Page")
@Menu(order = 1, icon = "vaadin:factory", title = "Products")
@PermitAll
@RolesAllowed({"USER", "ADMIN"})
public class ProductListView extends AbstractMainGrid<Product> implements AfterNavigationObserver {
    public static final  String                 CREATE_PRODUCT_BUTTON             = "create-product-button";
    private static final String                 PARAM_AI_PANEL                    = "aiPanel";
    public static final  String                 PRODUCT_AI_PANEL_BUTTON           = "product-ai-panel-button";
    public static final  String                 PRODUCT_GLOBAL_FILTER             = "product-global-filter";
    public static final  String                 PRODUCT_GRID                      = "product-grid";
    public static final  String                 PRODUCT_GRID_ACCESS_PREFIX        = "product-grid-access-";
    public static final  String                 PRODUCT_GRID_DELETE_BUTTON_PREFIX = "product-grid-delete-button-prefix-";
    public static final  String                 PRODUCT_GRID_EDIT_BUTTON_PREFIX   = "product-grid-edit-button-prefix-";
    public static final  String                 PRODUCT_GRID_NAME_PREFIX          = "product-grid-name-";
    public static final  String                 PRODUCT_LIST_PAGE_TITLE           = "product-list-page-title";
    public static final  String                 PRODUCT_ROW_COUNTER               = "product-row-counter";
    public static final  String                 ROUTE                             = "product-list";
    private final        Button                 aiToggleButton;
    private final        SplitLayout            bodySplit;
    private final        ChatAgentPanel         chatAgentPanel;
    private              boolean                chatOpen                          = false;
    private final        Div                    chatPane;
    private final        ProductAclApi          productAclApi;
    private final        ProductApi             productApi;
    private              boolean                restoringFromUrl                  = false;
    private final        StableDiffusionService stableDiffusionService;
    private final        UserApi                userApi;
    private final        UserGroupApi           userGroupApi;

    public ProductListView(ProductApi productApi, ProductAclApi productAclApi, UserApi userApi, UserGroupApi userGroupApi,
                           Clock clock, AiFilterService aiFilterService, JsonMapper mapper, StableDiffusionService stableDiffusionService,
                           AiAssistantService aiAssistantService, AuthenticationProvider mcpAuthProvider,
                           ChatPanelSessionState chatPanelSessionState) {
        super(clock);
        this.productApi             = productApi;
        this.productAclApi          = productAclApi;
        this.userApi                = userApi;
        this.userGroupApi           = userGroupApi;
        this.stableDiffusionService = stableDiffusionService;

        // Build the smart header
        add(
                createSmartHeader(
                        "Products",
                        PRODUCT_LIST_PAGE_TITLE,
                        VaadinIcon.CUBE,
                        CREATE_PRODUCT_BUTTON,
                        () -> openProductDialog(null),
                        PRODUCT_ROW_COUNTER,
                        PRODUCT_GLOBAL_FILTER,
                        aiFilterService, mapper, "Product"
                )
        );

        // AI toggle button - appended to the header's right side
        aiToggleButton = new Button("AI");
        aiToggleButton.setId(PRODUCT_AI_PANEL_BUTTON);
        aiToggleButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        aiToggleButton.getElement().setAttribute("title", "AI Assistant");
        addHeaderButton(aiToggleButton);

        // Chat panel (session-aware: reuses conversationId + replays history on F5)
        chatAgentPanel = new ChatAgentPanel(aiAssistantService, mcpAuthProvider, userApi, chatPanelSessionState);
        chatAgentPanel.setSizeFull();

        chatPane = new Div(chatAgentPanel);
        chatPane.getStyle()
                .set("height", "100%")
                .set("overflow", "hidden")
                .set("transition", "width 0.3s ease, opacity 0.3s ease")
                .set("width", "0")
                .set("opacity", "0")
                .set("min-width", "0");

        // SplitLayout: grid (left) | chat panel (right)
        bodySplit = new SplitLayout(getGridPanelWrapper(), chatPane);
        bodySplit.setOrientation(SplitLayout.Orientation.HORIZONTAL);
        bodySplit.setSizeFull();
        bodySplit.setSplitterPosition(100);

        add(bodySplit);

        aiToggleButton.addClickListener(e -> {
            chatOpen = !chatOpen;
            applyChatPaneState(chatOpen);
            updateUrlParameters();
        });
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        getElement().getParent().getComponent()
                .ifPresent(component -> {
                    if (component instanceof MainLayout mainLayout) {
                        mainLayout.getBreadcrumbs().clear();
                        mainLayout.getBreadcrumbs().addItem("Products", ProductListView.class);
                    }
                });

        // Restore panel open/closed state from URL parameter
        restoringFromUrl = true;
        QueryParameters qp         = event.getLocation().getQueryParameters();
        boolean         shouldOpen = qp.getParameters().containsKey(PARAM_AI_PANEL);
        if (shouldOpen != chatOpen) {
            chatOpen = shouldOpen;
            applyChatPaneState(chatOpen);
        }
        restoringFromUrl = false;

        // Wire current user into the chat panel for avatar/name display
        final String userEmail  = SecurityUtils.getUserEmail();
        User         userFromDb = null;
        if (!userEmail.equals(SecurityUtils.GUEST)) {
            try {
                userFromDb = userApi.getByEmail(userEmail).get();
            } catch (Exception e) {
                // User not found - default avatar will be used
            }
        }
        chatAgentPanel.setCurrentUser(userFromDb);

        refreshGrid();
    }

    /**
     * Opens or closes the chat pane and keeps the toggle button styling in sync.
     */
    private void applyChatPaneState(boolean open) {
        if (open) {
            chatPane.getStyle()
                    .set("width", "35%")
                    .set("opacity", "1")
                    .set("min-width", "280px");
            bodySplit.setSplitterPosition(65);
            aiToggleButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        } else {
            chatPane.getStyle()
                    .set("width", "0")
                    .set("opacity", "0")
                    .set("min-width", "0");
            bodySplit.setSplitterPosition(100);
            aiToggleButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }
    }

    private void confirmDelete(Product product) {
        String message = "Are you sure you want to delete product \"" + product.getName() + "\"?";
        ConfirmDialog dialog = new ConfirmDialog(
                "Confirm Delete",
                message,
                "Delete",
                () -> {
                    productApi.deleteById(product.getId());
                    refreshGrid();
                    Notification.show("Product deleted", 3000, Notification.Position.BOTTOM_START);
                }
        );
        dialog.open();
    }

    /**
     * Creates a searchable text string from a Product for global filtering
     */
    private String getSearchableText(Product product) {
        if (product == null) return "";

        StringBuilder searchText = new StringBuilder();

        if (product.getKey() != null) {
            searchText.append(product.getKey()).append(" ");
        }

        if (product.getName() != null) {
            searchText.append(product.getName()).append(" ");
        }

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)
                .withZone(Clock.systemDefaultZone().getZone())
                .withLocale(getLocale());

        if (product.getCreated() != null) {
            searchText.append(dateTimeFormatter.format(product.getCreated())).append(" ");
        }

        if (product.getUpdated() != null) {
            searchText.append(dateTimeFormatter.format(product.getUpdated())).append(" ");
        }

        return searchText.toString().trim();
    }

    protected void initGrid(Clock clock) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).withZone(clock.getZone()).withLocale(getLocale());

        getGrid().setId(PRODUCT_GRID);

        getGrid().addItemClickListener(event -> {
            Product             selectedProduct = event.getItem();
            Map<String, String> params          = new HashMap<>();
            params.put("product", String.valueOf(selectedProduct.getId()));
            UI.getCurrent().navigate(VersionListView.class, QueryParameters.simple(params));
        });

        {
            Grid.Column<Product> keyColumn = getGrid().addColumn(Product::getKey);
            VaadinUtil.addSimpleHeader(keyColumn, "Key", VaadinIcon.KEY);
        }
        {
            Grid.Column<Product> avatarColumn = getGrid().addColumn(new ComponentRenderer<>(product -> {
                com.vaadin.flow.component.html.Image avatar = new com.vaadin.flow.component.html.Image();
                avatar.setWidth("24px");
                avatar.setHeight("24px");
                avatar.getStyle()
                        .set("border-radius", "var(--lumo-border-radius)")
                        .set("object-fit", "cover")
                        .set("display", "block")
                        .set("margin", "0")
                        .set("padding", "0");
                avatar.setSrc(product.getAvatarUrl());
                avatar.setAlt(product.getName());
                return avatar;
            }));
            avatarColumn.setWidth("48px");
            avatarColumn.setFlexGrow(0);
            avatarColumn.setHeader("");
        }
        {
            Grid.Column<Product> nameColumn = getGrid().addColumn(new ComponentRenderer<>(product -> {
                Div div = new Div();
                div.add(product.getName());
                div.setId(PRODUCT_GRID_NAME_PREFIX + product.getName());
                return div;
            }));
            nameColumn.setComparator((product1, product2) ->
                    product1.getName().compareToIgnoreCase(product2.getName()));
            VaadinUtil.addSimpleHeader(nameColumn, "Name", VaadinIcon.CUBE);
        }
        {
            Grid.Column<Product> aclColumn = getGrid().addColumn(new ComponentRenderer<>(product -> {
                List<ProductAclEntry> aclEntries = productAclApi.getAcl(product.getId());

                if (aclEntries.isEmpty()) {
                    Span badge = new Span("Owner only");
                    badge.getStyle()
                            .set("display", "inline-block")
                            .set("padding", "2px 8px")
                            .set("border-radius", "12px")
                            .set("background-color", "var(--lumo-contrast-10pct)")
                            .set("font-size", "var(--lumo-font-size-s)")
                            .set("color", "var(--lumo-secondary-text-color)");
                    return badge;
                }

                long userCount  = aclEntries.stream().filter(ProductAclEntry::isUserEntry).count();
                long groupCount = aclEntries.stream().filter(ProductAclEntry::isGroupEntry).count();

                HorizontalLayout layout = new HorizontalLayout();
                layout.setId(PRODUCT_GRID_ACCESS_PREFIX + product.getName());
                layout.setSpacing(true);
                layout.setPadding(false);
                layout.getStyle().set("flex-wrap", "wrap");

                if (userCount > 0) {
                    Span userBadge = new Span(userCount + " " + (userCount == 1 ? "user" : "users"));
                    userBadge.getElement().getThemeList().add("badge");
                    userBadge.getStyle()
                            .set("display", "inline-block")
                            .set("padding", "2px 8px")
                            .set("border-radius", "12px")
                            .set("background-color", "var(--lumo-primary-color-10pct)")
                            .set("color", "var(--lumo-primary-text-color)")
                            .set("font-size", "var(--lumo-font-size-s)")
                            .set("font-weight", "500");
                    layout.add(userBadge);
                }

                if (groupCount > 0) {
                    Span groupBadge = new Span(groupCount + " " + (groupCount == 1 ? "group" : "groups"));
                    groupBadge.getElement().getThemeList().add("badge");
                    groupBadge.getStyle()
                            .set("display", "inline-block")
                            .set("padding", "2px 8px")
                            .set("border-radius", "12px")
                            .set("background-color", "var(--lumo-success-color-10pct)")
                            .set("color", "var(--lumo-success-text-color)")
                            .set("font-size", "var(--lumo-font-size-s)")
                            .set("font-weight", "500");
                    layout.add(groupBadge);
                }

                return layout;
            }));
            VaadinUtil.addSimpleHeader(aclColumn, "Access", VaadinIcon.LOCK);
        }
        {
            Grid.Column<Product> createdColumn = getGrid().addColumn(product -> dateTimeFormatter.format(product.getCreated()));
            VaadinUtil.addSimpleHeader(createdColumn, "Created", VaadinIcon.CALENDAR);
        }
        {
            Grid.Column<Product> updatedColumn = getGrid().addColumn(product -> dateTimeFormatter.format(product.getUpdated()));
            VaadinUtil.addSimpleHeader(updatedColumn, "Updated", VaadinIcon.CALENDAR);
        }
        VaadinUtil.addActionColumn(
                getGrid(),
                PRODUCT_GRID_EDIT_BUTTON_PREFIX,
                PRODUCT_GRID_DELETE_BUTTON_PREFIX,
                Product::getName,
                this::openProductDialog,
                this::confirmDelete
        );
    }

    private void openProductDialog(Product product) {
        ProductDialog dialog = new ProductDialog(product, stableDiffusionService, productApi, productAclApi, userApi, userGroupApi);
        dialog.addOpenedChangeListener(event -> {
            if (!event.isOpened()) {
                refreshGrid();
            }
        });
        dialog.open();
    }

    private void refreshGrid() {
        getDataProvider().getItems().clear();
        productApi.getAll().stream()
                .filter(p -> !DefaultEntitiesInitializer.DEFAULT_NAME.equals(p.getName()))
                .forEach(p -> getDataProvider().getItems().add(p));
        getDataProvider().refreshAll();
        getGrid().getDataProvider().refreshAll();
        getUI().ifPresent(ui -> ui.push());
    }

    /**
     * Pushes the current panel-open state into the URL so F5 restores it.
     */
    private void updateUrlParameters() {
        if (restoringFromUrl) return;
        Map<String, String> params = new HashMap<>();
        if (chatOpen) {
            params.put(PARAM_AI_PANEL, "open");
        }
        QueryParameters qp = QueryParameters.simple(params);
        getUI().ifPresent(ui -> ui.navigate(ProductListView.class, qp));
    }
}
