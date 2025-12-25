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
import de.bushnaq.abdalla.kassandra.ai.narrator.TtsCacheManager;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.repository.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
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
 * configures Spring Security properties to interact with this Keycloak instance.
 *
 * @author Abdalla Bushnaq
 */
@Testcontainers
public class AbstractKeycloakUiTestUtil extends AbstractUiTestUtil {
    private static final Logger                     logger   = LoggerFactory.getLogger(AbstractKeycloakUiTestUtil.class);
    protected static     int                        allocatedPort;
    private static       KeycloakContainer          keycloakInstance;
    private static final KeycloakContainer          keycloak = getKeycloakContainer();// Start Keycloak container with realm configuration
    @Autowired
    protected            PlatformTransactionManager transactionManager;
    @Autowired
    protected            UserRepository             userRepository;

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
            keycloakInstance = new KeycloakContainer("quay.io/keycloak/keycloak:24.0.1")
                    .withRealmImportFile("keycloak/project-hub-realm.json")
                    .withAdminUsername("admin")
                    .withAdminPassword("admin")
                    .withExposedPorts(8080, 8443)
                    .withLogConsumer(outputFrame -> System.out.println("Keycloak: " + outputFrame.getUtf8String()))
                    .withEnv("KC_HOSTNAME_STRICT", "false")
                    .withEnv("KC_HOSTNAME_STRICT_HTTPS", "false")
                    .withReuse(true); // Enable container reuse

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

    // Configure Spring Security to use the Keycloak container
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

        // Get the actual URL that's accessible from outside the container
        String externalUrl = getPublicFacingUrl(keycloak);
        System.out.println("Keycloak External URL: " + externalUrl);

        // Log all container environment information for debugging
        System.out.println("Keycloak Container:");
        System.out.println("  Auth Server URL: " + keycloak.getAuthServerUrl());
        System.out.println("  Container IP: " + keycloak.getHost());
        System.out.println("  HTTP Port Mapping: " + keycloak.getMappedPort(8080));
        System.out.println("  HTTPS Port Mapping: " + keycloak.getMappedPort(8443));

        // Override the authServerUrl with our public-facing URL
        String publicAuthServerUrl = externalUrl + "/";

        // Create properties with the public URL
        Map<String, String> props = new HashMap<>();
        props.put("spring.security.oauth2.client.provider.keycloak.issuer-uri", publicAuthServerUrl + "realms/project-hub-realm");
        props.put("spring.security.oauth2.client.provider.keycloak.authorization-uri", publicAuthServerUrl + "realms/project-hub-realm/protocol/openid-connect/auth");
        props.put("spring.security.oauth2.client.provider.keycloak.token-uri", publicAuthServerUrl + "realms/project-hub-realm/protocol/openid-connect/token");
        props.put("spring.security.oauth2.client.provider.keycloak.user-info-uri", publicAuthServerUrl + "realms/project-hub-realm/protocol/openid-connect/userinfo");
        props.put("spring.security.oauth2.client.provider.keycloak.jwk-set-uri", publicAuthServerUrl + "realms/project-hub-realm/protocol/openid-connect/certs");
        props.put("spring.security.oauth2.client.registration.keycloak.client-id", "kassandra-client");
        props.put("spring.security.oauth2.client.registration.keycloak.client-secret", "test-client-secret");
        props.put("spring.security.oauth2.client.registration.keycloak.scope", "openid,profile,email");
        props.put("spring.security.oauth2.client.registration.keycloak.authorization-grant-type", "authorization_code");
        props.put("spring.security.oauth2.client.registration.keycloak.redirect-uri", "{baseUrl}/login/oauth2/code/{registrationId}");

        props.put("spring.security.oauth2.resourceserver.jwt.issuer-uri", publicAuthServerUrl + "realms/project-hub-realm");

        // Register all properties
        props.forEach((key, value) -> registry.add(key, () -> value));
    }

    /**
     * Ensure the test user exists in the database with ADMIN role before each test.
     * This is critical because OIDC authentication will look up the user by email,
     * and if not found, assigns only USER role by default.
     * <p>
     * Uses explicit transaction management to ensure the user is committed
     * and visible to all subsequent transactions including API calls.
     */
    @BeforeEach
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void setupTestUser() {
        String testUserEmail = "christopher.paul@kassandra.org";
//        var existingUser = userRepository.findByEmail(testUserEmail);
//        if (existingUser.isEmpty())
//        {
        LocalDate firstDate = ParameterOptions.getNow().toLocalDate().minusYears(2);
        User      saved     = addUser("Christopher Paul", testUserEmail, "ADMIN,USER", "de", "nw", firstDate, generateUserColor(userIndex), 0.5f);
        userIndex--;//ensure Christopher Paul is always the first user created
//        } else {
//            // Ensure existing user has ADMIN role
//            UserDAO user = existingUser.get();
//            if (!user.hasRole("ADMIN")) {
//                user.addRole("ADMIN");
//                userRepository.save(user);
//                logger.info("Added ADMIN role to existing test user: {}", testUserEmail);
//            } else {
//                logger.info("Test user already exists with ADMIN role: {}", testUserEmail);
//            }
//        }
    }

    @AfterAll
    public static void tearDown() {
        if (TtsCacheManager.getCacheMiss() != 0) {
            logger.warn("*** TTS CACHE MISSES: {} ***", TtsCacheManager.getCacheMiss());
        }
    }

    /**
     * Update Keycloak client redirect URI to match the allocated random port.
     *
     * @param port
     */
    private static void updateKeycloakClientRedirectUri(int port) {
        String redirectUri = "http://localhost:" + port + "/login/oauth2/code/*";
        String webOrigin   = "http://localhost:" + port;

        // Use Keycloak Admin API to update client
        var adminClient = org.keycloak.admin.client.KeycloakBuilder.builder()
                .serverUrl(keycloak.getAuthServerUrl())
                .realm("master")
                .username("admin")
                .password("admin")
                .clientId("admin-cli")
                .build();

        var client = adminClient.realm("project-hub-realm")
                .clients()
                .findByClientId("kassandra-client")
                .get(0);

        client.setRedirectUris(List.of(redirectUri));
        client.setWebOrigins(List.of(webOrigin));

        adminClient.realm("project-hub-realm")
                .clients()
                .get(client.getId())
                .update(client);

        System.out.println("=== Updated Keycloak redirect URI to: " + redirectUri + " ===");
    }
}
