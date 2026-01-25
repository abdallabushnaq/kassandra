/*
 *
 * Copyright (C) 2025-2025 Abdalla Bushnaq
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

package de.bushnaq.abdalla.kassandra.rest.api;

import de.bushnaq.abdalla.kassandra.dto.Version;
import de.bushnaq.abdalla.kassandra.util.AbstractEntityGenerator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerErrorException;

import java.util.List;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.*;


@Tag("UnitTest")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
public class VersionApiTest extends AbstractEntityGenerator {
    private static final long   FAKE_ID     = 999999L;
    private static final String SECOND_NAME = "SECOND_NAME";

    @Test
    public void anonymousSecurity() {
        {
            setUser("admin-user", "ROLE_ADMIN");
            addRandomProducts(1);
            SecurityContextHolder.clearContext();
        }

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            addRandomVersion(expectedProducts.getFirst());
        });

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            List<Version> allVersions = versionApi.getAll();
        });
        {
            Version version = expectedVersions.getFirst();
            String  name    = version.getName();
            version.setName(SECOND_NAME);
            try {
                updateVersion(version);
                fail("should not be able to update");
            } catch (AuthenticationCredentialsNotFoundException e) {
                //restore fields to match db for later tests in @AfterEach
                version.setName(name);
            }
        }

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            removeVersion(expectedVersions.get(0).getId());
        });

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            Version version = versionApi.getById(expectedVersions.getFirst().getId());
        });
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void create() throws Exception {
        addRandomProducts(1);
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void createDuplicateNameFails() throws Exception {
        // Create product first
        addRandomProducts(1);

        // Create first version
        Version version1 = addVersion(expectedProducts.get(0), "Version1");

        try {
            // Try to create a second version with the same name
            Version version2 = addVersion(expectedProducts.get(0), "Version1");
            fail("Should not be able to create a version with duplicate name");
        } catch (Exception e) {
            // Expected exception for duplicate name
            assertTrue(e.getMessage().contains("CONFLICT") || e.getMessage().contains("already exists"));
        }
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void delete() throws Exception {
        //create the users
        addRandomProducts(2);
        removeVersion(expectedVersions.getFirst().getId());
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteUsingFakeId() throws Exception {
        addRandomProducts(2);
        try {
            removeVersion(FAKE_ID);
        } catch (ServerErrorException e) {
            //expected
        }
    }

    @Test
    public void getAll() throws Exception {
        {
            setUser("admin-user", "ROLE_ADMIN");
            addRandomProducts(3);
            List<Version> allVersions = versionApi.getAll();
            assertEquals(1 + 3, allVersions.size());// including the "Default" Version
        }
        {
            setUser("user", "ROLE_USER");
            List<Version> allVersions = versionApi.getAll();
            assertEquals(0, allVersions.size());// no acl
        }
    }

    @Test
    public void getAllByProductId() throws Exception {
        setUser("admin-user", "ROLE_ADMIN");
        addRandomProducts(3);
        List<Version> allVersions = versionApi.getAll(expectedProducts.getFirst().getId());
        assertEquals(1, allVersions.size());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void getAllEmpty() throws Exception {
        List<Version> allVersions = versionApi.getAll();
        assertEquals(0, allVersions.size());
    }

    @Test
    public void getByFakeId() throws Exception {
        setUser("admin-user", "ROLE_ADMIN");
        addRandomProducts(1);
        try {
            versionApi.getById(FAKE_ID);
            fail("Version should not exist");
        } catch (ResponseStatusException e) {
            //expected
        }
    }

    @Test
    public void getById() throws Exception {
        setUser("admin-user", "ROLE_ADMIN");
        addRandomProducts(1);
        Version version = versionApi.getById(expectedVersions.getFirst().getId());
        assertEquals(expectedVersions.getFirst().getId(), version.getId());
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void update() throws Exception {
        addRandomProducts(2);
        Version version = expectedVersions.getFirst();
        version.setName(SECOND_NAME);
        updateVersion(version);
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void updateToDuplicateNameFails() throws Exception {
        // Create product first
        addRandomProducts(1);

        // Create two versions
        Version version1 = addVersion(expectedProducts.get(0), "Version1");
        Version version2 = addVersion(expectedProducts.get(0), "Version2");

        // Try to update version2 to have the same name as version1
        String originalName = version2.getName();
        version2.setName(version1.getName());

        try {
            updateVersion(version2);
            fail("Should not be able to update a version to have a duplicate name");
        } catch (ResponseStatusException e) {
            // Expected exception for duplicate name
            assertTrue(e.getMessage().contains("CONFLICT") || e.getMessage().contains("already exists"));
        }

        // Restore original name for cleanup
        version2.setName(originalName);
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void updateUsingFakeId() throws Exception {
        addRandomProducts(2);
        Version version = expectedVersions.getFirst();
        Long    id      = version.getId();
        String  name    = version.getName();
        version.setId(FAKE_ID);
        version.setName(SECOND_NAME);
        try {
            updateVersion(version);
            fail("should not be able to update");
        } catch (ServerErrorException e) {
            //restore fields to match db for later tests in @AfterEach
            version.setId(id);
            version.setName(name);
        }
    }

    @Test
    public void userSecurity() {
        {
            setUser("admin-user", "ROLE_ADMIN");
            addRandomProducts(1);
            setUser("user", "ROLE_USER");
        }

        assertThrows(AccessDeniedException.class, () -> {
            addRandomVersion(expectedProducts.getFirst());
        });

        {
            Version testVersion = expectedVersions.getFirst();
            String  name        = testVersion.getName();
            testVersion.setName(SECOND_NAME);
            try {
                updateVersion(testVersion);
            } catch (AccessDeniedException e) {
                //restore fields to match db for later tests in @AfterEach
                testVersion.setName(name);
            }
        }

        assertThrows(AccessDeniedException.class, () -> {
            removeVersion(expectedVersions.get(0).getId());
        });
    }
}
