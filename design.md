# Kassandra Design Notes

This document is a starting point for understanding the system's more involved designs. It deliberately records responsibilities, invariants, and limitations rather than every implementation detail.

## Planning undo and redo

### Purpose

Undo/redo restores planning data after a user-visible change. It covers the planning hierarchy:

```text
Product -> Version -> Feature -> Sprint -> Task -> child Task
                                             -> Worklog
                                             -> Relation
```

The mechanism is product-scoped and durable: history survives server restarts and can restore a soft-deleted planning tree. It is not a replacement for the audit trail; Hibernate Envers records audit revisions independently and can support future audit views.

### Model

`UndoableOperationDAO` is one user-visible action. Its immutable identity, actor, timestamp, product, summary, and per-product sequence number define the history order. `undone` is the history cursor: applied operations have `undone = false`; reverted operations have `undone = true`.

Each operation owns `UndoableOperationEntryDAO` records. An entry contains:

- the entity type and ID;
- a JSON snapshot from before the mutation;
- a JSON snapshot from after the mutation;
- a restore order.

A missing `beforeSnapshot` represents creation; a missing `afterSnapshot` represents deletion. The snapshots make replay deterministic without requiring each controller or service to implement a bespoke inverse action.

`restoreOrder` is important for trees: children are restored before their parents are removed, and parents are restored before their children are needed again. A task-tree delete also captures updates to inbound predecessor relations.

### Write and replay flow

`PlanningChangeService` is the only owner of journal creation and replay:

1. A planning mutation captures its affected entity snapshots in a single transaction.
2. The service creates one `UndoableOperationDAO` for the resolved product and persists all entries.
3. Undo restores each entry's `beforeSnapshot`; redo restores its `afterSnapshot`.
4. Replaying a snapshot first revives a soft-deleted row when necessary, then merges the restored entity state.

New mutations after an undo discard the redo branch for that product. This keeps the history linear, matching the familiar desktop-application undo model.

The service has dedicated tree-deletion methods for task and planning roots. Do not replace these with repository deletion calls: the journal must see every affected row, including descendants, worklogs, and relations.

### API and security

`UndoRedoController` exposes product-scoped undo, redo, range replay, history, and replay preview endpoints. It delegates all state changes to `PlanningChangeService`.

The preview endpoint is the authority for the range that a selected history item affects. A request to undo or redo an older item replays every consecutive operation required to reach that history position. Clients must never calculate that range themselves.

All endpoints enforce product access through `AclSecurityService`; aggregate history validates access to every requested product. Stored snapshots are never exposed by the API. `UndoRedoHistory` projects only display metadata and derived field changes.

The aggregate history endpoint accepts a limit. The database query is paged before results are returned; clients must not fetch and truncate history themselves. Replay preview is intentionally unbounded because confirmation must disclose every operation that will be replayed.

### UI flow

`MainLayout` owns the global action-history toggle and its right-side `UndoHistoryPanel`. Routed views publish their active product scope through `setActiveProductId(s)`; views must use `MainLayout.findParent(this)` because routed content is hosted inside a `SplitLayout`.

`UndoHistoryPanel` retrieves the bounded history, displays applied and undone operations, and opens `UndoHistoryConfirmationDialog` after loading the server preview. Selecting an applied operation requests undo; selecting an undone operation requests redo. Confirmation triggers the appropriate range endpoint, closes the panel, and reloads the current view.

`MainLayout` caches active product DTOs and encountered actor DTOs for the Vaadin session. Avatar resolvers read that cache only; history and preview loading warm the actor cache. This avoids a REST call for every rendered history row while retaining hash-based avatar URLs.

### Important limitations

- History is linear **per product**. Multi-product pages aggregate entries for display, but replay always affects the selected operation's original product.
- Undo restores snapshots, not intent. Concurrent edits to the same planning data can be overwritten by a later replay. Conflict detection is not implemented.
- Soft deletes are required for reversible tree deletion. Physical deletion for GDPR retention policies is a future, separate process and will make expired history non-replayable.
- `updateBatch(...)` currently journals every supplied entity, including unchanged entries. A completely unchanged batch can therefore appear in history; the UI labels it “No updates in any fields.” Avoiding all-no-op operations at write time is a future improvement.
- Relation snapshots normalize regenerated nested IDs and collection order when deriving display field changes. This prevents internal relation UUID changes from being presented as planning changes.

### Where to start

| Concern | Primary class |
| --- | --- |
| Snapshot capture, tree deletion, history ordering, replay | `PlanningChangeService` |
| Persistent operation and entry schema | `UndoableOperationDAO`, `UndoableOperationEntryDAO` |
| Authorized REST projection and replay endpoints | `UndoRedoController` |
| REST client used by the UI | `UndoRedoApi` |
| Global history scope, toggle, and avatar caches | `MainLayout` |
| History list and selected-operation handling | `UndoHistoryPanel` |
| Confirmation rendering for the server preview | `UndoHistoryConfirmationDialog` |
