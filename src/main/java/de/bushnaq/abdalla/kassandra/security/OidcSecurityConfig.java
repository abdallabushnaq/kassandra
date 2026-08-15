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

package de.bushnaq.abdalla.kassandra.security;

import de.bushnaq.abdalla.kassandra.ui.view.LoginView;
import de.bushnaq.abdalla.kassandra.ui.view.RecoveryView;
import de.bushnaq.abdalla.kassandra.ui.view.SetupView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Configures OIDC browser authentication with dynamically persisted client registrations.
 */
@EnableWebSecurity
@Configuration
public class OidcSecurityConfig {

    @Autowired
    private       ClientRegistrationRepository clientRegistrationRepository;
    @Autowired
    private       CustomOidcUserService        customOidcUserService;
    @Autowired
    private       OidcIdentityLinkAuthenticationSuccessHandler oidcIdentityLinkAuthenticationSuccessHandler;
    @Autowired
    private       SetupRecoveryAuthenticationProvider setupRecoveryAuthenticationProvider;
    private final Logger                       logger = LoggerFactory.getLogger(OidcSecurityConfig.class);

    /**
     * Configures OIDC logout and returns users to the dynamic login page.
     *
     * @return logout success handler
     */
    private OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler logoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        logoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/ui/login");
        logoutSuccessHandler.setDefaultTargetUrl("/ui/login");
        return logoutSuccessHandler;
    }

    /**
     * Creates the Vaadin security chain and delegates authorization requests to the selected provider.
     *
     * @param http Spring Security HTTP configuration
     * @return configured filter chain
     * @throws Exception when Spring Security cannot build the chain
     */
    @Bean
    @Order(3)
    public SecurityFilterChain oidcSecurityFilterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring dynamic OAuth2/OIDC security for Vaadin UI");
        http.with(vaadin(), vaadin -> vaadin.loginView("/login", "/ui/login"));

        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/").permitAll()
                .requestMatchers("/" + LoginView.ROUTE).permitAll()
                .requestMatchers("/" + RecoveryView.ROUTE).permitAll()
                .requestMatchers("/" + RecoveryView.ROUTE + "/login").permitAll()
                .requestMatchers("/" + SetupView.ROUTE).permitAll()
                .requestMatchers("/ui/" + SetupView.ROUTE).permitAll()
                .requestMatchers("/ui/" + RecoveryView.ROUTE).permitAll()
                .requestMatchers("/VAADIN/**").permitAll()
                .requestMatchers("/css/**").permitAll()
                .requestMatchers("/styles.css").permitAll()
                .requestMatchers("/js/**").permitAll()
                .requestMatchers("/ui/icons/**").permitAll()
                .requestMatchers("/ui/images/**").permitAll()
                .requestMatchers("/ui/report/**").permitAll()
                .requestMatchers("/frontend/**").permitAll()
                .requestMatchers("/frontend-es5/**").permitAll()
                .requestMatchers("/frontend-es6/**").permitAll()
                .requestMatchers("/oauth2/**").permitAll()
                .requestMatchers("/login/oauth2/**").permitAll());

        DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository,
                OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
        resolver.setAuthorizationRequestCustomizer(
                builder -> builder.additionalParameters(parameters -> parameters.put("prompt", "login")));

        oidcIdentityLinkAuthenticationSuccessHandler.setDefaultTargetUrl("/ui/");
        http.oauth2Login(oauth2Config -> oauth2Config
                .loginPage("/" + LoginView.ROUTE)
                .defaultSuccessUrl("/ui/", true)
                .authorizationEndpoint(authorization -> authorization.authorizationRequestResolver(resolver))
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(customOidcUserService))
                .successHandler(oidcIdentityLinkAuthenticationSuccessHandler));
        http.authenticationProvider(setupRecoveryAuthenticationProvider);
        http.formLogin(formLogin -> formLogin
                .loginPage("/" + RecoveryView.ROUTE)
                .loginProcessingUrl("/" + RecoveryView.ROUTE + "/login")
                .defaultSuccessUrl("/ui/" + SetupView.ROUTE, true)
                .permitAll());
        http.logout(logout -> logout
                .logoutRequestMatcher(request -> "/logout".equals(request.getServletPath()))
                .logoutSuccessHandler(oidcLogoutSuccessHandler()));

        return http.build();
    }
}
