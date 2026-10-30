<!--
Copyright (C) 2025-2026 Abdalla Bushnaq
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Demo burn-down discrepancies

This explains the fixes against the saved `Demo-3_.zip` database snapshot. It
separates **planned work** (the Gantt-derived guide) from **actual work** (the
synthetic worklogs). They were calculated by different paths, and comparing
their slopes without checking both paths made several sprints appear to run
faster or slower than their schedules. Estimate inflation is still disabled;
none of these fixes changes task estimates.

## Data flow and where to review

| Class | Location | Role |
| --- | --- | --- |
| `Demo` | `src\test\java\de\bushnaq\abdalla\kassandra\ui\Demo.java:172-178` | Calls fixture generation or snapshot restore before opening the interactive demo. |
| `AbstractGanttTestUtil` | `src\test\java\de\bushnaq\abdalla\kassandra\util\AbstractGanttTestUtil.java:247-267,313-325,390-401` | Reuses the saved database if present; for fresh data, levels sprints, then generates sickness and worklogs. |
| `H2DatabaseStateManager` | `src\test\java\de\bushnaq\abdalla\kassandra\util\H2DatabaseStateManager.java:99-166` | Exports timestamped snapshots and loads the newest one with a matching prefix. |
| `OffDayGenerator` | `src\test\java\de\bushnaq\abdalla\kassandra\util\OffDayGenerator.java:115-130` | Adds generated absences to the live user calendar as well as the in-memory off-day collection. |
| `BurnDownUtil` | `src\test\java\de\bushnaq\abdalla\kassandra\util\BurnDownUtil.java:104-167` | Advances synthetic worklogs by date and task. |
| `TaskUtil` | `src\main\java\de\bushnaq\abdalla\util\TaskUtil.java:95-169` | Computes each task's possible work on a simulated day. |
| `GanttUtil` / `User` | `src\main\java\de\bushnaq\abdalla\kassandra\report\gantt\GanttUtil.java:393-432`; `src\main\java\de\bushnaq\abdalla\kassandra\dto\User.java:318-342` | Levels the schedule using either actual calendars or isolated calendars without sickness. |
| `QualityBoard` / `ActiveSprints` | `src\main\java\de\bushnaq\abdalla\kassandra\ui\view\QualityBoard.java:631`; `src\main\java\de\bushnaq\abdalla\kassandra\ui\view\ActiveSprints.java:380-381` | Re-level the loaded sprint before building its combined Gantt/burn-down chart. |
| `GanttBurndownChartService` | `src\main\java\de\bushnaq\abdalla\kassandra\service\GanttBurndownChartService.java:335-351,578-726,814-826` | Converts scheduled task times into daily work for the two guides (`withoutBuffer` and `withBuffer`). |
| `GanttBurndownChart` / `BurndownRenderer` | `src\main\frontend\js\burndown\GanttBurndownChart.ts:25-51,83-89`; `src\main\frontend\js\burndown\BurndownRenderer.ts:99-110` | Compose and draw the server-provided chart data; they do not generate the guide or worklogs. No renderer change was needed. |

The **without-buffer** guide is the relevant optimum for estimated work on
cost-bearing tasks. The with-buffer guide includes additional scheduled
non-cost-bearing tasks; neither is a record of what somebody actually worked.
The renderer reads these arrays from the DTO, so an incorrect array appears
as an incorrect line even when drawing is working correctly.

## Root causes and fixes

### 1. The planned guide was changed by sickness generated *after* scheduling

`AbstractGanttTestUtil:390-401,313-325` levels the Gantt schedule before generating
random sick days. Previously, `GanttBurndownChartService:687-697` checked
the task's **current** effective calendar on every intermediate scheduled
day. Once a user was marked sick, that day vanished from the *original plan*,
but the task's scheduled start and finish had not changed. The resulting
guide started below the sprint's estimated work and could look artificially
easy to beat. This is a guide error, not evidence that the estimate was
inflated or that the user worked during sickness.

`GanttBurndownChartService.isPlannedWorkingDay` at `:706-723` now counts a
scheduled intermediate day when **sickness is the only new reason** it is
non-working. It still rejects a non-working sprint day, the user's ordinary
non-working weekday, and other calendar exceptions such as vacation and
holidays. Actual work still checks the effective calendar in
`BurnDownUtil:123`, so a sick day contributes **zero actual work**. This
distinction is intentional: the optimum shows the original plan; actuals
reflect what happened.

That guide correction alone did **not** solve the issue on the combined
charts: both chart views re-level the loaded tasks against the *current*
calendar first. In the saved Oslo sprint, the optimum changed from about
249.987h before that re-leveling to 302.484h afterward (250h estimated);
in Madrid, it changed from about 171.993h to 178.742h (172h estimated).
Sickness postponed the scheduled task finish, and the guide then counted
the sick days *inside the extended schedule* as originally planned work.
That double treatment explains why the actual curve can start below the
minimum guide and later appear to catch up.

`GanttUtil.levelResources(..., boolean includeSickDays)` now lets callers
choose whether sickness changes task dates. Its existing four-argument
entry point retains sick-aware behavior. `QualityBoard` and `ActiveSprints`
explicitly request `false` for their combined charts; other callers still
use actual calendars. `User.createCalendarWithoutSickDays` builds a separate,
temporary derived calendar with the same parent, work weeks, vacations,
and location holidays, excluding only `SICK` off-days. The planner removes
these temporary calendars afterward; it never alters the user's real
calendar, so sickness still prevents actual work.

For sprints whose calendar is their owner's personal calendar,
`GanttBurndownChartService.isPlannedWorkingDay` now checks that calendar's
parent for sprint-wide working days. Otherwise the owner's sick days would
also be rejected as *sprint holidays*, even for an original-plan guide.
Choosing a new sick-aware plan in the future will also require choosing
matching guide semantics; this change adds no project-manager UI control.

`User.initializeLocationsAndOffdays` at
`src\main\java\de\bushnaq\abdalla\kassandra\dto\User.java:318-325` names
restored exceptions with `OffDayType.name()` (for example `SICK`). The
in-memory generator previously named newly added exceptions `sick`. The
one-line change in `OffDayGenerator:129` uses `SICK` in both paths so the
guide can recognize a newly generated sick day. The generator *already*
added off-days to the in-memory calendar; it was the inconsistent exception
**name**, not a missing calendar update, that needed correcting here.

### 2. An afternoon task start lost a second lunch break

For a task spanning multiple days and starting at or after 13:00,
`GanttBurndownChartService:651-655` previously subtracted an hour from the
interval from its start to 16:30. That interval contains no lunch. The fix
counts the whole afternoon interval before applying user availability.
Morning starts still deduct lunch, and the finish-day calculation is
unchanged. This understated the old guide separately from sick days.

### 3. Simulated task work did not follow partial-day Gantt timing

For a task starting and finishing on the same day,
`TaskUtil.getTaskWorkPerDay` at `:121-133` used to apply availability to the
whole elapsed interval **before** deducting a full hour of lunch. For
example, five elapsed hours at 90% availability with an intervening lunch
gave `5h * 0.9 - 1h = 3h30m`; the schedule requires
`(5h - 1h) * 0.9 = 3h36m`. The corrected calculation deducts lunch first.
Even a small remainder can prevent a task from being marked done until the
next day, holding dependent tasks back without anyone being sick.

On a task's scheduled **finish day**, `TaskUtil:150-162` previously offered
the entire 7.5-hour workday even if the Gantt task finished partway through
that day. It now offers the scheduled partial-day amount when the remaining
estimate fits. If work is genuinely behind the scheduled partial day, it
allows a full available day to catch up. The comparison tolerates one
second of float-to-integer rounding; the full-day fallback is rounded to
seconds. The generator still caps this offer to remaining task work and
the user's shared daily capacity.

### 4. One user could log multiple full days on one date

The old `BurnDownUtil.generateWorklogs` evaluated each task independently;
two eligible tasks assigned to one user could each receive a full day.
`BurnDownUtil:109,137-152` now tracks total work by user ID **per date**.
Each task receives at most the lesser of its computed work, the user's
remaining daily availability (`7.5h * availability`), and the task's
remaining estimate. No zero-duration worklog is created. Task order still
decides which eligible task receives the available time first; this is a
capacity limit, not a rescheduling or task-priority algorithm.

## Interpreting the five saved sprints

| Sprint | What the saved data indicates | Relevant corrections |
| --- | --- | --- |
| Barcelona | The first days lagged the optimum despite no early sickness. A same-day task retained a small remainder under the old lunch/availability calculation, so its dependents could not start as scheduled. | Same-day lunch calculation and partial finish-day behavior in `TaskUtil`; corrected afternoon guide work where applicable. |
| Berlin | The completed work matches the minimum estimate, not an inflated maximum. The initially low *without-buffer* guide makes the actual starting level appear close to the higher guideline; subsequent guide work makes it look as though actuals catch up. | Keep late-added sickness out of the original guide; restore lost afternoon work. |
| Prague | The low guide and over-capacity worklogs can make actuals look faster than the schedule. The saved worklogs include a user-day of about **10.501h** against **6.75h** availability. | Planned-day guide rule and shared daily user cap. |
| London | The saved worklogs include a user-day of **12h** against **6h** availability. The guide also lost planned time under the old calendar/lunch logic. | Shared daily user cap and guide fixes. |
| Athens | The saved worklogs include a user-day of **8.75h** against **3.75h** availability; a faster-than-optimum slope is therefore not evidence of a feasible schedule. | Shared daily user cap and guide fixes. |

These are explanations of the **saved, pre-fix data**, not a claim that the
five rendered charts have already been checked after regeneration. Per-task
second truncation can also leave tiny differences between the sum of guide
entries and a sprint's rounded estimate; the fix targets the material
hour-scale discrepancies, not exact equality to the second for every
fixture.

## Regression coverage and remaining fixture step

`BurnDownUtilTest.plannedGuideIgnoresSickDaysAndCountsAfternoonStarts`
(`src\test\java\de\bushnaq\abdalla\kassandra\report\burndown\BurnDownUtilTest.java:205-239`)
checks that a post-scheduling sick day retains planned work, an ordinary
vacation day does not, and an afternoon start retains its work. The same
class tests lunch and scheduled finish-day work at `:325-356`, and shared
user capacity with a skipped sick day at `:358-423`. These focused cases
passed; they do **not** substitute for checking every date of all five
rendered Demo charts.

`BurnDownUtilTest.sicknessFreeLevelingKeepsTheGuideAtTheMinimumEstimate`
uses a small sprint with sickness, vacation, a holiday, and a three-day
work week. It checks that sickness-free task dates yield a guide beginning
at the minimum estimate, sick-aware leveling postpones the task, actual
calendars still reject sick days, and temporary planning calendars are
removed after leveling. The saved Oslo/Madrid snapshot was not regenerated.

`AbstractGanttTestUtil:250-267` restores `Demo-3_.zip` whenever the snapshot
is present. Its persisted worklogs were generated before these fixes and
cannot be repaired by recalculating the chart DTO alone. Leave that
snapshot intact for comparison. To regenerate, preserve the old archive
**outside** `test-database-snapshots` (not merely under a different filename
with the `Demo-3_` prefix), then run `Demo.testDemo` once. The snapshot
manager (`H2DatabaseStateManager:99-166`) exports a newly timestamped
`Demo-3_*.zip` after generation and otherwise chooses the newest matching
archive. Compare the five charts against the preserved fixture. This
regeneration was deliberately not performed as part of the fix because Demo
generation is lengthy and the test then waits for its browser to be closed.
