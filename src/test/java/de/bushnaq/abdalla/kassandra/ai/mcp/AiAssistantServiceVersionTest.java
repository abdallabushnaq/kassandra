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

package de.bushnaq.abdalla.kassandra.ai.mcp;

import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.dto.Version;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AiAssistantService.processQuery method for Version operations.
 * Tests are similar to AiAssistantServiceFeatureTest but for versions.
 * Tests use the configured ChatModel (Anthropic Claude Haiku 3).
 */
@Tag("AiUnitTest")
@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.profiles.active=test"
        }
)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class AiAssistantServiceVersionTest extends AbstractMcpTest {

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void testAdd(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // Get the second product to add versions to (skip Default product at index 0)
        Product firstProduct = productApi.getAll().get(1);

        {
            processQuery("Add a new version with the name 3.1.5 to product " + firstProduct.getName() + ".");
            List<Version>     versions = versionApi.getAll(firstProduct.getId());
            Optional<Version> version  = versions.stream().filter(v -> v.getName().equals("3.1.5")).findFirst();
            assertTrue(version.isPresent(), "Version should be created");
            assertEquals("3.1.5", version.get().getName(), "Version name should match");
            assertEquals(firstProduct.getId(), version.get().getProductId(), "Version should belong to correct product");
        }
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testDelete(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // Get the second product to add versions to (skip Default product at index 0)
        Product firstProduct = productApi.getAll().get(1);

        //---add
        processQuery("Add a new version with the name 2.0.0-beta to product " + firstProduct.getName() + ".");
        List<Version>     versions = versionApi.getAll(firstProduct.getId());
        Optional<Version> version  = versions.stream().filter(v -> v.getName().equals("2.0.0-beta")).findFirst();
        assertTrue(version.isPresent(), "Version should be created before deletion test");
        log.info("Version ID: {}.", version.get().getId());

        // Store initial count
        int versionCountBeforeDelete = versions.size();

        //---delete
        processQuery("Please delete the version you just created.");
        List<Version>     versionsAfterDelete = versionApi.getAll(firstProduct.getId());
        Optional<Version> deletedVersion      = versionsAfterDelete.stream().filter(v -> v.getName().equals("2.0.0-beta")).findFirst();
        assertFalse(deletedVersion.isPresent(), "Version '2.0.0-beta' should be deleted");
        assertEquals(versionCountBeforeDelete - 1, versionsAfterDelete.size(), "Version count should decrease by 1");
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void testGet(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // Get the second product to list versions from (skip Default product at index 0)
        Product       firstProduct = productApi.getAll().get(1);
        List<Version> allVersions  = versionApi.getAll(firstProduct.getId());

        String response = processQuery("list all versions for product " + firstProduct.getName() + ".");
        allVersions.forEach(version ->
                assertTrue(response.toLowerCase(Locale.ROOT).contains(version.getName().toLowerCase()),
                        "Version name missing: " + version.getName())
        );

        // Verify the response mentions it's specifically for the requested product
        assertTrue(response.toLowerCase(Locale.ROOT).contains(firstProduct.getName().toLowerCase()),
                "Response should mention the product name");
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void testGetAll(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // Get all versions across all products
        List<Version> allVersions = versionApi.getAll();

        String response = processQuery("list all versions.");

        // Verify that ALL versions are mentioned in the response
        allVersions.forEach(version ->
                assertTrue(response.toLowerCase(Locale.ROOT).contains(version.getName().toLowerCase()),
                        "Version name missing: " + version.getName())
        );

        log.info("Total versions: {}, All versions verified in response", allVersions.size());
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void testGetById(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // Get a version from the second product (skip Default product)
        Product       secondProduct = productApi.getAll().get(1);
        List<Version> versions      = versionApi.getAll(secondProduct.getId());
        Version       firstVersion  = versions.getFirst();

        String response = processQuery("Get version details for version " + firstVersion.getName() + " of product " + secondProduct.getName() + ".");

        // Verify response contains version details
        assertTrue(response.toLowerCase(Locale.ROOT).contains(firstVersion.getName().toLowerCase()),
                "Response should contain version name");
        assertTrue(response.toLowerCase(Locale.ROOT).contains(secondProduct.getName().toLowerCase()) ||
                        response.contains(String.valueOf(firstVersion.getProductId())),
                "Response should reference the product");
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void testMultipleVersionsForProduct(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // Get the second product (skip Default product at index 0)
        Product firstProduct        = productApi.getAll().get(1);
        int     initialVersionCount = versionApi.getAll(firstProduct.getId()).size();

        // Count how many of these versions already exist (realistic scenario - some versions may already be there)
        List<Version> existingVersions     = versionApi.getAll(firstProduct.getId());
        long          existingVersion100   = existingVersions.stream().filter(v -> v.getName().equals("1.0.0")).count();
        long          existingVersion110   = existingVersions.stream().filter(v -> v.getName().equals("1.1.0")).count();
        long          existingVersion120   = existingVersions.stream().filter(v -> v.getName().equals("1.2.0")).count();
        int           alreadyExistingCount = (int) (existingVersion100 + existingVersion110 + existingVersion120);
        int           expectedNewVersions  = 3 - alreadyExistingCount;

        log.info("Before request: {} versions exist, {} of target versions already exist (1.0.0: {}, 1.1.0: {}, 1.2.0: {})",
                initialVersionCount, alreadyExistingCount, existingVersion100 > 0, existingVersion110 > 0, existingVersion120 > 0);

        // Add multiple versions - AI should handle duplicates gracefully
        String response = processQuery("Add three new versions to product " + firstProduct.getName() +
                ": version 1.0.0, version 1.1.0, and version 1.2.0.");

        List<Version> versionsAfter = versionApi.getAll(firstProduct.getId());

        // Verify all three versions exist (whether newly created or already existed)
        Optional<Version> version100 = versionsAfter.stream().filter(v -> v.getName().equals("1.0.0")).findFirst();
        Optional<Version> version110 = versionsAfter.stream().filter(v -> v.getName().equals("1.1.0")).findFirst();
        Optional<Version> version120 = versionsAfter.stream().filter(v -> v.getName().equals("1.2.0")).findFirst();

        assertTrue(version100.isPresent(), "Version 1.0.0 should exist (created or already existed)");
        assertTrue(version110.isPresent(), "Version 1.1.0 should exist (created or already existed)");
        assertTrue(version120.isPresent(), "Version 1.2.0 should exist (created or already existed)");

        // Verify version count increased by only the new versions (not duplicates)
        assertEquals(initialVersionCount + expectedNewVersions, versionsAfter.size(),
                "Should have " + expectedNewVersions + " more versions than before (3 requested - " + alreadyExistingCount + " already existing)");

        // Verify AI recognized existing versions in response
        if (alreadyExistingCount > 0) {
            assertTrue(response.toLowerCase().contains("already exist") || response.toLowerCase().contains("already present") ||
                            response.toLowerCase().contains("already created") || response.toLowerCase().contains("duplicate"),
                    "AI should mention that some versions already exist");
            log.info("AI correctly identified existing versions: {}", response);
        }

        // Verify all belong to the same product
        assertEquals(firstProduct.getId(), version100.get().getProductId(),
                "Version 1.0.0 should belong to correct product");
        assertEquals(firstProduct.getId(), version110.get().getProductId(),
                "Version 1.1.0 should belong to correct product");
        assertEquals(firstProduct.getId(), version120.get().getProductId(),
                "Version 1.2.0 should belong to correct product");

        log.info("After request: {} versions exist, successfully created {} new versions",
                versionsAfter.size(), expectedNewVersions);
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void testUpdate(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // Get the second product to add versions to (skip Default product at index 0)
        Product firstProduct = productApi.getAll().get(1);

        {
            log.info("Product ID: {}, Product Name: {}", firstProduct.getId(), firstProduct.getName());
            processQuery("Add a new version with the name 1.0.0-alpa to product " + firstProduct.getName() + ".");
            Optional<Version> misspelledVersion = versionApi.getByName(firstProduct.getId(), "1.0.0-alpa");
            if (misspelledVersion.isEmpty()) {
                Optional<Version> version = versionApi.getByName(firstProduct.getId(), "1.0.0-alpha");
                {
                    processQuery("Please fix the typo in the version you created to 1.0.0-alpha I specifically want alpa.");
                    List<Version>     versions        = versionApi.getAll(firstProduct.getId());
                    Optional<Version> updatedVersion  = versions.stream().filter(v -> v.getName().equals("1.0.0-alpa")).findFirst();
                    Optional<Version> mistypedVersion = versions.stream().filter(v -> v.getName().equals("1.0.0-alpha")).findFirst();
                    assertTrue(updatedVersion.isPresent(), "Version should be renamed");
                    assertTrue(mistypedVersion.isEmpty(), "Mistyped version should not exist");
                    log.info("Updated version ID is: {}.", updatedVersion.get().getId());
                }
            } else {
                {
                    processQuery("Please fix the typo in the version you created to 1.0.0-alpha.");
                    List<Version>     versions        = versionApi.getAll(firstProduct.getId());
                    Optional<Version> updatedVersion  = versions.stream().filter(v -> v.getName().equals("1.0.0-alpha")).findFirst();
                    Optional<Version> mistypedVersion = versions.stream().filter(v -> v.getName().equals("1.0.0-alpa")).findFirst();
                    assertTrue(updatedVersion.isPresent(), "Version should be renamed");
                    assertTrue(mistypedVersion.isEmpty(), "Mistyped version should not exist");
                    log.info("Updated version ID is: {}.", updatedVersion.get().getId());
                }
            }
        }

    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void testUpdateComplexScenario(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // Get the second product (skip Default product at index 0)
        Product firstProduct = productApi.getAll().get(1);

        // Create a version with a typo
        {
            processQuery("Add a new version with the name 2.5.3-RC1 to product " + firstProduct.getName() + ".");
            List<Version>     versions = versionApi.getAll(firstProduct.getId());
            Optional<Version> version  = versions.stream().filter(v -> v.getName().equals("2.5.3-RC1")).findFirst();
            assertTrue(version.isPresent(), "Version should be created");
            log.info("Version 2.5.3-RC1 ID is: {}.", version.get().getId());
        }

        // Update to final release
        {
            processQuery("Please update the version you just created to 2.5.3-final.");
            List<Version>     versions     = versionApi.getAll(firstProduct.getId());
            Optional<Version> finalVersion = versions.stream().filter(v -> v.getName().equals("2.5.3-final")).findFirst();
            Optional<Version> rc1Version   = versions.stream().filter(v -> v.getName().equals("2.5.3-RC1")).findFirst();
            assertTrue(finalVersion.isPresent(), "Version should be updated to final");
            assertTrue(rc1Version.isEmpty(), "RC1 version should not exist anymore");
            assertEquals(firstProduct.getId(), finalVersion.get().getProductId(),
                    "Version should still belong to the same product");
        }
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void testVersionAcrossMultipleProducts(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // Get two different products (skip Default product at index 0)
        List<Product> allProducts = productApi.getAll();
        assertTrue(allProducts.size() >= 3, "Need at least 3 products for this test (including Default)");

        Product firstProduct  = allProducts.get(1);
        Product secondProduct = allProducts.get(2);

        // Add same version name to different products
        processQuery("Add a new version with the name 3.0.0 to product " + firstProduct.getName() + ".");
        processQuery("Add a new version with the name 3.0.0 to product " + secondProduct.getName() + ".");

        List<Version> firstProductVersions  = versionApi.getAll(firstProduct.getId());
        List<Version> secondProductVersions = versionApi.getAll(secondProduct.getId());

        Optional<Version> version1 = firstProductVersions.stream()
                .filter(v -> v.getName().equals("3.0.0")).findFirst();
        Optional<Version> version2 = secondProductVersions.stream()
                .filter(v -> v.getName().equals("3.0.0")).findFirst();

        assertTrue(version1.isPresent(), "Version 3.0.0 should exist in first product");
        assertTrue(version2.isPresent(), "Version 3.0.0 should exist in second product");
        assertNotEquals(version1.get().getId(), version2.get().getId(),
                "Versions should have different IDs");
        assertEquals(firstProduct.getId(), version1.get().getProductId(),
                "First version should belong to first product");
        assertEquals(secondProduct.getId(), version2.get().getProductId(),
                "Second version should belong to second product");
    }
}









