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
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package de.bushnaq.abdalla.kassandra.rest.api;

import de.bushnaq.abdalla.kassandra.dao.OidcProviderDAO;
import de.bushnaq.abdalla.kassandra.dao.UserGroupDAO;
import de.bushnaq.abdalla.kassandra.dao.UserDAO;
import de.bushnaq.abdalla.kassandra.dto.AuditPage;
import de.bushnaq.abdalla.kassandra.repository.OidcProviderRepository;
import de.bushnaq.abdalla.kassandra.repository.UserGroupRepository;
import de.bushnaq.abdalla.kassandra.repository.UserRepository;
import de.bushnaq.abdalla.kassandra.service.ServerSettingsCatalogue.Keys;
import de.bushnaq.abdalla.kassandra.service.ServerSettingsService;
import de.bushnaq.abdalla.kassandra.service.UserGroupService;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractUiTestUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the authenticated, filtered, and database-paged audit API.
 */
@Tag("UnitTest")
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
public class AuditLogApiTest extends AbstractUiTestUtil {
    @Autowired
    private JsonMapper             jsonMapper;
    @Autowired
    private Environment            environment;
    @Autowired
    private RestTemplate           restTemplate;
    private AuditLogApi            auditLogApi;
    @Autowired
    private OidcProviderRepository providerRepository;
    @Autowired
    private UserGroupRepository    groupRepository;
    @Autowired
    private UserRepository         userRepository;
    @Autowired
    private ServerSettingsService  settingsService;
    @Autowired
    private UserGroupService       groupService;

    /**
     * Starts each test with a clean database and a client aimed at its random port.
     *
     * @param testInfo test metadata
     */
    @Override
    @BeforeEach
    protected void beforeEach(TestInfo testInfo) {
        super.beforeEach(testInfo);
        auditLogApi = new AuditLogApi(restTemplate, jsonMapper,
                "http://localhost:" + environment.getRequiredProperty("local.server.port") + "/api");
    }

    /**
     * Includes historical creates, updates, and deletes, without leaking stored secrets.
     */
    @Test
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void tracksChangesAndNeverReturnsProviderSecrets() {
        OidcProviderDAO provider = new OidcProviderDAO();
        provider.setDisplayName("Audit provider");
        provider.setIssuerUri("https://audit.example.com");
        provider.setClientId("audit-client");
        provider.setClientSecretEncrypted("secret-ciphertext");
        provider.setRegistrationId("audit-oidc");
        provider = providerRepository.saveAndFlush(provider);
        provider.setEnabled(true);
        providerRepository.saveAndFlush(provider);
        providerRepository.delete(provider);
        providerRepository.flush();

        AuditPage changes = auditLogApi.getPage("christopher.paul@kassandra.org", null, "Identity Provider",
                null, null, 0, 50);
        assertEquals(3, changes.total());
        assertEquals(3, changes.items().size());
        assertTrue(changes.items().stream().anyMatch(event -> "CREATE".equals(event.action())));
        assertTrue(changes.items().stream().anyMatch(event -> "UPDATE".equals(event.action())));
        assertTrue(changes.items().stream().anyMatch(event -> "DELETE".equals(event.action())));
        assertTrue(changes.items().stream().anyMatch(event -> "DELETE".equals(event.action())
                && "Audit provider".equals(event.label())));
        assertTrue(changes.items().stream().anyMatch(event -> "UPDATE".equals(event.action())
                && event.fieldChanges().stream().anyMatch(change -> change.contains("enabled: false -> true"))));
        assertTrue(changes.items().stream().allMatch(event -> "christopher.paul@kassandra.org".equals(event.actor())));
        assertFalse(changes.toString().contains("secret-ciphertext"));
        assertTrue(entityManager.createNativeQuery("SELECT column_name FROM INFORMATION_SCHEMA.COLUMNS"
                        + " WHERE table_name = 'OIDC_PROVIDERS_AUD' AND column_name = 'CLIENT_SECRET_ENCRYPTED'")
                .getResultList().isEmpty());
    }

    /**
     * Applies user, action, date search, timeframe, and paging at the API boundary.
     */
    @Test
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void filtersAndPagesChanges() {
        UserGroupDAO group = new UserGroupDAO();
        group.setName("Audited team");
        group = groupRepository.save(group);
        group.setDescription("Updated description");
        groupRepository.save(group);

        Instant   start  = Instant.now().minus(1, ChronoUnit.DAYS);
        AuditPage first  = auditLogApi.getPage("christopher", null, "Audited team", start, null, 0, 1);
        AuditPage second = auditLogApi.getPage("christopher", null, "Audited team", start, null, 1, 1);
        assertEquals(2, first.total());
        assertEquals(1, first.items().size());
        assertEquals(1, second.items().size());
        assertNotEquals(first.items().getFirst().revision(), second.items().getFirst().revision());
        assertEquals(1, auditLogApi.getPage(null, "UPDATE", "Audited team", null, null, 0, 50).total());
        assertTrue(first.items().stream().anyMatch(event -> event.fieldChanges().stream()
                .anyMatch(change -> change.contains("description: null -> Updated description"))));
        assertTrue(first.items().stream().allMatch(event -> !event.replay()));
        AuditPage dated = auditLogApi.getPage(null, null, Instant.now().toString().substring(0, 10),
                null, null, 0, 50);
        assertTrue(dated.total() >= 2);
        assertTrue(dated.items().stream().anyMatch(event -> "Audited team".equals(event.label())));
        assertEquals(0, auditLogApi.getPage(null, null, "Audited team",
                Instant.now().plus(1, ChronoUnit.DAYS), null, 0, 50).total());
    }

    /**
     * Shows setting updates while keeping their persisted secret values out of Envers.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void settingsAuditOmitsSecretValues() {
        settingsService.update(Keys.OPENAI_API_KEY, "very-sensitive-test-key", false);
        settingsService.update(Keys.OPENAI_API_KEY, "another-sensitive-test-key", false);

        AuditPage changes = auditLogApi.getPage("admin-user", null, Keys.OPENAI_API_KEY,
                null, null, 0, 50);
        assertEquals(2, changes.total());
        assertEquals("Server Setting", changes.items().getFirst().entityType());
        assertTrue(changes.items().stream().anyMatch(event -> "UPDATE".equals(event.action())));
        assertFalse(changes.toString().contains("very-sensitive-test-key"));
        assertFalse(changes.toString().contains("another-sensitive-test-key"));
        assertTrue(changes.items().stream().allMatch(event -> event.fieldChanges().stream()
                .noneMatch(change -> change.toLowerCase().contains("value"))));
        assertTrue(entityManager.createNativeQuery("SELECT column_name FROM INFORMATION_SCHEMA.COLUMNS"
                        + " WHERE table_name = 'SERVER_SETTINGS_AUD' AND column_name = 'SETTING_VALUE'")
                .getResultList().isEmpty());
    }

    /**
     * Exposes old and new values only for non-secret server settings.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void settingsAuditShowsSafeValueChanges() {
        settingsService.update(Keys.OPENAI_BASE_URL, "https://first.example.org", false);
        settingsService.update(Keys.OPENAI_BASE_URL, "https://second.example.org", false);

        AuditPage changes = auditLogApi.getPage("admin-user", "UPDATE", Keys.OPENAI_BASE_URL,
                null, null, 0, 50);
        assertTrue(changes.items().stream().anyMatch(event -> event.fieldChanges().stream()
                .anyMatch(change -> change.contains("https://first.example.org -> https://second.example.org"))));
    }

    /**
     * Keeps user and membership changes searchable even after the user is deleted.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void tracksUsersAndGroupMembership() {
        UserDAO user = new UserDAO();
        user.setName("Audit Person");
        user.setEmail("audited-person@example.org");
        user.setColor(Color.BLUE);
        user = userRepository.save(user);
        user.setName("Renamed Person");
        user = userRepository.save(user);

        UserGroupDAO group = new UserGroupDAO();
        group.setName("Membership audit");
        group = groupRepository.save(group);
        group.addMember(user.getId());
        groupRepository.save(group);
        userRepository.delete(user);
        groupService.deleteGroup(group.getId());

        AuditPage userChanges = auditLogApi.getPage(null, null, "audited-person@example.org",
                null, null, 0, 50);
        assertEquals(3, userChanges.total());
        assertTrue(userChanges.items().stream().anyMatch(event -> "DELETE".equals(event.action())));
        AuditPage groupChanges = auditLogApi.getPage(null, null, "Membership audit", null, null, 0, 50);
        assertEquals(3, groupChanges.total());
        assertTrue(groupChanges.items().stream().anyMatch(event -> "UPDATE".equals(event.action())
                && event.fieldChanges().stream().anyMatch(change -> change.startsWith("memberIds:"))));
        assertTrue(groupChanges.items().stream().anyMatch(event -> "DELETE".equals(event.action())
                && "Membership audit".equals(event.label())));
    }

    /**
     * Denies non-administrators access to all audit data.
     */
    @Test
    @WithMockUser(username = "user", roles = "USER")
    public void deniesNonAdministrators() {
        assertThrows(AccessDeniedException.class, () -> auditLogApi.getPage(null, null, null, null, null, 0, 50));
    }
}
