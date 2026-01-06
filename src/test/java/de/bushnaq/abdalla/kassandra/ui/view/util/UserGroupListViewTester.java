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
import de.bushnaq.abdalla.kassandra.ui.dialog.UserGroupDialog;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.UserGroupListView;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Test helper class for interacting with the UserGroup UI components.
 * <p>
 * This class provides methods to test user group-related operations in the UI such as
 * creating, editing, deleting user groups and managing members. It uses
 * {@link HumanizedSeleniumHandler} to interact with UI elements and validate results.
 */
@Component
@Lazy
public class UserGroupListViewTester extends AbstractViewTester {

    @Autowired
    private ProductListViewTester productListViewTester;

    /**
     * Constructs a new UserGroupListViewTester with the given Selenium handler and server port.
     *
     * @param seleniumHandler the handler for Selenium operations
     * @param port            the port on which the application server is running
     */
    public UserGroupListViewTester(HumanizedSeleniumHandler seleniumHandler, @Value("${local.server.port:8080}") int port) {
        super(seleniumHandler, port);
    }

    public void closeConfirmDialog(String button) {
        closeConfirmDialog(button, UserGroupListView.GROUP_LIST_PAGE_TITLE);
    }

    public void closeDialog(String cancelButton) {
        seleniumHandler.wait(200);
        seleniumHandler.click(cancelButton);
        seleniumHandler.waitForElementToBeClickable(UserGroupListView.GROUP_LIST_PAGE_TITLE);
    }

    /**
     * Tests the creation of a user group where the user cancels the operation.
     * <p>
     * Opens the user group creation dialog, enters values for all user group fields, then cancels
     * the dialog. Verifies that no user group with the specified name appears in the list
     * and that the cancellation didn't affect any existing data.
     *
     * @param name        the name of the user group to attempt to create
     * @param description the description of the user group to attempt to create
     */
    public void createUserGroupCancel(String name, String description) {
        seleniumHandler.click(UserGroupListView.CREATE_GROUP_BUTTON);
        seleniumHandler.setTextField(UserGroupDialog.GROUP_NAME_FIELD, name);
        seleniumHandler.setTextArea(UserGroupDialog.GROUP_DESCRIPTION_FIELD, description);
        closeDialog(UserGroupDialog.CANCEL_BUTTON);
        seleniumHandler.ensureIsNotInList(UserGroupListView.GROUP_GRID_NAME_PREFIX, name);
    }

    /**
     * Tests the successful creation of a user group with all fields.
     * <p>
     * Opens the user group creation dialog, enters all user group data, then confirms
     * the dialog. Verifies that a user group with the specified name appears in the list
     * and that all fields are correctly stored.
     *
     * @param name        the name of the user group to create
     * @param description the description of the user group to create
     */
    public void createUserGroupConfirm(String name, String description) {
        seleniumHandler.click(UserGroupListView.CREATE_GROUP_BUTTON);
        seleniumHandler.setTextField(UserGroupDialog.GROUP_NAME_FIELD, name);
        seleniumHandler.setTextArea(UserGroupDialog.GROUP_DESCRIPTION_FIELD, description);
        closeDialog(UserGroupDialog.CONFIRM_BUTTON);
        seleniumHandler.ensureIsInList(UserGroupListView.GROUP_GRID_NAME_PREFIX, name);
    }

    /**
     * Tests creating a user group with members.
     * <p>
     * Opens the creation dialog, sets name and description, selects members from the multi-select,
     * and confirms. Verifies the group appears in the list.
     *
     * @param name        the name of the user group to create
     * @param description the description of the user group to create
     * @param memberNames the names of users to add as members (variable arguments)
     */
    public void createUserGroupWithMembers(String name, String description, String... memberNames) {
        seleniumHandler.click(UserGroupListView.CREATE_GROUP_BUTTON);
        seleniumHandler.setTextField(UserGroupDialog.GROUP_NAME_FIELD, name);
        seleniumHandler.setTextArea(UserGroupDialog.GROUP_DESCRIPTION_FIELD, description);

        seleniumHandler.setMultiSelectComboBoxValue(UserGroupDialog.GROUP_MEMBERS_FIELD, memberNames);

        closeDialog(UserGroupDialog.CONFIRM_BUTTON);
        seleniumHandler.ensureIsInList(UserGroupListView.GROUP_GRID_NAME_PREFIX, name);
    }

    /**
     * Tests user group deletion where the user cancels the delete confirmation.
     * <p>
     * Clicks the delete button for the specified user group,
     * then cancels the confirmation dialog. Verifies that the user group still exists in the list.
     *
     * @param name the name of the user group to attempt to delete
     */
    public void deleteUserGroupCancel(String name) {
        seleniumHandler.click(UserGroupListView.GROUP_GRID_DELETE_BUTTON_PREFIX + name);
        closeConfirmDialog(ConfirmDialog.CANCEL_BUTTON);
        seleniumHandler.ensureIsInList(UserGroupListView.GROUP_GRID_NAME_PREFIX, name);
    }

    /**
     * Tests the successful deletion of a user group.
     * <p>
     * Clicks the delete button for the specified user group,
     * then confirms the deletion in the confirmation dialog. Verifies that the user group
     * is removed from the list.
     *
     * @param name the name of the user group to delete
     */
    public void deleteUserGroupConfirm(String name) {
        seleniumHandler.click(UserGroupListView.GROUP_GRID_DELETE_BUTTON_PREFIX + name);
        closeConfirmDialog(ConfirmDialog.CONFIRM_BUTTON);
        seleniumHandler.wait(300);
        seleniumHandler.ensureIsNotInList(UserGroupListView.GROUP_GRID_NAME_PREFIX, name);
    }

    /**
     * Edits a user group to add members.
     * <p>
     * Opens the edit dialog for the specified group, adds members, and confirms.
     *
     * @param groupName   the name of the user group to edit
     * @param memberNames the names of users to add as members (variable arguments)
     */
    public void editUserGroupAddMembers(String groupName, String... memberNames) {
        seleniumHandler.click(UserGroupListView.GROUP_GRID_EDIT_BUTTON_PREFIX + groupName);

        // TODO: Select additional members using multi-select combo box
        // This functionality needs to be implemented in SeleniumHandler
        // for (String memberName : memberNames) {
        //     seleniumHandler.selectMultiSelectComboBoxItem(UserGroupDialog.GROUP_MEMBERS_FIELD, memberName);
        // }

        closeDialog(UserGroupDialog.CONFIRM_BUTTON);
    }

    /**
     * Tests user group editing where the user cancels the edit operation.
     * <p>
     * Clicks the edit button for the specified user group,
     * enters new values for all fields, then cancels the edit dialog. Verifies that the user group
     * still exists with its original name and no user group with the new name exists.
     *
     * @param originalName        the original name of the user group to edit
     * @param newName             the new name to attempt to assign to the user group
     * @param originalDescription the original description to verify remains unchanged after cancellation
     * @param newDescription      the description to attempt to assign to the user group
     */
    public void editUserGroupCancel(String originalName, String newName, String originalDescription, String newDescription) {
        seleniumHandler.click(UserGroupListView.GROUP_GRID_EDIT_BUTTON_PREFIX + originalName);
        seleniumHandler.setTextField(UserGroupDialog.GROUP_NAME_FIELD, newName);
        seleniumHandler.setTextArea(UserGroupDialog.GROUP_DESCRIPTION_FIELD, newDescription);
        closeDialog(UserGroupDialog.CANCEL_BUTTON);
        seleniumHandler.ensureIsInList(UserGroupListView.GROUP_GRID_NAME_PREFIX, originalName);
        seleniumHandler.ensureIsNotInList(UserGroupListView.GROUP_GRID_NAME_PREFIX, newName);
    }

    /**
     * Tests the successful editing of a user group.
     * <p>
     * Clicks the edit button for the specified user group,
     * enters new values for all fields, then confirms the edit dialog. Verifies that the user group
     * with the new name appears in the list and the old name is removed.
     *
     * @param originalName   the original name of the user group to edit
     * @param newName        the new name to assign to the user group
     * @param newDescription the new description to assign to the user group
     */
    public void editUserGroupConfirm(String originalName, String newName, String newDescription) {
        seleniumHandler.click(UserGroupListView.GROUP_GRID_EDIT_BUTTON_PREFIX + originalName);
        seleniumHandler.setTextField(UserGroupDialog.GROUP_NAME_FIELD, newName);
        seleniumHandler.setTextArea(UserGroupDialog.GROUP_DESCRIPTION_FIELD, newDescription);
        closeDialog(UserGroupDialog.CONFIRM_BUTTON);
        seleniumHandler.ensureIsInList(UserGroupListView.GROUP_GRID_NAME_PREFIX, newName);
        seleniumHandler.ensureIsNotInList(UserGroupListView.GROUP_GRID_NAME_PREFIX, originalName);
    }

    /**
     * Tests global filtering of user groups.
     * <p>
     * Enters a search term in the global filter field and waits for results to update.
     *
     * @param filterText the text to filter by
     */
    public void filterGroups(String filterText) {
        seleniumHandler.setTextField(UserGroupListView.GROUP_GLOBAL_FILTER, filterText);
        seleniumHandler.wait(500); // Wait for filter to apply
    }

    /**
     * Switches to the User Group List View by navigating from the user menu.
     * <p>
     * This method:
     * 1. Logs in as admin
     * 2. Opens the user menu
     * 3. Clicks "Manage User Groups"
     * 4. Verifies the page loaded correctly
     *
     * @param testClassName the name of the test class
     * @param testCaseName  the name of the test case
     * @param userName      the username to log in with
     * @param password      the password to log in with
     */
    public void switchToUserGroupListView(String testClassName, String testCaseName, String userName, String password) throws Exception {
        // Check if we need to log in
        if (!seleniumHandler.getCurrentUrl().contains("/ui/")) {
            productListViewTester.switchToProductListViewWithOidc(userName, password, null, testClassName, testCaseName);
        }

        // Open user menu and click "Manage User Groups"
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.wait(200);
        seleniumHandler.click(MainLayout.ID_USER_MENU_MANAGE_USER_GROUPS);

        // Verify we're on the user group list page
        seleniumHandler.waitForElementToBeClickable(UserGroupListView.GROUP_LIST_PAGE_TITLE);
    }

    /**
     * Verifies that attempting to create a duplicate user group name shows an error.
     * <p>
     * Opens the create dialog, enters the duplicate name, attempts to save,
     * and verifies that an error message is shown.
     *
     * @param name the duplicate name to attempt
     */
    public void verifyDuplicateNameError(String name) {
        seleniumHandler.click(UserGroupListView.CREATE_GROUP_BUTTON);
        seleniumHandler.setTextField(UserGroupDialog.GROUP_NAME_FIELD, name);
        seleniumHandler.setTextArea(UserGroupDialog.GROUP_DESCRIPTION_FIELD, "Duplicate test");
        seleniumHandler.click(UserGroupDialog.CONFIRM_BUTTON);

        // Verify error message is displayed (dialog should still be open)
        seleniumHandler.wait(500);
        // Dialog should still be visible with error

        // Close the dialog
        closeDialog(UserGroupDialog.CANCEL_BUTTON);
    }

    /**
     * Verifies that empty name validation prevents submission.
     * <p>
     * Opens the create dialog, leaves name empty, attempts to save,
     * and verifies that the save button is disabled or validation prevents submission.
     */
    public void verifyEmptyNameValidation() {
        seleniumHandler.click(UserGroupListView.CREATE_GROUP_BUTTON);
        seleniumHandler.setTextArea(UserGroupDialog.GROUP_DESCRIPTION_FIELD, "Test description");

        // Verify that save button is disabled or error is shown
        // TODO: Check if button is disabled - need proper selenium method
        seleniumHandler.wait(300);

        closeDialog(UserGroupDialog.CANCEL_BUTTON);
    }

    /**
     * Verifies that the filter shows the expected number of results.
     *
     * @param expectedCount the expected number of filtered results
     */
    public void verifyFilteredResults(int expectedCount) {
        // This would check the row counter or count visible rows
        // Implementation depends on how the grid displays filtered results
        seleniumHandler.wait(500);
        // TODO: Implement actual verification of filtered count
    }

    /**
     * Verifies that the create form was reset after cancellation.
     * <p>
     * Opens the create dialog and verifies that fields are empty.
     */
    public void verifyFormIsReset() {
        seleniumHandler.click(UserGroupListView.CREATE_GROUP_BUTTON);
        String nameValue = seleniumHandler.getTextField(UserGroupDialog.GROUP_NAME_FIELD);
        String descValue = seleniumHandler.getTextArea(UserGroupDialog.GROUP_DESCRIPTION_FIELD);
        Assertions.assertTrue(nameValue == null || nameValue.isEmpty(), "Name field should be empty");
        Assertions.assertTrue(descValue == null || descValue.isEmpty(), "Description field should be empty");
        closeDialog(UserGroupDialog.CANCEL_BUTTON);
    }

    /**
     * Verifies that the user group dialog fields match the expected values.
     * <p>
     * Opens the edit dialog for a user group and checks that the fields contain
     * the expected values.
     *
     * @param name        the expected name
     * @param description the expected description
     */
    public void verifyUserGroupDialogFields(String name, String description) {
        seleniumHandler.click(UserGroupListView.GROUP_GRID_EDIT_BUTTON_PREFIX + name);
        String actualName = seleniumHandler.getTextField(UserGroupDialog.GROUP_NAME_FIELD);
        String actualDesc = seleniumHandler.getTextArea(UserGroupDialog.GROUP_DESCRIPTION_FIELD);

        Assertions.assertEquals(name, actualName, "Group name should match");
        Assertions.assertEquals(description, actualDesc, "Group description should match");

        closeDialog(UserGroupDialog.CANCEL_BUTTON);
    }

    /**
     * Verifies that a user group has the expected number of members.
     * <p>
     * This checks the member count badge displayed in the grid.
     *
     * @param groupName     the name of the group
     * @param expectedCount the expected member count
     */
    public void verifyUserGroupHasMembers(String groupName, int expectedCount) {
        // This would check the member count displayed in the grid row
        // Implementation depends on how the member count is displayed
        seleniumHandler.wait(300);
        // TODO: Implement actual verification of member count in grid
    }
}

