# AI Questions

## Server Settings Design

1. create a markdown file docs/design/server-settings-design.md explaining how server settings work
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

# Prevent Video Overwrite

In GenerateScreenshotsIT, it seems that we start and stop the screen capturing during the test. Everytime we do that, we
overwrite the existing video. The screen capturing should be started and stopped only one time during the test covering
all the tests, so that we can later take a look at what was done and pinpoint issues.
We seem to stop the recording by calling seleniumHandler.destroy () (commented with: A local logout can leave Keycloak
SSO cookies; isolate both origins between accounts.) and restart it by logging into kassandra.
Can we remove the cookies without having to call destroy?
Worst case we add an optional parameter boolean stopRecording.

# Updating wiki pages

1. All the screenshots we take and store in kassandra.wiki are supposed to show up on one of the markdown pages in the
   wiki. We need to make sure it is referenced on one of the pages to describe the product or we need to remove it.

# Analyze Burn-Down-Chart

In the Demo test class as in other classes, we generate synthetic data to simulate a team of people working on projects.
We generate sprints with tasks and dependencies and calculate a Gantt chart.
This Gantt chart already ensures that all user availability is taken into account.
So, ideally, if nobody gets sick or tasks take longer or shorter than estimated, the sprint should finish exactly at the
end of the Gantt chart.
We go through the days of the sprint and burn down tasks as fast as the users availability allows.
We do generate random user sick days. They might delay the borning down of the chart.
All this is done only at the start of the test when no database backup is found, and it takes about 30min, so not a good
thing to run many times or debug.
Remark: But we do not use delay in our synthetic data because we noticed that even without delay the graph is not
correct.
We will add delay later after we have fixed this bug.

## Problem

In the burn-down-chart we show min/max optimum guide-lines. The minimum line should show the optimum burn down when
everything works as expected.
Barcelona sprint is burning down in the first 3 days slower than optimum, about 3h missing. None of the users is sick at
that time. Why is it burning down slower than optimum?
Berlin sprint starts at max estimation but then catches up. Why is it starting at max estimation?
Prague sprint burns down much faster than optimum. How is that possible?
London sprint burns down faster than optimum. How is that possible?
Athens sprint burns down faster than optimum. How is that possible?

## Analyzing

I suggest to export the synthetic data to a file and then analyze it in a separate test class. We can then analyze it
separately.
But I leave it to you to decide how to do that.

## Classes involved

1. Demo: the test class that generates synthetic data and later runs the application as a demo.
2. AbstractGanttTestUtil: Generates synthetic data.
3. GanttBurndownChart.ts: The chart that shows the burn down chart.
4. GanttBurndownChart.ts: The internal renderer that draws the chart.
5. GanttBurndownChartService: The service that calculates the data for the chart including the guides.
