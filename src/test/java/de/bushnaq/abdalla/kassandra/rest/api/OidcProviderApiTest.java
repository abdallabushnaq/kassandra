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

package de.bushnaq.abdalla.kassandra.rest.api;

import de.bushnaq.abdalla.kassandra.dao.OidcProviderDAO;
import de.bushnaq.abdalla.kassandra.dto.OidcProvider;
import de.bushnaq.abdalla.kassandra.repository.OidcProviderRepository;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractUiTestUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the REST API boundary for OpenID Connect provider management.
 */
@Tag("UnitTest")
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
public class OidcProviderApiTest extends AbstractUiTestUtil {

    @Autowired
    private JsonMapper             jsonMapper;
    @Autowired
    private OidcProviderApi        oidcProviderApi;
    @Autowired
    private OidcProviderRepository oidcProviderRepository;

    /**
     * Returns administrative provider data without client secrets.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void administratorGetsProvidersWithoutSecrets() {
        saveProvider("Provider", true);

        OidcProvider provider = oidcProviderApi.getAll().getFirst();

        assertEquals("Provider", provider.getDisplayName());
        assertFalse(jsonMapper.writeValueAsString(provider).contains("clientSecret"));
    }

    /**
     * Allows administrators to list and disable a configured provider through the API.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void administratorCanListAndDisableProvider() {
        OidcProviderDAO provider = saveProvider("Provider", true);

        assertEquals(1, oidcProviderApi.getAll().size());
        oidcProviderApi.disable(provider.getId());

        assertFalse(oidcProviderRepository.findById(provider.getId()).orElseThrow().isEnabled());
    }

    /**
     * Prevents regular users from accessing provider administration.
     */
    @Test
    @WithMockUser(username = "user", roles = "USER")
    public void regularUserCannotManageProviders() {
        assertThrows(AccessDeniedException.class, () -> oidcProviderApi.getAll());
    }

    private OidcProviderDAO saveProvider(String displayName, boolean enabled) {
        OidcProviderDAO provider = new OidcProviderDAO();
        provider.setDisplayName(displayName);
        provider.setIssuerUri("https://" + displayName.toLowerCase() + ".example.test");
        provider.setClientId("client-" + displayName);
        provider.setClientSecretEncrypted("encrypted-secret-" + displayName);
        provider.setRegistrationId("oidc-" + displayName.toLowerCase());
        provider.setScopes("openid,profile,email");
        provider.setEnabled(enabled);
        return oidcProviderRepository.saveAndFlush(provider);
    }
}
