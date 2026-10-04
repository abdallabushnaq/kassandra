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

## What are features?

A **feature** groups related work within a product version. Each feature contains
sprints. The **Features** page lists the features of the currently selected
product/version combination in the filter.

## Choose versions

![Version selector](images/help/light/FeatureListView/feature-version-selector.png)

Use the **Version Selector** to select one or more versions. All features of the
selected versions will be shown. Entries include the product name so you can
distinguish versions with the same name. Clear the selection with **X** to show
features across all versions.

## Filtering and sorting

![AI powered filter field](images/help/light/FeatureListView/feature-global-filter.png)

{{include:filtering}}

Enter a feature name such as **Config server** or describe the features you want
to find. AI-powered queries require a configured, available AI service.

![Filter help button](images/help/light/FeatureListView/feature-global-filter-help-button.png)

The **question-mark button beside the filter** opens filtering examples.

![Matching row count](images/help/light/FeatureListView/feature-row-counter.png)

**Showing ... rows** reports how many rows match out of the available features.

{{include:sorting}}

## Create a feature

![Create feature button](images/help/light/FeatureListView/create-feature-button.png)

Use **Create (+)** at the upper right to add a feature to the current version.
The creation context comes from your navigation into the Features page; selecting
multiple versions for viewing does not create a feature in each of them.

## Edit a feature

![Edit feature button](images/help/light/FeatureListView/feature-grid-edit-button-prefix-.png)

Use the **pencil/notepad icon** in the feature row's **Actions** column to edit
its name or generate a new icon/header.
Image generation is an optional feature that needs a running **Stable Diffusion
server**.
Use the action button, rather than the row, to avoid navigating to sprints.

## Delete a feature

![Delete feature button](images/help/light/FeatureListView/feature-grid-delete-button-prefix-.png)

Use the **trash-can icon** in the feature row's **Actions** column to delete a
feature you no longer need. Check the feature name before confirming **Delete**.
Deleting a feature also removes all its **Sprints, Stories, Tasks and worklogs**.

## Undo and redo

![Product-scoped history button](images/help/light/FeatureListView/main-layout-action-history-button.png)

{{include:undo-redo}}

## Use the AI assistant

![AI assistant button](images/help/light/FeatureListView/feature-ai-panel-button.png)

Use **AI** at the upper right to open or close the assistant.
AI features require configured, available services.
