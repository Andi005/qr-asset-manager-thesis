# Protocol: controlled inventory, Excel vs QR (P2)

**Status: DRAFT.** Aim: measure, under the same conditions, how long a physical inventory takes and how many errors it produces with the current method (Excel) and with QR scanning.

## Setup
- **Assets:** 30-50 real devices in one or two rooms, each with a printed QR label (printed after deployment, once the URL is final).
- **Reference list (ground truth):** prepared by the researcher before the runs. Tag, serial number and location of every device, checked by hand twice. Also note 2-3 devices that are in the room but *not* on the expected list (unexpected), and 2-3 that are on the list but deliberately *not* in the room (missing).
- **Two comparable sets:** split the devices into set A and set B, with a similar mix of categories and locations.
- **Operators:** at least 2 people who do inventories in real life, or IT staff. Each operator does one set with each method, in a **counterbalanced order** (this removes the learning effect):
  - Operator 1: Excel on A, then QR on B.
  - Operator 2: QR on A, then Excel on B.
- **Same instructions, same rooms, same time of day, one researcher observing.**

## Run 1: Excel (the current method)
- The operator gets the expected list for the set as an Excel sheet on a laptop. For every device found they mark it present and **type the serial number** and the room, the way it's done today. Devices not on the list are added as new rows.
- **Timing:** from "start" (operator at the first device) to "done" (operator says finished). Use a stopwatch, and note the clock times.

## Run 2: QR (the system's inventory session mode)
- The researcher starts an inventory session with the set's expected list. The operator scans each label with a phone. Devices without a readable label are entered by hand (a fallback, counted as manual typing).
- **Timing:** the system records the session's start, every scan's timestamp, and the end. Also use a stopwatch as a cross-check.

## What is measured (per operator, per run)
| Measure | Excel run | QR run |
|---|---|---|
| Total time (min:s) | stopwatch | session duration (+ stopwatch) |
| Missed assets (on the list, in the room, not recorded) | compare the sheet with the reference | the "missing" list vs the reference |
| Wrong entries (wrong tag / serial / room) | compare the sheet with the reference | compare scans with the reference |
| Unexpected assets correctly reported | count | the "unexpected" list |
| Manual typing (characters typed) | characters typed into the sheet (count from the sheet) | characters typed in manual-entry fallbacks |

## After the runs
- Export the QR session as CSV; save the Excel sheet. Put both in `data/` as `inventory-<operator>-<method>.*`.
- Fill `data/inventory-results.csv` with one row per operator and method: `operator,method,set,seconds,missed,wrong,unexpected_found,typed_chars`.
- Report the means and the differences (Excel vs QR) in a table and a bar chart. Descriptive only; with this sample size there are no significance tests (proposal §5).

## Threats to validity (for the thesis)
- Few operators.
- Sets A and B are not identical.
- Operators know they are being observed.
- The researcher is also the developer.
