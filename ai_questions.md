# AI Questions

## Narration Design

1. create a markdown file docs/design/narration-design.md explaining how narrator class works
2. include functionality of chatterbox, CacheManager.
3. explain where wav files are cached.
4. explain about narrator voices and voice synchronization.
5. use mermaid to visualize the flow of narration and caching process or class relations.

## AI Filter Design

1. create a markdown file docs/design/ai-filter-design.md explaining how AI filter works
2. include both java and js implementation.
3. explain the basic idea.
4. use mermaid to visualize the flow of narration and caching process or class relations.

## Stable Diffusion Design

1. create a markdown file docs/design/stable-diffusion-design.md explaining how Stable Diffusion works
2. include the user avatar generation and project/feature/sprint header image generation.
3. explain light/dark mode
4. use mermaid to visualize the flow of image generation and caching process or class relations.

## MCP Design

1. create a markdown file docs/design/mcp-design.md explaining how MCP works
2. explain features of ChatAgentPanel, ChatPanelSessionState
3. explain the basic idea.
4. use mermaid to visualize the flow of MCP and/or class relations.

## Improve kassandra readme.md

1. use mermaid to visualize the systems kassandra interacts with.
2. mention the database, ID server, stable diffusion, LM Studio, Chatterbox.
3. visualize the backend and the UI portal and how the portal talks with the backend using the apis.
4. API first is our motto.
5. please visualize the PMC server and what ai systems it interacts with.

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

## Add Audit support

We already added Hibernate envers to implement undo/redo.
Please see ../kassandra.wiki/undo-redo-design.md for details.

Now we want to add audit capability.

1. Add admin page Audit that lists all changes in the database: Who did what and when.
2. Must support filtering by user, date, action and specific timeframe.
3. Must support search by user name, email, action and date.
4. Must support pagination.
5. Most important events to track are:
    1. ID provider creation, update and deletion.
    2. User creation, update and deletion.
    3. User group creation, update and deletion.
    4. server setting updates.

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

