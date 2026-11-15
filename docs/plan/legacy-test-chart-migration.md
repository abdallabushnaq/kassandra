# Migrating test evidence away from legacy charts

## Scope and approval

Replace legacy rendering used to produce test evidence, without removing or
changing the production legacy classes or their UI callers. First migrate and
validate one existing test class. Do not roll out the change to other tests until
the pilot is accepted.

Calendar migration is explicitly out of scope: its UI has already been replaced
with a Vaadin component.

Status: the Gantt and burndown evidence callers using
`AbstractGanttTestUtil` now share the modern rendering path. `BurndownTest`
still has a pre-existing local reference comparison failure described below.

## Findings

- `AbstractGanttTestUtil.generateGanttChart()` and `generateBurndownChart()` already
  receive a sprint UUID. They load the sprint, users, tasks, and worklogs through
  `PersistingEntityGenerator`'s REST clients, initialize the relationships, and
  recalculate the sprint before constructing legacy charts
  (`AbstractGanttTestUtil.java:214-246`).
- `GanttGenerator` also accepts UUIDs, but resolves them from its own
  `EntityGenerator` collections, not from the database
  (`GanttGenerator.java:497-515`). Passing a persisted sprint UUID to a new
  generator is therefore not sufficient, and copying only the sprint into the
  generator would omit its users, tasks, and worklogs.
- The production `GanttChartService.build(Sprint, LocalDateTime, boolean)` and
  `GanttBurndownChartService.build(Sprint, LocalDateTime, boolean)` already accept
  fully populated sprints. There is no need to change a production API or persist
  in-memory fixtures just to render them.
- `GanttGenerator` already renders the production JavaScript charts through a
  local HTTP server and headless Chrome, then writes SVG and PNG. Reuse this
  pipeline rather than introducing a second browser/rendering implementation
  (`GanttGenerator.java:127-183`).
- Gantt rendering in `AbstractGanttTestUtil` also invokes `storeResult()` and
  `compareResults()`. These are scheduling-data regression checks, not rendering
  details, and must remain in the shared test lifecycle.
- `GanttGenerator.generateBurndownChart()` currently renders the combined
  Gantt/burndown chart. Replacing a standalone legacy burndown artifact with it is
  an intentional evidence-layout change, not a byte-compatible replacement.
- Test references to `RenderUtil` outside the shared chart helper are currently
  chart-ID constants used by UI tests. They do not themselves invoke legacy
  rendering and do not require replacement for this migration.

## Original pilot design

### 1. Add a prepared-sprint entry point to `GanttGenerator`

Add these overloads, with Javadoc documenting that the sprint must already have
its users, tasks, worklogs, calendars, and calculated state initialized:

```java
public void generateGanttChart(
        Sprint sprint, String testFolder, String name, double visualScale)
        throws Exception;

public void generateGanttBurndownChart(
        Sprint sprint, String testFolder, String name, double visualScale)
        throws Exception;
```

Here `name` is the output filename stem, without an extension. Explicit names let
the persisted-fixture tests retain their existing case-indexed artifact paths.
Use the generator's existing `testTheme` field and existing capture code.

The current UUID-based methods keep their signatures, naming, theme, scale,
capture sizing, and fixture preparation behavior. Both paths delegate to the
same underlying rendering methods, but the prepared-sprint path fits the
viewport to the timeline so long charts are not cropped. The prepared-sprint overloads must not call
`initializeSprint()`: that method uses the generator's own collections and could
overwrite relationships loaded through REST. They must not level resources,
generate fixtures, or write scheduling state back to the database.

### 2. Separate evidence rendering from regression checks

In `AbstractGanttTestUtil`, extract only the existing legacy chart construction
and rendering into two protected hooks:

```java
protected void renderGanttChart(TestInfo testInfo, Sprint sprint)
        throws Exception;

protected void renderBurndownChart(
        TestInfo testInfo, Sprint sprint, int width, int height)
        throws Exception;
```

The existing `generate*Chart()` methods retain REST loading, initialization,
recalculation, JSON reference handling, and comparisons, then invoke the rendering
hooks. Default hook implementations remain legacy during the pilot, so no other
test class changes renderer implicitly.

This is a temporary migration seam, not a permanent renderer framework or a new
application configuration flag.

### 3. Migrate only `OldGanttTest`

Override the two rendering hooks in `OldGanttTest` and delegate to the new
prepared-sprint overloads. Propagate its selected theme explicitly and retain its
existing output directory and filename stems. Keep all three existing fixture
setups, leveling calls, worklog generation, authentication, database cleanup, and
JSON regression checks unchanged.

The first case selects its named persisted sprint explicitly instead of relying
on the order returned by `sprintApi.getAll()`; otherwise the automatically
created Backlog can be chosen nondeterministically.

Use the modern combined chart for the former standalone burndown artifact:
it retains burndown evidence and adds the associated Gantt view. Document this
layout change; do not claim image parity with the legacy SVG.

The legacy width/height arguments are not equivalent to the modern chart's
visual scale and content-driven dimensions. The pilot uses only the default
legacy dimensions. Keep other callers on the old path until any custom sizing
requirements have been checked, rather than translating pixels into an arbitrary
scale.

Affected Java classes for the pilot:

- `src\test\java\de\bushnaq\abdalla\kassandra\util\GanttGenerator.java`
- `src\test\java\de\bushnaq\abdalla\kassandra\util\AbstractGanttTestUtil.java`
- `src\test\java\de\bushnaq\abdalla\kassandra\report\gantt\OldGanttTest.java`

No Maven dependencies or production code changes are required.

## Pilot validation and acceptance criteria

1. Run the existing `OldGanttTest` before editing to establish its baseline and
   confirm that the three existing local JSON reference files are present. Do not
   regenerate or overwrite reference data to make the migration pass.
2. Implement the bridge and migrate the pilot. Compile with the IDE build tool,
   then run:

   ```bash
   mvn test -Dtest="OldGanttTest,DynamicGanttTest"
   ```

   `OldGanttTest` checks the persisted-fixture bridge and retained comparisons.
   `DynamicGanttTest` guards existing UUID-based Gantt and burndown generation.
   Both carry `@Tag("UnitTest")`; chart evidence additionally requires headless
   Chrome/ChromeDriver and the generated static JavaScript resources. No
   Keycloak container or AI service should be needed.
3. If the static resources are absent or stale, rebuild them with the existing
   `npm run build:static` script before rerunning. Do not install dependencies
   unless the command reports a missing dependency.
4. Require all three pilot methods and their existing scheduling-data comparisons
   to pass. Do not overwrite the existing local JSON reference files.
5. Require a newly generated Gantt SVG/PNG and combined burndown SVG/PNG for each
   pilot case, using case-indexed stems so evidence cannot collide or be confused
   with stale output. Check SVG chart content, task labels, dates, and theme, and
   decode PNGs to verify nonzero, unclipped dimensions. Inspect representative
   artifacts visually rather than accepting file existence alone.
6. Confirm the pilot rendering hooks invoke only the modern path and do not
   construct `LegacyRenderDao`, `LegacyGanttChart`, or `LegacyBurnDownChart`.
   Other tests must continue using their existing rendering paths.
7. Record commands, pass/fail results, artifact locations, and any remaining
   limitations in this document. A pre-existing baseline failure must be reported
   separately and must not be hidden by updating expected results.

The pilot proves the persisted-sprint bridge and Gantt/burndown evidence pipeline.
It does not prove multi-sprint overview equivalence.

### Local pilot result

- The original `OldGanttTest` passed all three cases through an IntelliJ JUnit
  run. The first migrated run exposed nondeterministic selection of Backlog for
  case 1; selecting the named sprint fixed the fixture selection without changing
  the reference files.
- `npm run build:static` and the IntelliJ project build succeeded. The final
  `mvn -q test -Dtest="OldGanttTest,DynamicGanttTest"` run passed all six
  tests, including the three existing JSON comparisons. The reference files
  are local, ignored files rather than checked-in fixtures; the existing helper
  only creates a reference when it is absent.
- All six SVG and six PNG artifacts were generated under
  `test-results\de.bushnaq.abdalla.kassandra.report.gantt.OldGanttTest`.
  The PNGs decoded successfully and matched or exceeded their SVG element
  dimensions. The last child-task label was present in both SVGs for each case;
  representative images were visually inspected for dates, task labels, theme,
  and complete timelines.
- The legacy standalone burndown output is now a combined chart for this pilot.
  Custom legacy pixel dimensions remain unsupported in this pilot. The prepared
  path enlarges the capture viewport for the complete timeline, so long sprints
  can produce wide images; browser screenshot limits remain a consideration for
  larger future fixtures.

### `BurndownTest` migration result

- Both rendering hooks now use prepared sprints and propagate the active
  graphics theme. The former standalone burndown evidence is a combined chart.
  The test selects its generated sprint by name rather than relying on database
  ordering; both existing local references identify that sprint as `Tokyo`.
- The baseline and migrated `mvn -q test -Dtest="BurndownTest"` runs each
  passed case 1 and failed case 2 at the same existing JSON comparison:
  Kristen Hubbell's created timestamp was expected to be
  `2023-05-05T08:00:02+02:00`, but the loaded fixture contained
  `2023-05-05T08:00:08+02:00`. The local reference files were not overwritten.
  Consequently, case 2's remaining comparisons have not been verified.
- Both cases produced case-indexed Gantt and combined burndown SVG/PNG files in
  `test-results\de.bushnaq.abdalla.kassandra.report.burndown.BurndownTest`.
  The images used the dark theme and included their fixture task labels; the
  combined images were visually inspected. PNG capture validates that the
  image covers the SVG element. Custom legacy burndown dimensions remain
  unsupported by this class.

## Shared renderer rollout

- `AbstractGanttTestUtil` now renders prepared sprints directly with
  `GanttGenerator`, using the active theme and preserving the existing names,
  fixture preparation, and JSON comparisons. The temporary overrides in
  `OldGanttTest` and `BurndownTest` and the unused legacy custom-dimensions
  overload are removed. `BurnDownUtilTest` now uses the same path.
- The legacy `BurnDownUtilTest` baseline passed all seven tests. After the
  rollout, `mvn -q test -Dtest="BurnDownUtilTest,BurndownTest,OldGanttTest,DynamicGanttTest"`
  passed 14 of 15 tests; the only failure was the previously documented
  `BurndownTest` case 2 timestamp mismatch. Its references were not changed.
  The three evidence-generating `BurnDownUtilTest` cases each produced Gantt
  and combined burndown SVG/PNG files; representative light-theme images were
  inspected.
- `DynamicGanttTest`, `DynamicGanttBurndownTest`, `OverviewChartTest`, and
  `MPXJReader` already use `GanttGenerator`. Remaining test references to
  `RenderUtil` outside calendar coverage are UI chart identifiers, not legacy
  chart rendering. Calendar tests, legacy production classes, and legacy UI
  callers remain untouched.
