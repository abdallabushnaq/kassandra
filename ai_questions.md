# AI Questions

## Server Settings Design

1. create a markdown file docs/design/server-settings-design.md explaining how server settings work
2. use mermaid to visualize flows or relations.

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

# Audit Log

## Add GDPR support

To add GDPR support we need several intermediate steps:

1. Add settings to control GDPR:
    1. Number of months until a disabled user gets automatically anonymized.
    2. Number of months until an anonymized user gets automatically deleted .
    3. Number of months until an abandoned sprint gets automatically closed.
    4. Number of months until a closed sprint gets automatically deleted.
    5. Number of months until an abandoned feature gets automatically closed.
    6. Number of months until a closed feature gets automatically deleted.
    7. Number of months until an abandoned version gets automatically closed.
    8. Number of months until a closed version gets automatically deleted.
    9. Number of months until an abandoned project gets automatically closed.
    10. Number of months until a closed project gets automatically deleted.
2. Implement sprint, feature, version and project closing support. Can be reopened by a user with access.
3. Implement user anonymization. User name and email address should be replaced with a random string.
4. Implement a background job that will run every night and check for users, sprints, features, versions and projects
   that need to be anonymized, closed or deleted.

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


