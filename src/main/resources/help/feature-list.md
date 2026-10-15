<!--
Copyright (C) 2025-2026 Abdalla Bushnaq
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

A **feature** groups related work within one **product version** and contains
the **sprints** used to plan that work. Use this page to find a feature, open its
sprints, or add another area of work to a version.

The planning hierarchy is **Product > Version > Feature > Sprint**. A feature
belongs to one version, even when you view several versions together. Its access
comes from its parent product; there are no separate access selectors here.

Open a chapter for the task you need. Use **Expand all** to read or search the
whole page, and **Collapse all** to return to the chapter overview.

## Find and compare features

### Compare work across versions

![Version selector](images/help/light/FeatureListView/feature-version-selector.png)

The **version selector** lets you view features from one or more versions,
including versions of different accessible products. Entries use **Product /
Version** labels because different products can have the same version name.
Use the **Product** and **Version** columns to distinguish similar feature names.

Entering from a version normally selects that version. Clearing the selector
with **X** shows features across all accessible versions; it does not delete
features or detach them from their versions.

With exactly one version selected, the page heading shows its name. With none
or several selected, it shows **Features**. The version selection is kept in
the URL, so it can survive a browser refresh or be bookmarked.

**Viewing scope is not creation context.** The selector changes which features
you see, not the version used by **Create**. See "Add work to the right version"
below before creating a feature.

### Find a feature

![Feature filter](images/help/light/FeatureListView/feature-global-filter.png)

{{include:filtering}}

Try **Config server**, or describe the features you need, for example **features
created after January 2024**.

![Filter help button](images/help/light/FeatureListView/feature-global-filter-help-button.png)

The question mark **beside the filter** opens search examples.

**Current filtering behavior:** text searches and the version selector replace
each other's filter; they do not combine. Applying a text search searches the
loaded accessible features, even outside the selected versions. Changing the
version selection replaces that search with the version filter. Clearing the
text field clears the active filter, even if version chips remain selected.
To return to a version-scoped list, change the version selection again.

![Matching row count](images/help/light/FeatureListView/feature-row-counter.png)

{{include:sorting}}

### Open a feature's sprints

Choose a feature row to open its **sprints**. This is where you continue planning
the feature's work; opening the row does not edit the feature.

{{include:table}}

## Create, edit and delete features

### Add work to the right version

![Create feature button](images/help/light/FeatureListView/create-feature-button.png)

Before using **Create**, open the intended product's **Versions** page and enter
Features through the version that should own the new work. Creation uses that
navigation context, not the version selector or the set of visible rows.
Selecting several versions does not create a copy in each one.

Enter a required name that is unique **within that version**, then **Save**.
The new feature belongs to that version; it does not create sprints
automatically. **Cancel** discards unsaved form changes.

If Save reports a duplicate name, choose another name within that version.
For a permission or other dialog-level error, inspect the message and the
current list before retrying: the feature and its images are saved separately,
so an error does not necessarily mean that no changes were applied.

If you arrived from the main menu, the page can use an automatically chosen
product/version context. Do not rely on that default when the destination matters;
enter through the intended version first. You need an existing product and
version before a feature can be created.

### Rename a feature without moving its work

![Edit feature button](images/help/light/FeatureListView/feature-grid-edit-button-prefix-.png)

Use the row's **edit action** to change that feature's name or optional
icon/header. Editing affects the feature in that row, even if it belongs to a
different version from your creation context. Renaming keeps its parent version
and existing sprints; this dialog does not move a feature between versions.

Optional image generation is available only when the **Stable Diffusion**
service is available. The magic-wand button also needs a non-empty name.
Accepting images returns them to the form; **Save** applies them to the feature.
You do not need custom images to organize work.

### Remove a feature and its planned work

![Delete feature button](images/help/light/FeatureListView/feature-grid-delete-button-prefix-.png)

**Deleting a feature also removes its sprints, stories, tasks and worklogs from
the active planning data.** It leaves the parent product/version and other
features in place. To remove only one sprint, work from the sprint list instead.

When comparing versions, check the row's **Product** and **Version** as well as
the feature name before opening Delete: the confirmation names the feature,
which may share its name with a feature elsewhere. Cancel if it is not the
intended work.

## Recover changes

![Product-scoped history button](images/help/light/FeatureListView/main-layout-action-history-button.png)

{{include:undo-redo}}

On this page, history follows the **product in your navigation context**, not
every product represented by the version selector. If you changed a feature
from another product, return to **Products** to find that product's history.

## Use the main menu and header

{{include:main-menu}}

### Understand the Features page header

The page heading reflects the **version selector**: one selected version shows
its name; none or several show **Features**. This is your viewing scope, not
necessarily the context used for creating work.

Use the **version selector**, **filter** and matching row count to find work;
see **Find and compare features**. **Create (+)** adds work to your
navigation-context version; see **Create, edit and delete features**.
**AI** toggles the assistant. The page's **question-mark icon** opens this
help; the separate question mark beside the filter opens search examples.

## Work with the AI assistant

![AI assistant button](images/help/light/FeatureListView/feature-ai-panel-button.png)

The **AI** button opens or closes the assistant alongside the list. It receives
your navigation-context product and version, not a new creation context from
the version selector. When comparing versions, name the intended product and
version explicitly in requests to create or change work.

The assistant requires configured, available AI services and operates with your
access permissions. Opening it alone does not change features.

## Troubleshoot missing features

Clear the text filter, then change the version selection to the intended
version. If you are unsure which version owns the feature, clear the version
selector to inspect all accessible features and check their Product/Version
columns.

If it is still missing, check access to its parent product or look for a recorded
deletion in that product's history. Creating a similarly named feature would
create new work, not recover the original sprints.
