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

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import de.bushnaq.abdalla.kassandra.ui.view.LoginView;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
@Configuration
public class SecurityConfig extends VaadinWebSecurity {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SecurityUserDetailsService userDetailsService;

    @Bean
    public AuthenticationManager authenticationManager() {
        // Create authentication provider for the test users
        DaoAuthenticationProvider testProvider = new DaoAuthenticationProvider();
        testProvider.setPasswordEncoder(passwordEncoder());
        testProvider.setUserDetailsService(testUsers());

        // Create authentication provider for the regular users
        DaoAuthenticationProvider regularProvider = new DaoAuthenticationProvider();
        regularProvider.setPasswordEncoder(passwordEncoder());
        regularProvider.setUserDetailsService(userDetailsService);

        // Return a provider manager with both providers
        return new ProviderManager(testProvider, regularProvider);
    }

    /**
     * Separate security configuration specifically for API endpoints
     * This uses HTTP Basic Authentication for API security
     */
    @Bean
    @Order(2) // Lower precedence than H2 console but higher than Vaadin
    public SecurityFilterChain basicAuthApiSecurityFilterChain(HttpSecurity http) throws Exception {
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
                .authenticationManager(authenticationManager()) // Use our combined authentication manager
                .build();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        logger.info(">>> Configuring security chain (4/4) basic authentication for vaadin");
        // Configure for all non-API endpoints (Vaadin UI)


        // Set the login view
        setLoginView(http, LoginView.class);

        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(new AntPathRequestMatcher("/")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/" + LoginView.ROUTE)).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/VAADIN/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/ui/icons/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/ui/images/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/frontend/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/frontend-es5/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/frontend-es6/**")).permitAll()
//                .requestMatchers(new AntPathRequestMatcher("/oauth2/**")).permitAll()
//                .requestMatchers(new AntPathRequestMatcher("/login/oauth2/**")).permitAll()
        );

        // Call the parent configuration to handle Vaadin-specific security
        super.configure(http);

        // Set the default success URL after login to ProductListView
        http.formLogin(formLogin -> formLogin.defaultSuccessUrl("/ui/product-list", true));
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
        // Create an admin user for admin role tests
        UserDetails adminUser = User.builder()
                .username("admin-user")
                .password(passwordEncoder().encode("test-password"))
                .roles("ADMIN")
                .build();

        // Create a regular user for USER role tests
        UserDetails regularUser = User.builder()
                .username("user")
                .password(passwordEncoder().encode("test-password"))
                .roles("USER")  // Only USER role, no ADMIN privileges
                .build();

        logger.info("Created default test user/admin users.");

        return new InMemoryUserDetailsManager(adminUser, regularUser);
    }

    /**
     * Configure a combined UserDetailsService that checks both test users and regular users
     */
    @Bean
    @Primary
    public UserDetailsService userDetailsService() {
        return username -> {
            try {
                // First try the test users
                return testUsers().loadUserByUsername(username);
            } catch (UsernameNotFoundException e) {
                // If not found among test users, try the regular user service
                return userDetailsService.loadUserByUsername(username);
            }
        };
    }
}
