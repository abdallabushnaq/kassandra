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

import de.bushnaq.abdalla.kassandra.dao.OidcProviderDAO;
import de.bushnaq.abdalla.kassandra.repository.OidcProviderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves enabled OIDC client registrations from persisted provider configuration.
 */
@Component
public class DatabaseClientRegistrationRepository implements ClientRegistrationRepository, Iterable<ClientRegistration> {

    @Autowired
    private OidcClientRegistrationFactory clientRegistrationFactory;
    private final Map<String, ClientRegistration> registrations = new ConcurrentHashMap<>();
    @Autowired
    private OidcProviderRepository         oidcProviderRepository;

    /**
     * Finds an enabled client registration.
     *
     * @param registrationId persistent provider registration identifier
     * @return matching enabled client registration, or null when unavailable
     */
    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        if (registrationId == null) {
            return null;
        }

        ClientRegistration cachedRegistration = registrations.get(registrationId);
        if (cachedRegistration != null) {
            return cachedRegistration;
        }

        return oidcProviderRepository.findByRegistrationId(registrationId)
                .filter(OidcProviderDAO::isEnabled)
                .map(provider -> registrations.computeIfAbsent(
                        registrationId, ignored -> clientRegistrationFactory.create(provider)))
                .orElse(null);
    }

    /**
     * Invalidates cached registration data after a provider mutation.
     *
     * @param registrationId registration identifier to evict
     */
    public void invalidate(String registrationId) {
        registrations.remove(registrationId);
    }

    /**
     * Lists the currently enabled registrations.
     *
     * @return iterator over enabled client registrations
     */
    @Override
    public Iterator<ClientRegistration> iterator() {
        List<ClientRegistration> enabledRegistrations = new ArrayList<>();
        for (OidcProviderDAO provider : oidcProviderRepository.findByEnabledTrueOrderByDisplayNameAsc()) {
            ClientRegistration registration = findByRegistrationId(provider.getRegistrationId());
            if (registration != null) {
                enabledRegistrations.add(registration);
            }
        }
        return enabledRegistrations.iterator();
    }
}
