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
import de.bushnaq.abdalla.kassandra.service.UserRoleService;
import de.bushnaq.abdalla.kassandra.ui.view.LoginView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Security configuration for OAuth2/OIDC authentication.
 * This class configures Spring Security to use OAuth2/OIDC for authentication with Vaadin UI.
 * It's only enabled when the 'spring.security.oauth2.client.registration.keycloak.client-id' property is defined.
 */
@EnableWebSecurity
@Configuration
@ConditionalOnProperty(name = "spring.security.oauth2.client.registration.keycloak.client-id")
public class OidcSecurityConfig extends VaadinWebSecurity {

    @Autowired(required = false)
    private       ClientRegistrationRepository clientRegistrationRepository;
    @Autowired
    private CustomOidcUserService customOidcUserService;
    private final Logger                       logger = LoggerFactory.getLogger(OidcSecurityConfig.class);
    @Autowired
    private       UserRoleService              userRoleService;

    /**
     * Configures Spring Security to use OAuth2 login for Vaadin UI pages.
     * This security configuration is now specifically for Vaadin UI endpoints,
     * separate from the API security configuration.
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        logger.info(">>> Configuring security chain (2/4) OAuth2/OIDC security for Vaadin UI");

        // Check if client registration repository is available
        if (clientRegistrationRepository == null) {
            logger.error("ClientRegistrationRepository is null - OAuth2 endpoints will not be available!");
        } else {
            try {
                logger.info("Checking for keycloak client registration");
                clientRegistrationRepository.findByRegistrationId("keycloak");
                logger.info("Found keycloak client registration");
            } catch (Exception e) {
                logger.error("Error finding keycloak client registration", e);
            }
        }

        // Set up Vaadin specific security configuration first
        setLoginView(http, LoginView.class);

        // Allow access to the login page and static resources without authentication
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(new AntPathRequestMatcher("/")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/" + LoginView.ROUTE)).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/VAADIN/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/ui/icons/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/ui/images/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/frontend/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/frontend-es5/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/frontend-es6/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/oauth2/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/login/oauth2/**")).permitAll());

        // Configure OAuth2 login support for Vaadin UI
        if (clientRegistrationRepository != null) {
            logger.info("Configuring OAuth2 login with base URI: {}",
                    OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);

            // Configure OAuth2 login with custom OIDC user service
            http.oauth2Login(oauth2Config -> {
                oauth2Config.loginPage("/" + LoginView.ROUTE)
                        .defaultSuccessUrl("/ui/product-list", true) // Redirects to ProductListView after successful OIDC login
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(customOidcUserService)); // Load roles ONCE during authentication
            });

            // Configure OAuth2 logout
            http.logout(logout -> logout
                    .logoutSuccessHandler(oidcLogoutSuccessHandler())
            );

            // Note: We no longer configure JWT resource server here, as it's now in ApiSecurityConfig
        }

        // Complete Vaadin security configuration
        super.configure(http);
    }


    /**
     * Configure the OAuth2 logout success handler.
     */
    private OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler logoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        // Set the URL to redirect to after logout
        logoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}");
        return logoutSuccessHandler;
    }
}
