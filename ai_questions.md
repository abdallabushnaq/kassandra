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

lets add a much elaborate pagination menu below the Audit page. Similar to google, we want to show:
Previous 1 2 3 4 5 6 7 8 9 10 Next
We show 10 page numbers max
If we show the first page, Previous is not shown.
If we show the last page, Next is not shown.

