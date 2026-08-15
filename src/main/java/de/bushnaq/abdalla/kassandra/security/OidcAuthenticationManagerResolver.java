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

import com.nimbusds.jwt.SignedJWT;
import de.bushnaq.abdalla.kassandra.repository.OidcProviderRepository;
import de.bushnaq.abdalla.kassandra.service.OidcIdentityService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Selects JWT authentication managers only for enabled, persisted OIDC issuers.
 */
@Component
public class OidcAuthenticationManagerResolver implements AuthenticationManagerResolver<HttpServletRequest> {

    private final BearerTokenResolver                bearerTokenResolver = new DefaultBearerTokenResolver();
    private final Map<String, AuthenticationManager> managers            = new ConcurrentHashMap<>();
    @Autowired
    private       OidcIdentityService                oidcIdentityService;
    @Autowired
    private       OidcProviderRepository             oidcProviderRepository;

    /**
     * Evicts the cached authentication manager after an OIDC provider changes.
     *
     * @param issuerUri exact provider issuer URI
     */
    public void invalidate(String issuerUri) {
        managers.remove(issuerUri);
    }

    /**
     * Resolves a trusted authentication manager for the bearer token issuer.
     *
     * @param request API request carrying a bearer token
     * @return authentication manager for an enabled configured issuer
     * @throws InvalidBearerTokenException when the token is malformed or its issuer is not enabled
     */
    @Override
    public AuthenticationManager resolve(HttpServletRequest request) {
        String token = bearerTokenResolver.resolve(request);
        if (token == null) {
            return null;
        }
        String issuerUri = readIssuer(token);
        if (oidcProviderRepository.findByIssuerUriAndEnabledTrue(issuerUri).isEmpty()) {
            throw new InvalidBearerTokenException("JWT issuer is not enabled");
        }
        return managers.computeIfAbsent(issuerUri, this::createAuthenticationManager);
    }

    private AuthenticationManager createAuthenticationManager(String issuerUri) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> oidcIdentityService
                .resolveRoles(jwt.getIssuer().toString(), jwt.getSubject(), null, null, false)
                .stream()
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList());
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(JwtDecoders.fromIssuerLocation(issuerUri));
        provider.setJwtAuthenticationConverter(converter);
        return provider::authenticate;
    }

    private String readIssuer(String token) {
        try {
            String issuerUri = SignedJWT.parse(token).getJWTClaimsSet().getIssuer();
            if (issuerUri == null || issuerUri.isBlank()) {
                throw new InvalidBearerTokenException("JWT issuer is missing");
            }
            return issuerUri;
        } catch (ParseException e) {
            throw new InvalidBearerTokenException("JWT cannot be parsed", e);
        }
    }
}
