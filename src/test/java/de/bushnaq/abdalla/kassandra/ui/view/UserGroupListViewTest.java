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

package de.bushnaq.abdalla.kassandra.ui.view;

import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.util.UserGroupListViewTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Integration test for the UserGroupListView UI component.
 * Tests CRUD (Create, Read, Update, Delete) operations for user groups in the UI.
 * <p>
 * These tests verify that:
 * - User groups can be created with appropriate details
 * - Created user groups appear correctly in the list
 * - User groups can be edited and changes are reflected in the UI
 * - User groups can be deleted from the system
 * - Cancellation of operations works as expected
 * - Members can be added and removed from groups
 * <p>
 * The tests use {@link UserGroupListViewTester} to interact with the UI elements
 * and verify the expected behavior.
 */
@Tag("IntegrationUiTest")
@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=${test.server.port:0}",
                "spring.profiles.active=test",
                "spring.security.basic.enabled=false"// Disable basic authentication for these tests
        }
)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserGroupListViewTest extends AbstractKeycloakUiTestUtil {
    private final String                   description    = "Test group description";
    private final String                   name           = "UserGroup-Test";
    private final String                   newDescription = "Updated group description";
    private final String                   newName        = "NewUserGroup-Test";
    @Autowired
    private       HumanizedSeleniumHandler seleniumHandler;
    @Autowired
    private       UserGroupListViewTester  userGroupListViewTester;

    @BeforeEach
    public void setupTest(TestInfo testInfo) throws Exception {
        userGroupListViewTester.switchToUserGroupListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo), "christopher.paul@kassandra.org", "password");
    }

    /**
     * Tests the behavior when creating a user group but canceling the operation.
     * <p>
     * Verifies that when a user clicks the create user group button, enters a name, and then
     * cancels the operation, no user group is created in the list.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateCancel() throws Exception {
        userGroupListViewTester.createUserGroupCancel(name, description);
        userGroupListViewTester.verifyFormIsReset();
    }

    /**
     * Tests the behavior when successfully creating a user group.
     * <p>
     * Verifies that when a user clicks the create user group button, enters all user group fields,
     * and confirms the creation, the user group appears in the list.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateConfirm() throws Exception {
        userGroupListViewTester.createUserGroupConfirm(name, description);
        userGroupListViewTester.verifyUserGroupDialogFields(name, description);
    }

    /**
     * Tests creating a user group with members.
     * <p>
     * Creates a group and adds members to it during creation.
     * Verifies that the group appears with the correct member count.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateWithMembers() throws Exception {
        userGroupListViewTester.createUserGroupWithMembers(name, description, "Christopher Paul", "David Johnson");
        userGroupListViewTester.verifyUserGroupHasMembers(name, 2);
    }

    /**
     * Tests the behavior when attempting to delete a user group but canceling the operation.
     * <p>
     * Creates a user group, then attempts to delete it but cancels the confirmation dialog.
     * Verifies that the user group remains in the list.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testDeleteCancel() throws Exception {
        userGroupListViewTester.createUserGroupConfirm(name, description);
        userGroupListViewTester.deleteUserGroupCancel(name);
    }

    /**
     * Tests the behavior when successfully deleting a user group.
     * <p>
     * Creates a user group, then deletes it by confirming the deletion in the confirmation dialog.
     * Verifies that the user group is removed from the list.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testDeleteConfirm() throws Exception {
        userGroupListViewTester.createUserGroupConfirm(name, description);
        userGroupListViewTester.deleteUserGroupConfirm(name);
    }

    /**
     * Tests validation for duplicate user group names.
     * <p>
     * Creates a user group, then attempts to create another group with the same name.
     * Verifies that an error message is displayed.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testDuplicateNameValidation() throws Exception {
        userGroupListViewTester.createUserGroupConfirm(name, description);
        userGroupListViewTester.verifyDuplicateNameError(name);
    }

    /**
     * Tests the behavior when attempting to edit a user group but canceling the operation.
     * <p>
     * Creates a user group, attempts to edit its fields (name and description),
     * but cancels the edit dialog.
     * Verifies that the original user group details remain unchanged and the new values are not applied.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEditCancel() throws Exception {
        userGroupListViewTester.createUserGroupConfirm(name, description);
        userGroupListViewTester.editUserGroupCancel(name, newName, description, newDescription);
        userGroupListViewTester.verifyUserGroupDialogFields(name, description);
    }

    /**
     * Tests the behavior when successfully editing a user group.
     * <p>
     * Creates a user group, edits all user group fields, and confirms the edit.
     * Verifies that the user group with the new name appears in the list and the old name is removed.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEditConfirm() throws Exception {
        userGroupListViewTester.createUserGroupConfirm(name, description);
        userGroupListViewTester.editUserGroupConfirm(name, newName, newDescription);
        userGroupListViewTester.verifyUserGroupDialogFields(newName, newDescription);
    }

    /**
     * Tests adding and removing members from a user group.
     * <p>
     * Creates a group with initial members, then edits to add more members,
     * verifies the member count updates correctly.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEditMembers() throws Exception {
        userGroupListViewTester.createUserGroupWithMembers(name, description, "Christopher Paul");
        userGroupListViewTester.verifyUserGroupHasMembers(name, 1);

        userGroupListViewTester.editUserGroupAddMembers(name, "David Johnson", "Emily Wilson");
        userGroupListViewTester.verifyUserGroupHasMembers(name, 3);
    }

    /**
     * Tests validation for empty user group name.
     * <p>
     * Attempts to create a user group with an empty name.
     * Verifies that validation prevents submission.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEmptyNameValidation() throws Exception {
        userGroupListViewTester.verifyEmptyNameValidation();
    }

    /**
     * Tests the global filter functionality.
     * <p>
     * Creates multiple user groups and tests filtering by name.
     * Verifies that only matching groups are displayed.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testFilterGroups() throws Exception {
        userGroupListViewTester.createUserGroupConfirm("Developers", "Development team");
        userGroupListViewTester.createUserGroupConfirm("Testers", "QA team");
        userGroupListViewTester.filterGroups("Dev");
        userGroupListViewTester.verifyFilteredResults(1);
    }
}

