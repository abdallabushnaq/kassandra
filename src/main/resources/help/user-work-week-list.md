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

**User Work Week** assigns an existing weekly schedule to a user from an
effective start date. This page changes the user's assignment, not the shared
schedule definition used by other people.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Find the user's assignments

The account menu opens your own assignments. Check the user context before
changing someone else's schedule.

![Assignment filter](images/help/light/UserWorkWeekListView/user-work-week-global-filter.png)

{{include:filtering}}

![Filter examples](images/help/light/UserWorkWeekListView/user-work-week-global-filter-help-button.png)

The filter question mark opens examples.

![Matching assignments](images/help/light/UserWorkWeekListView/user-work-week-row-counter.png)

{{include:sorting}}

## Assign or correct a work week

![Create assignment](images/help/light/UserWorkWeekListView/create-user-work-week-button.png)

Create an assignment with the date from which the selected work week should
apply. Save applies it; Cancel discards unsaved changes. A later assignment
replaces the earlier schedule from its effective date rather than combining
both schedules.

![Edit assignment](images/help/light/UserWorkWeekListView/user-work-week-edit-button-.png)

Edit corrects the start date or chosen work week of the existing assignment.
To introduce a future schedule change while preserving history, create a new
dated assignment instead.

![Delete assignment](images/help/light/UserWorkWeekListView/user-work-week-delete-button-.png)

Delete removes an assignment, not its shared work-week definition. The previous
assignment may then apply for longer. The user must retain at least one
assignment, and the earliest assignment is protected even when later ones
exist. These changes are not restored through planning undo/redo.

If the needed definition is unavailable, an administrator must create it in
**Manage Work Weeks**. Editing a shared definition there affects its users.

## Inspect the resulting calendar

![Previous year](images/help/light/UserWorkWeekListView/calendar-prev-year-btn.png)

![Next year](images/help/light/UserWorkWeekListView/calendar-next-year-btn.png)

Inspect other years without removing assignment history from the table.

![Holiday legend](images/help/light/UserWorkWeekListView/calendar-legend-item-holiday.png)

The calendar also reflects holidays and absences, not just the work-week
assignment. If capacity looks wrong, check Availability, Location and Off Days
as well as the effective schedule.

## Use the main menu and header

{{include:main-menu}}

{{include-admin:main-menu-admin}}

![User Work Week help](images/help/light/UserWorkWeekListView/user-work-week-help-button.png)

The page question mark opens this help. **Manage Work Week** in your account
menu is distinct from the administrator's **Manage Work Weeks** definitions page.
