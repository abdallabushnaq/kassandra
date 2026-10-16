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

**Availability** records the share of a user's working time available for
planning, with an effective start date. It is not a task assignment or a
replacement for the user's work week and off days.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Find availability records

The account menu opens your own availability. When managing another user,
check the user context before changing a record.

![Availability filter](images/help/light/AvailabilityListView/availability-global-filter.png)

{{include:filtering}}

![Filter examples](images/help/light/AvailabilityListView/availability-global-filter-help-button.png)

The small question mark beside the filter opens examples.

![Matching records](images/help/light/AvailabilityListView/availability-row-counter.png)

{{include:sorting}}

## Record a change in capacity

![Create availability](images/help/light/AvailabilityListView/create-availability-button.png)

Use Create for a change that takes effect on a new date. Enter the start date
and availability percentage, then Save. A later effective record replaces the
earlier percentage from its start date onward; it does not add to it.

![Edit availability](images/help/light/AvailabilityListView/availability-edit-button-.png)

Use Edit to correct an existing date or percentage. Save applies the change;
Cancel leaves the saved record unchanged.

![Delete availability](images/help/light/AvailabilityListView/availability-delete-button-.png)

Delete removes the record after confirmation. This can change which earlier
percentage applies to subsequent dates. A user must retain at least one
availability record; the last record cannot be deleted.

These changes affect planning capacity. They are not covered by planning
undo/redo; inspect the saved list before retrying after an error.

## Read the calendar

![Previous year](images/help/light/AvailabilityListView/availability-calendar-prev-year-btn.png)

![Next year](images/help/light/AvailabilityListView/availability-calendar-next-year-btn.png)

Use the arrows to inspect another year. The table still contains the user's
availability history, not just records beginning in that year.

![Availability legend](images/help/light/AvailabilityListView/availability-calendar-legend-item-.png)

Use the legend to interpret the calendar's availability colors. Clicking a day
opens the effective availability record when one exists. To plan a future
change rather than edit the earlier record, use Create with the new start date.

## Use the main menu and header

{{include:main-menu}}

{{include-admin:main-menu-admin}}

![Availability help](images/help/light/AvailabilityListView/availability-help-button.png)

The page question mark opens this help. If capacity looks wrong, check the
effective start dates, work-week assignment and absences as well as the percentage.
