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

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.bushnaq.abdalla.kassandra.security.SetupRecoveryAuthenticationProvider;

/**
 * Provides the restricted local recovery sign-in form.
 */
@Route(RecoveryView.ROUTE)
@PageTitle("Recover Kassandra access")
@AnonymousAllowed
public class RecoveryView extends VerticalLayout {

    /**
     * The public recovery route.
     */
    public static final String ROUTE = "recovery";

    /**
     * Creates the restricted recovery login form.
     */
    public RecoveryView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        LoginForm recoveryForm = new LoginForm();
        recoveryForm.setAction(ROUTE + "/login");
        recoveryForm.setForgotPasswordButtonVisible(false);
        add(new H1("Kassandra recovery"),
                new Paragraph("Recovery account: " + SetupRecoveryAuthenticationProvider.USERNAME),
                recoveryForm);
    }
}
