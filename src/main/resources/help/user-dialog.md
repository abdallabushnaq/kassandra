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

Create or maintain a Kassandra user and their initial planning setup. This
form does not create a password or configure an external identity provider.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Identify the person and grant appropriate roles

![User name](images/help/light/UserDialog/user-name-field.png)

![User email](images/help/light/UserDialog/user-email-field.png)

Name and email are required. Resolve duplicate-field errors instead of
creating another account for the same person.

![Roles](images/help/light/UserDialog/user-dialog-roles.png)

Choose at least one role. New users default to **USER**. **ADMIN** grants
administrative access, including access to all products; use product access
grants and groups when a person only needs to collaborate on selected work.

## Set the working period

![First working day](images/help/light/UserDialog/user-first-working-day-picker.png)

![Last working day](images/help/light/UserDialog/user-last-working-day-picker.png)

These dates describe the user's working period, not a vacation. The date
pickers constrain the last day not to precede the first. Use Off Days for
temporary absences.

## Initialize a new user's calendar

![Initial country](images/help/light/UserDialog/user-location-country-combo.png)

![Initial state or region](images/help/light/UserDialog/user-location-state-combo.png)

![Initial work week](images/help/light/UserDialog/user-work-week-combo.png)

These required selections appear **only when creating a user**. Country
determines the regions; choose a region again after changing country. The
location supplies holidays and the work week supplies regular working times.

The initial assignments start on First Working Day, or today if that date is
empty. Editing an existing user does not rewrite these assignments. Use that
user's Location and Work Week pages to make dated changes.

## Save the account and optional avatar

![Optional avatar](images/help/light/UserDialog/generate-user-image-button.png)

When the image service is available, a non-empty name enables the magic wand.
User images have light/dark avatars without the product-style headers.
**Accept** returns the image choices here; it is not an account save.

![Save user](images/help/light/UserDialog/save-user-button.png)

![Cancel user changes](images/help/light/UserDialog/cancel-user-button.png)

**Save** applies the account and image changes and, for a new user, creates the
initial calendar assignments. These are separate requests: after an error,
inspect the user and assignments before retrying. **Cancel** discards unsaved
input, not earlier successful requests. User administration is not covered by
planning undo/redo.
