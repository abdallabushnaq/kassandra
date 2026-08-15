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

import de.bushnaq.abdalla.kassandra.service.SecurityConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Authenticates the restricted local recovery account used only for security setup.
 */
@Component
public class SetupRecoveryAuthenticationProvider implements AuthenticationProvider {

    /**
     * Fixed principal accepted by the local recovery form.
     */
    public static final String USERNAME = "setup-recovery";

    @Autowired
    private SecurityConfigurationService securityConfigurationService;

    /**
     * Authenticates the fixed recovery principal using the persisted bcrypt password hash.
     *
     * @param authentication recovery form authentication
     * @return restricted setup administrator authentication
     * @throws BadCredentialsException when the principal or password is invalid
     */
    @Override
    public Authentication authenticate(Authentication authentication) {
        if (!USERNAME.equals(authentication.getName())
                || !securityConfigurationService.matchesRecoveryPassword(String.valueOf(authentication.getCredentials()))) {
            throw new BadCredentialsException("Invalid recovery credentials");
        }
        return new UsernamePasswordAuthenticationToken(
                USERNAME,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_SETUP_ADMIN")));
    }

    /**
     * Determines whether this provider handles username/password recovery authentication.
     *
     * @param authentication authentication token class
     * @return true for username/password authentication tokens
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
