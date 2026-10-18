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

1. I now noticed that if the detailed vie wopens, the list gets a horiuontal scol bar, although no text needs scrolling.
   can we try to prevent that?
2. secret values should be encrypted in the database, so instead of trying to guess if a value is a secret or not, we
   could just show the encrypted values.
3. updated server settings shoud show oldvalue-newvalue.

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

# Harmonize Smart Headers

Most pages have some kind of smart header. With smart header we mean a header on the top below the toolbar and
breadcrumbs that shows information and allows placing buttons.<br>
Some, like ProductListView, are based on AbstractMainGrid that handles the smart header.
Some, like Backlog, just create it from scratch.<br>
Some, are missing such a header totally.

We want to harmonize the smart headers across all pages, so that they look similar and behave similar and use the same
case.

Let's extract the smart header from AbstractMainGrid and pages like Backlog into a new class AbstractSmartHeader that
can be used by all pages.
It might be good if AbstractMainGrid extends AbstractSmartHeader, but please check your options are select the one that
best harmonizes all pages, reduces similar code and is easiest to maintain.

The look and feel of the headers should be similar. the user should find elements always at similar locations.
The location of the Avatar/Icon, the Title, the smart filter, all other filters, the buttons and the help icon
e.g. the buttons should be on the right side.

# Optimizing Screenshots

GenerateScreenshotsIT currently generates screenshots for all pages. It uses a tolerance level of 4 (I already raised it
to 8).
This tolerance level is not enough. If we generate all screenshots with same database, it still overwrites many
screenshots.
We need to collect the maximum level of discrepancy we found in a pair of screenshots and print this out at the end of
the test so that we can see how far we need to go with our tolerance level.
We should also list the png with that high discrepancy.
Maybe we should keep a sorted list that we can export to a file so that we can analyze the situation, just in case some
images are actually different. After we take a look at the result, we will decide how to better handle this situation.
Maybe we need to add a new tolerance level, e.g. 16 or 32, or maybe we need to change the way we check the differences.
