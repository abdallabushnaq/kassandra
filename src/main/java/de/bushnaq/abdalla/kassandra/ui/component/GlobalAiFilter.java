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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import de.bushnaq.abdalla.kassandra.ai.AiFilterGenerator;
import de.bushnaq.abdalla.kassandra.ai.AiFilterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Enhanced global filter component that supports both simple text search and natural language queries.
 * This component integrates with NaturalLanguageSearchService to parse complex search queries.
 */
public class GlobalAiFilter<T> extends HorizontalLayout {

    private static final Logger          logger = LoggerFactory.getLogger(GlobalAiFilter.class);
    private final        AiFilterService aiFilterService;
    private final        String          entityType;
    private final        ObjectMapper    filterMapper;
    private final        Grid<T>         grid;
    private final        TextField       searchField;
    private final        Span            statusSpan;

    public GlobalAiFilter(String fieldId,
                          Grid<T> grid,
                          AiFilterService aiFilterService,
                          ObjectMapper mapper,
                          String entityType) {

        this.grid            = grid;
        this.aiFilterService = aiFilterService;
        this.entityType      = entityType;

        // Create a separate ObjectMapper for filtering that includes @JsonIgnore fields
        this.filterMapper = mapper.copy();
        // Use custom annotation introspector that ignores @JsonIgnore but preserves other annotations
//        this.filterMapper.setAnnotationIntrospector(new FilterAnnotationIntrospector());

        setAlignItems(FlexComponent.Alignment.CENTER);
        setSpacing(true);

        // Create search field
        searchField = new TextField();
        searchField.setId(fieldId);
        searchField.setPlaceholder("AI powered filter");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.ON_CHANGE); // Use ON_CHANGE but we'll control when to actually search
        searchField.setWidth("350px");

        // Add magnifying glass icon as prefix
        Icon searchIcon = new Icon(VaadinIcon.SEARCH);
        searchIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
        searchField.setPrefixComponent(searchIcon);

        // Create help button
        Button helpButton = new Button(new Icon(VaadinIcon.QUESTION_CIRCLE));
        helpButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        helpButton.getElement().setAttribute("title", "Search Help");
        helpButton.addClickListener(e -> showSearchHelp());

        // Create status span for feedback
        statusSpan = new Span();
        statusSpan.getStyle().set("font-size", "var(--lumo-font-size-s)");
        statusSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
        statusSpan.setVisible(false);

        // Add search listener - use a more reliable approach for Enter key
        searchField.getElement().addEventListener("keydown", event -> {
            if ("Enter".equals(event.getEventData().getString("event.key"))) {
                performSearch();
            }
        }).addEventData("event.key");

        // Also trigger when field loses focus (blur) if there's a value
        searchField.addBlurListener(e -> {
            if (searchField.getValue() != null && !searchField.getValue().trim().isEmpty()) {
                performSearch();
            }
        });

        // Handle clear button - immediately clear filters when field is cleared
        searchField.addValueChangeListener(e -> {
            if (e.getValue() == null || e.getValue().trim().isEmpty()) {
                clearFilters();
            }
            // Don't trigger search on value change - only on Enter or blur
        });

        add(searchField, helpButton, statusSpan);
    }

    private void applySearchQuery(Pattern regexPattern) {
        ListDataProvider<T> dataProvider = (ListDataProvider<T>) grid.getDataProvider();
        dataProvider.setFilter(item -> {
                    try {
                        String json = filterMapper.writerWithDefaultPrettyPrinter().writeValueAsString(item);
                        // Apply the LLM-generated regex pattern
                        return regexPattern.matcher(json).find();
                    } catch (JsonProcessingException e) {
                        logger.error("Error serializing item to JSON for filtering", e);
                        return false;
                    }
                }
        );

    }

    private void clearFilters() {
        ListDataProvider<T> dataProvider = (ListDataProvider<T>) grid.getDataProvider();
        dataProvider.clearFilters();
        statusSpan.setVisible(false);
    }

    private String getEntitySpecificHelpText() {
        return switch (entityType) {
            case "Version" -> """
                    Smart Search Examples for Versions:
                    
                    • Simple: "1.2.3" → searches for exact version
                    • Version comparisons: "version greater than 1.0.0"
                    • Version ranges: "versions between 1.0.0 and 2.0.0"
                    • Beta/Alpha: "name contains beta" or "alpha versions"
                    • Date ranges: "versions created after January 2024"
                    • Combined: "beta versions created after 2024"
                    
                    Available columns: name, key, created, updated
                    """;
            case "Product" -> """
                    Smart Search Examples for Products:
                    
                    • Simple: "Project ABC" → searches everywhere
                    • Column-specific: "name:Product" or "key:PROJ-123"
                    • Natural language: "products created after January 2024"
                    • Date ranges: "items created before 2024-12-01"
                    • Combined: "name contains project created after january"
                    
                    Available columns: name, key, created, updated
                    """;
            default -> """
                    Smart Search Examples:
                    
                    • Simple: "search term" → searches everywhere
                    • Column-specific: "name:value" or "key:identifier"
                    • Natural language: "items created after January 2024"
                    • Date ranges: "items created before 2024-12-01"
                    
                    Available columns: name, key, created, updated
                    """;
        };
    }

    public TextField getSearchField() {
        return searchField;
    }

    private void performSearch() {
        String searchValue = searchField.getValue();

        if (searchValue == null || searchValue.trim().isEmpty()) {
            clearFilters();
            return;
        }
        int    tryCount    = 10;
        String regexString = "";
//        do {
        // Parse the query using natural language service with entity type
        try {
//                regexString = aiFilterService.parseQuery(searchValue, entityType, AiFilterGenerator.FilterType.JAVASCRIPT);
//                Pattern regexPattern = Pattern.compile(regexString);
//                applySearchQuery(regexPattern);
            AiFilterGenerator.FilterType filterType = AiFilterGenerator.FilterType.JAVASCRIPT;
            switch (filterType) {
                case JAVASCRIPT: {
                    // Parse the query using JavaScript generation
                    String              javascriptFunction = aiFilterService.parseQuery(searchValue, entityType, filterType);
                    ListDataProvider<T> dataProvider       = (ListDataProvider<T>) grid.getDataProvider();
                    dataProvider.setFilter(item -> {
//                                    String json = filterMapper.writerWithDefaultPrettyPrinter().writeValueAsString(item);
                                // Apply the LLM-generated regex pattern
                                return aiFilterService.applyJavaScriptSearchQuery(javascriptFunction, item, LocalDate.now());
                            }
                    );

//                        List<T> filtered = aiFilterService.applyJavaScriptSearchQuery(javascriptFunction, testProducts, now);
//                        System.out.println("\n=== Products matched by JavaScript filter ===");
//                        System.out.println("JavaScript function: " + javascriptFunction);
//                        for (T product : filtered) {
//                            String json = filterMapper.writeValueAsString(product);
//                            System.out.println(json);
//                        }
//                        return filtered;
                }
                case JAVA: {
                    // Parse the query using Java generation and get compiled predicate
                    var javaPredicate = aiFilterService.parseQueryToPredicate(searchValue, entityType, LocalDate.now());
//                        List<T>             filtered      = testProducts.stream().filter(javaPredicate).collect(Collectors.toList());
                    ListDataProvider<T> dataProvider = (ListDataProvider<T>) grid.getDataProvider();
//                        dataProvider.setFilter(javaPredicate);
                    dataProvider.setFilter(javaPredicate::test
                    );

//                        System.out.println("\n=== Products matched by Java filter ===");
//                        for (T product : filtered) {
//                            String json = filterMapper.writeValueAsString(product);
//                            System.out.println(json);
//                        }
//                        return filtered;
                }
            }
        } catch (PatternSyntaxException e) {
            logger.error("Invalid regex pattern '{}', falling back to simple text search: {}", regexString, e.getMessage());
        }
//        } while (--tryCount > 0);

        // Show feedback to user about what was understood
        showSearchFeedback(regexString, searchValue);
    }

    private void showErrorFeedback() {
        statusSpan.setText("Using simple text search");
        statusSpan.getStyle().set("color", "var(--lumo-error-color)");
        statusSpan.setVisible(true);
    }

    private void showSearchFeedback(String searchQuery, String originalQuery) {
        StringBuilder feedback = new StringBuilder();

        feedback.append("Using pattern: '").append(searchQuery).append("'");

        if (!feedback.isEmpty()) {
            statusSpan.setText(feedback.toString().trim());
            statusSpan.getStyle().set("color", "var(--lumo-success-color)");
            statusSpan.setVisible(true);
        } else {
            statusSpan.setVisible(false);
        }
    }

    private void showSearchHelp() {
        String helpText = getEntitySpecificHelpText();

        Notification notification = new Notification();
        notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
        notification.setDuration(8000);
        notification.setPosition(Notification.Position.TOP_CENTER);

        Span content = new Span(helpText);
        content.getStyle().set("white-space", "pre-line");
        notification.add(content);

        notification.open();
    }

    /**
     * Custom annotation introspector that ignores @JsonIgnore annotations
     * but preserves all other Jackson annotations.
     */
    private static class FilterAnnotationIntrospector extends JacksonAnnotationIntrospector {
        @Override
        public boolean hasIgnoreMarker(com.fasterxml.jackson.databind.introspect.AnnotatedMember m) {
            // Don't ignore fields marked with @JsonIgnore for filtering purposes
            // but still process other ignore markers from the parent class
            if (m.hasAnnotation(JsonIgnore.class)) {
                return false;
            }
            return super.hasIgnoreMarker(m);
        }
    }
}
