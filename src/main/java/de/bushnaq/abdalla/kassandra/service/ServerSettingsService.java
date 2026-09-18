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

package de.bushnaq.abdalla.kassandra.service;

import de.bushnaq.abdalla.kassandra.dao.ServerSettingDAO;
import de.bushnaq.abdalla.kassandra.config.KassandraProperties;
import de.bushnaq.abdalla.kassandra.dto.ServerSetting;
import de.bushnaq.abdalla.kassandra.dto.ServerSettingTestResult;
import de.bushnaq.abdalla.kassandra.repository.ServerSettingRepository;
import de.bushnaq.abdalla.kassandra.security.SecuritySecretService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

/**
 * Validates, persists, and safely exposes administrator-managed server settings.
 */
@Service
public class ServerSettingsService {

    private static final Duration TEST_TIMEOUT                = Duration.ofSeconds(10);
    private static final String   CATEGORY_ENABLED_KEY_PREFIX = "kassandra.server-settings.category.";

    @Autowired
    private Environment             environment;
    @Autowired
    private SecuritySecretService   securitySecretService;
    @Autowired
    private ServerSettingsCatalogue serverSettingsCatalogue;
    @Autowired
    private ServerSettingRepository serverSettingRepository;

    /**
     * Creates values for catalogue entries which have not yet been persisted.
     * Existing operator-managed database values always take precedence.
     */
    @EventListener(ContextRefreshedEvent.class)
    @Transactional
    public void initializeMissingSettings() {
        for (ServerSettingsCatalogue.Definition definition : serverSettingsCatalogue.list()) {
            if (serverSettingRepository.existsById(definition.key())) {
                continue;
            }
            String value = environment.getProperty(definition.key(), definition.defaultValue());
            validate(definition, value);
            ServerSettingDAO setting = newSetting(definition);
            setting.setEncrypted(definition.secret() && !value.isBlank());
            setting.setValue(setting.isEncrypted() ? securitySecretService.encrypt(value) : value);
            serverSettingRepository.save(setting);
        }
    }

    /**
     * Lists supported settings with safe values for administrator display.
     *
     * @return settings sorted by category and label
     */
    @Transactional
    public List<ServerSetting> list() {
        return serverSettingsCatalogue.list().stream()
                .sorted(Comparator.comparing((ServerSettingsCatalogue.Definition definition) -> definition.category().order())
                        .thenComparing(ServerSettingsCatalogue.Definition::label))
                .map(this::toDto)
                .toList();
    }

    /**
     * Gets a current non-secret setting value.
     *
     * @param key          setting key
     * @param defaultValue fallback value
     * @return persisted setting value
     * @throws IllegalArgumentException when the setting is unknown or secret
     */
    @Transactional
    public String value(String key, String defaultValue) {
        ServerSettingsCatalogue.Definition definition = serverSettingsCatalogue.get(key);
        if (definition.secret()) {
            throw new IllegalArgumentException("Secret settings cannot be read as plain text");
        }
        return serverSettingRepository.findById(key)
                .map(ServerSettingDAO::getValue)
                .filter(value -> !value.isBlank())
                .orElse(defaultValue);
    }

    /**
     * Gets a current secret setting value for an application integration.
     *
     * @param key secret setting key
     * @return decrypted secret or an empty string when none is configured
     * @throws IllegalArgumentException when the setting is not a secret
     */
    @Transactional
    public String secretValue(String key) {
        ServerSettingsCatalogue.Definition definition = serverSettingsCatalogue.get(key);
        if (!definition.secret()) {
            throw new IllegalArgumentException("Only secret settings can be decrypted");
        }
        return serverSettingRepository.findById(key)
                .filter(ServerSettingDAO::isEncrypted)
                .map(ServerSettingDAO::getValue)
                .map(securitySecretService::decrypt)
                .orElse("");
    }

    /**
     * Tests a URL setting without persisting a proposed value.
     *
     * @param key            setting key
     * @param candidateValue proposed setting value, or null to test the stored value
     * @return connection test result
     * @throws IllegalArgumentException when the setting has no registered test
     */
    public ServerSettingTestResult test(String key, String candidateValue) {
        ServerSettingsCatalogue.Definition definition = serverSettingsCatalogue.get(key);
        requireEnabledCategory(definition.category());
        if (!definition.testable()) {
            throw new IllegalArgumentException("This setting does not support a connection test");
        }
        String value = candidateValue == null || candidateValue.isBlank()
                ? value(key, definition.defaultValue())
                : candidateValue;
        validate(definition, value);
        String path = "stable-diffusion.api-url".equals(key) ? "/sdapi/v1/options" : "/api/v1/models";
        try {
            HttpStatusCode status = WebClient.create(value).get().uri(path).exchangeToMono(response -> {
                HttpStatusCode responseStatus = response.statusCode();
                return response.releaseBody().thenReturn(responseStatus);
            }).block(TEST_TIMEOUT);
            return testResult(status != null && status.is2xxSuccessful(),
                    status == null ? "The server did not return a response." : "Server returned HTTP " + status.value() + ".");
        } catch (Exception e) {
            return testResult(false, "Connection failed: " + e.getMessage());
        }
    }

    /**
     * Updates one setting after validation. Empty secret values retain an existing secret.
     *
     * @param key         setting key
     * @param value       proposed value
     * @param clearSecret whether to remove a stored secret
     * @return updated safe setting representation
     * @throws IllegalArgumentException when the value is invalid
     */
    @Transactional
    public ServerSetting update(String key, String value, boolean clearSecret) {
        ServerSettingsCatalogue.Definition definition = serverSettingsCatalogue.get(key);
        requireEnabledCategory(definition.category());
        ServerSettingDAO setting = serverSettingRepository.findById(key).orElseGet(() -> newSetting(definition));
        if (definition.secret() && clearSecret) {
            setting.setEncrypted(false);
            setting.setValue("");
        } else if (!definition.secret() || (value != null && !value.isBlank())) {
            value = value == null || value.isBlank() ? definition.defaultValue() : value;
            validate(definition, value);
            setting.setEncrypted(definition.secret());
            setting.setValue(definition.secret() ? securitySecretService.encrypt(value) : value.trim());
        }
        serverSettingRepository.saveAndFlush(setting);
        if ("kassandra.holidays.look-ahead-months".equals(key)) {
            KassandraProperties.setHolidayLookAheadMonths(Long.parseLong(setting.getValue()));
        }
        return toDto(definition, setting);
    }

    /**
     * Updates the enabled state of an optional server settings category.
     *
     * @param categoryKey category key
     * @param enabled     whether settings in the category can be managed
     * @throws IllegalArgumentException when the category does not support enablement
     */
    @Transactional
    public void updateCategoryEnabled(String categoryKey, boolean enabled) {
        ServerSettingsCatalogue.Category category = serverSettingsCatalogue.getCategory(categoryKey);
        if (!category.allowDisable()) {
            throw new IllegalArgumentException(category.label() + " cannot be disabled");
        }
        String           key     = categoryEnabledKey(category);
        ServerSettingDAO setting = serverSettingRepository.findById(key).orElseGet(() -> newCategorySetting(key));
        setting.setValue(Boolean.toString(enabled));
        serverSettingRepository.saveAndFlush(setting);
    }

    private String categoryEnabledKey(ServerSettingsCatalogue.Category category) {
        return CATEGORY_ENABLED_KEY_PREFIX + category.key() + ".enabled";
    }

    private boolean categoryEnabled(ServerSettingsCatalogue.Category category) {
        return serverSettingRepository.findById(categoryEnabledKey(category))
                .map(ServerSettingDAO::getValue)
                .map(Boolean::parseBoolean)
                .orElse(category.enabledByDefault());
    }

    private ServerSettingDAO newCategorySetting(String key) {
        ServerSettingDAO setting = new ServerSettingDAO();
        setting.setEncrypted(false);
        setting.setKey(key);
        return setting;
    }

    private ServerSettingDAO newSetting(ServerSettingsCatalogue.Definition definition) {
        ServerSettingDAO setting = new ServerSettingDAO();
        setting.setEncrypted(false);
        setting.setKey(definition.key());
        setting.setValue(definition.defaultValue());
        return setting;
    }

    private ServerSettingTestResult testResult(boolean successful, String message) {
        ServerSettingTestResult result = new ServerSettingTestResult();
        result.setMessage(message);
        result.setSuccessful(successful);
        return result;
    }

    private ServerSetting toDto(ServerSettingsCatalogue.Definition definition) {
        return toDto(definition, serverSettingRepository.findById(definition.key()).orElseGet(() -> newSetting(definition)));
    }

    private ServerSetting toDto(ServerSettingsCatalogue.Definition definition, ServerSettingDAO setting) {
        ServerSetting result = new ServerSetting();
        result.setCategoryIcon(definition.category().icon());
        result.setCategoryAllowDisable(definition.category().allowDisable());
        result.setCategoryEnabled(categoryEnabled(definition.category()));
        result.setCategoryKey(definition.category().key());
        result.setCategoryLabel(definition.category().label());
        result.setConfigured(!setting.getValue().isBlank());
        result.setDescription(definition.description());
        result.setKey(definition.key());
        result.setLabel(definition.label());
        result.setMaximum(definition.maximum());
        result.setMinimum(definition.minimum());
        result.setOptions(definition.options());
        result.setRestartRequired(definition.restartRequired());
        result.setSecret(definition.secret());
        result.setTestable(definition.testable());
        result.setType(definition.type().name());
        result.setValue(definition.secret() ? null : setting.getValue().isBlank() ? definition.defaultValue() : setting.getValue());
        return result;
    }

    private void requireEnabledCategory(ServerSettingsCatalogue.Category category) {
        if (!categoryEnabled(category)) {
            throw new IllegalArgumentException(category.label() + " is disabled");
        }
    }

    private void validate(ServerSettingsCatalogue.Definition definition, String value) {
        if (value == null || value.isBlank() && !definition.secret() && definition.type() != ServerSettingsCatalogue.Type.TEXT) {
            throw new IllegalArgumentException(definition.label() + " is required");
        }
        try {
            switch (definition.type()) {
                case BOOLEAN -> {
                    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                        throw new IllegalArgumentException(definition.label() + " must be true or false");
                    }
                }
                case COLOUR -> {
                    if (!value.matches("^#[0-9a-fA-F]{6}$")) {
                        throw new IllegalArgumentException(definition.label() + " must be a six-digit CSS hex colour");
                    }
                }
                case DECIMAL -> range(definition, Double.parseDouble(value));
                case INTEGER -> range(definition, Long.parseLong(value));
                case PASSWORD, TEXT -> maximumLength(definition, value);
                case URL -> {
                    maximumLength(definition, value);
                    if (!value.matches("^https?://[^\\s]+$")) {
                        throw new IllegalArgumentException(definition.label() + " must be an HTTP or HTTPS URL");
                    }
                }
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(definition.label() + " must be a number", e);
        }
    }

    private void maximumLength(ServerSettingsCatalogue.Definition definition, String value) {
        if (definition.maximum() != null && value.length() > definition.maximum()) {
            throw new IllegalArgumentException(definition.label() + " is too long");
        }
    }

    private void range(ServerSettingsCatalogue.Definition definition, double value) {
        if (!Double.isFinite(value) || definition.minimum() != null && value < definition.minimum()
                || definition.maximum() != null && value > definition.maximum()) {
            throw new IllegalArgumentException(definition.label() + " is outside the allowed range");
        }
    }
}
