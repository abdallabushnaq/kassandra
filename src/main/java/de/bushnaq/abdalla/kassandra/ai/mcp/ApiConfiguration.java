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

import de.bushnaq.abdalla.kassandra.rest.api.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Configuration for AI-specific API beans.
 * <p>
 * These are plain *Api instances (not adapters). The SecurityContext is propagated to the
 * tool-execution thread via {@link ToolContextHelper}, so {@code AbstractApi.createAuthHeaders()}
 * works natively — no adapter override is needed.
 */
@Configuration
public class ApiConfiguration {

    @Bean
    @Qualifier("aiFeatureApi")
    public FeatureApi aiFeatureApi(RestTemplate restTemplate, JsonMapper jsonMapper) {
        return new FeatureApi(restTemplate, jsonMapper);
    }

    @Bean
    @Qualifier("aiProductAclApi")
    public ProductAclApi aiProductAclApi(RestTemplate restTemplate, JsonMapper jsonMapper) {
        return new ProductAclApi(restTemplate, jsonMapper);
    }

    @Bean
    @Qualifier("aiProductApi")
    public ProductApi aiProductApi(RestTemplate restTemplate, JsonMapper jsonMapper) {
        return new ProductApi(restTemplate, jsonMapper);
    }

    @Bean
    @Qualifier("aiSprintApi")
    public SprintApi aiSprintApi(RestTemplate restTemplate, JsonMapper jsonMapper) {
        return new SprintApi(restTemplate, jsonMapper);
    }

    @Bean
    @Qualifier("aiUserApi")
    public UserApi aiUserApi(RestTemplate restTemplate, JsonMapper jsonMapper) {
        return new UserApi(restTemplate, jsonMapper);
    }

    @Bean
    @Qualifier("aiUserGroupApi")
    public UserGroupApi aiUserGroupApi(RestTemplate restTemplate, JsonMapper jsonMapper) {
        return new UserGroupApi(restTemplate, jsonMapper);
    }

    @Bean
    @Qualifier("aiVersionApi")
    public VersionApi aiVersionApi(RestTemplate restTemplate, JsonMapper jsonMapper) {
        return new VersionApi(restTemplate, jsonMapper);
    }
}
