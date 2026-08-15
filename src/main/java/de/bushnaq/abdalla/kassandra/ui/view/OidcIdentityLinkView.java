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

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.bushnaq.abdalla.kassandra.dao.OidcProviderDAO;
import de.bushnaq.abdalla.kassandra.dao.UserDAO;
import de.bushnaq.abdalla.kassandra.security.OidcIdentityLinkService;
import de.bushnaq.abdalla.kassandra.service.OidcProviderService;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import jakarta.annotation.security.RolesAllowed;

/**
 * Lets an administrator explicitly bind an OIDC identity to an existing Kassandra user.
 */
@Route(value = OidcIdentityLinkView.ROUTE, layout = MainLayout.class)
@PageTitle("Link OIDC Identity")
@RolesAllowed("ADMIN")
public class OidcIdentityLinkView extends VerticalLayout {

    /**
     * Route for explicit OIDC identity linking.
     */
    public static final String ROUTE = "link-identity";
    /**
     * Identifier of the user selection field.
     */
    public static final String USER_FIELD = "link-identity-user-field";
    /**
     * Identifier of the identity-provider selection field.
     */
    public static final String PROVIDER_FIELD = "link-identity-provider-field";
    /**
     * Identifier of the button that begins OIDC authentication for linking.
     */
    public static final String AUTHENTICATE_AND_LINK_BUTTON = "authenticate-and-link-button";

    /**
     * Creates the identity linking view.
     *
     * @param oidcIdentityLinkService identity-link authorization coordinator
     * @param oidcProviderService OIDC provider configuration service
     */
    public OidcIdentityLinkView(OidcIdentityLinkService oidcIdentityLinkService,
                                OidcProviderService oidcProviderService) {
        ComboBox<UserDAO> user = new ComboBox<>("Kassandra user");
        user.setId(USER_FIELD);
        user.setItems(oidcIdentityLinkService.getUsers());
        user.setItemLabelGenerator(value -> value.getName() + " <" + value.getEmail() + ">");

        ComboBox<OidcProviderDAO> provider = new ComboBox<>("Identity provider");
        provider.setId(PROVIDER_FIELD);
        provider.setItems(oidcProviderService.getProviders().stream().filter(OidcProviderDAO::isEnabled).toList());
        provider.setItemLabelGenerator(OidcProviderDAO::getDisplayName);

        Button link = new Button("Authenticate and link", event -> {
            if (user.isEmpty() || provider.isEmpty()) {
                Notification.show("Select a Kassandra user and an identity provider");
                return;
            }
            try {
                oidcIdentityLinkService.startLink(user.getValue().getId(), provider.getValue().getRegistrationId());
                getUI().ifPresent(ui -> ui.getPage()
                        .setLocation("/oauth2/authorization/" + provider.getValue().getRegistrationId()));
            } catch (IllegalArgumentException | IllegalStateException e) {
                Notification.show(e.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        link.setId(AUTHENTICATE_AND_LINK_BUTTON);

        setSizeFull();
        add(new H1("Link OIDC Identity"),
                new Paragraph("Kassandra will authenticate with the selected provider and bind its immutable issuer and subject to the selected user."),
                user, provider, link);
    }
}
