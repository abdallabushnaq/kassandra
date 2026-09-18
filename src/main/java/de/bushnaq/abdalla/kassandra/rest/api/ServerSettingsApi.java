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

package de.bushnaq.abdalla.kassandra.rest.api;

import de.bushnaq.abdalla.kassandra.dto.ServerSetting;
import de.bushnaq.abdalla.kassandra.dto.ServerSettingTestResult;
import de.bushnaq.abdalla.kassandra.dto.ServerSettingUpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.List;

/**
 * REST client for administrator-managed server settings.
 */
@Service
public class ServerSettingsApi extends AbstractApi {

    /**
     * Creates a settings REST client with an explicit API base URL.
     *
     * @param restTemplate HTTP client
     * @param jsonMapper   JSON mapper
     * @param baseUrl      REST API base URL
     */
    public ServerSettingsApi(RestTemplate restTemplate, JsonMapper jsonMapper, String baseUrl) {
        super(restTemplate, jsonMapper, baseUrl);
    }

    /**
     * Creates a settings REST client using the configured local API endpoint.
     *
     * @param restTemplate HTTP client
     * @param jsonMapper   JSON mapper
     */
    @Autowired
    public ServerSettingsApi(RestTemplate restTemplate, JsonMapper jsonMapper) {
        super(restTemplate, jsonMapper);
    }

    /**
     * Lists administrator-managed settings.
     *
     * @return settings with safe values
     */
    public List<ServerSetting> getAll() {
        ResponseEntity<ServerSetting[]> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/server-settings", HttpMethod.GET, createHttpEntity(), ServerSetting[].class));
        return Arrays.asList(response.getBody());
    }

    /**
     * Tests an unsaved or persisted connection setting.
     *
     * @param key     setting key
     * @param request candidate value request
     * @return test result
     */
    public ServerSettingTestResult test(String key, ServerSettingUpdateRequest request) {
        ResponseEntity<ServerSettingTestResult> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/server-settings/{key}/test", HttpMethod.POST, createHttpEntity(request),
                ServerSettingTestResult.class, key));
        return response.getBody();
    }

    /**
     * Updates an administrator-managed setting.
     *
     * @param key     setting key
     * @param request update request
     * @return updated safe setting
     */
    public ServerSetting update(String key, ServerSettingUpdateRequest request) {
        ResponseEntity<ServerSetting> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/server-settings/{key}", HttpMethod.PUT, createHttpEntity(request), ServerSetting.class, key));
        return response.getBody();
    }

    /**
     * Enables or disables an optional server settings category.
     *
     * @param categoryKey category key
     * @param enabled     whether settings in the category can be managed
     */
    public void updateCategoryEnabled(String categoryKey, boolean enabled) {
        ServerSettingUpdateRequest request = new ServerSettingUpdateRequest();
        request.setValue(Boolean.toString(enabled));
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/server-settings/categories/{categoryKey}/enabled", HttpMethod.PUT, createHttpEntity(request),
                Void.class, categoryKey));
    }
}
