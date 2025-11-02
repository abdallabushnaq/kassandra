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

package de.bushnaq.abdalla.projecthub.ui;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.bushnaq.abdalla.projecthub.ai.narrator.Narrator;
import de.bushnaq.abdalla.projecthub.ai.narrator.NarratorAttribute;
import de.bushnaq.abdalla.projecthub.ui.dialog.UserDialog;
import de.bushnaq.abdalla.projecthub.ui.util.AbstractUiTestUtil;
import de.bushnaq.abdalla.projecthub.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.projecthub.ui.view.LoginView;
import de.bushnaq.abdalla.projecthub.ui.view.UserListView;
import de.bushnaq.abdalla.projecthub.ui.view.util.ProductListViewTester;
import de.bushnaq.abdalla.projecthub.ui.view.util.UserListViewTester;
import de.bushnaq.abdalla.projecthub.util.RandomCase;
import de.bushnaq.abdalla.projecthub.util.TestInfoUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=8080",
                "spring.profiles.active=test",
                // Disable basic authentication for these tests
                "spring.security.basic.enabled=false"
        }
)
@AutoConfigureMockMvc
@Transactional
@Testcontainers
@Disabled
public class UsersInstructionVideo extends AbstractUiTestUtil {
    public static final  NarratorAttribute        INTENSE  = new NarratorAttribute().withExaggeration(.7f).withCfgWeight(.3f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    public static final  NarratorAttribute        NORMAL   = new NarratorAttribute().withExaggeration(.5f).withCfgWeight(.5f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    // Start Keycloak container with realm configuration
    @Container
    private static final KeycloakContainer        keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:24.0.1")
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
    @Autowired
    private              ProductListViewTester    productListViewTester;
    @Autowired
    private              HumanizedSeleniumHandler seleniumHandler;
    @Autowired
    private              UserListViewTester       userListViewTester;

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void createVideo(RandomCase randomCase, TestInfo testInfo) throws Exception {

        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        generateProductsIfNeeded(testInfo, randomCase);
        Narrator paul = Narrator.withChatterboxTTS("tts/" + testInfo.getTestClass().get().getSimpleName());
        HumanizedSeleniumHandler.setHumanize(true);
        seleniumHandler.getAndCheck("http://localhost:" + "8080" + "/ui/" + LoginView.ROUTE);
        seleniumHandler.showOverlay("Kassandra Users", "Introduction Video");
        seleniumHandler.startRecording(testInfo.getTestClass().get().getSimpleName(), "Users Introduction Video");
        seleniumHandler.wait(3000);
        paul.narrateAsync(NORMAL, "Hi everyone, Christopher Paul here from kassandra.org. Today we're going to learn about User Management in Kassandra. As an administrator, the Users page is where you add team members to the system so they can access Kassandra and be assigned to projects.");
        seleniumHandler.hideOverlay();
        productListViewTester.switchToProductListViewWithOidc("christopher.paul@kassandra.org", "password", "../kassandra.wiki/screenshots/login-view.png", testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));

        //---------------------------------------------------------------------------------------
        // Navigate to Users Page
        //---------------------------------------------------------------------------------------

        seleniumHandler.setHighlightEnabled(true);//highlight elements starting now
        paul.narrateAsync(NORMAL, "Let's navigate to the Users page from the main menu.");
        seleniumHandler.click(MainLayout.ID_TAB_USERS);

        //---------------------------------------------------------------------------------------
        // Explain User List Page Purpose
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(UserListView.USER_LIST_PAGE_TITLE);
        paul.narrate(NORMAL, "This is the Users page. Here you can see all team members who have access to Kassandra.");

        //---------------------------------------------------------------------------------------
        // Explain Grid Columns
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(UserListView.USER_GRID);
        paul.narrate(NORMAL, "The grid shows use information.");

        //---------------------------------------------------------------------------------------
        // Explain Employment Dates
        //---------------------------------------------------------------------------------------

        //---------------------------------------------------------------------------------------
        // Create New User - Scenario Introduction
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "Let me show you how to add a new team member. We just hired a new developer named Sarah Johnson who needs access to Kassandra.");

        //---------------------------------------------------------------------------------------
        // Open Create User Dialog
        //---------------------------------------------------------------------------------------

        paul.narrateAsync(NORMAL, "Let's click the Create button to open the user dialog.");
        seleniumHandler.click(UserListView.CREATE_USER_BUTTON);

        //---------------------------------------------------------------------------------------
        // Fill User Name
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "First, we enter the user's full name. This is how they'll appear in task assignments and reports throughout Kassandra.");
        final String userName = "Sarah Johnson";
        seleniumHandler.setTextField(UserDialog.USER_NAME_FIELD, userName);

        //---------------------------------------------------------------------------------------
        // Fill User Email
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "Next, their email address. This is the most important field - it's the unique identifier Sarah will use to log into Kassandra. Make sure it matches their authentication email exactly.");
        final String userEmail = "sarah.johnson@kassandra.org";
        seleniumHandler.setTextField(UserDialog.USER_EMAIL_FIELD, userEmail);

        //---------------------------------------------------------------------------------------
        // Set First Working Day
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "The First Working Day is the date when Sarah will start working and can be assigned tasks. Let's set it to July first, twenty twenty-five.");
        final LocalDate firstWorkingDay = LocalDate.of(2025, 7, 1);
        seleniumHandler.setDatePickerValue(UserDialog.USER_FIRST_WORKING_DAY_PICKER, firstWorkingDay);

        //---------------------------------------------------------------------------------------
        // Explain Last Working Day
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(UserDialog.USER_LAST_WORKING_DAY_PICKER);
        paul.narrate(NORMAL, "The Last Working Day is optional and only used when someone leaves the company. We'll leave it empty for Sarah.");

        //---------------------------------------------------------------------------------------
        // Save User
        //---------------------------------------------------------------------------------------

        paul.narrateAsync(NORMAL, "Now let's save to create Sarah's user account.");
        seleniumHandler.click(UserDialog.CONFIRM_BUTTON);

        //---------------------------------------------------------------------------------------
        // Verify Creation
        //---------------------------------------------------------------------------------------

        seleniumHandler.wait(1000);
        seleniumHandler.highlight(UserListView.USER_GRID_NAME_PREFIX + userName);
        paul.narrate(NORMAL, "Perfect! Sarah Johnson now appears in our user list. She can now log into Kassandra using her email address and be assigned to project tasks.");

        //---------------------------------------------------------------------------------------
        // Explain Edit/Delete Capabilities
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(
                UserListView.USER_GRID_EDIT_BUTTON_PREFIX + userName,
                UserListView.USER_GRID_DELETE_BUTTON_PREFIX + userName
        );
        paul.narrate(NORMAL, "You can edit any user's information using the notepad icon on the right, or remove them from the system using the trashcan icon. However, be careful when deleting users who have been assigned tasks, as this may affect your project history and reporting.");

        //---------------------------------------------------------------------------------------
        // Explain Row Counter
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(UserListView.USER_ROW_COUNTER);
        paul.narrate(NORMAL, "The counter at the top shows you how many users currently have access to the system.");

        //---------------------------------------------------------------------------------------
        // Explain Global Filter
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(UserListView.USER_GLOBAL_FILTER);
        paul.narrate(NORMAL, "As your team grows, you can use the search filter to quickly find specific users by typing their name or email address.");

        //---------------------------------------------------------------------------------------
        // Closing
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "That's all there is to managing users in Kassandra. Remember, add new team members before they need to log in, and make sure to use their correct authentication email address. Thanks for watching!");

        seleniumHandler.waitUntilBrowserClosed(5000);
    }

    // Method to get the public-facing URL, fixing potential redirect issues
    private static String getPublicFacingUrl(KeycloakContainer container) {
        return String.format("http://%s:%s",
                container.getHost(),
                container.getMappedPort(8080));
    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                new RandomCase(1, LocalDate.parse("2025-05-01"), Duration.ofDays(10), 0, 0, 0, 0, 0, 6, 8, 12, 6, 13)//
        };
        return Arrays.stream(randomCases).toList();
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
