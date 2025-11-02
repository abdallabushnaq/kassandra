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

package de.bushnaq.abdalla.kassandra.ui.introduction;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.bushnaq.abdalla.kassandra.ai.narrator.Narrator;
import de.bushnaq.abdalla.kassandra.ai.narrator.NarratorAttribute;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.introduction.util.InstructionVideosUtil;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.LoginView;
import de.bushnaq.abdalla.kassandra.ui.view.UserProfileView;
import de.bushnaq.abdalla.kassandra.ui.view.util.ProductListViewTester;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

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
public class UserProfileIntroductionVideoTest extends AbstractKeycloakUiTestUtil {
    public static final  NarratorAttribute        NORMAL      = new NarratorAttribute().withExaggeration(.5f).withCfgWeight(.5f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    public static final  String                   VIDEO_TITLE = "User Profile Introduction Video";
    // Start Keycloak container with realm configuration
    @Container
    private static final KeycloakContainer        keycloak    = new KeycloakContainer("quay.io/keycloak/keycloak:24.0.1")
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
        seleniumHandler.showOverlay("Kassandra User Profile", InstructionVideosUtil.VIDEO_SUBTITLE);
        seleniumHandler.startRecording(InstructionVideosUtil.TARGET_FOLDER, VIDEO_TITLE);
        seleniumHandler.wait(3000);
        paul.narrateAsync(NORMAL, "Hi everyone, Christopher Paul here from kassandra.org. Today we're going to learn about managing your user profile in Kassandra. Your profile contains important information about how you're identified in the system and displayed in reports.");
        seleniumHandler.hideOverlay();
        productListViewTester.switchToProductListViewWithOidc("christopher.paul@kassandra.org", "password", "../kassandra.wiki/screenshots/login-view.png", testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));

        //---------------------------------------------------------------------------------------
        // Navigate to Profile Page
        //---------------------------------------------------------------------------------------

        seleniumHandler.setHighlightEnabled(true);//highlight elements starting now
        paul.narrateAsync(NORMAL, "Let's open the user menu and navigate to the User Profile page.");
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_VIEW_PROFILE);

        //---------------------------------------------------------------------------------------
        // Explain Profile Page Purpose
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(UserProfileView.PROFILE_PAGE_TITLE);
        paul.narrate(NORMAL, "This is your personal profile page where you can edit your display name and personal color.");

        //---------------------------------------------------------------------------------------
        // Use Case - Name Change
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(UserProfileView.USER_NAME_FIELD);
        paul.narrate(NORMAL, "Let's say I recently married and decided to change my last name to my wife's. I'll update my display name to reflect this change. This is the name that will appear in task assignments, reports, and calendars.");
        seleniumHandler.setTextField(UserProfileView.USER_NAME_FIELD, "Christopher Wilson");

        //---------------------------------------------------------------------------------------
        // Use Case - Color Change
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(UserProfileView.USER_COLOR_PICKER);
        paul.narrate(NORMAL, "I also want to change my color. I'm working with another developer who is already using red, so let me pick a different color to better distinguish my tasks from hers.");
        seleniumHandler.setColorPickerValue(UserProfileView.USER_COLOR_PICKER, "#00FF00");
        seleniumHandler.wait(1000);

        //---------------------------------------------------------------------------------------
        // Explain Color Usage
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "This color is used throughout Kassandra in Gantt charts and resource utilization graphs, making it easy to identify who's working on what.");

        //---------------------------------------------------------------------------------------
        // Save Changes
        //---------------------------------------------------------------------------------------

        paul.narrateAsync(NORMAL, "Now let's save these changes.");
        seleniumHandler.click(UserProfileView.SAVE_PROFILE_BUTTON);

        //---------------------------------------------------------------------------------------
        // Verify Changes
        //---------------------------------------------------------------------------------------

        seleniumHandler.wait(1000);
        paul.narrate(NORMAL, "Perfect! The profile has been updated successfully. These changes are immediately reflected throughout the system.");

        //---------------------------------------------------------------------------------------
        // Closing
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "That's all there is to managing your profile in Kassandra. Keep your name up to date and choose a distinct color so your team members can easily identify your work. Thanks for watching!");

        seleniumHandler.waitUntilBrowserClosed(5000);
    }

//    // Method to get the public-facing URL, fixing potential redirect issues
//    private static String getPublicFacingUrl(KeycloakContainer container) {
//        return String.format("http://%s:%s",
//                container.getHost(),
//                container.getMappedPort(8080));
//    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                new RandomCase(1, LocalDate.parse("2025-05-01"), Duration.ofDays(10), 0, 0, 0, 0, 0, 6, 8, 12, 6, 13)//
        };
        return Arrays.stream(randomCases).toList();
    }

//    // Configure Spring Security to use the Keycloak container
//    @DynamicPropertySource
//    static void registerKeycloakProperties(DynamicPropertyRegistry registry) {
//        // Start the container
//        keycloak.start();
//
//        // Get the actual URL that's accessible from outside the container
//        String externalUrl = getPublicFacingUrl(keycloak);
//        System.out.println("Keycloak External URL: " + externalUrl);
//
//        // Log all container environment information for debugging
//        System.out.println("Keycloak Container:");
//        System.out.println("  Auth Server URL: " + keycloak.getAuthServerUrl());
//        System.out.println("  Container IP: " + keycloak.getHost());
//        System.out.println("  HTTP Port Mapping: " + keycloak.getMappedPort(8080));
//        System.out.println("  HTTPS Port Mapping: " + keycloak.getMappedPort(8443));
//
//        // Override the authServerUrl with our public-facing URL
//        String publicAuthServerUrl = externalUrl + "/";
//
//        // Create properties with the public URL
//        Map<String, String> props = new HashMap<>();
//        props.put("spring.security.oauth2.client.provider.keycloak.issuer-uri", publicAuthServerUrl + "realms/project-hub-realm");
//        props.put("spring.security.oauth2.client.provider.keycloak.authorization-uri", publicAuthServerUrl + "realms/project-hub-realm/protocol/openid-connect/auth");
//        props.put("spring.security.oauth2.client.provider.keycloak.token-uri", publicAuthServerUrl + "realms/project-hub-realm/protocol/openid-connect/token");
//        props.put("spring.security.oauth2.client.provider.keycloak.user-info-uri", publicAuthServerUrl + "realms/project-hub-realm/protocol/openid-connect/userinfo");
//        props.put("spring.security.oauth2.client.provider.keycloak.jwk-set-uri", publicAuthServerUrl + "realms/project-hub-realm/protocol/openid-connect/certs");
//        props.put("spring.security.oauth2.client.registration.keycloak.client-id", "project-hub-client");
//        props.put("spring.security.oauth2.client.registration.keycloak.client-secret", "test-client-secret");
//        props.put("spring.security.oauth2.client.registration.keycloak.scope", "openid,profile,email");
//        props.put("spring.security.oauth2.client.registration.keycloak.authorization-grant-type", "authorization_code");
//        props.put("spring.security.oauth2.client.registration.keycloak.redirect-uri", "{baseUrl}/login/oauth2/code/{registrationId}");
//
//        props.put("spring.security.oauth2.resourceserver.jwt.issuer-uri", publicAuthServerUrl + "realms/project-hub-realm");
//
//        // Register all properties
//        props.forEach((key, value) -> registry.add(key, () -> value));
//    }

}

