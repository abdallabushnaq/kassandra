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
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Creates the OpenAI-compatible chat client from persisted administrator settings.
 */
@Configuration
public class OpenAiConfiguration {

    @Autowired
    private ServerSettingsService serverSettingsService;

    /**
     * Creates the low-level OpenAI-compatible API client during application startup.
     *
     * @param restClientBuilderProvider configured REST client builder provider
     * @param webClientBuilderProvider  configured reactive web client builder provider
     * @param responseErrorHandler      configured HTTP response-error handler provider
     * @return configured OpenAI API client
     */
    @Bean
    public OpenAiApi openAiApi(ObjectProvider<RestClient.Builder> restClientBuilderProvider,
                               ObjectProvider<WebClient.Builder> webClientBuilderProvider,
                               ObjectProvider<ResponseErrorHandler> responseErrorHandler) {
        return OpenAiApi.builder()
                .baseUrl(value(Keys.OPENAI_BASE_URL))
                .apiKey(serverSettingsService.secretValue(Keys.OPENAI_API_KEY))
                .restClientBuilder(restClientBuilderProvider.getIfAvailable(RestClient::builder))
                .webClientBuilder(webClientBuilderProvider.getIfAvailable(WebClient::builder))
                .responseErrorHandler(responseErrorHandler.getIfAvailable(() -> RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER))
                .build();
    }

    /**
     * Creates the chat model used by Kassandra's AI features.
     *
     * @param openAiApi                         low-level OpenAI-compatible API client
     * @param toolCallingManager                tool invocation manager
     * @param retryTemplate                     retry template provider
     * @param observationRegistry               observation registry provider
     * @param observationConvention             observation convention provider
     * @param toolExecutionEligibilityPredicate tool eligibility predicate provider
     * @return configured OpenAI chat model
     */
    @Bean
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi, ToolCallingManager toolCallingManager,
                                           ObjectProvider<RetryTemplate> retryTemplate,
                                           ObjectProvider<ObservationRegistry> observationRegistry,
                                           ObjectProvider<ChatModelObservationConvention> observationConvention,
                                           ObjectProvider<ToolExecutionEligibilityPredicate> toolExecutionEligibilityPredicate) {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(defaultChatOptions())
                .toolCallingManager(toolCallingManager)
                .toolExecutionEligibilityPredicate(
                        toolExecutionEligibilityPredicate.getIfUnique(DefaultToolExecutionEligibilityPredicate::new))
                .retryTemplate(retryTemplate.getIfUnique(() -> RetryUtils.DEFAULT_RETRY_TEMPLATE))
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .build();
        observationConvention.ifAvailable(chatModel::setObservationConvention);
        return chatModel;
    }

    private OpenAiChatOptions defaultChatOptions() {
        return OpenAiChatOptions.builder()
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
