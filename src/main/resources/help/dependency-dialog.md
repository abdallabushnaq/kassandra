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

Choose the tasks that must finish before the task named in the dialog can
start. These are its **predecessors**, not the tasks that should follow it.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Choose predecessors in the sprint

![Predecessor choices](images/help/light/DependencyDialog/dependency-dialog-choices.png)

The list follows the sprint's task order and hierarchy. Check each required
predecessor; uncheck a selected task to remove that explicit dependency.

The task itself, its ancestors and its descendants are disabled to prevent
those invalid relationships. A disabled entry is not evidence that it has
been removed from the sprint. The tooltip explains the restriction.

## Enter task order numbers safely

![Quick dependency editor](images/help/light/DependencyDialog/dependency-dialog-quick-edit.png)

**Quick Edit** uses the displayed numeric order IDs, for example **3, 5, 12**,
not task keys or names. Editing this field replaces the enabled checkbox
selection; it does not append to it. Clearing it clears the enabled choices.
Checkbox changes update the text in turn.

Invalid numbers and numbers without an eligible matching task do not select a
predecessor. Check the resulting checkbox selection before saving rather than
assuming every typed token was accepted.

## Apply the selection to your Backlog edit

![Save dependencies](images/help/light/DependencyDialog/save-dependency-button.png)

![Cancel dependency changes](images/help/light/DependencyDialog/cancel-dependency-button.png)

**Save** replaces this task's visible predecessor relationships with the
selected eligible tasks. Other internally maintained relationships are not
edited here. The selection returns to the enclosing Backlog edit session;
use that page's **Save** to persist the planning changes.

**Cancel** closes this dialog without applying its selection. Cancelling the
enclosing Backlog edit discards the staged changes instead of saving them.
