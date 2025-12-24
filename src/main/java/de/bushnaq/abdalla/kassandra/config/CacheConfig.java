/*
 *
 * Copyright (C) 2025-2025 Abdalla Bushnaq
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

package de.bushnaq.abdalla.kassandra.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Spring Cache.
 * Enables caching support for the application, particularly for user role lookups
 * to avoid repeated database queries during OIDC authentication.
 */
@Configuration
@EnableCaching
public class CacheConfig {
    // Spring Boot auto-configures a simple ConcurrentMapCacheManager by default
    // This provides in-memory caching suitable for single-instance deployments
    // For production with multiple instances, consider Redis or Hazelcast
}

