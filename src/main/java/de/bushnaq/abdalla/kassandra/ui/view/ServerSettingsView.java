/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.bushnaq.abdalla.kassandra.dto.ServerSetting;
import de.bushnaq.abdalla.kassandra.dto.ServerSettingTestResult;
import de.bushnaq.abdalla.kassandra.dto.ServerSettingUpdateRequest;
import de.bushnaq.abdalla.kassandra.rest.api.ServerSettingsApi;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides a category-first administrator experience for persisted server settings.
 */
@Route(value = ServerSettingsView.ROUTE, layout = MainLayout.class)
@PageTitle("Server Settings")
@RolesAllowed("ADMIN")
public class ServerSettingsView extends VerticalLayout {

    /**
     * Route for server settings administration.
     */
    public static final String ROUTE = "server-settings";

    private       VerticalLayout      detail;
    private       String              selectedCategory;
    private final ServerSettingsApi   serverSettingsApi;
    private       List<ServerSetting> settings = List.of();

    /**
     * Creates the dynamically generated server settings view.
     *
     * @param serverSettingsApi settings REST client
     */
    public ServerSettingsView(ServerSettingsApi serverSettingsApi) {
        this.serverSettingsApi = serverSettingsApi;
        setSizeFull();
        setPadding(true);
        getStyle().set("overflow", "hidden");
        refresh();
    }

    private Component createCategoryRow(String category, List<ServerSetting> categorySettings) {
        ServerSetting categoryDefinition = categorySettings.getFirst();
        Button categoryButton = new Button(categoryDefinition.getCategoryLabel(), event -> {
            selectedCategory = category;
            render();
        });
        categoryButton.setIcon(VaadinIcon.valueOf(categoryDefinition.getCategoryIcon()).create());
        categoryButton.setWidthFull();
        if (category.equals(selectedCategory)) {
            categoryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        } else {
            categoryButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        }
        categoryButton.getStyle().set("justify-content", "flex-start");
        return categoryButton;
    }

    private Component createEditor(ServerSetting setting, boolean categoryEnabled) {
        if ("BOOLEAN".equals(setting.getType())) {
            Checkbox field = new Checkbox();
            field.setValue(Boolean.parseBoolean(setting.getValue()));
            return saveRow(setting, field, () -> Boolean.toString(field.getValue()), categoryEnabled);
        }
        HasValue<?, String> field;
        if (setting.isSecret()) {
            PasswordField passwordField = new PasswordField();
            if (setting.isConfigured()) {
                passwordField.setPlaceholder("Configured; leave empty to retain");
            }
            field = passwordField;
        } else {
            TextField textField = new TextField();
            textField.setValue(setting.getValue() == null ? "" : setting.getValue());
            field = textField;
        }
        return saveRow(setting, (Component) field, field::getValue, categoryEnabled);
    }

    private Component createSetting(ServerSetting setting, boolean categoryEnabled) {
        VerticalLayout row = new VerticalLayout();
        row.setPadding(false);
        row.setSpacing(false);
        H3        label       = new H3(setting.getLabel());
        Paragraph description = new Paragraph(setting.getDescription());
        if (!categoryEnabled) {
            label.getStyle().set("color", "var(--lumo-disabled-text-color)");
            description.getStyle().set("color", "var(--lumo-disabled-text-color)");
        }
        row.add(label, description);
        if (setting.isRestartRequired()) {
            Paragraph restartRequired = new Paragraph("A restart is required for this change to take effect.");
            restartRequired.getStyle().set("color",
                    categoryEnabled ? "var(--lumo-secondary-text-color)" : "var(--lumo-disabled-text-color)");
            row.add(restartRequired);
        }
        row.add(createEditor(setting, categoryEnabled));
        row.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
        row.getStyle().set("padding-bottom", "var(--lumo-space-m)");
        return row;
    }

    private Map<String, List<ServerSetting>> groupedSettings() {
        Map<String, List<ServerSetting>> result = new LinkedHashMap<>();
        for (ServerSetting setting : settings) {
            result.computeIfAbsent(setting.getCategoryKey(), ignored -> new ArrayList<>()).add(setting);
        }
        return result;
    }

    private void refresh() {
        settings = serverSettingsApi.getAll();
        render();
    }

    private void render() {
        removeAll();
        add(new H1("Server Settings"), new Paragraph(
                "Changes apply immediately unless a setting is marked as requiring a restart."));
        VerticalLayout categories = new VerticalLayout();
        categories.setPadding(false);
        categories.setSpacing(false);
        categories.setWidth("220px");
        categories.setFlexShrink(0);
        groupedSettings().forEach((category, categorySettings) -> categories.add(createCategoryRow(category, categorySettings)));
        detail = new VerticalLayout();
        detail.setPadding(false);
        detail.setSpacing(true);
        detail.setHeightFull();
        detail.setMinHeight("0");
        detail.setWidth("600px");
        detail.setMaxWidth("600px");
        detail.setFlexShrink(0);
        detail.getStyle()
                .set("overflow-y", "auto")
                .set("padding-left", "var(--lumo-space-l)")
                .set("padding-right", "var(--lumo-space-l)");
        HorizontalLayout content = new HorizontalLayout(categories, detail);
        content.setWidthFull();
        content.setMinHeight("0");
        content.setPadding(true);
        content.setSpacing(true);
        addAndExpand(content);
        if (selectedCategory == null) {
            selectedCategory = "general";
        }
        renderDetail();
    }

    private void renderDetail() {
        detail.removeAll();
        settings.stream()
                .filter(setting -> selectedCategory.equals(setting.getCategoryKey()))
                .findFirst()
                .ifPresent(setting -> {
                    HorizontalLayout heading = new HorizontalLayout(new H1(setting.getCategoryLabel()));
                    heading.setWidthFull();
                    heading.setAlignItems(Alignment.CENTER);
                    if (setting.isCategoryAllowDisable()) {
                        Checkbox enabled = new Checkbox("Enabled", setting.isCategoryEnabled());
                        enabled.addValueChangeListener(event -> updateCategoryEnabled(selectedCategory, event.getValue()));
                        heading.add(enabled);
                    }
                    detail.add(heading);
                });
        settings.stream()
                .filter(setting -> selectedCategory.equals(setting.getCategoryKey()))
                .forEach(setting -> detail.add(createSetting(setting, setting.isCategoryEnabled())));
    }

    private Component saveRow(ServerSetting setting, Component field, java.util.function.Supplier<String> value, boolean categoryEnabled) {
        ((HasSize) field).setWidthFull();
        Button save = new Button("Save", event -> save(setting, value.get(), false));
        if (field instanceof HasEnabled enabledField) {
            enabledField.setEnabled(categoryEnabled);
        }
        save.setEnabled(categoryEnabled);
        HorizontalLayout result = new HorizontalLayout(field, save);
        if (setting.isTestable()) {
            Button test = new Button("Test connection", event -> test(setting, value.get()));
            test.setEnabled(categoryEnabled);
            result.add(test);
        }
        if (setting.isSecret() && setting.isConfigured()) {
            Button clear = new Button("Clear stored secret", event -> save(setting, "", true));
            clear.setEnabled(categoryEnabled);
            result.add(clear);
        }
        result.setWidthFull();
        result.setAlignItems(Alignment.END);
        return result;
    }

    private void save(ServerSetting setting, String value, boolean clearSecret) {
        try {
            ServerSettingUpdateRequest request = new ServerSettingUpdateRequest();
            request.setClearSecret(clearSecret);
            request.setValue(value);
            ServerSetting saved = serverSettingsApi.update(setting.getKey(), request);
            Notification.show(saved.isRestartRequired() ? "Saved. Restart Kassandra to apply this setting." : "Setting saved.");
            refresh();
        } catch (IllegalArgumentException | ResponseStatusException e) {
            Notification.show(e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void updateCategoryEnabled(String category, boolean enabled) {
        try {
            serverSettingsApi.updateCategoryEnabled(category, enabled);
            refresh();
        } catch (IllegalArgumentException | ResponseStatusException e) {
            Notification.show(e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void test(ServerSetting setting, String value) {
        try {
            ServerSettingUpdateRequest request = new ServerSettingUpdateRequest();
            request.setValue(value);
            ServerSettingTestResult result       = serverSettingsApi.test(setting.getKey(), request);
            Notification            notification = Notification.show(result.getMessage(), 5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(result.isSuccessful() ? NotificationVariant.LUMO_SUCCESS : NotificationVariant.LUMO_ERROR);
        } catch (IllegalArgumentException | ResponseStatusException e) {
            Notification notification = Notification.show(e.getMessage(), 5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
