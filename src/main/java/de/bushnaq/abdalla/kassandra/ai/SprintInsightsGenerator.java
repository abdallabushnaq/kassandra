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

package de.bushnaq.abdalla.kassandra.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * AI-powered insights generator for sprint data analysis.
 * Analyzes JSON lists of sprints and provides AI-generated insights about patterns, trends, and recommendations.
 * Uses openthinker:7b model for deep thinking analysis.
 */
@Component
public class SprintInsightsGenerator {
    private static final String ANSI_GREEN  = "\u001B[32m";
    private static final String ANSI_RESET  = "\u001B[0m";
    private static final String ANSI_YELLOW = "\u001B[33m";

    // Model specifically chosen for deep thinking analysis
    private static final String SPRINT_INSIGHTS_MODEL = "deepseek-r1:8b";

    private static final String SPRINT_INSIGHTS_PROMPT_TEMPLATE = """
            CRITICAL INSTRUCTION: You MUST analyze the specific JSON data provided below. DO NOT return template responses with placeholders like [Sprint Name] or [Actual Progress Percentage]. Any response containing square brackets [ ] will be considered a failure.
            
            You are analyzing REAL sprint data. Here is what the data contains:
            
            DATA EXPLANATION:
            - actualProgressDisplay: "95%%" means 95%% complete, "100%%" means 100%% complete
            - expectedProgressDisplay: "88%%" means expected to be 88%% complete at current time
            - delayFraction: 0.12 means 12%% behind schedule, -0.05 means 5%% ahead of schedule
            - status: "GOOD", "WARNING", or "CRITICAL" indicates sprint health
            - effortEstimateDisplay vs originalEstimation: shows if estimates changed
            - currentEfficiency vs optimalEfficiency: shows team performance gaps
            - effortSpentDisplay: actual time worked (e.g., "3d 22h 12m")
            - effortRemainingDisplay: time remaining (e.g., "4h 48m")
            - currentEffortDelayDisplay: effort delay with percentage (e.g., "2h 15m (12%%)")
            - extrapolatedScheduleDelayDisplay: projected delay with percentage (e.g., "1d 2h (8%%)")
            
            EXAMPLE OF CORRECT ANALYSIS:
            "Sprint 'paris' is 95%% complete but has a WARNING status due to being 12%% behind schedule. The team is operating at 85%% efficiency but needs 120%% efficiency to finish on time."
            
            EXAMPLE OF INCORRECT RESPONSE (DO NOT DO THIS):
            "Sprint '[Sprint Name]' is [Actual Progress Percentage]%% complete..."
            
            YOUR TASK:
            1. Count the actual number of sprints in the data
            2. Name each sprint specifically by reading the "sprintName" field
            3. Calculate the actual status distribution (how many GOOD vs WARNING vs CRITICAL)
            4. Identify specific sprints with problems and explain why
            5. Use the actual percentage values from actualProgressDisplay, dates, and efficiency numbers from the data
            
            REQUIRED RESPONSE FORMAT:
            ## Sprint Portfolio Analysis - [TODAY'S DATE]
            
            ### Portfolio Overview
            - Total sprints analyzed: [COUNT THE ACTUAL SPRINTS]
            - Completed sprints: [LIST WHICH ONES HAVE actualProgressDisplay = "100%%"]
            - Active sprints: [LIST WHICH ONES ARE IN PROGRESS]
            - Future sprints: [LIST WHICH ONES HAVE actualProgressDisplay = "0%%"]
            
            ### Status Distribution
            - GOOD status: [COUNT AND NAME THE ACTUAL SPRINTS]
            - WARNING status: [COUNT AND NAME THE ACTUAL SPRINTS]
            - CRITICAL status: [COUNT AND NAME THE ACTUAL SPRINTS]
            
            ### Sprint-Specific Analysis
            [FOR EACH SPRINT, USE ITS ACTUAL NAME AND DATA]:
            
            **[ACTUAL SPRINT NAME]**:
            - Progress: [ACTUAL actualProgressDisplay] complete ([AHEAD/BEHIND] schedule by [ACTUAL delayFraction * 100]%%)
            - Status: [ACTUAL status]
            - Efficiency: [ACTUAL currentEfficiency] (needs [ACTUAL optimalEfficiency])
            - Time spent: [ACTUAL effortSpentDisplay] of [ACTUAL effortEstimateDisplay] estimated
            - [SPECIFIC INSIGHT ABOUT THIS SPRINT]
            
            ### Key Findings
            1. [SPECIFIC FINDING USING ACTUAL SPRINT NAMES AND NUMBERS FROM THE DATA]
            2. [SPECIFIC FINDING USING ACTUAL SPRINT NAMES AND NUMBERS FROM THE DATA]
            3. [SPECIFIC FINDING USING ACTUAL SPRINT NAMES AND NUMBERS FROM THE DATA]
            
            ### Recommendations
            [SPECIFIC RECOMMENDATIONS BASED ON THE ACTUAL DATA PATTERNS]
            
            REMEMBER: Use the actual sprint names from "sprintName" field and actual percentages from "actualProgressDisplay" and other display fields. NO placeholders allowed.
            
            JSON DATA TO ANALYZE:
            %s
            """;

    private static final Logger          logger = LoggerFactory.getLogger(SprintInsightsGenerator.class);
    private final        OllamaChatModel chatModel;

    @Autowired
    public SprintInsightsGenerator(OllamaChatModel defaultChatModel) {
        // Create a dedicated chat model for deep thinking analysis with extended timeout
        OllamaApi ollamaApi = OllamaApi.builder().build();
        this.chatModel = OllamaChatModel.builder().ollamaApi(ollamaApi).defaultOptions(OllamaOptions.builder().model(SPRINT_INSIGHTS_MODEL).temperature(0.9).build()).build();
    }

    /**
     * Analyzes sprint data and generates insights focused on a specific question or area.
     *
     * @param sprintJsonData The JSON string containing an array of sprint objects
     * @param focusQuestion  Specific question or area to focus the analysis on
     * @return A targeted analysis report answering the specific question
     * @throws RuntimeException if analysis fails
     */
    public String generateFocusedInsights(String sprintJsonData, String focusQuestion) {
        if (sprintJsonData == null || sprintJsonData.trim().isEmpty()) {
            return "No sprint data provided for analysis.";
        }

        if (focusQuestion == null || focusQuestion.trim().isEmpty()) {
            return generateInsights(sprintJsonData);
        }

        logger.info("Generating focused AI insights for sprint data with question: '{}'", focusQuestion);

        try {
            // Create a focused prompt that includes the specific question
            String focusedPrompt = SPRINT_INSIGHTS_PROMPT_TEMPLATE.replace("\"\n\nJSON DATA TO ANALYZE:",
                    "Focus your analysis on this specific question:\n" + focusQuestion + "\n\nJSON DATA TO ANALYZE:");

            String formattedPrompt = String.format(focusedPrompt, sprintJsonData);


            Prompt prompt = new Prompt(formattedPrompt);

            System.out.printf("Focused Sprint Insights LLM prompt for '%s':\n%s%s%s\n\n", focusQuestion, ANSI_GREEN, formattedPrompt, ANSI_RESET);
            logger.debug("Focused Sprint Insights LLM prompt for '{}': {}", focusQuestion, formattedPrompt);
            logger.debug("Focused Sprint Insights LLM prompt for '{}': {}", focusQuestion, formattedPrompt);

            ChatResponse response = chatModel.call(prompt);
            String       content  = response.getResult().getOutput().getText();

            System.out.printf("Focused Sprint Insights LLM response:\n\n%s%s%s\n\n", ANSI_YELLOW, content, ANSI_RESET);
            logger.debug("Focused Sprint Insights LLM response: {}", content);

            if (content == null || content.trim().isEmpty()) {
                logger.warn("Focused sprint insights generation failed, result is empty");
                return "Failed to generate focused insights - empty response from AI model.";
            }

            return content.trim();

        } catch (Exception e) {
            logger.error("Error generating focused sprint insights: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate focused sprint insights: " + e.getMessage(), e);
        }
    }

    /**
     * Analyzes a JSON list of sprints and generates AI-powered insights.
     *
     * @param sprintJsonData The JSON string containing an array of sprint objects
     * @return A formatted analysis report with insights and recommendations
     * @throws RuntimeException if analysis fails
     */
    public String generateInsights(String sprintJsonData) {
        if (sprintJsonData == null || sprintJsonData.trim().isEmpty()) {
            return "No sprint data provided for analysis.";
        }

        logger.info("Generating AI insights for sprint data using model: {}", SPRINT_INSIGHTS_MODEL);

        try {
            // Create prompt with the sprint data
            String formattedPrompt = String.format(SPRINT_INSIGHTS_PROMPT_TEMPLATE, sprintJsonData);

            Prompt prompt = new Prompt(formattedPrompt);

            System.out.printf("Sprint Insights LLM prompt:\n%s%s%s\n\n", ANSI_GREEN, formattedPrompt, ANSI_RESET);
            logger.debug("Sprint Insights LLM prompt: {}", formattedPrompt);

            ChatResponse response = chatModel.call(prompt);
            String       content  = response.getResult().getOutput().getText();

            System.out.printf("Sprint Insights LLM response:\n\n%s%s%s\n\n", ANSI_YELLOW, content, ANSI_RESET);
            logger.debug("Sprint Insights LLM response: {}", content);
            logger.debug("Sprint Insights LLM prompt: {}", formattedPrompt);

            if (content == null || content.trim().isEmpty()) {
                logger.warn("Sprint insights generation failed, result is empty");
                return "Failed to generate insights - empty response from AI model.";
            }

            return content.trim();

        } catch (Exception e) {
            logger.error("Error generating sprint insights: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate sprint insights: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a quick summary of key metrics from the sprint data.
     *
     * @param sprintJsonData The JSON string containing an array of sprint objects
     * @return A concise summary of key sprint metrics
     * @throws RuntimeException if analysis fails
     */
    public String generateQuickSummary(String sprintJsonData) {
        return generateFocusedInsights(sprintJsonData,
                "Provide only a brief summary with key metrics: total sprints, status distribution, " +
                        "and top 2 most important insights. Keep it under 200 words.");
    }
}

