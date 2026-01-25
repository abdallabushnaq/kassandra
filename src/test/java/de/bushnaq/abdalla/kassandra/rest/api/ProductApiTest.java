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

package de.bushnaq.abdalla.kassandra.rest.api;

import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractUiTestUtil;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ServerErrorException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.*;


@Tag("UnitTest")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
public class ProductApiTest extends AbstractUiTestUtil {
    private static final long   FAKE_ID     = 999999L;
    private static final String SECOND_NAME = "SECOND_NAME";

    private User admin1;
    private User user1;
    private User user2;
    private User user3;

    @Test
    public void anonymousSecurity() {
        {
            setUser("admin-user", "ROLE_ADMIN");
            addRandomProducts(1);
            SecurityContextHolder.clearContext();
        }

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            addRandomProducts(1);
        });

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            List<Product> allProducts = productApi.getAll();
        });
        {
            Product product = expectedProducts.getFirst();
            String  name    = product.getName();
            product.setName(SECOND_NAME);
            try {
                updateProduct(product);
                fail("should not be able to update");
            } catch (AuthenticationCredentialsNotFoundException e) {
                //expected
                product.setName(name);
            }
        }

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            removeProduct(expectedProducts.get(0).getId());
        });
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            Product product = productApi.getById(expectedProducts.getFirst().getId());
        });
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")//works
    public void create() throws Exception {
        addRandomProducts(1);
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void createDuplicateNameFails() throws Exception {
        Product product1 = addProduct("Product1");
        try {
            Product product2 = addProduct("Product1");
            fail("Should not be able to create a product with duplicate name");
        } catch (Exception e) {
            // Expected exception for duplicate name
            assertTrue(e.getMessage().contains("CONFLICT") || e.getMessage().contains("already exists"));
        }
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void delete() throws Exception {
        addRandomProducts(2);
        removeProduct(expectedProducts.get(0).getId());
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteUsingFakeId() throws Exception {
        addRandomProducts(2);
        try {
            removeProduct(FAKE_ID);
        } catch (ServerErrorException e) {
            //expected
        }
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void getAll(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        setUser(user1.getEmail(), "ROLE_USER");
        addProduct("Product A");
        addProduct("Product B");
        addProduct("Product C");
        List<Product> allProducts = productApi.getAll();
        assertEquals(1 + 3, allProducts.size());// the "Default" Product is always there
    }

    @Test
    @WithMockUser(roles = "USER")
    public void getAllEmpty() throws Exception {
        List<Product> allProducts = productApi.getAll();
    }

    @Test
    public void getByFakeId() throws Exception {
        setUser("admin-user", "ROLE_ADMIN");
        addRandomProducts(1 + 1);// the "Default" Product is always there
        // Try to get non-existent product (admin)
        try {
            productApi.getById(FAKE_ID);
            fail("Product should not exist");
        } catch (ServerErrorException e) {
            //expected
        }
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void getById(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        setUser(user1.getEmail(), "ROLE_USER");
        addProduct("Product A");
        Product product = productApi.getById(expectedProducts.getFirst().getId());
        assertProductEquals(expectedProducts.getFirst(), product, true);//shallow test
    }

    private void init(RandomCase randomCase, TestInfo testInfo) throws Exception {
        Authentication roleAdmin = setUser("admin-user", "ROLE_ADMIN");
        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        generateProductsIfNeeded(testInfo, randomCase);
        admin1 = userApi.getByEmail("christopher.paul@kassandra.org");
        user1  = userApi.getByEmail("kristen.hubbell@kassandra.org");
        user2  = userApi.getByEmail("claudine.fick@kassandra.org");
        user3  = userApi.getByEmail("randy.asmus@kassandra.org");

        setUser(roleAdmin);
    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                new RandomCase(1, OffsetDateTime.parse("2025-08-11T08:00:00+01:00"), LocalDate.parse("2025-08-04"), Duration.ofDays(10), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 13)//
        };
        return Arrays.stream(randomCases).toList();
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testAdminCanAccessAnyProduct() throws Exception {
        // Create product as one user
        Product product = addProduct("Admin Access Test");

        // Admin should always have access regardless of ACL
        Product retrieved = productApi.getById(product.getId());
        assertNotNull(retrieved);

        // Admin can update
        product.setName("Admin Updated");
        updateProduct(product);

        // Admin can delete
        removeProduct(product.getId());
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    // todo  no suitable HttpMessageConverter found
    public void testCreatorAutoGrantedAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        // When a user creates a product, they should automatically get access
        setUser(user1.getEmail(), "ROLE_USER");
        Product product = addProduct("Creator Test Product");

        // Creator should be able to access their own product
        Product retrieved = productApi.getById(product.getId());
        assertNotNull(retrieved);
        assertEquals(product.getName(), retrieved.getName());

        // Creator should be able to update their product
        product.setName("Updated Creator Product");
        updateProduct(product);

        Product updated = productApi.getById(product.getId());
        assertEquals("Updated Creator Product", updated.getName());
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    // todo  no suitable HttpMessageConverter found
    public void testDeleteProductRemovesAclEntries(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        // Create product as user
        setUser(user1.getEmail(), "ROLE_USER");
        Product product = addProduct("Product To Delete");

        // Verify creator has access
        Product retrieved = productApi.getById(product.getId());
        assertNotNull(retrieved);

        // Delete product
        removeProduct(product.getId());

        // Product should no longer exist
        assertThrows(AccessDeniedException.class, () -> {
            productApi.getById(product.getId());
        });
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    // todo  no suitable HttpMessageConverter found
    public void testGetAllOnlyReturnsAccessibleProducts(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        setUser("admin-user", "ROLE_ADMIN");
        addProduct("product 1");
        addProduct("product 2");
        addProduct("product 3");

        // Admin sees all products
        List<Product> adminProducts = productApi.getAll();
        assertEquals(1 + 3, adminProducts.size());// the "Default" Product is always there

        // Regular user without any ACL entries sees no products
        setUser(user1.getEmail(), "ROLE_USER");
        List<Product> userProducts = productApi.getAll();
        assertEquals(1, userProducts.size());// the "Default" Product is always there
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testMultipleUsersCreateProducts(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        // User 1 creates product
        setUser(user1.getEmail(), "ROLE_USER");
        Product p1 = addProduct("Product 1");

        // User 2 creates product
        setUser(user2.getEmail(), "ROLE_USER");
        Product p2 = addProduct("Product 2");

        // User 3 creates product
        setUser(user3.getEmail(), "ROLE_USER");
        Product p3 = addProduct("Product 3");

        // Each user can only see their own product
        List<Product> user3Products = productApi.getAll();
        assertEquals(1 + 1, user3Products.size());// the "Default" Product is always there
        assertEquals("Product 3", user3Products.get(1).getName());

        setUser(user1.getEmail(), "ROLE_USER");
        List<Product> user1Products = productApi.getAll();
        assertEquals(1 + 1, user1Products.size());// the "Default" Product is always there
        assertEquals("Product 1", user1Products.get(1).getName());

        // Admin can see all products
        setUser("admin-user", "ROLE_ADMIN");
        List<Product> adminProducts = productApi.getAll();
        assertEquals(1 + 3, adminProducts.size());// the "Default" Product is always there
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testProductCreatorGetsAutomaticAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        // User 1 creates a product
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");

        // User 1 should be able to access their product
        Product retrieved = productApi.getById(product1.getId());
        assertNotNull(retrieved);
        assertEquals("User1 Product", retrieved.getName());

        // User 2 creates a different product
        setUser(user2.getEmail(), "ROLE_USER");
        Product product2 = addProduct("User2 Product");

        // User 2 can access their own product
        Product retrieved2 = productApi.getById(product2.getId());
        assertNotNull(retrieved2);

        // User 2 cannot access User 1's product
        assertThrows(AccessDeniedException.class, () -> {
            productApi.getById(product1.getId());
        });
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void update() throws Exception {
        addRandomProducts(2);
        Product product = expectedProducts.getFirst();
        product.setName(SECOND_NAME);
        updateProduct(product);
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void updateToDuplicateNameFails() throws Exception {
        // Create two products
        addRandomProducts(2);
        Product product1 = expectedProducts.get(0);
        Product product2 = expectedProducts.get(1);

        // Try to update product2 to have the same name as product1
        String originalName = product2.getName();
        product2.setName(product1.getName());

        try {
            updateProduct(product2);
            fail("Should not be able to update a product to have a duplicate name");
        } catch (Exception e) {
            // Expected exception for duplicate name
            assertTrue(e.getMessage().contains("CONFLICT") || e.getMessage().contains("already exists"));
        }

        // Restore original name for cleanup
        product2.setName(originalName);
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void updateUsingFakeId() throws Exception {
        addRandomProducts(2);
        Product product = expectedProducts.getFirst();
        Long    id      = product.getId();
        String  name    = product.getName();
        product.setId(FAKE_ID);
        product.setName(SECOND_NAME);
        try {
            updateProduct(product);
            fail("should not be able to update");
        } catch (ServerErrorException e) {
            //restore fields to match db for later tests in @AfterEach
            product.setId(id);
            product.setName(name);
        }
    }

    @Test
    public void userSecurity() {
        {
            setUser("admin-user", "ROLE_ADMIN");
            addRandomProducts(1);
        }
        setUser("user", "ROLE_USER");
        // User without ACL cannot access products
        assertThrows(AccessDeniedException.class, () -> {
            productApi.getById(expectedProducts.getFirst().getId());
        });

        assertThrows(AccessDeniedException.class, () -> {
            Product product = expectedProducts.getFirst();
            String  name    = product.getName();
            product.setName(SECOND_NAME);
            updateProduct(product);
        });

        assertThrows(AccessDeniedException.class, () -> {
            removeProduct(expectedProducts.get(0).getId());
        });
    }
}