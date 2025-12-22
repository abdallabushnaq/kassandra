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

package de.bushnaq.abdalla.kassandra.ui.view.util;

import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.LoginView;
import de.bushnaq.abdalla.kassandra.ui.view.ProductListView;
import de.bushnaq.abdalla.kassandra.ui.view.UserProfileView;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Test helper class for interacting with the UserProfile UI components.
 * <p>
 * This class provides methods to test user profile operations in the UI such as
 * editing name and color. It uses {@link HumanizedSeleniumHandler} to interact
 * with UI elements and validate results.
 */
@Component
@Lazy
public class UserProfileViewTester extends AbstractViewTester {

    /**
     * Constructs a new UserProfileViewTester with the given Selenium handler and server port.
     *
     * @param seleniumHandler the handler for Selenium operations
     * @param port            the port on which the application server is running
     */
    public UserProfileViewTester(HumanizedSeleniumHandler seleniumHandler, @Value("${local.server.port:8080}") int port) {
        super(seleniumHandler, port);
    }

    /**
     * Tests editing both name and color in the user profile.
     * <p>
     * Changes both name and color fields, then clicks save. Verifies that both changes
     * are persisted by checking the field values after a page reload using verifyProfileFields.
     *
     * @param newName  the new name to set for the user
     * @param newColor the new hex color code (with # prefix) for the user
     */
    public void editProfileNameAndColor(String newName, String newColor) {
        // Get the current email (which is read-only and won't change)
        String currentEmail = seleniumHandler.getTextField(UserProfileView.USER_EMAIL_FIELD);

        seleniumHandler.setTextField(UserProfileView.USER_NAME_FIELD, newName);
        seleniumHandler.setColorPickerValue(UserProfileView.USER_COLOR_PICKER, newColor);
        seleniumHandler.click(UserProfileView.SAVE_PROFILE_BUTTON);

        // Wait for success notification
        seleniumHandler.waitForNotification("success");

        // Reload the page to verify the changes were persisted
        seleniumHandler.getAndCheck("http://localhost:" + port + "/ui/" + UserProfileView.ROUTE);
        seleniumHandler.waitUntil(ExpectedConditions.elementToBeClickable(By.id(UserProfileView.PROFILE_PAGE_TITLE)));

        // Verify both fields were saved using verifyProfileFields
        verifyProfileFields(newName, currentEmail, newColor);
    }

    /**
     * Navigates to the UserProfileView via the user menu.
     * <p>
     * Clicks on the user menu in the navigation bar and selects "View Profile"
     * from the dropdown. Waits for the profile page to load.
     */
    public void navigateToProfileFromMenu() {
        // Click on the user menu
        seleniumHandler.click(MainLayout.ID_USER_MENU);

        seleniumHandler.wait(200);
        // Click on "View Profile" menu item
        seleniumHandler.click(MainLayout.ID_USER_MENU_VIEW_PROFILE);

        // Wait for the profile page to load
        seleniumHandler.waitUntil(ExpectedConditions.elementToBeClickable(By.id(UserProfileView.PROFILE_PAGE_TITLE)));
    }

    /**
     * Navigates to the UserProfileView directly via URL.
     * <p>
     * Opens the profile URL directly and waits for the page to load
     * by checking for the presence of the page title element.
     */
    public void switchToUserProfileView(String recordingFolderName, String testName) {
        // Check if we need to log in
        if (!seleniumHandler.getCurrentUrl().contains("/ui/")) {
            seleniumHandler.getAndCheck("http://localhost:" + port + "/ui/" + LoginView.ROUTE);
            seleniumHandler.startRecording(recordingFolderName, testName);
            seleniumHandler.setLoginUser("admin-user");
            seleniumHandler.setLoginPassword("test-password");
            seleniumHandler.loginSubmit();
            seleniumHandler.waitUntil(ExpectedConditions.elementToBeClickable(By.id(ProductListView.PRODUCT_LIST_PAGE_TITLE)));
        }
        seleniumHandler.getAndCheck("http://localhost:" + port + "/ui/" + UserProfileView.ROUTE);
        seleniumHandler.waitUntil(ExpectedConditions.elementToBeClickable(By.id(UserProfileView.PROFILE_PAGE_TITLE)));
    }

    /**
     * Tests validation by attempting to save an empty name.
     * <p>
     * Clears the name field and attempts to save. Verifies that an error notification
     * is shown and the form is not saved.
     *
     * @param originalName  the original name to verify remains unchanged
     * @param originalEmail the original email
     * @param originalColor the original color
     */
    public void testEmptyNameValidation(String originalName, String originalEmail, String originalColor) {
        // Clear the name field
        seleniumHandler.setTextField(UserProfileView.USER_NAME_FIELD, "");
        seleniumHandler.click(UserProfileView.SAVE_PROFILE_BUTTON);

        // Wait for error notification
        seleniumHandler.waitForNotification("error");

        // Reload and verify the name wasn't changed
        seleniumHandler.getAndCheck("http://localhost:" + port + "/ui/" + UserProfileView.ROUTE);
        seleniumHandler.waitUntil(ExpectedConditions.elementToBeClickable(By.id(UserProfileView.PROFILE_PAGE_TITLE)));

        // Verify all fields remain unchanged using verifyProfileFields
        verifyProfileFields(originalName, originalEmail, originalColor);
    }

    /**
     * Verifies that the profile form fields contain the expected values.
     * <p>
     * Checks that the name, email, and color fields have the specified values.
     * This method is useful for verifying initial state or confirming that changes
     * were properly persisted.
     *
     * @param expectedName  the expected name value
     * @param expectedEmail the expected email value (read-only)
     * @param expectedColor the expected hex color code (with # prefix)
     */
    public void verifyProfileFields(String expectedName, String expectedEmail, String expectedColor) {
        String actualName  = seleniumHandler.getTextField(UserProfileView.USER_NAME_FIELD);
        String actualEmail = seleniumHandler.getTextField(UserProfileView.USER_EMAIL_FIELD);
        String actualColor = seleniumHandler.getColorPickerValue(UserProfileView.USER_COLOR_PICKER);

        Assertions.assertEquals(expectedName, actualName, "Name mismatch");
        Assertions.assertEquals(expectedEmail, actualEmail, "Email mismatch");
        Assertions.assertEquals(expectedColor, actualColor, "Color mismatch");
    }
}

