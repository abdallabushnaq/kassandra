# Ai Insights

## New Direction

Create as many insights as possible using java, then ask the model to summarize it focusing on the most important
insights and describing it to someone missing context.

# Unplanned Sick-leave-Impact Detection and Evaluation

Let's write a service that detects impact of unplanned-sick-leave to a given sprint.
It will need to find the critical path (task.isCritical ()) and evaluate if the task-user-calendar has a sick-leave
record for any day of that task. This will delay the task by the number of days of sick-leave. The delay will be
propagated to all dependent tasks.
We measure sprint impact by 2 different values:

1. Schedule-Delay caused by this factor (sum of delay of all tasks on the critical path).
2. Effort-Delay caused by this factor (sum of delay of all tasks).

Let's use SnapshotLogGenerator.getSickDays to test if our detector found the correct days.
SprintInsightsGeneratorIT is using synthetic data from RandomCases with a sick-leave probability of 50% per sprint.
We could run the test with a probability of 0% to generate an alternative sprints that have no delay impact caused by
sick-leave.
This way we can test if our detection algorithm works correctly.

## Plan

write a test in SprintInsightsGeneratorIT that generates 2 different snapshots:

- One with 100% sick-leave probability and
- one with 0% sick-leave probability.

1. iterate over all sprints in the snapshot with the 100% probability.
2. calculate PrintImpact using the Sick-leave-Impact-Contributor.
3. test if our algorithm found all sick-leave days by comparing with SnapshotLogGenerator.getSickDays.
4. test if our algorithm calculated the impact correctly by comparing the results with the snapshot with 0% probability.

# History

SprintInsightsGenerator was a first try to generate insights from sprint data. We used the same data available to a
human project-manager on the QualityBoard view.
As input, we use List<SprintStatistics> and an elaborate prompt.
We are trying to give the AI model as much data as possible to analyze the sprint situation without overloading it with
data.
This try was not successful. Partly because we used a very small model (qwen3.8-27b) not capable of this kind of
thinking. But we are also not so sure about the whole approach. What is possible with a model of this small size?
The tests we run also lacked any actual meaning. How do we test such a feature? This needs to be solved too.

A human project-manager additionally to the QualityBoard panel can get more data from the charts.

1. check excessive unplanned overlapping sick leaves (resource drain) visible in the chart.
2. excessive tasks where actual work exceeded the maximum estimated work (underestimation) visible in the chart.
3. tasks that had to wait for their dependencies and therefore were delayed (domino effect) visible in the chart.
4. tasks that where not started in-time, because their resources where busy in other projects that where delayed
   (resource drain) visible in the chart.
5. The linear `Extrapolated Sprint Release Date` is overly optimistic in burn-down charts that have more resources
   burning down in teh beginning than in the end.
6. The linear `Extrapolated Sprint Release Date` is overly pessimistic in burn-down charts that have less resources
   burning down in the beginning than in the end.

We now want to try to use a little bit different strategy.

## Create as much insights as possible using java, then ask the model to summarize it focusing on the most important insights and describing it to someone missing context.

## Selected-sprint evidence prototype

`BurndownEvidenceService` compares the last completed day's actual remaining effort with the recalculated
with-buffer guide from the same selected-sprint chart. The comparison uses the unpadded series origin and
returns seconds; a positive gap means more work remains than the guide shows. It excludes in-progress-day
burn and reports missing points rather than treating them as zero.

This is a live chart observation, not a reconstruction of the original plan or an as-of historical replay:
the actual series uses the chart's current estimate. The comparison does not assign causes or change the chart.

The server-side task snapshot pairs this comparison with the same sprint's task facts: scheduled dates,
current workflow status and maximum estimate, logged effort through the chart cutoff, and predecessor
IDs. Explicit dependencies are kept separate from scheduler-generated resource ordering. References
outside the loaded sprint are counted, not exposed. Scheduled dates and current status do not prove a
historical dependency wait, and the current estimate is not evidence of what was estimated earlier.
Snapshots are immutable in memory; this step does not persist history, send data to a model, or change the UI.

`EstimateOverrunContributor` uses two captures for the same sprint with advancing completed-day cutoffs.
It flags tasks whose total logged effort newly exceeds the maximum recorded in the earlier capture,
even if the estimate was later raised. Missing baselines and decreasing logged totals are reported as
unavailable, not as evidence of no overrun. An observation may reflect edited or backfilled worklogs;
it does not establish when the overrun happened or how much of the burn-down gap it explains.

`DependencyBlockerContributor` looks for tasks still marked TODO with no logged effort through the
chart cutoff, after a start captured in advance. It groups explicit prerequisites observed unfinished
in both snapshots under one successor. Hidden resource-ordering edges are not blockers; new edges or
missing prerequisite history are not proof of a wait. The result does not measure continuous waiting
time or attribute any portion of the chart gap.

`CapacityOverlapContributor` reports completed days when two or more distinct people with scheduled
sprint tasks were unavailable due to sickness. It uses the recalculated sickness-free chart schedule
and the same working-day rules as the guide. It excludes planned leave and holidays, counts a person
only once per day, and retains no names, task links, or individual absences. The counts do not estimate
lost hours or establish causation. Missing schedule or calendar inputs are reported separately.

`ReleaseForecastContributor` compares the chart's linear release date with a fresh schedule of
remaining task effort. The scenario uses the sprint's current assignments, calendars, availability
and explicit dependency constraints; it excludes delivery buffers and does not change the live
schedule. The signed difference is in calendar days: positive means the remaining-work schedule
finishes later. This is a conditional outlook, not proof of forecast bias or an explanation of the
historical gap. Cross-project contention remains unavailable without reliable allocation evidence.

`BurndownExplanationService` assembles a deterministic review brief. It groups task observations
connected by explicit prerequisites observed as blockers, including multi-step chains, and ranks
groups by blocked handoffs. Among overrun-only groups it prefers unfinished work before ranking
by the largest individual overrun. A dependency link
does not establish that an overrun caused a blocker. Aggregate capacity counts and the forward
release outlook stay separate, and the brief does not subtract any signal from the chart gap.

`BurndownExplanationGenerator.select(Brief)` is the opt-in evaluation path. It sends bounded groups
with request-local aliases and asks for at most three ranked IDs using native JSON Schema. Java
rejects unknown or repeated IDs and incomplete responses; the deterministic brief is always
preserved. There is no scheduler or AI chart integration yet. With the Qwen model loaded in LM Studio
on port 1234, run the synthetic end-to-end transport check with:

```bash
mvn test -Dtest=BurndownEvidenceServiceTest -Dkassandra.burndown.qwen.smoke=true
```

Run the opt-in synthetic ranking evaluation with:

```bash
mvn test '-Dtest=BurndownEvidenceServiceTest#evaluatesQwenUsefulAndRepeatableRankings' -Dkassandra.burndown.qwen.evaluate=true
```

It runs each frozen brief twice unchanged and once with candidate order reversed, keeping IDs, the
schema, and evidence fixed. It reports the expected and actual leading evidence and review action
per run, plus repeat agreement, fallback count, and elapsed time. Missing evidence bypasses the
model and is counted separately. For the aggregate-capacity comparison, the deterministic brief
keeps capacity separate; the evaluation uses its first task group if one exists, and the capacity
observation otherwise. The default model is `qwen/qwen3.8-27b`; to compare the 9B model after loading it,
add `-Dkassandra.burndown.eval.model=qwen/qwen3.5-9b`. This reports results, not a release verdict.

The selector explicitly sends temperature `0`, seed `42`, reasoning effort `medium`, and a
4,096-token generation limit (reasoning and final output combined). These settings do not
guarantee identical output. For the 27B evaluation, load LM Studio with a 32,768-token context
and one concurrent prediction. Context size is a model-load setting, separate from the request's
generation limit. Record the actual context and quantization when comparing results.

In the first 32K run (Qwen3.8-27B, Q4_K_M), Qwen picked the expected lead in eight of nine
live requests, with no fallbacks. All three identical-request pairs agreed; two of three
candidate-order reversals preserved the lead. The missing-history case bypassed Qwen.
The baseline matched three of four fixture expectations. Qwen's only improvement was the
capacity-versus-resolved-overrun case, where reversing candidate order removed that improvement.
That comparison uses a task-first evaluation convention, so it does not establish an advantage
over a baseline that also prioritizes capacity ahead of resolved overruns.

To debug streamed Insights requests, set
`logging.level.de.bushnaq.abdalla.kassandra.ai.insights.SprintInsightsGenerator=DEBUG`.
The log contains full prompts and responses, including partial responses on errors or cancellation,
and therefore can contain project data.
