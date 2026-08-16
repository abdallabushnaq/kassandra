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

package de.bushnaq.abdalla.kassandra.ui.util;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.ai.tts.narrator.Narrator;
import de.bushnaq.abdalla.kassandra.ai.tts.narrator.TtsCacheManager;
import de.bushnaq.abdalla.kassandra.repository.OidcProviderRepository;
import de.bushnaq.abdalla.kassandra.repository.UserRepository;
import de.bushnaq.abdalla.kassandra.security.DatabaseClientRegistrationRepository;
import de.bushnaq.abdalla.kassandra.security.OidcAuthenticationManagerResolver;
import de.bushnaq.abdalla.kassandra.service.OidcIdentityService;
import de.bushnaq.abdalla.kassandra.service.OidcProviderService;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract test utility class for UI tests that require Keycloak authentication.
 * It sets up a Keycloak container with a predefined realm configuration and
 * creates a persisted OIDC provider and explicit identity link for the test administrator.
 *
 * @author Abdalla Bushnaq
 */
@Testcontainers
public class AbstractKeycloakUiTestUtil extends AbstractUiTestUtil {
    private static final String                     KEYCLOAK_CLIENT_ID     = "kassandra-client";
    private static final String                     KEYCLOAK_CLIENT_SECRET = "test-client-secret";
    private static final String                     KEYCLOAK_REALM         = "project-hub-realm";
    private static final String                     KEYCLOAK_PROVIDER_NAME = "Test Keycloak";
    private static final String                     TEST_USER_EMAIL        = "christopher.paul@kassandra.org";
    private static final Logger                     logger                 = LoggerFactory.getLogger(AbstractKeycloakUiTestUtil.class);
    protected static     int                        allocatedPort;
    private static       KeycloakContainer          keycloakInstance;
    private static final KeycloakContainer          keycloak               = getKeycloakContainer();// Start Keycloak container with realm configuration
    @Autowired
    protected            PlatformTransactionManager transactionManager;
    @Autowired
    protected            OidcIdentityService        oidcIdentityService;
    @Autowired
    protected            OidcProviderService        oidcProviderService;
    @Autowired
    private              OidcProviderRepository     oidcProviderRepository;
    @Autowired
    private              DatabaseClientRegistrationRepository clientRegistrationRepository;
    @Autowired
    private              OidcAuthenticationManagerResolver    oidcAuthenticationManagerResolver;
    @Autowired
    protected            UserRepository             userRepository;

    @BeforeAll
    static void beforeAll() {
        StableDiffusionService.setEnabled(true);
    }

    private static synchronized KeycloakContainer getKeycloakContainer() {
        // Allocate port in static initializer (runs before everything)
        // we need to do this, as the random port must be known before the Spring context is started
        try (ServerSocket socket = new ServerSocket(0)) {
            allocatedPort = socket.getLocalPort();
            System.setProperty("test.server.port", String.valueOf(allocatedPort));
            System.out.println("=== Allocated port: " + allocatedPort + " ===");
        } catch (IOException e) {
            throw new RuntimeException("Failed to allocate port", e);
        }
        //start container
        if (keycloakInstance == null) {
            keycloakInstance = new KeycloakContainer("quay.io/keycloak/keycloak:26.5.6")
                    .withRealmImportFile("keycloak/project-hub-realm-realm.json")
                    .withAdminUsername("admin")
                    .withAdminPassword("admin")
                    .withLogConsumer(outputFrame -> System.out.println("Keycloak: " + outputFrame.getUtf8String()))
                    .withEnv("KC_HOSTNAME_STRICT", "false")
                    .withEnv("KC_HOSTNAME_STRICT_HTTPS", "false")
                    //.withEnv("KC_HTTP_ENABLED", "true")
                    .withReuse(true); // Enable container reuse
            //.waitingFor(Wait.forHttp("/realms/project-hub-realm")
            //        .forPort(8080) // Make sure wait strategy uses the correct port
            //        .forStatusCode(200));

            System.out.println("=== CREATING NEW KEYCLOAK CONTAINER ===");
        } else {
            System.out.println("=== REUSING EXISTING KEYCLOAK CONTAINER === ON PORT " + keycloakInstance.getHttpPort());
        }

        return keycloakInstance;
    }

    // Method to get the public-facing URL, fixing potential redirect issues
    private static String getPublicFacingUrl(KeycloakContainer container) {
        return String.format("http://%s:%s",
                container.getHost(),
                container.getMappedPort(8080));
    }

    // Starts Keycloak before the Spring test context is created.
    @DynamicPropertySource
    static void registerKeycloakProperties(DynamicPropertyRegistry registry) {
        // Ensure container is started
        if (!keycloak.isRunning()) {
            System.out.println("=== STARTING KEYCLOAK CONTAINER ===");
            keycloak.start();
            updateKeycloakClientRedirectUri(allocatedPort);
        } else {
            System.out.println("=== KEYCLOAK CONTAINER ALREADY RUNNING === ON PORT " + keycloakInstance.getHttpPort());
        }

    }

    /**
     * Creates the persisted OIDC provider and links the Keycloak test administrator before each test.
     * <p>
     * Uses explicit transaction management to ensure the user is committed
     * and visible to all subsequent transactions including API calls.
     */
    @BeforeEach
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void setupTestUser() {
        LocalDate firstDate     = ParameterOptions.getNow().toLocalDate().minusYears(2);
        peg.addUser("Christopher Paul", TEST_USER_EMAIL, "ADMIN,USER", "de", "nw", firstDate, peg.generateUserColor(peg.getUserIndex()), 0.5f, null);
        var provider = oidcProviderService.createProvider(
                KEYCLOAK_PROVIDER_NAME,
                getIssuerUri(),
                KEYCLOAK_CLIENT_ID,
                KEYCLOAK_CLIENT_SECRET,
                List.of("openid", "profile", "email"));
        var testUser = userRepository.findByEmail(TEST_USER_EMAIL)
                .orElseThrow(() -> new IllegalStateException("The Keycloak test user was not created"));
        oidcIdentityService.linkIdentity(testUser.getId(), provider.getRegistrationId(), getKeycloakUserSubject(TEST_USER_EMAIL));
        //ensure tests that generate more users will find the correct expectations.
        peg.getUsers().clear();
        peg.setUserIndex(peg.getUserIndex() - 1);//ensure Christopher Paul is always the first user created
    }

    @Override
    protected void generateProductsIfNeeded(TestInfo testInfo, RandomCase randomCase) throws Exception {
        super.generateProductsIfNeeded(testInfo, randomCase);
        refreshRestoredKeycloakProvider();
    }

    private static String getIssuerUri() {
        return getPublicFacingUrl(keycloak) + "/realms/" + KEYCLOAK_REALM;
    }

    private void refreshRestoredKeycloakProvider() {
        String issuerUri = getIssuerUri();
        var provider = oidcProviderRepository.findAll().stream()
                .filter(candidate -> KEYCLOAK_PROVIDER_NAME.equals(candidate.getDisplayName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("The Keycloak test provider was not restored"));
        if (issuerUri.equals(provider.getIssuerUri())) {
            return;
        }

        String previousIssuerUri = provider.getIssuerUri();
        provider.setIssuerUri(issuerUri);
        oidcProviderRepository.save(provider);
        clientRegistrationRepository.invalidate(provider.getRegistrationId());
        oidcAuthenticationManagerResolver.invalidate(previousIssuerUri);
        oidcAuthenticationManagerResolver.invalidate(issuerUri);
    }

    private static String getKeycloakUserSubject(String username) {
        var adminClient = org.keycloak.admin.client.KeycloakBuilder.builder()
                .serverUrl(keycloak.getAuthServerUrl())
                .realm("master")
                .username("admin")
                .password("admin")
                .clientId("admin-cli")
                .build();
        try {
            return adminClient.realm(KEYCLOAK_REALM)
                    .users()
                    .searchByUsername(username, true)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Keycloak test user does not exist: " + username))
                    .getId();
        } finally {
            adminClient.close();
        }
    }

    @AfterAll
    public static void tearDown() {
        if (TtsCacheManager.getCacheMiss() != 0) {
            logger.warn("*** TTS CACHE MISSES: {} ***", TtsCacheManager.getCacheMiss());
        }
        Narrator.resetCache();
    }

    /**
     * Update Keycloak client redirect URI to match the allocated random port.
     *
     * @param port
     */
    /**
     * Update Keycloak client redirect URI to match the allocated random port.
     * Also registers the post-logout redirect URI so that RP-initiated logout
     * (OIDC end_session_endpoint with post_logout_redirect_uri) is accepted by
     * Keycloak rather than rejected for an unregistered redirect URI.  Without
     * this, Keycloak keeps its SSO session alive and the user is silently
     * re-authenticated after a logout.
     *
     * @param port the random server port allocated for this test run
     */
    private static void updateKeycloakClientRedirectUri(int port) {
        String redirectUri         = "http://localhost:" + port + "/login/oauth2/code/*";
        String webOrigin           = "http://localhost:" + port;
        String postLogoutRedirects = "http://localhost:" + port + "/*";

        // Use Keycloak Admin API to update client
        var adminClient = org.keycloak.admin.client.KeycloakBuilder.builder()
                .serverUrl(keycloak.getAuthServerUrl())
                .realm("master")
                .username("admin")
                .password("admin")
                .clientId("admin-cli")
                .build();

        var client = adminClient.realm(KEYCLOAK_REALM)
                .clients()
                .findByClientId(KEYCLOAK_CLIENT_ID)
                .get(0);

        client.setRedirectUris(List.of(redirectUri));
        client.setWebOrigins(List.of(webOrigin));
        // Keycloak 18+ validates post_logout_redirect_uri against this list.
        // Stored via the client attributes map (works with all Keycloak admin client versions).
        // Registering a wildcard for localhost allows OidcClientInitiatedLogoutSuccessHandler
        // to redirect back to /ui/login after Keycloak invalidates its session.
        Map<String, String> attributes = client.getAttributes() != null
                ? new HashMap<>(client.getAttributes())
                : new HashMap<>();
        attributes.put("post.logout.redirect.uris", postLogoutRedirects);
        client.setAttributes(attributes);

        adminClient.realm(KEYCLOAK_REALM)
                .clients()
                .get(client.getId())
                .update(client);

        System.out.println("=== Updated Keycloak redirect URI to: " + redirectUri + " ===");
        System.out.println("=== Updated Keycloak post-logout redirect URI to: " + postLogoutRedirects + " ===");
    }
}
