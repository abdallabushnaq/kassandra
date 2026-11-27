# AI Questions

# Rename Test DB snapshots

Currently, our integration tests generate database snapshots of the synthetic data they generate to reuse on the next
run. These snapshots are stored in 'test-database-snapshots' and are named after the test class that generated them.

We want to rename these snapshots to match the RandomCase parameters it used to generate that data.
This way, snapshots can be reused between tests, if they use the exact same parameters.

In parallel, we also generate a log for generated sick-days sickDayLog. Let's change that name too to match
the <snapshotName>.log. Let's also store it in the same folder as the snapshot, as we will need it as input for later
tests.

## Example

Demo uses RandomCases.listRandomCases.
We can then use a snapshotName (AbstractGanttTestUtil.generateProductsInternal) like <numberOfFeatures>
_<numberOfSprints>_<numberOfTasksPerSprint>_...snapshot.
In this case we would end up with something like 8_8_1_2_1_2_1_5-...snapshot.
We no longer need to use the hash.

Let's add a method to RandomCase.generateName () that will return such a name.

Implement this for Demo and GenerateScreenshotsIT.

# Create Introduction Video for our Interactive Charts

# Add Replay-Slider to Burndown Chart

This slider sits in the x-axis chart part under the burndown week days and only when weekdays are visible.
It is a slider that usually has a knob below today's day or the last day of the chart.
The slider is interactive.
A user can slide the slider back to any previous day in the chart and the chart will redraw to only show work input
until that day.
This allows a user to replay the burndown chart and see how the burndown chart evolved over time.
To allow TypeScript to do this, it needs to know when work was logged.
GanttBurndownChartService calculates usersWorkPerDayAccumulated and hands that over to the TS code.
This pre-cooking needs to be done now in TS code, so that we can draw the chart for any given day.
Let's keep the code very similar to the java code, almost line-by-lline to make code review easier.
Also take over any code comment.
The slider basically controls what worklogs are taking into consideration.
Keep the chart, guide, extrapolated release all the same.
just change the worklogs that are taken into consideration.
Keep the rest of the calculation as it is.
The maximum of the y-axis in BurndownRenderer maxWorked.
This value is first set to sprint.getWorked () + sprint.getRemaining () in GanttBurndownChartService.build.
sprint.getWorked () is calculated as the sum of all tasks timeSpent (see Sprint.propagateTimeTracking).
A task timeSpend is calculated as the sum of all worklog.getTimeSpent () (see Task.addWorklog).
sprint.getRemaining () is calculated as the sum of all tasks remainingEstimate (see Sprint.propagateTimeTracking).
A task remainingEstimate is calculated as the last worklog.getTimeSpent () (see Task.addWorklog).
So, when we slide the slider back to a previous day, we need to recalculate sprint.getWorked () and sprint.getRemaining
().
sprint.getWorked () will only include worklog.getTimeSpent () of work logged until the slide day.
sprint.getRemaining () will only include the worklog.getTimeSpent () of the slide day.

# Add Chart Zoom-O-Meter

Each of our charts has a CaptionElement and FooterElement area.
The CaptionElement shows a leftText and a rightText.
We now want to add a Zoom-O-Meter.
This new element shows a rectangle of header height and a fixed width, lets make it 256px for now.
The left side symbolizes the minimum zoom level, which the right side symbolizes the highest zoom level.
A red vertical red line shows the current zoom level.
black/gray vertical lines show xxx-at-day-width thresholds that trigger some visual element appearance.
Example:
If kassandra.gantt.render-task-border-at-day-width=20, then at zoom level 20, we draw that line. if the red zoom level
is further right, we draw task borders.
every threshold has a tooltip that explains what it triggers.
The number of thresholds depends on the type of chart.

# Better Synthetic Data

We need to generate better synthetic data for our tests with more variety of use cases.

1. excessive unplanned overlapping sick leaves (resource drain). We currently have a probability for this. Let's ensure
   one of the sprints includes it.
2. excessive tasks where actual work exceeded the maximum estimated work (underestimation) visible in the chart. We
   currently have a probability for this. Let's ensure one of the sprints includes it.
3. tasks that had to wait for their dependencies and therefore were delayed (domino effect) visible in the chart. We
   currently have a probability for this to happen, but only due to underestimation. Let's ensure one of the sprints
   includes it.
4. tasks that where not started in-time, because their resources where busy in other projects that where delayed
   (resource drain) visible in the chart. We currently cannot generate this kind of data, as we always burn down sprint
   by sprint and do not take global resource utilization into account. Let's ensure one of the sprints
   includes it.
5. The linear `Extrapolated Sprint Release Date` is overly optimistic in burn-down charts that have more resources
   burning down in the beginning than in the end. We do not generate this kind of sprint. Let's ensure one of the
   sprints
   includes it.
6. The linear `Extrapolated Sprint Release Date` is overly pessimistic in burn-down charts that have less resources
   burning down in the beginning than in the end. We do not generate this kind of sprint. Let's ensure one of the
   sprints includes it.

# Random Case Aware Stable Diffusion Cache for our Tests

Let's make CacheAwareAvatarService use different cache folders for different RandomCases.
If test is run with randomCase 1 and then with RandomCase 2, 2 will overwrite the results of randomCase 1.
To prevent that, it needs to switch the folder.

# Raise coverage to 80%

The overall target is to raise the test coverage to 80%.
Take a look at focs/plan/test-coverage.md and pick one of the *.ui.view.* classes that currently has 0%.

Lets take a look at all

# Server Settings Design

1. create a markdown file docs/design/server-settings-design.md explaining how server settings work.
2. use mermaid to visualize flows or relations if appropriate for this design.

# Page Breadcrumbs

1. several pages do not update the breadcrumbs
2. examples are:
    1. UserProfileView
    2. ServerSettingsView
    3. OidcProviderManagementView
    4. AboutView
3. Use InsightsView as a good example of how to update breadcrumbs.

## Add GDPR support

To add GDPR support we need several intermediate steps:

1. Add settings to control GDPR:
    1. Number of months until an abandoned user gets automatically disabled.
    2. Number of months until a disabled user gets automatically anonymized.
    3. Number of months until an anonymized user gets automatically deleted .
    4. Number of months until an abandoned sprint gets automatically closed.
    5. Number of months until a closed sprint gets automatically deleted.
    6. Number of months until an abandoned feature gets automatically closed.
    7. Number of months until a closed feature gets automatically deleted.
    8. Number of months until an abandoned version gets automatically closed.
    9. Number of months until a closed version gets automatically deleted.
    10. Number of months until an abandoned project gets automatically closed.
    11. Number of months until a closed project gets automatically deleted.
2. define disabled/disabled logic for sprints, features, versions and projects.
    1. If a user has not logged in for X months, the use seen as abandoned.
    2. If a sprint has no worklog updates for X months, it is seen as abandoned.
    3. If a feature has only closed sprints, for X months, it is seen as abandoned.
    4. If a version has only closed features, for X months, it is seen as abandoned.
    5. If a project has only closed versions, for X months, it is seen as abandoned.
3. Implement sprint, feature, version and project closing support. Can be reopened by a user with access.
4. Implement user anonymization. User name and email address should be replaced with a random UUID.
5. Implement a background job that will run every night and check for users, sprints, features, versions and projects
   that need to be anonymized, closed or deleted.
6. We need to mark user/sprints/features/versions/projects as closed, disabled in the database and the UI.
    1. disabled icon (vaadin:lock)
    2. closed icon (vaadin:lock)
    3. predicted Date of deletion in the UI together with icon (vaadin:close-circle).
7. It should be possible to move the predicted Date of deletion to a later date, e.g. if a user is on vacation and will
   be back in 2 months, we should be able to move the predicted Date of deletion to 3 months from now. It should not be
   possible to stop the predicted Date of deletion, as this would be a GDPR violation.
8. Any GDPR related deletion must not be undoable. It must not be available in Hibernate Envers.

# Update Audit Log

1. I now noticed that if the detailed view opens, the list gets a horizontal scroll bar, although no text needs
   scrolling.
   can we try to prevent that?
2. secret values should be encrypted in the database, so instead of trying to guess if a value is a secret or not, we
   could just show the encrypted values.
3. updated server settings should show oldvalue-newvalue.

# Better Pagination Menu

let's add a much more elaborate pagination menu below the Audit page. Similar to google, we want to show:

```Previous 1 2 3 4 5 6 7 8 9 10 Next```

1. We show 10 page numbers max
2. If we show the first page, Previous is not shown.
3. If we show the last page, Next is not shown.

# Run Instruction Videos as Tests in Headless Mode

In headless mode we currently do not run the instruction videos, as they are not actually tests.
They lack expected outcome for any step in the video.
However, we should convert them to tests and allow running them in headless mode.
In headless mode we must not record the video.
In headless mode we must not support narration.
We need to add expected results similar as we do for other UI tests like ActiveSprintsIT or AvailabilityListViewIT or
any tests in the de.bushnaq.abdalla.kassandra.ui.view package.
e.g. we press the button to add a user, and then we need to use the UserApi to check if that user has been created.
Every action we execute in the UI that has an expected outcome must be checked in the test code.

# How To Handle Planned Sick-Days

At the moment we handle sick days as unplanned in our sprints and charts.
But, sometimes sick days can be planned.
e.g. if a suer has a planned surgery, we can plan this time into our project.
At this moment, GanttBurndownChartService.isPlannedWorkingDay assumes that all sick days are unplanned.
We need to add a new OffDayType planned sick day.

# Add Zoom Adaptation to Gantt Chart

All our charts support day width zoom (zooms horizontally) and full chart zoom (both directions).
We want the chart to adapt what it renders depending on the zoom level.

1. current task body has many details that can be distracting at low zoom levels:
    1. task name
    2. task assignee
    3. progress bar (lets render this only at high zoom levels)
    4. progress text (lets render this only at high zoom levels)
    5. distinguishes off-days from working days (lets render this only at high zoom levels, lets also reduce the task
       body height at low zoom levels)
    6. border (lets not render this at low zoom levels)
2. current milestone body has many details that can be distracting at low zoom levels:
    1. milestone name
    2. milestone start date (lets render this only at high zoom levels)
    3. milestone start time (lets render this only at high zoom levels)
    4. milestone border (lets not render this at low zoom levels)
3. dependency lines between tasks/stories/milestones (lets not render this at low zoom levels)

We want to keep all these zoom levels independently configurable as properties.
Let us measure zoom level by dayWidth.
So, renderTaskProgressBarAtDayWidth could be set to 20 and renderTaskProgressTextAtDayWidth could be set at 22

# Gantt-Chart rendering

1. milestone dates overlap milestone tasks.
2. milestones have a empty row between them and the year.

# Burn-Down-Chart Rendering

1. Burn down chart grid is one line too high.
2. Let us draw the vertical grid lines for the Burn-down-chart above the graph. Gantt chart should keep the grid below
   the chart.
3. Horizontal grid should only reach until the end of the chart, not end of the screen.
4. legend should be drawn above the grid in z axis.
5. burndown chart guides and interpolation lines should have black background in dark mode.

# Disabling Video Recording for Tests

We want to be able to disable video recording for tests.
Let's add a boolean value in VideoRecorder.
Let's allow setting that to false in SeleniumHandler.

# Redesign Undo/Redo

Currently, we implemented a undo/redo architecture per project. See ..\kassandra.wiki\undo-redo-design.md.
This means that if a user is adding a worklog to a test belonging to a sprint. This operation can only be undone without
side effects, if no other user has changed any other sprint belonging to the same project in the meantime.

This scope is safe but not practical.

Usually users want to undo/redo their own actions, even if other users have changed other sprints in the meantime. For
tasks, it is clear. A task belongs to a user and only that user can change anything for that task.

Scoping undo/redo only for a users actions is also not so simple, as changes to a task might change a sprint start/end
time. E.g. adding a task with a manual date might change the sprint start time. adding a task might change the
sprint end time. But all these changes are calculated every time we draw the sprint. They are not destructive.

Can you suggest a better design for our undo/redo architecture that is more practical for our users?


