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

Define a reusable weekly timetable. Creating it does not assign it to anyone;
editing an existing definition affects the users whose assignments reference it.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Decide whether to edit a shared definition

![Work-week name](images/help/light/WorkWeekDialog/work-week-name-field.png)

Choose a distinct, required name that helps people select the right schedule.

![Work-week description](images/help/light/WorkWeekDialog/work-week-description-field.png)

Use the description to explain the working pattern. If only one person's
schedule changes from a future date, create a different definition and assign
it on that person's Work Week page instead of changing a shared definition.

## Define working days and breaks

![Example weekday schedule](images/help/light/WorkWeekDialog/work-week-dialog-day-monday.png)

Each row contains **Working**, **Work Start**, **Lunch Start**, **Lunch End**
and **Work End** in that order. Enable Working for each scheduled day and
enter its work and break times.

Disabling a day disables its time editors; saving stores that day as
non-working without times. Check all seven rows rather than assuming a
Monday-to-Friday pattern. Lunch separates the two work periods; it is not
additional working time.

## Save the definition, then assign it if needed

![Save work week](images/help/light/WorkWeekDialog/work-week-dialog-confirm.png)

![Cancel work-week changes](images/help/light/WorkWeekDialog/work-week-dialog-cancel.png)

**Save** creates or updates the shared definition, refreshes the list and
closes the dialog after success. It does not create a dated assignment.
Check any reported save error, including rejected schedule values or a
duplicate name, before retrying.

**Cancel** discards unsaved inputs. Shared schedule changes are not covered by
planning undo/redo; inspect existing assignments before editing a definition
that others already use.
