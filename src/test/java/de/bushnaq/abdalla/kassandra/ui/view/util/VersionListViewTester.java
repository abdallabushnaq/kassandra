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
import de.bushnaq.abdalla.kassandra.ui.dialog.VersionDialog;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.FeatureListView;
import de.bushnaq.abdalla.kassandra.ui.view.VersionListView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static de.bushnaq.abdalla.kassandra.ui.view.VersionListView.VERSION_GRID_NAME_PREFIX;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test helper class for interacting with the Version UI components.
 * <p>
 * This class provides methods to test version-related operations in the UI such as
 * creating, editing, deleting versions and navigating between views. It uses
 * {@link HumanizedSeleniumHandler} to interact with UI elements and validate results.
 * <p>
 * Versions represent a specific release of a product and contain multiple features.
 */
@Component
public class VersionListViewTester extends AbstractViewTester {

    /**
     * Constructs a new VersionViewTester with the given Selenium handler.
     *
     * @param seleniumHandler the handler for Selenium operations
     */
    public VersionListViewTester(HumanizedSeleniumHandler seleniumHandler, @Value("${local.server.port:8080}") int port) {
        super(seleniumHandler, port);
    }

    public void closeDialog(String cancelButton) {
        seleniumHandler.wait(200);
        seleniumHandler.click(cancelButton);
        seleniumHandler.waitForElementToBeClickable(VersionListView.VERSION_LIST_PAGE_TITLE);
    }

    /**
     * Tests the creation of a version where the user cancels the operation.
     * <p>
     * Opens the version creation dialog, enters the given version name, then cancels
     * the dialog. Verifies that no version with the specified name appears in the version list.
     *
     * @param name the name of the version to attempt to create
     */
    public void createVersionCancel(String name) {
        seleniumHandler.click(VersionListView.CREATE_VERSION_BUTTON);
        seleniumHandler.setTextField(VersionDialog.VERSION_NAME_FIELD, name);
        closeDialog(VersionDialog.CANCEL_BUTTON);
        seleniumHandler.ensureIsNotInList(VersionListView.VERSION_GRID_NAME_PREFIX, name);
    }

    /**
     * Tests the successful creation of a version.
     * <p>
     * Opens the version creation dialog, enters the given version name, then confirms
     * the dialog. Verifies that a version with the specified name appears in the version list.
     *
     * @param name the name of the version to create
     */
    public void createVersionConfirm(String name) {
        seleniumHandler.click(VersionListView.CREATE_VERSION_BUTTON);
        seleniumHandler.setTextField(VersionDialog.VERSION_NAME_FIELD, name);
        closeDialog(VersionDialog.CONFIRM_BUTTON);
        seleniumHandler.ensureIsInList(VersionListView.VERSION_GRID_NAME_PREFIX, name);
    }

    /**
     * Tests the attempt to create a version with a name that already exists.
     * <p>
     * Opens the version creation dialog, enters a name that already exists,
     * clicks the save button, and verifies that an error message is displayed
     * indicating the name is already in use.
     *
     * @param name the duplicate name to attempt to use
     */
    public void createVersionWithDuplicateName(String name) {
        seleniumHandler.click(VersionListView.CREATE_VERSION_BUTTON);
        seleniumHandler.setTextField(VersionDialog.VERSION_NAME_FIELD, name);
        closeDialog(VersionDialog.CONFIRM_BUTTON);

        // Check for field error message instead of notification
        String errorMessage = seleniumHandler.getFieldErrorMessage(VersionDialog.VERSION_NAME_FIELD);
        assertNotNull(errorMessage, "Error message should be present on the name field");
        assertTrue(errorMessage.contains("already exists"), "Error message should indicate version already exists");

        closeDialog(VersionDialog.CANCEL_BUTTON);
        seleniumHandler.ensureElementCountInGrid(VersionListView.VERSION_GRID, VERSION_GRID_NAME_PREFIX, name, 1);
    }

    /**
     * Tests version deletion where the user cancels the delete confirmation.
     * <p>
     * Clicks the delete button for the specified version,
     * then cancels the confirmation dialog. Verifies that the version still exists in the list.
     *
     * @param name the name of the version to attempt to delete
     */
    public void deleteVersionCancel(String name) {
        seleniumHandler.click(VersionListView.VERSION_GRID_DELETE_BUTTON_PREFIX + name);
        closeConfirmDialog(ConfirmDialog.CANCEL_BUTTON);
        seleniumHandler.waitForElementInvisibility(VersionDialog.VERSION_DIALOG);
        seleniumHandler.ensureIsInList(VersionListView.VERSION_GRID_NAME_PREFIX, name);
    }

    /**
     * Tests the successful deletion of a version.
     * <p>
     * Clicks the delete button for the specified version,
     * then confirms the deletion in the confirmation dialog. Verifies that the version
     * is removed from the version list.
     *
     * @param name the name of the version to delete
     */
    public void deleteVersionConfirm(String name) {
        seleniumHandler.click(VersionListView.VERSION_GRID_DELETE_BUTTON_PREFIX + name);
        closeConfirmDialog(ConfirmDialog.CONFIRM_BUTTON);
        seleniumHandler.waitForElementInvisibility(VersionDialog.VERSION_DIALOG);
        seleniumHandler.ensureIsNotInList(VersionListView.VERSION_GRID_NAME_PREFIX, name);
    }

    /**
     * Tests version editing where the user cancels the edit operation.
     * <p>
     * Clicks the edit button for the specified version,
     * enters a new name, then cancels the edit dialog. Verifies that the version
     * still exists with its original name and no version with the new name exists.
     *
     * @param name    the original name of the version to edit
     * @param newName the new name to attempt to assign to the version
     */
    public void editVersionCancel(String name, String newName) {
        seleniumHandler.click(VersionListView.VERSION_GRID_EDIT_BUTTON_PREFIX + name);
        seleniumHandler.setTextField(VersionDialog.VERSION_NAME_FIELD, newName);
        closeDialog(VersionDialog.CANCEL_BUTTON);
        seleniumHandler.ensureIsInList(VersionListView.VERSION_GRID_NAME_PREFIX, name);
        seleniumHandler.ensureIsNotInList(VersionListView.VERSION_GRID_NAME_PREFIX, newName);
    }

    /**
     * Tests the successful editing of a version.
     * <p>
     * Clicks the edit button for the specified version,
     * enters a new name, then confirms the edit. Verifies that the version with
     * the new name appears in the list and the version with the old name is gone.
     *
     * @param name    the original name of the version to edit
     * @param newName the new name to assign to the version
     */
    public void editVersionConfirm(String name, String newName) {
        seleniumHandler.click(VersionListView.VERSION_GRID_EDIT_BUTTON_PREFIX + name);
        seleniumHandler.setTextField(VersionDialog.VERSION_NAME_FIELD, newName);
        closeDialog(VersionDialog.CONFIRM_BUTTON);
        seleniumHandler.ensureIsInList(VersionListView.VERSION_GRID_NAME_PREFIX, newName);
        seleniumHandler.ensureIsNotInList(VersionListView.VERSION_GRID_NAME_PREFIX, name);
    }

    /**
     * Tests the attempt to edit a version to have a name that already exists.
     * <p>
     * Clicks the edit button for the specified version,
     * changes the name to one that already exists, clicks the save button, and verifies
     * that an error message is displayed indicating the name is already in use.
     *
     * @param originalName  the name of the version to edit
     * @param duplicateName the duplicate name that already exists
     */
    public void editVersionWithDuplicateName(String originalName, String duplicateName) {
        seleniumHandler.click(VersionListView.VERSION_GRID_EDIT_BUTTON_PREFIX + originalName);
        seleniumHandler.setTextField(VersionDialog.VERSION_NAME_FIELD, duplicateName);
        closeDialog(VersionDialog.CONFIRM_BUTTON);

        // Check for field error message instead of notification
        String errorMessage = seleniumHandler.getFieldErrorMessage(VersionDialog.VERSION_NAME_FIELD);
        assertNotNull(errorMessage, "Error message should be present on the name field");
        assertTrue(errorMessage.contains("already exists"), "Error message should indicate version already exists");

        closeDialog(VersionDialog.CANCEL_BUTTON);
        seleniumHandler.ensureIsInList(VersionListView.VERSION_GRID_NAME_PREFIX, originalName);
        seleniumHandler.ensureIsInList(VersionListView.VERSION_GRID_NAME_PREFIX, duplicateName);
    }

    /**
     * Selects a version from the version grid and navigates to its features.
     * <p>
     * Clicks on the specified version row in the version grid, which should
     * navigate to the FeatureListView for that version.
     *
     * @param name the name of the version to select
     */
    public void selectVersion(String name) {
        seleniumHandler.selectGridRow(VERSION_GRID_NAME_PREFIX, FeatureListView.class, name);
    }
}
