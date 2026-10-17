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

Review the complete planning-operation range before undoing or redoing it.
Opening this confirmation or its help does not replay anything.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Review the whole range, not only the clicked operation

![Operations selected for undo](images/help/light/UndoHistoryConfirmationDialog/undo/undo-history-confirmation-operations.png)

The server selects the affected range. Choosing an older history entry can
include intervening operations; the confirmation is not a request to reverse
only the item you clicked.

Check each operation's product, actor, timestamp and summary. Expand an
operation to inspect the affected entities and available field changes.
Scroll through the preview before confirming a range that spans several
operations.

## Check the direction

![Operations selected for redo](images/help/light/UndoHistoryConfirmationDialog/redo/undo-history-confirmation-operations.png)

**Undo** reverses recorded planning changes. **Redo** reapplies previously
undone changes. Read the dialog heading and action caption: the same history
workflow can open either direction.

The preview describes recorded planning operations, not every action performed
elsewhere in the application. User administration and other unrecorded settings
are not restored by this confirmation.

## Confirm only the range you intend

![Confirm undo](images/help/light/UndoHistoryConfirmationDialog/undo/undo-history-confirmation-confirm.png)

![Confirm redo](images/help/light/UndoHistoryConfirmationDialog/redo/undo-history-confirmation-confirm.png)

![Cancel replay](images/help/light/UndoHistoryConfirmationDialog/undo/undo-history-confirmation-cancel.png)

**Undo operations** or **Redo operations** submits the displayed range for
replay. This can affect multiple entities and the current page refreshes after
successful replay. If the server rejects the replay, read the error and
refresh history before trying again.

**Cancel** leaves the planning data unchanged. Use it when any operation or
the selected direction is unexpected; opening help is never confirmation.
