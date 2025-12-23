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

package de.bushnaq.abdalla.kassandra.ui.view;

import de.bushnaq.abdalla.kassandra.ui.util.AbstractUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.util.UserProfileViewTester;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for the UserProfileView UI component.
 * Tests profile editing operations for the current user.
 * <p>
 * These tests verify that:
 * - Users can edit both name and color and see the changes persisted
 * - Validation works correctly (e.g., name cannot be empty)
 * - Navigation to the profile page works via the user menu
 * <p>
 * The tests use {@link UserProfileViewTester} to interact with the UI elements
 * and verify the expected behavior.
 */
@Tag("IntegrationUiTest")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@Slf4j
public class UserProfileViewTest extends AbstractUiTestUtil {
    private final String                   newColor = "#00ff00"; // Green
    private final String                   newName  = "UpdatedUser";
    @Value("${local.server.port:8080}")
    private       int                      port;
    @Autowired
    private       HumanizedSeleniumHandler seleniumHandler;
    @Autowired
    private       UserProfileViewTester    userProfileViewTester;

    @BeforeEach
    public void setupTest(TestInfo testInfo) {
        userProfileViewTester.switchToUserProfileView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
    }

    /**
     * Tests editing both name and color in a single save operation.
     * <p>
     * Verifies that when a user changes both their name and color in the profile form
     * and clicks save, both changes are persisted and visible after a page reload.
     */
    @Test
    public void testEditNameAndColor() {
        userProfileViewTester.editProfileNameAndColor(newName, newColor);
    }

    /**
     * Tests validation when attempting to save with an empty name.
     * <p>
     * Verifies that when a user clears the name field and attempts to save,
     * an error notification is shown and the change is not persisted.
     */
    @Test
    public void testEmptyNameValidation() {
        // Get the current field values first
        String originalName  = seleniumHandler.getTextField(UserProfileView.USER_NAME_FIELD);
        String originalEmail = seleniumHandler.getTextField(UserProfileView.USER_EMAIL_FIELD);
        String originalColor = seleniumHandler.getColorPickerValue(UserProfileView.USER_COLOR_PICKER);

        userProfileViewTester.testEmptyNameValidation(originalName, originalEmail, originalColor);
    }

    /**
     * Tests navigation to the profile page from the user menu.
     * <p>
     * Verifies that clicking "View Profile" from the user menu dropdown
     * successfully navigates to the profile page.
     */
    @Test
    public void testNavigateToProfileFromMenu() {
        // First navigate away from the profile page
        seleniumHandler.getAndCheck("http://localhost:" + port + "/ui/" + ProductListView.ROUTE);

        // Then use the menu to navigate back
        userProfileViewTester.navigateToProfileFromMenu();

        // Verify we're on the profile page by checking for the title
        seleniumHandler.waitForElementToBeClickable(UserProfileView.PROFILE_PAGE_TITLE);
    }
}

