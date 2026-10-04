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
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.bushnaq.abdalla.kassandra.dto.OidcProvider;
import de.bushnaq.abdalla.kassandra.dto.OidcProviderCreateRequest;
import de.bushnaq.abdalla.kassandra.rest.api.OidcProviderApi;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Allows Kassandra administrators to manage trusted OIDC providers.
 */
@Route(value = OidcProviderManagementView.ROUTE, layout = MainLayout.class)
@PageTitle("Identity Providers")
@RolesAllowed("ADMIN")
public class OidcProviderManagementView extends VerticalLayout {
    /**
     * Identifier of the identity-provider page title.
     */
    public static final String PAGE_TITLE_ID = "identity-providers-page-title";

    /**
     * Identifier of the provider name field.
     */
    public static final String NAME_FIELD_ID          = "identity-provider-name";
    /**
     * Identifier of the issuer URI field.
     */
    public static final String ISSUER_FIELD_ID        = "identity-provider-issuer";
    /**
     * Identifier of the client ID field.
     */
    public static final String CLIENT_ID_FIELD_ID     = "identity-provider-client-id";
    /**
     * Identifier of the client secret field.
     */
    public static final String CLIENT_SECRET_FIELD_ID = "identity-provider-client-secret";
    /**
     * Identifier of the scopes field.
     */
    public static final String SCOPES_FIELD_ID        = "identity-provider-scopes";
    /**
     * Identifier of the add-provider button.
     */
    public static final String ADD_BUTTON_ID          = "identity-provider-add-button";
    /**
     * Identifier of the providers grid.
     */
    public static final String GRID_ID                = "identity-providers-grid";
    /**
     * Prefix for provider enable/disable buttons, followed by the provider ID.
     */
    public static final String STATE_BUTTON_PREFIX    = "identity-provider-state-";
    /**
     * Prefix for provider delete buttons, followed by the provider ID.
     */
    public static final String DELETE_BUTTON_PREFIX   = "identity-provider-delete-";

    /**
     * Route for OIDC provider administration.
     */
    public static final String ROUTE = "identity-providers";

    private final Grid<OidcProvider> grid = new Grid<>(OidcProvider.class, false);
    private final OidcProviderApi    oidcProviderApi;

    /**
     * Creates the identity-provider management view.
     *
     * @param oidcProviderApi OIDC provider REST client
     */
    public OidcProviderManagementView(OidcProviderApi oidcProviderApi) {
        this.oidcProviderApi = oidcProviderApi;

        setSizeFull();
        H1 title = new H1("Identity Providers");
        title.setId(PAGE_TITLE_ID);
        add(title,
                new Paragraph("Client secrets are encrypted and are never displayed after they have been saved."),
                createProviderForm());
        configureGrid();
        add(grid);
        refreshGrid();
    }

    private HorizontalLayout createProviderForm() {
        TextField     displayName  = new TextField("Name");
        TextField     issuerUri    = new TextField("Issuer URI");
        TextField     clientId     = new TextField("Client ID");
        PasswordField clientSecret = new PasswordField("Client secret");
        TextField     scopes       = new TextField("Scopes");
        displayName.setId(NAME_FIELD_ID);
        issuerUri.setId(ISSUER_FIELD_ID);
        clientId.setId(CLIENT_ID_FIELD_ID);
        clientSecret.setId(CLIENT_SECRET_FIELD_ID);
        scopes.setId(SCOPES_FIELD_ID);
        scopes.setValue("openid,profile,email");
        Button addProvider = new Button("Add provider", event -> {
            try {
                oidcProviderApi.create(createProviderRequest(
                        displayName.getValue(),
                        issuerUri.getValue(),
                        clientId.getValue(),
                        clientSecret.getValue(),
                        List.of(scopes.getValue().split(","))));
                displayName.clear();
                issuerUri.clear();
                clientId.clear();
                clientSecret.clear();
                refreshGrid();
                Notification.show("Identity provider added");
            } catch (IllegalArgumentException | IllegalStateException | ResponseStatusException e) {
                Notification.show(e.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });

        addProvider.setId(ADD_BUTTON_ID);
        HorizontalLayout form = new HorizontalLayout(displayName, issuerUri, clientId, clientSecret, scopes, addProvider);
        form.setWidthFull();
        form.setAlignItems(Alignment.END);
        return form;
    }

    private void configureGrid() {
        grid.setId(GRID_ID);
        grid.addColumn(OidcProvider::getDisplayName).setHeader("Name").setAutoWidth(true);
        grid.addColumn(OidcProvider::getIssuerUri).setHeader("Issuer").setFlexGrow(1);
        grid.addColumn(provider -> provider.isEnabled() ? "Enabled" : "Disabled").setHeader("Status").setAutoWidth(true);
        grid.addComponentColumn(this::createActions).setHeader("Actions").setAutoWidth(true);
        grid.setSizeFull();
    }

    private HorizontalLayout createActions(OidcProvider provider) {
        Button state = new Button(provider.isEnabled() ? "Disable" : "Enable", event -> {
            try {
                if (provider.isEnabled()) {
                    oidcProviderApi.disable(provider.getId());
                } else {
                    oidcProviderApi.enable(provider.getId());
                }
                refreshGrid();
            } catch (IllegalArgumentException | IllegalStateException | ResponseStatusException e) {
                Notification.show(e.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        Button delete = new Button("Delete", event -> {
            try {
                oidcProviderApi.deleteById(provider.getId());
                refreshGrid();
            } catch (IllegalArgumentException | IllegalStateException | ResponseStatusException e) {
                Notification.show(e.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        state.setId(STATE_BUTTON_PREFIX + provider.getId());
        delete.setId(DELETE_BUTTON_PREFIX + provider.getId());
        return new HorizontalLayout(state, delete);
    }

    private void refreshGrid() {
        grid.setItems(oidcProviderApi.getAll());
    }

    private OidcProviderCreateRequest createProviderRequest(String displayName, String issuerUri, String clientId,
                                                            String clientSecret, List<String> scopes) {
        OidcProviderCreateRequest request = new OidcProviderCreateRequest();
        request.setDisplayName(displayName);
        request.setIssuerUri(issuerUri);
        request.setClientId(clientId);
        request.setClientSecret(clientSecret);
        request.setScopes(scopes);
        return request;
    }
}
