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

package de.bushnaq.abdalla.kassandra.rest.controller;

import de.bushnaq.abdalla.kassandra.dto.ServerSetting;
import de.bushnaq.abdalla.kassandra.dto.ServerSettingTestResult;
import de.bushnaq.abdalla.kassandra.dto.ServerSettingUpdateRequest;
import de.bushnaq.abdalla.kassandra.service.ServerSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API for administrator-managed server settings.
 */
@RestController
@RequestMapping("/api/server-settings")
public class ServerSettingsController {

    @Autowired
    private ServerSettingsService serverSettingsService;

    /**
     * Lists supported settings and their safe values.
     *
     * @return settings visible to an administrator
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ServerSetting> list() {
        return serverSettingsService.list();
    }

    /**
     * Tests an unsaved or persisted connection setting.
     *
     * @param key     setting key
     * @param request candidate value request
     * @return connection test result
     */
    @PostMapping("/{key:.+}/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ServerSettingTestResult test(@PathVariable String key, @RequestBody ServerSettingUpdateRequest request) {
        return serverSettingsService.test(key, request.getValue());
    }

    /**
     * Updates a single administrator-managed setting.
     *
     * @param key     setting key
     * @param request update request
     * @return updated safe setting
     */
    @PutMapping("/{key:.+}")
    @PreAuthorize("hasRole('ADMIN')")
    public ServerSetting update(@PathVariable String key, @RequestBody ServerSettingUpdateRequest request) {
        return serverSettingsService.update(key, request.getValue(), request.isClearSecret());
    }

    /**
     * Enables or disables an optional server settings category.
     *
     * @param categoryKey category key
     * @param request     request whose value is {@code true} or {@code false}
     * @throws IllegalArgumentException when the value is not a boolean
     */
    @PutMapping("/categories/{categoryKey}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public void updateCategoryEnabled(@PathVariable String categoryKey, @RequestBody ServerSettingUpdateRequest request) {
        if (!"true".equalsIgnoreCase(request.getValue()) && !"false".equalsIgnoreCase(request.getValue())) {
            throw new IllegalArgumentException("Category enabled value must be true or false");
        }
        serverSettingsService.updateCategoryEnabled(categoryKey, Boolean.parseBoolean(request.getValue()));
    }
}
