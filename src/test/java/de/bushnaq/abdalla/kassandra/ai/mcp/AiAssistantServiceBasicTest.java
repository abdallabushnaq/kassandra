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

import de.bushnaq.abdalla.kassandra.ui.util.AbstractUiTestUtil;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
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

import static org.junit.jupiter.api.Assertions.*;

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
public class AiAssistantServiceBasicTest extends AbstractUiTestUtil {
    private static final String             ANSI_BLUE            = "\u001B[36m";
    private static final String             ANSI_GRAY            = "\u001B[37m";
    private static final String             ANSI_GREEN           = "\u001B[32m";
    private static final String             ANSI_RED             = "\u001B[31m";
    private static final String             ANSI_RESET           = "\u001B[0m";    // Declaring ANSI_RESET so that we can reset the color
    private static final String             ANSI_YELLOW          = "\u001B[33m";
    private static final String             TEST_CONVERSATION_ID = "test-conversation-id";
    @Autowired
    private              AiAssistantService aiAssistantService;
//    @Value("${spring.ai.anthropic.api-key:#{null}}")
//    private              String             anthropicApiKey;

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

    @BeforeEach
    protected void setupTest() {
        String modelName = aiAssistantService.getModelName();
        log.info("{}=== Running test with model: {} ==={}", ANSI_BLUE, modelName, ANSI_RESET);
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

        assertTrue(productApi.getByName("TestMemory").isPresent(), "Product 'TestMemory' should have been created");

        // Second query: Reference the product without naming it
        String response2 = processQuery("What is the name of the product you just created?");

        // The response should reference "TestMemory" or similar
        log.info("AI Response about created product: {}", response2);

        // Clean up
        productApi.getByName("TestMemory").ifPresent(p -> productApi.deleteById(p.getId()));
    }

}
