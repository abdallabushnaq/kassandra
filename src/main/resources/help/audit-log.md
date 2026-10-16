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

**Audit** is the administrator's read-only record of changes. It answers who
changed what and when; it does not reverse those changes.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Find recorded changes

![Audit search](images/help/light/AuditLogView/audit-search-field.png)

Search using a name, email, action or date such as **YYYY-MM-DD**. This is
server-side audit search, not the AI-powered entity-list filter.

![Actor filter](images/help/light/AuditLogView/audit-user-filter.png)

Use **User name or email** to narrow the actor.

![Action filter](images/help/light/AuditLogView/audit-action-filter.png)

Choose All, CREATE, UPDATE or DELETE.

![Inclusive start](images/help/light/AuditLogView/audit-from-filter.png)

![Exclusive end](images/help/light/AuditLogView/audit-to-filter.png)

**From** is inclusive and **To** is exclusive. The end must be after the start.
The filters use the server's time zone; the table explicitly displays timestamps
in **UTC**, so take that difference into account when investigating a time.

![Apply filters](images/help/light/AuditLogView/audit-apply-button.png)

Press **Apply** to fetch matching results and return to the first page. Editing
the filter fields alone does not apply them.

## Inspect an update

![Audit grid](images/help/light/AuditLogView/audit-grid.png)

The table identifies When (UTC), Who, Action, Type and What. Several rows can
describe related changes; do not assume one row represents an entire workflow.

![Expanded update](images/help/light/AuditLogView/audit-details-.png)

Click an **UPDATE** row to inspect its field changes. Click again to close it,
or choose another update. Only one detail section is open at a time; CREATE
and DELETE rows do not open update-field details.

Use **Undo history** on supported planning pages when you need to replay a
recorded operation. Audit pagination and expanding a row do not change data.

## Browse older results

![Previous page](images/help/light/AuditLogView/audit-previous-button.png)

![Next page](images/help/light/AuditLogView/audit-next-button.png)

Previous and Next move through matching results, **50 records per page**.
Disabled buttons mark the boundaries of the current result set.

![Page number](images/help/light/AuditLogView/audit-page-number.png)

![Go to page](images/help/light/AuditLogView/audit-go-button.png)

Enter a page number from 1 through the displayed last page and press **Go**.
Refine the filters if there are too many results; an empty result is not proof
that an operation never occurred under another actor, label or time range.

## Use the main menu and header

{{include:main-menu}}

{{include-admin:main-menu-admin}}

![Audit help](images/help/light/AuditLogView/audit-help-button.png)

The question mark beside **Audit** opens this help. Audit access is restricted
to administrators.
