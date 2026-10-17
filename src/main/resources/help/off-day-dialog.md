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

Record a user's absence without changing their regular work-week definition
or removing their tasks.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Choose the full absence period

![First day](images/help/light/OffDayDialog/offday-start-date-field.png)

![Last day](images/help/light/OffDayDialog/offday-end-date-field.png)

Both dates are required and the last must not precede the first. A one-day
absence uses the same date in both fields.

The range must include a working day. Working days depend on this user's
effective work week, not necessarily Monday to Friday. Regional public
holidays also depend on the user's location.

## Understand which days will be stored

![Absence type](images/help/light/OffDayDialog/offday-type-field.png)

Choose the appropriate absence type, such as vacation, sick leave or business
trip. The type identifies the absence in the calendar.

Saving excludes non-working weekdays and public holidays and splits the
remaining dates into separate continuous records. A range crossing a weekend
may therefore create more than one row. When editing, the first resulting
range keeps the original record's identity and additional ranges are created
as new records.

## Save and check the resulting records

![Save absence](images/help/light/OffDayDialog/offday-dialog-confirm.png)

![Cancel absence changes](images/help/light/OffDayDialog/offday-dialog-cancel.png)

**Save** applies the resulting ranges for the user whose page opened the
dialog. Correct invalid dates or a reported conflict. After a save error,
inspect the list before retrying because split ranges are saved separately.

**Cancel** discards unsaved input. Saved off-day changes are not covered by
planning undo/redo. To undo an unintended absence, correct or remove its
resulting records on the Off Days page.
