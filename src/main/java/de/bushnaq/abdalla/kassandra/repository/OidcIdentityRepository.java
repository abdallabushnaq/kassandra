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

import de.bushnaq.abdalla.kassandra.dao.OidcIdentityDAO;
import de.bushnaq.abdalla.kassandra.dao.OidcProviderDAO;
import de.bushnaq.abdalla.kassandra.dao.UserDAO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accesses explicit links between OIDC subjects and Kassandra users.
 */
public interface OidcIdentityRepository extends JpaRepository<OidcIdentityDAO, UUID> {

    /**
     * Tests whether a provider has linked identities.
     *
     * @param provider provider to inspect
     * @return true when at least one identity references the provider
     */
    boolean existsByProvider(OidcProviderDAO provider);

    /**
     * Finds an identity by its provider and immutable OIDC subject.
     *
     * @param provider configured provider
     * @param subject immutable OIDC subject
     * @return matching identity, if linked
     */
    Optional<OidcIdentityDAO> findByProviderAndSubject(OidcProviderDAO provider, String subject);

    /**
     * Lists all identities explicitly linked to a Kassandra user.
     *
     * @param user Kassandra user
     * @return linked identities
     */
    List<OidcIdentityDAO> findByUser(UserDAO user);
}
