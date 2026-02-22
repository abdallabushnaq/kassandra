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

import de.bushnaq.abdalla.kassandra.ui.util.AbstractUiTestUtil;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import de.bushnaq.abdalla.profiler.TimeKeeping;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static de.bushnaq.abdalla.util.AnsiColorConstants.ANSI_BLUE;
import static de.bushnaq.abdalla.util.AnsiColorConstants.ANSI_RESET;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class AbstractMcpTest extends AbstractUiTestUtil {
    protected static final String             TEST_CONVERSATION_ID = "test-conversation-id";
    @Autowired
    protected              AiAssistantService aiAssistantService;

    protected void init(RandomCase randomCase, TestInfo testInfo) throws Exception {
        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        generateProductsIfNeeded(testInfo, randomCase);
    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                new RandomCase(3, OffsetDateTime.parse("2025-08-11T08:00:00+01:00"), LocalDate.parse("2025-08-04"), Duration.ofDays(10), 2, 2, 2, 2, 2, 2, 1, 5, 5, 8, 8, 6, 7)//
        };
        return Arrays.stream(randomCases).toList();
    }

    protected String processQuery(String query) {
        try (TimeKeeping t = new TimeKeeping()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String         username       = authentication.getName();
//            log.info("{}{}: {}{}{}", ANSI_YELLOW, username, ANSI_BLUE, query, ANSI_RESET);
            QueryResult result    = aiAssistantService.processQueryWithThinking(username, query, TEST_CONVERSATION_ID);
            String      modelName = aiAssistantService.getModelName();
            String      response  = result.content();
//            if (result.hasThinking()) {
//                for (ThinkingStep step : result.thinkingSteps()) {
//                    log.info("{}Tool{}{}: {}{}{}", ANSI_GRAY, step.toolName(), ANSI_RESET, ANSI_DARK_GRAY, step.agentThinking().innerThought(), ANSI_RESET);
//                }
//            }
//            log.info("({}{}ms){}: {}{}{}", ANSI_YELLOW, t.getDelta().getNano() / 1000000, modelName, ANSI_GREEN, response, ANSI_RESET);
            assertNotNull(response, "Response should not be null");
            return response;
        }
    }

    @BeforeEach
    protected void setupTest() {
        String modelName = aiAssistantService.getModelName();
        aiAssistantService.clearConversation(TEST_CONVERSATION_ID);
        log.info("{}=== Running test with model: {} ==={}", ANSI_BLUE, modelName, ANSI_RESET);
    }

}
