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

import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
public class AiAssistantServiceProductTest extends AbstractMcpTest {

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void testAdd(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        //add
        processQuery("Add a new product with the name Andromsda.");
        if (productApi.getByName("Andromsda").isEmpty()) {
            processQuery("You mistyped the product name, it should be Andromsda.");
        }
        assertTrue(productApi.getByName("Andromsda").isPresent(), "Product should be created");
        assertFalse(productApi.getByName("Andromsda").get().getAvatarHash().isEmpty(), "Product avatar URL should not be empty");
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testDelete(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        //---create
        processQuery("Add a new product with the name Andromeda.");
        assertTrue(productApi.getByName("Andromeda").isPresent(), "Product should be created before deletion test");
        log.info("Product ID: {}.", productApi.getByName("Andromeda").get().getId());

        //---delete
        String response = processQuery("Please delete the product you created.");
        if (productApi.getByName("Andromeda").isEmpty() && (response.toLowerCase().contains("are you sure") || response.toLowerCase().contains("would you like me to delete it now?"))) {
            processQuery("I am sure.");
        }
        assertTrue(productApi.getByName("Andromeda").isEmpty(), "Product 'Andromeda' should be deleted");

    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void testGet(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        //---get all
        String response = processQuery("list all products.");
        productApi.getAll().forEach(product -> assertTrue(response.toLowerCase(Locale.ROOT).contains(product.getName().toLowerCase()), "Product name missing: " + product.getName()));
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void testUpdate(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        //---create
        processQuery("Add a new product with the name Andromsda.");
        Optional<Product> product = productApi.getByName("Andromsda");
        if (productApi.getByName("Andromsda").isPresent()) {
            log.info("Product ID: {}.", productApi.getByName("Andromsda").get().getId());
            processQuery("Please fix the name in the product you created to Andromsda.");
        } else {
            log.info("Product ID: {}.", productApi.getByName("Andromeda").get().getId());
            //---update
            processQuery("Please fix the typo in the product you created to Andromeda.");
            assertTrue(productApi.getByName("Andromeda").isPresent(), "Product should be renamed");
            assertTrue(productApi.getByName("Andromsda").isEmpty(), "Mistyped product should not exist");
        }
    }
}
