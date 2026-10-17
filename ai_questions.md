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

# Disable Stable Diffusion for Most Tests

a lot of our tests will trigger stable diffusion when creating a user a product a featrure or a sprint.
most of them actually do not need to generate these images, as they are never shown in a ui.
lets ensure these tests will disable stable diffusion service .

test that need the service are

1. all introduction video tests
2. Demo
3. GenerateScreenshotsIT
4. all tests in the de.bushnaq.abdalla.kassandra.ui.view package

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

# Create Help Pages

Let's create a plan on how to add help pages to the UI pages using markdown.
Vaadin renders it inside a Help drawer.
Let's first implement it for one page, e.g. ProductListView.
Create the help icon and the drawer.
Create the markdown page.
How can we reference specific elements like buttons or select boxes? The user does not know their names. could we take
them from the screenshots we have at: ../kassandra.wiki/light-screenshots or ../kassandra.wiki/dark-screenshots?
Create the vaadin markdown renderer.
Create a document to define help page design and standards.

# help panel for all pages

feature-list.md for FeatureListView is now our template for all other page help panels.

Let's align the ProductListView to this new standard.

1. referencing elements using its screenshot. if a screenshot is missing, we need to add it to GenerateScreenshotsIT to
   generate it.
2. always light/dark versions.
3. Includes the main menu chapter.
4. all chapters are collapsible.

after that we will create help panels for most other pages:

1. AboutView
2. ActiveSprints
3. AuditLogView
4. AvailabilityListView
5. Backlog
6. Kassandra
7. LocationListView
8. OffDayListView
9. OidcProviderManagementView
10. ProductListView
11. QualityBoard
12. ServerSettingsView
13. SprintListView
14. UserGroupListView
15. UserListView
16. UserProfileView
17. UserWorkWeekListView
18. VersionLIstView
19. WorkWeekListView

I left out pages that:

1. are legacy (@Deprecated)
2. are related to first time installation and configuration because i am not sure they have access to the resources
   needed for the help page. eventually we do want to add help pages for them too, but let's start with the main pages
   first.
3. we also left out all Dialogs, we will come back to them later.

# Dialog Help Pages

We created for all kassandra pages help pages, e.g. FeatureListView.
We created one example of a help page for the ProductDialog.

Now we want to create help pages for all dialogs.

1. AvailabilityDialog
2. DependencyDialog
3. FeatureDialog
4. ImagePromptDialog
5. LocationDialog
6. OffDayDialog
7. SprintDialog
8. TaskDialog
9. UndoHistoryConfirmationDialog
10. UserDialog
11. UserGroupDialog
12. UserWorkWeekDialog
13. VersionDialog
14. WorklogDialog
15. WorkWeekDialog

I left out dialogs that:

1. are legacy (@Deprecated)
2. are used for many different use cases (ConfirmDialog).

Let's align the dialog help to our standard.

1. referencing elements using its screenshot. if a screenshot is missing, we need to add it to GenerateScreenshotsIT to
   generate it.
2. always light/dark versions.
3. all chapters are collapsible (see FeatureListView).

see help-page-prompt.md and help-page-prompt.md for details on how to create good help page.
