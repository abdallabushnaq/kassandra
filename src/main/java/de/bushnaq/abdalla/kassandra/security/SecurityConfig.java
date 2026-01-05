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

import de.bushnaq.abdalla.kassandra.ui.view.LoginView;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static com.vaadin.flow.spring.security.VaadinSecurityConfigurer.vaadin;

@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
@Configuration
public class SecurityConfig {
    public static final String OIDC_PASSWORD = "password";
    public static final String TEST_PASSWORD = "test-password";
    Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Creates the global authentication manager.
     * This is critical for @WithMockUser in tests to work properly in Spring Security 7.
     * The authentication manager must be exposed as a bean to be available for test security context.
     */
    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider daoAuthenticationProvider) {
        // Return a provider manager with the DAO authentication provider
        return new ProviderManager(daoAuthenticationProvider);
    }

    /**
     * Separate security configuration specifically for API endpoints
     * This uses HTTP Basic Authentication for API security
     */
    @Bean
    @Order(2) // Lower precedence than H2 console but higher than Vaadin
    public SecurityFilterChain basicAuthApiSecurityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        logger.info(">>> Configuring security chain (3/4) basic authentication for REST API endpoints");
        return http
                .securityMatcher("/api/**") // Apply this configuration only to API endpoints
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for API endpoints
                .httpBasic(httpBasic -> {
                }) // Enable HTTP Basic Auth for APIs
                .exceptionHandling(handling -> handling
                        // Return 401 for unauthenticated requests instead of redirecting
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("Authentication required");
                        })
                        // Return 403 for unauthorized requests
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write("Access denied");
                        })
                )
                .authenticationManager(authenticationManager) // Use the injected authentication manager
                .build();
    }

    /**
     * Creates an authentication provider for test users.
     * This is a separate bean so it can be used by multiple authentication managers.
     */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(testUsers());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Separate security configuration for H2 Console
     * This must have higher precedence to avoid conflicts with Vaadin security
     */
    @Bean
    @Order(1) // Highest precedence for H2 console
    public SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity http) throws Exception {
        logger.info(">>> Configuring security chain (1/4) H2 console");
        return http
                .securityMatcher("/h2-console/**") // Apply only to H2 console
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll()
                )
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for H2 console
                .formLogin(AbstractHttpConfigurer::disable) // Disable form login
                .httpBasic(AbstractHttpConfigurer::disable) // Disable HTTP basic auth
                .logout(AbstractHttpConfigurer::disable) // Disable logout
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.disable())
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configure test users for API testing with appropriate roles
     */
    @Bean
    public InMemoryUserDetailsManager testUsers() {
        UserDetails adminUser = User.builder()
                .username("admin-user")
                .password(passwordEncoder().encode(TEST_PASSWORD))
                .roles("ADMIN")
                .build();

        UserDetails user = User.builder()
                .username("user")
                .password(passwordEncoder().encode(TEST_PASSWORD))
                .roles("USER")  // Only USER role, no ADMIN privileges
                .build();

        UserDetails admin1 = User.builder()
                .username("christopher.paul@kassandra.org")
                .password(passwordEncoder().encode(TEST_PASSWORD))
                .roles("ADMIN")
                .build();

        UserDetails user1 = User.builder()
                .username("kristen.hubbell@kassandra.org")
                .password(passwordEncoder().encode(TEST_PASSWORD))
                .roles("USER")  // Only USER role, no ADMIN privileges
                .build();

        UserDetails user2 = User.builder()
                .username("claudine.fick@kassandra.org")
                .password(passwordEncoder().encode(TEST_PASSWORD))
                .roles("USER")  // Only USER role, no ADMIN privileges
                .build();

        UserDetails user3 = User.builder()
                .username("randy.asmus@kassandra.org")
                .password(passwordEncoder().encode(TEST_PASSWORD))
                .roles("USER")  // Only USER role, no ADMIN privileges
                .build();

        logger.info("Created default test user/admin users.");

        return new InMemoryUserDetailsManager(adminUser, user, admin1, user1, user2, user3);
    }

    /**
     * Vaadin security filter chain for basic authentication.
     * This is only active when OIDC is NOT configured (i.e., when OidcSecurityConfig is not loaded).
     * When OIDC is configured, OidcSecurityConfig will provide the Vaadin security instead.
     */
    @Bean
    @Order(4) // Lowest precedence - catch-all for Vaadin UI
    @ConditionalOnMissingBean(name = "oidcSecurityFilterChain")
    public SecurityFilterChain vaadinSecurityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        logger.info(">>> Configuring security chain (4/4) basic authentication for Vaadin");

        // Set up Vaadin specific security configuration
        http.with(vaadin(), vaadin -> vaadin.loginView("/login", "/"));

        // Allow access to the login page and static resources without authentication
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/").permitAll()
                .requestMatchers("/" + LoginView.ROUTE).permitAll()
                .requestMatchers("/VAADIN/**").permitAll()
                .requestMatchers("/ui/icons/**").permitAll()
                .requestMatchers("/ui/images/**").permitAll()
                .requestMatchers("/frontend/**").permitAll()
                .requestMatchers("/frontend-es5/**").permitAll()
                .requestMatchers("/frontend-es6/**").permitAll()
        );

        // Configure form login for test users
        http.formLogin(form -> form
                .loginPage("/" + LoginView.ROUTE)
                .defaultSuccessUrl("/ui/product-list", true)
                .permitAll()
        );

        // Configure logout
        http.logout(logout -> logout
                .logoutSuccessUrl("/" + LoginView.ROUTE)
                .permitAll()
        );

        // Use the authentication manager with test users
        http.authenticationManager(authenticationManager);

        return http.build();
    }
}
