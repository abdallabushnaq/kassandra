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
import de.bushnaq.abdalla.kassandra.ui.dialog.WorkWeekDialog;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.WorkWeekListView;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test helper class for interacting with the Work Week admin view UI components.
 * <p>
 * This class provides methods to test CRUD operations for global work week definitions
 * in the UI such as creating, editing, and deleting work week records and navigating to
 * the view. It uses {@link HumanizedSeleniumHandler} to interact with UI elements and
 * validate results.
 */
@Component
@Lazy
public class WorkWeekListViewTester extends AbstractViewTester {

    @Autowired
    AboutViewTester aboutViewTester;
    @Autowired
    private ProductListViewTester productListViewTester;

    /**
     * Constructs a new WorkWeekListViewTester with the given Selenium handler and server port.
     *
     * @param seleniumHandler the handler for Selenium operations
     * @param port            the port on which the application server is running
     */
    public WorkWeekListViewTester(HumanizedSeleniumHandler seleniumHandler, @Value("${local.server.port:8080}") int port) {
        super(seleniumHandler, port);
    }

    /**
     * Closes the confirm dialog by clicking the given button, then waits for the page title to reappear.
     *
     * @param button the element ID of the button to click (cancel or confirm)
     */
    public void closeConfirmDialog(String button) {
        closeConfirmDialog(button, WorkWeekListView.PAGE_TITLE);
    }

    /**
     * Closes the work-week dialog by clicking the given button, then waits for the page title to reappear.
     *
     * @param confirmButton the element ID of the button to click
     */
    public void closeDialog(String confirmButton) {
        seleniumHandler.wait(200);
        seleniumHandler.click(confirmButton);
        seleniumHandler.waitForElementToBeClickable(WorkWeekListView.PAGE_TITLE);
    }

    /**
     * Tests the creation of a work week where the user cancels the operation.
     * <p>
     * Opens the creation dialog, enters a name and description, then cancels the dialog.
     * Verifies that no record with the specified name appears in the list.
     *
     * @param name        the name for the work week
     * @param description the description for the work week
     */
    public void createWorkWeekCancel(String name, String description) {
        seleniumHandler.click(WorkWeekListView.CREATE_BUTTON);
        seleniumHandler.setTextField(WorkWeekDialog.NAME_FIELD, name);
        seleniumHandler.setTextField(WorkWeekDialog.DESCRIPTION_FIELD, description);
        closeDialog(WorkWeekDialog.CANCEL_BUTTON);

        // Verify the record does not appear in the list
        seleniumHandler.ensureIsNotInList(WorkWeekListView.GRID_NAME_PREFIX, name);
    }

    /**
     * Tests the successful creation of a work week.
     * <p>
     * Opens the creation dialog, enters a name and description, then confirms the dialog.
     * Verifies that the record appears in the list and that the description was stored correctly
     * by reading it back via the edit dialog.
     *
     * @param name        the name for the work week
     * @param description the description for the work week
     */
    public void createWorkWeekConfirm(String name, String description) {
        seleniumHandler.click(WorkWeekListView.CREATE_BUTTON);
        seleniumHandler.setTextField(WorkWeekDialog.NAME_FIELD, name);
        seleniumHandler.setTextField(WorkWeekDialog.DESCRIPTION_FIELD, description);
        closeDialog(WorkWeekDialog.CONFIRM_BUTTON);

        seleniumHandler.wait(300);
        // Verify the record appears in the list by name
        seleniumHandler.ensureIsInList(WorkWeekListView.GRID_NAME_PREFIX, name);

        // Verify the description was stored correctly by reading it back from the edit dialog
        verifyWorkWeekValues(name, name, description);
    }

    /**
     * Tests work week deletion where the user cancels the delete confirmation.
     * <p>
     * Clicks the delete button for the specified work week, then cancels the confirmation dialog.
     * Verifies that the record still exists in the list.
     *
     * @param name the name of the work week to attempt to delete
     */
    public void deleteWorkWeekCancel(String name) {
        seleniumHandler.click(WorkWeekListView.GRID_DELETE_BUTTON_PREFIX + name);
        closeConfirmDialog(ConfirmDialog.CANCEL_BUTTON);
        seleniumHandler.ensureIsInList(WorkWeekListView.GRID_NAME_PREFIX, name);
    }

    /**
     * Tests the successful deletion of a work week.
     * <p>
     * Clicks the delete button for the specified work week, then confirms the deletion.
     * Verifies that the record is removed from the list.
     *
     * @param name the name of the work week to delete
     */
    public void deleteWorkWeekConfirm(String name) {
        seleniumHandler.click(WorkWeekListView.GRID_DELETE_BUTTON_PREFIX + name);
        closeConfirmDialog(ConfirmDialog.CONFIRM_BUTTON);
        seleniumHandler.ensureIsNotInList(WorkWeekListView.GRID_NAME_PREFIX, name);
    }

    /**
     * Tests editing a work week where the user cancels the edit operation.
     * <p>
     * Verifies the original values, opens the edit dialog, enters new name and description,
     * then cancels. Verifies that the original record values remain unchanged.
     *
     * @param originalName        the current name of the work week to edit
     * @param newName             the new name to attempt to assign
     * @param originalDescription the original description to verify remains unchanged after cancellation
     * @param newDescription      the new description to attempt to assign
     */
    public void editWorkWeekCancel(String originalName, String newName,
                                   String originalDescription, String newDescription) {
        // Verify original values first
        verifyWorkWeekValues(originalName, originalName, originalDescription);

        // Edit but cancel
        seleniumHandler.click(WorkWeekListView.GRID_EDIT_BUTTON_PREFIX + originalName);
        seleniumHandler.setTextField(WorkWeekDialog.NAME_FIELD, newName);
        seleniumHandler.setTextField(WorkWeekDialog.DESCRIPTION_FIELD, newDescription);
        closeDialog(WorkWeekDialog.CANCEL_BUTTON);

        // Verify original record still exists; new name should not appear (if different)
        seleniumHandler.ensureIsInList(WorkWeekListView.GRID_NAME_PREFIX, originalName);
        if (!originalName.equals(newName)) {
            seleniumHandler.ensureIsNotInList(WorkWeekListView.GRID_NAME_PREFIX, newName);
        }

        // Verify the values did not change
        verifyWorkWeekValues(originalName, originalName, originalDescription);
    }

    /**
     * Tests the successful editing of a work week.
     * <p>
     * Opens the edit dialog, changes the name and description, then confirms the edit.
     * Verifies that the record with the new values appears in the list and the old name
     * is no longer present (when the name changed).
     *
     * @param originalName   the current name of the work week to edit
     * @param newName        the new name to assign
     * @param newDescription the new description to assign
     */
    public void editWorkWeekConfirm(String originalName, String newName, String newDescription) {
        seleniumHandler.click(WorkWeekListView.GRID_EDIT_BUTTON_PREFIX + originalName);
        seleniumHandler.setTextField(WorkWeekDialog.NAME_FIELD, newName);
        seleniumHandler.setTextField(WorkWeekDialog.DESCRIPTION_FIELD, newDescription);
        closeDialog(WorkWeekDialog.CONFIRM_BUTTON);

        seleniumHandler.wait(300);

        // Verify the new name exists and the old one is gone (if names are different)
        seleniumHandler.ensureIsInList(WorkWeekListView.GRID_NAME_PREFIX, newName);
        if (!originalName.equals(newName)) {
            seleniumHandler.ensureIsNotInList(WorkWeekListView.GRID_NAME_PREFIX, originalName);
        }

        // Verify the updated values
        verifyWorkWeekValues(newName, newName, newDescription);
    }

    /**
     * Navigates to the WorkWeekListView.
     * <p>
     * Logs in via OIDC if not already authenticated, then navigates directly to the
     * work-week-list URL and waits for the page title to become clickable.
     *
     * @param recordingFolderName the folder name for recording the test
     * @param testName            the name of the test for recording
     * @throws Exception if navigation or login fails
     */
    public void switchToWorkWeekListView(String recordingFolderName, String testName) throws Exception {
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

//        String url = "http://localhost:" + port + "/ui/" + WorkWeekListView.ROUTE;
//        seleniumHandler.getAndCheck(url);
//        seleniumHandler.waitForElementToBeClickable(WorkWeekListView.PAGE_TITLE);
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_MANAGE_WORK_WEEKS);
        seleniumHandler.waitForElementToBeClickable(WorkWeekListView.PAGE_TITLE);
    }

    /**
     * Verifies that attempting to delete a work week that is referenced by users shows an
     * informational "Cannot Delete" dialog instead of a delete confirmation.
     * <p>
     * Clicks the delete button for the given work week, waits for the "Cannot Delete" dialog
     * to appear (identified by the OK button), then closes it and verifies the work week is
     * still present in the list.
     *
     * @param name the name of the work week to attempt to delete
     */
    public void verifyCannotDeleteReferencedWorkWeek(String name) {
        seleniumHandler.click(WorkWeekListView.GRID_DELETE_BUTTON_PREFIX + name);
        // The "Cannot Delete" info dialog opens – wait for its OK button
        seleniumHandler.waitForElementToBeClickable(WorkWeekListView.GRID_CANNOT_DELETE_BUTTON);
        WebElement okButton = seleniumHandler.findElement(By.id(WorkWeekListView.GRID_CANNOT_DELETE_BUTTON));
        assertTrue(okButton.isDisplayed(), "Cannot-delete OK button should be visible");
        // Close the dialog
        seleniumHandler.click(WorkWeekListView.GRID_CANNOT_DELETE_BUTTON);
        seleniumHandler.waitForElementToBeClickable(WorkWeekListView.PAGE_TITLE);
        // Verify the work week is still in the list
        seleniumHandler.ensureIsInList(WorkWeekListView.GRID_NAME_PREFIX, name);
    }

    /**
     * Verifies the name and description of a work week by opening its edit dialog,
     * reading the field values, and then cancelling without saving.
     *
     * @param name                the name of the work week whose record to open
     * @param expectedName        the expected name value
     * @param expectedDescription the expected description value
     */
    private void verifyWorkWeekValues(String name, String expectedName, String expectedDescription) {
        seleniumHandler.click(WorkWeekListView.GRID_EDIT_BUTTON_PREFIX + name);
        String actualName        = seleniumHandler.getTextField(WorkWeekDialog.NAME_FIELD);
        String actualDescription = seleniumHandler.getTextField(WorkWeekDialog.DESCRIPTION_FIELD);
        closeDialog(WorkWeekDialog.CANCEL_BUTTON);
        Assertions.assertEquals(expectedName, actualName,
                "Name mismatch for work week: " + name);
        Assertions.assertEquals(expectedDescription, actualDescription,
                "Description mismatch for work week: " + name);
    }
}

