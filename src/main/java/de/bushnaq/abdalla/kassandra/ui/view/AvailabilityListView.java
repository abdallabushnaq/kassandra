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

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.router.*;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.ai.AiFilterService;
import de.bushnaq.abdalla.kassandra.dto.Availability;
import de.bushnaq.abdalla.kassandra.dto.Location;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.rest.api.AvailabilityApi;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.component.AbstractMainGrid;
import de.bushnaq.abdalla.kassandra.ui.component.AvailabilityCalendarComponent;
import de.bushnaq.abdalla.kassandra.ui.dialog.AvailabilityDialog;
import de.bushnaq.abdalla.kassandra.ui.dialog.ConfirmDialog;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;
import jakarta.annotation.security.PermitAll;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.text.NumberFormat;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Route(value = "availability/:user-email?", layout = MainLayout.class)
@PageTitle("User Availability")
@PermitAll
public class AvailabilityListView extends AbstractMainGrid<Availability> implements BeforeEnterObserver, AfterNavigationObserver {
    public static final String                        AVAILABILITY_GLOBAL_FILTER             = "availability-global-filter";
    public static final String                        AVAILABILITY_GRID                      = "availability-grid";
    public static final String                        AVAILABILITY_GRID_AVAILABILITY_PREFIX  = "availability-value-";
    public static final String                        AVAILABILITY_GRID_DELETE_BUTTON_PREFIX = "availability-delete-button-";
    public static final String                        AVAILABILITY_GRID_EDIT_BUTTON_PREFIX   = "availability-edit-button-";
    public static final String                        AVAILABILITY_GRID_START_DATE_PREFIX    = "availability-start-";
    public static final String                        AVAILABILITY_LIST_PAGE_TITLE           = "availability-page-title";
    public static final String                        AVAILABILITY_ROW_COUNTER               = "availability-row-counter";
    public static final String                        CREATE_AVAILABILITY_BUTTON             = "create-availability-button";
    public static final String                        ROUTE                                  = "availability";
    private final       AvailabilityApi               availabilityApi;
    private             User                          currentUser;
    private final       DateTimeFormatter             dateFormatter                          = DateTimeFormatter.ISO_LOCAL_DATE /*DateTimeFormatter.ofPattern("yyyy-MM-dd")*/;// Format dates consistently
    private final       UserApi                       userApi;
    private             AvailabilityCalendarComponent yearCalendar;

    public AvailabilityListView(AvailabilityApi availabilityApi, UserApi userApi, Clock clock, AiFilterService aiFilterService, JsonMapper mapper) {
        super(clock);
        this.availabilityApi = availabilityApi;
        this.userApi         = userApi;

        add(
                createSmartHeader(
                        "User Availability",
                        AVAILABILITY_LIST_PAGE_TITLE,
                        VaadinIcon.CHART,
                        CREATE_AVAILABILITY_BUTTON,
                        () -> openAvailabilityDialog(null),
                        AVAILABILITY_ROW_COUNTER,
                        AVAILABILITY_GLOBAL_FILTER,
                        aiFilterService, mapper, "Availability"
                ),
                new HorizontalLayout(getGridPanelWrapper(), createCalendar())
        );
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        refreshGrid();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Get username from URL parameter or use the currently authenticated user
        String userEmailParam = event.getRouteParameters().get("user-email").orElse(null);

        Authentication authentication  = SecurityContextHolder.getContext().getAuthentication();
        String         currentUsername = authentication != null ? authentication.getName() : null;

        // If no username is provided, use the current authenticated user
        // Store in a final variable to use in lambda
        final String userEmail = (userEmailParam == null && currentUsername != null) ? currentUsername : userEmailParam;

        if (userEmail != null) {
            try {
                // Find user by username using the direct getByName method
                currentUser = userApi.getByEmail(userEmail).get();
            } catch (ResponseStatusException ex) {
                if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                    // Create a new user since one wasn't found
                    User newUser = createDefaultUser(userEmail);
                    currentUser = userApi.persist(newUser);
                    Notification notification = Notification.show("Created new user: " + userEmail, 3000, Notification.Position.MIDDLE);
                    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    throw ex;
                }
            }
        } else {
            // Redirect to main page if no username and not authenticated
            event.forwardTo("");
        }
    }

    private void confirmDelete(Availability availability) {
        if (getDataProvider().getItems().size() <= 1) {
            Notification notification = Notification.show("Cannot delete - Users must have at least one availability", 3000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        ConfirmDialog dialog = new ConfirmDialog("Confirm Delete",
                "Are you sure you want to delete this availability record?",
                "Delete",
                () -> {
                    try {
                        availabilityApi.deleteById(currentUser.getId(), availability.getId());
                        refreshGrid();
                        Notification.show("Availability deleted", 3000, Notification.Position.MIDDLE);
                    } catch (Exception ex) {
                        Notification notification = Notification.show("Failed to delete: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                });
        dialog.open();
    }

    private AvailabilityCalendarComponent createCalendar() {
        // Create a new AvailabilityCalendarComponent with the current user and the current year
        if (yearCalendar != null)
            return yearCalendar;
        yearCalendar = new AvailabilityCalendarComponent(currentUser, ParameterOptions.getLocalNow().getYear(), this::handleCalendarDayClick);
        // Set up handler to refresh grid when year changes
        yearCalendar.setYearChangeHandler(year -> refreshGrid());
        return yearCalendar;
    }

    /**
     * Creates a default user with standard availability and location
     *
     * @param username The name for the new user
     * @return A new User object with default settings
     */
    private User createDefaultUser(String username) {
        User user = new User();
        user.setName(username);
        user.setEmail(username);
        user.setFirstWorkingDay(java.time.LocalDate.now());
        user.setColor(new java.awt.Color(51, 102, 204));
        Availability availability = new Availability(1.0f, java.time.LocalDate.now());
        user.addAvailability(availability);
        Location location = new Location("DE", "nw", java.time.LocalDate.now());
        user.addLocation(location);
        return user;
    }

    /**
     * Handles clicks on calendar days
     *
     * @param date The date that was clicked
     */
    private void handleCalendarDayClick(java.time.LocalDate date) {
        // Find the availability that applies to this date
        List<Availability> sortedAvailabilities = currentUser.getAvailabilities().stream()
                .sorted(Comparator.comparing(Availability::getStart).reversed())
                .collect(Collectors.toList());

        for (Availability availability : sortedAvailabilities) {
            if (!availability.getStart().isAfter(date)) {
                // Open dialog to edit this availability
                openAvailabilityDialog(availability);
                break;
            }
        }
    }

    protected void initGrid(Clock clock) {
        getGrid().setId(AVAILABILITY_GRID);


        // Start Date Column
        {
            Grid.Column<Availability> startColumn = getGrid().addColumn(new ComponentRenderer<>(availability -> {
                String startDateStr = availability.getStart().format(dateFormatter);
                Span   span         = new Span(startDateStr);
                span.setId(AVAILABILITY_GRID_START_DATE_PREFIX + startDateStr);
                return span;
            }));

//            VaadinUtil.addFilterableHeader(
//                    grid,
//                    startColumn,
//                    "Start Date",
//                    VaadinIcon.CHART,
//                    availability -> availability.getStart().format(dateFormatter)
//            );
            VaadinUtil.addSimpleHeader(startColumn, "Start Date", VaadinIcon.CALENDAR);
        }

        // Availability Column with percentage formatting
        {
            NumberFormat percentageFormat = NumberFormat.getPercentInstance(Locale.US);
            Grid.Column<Availability> availabilityColumn = getGrid().addColumn(new NumberRenderer<>(
                    availability -> availability.getAvailability(),
                    percentageFormat
            ));

//            VaadinUtil.addFilterableHeader(
//                    grid,
//                    availabilityColumn,
//                    "Availability",
//                    VaadinIcon.CHART,
//                    availability -> percentageFormat.format(availability.getAvailability())
//            );
            VaadinUtil.addSimpleHeader(availabilityColumn, "Availability", VaadinIcon.CHART);
        }

        // Add actions column with delete validation
        VaadinUtil.addActionColumn(
                getGrid(),
                AVAILABILITY_GRID_EDIT_BUTTON_PREFIX,
                AVAILABILITY_GRID_DELETE_BUTTON_PREFIX,
                availability -> availability.getStart().format(dateFormatter),
                this::openAvailabilityDialog,
                this::confirmDelete,
                availability -> {
                    // Validate: Users must have at least one availability
                    if (getDataProvider().getItems().size() <= 1) {
                        return VaadinUtil.DeleteValidationResult.invalid("Cannot delete - Users must have at least one availability");
                    }
                    return VaadinUtil.DeleteValidationResult.valid();
                }
        );

    }

    private void openAvailabilityDialog(Availability availability) {
        // Create new or edit existing availability
        AvailabilityDialog dialog = new AvailabilityDialog(
                availability,
                this.currentUser,
                this.availabilityApi,
                this::refreshGrid);
        dialog.open();
    }

    private void refreshGrid() {
        if (currentUser != null) {
            // Get a fresh copy of the user to ensure we have the latest data
            currentUser = userApi.getById(currentUser.getId());
            currentUser.initialize();
            // Sort availabilities by start date in descending order (latest first)
            List<Availability> sortedAvailabilities = currentUser.getAvailabilities().stream().sorted(Comparator.comparing(Availability::getStart).reversed()).collect(Collectors.toList());
            getDataProvider().getItems().clear();
            getDataProvider().getItems().addAll(sortedAvailabilities);
            getDataProvider().refreshAll();

            // Update the calendar with the latest data
            if (yearCalendar != null) {
                yearCalendar.updateCalendar(currentUser, sortedAvailabilities);
            }
        }
    }
}
