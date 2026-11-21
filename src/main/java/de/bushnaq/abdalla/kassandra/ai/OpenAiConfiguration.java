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

package de.bushnaq.abdalla.kassandra.ai;

import de.bushnaq.abdalla.kassandra.service.ServerSettingsService;
import de.bushnaq.abdalla.kassandra.service.ServerSettingsCatalogue.Keys;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Creates the OpenAI-compatible chat client from persisted administrator settings.
 */
@Configuration
public class OpenAiConfiguration {

    @Autowired
    private ServerSettingsService serverSettingsService;

    /**
     * Creates the chat model used by Kassandra's AI features.
     *
     * @param observationRegistry               observation registry provider
     * @param observationConvention             observation convention provider
     * @return configured OpenAI chat model
     */
    @Bean
    public OpenAiChatModel openAiChatModel(ObjectProvider<ObservationRegistry> observationRegistry,
                                           ObjectProvider<ChatModelObservationConvention> observationConvention) {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .options(defaultChatOptions())
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .build();
        observationConvention.ifAvailable(chatModel::setObservationConvention);
        return chatModel;
    }

    private OpenAiChatOptions defaultChatOptions() {
        return OpenAiChatOptions.builder()
                .baseUrl(value(Keys.OPENAI_BASE_URL).replaceAll("/+$", "") + "/v1")
                .apiKey(serverSettingsService.secretValue(Keys.OPENAI_API_KEY))
                .timeout(Duration.ofMinutes(10))
                .model(value(Keys.AI_MCP_MODEL))
                .temperature(Double.parseDouble(value(Keys.AI_TEMPERATURE)))
                .maxTokens(integer(Keys.AI_MAX_TOKENS))
                .seed(integer(Keys.AI_SEED))
                .build();
    }

    private int integer(String key) {
        return Integer.parseInt(value(key));
    }

    private String value(String key) {
        return serverSettingsService.value(key);
    }
}
