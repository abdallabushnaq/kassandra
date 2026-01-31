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

package de.bushnaq.abdalla.kassandra.ai.mcp.api.user;

import de.bushnaq.abdalla.kassandra.ai.mcp.api.AuthenticationProvider;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * UserApi adapter that uses the current user's OIDC token for authentication.
 * The token is captured by AuthenticationProvider at the start of AI query processing.
 */
public class UserApiAdapter extends UserApi {

    private final AuthenticationProvider authProvider;

    public UserApiAdapter(RestTemplate restTemplate, JsonMapper jsonMapper, AuthenticationProvider authProvider) {
        super(restTemplate, jsonMapper);
        this.authProvider = authProvider;
    }

    @Override
    protected HttpHeaders createAuthHeaders() {
        return authProvider.createAuthHeaders();
    }
}
