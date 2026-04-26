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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;

import static com.vaadin.flow.spring.security.VaadinSecurityConfigurer.vaadin;

/**
 * Security configuration for OAuth2/OIDC authentication.
 * This class configures Spring Security to use OAuth2/OIDC for authentication with Vaadin UI.
 * It's only enabled when the 'spring.security.oauth2.client.registration.keycloak.client-id' property is defined.
 */
@EnableWebSecurity
@Configuration
@ConditionalOnProperty(name = "spring.security.oauth2.client.registration.keycloak.client-id")
public class OidcSecurityConfig {

    @Autowired(required = false)
    private       ClientRegistrationRepository clientRegistrationRepository;
    @Autowired
    private       CustomOidcUserService        customOidcUserService;
    private final Logger                       logger = LoggerFactory.getLogger(OidcSecurityConfig.class);

    /**
     * Configure the OAuth2 logout success handler.
     */
    private OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler logoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        // After Keycloak invalidates its session it will redirect the browser here.
        // Using the explicit login-view path avoids issues with {baseUrl} (bare root) not being
        // registered as a valid post_logout_redirect_uri in the Keycloak client.
        logoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/ui/login");
        // Fallback redirect when the OIDC provider does not advertise an end_session_endpoint
        // in its discovery document.  Without this, the parent SimpleUrlLogoutSuccessHandler
        // would redirect to "/" which has no Vaadin view and causes a NoResourceFoundException.
        logoutSuccessHandler.setDefaultTargetUrl("/ui/login");
        return logoutSuccessHandler;
    }

    /**
     * Configures Spring Security to use OAuth2 login for Vaadin UI pages.
     * This security configuration is now specifically for Vaadin UI endpoints,
     * separate from the API security configuration.
     */
    @Bean
    @Order(3) // Lower precedence than API security
    public SecurityFilterChain oidcSecurityFilterChain(HttpSecurity http) throws Exception {
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

        // Set up Vaadin specific security configuration.
        // The second argument is the default redirect URL Vaadin uses after successful authentication
        // when no saved request is present.  "/" has no Vaadin view mapped to it (Vaadin lives under
        // /ui/*), so we use /ui/login instead — the same destination we use everywhere else.
        http.with(vaadin(), vaadin -> vaadin.loginView("/login", "/ui/login"));

        // Allow access to the login page and static resources without authentication
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/").permitAll()
                .requestMatchers("/" + LoginView.ROUTE).permitAll()
                .requestMatchers("/VAADIN/**").permitAll()
                .requestMatchers("/css/**").permitAll()
                .requestMatchers("/styles.css").permitAll()
                .requestMatchers("/js/tooltips.js").permitAll()
                .requestMatchers("/ui/icons/**").permitAll()
                .requestMatchers("/ui/images/**").permitAll()
                .requestMatchers("/frontend/**").permitAll()
                .requestMatchers("/frontend-es5/**").permitAll()
                .requestMatchers("/frontend-es6/**").permitAll()
                .requestMatchers("/oauth2/**").permitAll()
                .requestMatchers("/login/oauth2/**").permitAll());

        // Configure OAuth2 login support for Vaadin UI
        if (clientRegistrationRepository != null) {
            logger.info("Configuring OAuth2 login with base URI: {}",
                    OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);

            // Always require credentials on the Keycloak login page, even when Keycloak has an
            // active SSO session for the user.  Without prompt=login, Keycloak silently
            // re-authenticates after logout (via its own SSO cookie), making logout appear broken.
            DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(
                    clientRegistrationRepository,
                    OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
            resolver.setAuthorizationRequestCustomizer(
                    builder -> builder.additionalParameters(params -> params.put("prompt", "login")));

            // Configure OAuth2 login with custom OIDC user service
            http.oauth2Login(oauth2Config -> {
                oauth2Config.loginPage("/" + LoginView.ROUTE)
                        .defaultSuccessUrl("/ui/", true)
                        .authorizationEndpoint(auth -> auth
                                .authorizationRequestResolver(resolver))
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(customOidcUserService)); // Load roles ONCE during authentication
            });
            // Configure OAuth2 logout.
            // Use AntPathRequestMatcher without a method restriction so that the browser
            // GET issued by ui.getPage().setLocation("/logout") is caught by the LogoutFilter.
            // The CSRF risk for unauthenticated-GET logout is low: the worst an attacker can do
            // is log the user out — they cannot gain access to the session.
            http.logout(logout -> logout
                    .logoutRequestMatcher(request -> "/logout".equals(request.getServletPath()))
                    .logoutSuccessHandler(oidcLogoutSuccessHandler())
            );

            // Note: We no longer configure JWT resource server here, as it's now in ApiSecurityConfig
        }

        return http.build();
    }
}
