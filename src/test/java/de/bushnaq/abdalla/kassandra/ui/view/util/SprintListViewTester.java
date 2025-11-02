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
import de.bushnaq.abdalla.kassandra.ui.dialog.SprintDialog;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.SprintListView;
import de.bushnaq.abdalla.kassandra.ui.view.SprintQualityBoard;
import org.springframework.stereotype.Component;

import static de.bushnaq.abdalla.kassandra.ui.view.SprintListView.SPRINT_GRID_NAME_PREFIX;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test helper class for interacting with the Sprint UI components.
 * <p>
 * This class provides methods to test sprint-related operations in the UI such as
 * creating, editing, deleting sprints and navigating between views. It uses
 * {@link HumanizedSeleniumHandler} to interact with UI elements and validate results.
 * <p>
 * Sprints represent time-boxed work periods within a feature and contain tasks.
 * In the hierarchy: Products contain Versions, Versions contain Features, Features contain Sprints,
 * and Sprints contain Tasks.
 */
@Component
public class SprintListViewTester {
    private final HumanizedSeleniumHandler seleniumHandler;

    /**
     * Constructs a new SprintViewTester with the given Selenium handler.
     *
     * @param seleniumHandler the handler for Selenium operations
     */
    public SprintListViewTester(HumanizedSeleniumHandler seleniumHandler) {
        this.seleniumHandler = seleniumHandler;
    }

    public void configSprint(String sprintName) {
        seleniumHandler.click(SprintListView.SPRINT_GRID_CONFIG_BUTTON_PREFIX + sprintName);
    }

    /**
     * Tests the creation of a sprint where the user cancels the operation.
     * <p>
     * Opens the sprint creation dialog, enters the given sprint name, then cancels
     * the dialog. Verifies that no sprint with the specified name appears in the sprint list.
     *
     * @param name the name of the sprint to attempt to create
     */
    public void createSprintCancel(String name) {
        seleniumHandler.click(SprintListView.CREATE_SPRINT_BUTTON);
        seleniumHandler.setTextField(SprintDialog.SPRINT_NAME_FIELD, name);
        seleniumHandler.click(SprintDialog.CANCEL_BUTTON);
        seleniumHandler.ensureIsNotInList(SprintListView.SPRINT_GRID_NAME_PREFIX, name);
    }

    /**
     * Tests the successful creation of a sprint.
     * <p>
     * Opens the sprint creation dialog, enters the given sprint name, then confirms
     * the dialog. Verifies that a sprint with the specified name appears in the sprint list.
     *
     * @param name the name of the sprint to create
     */
    public void createSprintConfirm(String name) {
        seleniumHandler.click(SprintListView.CREATE_SPRINT_BUTTON);
        seleniumHandler.setTextField(SprintDialog.SPRINT_NAME_FIELD, name);
        seleniumHandler.click(SprintDialog.CONFIRM_BUTTON);
        seleniumHandler.ensureIsInList(SprintListView.SPRINT_GRID_NAME_PREFIX, name);
    }

    /**
     * Tests the attempt to create a sprint with a name that already exists.
     * <p>
     * Opens the sprint creation dialog, enters a name that already exists,
     * clicks the save button, and verifies that an error message is displayed
     * indicating the name is already in use.
     *
     * @param name the duplicate name to attempt to use
     */
    public void createSprintWithDuplicateName(String name) {
        seleniumHandler.click(SprintListView.CREATE_SPRINT_BUTTON);
        seleniumHandler.setTextField(SprintDialog.SPRINT_NAME_FIELD, name);
        seleniumHandler.click(SprintDialog.CONFIRM_BUTTON);

        // Check for field error message instead of notification
        String errorMessage = seleniumHandler.getFieldErrorMessage(SprintDialog.SPRINT_NAME_FIELD);
        assertNotNull(errorMessage, "Error message should be present on the name field");
        assertTrue(errorMessage.contains("already exists"), "Error message should indicate sprint already exists");

        seleniumHandler.click(SprintDialog.CANCEL_BUTTON);
        seleniumHandler.ensureElementCountInGrid(SprintListView.SPRINT_GRID, SPRINT_GRID_NAME_PREFIX, name, 1);
    }

    /**
     * Tests sprint deletion where the user cancels the delete confirmation.
     * <p>
     * Clicks the delete button for the specified sprint,
     * then cancels the confirmation dialog. Verifies that the sprint still exists in the list.
     *
     * @param name the name of the sprint to attempt to delete
     */
    public void deleteSprintCancel(String name) {
        seleniumHandler.click(SprintListView.SPRINT_GRID_DELETE_BUTTON_PREFIX + name);
        seleniumHandler.click(ConfirmDialog.CANCEL_BUTTON);
        seleniumHandler.ensureIsInList(SprintListView.SPRINT_GRID_NAME_PREFIX, name);
    }

    /**
     * Tests the successful deletion of a sprint.
     * <p>
     * Clicks the delete button for the specified sprint,
     * then confirms the deletion in the confirmation dialog. Verifies that the sprint
     * is removed from the sprint list.
     *
     * @param name the name of the sprint to delete
     */
    public void deleteSprintConfirm(String name) {
        seleniumHandler.click(SprintListView.SPRINT_GRID_DELETE_BUTTON_PREFIX + name);
        seleniumHandler.click(ConfirmDialog.CONFIRM_BUTTON);
        seleniumHandler.ensureIsNotInList(SprintListView.SPRINT_GRID_NAME_PREFIX, name);
    }

    /**
     * Tests sprint editing where the user cancels the edit operation.
     * <p>
     * Clicks the edit button for the specified sprint,
     * enters a new name, then cancels the edit dialog. Verifies that the sprint
     * still exists with its original name and no sprint with the new name exists.
     *
     * @param name    the original name of the sprint to edit
     * @param newName the new name to attempt to assign to the sprint
     */
    public void editSprintCancel(String name, String newName) {
        seleniumHandler.click(SprintListView.SPRINT_GRID_EDIT_BUTTON_PREFIX + name);
        seleniumHandler.setTextField(SprintDialog.SPRINT_NAME_FIELD, newName);
        seleniumHandler.click(SprintDialog.CANCEL_BUTTON);
        seleniumHandler.ensureIsInList(SprintListView.SPRINT_GRID_NAME_PREFIX, name);
        seleniumHandler.ensureIsNotInList(SprintListView.SPRINT_GRID_NAME_PREFIX, newName);
    }

    /**
     * Tests the successful editing of a sprint.
     * <p>
     * Clicks the edit button for the specified sprint,
     * enters a new name, then confirms the edit. Verifies that the sprint with
     * the new name appears in the list and the sprint with the old name is gone.
     *
     * @param name    the original name of the sprint to edit
     * @param newName the new name to assign to the sprint
     */
    public void editSprintConfirm(String name, String newName) {
        seleniumHandler.click(SprintListView.SPRINT_GRID_EDIT_BUTTON_PREFIX + name);
        seleniumHandler.setTextField(SprintDialog.SPRINT_NAME_FIELD, newName);
        seleniumHandler.click(SprintDialog.CONFIRM_BUTTON);
        seleniumHandler.ensureIsInList(SprintListView.SPRINT_GRID_NAME_PREFIX, newName);
        seleniumHandler.ensureIsNotInList(SprintListView.SPRINT_GRID_NAME_PREFIX, name);
    }

    /**
     * Tests the attempt to edit a sprint to have a name that already exists.
     * <p>
     * Clicks the edit button for the specified sprint,
     * changes the name to one that already exists, clicks the save button, and verifies
     * that an error message is displayed indicating the name is already in use.
     *
     * @param originalName  the name of the sprint to edit
     * @param duplicateName the duplicate name that already exists
     */
    public void editSprintWithDuplicateName(String originalName, String duplicateName) {
        seleniumHandler.click(SprintListView.SPRINT_GRID_EDIT_BUTTON_PREFIX + originalName);
        seleniumHandler.setTextField(SprintDialog.SPRINT_NAME_FIELD, duplicateName);
        seleniumHandler.click(SprintDialog.CONFIRM_BUTTON);

        // Check for field error message instead of notification
        String errorMessage = seleniumHandler.getFieldErrorMessage(SprintDialog.SPRINT_NAME_FIELD);
        assertNotNull(errorMessage, "Error message should be present on the name field");
        assertTrue(errorMessage.contains("already exists"), "Error message should indicate sprint already exists");

        seleniumHandler.click(SprintDialog.CANCEL_BUTTON);
        seleniumHandler.ensureIsInList(SPRINT_GRID_NAME_PREFIX, originalName);
        seleniumHandler.ensureIsInList(SPRINT_GRID_NAME_PREFIX, duplicateName);
    }

    /**
     * Selects a sprint from the sprint grid and navigates to its tasks.
     * <p>
     * Clicks on the specified sprint row in the sprint grid, which should
     * navigate to the SprintView for that sprint, where tasks can be managed.
     *
     * @param name the name of the sprint to select
     */
    public void selectSprint(String name) {
        seleniumHandler.selectGridRow(SPRINT_GRID_NAME_PREFIX, SprintQualityBoard.class, name);
    }
}
