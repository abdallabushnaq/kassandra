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
import de.bushnaq.abdalla.kassandra.ui.dialog.ProductDialog;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.ProductListView;
import de.bushnaq.abdalla.kassandra.ui.view.VersionListView;
import lombok.extern.log4j.Log4j2;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import static de.bushnaq.abdalla.kassandra.ui.view.ProductListView.PRODUCT_GRID_NAME_PREFIX;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test helper class for interacting with the Product UI components.
 * <p>
 * This class provides methods to test product-related operations in the UI such as
 * creating, editing, deleting products and navigating between views. It uses
 * {@link HumanizedSeleniumHandler} to interact with UI elements and validate results.
 */
@Component
@Lazy
@Log4j2
public class ProductListViewTester extends AbstractViewTester {

    /**
     * Constructs a new ProductViewTester with the given Selenium handler and server port.
     *
     * @param seleniumHandler the handler for Selenium operations
     * @param port            the port on which the application server is running
     */
    public ProductListViewTester(HumanizedSeleniumHandler seleniumHandler, @Value("${local.server.port:8080}") int port) {
        super(seleniumHandler, port);
    }

    public void closeConfirmDialog(String button) {
        closeConfirmDialog(button, ProductListView.PRODUCT_LIST_PAGE_TITLE);
    }

    public void closeDialog(String buttonId) {
        seleniumHandler.wait(200);
        seleniumHandler.click(buttonId);
        seleniumHandler.waitForElementToBeClickable(ProductListView.PRODUCT_LIST_PAGE_TITLE);
        seleniumHandler.waitForPageLoaded();
        seleniumHandler.wait(200);
    }

    /**
     * Tests the creation of a product where the user cancels the operation.
     * <p>
     * Opens the product creation dialog, enters the given product name, then cancels
     * the dialog. Verifies that no product with the specified name appears in the product list.
     *
     * @param name the name of the product to attempt to create
     */
    public void createProductCancel(String name) {
        seleniumHandler.click(ProductListView.CREATE_PRODUCT_BUTTON);
        seleniumHandler.setTextField(ProductDialog.PRODUCT_NAME_FIELD, name);
        closeDialog(ProductDialog.CANCEL_BUTTON);
        seleniumHandler.ensureIsNotInList(ProductListView.PRODUCT_GRID_NAME_PREFIX, name);
    }

    /**
     * Tests the successful creation of a product.
     * <p>
     * Opens the product creation dialog, enters the given product name, then confirms
     * the dialog. Verifies that a product with the specified name appears in the product list.
     *
     * @param name the name of the product to create
     */
    public void createProductConfirm(String name) {
        seleniumHandler.click(ProductListView.CREATE_PRODUCT_BUTTON);
        seleniumHandler.setTextField(ProductDialog.PRODUCT_NAME_FIELD, name);
        closeDialog(ProductDialog.CONFIRM_BUTTON);
        seleniumHandler.ensureIsInList(ProductListView.PRODUCT_GRID_NAME_PREFIX, name);
    }

    /**
     * Tests the creation of a product with ACL assignments (users and groups).
     * <p>
     * Opens the product creation dialog, enters product name, selects users and groups
     * for ACL, then confirms the dialog. Verifies that the product appears in the list.
     *
     * @param name       the name of the product to create
     * @param userNames  names of users to grant access (varargs)
     * @param groupNames names of groups to grant access (varargs)
     */
    public void createProductWithAcl(String name, String[] userNames, String[] groupNames) {
        seleniumHandler.click(ProductListView.CREATE_PRODUCT_BUTTON);
        seleniumHandler.setTextField(ProductDialog.PRODUCT_NAME_FIELD, name);

        // Select users for ACL
        if (userNames != null) {
            seleniumHandler.setMultiSelectComboBoxValue(ProductDialog.PRODUCT_ACL_USERS_FIELD, userNames);
        }

        // Select groups for ACL
        if (groupNames != null) {
            seleniumHandler.setMultiSelectComboBoxValue(ProductDialog.PRODUCT_ACL_GROUPS_FIELD, groupNames);
        }

        closeDialog(ProductDialog.CONFIRM_BUTTON);
        seleniumHandler.ensureIsInList(ProductListView.PRODUCT_GRID_NAME_PREFIX, name);
    }

    /**
     * Tests the behavior when attempting to create a product with a name that already exists.
     * <p>
     * Opens the product creation dialog, enters a name that already exists in the product list,
     * and attempts to confirm the dialog. Verifies that an error message appears on the name field
     * and the duplicate product is not created.
     *
     * @param name the duplicate name to attempt to use for the product
     */
    public void createProductWithDuplicateName(String name) {
        seleniumHandler.click(ProductListView.CREATE_PRODUCT_BUTTON);
        seleniumHandler.setTextField(ProductDialog.PRODUCT_NAME_FIELD, name);
        seleniumHandler.wait(200);
        seleniumHandler.click(ProductDialog.CONFIRM_BUTTON);//do not use closeDialog to avoid waiting for invisibility

        // Check for field error message instead of notification
        String errorMessage = seleniumHandler.getFieldErrorMessage(ProductDialog.PRODUCT_NAME_FIELD);
        assertNotNull(errorMessage, "Error message should be present on the name field");
        assertTrue(errorMessage.contains("A product with name '" + name + "' already exists"), "Unexpected error message format");

        closeDialog(ProductDialog.CANCEL_BUTTON);
        seleniumHandler.ensureElementCountInGrid(ProductListView.PRODUCT_GRID, PRODUCT_GRID_NAME_PREFIX, name, 1);
    }

    /**
     * Tests product deletion where the user cancels the delete confirmation.
     * <p>
     * Clicks the delete button for the specified product,
     * then cancels the confirmation dialog. Verifies that the product still exists in the list.
     *
     * @param name the name of the product to attempt to delete
     */
    public void deleteProductCancel(String name) {
        seleniumHandler.click(ProductListView.PRODUCT_GRID_DELETE_BUTTON_PREFIX + name);
        closeConfirmDialog(ConfirmDialog.CANCEL_BUTTON);
        seleniumHandler.ensureIsInList(ProductListView.PRODUCT_GRID_NAME_PREFIX, name);
    }

    /**
     * Tests the successful deletion of a product.
     * <p>
     * Clicks the delete button for the specified product,
     * then confirms the deletion in the confirmation dialog. Verifies that the product
     * is removed from the product list.
     *
     * @param name the name of the product to delete
     */
    public void deleteProductConfirm(String name) {
        seleniumHandler.click(ProductListView.PRODUCT_GRID_DELETE_BUTTON_PREFIX + name);
        closeConfirmDialog(ConfirmDialog.CONFIRM_BUTTON);
        seleniumHandler.ensureIsNotInList(ProductListView.PRODUCT_GRID_NAME_PREFIX, name);
    }

    /**
     * Tests editing a product to add ACL assignments.
     * <p>
     * Opens the edit dialog for the specified product, adds users and groups to ACL,
     * then confirms the changes.
     *
     * @param name       the name of the product to edit
     * @param userNames  names of users to grant access (varargs)
     * @param groupNames names of groups to grant access (varargs)
     */
    public void editProductAddAcl(String name, String[] userNames, String[] groupNames) {
        seleniumHandler.click(ProductListView.PRODUCT_GRID_EDIT_BUTTON_PREFIX + name);

        // Select users for ACL
        if (userNames != null) {
            seleniumHandler.setMultiSelectComboBoxValue(ProductDialog.PRODUCT_ACL_USERS_FIELD, userNames);
        }

        // Select groups for ACL
        if (groupNames != null) {
            seleniumHandler.setMultiSelectComboBoxValue(ProductDialog.PRODUCT_ACL_GROUPS_FIELD, groupNames);
        }

        closeDialog(ProductDialog.CONFIRM_BUTTON);
    }

    /**
     * Tests product editing where the user cancels the edit operation.
     * <p>
     * Clicks the edit button for the specified product,
     * enters a new name, then cancels the edit dialog. Verifies that the product
     * still exists with its original name and no product with the new name exists.
     *
     * @param name    the original name of the product to edit
     * @param newName the new name to attempt to assign to the product
     */
    public void editProductCancel(String name, String newName) {
        seleniumHandler.click(ProductListView.PRODUCT_GRID_EDIT_BUTTON_PREFIX + name);
        seleniumHandler.setTextField(ProductDialog.PRODUCT_NAME_FIELD, newName);
        closeDialog(ProductDialog.CANCEL_BUTTON);
        seleniumHandler.ensureIsInList(ProductListView.PRODUCT_GRID_NAME_PREFIX, name);
        seleniumHandler.ensureIsNotInList(ProductListView.PRODUCT_GRID_NAME_PREFIX, newName);
    }

    /**
     * Tests the successful editing of a product.
     * <p>
     * Clicks the edit button for the specified product,
     * enters a new name, then confirms the edit. Verifies that the product with
     * the new name appears in the list and the product with the old name is gone.
     *
     * @param name    the original name of the product to edit
     * @param newName the new name to assign to the product
     */
    public void editProductConfirm(String name, String newName) {
        seleniumHandler.click(ProductListView.PRODUCT_GRID_EDIT_BUTTON_PREFIX + name);
        seleniumHandler.setTextField(ProductDialog.PRODUCT_NAME_FIELD, newName);
        closeDialog(ProductDialog.CONFIRM_BUTTON);
        seleniumHandler.ensureIsInList(ProductListView.PRODUCT_GRID_NAME_PREFIX, newName);
        seleniumHandler.ensureIsNotInList(ProductListView.PRODUCT_GRID_NAME_PREFIX, name);
    }

    /**
     * Tests editing a product to remove ACL assignments.
     * <p>
     * Opens the edit dialog, removes users and groups from ACL, then confirms.
     *
     * @param name       the name of the product to edit
     * @param userNames  names of users to revoke access (varargs)
     * @param groupNames names of groups to revoke access (varargs)
     */
    public void editProductRemoveAcl(String name, String[] userNames, String[] groupNames) {
        seleniumHandler.click(ProductListView.PRODUCT_GRID_EDIT_BUTTON_PREFIX + name);

        // Deselect users from ACL
        if (userNames != null) {
            seleniumHandler.setMultiSelectComboBoxValue(ProductDialog.PRODUCT_ACL_USERS_FIELD, userNames);
        }

        // Deselect groups from ACL
        if (groupNames != null) {
            seleniumHandler.setMultiSelectComboBoxValue(ProductDialog.PRODUCT_ACL_GROUPS_FIELD, groupNames);
        }

        closeDialog(ProductDialog.CONFIRM_BUTTON);
    }

    /**
     * Tests product editing with a duplicate name that should fail.
     * <p>
     * Clicks the edit button for the specified product,
     * enters a name that already exists, then attempts to confirm the edit.
     * Verifies that an error message appears and the original name is preserved.
     *
     * @param name    the original name of the product to edit
     * @param newName the duplicate name to attempt to use
     */
    public void editProductWithDuplicateNameFails(String name, String newName) {
        seleniumHandler.click(ProductListView.PRODUCT_GRID_EDIT_BUTTON_PREFIX + name);
        seleniumHandler.setTextField(ProductDialog.PRODUCT_NAME_FIELD, newName);
        seleniumHandler.wait(200);
        seleniumHandler.click(ProductDialog.CONFIRM_BUTTON);//do not use closeDialog to avoid waiting for invisibility

        // Check for field error message instead of notification
        String errorMessage = seleniumHandler.getFieldErrorMessage(ProductDialog.PRODUCT_NAME_FIELD);
        assertNotNull(errorMessage, "Error message should be present on the name field");
        assertTrue(errorMessage.contains("A product with name '" + newName + "' already exists"), "Unexpected error message format");

        closeDialog(ProductDialog.CANCEL_BUTTON);
        seleniumHandler.ensureElementCountInGrid(ProductListView.PRODUCT_GRID, PRODUCT_GRID_NAME_PREFIX, name, 1);
    }

    /**
     * Selects a product from the product grid and navigates to its versions.
     * <p>
     * Clicks on the specified product row in the product grid, which should
     * navigate to the VersionListView for that product.
     *
     * @param name the name of the product to select
     */
    public void selectProduct(String name) {
        seleniumHandler.selectGridRow(PRODUCT_GRID_NAME_PREFIX, VersionListView.class, name);
    }

    /**
     * Navigates to the ProductListView.
     *
     */
    public void switchToProductListView() throws Exception {
        seleniumHandler.click(ProductListView.MENU_ITEM_ID);
        seleniumHandler.waitForElementToBeClickable(ProductListView.PRODUCT_LIST_PAGE_TITLE);
    }

    /**
     * Verifies that the ACL column displays the expected access information.
     * <p>
     * Checks the Access column in the grid for the specified product to verify
     * it shows the correct user and group counts or "Owner only".
     *
     * @param name               the name of the product to verify
     * @param expectedUserCount  expected number of users with access (-1 to skip check)
     * @param expectedGroupCount expected number of groups with access (-1 to skip check)
     */
    public void verifyProductAclDisplay(String name, int expectedUserCount, int expectedGroupCount) {
        // Find the product row in the grid
        String productRowId = ProductListView.PRODUCT_GRID_NAME_PREFIX + name;
        seleniumHandler.waitForElementToBeLocated(productRowId);

        // Get the grid and find the row
        WebElement aclCell  = seleniumHandler.findElement(By.id(ProductListView.PRODUCT_GRID_ACCESS_PREFIX + name));
        String     cellText = aclCell.getText();

        if (expectedUserCount >= 0) {
            String userText = expectedUserCount == 1 ? "1 user" : expectedUserCount + " users";
            assertTrue(cellText.contains(userText),
                    "Expected '" + userText + "' but got: " + cellText);
        }
        if (expectedGroupCount >= 0) {
            String groupText = expectedGroupCount == 1 ? "1 group" : expectedGroupCount + " groups";
            assertTrue(cellText.contains(groupText),
                    "Expected '" + groupText + "' but got: " + cellText);
        }
    }
}
