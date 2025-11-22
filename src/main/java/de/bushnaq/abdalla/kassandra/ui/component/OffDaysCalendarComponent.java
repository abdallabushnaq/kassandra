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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.OffDay;
import de.bushnaq.abdalla.kassandra.dto.OffDayType;
import de.bushnaq.abdalla.kassandra.dto.User;
import lombok.Getter;
import lombok.Setter;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectCalendarException;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A component that displays a year calendar with user's off days
 * highlighted according to their type.
 */
public class OffDaysCalendarComponent extends VerticalLayout {

    public static final  String                     CALENDAR_NEXT_YEAR_BTN              = "calendar-next-year-btn";
    public static final  String                     CALENDAR_PREV_YEAR_BTN              = "calendar-prev-year-btn";
    private static final String                     CLASS_FILLING_DAY                   = "calendar-filling-day";
    private static final String                     CLASS_HOLIDAY_DAY                   = "calendar-holiday-day";
    private static final String                     CLASS_MONTH_NAME                    = "calendar-month-name";
    private static final String                     CLASS_NORMAL_DAY                    = "calendar-normal-day";
    private static final String                     CLASS_SICK_DAY                      = "calendar-sick-day";
    private static final String                     CLASS_TODAY                         = "calendar-today";
    private static final String                     CLASS_TRIP_DAY                      = "calendar-trip-day";
    private static final String                     CLASS_VACATION_DAY                  = "calendar-vacation-day";
    private static final String                     CLASS_WEEKEND_DAY                   = "calendar-weekend-day";
    private static final String                     DAY_SIZE_PX                         = "36px"; // Increased from 24px (50% larger)
    private static final String                     LEGEND_ITEM_ID_PREFIX               = "calendar-legend-item-";
    public static final  String                     LEGEND_ITEM_ID_PREFIX_BUSINESS_TRIP = "calendar-legend-item-business-trip";
    public static final  String                     LEGEND_ITEM_ID_PREFIX_HOLIDAY       = "calendar-legend-item-holiday";
    public static final  String                     LEGEND_ITEM_ID_PREFIX_SICK_LEAVE    = "calendar-legend-item-sick-leave";
    public static final  String                     LEGEND_ITEM_ID_PREFIX_VACATION      = "calendar-legend-item-vacation";
    private static final int                        MONTHS_PER_ROW                      = 4;
    @Getter
    private              int                        currentYear;
    private final        Consumer<LocalDate>        dayClickHandler;
    private final        Map<LocalDate, OffDayType> offDayMap                           = new HashMap<>();
    private              User                       user;
    /**
     * -- SETTER --
     * Sets the handler that will be called when the year changes.
     *
     * @param yearChangeHandler Consumer that handles year changes
     */
    @Setter
    private              Consumer<Integer>          yearChangeHandler;

    /**
     * Creates a new year calendar component for the given user and year.
     *
     * @param user            The user whose calendar should be displayed
     * @param year            The year to display
     * @param dayClickHandler Consumer that handles clicks on calendar days that have off days
     */
    public OffDaysCalendarComponent(User user, int year, Consumer<LocalDate> dayClickHandler) {
        this.user            = user;
        this.currentYear     = year;
        this.dayClickHandler = dayClickHandler;

        // Set fixed width that won't resize
        setWidth("1130px");
        setHeight("auto");
        setPadding(false);
        setSpacing(false);

        // Prevent resizing of the component
        getStyle()
                .set("flex-shrink", "0")
                .set("flex-grow", "0");
    }

    /**
     * Builds a map of dates to off day types for quick lookup.
     */
    private void buildOffDayMap() {
        offDayMap.clear();

        for (OffDay offDay : user.getOffDays()) {
            LocalDate  start = offDay.getFirstDay();
            LocalDate  end   = offDay.getLastDay();
            OffDayType type  = offDay.getType();

            // Add each day in the range to the map
            LocalDate current = start;
            while (!current.isAfter(end)) {
                // Only include days for the current year
                if (current.getYear() == currentYear) {
                    offDayMap.put(current, type);
                }
                current = current.plusDays(1);
            }
        }
    }

    /**
     * Creates a component representing a single day in the calendar.
     */
    private Component createDayComponent(int day, LocalDate date, boolean isWeekend, boolean isFillingDay) {
        return createDayComponent(day, date, isWeekend, isFillingDay, false);
    }

    /**
     * Creates a component representing a single day in the calendar.
     */
    private Component createDayComponent(int day, LocalDate date, boolean isWeekend, boolean isFillingDay, boolean isToday) {
        Div dayComponent = new Div();
        dayComponent.setText(String.valueOf(day));

        // Set common styles for all day components
        dayComponent.getStyle()
                .set("width", DAY_SIZE_PX)
                .set("height", DAY_SIZE_PX)
                .set("line-height", DAY_SIZE_PX)
                .set("text-align", "center")
                .set("cursor", "default")
                .set("font-weight", "bold")
                .set("font-size", "0.85rem")
                .set("box-sizing", "border-box"); // Ensure padding is included in the element's dimensions

        // Apply appropriate class based on day type
        if (isFillingDay) {
            dayComponent.addClassName(CLASS_FILLING_DAY);
        } else {
            // Use the user's calendar directly to determine the day type
            ProjectCalendar calendar = user.getCalendar();

            // Check if it's a weekend or special day
            if (!isFillingDay) {
                ProjectCalendarException exception = calendar.getException(date);

                if (exception != null) {
                    // This is a special day (vacation, sick, holiday, trip)
                    String name = exception.getName();

                    if (name.equals(OffDayType.VACATION.name())) {
                        dayComponent.addClassName(CLASS_VACATION_DAY);
                    } else if (name.equals(OffDayType.SICK.name())) {
                        dayComponent.addClassName(CLASS_SICK_DAY);
                    } else if (name.equals(OffDayType.TRIP.name())) {
                        dayComponent.addClassName(CLASS_TRIP_DAY);
                    } else {
                        dayComponent.addClassName(CLASS_HOLIDAY_DAY);
                    }

                    // Make special days clickable for editing
                    dayComponent.getStyle().set("cursor", "pointer");
                    dayComponent.getElement().addEventListener("click", event -> {
                        if (dayClickHandler != null) {
                            dayClickHandler.accept(date);
                        }
                    });
                } else if (!calendar.isWorkingDay(date.getDayOfWeek())) {
                    // Weekend based on calendar definition
                    dayComponent.addClassName(CLASS_WEEKEND_DAY);
                } else {
                    // Normal working day
                    dayComponent.addClassName(CLASS_NORMAL_DAY);
                }
            }
        }

        // Mark today with a special highlight
        if (isToday) {
            dayComponent.addClassName(CLASS_TODAY);
        }

        return dayComponent;
    }

    /**
     * Creates a legend explaining the color coding in the calendar.
     */
    private Component createLegend() {
        HorizontalLayout legend = new HorizontalLayout();
        legend.setWidthFull();
        legend.addClassNames(
                LumoUtility.Padding.SMALL,
                LumoUtility.Margin.Top.MEDIUM);

        // Removed "Normal Day" and "Weekend" from the legend as requested
        legend.add(createLegendItem("Vacation", CLASS_VACATION_DAY));
        legend.add(createLegendItem("Sick Leave", CLASS_SICK_DAY));
        legend.add(createLegendItem("Holiday", CLASS_HOLIDAY_DAY));
        legend.add(createLegendItem("Business Trip", CLASS_TRIP_DAY));

        return legend;
    }

    /**
     * Creates a single legend item with a color box and label.
     */
    private Component createLegendItem(String label, String colorClass) {
        HorizontalLayout item = new HorizontalLayout();
        item.setId(LEGEND_ITEM_ID_PREFIX + label.toLowerCase().replace(" ", "-"));
        item.setSpacing(true);
        item.setAlignItems(FlexComponent.Alignment.CENTER);

        // Color box
        Div colorBox = new Div();
        colorBox.addClassName(colorClass);
        colorBox.getStyle()
                .set("width", "16px")
                .set("height", "16px");

        // Label
        Span labelSpan = new Span(label);

        item.add(colorBox, labelSpan);
        return item;
    }

    /**
     * Creates a component representing a single month.
     */
    private Component createMonthComponent(Month month) {
        Div monthComponent = new Div();
        monthComponent.addClassNames(
                LumoUtility.Padding.SMALL,
                LumoUtility.Margin.SMALL);

        // Use fixed width for consistent sizing across all containers
        // With 4 months per row and total width of 1067px, each month gets ~266px
        // Subtracting margin and padding
        monthComponent.getStyle().set("width", "272px");
        monthComponent.getStyle().set("box-sizing", "border-box");

        // Month name header
        Span monthName = new Span(month.toString());
        monthName.addClassNames(
                CLASS_MONTH_NAME,
                LumoUtility.Display.BLOCK,
                LumoUtility.FontWeight.BOLD,
                LumoUtility.Padding.XSMALL);
        monthComponent.add(monthName);

        // Days of week header (Mon-Sun)
        HorizontalLayout weekdaysHeader = new HorizontalLayout();
        weekdaysHeader.setSpacing(false);
        weekdaysHeader.setPadding(false);
        weekdaysHeader.setWidthFull();
        weekdaysHeader.getStyle().set("justify-content", "space-between");

        String[] weekdays = {"M", "T", "W", "T", "F", "S", "S"};
        for (String day : weekdays) {
            Span dayLabel = new Span(day);
            dayLabel.getStyle()
                    .set("width", DAY_SIZE_PX)
                    .set("height", DAY_SIZE_PX)
                    .set("line-height", DAY_SIZE_PX)
                    .set("text-align", "center")
                    .set("font-weight", "bold")
                    .set("font-size", "0.8rem");
            weekdaysHeader.add(dayLabel);
        }
        monthComponent.add(weekdaysHeader);

        // Calendar days grid
        Div calendarGrid = new Div();
        calendarGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(7, 1fr)")
                .set("gap", "1px")
                .set("width", "100%");  // Enable this line to ensure full width

        // Add some bottom margin to ensure space between month name and first row
        calendarGrid.getStyle().set("margin-top", "4px");
        LocalDate today = ParameterOptions.getLocalNow().toLocalDate();
        // First day of month
        LocalDate firstOfMonth = LocalDate.of(currentYear, month, 1);
        // Determine first day of week for this month (0 = Monday, 6 = Sunday in our display)
        int firstDayOfWeek = firstOfMonth.getDayOfWeek().getValue() - 1; // Monday is 1 in DayOfWeek, but we want 0
        // Last day of month
        int lastDay = firstOfMonth.lengthOfMonth();
        // Add filling days from previous month
        if (firstDayOfWeek > 0) {
            // Get the previous month
            LocalDate prevMonthDate    = firstOfMonth.minusMonths(1);
            int       prevMonthLastDay = prevMonthDate.lengthOfMonth();
            for (int i = 0; i < firstDayOfWeek; i++) {
                int       day  = prevMonthLastDay - firstDayOfWeek + i + 1;
                LocalDate date = LocalDate.of(month.getValue() == 1 ? currentYear - 1 : currentYear, month.getValue() == 1 ? 12 : month.getValue() - 1, day);
                calendarGrid.add(createDayComponent(day, date, true, true));
            }
        }

        // Add days for current month
        for (int day = 1; day <= lastDay; day++) {
            LocalDate date      = LocalDate.of(currentYear, month, day);
            boolean   isWeekend = isWeekend(date);
            // Check if it's today
            boolean isToday = date.equals(today);
            calendarGrid.add(createDayComponent(day, date, isWeekend, false, isToday));
        }

        // Add filling days from next month
        int fillingDaysNeeded = 7 - ((firstDayOfWeek + lastDay) % 7);
        if (fillingDaysNeeded < 7) { // Skip if it's exactly 7 (full week)
            for (int i = 1; i <= fillingDaysNeeded; i++) {
                LocalDate date = LocalDate.of(month.getValue() == 12 ? currentYear + 1 : currentYear, month.getValue() == 12 ? 1 : month.getValue() + 1, i);
                calendarGrid.add(createDayComponent(i, date, false, true));
            }
        }

        monthComponent.add(calendarGrid);
        return monthComponent;
    }

    /**
     * Creates the year header with navigation buttons.
     */
    private Component createYearHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        Button prevYearBtn = new Button(new Icon(VaadinIcon.ANGLE_LEFT));
        prevYearBtn.setId(CALENDAR_PREV_YEAR_BTN);
        prevYearBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        prevYearBtn.addClickListener(e -> setYear(currentYear - 1));

        H4 yearLabel = new H4(String.valueOf(currentYear));
        yearLabel.addClassNames(LumoUtility.Margin.NONE);

        Button nextYearBtn = new Button(new Icon(VaadinIcon.ANGLE_RIGHT));
        nextYearBtn.setId(CALENDAR_NEXT_YEAR_BTN);
        nextYearBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        nextYearBtn.addClickListener(e -> setYear(currentYear + 1));

        header.add(prevYearBtn, yearLabel, nextYearBtn);
        return header;
    }


    /**
     * Checks if a date falls on a weekend based on the user's calendar.
     */
    private boolean isWeekend(LocalDate date) {
        ProjectCalendar calendar = user.getCalendar();
        return !calendar.isWorkingDay(date.getDayOfWeek());
    }

    /**
     * Updates the calendar to display the specified year.
     *
     * @param year The year to display
     */
    public void setYear(int year) {
        this.currentYear = year;
        updateCalendar(user);
        // Notify the year change handler
        if (yearChangeHandler != null) {
            yearChangeHandler.accept(year);
        }
    }

    /**
     * Updates the calendar display based on given user and current year.
     *
     * @param updatedUser The latest user data to use for rendering the calendar
     */
    public void updateCalendar(User updatedUser) {
        // Update user reference to latest data
        if (updatedUser != null) {
            this.user = updatedUser;
        }
        // Clear previous content
        removeAll();
        // Build the off day map for quick lookup
        buildOffDayMap();
        // Create year navigation header
        add(createYearHeader());
        // Create the calendar grid (3 rows x 4 months each)
        Div calendarGrid = new Div();
        calendarGrid.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexWrap.WRAP, LumoUtility.Width.FULL);
        // Add each month
        for (int monthIndex = 0; monthIndex < 12; monthIndex++) {
            calendarGrid.add(createMonthComponent(Month.of(monthIndex + 1)));
        }
        add(calendarGrid);
        // Add legend
        add(createLegend());
    }
}
