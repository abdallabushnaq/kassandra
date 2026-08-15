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

package de.bushnaq.abdalla.kassandra.repository;

import de.bushnaq.abdalla.kassandra.dao.OidcProviderDAO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accesses configured OpenID Connect providers.
 */
public interface OidcProviderRepository extends JpaRepository<OidcProviderDAO, UUID> {

    /**
     * Finds all enabled providers in deterministic display order.
     *
     * @return enabled providers
     */
    List<OidcProviderDAO> findByEnabledTrueOrderByDisplayNameAsc();

    /**
     * Finds a provider by its immutable Spring Security registration identifier.
     *
     * @param registrationId registration identifier
     * @return matching provider, if present
     */
    Optional<OidcProviderDAO> findByRegistrationId(String registrationId);

    /**
     * Finds an enabled provider by its exact OIDC issuer URI.
     *
     * @param issuerUri issuer URI from a validated ID or access token
     * @return matching enabled provider, if present
     */
    Optional<OidcProviderDAO> findByIssuerUriAndEnabledTrue(String issuerUri);

    /**
     * Finds a provider by its exact issuer URI.
     *
     * @param issuerUri issuer URI
     * @return matching provider, if present
     */
    Optional<OidcProviderDAO> findByIssuerUri(String issuerUri);
}
