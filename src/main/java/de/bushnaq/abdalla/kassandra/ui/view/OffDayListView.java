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
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.ai.AiFilterService;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.dto.Location;
import de.bushnaq.abdalla.kassandra.rest.api.OffDayApi;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.component.AbstractMainGrid;
import de.bushnaq.abdalla.kassandra.ui.component.OffDaysCalendarComponent;
import de.bushnaq.abdalla.kassandra.ui.dialog.ConfirmDialog;
import de.bushnaq.abdalla.kassandra.ui.dialog.OffDayDialog;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;
import jakarta.annotation.security.PermitAll;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "offday/:user-email?", layout = MainLayout.class)
@PageTitle("User Off Days")
@PermitAll
public class OffDayListView extends AbstractMainGrid<OffDay> implements BeforeEnterObserver, AfterNavigationObserver {
    public static final String                   CREATE_OFFDAY_BUTTON             = "create-offday-button";
    public static final String                   OFFDAY_GLOBAL_FILTER             = "offday-global-filter";
    public static final String                   OFFDAY_GRID                      = "offday-grid";
    public static final String                   OFFDAY_GRID_DELETE_BUTTON_PREFIX = "offday-delete-button-";
    public static final String                   OFFDAY_GRID_EDIT_BUTTON_PREFIX   = "offday-edit-button-";
    public static final String                   OFFDAY_GRID_END_DATE_PREFIX      = "offday-end-";
    public static final String                   OFFDAY_GRID_START_DATE_PREFIX    = "offday-start-";
    public static final String                   OFFDAY_GRID_TYPE_PREFIX          = "offday-type-";
    public static final String                   OFFDAY_LIST_PAGE_TITLE           = "offday-page-title";
    public static final String                   OFFDAY_ROW_COUNTER               = "offday-row-counter";
    public static final String                   ROUTE                            = "offday";
    private             User                     currentUser;
    private final       OffDayApi                offDayApi;
    private final       UserApi                  userApi;
    private             OffDaysCalendarComponent yearCalendar;


    public OffDayListView(OffDayApi offDayApi, UserApi userApi, Clock clock, AiFilterService aiFilterService, ObjectMapper mapper) {
        super(clock);
        this.offDayApi = offDayApi;
        this.userApi   = userApi;

        add(
                createSmartHeader(
                        "User Off-Days",
                        OFFDAY_LIST_PAGE_TITLE,
                        VaadinIcon.CALENDAR,
                        CREATE_OFFDAY_BUTTON,
                        () -> openOffDayDialog(null),
                        OFFDAY_ROW_COUNTER,
                        OFFDAY_GLOBAL_FILTER,
                        aiFilterService, mapper, "OffDay"
                ),
                new HorizontalLayout(getGridPanelWrapper(), createCalendar())
        );
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        refreshOffDayGrid();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Get userEmail from URL parameter or use the currently authenticated user
        String userEmailParam = event.getRouteParameters().get("user-email").orElse(null);

        Authentication authentication   = SecurityContextHolder.getContext().getAuthentication();
        String         currentUserEmail = authentication != null ? authentication.getName() : null;

        // If no userEmail is provided, use the current authenticated user
        // Store in a final variable to use in lambda
        final String userEmail = (userEmailParam == null && currentUserEmail != null) ?
                currentUserEmail : userEmailParam;

        if (userEmail != null) {
            try {
                // Find user by userEmail using the direct getByName method
                List<User> all = userApi.getAll();
                currentUser = userApi.getByEmail(userEmail);
                currentUser.initialize();
            } catch (ResponseStatusException ex) {
                if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                    // Create a new user since one wasn't found
                    User newUser = createDefaultUser(userEmail);
                    currentUser = userApi.persist(newUser);
                    currentUser.initialize();
                    Notification notification = Notification.show("Created new user: " + userEmail, 3000, Notification.Position.MIDDLE);
                    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    throw ex;
                }
            }
        } else {
            // Redirect to main page if no userEmail and not authenticated
            event.forwardTo("");
        }
        createCalendar();
    }

    private void confirmDelete(OffDay offDay) {
        ConfirmDialog dialog = new ConfirmDialog("Confirm Delete", "Are you sure you want to delete this off day record?", "Delete",
                () -> {
                    try {
                        offDayApi.deleteById(currentUser, offDay);
                        refreshOffDayGrid();
                        // Update the calendar with the fresh user data
                        if (yearCalendar != null) {
                            yearCalendar.updateCalendar(currentUser);
                        }
                        Notification.show("Off day deleted", 3000, Notification.Position.MIDDLE);
                    } catch (Exception ex) {
                        Notification notification = Notification.show("Failed to delete: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                });
        dialog.open();
    }

    private OffDaysCalendarComponent createCalendar() {
        // Create a new OffDaysCalendarComponent with the current user and the current year
        if (yearCalendar != null)
            return yearCalendar;
        yearCalendar = new OffDaysCalendarComponent(currentUser, ParameterOptions.getLocalNow().getYear(), this::handleCalendarDayClick);
        // Set up handler to refresh grid when year changes
        yearCalendar.setYearChangeHandler(year -> refreshOffDayGrid());
        return yearCalendar;
    }

    /**
     * Creates a default user with standard settings
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
        Availability availability = new Availability(1.0f, ParameterOptions.getLocalNow().toLocalDate());
        user.addAvailability(availability);
        de.bushnaq.abdalla.kassandra.dto.Location location = new Location("DE", "nw", ParameterOptions.getLocalNow().toLocalDate());
        user.addLocation(location);
        return user;
    }

    /**
     * Handles clicks on calendar days that have off days
     *
     * @param date The date that was clicked
     */
    private void handleCalendarDayClick(java.time.LocalDate date) {
        if (currentUser != null) {
            // Find the off day that contains this date
            OffDay matchingOffDay = currentUser.getOffDays().stream()
                    .filter(offDay ->
                            (date.equals(offDay.getFirstDay()) || date.isAfter(offDay.getFirstDay())) &&
                                    (date.equals(offDay.getLastDay()) || date.isBefore(offDay.getLastDay())))
                    .findFirst()
                    .orElse(null);

            if (matchingOffDay != null) {
                // Open the dialog to edit this off day
                openOffDayDialog(matchingOffDay);
            }
        }
    }

    protected void initGrid(Clock clock) {
        getGrid().setId(OFFDAY_GRID);

        // Format dates consistently
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // First Day Column
        {
            Grid.Column<OffDay> firstDayColumn = getGrid().addColumn(new ComponentRenderer<>(offDay -> {
                String startDateStr = offDay.getFirstDay().format(dateFormatter);
                Span   span         = new Span(startDateStr);
                span.setId(OFFDAY_GRID_START_DATE_PREFIX + offDay.getId());
                return span;
            }));

//            VaadinUtil.addFilterableHeader(
//                    grid,
//                    firstDayColumn,
//                    "First Day",
//                    VaadinIcon.CALENDAR,
//                    offDay -> offDay.getFirstDay().format(dateFormatter)
//            );
            VaadinUtil.addSimpleHeader(firstDayColumn, "First Day", VaadinIcon.CALENDAR);
        }

        // Last Day Column
        {
            Grid.Column<OffDay> lastDayColumn = getGrid().addColumn(new ComponentRenderer<>(offDay -> {
                String endDateStr = offDay.getLastDay().format(dateFormatter);
                Span   span       = new Span(endDateStr);
                span.setId(OFFDAY_GRID_END_DATE_PREFIX + offDay.getId());
                return span;
            }));

//            VaadinUtil.addFilterableHeader(
//                    grid,
//                    lastDayColumn,
//                    "Last Day",
//                    VaadinIcon.CALENDAR,
//                    offDay -> offDay.getLastDay().format(dateFormatter)
//            );
            VaadinUtil.addSimpleHeader(lastDayColumn, "Last Day", VaadinIcon.CALENDAR);
        }

        // Type Column
        {
            Grid.Column<OffDay> typeColumn = getGrid().addColumn(new ComponentRenderer<>(offDay -> {
                OffDayType type = offDay.getType();
                Span       span = new Span(type.name());
                span.setId(OFFDAY_GRID_TYPE_PREFIX + offDay.getId());
                return span;
            }));

//            VaadinUtil.addFilterableHeader(
//                    grid,
//                    typeColumn,
//                    "Type",
//                    VaadinIcon.TAGS,
//                    offDay -> offDay.getType().name()
//            );
            VaadinUtil.addSimpleHeader(typeColumn, "Type", VaadinIcon.TAGS);
        }

        // Add action column using VaadinUtil
        VaadinUtil.addActionColumn(
                getGrid(),
                OFFDAY_GRID_EDIT_BUTTON_PREFIX,
                OFFDAY_GRID_DELETE_BUTTON_PREFIX,
                offDay -> String.valueOf(offDay.getId()),
                this::openOffDayDialog,
                this::confirmDelete
        );

    }

    private void openOffDayDialog(OffDay offDay) {
        // Create new or edit existing off day
        OffDayDialog dialog = new OffDayDialog(
                offDay,
                this.currentUser,
                this.offDayApi,
                () -> {
                    refreshOffDayGrid();
                    if (yearCalendar != null) {
                        // Pass the updated user to the calendar
                        yearCalendar.updateCalendar(currentUser);
                    }
                });
        dialog.open();
    }


    private void refreshOffDayGrid() {
        if (currentUser != null) {
            // Reload the user to get fresh data
            currentUser = userApi.getById(currentUser.getId());
            currentUser.initialize();

            // Get the selected year from the calendar (or use current year if calendar not yet initialized)
            int selectedYear = (yearCalendar != null) ? yearCalendar.getCurrentYear() : ParameterOptions.getLocalNow().getYear();

            // Filter off days to only show those that overlap with the selected year
            // Sort off days by start date (newest to oldest)
            List<OffDay> sortedOffDays = currentUser.getOffDays().stream()
                    .filter(offDay -> {
                        // Include off day if it overlaps with the selected year
                        int startYear = offDay.getFirstDay().getYear();
                        int endYear   = offDay.getLastDay().getYear();
                        return startYear == selectedYear || endYear == selectedYear ||
                                (startYear < selectedYear && endYear > selectedYear);
                    })
                    .sorted(Comparator.comparing(OffDay::getFirstDay).reversed())
                    .collect(Collectors.toList());

            getDataProvider().getItems().clear();
            getDataProvider().getItems().addAll(sortedOffDays);
            getDataProvider().refreshAll();

            yearCalendar.updateCalendar(currentUser);
        }
    }

}
