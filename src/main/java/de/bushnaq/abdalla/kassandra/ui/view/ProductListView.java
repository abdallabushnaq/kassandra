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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import de.bushnaq.abdalla.kassandra.ai.AiFilterService;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.rest.api.ProductApi;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.component.AbstractMainGrid;
import de.bushnaq.abdalla.kassandra.ui.dialog.ConfirmDialog;
import de.bushnaq.abdalla.kassandra.ui.dialog.ProductDialog;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.Map;

@Route("product-list")
@PageTitle("Product List Page")
@Menu(order = 1, icon = "vaadin:factory", title = "Products")
@PermitAll
@RolesAllowed({"USER", "ADMIN"})
public class ProductListView extends AbstractMainGrid<Product> implements AfterNavigationObserver {
    public static final String                 CREATE_PRODUCT_BUTTON             = "create-product-button";
    public static final String                 PRODUCT_GLOBAL_FILTER             = "product-global-filter";
    public static final String                 PRODUCT_GRID                      = "product-grid";
    public static final String                 PRODUCT_GRID_DELETE_BUTTON_PREFIX = "product-grid-delete-button-prefix-";
    public static final String                 PRODUCT_GRID_EDIT_BUTTON_PREFIX   = "product-grid-edit-button-prefix-";
    public static final String                 PRODUCT_GRID_NAME_PREFIX          = "product-grid-name-";
    public static final String                 PRODUCT_LIST_PAGE_TITLE           = "product-list-page-title";
    public static final String                 PRODUCT_ROW_COUNTER               = "product-row-counter";
    public static final String                 ROUTE                             = "product-list";
    private final       ProductApi             productApi;
    private final       StableDiffusionService stableDiffusionService;

    public ProductListView(ProductApi productApi, Clock clock, AiFilterService aiFilterService, ObjectMapper mapper, StableDiffusionService stableDiffusionService) {
        super(clock);
        this.productApi             = productApi;
        this.stableDiffusionService = stableDiffusionService;

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
                ),
                getGridPanelWrapper()
        );
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
        refreshGrid();
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

        // Add key
        if (product.getKey() != null) {
            searchText.append(product.getKey()).append(" ");
        }

        // Add name
        if (product.getName() != null) {
            searchText.append(product.getName()).append(" ");
        }

        // Add formatted dates
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

        // Add click listener to navigate to VersionView with the selected product ID
        getGrid().addItemClickListener(event -> {
            Product selectedProduct = event.getItem();
            // Create parameters map
            Map<String, String> params = new HashMap<>();
            params.put("product", String.valueOf(selectedProduct.getId()));
            // Navigate with query parameters
            UI.getCurrent().navigate(VersionListView.class, QueryParameters.simple(params));
        });

        {
            Grid.Column<Product> keyColumn = getGrid().addColumn(Product::getKey);
            VaadinUtil.addSimpleHeader(keyColumn, "Key", VaadinIcon.KEY);
        }
        {
            // Add avatar image column
            Grid.Column<Product> avatarColumn = getGrid().addColumn(new ComponentRenderer<>(product -> {
//                if (product.getAvatarPrompt() != null && !product.getAvatarPrompt().isEmpty())
                {
                    // Product has a custom image - use URL-based loading
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
                    avatar.setSrc(product.getAvatarUrl());
                    avatar.setAlt(product.getName());
                    return avatar;
                }
//                else {
//                    // No custom image - show default VaadinIcon
//                    Icon defaultIcon = new Icon(VaadinIcon.CUBE);
//                    defaultIcon.setSize("20px");
//                    defaultIcon.getStyle()
//                            .set("color", "var(--lumo-contrast-50pct)")
//                            .set("padding", "0")
//                            .set("margin", "0")
//                            .set("display", "block");
//                    return defaultIcon;
//                }
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

            // Configure a custom comparator to properly sort by the name property
            nameColumn.setComparator((product1, product2) ->
                    product1.getName().compareToIgnoreCase(product2.getName()));

            VaadinUtil.addSimpleHeader(nameColumn, "Name", VaadinIcon.CUBE);
        }
        {
            Grid.Column<Product> createdColumn = getGrid().addColumn(product -> dateTimeFormatter.format(product.getCreated()));
            VaadinUtil.addSimpleHeader(createdColumn, "Created", VaadinIcon.CALENDAR);
        }
        {
            Grid.Column<Product> updatedColumn = getGrid().addColumn(product -> dateTimeFormatter.format(product.getUpdated()));
            VaadinUtil.addSimpleHeader(updatedColumn, "Updated", VaadinIcon.CALENDAR);
        }
        // Add actions column using VaadinUtil
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
        ProductDialog dialog = new ProductDialog(product, stableDiffusionService, productApi);
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
        getDataProvider().getItems().addAll(productApi.getAll());

        // Force complete refresh of the grid
        getDataProvider().refreshAll();

        // Force the grid to re-render
        getGrid().getDataProvider().refreshAll();

        // Push UI updates if in push mode
        getUI().ifPresent(ui -> ui.push());
    }
}
