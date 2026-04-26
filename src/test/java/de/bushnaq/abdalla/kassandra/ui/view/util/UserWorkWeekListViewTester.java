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

package de.bushnaq.abdalla.kassandra.ui.view.util;

import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.dialog.ConfirmDialog;
import de.bushnaq.abdalla.kassandra.ui.dialog.UserWorkWeekDialog;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.UserWorkWeekListView;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test helper class for interacting with the User Work Week UI components.
 * <p>
 * This class provides methods to test work-week-assignment-related operations in the UI such as
 * creating, editing, deleting work week assignment records and navigating between views. It uses
 * {@link HumanizedSeleniumHandler} to interact with UI elements and validate results.
 */
@Component
@Lazy
public class UserWorkWeekListViewTester extends AbstractViewTester {

    @Autowired
    AboutViewTester aboutViewTester;
    @Autowired
    private ProductListViewTester productListViewTester;

    /**
     * Constructs a new UserWorkWeekListViewTester with the given Selenium handler and server port.
     *
     * @param seleniumHandler the handler for Selenium operations
     * @param port            the port on which the application server is running
     */
    public UserWorkWeekListViewTester(HumanizedSeleniumHandler seleniumHandler, @Value("${local.server.port:8080}") int port) {
        super(seleniumHandler, port);
    }

    /**
     * Closes the confirm dialog by clicking the given button, then waits for the page title to reappear.
     *
     * @param button the element ID of the button to click (cancel or confirm)
     */
    public void closeConfirmDialog(String button) {
        closeConfirmDialog(button, UserWorkWeekListView.PAGE_TITLE);
    }

    /**
     * Closes the work-week dialog by clicking the given button, then waits for the page title to reappear.
     *
     * @param confirmButton the element ID of the button to click
     */
    public void closeDialog(String confirmButton) {
        seleniumHandler.wait(200);
        seleniumHandler.click(confirmButton);
        seleniumHandler.waitForElementToBeClickable(UserWorkWeekListView.PAGE_TITLE);
    }

    /**
     * Tests attempting to create a duplicate work week assignment with the same start date.
     * <p>
     * Opens the creation dialog, enters a start date that already exists, then verifies that
     * the confirm button is disabled (uniqueness validation). Cancels the dialog afterwards.
     *
     * @param existingStartDate the start date that already has an assignment
     * @param workWeekName      the work week name to enter in the dialog
     */
    public void createDuplicateDateAssignment(LocalDate existingStartDate, String workWeekName) {
        // Try to create a duplicate record
        seleniumHandler.click(UserWorkWeekListView.CREATE_BUTTON);
        seleniumHandler.setDatePickerValue(UserWorkWeekDialog.START_DATE_FIELD, existingStartDate);
        seleniumHandler.setComboBoxValue(UserWorkWeekDialog.WORK_WEEK_COMBO_FIELD, workWeekName);
        seleniumHandler.wait(200);
        seleniumHandler.waitForElementToBeDisabled(UserWorkWeekDialog.CONFIRM_BUTTON);

        // Cancel the dialog since we expect it to still be open
        closeDialog(UserWorkWeekDialog.CANCEL_BUTTON);
    }

    /**
     * Tests the creation of a work week assignment where the user cancels the operation.
     * <p>
     * Opens the creation dialog, enters values, then cancels. Verifies that no record with
     * the specified start date appears in the list.
     *
     * @param startDate    the start date for the assignment
     * @param workWeekName the name of the work week to assign
     */
    public void createWorkWeekAssignmentCancel(LocalDate startDate, String workWeekName) {
        seleniumHandler.click(UserWorkWeekListView.CREATE_BUTTON);
        seleniumHandler.setDatePickerValue(UserWorkWeekDialog.START_DATE_FIELD, startDate);
        seleniumHandler.setComboBoxValue(UserWorkWeekDialog.WORK_WEEK_COMBO_FIELD, workWeekName);
        closeDialog(UserWorkWeekDialog.CANCEL_BUTTON);

        // Verify the record doesn't appear in the list
        String startDateStr = startDate.format(dateFormatter);
        seleniumHandler.ensureIsNotInList(UserWorkWeekListView.GRID_START_DATE_PREFIX, startDateStr);
    }

    /**
     * Tests the successful creation of a work week assignment.
     * <p>
     * Opens the creation dialog, enters start date and work week, then confirms. Verifies that
     * a record with the specified start date and work week name appears in the list.
     *
     * @param startDate    the start date for the assignment
     * @param workWeekName the name of the work week to assign
     */
    public void createWorkWeekAssignmentConfirm(LocalDate startDate, String workWeekName) {
        seleniumHandler.click(UserWorkWeekListView.CREATE_BUTTON);
        seleniumHandler.setDatePickerValue(UserWorkWeekDialog.START_DATE_FIELD, startDate);
        seleniumHandler.setComboBoxValue(UserWorkWeekDialog.WORK_WEEK_COMBO_FIELD, workWeekName);
        closeDialog(UserWorkWeekDialog.CONFIRM_BUTTON);

        seleniumHandler.wait(300);
        // Verify the record appears in the list
        String startDateStr = startDate.format(dateFormatter);
        seleniumHandler.ensureIsInList(UserWorkWeekListView.GRID_START_DATE_PREFIX, startDateStr);

        // Verify the work week name is displayed correctly
        seleniumHandler.ensureIsInList(UserWorkWeekListView.GRID_NAME_PREFIX, workWeekName);
    }

    /**
     * Tests work week assignment deletion where the user cancels the delete confirmation.
     * <p>
     * Clicks the delete button for the specified record, then cancels the confirmation dialog.
     * Verifies that the record still exists in the list.
     *
     * @param startDate the start date of the assignment to attempt to delete
     */
    public void deleteWorkWeekAssignmentCancel(LocalDate startDate) {
        String startDateStr = startDate.format(dateFormatter);
        seleniumHandler.click(UserWorkWeekListView.GRID_DELETE_BUTTON_PREFIX + startDateStr);
        closeConfirmDialog(ConfirmDialog.CANCEL_BUTTON);
        seleniumHandler.ensureIsInList(UserWorkWeekListView.GRID_START_DATE_PREFIX, startDateStr);
    }

    /**
     * Tests the successful deletion of a work week assignment.
     * <p>
     * Clicks the delete button for the specified record, then confirms the deletion.
     * Verifies that the record is removed from the list.
     *
     * @param startDate the start date of the assignment to delete
     */
    public void deleteWorkWeekAssignmentConfirm(LocalDate startDate) {
        String startDateStr = startDate.format(dateFormatter);
        seleniumHandler.click(UserWorkWeekListView.GRID_DELETE_BUTTON_PREFIX + startDateStr);
        closeConfirmDialog(ConfirmDialog.CONFIRM_BUTTON);
        seleniumHandler.ensureIsNotInList(UserWorkWeekListView.GRID_START_DATE_PREFIX, startDateStr);
    }

    /**
     * Tests the editing of a work week assignment where the user cancels the edit operation.
     * <p>
     * Opens the edit dialog, changes start date and work week, then cancels. Verifies that the
     * record still maintains its original values and no record with the new date was created.
     *
     * @param originalStartDate    the original start date of the record to edit
     * @param newStartDate         the new start date to attempt to assign
     * @param originalWorkWeekName the original work week name to verify remains unchanged after cancellation
     * @param newWorkWeekName      the work week name to attempt to assign
     */
    public void editWorkWeekAssignmentCancel(LocalDate originalStartDate, LocalDate newStartDate,
                                             String originalWorkWeekName, String newWorkWeekName) {
        String originalDateStr = originalStartDate.format(dateFormatter);

        // Verify original values first
        verifyWorkWeekValues(originalDateStr, originalWorkWeekName);

        // Edit the record but cancel
        seleniumHandler.click(UserWorkWeekListView.GRID_EDIT_BUTTON_PREFIX + originalDateStr);
        seleniumHandler.setDatePickerValue(UserWorkWeekDialog.START_DATE_FIELD, newStartDate);
        seleniumHandler.setComboBoxValue(UserWorkWeekDialog.WORK_WEEK_COMBO_FIELD, newWorkWeekName);
        closeDialog(UserWorkWeekDialog.CANCEL_BUTTON);

        // Verify original record still exists and new date was not created
        seleniumHandler.ensureIsInList(UserWorkWeekListView.GRID_START_DATE_PREFIX, originalDateStr);
        if (!originalStartDate.equals(newStartDate)) {
            String newDateStr = newStartDate.format(dateFormatter);
            seleniumHandler.ensureIsNotInList(UserWorkWeekListView.GRID_START_DATE_PREFIX, newDateStr);
        }

        // Verify the values didn't change
        verifyWorkWeekValues(originalDateStr, originalWorkWeekName);
    }

    /**
     * Tests the successful editing of a work week assignment.
     * <p>
     * Opens the edit dialog, changes start date and work week, then confirms. Verifies that the
     * record with the new values appears in the list and the old start date is gone.
     *
     * @param originalStartDate the original start date of the record to edit
     * @param newStartDate      the new start date to assign
     * @param newWorkWeekName   the new work week name to assign
     */
    public void editWorkWeekAssignmentConfirm(LocalDate originalStartDate, LocalDate newStartDate, String newWorkWeekName) {
        String originalDateStr = originalStartDate.format(dateFormatter);

        // Edit the record and confirm
        seleniumHandler.click(UserWorkWeekListView.GRID_EDIT_BUTTON_PREFIX + originalDateStr);
        seleniumHandler.setDatePickerValue(UserWorkWeekDialog.START_DATE_FIELD, newStartDate);
        seleniumHandler.setComboBoxValue(UserWorkWeekDialog.WORK_WEEK_COMBO_FIELD, newWorkWeekName);
        closeDialog(UserWorkWeekDialog.CONFIRM_BUTTON);

        seleniumHandler.wait(300);
        String newDateStr = newStartDate.format(dateFormatter);

        // Verify the new record exists and the old one is gone (if dates are different)
        seleniumHandler.ensureIsInList(UserWorkWeekListView.GRID_START_DATE_PREFIX, newDateStr);
        if (!originalStartDate.equals(newStartDate)) {
            seleniumHandler.ensureIsNotInList(UserWorkWeekListView.GRID_START_DATE_PREFIX, originalDateStr);
        }

        // Verify the updated work week name
        verifyWorkWeekValues(newDateStr, newWorkWeekName);
    }

    /**
     * Navigates to the UserWorkWeekListView for a specific user.
     * <p>
     * Opens the user work week list URL directly, logs in if needed, and waits for the page to load
     * by checking for the presence of the page title element.
     *
     * @param recordingFolderName the folder name for recording the test
     * @param testName            the name of the test for recording
     * @param username            the username (email) for which to view work week assignments
     * @throws Exception if navigation or login fails
     */
    public void switchToUserWorkWeekListView(String recordingFolderName, String testName, String username) throws Exception {
        // Check if we need to log in
        if (!seleniumHandler.getCurrentUrl().contains("/ui/")) {
            aboutViewTester.login(
                    "christopher.paul@kassandra.org",
                    "password",
                    null,
                    recordingFolderName,
                    testName
            );
        }

        // Navigate to the user work week view for the specific user
//        String url = "http://localhost:" + port + "/ui/" + UserWorkWeekListView.ROUTE;
//        if (username != null) {
//            url += "/" + username;
//        }
//
//        seleniumHandler.getAndCheck(url);
//        seleniumHandler.waitForElementToBeClickable(UserWorkWeekListView.PAGE_TITLE);
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_WORK_WEEK);
        seleniumHandler.waitForElementToBeClickable(UserWorkWeekListView.PAGE_TITLE);
    }

    /**
     * Tests that a user cannot delete their only work week assignment.
     * <p>
     * Verifies that the delete button is disabled when the user has only one work week assignment.
     *
     * @param startDate the start date of the only work week assignment
     */
    public void verifyCannotDeleteOnlyAssignment(LocalDate startDate) {
        String startDateStr = startDate.format(dateFormatter);

        // Find the delete button element for the specified assignment
        WebElement deleteButton         = seleniumHandler.findElement(By.id(UserWorkWeekListView.GRID_DELETE_BUTTON_PREFIX + startDateStr));
        boolean    hasDisabledAttribute = deleteButton.getAttribute("disabled") != null;
        assertTrue(hasDisabledAttribute, "Delete button should be disabled for the only work week assignment (missing disabled attribute)");
    }

    /**
     * Verifies the work week name for a specific assignment by opening the edit dialog,
     * reading the combo box value, and cancelling without saving.
     *
     * @param startDateStr         the formatted start date string of the record to verify
     * @param expectedWorkWeekName the expected work week name
     */
    private void verifyWorkWeekValues(String startDateStr, String expectedWorkWeekName) {
        seleniumHandler.click(UserWorkWeekListView.GRID_EDIT_BUTTON_PREFIX + startDateStr);
        String actualWorkWeekName = seleniumHandler.getComboBoxValue(UserWorkWeekDialog.WORK_WEEK_COMBO_FIELD);
        closeDialog(UserWorkWeekDialog.CANCEL_BUTTON);
        Assertions.assertEquals(expectedWorkWeekName.toLowerCase(), actualWorkWeekName.toLowerCase(),
                "Work week name mismatch for record with start date: " + startDateStr);
    }
}

