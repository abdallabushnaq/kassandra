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

A **version** groups a product's features, for example one release. The planning
hierarchy is **Product > Version > Feature > Sprint**. Access comes from the
parent product, not separate version grants.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Find versions and open their features

![Product selector](images/help/light/VersionListView/product-selector.png)

Select one or several **products** to compare their versions. Clear the selector
to view versions across accessible products. One selected product supplies the
heading; otherwise the heading is **Versions**. Selection is kept in the URL.

![Version filter](images/help/light/VersionListView/version-global-filter.png)

{{include:filtering}}

The product selector and text search replace each other's active table filter;
they do not combine. Clearing the text filter clears the active filter even
when product chips remain. Change the product selection again to restore its scope.

![Search examples](images/help/light/VersionListView/version-global-filter-help-button.png)

The question mark beside the filter opens search examples.

![Matching versions](images/help/light/VersionListView/version-row-counter.png)

{{include:sorting}}

Choose a version row to open its **Features**. Check the **Product** column
when different products use the same version name.

{{include:table}}

## Create, edit and delete versions

### Create within the intended product

![Create version](images/help/light/VersionListView/create-version-button.png)

Enter **Versions through the intended product row** before using Create.
Creation uses that navigation-context product, not the product selector.
Selecting several products does not create a version in each one.

Enter the version's name, then **Save**. **Cancel** discards unsaved changes.
You need an existing product before creating its releases. A duplicate name
within that product must be changed; a new version does not create features.

### Rename an existing version

![Edit version](images/help/light/VersionListView/version-grid-edit-button-prefix-.png)

The row's edit action changes that version, even when it belongs to a product
different from the creation context. Renaming does not move its features or
change its parent product.

### Remove a release and its work

![Delete version](images/help/light/VersionListView/version-grid-delete-button-prefix-.png)

Deleting removes the version and its contained features, sprints, stories,
tasks and worklogs from active planning data. It leaves the parent product and
other versions in place. Check the row's product and the confirmation before proceeding.

## Recover changes

![Planning history](images/help/light/MainLayout/main-layout-action-history-button.png)

{{include:undo-redo}}

History follows the **product in your navigation context**, not every product
selected for comparison. Return to **Products** to find another product's history.

## Work with the AI assistant

![Version assistant](images/help/light/VersionListView/version-ai-panel-button.png)

**AI** toggles the assistant alongside the list. Its context is the product you
entered through, not a replacement context from the product selector. State the
intended product and version explicitly when comparing releases.

AI services must be configured and available. Opening the pane alone changes no data;
requested actions operate with your permissions.

## Use the main menu and header

{{include:main-menu}}

{{include-admin:main-menu-admin}}

![Versions help](images/help/light/VersionListView/version-help-button.png)

The header's question mark opens this help, not filter examples. The heading
reflects the viewing selection; use the navigation context for Create and history.
If no product exists, start from **Products**.
