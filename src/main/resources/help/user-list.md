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

**Users** manages application users and planning information. It is restricted
to administrators. An application user record is not an account created at an
external identity provider.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Find users and compare planning information

![User filter](images/help/light/UserListView/user-global-filter.png)

{{include:filtering}}

![Filter examples](images/help/light/UserListView/user-global-filter-help-button.png)

The filter's question mark opens examples.

![Matching users](images/help/light/UserListView/user-row-counter.png)

{{include:sorting}}

![User table](images/help/light/UserListView/user-grid.png)

Compare email, roles, first/last working day, availability, country, region and
work week. Effective-dated values are a summary, not the complete history of
each user's scheduling records.

## Create or update an application user

![Create user](images/help/light/UserListView/create-user-button.png)

Create opens the user form. Supply the required identity and planning fields,
then Save. Cancel discards unsaved changes. Creating this record does not
register credentials at the external identity provider.

![Edit user](images/help/light/UserListView/user-grid-edit-button-prefix-.png)

Edit changes the user in that row. Check email as well as name when users have
similar names. Give **ADMIN** only to people who should administer the server;
the last administrator's role is protected.

Application roles and product grants are separate. A USER needs product access
through an explicit user/group grant; ADMIN provides broader server access.
Use the dedicated working-time pages for effective-dated scheduling history.

## Delete carefully

![Delete user](images/help/light/UserListView/user-grid-delete-button-prefix-.png)

Check the confirmation before deleting an application user. This is not a way
to revoke just one product grant, and it does not delete their external
identity-provider account. Inspect any error notification before retrying.
User administration is not replayed through planning undo/redo.

## Work with the AI assistant

![User assistant](images/help/light/UserListView/user-ai-panel-button.png)

**AI** opens the assistant alongside the list. Use an unambiguous user name or
email in requests and review the resulting changes. The service must be
configured and available, and actions use your access permissions.
Opening the pane alone does not edit users.

## Use the main menu and header

{{include:main-menu}}

{{include-admin:main-menu-admin}}

![Users help](images/help/light/UserListView/user-help-button.png)

The header's question mark opens this help. Your own profile and working-time
pages remain accessible through the account menu.
