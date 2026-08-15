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

package de.bushnaq.abdalla.kassandra.ui.view;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import de.bushnaq.abdalla.kassandra.dao.OidcProviderDAO;
import de.bushnaq.abdalla.kassandra.service.OidcProviderService;
import de.bushnaq.abdalla.kassandra.service.SecurityConfigurationService;

import java.util.List;

import static de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil.DIALOG_DEFAULT_WIDTH;

/**
 * Login view that lists the currently enabled OIDC providers.
 */
@Route(LoginView.ROUTE)
@PageTitle("Login | Kassandra")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    /**
     * Identifier of the login view root component.
     */
    public static final String LOGIN_VIEW = "login-view";
    /**
     * Identifier used for the sole provider button to preserve existing UI test integration.
     */
    public static final String OIDC_LOGIN_BUTTON = "oidc-login-button";
    /**
     * The public login route.
     */
    public static final String ROUTE      = "login";

    /**
     * Creates the dynamic provider selection page.
     *
     * @param oidcProviderService provider configuration service
     */
    public LoginView(OidcProviderService oidcProviderService, SecurityConfigurationService securityConfigurationService) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        VerticalLayout centeringLayout = new VerticalLayout();
        centeringLayout.setId(LOGIN_VIEW);
        centeringLayout.setWidth(DIALOG_DEFAULT_WIDTH);
        centeringLayout.setPadding(false);
        centeringLayout.setSpacing(true);
        centeringLayout.setAlignSelf(Alignment.CENTER);
        centeringLayout.setAlignItems(Alignment.CENTER);

        H1 title = new H1("Kassandra");
        title.addClassNames(Margin.Bottom.MEDIUM);
        centeringLayout.add(title, createProviderButtons(oidcProviderService, securityConfigurationService));
        add(centeringLayout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        // Authentication errors are handled by Spring Security.
    }

    private Anchor createProviderButton(OidcProviderDAO provider, boolean onlyProvider) {
        Anchor loginButton = new Anchor("/oauth2/authorization/" + provider.getRegistrationId(),
                "Sign in with " + provider.getDisplayName());
        loginButton.setRouterIgnore(true);
        loginButton.setId(onlyProvider ? OIDC_LOGIN_BUTTON : OIDC_LOGIN_BUTTON + "-" + provider.getRegistrationId());
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
                .set("min-width", "250px");
        return loginButton;
    }

    private Div createProviderButtons(OidcProviderService oidcProviderService,
                                      SecurityConfigurationService securityConfigurationService) {
        VerticalLayout container = new VerticalLayout();
        container.setMaxWidth("400px");
        container.setAlignItems(Alignment.CENTER);
        container.setPadding(true);
        container.setSpacing(true);

        Paragraph instructions = new Paragraph("Please sign in with your organizational account");
        instructions.getStyle()
                .set("text-align", "center")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-bottom", "var(--lumo-space-m)");
        container.add(instructions);

        List<OidcProviderDAO> providers = oidcProviderService.getProviders().stream()
                .filter(OidcProviderDAO::isEnabled)
                .toList();
        for (OidcProviderDAO provider : providers) {
            container.add(createProviderButton(provider, providers.size() == 1));
        }
        if (container.getComponentCount() == 1) {
            Anchor setupLink = new Anchor("/ui/" + SetupView.ROUTE, "Set up Kassandra");
            setupLink.setRouterIgnore(true);
            container.add(new Paragraph("No sign-in provider has been configured yet."), setupLink);
        }
        if (securityConfigurationService.hasRecoveryCredential()) {
            Anchor recoveryLink = new Anchor("/ui/" + RecoveryView.ROUTE, "Use the recovery account");
            recoveryLink.setRouterIgnore(true);
            container.add(recoveryLink);
        }

        Div wrapper = new Div(container);
        wrapper.getStyle()
                .set("background-color", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "0 2px 10px var(--lumo-shade-20pct)")
                .set("padding", "var(--lumo-space-l)");
        return wrapper;
    }
}
