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

import de.bushnaq.abdalla.kassandra.ui.util.AbstractUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.util.ProductListViewTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for the ProductListView UI component.
 * Tests create, edit, and delete operations for products in the UI.
 * <p>
 * These tests use {@link ProductListViewTester} to interact with the UI elements
 * and verify the expected behavior.
 */
@Tag("IntegrationUiTest")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class ProductListViewTest extends AbstractUiTestUtil {
    private final String                   name    = "Product-2";
    private final String                   newName = "NewProduct-2";
    @Autowired
    private       ProductListViewTester    productListViewTester;
    @Autowired
    private       HumanizedSeleniumHandler seleniumHandler;

    @BeforeEach
    public void setupTest(TestInfo testInfo) {
        productListViewTester.switchToProductListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
    }

    /**
     * Tests the behavior when creating a product but canceling the operation.
     * <p>
     * Verifies that when a user clicks the create product button, enters a name, and then
     * cancels the operation, no product is created in the list.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    public void testCreateCancel() throws Exception {
        productListViewTester.createProductCancel(name);
    }

    /**
     * Tests the behavior when successfully creating a product.
     * <p>
     * Verifies that when a user clicks the create product button, enters a name, and confirms
     * the creation, the product appears in the list.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    public void testCreateConfirm() throws Exception {
        productListViewTester.createProductConfirm(name);
    }

    /**
     * Tests the behavior when attempting to create a product with a name that already exists.
     * <p>
     * Creates a product, then tries to create another product with the same name.
     * Verifies that the system displays an error and prevents the creation of the duplicate product.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    public void testCreateDuplicateNameFails() throws Exception {
        // First, create a product
        productListViewTester.createProductConfirm(name);
        // Then try to create another product with the same name
        productListViewTester.createProductWithDuplicateName(name);
    }

    /**
     * Tests the behavior when attempting to delete a product but canceling the operation.
     * <p>
     * Creates a product, then attempts to delete it but cancels the confirmation dialog.
     * Verifies that the product remains in the list.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    public void testDeleteCancel() throws Exception {
        productListViewTester.createProductConfirm(name);
        productListViewTester.deleteProductCancel(name);
    }

    /**
     * Tests the behavior when successfully deleting a product.
     * <p>
     * Creates a product, then deletes it by confirming the deletion in the confirmation dialog.
     * Verifies that the product is removed from the list.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    public void testDeleteConfirm() throws Exception {
        productListViewTester.createProductConfirm(name);
        productListViewTester.deleteProductConfirm(name);
    }

    /**
     * Tests the behavior when attempting to edit a product but canceling the operation.
     * <p>
     * Creates a product, attempts to edit its name, but cancels the edit dialog.
     * Verifies that the original name remains unchanged and the new name is not present.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    public void testEditCancel() throws Exception {
        productListViewTester.createProductConfirm(name);
        productListViewTester.editProductCancel(name, newName);
    }

    /**
     * Tests the behavior when successfully editing a product.
     * <p>
     * Creates a product, edits its name, and confirms the edit.
     * Verifies that the product with the new name appears in the list and the old name is removed.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    public void testEditConfirm() throws Exception {
        productListViewTester.createProductConfirm(name);
        productListViewTester.editProductConfirm(name, newName);
    }

    @Test
    public void testEditDuplicateNameFails() throws Exception {
        productListViewTester.createProductConfirm(name);
        productListViewTester.createProductConfirm(newName);
        productListViewTester.editProductWithDuplicateNameFails(name, newName);
    }
}
