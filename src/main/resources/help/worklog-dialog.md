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

Record work performed on the task shown in the dialog, or correct an existing
entry. Worklogs change the task's effort totals, not just a separate journal.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Record effort and revise the remaining estimate

![Time spent](images/help/light/WorklogDialog/worklog-time-spent-field.png)

Enter nonzero **Time Spent** using work-duration notation such as **1d 2h 30m**.
When editing, this is the replacement duration for the entry, not extra time
to add. The task total is adjusted by the difference from its old duration.

![Time remaining](images/help/light/WorklogDialog/worklog-time-remaining-field.png)

Typing Time Spent suggests Time Remaining by subtracting it from the task's
current remaining estimate, stopping at zero. You can replace the suggestion
with your own estimate. When editing an old entry, review this carefully:
the suggestion still starts from the task's current estimate.

Remaining time is optional. Leaving it blank is not the same as entering
zero: on creation it leaves the task without a specified remaining estimate;
when editing, a blank does not replace the task's current remaining estimate.

## Place the work in time

![Work date and time](images/help/light/WorklogDialog/worklog-datetime-picker.png)

Date & Time is required. New entries start with the current local date and
time; editing loads the entry's existing date. Check it when entering work
after the day it was performed.

![Work comment](images/help/light/WorklogDialog/worklog-comment-field.png)

The optional comment explains the work or correction. It does not change the
task name or notes.

## Save independently of the task form

![Save worklog](images/help/light/WorklogDialog/save-worklog-button.png)

![Cancel worklog changes](images/help/light/WorklogDialog/cancel-worklog-button.png)

**Save** persists the entry, updates the task and closes this dialog after
success. From Task Details, you do not need to select Save Task afterwards
to commit the worklog. Closing the task form does not undo this save.

**Cancel** discards unsaved worklog input. Correct duration or missing-date
errors before saving again. If persistence fails, inspect both the entry and
task totals before retrying because their updates use separate requests.
