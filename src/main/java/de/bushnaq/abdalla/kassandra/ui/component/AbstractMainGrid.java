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

package de.bushnaq.abdalla.kassandra.ui.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.bushnaq.abdalla.kassandra.ai.filter.AiFilterService;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;
import lombok.AccessLevel;
import lombok.Getter;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.ArrayList;

@Getter
public abstract class AbstractMainGrid<T> extends Main {
    private       String              darkHeaderBackgroundUrl;
    private final ListDataProvider<T> dataProvider;
    private final Grid<T>             grid;
    private final VerticalLayout      gridPanelWrapper;
    @Getter(AccessLevel.NONE)
    private final VerticalLayout      pageContent;
    /**
     * Reference to the {@code H2} title element of the most recently built smart header.
     * Subclasses may call {@code getHeaderPageTitle()} to update the title text dynamically.
     */
    private       H2                  headerPageTitle;
    private       String              lightHeaderBackgroundUrl;
    private       Registration        rowCounterRegistration;
    private       SmartHeader         smartHeader;
    /**
     * Registration for the {@link ThemeChangedEvent} listener; removed in {@link #onDetach}.
     */
    private       Registration        themeChangedRegistration;

    public AbstractMainGrid(Clock clock) {
        setClassName("grid-wrapper");
        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        this.getStyle().set("padding-left", "var(--lumo-space-m)");
        this.getStyle().set("padding-right", "var(--lumo-space-m)");

        pageContent = new VerticalLayout();
        pageContent.setPadding(false);
        pageContent.setSpacing(false);
        pageContent.setWidthFull();
        pageContent.getStyle().set("min-height", "0");
        Scroller pageScroller = new Scroller(pageContent);
        pageScroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        pageScroller.setSizeFull();
        pageScroller.getStyle().set("min-height", "0");
        super.add(pageScroller);

        grid         = new Grid<>();
        dataProvider = new ListDataProvider<T>(new ArrayList<>());
        grid.setDataProvider(dataProvider);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);
        grid.setWidthFull();
        grid.setAllRowsVisible(true);
//        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        gridPanelWrapper = new VerticalLayout();
        gridPanelWrapper.setPadding(false);
        gridPanelWrapper.setSpacing(false);
        gridPanelWrapper.setWidthFull();
        gridPanelWrapper.addClassName("grid-panel-wrapper");

        var innerWrapper = new VerticalLayout();
        innerWrapper.setPadding(false);
        innerWrapper.setSpacing(false);
        gridPanelWrapper.add(innerWrapper);

        var gridPanel = new VerticalLayout(grid);
        gridPanel.setPadding(false);
        gridPanel.setSpacing(false);
        gridPanel.setWidthFull();
        gridPanel.addClassName("grid-panel");

        innerWrapper.add(gridPanel);

        initGrid(clock);
    }

    /**
     * Adds view content inside the shared page scroller.
     *
     * @param components the components to add
     */
    @Override
    public void add(Component... components) {
        pageContent.add(components);
    }

    /**
     * Removes view content from inside the shared page scroller.
     *
     * @param components the components to remove
     */
    @Override
    public void remove(Component... components) {
        pageContent.remove(components);
    }

    /**
     * Creates a standardized header layout with a title, icon, smart global filter, and create button.
     * The smart filter supports both simple text search and natural language queries.
     *
     * @param title                    The title text to display
     * @param titleId                  ID for the title component
     * @param titleIcon                Icon to display next to the title
     * @param createButtonId           ID for the create button
     * @param createButtonClickHandler Click handler for the create button
     * @param grid                     Optional grid to count rows from (can be null)
     * @param rowCounterId             Optional ID for row counter (can be null if grid is null)
     * @param globalFilterId           ID for the global filter field
     * @param aiFilterService          Natural language search service
     * @param mapper                   JsonMapper for JSON serialization
     * @param entityType               The type of entity being searched (e.g., "Product", "Version")
     * @return the configured shared header
     */
    private <T> SmartHeader createSmartHeader(
            String title,
            String titleId,
            Component titleIcon,
            String createButtonId,
            VaadinUtil.CreateButtonClickHandler createButtonClickHandler,
            Grid<T> grid,
            String rowCounterId,
            String globalFilterId,
            AiFilterService aiFilterService,
            JsonMapper mapper,
            String entityType) {

        SmartHeader headerLayout = new SmartHeader(title, titleId, titleIcon);
        headerLayout.setFallbackIcon(switch (entityType) {
            case "Product" -> VaadinIcon.FACTORY;
            case "Version" -> VaadinIcon.ARCHIVE;
            case "Feature" -> VaadinIcon.LIGHTBULB;
            case "Sprint" -> VaadinIcon.CALENDAR;
            default -> VaadinIcon.GRID;
        });
        headerLayout.setId("page-layout");
        smartHeader     = headerLayout;
        headerPageTitle = headerLayout.getPageTitle();

        // Add smart global filter to the left side so filters stay with the title.
        if (globalFilterId != null && grid != null) {
            GlobalAiFilter<T> smartFilter =
                    new GlobalAiFilter<>(
                            globalFilterId,
                            grid,
                            aiFilterService,
                            mapper,
                            entityType
                    );
            headerLayout.setSearch(smartFilter);
        }

        if (rowCounterRegistration != null) {
            rowCounterRegistration.remove();
            rowCounterRegistration = null;
        }
        // Add row counter to the left side before the action controls.
        if (grid != null && rowCounterId != null) {
            Span rowCounter = new Span();
            rowCounter.setId(rowCounterId);
            rowCounter.getStyle().set("color", "var(--lumo-secondary-text-color)");

            // Initial counter update
            updateRowCounter(grid, rowCounter);

            // Register a ComponentEventListener for grid's data changes, will be triggered when filtering
            rowCounterRegistration = grid.getDataProvider().addDataProviderListener(event -> {
                updateRowCounter(grid, rowCounter);
            });

            headerLayout.setSummary(rowCounter);
        }

        Button createButton = new Button("Create", new Icon(VaadinIcon.PLUS));
        createButton.setId(createButtonId);
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        createButton.addClickListener(e -> createButtonClickHandler.onClick());

        headerLayout.addActions(createButton);
        return headerLayout;
    }

    /**
     * Creates a shared header for the supplied grid.
     *
     * @param <T>                      grid item type
     * @param title                    page title
     * @param titleId                  title identifier
     * @param titleIcon                page icon
     * @param createButtonId           create action identifier
     * @param createButtonClickHandler create action
     * @param grid                     grid to filter and count
     * @param rowCounterId             counter identifier
     * @param globalFilterId           search identifier
     * @param aiFilterService          natural-language filter service
     * @param mapper                   JSON mapper
     * @param entityType               searchable entity type
     * @return configured shared header
     */
    public <T> SmartHeader createSmartHeader(
            String title,
            String titleId,
            VaadinIcon titleIcon,
            String createButtonId,
            VaadinUtil.CreateButtonClickHandler createButtonClickHandler,
            Grid<T> grid,
            String rowCounterId,
            String globalFilterId,
            AiFilterService aiFilterService,
            JsonMapper mapper,
            String entityType) {
        return createSmartHeader(title, titleId, new Icon(titleIcon), createButtonId, createButtonClickHandler, grid, rowCounterId, globalFilterId, aiFilterService, mapper, entityType);
    }

    /**
     * Creates a shared header for this view's grid.
     *
     * @param title                    page title
     * @param titleId                  title identifier
     * @param titleIcon                page icon
     * @param createButtonId           create action identifier
     * @param createButtonClickHandler create action
     * @param rowCounterId             counter identifier
     * @param globalFilterId           search identifier
     * @param aiFilterService          natural-language filter service
     * @param mapper                   JSON mapper
     * @param entityType               searchable entity type
     * @return configured shared header
     */
    public SmartHeader createSmartHeader(
            String title,
            String titleId,
            VaadinIcon titleIcon,
            String createButtonId,
            VaadinUtil.CreateButtonClickHandler createButtonClickHandler,
            String rowCounterId,
            String globalFilterId,
            AiFilterService aiFilterService,
            JsonMapper mapper,
            String entityType) {
        return createSmartHeader(title, titleId, new Icon(titleIcon), createButtonId, createButtonClickHandler, grid, rowCounterId, globalFilterId, aiFilterService, mapper, entityType);
    }

    /**
     * Creates a shared header with an entity avatar for this view's grid.
     *
     * @param title                    page title
     * @param titleId                  title identifier
     * @param image                    entity avatar, or null
     * @param createButtonId           create action identifier
     * @param createButtonClickHandler create action
     * @param rowCounterId             counter identifier
     * @param globalFilterId           search identifier
     * @param aiFilterService          natural-language filter service
     * @param mapper                   JSON mapper
     * @param entityType               searchable entity type
     * @return configured shared header
     */
    public SmartHeader createSmartHeader(
            String title,
            String titleId,
            Image image,
            String createButtonId,
            VaadinUtil.CreateButtonClickHandler createButtonClickHandler,
            String rowCounterId,
            String globalFilterId,
            AiFilterService aiFilterService,
            JsonMapper mapper,
            String entityType) {
        return createSmartHeader(title, titleId, image, createButtonId, createButtonClickHandler, grid, rowCounterId, globalFilterId, aiFilterService, mapper, entityType);
    }

    protected abstract void initGrid(Clock clock);

    /**
     * Subscribes to {@link ThemeChangedEvent} so that the grid's component renderers
     * are re-evaluated with the new theme (e.g. to swap light/dark avatar URLs).
     *
     * @param attachEvent the attach event
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        themeChangedRegistration = ComponentUtil.addListener(
                attachEvent.getUI(), ThemeChangedEvent.class,
                e -> {
                    getDataProvider().refreshAll();
                });
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
     * Set the theme-specific background images of the current smart header.
     *
     * @param lightUrl Cache-busted light header URL, or null to remove the background
     * @param darkUrl  Cache-busted dark header URL, or null to remove the background
     */
    public void setSmartHeaderBackgroundUrls(String lightUrl, String darkUrl) {
        lightHeaderBackgroundUrl = lightUrl;
        darkHeaderBackgroundUrl  = darkUrl;
        MainLayout.findParent(this)
                .filter(MainLayout.class::isInstance)
                .map(MainLayout.class::cast)
                .ifPresent(mainLayout -> mainLayout.setHeaderBackgroundUrls(lightUrl, darkUrl));
    }

    /**
     * Updates a row counter component with the current visible row count vs total rows
     *
     * @param <T>        The type of items in the grid
     * @param grid       The grid to count rows from
     * @param rowCounter The Span component to update with the row count
     */
    private static <T> void updateRowCounter(Grid<T> grid, Span rowCounter) {
        DataProvider<T, ?> dataProvider = grid.getDataProvider();

        // Get counts based on the type of DataProvider
        int totalSize;
        int filteredSize;

        // Handle ListDataProvider
        ListDataProvider<T> listProvider = (ListDataProvider<T>) dataProvider;
        totalSize = listProvider.getItems().size();

        // Handle the case when no filter is applied yet (filter is null)
        if (listProvider.getFilter() == null) {
            filteredSize = totalSize;
        } else {
            filteredSize = listProvider.getItems().stream()
                    .filter(item -> listProvider.getFilter().test(item))
                    .toList().size();
        }

        String text = String.format("Showing %d of %d rows", filteredSize, totalSize);
        rowCounter.setText(text);
    }

}
