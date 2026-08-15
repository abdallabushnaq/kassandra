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

import de.bushnaq.abdalla.kassandra.ui.view.OidcIdentityLinkView;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Restores the initiating administrator after an identity-link authorization flow.
 */
@Component
public class OidcIdentityLinkAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Autowired
    private OidcIdentityLinkService oidcIdentityLinkService;

    /**
     * Restores the prior administrator when the OIDC flow was used only to link an identity.
     *
     * @param request callback request
     * @param response callback response
     * @param authentication newly authenticated provider identity
     * @throws IOException when the redirect cannot be written
     * @throws ServletException when the parent handler cannot complete the response
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        Authentication originalAuthentication = oidcIdentityLinkService.consumeOriginalAuthentication();
        if (originalAuthentication != null) {
            SecurityContextHolder.getContext().setAuthentication(originalAuthentication);
            new HttpSessionSecurityContextRepository().saveContext(SecurityContextHolder.getContext(), request, response);
            getRedirectStrategy().sendRedirect(request, response, "/ui/" + OidcIdentityLinkView.ROUTE);
            return;
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
