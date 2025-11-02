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

package de.bushnaq.abdalla.kassandra.security;

import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Keycloak test container configuration for OIDC integration testing.
 * This class sets up a Keycloak instance in a Docker container for testing purposes.
 */
//@TestConfiguration
//@Lazy
//TODO remove
public class KeycloakTestContainer {

    private static final String KEYCLOAK_VERSION    = "24.0.1";
    private static final String TEST_ADMIN_PASSWORD = "admin";
    private static final String TEST_ADMIN_USERNAME = "admin";
    private static final String TEST_CLIENT_ID      = "project-hub-client";
    private static final String TEST_CLIENT_SECRET  = "test-client-secret";
    private static final String TEST_PASSWORD       = "password";
    private static final String TEST_REALM          = "project-hub-realm";
    private static final String TEST_USER           = "testuser";
    @LocalServerPort
    private static       int    port;

    /**
     * Creates a test realm configuration dynamically if you don't want to use the JSON import method.
     * This is an alternative approach to using a realm import file.
     */
//    public static void configureKeycloakRealmProgrammatically(KeycloakContainer keycloak) {
//        try (var admin = keycloak.getKeycloakAdminClient()) {
//            // 1. Create realm
//            var realm = admin.realms().findAll().stream()
//                    .filter(r -> TEST_REALM.equals(r.getRealm()))
//                    .findFirst()
//                    .orElse(null);
//
//            if (realm == null) {
//                realm = new org.keycloak.representations.idm.RealmRepresentation();
//                realm.setRealm(TEST_REALM);
//                realm.setEnabled(true);
//                realm.setDisplayName("Project Hub Test Realm");
//                admin.realms().create(realm);
//            }
//
//            // 2. Create client
//            var client = new org.keycloak.representations.idm.ClientRepresentation();
//            client.setClientId(TEST_CLIENT_ID);
//            client.setEnabled(true);
//            client.setRedirectUris(java.util.List.of("http://localhost:" + port + "/*"));
//            client.setClientAuthenticatorType("client-secret");
//            client.setSecret(TEST_CLIENT_SECRET);
//            client.setProtocol("openid-connect");
//            client.setDirectAccessGrantsEnabled(true);
//            client.setStandardFlowEnabled(true);
//            client.setImplicitFlowEnabled(false);
//            client.setPublicClient(false);
//
//            admin.realm(TEST_REALM).clients().create(client);
//
//            // 3. Create test user
//            var user = new org.keycloak.representations.idm.UserRepresentation();
//            user.setUsername(TEST_USER);
//            user.setEnabled(true);
//            user.setEmail(TEST_USER + "@example.com");
//            user.setFirstName("Test");
//            user.setLastName("User");
//            user.setEmailVerified(true);
//
//            var userId = admin.realm(TEST_REALM).users().create(user).getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
//
//            // 4. Set user password
//            var credentials = new org.keycloak.representations.idm.CredentialRepresentation();
//            credentials.setTemporary(false);
//            credentials.setType("password");
//            credentials.setValue(TEST_PASSWORD);
//            admin.realm(TEST_REALM).users().get(userId).resetPassword(credentials);
//
//            // 5. Assign roles
//            var role = admin.realm(TEST_REALM).roles().get("offline_access").toRepresentation();
//            admin.realm(TEST_REALM).users().get(userId).roles().realmLevel().add(java.util.List.of(role));
//        }
//    }

    /**
     * Get OAuth2 properties for Spring Security configuration based on the running Keycloak container
     */
//    public static Map<String, String> getOAuth2Properties(KeycloakContainer keycloak) {
//        // Ensure the auth server URL ends with a slash
//        String authServerUrl = keycloak.getAuthServerUrl();
//        if (!authServerUrl.endsWith("/")) {
//            authServerUrl = authServerUrl + "/";
//        }
//
//        String realm = TEST_REALM;
//
//        // Use a mutable map since we have more than 10 entries (Map.of is limited to 10)
//        Map<String, String> props = new HashMap<>();
//
//        // Provider configuration
//        props.put("spring.security.oauth2.client.provider.keycloak.issuer-uri", authServerUrl + "realms/" + realm);
//        props.put("spring.security.oauth2.client.provider.keycloak.authorization-uri", authServerUrl + "realms/" + realm + "/protocol/openid-connect/auth");
//        props.put("spring.security.oauth2.client.provider.keycloak.token-uri", authServerUrl + "realms/" + realm + "/protocol/openid-connect/token");
//        props.put("spring.security.oauth2.client.provider.keycloak.user-info-uri", authServerUrl + "realms/" + realm + "/protocol/openid-connect/userinfo");
//        props.put("spring.security.oauth2.client.provider.keycloak.jwk-set-uri", authServerUrl + "realms/" + realm + "/protocol/openid-connect/certs");
//
//        // Client registration
//        props.put("spring.security.oauth2.client.registration.keycloak.client-id", TEST_CLIENT_ID);
//        props.put("spring.security.oauth2.client.registration.keycloak.client-secret", TEST_CLIENT_SECRET);
//        props.put("spring.security.oauth2.client.registration.keycloak.scope", "openid,profile,email");
//        props.put("spring.security.oauth2.client.registration.keycloak.authorization-grant-type", "authorization_code");
//        props.put("spring.security.oauth2.client.registration.keycloak.redirect-uri", "{baseUrl}/login/oauth2/code/{registrationId}");
//
//        // Resource server for API endpoints
//        props.put("spring.security.oauth2.resourceserver.jwt.issuer-uri", authServerUrl + "realms/" + realm);
//
//        // For debugging
//        System.out.println("OIDC Configuration - Auth Server URL: " + authServerUrl);
//        System.out.println("OIDC Configuration - Issuer URI: " + props.get("spring.security.oauth2.client.provider.keycloak.issuer-uri"));
//
//        return props;
//    }
//
//    /**
//     * Get test user credentials for authentication
//     */
//    public static Map<String, String> getTestUserCredentials() {
//        return Map.of(
//                "username", TEST_USER,
//                "password", TEST_PASSWORD
//        );
//    }
//
//    @Bean
//    public KeycloakContainer keycloakContainer() {
//        KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:" + KEYCLOAK_VERSION)
//                .withRealmImportFile("keycloak/project-hub-realm.json")
//                .withAdminUsername(TEST_ADMIN_USERNAME)
//                .withAdminPassword(TEST_ADMIN_PASSWORD)
//                .withEnv("KEYCLOAK_LOGLEVEL", "INFO")
//                .withEnv("ROOT_LOGLEVEL", "INFO")
//                .waitingFor(Wait.forHttp("/").forStatusCode(200));
//
//        return keycloak;
//    }
}
