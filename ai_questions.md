# AI Questions

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

# Add Zoom Adaptation to Sprint Overview Chart

All our charts support day width zoom (zooms horizontally) and full chart zoom (both directions).
We want the chart to adapt what it renders depending on the zoom level.
There currently is too much unused vertical space between the lanes.
Current Sprint Overview Chart should show:

1. sprint name
2. sprint start date in lower left corner (lets add a property to hide this at low zoom levels)
3. sprint end date in lower right corner (lets add a property to hide this at low zoom levels)
4. sprint progress gray background color covering the left percentage of the bar (lets add a property to hide this at
   low zoom levels)
5. sprint status in upper right corner (lets add a property to hide this at low zoom levels)

We want to keep all these zoom levels independently configurable as properties.
Let us measure zoom level by dayWidth.
So, renderSprintStartDateAtDayWidth could be set to 20 and renderSprintEndDateAtDayWidth could be set at 22

# Gantt-Chart rendering

# Burn-Down-Chart Rendering

1. Burn down chart grid is one line too high.
2. Let us draw the vertical grid lines for the Burn-down-chart above the graph. Gantt chart should keep the grid below
   the chart.
3. Horizontal grid should only reach until the end of the chart, not end of the screen.
4. legend should be drawn above the grid in z axis.

# Disabling Video Recording for Tests

We want to be able to disable video recording for tests.
Let's add a boolean value in VideoRecorder.
Let's allow setting that to false in SeleniumHandler.

# Stable Diffusion Cache for our Tests

Let's introduce a file based cache for our Stable Diffusion generateds images.
If an image has same size, diffusion parameters including prompt and negative prompt, we should reuse the image from the
cache instead of generating it again.
This will be only used by our test code.
Let's add a new class CacheAwareAvatarService extending AvatarService to check if an image has already been generated
and stored in the cache.
We will use this in our tests whenever we need to generate an image e.g. PersistingEntityGenerator.

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

# Optimize ColorUtils.intToHex

ColorUtils.intToHex takes 2 parameters, the second one being a default color. let's get rid of that second parameter
everywhere we call it.