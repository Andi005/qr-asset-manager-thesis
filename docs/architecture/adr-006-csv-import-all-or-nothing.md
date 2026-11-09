# ADR 006: CSV import is all or nothing, with a full error report

**Context.** Existing records live in hand-kept Excel sheets. They contain duplicates, impossible dates and typos, as the fictional `MOCK-inventory.csv` illustrates. An import could skip bad rows, stop at the first error, or refuse the whole file.

**Decision.**
- The input is CSV saved from Excel. Reading `.xlsx` directly is out of scope (a named limitation).
- The parser tolerates what Excel produces: a BOM, `;` or `,`, Windows-1252 (with a warning), reordered columns, and Albanian or English headers.
- The validator checks **every** row and collects **all** problems with line and column, instead of stopping at the first one.
- **All or nothing:** if any row has an error, nothing is written. Otherwise every row is written in one transaction.
- Missing serial numbers are warnings, not errors (FR-01 makes the serial optional).
- A location that differs from a known one only by `ë`/`ç` or letter case is an error, because otherwise a typo would silently create a second room.

**Consequences.**
- The database never ends up with half an import that is hard to find and undo.
- One report shows the user everything to fix in the sheet, in one pass.
- Rows with their own tag are written first, then the tag sequence moves past them, so tags generated later (for rows without a tag, or new assets) never collide with imported ones.
