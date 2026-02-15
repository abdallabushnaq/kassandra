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
import de.bushnaq.abdalla.kassandra.dto.Version;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractUiTestUtil;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AiAssistantService.processQuery method for Feature operations.
 * Tests are similar to AiAssistantServiceProductTest but for features.
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
public class AiAssistantServiceFeatureTest extends AbstractUiTestUtil {
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

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void testAdd(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // Get the first product and version to add features to
        Version firstVersion = versionApi.getAll().get(1);
        Product product      = productApi.getById(firstVersion.getProductId());

        {
            processQuery("Add a new feature with the name Galaxxy to version " + firstVersion.getName() + " of product " + product.getName() + ".");
            List<Feature>     features = featureApi.getAll(firstVersion.getId());
            Optional<Feature> feature  = features.stream().filter(f -> f.getName().equals("Galaxxy")).findFirst();
            assertTrue(feature.isPresent(), "Feature should be created");
            assertFalse(feature.get().getAvatarHash().isEmpty(), "Feature avatar hash should not be empty");
        }
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testDelete(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // Get the first product and version to add features to
        Version firstVersion = versionApi.getAll().get(1);
        Product product      = productApi.getById(firstVersion.getProductId());

        //---add
        processQuery("Add a new feature with the name Galaxy to version " + firstVersion.getName() + " of product " + product.getName() + ".");
        List<Feature>     features = featureApi.getAll(firstVersion.getId());
        Optional<Feature> feature  = features.stream().filter(f -> f.getName().equalsIgnoreCase("Galaxy")).findFirst();
        assertTrue(feature.isPresent(), "Feature should be created before deletion test");
        log.info("Features ID: {}.", feature.get().getId());
//        printTables();

        //---delete
        processQuery("Please delete the feature you just created.");
        features = featureApi.getAll(firstVersion.getId());
        Optional<Feature> deletedFeature = features.stream().filter(f -> f.getName().equals("Galaxy")).findFirst();
//        printTables();
        assertFalse(deletedFeature.isPresent(), "Feature 'Galaxy' should be deleted");
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void testGet(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // Get the first product and version to list features from
        Version       firstVersion = versionApi.getAll().get(1);
        Product       product      = productApi.getById(firstVersion.getProductId());
        List<Feature> allFeatures  = featureApi.getAll(firstVersion.getId());

        String response = processQuery("list all features for version " + firstVersion.getName() + " of product " + product.getName() + ".");
        allFeatures.forEach(feature -> assertTrue(response.toLowerCase(Locale.ROOT).contains(feature.getName().toLowerCase()), "Feature name missing: " + feature.getName()));
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void testUpdate(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // Get the first product and version to add features to
        Version firstVersion = versionApi.getAll().get(1);
        Product product      = productApi.getById(firstVersion.getProductId());

        {
            processQuery("Add a new feature with the name Galaxxy to version " + firstVersion.getName() + " of product " + product.getName() + ".");
            List<Feature>     features = featureApi.getAll(firstVersion.getId());
            Optional<Feature> feature  = features.stream().filter(f -> f.getName().equals("Galaxxy")).findFirst();
            assertTrue(feature.isPresent(), "Feature should be created");
            log.info("Features ID: {}.", feature.get().getId());
        }

        {
            processQuery("Please fix the typo in the feature you created to Galaxy.");
            List<Feature>     features        = featureApi.getAll(firstVersion.getId());
            Optional<Feature> updatedFeature  = features.stream().filter(f -> f.getName().equals("Galaxy")).findFirst();
            Optional<Feature> mistypedFeature = features.stream().filter(f -> f.getName().equals("Galaxxy")).findFirst();
            assertTrue(updatedFeature.isPresent(), "Feature should be renamed");
            assertTrue(mistypedFeature.isEmpty(), "Mistyped feature should not exist");
        }
    }
}
