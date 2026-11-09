# Sample data (MOCK, fictional)

Organisations won't share real asset records, so `MOCK-inventory.csv` is **invented**. It looks like a hand-kept Excel sheet saved as CSV in Albanian regional settings:
- `;` as the separator, UTF-8 with a BOM, CRLF line endings;
- dates as `dd.MM.yyyy`;
- 45 rows plus an empty trailing row.

Some rows contain **deliberate mistakes**, as real hand-kept files do. The importer must report them (checked by `ImportValidatorTest`):
- a missing serial number;
- a duplicate tag;
- an impossible date (31.02);
- an unknown category and an unknown status;
- an empty model;
- a wrongly formatted tag (`it-26`);
- the same serial number twice;
- an ISO date with month 13;
- a location typed without `ë`.

The `Shënime` (notes) column says what's wrong in each such row, so it's easy to check the import report against it. Never present this file as real data.
