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

Create a release grouping within a product, or rename an existing version
without replacing its features and sprints.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Name the version in the intended product

![Version name](images/help/light/VersionDialog/version-name-field.png)

The name is required and must be unique **within its product**. Creation uses
the product context of the page that opened this dialog. Enter through the
intended product before creating a version, especially when comparing several
products in a combined list.

Renaming keeps the version's identity and contained work. This form does not
move the version to another product or change its access grants.

## Save or correct a duplicate name

![Save version](images/help/light/VersionDialog/save-version-button.png)

**Save** submits the name. A successful save closes the dialog and refreshes
the list. A new version has no automatically created features or sprints.

If the name already exists in that product, choose another. A dialog-level
error may indicate insufficient access or another failed operation; read the
message rather than assuming a retry will succeed.

## Leave existing work unchanged

![Cancel version changes](images/help/light/VersionDialog/cancel-version-button.png)

**Cancel** discards the unsaved name. It does not undo an earlier saved
rename. Review recorded version changes in the parent product's planning
history when recovery is needed.
