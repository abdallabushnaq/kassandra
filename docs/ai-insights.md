# Ai Insights

## New Direction

Create as much insights as possible using java, then ask the model to summarize it focusing on the most important
insights and describing it to someone missing context.

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

# Direction

**Question**: I am confused. You wrote 4 contributors, but the result of each is different and not comparable with each
other. In the end members of the team want to understand what caused the main delay, so that we can think about
countermeasures. For that we need to have comparability between the contributors.
A good candidate would have been person days of delay.
Can you explain in short the reasoning behind your approach?   
Is there something I am missing?
please do not change any code.

**Answer**: You’re not missing anything. The contributors were designed to surface evidence worth investigating, not to
measure what caused the most delay. Estimate overruns report extra logged effort; dependency blockers identify
unfinished handoffs; sickness overlap counts simultaneous absences. The release forecast is different again: it looks
forward and isn’t evidence of a past cause. The brief ranks what to review first, not what had the biggest impact.
Person-days are a good common unit for effort or capacity lost, but not necessarily for release delay. Losing two
person-days on work that had room to slip might not move the release at all; one blocked handoff can move it by several
days. If the team wants to prioritize countermeasures by delivery impact, I’d compare working days of release delay:
replay the same historical schedule with and without each factor, using a saved baseline and reliable history. Those
estimates can overlap, so they shouldn’t simply be added. The current prototype doesn’t make that comparison, and AI
ranking can’t fill in the missing evidence.

## Model evaluation and background execution

Keep Qwen3.8-27B as the first candidate. Use Qwen3.5-9B as a comparison, not as a replacement based on
inference speed alone. The initial LM Studio probes established native JSON output, not consistent
ranking quality. The one improvement over the current deterministic brief favored an unfinished
overrun over a resolved one, a preference the baseline can also implement.

Evaluate both models through the same Spring AI selector on frozen evidence packets, against a
deterministic baseline that also accounts for unfinished work. Include competing signals, similar
curves with different drivers, and cases that require abstention. Define acceptable leading evidence
groups and review actions before running the holdout. Repeat identical requests with the model build,
quantization, runtime, prompt, schema, and generation settings pinned; separately shuffle candidate
order to check for position bias. Measure agreement on the leading evidence and action, appropriate
abstention, and fallbacks. A model must improve the review decision over the baseline consistently;
well-formed JSON and faster inference are not enough.

If a model passes, run selection off the chart request path, for example from a scheduled job on new
snapshots. Reuse the validated result for unchanged evidence and model configuration. Keep the
deterministic brief available when no current AI result exists, and never present a result from an
older snapshot as current.

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

## Demo and handoff

In **Active Sprints**, select one sprint and click **Burn-down brief** beside the chart controls.
The preview uses the same chart and shows the completed-day gap, aggregate capacity observations,
and the separate release outlook. It does not call a model. Without persisted earlier snapshots,
the preview reports estimate-overrun and dependency-blocker history as unavailable.

Next: persist comparable snapshots and add scoped task evidence to the preview. Compare the
selector with a stronger Java baseline on a de-identified holdout and use blinded manager review
before enabling background AI selection. Keep cross-project contention and numerical historical
gap attribution deferred until there is reliable evidence for them.

## Insights page summaries

The Insights page streams Markdown reports as they are generated. **Sprint Summary** lists started
sprints with positive logged effort and sends only the selected sprint's statistics and task facts
to the configured insights model. Its prompt asks for a self-contained explanation grounded in
those facts; it does not use the burn-down evidence selector or infer historical causes.
Delivery-buffer entries are identified as schedule reserve, not unfinished work. Critical findings
may use the theme's error color with an explicit textual label.

When the LM Studio settings category is enabled, Insights loads its configured model using the
shared LM Studio context and load settings before streaming. This may unload another loaded model.
Otherwise the configured OpenAI-compatible endpoint manages model availability. The Insights
reasoning-effort setting defaults to the model's behavior; low, medium and high are sent only when
selected and require model/server support.

To debug streamed Insights requests, set
`logging.level.de.bushnaq.abdalla.kassandra.ai.insights.SprintInsightsGenerator=DEBUG`.
The log contains full prompts and responses, including partial responses on errors or cancellation,
and therefore can contain project data.
