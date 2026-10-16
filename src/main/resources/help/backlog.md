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

**Backlog** is the planning workspace for a selected sprint: its hierarchy,
estimates, assignments and dependencies. The separate backlog grid supports
moving work into and out of the sprint.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Select and find work

![Sprint selection](images/help/light/Backlog/sprint-selector.png)

Choose the sprint whose work you intend to change. Check the heading and
hierarchy before creating or moving tasks.

![Task search](images/help/light/Backlog/search-field.png)

The text search narrows displayed tasks as you type; it is not the AI-powered
filter used on entity lists.

![Assigned users](images/help/light/Backlog/user-selector.png)

Use the user selector to narrow work by assignee.

![Clear filters](images/help/light/Backlog/clear-filter-button.png)

Clear filters restores the unfiltered view; it does not delete work.

![Expand hierarchy](images/help/light/Backlog/expand-toggle-button.png)

Expand or collapse the hierarchy to inspect child tasks. A collapsed or filtered
task is not deleted.

## Create and arrange work

![Create a milestone](images/help/light/Backlog/create-milestone-button.png)

Create Milestone adds a dated milestone to the selected sprint.

![Create a story](images/help/light/Backlog/create-story-button.png)

Create Story adds a top-level story.

![Create a task](images/help/light/Backlog/create-task-button.png)

Create Task adds work using the current selection/context. Select the intended
parent and inspect the resulting hierarchy. These creation actions persist work;
they are not merely unsaved grid edits.

![Sprint task hierarchy](images/help/light/Backlog/backlog-task-grid.png)

Drag and drop moves work within its hierarchy or onto another story. The
separate backlog grid supports transferring work between backlog and sprint.
Check the destination before dropping; moving work is not copying it.

## Edit fields and dependencies

![Enter edit mode](images/help/light/Backlog/edit-tasks-button.png)

**Edit** enables grid editing.

![Save edited tasks](images/help/light/Backlog/save-tasks-button.png)

**Save** applies edited task fields.

![Cancel edits](images/help/light/Backlog/cancel-tasks-button.png)

**Cancel** abandons pending field edits. Do not use it as an undo for independently
persisted actions such as creation, deletion, transfers or sprint-status changes.

![Dependencies](images/help/light/Backlog/task-grid-dependency-.png)

In edit mode, use a dependency cell to inspect or change task relationships.
The page also supports **Ctrl + drag and drop** to add or remove dependencies.
Dependencies affect scheduling; avoid creating circular relationships.

![Delete work](images/help/light/Backlog/task-grid-delete-button-prefix-.png)

The row delete action removes existing work. Deleting a parent also affects its
children; check the confirmation and hierarchy before proceeding.

## Track sprint status and schedule

![Sprint status](images/help/light/Backlog/sprint-status-selector.png)

The sprint status selector changes the **sprint**, not an individual task.
Status changes save immediately and are separate from grid Save/Cancel.

![Gantt chart](images/help/light/Backlog/backlog-gantt-chart-container.png)

The Gantt chart shows the calculated schedule. Investigate estimates, capacity,
assignments and dependencies if dates are not what you expected.

![Open chart separately](images/help/light/Backlog/open-gantt-chart-button.png)

Open the chart in a separate window for more space. Allow the browser popup
when prompted; opening the chart does not change the plan.

## Recover changes

![Planning history](images/help/light/MainLayout/main-layout-action-history-button.png)

{{include:undo-redo}}

Use the selected work's product context, or return to Products for broader
history. History replays recorded planning changes, not unsaved field input.

## Use the main menu and header

{{include:main-menu}}

{{include-admin:main-menu-admin}}

![Backlog help](images/help/light/Backlog/backlog-help-button.png)

The header's question mark opens this help without leaving your planning
workspace. Close it to return to the same selection and pending edits.
