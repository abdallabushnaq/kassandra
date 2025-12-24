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

package de.bushnaq.abdalla.kassandra.security;

import de.bushnaq.abdalla.kassandra.service.UserRoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Security configuration for REST API endpoints.
 * This class configures Spring Security to use JWT tokens for REST API authentication.
 * It's only enabled when the 'spring.security.oauth2.client.registration.keycloak.client-id' property is defined.
 */
@EnableWebSecurity
@Configuration
@ConditionalOnProperty(name = "spring.security.oauth2.client.registration.keycloak.client-id")
public class OidcApiSecurityConfig {

    private final Logger logger = LoggerFactory.getLogger(OidcApiSecurityConfig.class);

    @Autowired
    private UserRoleService userRoleService;

    /**
     * Configures Spring Security for REST API endpoints.
     * This creates a separate security filter chain for the REST API that uses JWT tokens for authentication.
     */
    @Bean
    @Order(1) // Higher precedence than the Vaadin security filter chain
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        logger.info(">>> Configuring security chain (1/4) JWT security for REST API endpoints");

        // Configure security for REST API endpoints
        http
                // Apply this filter chain only to API endpoints
                .securityMatcher("/api/**")
                // Disable CSRF for API endpoints
                .csrf(csrf -> csrf.disable())
                // Configure session management to be stateless
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Configure authorization for API endpoints
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(new AntPathRequestMatcher("/api/**")).authenticated())
                // Configure both JWT token authentication AND HTTP Basic auth for API endpoints
                .httpBasic() // Add HTTP Basic Authentication support
                .and()
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }

    /**
     * Creates a JWT converter to load roles from database for REST API authorization.
     * This replaces OIDC token-based roles with local database roles.
     * This is crucial for the @PreAuthorize annotations in REST controllers to work properly.
     */
    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Extract email from JWT token
            String email = jwt.getClaim("email");
            if (email == null || email.isEmpty()) {
                // Fallback to preferred_username or subject
                email = jwt.getClaim("preferred_username");
                if (email == null || email.isEmpty()) {
                    email = jwt.getSubject();
                }
            }

//            logger.debug("Loading roles for OIDC user: {}", email);

            // Load roles from database
            List<String> dbRoles = userRoleService.getRolesByEmail(email);

            Set<GrantedAuthority> authorities = new HashSet<>();
            if (!dbRoles.isEmpty()) {
                // Use database roles
                dbRoles.forEach(role ->
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role))
                );
//                logger.debug("Loaded roles from database for {}: {}", email, dbRoles);
            } else {
                // No user in database - assign default USER role
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                logger.warn("User {} not found in database. Assigned default USER role.", email);
            }

            return authorities;
        });
        return jwtConverter;
    }
}
