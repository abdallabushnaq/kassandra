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
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AiAssistantService.processQuery method.
 * Tests are based on scenarios from KassandraIntroductionVideo.
 * <table>
 * <tr><td>atlas/intersync-gemma-7b-instruct-function-calling:latest</td><td>...</td></tr>
 * <tr><td>openthinker:7b</td><td>does not support tools</td></tr>
 * <tr><td>ministral-3:8b</td><td>fails one test</td></tr>
 * <tr><td>deepseek-r1:8b</td><td>does not support tools</td></tr>
 * <tr><td>deepseek-coder:latest</td><td>does not support tools</td></tr>
 * <tr><td>mistral-nemo</td><td>does not support tools</td></tr>
 * </table>
 *
 *
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
public class AiAssistantServiceTest extends AbstractUiTestUtil {
    private static final String             ANSI_BLUE            = "\u001B[36m";
    private static final String             ANSI_GRAY            = "\u001B[37m";
    private static final String             ANSI_GREEN           = "\u001B[32m";
    private static final String             ANSI_RED             = "\u001B[31m";
    private static final String             ANSI_RESET           = "\u001B[0m";    // Declaring ANSI_RESET so that we can reset the color
    private static final String             ANSI_YELLOW          = "\u001B[33m";
    private static final String             TEST_CONVERSATION_ID = "test-conversation-id";
    private static final String             TEST_MODEL_NAME      = "atlas/intersync-gemma-7b-instruct-function-calling:latest";
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String         username       = authentication.getName();
        log.info("{}{}: {}{}{}", ANSI_YELLOW, username, ANSI_BLUE, query, ANSI_RESET);
        QueryResult result    = aiAssistantService.processQueryWithThinking(query, TEST_CONVERSATION_ID);
        String      modelName = aiAssistantService.getModelName();
        String      response  = result.content();
        if (result.hasThinking()) {
            log.info("{}--- Thinking Process ---{}n{}{}{}n{}--- End of Thinking Process ---{}", ANSI_GRAY, ANSI_RESET, ANSI_GRAY, result.thinking(), ANSI_RESET, ANSI_GRAY, ANSI_RESET);
        }
        log.info("{}{}: {}{}{}", ANSI_YELLOW, modelName, ANSI_GREEN, response, ANSI_RESET);
        assertNotNull(response, "Response should not be null");
        return response;
    }

    /**
     * Override the chat model to use deepseek-coder-v2:latest for tests.
     * This runs before each test method.
     */
    @BeforeEach
    protected void setupTestModel() {
        log.info("{}=== Configuring test model: {} ==={}", ANSI_BLUE, TEST_MODEL_NAME, ANSI_RESET);

        // Create a custom OllamaChatModel with the test model
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl("http://localhost:11434")
                .build();

        OllamaChatModel testChatModel = OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaChatOptions.builder()
                        .model(TEST_MODEL_NAME)
                        .temperature(0.1)
                        .topP(0.98)
                        .build())
                .build();

        // Use setter to inject the test model into the service
        aiAssistantService.setChatModel(testChatModel);

        log.info("{}✓ Test model configured successfully{}", ANSI_GREEN, ANSI_RESET);
    }

    @Test
    @Order(1)
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void test_01_GetModelName() {
        String modelName = aiAssistantService.getModelName();

        assertNotNull(modelName, "Model name should not be null");
        assertFalse(modelName.isEmpty(), "Model name should not be empty");

        log.info("AI Model Name: {}", modelName);
    }

    @Test
    @Order(2)
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void test_02_GetAvailableTools() {
        String availableTools = aiAssistantService.getAvailableTools();

        assertNotNull(availableTools, "Available tools list should not be null");
        assertFalse(availableTools.isEmpty(), "Available tools list should not be empty");

        // Verify it contains some expected tool categories
        assertTrue(availableTools.contains("Product") || availableTools.contains("product"),
                "Should contain Product tools");
        assertTrue(availableTools.contains("Version") || availableTools.contains("version"),
                "Should contain Version tools");
        assertTrue(availableTools.contains("Version") || availableTools.contains("feature"),
                "Should contain Feature tools");
        assertTrue(availableTools.contains("Version") || availableTools.contains("user"),
                "Should contain User tools");
        assertTrue(availableTools.contains("Version") || availableTools.contains("sprint"),
                "Should contain Sprint tools");

        log.info("Available AI Tools:\n{}", availableTools);
    }

    @Test
    @Order(3)
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void test_03_ClearConversation() {
        // Setup: Add products
        addRandomProducts(1);

        // First conversation
        String response1 = processQuery("List all products.");

        // Clear conversation
        aiAssistantService.clearConversation(TEST_CONVERSATION_ID);

        // Try to reference previous context - should not remember
        String response2 = processQuery("What did I ask you before?");
        log.info("AI Response after clearing conversation: {}", response2);
    }

    @Test
    @Order(4)
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void test_04_ConversationMemory() {
        // Test that the AI remembers previous context

        // First query: Create a product
        String response1 = processQuery("Add a new product called TestMemory.");

        // Second query: Reference the product without naming it
        String response2 = processQuery("What is the name of the product you just created?");

        // The response should reference "TestMemory" or similar
        log.info("AI Response about created product: {}", response2);

        // Clean up
        productApi.getByName("TestMemory").ifPresent(p -> productApi.deleteById(p.getId()));
    }

    @Order(10)
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void test_10_AddProduct(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        {
            processQuery("Add a new product with the name Andromsda.");
            assertTrue(productApi.getByName("Andromsda").isPresent(), "Product should be created");
        }
    }

    @Order(11)
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void test_11_AddProductWithTypoAndRename(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        {
            processQuery("Add a new product with the name Andromsda.");
            assertTrue(productApi.getByName("Andromsda").isPresent(), "Product should be created");
        }

        {
            processQuery("Please fix the typo in the product you created to Andromeda.");
            assertTrue(productApi.getByName("Andromeda").isPresent(), "Product should be renamed");
            assertTrue(productApi.getByName("Andromsda").isEmpty(), "Mistyped product should not exist");
        }
    }

    @Order(12)
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void test_12_DeleteCreatedProduct(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        processQuery("Add a new product with the name Andromeda.");

        // Wait for product to be created
        Optional<Product> product = productApi.getByName("Andromeda");
        assertTrue(product.isPresent(), "Product should be created before deletion test");

        processQuery("Please delete the product you created.");

        // Verify the product was deleted
        Optional<Product> deletedProduct = productApi.getByName("Andromeda");
        assertFalse(deletedProduct.isPresent(), "Product 'Andromeda' should be deleted");

    }

    @Order(13)
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void test_13_RenameAllVersionsAndBack(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        List<Version> originalVersions = versionApi.getAll();
        {
            processQuery("Please rename all versions by removing the last digit.");

            // Verify versions were renamed (e.g., "1.0.0" -> "1.0")
            List<Version> renamedVersions = versionApi.getAll();
            assertEquals(originalVersions.size(), renamedVersions.size(), "Version count should remain the same");

            for (int i = 0; i < originalVersions.size(); i++) {
                Version originalVersion = originalVersions.get(i);
                String  originalName    = originalVersion.getName();
                String  expectedRenamed;
                if (originalName.lastIndexOf('.') == -1) {
                    expectedRenamed = originalName;
                } else {
                    expectedRenamed = originalName.substring(0, originalName.lastIndexOf('.'));
                }
                Version renamedVersion = renamedVersions.stream()
                        .filter(v -> v.getId().equals(originalVersion.getId()))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("Version with ID " + originalVersion.getId() + " not found"));
                assertEquals(expectedRenamed, renamedVersion.getName(), "Version name should be renamed correctly for version ID: " + originalVersion.getId());
            }
        }
        {
            processQuery("Can you rename all versions back how they where before you changed them?");
            List<Version> renamedVersions = versionApi.getAll();
            assertEquals(originalVersions.size(), renamedVersions.size(), "Version count should remain the same");
            for (int i = 0; i < originalVersions.size(); i++) {
                Version originalVersion = originalVersions.get(i);
                String  originalName    = originalVersion.getName();
                String  expectedRenamed = originalName;
                Version renamedVersion = renamedVersions.stream()
                        .filter(v -> v.getId().equals(originalVersion.getId()))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("Version with ID " + originalVersion.getId() + " not found"));
                assertEquals(expectedRenamed, renamedVersion.getName(), "Version name should be renamed correctly for version ID: " + originalVersion.getId());
            }
        }
    }

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
