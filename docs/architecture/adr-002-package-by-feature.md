# ADR 002: Package by feature, with layers inside each feature

**Context.** A Spring application can be split by technical layer (`controllers/`, `services/`, `repositories/`) or by feature (`asset/`, `assignment/`, `license/`).

**Decision.** Package by feature under `io.github.andi005.qram`: `asset`, `employee`, `assignment`, `license`, `history`, `importer`, `qr`, `inventory`, `dashboard`, `security`, `common`. Inside each feature the classic layers stay strict: **Controller** (HTTP in and out) → **Service** (business rules, transactions) → **Repository** (database). A controller never uses another feature's repository directly; it asks that feature's service.

**Consequences.**
- Everything about one feature is in one folder, which is easy to find, review and explain.
- The layer rule keeps business rules in services, so they are unit-testable without HTTP or a database.
- Features that depend on each other (assignment → asset, employee) do so through services, which keeps the dependencies visible.
