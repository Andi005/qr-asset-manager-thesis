# ADR 005: Assignment history as rows; "assigned" is never a status

**Context.** The proposal names missing history as a core problem: hand-kept sheets overwrite the "person" column when a device moves, so nobody can say who had it last year. One option was an `assigned_to` column on the asset plus a separate history table. Another was a status value `ASSIGNED`.

**Decision.**
- One table, `assignment`, with one row per holding period: asset, employee, `assigned_at`, `returned_at` (null while it lasts), and who did it.
- The **current holder** is the open row. A partial unique index (`unique (asset_id) where returned_at is null`) guarantees at most one per asset, even if two people click at the same moment.
- `asset.status` describes only the physical state (IN_STOCK, IN_REPAIR, RETIRED). "Assigned" is derived from the open row and never stored.
- Assign and return write the row and a `change_log` line in the same transaction (`ChangeLogService.record` requires an existing transaction).

**Consequences.**
- The current holder and the history come from the same data and can't contradict each other.
- "Who had asset X on date D?" is a single query (`AssignmentRepository.holderAt`).
- Rows are closed, never deleted, and foreign keys are RESTRICT, so the history survives employees leaving and assets being retired.
- Retiring or sending an asset to repair first requires a return, so a retired asset never has a holder.
