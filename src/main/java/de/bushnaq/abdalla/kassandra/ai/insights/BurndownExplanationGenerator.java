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

package de.bushnaq.abdalla.kassandra.ai.insights;

import com.openai.errors.OpenAIException;
import de.bushnaq.abdalla.kassandra.dto.TaskStatus;
import de.bushnaq.abdalla.kassandra.service.BurndownExplanationService.Brief;
import de.bushnaq.abdalla.kassandra.service.BurndownExplanationService.GapAttribution;
import de.bushnaq.abdalla.kassandra.service.BurndownExplanationService.TaskGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.FinishReasonCategory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel.ResponseFormat;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;

/**
 * Optionally asks a model to rank the deterministic burn-down review brief.
 * The Java-generated brief remains authoritative: the model receives bounded, task-free summaries
 * under temporary aliases and can only return those aliases, so it cannot invent evidence or replace the baseline.
 */
@Component
@Slf4j
public class BurndownExplanationGenerator {

    /** Initial evaluation candidate; callers can supply another model through the package-private test seam. */
    private static final String MODEL = "qwen/qwen3.8-27b";
    /** Bound task-group payload size so the model request stays focused and predictable. */
    private static final int MAX_TASK_GROUPS = 8;
    /** Limit the model to a short actionable ordering rather than requesting a full rewrite of the brief. */
    private static final int MAX_SELECTION = 3;
    /** Use strict parsing so malformed or ambiguous model output is rejected rather than partially trusted. */
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();
    private static final String INSTRUCTIONS = """
            Rank up to three evidence groups for review of a positive historical sprint burn-down gap.
            The guide is recalculated; it is not the original plan. Prefer actionable evidence affecting
            unfinished work. Blocked handoffs and estimate overruns in one group are linked for review,
            not proof that one caused the other. Aggregate capacity overlap is independent evidence:
            don't link it to tasks by date alone. Never add effects or claim the gap is quantified.
            Choose only supplied groups and abstain with an empty array if none merit review.
            """;

    /** Outcomes distinguish a valid ranking from abstention and from any condition that requires the baseline. */
    public enum Status {
        /** The model returned a valid non-empty ranking. */
        SELECTED,
        /** The model returned a valid empty ranking because no candidate merited review. */
        ABSTAINED,
        /** No eligible positive-gap brief or review candidates were available. */
        NOT_APPLICABLE,
        /** The model request failed; use the deterministic baseline. */
        REQUEST_FAILED,
        /** The response did not contain a completed generation. */
        INCOMPLETE,
        /** The response was not a valid ranking of the request's allowed aliases. */
        INVALID_SELECTION
    }

    /**
     * A model-selected item resolved back to server-owned evidence.
     * Aggregate capacity is deliberately represented separately because it has no task group.
     */
    public sealed interface Review permits TaskReview, CapacityReview {
    }

    /**
     * A task-related evidence group selected for human review.
     *
     * @param group server-generated observations for the selected group
     */
    public record TaskReview(TaskGroup group) implements Review {
    }

    /** Aggregate sickness-overlap evidence selected without exposing individual people or tasks. */
    public record CapacityReview() implements Review {
    }

    /** Model-selection result that always retains the deterministic baseline. */
    public record Result(Brief baseline, Status status, List<Review> orderedReviews) {
        /**
         * @param baseline unchanged deterministic brief used as the fallback
         * @param status whether a ranking was selected, declined, or unavailable
         * @param orderedReviews validated model-selected order; empty if there is no selection
         */
        public Result {
            // Return a stable result even if the caller later mutates its input list.
            orderedReviews = List.copyOf(orderedReviews);
        }
    }

    /**
     * Bounded model-facing summary paired with the server-side item it is allowed to select.
     *
     * @param id request-local alias returned by the model
     * @param review server-side evidence resolved if the alias is selected
     * @param facts compact summary sent to the model
     */
    private record Candidate(String id, Review review, Map<String, ?> facts) {
    }

    @Autowired
    private ChatModel chatModel;

    /**
     * Requests one native-schema ranking for an already authorized brief, without changing the baseline.
     *
     * @param brief deterministic brief for the selected sprint
     * @return the unchanged baseline and a validated selection or an explicit fallback status
     */
    public Result select(Brief brief) {
        return select(brief, MODEL, false);
    }

    /**
     * Evaluation seam used to compare models and candidate-order sensitivity without changing the public default.
     */
    Result select(Brief brief, String model, boolean reverseCandidates) {
        // A model ranking is useful only when the chart has a positive, unquantified gap and evidence to review.
        if (brief.gapAttribution() != GapAttribution.NOT_QUANTIFIED)
            return new Result(brief, Status.NOT_APPLICABLE, List.of());
        List<Candidate> candidates = candidates(brief);
        if (candidates.isEmpty())
            return new Result(brief, Status.NOT_APPLICABLE, List.of());

        // Keep the alias-to-evidence mapping local to this request; model output never carries task IDs.
        Map<String, Review> allowed = new LinkedHashMap<>();
        candidates.forEach(candidate -> allowed.put(candidate.id(), candidate.review()));
        long started = System.nanoTime();
        ChatResponse response;
        try {
            // Reversing only the presentation order is an evaluation probe for position bias.
            response = chatModel.call(prompt(brief, reverseCandidates ? candidates.reversed() : candidates, allowed.keySet(), model));
        } catch (OpenAIException e) {
            return fallback(brief, Status.REQUEST_FAILED, started, model);
        }
        // A response without a completed generation may be truncated; do not infer a ranking from partial text.
        var generation = response == null ? null : response.getResult();
        if (generation == null || generation.getMetadata().getFinishReasonCategory() != FinishReasonCategory.COMPLETED)
            return fallback(brief, Status.INCOMPLETE, started, model);

        // Accept only a complete, unique ordering of aliases from this request's allow-list.
        List<Review> reviews = resolve(generation.getOutput().getText(), allowed);
        if (reviews == null)
            return fallback(brief, Status.INVALID_SELECTION, started, model);
        Status status = reviews.isEmpty() ? Status.ABSTAINED : Status.SELECTED;
        log.debug("Burn-down selection model={} status={} elapsedMs={} usage={}", model, status, elapsedMs(started),
                response.getMetadata().getUsage());
        return new Result(brief, status, reviews);
    }

    private static Prompt prompt(Brief brief, List<Candidate> candidates, Set<String> ids, String model) {
        // The schema restricts each answer to known aliases and caps the requested ranking.
        String schema = JSON.writeValueAsString(Map.of("type", "object", "additionalProperties", false,
                "required", List.of("orderedGroupIds"), "properties", Map.of("orderedGroupIds",
                        Map.of("type", "array", "maxItems", MAX_SELECTION, "items", Map.of("type", "string", "enum", ids)))));
        // Stable generation settings make evaluation repeatable where the model server supports them.
        var options = OpenAiChatOptions.builder().model(model).temperature(0.0).seed(42)
                .reasoningEffort("medium").maxTokens(4096).maxRetries(0)
                .responseFormat(ResponseFormat.builder().jsonSchema(schema).strict(true).build()).build();
        // Send only brief-level context and compact candidate facts; no names, worklog text, or task identifiers are needed.
        String facts = JSON.writeValueAsString(Map.of(
                "throughDate", brief.chart().throughDate().toString(), "comparisonSeconds", brief.chart().comparison(),
                "taskCoverage", brief.taskCoverage(), "capacityCoverage", brief.capacity().coverage(),
                "taskGroupsOmitted", Math.max(0, brief.taskGroups().size() - MAX_TASK_GROUPS),
                "candidates", candidates.stream().map(Candidate::facts).toList()));
        return new Prompt(List.of(new SystemMessage(INSTRUCTIONS), new UserMessage(facts)), options);
    }

    private static List<Candidate> candidates(Brief brief) {
        List<Candidate> candidates = new ArrayList<>();
        // The brief already ranks groups deterministically; preserve that order and omit excess groups from the prompt.
        for (var group : brief.taskGroups().stream().limit(MAX_TASK_GROUPS).toList()) {
            String id = "G" + (candidates.size() + 1);
            Set<UUID> prerequisites = new HashSet<>();
            // This count is descriptive evidence only; a prerequisite link does not prove that the overrun caused a block.
            group.blockers().forEach(blocker -> prerequisites.addAll(blocker.unfinishedPredecessors()));
            candidates.add(new Candidate(id, new TaskReview(group), Map.of("id", id, "type", "TASK_GROUP",
                    "blockedHandoffs", group.blockers().size(), "overruns", group.overruns().size(),
                    "overrunningPrerequisites", group.overruns().stream().filter(overrun -> prerequisites.contains(overrun.taskId())).count(),
                    "largestOverrunSeconds", group.overruns().stream().mapToLong(o -> o.excessSeconds()).max().orElse(0),
                    "unfinishedOverruns", group.overruns().stream().filter(o -> o.currentStatus() == TaskStatus.TODO
                            || o.currentStatus() == TaskStatus.IN_PROGRESS).count(),
                    "action", group.suggestedAction())));
        }
        var overlaps = brief.capacity().overlaps();
        // Capacity has no task group and remains an independent aggregate candidate.
        if (!overlaps.isEmpty()) {
            String id = "G" + (candidates.size() + 1);
            candidates.add(new Candidate(id, new CapacityReview(), Map.of("id", id, "type", "AGGREGATE_CAPACITY",
                    "overlapDays", overlaps.size(), "peakUnavailableAssignees",
                    overlaps.stream().mapToInt(day -> day.unavailableAssignees()).max().orElseThrow())));
        }
        return candidates;
    }

    private static List<Review> resolve(String text, Map<String, Review> allowed) {
        // Bound parsing work and reject absent output before attempting JSON interpretation.
        if (text == null || text.isBlank() || text.length() > 512)
            return null;
        try {
            var root = JSON.readTree(text);
            var ids = root.get("orderedGroupIds");
            // Require exactly the requested object shape; extra fields could conceal an unsupported response.
            if (!root.isObject() || root.size() != 1 || ids == null || !ids.isArray() || ids.size() > MAX_SELECTION)
                return null;
            Set<String> seen = new HashSet<>();
            List<Review> reviews = new ArrayList<>();
            // Reject unknown and repeated aliases; silently dropping them could change the model's intended order.
            for (var id : ids) {
                if (!id.isString() || !allowed.containsKey(id.stringValue()) || !seen.add(id.stringValue()))
                    return null;
                reviews.add(allowed.get(id.stringValue()));
            }
            return reviews;
        } catch (JacksonException e) {
            return null;
        }
    }

    private static Result fallback(Brief brief, Status status, long started, String model) {
        // A failed selection is observable in logs and returns the deterministic brief unchanged.
        log.warn("Burn-down selection used the deterministic baseline: model={} status={} elapsedMs={}",
                model, status, elapsedMs(started));
        return new Result(brief, status, List.of());
    }

    /** Converts a monotonic timer reading into elapsed milliseconds for diagnostic logging. */
    private static long elapsedMs(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }
}
