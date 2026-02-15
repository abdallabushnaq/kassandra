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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
public class AiAssistantServiceFeatureTest extends AbstractMcpTest {

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
//        printTables(new String[]{"FEATURES"});

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
            log.info("Feature Galaxxy ID is: {}.", feature.get().getId());
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
