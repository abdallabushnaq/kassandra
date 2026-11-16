# Failing tests

## Run

- Date: 2026-11-15; revision: `c7ab5bb1`.
- Workflow: `.github/workflows/maven-build.yml`.
- The workflow's `mvn clean -B package --file pom.xml -DskipTests` step succeeded.
- The workflow's `mvn test -Dselenium.headless=true` step failed after 11 minutes 18 seconds:
  **435 tests, 2 failures, 3 errors, 0 skipped**.
- Test environment variables matched the workflow's non-secret settings: `SELENIUM_HEADLESS=true`,
  `DOCKER_HOST=tcp://localhost:2375`, `SERVER_PORT=8080`, and `MAVEN_OPTS=-Xmx2048m`.
- This was a local Windows 11 run with Amazon Corretto 25.0.1 and Chrome 157.0.8103.51, not the
  workflow's Ubuntu runner with Temurin 25. The workflow's fresh `npm ci` and runner/service setup
  were not reproduced. These results therefore do not establish which failures occur on GitHub Actions.
- Details below are from `target/surefire-reports/TEST-*.xml`; parameterized invocation numbers are
  those reported by Surefire for this run.

## Failures and errors

1. **`GanttZoomTest.rendersConfiguredZoomDetails`** — assertion failure in
   `src/test/java/de/bushnaq/abdalla/kassandra/config/GanttZoomTest.java:99` (verification at line 202).
   Browser verification reported `gantt renderTaskProgressBarAtDayWidth visibility at threshold 10.5:
   expected true, found false`, from `gantt-zoom-check.js:484`.
   **Status:** Updated the test for the current renderer; the focused Gantt test class passed on
   2026-11-15. It now checks zoom using progress text and checks the current task geometry.

2. **`GanttZoomTest.inspectsTaskPredecessors`** — `StaleElementReferenceException` in
   `src/test/java/de/bushnaq/abdalla/kassandra/config/GanttZoomTest.java:282`.
   Chrome reported that the selected `.gantt-task.is-selected` element was stale while reading its
   `data-task-id` attribute.
   **Status:** Selection waits now retry stale elements; the focused Gantt test class passed on
   2026-11-15.

3. **`BurndownTest.burnDown(RandomCase, TestInfo)[2]`** — assertion failure for random case index 2
   in `src/test/java/de/bushnaq/abdalla/kassandra/report/burndown/BurndownTest.java:75`.
   `DTOAsserts.assertUserEquals` at line 171 reported that user `Kristen Hubbell` had created date
   `2023-05-05T08:00:08+02:00`, instead of the expected `2023-05-05T08:00:02+02:00`.

4. **`TestTest.test(String, TestInfo)[6]`** — `NullPointerException` in
   `src/test/java/de/bushnaq/abdalla/kassandra/util/EntityGenerator.java:291`: `user` was null when
   calling `getId()`. The call originated in `GanttGenerator.generateWorklogs` at line 530 while
   processing the reference sprint (`AbstractGanttTester.java:110`).

5. **`TestTest.test(String, TestInfo)[7]`** — `NullPointerException` in
   `src/test/java/de/bushnaq/abdalla/kassandra/util/GanttGenerator.java:510`: `Task.getStart()`
   returned null before `toLocalDate()` was called. This occurred while generating worklogs for
   the leveled sprint (`AbstractGanttTester.java:111`).

The two `TestTest` cases use dynamically discovered MPP files. The Surefire report identifies their
invocation numbers but does not record the file names, so the numbers should not be assumed stable
across environments or runs.

## Rerun after clearing fixtures

- Date: 2026-11-15; revision: `08ee9bbd`.
- The user removed previous snapshots and recorded results before the rerun.
- `mvn test -Dselenium.headless=true` failed after 12 minutes 47 seconds:
  **435 tests, 0 failures, 2 errors, 0 skipped**.
- This was a local Windows 11 run with Amazon Corretto 25.0.1. The workflow's environment and
  setup steps were not reproduced; this result does not establish what happens on GitHub Actions.
- `GanttZoomTest` passed all four tests. Both `BurndownTest` cases passed, including failure 3.
  The burndown cases generated fresh test data, and the missing case-2 reference was created from
  the current result during the run. Its pass therefore does not independently validate the
  generated result against the earlier reference.
- The two `TestTest` errors remain: invocation `[6]` again failed because a user was null, and
  invocation `[7]` because a task start was null, at the same locations described above.

## TestTest follow-up

- On 2026-11-16, `MPXJReader` was updated to associate MPP resources with users by resource
  identity, including resources without names.
- `mvn test -Dtest=TestTest -Dselenium.headless=true` ran seven cases: six passed and one
  errored. The previously failing `test8-private.mpp` case passed; `test9-private.mpp` still
  failed with a null task start. The full suite was not rerun.
- Later on 2026-11-16, `MPXJReader` was updated to identify tasks, parents, and predecessors by
  MPP unique ID instead of name. A subsequent `TestTest` run passed all seven cases, including
  `test9-private.mpp`. `CriticalTest` and `GanttTest` also passed. The full suite was not rerun.
