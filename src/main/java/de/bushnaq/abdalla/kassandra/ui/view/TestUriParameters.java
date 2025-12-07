package de.bushnaq.abdalla.kassandra.ui.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Route("test")
@PageTitle("test-uri-parameters")
@PermitAll // When security is enabled, allow all authenticated users
@Menu(order = 0, icon = "vaadin:calendar", title = "TestUriParameters")
@Slf4j
class TestUriParameters extends Main implements /*BeforeEnterObserver,*/ AfterNavigationObserver {

    private final boolean                    dataLoaded      = false;
    private final MultiSelectListBox<String> sprintsSelect;
    private       boolean                    updatingFromUrl = false;
    private final MultiSelectListBox<String> usersSelect;

    TestUriParameters() {

        setWidthFull();
//        setPadding(false);
//        setSpacing(false);
        addClassName("sprints-view");

        // Add multi-select boxes for URL parameter testing
        HorizontalLayout selectLayout = new HorizontalLayout();
        selectLayout.setPadding(true);
        selectLayout.setSpacing(true);

        // Create multi-select box for sprints parameter
        VerticalLayout sprintsLayout = new VerticalLayout();
        sprintsLayout.setPadding(false);
        sprintsLayout.setSpacing(false);
        Span sprintsLabel = new Span("Sprints");
        sprintsLabel.getStyle().set("font-weight", "bold");
        sprintsSelect = new MultiSelectListBox<>();
        sprintsSelect.setItems("1", "2", "3");
        sprintsSelect.addSelectionListener(event -> {
            if (!updatingFromUrl) {
                updateUrlParameter("sprints", event.getValue());
            }
        });
        sprintsLayout.add(sprintsLabel, sprintsSelect);

        // Create multi-select box for users parameter
        VerticalLayout usersLayout = new VerticalLayout();
        usersLayout.setPadding(false);
        usersLayout.setSpacing(false);
        Span usersLabel = new Span("Users");
        usersLabel.getStyle().set("font-weight", "bold");
        usersSelect = new MultiSelectListBox<>();
        usersSelect.setItems("1", "2", "3");
        usersSelect.addSelectionListener(event -> {
            if (!updatingFromUrl) {
                updateUrlParameter("users", event.getValue());
            }
        });
        usersLayout.add(usersLabel, usersSelect);

        selectLayout.add(sprintsLayout, usersLayout);
        add(selectLayout);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        log.info("");
        log.info("== afterNavigation start");
        // Capture and log URL parameters when navigating to the page
        QueryParameters           queryParameters = event.getLocation().getQueryParameters();
        Map<String, List<String>> parametersMap   = queryParameters.getParameters();

        if (!parametersMap.isEmpty()) {
            log.info("URL Parameters captured:");
            parametersMap.forEach((key, values) -> {
                log.info("  {} = {}", key, String.join(", ", values));
            });
        } else {
            log.info("No URL parameters present");
        }

        // Populate multi-select boxes from URL parameters (or clear them if no params)
        populateMultiSelectFromUrl(parametersMap);
        log.info("== afterNavigation end");
    }

    private void populateMultiSelectFromUrl(Map<String, List<String>> parametersMap) {
        // Set flag to prevent selection listeners from triggering navigation
        updatingFromUrl = true;

        try {
            // Populate sprints multi-select
            if (parametersMap.containsKey("sprints")) {
                List<String> sprintValues = parametersMap.get("sprints");
                // Filter to only valid values (1, 2, 3)
                java.util.Set<String> validSprintValues = sprintValues.stream()
                        .filter(v -> v.equals("1") || v.equals("2") || v.equals("3"))
                        .collect(java.util.stream.Collectors.toSet());

                if (!validSprintValues.isEmpty()) {
                    sprintsSelect.setValue(validSprintValues);
                    log.info("Populated sprints multi-select with values: {}", validSprintValues);
                } else {
                    sprintsSelect.deselectAll();
                    log.info("Cleared sprints multi-select as no valid values found");
                }
            } else {
                sprintsSelect.deselectAll();
                log.info("Cleared sprints multi-select as no valid values found");
            }

            // Populate users multi-select
            if (parametersMap.containsKey("users")) {
                List<String> userValues = parametersMap.get("users");
                // Filter to only valid values (1, 2, 3)
                java.util.Set<String> validUserValues = userValues.stream()
                        .filter(v -> v.equals("1") || v.equals("2") || v.equals("3"))
                        .collect(java.util.stream.Collectors.toSet());

                if (!validUserValues.isEmpty()) {
                    usersSelect.setValue(validUserValues);
                    log.info("Populated users multi-select with values: {}", validUserValues);
                } else {
                    usersSelect.deselectAll();
                    log.info("Cleared users multi-select as no valid values found");
                }
            } else {
                usersSelect.deselectAll();
                log.info("Cleared users multi-select as no valid values found");
            }
        } finally {
            // Always reset the flag
            updatingFromUrl = false;
        }
    }

    private void updateUrlParameter(String paramName, java.util.Set<String> selectedValues) {
        UI              ui              = UI.getCurrent();
        Location        currentLocation = ui.getInternals().getActiveViewLocation();
        QueryParameters currentParams   = currentLocation.getQueryParameters();

        // Create a new map with existing parameters
        Map<String, List<String>> newParams = new java.util.HashMap<>(currentParams.getParameters());

        // Add, update, or remove the parameter based on selection
        if (selectedValues.isEmpty()) {
            newParams.remove(paramName);
            log.info("Removed URL parameter: {}", paramName);
        } else {
            List<String> valuesList = new java.util.ArrayList<>(selectedValues);
            newParams.put(paramName, valuesList);
            log.info("Updated URL parameter: {} = {}", paramName, String.join(", ", valuesList));
        }

        // Navigate to the same route with updated parameters
        log.info(" + Navigating to updated URL with parameters: {}", newParams);
        QueryParameters updatedParams = new QueryParameters(newParams);
        ui.navigate(currentLocation.getPath(), updatedParams);
    }


}

