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

package de.bushnaq.abdalla.kassandra.ui.introduction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.bushnaq.abdalla.kassandra.ai.tts.narrator.Narrator;
import de.bushnaq.abdalla.kassandra.ai.tts.narrator.NarratorAttribute;
import de.bushnaq.abdalla.kassandra.dao.SecurityConfigurationDAO;
import de.bushnaq.abdalla.kassandra.repository.OidcIdentityRepository;
import de.bushnaq.abdalla.kassandra.service.SecurityConfigurationService;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.dialog.UserDialog;
import de.bushnaq.abdalla.kassandra.ui.introduction.util.InstructionVideo;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.AboutView;
import de.bushnaq.abdalla.kassandra.ui.view.LoginView;
import de.bushnaq.abdalla.kassandra.ui.view.SetupView;
import de.bushnaq.abdalla.kassandra.ui.view.UserListView;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tutorial and Selenium integration test for securely setting up a new Kassandra installation.
 * <p>
 * Unlike the usual OIDC UI tests, this test deliberately does not extend
 * {@code AbstractKeycloakUiTestUtil}: the application must begin without an OIDC provider so the
 * public first-run screen remains visible. It creates its Keycloak fixture independently and only
 * uses it after the setup wizard has accepted the provider configuration.
 */
@Tag("IntroductionVideo")
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=${test.server.port}",
                "KASSANDRA_SETUP_TOKEN=first-run-setup-token",
                "spring.security.basic.enabled=false"
        }
)
@AutoConfigureMockMvc
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class InitialSetupIntroductionVideo15IT extends AbstractUiTestUtil {

    private static final String                       EXAMPLE_USER_EMAIL     = "grace.martin@kassandra.org";
    private static final String                       EXAMPLE_USER_NAME      = "Grace Martin";
    private static final KeycloakContainer            KEYCLOAK               = createKeycloak();
    private static final String                       KEYCLOAK_CLIENT_ID     = "kassandra-client";
    private static final String                       KEYCLOAK_CLIENT_SECRET = "test-client-secret";
    private static final String                       KEYCLOAK_PASSWORD      = "Kassandra-Green-Atlas-73!";
    private static final String                       KEYCLOAK_REALM         = "project-hub-realm";
    private static final NarratorAttribute            NORMAL                 = new NarratorAttribute()
            .withExaggeration(.5f).withCfgWeight(.5f).withTemperature(1f);
    private static final String                       RECOVERY_PASSWORD      = "recovery-password";
    private static final int                          SERVER_PORT            = allocateServerPort();
    private static final String                       SETUP_TOKEN            = "first-run-setup-token";
    private static final String                       TEST_USER_EMAIL        = "christopher.paul@kassandra.org";
    private static final InstructionVideo             VIDEO                  = new InstructionVideo();
    @Autowired
    private              OidcIdentityRepository       oidcIdentityRepository;
    @Autowired
    private              SecurityConfigurationService securityConfigurationService;
    @Autowired
    private              HumanizedSeleniumHandler     seleniumHandler;

    private static int allocateServerPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to allocate the first-run setup test server port", e);
        }
    }

    private void authenticateWithKeycloak(String username, boolean restartExistingKeycloakLogin) {
        seleniumHandler.waitForElementToBeClickable("kc-login");
        if (restartExistingKeycloakLogin && seleniumHandler.isElementPresent(By.id("kc-attempted-username"))) {
            seleniumHandler.click("reset-login");
            seleniumHandler.waitForElementToBeClickable("username");
        }
        WebElement usernameField = seleniumHandler.findElement(By.id("username"));
        WebElement password      = seleniumHandler.findElement(By.id("password"));
        seleniumHandler.typeIntoElement(usernameField, username);
        seleniumHandler.typeIntoElement(password, KEYCLOAK_PASSWORD);
        seleniumHandler.clickElement(seleniumHandler.findElement(By.id("kc-login")));
    }

    private static void configureKeycloakRedirectUri() {
        var adminClient = org.keycloak.admin.client.KeycloakBuilder.builder()
                .serverUrl(KEYCLOAK.getAuthServerUrl())
                .realm("master")
                .username("admin")
                .password("admin")
                .clientId("admin-cli")
                .build();
        try {
            var client = adminClient.realm(KEYCLOAK_REALM).clients().findByClientId(KEYCLOAK_CLIENT_ID).getFirst();
            client.setRedirectUris(List.of("http://localhost:" + SERVER_PORT + "/login/oauth2/code/*"));
            client.setWebOrigins(List.of("http://localhost:" + SERVER_PORT));
            Map<String, String> attributes = client.getAttributes() == null
                    ? new HashMap<>()
                    : new HashMap<>(client.getAttributes());
            attributes.put("post.logout.redirect.uris", "http://localhost:" + SERVER_PORT + "/*");
            client.setAttributes(attributes);
            adminClient.realm(KEYCLOAK_REALM).clients().get(client.getId()).update(client);
            resetPassword(adminClient, TEST_USER_EMAIL);
            createExampleIdentity(adminClient);
        } finally {
            adminClient.close();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (!KEYCLOAK.isRunning()) {
            KEYCLOAK.start();
            configureKeycloakRedirectUri();
        }
        registry.add("test.server.port", () -> SERVER_PORT);
    }

    /**
     * Configures the shared metadata used when this test is run as a visible tutorial video.
     */
    @BeforeAll
    static void configureVideo() {
        VIDEO.setVersion(1);
        VIDEO.setTitle("15 Initial Kassandra Setup");
        VIDEO.setDescription(
                "A guided first-run setup of Kassandra: protecting the installation with a deployment token, "
                        + "creating the recovery account, configuring OpenID Connect, claiming the first administrator, "
                        + "and onboarding another user through their normal first sign-in.");
    }

    private static void createExampleIdentity(org.keycloak.admin.client.Keycloak adminClient) {
        var    users         = adminClient.realm(KEYCLOAK_REALM).users();
        var    existingUsers = users.searchByUsername(EXAMPLE_USER_EMAIL, true);
        String userId;
        if (existingUsers.isEmpty()) {
            var user = new org.keycloak.representations.idm.UserRepresentation();
            user.setUsername(EXAMPLE_USER_EMAIL);
            user.setEmail(EXAMPLE_USER_EMAIL);
            user.setFirstName("Alex");
            user.setLastName("Morgan");
            user.setEnabled(true);
            try (var response = users.create(user)) {
                userId = org.keycloak.admin.client.CreatedResponseUtil.getCreatedId(response);
            }
        } else {
            userId = existingUsers.getFirst().getId();
        }
        resetPassword(users, userId);
    }

    private void createExampleUser(Narrator narrator) throws Exception {
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_MANAGE_USERS);
        seleniumHandler.waitForElementToBeClickable(UserListView.CREATE_USER_BUTTON);
        narrate(narrator,
                "For this example, Grace Martin already exists in Keycloak. As an administrator, I will create Grace's Kassandra user record. The email address must exactly match Grace's email address in Keycloak; it is how the first sign-in finds the intended user.");
        seleniumHandler.click(UserListView.CREATE_USER_BUTTON);
        seleniumHandler.setTextField(UserDialog.USER_NAME_FIELD, EXAMPLE_USER_NAME);
        seleniumHandler.setTextField(UserDialog.USER_EMAIL_FIELD, EXAMPLE_USER_EMAIL);
        seleniumHandler.setDatePickerValue(UserDialog.USER_FIRST_WORKING_DAY_PICKER, LocalDate.now());
        seleniumHandler.setComboBoxValue(UserDialog.USER_LOCATION_COUNTRY_COMBO, "United States (US)");
        seleniumHandler.setComboBoxValue(UserDialog.USER_LOCATION_STATE_COMBO, "California (ca)");
        seleniumHandler.setComboBoxValue(UserDialog.USER_WORK_WEEK_COMBO, "Western 5x8");
        seleniumHandler.click(UserDialog.CONFIRM_BUTTON);
        seleniumHandler.ensureIsInList(UserListView.USER_GRID_NAME_PREFIX, EXAMPLE_USER_NAME);
        narrate(narrator,
                "Grace is now a Kassandra user with the standard User role. Creating this record alone does not grant a login session; Grace must complete her own normal provider sign-in.");
    }

    private static KeycloakContainer createKeycloak() {
        return new KeycloakContainer("quay.io/keycloak/keycloak:26.5.6")
                .withRealmImportFile("keycloak/project-hub-realm-realm.json")
                .withAdminUsername("admin")
                .withAdminPassword("admin")
                .withEnv("KC_HOSTNAME_STRICT", "false")
                .withEnv("KC_HOSTNAME_STRICT_HTTPS", "false")
                .withReuse(true);
    }

    /**
     * Starts a clean Kassandra server and uses the browser to complete its protected first-run setup.
     * In headless mode this verifies the same flow without narration or video output.
     *
     * @param testInfo JUnit metadata used to name the optional recording
     * @throws Exception when a Selenium or OIDC interaction fails
     */
    @Test
    public void createVideo(TestInfo testInfo) throws Exception {
        boolean headless = HumanizedSeleniumHandler.isSeleniumHeadless();
        seleniumHandler.setWindowSize(InstructionVideo.VIDEO_WIDTH, InstructionVideo.VIDEO_HEIGHT);
        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().orElseThrow().getName());
        setTestCaseName(getClass().getName(), testInfo.getTestMethod().orElseThrow().getName());

        Narrator narrator = headless ? null : Narrator.withChatterboxTTS("tts/" + getClass().getSimpleName());
        Narrator grace    = headless ? null : Narrator.withChatterboxTTS("tts/" + getClass().getSimpleName(), "grace");
        if (!headless) {
            narrator.setEnabled(true);
            grace.setEnabled(true);
            HumanizedSeleniumHandler.setHumanize(true);
            seleniumHandler.showOverlay(VIDEO.getTitle(), InstructionVideo.VIDEO_SUBTITLE);
            startRecording();
            narrator.narrateAsync(NORMAL, "Welcome to Kassandra. On a new installation, no one can sign in until an administrator completes this protected setup.");
            seleniumHandler.hideOverlay();
        }

        seleniumHandler.getAndCheck("http://localhost:" + SERVER_PORT + "/ui/" + LoginView.ROUTE);
        seleniumHandler.waitForElementToBeClickable(LoginView.SETUP_LINK);
        narrate(narrator, "There is no configured identity provider yet, so Kassandra offers the setup wizard instead of a sign-in button.");
        seleniumHandler.click(LoginView.SETUP_LINK);

        seleniumHandler.waitForElementToBeInteractable(SetupView.SETUP_TOKEN_FIELD);
        narrate(narrator,
                "The deployment setup token protects this one-time operation. Before starting Kassandra, your deployment administrator generates a long random secret and supplies it in the KASSANDRA SETUP TOKEN environment variable or deployment secret. It is not created in the Kassandra user interface.");
        narrate(narrator,
                "Enter that deployment secret here, then create a strong recovery password. The recovery account is restricted to setup, and lets you repair the sign-in provider configuration later.");
        seleniumHandler.setTextField(SetupView.SETUP_TOKEN_FIELD, SETUP_TOKEN);
        seleniumHandler.setTextField(SetupView.RECOVERY_PASSWORD_FIELD, RECOVERY_PASSWORD);
        seleniumHandler.setTextField(SetupView.RECOVERY_PASSWORD_CONFIRMATION_FIELD, RECOVERY_PASSWORD);
        seleniumHandler.click(SetupView.CLAIM_SETUP_BUTTON);

        seleniumHandler.waitForElementToBeInteractable(SetupView.PROVIDER_NAME_FIELD);
        assertEquals(SecurityConfigurationDAO.SetupState.SETUP_IN_PROGRESS,
                securityConfigurationService.getConfiguration().getSetupState());
        narrate(narrator,
                "Now configure the organisation's OpenID Connect provider. Kassandra validates its discovery document before enabling sign-in. The provider name is only the label users see on Kassandra's sign-in page. Choose a clear name, such as Company Keycloak.");
        narrate(narrator,
                "For Keycloak, select the realm you want to use. Its issuer URI is the realm address, which you can copy from Realm settings, General, OpenID Endpoint Configuration. Use the issuer value from that document, not the administrator console URL.");
        seleniumHandler.setTextField(SetupView.PROVIDER_NAME_FIELD, "Kassandra test identity provider");
        seleniumHandler.setTextField(SetupView.PROVIDER_ISSUER_URI_FIELD, issuerUri());
        narrate(narrator,
                "Create an OpenID Connect client for Kassandra in that realm. Copy its Client ID from the client settings, then copy the client secret from the Credentials tab. Keep the secret in your password manager or deployment secret store.");
        seleniumHandler.setTextField(SetupView.PROVIDER_CLIENT_ID_FIELD, KEYCLOAK_CLIENT_ID);
        seleniumHandler.setTextField(SetupView.PROVIDER_CLIENT_SECRET_FIELD, KEYCLOAK_CLIENT_SECRET);
        narrate(narrator,
                "The default scopes request the OpenID Connect identity, profile, and email claims. OpenID is required; profile and email let Kassandra create the first administrator with a useful name and email address. Leave these defaults unless your provider requires different scopes.");
        seleniumHandler.click(SetupView.VALIDATE_PROVIDER_BUTTON);

        seleniumHandler.waitForElementToBeClickable(LoginView.OIDC_LOGIN_BUTTON);
        narrate(narrator, "The provider is ready. The first successful browser sign-in claims the initial Kassandra administrator account.");
        loginWithKeycloak(TEST_USER_EMAIL, false);
        seleniumHandler.waitForElementToBeClickable(AboutView.ABOUT_PAGE_TITLE);

        assertEquals(SecurityConfigurationDAO.SetupState.READY, securityConfigurationService.getConfiguration().getSetupState());
        narrate(narrator,
                "Setup is complete and the first administrator can now sign in. To onboard additional people, administrators create their Kassandra user records.");
        createExampleUser(narrator);
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_LOGOUT);
        seleniumHandler.waitForElementToBeClickable(LoginView.OIDC_LOGIN_BUTTON);
        narrate(narrator,
                "Grace Martin is now ready to use Kassandra. I will sign out and hand the session over to Grace, who will sign in through the identity provider using her own credentials.");
        if (grace != null) {
            grace.narrateAsync(NORMAL,
                    "Hi, Grace Martin here. My Kassandra administrator has already created my user record, so I can now choose our identity provider and sign in with my own account.");
        }
        loginWithKeycloak(EXAMPLE_USER_EMAIL, true);
        seleniumHandler.waitForElementToBeClickable(AboutView.ABOUT_PAGE_TITLE);
        assertEquals(2, oidcIdentityRepository.count());
        narrate(grace,
                "I now have access to Kassandra. On this first sign-in, Kassandra matched my email address to my pre-created user record and securely stored my identity provider account.");
        narrate(grace,
                "From now on, Kassandra recognises me by my identity provider's immutable subject, even if my display name changes. An identity with no matching Kassandra user remains denied.");
    }

    private String issuerUri() {
        return "http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080) + "/realms/" + KEYCLOAK_REALM;
    }

    private void loginWithKeycloak(String username, boolean restartExistingKeycloakLogin) {
        seleniumHandler.click(LoginView.OIDC_LOGIN_BUTTON);
        authenticateWithKeycloak(username, restartExistingKeycloakLogin);
    }

    private void narrate(Narrator narrator, String text) throws Exception {
        if (narrator != null) {
            narrator.narrate(NORMAL, text).pause();
        }
    }

    private static void resetPassword(org.keycloak.admin.client.Keycloak adminClient, String username) {
        var user = adminClient.realm(KEYCLOAK_REALM).users().searchByUsername(username, true).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Keycloak test user does not exist: " + username));
        resetPassword(adminClient.realm(KEYCLOAK_REALM).users(), user.getId());
    }

    private static void resetPassword(org.keycloak.admin.client.resource.UsersResource users, String userId) {
        var password = new org.keycloak.representations.idm.CredentialRepresentation();
        password.setType(org.keycloak.representations.idm.CredentialRepresentation.PASSWORD);
        password.setValue(KEYCLOAK_PASSWORD);
        password.setTemporary(false);
        users.get(userId).resetPassword(password);
    }

    private void startRecording() throws IOException {
        String videoName = "15-initial-kassandra-setup-" + VIDEO.getVersion();
        seleniumHandler.startRecording(InstructionVideo.TARGET_FOLDER, videoName);
        Path sidecarPath = Paths.get("test-recordings", InstructionVideo.TARGET_FOLDER, videoName + ".json");
        Files.createDirectories(sidecarPath.getParent());
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("title", VIDEO.getTitle());
        metadata.put("description", VIDEO.getDescription());
        metadata.put("tags", VIDEO.getTags());
        metadata.put("categoryId", VIDEO.getCategoryId());
        metadata.put("privacyStatus", VIDEO.getPrivacyStatus());
        metadata.put("playlistId", VIDEO.getPlaylistId());
        new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(sidecarPath.toFile(), metadata);
    }
}
