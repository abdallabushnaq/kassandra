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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Map;
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
     * Extract Keycloak roles from token/userinfo claims and add them as Spring Security authorities.
     */
    @SuppressWarnings("unchecked")
    private void extractKeycloakRoles(Set<GrantedAuthority> mappedAuthorities, Map<String, Object> claims) {
        // Check for realm_access claim which contains roles
        if (claims.containsKey("realm_access")) {
            Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
            if (realmAccess.containsKey("roles")) {
                ((Iterable<String>) realmAccess.get("roles")).forEach(role -> {
                    mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                });
            }
        }
    }

    /**
     * Creates a JWT converter to extract roles from JWT tokens for REST API authorization.
     * This is crucial for the @PreAuthorize annotations in REST controllers to work properly.
     */
    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Log that this converter is being called
//            logger.info("JWT Authentication Converter called for token with subject: {}, issued by: {}", jwt.getSubject(), jwt.getIssuer());

            // Extract roles from the JWT claims
            Map<String, Object> claims = jwt.getClaims();
//            logger.debug("JWT Token claims: {}", claims);

            Set<GrantedAuthority> authorities = new HashSet<>();

            // Extract realm roles from the token
//            logger.debug("Extracting Keycloak roles from JWT token");
            extractKeycloakRoles(authorities, claims);

            // Log the extracted authorities
//            if (!authorities.isEmpty()) {
//                logger.debug("Extracted authorities from JWT: {}", authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(", ")));
//            } else {
//                logger.warn("No authorities extracted from JWT token");
//            }

            // Add default user role if no roles are found
            if (authorities.isEmpty()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
//                logger.debug("Added default ROLE_USER authority");
            }

            return authorities;
        });
        return jwtConverter;
    }
}
