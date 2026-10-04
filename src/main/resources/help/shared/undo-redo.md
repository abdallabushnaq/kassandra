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

Use the **history button in the top navigation** to inspect recorded planning
changes, including creation, editing and deletion. Expand an entry to check
what changed and which product it belongs to. **Undo** reverses applied changes;
**Redo** reapplies undone changes.

History is scoped to a **product**, not to an individual row, feature or sprint.
Undoing through an older entry also reverses the later applied entries for that
product. Redoing through an entry can reapply preceding undone entries. **Review
all affected changes in the confirmation**, including work outside the current
list, before proceeding. Another product is not rolled back with it.

The **Products** page can show history for multiple products, including deleted
products you are permitted to recover. Use it to find a recorded deletion when
the product is no longer in the list. Undoing that deletion restores its recorded
planning hierarchy; creating a new item with the same name does not.

History shows a limited set of recent operations and is not a backup of every
application setting. Use the entry details and confirmation to establish what
will be restored rather than assuming that every setting or image is included.
