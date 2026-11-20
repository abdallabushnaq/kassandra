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

import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.rest.controller.OverviewController;
import de.bushnaq.abdalla.kassandra.rest.dto.overview.SprintOverviewDto;
import de.bushnaq.abdalla.kassandra.service.SprintsOverviewService;
import de.bushnaq.abdalla.kassandra.util.AbstractTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies sprint overview request parameters and the API security boundary.
 */
@Tag("UnitTest")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
class OverviewApiTest extends AbstractTestUtil {
    @Autowired
    private MockMvc                applicationMvc;
    private MockMvc                mvc;
    private SprintsOverviewService service;

    @BeforeEach
    void setUpController() {
        service = mock(SprintsOverviewService.class);
        OverviewController controller = new OverviewController();
        ReflectionTestUtils.setField(controller, "sprintsOverviewService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        SprintOverviewDto result = new SprintOverviewDto();
        result.meta.laneCount = 17;
        when(service.getOverview(any(LocalDateTime.class), nullable(Integer.class), anyBoolean())).thenReturn(result);
    }

    @Test
    void defaultsToLightThemeAndNoLimit() throws Exception {
        LocalDateTime before = ParameterOptions.getLocalNow();
        mvc.perform(get("/api/overview/sprints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.laneCount").value(17));
        LocalDateTime after = ParameterOptions.getLocalNow();

        ArgumentCaptor<LocalDateTime> now = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(service).getOverview(now.capture(), isNull(), eq(false));
        assertFalse(now.getValue().isBefore(before));
        assertFalse(now.getValue().isAfter(after));
    }

    @ParameterizedTest
    @CsvSource({"dark, true", "DARK, true", "light, false", "other, false"})
    void forwardsLimitAndRecognizesDarkTheme(String theme, boolean dark) throws Exception {
        mvc.perform(get("/api/overview/sprints").param("limitMonths", "6").param("theme", theme))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.laneCount").value(17));
        verify(service).getOverview(any(LocalDateTime.class), eq(6), eq(dark));
    }

    @Test
    void rejectsNonNumericLimit() throws Exception {
        mvc.perform(get("/api/overview/sprints").param("limitMonths", "invalid"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void apiRequiresAuthentication() throws Exception {
        applicationMvc.perform(get("/api/overview/sprints").with(anonymous()))
                .andExpect(status().isUnauthorized());
        String response = applicationMvc.perform(get("/api/overview/sprints").with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(response.contains("\"lanes\""));
    }
}
