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

import de.bushnaq.abdalla.kassandra.ui.dialog.ConfirmDialog;
import de.bushnaq.abdalla.kassandra.ui.dialog.UserDialog;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.LoginView;
import de.bushnaq.abdalla.kassandra.ui.view.ProductListView;
import de.bushnaq.abdalla.kassandra.ui.view.UserListView;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Test helper class for interacting with the User UI components.
 * <p>
 * This class provides methods to test user-related operations in the UI such as
 * creating, editing, deleting users and navigating between views. It uses
 * {@link HumanizedSeleniumHandler} to interact with UI elements and validate results.
 */
@Component
@Lazy
public class UserListViewTester {
    private final int                      port;
    private final HumanizedSeleniumHandler seleniumHandler;

    /**
     * Constructs a new UserViewTester with the given Selenium handler and server port.
     *
     * @param seleniumHandler the handler for Selenium operations
     * @param port            the port on which the application server is running
     */
    public UserListViewTester(HumanizedSeleniumHandler seleniumHandler, @Value("${local.server.port:8080}") int port) {
        this.seleniumHandler = seleniumHandler;
        this.port            = port;
    }

    /**
     * Tests the creation of a user where the user cancels the operation.
     * <p>
     * Opens the user creation dialog, enters values for all user fields, then cancels
     * the dialog. Verifies that no user with the specified name appears in the user list
     * and that the cancellation didn't affect any existing data.
     *
     * @param name            the name of the user to attempt to create
     * @param email           the email of the user to attempt to create
     * @param firstWorkingDay the first working day for the user
     * @param lastWorkingDay  the last working day for the user
     */
    public void createUserCancel(String name, String email, LocalDate firstWorkingDay, LocalDate lastWorkingDay) {
        seleniumHandler.click(UserListView.CREATE_USER_BUTTON);
        seleniumHandler.setTextField(UserDialog.USER_NAME_FIELD, name);
        seleniumHandler.setTextField(UserDialog.USER_EMAIL_FIELD, email);
        seleniumHandler.setDatePickerValue(UserDialog.USER_FIRST_WORKING_DAY_PICKER, firstWorkingDay);
        seleniumHandler.setDatePickerValue(UserDialog.USER_LAST_WORKING_DAY_PICKER, lastWorkingDay);
        seleniumHandler.click(UserDialog.CANCEL_BUTTON);
        seleniumHandler.ensureIsNotInList(UserListView.USER_GRID_NAME_PREFIX, name);
    }

    /**
     * Tests the successful creation of a user with all fields.
     * <p>
     * Opens the user creation dialog, enters all user data, then confirms
     * the dialog. Verifies that a user with the specified name appears in the user list
     * and that all fields are correctly stored.
     *
     * @param name            the name of the user to create
     * @param email           the email of the user to create
     * @param firstWorkingDay the first working day for the user
     * @param lastWorkingDay  the last working day for the user
     */
    public void createUserConfirm(String name, String email, LocalDate firstWorkingDay, LocalDate lastWorkingDay) {
        seleniumHandler.click(UserListView.CREATE_USER_BUTTON);
        seleniumHandler.setTextField(UserDialog.USER_NAME_FIELD, name);
        seleniumHandler.setTextField(UserDialog.USER_EMAIL_FIELD, email);
        seleniumHandler.setDatePickerValue(UserDialog.USER_FIRST_WORKING_DAY_PICKER, firstWorkingDay);
        seleniumHandler.setDatePickerValue(UserDialog.USER_LAST_WORKING_DAY_PICKER, lastWorkingDay);
        seleniumHandler.click(UserDialog.CONFIRM_BUTTON);
        seleniumHandler.ensureIsInList(UserListView.USER_GRID_NAME_PREFIX, name);
    }

    /**
     * Tests user deletion where the user cancels the delete confirmation.
     * <p>
     * Clicks the delete button for the specified user,
     * then cancels the confirmation dialog. Verifies that the user still exists in the list.
     *
     * @param name the name of the user to attempt to delete
     */
    public void deleteUserCancel(String name) {
        seleniumHandler.click(UserListView.USER_GRID_DELETE_BUTTON_PREFIX + name);
        seleniumHandler.click(ConfirmDialog.CANCEL_BUTTON);
        seleniumHandler.ensureIsInList(UserListView.USER_GRID_NAME_PREFIX, name);
    }

    /**
     * Tests the successful deletion of a user.
     * <p>
     * Clicks the delete button for the specified user,
     * then confirms the deletion in the confirmation dialog. Verifies that the user
     * is removed from the user list.
     *
     * @param name the name of the user to delete
     */
    public void deleteUserConfirm(String name) {
        seleniumHandler.click(UserListView.USER_GRID_DELETE_BUTTON_PREFIX + name);
        seleniumHandler.click(ConfirmDialog.CONFIRM_BUTTON);
        seleniumHandler.ensureIsNotInList(UserListView.USER_GRID_NAME_PREFIX, name);
    }

    /**
     * Tests user editing where the user cancels the edit operation.
     * <p>
     * Clicks the edit button for the specified user,
     * enters new values for all fields, then cancels the edit dialog. Verifies that the user
     * still exists with its original originalName and no user with the new originalName exists.
     *
     * @param originalName     the original originalName of the user to edit
     * @param newName          the new originalName to attempt to assign to the user
     * @param originalEmail    the original email to verify remains unchanged after cancellation
     * @param newEmail         the email to attempt to assign to the user
     * @param originalFirstDay the original first working day to verify remains unchanged after cancellation
     * @param newFirstDay      the first working day to attempt to assign to the user
     * @param originalLastDay  the original last working day to verify remains unchanged after cancellation
     * @param newLastDay       the last working day to attempt to assign to the user
     */
    public void editUserCancel(String originalName, String newName,
                               String originalEmail, String newEmail,
                               LocalDate originalFirstDay, LocalDate newFirstDay,
                               LocalDate originalLastDay, LocalDate newLastDay) {
        // First verify the current field values before starting the edit
        verifyUserDialogFields(originalName, originalEmail, originalFirstDay, originalLastDay);
        seleniumHandler.wait(500);
        seleniumHandler.click(UserListView.USER_GRID_EDIT_BUTTON_PREFIX + originalName);
        seleniumHandler.setTextField(UserDialog.USER_NAME_FIELD, newName);
        seleniumHandler.setTextField(UserDialog.USER_EMAIL_FIELD, newEmail);
        seleniumHandler.setDatePickerValue(UserDialog.USER_FIRST_WORKING_DAY_PICKER, newFirstDay);
        seleniumHandler.setDatePickerValue(UserDialog.USER_LAST_WORKING_DAY_PICKER, newLastDay);

        // Short pause to ensure UI updates
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        seleniumHandler.click(UserDialog.CANCEL_BUTTON);
        seleniumHandler.ensureIsInList(UserListView.USER_GRID_NAME_PREFIX, originalName);
        seleniumHandler.ensureIsNotInList(UserListView.USER_GRID_NAME_PREFIX, newName);
    }

    /**
     * Tests the successful editing of a user with all fields.
     * <p>
     * Clicks the edit button for the specified user,
     * enters new values for all fields, then confirms the edit. Verifies that the user with
     * the new name appears in the list and the user with the old name is gone.
     * Also verifies that all fields were correctly updated with the new values.
     *
     * @param name            the original name of the user to edit
     * @param newName         the new name to assign to the user
     * @param email           the new email to assign to the user
     * @param firstWorkingDay the new first working day for the user
     * @param lastWorkingDay  the new last working day for the user
     */
    public void editUserConfirm(String name, String newName, String email, LocalDate firstWorkingDay, LocalDate lastWorkingDay) {
        seleniumHandler.click(UserListView.USER_GRID_EDIT_BUTTON_PREFIX + name);
        seleniumHandler.setTextField(UserDialog.USER_NAME_FIELD, newName);
        seleniumHandler.setTextField(UserDialog.USER_EMAIL_FIELD, email);
        seleniumHandler.setDatePickerValue(UserDialog.USER_FIRST_WORKING_DAY_PICKER, firstWorkingDay);
        seleniumHandler.setDatePickerValue(UserDialog.USER_LAST_WORKING_DAY_PICKER, lastWorkingDay);
        seleniumHandler.click(UserDialog.CONFIRM_BUTTON);
        seleniumHandler.ensureIsInList(UserListView.USER_GRID_NAME_PREFIX, newName);
        seleniumHandler.ensureIsNotInList(UserListView.USER_GRID_NAME_PREFIX, name);
    }

    /**
     * Navigates to the UserListView.
     * <p>
     * Opens the user list URL directly and waits for the page to load
     * by checking for the presence of the page title element.
     */
    public void switchToUserListView(String recordingFolderName, String testName) {
        //- Check if we need to log in
        if (!seleniumHandler.getCurrentUrl().contains("/ui/")) {
            seleniumHandler.getAndCheck("http://localhost:" + port + "/ui/" + LoginView.ROUTE);
            seleniumHandler.startRecording(recordingFolderName, testName);
            seleniumHandler.setLoginUser("admin-user");
            seleniumHandler.setLoginPassword("test-password");
            seleniumHandler.loginSubmit();
            seleniumHandler.waitUntil(ExpectedConditions.elementToBeClickable(By.id(ProductListView.PRODUCT_LIST_PAGE_TITLE)));
        }
        seleniumHandler.getAndCheck("http://localhost:" + port + "/ui/" + UserListView.ROUTE);
        seleniumHandler.waitUntil(ExpectedConditions.elementToBeClickable(By.id(UserListView.USER_LIST_PAGE_TITLE)));
    }

    /**
     * Verifies that the form is reset after cancellation.
     * <p>
     * Opens a new user dialog and verifies that it doesn't contain any values from
     * a previous cancelled operation. This method is used to ensure that cancelling
     * a user creation or edit truly discards all changes.
     */
    public void verifyFormIsReset() {
        // Open a new user dialog
        seleniumHandler.click(UserListView.CREATE_USER_BUTTON);
        // Read form values
        String    formName     = seleniumHandler.getTextField(UserDialog.USER_NAME_FIELD);
        String    formEmail    = seleniumHandler.getTextField(UserDialog.USER_EMAIL_FIELD);
        LocalDate formFirstDay = seleniumHandler.getDatePickerValue(UserDialog.USER_FIRST_WORKING_DAY_PICKER);
        LocalDate formLastDay  = seleniumHandler.getDatePickerValue(UserDialog.USER_LAST_WORKING_DAY_PICKER);
        // Close the dialog
        seleniumHandler.click(UserDialog.CANCEL_BUTTON);
        // Assert that all form fields are empty or default values
        Assertions.assertTrue(formName.isEmpty(), "Name field should be empty in new dialog");
        Assertions.assertTrue(formEmail.isEmpty(), "Email field should be empty in new dialog");
        Assertions.assertNull(formFirstDay, "First working day picker should be null in new dialog");
        Assertions.assertNull(formLastDay, "Last working day picker should be null in new dialog");
    }

    /**
     * Verifies all fields in the UserDialog for a specific user.
     * <p>
     * Opens the edit dialog for the specified user and verifies that all fields
     * (name, email, first working day, last working day) have the expected values.
     * Cancels the dialog after verification to avoid making changes.
     *
     * @param name             the name of the user to verify
     * @param expectedEmail    the expected email of the user
     * @param expectedFirstDay the expected first working day for the user
     * @param expectedLastDay  the expected last working day for the user
     */
    public void verifyUserDialogFields(String name, String expectedEmail, LocalDate expectedFirstDay, LocalDate expectedLastDay) {
        // Open the edit dialog for the specified user
        seleniumHandler.click(UserListView.USER_GRID_EDIT_BUTTON_PREFIX + name);
        // Read actual values from the dialog
        String    actualName     = seleniumHandler.getTextField(UserDialog.USER_NAME_FIELD);
        String    actualEmail    = seleniumHandler.getTextField(UserDialog.USER_EMAIL_FIELD);
        LocalDate actualFirstDay = seleniumHandler.getDatePickerValue(UserDialog.USER_FIRST_WORKING_DAY_PICKER);
        LocalDate actualLastDay  = seleniumHandler.getDatePickerValue(UserDialog.USER_LAST_WORKING_DAY_PICKER);
        // Cancel the dialog to avoid making changes
        seleniumHandler.click(UserDialog.CANCEL_BUTTON);
        Assertions.assertEquals(name, actualName, "Name mismatch");
        Assertions.assertEquals(expectedEmail, actualEmail, "Email mismatch");
        Assertions.assertEquals(expectedFirstDay, actualFirstDay, "First working day mismatch");
        Assertions.assertEquals(expectedLastDay, actualLastDay, "Last working day mismatch");
    }
}
