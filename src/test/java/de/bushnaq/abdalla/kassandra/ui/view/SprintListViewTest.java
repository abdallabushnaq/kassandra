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

import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.util.FeatureListViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.ProductListViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.SprintListViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.VersionListViewTester;
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
 * Integration test for the SprintListView UI component.
 * Tests create, edit, and delete operations for sprints in the UI.
 * <p>
 * These tests use {@link SprintListViewTester} to interact with the UI elements
 * and verify the expected behavior. Each test requires a product, version, and project
 * to be created first, as sprints exist within the context of a project.
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
//@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class SprintListViewTest extends AbstractKeycloakUiTestUtil {
    @Autowired
    private       FeatureListViewTester    featureListViewTester;
    private final String                   newSprintName = "NewSprint-3";
    @Autowired
    private       ProductListViewTester    productListViewTester;
    private final String                   productName   = "Feature-3";
    private final String                   projectName   = "Feature-3";
    @Autowired
    private       HumanizedSeleniumHandler seleniumHandler;
    @Autowired
    private       SprintListViewTester     sprintListViewTester;
    private final String                   sprintName    = "Sprint-3";
    @Autowired
    private       VersionListViewTester    versionListViewTester;
    private final String                   versionName   = "Version-3";

    /**
     * Setup method that runs before each test.
     * <p>
     * Creates a product, selects it, then creates a version, selects it, and finally
     * creates a project and selects it. This establishes the required hierarchy for testing
     * sprint operations, as sprints exist within projects, which exist within versions,
     * which exist within products.
     *
     * @throws Exception if any error occurs during setup
     */
    @BeforeEach
    public void createPrerequisites(TestInfo testInfo) throws Exception {
        // Navigate to the product list and create a product
        productListViewTester.switchToProductListViewWithOidc(
                "christopher.paul@kassandra.org",
                "password",
                null,
                testInfo.getTestClass().get().getSimpleName(),
                generateTestCaseName(testInfo)
        );
//        seleniumHandler.startRecording(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
        productListViewTester.createProductConfirm(productName);
        productListViewTester.selectProduct(productName);

        // Create a version
        versionListViewTester.createVersionConfirm(versionName);
        versionListViewTester.selectVersion(versionName);

        // Create a project
        featureListViewTester.createFeatureConfirm(projectName);
        featureListViewTester.selectFeature(projectName);
    }

    /**
     * Tests the behavior when creating a sprint but canceling the operation.
     * <p>
     * Verifies that when a user clicks the create sprint button, enters a name, and then
     * cancels the operation, no sprint is created in the list.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateCancel() throws Exception {
        sprintListViewTester.createSprintCancel(sprintName);
    }

    /**
     * Tests the behavior when successfully creating a sprint.
     * <p>
     * Verifies that when a user clicks the create sprint button, enters a name, and confirms
     * the creation, the sprint appears in the list.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateConfirm() throws Exception {
        sprintListViewTester.createSprintConfirm(sprintName);
    }

    /**
     * Tests the behavior when attempting to create a sprint with a name that already exists.
     * <p>
     * Creates a sprint, then tries to create another sprint with the same name.
     * Verifies that the system displays an error and prevents the creation of the duplicate sprint.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateDuplicateNameFails() throws Exception {
        // First, create a sprint
        sprintListViewTester.createSprintConfirm(sprintName);
        // Then try to create another sprint with the same name
        sprintListViewTester.createSprintWithDuplicateName(sprintName);
    }

    /**
     * Tests the behavior when attempting to delete a sprint but canceling the operation.
     * <p>
     * Creates a sprint, then attempts to delete it but cancels the confirmation dialog.
     * Verifies that the sprint remains in the list.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testDeleteCancel() throws Exception {
        sprintListViewTester.createSprintConfirm(sprintName);
        sprintListViewTester.deleteSprintCancel(sprintName);
    }

    /**
     * Tests the behavior when successfully deleting a sprint.
     * <p>
     * Creates a sprint, then deletes it by confirming the deletion in the confirmation dialog.
     * Verifies that the sprint is removed from the list.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testDeleteConfirm() throws Exception {
        sprintListViewTester.createSprintConfirm(sprintName);
        sprintListViewTester.deleteSprintConfirm(sprintName);
    }

    /**
     * Tests the behavior when attempting to edit a sprint but canceling the operation.
     * <p>
     * Creates a sprint, attempts to edit its name, but cancels the edit dialog.
     * Verifies that the original name remains unchanged and the new name is not present.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEditCancel() throws Exception {
        sprintListViewTester.createSprintConfirm(sprintName);
        sprintListViewTester.editSprintCancel(sprintName, newSprintName);
    }

    /**
     * Tests the behavior when successfully editing a sprint.
     * <p>
     * Creates a sprint, edits its name, and confirms the edit.
     * Verifies that the sprint with the new name appears in the list and the old name is removed.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEditConfirm() throws Exception {
        sprintListViewTester.createSprintConfirm(sprintName);
        sprintListViewTester.editSprintConfirm(sprintName, newSprintName);
    }

    /**
     * Tests the behavior when attempting to edit a sprint to have a name that already exists.
     * <p>
     * Creates two sprints, then tries to update the second sprint to have the same name as the first.
     * Verifies that the system displays an error and prevents the update.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEditDuplicateNameFails() throws Exception {
        // First, create two sprints with different names
        sprintListViewTester.createSprintConfirm(sprintName);
        sprintListViewTester.createSprintConfirm(newSprintName);

        // Then try to edit the second sprint to have the same name as the first
        sprintListViewTester.editSprintWithDuplicateName(newSprintName, sprintName);
    }
}
