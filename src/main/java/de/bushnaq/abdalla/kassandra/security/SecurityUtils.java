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

import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    /**
     * Gets the currently authenticated user.
     *
     * @return The UserDetails of the authenticated user or null if not authenticated
     */
    public static UserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return (UserDetails) principal;
        }

        return null;
    }

    /**
     * Gets the current HttpServletRequest
     *
     * @return the current HttpServletRequest
     */
    public static HttpServletRequest getHttpServletRequest() {
        return VaadinServletRequest.getCurrent();
    }

    /**
     * Gets the current HttpServletResponse
     *
     * @return the current HttpServletResponse
     */
    public static HttpServletResponse getHttpServletResponse() {
        return VaadinServletResponse.getCurrent();
    }

    /**
     * Gets the email of the currently authenticated user.
     * Works with both regular authentication and OIDC.
     *
     * @return The email of the authenticated user or "Guest" if not authenticated
     */
    public static String getUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String         userEmail      = authentication != null ? authentication.getName() : "Guest";

        // If using OIDC, try to get the email address from authentication details
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser) {
            String email = oidcUser.getEmail();
            if (email != null && !email.isEmpty()) {
                userEmail = email;
            }
        }
        return userEmail;
    }

    /**
     * Checks if the current user has the ADMIN role.
     *
     * @return true if the user is an admin, false otherwise
     */
    public static boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> "ROLE_ADMIN".equals(grantedAuthority.getAuthority()));
    }

    /**
     * Checks if the current user has the USER role.
     *
     * @return true if the user has user access, false otherwise
     */
    public static boolean isUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> "ROLE_USER".equals(grantedAuthority.getAuthority()));
    }
}
