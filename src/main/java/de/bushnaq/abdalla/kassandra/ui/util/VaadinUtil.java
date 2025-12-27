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
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import de.bushnaq.abdalla.kassandra.dto.Feature;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.TaskStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
     * Creates a standardized dialog button layout with Save and Cancel buttons
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
        return createDialogButtonLayout(saveButtonText, saveButtonId, cancelButtonText, cancelButtonId, saveClickHandler, dialog, null);
    }

    /**
     * Creates a standardized dialog button layout with Save and Cancel buttons, with validation support
     *
     * @param saveButtonText   Text for the save button (e.g., "Save", "Create", "Confirm")
     * @param saveButtonId     ID for the save button
     * @param cancelButtonText Text for the cancel button (e.g., "Cancel", "Close")
     * @param cancelButtonId   ID for the cancel button
     * @param saveClickHandler Click handler for the save button
     * @param dialog           The dialog instance that contains these buttons (for closing when cancel is clicked)
     * @param binder           Optional binder to control the save button's enabled state based on validation
     * @return A configured HorizontalLayout containing the dialog buttons
     */
    public static HorizontalLayout createDialogButtonLayout(
            String saveButtonText,
            String saveButtonId,
            String cancelButtonText,
            String cancelButtonId,
            SaveButtonClickHandler saveClickHandler,
            Dialog dialog,
            Binder<?> binder) {

        Button saveButton = new Button(saveButtonText, new Icon(VaadinIcon.CHECK));
        saveButton.setId(saveButtonId);
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> saveClickHandler.onClick());

        if (binder != null) {
            saveButton.setEnabled(binder.isValid());
            binder.addStatusChangeListener(event -> saveButton.setEnabled(binder.isValid()));
        }

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

    /**
     * Creates a reusable error message component for dialogs to display field-independent errors.
     * This component is initially hidden and can be shown/hidden and updated with error messages.
     * Typically used for access control errors, authorization failures, or other general errors.
     *
     * @return A Span component configured as an error message display
     */
    public static Span createDialogErrorMessage() {
        Span errorMessage = new Span();
        errorMessage.getStyle()
                .set("color", "var(--lumo-error-text-color)")
                .set("background-color", "var(--lumo-error-color-10pct)")
                .set("padding", "var(--lumo-space-s)")
                .set("border-radius", "var(--lumo-border-radius)")
                .set("border", "1px solid var(--lumo-error-color-50pct)")
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "var(--lumo-space-s)");
        errorMessage.setVisible(false);
        errorMessage.setWidthFull();
        return errorMessage;
    }

    public static HorizontalLayout createDialogHeader(String title, VaadinIcon icon) {
        return createDialogHeader(title, null, new Icon(icon));
    }

    public static HorizontalLayout createDialogHeader(String title, String id, VaadinIcon icon) {
        return createDialogHeader(title, id, new Icon(icon));
    }

    public static HorizontalLayout createDialogHeader(String title, String id, Icon icon) {
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLayout.setSpacing(true);

        H3 titleLabel = new H3(title);
        if (id != null)
            titleLabel.setId(id);
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
        return createDialogHeader(title, null, new Icon(icon));
    }

    /**
     * Generates a unique lane ID for Selenium testing based on a Feature and TaskStatus enum.
     * Type-safe method that accepts a Feature object and TaskStatus enum directly.
     *
     * @param feature The Feature object
     * @param status  The TaskStatus enum value
     * @return A lane ID in the format: feature-name-with-hyphens-STATUS
     */
    public static String generateFeatureLaneId(Feature feature, TaskStatus status) {
        return generateLaneId(feature.getName(), status);
    }

    /**
     * Generates a unique lane ID for Selenium testing based on a name and TaskStatus enum.
     * This is used to create predictable IDs for drag-and-drop testing in Selenium.
     *
     * @param name   The name of the story, feature, or other entity (e.g., "Config API Implementation")
     * @param status The TaskStatus enum value
     * @return A lane ID in the format: name-with-hyphens-STATUS (e.g., "Config-API-Implementation-IN-PROGRESS")
     */
    private static String generateLaneId(String name, TaskStatus status) {
        // Replace all non-alphanumeric characters with hyphens
        String sanitizedName = name.replaceAll("[^a-zA-Z0-9]", "-");
        // Replace underscores in status with hyphens
        String sanitizedStatus = status.name().replace("_", "-");
        return sanitizedName + "-" + sanitizedStatus;
    }

    /**
     * Generates a unique lane ID for Selenium testing based on a Task (story) and TaskStatus enum.
     * Type-safe method that accepts a Task object and TaskStatus enum directly.
     *
     * @param story  The Task object representing a story
     * @param status The TaskStatus enum value
     * @return A lane ID in the format: story-name-with-hyphens-STATUS
     */
    public static String generateStoryLaneId(Task story, TaskStatus status) {
        return generateLaneId(story.getName(), status);
    }

    /**
     * Handles exceptions from API calls, with special handling for unique constraint violations (409 CONFLICT)
     * and access control errors (403 FORBIDDEN).
     * This method parses field-specific error information from the exception and routes it to the appropriate
     * field error handler. Access control errors are always routed to the default handler (typically dialog-level error).
     *
     * @param e                   The exception to handle
     * @param fieldErrorHandlers  Map of field names to their error handlers (e.g., "name" -> nameField::setError)
     * @param defaultErrorHandler Optional fallback handler for fields not in the map or generic errors
     * @return true if the error was handled, false if it should be propagated
     */
    public static boolean handleApiException(
            Exception e,
            Map<String, FieldErrorHandler> fieldErrorHandlers,
            Consumer<String> defaultErrorHandler) {

        if (e instanceof ResponseStatusException rse) {

            // Handle access control errors (403 FORBIDDEN) - always use default handler
            if (HttpStatus.FORBIDDEN.equals(rse.getStatusCode())) {
                String errorMessage = rse.getReason() != null ? rse.getReason() : "Access Denied";
                // Strip "Access Denied: " prefix if present for cleaner display
                if (errorMessage.startsWith("Access Denied: ")) {
                    errorMessage = errorMessage.substring("Access Denied: ".length());
                }

                if (defaultErrorHandler != null) {
                    defaultErrorHandler.accept(errorMessage);
                } else {
                    showErrorNotification(errorMessage);
                }
                return true;
            }

            // Handle unique constraint violations (409 CONFLICT) - route to specific field
            if (HttpStatus.CONFLICT.equals(rse.getStatusCode())) {
                String reason = rse.getReason();

                // Parse field information from the error message
                // Format: "message|field=fieldName|value=fieldValue"
                String fieldName    = null;
                String errorMessage = reason;

                if (reason != null && reason.contains("|field=")) {
                    String[] parts = reason.split("\\|");
                    errorMessage = parts[0]; // The main message

                    for (String part : parts) {
                        if (part.startsWith("field=")) {
                            fieldName = part.substring("field=".length());
                        }
                    }
                }

                // Try to find a specific handler for this field
                if (fieldName != null && fieldErrorHandlers.containsKey(fieldName)) {
                    fieldErrorHandlers.get(fieldName).setError(errorMessage);
                } else if (defaultErrorHandler != null) {
                    // Use default handler if field not found or no field specified
                    defaultErrorHandler.accept(errorMessage);
                } else if (!fieldErrorHandlers.isEmpty()) {
                    // Fallback: use the first field handler if no default specified
                    fieldErrorHandlers.values().iterator().next().setError(errorMessage);
                } else {
                    // Last resort: show notification
                    showErrorNotification(errorMessage);
                }

                return true; // Error was handled
            }
        }

        // For other errors, use default handler or show notification
        String errorMessage = "An error occurred: " + e.getMessage();
        if (defaultErrorHandler != null) {
            defaultErrorHandler.accept(errorMessage);
        } else {
            showErrorNotification(errorMessage);
        }
        return true; // Error was handled
    }

    /**
     * Convenience method for handling API exceptions when you only have one field.
     *
     * @param e                 The exception to handle
     * @param fieldName         The name of the field (should match the field name in the error response)
     * @param fieldErrorHandler The error handler for this field
     */
    public static boolean handleApiException(
            Exception e,
            String fieldName,
            FieldErrorHandler fieldErrorHandler) {
        Map<String, FieldErrorHandler> handlers = new HashMap<>();
        handlers.put(fieldName, fieldErrorHandler);
        return handleApiException(e, handlers, null);
    }

    /**
     * Convenience method for handling API exceptions with a single field and a default error handler.
     * Field-specific errors (like CONFLICT) are routed to the field handler.
     * Field-independent errors (like FORBIDDEN) are routed to the default handler.
     *
     * @param e                   The exception to handle
     * @param fieldName           The name of the field (should match the field name in the error response)
     * @param fieldErrorHandler   The error handler for this field
     * @param defaultErrorHandler The default error handler for non-field-specific errors
     */
    public static boolean handleApiException(
            Exception e,
            String fieldName,
            FieldErrorHandler fieldErrorHandler,
            Consumer<String> defaultErrorHandler) {
        Map<String, FieldErrorHandler> handlers = new HashMap<>();
        handlers.put(fieldName, fieldErrorHandler);
        return handleApiException(e, handlers, defaultErrorHandler);
    }

    /**
     * Hides the error message in a dialog error message component.
     *
     * @param errorMessageComponent The error message component created by createDialogErrorMessage()
     */
    public static void hideDialogError(Span errorMessageComponent) {
        errorMessageComponent.setVisible(false);
        errorMessageComponent.removeAll();
    }

    /**
     * Shows an error message in a dialog error message component.
     * Automatically adds an error icon and makes the component visible.
     *
     * @param errorMessageComponent The error message component created by createDialogErrorMessage()
     * @param message               The error message to display
     */
    public static void showDialogError(Span errorMessageComponent, String message) {
        errorMessageComponent.removeAll();
        Icon errorIcon = new Icon(VaadinIcon.WARNING);
        errorIcon.getStyle().set("color", "var(--lumo-error-text-color)");
        errorMessageComponent.add(errorIcon, new Span(message));
        errorMessageComponent.setVisible(true);
    }

    /**
     * Shows an error notification to the user.
     *
     * @param message The error message to display
     */
    private static void showErrorNotification(String message) {
        Notification notification = new Notification(message, 5000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.open();
    }

    /**
     * Functional interface for create button click handlers
     */
    @FunctionalInterface
    public interface CreateButtonClickHandler {
        void onClick();
    }

    /**
     * Functional interface for field-specific error handlers.
     * Implementations should set the error message on the appropriate field component.
     */
    @FunctionalInterface
    public interface FieldErrorHandler {
        /**
         * Sets an error message on a specific field.
         *
         * @param errorMessage The error message to display
         */
        void setError(String errorMessage);
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
