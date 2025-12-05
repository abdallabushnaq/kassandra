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

package de.bushnaq.abdalla.kassandra.ui.util;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Utility class for common Vaadin UI components and layouts
 */
public final class VaadinUtil {
    public static String DIALOG_DEFAULT_WIDTH = "480px";

    /**
     * Creates a standardized action column with edit and delete buttons for a grid
     *
     * @param <T>                    The type of the item displayed in the grid
     * @param editButtonIdPrefix     Prefix for the edit button ID (will be appended with the item identifier)
     * @param deleteButtonIdPrefix   Prefix for the delete button ID (will be appended with the item identifier)
     * @param itemIdentifierFunction Function to extract a unique identifier from the item (typically getName or getId)
     * @param editClickHandler       Handler for edit button clicks
     * @param deleteClickHandler     Handler for delete button clicks
     * @return A configured Grid.Column with the action buttons
     */
    public static <T> Grid.Column<T> addActionColumn(
            Grid<T> grid,
            String editButtonIdPrefix,
            String deleteButtonIdPrefix,
            Function<T, String> itemIdentifierFunction,
            Consumer<T> editClickHandler,
            Consumer<T> deleteClickHandler) {

        return addActionColumn(grid, editButtonIdPrefix, deleteButtonIdPrefix, itemIdentifierFunction,
                editClickHandler, deleteClickHandler, null);
    }

    /**
     * Creates a standardized action column with edit and delete buttons for a grid, with optional delete button validation
     *
     * @param <T>                    The type of the item displayed in the grid
     * @param editButtonIdPrefix     Prefix for the edit button ID (will be appended with the item identifier)
     * @param deleteButtonIdPrefix   Prefix for the delete button ID (will be appended with the item identifier)
     * @param itemIdentifierFunction Function to extract a unique identifier from the item (typically getName or getId)
     * @param editClickHandler       Handler for edit button clicks
     * @param deleteClickHandler     Handler for delete button clicks
     * @param deleteValidator        Optional validator function that returns a validation result to control delete button state
     * @return A configured Grid.Column with the action buttons
     */
    public static <T> Grid.Column<T> addActionColumn(
            Grid<T> grid,
            String editButtonIdPrefix,
            String deleteButtonIdPrefix,
            Function<T, String> itemIdentifierFunction,
            Consumer<T> editClickHandler,
            Consumer<T> deleteClickHandler,
            Function<T, DeleteValidationResult> deleteValidator) {

        return grid.addColumn(new ComponentRenderer<>(item -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setAlignItems(FlexComponent.Alignment.CENTER);
            layout.setSpacing(true);

            Button editButton = new Button(new Icon(VaadinIcon.EDIT));
            editButton.setId(editButtonIdPrefix + itemIdentifierFunction.apply(item));
            editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            editButton.addClickListener(e -> editClickHandler.accept(item));
//            editButton.getElement().setAttribute("title", "Edit");

            Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
            deleteButton.setId(deleteButtonIdPrefix + itemIdentifierFunction.apply(item));
            deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(e -> deleteClickHandler.accept(item));
//            deleteButton.getElement().setAttribute("title", "Delete");

            // Apply validation if validator is provided
            if (deleteValidator != null) {
                DeleteValidationResult validationResult = deleteValidator.apply(item);
                if (!validationResult.isValid()) {
                    deleteButton.setEnabled(false);
                    deleteButton.getElement().setAttribute("title", validationResult.getMessage());
                }
            }

            layout.add(editButton, deleteButton);
            return layout;
        })).setHeader("Actions").setFlexGrow(0).setWidth("120px");
    }

    /**
     * Applies filter headers to all columns in a grid.
     *
     * @param <T>             The type of items in the grid
     * @param grid            The grid to which filtering will be added
     * @param filterFunctions Map of column keys to filter functions
     * @param sortable        Whether the columns should be sortable
     * @return Map of column keys to their corresponding filter fields
     */
    private static <T> Map<String, TextField> addFilterHeadersToGrid(
            Grid<T> grid,
            Map<String, Function<T, String>> filterFunctions,
            boolean sortable) {

        Map<String, TextField> filterFields = new HashMap<>();

        for (Grid.Column<T> column : grid.getColumns()) {
            String key = column.getKey();
            if (filterFunctions.containsKey(key)) {
                TextField filterField = addFilterableHeader(
                        grid,
                        column,
                        column.getHeaderText(),
                        filterFunctions.get(key),
                        sortable
                );
                filterFields.put(key, filterField);
            }
        }

        return filterFields;
    }

    /**
     * Applies filter headers to all columns in a grid.
     *
     * @param <T>             The type of items in the grid
     * @param grid            The grid to which filtering will be added
     * @param filterFunctions Map of column keys to filter functions
     * @return Map of column keys to their corresponding filter fields
     */
    private static <T> Map<String, TextField> addFilterHeadersToGrid(
            Grid<T> grid,
            Map<String, Function<T, String>> filterFunctions) {
        return addFilterHeadersToGrid(grid, filterFunctions, true);
    }

    /**
     * Adds filtering capability to a grid column.
     *
     * @param <T>            The type of items in the grid
     * @param grid           The grid to which filtering will be added
     * @param column         The column to make filterable
     * @param headerText     The text to display in the column header
     * @param headerIcon     The icon to display in the column header (optional, can be null)
     * @param filterFunction The function that extracts the property to filter on from the item
     * @param sortable       Whether the column should be sortable
     * @return The filter text field for further customization if needed
     */
    private static <T> TextField addFilterableHeader(
            Grid<T> grid,
            Grid.Column<T> column,
            String headerText,
            Icon headerIcon,
            Function<T, String> filterFunction,
            boolean sortable) {

        // Create filter field
        TextField filterField = new TextField();
        filterField.setPlaceholder("Filter");
        filterField.setClearButtonVisible(true);
        filterField.setValueChangeMode(ValueChangeMode.EAGER);
        filterField.addClassName("filter-text-field");
        filterField.getStyle().set("max-width", "100%");
        filterField.getStyle().set("--lumo-contrast-10pct", "var(--lumo-shade-10pct)");

        // Add filter change listener
        filterField.addValueChangeListener(e -> {
            ListDataProvider<T> dataProvider = (ListDataProvider<T>) grid.getDataProvider();
            dataProvider.setFilter(item -> {
                String value = filterFunction.apply(item);
                return value != null &&
                        value.toLowerCase().contains(e.getValue().toLowerCase());
            });

            // Update any row counters in the UI
//            updateRowCounters(grid);
        });

        // Create column header layout
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);

        // Create header with icon and text
        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);

        if (headerIcon != null) {
            header.add(headerIcon);
        }

        Span headerLabel = new Span(headerText);
        header.add(headerLabel);

        // Add sort indicator if column is sortable
        if (sortable) {
            column.setSortable(true);
        }

        layout.add(header, filterField);
        column.setHeader(layout);

        return filterField;
    }

    /**
     * Adds filtering capability to a grid column with text header.
     *
     * @param <T>            The type of items in the grid
     * @param grid           The grid to which filtering will be added
     * @param column         The column to make filterable
     * @param headerText     The text to display in the column header
     * @param filterFunction The function that extracts the property to filter on from the item
     * @param sortable       Whether the column should be sortable
     * @return The filter text field for further customization if needed
     */
    public static <T> TextField addFilterableHeader(
            Grid<T> grid,
            Grid.Column<T> column,
            String headerText,
            Function<T, String> filterFunction,
            boolean sortable) {
        return addFilterableHeader(grid, column, headerText, (Icon) null, filterFunction, sortable);
    }

    /**
     * Adds filtering capability to a grid column with text header.
     *
     * @param <T>            The type of items in the grid
     * @param grid           The grid to which filtering will be added
     * @param column         The column to make filterable
     * @param headerText     The text to display in the column header
     * @param filterFunction The function that extracts the property to filter on from the item
     * @return The filter text field for further customization if needed
     */
    public static <T> TextField addFilterableHeader(
            Grid<T> grid,
            Grid.Column<T> column,
            String headerText,
            Function<T, String> filterFunction) {
        return addFilterableHeader(grid, column, headerText, (Icon) null, filterFunction, true);
    }

    /**
     * Adds filtering capability to a grid column with icon.
     *
     * @param <T>            The type of items in the grid
     * @param grid           The grid to which filtering will be added
     * @param column         The column to make filterable
     * @param headerText     The text to display in the column header
     * @param iconName       The Vaadin icon to display in the header
     * @param filterFunction The function that extracts the property to filter on from the item
     * @param sortable       Whether the column should be sortable
     * @return The filter text field for further customization if needed
     */
    public static <T> TextField addFilterableHeader(
            Grid<T> grid,
            Grid.Column<T> column,
            String headerText,
            VaadinIcon iconName,
            Function<T, String> filterFunction,
            boolean sortable) {
        return addFilterableHeader(grid, column, headerText, new Icon(iconName), filterFunction, sortable);
    }

    /**
     * Adds filtering capability to a grid column with icon.
     *
     * @param <T>            The type of items in the grid
     * @param grid           The grid to which filtering will be added
     * @param column         The column to make filterable
     * @param headerText     The text to display in the column header
     * @param iconName       The Vaadin icon to display in the header
     * @param filterFunction The function that extracts the property to filter on from the item
     * @return The filter text field for further customization if needed
     */
    public static <T> TextField addFilterableHeader(
            Grid<T> grid,
            Grid.Column<T> column,
            String headerText,
            VaadinIcon iconName,
            Function<T, String> filterFunction) {
        return addFilterableHeader(grid, column, headerText, new Icon(iconName), filterFunction, true);
    }

    /**
     * Adds a simple header to a grid column without filtering capability.
     *
     * @param <T>        The type of items in the grid
     * @param column     The column to set the header for
     * @param headerText The text to display in the column header
     * @param headerIcon The icon to display in the column header (optional, can be null)
     * @param sortable   Whether the column should be sortable
     */
    public static <T> void addSimpleHeader(
            Grid.Column<T> column,
            String headerText,
            Icon headerIcon,
            boolean sortable) {

        // Create column header layout
        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);
        header.setPadding(false);

        if (headerIcon != null) {
            header.add(headerIcon);
        }

        Span headerLabel = new Span(headerText);
        header.add(headerLabel);

        // Add sort capability if requested
        if (sortable) {
            column.setSortable(true);
        }

        column.setHeader(header);
    }

    /**
     * Adds a simple header to a grid column with VaadinIcon.
     */
    public static <T> void addSimpleHeader(
            Grid.Column<T> column,
            String headerText,
            VaadinIcon headerIcon,
            boolean sortable) {
        addSimpleHeader(column, headerText, new Icon(headerIcon), sortable);
    }

    /**
     * Adds a simple header to a grid column with text only.
     */
    public static <T> void addSimpleHeader(
            Grid.Column<T> column,
            String headerText,
            boolean sortable) {
        addSimpleHeader(column, headerText, (Icon) null, sortable);
    }

    /**
     * Adds a simple sortable header to a grid column with VaadinIcon.
     */
    public static <T> void addSimpleHeader(
            Grid.Column<T> column,
            String headerText,
            VaadinIcon headerIcon) {
        addSimpleHeader(column, headerText, new Icon(headerIcon), true);
    }

    /**
     * Adds a simple sortable header to a grid column with text only.
     */
    public static <T> void addSimpleHeader(
            Grid.Column<T> column,
            String headerText) {
        addSimpleHeader(column, headerText, (Icon) null, true);
    }

    /**
     * Creates a standardized dialog button layout with save and cancel buttons
     *
     * @param saveButtonText   Text for the save button (e.g., "Save", "Create", "Confirm")
     * @param saveButtonId     ID for the save button
     * @param cancelButtonText Text for the cancel button (e.g., "Cancel", "Close")
     * @param cancelButtonId   ID for the cancel button
     * @param saveClickHandler Click handler for the save button
     * @param dialog           The dialog instance that contains these buttons (for closing when cancel is clicked)
     * @return A configured HorizontalLayout containing the dialog buttons
     */
    public static HorizontalLayout createDialogButtonLayout(
            String saveButtonText,
            String saveButtonId,
            String cancelButtonText,
            String cancelButtonId,
            SaveButtonClickHandler saveClickHandler,
            Dialog dialog) {

        Button saveButton = new Button(saveButtonText, new Icon(VaadinIcon.CHECK));
        saveButton.setId(saveButtonId);
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> saveClickHandler.onClick());

        Button cancelButton = new Button(cancelButtonText, new Icon(VaadinIcon.CLOSE));
        cancelButton.setId(cancelButtonId);
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClickListener(event -> dialog.close());

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        buttonLayout.getStyle().set("margin-top", "var(--lumo-space-xl)");

        return buttonLayout;
    }

    public static HorizontalLayout createDialogHeader(String title, VaadinIcon icon) {
        return createDialogHeader(title, new Icon(icon));
    }

    public static HorizontalLayout createDialogHeader(String title, Icon icon) {
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLayout.setSpacing(true);

        H3 titleLabel = new H3(title);
        titleLabel.getStyle().set("margin", "0");

        if (icon != null) {
            icon.getStyle().set("margin-right", "0.5em");
            headerLayout.add(icon, titleLabel);
        } else {
            headerLayout.add(titleLabel);
        }

        // Add bottom margin to create space between the header and content
        headerLayout.getStyle().set("margin-bottom", "var(--lumo-space-m)");

        return headerLayout;
    }

    public static HorizontalLayout createDialogHeader(String title, Component iconComponent) {
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLayout.setSpacing(true);

        iconComponent.getElement().getStyle().set("margin-right", "0.5em");

        H3 titleLabel = new H3(title);
        titleLabel.getStyle().set("margin", "0");

        headerLayout.add(iconComponent, titleLabel);

        // Add bottom margin to create space between the header and content
        headerLayout.getStyle().set("margin-bottom", "var(--lumo-space-m)");

        return headerLayout;
    }

    public static HorizontalLayout createDialogHeader(String title, String icon) {
        return createDialogHeader(title, new Icon(icon));
    }

    /**
     * Functional interface for create button click handlers
     */
    @FunctionalInterface
    public interface CreateButtonClickHandler {
        void onClick();
    }

    /**
     * Functional interface for dialog save button click handlers
     */
    @FunctionalInterface
    public interface SaveButtonClickHandler {
        void onClick();
    }

    /**
     * Class to represent the result of delete button validation
     */
    public static class DeleteValidationResult {
        private final String  message;
        private final boolean valid;

        private DeleteValidationResult(boolean valid, String message) {
            this.valid   = valid;
            this.message = message;
        }

        /**
         * @return the message to display as tooltip for the delete button
         */
        public String getMessage() {
            return message;
        }

        /**
         * Creates an invalid result with a specified error message
         */
        public static DeleteValidationResult invalid(String message) {
            return new DeleteValidationResult(false, message);
        }

        /**
         * @return true if the delete action is valid, false otherwise
         */
        public boolean isValid() {
            return valid;
        }

        /**
         * Creates a valid result with no message
         */
        public static DeleteValidationResult valid() {
            return new DeleteValidationResult(true, "Delete");
        }
    }
}
