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

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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
public class AiAssistantServiceBasicTest extends AbstractMcpTest {

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
