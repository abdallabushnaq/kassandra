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

### Administer the server

Your account menu includes the following administration entries. These affect
server configuration, people or shared planning rules, not just the current page.

#### Identity Providers

![Identity Providers menu entry](images/help/light/MainLayout/main-layout-user-menu-identity-providers.png)

Configure the OIDC providers offered on the sign-in page: their display name,
issuer, client ID, client secret and scopes. You can add, enable/disable or
delete a provider. Saved client secrets are not displayed
again. Check the issuer and client configuration before changing sign-in access;
this page configures authentication, not product access.

#### Manage User Groups

![Manage User Groups menu entry](images/help/light/MainLayout/main-layout-user-menu-manage-user-groups.png)

Create and edit named groups, their descriptions and membership. Grant a group
access from a product's access controls when its members should collaborate on
that product. Membership and application roles are different: adding someone
to a group does not make them an administrator.

#### Manage Users

![Manage Users menu entry](images/help/light/MainLayout/main-layout-user-menu-manage-users.png)

Create or edit application users, their profile/planning information and their
**USER**/**ADMIN** roles. Use the row actions to edit or remove a user.
Application user records are not
accounts created at the external identity provider. Assign **ADMIN** only to
people who should manage the server; the last administrator's role is protected.

#### Manage Work Weeks

![Manage Work Weeks menu entry](images/help/light/MainLayout/main-layout-user-menu-manage-work-weeks.png)

Define reusable weekly schedules, including working days and hours.
The list shows how many users reference each definition. To apply a schedule
to a person, use that person's **Manage Work Week** assignments with an effective
start date. Editing a shared definition affects its users; a definition that
still has user assignments cannot be deleted.

#### Manage Settings

![Manage Settings menu entry](images/help/light/MainLayout/main-layout-user-menu-manage-settings.png)

Configure the server by category. Each setting has its own **Save** action;
settings that support a connection check also have **Test connection**.
Optional categories can be enabled/disabled. Secret values are not revealed:
enter a replacement to change one, or use **Clear stored secret** where offered.
These settings affect all users, not just your browser or the current product.

#### Audit

![Audit menu entry](images/help/light/MainLayout/main-layout-user-menu-audit.png)

Inspect recorded changes by time, actor, action and entity. Search or filter by
user, action and date range, then use **Apply**; timestamps are displayed in UTC
and the end of the range is exclusive. Expand an update to inspect its changed
fields, and use pagination for older records. The audit log is for inspection;
use **Undo history** to reverse or reapply supported planning changes.

#### Kassandra assistant page

![Kassandra menu entry](images/help/light/MainLayout/main-layout-tab-kassandra.png)

The **Kassandra** entry in the top navigation opens the administrator's standalone
AI assistant. It requires configured, available AI services. Unlike a planning
page's **AI** pane, it is not tied to the version you are currently viewing;
state the intended product, version and work explicitly in requests.
