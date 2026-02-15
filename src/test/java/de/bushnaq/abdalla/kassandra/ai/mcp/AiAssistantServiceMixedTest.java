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

import de.bushnaq.abdalla.kassandra.dto.Feature;
import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Version;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Order;
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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for AiAssistantService.processQuery method.
 * Tests are based on scenarios from KassandraIntroductionVideo.
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
public class AiAssistantServiceMixedTest extends AbstractMcpTest {


//    @Order(13)
//    @ParameterizedTest
//    @MethodSource("listRandomCases")
//    @WithMockUser(username = "admin-user", roles = "ADMIN")
//    public void test_13_RenameAllVersionsAndBack(RandomCase randomCase, TestInfo testInfo) throws Exception {
//        init(randomCase, testInfo);
//        List<Version> originalVersions = versionApi.getAll();
//        {
//            processQuery("Please rename all versions by removing the last digit.");
//
//            // Verify versions were renamed (e.g., "1.0.0" -> "1.0")
//            List<Version> renamedVersions = versionApi.getAll();
//            assertEquals(originalVersions.size(), renamedVersions.size(), "Version count should remain the same");
//
//            for (int i = 0; i < originalVersions.size(); i++) {
//                Version originalVersion = originalVersions.get(i);
//                String  originalName    = originalVersion.getName();
//                String  expectedRenamed;
//                if (originalName.lastIndexOf('.') == -1) {
//                    expectedRenamed = originalName;
//                } else {
//                    expectedRenamed = originalName.substring(0, originalName.lastIndexOf('.'));
//                }
//                Version renamedVersion = renamedVersions.stream()
//                        .filter(v -> v.getId().equals(originalVersion.getId()))
//                        .findFirst()
//                        .orElseThrow(() -> new AssertionError("Version with ID " + originalVersion.getId() + " not found"));
//                assertEquals(expectedRenamed, renamedVersion.getName(), "Version name should be renamed correctly for version ID: " + originalVersion.getId());
//            }
//        }
//        {
//            processQuery("Can you rename all versions back how they where before you changed them?");
//            List<Version> renamedVersions = versionApi.getAll();
//            assertEquals(originalVersions.size(), renamedVersions.size(), "Version count should remain the same");
//            for (int i = 0; i < originalVersions.size(); i++) {
//                Version originalVersion = originalVersions.get(i);
//                String  originalName    = originalVersion.getName();
//                String  expectedRenamed = originalName;
//                Version renamedVersion = renamedVersions.stream()
//                        .filter(v -> v.getId().equals(originalVersion.getId()))
//                        .findFirst()
//                        .orElseThrow(() -> new AssertionError("Version with ID " + originalVersion.getId() + " not found"));
//                assertEquals(expectedRenamed, renamedVersion.getName(), "Version name should be renamed correctly for version ID: " + originalVersion.getId());
//            }
//        }
//    }

    @Order(14)
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void test_14_ListProductsWithVersionsAndFeatures(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        String response = processQuery("List all products with their versions and features in a table so that every row has only one feature.");

        List<Product> allProducts = productApi.getAll();
        log.info("Total products in system: {}", allProducts.size());
        List<Version> allVersions = versionApi.getAll();
        log.info("Total versions in system: {}", allVersions.size());
        List<Feature> allFeatures = featureApi.getAll();
        log.info("Total features in system: {}", allFeatures.size());
        allProducts.forEach(product -> assertTrue(response.toLowerCase(Locale.ROOT).contains(product.getName().toLowerCase()), "Product name missing: " + product.getName()));
        allVersions.forEach(version -> assertTrue(response.toLowerCase(Locale.ROOT).contains(version.getName().toLowerCase()), "Version name missing: " + version.getName()));
        allFeatures.forEach(feature -> assertTrue(response.toLowerCase(Locale.ROOT).contains(feature.getName().toLowerCase()), "Feature name missing: " + feature.getName()));
    }

    @Order(15)
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void test_15_ListAllSprints(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // Test query: "List all sprints in a table."
        String response = processQuery("List all sprints in a table.");

        // Verify that the response contains sprint information
        List<Sprint> allSprints = sprintApi.getAll();
        allSprints.forEach(sprint -> assertTrue(response.toLowerCase(Locale.ROOT).contains(sprint.getName().toLowerCase()), "Sprint name missing: " + sprint.getName()));
    }

}
