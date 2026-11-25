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

package de.bushnaq.abdalla.kassandra.ui.component;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import de.bushnaq.abdalla.kassandra.ai.lmstudio.LmStudioService;
import de.bushnaq.abdalla.kassandra.ai.insights.SprintInsightsGenerator;
import de.bushnaq.abdalla.kassandra.ai.mcp.AiAssistantService;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Status;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.Worklog;
import de.bushnaq.abdalla.kassandra.dto.ServerSetting;
import de.bushnaq.abdalla.kassandra.rest.api.ServerSettingsApi;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import de.bushnaq.abdalla.kassandra.service.ServerSettingsCatalogue;
import de.bushnaq.abdalla.kassandra.service.ServerSettingsCatalogue.Keys;
import de.bushnaq.abdalla.kassandra.service.ServerSettingsService;
import de.bushnaq.abdalla.kassandra.ui.view.InsightsView;
import de.bushnaq.abdalla.kassandra.ui.view.SprintStatistics;
import de.bushnaq.abdalla.kassandra.ui.view.ServerSettingsView;
import de.bushnaq.abdalla.kassandra.util.AbstractTestUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("UnitTest")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
class AiMarkdownRenderingTest extends AbstractTestUtil {
    private static final String RESPONSE = "## Heading\n\n- **Bold**\n- [Link](https://example.com)\n\n| A | B |\n| - | - |\n| 1 | 2 |";

    @Test
    void chatReplaysOnlyAssistantMessagesAsMarkdown() {
        ChatPanelSessionState state = new ChatPanelSessionState();
        state.getOrCreateSlot("route").messages.addAll(List.of(
                new ChatPanelSessionState.ChatMessageRecord("user", "**literal**"),
                new ChatPanelSessionState.ChatMessageRecord("ai", RESPONSE),
                new ChatPanelSessionState.ChatMessageRecord("tool", "**tool**")));
        ChatAgentPanel panel = panel(state);

        panel.restoreOrStart("route");
        Div history = (Div) ReflectionTestUtils.getField(panel, "conversationHistory");
        List<Component> messages = history.getChildren().toList();
        assertEquals(3, messages.size());
        Span user = (Span) messages.get(0).getChildren().skip(1).findFirst().orElseThrow();
        assertEquals("**literal**", user.getText());
        Markdown response = (Markdown) messages.get(1).getChildren().skip(1).findFirst().orElseThrow();
        assertEquals(RESPONSE, response.getContent());
        assertEquals(ChatAgentPanel.AI_LAST_RESPONSE, response.getId().orElseThrow());
        assertTrue(response.isLineBreaks());
        Span tool = (Span) messages.get(2).getChildren().skip(1).findFirst().orElseThrow();
        assertEquals("**tool**", tool.getText());
    }

    @Test
    void streamingBubbleUsesMarkdownAndPreservesSoftBreaks() {
        ChatAgentPanel panel = panel(null);
        Markdown response = ReflectionTestUtils.invokeMethod(panel, "startAiStreamingBubble");
        assertNotNull(response);
        response.setContent(RESPONSE);
        assertEquals(RESPONSE, response.getContent());
        assertTrue(response.isLineBreaks());
        assertEquals(ChatAgentPanel.AI_LAST_RESPONSE, response.getId().orElseThrow());
    }

    @Test
    void insightsRenderCompleteMarkdownAndKeepEmptyStateAndTimestamp() {
        InsightsView view = new InsightsView(null, null, null, null, null, null, null, Clock.systemUTC());
        Div content = (Div) ReflectionTestUtils.getField(view, "insightsContent");
        ReflectionTestUtils.invokeMethod(view, "displayInsights", RESPONSE);
        List<Component> children = content.getChildren().toList();
        assertEquals(2, children.size());
        Markdown response = (Markdown) children.get(0);
        assertEquals(RESPONSE, response.getContent());
        assertTrue(response.isLineBreaks());
        assertTrue(((Paragraph) children.get(1)).getText().startsWith("Generated on: "));

        ReflectionTestUtils.invokeMethod(view, "displayInsights", " ");
        children = content.getChildren().toList();
        assertEquals(1, children.size());
        assertEquals("No insights generated. Please try again.", ((Paragraph) children.get(0)).getText());
    }

    @Test
    void insightsUnwrapMarkdownReportsWithoutRemovingNestedCodeBlocks() {
        InsightsView view = new InsightsView(null, null, null, null, null, null, null, Clock.systemUTC());
        Div content = (Div) ReflectionTestUtils.getField(view, "insightsContent");
        String report = "## Heading\n\n```java\nint value = 1;\n```\n";
        for (String wrapped : List.of("```markdown\n" + report + "```", "```markdown\n" + report,
                "```MARKDOWN\r\n" + report + "```\r\n")) {
            ReflectionTestUtils.invokeMethod(view, "displayInsights", wrapped);
            Markdown markdown = (Markdown) content.getChildren().findFirst().orElseThrow();
            assertEquals(report, markdown.getContent());
        }
        String ordinaryCode = "```java\nint value = 1;\n```";
        ReflectionTestUtils.invokeMethod(view, "displayInsights", ordinaryCode);
        assertEquals(ordinaryCode, ((Markdown) content.getChildren().findFirst().orElseThrow()).getContent());
    }

    @Test
    void onlyStartedSprintsWithPositiveLoggedWorkAreEligible() {
        InsightsView view = new InsightsView(null, null, null, null, null, null, null, Clock.systemUTC());
        Sprint sprint = new Sprint();
        sprint.setStatus(Status.STARTED);
        assertEquals(false, ReflectionTestUtils.invokeMethod(view, "isEligibleForSummary", sprint));
        Worklog worklog = new Worklog();
        worklog.setTimeSpent(Duration.ZERO);
        sprint.getWorklogs().add(worklog);
        assertEquals(false, ReflectionTestUtils.invokeMethod(view, "isEligibleForSummary", sprint));
        worklog.setTimeSpent(Duration.ofMinutes(-1));
        assertEquals(false, ReflectionTestUtils.invokeMethod(view, "isEligibleForSummary", sprint));
        worklog.setTimeSpent(Duration.ofMinutes(1));
        assertEquals(true, ReflectionTestUtils.invokeMethod(view, "isEligibleForSummary", sprint));
        sprint.setStatus(Status.CLOSED);
        assertEquals(false, ReflectionTestUtils.invokeMethod(view, "isEligibleForSummary", sprint));
        sprint.setStatus(Status.CREATED);
        assertEquals(false, ReflectionTestUtils.invokeMethod(view, "isEligibleForSummary", sprint));
    }

    @Test
    void selectedSprintSummaryStreamsOnlyItsSuppliedData() {
        ChatModel model = mock(ChatModel.class);
        ServerSettingsService settings = mock(ServerSettingsService.class);
        when(settings.value(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        when(model.stream(any(Prompt.class))).thenReturn(Flux.just(
                new ChatResponse(List.of(new Generation(new AssistantMessage("## Selected")))),
                new ChatResponse(List.of(new Generation(new AssistantMessage("\nDetails"))))));
        SprintInsightsGenerator generator = new SprintInsightsGenerator(model, settings, mock(LmStudioService.class));
        assertEquals(List.of("## Selected", "\nDetails"),
                generator.streamSprintSummary("{\"sprint\":\"Selected\"}").collectList().block());
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(model).stream(prompt.capture());
        String text = prompt.getValue().getUserMessages().getFirst().getText();
        assertTrue(text.contains("{\"sprint\":\"Selected\"}"));
        assertTrue(text.contains("someone unfamiliar"));
        assertTrue(text.contains("without enclosing the entire response"));
        assertTrue(text.contains("deliveryBuffer"));
        assertTrue(text.contains("not work to be performed"));
        assertTrue(text.contains("--lumo-error-text-color"));
        assertFalse(text.contains("Sprint Portfolio Analysis"));
    }

    @Test
    void selectedSprintPayloadContainsItsTaskFacts() {
        InsightsView view = new InsightsView(null, null, null, null, null, null, null, Clock.systemUTC());
        LocalDateTime start = LocalDateTime.of(2026, 1, 5, 9, 0);
        Sprint sprint = new Sprint();
        sprint.setName("Selected");
        sprint.setStart(start);
        sprint.setEnd(start.plusDays(5));
        sprint.setReleaseDate(start.plusDays(5));
        sprint.setWorked(Duration.ofHours(2));
        sprint.setRemaining(Duration.ofHours(3));
        sprint.setOriginalEstimation(Duration.ofHours(5));
        Task task = new Task();
        task.setName("Selected task");
        task.setTimeSpent(Duration.ofHours(2));
        sprint.getTasks().add(task);
        Task buffer = new Task();
        buffer.setName(Task.DELIVERY_BUFFER.toUpperCase(Locale.ROOT));
        sprint.getTasks().add(buffer);
        SprintStatistics statistics = new SprintStatistics(sprint, start.plusDays(1));
        String json = ReflectionTestUtils.invokeMethod(view, "createSprintSummaryJson", sprint, statistics, "Product / Feature / Selected");
        assertNotNull(json);
        assertTrue(json.contains("Selected task"));
        assertTrue(json.contains("\"timeSpentSeconds\" : 7200"));
        assertTrue(json.contains("\"deliveryBuffer\" : false"));
        assertTrue(json.contains("\"deliveryBuffer\" : true"));
        assertTrue(json.contains("Product / Feature / Selected"));
        Sprint other = new Sprint();
        other.setName("Other sprint");
        Task otherTask = new Task();
        otherTask.setName("Other task");
        other.getTasks().add(otherTask);
        assertFalse(json.contains(other.getName()));
        assertFalse(json.contains(otherTask.getName()));
    }

    @Test
    void portfolioStreamingPassesTheQuestionAndPropagatesFailures() {
        ChatModel model = mock(ChatModel.class);
        ServerSettingsService settings = mock(ServerSettingsService.class);
        when(settings.value(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        when(model.stream(any(Prompt.class))).thenReturn(Flux.error(new IllegalStateException("Model unavailable")));
        SprintInsightsGenerator generator = new SprintInsightsGenerator(model, settings, mock(LmStudioService.class));
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> generator.streamInsights("[]", "Which sprint is at risk?").collectList().block());
        assertEquals("Model unavailable", error.getMessage());
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(model).stream(prompt.capture());
        String text = prompt.getValue().getUserMessages().getFirst().getText();
        assertTrue(text.contains("Which sprint is at risk?"));
        assertTrue(text.contains("not a task to be worked on"));
        assertTrue(text.contains("--lumo-error-text-color"));
    }

    @Test
    void streamingLogsPromptsAndFullOrPartialResponsesAtDebugLevel() {
        ChatModel model = mock(ChatModel.class);
        ServerSettingsService settings = mock(ServerSettingsService.class);
        when(settings.value(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        ChatResponse chunk = new ChatResponse(List.of(new Generation(new AssistantMessage("Partial response"))));
        when(model.stream(any(Prompt.class))).thenReturn(
                Flux.just(chunk),
                Flux.concat(Flux.just(chunk), Flux.error(new IllegalStateException("Model unavailable"))),
                Flux.concat(Flux.just(chunk), Flux.never()));
        SprintInsightsGenerator generator = new SprintInsightsGenerator(model, settings, mock(LmStudioService.class));
        Logger logger = (Logger) LoggerFactory.getLogger(SprintInsightsGenerator.class);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        try {
            generator.streamSprintSummary("{\"sprint\":\"Selected\"}").subscribe();
            generator.streamSprintSummary("{}").onErrorComplete().subscribe();
            generator.streamSprintSummary("{}").take(1).subscribe();
            List<String> messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
            assertEquals(3, messages.stream().filter(message -> message.contains(" prompt:\n")).count());
            assertTrue(messages.stream().anyMatch(message -> message.contains("{\"sprint\":\"Selected\"}")));
            assertTrue(messages.stream().anyMatch(message -> message.contains("response (onComplete):\nPartial response")));
            assertTrue(messages.stream().anyMatch(message -> message.contains("response (onError):\nPartial response")));
            assertTrue(messages.stream().anyMatch(message -> message.contains("response (cancel):\nPartial response")));
            logger.setLevel(Level.INFO);
            generator.streamSprintSummary("{}").take(1).subscribe();
            assertEquals(messages.size(), appender.list.size());
        } finally {
            logger.setLevel(previousLevel);
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void insightsLoadsConfiguredModelBeforeStreamingWithConfiguredOptions() {
        ChatModel model = mock(ChatModel.class);
        LmStudioService lmStudio = mock(LmStudioService.class);
        ServerSettingsService settings = mock(ServerSettingsService.class);
        when(settings.value(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        when(settings.value(Keys.AI_INSIGHTS_MODEL, "")).thenReturn("insights-model", "another-model");
        when(settings.value(Keys.AI_INSIGHTS_REASONING_EFFORT, "default")).thenReturn("medium");
        when(settings.value(Keys.AI_TEMPERATURE, "0.0")).thenReturn("0.5");
        when(settings.isCategoryEnabled(ServerSettingsCatalogue.LM_STUDIO.key())).thenReturn(true);
        when(lmStudio.ensureModelLoaded("insights-model")).thenReturn(true);
        when(model.stream(any(Prompt.class))).thenReturn(Flux.empty());
        SprintInsightsGenerator generator = new SprintInsightsGenerator(model, settings, lmStudio);

        generator.streamSprintSummary("{}").blockLast();

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        InOrder order = inOrder(lmStudio, model);
        order.verify(lmStudio).ensureModelLoaded("insights-model");
        order.verify(model).stream(prompt.capture());
        OpenAiChatOptions options = (OpenAiChatOptions) prompt.getValue().getOptions();
        assertEquals("insights-model", options.getModel());
        assertEquals("medium", options.getReasoningEffort());
        assertEquals(0.5, options.getTemperature());
        assertEquals(20480, options.getMaxTokens());
        assertEquals(42, options.getSeed());
        verify(settings, times(1)).value(Keys.AI_INSIGHTS_MODEL, "");
    }

    @Test
    void insightsSkipsLmStudioWhenDisabledAndLeavesReasoningAtModelDefault() {
        ChatModel model = mock(ChatModel.class);
        LmStudioService lmStudio = mock(LmStudioService.class);
        ServerSettingsService settings = mock(ServerSettingsService.class);
        when(settings.value(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        when(settings.value(Keys.AI_INSIGHTS_MODEL, "")).thenReturn("other-provider-model");
        when(model.stream(any(Prompt.class))).thenReturn(Flux.empty());
        SprintInsightsGenerator generator = new SprintInsightsGenerator(model, settings, lmStudio);

        generator.streamInsights("[]", null).blockLast();

        verifyNoInteractions(lmStudio);
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(model).stream(prompt.capture());
        OpenAiChatOptions options = (OpenAiChatOptions) prompt.getValue().getOptions();
        assertEquals("other-provider-model", options.getModel());
        assertNull(options.getReasoningEffort());
    }

    @Test
    void insightsReportsModelLoadingFailureWithoutStartingARequest() {
        ChatModel model = mock(ChatModel.class);
        LmStudioService lmStudio = mock(LmStudioService.class);
        ServerSettingsService settings = mock(ServerSettingsService.class);
        when(settings.value(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        when(settings.value(Keys.AI_INSIGHTS_MODEL, "")).thenReturn("insights-model");
        when(settings.isCategoryEnabled(ServerSettingsCatalogue.LM_STUDIO.key())).thenReturn(true);
        SprintInsightsGenerator generator = new SprintInsightsGenerator(model, settings, lmStudio);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> generator.streamInsights("[]", null).blockLast());

        assertTrue(error.getMessage().contains("Failed to load Insights model 'insights-model'"));
        verify(lmStudio).ensureModelLoaded("insights-model");
        verifyNoInteractions(model);
    }

    @Test
    void reasoningEffortSettingUsesASelectEditor() {
        ServerSettingsApi api = mock(ServerSettingsApi.class);
        when(api.getAll()).thenReturn(List.of());
        ServerSettingsView view = new ServerSettingsView(api);
        ServerSetting setting = new ServerSetting();
        setting.setType("TEXT");
        setting.setKey(Keys.AI_INSIGHTS_REASONING_EFFORT);
        setting.setValue("default");
        setting.setOptions(List.of("default", "low", "medium", "high"));

        HorizontalLayout row = ReflectionTestUtils.invokeMethod(view, "createEditor", setting, true);

        assertNotNull(row);
        Select<?> select = (Select<?>) row.getChildren().findFirst().orElseThrow();
        assertEquals("default", select.getValue());
        assertTrue(select.isEnabled());
    }

    private ChatAgentPanel panel(ChatPanelSessionState state) {
        return new ChatAgentPanel(mock(AiAssistantService.class), mock(UserApi.class), state,
                mock(ServerSettingsService.class), mock(LmStudioService.class));
    }
}
