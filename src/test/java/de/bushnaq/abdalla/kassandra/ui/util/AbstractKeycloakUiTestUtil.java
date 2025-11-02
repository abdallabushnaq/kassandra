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
import de.bushnaq.abdalla.kassandra.ai.narrator.TtsCacheManager;
import org.junit.jupiter.api.AfterAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
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
    private static final Logger            logger   = LoggerFactory.getLogger(AbstractKeycloakUiTestUtil.class);
    private static       KeycloakContainer keycloakInstance;
    // Start Keycloak container with realm configuration
    private static final KeycloakContainer keycloak = getKeycloakContainer();

    private static synchronized KeycloakContainer getKeycloakContainer() {
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
        props.put("spring.security.oauth2.client.registration.keycloak.client-id", "project-hub-client");
        props.put("spring.security.oauth2.client.registration.keycloak.client-secret", "test-client-secret");
        props.put("spring.security.oauth2.client.registration.keycloak.scope", "openid,profile,email");
        props.put("spring.security.oauth2.client.registration.keycloak.authorization-grant-type", "authorization_code");
        props.put("spring.security.oauth2.client.registration.keycloak.redirect-uri", "{baseUrl}/login/oauth2/code/{registrationId}");

        props.put("spring.security.oauth2.resourceserver.jwt.issuer-uri", publicAuthServerUrl + "realms/project-hub-realm");

        // Register all properties
        props.forEach((key, value) -> registry.add(key, () -> value));
    }

    @AfterAll
    public static void tearDown() {
        if (TtsCacheManager.getCacheMiss() != 0) {
            logger.warn("*** TTS CACHE MISSES: {} ***", TtsCacheManager.getCacheMiss());
        }
    }
}
