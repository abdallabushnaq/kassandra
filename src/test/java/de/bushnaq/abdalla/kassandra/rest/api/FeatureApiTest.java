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

import de.bushnaq.abdalla.kassandra.dto.Feature;
import de.bushnaq.abdalla.kassandra.util.AbstractEntityGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerErrorException;

import java.util.List;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class FeatureApiTest extends AbstractEntityGenerator {
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
            addRandomFeature(expectedVersions.getFirst());
        });

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            List<Feature> allFeatures = featureApi.getAll();
        });

        {
            Feature feature = expectedFeatures.getFirst();
            Long    id      = feature.getId();
            String  name    = feature.getName();
            feature.setName(SECOND_NAME);
            try {
                updateFeature(feature);
                fail("should not be able to update");
            } catch (AuthenticationCredentialsNotFoundException e) {
                //restore fields to match db for later tests in @AfterEach
                feature.setName(name);
            }
        }

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            removeFeature(expectedFeatures.get(0).getId());
        });

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            Feature feature = featureApi.getById(expectedFeatures.getFirst().getId());
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
        // Create product and version first
        addRandomProducts(1);

        // Create first feature
        Feature feature1 = addFeature(expectedVersions.get(0), "Feature1");

        try {
            // Try to create a second feature with the same name
            Feature feature2 = addFeature(expectedVersions.get(0), "Feature1");
            fail("Should not be able to create a feature with duplicate name");
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
        removeFeature(expectedFeatures.getFirst().getId());
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteUsingFakeId() throws Exception {
        addRandomProducts(2);
        try {
            removeFeature(FAKE_ID);
        } catch (ServerErrorException e) {
            //expected
        }
    }

    @Test
    public void getAll() throws Exception {
        {
            setUser("admin-user", "ROLE_ADMIN");
            addRandomProducts(3);
            setUser("user", "ROLE_USER");
        }
        List<Feature> allFeatures = featureApi.getAll();
        assertEquals(3, allFeatures.size());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void getAllEmpty() throws Exception {
        List<Feature> allFeatures = featureApi.getAll();
        assertEquals(0, allFeatures.size());
    }

    @Test
    public void getByFakeId() throws Exception {
        {
            setUser("admin-user", "ROLE_ADMIN");
            addRandomProducts(1);
            setUser("user", "ROLE_USER");
        }
        try {
            featureApi.getById(FAKE_ID);
            fail("Feature should not exist");
        } catch (ResponseStatusException e) {
            //expected
        }
    }

    @Test
    public void getById() throws Exception {
        {
            setUser("admin-user", "ROLE_ADMIN");
            addRandomProducts(1);
            setUser("user", "ROLE_USER");
        }
        Feature feature = featureApi.getById(expectedFeatures.getFirst().getId());
        assertFeatureEquals(expectedFeatures.getFirst(), feature, true); // shallow test
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void update() throws Exception {
        addRandomProducts(2);
        Feature feature = expectedFeatures.getFirst();
        feature.setName(SECOND_NAME);
        updateFeature(feature);
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void updateToDuplicateNameFails() throws Exception {
        // Create product and version first
        addRandomProducts(1);

        // Create two features
        Feature feature1 = addFeature(expectedVersions.get(0), "Feature1");
        Feature feature2 = addFeature(expectedVersions.get(0), "Feature2");

        // Try to update feature2 to have the same name as feature1
        String originalName = feature2.getName();
        feature2.setName(feature1.getName());

        try {
            updateFeature(feature2);
            fail("Should not be able to update a feature to have a duplicate name");
        } catch (Exception e) {
            // Expected exception for duplicate name
            assertTrue(e.getMessage().contains("CONFLICT") || e.getMessage().contains("already exists"));
        }

        // Restore original name for cleanup
        feature2.setName(originalName);
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void updateUsingFakeId() throws Exception {
        addRandomProducts(2);
        Feature feature = expectedFeatures.getFirst();
        Long    id      = feature.getId();
        String  name    = feature.getName();
        feature.setId(FAKE_ID);
        feature.setName(SECOND_NAME);
        try {
            updateFeature(feature);
            fail("should not be able to update");
        } catch (ServerErrorException e) {
            //expected
            feature.setId(id);
            feature.setName(name);
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
            addRandomFeature(expectedVersions.getFirst());
        });

        {
            Feature feature = expectedFeatures.getFirst();
            String  name    = feature.getName();
            feature.setName(SECOND_NAME);
            try {
                updateFeature(feature);
                fail("should not be able to update");
            } catch (AccessDeniedException e) {
                //restore fields to match db for later tests in @AfterEach
                feature.setName(name);
            }
        }

        assertThrows(AccessDeniedException.class, () -> {
            removeFeature(expectedFeatures.get(0).getId());
        });
    }
}
