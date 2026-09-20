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

import de.bushnaq.abdalla.kassandra.dto.ServerSetting;
import de.bushnaq.abdalla.kassandra.dto.ServerSettingTestResult;
import de.bushnaq.abdalla.kassandra.dto.ServerSettingUpdateRequest;
import de.bushnaq.abdalla.kassandra.config.KassandraProperties;
import de.bushnaq.abdalla.kassandra.service.ServerSettingsCatalogue.Keys;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractUiTestUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the REST API boundary for administrator-managed server settings.
 */
@Tag("UnitTest")
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
public class ServerSettingsApiTest extends AbstractUiTestUtil {

    @Autowired
    private ServerSettingsApi serverSettingsApi;

    /**
     * Lists catalogued settings while keeping secret values undisclosed.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void administratorGetsSafeSettingMetadata() {
        ServerSetting secret = serverSettingsApi.getAll().stream()
                .filter(setting -> Keys.OPENAI_API_KEY.equals(setting.getKey()))
                .findFirst()
                .orElseThrow();

        assertTrue(secret.isSecret());
        assertEquals(null, secret.getValue());
        assertTrue(secret.isRestartRequired());
    }

    /**
     * Exposes the default OpenAI-compatible API base URL without exposing its API key.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void administratorGetsOpenAiConnectionDefaults() {
        ServerSetting baseUrl = serverSettingsApi.getAll().stream()
                .filter(setting -> Keys.OPENAI_BASE_URL.equals(setting.getKey()))
                .findFirst()
                .orElseThrow();

        assertEquals("http://localhost:1234", baseUrl.getValue());
        assertTrue(baseUrl.isRestartRequired());
    }

    /**
     * Applies model selections to subsequent AI requests without requiring a restart.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void administratorCanUpdateAssistantModelWithoutRestart() {
        ServerSetting assistantModel = serverSettingsApi.getAll().stream()
                .filter(setting -> Keys.AI_MCP_MODEL.equals(setting.getKey()))
                .findFirst()
                .orElseThrow();

        assertFalse(assistantModel.isRestartRequired());
    }

    /**
     * Rejects log levels that cannot be applied by Spring Boot's logging system.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void administratorCannotSaveInvalidLogLevel() {
        ServerSettingUpdateRequest request = new ServerSettingUpdateRequest();
        request.setValue("VERBOSE");

        assertThrows(Exception.class, () -> serverSettingsApi.update(
                Keys.MCP_TOOL_CALLBACK_LOGGING_LEVEL, request));
    }

    /**
     * Persists a valid setting value through the secured API.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void administratorUpdatesSetting() {
        ServerSettingUpdateRequest request = new ServerSettingUpdateRequest();
        request.setValue("12");

        ServerSetting updated = serverSettingsApi.update(Keys.UNDO_REDO_HISTORY_LIMIT, request);

        assertEquals("12", updated.getValue());
    }

    /**
     * Rejects values outside the catalogue-defined limits.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void administratorCannotSaveInvalidValue() {
        ServerSettingUpdateRequest request = new ServerSettingUpdateRequest();
        request.setValue("0");

        assertThrows(Exception.class, () -> serverSettingsApi.update(Keys.UNDO_REDO_HISTORY_LIMIT, request));
    }

    /**
     * Resets an empty context length to its declared zero value.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void administratorCanResetContextLengthToDefault() {
        ServerSettingUpdateRequest request = new ServerSettingUpdateRequest();
        request.setValue("");
        serverSettingsApi.updateCategoryEnabled("lm-studio", true);

        ServerSetting updated = serverSettingsApi.update(Keys.LM_STUDIO_CONTEXT_LENGTH, request);

        assertEquals("20480", updated.getValue());
    }

    /**
     * Keeps settings in an optional category unavailable until an administrator enables it.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void administratorEnablesOptionalCategoryBeforeUpdatingItsSettings() {
        ServerSetting lmStudio = serverSettingsApi.getAll().stream()
                .filter(setting -> Keys.LM_STUDIO_CONTEXT_LENGTH.equals(setting.getKey()))
                .findFirst()
                .orElseThrow();
        ServerSettingUpdateRequest request = new ServerSettingUpdateRequest();
        request.setValue("4096");

        assertFalse(lmStudio.isCategoryEnabled());
        assertTrue(lmStudio.isCategoryAllowDisable());
        assertThrows(Exception.class, () -> serverSettingsApi.update(lmStudio.getKey(), request));

        serverSettingsApi.updateCategoryEnabled(lmStudio.getCategoryKey(), true);

        assertEquals("4096", serverSettingsApi.update(lmStudio.getKey(), request).getValue());
    }

    /**
     * Delegates connectivity testing to the selected setting definition.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void administratorTestsConnectionUsingSettingDefinition() {
        ServerSettingUpdateRequest request = new ServerSettingUpdateRequest();
        request.setValue("http://127.0.0.1:1");
        serverSettingsApi.updateCategoryEnabled("lm-studio", true);
        serverSettingsApi.updateCategoryEnabled("stable-diffusion", true);

        ServerSettingTestResult lmStudioResult        = serverSettingsApi.test(Keys.LM_STUDIO_API_URL, request);
        ServerSettingTestResult stableDiffusionResult = serverSettingsApi.test(Keys.STABLE_DIFFUSION_API_URL, request);

        assertFalse(lmStudioResult.isSuccessful());
        assertFalse(stableDiffusionResult.isSuccessful());
    }

    /**
     * Runs a setting's runtime update callback after persisting its value.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void administratorUpdatesHolidayLookAheadRuntimeValue() {
        ServerSettingUpdateRequest request = new ServerSettingUpdateRequest();
        request.setValue("36");

        serverSettingsApi.update(Keys.HOLIDAY_LOOK_AHEAD_MONTHS, request);

        assertEquals(36, KassandraProperties.getHolidayLookAheadMonths());
    }

    /**
     * Prevents ordinary users from reading server settings.
     */
    @Test
    @WithMockUser(username = "user", roles = "USER")
    public void regularUserCannotManageSettings() {
        assertThrows(AccessDeniedException.class, () -> serverSettingsApi.getAll());
    }
}
