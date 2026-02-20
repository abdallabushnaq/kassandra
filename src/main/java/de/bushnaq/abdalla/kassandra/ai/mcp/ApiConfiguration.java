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
import de.bushnaq.abdalla.kassandra.ai.mcp.api.feature.FeatureApiAdapter;
import de.bushnaq.abdalla.kassandra.ai.mcp.api.product.ProductAclApiAdapter;
import de.bushnaq.abdalla.kassandra.ai.mcp.api.product.ProductApiAdapter;
import de.bushnaq.abdalla.kassandra.ai.mcp.api.sprint.SprintApiAdapter;
import de.bushnaq.abdalla.kassandra.ai.mcp.api.user.UserApiAdapter;
import de.bushnaq.abdalla.kassandra.ai.mcp.api.usergroup.UserGroupApiAdapter;
import de.bushnaq.abdalla.kassandra.ai.mcp.api.version.VersionApiAdapter;
import de.bushnaq.abdalla.kassandra.rest.api.*;
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
    @Qualifier("aiFeatureApi")
    public FeatureApi aiFeatureApi(RestTemplate restTemplate, JsonMapper jsonMapper,
                                   AuthenticationProvider authProvider) {
        return new FeatureApiAdapter(restTemplate, jsonMapper, authProvider);
    }

    @Bean
    @Qualifier("aiProductAclApi")
    public ProductAclApi aiProductAclApi(RestTemplate restTemplate, JsonMapper jsonMapper,
                                         AuthenticationProvider authProvider) {
        return new ProductAclApiAdapter(restTemplate, jsonMapper, authProvider);
    }

    @Bean
    @Qualifier("aiProductApi")
    public ProductApi aiProductApi(RestTemplate restTemplate, JsonMapper jsonMapper,
                                   AuthenticationProvider authProvider) {
        return new ProductApiAdapter(restTemplate, jsonMapper, authProvider);
    }

    @Bean
    @Qualifier("aiSprintApi")
    public SprintApi aiSprintApi(RestTemplate restTemplate, JsonMapper jsonMapper,
                                 AuthenticationProvider authProvider) {
        return new SprintApiAdapter(restTemplate, jsonMapper, authProvider);
    }

    @Bean
    @Qualifier("aiUserApi")
    public UserApi aiUserApi(RestTemplate restTemplate, JsonMapper jsonMapper,
                             AuthenticationProvider authProvider) {
        return new UserApiAdapter(restTemplate, jsonMapper, authProvider);
    }

    @Bean
    @Qualifier("aiUserGroupApi")
    public UserGroupApi aiUserGroupApi(RestTemplate restTemplate, JsonMapper jsonMapper,
                                       AuthenticationProvider authProvider) {
        return new UserGroupApiAdapter(restTemplate, jsonMapper, authProvider);
    }

    @Bean
    @Qualifier("aiVersionApi")
    public VersionApi aiVersionApi(RestTemplate restTemplate, JsonMapper jsonMapper,
                                   AuthenticationProvider authProvider) {
        return new VersionApiAdapter(restTemplate, jsonMapper, authProvider);
    }
}
