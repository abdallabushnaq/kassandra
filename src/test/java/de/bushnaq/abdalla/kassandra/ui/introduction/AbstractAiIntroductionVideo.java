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

package de.bushnaq.abdalla.kassandra.ui.introduction;

import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.Kassandra;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.Locale;

/**
 * Abstract base class for introduction video tests that interact with the Kassandra AI chat panel.
 * <p>
 * Provides helpers to:
 * <ul>
 *   <li>Wait for the AI to finish responding ({@link #waitForAi()})</li>
 *   <li>Wait and return the AI's reply text ({@link #submitQueryAndWaitForAiAndGetResponse()})</li>
 *   <li>Read the text of the last AI response bubble ({@link #getLastAiResponse()})</li>
 *   <li>Detect whether the agent is asking for explicit confirmation ({@link #isAgentAskingForConfirmation()})</li>
 * </ul>
 */
@Slf4j
public abstract class AbstractAiIntroductionVideo extends AbstractKeycloakUiTestUtil {

    /**
     * Phrases that indicate the AI is asking the user to confirm a destructive or significant action.
     * Deliberately narrow to avoid false positives from "Can I help you with anything else?"-style
     * tail questions that agents commonly append after completing a task.
     */
    private static final String[] CONFIRMATION_PHRASES = {
            "are you sure",
            "please confirm",
            "do you want me to",
            "shall i",
            "would you like me to",
            "do you confirm",
            "can you confirm",
            "confirm"
    };

    @Autowired
    protected HumanizedSeleniumHandler seleniumHandler;

    /**
     * Returns the text content of the last AI response bubble currently visible in the chat panel.
     * Reads the element with id {@link Kassandra#AI_LAST_RESPONSE} directly from the DOM.
     * <p>
     * Safe to call immediately after {@link #waitForAi()} because the submit button is re-enabled
     * only after the AI message has already been appended to the conversation history.
     *
     * @return the AI response text, or an empty string if the element is not found
     */
    protected String getLastAiResponse() {
        Object result = seleniumHandler.executeJavaScript(
                "var els = document.querySelectorAll('#" + Kassandra.AI_LAST_RESPONSE + "');" +
                        "var el = els.length > 0 ? els[els.length - 1] : null;" +
                        "return el ? el.innerText : '';");
        return result != null ? result.toString() : "";
    }

    /**
     * Returns {@code true} if the last agent response contains a phrase that indicates the agent
     * is asking the user to explicitly confirm a pending action (e.g. a deletion).
     * <p>
     * The check is intentionally narrow: it only matches well-known confirmation-request patterns
     * and does NOT trigger on generic tail questions like "Is there anything else I can help you with?".
     * Logs the detected phrase at WARN level to aid debugging of false positives.
     *
     * @return {@code true} if a confirmation phrase is detected in the last agent response
     */
    protected boolean isAgentAskingForConfirmation() {
        String response = getLastAiResponse();
        if (response == null || response.isBlank()) {
            return false;
        }
        String lower = response.toLowerCase(Locale.ROOT);
        for (String phrase : CONFIRMATION_PHRASES) {
            if (lower.contains(phrase)) {
                log.warn("Agent is asking for confirmation — detected phrase '{}' in response: {}", phrase, response);
                return true;
            }
        }
        return false;
    }

    protected void processQuery(String query) {
        seleniumHandler.setTextArea(Kassandra.AI_QUERY_INPUT, query);
        submitQuery();
    }

    protected String processQueryAndWaitForAnswer(String query) {
        seleniumHandler.setTextArea(Kassandra.AI_QUERY_INPUT, query);
        return submitQueryAndWaitForAiAndGetResponse();
    }

    protected void submitQuery() {
        seleniumHandler.click(Kassandra.AI_SUBMIT_BUTTON);
        seleniumHandler.waitForElementToBeDisabled(Kassandra.AI_SUBMIT_BUTTON);
    }

    /**
     * Waits for the AI to finish responding and returns the reply text.
     * Equivalent to calling {@link #waitForAi()} followed by {@link #getLastAiResponse()}.
     *
     * @return the AI response text, or an empty string if the element is not found
     */
    protected String submitQueryAndWaitForAiAndGetResponse() {
        submitQuery();
        waitForAi();
        return getLastAiResponse();
    }

    /**
     * Waits up to 240 seconds for the AI submit button to become enabled again,
     * indicating that the AI has finished processing and its reply has been rendered.
     */
    protected void waitForAi() {
        seleniumHandler.pushWaitDuration(Duration.ofSeconds(240));
        seleniumHandler.waitForElementToBeEnabled(Kassandra.AI_SUBMIT_BUTTON);
        seleniumHandler.popWaitDuration();
    }
}
