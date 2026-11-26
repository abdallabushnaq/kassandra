/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.service;

import com.openai.errors.OpenAIException;
import de.bushnaq.abdalla.kassandra.ai.insights.BurndownExplanationGenerator;
import de.bushnaq.abdalla.kassandra.dto.Availability;
import de.bushnaq.abdalla.kassandra.dto.OffDayType;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.TaskStatus;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.Worklog;
import de.bushnaq.abdalla.kassandra.rest.dto.gantt.AuthorSeriesDto;
import de.bushnaq.abdalla.kassandra.rest.dto.gantt.GanttBurndownChartDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.FinishReasonCategory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@Tag("UnitTest")
@Slf4j
class BurndownEvidenceServiceTest {

    private record RankingCase(String name, BurndownExplanationService.Brief brief, RankingLead expected) {
    }

    private record RankingLead(String evidence, String action) {
    }

    private record RankingDecision(BurndownExplanationGenerator.Status status, RankingLead lead) {
        boolean isModelAnswer() {
            return status == BurndownExplanationGenerator.Status.SELECTED || status == BurndownExplanationGenerator.Status.ABSTAINED;
        }
    }

    private final BurndownEvidenceService service = new BurndownEvidenceService();

    @Test
    void selectsOnlyKnownGroupsAndKeepsTheBaselineUnchanged() {
        var brief = selectionFixture();
        var model = mock(ChatModel.class);
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            var options = (OpenAiChatOptions) prompt.getOptions();
            assertEquals("qwen/qwen3.8-27b", options.getModel());
            assertEquals(0.0, options.getTemperature());
            assertEquals(42, options.getSeed());
            assertEquals("medium", options.getReasoningEffort());
            assertEquals(4096, options.getMaxTokens());
            assertTrue(options.getResponseFormat().getJsonSchema().contains("orderedGroupIds"));
            for (var group : brief.taskGroups())
                assertFalse(prompt.getUserMessage().getText().contains(group.id().toString()));
            return selectionResponse("{\"orderedGroupIds\":[\"G2\",\"G1\"]}", FinishReasonCategory.COMPLETED);
        });
        var result = selector(model).select(brief);
        assertEquals(BurndownExplanationGenerator.Status.SELECTED, result.status());
        assertSame(brief, result.baseline());
        assertEquals(List.of(new BurndownExplanationGenerator.TaskReview(brief.taskGroups().get(1)),
                new BurndownExplanationGenerator.TaskReview(brief.taskGroups().getFirst())), result.orderedReviews());
    }

    @Test
    void distinguishesAbstentionInvalidResponsesAndRequestFailures() {
        var brief = selectionFixture();
        var model = mock(ChatModel.class);
        var selector = selector(model);
        for (String content : List.of("not json", "{\"orderedGroupIds\":[\"G1\",\"G1\"]}",
                "{\"orderedGroupIds\":[\"G3\"]}", "{\"orderedGroupIds\":[1]}",
                "{\"orderedGroupIds\":[],\"orderedGroupIds\":[]}", "{\"orderedGroupIds\":[]} {}",
                "{\"orderedGroupIds\":[],\"extra\":1}")) {
            when(model.call(any(Prompt.class))).thenReturn(selectionResponse(content, FinishReasonCategory.COMPLETED));
            var result = selector.select(brief);
            assertEquals(BurndownExplanationGenerator.Status.INVALID_SELECTION, result.status(), content);
            assertSame(brief, result.baseline());
            assertTrue(result.orderedReviews().isEmpty());
        }
        when(model.call(any(Prompt.class))).thenReturn(selectionResponse("{\"orderedGroupIds\":[]}", FinishReasonCategory.COMPLETED));
        assertEquals(BurndownExplanationGenerator.Status.ABSTAINED, selector.select(brief).status());
        when(model.call(any(Prompt.class))).thenReturn(selectionResponse("{\"orderedGroupIds\":[]}", FinishReasonCategory.TRUNCATED));
        assertEquals(BurndownExplanationGenerator.Status.INCOMPLETE, selector.select(brief).status());
        when(model.call(any(Prompt.class))).thenThrow(new OpenAIException("mock request failure", null));
        assertEquals(BurndownExplanationGenerator.Status.REQUEST_FAILED, selector.select(brief).status());
    }

    @Test
    void skipsModelWithoutEvidenceAndResolvesAggregateCapacitySeparately() {
        var fixture = selectionFixture();
        var model = mock(ChatModel.class);
        var selector = selector(model);
        var empty = new BurndownExplanationService.Brief(fixture.chart(), fixture.gapAttribution(), List.of(),
                fixture.taskCoverage(), fixture.capacity(), fixture.outlook());
        assertEquals(BurndownExplanationGenerator.Status.NOT_APPLICABLE, selector.select(empty).status());
        verifyNoInteractions(model);

        var day = fixture.chart().throughDate();
        var capacity = new CapacityOverlapContributor.Result(CapacityOverlapContributor.Coverage.AVAILABLE, day, 0,
                List.of(new CapacityOverlapContributor.DayOverlap(day, 2)));
        var withCapacity = new BurndownExplanationService.Brief(fixture.chart(), fixture.gapAttribution(), fixture.taskGroups(),
                fixture.taskCoverage(), capacity, fixture.outlook());
        when(model.call(any(Prompt.class))).thenReturn(selectionResponse("{\"orderedGroupIds\":[\"G3\"]}", FinishReasonCategory.COMPLETED));
        var result = selector.select(withCapacity);
        assertEquals(BurndownExplanationGenerator.Status.SELECTED, result.status());
        assertEquals(List.of(new BurndownExplanationGenerator.CapacityReview()), result.orderedReviews());
    }

    @Test
    @EnabledIfSystemProperty(named = "kassandra.burndown.qwen.smoke", matches = "true")
    void smokeTestsQwenNativeSchemaThroughSpringAi() {
        var model = OpenAiChatModel.builder().options(OpenAiChatOptions.builder()
                .baseUrl("http://localhost:1234/v1").apiKey("lm-studio").timeout(Duration.ofMinutes(10)).build()).build();
        var brief = selectionFixture();
        var result = selector(model).select(brief);
        assertTrue(result.status() == BurndownExplanationGenerator.Status.SELECTED
                || result.status() == BurndownExplanationGenerator.Status.ABSTAINED, () -> "Native schema smoke: " + result.status());
        assertSame(brief, result.baseline());
    }

    @Test
    void baselinePrefersAnUnfinishedOverrunAndCandidateReversalPreservesAliasesAndSchema() {
        var fixture = selectionFixture();
        assertEquals(fixture.taskGroups().reversed(), BurndownExplanationService.rankTaskGroups(fixture.taskGroups()));
        var model = mock(ChatModel.class);
        var prompts = new ArrayList<Prompt>();
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            prompts.add(invocation.getArgument(0));
            return selectionResponse("{\"orderedGroupIds\":[\"G2\"]}", FinishReasonCategory.COMPLETED);
        });
        var selector = selector(model);
        var normal = evaluateSelection(selector, fixture, "evaluation-model", false);
        var reversed = evaluateSelection(selector, fixture, "evaluation-model", true);
        assertEquals(normal.orderedReviews(), reversed.orderedReviews());
        assertEquals(List.of(new BurndownExplanationGenerator.TaskReview(fixture.taskGroups().get(1))), reversed.orderedReviews());
        var originalOptions = (OpenAiChatOptions) prompts.getFirst().getOptions();
        var reversedOptions = (OpenAiChatOptions) prompts.getLast().getOptions();
        assertEquals("evaluation-model", originalOptions.getModel());
        assertEquals(originalOptions.getResponseFormat().getJsonSchema(), reversedOptions.getResponseFormat().getJsonSchema());
        var originalText = prompts.getFirst().getUserMessage().getText();
        var reversedText = prompts.getLast().getUserMessage().getText();
        assertTrue(originalText.indexOf("\"id\":\"G1\"") < originalText.indexOf("\"id\":\"G2\""));
        assertTrue(reversedText.indexOf("\"id\":\"G2\"") < reversedText.indexOf("\"id\":\"G1\""));
    }

    @Test
    @EnabledIfSystemProperty(named = "kassandra.burndown.qwen.evaluate", matches = "true")
    void evaluatesQwenUsefulAndRepeatableRankings() {
        String modelId = System.getProperty("kassandra.burndown.eval.model", "qwen/qwen3.8-27b");
        var model = OpenAiChatModel.builder().options(OpenAiChatOptions.builder()
                .baseUrl("http://localhost:1234/v1").apiKey("lm-studio").timeout(Duration.ofMinutes(10)).build()).build();
        var selector = selector(model);
        int baselineHits = 0, selectorHits = 0, repeatMatches = 0, reversedMatches = 0, comparedCases = 0;
        int failures = 0, abstentions = 0, skippedCalls = 0;
        var cases = rankingCases();
        for (var fixture : cases) {
            var ranked = fixture.brief().taskGroups();
            RankingLead baseline = !ranked.isEmpty() ? taskLead(ranked.getFirst())
                    : fixture.brief().capacity().overlaps().isEmpty() ? null : capacityLead();
            if (Objects.equals(baseline, fixture.expected()))
                baselineHits++;
            var decisions = new ArrayList<RankingDecision>();
            for (int attempt = 0; attempt < 3; attempt++) {
                boolean reverse = attempt == 2;
                long started = System.nanoTime();
                var result = evaluateSelection(selector, fixture.brief(), modelId, reverse);
                RankingLead lead = switch (result.orderedReviews().isEmpty() ? null : result.orderedReviews().getFirst()) {
                    case BurndownExplanationGenerator.TaskReview task -> taskLead(task.group());
                    case BurndownExplanationGenerator.CapacityReview ignored -> capacityLead();
                    case null -> null;
                };
                boolean valid = switch (result.status()) {
                    case SELECTED, ABSTAINED -> true;
                    case NOT_APPLICABLE -> fixture.expected() == null;
                    default -> false;
                };
                if (!valid)
                    failures++;
                if (result.status() == BurndownExplanationGenerator.Status.ABSTAINED)
                    abstentions++;
                if (result.status() == BurndownExplanationGenerator.Status.NOT_APPLICABLE)
                    skippedCalls++;
                if (valid && Objects.equals(lead, fixture.expected()))
                    selectorHits++;
                decisions.add(new RankingDecision(result.status(), lead));
                log.info("Burn-down evaluation model={} case={} attempt={} reversed={} baseline={} expected={} status={} lead={} elapsedMs={}",
                        modelId, fixture.name(), attempt + 1, reverse, baseline, fixture.expected(), result.status(), lead,
                        (System.nanoTime() - started) / 1_000_000);
            }
            if (!ranked.isEmpty() || !fixture.brief().capacity().overlaps().isEmpty())
                comparedCases++;
            if (decisions.getFirst().isModelAnswer() && decisions.getFirst().equals(decisions.get(1)))
                repeatMatches++;
            if (decisions.getFirst().isModelAnswer() && decisions.getFirst().equals(decisions.getLast()))
                reversedMatches++;
        }
        log.info("Burn-down evaluation model={} baseline={}/{} selectorHits={}/{} repeatMatches={}/{} reversedMatches={}/{} fallbacks={} modelAbstentions={} skippedCalls={}",
                modelId, baselineHits, cases.size(), selectorHits, 3 * cases.size(), repeatMatches, comparedCases, reversedMatches,
                comparedCases, failures, abstentions, skippedCalls);
    }

    private static List<RankingCase> rankingCases() {
        var fixture = selectionFixture();
        var day = fixture.chart().throughDate();
        var resolved = fixture.taskGroups().getFirst();
        var active = fixture.taskGroups().getLast();
        var upstream = active.id();
        var handoff = new UUID(0, 3);
        var downstream = new UUID(0, 4);
        var chain = new BurndownExplanationService.TaskGroup(upstream, List.of(
                new DependencyBlockerContributor.Observation(handoff, day.atStartOfDay(), List.of(upstream)),
                new DependencyBlockerContributor.Observation(downstream, day.atStartOfDay(), List.of(handoff))),
                active.overruns(), BurndownExplanationService.Action.REVIEW_BLOCKED_HANDOFF);
        var capacity = new CapacityOverlapContributor.Result(CapacityOverlapContributor.Coverage.AVAILABLE, day, 0, List.of(
                new CapacityOverlapContributor.DayOverlap(day.minusDays(1), 3), new CapacityOverlapContributor.DayOverlap(day, 3)));
        var missing = new BurndownExplanationService.TaskCoverage(DependencyBlockerContributor.Coverage.NO_PRIOR_SNAPSHOT, 0,
                EstimateOverrunContributor.Coverage.NO_PRIOR_SNAPSHOT, 0, 0);
        return List.of(
                rankingCase("active-over-resolved", fixture, List.of(resolved, active), fixture.capacity(), fixture.taskCoverage(), taskLead(active)),
                rankingCase("observed-chain-with-capacity", fixture, List.of(resolved, chain), capacity, fixture.taskCoverage(), taskLead(chain)),
                rankingCase("capacity-without-active-task-signal", fixture, List.of(resolved), capacity, fixture.taskCoverage(), capacityLead()),
                rankingCase("missing-history", fixture, List.of(), fixture.capacity(), missing, null));
    }

    private static RankingCase rankingCase(String name, BurndownExplanationService.Brief template,
                                           List<BurndownExplanationService.TaskGroup> groups, CapacityOverlapContributor.Result capacity,
                                           BurndownExplanationService.TaskCoverage coverage, RankingLead expected) {
        return new RankingCase(name, new BurndownExplanationService.Brief(template.chart(), template.gapAttribution(),
                BurndownExplanationService.rankTaskGroups(groups), coverage, capacity, template.outlook()), expected);
    }

    private static RankingLead taskLead(BurndownExplanationService.TaskGroup group) {
        return new RankingLead("task-group-" + group.id().getLeastSignificantBits(), group.suggestedAction().name());
    }

    private static RankingLead capacityLead() {
        return new RankingLead("aggregate-capacity", "review-aggregate-capacity");
    }

    private static BurndownExplanationGenerator.Result evaluateSelection(BurndownExplanationGenerator selector,
                                                                        BurndownExplanationService.Brief brief, String model,
                                                                        boolean reverse) {
        return ReflectionTestUtils.invokeMethod(selector, "select", brief, model, reverse);
    }

    private static ChatResponse selectionResponse(String content, FinishReasonCategory finish) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content),
                ChatGenerationMetadata.builder().finishReasonCategory(finish).build())));
    }

    private static BurndownExplanationGenerator selector(ChatModel chatModel) {
        var selector = new BurndownExplanationGenerator();
        ReflectionTestUtils.setField(selector, "chatModel", chatModel);
        return selector;
    }

    private static BurndownExplanationService.Brief selectionFixture() {
        LocalDate day = LocalDate.of(2026, 11, 17);
        var chart = new BurndownEvidenceService.Evidence(day.plusDays(1).atStartOfDay(), day,
                BurndownEvidenceService.Coverage.AVAILABLE, new BurndownEvidenceService.Comparison(72_000, 54_000, 18_000));
        var coverage = new BurndownExplanationService.TaskCoverage(DependencyBlockerContributor.Coverage.COMPARABLE_WINDOW, 0,
                EstimateOverrunContributor.Coverage.COMPARABLE_WINDOW, 2, 0);
        var groups = List.of(overrunGroup(1, day, TaskStatus.DONE, 43_200), overrunGroup(2, day, TaskStatus.IN_PROGRESS, 10_800));
        return new BurndownExplanationService.Brief(chart, BurndownExplanationService.GapAttribution.NOT_QUANTIFIED, groups, coverage,
                new CapacityOverlapContributor.Result(CapacityOverlapContributor.Coverage.AVAILABLE, day, 0, List.of()), null);
    }

    private static BurndownExplanationService.TaskGroup overrunGroup(int id, LocalDate day, TaskStatus status, long excess) {
        UUID task = new UUID(0, id);
        var observation = new EstimateOverrunContributor.Observation(task, status, day.minusDays(1).atStartOfDay(),
                3600, 0, 3600 + excess, excess);
        return new BurndownExplanationService.TaskGroup(task, List.of(), List.of(observation),
                BurndownExplanationService.Action.REVIEW_ESTIMATE_AND_REMAINING_WORK);
    }

    @Test
    void comparesActualAndWithBufferGuideAtTheSameUnpaddedDayBoundary() {
        GanttBurndownChartDto chart = chart();
        var evidence = service.capture(chart);
        assertEquals(chart.meta.now, evidence.capturedAt());
        assertEquals(LocalDate.of(2026, 11, 17), evidence.throughDate());
        assertEquals(BurndownEvidenceService.Coverage.AVAILABLE, evidence.coverage());
        assertEquals(new BurndownEvidenceService.Comparison(12600, 10800, 1800), evidence.comparison());
    }

    @Test
    void preservesNegativeGapsAndDoesNotReadWorkFromTheInProgressDay() {
        GanttBurndownChartDto chart = chart();
        chart.ganttGuideWithBuffer = List.of(21600L, 18000L, 14400L, 0L);
        assertEquals(-1800, service.capture(chart).comparison().gapSeconds());
    }

    @Test
    void doesNotCompareAnIncompleteAuthorSeriesOrMissingGuide() {
        GanttBurndownChartDto chart = chart();
        chart.authors.get(0).workPerDay = List.of(0L, 3600L);
        var missingActual = service.capture(chart);
        assertEquals(BurndownEvidenceService.Coverage.NO_ACTUAL_POINT, missingActual.coverage());
        assertNull(missingActual.comparison());

        chart.ganttGuideWithBuffer = null;
        var missingGuide = service.capture(chart);
        assertEquals(BurndownEvidenceService.Coverage.NO_GUIDE_POINT, missingGuide.coverage());
        assertNull(missingGuide.comparison());
    }

    @Test
    void reportsNoComparisonBeforeTheFirstCompletedSprintDayOrWithoutANowMarker() {
        GanttBurndownChartDto chart = chart();
        chart.meta.now = chart.meta.sprintStart.plusHours(1);
        assertEquals(BurndownEvidenceService.Coverage.NO_COMPLETED_SPRINT_DAY, service.capture(chart).coverage());
        chart.meta.now = null;
        assertEquals(BurndownEvidenceService.Coverage.NO_CURRENT_MARKER, service.capture(chart).coverage());
    }

    @Test
    void capturesScopedTaskFactsAndFreezesDependencies() {
        GanttBurndownChartDto chart = chart();
        Sprint sprint = sprint(chart);
        Task successor = new Task();
        successor.setMinEstimate(Duration.ofHours(1));
        successor.setMaxEstimate(Duration.ofHours(2));
        successor.setStart(chart.meta.sprintStart.plusDays(1));
        successor.setTaskStatus(TaskStatus.IN_PROGRESS);
        Task explicit = new Task();
        Task resourceOrder = new Task();
        Task outsideSprint = new Task();
        outsideSprint.setSprintId(new Sprint().getId());
        successor.addPredecessor(explicit, true);
        successor.addPredecessor(resourceOrder, false);
        successor.addPredecessor(outsideSprint, true);
        sprint.setTasks(List.of(successor, explicit, resourceOrder, outsideSprint));
        Worklog foreignLog = worklog(successor, chart.meta.sprintStart, 100);
        foreignLog.setSprintId(outsideSprint.getSprintId());
        sprint.setWorklogs(List.of(worklog(successor, chart.meta.sprintStart.minusDays(1), 1),
                worklog(successor, chart.meta.now.toLocalDate().atStartOfDay().minusSeconds(1), 2),
                worklog(successor, chart.meta.now.toLocalDate().atStartOfDay(), 4), foreignLog));

        var snapshot = service.capture(sprint, chart);
        successor.getPredecessors().clear();
        successor.setMaxEstimate(Duration.ofHours(8));
        assertEquals(3, snapshot.tasks().size());
        var captured = snapshot.tasks().stream().filter(fact -> fact.taskId().equals(successor.getId())).findFirst().orElseThrow();
        assertEquals(new BurndownEvidenceService.TaskEvidence(successor.getId(), true, TaskStatus.IN_PROGRESS,
                successor.getStart(), null, 7200L, 10800L,
                List.of(explicit.getId()), List.of(resourceOrder.getId()), 1), captured);
    }

    @Test
    void distinguishesZeroEffortFromMissingWorkAndRejectsChartsFromAnotherSprint() {
        GanttBurndownChartDto chart = chart();
        Sprint sprint = sprint(chart);
        Task task = new Task();
        sprint.setTasks(List.of(task));

        assertEquals(0L, service.capture(sprint, chart).tasks().getFirst().loggedSeconds());
        sprint.setWorklogs(List.of(worklog(task, null, 1)));
        assertNull(service.capture(sprint, chart).tasks().getFirst().loggedSeconds());
        sprint.setWorklogs(null);
        assertNull(service.capture(sprint, chart).tasks().getFirst().loggedSeconds());
        assertThrows(IllegalArgumentException.class, () -> service.capture(new Sprint(), chart));
    }

    @Test
    void detectsCrossingTheEarlierMaximumEvenAfterAnEstimateIncrease() {
        GanttBurndownChartDto chart = chart();
        Sprint sprint = sprint(chart);
        Task task = effortTask(2);
        sprint.setTasks(List.of(task));
        LocalDateTime firstWork = chart.meta.sprintStart;
        LocalDateTime crossingWork = chart.meta.now.toLocalDate().atTime(9, 0);
        sprint.setWorklogs(List.of(worklog(task, firstWork, 2)));
        var prior = service.capture(sprint, chart);

        task.setMaxEstimate(Duration.ofHours(5));
        sprint.setWorklogs(List.of(worklog(task, firstWork, 2), worklog(task, crossingWork, 1)));
        chart.meta.now = chart.meta.now.plusDays(1);
        var result = new EstimateOverrunContributor().evaluate(service.capture(sprint, chart), prior);

        assertEquals(EstimateOverrunContributor.Coverage.COMPARABLE_WINDOW, result.coverage());
        assertEquals(crossingWork.toLocalDate().minusDays(1), result.afterDate());
        assertEquals(crossingWork.toLocalDate(), result.throughDate());
        assertEquals(1, result.comparedTasks());
        assertEquals(0, result.unavailableTasks());
        assertEquals(List.of(new EstimateOverrunContributor.Observation(task.getId(), task.getTaskStatus(), prior.chart().capturedAt(),
                7200L, 7200L, 10800L, 3600L)), result.observations());
    }

    @Test
    void distinguishesNoNewOverrunFromUnavailableTaskHistory() {
        GanttBurndownChartDto chart = chart();
        Sprint sprint = sprint(chart);
        Task alreadyOver = effortTask(1);
        Task unchanged = effortTask(2);
        Task edited = effortTask(3);
        Task missingEstimate = effortTask(0);
        Task newTask = effortTask(1);
        LocalDateTime day = chart.meta.sprintStart;
        sprint.setTasks(List.of(alreadyOver, unchanged, edited, missingEstimate));
        sprint.setWorklogs(List.of(worklog(alreadyOver, day, 2), worklog(unchanged, day, 2), worklog(edited, day, 2)));
        var prior = service.capture(sprint, chart);

        chart.meta.now = chart.meta.now.plusDays(1);
        sprint.setTasks(List.of(alreadyOver, unchanged, edited, missingEstimate, newTask));
        sprint.setWorklogs(List.of(worklog(alreadyOver, day, 3), worklog(unchanged, day, 2), worklog(edited, day, 1),
                worklog(missingEstimate, day, 3), worklog(newTask, day, 3)));
        var result = new EstimateOverrunContributor().evaluate(service.capture(sprint, chart), prior);

        assertEquals(2, result.comparedTasks());
        assertEquals(3, result.unavailableTasks());
        assertEquals(List.of(), result.observations());
    }

    @Test
    void requiresEarlierEvidenceFromTheSameSprintAndAnAdvancingCutoff() {
        GanttBurndownChartDto chart = chart();
        Sprint sprint = sprint(chart);
        var snapshot = service.capture(sprint, chart);
        var contributor = new EstimateOverrunContributor();
        assertEquals(EstimateOverrunContributor.Coverage.NO_PRIOR_SNAPSHOT, contributor.evaluate(snapshot, null).coverage());

        chart.meta.now = chart.meta.now.plusMinutes(1);
        assertEquals(EstimateOverrunContributor.Coverage.NON_ADVANCING_WINDOW,
                contributor.evaluate(service.capture(sprint, chart), snapshot).coverage());
        chart.meta.now = null;
        assertEquals(EstimateOverrunContributor.Coverage.NO_COMPLETED_DAY_CUTOFF,
                contributor.evaluate(service.capture(sprint, chart), snapshot).coverage());

        GanttBurndownChartDto otherChart = chart();
        var otherSnapshot = service.capture(sprint(otherChart), otherChart);
        assertThrows(IllegalArgumentException.class, () -> contributor.evaluate(snapshot, otherSnapshot));
    }

    @Test
    void groupsUnfinishedExplicitPrerequisitesAfterThePreviouslyScheduledStart() {
        GanttBurndownChartDto chart = chart();
        Sprint sprint = sprint(chart);
        Task successor = effortTask(2);
        Task first = effortTask(1);
        Task second = effortTask(1);
        Task resourceOrder = effortTask(1);
        Task finished = effortTask(1);
        Task outsideSprint = effortTask(1);
        LocalDateTime priorStart = chart.meta.now.plusHours(1);
        successor.setStart(priorStart);
        successor.addPredecessor(first, true);
        successor.addPredecessor(second, true);
        successor.addPredecessor(resourceOrder, false);
        successor.addPredecessor(finished, true);
        successor.addPredecessor(outsideSprint, true);
        sprint.setTasks(List.of(successor, first, second, resourceOrder, finished));
        var prior = service.capture(sprint, chart);

        chart.meta.now = chart.meta.now.plusDays(1);
        successor.setStart(chart.meta.now.plusDays(1));
        second.setTaskStatus(TaskStatus.IN_PROGRESS);
        finished.setTaskStatus(TaskStatus.DONE);
        var result = new DependencyBlockerContributor().evaluate(service.capture(sprint, chart), prior);

        assertEquals(DependencyBlockerContributor.Coverage.COMPARABLE_WINDOW, result.coverage());
        assertEquals(1, result.unavailableTasks());
        var blockers = List.of(first.getId(), second.getId()).stream().sorted().toList();
        assertEquals(List.of(new DependencyBlockerContributor.Observation(successor.getId(), priorStart, blockers)),
                result.observations());
    }

    @Test
    void doesNotTurnCoincidenceOrAChangedDependencyIntoAnObservedBlocker() {
        GanttBurndownChartDto chart = chart();
        Sprint sprint = sprint(chart);
        Task predecessor = effortTask(1);
        Task reopened = effortTask(1);
        reopened.setTaskStatus(TaskStatus.DONE);
        Task started = effortTask(1);
        Task futureStart = effortTask(1);
        Task newEdge = effortTask(1);
        Task afterReopen = effortTask(1);
        Task resourceOnly = effortTask(1);
        LocalDateTime start = chart.meta.now.plusHours(1);
        started.setStart(start);
        started.addPredecessor(predecessor, true);
        futureStart.setStart(start.plusDays(1));
        futureStart.addPredecessor(predecessor, true);
        newEdge.setStart(start);
        afterReopen.setStart(start);
        afterReopen.addPredecessor(reopened, true);
        resourceOnly.setStart(start);
        resourceOnly.addPredecessor(predecessor, false);
        sprint.setTasks(List.of(predecessor, reopened, started, futureStart, newEdge, afterReopen, resourceOnly));
        var prior = service.capture(sprint, chart);

        chart.meta.now = chart.meta.now.plusDays(1);
        sprint.setWorklogs(List.of(worklog(started, start, 1)));
        newEdge.addPredecessor(predecessor, true);
        reopened.setTaskStatus(TaskStatus.TODO);
        var result = new DependencyBlockerContributor().evaluate(service.capture(sprint, chart), prior);

        assertEquals(2, result.unavailableTasks());
        assertEquals(List.of(), result.observations());
    }

    @Test
    void requiresAnEarlierSameSprintCaptureButAllowsABaselineBeforeAnyCompletedDay() {
        GanttBurndownChartDto chart = chart();
        Sprint sprint = sprint(chart);
        chart.meta.now = chart.meta.sprintStart;
        var beforeFirstDay = service.capture(sprint, chart);
        var contributor = new DependencyBlockerContributor();
        assertEquals(DependencyBlockerContributor.Coverage.NO_PRIOR_SNAPSHOT,
                contributor.evaluate(beforeFirstDay, null).coverage());
        assertEquals(DependencyBlockerContributor.Coverage.NO_COMPLETED_DAY_CUTOFF,
                contributor.evaluate(beforeFirstDay, beforeFirstDay).coverage());

        chart.meta.now = chart.meta.now.plusDays(1);
        var current = service.capture(sprint, chart);
        assertEquals(DependencyBlockerContributor.Coverage.COMPARABLE_WINDOW,
                contributor.evaluate(current, beforeFirstDay).coverage());
        assertEquals(DependencyBlockerContributor.Coverage.NON_ADVANCING_WINDOW,
                contributor.evaluate(current, current).coverage());
        var otherChart = chart();
        assertThrows(IllegalArgumentException.class, () -> contributor.evaluate(current, service.capture(sprint(otherChart), otherChart)));
    }

    @Test
    void countsDistinctScheduledAssigneesOnlyOnCompletedSicknessOnlyWorkingDays() {
        GanttBurndownChartDto chart = chart();
        Sprint sprint = sprint(chart);
        var calendar = sprint.getProjectFile().addDefaultBaseCalendar();
        sprint.setCalendar(calendar);
        LocalDate monday = chart.meta.sprintStart.toLocalDate();
        LocalDate tuesday = monday.plusDays(1);
        User first = sickUser(sprint, monday, monday.plusDays(2));
        User second = sickUser(sprint, monday, monday.plusDays(2));
        User unassigned = sickUser(sprint, monday, monday.plusDays(2));
        first.getCalendar().addCalendarException(tuesday).setName(OffDayType.VACATION.name());
        sprint.setUserMap(Map.of(first.getId(), first, second.getId(), second, unassigned.getId(), unassigned));

        Task duplicateAssignment = scheduledTask(first, monday, monday.plusDays(2));
        Task firstTask = scheduledTask(first, monday, monday.plusDays(2));
        Task secondTask = scheduledTask(second, monday, monday.plusDays(2));
        Task missingSchedule = effortTask(1);
        missingSchedule.setResourceId(second.getId());
        sprint.setTasks(List.of(firstTask, duplicateAssignment, secondTask, missingSchedule));
        var result = new CapacityOverlapContributor().evaluate(sprint, chart);

        assertEquals(CapacityOverlapContributor.Coverage.AVAILABLE, result.coverage());
        assertEquals(tuesday, result.throughDate());
        assertEquals(1, result.tasksWithMissingInputs());
        assertEquals(List.of(new CapacityOverlapContributor.DayOverlap(monday, 2)), result.overlaps());

        sprint.setTasks(List.of(firstTask, duplicateAssignment));
        assertEquals(List.of(), new CapacityOverlapContributor().evaluate(sprint, chart).overlaps());
    }

    @Test
    void doesNotCountAProjectHolidayOrAnAssignmentEndingAtTheStartOfTheDay() {
        GanttBurndownChartDto chart = chart();
        Sprint sprint = sprint(chart);
        var calendar = sprint.getProjectFile().addDefaultBaseCalendar();
        sprint.setCalendar(calendar);
        LocalDate monday = chart.meta.sprintStart.toLocalDate();
        LocalDate tuesday = monday.plusDays(1);
        calendar.addCalendarException(monday).setName("Holiday");
        User first = sickUser(sprint, monday, tuesday);
        User second = sickUser(sprint, monday, tuesday);
        sprint.setUserMap(Map.of(first.getId(), first, second.getId(), second));
        Task firstTask = scheduledTask(first, monday, tuesday);
        Task secondTask = scheduledTask(second, monday, tuesday);
        secondTask.setFinish(tuesday.atStartOfDay());
        sprint.setTasks(List.of(firstTask, secondTask));

        var contributor = new CapacityOverlapContributor();
        assertEquals(List.of(), contributor.evaluate(sprint, chart).overlaps());
        sprint.setCalendar(null);
        assertEquals(CapacityOverlapContributor.Coverage.NO_SPRINT_CALENDAR, contributor.evaluate(sprint, chart).coverage());
        assertThrows(IllegalArgumentException.class, () -> contributor.evaluate(new Sprint(), chart));
    }

    @Test
    void comparesBothForecastDirectionsUsingRemainingEffortWithoutChangingTheLiveSchedule() {
        var chart = chart();
        Sprint sprint = sprint(chart);
        sprint.initialize();
        chart.meta.now = chart.meta.now.toLocalDate().atTime(8, 0);
        Task task = effortTask(40);
        task.setMinEstimate(Duration.ofHours(32));
        task.setRemainingEstimate(Duration.ofHours(2));
        task.setStart(chart.meta.sprintStart);
        sprint.setTasks(List.of(task));
        var contributor = new ReleaseForecastContributor();

        chart.meta.releaseDate = chart.meta.now.minusDays(1);
        var earlier = contributor.evaluate(sprint, chart);
        assertEquals(ReleaseForecastContributor.Coverage.AVAILABLE, earlier.coverage());
        assertEquals(ReleaseForecastContributor.Direction.LINEAR_EARLIER, earlier.direction());
        assertEquals(1L, earlier.differenceInCalendarDays());
        assertEquals(chart.meta.now.plusHours(2), earlier.scheduledRelease());
        assertEquals(chart.meta.sprintStart, task.getStart());
        assertEquals(Duration.ofHours(32), task.getMinEstimate());
        assertEquals(1, sprint.getTasks().size());

        chart.meta.releaseDate = chart.meta.now.plusDays(1);
        var later = contributor.evaluate(sprint, chart);
        assertEquals(ReleaseForecastContributor.Direction.LINEAR_LATER, later.direction());
        assertEquals(-1L, later.differenceInCalendarDays());

        chart.meta.releaseDate = chart.meta.now.plusHours(4);
        assertEquals(ReleaseForecastContributor.Direction.SAME_DAY, contributor.evaluate(sprint, chart).direction());
    }

    @Test
    void respectsDependencyAvailabilityAndCalendarInTheRemainingWorkSchedule() {
        var chart = chart();
        Sprint sprint = sprint(chart);
        sprint.initialize();
        chart.meta.now = chart.meta.now.toLocalDate().atTime(8, 0);
        chart.meta.releaseDate = chart.meta.now.plusDays(1);
        User user = sickUser(sprint, chart.meta.now.toLocalDate(), chart.meta.now.toLocalDate());
        user.addAvailability(new Availability(0.5f, chart.meta.sprintStart.toLocalDate()));
        sprint.setUserMap(Map.of(user.getId(), user));
        Task first = effortTask(5);
        first.setRemainingEstimate(Duration.ofHours(4));
        first.setResourceId(user.getId());
        first.setOrderId(1);
        Task successor = effortTask(5);
        successor.setRemainingEstimate(Duration.ofHours(1));
        successor.setOrderId(2);
        successor.addPredecessor(first, true);
        Task done = effortTask(5);
        done.setTaskStatus(TaskStatus.DONE);
        successor.addPredecessor(done, true);
        sprint.setTasks(List.of(first, successor, done));

        var result = new ReleaseForecastContributor().evaluate(sprint, chart);
        assertEquals(ReleaseForecastContributor.Coverage.AVAILABLE, result.coverage());
        assertEquals(chart.meta.now.plusDays(2).plusMinutes(90), result.scheduledRelease());
        assertEquals(2, successor.getPredecessors().size());
    }

    @Test
    void marksMissingScheduleInputsUnavailableRatherThanReturningAProjection() {
        var chart = chart();
        Sprint sprint = sprint(chart);
        sprint.initialize();
        chart.meta.releaseDate = chart.meta.now.plusDays(1);
        Task task = effortTask(2);
        task.setRemainingEstimate(Duration.ofHours(1));
        task.addPredecessor(effortTask(1), true);
        sprint.setTasks(List.of(task));
        var contributor = new ReleaseForecastContributor();
        var result = contributor.evaluate(sprint, chart);
        assertEquals(ReleaseForecastContributor.Coverage.INCOMPLETE_SCHEDULE, result.coverage());
        assertNull(result.scheduledRelease());
        assertThrows(IllegalArgumentException.class, () -> contributor.evaluate(new Sprint(), chart));

        task.setRemainingEstimate(Duration.ZERO);
        assertEquals(ReleaseForecastContributor.Coverage.NO_REMAINING_WORK, contributor.evaluate(sprint, chart).coverage());
    }

    @Test
    void ranksAConnectedHandoffChainOnceAndDoesNotAttributeTheGapToIt() {
        var chart = chart();
        Sprint sprint = sprint(chart);
        Task upstream = effortTask(1);
        Task handoff = effortTask(1);
        Task downstream = effortTask(1);
        Task independent = effortTask(1);
        LocalDateTime plannedStart = chart.meta.now.plusMinutes(15);
        handoff.setStart(plannedStart);
        downstream.setStart(plannedStart);
        handoff.addPredecessor(upstream, true);
        downstream.addPredecessor(handoff, true);
        independent.addPredecessor(downstream, false);
        sprint.setTasks(List.of(upstream, handoff, downstream, independent));
        var prior = service.capture(sprint, chart);

        upstream.setTaskStatus(TaskStatus.IN_PROGRESS);
        sprint.setWorklogs(List.of(worklog(upstream, plannedStart, 2), worklog(independent, plannedStart, 4)));
        chart.meta.now = chart.meta.now.plusDays(1);
        chart.ganttGuideWithBuffer = List.of(21600L, 14400L, 10800L, 6300L);
        var brief = explanationService().explain(sprint, chart, prior);

        assertEquals(BurndownExplanationService.GapAttribution.NOT_QUANTIFIED, brief.gapAttribution());
        assertEquals(2700L, brief.chart().comparison().gapSeconds());
        assertEquals(2, brief.taskGroups().size());
        var chain = brief.taskGroups().getFirst();
        assertEquals(BurndownExplanationService.Action.REVIEW_BLOCKED_HANDOFF, chain.suggestedAction());
        assertEquals(List.of(handoff.getId(), downstream.getId()).stream().sorted().toList(),
                chain.blockers().stream().map(DependencyBlockerContributor.Observation::taskId).sorted().toList());
        assertEquals(List.of(upstream.getId()), chain.overruns().stream().map(EstimateOverrunContributor.Observation::taskId).toList());
        var separate = brief.taskGroups().getLast();
        assertEquals(BurndownExplanationService.Action.REVIEW_ESTIMATE_AND_REMAINING_WORK, separate.suggestedAction());
        assertEquals(List.of(independent.getId()), separate.overruns().stream().map(EstimateOverrunContributor.Observation::taskId).toList());
        assertEquals(4, brief.taskCoverage().comparedEstimateTasks());
    }

    @Test
    void keepsCapacityAndOutlookSeparateAndDistinguishesMissingHistoryFromNoPositiveGap() {
        var chart = chart();
        Sprint sprint = sprint(chart);
        sprint.initialize();
        LocalDate day = chart.meta.sprintStart.toLocalDate();
        User first = sickUser(sprint, day, day);
        User second = sickUser(sprint, day, day);
        sprint.setUserMap(Map.of(first.getId(), first, second.getId(), second));
        sprint.setTasks(List.of(scheduledTask(first, day, day), scheduledTask(second, day, day)));
        chart.ganttGuideWithBuffer = List.of(21600L, 18000L, 14400L, 0L);
        var explanation = explanationService();

        var brief = explanation.explain(sprint, chart, null);
        assertEquals(BurndownExplanationService.GapAttribution.NO_POSITIVE_GAP, brief.gapAttribution());
        assertEquals(List.of(), brief.taskGroups());
        assertEquals(DependencyBlockerContributor.Coverage.NO_PRIOR_SNAPSHOT, brief.taskCoverage().dependencyCoverage());
        assertEquals(EstimateOverrunContributor.Coverage.NO_PRIOR_SNAPSHOT, brief.taskCoverage().estimateCoverage());
        assertEquals(List.of(new CapacityOverlapContributor.DayOverlap(day, 2)), brief.capacity().overlaps());
        assertEquals(ReleaseForecastContributor.Coverage.NO_LINEAR_FORECAST, brief.outlook().coverage());

        chart.ganttGuideWithBuffer = null;
        assertEquals(BurndownExplanationService.GapAttribution.NO_COMPARISON, explanation.explain(sprint, chart, null).gapAttribution());
    }

    private BurndownExplanationService explanationService() {
        var explanation = new BurndownExplanationService();
        ReflectionTestUtils.setField(explanation, "evidenceService", service);
        ReflectionTestUtils.setField(explanation, "estimateContributor", new EstimateOverrunContributor());
        ReflectionTestUtils.setField(explanation, "dependencyContributor", new DependencyBlockerContributor());
        ReflectionTestUtils.setField(explanation, "capacityContributor", new CapacityOverlapContributor());
        ReflectionTestUtils.setField(explanation, "forecastContributor", new ReleaseForecastContributor());
        return explanation;
    }

    private static User sickUser(Sprint sprint, LocalDate first, LocalDate last) {
        User user = new User();
        var calendar = sprint.getProjectFile().addDefaultDerivedCalendar();
        calendar.setParent(sprint.getCalendar());
        calendar.addCalendarException(first, last).setName(OffDayType.SICK.name());
        user.setCalendar(calendar);
        return user;
    }

    private static Task scheduledTask(User user, LocalDate start, LocalDate finish) {
        Task task = effortTask(1);
        task.setResourceId(user.getId());
        task.setStart(start.atTime(8, 0));
        task.setFinish(finish.atTime(16, 30));
        return task;
    }

    private static Task effortTask(long maximumHours) {
        Task task = new Task();
        task.setMinEstimate(Duration.ofHours(1));
        task.setMaxEstimate(Duration.ofHours(maximumHours));
        return task;
    }

    private static Sprint sprint(GanttBurndownChartDto chart) {
        Sprint sprint = new Sprint();
        sprint.setStart(chart.meta.sprintStart);
        chart.sprintId = sprint.getId();
        return sprint;
    }

    private static Worklog worklog(Task task, LocalDateTime start, long hours) {
        Worklog log = new Worklog();
        log.setTaskId(task.getId());
        log.setStart(start);
        log.setTimeSpent(Duration.ofHours(hours));
        return log;
    }

    private static GanttBurndownChartDto chart() {
        GanttBurndownChartDto chart = new GanttBurndownChartDto();
        chart.seriesStart = LocalDate.of(2026, 11, 16);
        chart.meta.sprintStart = chart.seriesStart.atTime(8, 0);
        chart.meta.chartStart = chart.seriesStart.minusDays(5).atStartOfDay();
        chart.meta.now = chart.seriesStart.plusDays(2).atTime(13, 30);
        chart.ganttGuideWithBuffer = List.of(21600L, 14400L, 10800L, 7200L);
        chart.ganttGuideWithoutBuffer = List.of(18000L, 10800L, 7200L, 3600L);
        chart.authors.add(author(7200, 3600, List.of(0L, 3600L, 7200L, 7200L)));
        chart.authors.add(author(5400, 5400, List.of(0L, 0L, 1800L, 5400L)));
        return chart;
    }

    private static AuthorSeriesDto author(long worked, long remaining, List<Long> accumulated) {
        AuthorSeriesDto author = new AuthorSeriesDto();
        author.totalWorkedSeconds = worked;
        author.totalRemainingSeconds = remaining;
        long previous = 0;
        for (Long value : accumulated) {
            author.workPerDay.add(value - previous);
            previous = value;
        }
        return author;
    }
}
