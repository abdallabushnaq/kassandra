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
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.rest.api;

import de.bushnaq.abdalla.kassandra.dto.OidcProvider;
import de.bushnaq.abdalla.kassandra.dto.OidcProviderCreateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * REST client for managing OpenID Connect providers.
 */
@Service
public class OidcProviderApi extends AbstractApi {

    /**
     * Creates an API client with an explicit base URL.
     *
     * @param restTemplate HTTP client
     * @param jsonMapper   JSON mapper
     * @param baseUrl      REST API base URL
     */
    public OidcProviderApi(RestTemplate restTemplate, JsonMapper jsonMapper, String baseUrl) {
        super(restTemplate, jsonMapper, baseUrl);
    }

    /**
     * Creates an API client using the configured local REST endpoint.
     *
     * @param restTemplate HTTP client
     * @param jsonMapper   JSON mapper
     */
    @Autowired
    public OidcProviderApi(RestTemplate restTemplate, JsonMapper jsonMapper) {
        super(restTemplate, jsonMapper);
    }

    /**
     * Creates an unconfigured API client for serialization frameworks.
     */
    public OidcProviderApi() {
    }

    /**
     * Creates and validates an OIDC provider.
     *
     * @param request provider configuration including its client secret
     * @return the created provider without its secret
     */
    public OidcProvider create(OidcProviderCreateRequest request) {
        ResponseEntity<OidcProvider> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/oidc-provider",
                HttpMethod.POST,
                createHttpEntity(request),
                OidcProvider.class
        ));
        return response.getBody();
    }

    /**
     * Deletes an OIDC provider.
     *
     * @param id provider identifier
     */
    public void deleteById(UUID id) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/oidc-provider/{id}",
                HttpMethod.DELETE,
                createHttpEntity(),
                Void.class,
                id
        ));
    }

    /**
     * Disables an OIDC provider.
     *
     * @param id provider identifier
     */
    public void disable(UUID id) {
        updateState(id, "disable");
    }

    /**
     * Enables an OIDC provider after discovery validation.
     *
     * @param id provider identifier
     */
    public void enable(UUID id) {
        updateState(id, "enable");
    }

    /**
     * Lists all providers for administration.
     *
     * @return configured providers
     */
    public List<OidcProvider> getAll() {
        ResponseEntity<OidcProvider[]> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/oidc-provider",
                HttpMethod.GET,
                createHttpEntity(),
                OidcProvider[].class
        ));
        return Arrays.asList(response.getBody());
    }

    private void updateState(UUID id, String action) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/oidc-provider/{id}/" + action,
                HttpMethod.PUT,
                createHttpEntity(),
                Void.class,
                id
        ));
    }
}
