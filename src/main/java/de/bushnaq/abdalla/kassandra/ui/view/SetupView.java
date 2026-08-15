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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.bushnaq.abdalla.kassandra.dao.SecurityConfigurationDAO;
import de.bushnaq.abdalla.kassandra.service.OidcProviderService;
import de.bushnaq.abdalla.kassandra.service.SecurityConfigurationService;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Guides a new installation through protected recovery-account and OIDC provider setup.
 */
@Route(SetupView.ROUTE)
@PageTitle("Set up Kassandra")
@AnonymousAllowed
public class SetupView extends VerticalLayout implements BeforeEnterObserver {

    /**
     * The public setup route.
     */
    public static final String ROUTE = "setup";

    private final OidcProviderService          oidcProviderService;
    private final SecurityConfigurationService securityConfigurationService;

    /**
     * Creates the setup wizard.
     *
     * @param securityConfigurationService security setup lifecycle service
     * @param oidcProviderService OIDC provider configuration service
     */
    public SetupView(SecurityConfigurationService securityConfigurationService, OidcProviderService oidcProviderService) {
        this.securityConfigurationService = securityConfigurationService;
        this.oidcProviderService = oidcProviderService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        showCurrentStep();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        SecurityConfigurationDAO.SetupState setupState = securityConfigurationService.getConfiguration().getSetupState();
        if (setupState == SecurityConfigurationDAO.SetupState.SETUP_REQUIRED) {
            return;
        }
        if (isRecoveryAdministrator()) {
            return;
        }
        if (setupState == SecurityConfigurationDAO.SetupState.SETUP_IN_PROGRESS) {
            event.forwardTo(RecoveryView.class);
            return;
        }
        if (!securityConfigurationService.isSetupRequired()) {
            event.forwardTo(LoginView.class);
        }
    }

    private void addProviderForm() {
        TextField displayName = new TextField("Provider name");
        TextField issuerUri = new TextField("Issuer URI");
        TextField clientId = new TextField("Client ID");
        PasswordField clientSecret = new PasswordField("Client secret");
        TextField scopes = new TextField("Scopes");
        scopes.setValue("openid,profile,email");
        Button save = new Button("Validate provider", event -> {
            try {
                oidcProviderService.createProvider(
                        displayName.getValue(),
                        issuerUri.getValue(),
                        clientId.getValue(),
                        clientSecret.getValue(),
                        List.of(scopes.getValue().split(",")));
                Notification.show("Provider validated. Sign in with it to claim the first Kassandra administrator account.",
                        7000, Notification.Position.MIDDLE);
                getUI().ifPresent(ui -> ui.navigate(LoginView.class));
            } catch (IllegalArgumentException | IllegalStateException e) {
                Notification.show(e.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });

        add(new H1("Configure sign-in"), new Paragraph(
                "Enter the details of the first OpenID Connect provider. Kassandra validates the issuer discovery document before enabling it."),
                form(displayName, issuerUri, clientId, clientSecret, scopes, save));
    }

    private void addRecoveryCredentialForm() {
        PasswordField setupToken = new PasswordField("Setup token");
        PasswordField recoveryPassword = new PasswordField("Recovery password");
        PasswordField confirmation = new PasswordField("Confirm recovery password");
        Button continueButton = new Button("Continue", event -> {
            if (!recoveryPassword.getValue().equals(confirmation.getValue())) {
                Notification.show("The recovery passwords do not match", 5000, Notification.Position.MIDDLE);
                return;
            }
            try {
                securityConfigurationService.claimSetup(setupToken.getValue(), recoveryPassword.getValue());
                showCurrentStep();
            } catch (IllegalArgumentException | IllegalStateException e) {
                Notification.show(e.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });

        add(new H1("Set up Kassandra"), new Paragraph(
                "Enter the deployment setup token and create a password for the restricted recovery account."),
                form(setupToken, recoveryPassword, confirmation, continueButton));
    }

    private FormLayout form(Component... components) {
        FormLayout formLayout = new FormLayout();
        formLayout.add(components);
        return formLayout;
    }

    private void showCurrentStep() {
        removeAll();
        if (securityConfigurationService.hasRecoveryCredential()) {
            addProviderForm();
        } else {
            addRecoveryCredentialForm();
        }
    }

    private boolean isRecoveryAdministrator() {
        return SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_SETUP_ADMIN".equals(authority.getAuthority()));
    }
}
