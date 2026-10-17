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

Set a user's planning capacity from a particular date. Use availability for a
capacity change, not for a dated absence or a different weekly timetable.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Choose when the change takes effect

![Effective start date](images/help/light/AvailabilityDialog/availability-start-date-field.png)

**Start Date** is required and must not duplicate another availability record
for this user. Create a new dated record for a new capacity period; edit an
existing record to correct that period instead.

Availability is effective-dated: it applies from its start until a later
availability record takes over. Moving a start date therefore changes the
period covered, not just the record's label.

## Set capacity rather than working hours

![Availability percentage](images/help/light/AvailabilityDialog/availability-percentage-field.png)

Enter **0 to 150 percent**, with **100** representing full availability.
This scales capacity; it does not change which weekdays are working days.
Use work-week assignments for weekly hours and Off Days for absences.

## Save or correct an error

![Save availability](images/help/light/AvailabilityDialog/availability-dialog-confirm.png)

![Cancel availability changes](images/help/light/AvailabilityDialog/availability-dialog-cancel.png)

**Save** creates or updates the record for the user whose availability page
opened this form, refreshes that page and closes the dialog. Check that user
before saving; this form does not select a different person.

Correct missing values, an out-of-range percentage or a duplicate start date
in the form. If saving reports an error, inspect the current records before
retrying. **Cancel** discards unsaved input; availability changes are not
covered by planning undo/redo.
