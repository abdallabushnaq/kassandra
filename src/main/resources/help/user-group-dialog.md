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

Create or maintain a group of collaborators. A group can receive product
access grants, so membership changes can affect access to existing work.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Give the group a clear purpose

![Group name](images/help/light/UserGroupDialog/user-group-name-field.png)

The required group name must be unique. Renaming an existing group does not
create a new group or require recreating its product grants.

![Group description](images/help/light/UserGroupDialog/user-group-description-field.png)

Use the optional description to explain who belongs in the group. The editor
limits it to 500 characters.

## Check the access consequences of membership

![Group members](images/help/light/UserGroupDialog/user-group-members-field.png)

Select the people who should belong to this group. Creating a group alone
does not grant it access to a product: grant that access in the product form.

Adding someone to a group that already has product access gives them that
route to the product and its work. Removing someone removes this group's
route, but not individual grants, another group's grants or administrator
access. Check all applicable grants before assuming access has been revoked.

## Apply the membership changes

![Save group](images/help/light/UserGroupDialog/save-user-group-button.png)

![Cancel group changes](images/help/light/UserGroupDialog/cancel-user-group-button.png)

**Save** applies the name, description and selected membership and closes the
dialog on success. Correct field errors or read the dialog-level failure
message before retrying.

**Cancel** discards unsaved selections. Saved group administration is not
replayed through planning undo/redo.
