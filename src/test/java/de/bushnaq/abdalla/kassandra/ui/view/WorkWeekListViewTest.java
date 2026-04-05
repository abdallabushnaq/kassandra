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

import de.bushnaq.abdalla.kassandra.config.DefaultEntitiesInitializer;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.view.util.WorkWeekListViewTester;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Integration test for the WorkWeekListView UI component.
 * Tests CRUD (Create, Read, Update, Delete) operations for global work week definitions in the UI.
 * <p>
 * These tests verify that:
 * - Work week records can be created with a name and description
 * - Created records appear correctly in the list with the stored values
 * - Records can be edited and changes are reflected in the UI
 * - Records can be deleted from the system
 * - Cancellation of create, edit, and delete operations leaves the data unchanged
 * <p>
 * Each test creates its own custom work week named {@code "Custom Test 4x8"} so as not to
 * interfere with the default work weeks seeded by
 * {@link de.bushnaq.abdalla.kassandra.config.DefaultEntitiesInitializer}.
 * <p>
 * The tests use {@link WorkWeekListViewTester} to interact with the UI elements
 * and verify the expected behaviour.
 */
@Tag("IntegrationUiTest")
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=${test.server.port:0}",
                "spring.security.basic.enabled=false" // Disable basic authentication for these tests
        }
)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class WorkWeekListViewTest extends AbstractKeycloakUiTestUtil {

    /**
     * Description assigned when editing the custom work week.
     */
    private final String editedDescription = "Updated 4-day work week description";

    /**
     * Name assigned when editing the custom work week.
     */
    private final String editedName = "Custom Test 4x8 Edited";

    /**
     * Description used when initially creating the custom work week.
     */
    private final String newDescription = "Test 4-day 8-hour work week";

    /**
     * Name of the custom work week created in tests that require one.
     */
    private final String newName = "Custom Test 4x8";

    @Autowired
    private WorkWeekListViewTester workWeekListViewTester;

    /**
     * Navigates to the WorkWeekListView before each test method.
     *
     * @param testInfo JUnit test info used to name the test recording
     * @throws Exception if navigation or login fails
     */
    @BeforeEach
    public void setupTest(TestInfo testInfo) throws Exception {
        workWeekListViewTester.switchToWorkWeekListView(
                testInfo.getTestClass().get().getSimpleName(),
                generateTestCaseName(testInfo));
    }

    /**
     * Tests the behavior when creating a work week but canceling the operation.
     * <p>
     * Verifies that when a user opens the create dialog, enters data, and then cancels,
     * no record is created in the list.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateCancel() {
        workWeekListViewTester.createWorkWeekCancel(newName, newDescription);
    }

    /**
     * Tests the behavior when successfully creating a work week.
     * <p>
     * Verifies that when a user enters all required fields and confirms, the record
     * appears in the list with the correct name and description.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateConfirm() {
        workWeekListViewTester.createWorkWeekConfirm(newName, newDescription);
    }

    /**
     * Tests the behavior when attempting to delete a work week but canceling.
     * <p>
     * Creates a work week, then attempts to delete it but cancels the confirmation dialog.
     * Verifies that the record remains in the list.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testDeleteCancel() {
        // Create a work week first, then attempt to delete but cancel
        workWeekListViewTester.createWorkWeekConfirm(newName, newDescription);
        workWeekListViewTester.deleteWorkWeekCancel(newName);
    }

    /**
     * Tests the behavior when successfully deleting a work week.
     * <p>
     * Creates a work week, then deletes it by confirming the deletion dialog.
     * Verifies that the record is removed from the list.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testDeleteConfirm() {
        // Create a work week first, then delete it
        workWeekListViewTester.createWorkWeekConfirm(newName, newDescription);
        workWeekListViewTester.deleteWorkWeekConfirm(newName);
    }

    /**
     * Tests the behavior when attempting to edit a work week but canceling.
     * <p>
     * Creates a work week, attempts to edit its name and description, then cancels.
     * Verifies that the original record details remain unchanged.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEditCancel() {
        // Create a work week first, then attempt to edit but cancel
        workWeekListViewTester.createWorkWeekConfirm(newName, newDescription);
        workWeekListViewTester.editWorkWeekCancel(newName, editedName, newDescription, editedDescription);
    }

    /**
     * Tests the behavior when successfully editing a work week.
     * <p>
     * Creates a work week, edits both its name and description, and confirms.
     * Verifies that the record with the new values appears in the list and the old
     * name is no longer present.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEditConfirm() {
        // Create a work week first, then edit it
        workWeekListViewTester.createWorkWeekConfirm(newName, newDescription);
        workWeekListViewTester.editWorkWeekConfirm(newName, editedName, editedDescription);
    }

    /**
     * Tests that attempting to delete a work week still referenced by users shows an
     * informational "Cannot Delete" dialog rather than a delete confirmation.
     * <p>
     * The default {@value DefaultEntitiesInitializer#WORK_WEEK_5X8} work week is seeded
     * at startup and assigned to every user, so it is always referenced and must not be deletable.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCannotDeleteReferencedWorkWeek() {
        workWeekListViewTester.verifyCannotDeleteReferencedWorkWeek(DefaultEntitiesInitializer.WORK_WEEK_5X8);
    }
}

