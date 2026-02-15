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
import de.bushnaq.abdalla.kassandra.ui.util.AbstractUiTestUtil;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import de.bushnaq.abdalla.profiler.TimeKeeping;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertNotNull;
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
public class AiAssistantServiceMixedTest extends AbstractUiTestUtil {
    private static final String             ANSI_BLUE            = "\u001B[36m";
    private static final String             ANSI_GRAY            = "\u001B[37m";
    private static final String             ANSI_GREEN           = "\u001B[32m";
    private static final String             ANSI_RED             = "\u001B[31m";
    private static final String             ANSI_RESET           = "\u001B[0m";    // Declaring ANSI_RESET so that we can reset the color
    private static final String             ANSI_YELLOW          = "\u001B[33m";
    private static final String             TEST_CONVERSATION_ID = "test-conversation-id";
    @Autowired
    private              AiAssistantService aiAssistantService;

    private void init(RandomCase randomCase, TestInfo testInfo) throws Exception {
        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        generateProductsIfNeeded(testInfo, randomCase);
    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                new RandomCase(3, OffsetDateTime.parse("2025-08-11T08:00:00+01:00"), LocalDate.parse("2025-08-04"), Duration.ofDays(10), 2, 2, 2, 2, 2, 2, 1, 5, 5, 8, 8, 6, 7)//
        };
        return Arrays.stream(randomCases).toList();
    }

    private String processQuery(String query) {
        try (TimeKeeping t = new TimeKeeping()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String         username       = authentication.getName();
            log.info("{}{}: {}{}{}", ANSI_YELLOW, username, ANSI_BLUE, query, ANSI_RESET);
            QueryResult result    = aiAssistantService.processQueryWithThinking(query, TEST_CONVERSATION_ID);
            String      modelName = aiAssistantService.getModelName();
            String      response  = result.content();
            if (result.hasThinking()) {
                log.info("{}--- Thinking Process ---{}n{}{}{}n{}--- End of Thinking Process ---{}", ANSI_GRAY, ANSI_RESET, ANSI_GRAY, result.thinking(), ANSI_RESET, ANSI_GRAY, ANSI_RESET);
            }
            log.info("({}){}{}: {}{}{}", ANSI_YELLOW, t.getDelta().getNano() / 1000, modelName, ANSI_GREEN, response, ANSI_RESET);
            assertNotNull(response, "Response should not be null");
            return response;
        }
    }

    @BeforeEach
    protected void setupTest() {
        String modelName = aiAssistantService.getModelName();
        log.info("{}=== Running test with model: {} ==={}", ANSI_BLUE, modelName, ANSI_RESET);
    }

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
