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

package de.bushnaq.abdalla.kassandra.ai.mcp;

import de.bushnaq.abdalla.kassandra.ai.mcp.api.AuthenticationProvider;
import de.bushnaq.abdalla.kassandra.ai.mcp.api.product.ProductApiAdapter;
import de.bushnaq.abdalla.kassandra.rest.api.ProductApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Configuration for AI-specific API beans.
 * Creates API instances that use the current user's OIDC token for authentication.
 */
@Configuration
public class ApiConfiguration {

    @Bean
    @Qualifier("aiProductApi")
    public ProductApi aiProductApi(RestTemplate restTemplate, JsonMapper jsonMapper,
                                   AuthenticationProvider authProvider) {
        return new ProductApiAdapter(restTemplate, jsonMapper, authProvider);
    }
}
