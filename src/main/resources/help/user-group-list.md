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

**User Groups** lets administrators maintain named sets of users. Grant a
group access from a product when its members should collaborate on that work.
Group membership does not grant the ADMIN role.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Find and inspect a group

![Group filter](images/help/light/UserGroupListView/user-group-global-filter.png)

{{include:filtering}}

![Filter examples](images/help/light/UserGroupListView/user-group-global-filter-help-button.png)

The question mark beside the filter opens examples.

![Matching groups](images/help/light/UserGroupListView/user-group-row-counter.png)

{{include:sorting}}

![Group table](images/help/light/UserGroupListView/user-group-grid.png)

Compare the name, description and member count. Choosing a row opens the group
for editing; it does not navigate into product work.

## Create and edit membership

![Create group](images/help/light/UserGroupListView/create-user-group-button.png)

Create a named group with its description and members, then Save. Cancel
discards unsaved changes. A new group does not automatically receive product access.

![Edit group](images/help/light/UserGroupListView/user-group-grid-edit-button-prefix-.png)

Edit updates the existing group and its membership. Membership changes can
change access to every product granting that group access. Removing a member
does not revoke a separate direct product grant or administrator access.

## Remove a group

![Delete group](images/help/light/UserGroupListView/user-group-grid-delete-button-prefix-.png)

Check the group named in the delete confirmation. Deleting a group is not the
same as deleting its member users, but can remove their group-based route to
product access. Review affected grants before proceeding.

Group changes are not restored by planning undo/redo. For a permission problem,
inspect membership, product grants and application roles separately.

## Use the main menu and header

{{include:main-menu}}

{{include-admin:main-menu-admin}}

![Groups help](images/help/light/UserGroupListView/user-group-help-button.png)

The header's question mark opens this help, separate from filter examples.
