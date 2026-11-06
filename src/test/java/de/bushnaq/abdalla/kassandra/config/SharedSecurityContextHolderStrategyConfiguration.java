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
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.config;

import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

/**
 * Shares Vaadin's security-context strategy across Spring test application contexts.
 */
@Configuration(proxyBeanMethods = false)
@Profile("test")
class SharedSecurityContextHolderStrategyConfiguration {
    private static final SecurityContextHolderStrategy STRATEGY = new VaadinAwareSecurityContextHolderStrategy();

    /**
     * Provides the same strategy instance to every test application context.
     *
     * @return the shared Vaadin-aware security-context strategy
     */
    @Bean
    SecurityContextHolderStrategy sharedSecurityContextHolderStrategy() {
        return STRATEGY;
    }
}
