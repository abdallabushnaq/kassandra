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
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import org.springframework.core.env.Environment;

import static de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil.DIALOG_DEFAULT_WIDTH;


/**
 * Login view that supports both Basic Authentication and OIDC authentication.
 */
@Route("login")
@PageTitle("Login | Project Hub")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {
    public static final String        LOGIN_VIEW               = "login-view";
    public static final String        LOGIN_VIEW_PASSWORD      = "login-view-password";
    public static final String        LOGIN_VIEW_SUBMIT_BUTTON = "login-view-submit-button";
    public static final String        LOGIN_VIEW_USERNAME      = "login-view-username";
    // ID for OIDC login button used in tests
    public static final String        OIDC_LOGIN_BUTTON        = "oidc-login-button";
    public static final String        ROUTE                    = "login";
    private final       Span          errorMessage             = new Span();
    private final       Button        loginButton              = new Button("Login");
    private final       FormLayout    loginForm                = new FormLayout();
    private final       boolean       oidcEnabled;
    private final       PasswordField passwordField            = new PasswordField("Password");
    private final       TextField     usernameField            = new TextField("Username");

    public LoginView(Environment environment) {
        this.oidcEnabled = environment.getProperty("spring.security.oauth2.client.registration.keycloak.client-id") != null;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // Create a centered container for the login form
        VerticalLayout centeringLayout = new VerticalLayout();
        centeringLayout.setId(LOGIN_VIEW);
        centeringLayout.setWidth(DIALOG_DEFAULT_WIDTH);
        centeringLayout.setPadding(false);
        centeringLayout.setSpacing(false);
        centeringLayout.setAlignSelf(Alignment.CENTER);
        centeringLayout.setAlignItems(Alignment.CENTER);

        H1 title = new H1("Kassandra");
        title.addClassNames(Margin.Bottom.MEDIUM);

        centeringLayout.add(title);

        // Create a container for authentication options
        VerticalLayout authContainer = new VerticalLayout();
        authContainer.setMaxWidth("400px");
        authContainer.setAlignItems(Alignment.CENTER);
        authContainer.getStyle().set("margin", "0 auto");

        // Add custom login form
        authContainer.add(new H3("Login with username and password"));
        authContainer.add(createLoginForm());

        // Add OIDC login option if it's enabled
        if (oidcEnabled) {
            Div separator = new Div();
            separator.setText("OR");
            separator.getStyle().set("margin", "5px 0");
            separator.getStyle().set("text-align", "center");
            separator.getStyle().set("width", "100%");
            authContainer.add(separator);

            // Create OIDC login button
            Anchor loginWithOidcButton = new Anchor("/oauth2/authorization/keycloak", "Login with keycloak");
            loginWithOidcButton.setRouterIgnore(true); // Prevent Vaadin from intercepting the link
            loginWithOidcButton.setId(OIDC_LOGIN_BUTTON);
            loginWithOidcButton.getStyle().set("background-color", "var(--lumo-primary-color)");
            loginWithOidcButton.getStyle().set("color", "var(--lumo-primary-contrast-color)");
            loginWithOidcButton.getStyle().set("padding", "10px 20px");
            loginWithOidcButton.getStyle().set("border-radius", "4px");
            loginWithOidcButton.getStyle().set("cursor", "pointer");
            loginWithOidcButton.getStyle().set("text-decoration", "none");
            loginWithOidcButton.getStyle().set("display", "block");
            loginWithOidcButton.getStyle().set("text-align", "center");
            loginWithOidcButton.getStyle().set("margin-top", "20px");

            authContainer.add(loginWithOidcButton);
        }

        centeringLayout.add(authContainer);
        add(centeringLayout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        // Check if authentication error occurred
        if (beforeEnterEvent.getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("error")) {
            showError("Invalid username or password. Please try again.");
        }
    }

    private Component createLoginForm() {
        // Configure username field
        usernameField.setId(LOGIN_VIEW_USERNAME);
        usernameField.setAutofocus(true);
        usernameField.setClearButtonVisible(true);
        usernameField.setRequiredIndicatorVisible(true);
        usernameField.setWidthFull();

        // Configure password field
        passwordField.setId(LOGIN_VIEW_PASSWORD);
        passwordField.setRequiredIndicatorVisible(true);
        passwordField.setWidthFull();

        // Configure error message
        errorMessage.getStyle().set("color", "var(--lumo-error-color)");
        errorMessage.getStyle().set("margin-bottom", "15px");
        errorMessage.setVisible(false);

        // Configure login button
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setId(LOGIN_VIEW_SUBMIT_BUTTON);
        loginButton.addClickListener(e -> login());
        loginButton.addClickShortcut(Key.ENTER);

        // Layout form fields
        VerticalLayout formContainer = new VerticalLayout();
        formContainer.setSpacing(true);
        formContainer.setPadding(false);
        formContainer.setWidthFull();

        formContainer.add(errorMessage);

        loginForm.add(usernameField, passwordField);
        loginForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1)
        );
        loginForm.setWidthFull();
        formContainer.add(loginForm);

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        buttonLayout.add(loginButton);
        formContainer.add(buttonLayout);

        // Create a form wrapper with a card-like appearance
        Div formWrapper = new Div(formContainer);
        formWrapper.setWidthFull();
        formWrapper.getStyle().set("background-color", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("box-shadow", "0 2px 10px var(--lumo-shade-20pct)")
                .set("padding", "var(--lumo-space-m)");

        return formWrapper;
    }

    private void login() {
        // Instead of creating hidden components, create a proper form that Spring Security can process
        // Create a form element using JavaScript
        String script = "const form = document.createElement('form');" +
                "form.method = 'post';" +
                "form.action = 'login';" +
                "form.style.display = 'none';" +

                // Add username field
                "const usernameInput = document.createElement('input');" +
                "usernameInput.type = 'text';" +
                "usernameInput.name = 'username';" +
                "usernameInput.value = " + "'" + usernameField.getValue() + "';" +
                "form.appendChild(usernameInput);" +

                // Add password field
                "const passwordInput = document.createElement('input');" +
                "passwordInput.type = 'password';" +
                "passwordInput.name = 'password';" +
                "passwordInput.value = " + "'" + passwordField.getValue() + "';" +
                "form.appendChild(passwordInput);" +

                // Append form to document body and submit
                "document.body.appendChild(form);" +
                "form.submit();";

        // Execute the JavaScript to create and submit the form
        getElement().executeJs(script);
    }

    private void showError(String message) {
        errorMessage.setText(message);
        errorMessage.setVisible(true);
    }
}
