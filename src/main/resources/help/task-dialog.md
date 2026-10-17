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

Inspect a task's calculated state, edit its planning inputs, or manage its
worklogs. Check the task key and name in the heading before making changes.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Distinguish results from planning inputs

![Calculated task values](images/help/light/TaskDialog/task-dialog-calculations.png)

Time spent, remaining estimate, progress, effective status, duration and finish
are displayed as results, not editable fields here. Worklogs affect the
displayed effort totals; the scheduling mode determines how start is entered.

![Estimate range](images/help/light/TaskDialog/task-dialog-estimates.png)

Enter minimum and maximum effort estimates using work-duration notation such
as **1d 2h**. These are effort estimates, not calendar finish dates. Blank
estimates are treated as zero; malformed durations prevent saving.

## Choose automatic or manual scheduling

![Manual scheduling](images/help/light/TaskDialog/task-dialog-manual-scheduling.png)

![Manual start date and time](images/help/light/TaskDialog/task-dialog-start.png)

With **Manual scheduling** selected, enter Start yourself. Otherwise the
calculated start is displayed. Changing the checkbox alone does not persist
the task; use **Save Task** to apply this mode along with the edited fields.

## Save task edits separately from worklogs

![Task name](images/help/light/TaskDialog/task-dialog-name.png)

![Task notes](images/help/light/TaskDialog/task-dialog-notes.png)

![Save Task](images/help/light/TaskDialog/save-task-button.png)

The name is required. **Save Task** applies the name, notes, estimates,
scheduling mode and start input. The dialog stays open after a successful
save, so **Close** is not a rollback of an earlier save.

## Correct or add work already performed

![Worklogs and actions](images/help/light/TaskDialog/task-dialog-worklogs.png)

**Log Work** is enabled only for the task's assigned user. Edit and delete
actions are enabled for the worklog author or an administrator.

Each worklog save or confirmed deletion takes effect independently of
**Save Task**. Deleting a worklog reduces logged time and recalculates the
task; closing Task Details does not reverse that deletion.

After a worklog change, the list and effort totals refresh. If an operation
reports an error, inspect the saved worklogs and task before retrying:
worklog and task updates are separate requests. **Close** discards unsaved
task-form input, not already persisted task or worklog changes.

![Close Task Details](images/help/light/TaskDialog/cancel-task-dialog-button.png)
