/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package de.bushnaq.abdalla.kassandra.rest.api;

import de.bushnaq.abdalla.kassandra.dto.AvatarWrapper;
import de.bushnaq.abdalla.kassandra.rest.controller.AvatarProxyController;
import de.bushnaq.abdalla.kassandra.rest.controller.DarkAvatarProxyController;
import de.bushnaq.abdalla.kassandra.util.AbstractTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies the light and dark frontend image proxy routes.
 */
@Tag("UnitTest")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
class AvatarProxyApiTest extends AbstractTestUtil {
    private static final UUID   ID    = UUID.fromString("6d918564-1ae0-4b79-9e03-fbcbd7b8dd09");
    private static final byte[] IMAGE = {1, 2, 3};

    @Autowired
    private MockMvc    applicationMvc;
    private FeatureApi featureApi;
    private MockMvc    mvc;
    private ProductApi productApi;
    private SprintApi  sprintApi;
    private UserApi    userApi;

    @BeforeEach
    void setUpProxies() {
        featureApi = mock(FeatureApi.class);
        productApi = mock(ProductApi.class);
        sprintApi  = mock(SprintApi.class);
        userApi    = mock(UserApi.class);
        mvc        = MockMvcBuilders.standaloneSetup(
                new AvatarProxyController(featureApi, productApi, sprintApi, userApi),
                new DarkAvatarProxyController(featureApi, productApi, sprintApi, userApi)).build();
    }

    @ParameterizedTest
    @CsvSource({
            "false, feature", "false, feature-header", "false, product", "false, product-header",
            "false, sprint", "false, sprint-header", "false, user",
            "true, feature", "true, feature-header", "true, product", "true, product-header",
            "true, sprint", "true, sprint-header", "true, user"
    })
    void servesImagesWithPngAndCacheHeaders(boolean dark, String resource) throws Exception {
        stubImage(dark, resource, new AvatarWrapper(IMAGE));

        mvc.perform(get(path(dark, resource)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("Cache-Control", "public, max-age=31536000, immutable"))
                .andExpect(content().bytes(IMAGE));
    }

    @ParameterizedTest
    @CsvSource({
            "false, feature", "false, feature-header", "false, product", "false, product-header",
            "false, sprint", "false, sprint-header", "false, user",
            "true, feature", "true, feature-header", "true, product", "true, product-header",
            "true, sprint", "true, sprint-header", "true, user"
    })
    void returnsNotFoundForMissingWrapperOrImage(boolean dark, String resource) throws Exception {
        stubImage(dark, resource, null);
        mvc.perform(get(path(dark, resource))).andExpect(status().isNotFound());

        stubImage(dark, resource, new AvatarWrapper(null));
        mvc.perform(get(path(dark, resource))).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    void frontendProxyRoutesAreAccessibleWithoutAuthentication(boolean dark) throws Exception {
        applicationMvc.perform(get(path(dark, "user").replace(ID.toString(), "not-a-uuid")).with(anonymous()))
                .andExpect(status().isBadRequest());
    }

    private String path(boolean dark, String resource) {
        return "/frontend/" + (dark ? "dark-avatar-proxy" : "avatar-proxy") + "/" + resource + "/" + ID;
    }

    private void stubImage(boolean dark, String resource, AvatarWrapper image) {
        switch (resource) {
            case "feature" -> {
                if (dark) when(featureApi.getDarkAvatarImage(ID)).thenReturn(image);
                else when(featureApi.getAvatarImage(ID)).thenReturn(image);
            }
            case "feature-header" -> {
                if (dark) when(featureApi.getDarkHeaderImage(ID)).thenReturn(image);
                else when(featureApi.getHeaderImage(ID)).thenReturn(image);
            }
            case "product" -> {
                if (dark) when(productApi.getDarkAvatarImage(ID)).thenReturn(image);
                else when(productApi.getAvatarImage(ID)).thenReturn(image);
            }
            case "product-header" -> {
                if (dark) when(productApi.getDarkHeaderImage(ID)).thenReturn(image);
                else when(productApi.getHeaderImage(ID)).thenReturn(image);
            }
            case "sprint" -> {
                if (dark) when(sprintApi.getDarkAvatarImage(ID)).thenReturn(image);
                else when(sprintApi.getAvatarImage(ID)).thenReturn(image);
            }
            case "sprint-header" -> {
                if (dark) when(sprintApi.getDarkHeaderImage(ID)).thenReturn(image);
                else when(sprintApi.getHeaderImage(ID)).thenReturn(image);
            }
            case "user" -> {
                if (dark) when(userApi.getDarkAvatarImage(ID)).thenReturn(image);
                else when(userApi.getAvatarImage(ID)).thenReturn(image);
            }
            default -> throw new IllegalArgumentException("Unknown image resource: " + resource);
        }
    }
}
