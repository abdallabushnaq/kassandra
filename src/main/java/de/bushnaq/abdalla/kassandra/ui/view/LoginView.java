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

package de.bushnaq.abdalla.kassandra.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import org.springframework.core.env.Environment;

import static de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil.DIALOG_DEFAULT_WIDTH;


/**
 * Login view that supports OIDC authentication only.
 * Form login has been removed - all authentication goes through OIDC provider.
 */
@Route("login")
@PageTitle("Login | Kassandra")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {
    public static final String  LOGIN_VIEW        = "login-view";
    // ID for OIDC login button used in tests
    public static final String  OIDC_LOGIN_BUTTON = "oidc-login-button";
    public static final String  ROUTE             = "login";
    private final       boolean oidcEnabled;

    public LoginView(Environment environment) {
        this.oidcEnabled = environment.getProperty("spring.security.oauth2.client.registration.keycloak.client-id") != null;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // Create a centered container for the login
        VerticalLayout centeringLayout = new VerticalLayout();
        centeringLayout.setId(LOGIN_VIEW);
        centeringLayout.setWidth(DIALOG_DEFAULT_WIDTH);
        centeringLayout.setPadding(false);
        centeringLayout.setSpacing(true);
        centeringLayout.setAlignSelf(Alignment.CENTER);
        centeringLayout.setAlignItems(Alignment.CENTER);

        H1 title = new H1("Kassandra");
        title.addClassNames(Margin.Bottom.MEDIUM);
        centeringLayout.add(title);

        // Add authentication options
        if (oidcEnabled) {
            centeringLayout.add(createOidcLoginButton());
        } else {
            centeringLayout.add(createOidcNotConfiguredMessage());
        }

        add(centeringLayout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        // OIDC handles authentication errors through its own mechanisms
        // No special handling needed here
    }

    /**
     * Creates the OIDC login button
     */
    private Component createOidcLoginButton() {
        VerticalLayout container = new VerticalLayout();
        container.setMaxWidth("400px");
        container.setAlignItems(Alignment.CENTER);
        container.setPadding(true);
        container.setSpacing(true);

        // Instructions
        Paragraph instructions = new Paragraph("Please sign in with your organizational account");
        instructions.getStyle()
                .set("text-align", "center")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-bottom", "var(--lumo-space-m)");
        container.add(instructions);

        // OIDC login button
        Anchor loginButton = new Anchor("/oauth2/authorization/keycloak", "🔐 Sign in with Keycloak");
        loginButton.setRouterIgnore(true); // Prevent Vaadin from intercepting the link
        loginButton.setId(OIDC_LOGIN_BUTTON);
        loginButton.getStyle()
                .set("background-color", "var(--lumo-primary-color)")
                .set("color", "var(--lumo-primary-contrast-color)")
                .set("padding", "var(--lumo-space-m) var(--lumo-space-l)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("cursor", "pointer")
                .set("text-decoration", "none")
                .set("display", "inline-block")
                .set("text-align", "center")
                .set("font-weight", "600")
                .set("font-size", "var(--lumo-font-size-m)")
                .set("min-width", "250px")
                .set("transition", "all 0.2s");

        // Add hover effect via JavaScript
        loginButton.getElement().executeJs(
                "this.addEventListener('mouseenter', () => { this.style.transform = 'translateY(-2px)'; this.style.boxShadow = '0 4px 12px rgba(0,0,0,0.15)'; });" +
                        "this.addEventListener('mouseleave', () => { this.style.transform = 'translateY(0)'; this.style.boxShadow = 'none'; });"
        );

        container.add(loginButton);

        // Wrap in a card-like container
        Div wrapper = new Div(container);
        wrapper.getStyle()
                .set("background-color", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "0 2px 10px var(--lumo-shade-20pct)")
                .set("padding", "var(--lumo-space-l)");

        return wrapper;
    }

    /**
     * Creates a message when OIDC is not configured
     */
    private Component createOidcNotConfiguredMessage() {
        VerticalLayout container = new VerticalLayout();
        container.setMaxWidth("400px");
        container.setAlignItems(Alignment.CENTER);
        container.setPadding(true);
        container.setSpacing(true);

        H3 heading = new H3("⚠️ Authentication Not Configured");
        heading.getStyle().set("color", "var(--lumo-error-color)");

        Paragraph message = new Paragraph(
                "OIDC authentication is not configured. " +
                        "Please configure Keycloak settings in application.properties."
        );
        message.getStyle()
                .set("text-align", "center")
                .set("color", "var(--lumo-secondary-text-color)");

        container.add(heading, message);

        Div wrapper = new Div(container);
        wrapper.getStyle()
                .set("background-color", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "0 2px 10px var(--lumo-shade-20pct)")
                .set("padding", "var(--lumo-space-l)");

        return wrapper;
    }
}
