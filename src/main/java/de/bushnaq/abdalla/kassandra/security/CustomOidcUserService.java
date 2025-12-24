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

import de.bushnaq.abdalla.kassandra.service.UserRoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Custom OIDC User Service that loads user roles from database ONCE during authentication
 * and stores them in the SecurityContext. This eliminates repeated database queries.
 * <p>
 * This is the proper Spring Security way to load custom authorities - the roles are
 * loaded during authentication and reused throughout the session.
 */
@Service
@Slf4j
public class CustomOidcUserService extends OidcUserService {

    private final UserRoleService userRoleService;

    public CustomOidcUserService(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // Delegate to the default implementation for loading the user
        OidcUser oidcUser = super.loadUser(userRequest);

        // Extract email from OIDC token
        String email = oidcUser.getEmail();
        if (email == null || email.isEmpty()) {
            email = oidcUser.getPreferredUsername();
        }
        if (email == null || email.isEmpty()) {
            email = oidcUser.getSubject();
        }

        log.info("🔍 Loading roles for OIDC user: {}", email);

        // Load roles from database ONCE
        List<String> dbRoles = userRoleService.getRolesByEmail(email);

        // Convert to GrantedAuthority
        Set<GrantedAuthority> authorities = new HashSet<>();
        if (!dbRoles.isEmpty()) {
            dbRoles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            log.info("✅ Loaded {} roles from database for {}: {}", dbRoles.size(), email, String.join(", ", dbRoles));
        } else {
            // No user in database - assign default USER role
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            log.warn("⚠️ User {} not found in database. Assigned default USER role.", email);
        }

        // Create new OidcUser with database roles
        // These roles are now stored in SecurityContext and reused for all authorization checks
        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }
}

