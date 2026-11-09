# ⚠️ MOCK interview (fallback only, NOT real data)

> **This interview never happened.** It's an invented stand-in, written so design work can start before the real interview. It must **never** be quoted, cited or presented as real in the thesis. Once the real interview is done (`interview-<date>.md`), its findings replace these assumptions wherever they differ.

**Invented setting:** the IT office of a fictional college ("Kolegji Shembull"), about 300 staff and students' labs, 2 IT staff. About 450 devices, kept in one shared Excel file (see the equally fictional [sample-data/MOCK-inventory.csv](sample-data/MOCK-inventory.csv)).

## Assumed answers (what a typical small IT office might say)
1. **Recording:** one Excel file on a shared drive. Columns: tag, category, manufacturer, model, serial, purchase date, warranty, location, status, notes. Serial numbers are often missing or mistyped.
2. **Several editors:** the two IT staff plus an administrator sometimes edit it. Copies get out of sync, and there are duplicate tags.
3. **Who has what:** a "location/person" column, overwritten when a device moves, so there's no history. "Who had it last year" is answered from memory or e-mails.
4. **Leaving staff:** a paper form; the Excel is updated "when someone remembers".
5. **Licenses:** a separate sheet, with expiry noticed when software stops working. One antivirus license lapsed last year.
6. **Warranties:** in the purchase invoices, rarely checked before calling support.
7. **Physical inventory:** once a year, two people, about 2-3 days for the whole building. They walk with a printed list and type serial numbers into the laptop. Errors: devices skipped, digits swapped.
8. **Access:** IT staff change records; the administration only needs to view and export.
9. **QR labels:** welcome, if printing is simple and the labels survive cleaning. The worry: someone outside scanning a label → it must need a login.
10. **Most important:** knowing at any moment who has which device, and not losing licenses.

## Assumptions derived from it (marked MOCK in requirements until confirmed)
- Three roles (ADMIN, IT_STAFF, VIEWER) are enough.
- About 1,000 assets is a realistic upper bound for performance.
- CSV export from Excel is an acceptable import path.
- Labels must carry no personal data, and scanning must require a login.
