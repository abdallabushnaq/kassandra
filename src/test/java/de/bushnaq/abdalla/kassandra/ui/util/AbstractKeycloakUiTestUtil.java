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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract test utility class for UI tests that require Keycloak authentication.
 * It sets up a Keycloak container with a predefined realm configuration and
 * configures Spring Security properties to interact with this Keycloak instance.
 *
 * @author Abdalla Bushnaq
 */
public class AbstractKeycloakUiTestUtil extends AbstractUiTestUtil {
    // Start Keycloak container with realm configuration
    @Container
    private static final KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:24.0.1")
            .withRealmImportFile("keycloak/project-hub-realm.json")
            .withAdminUsername("admin")
            .withAdminPassword("admin")
            // Expose on a fixed port for more reliable testing
            .withExposedPorts(8080, 8443)
            // Add debugging to see container output
            .withLogConsumer(outputFrame -> System.out.println("Keycloak: " + outputFrame.getUtf8String()))
            // Make Keycloak accessible from outside the container
            .withEnv("KC_HOSTNAME_STRICT", "false")
            .withEnv("KC_HOSTNAME_STRICT_HTTPS", "false");

    // Method to get the public-facing URL, fixing potential redirect issues
    private static String getPublicFacingUrl(KeycloakContainer container) {
        return String.format("http://%s:%s",
                container.getHost(),
                container.getMappedPort(8080));
    }


    // Configure Spring Security to use the Keycloak container
    @DynamicPropertySource
    static void registerKeycloakProperties(DynamicPropertyRegistry registry) {
        // Start the container
        keycloak.start();

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
}
