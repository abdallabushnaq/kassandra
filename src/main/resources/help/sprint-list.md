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

A **sprint** contains the stories and tasks planned for a feature. Use this page
to compare sprint schedules, open their quality information, or configure work
in Backlog. Access is inherited from the product.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Find and compare sprints

![Feature selector](images/help/light/SprintListView/feature-selector.png)

Select features using their **Product / Version / Feature** labels. Clearing
the selector shows all accessible sprints. One selected feature supplies the
heading; otherwise it shows **Sprints**. Selection is kept in the URL.

![Sprint filter](images/help/light/SprintListView/sprint-global-filter.png)

{{include:filtering}}

Text search and the feature selector replace the active table filter rather
than combine. Changing the selection restores feature filtering; clearing the
text filter clears the active filter even if chips remain.

![Filter examples](images/help/light/SprintListView/sprint-global-filter-help-button.png)

The filter's own question mark opens search examples.

![Matching sprint count](images/help/light/SprintListView/sprint-row-counter.png)

{{include:sorting}}

![Sprint overview chart](images/help/light/SprintListView/sprints-overview-chart-container.png)

Compare sprint timing in the overview chart. Check the Product, Version and
Feature columns to distinguish similarly named sprints.

## Open quality information or plan work

Choose a sprint row to open its **Quality Board**, not its edit form.

![Configure sprint work](images/help/light/SprintListView/sprint-grid-config-button-.png)

The row's **configuration action** opens **Backlog** for that sprint, where
you manage stories, tasks, dependencies and scheduling.

## Create, edit and delete sprints

![Create sprint](images/help/light/SprintListView/create-sprint-button.png)

Before Create, enter through the intended **Product > Version > Feature**.
Creation uses that feature context, not the feature selector used for comparison.
Enter the required sprint information and Save; Cancel discards unsaved changes.

![Edit sprint](images/help/light/SprintListView/sprint-grid-edit-button-.png)

Edit changes the sprint in that row, including the options offered in its form.
Check its parent columns before changing a schedule while comparing features.

![Delete sprint](images/help/light/SprintListView/sprint-grid-delete-button-.png)

Delete removes the sprint and its stories, tasks and worklogs from active
planning data. It does not remove the feature. Check the confirmation carefully.
The reserved **Backlog** sprint does not offer this delete action.

## Recover changes

![Planning history](images/help/light/MainLayout/main-layout-action-history-button.png)

{{include:undo-redo}}

History follows the navigation-context product, not all selected features'
products. Return to Products when you need another product's recorded changes.

## Work with the AI assistant

![Sprint assistant](images/help/light/SprintListView/sprint-ai-panel-button.png)

**AI** toggles the assistant. It receives your navigation context, not a new
creation destination from the feature selector. Name the product, version,
feature and sprint when requesting changes. Available AI services and your
normal access permissions are required.

## Use the main menu and header

{{include:main-menu}}

{{include-admin:main-menu-admin}}

![Sprints help](images/help/light/SprintListView/sprint-help-button.png)

The page question mark opens this help. The feature selection controls viewing;
Create and history follow navigation context. If no sprint appears, clear the
text search and reselect its feature before assuming it was deleted.
