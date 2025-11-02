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
import de.bushnaq.abdalla.kassandra.dto.Availability;
import de.bushnaq.abdalla.kassandra.dto.User;
import lombok.Getter;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A component that displays a year calendar with user's availability
 * visualized as colored backgrounds filling each day according to the availability percentage.
 */
public class AvailabilityCalendarComponent extends VerticalLayout {

    public static final  String                CALENDAR_NEXT_YEAR_BTN     = "availability-calendar-next-year-btn";
    public static final  String                CALENDAR_PREV_YEAR_BTN     = "availability-calendar-prev-year-btn";
    private static final String                DAY_SIZE_PX                = "36px"; // Increased from 24px (50% larger)
    public static final  String                LEGEND_ITEM_ID_0_PERCENT   = "availability-calendar-legend-item-0";
    public static final  String                LEGEND_ITEM_ID_100_PERCENT = "availability-calendar-legend-item-100";
    public static final  String                LEGEND_ITEM_ID_20_PERCENT  = "availability-calendar-legend-item-20";
    public static final  String                LEGEND_ITEM_ID_40_PERCENT  = "availability-calendar-legend-item-40";
    public static final  String                LEGEND_ITEM_ID_60_PERCENT  = "availability-calendar-legend-item-60";
    public static final  String                LEGEND_ITEM_ID_80_PERCENT  = "availability-calendar-legend-item-80";
    public static final  String                LEGEND_ITEM_ID_PREFIX      = "availability-calendar-legend-item-";
    private final        Map<LocalDate, Float> availabilityMap            = new HashMap<>();
    private final        Map<Float, String>    colorMap                   = new HashMap<>();
    @Getter
    private              int                   currentYear;
    private final        Consumer<LocalDate>   dayClickHandler;
    private              List<Availability>    sortedAvailabilities;
    private              User                  user;
    private              Consumer<Integer>     yearChangeHandler;

    /**
     * Creates a new year calendar component for the given user and year.
     *
     * @param user            The user whose calendar should be displayed
     * @param year            The year to display
     * @param dayClickHandler Consumer that handles clicks on calendar days
     */
    public AvailabilityCalendarComponent(User user, int year, Consumer<LocalDate> dayClickHandler) {
        this.user            = user;
        this.currentYear     = year;
        this.dayClickHandler = dayClickHandler;

        // Set fixed width that won't resize
        setWidth("1200px");
        setHeight("auto");
        setPadding(false);
        setSpacing(false);

        // Prevent resizing of the component
        getStyle()
                .set("flex-shrink", "0")
                .set("flex-grow", "0");

        // Initialize color map with distinct colors for different availability levels
        initializeColorMap();
    }

    /**
     * Builds a map of dates to availability values for quick lookup.
     * Uses the sorted availabilities list to determine availability for each day.
     */
    private void buildAvailabilityMap() {
        availabilityMap.clear();

        if (sortedAvailabilities == null || sortedAvailabilities.isEmpty()) {
            return;
        }

        // Process each day in the current year
        LocalDate yearStart = LocalDate.of(currentYear, 1, 1);
        LocalDate yearEnd   = LocalDate.of(currentYear, 12, 31);

        for (LocalDate date = yearStart; !date.isAfter(yearEnd); date = date.plusDays(1)) {
            // Find the applicable availability for this date
            Float availability = getAvailabilityForDate(date);
            if (availability != null) {
                availabilityMap.put(date, availability);
            }
        }
    }

    /**
     * Creates a component representing a single day in the calendar.
     * The day is filled with a colored background from the bottom up to the availability percentage.
     */
    private Component createDayComponent(int day, LocalDate date, boolean isWeekend, boolean isFillingDay) {
        return createDayComponent(day, date, isWeekend, isFillingDay, false);
    }

    /**
     * Creates a component representing a single day in the calendar.
     */
    private Component createDayComponent(int day, LocalDate date, boolean isWeekend, boolean isFillingDay, boolean isToday) {
        Div dayComponent = new Div();

        // Create a container for the day number and the availability fill
        Div dayContent = new Div();
        dayContent.setText(String.valueOf(day));
        dayContent.getStyle()
                .set("position", "relative")
                .set("z-index", "1")
                .set("font-weight", "bold")
                .set("font-size", "0.85rem");

        // Set common styles for all day components
        dayComponent.getStyle()
                .set("width", DAY_SIZE_PX)
                .set("height", DAY_SIZE_PX)
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("cursor", "default")
                .set("position", "relative")
                .set("box-sizing", "border-box")
                .set("border", "1px solid #e0e0e0");

        if (isFillingDay) {
            dayComponent.addClassName("calendar-filling-day");
        } else if (isWeekend) {
            dayComponent.addClassName("calendar-weekend-day");
        } else {
            dayComponent.addClassName("calendar-normal-day");
        }

        // Add availability fill if applicable and not a filling day
        if (!isFillingDay && availabilityMap.containsKey(date)) {
            float  availability      = availabilityMap.get(date);
            String color             = getColorForAvailability(availability);
            int    fillHeightPercent = Math.round(availability * 100);

            // Create a div for the colored background
            Div fillDiv = new Div();
            fillDiv.getStyle()
                    .set("position", "absolute")
                    .set("bottom", "0")
                    .set("left", "0")
                    .set("right", "0")
                    .set("height", fillHeightPercent + "%")
                    .set("background-color", color)
                    .set("z-index", "0");

            dayComponent.add(fillDiv);

            // Make days with availability clickable
            dayComponent.getStyle().set("cursor", "pointer");
            dayComponent.getElement().addEventListener("click", event -> {
                if (dayClickHandler != null) {
                    dayClickHandler.accept(date);
                }
            });
        }

        // Add the day number on top
        dayComponent.add(dayContent);

        // Mark today with a special highlight
        if (isToday) {
            dayComponent.addClassName("calendar-today");
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

        // Create legend items for different availability levels
        legend.add(createLegendItem("100%", 1.0f));
        legend.add(createLegendItem("80%", 0.8f));
        legend.add(createLegendItem("60%", 0.6f));
        legend.add(createLegendItem("40%", 0.4f));
        legend.add(createLegendItem("20%", 0.2f));
        legend.add(createLegendItem("0%", 0.0f));

        return legend;
    }

    /**
     * Creates a single legend item with a color box and label.
     */
    private Component createLegendItem(String label, float availability) {
        HorizontalLayout item = new HorizontalLayout();
        item.setId(LEGEND_ITEM_ID_PREFIX + label.replace("%", ""));
        item.setSpacing(true);
        item.setAlignItems(FlexComponent.Alignment.CENTER);

        // Color box
        Div    colorBox = new Div();
        String color    = getColorForAvailability(availability);
        colorBox.getStyle()
                .set("width", "16px")
                .set("height", "16px")
                .set("background-color", color)
                .set("border", "1px solid #ccc");

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
        monthComponent.getStyle().set("width", "272px");
        monthComponent.getStyle().set("box-sizing", "border-box");

        // Month name header
        Span monthName = new Span(month.toString());
        monthName.addClassNames(
                "calendar-month-name",
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
                .set("width", "100%");

        calendarGrid.getStyle().set("margin-top", "4px");
        LocalDate today = ParameterOptions.getLocalNow().toLocalDate();

        // First day of month
        LocalDate firstOfMonth = LocalDate.of(currentYear, month, 1);
        // Determine first day of week for this month (0 = Monday, 6 = Sunday in our display)
        int firstDayOfWeek = firstOfMonth.getDayOfWeek().getValue() - 1;
        // Last day of month
        int lastDay = firstOfMonth.lengthOfMonth();

        // Add filling days from previous month
        if (firstDayOfWeek > 0) {
            LocalDate prevMonthDate    = firstOfMonth.minusMonths(1);
            int       prevMonthLastDay = prevMonthDate.lengthOfMonth();
            for (int i = 0; i < firstDayOfWeek; i++) {
                int day = prevMonthLastDay - firstDayOfWeek + i + 1;
                LocalDate date = LocalDate.of(month.getValue() == 1 ? currentYear - 1 : currentYear,
                        month.getValue() == 1 ? 12 : month.getValue() - 1, day);
                calendarGrid.add(createDayComponent(day, date, true, true));
            }
        }

        // Add days for current month
        for (int day = 1; day <= lastDay; day++) {
            LocalDate date      = LocalDate.of(currentYear, month, day);
            boolean   isWeekend = isWeekend(date);
            boolean   isToday   = date.equals(today);
            calendarGrid.add(createDayComponent(day, date, isWeekend, false, isToday));
        }

        // Add filling days from next month
        int fillingDaysNeeded = 7 - ((firstDayOfWeek + lastDay) % 7);
        if (fillingDaysNeeded < 7) {
            for (int i = 1; i <= fillingDaysNeeded; i++) {
                LocalDate date = LocalDate.of(month.getValue() == 12 ? currentYear + 1 : currentYear,
                        month.getValue() == 12 ? 1 : month.getValue() + 1, i);
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
     * Gets the availability value for a specific date.
     * Uses the sorted availabilities list (sorted by start date descending).
     *
     * @param date The date to get availability for
     * @return The availability value (0.0 to 1.0) or null if not found
     */
    private Float getAvailabilityForDate(LocalDate date) {
        // Iterate through sorted availabilities (latest first)
        // and find the first one that starts on or before the given date
        for (Availability availability : sortedAvailabilities) {
            if (!availability.getStart().isAfter(date)) {
                return availability.getAvailability();
            }
        }
        return null;
    }

    /**
     * Gets the color for a specific availability value.
     *
     * @param availability The availability value (0.0 to 1.0)
     * @return The CSS color string
     */
    private String getColorForAvailability(float availability) {
        // Find the closest matching color in the map
        float closestKey = 0.0f;
        float minDiff    = Float.MAX_VALUE;

        for (float key : colorMap.keySet()) {
            float diff = Math.abs(availability - key);
            if (diff < minDiff) {
                minDiff    = diff;
                closestKey = key;
            }
        }

        return colorMap.get(closestKey);
    }

    /**
     * Initialize color map with distinct colors for different availability levels.
     * Using light, pastel colors similar to OffDaysCalendarComponent for consistency.
     */
    private void initializeColorMap() {
        // Define light pastel colors for different availability percentages
        // Matching the lightness of --calendar-sick-color: #F8D7DA
        colorMap.put(1.0f, "#D4EDDA"); // Light Green (100%) - matches vacation color
        colorMap.put(0.8f, "#D1F2EB"); // Pale Teal (80%)
        colorMap.put(0.6f, "#FFF3CD"); // Light Yellow (60%)
        colorMap.put(0.4f, "#FFE5CC"); // Light Orange (40%)
        colorMap.put(0.2f, "#F8D7DA"); // Light Red (20%) - matches sick color
        colorMap.put(0.0f, "#E2E3E5"); // Light Gray (0%)
    }

    /**
     * Checks if a date falls on a weekend (Saturday or Sunday).
     */
    private boolean isWeekend(LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        return dayOfWeek == 6 || dayOfWeek == 7; // Saturday or Sunday
    }

    /**
     * Updates the calendar to display the specified year.
     *
     * @param year The year to display
     */
    public void setYear(int year) {
        this.currentYear = year;
        updateCalendar(user, sortedAvailabilities);
        // Notify the year change handler
        if (yearChangeHandler != null) {
            yearChangeHandler.accept(year);
        }
    }

    /**
     * Sets the handler that will be called when the year changes.
     *
     * @param yearChangeHandler Consumer that handles year changes
     */
    public void setYearChangeHandler(Consumer<Integer> yearChangeHandler) {
        this.yearChangeHandler = yearChangeHandler;
    }

    /**
     * Updates the calendar display based on given user and availability data.
     *
     * @param updatedUser          The latest user data to use for rendering the calendar
     * @param sortedAvailabilities The list of availabilities sorted by start date descending
     */
    public void updateCalendar(User updatedUser, List<Availability> sortedAvailabilities) {
        // Update user reference to latest data
        if (updatedUser != null) {
            this.user = updatedUser;
        }

        // Update sorted availabilities
        this.sortedAvailabilities = sortedAvailabilities;

        // Clear previous content
        removeAll();

        // Build the availability map for quick lookup
        buildAvailabilityMap();

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

