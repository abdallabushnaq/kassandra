/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package de.bushnaq.abdalla.kassandra.rest.api;

import de.bushnaq.abdalla.kassandra.dto.AuditPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

/**
 * REST client for administrator audit history.
 */
@Service
public class AuditLogApi extends AbstractApi {
    /**
     * Creates a client with an explicit base URL for tests.
     *
     * @param restTemplate HTTP client
     * @param jsonMapper   JSON mapper
     * @param baseUrl      REST API base URL
     */
    public AuditLogApi(RestTemplate restTemplate, JsonMapper jsonMapper, String baseUrl) {
        super(restTemplate, jsonMapper, baseUrl);
    }

    /**
     * Creates the application audit client.
     *
     * @param restTemplate HTTP client
     * @param jsonMapper   JSON mapper
     */
    @Autowired
    public AuditLogApi(RestTemplate restTemplate, JsonMapper jsonMapper) {
        super(restTemplate, jsonMapper);
    }

    /**
     * Loads a database-paged set of changes.
     *
     * @param user   actor name or email
     * @param action change action
     * @param search free-text search
     * @param from   inclusive start
     * @param to     exclusive end
     * @param page   zero-based page
     * @param size   page size
     * @return matching audit entries and total count
     */
    public AuditPage getPage(String user, String action, String search, Instant from, Instant to, int page, int size) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(getBaseUrl() + "/audit")
                .queryParam("page", page).queryParam("size", size);
        if (user != null && !user.isBlank()) uri.queryParam("user", user);
        if (action != null && !action.isBlank()) uri.queryParam("action", action);
        if (search != null && !search.isBlank()) uri.queryParam("search", search);
        if (from != null) uri.queryParam("from", from);
        if (to != null) uri.queryParam("to", to);
        ResponseEntity<AuditPage> response = executeWithErrorHandling(() -> restTemplate.exchange(
                uri.build().encode().toUri(), HttpMethod.GET, createHttpEntity(), AuditPage.class));
        return response.getBody();
    }
}
