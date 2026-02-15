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

import de.bushnaq.abdalla.kassandra.dto.User;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
public class AiAssistantServiceUserTest extends AbstractMcpTest {

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testAdd(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        String         response = processQuery("create a new user Ahmet Mustafa, ahmet.mustafa@kassandra.org.");
        Optional<User> user     = userApi.getByEmail("ahmet.mustafa@kassandra.org");
        assertTrue(user.isPresent(), "User should have been created");
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testDelete(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        //---create
        processQuery("create a new user Ahmet Mustafa, ahmet.mustafa@kassandra.org.");
        assertTrue(userApi.getByEmail("ahmet.mustafa@kassandra.org").isPresent(), "User should be created before deletion test");
        log.info("User ID: {}.", userApi.getByEmail("ahmet.mustafa@kassandra.org").get().getId());

        //---delete
        String response = processQuery("Please delete the user you just created.");
        if (userApi.getByEmail("ahmet.mustafa@kassandra.org").isEmpty() && response.toLowerCase().contains("are you sure")) {
            processQuery("I am sure.");
        }
        assertTrue(userApi.getByEmail("ahmet.mustafa@kassandra.org").isEmpty(), "User ' ahmet.mustafa@kassandra.org' should be deleted");

    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testGetAll(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        List<User> allUsers = userApi.getAll();

        String response = processQuery("list all user email addresses.");
        allUsers.forEach(user -> assertTrue(response.toLowerCase(Locale.ROOT).contains(user.getEmail().toLowerCase()), "User email missing: " + user.getEmail()));
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testUpdate(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        User user = userApi.getAll().get(1); // get any user
        log.info("User ID: {}.", user.getId());
        processQuery("update user " + user.getName() + " last name to 'Salam'.");
        {
            User   updatedUser = userApi.getById(user.getId()); // get any user
            String lastName    = updatedUser.getName().substring(updatedUser.getName().indexOf(' ') + 1);
            assertEquals("Salam", lastName, "User last name should be updated to 'Salam'");
        }
    }


}
