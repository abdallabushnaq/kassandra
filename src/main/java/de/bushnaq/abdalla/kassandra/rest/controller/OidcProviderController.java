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

package de.bushnaq.abdalla.kassandra.rest.controller;

import de.bushnaq.abdalla.kassandra.dao.OidcProviderDAO;
import de.bushnaq.abdalla.kassandra.dto.OidcProvider;
import de.bushnaq.abdalla.kassandra.dto.OidcProviderCreateRequest;
import de.bushnaq.abdalla.kassandra.service.OidcProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing persisted OpenID Connect providers.
 */
@RestController
@RequestMapping("/api/oidc-provider")
public class OidcProviderController {

    @Autowired
    private OidcProviderService oidcProviderService;

    /**
     * Creates and validates an OIDC provider.
     *
     * @param request provider configuration
     * @return created provider without its client secret
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SETUP_ADMIN')")
    public OidcProvider create(@RequestBody OidcProviderCreateRequest request) {
        return toDto(oidcProviderService.createProvider(
                request.getDisplayName(),
                request.getIssuerUri(),
                request.getClientId(),
                request.getClientSecret(),
                request.getScopes()
        ));
    }

    /**
     * Deletes an OIDC provider.
     *
     * @param id provider identifier
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id) {
        oidcProviderService.deleteProvider(id);
    }

    /**
     * Disables an OIDC provider.
     *
     * @param id provider identifier
     */
    @PutMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public void disable(@PathVariable UUID id) {
        oidcProviderService.disableProvider(id);
    }

    /**
     * Enables an OIDC provider after discovery validation.
     *
     * @param id provider identifier
     */
    @PutMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public void enable(@PathVariable UUID id) {
        oidcProviderService.enableProvider(id);
    }

    /**
     * Lists all configured providers for administration.
     *
     * @return configured providers without client secrets
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<OidcProvider> getAll() {
        return oidcProviderService.getProviders().stream().map(this::toDto).toList();
    }

    private OidcProvider toDto(OidcProviderDAO provider) {
        OidcProvider dto = new OidcProvider();
        dto.setId(provider.getId());
        dto.setDisplayName(provider.getDisplayName());
        dto.setIssuerUri(provider.getIssuerUri());
        dto.setRegistrationId(provider.getRegistrationId());
        dto.setEnabled(provider.isEnabled());
        dto.setScopes(Arrays.stream(provider.getScopes().split(",")).map(String::trim).toList());
        return dto;
    }
}
