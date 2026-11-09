# Usability test: tasks (P3)

**Status: DRAFT.** 5-8 participants (IT staff, administrative staff, and a few students), one at a time, about 25 minutes each. The researcher reads the tasks aloud and gives a printed card; the participant thinks aloud. No help unless stuck for more than 3 minutes (that counts as "with help").

**Setup:** the deployed system (warmed up), a phone with a camera, a laptop, a printed label on a real device, the demo data, and a VIEWER/IT_STAFF login prepared.

## Task cards (Albanian, as given to participants)
**D1. Regjistroni një aset të ri.**
Një laptop i ri sapo ka ardhur: Lenovo ThinkPad T14, nr. serik PF4ABC12, blerë më 01.11.2026, garancia deri më 01.11.2029, vendi: Ndërtesa B / Laboratori 1. Regjistrojeni në sistem.

**D2. Ngarkojeni asetin te një punonjës.**
Laptopi IT-000012 duhet t'i jepet punonjëses Drita Krasniqi. Bëjeni këtë në sistem.

**D3. Gjeni licencat që skadojnë brenda 90 ditësh.**
Cilat licenca softuerike skadojnë brenda 90 ditëve të ardhshme? Cila skadon e para?

**D4. Lokalizoni një aset me skanim.**
Me telefon, skanoni etiketën në pajisjen para jush. Kush e ka këtë pajisje tani dhe ku duhet të ndodhet?

## What the researcher records (per participant, per task)
`participant,task,result,seconds,errors,notes`
- **result:** `ok` (done alone) / `help` (done with help) / `fail`.
- **seconds:** from finishing reading the card to the answer or the saved record.
- **errors:** wrong clicks that needed going back, plus wrong data entered.
- **notes:** think-aloud quotes, confusions.

Save as `data/usability-results.csv`. Then the participant fills in the [SUS questionnaire](sus-questionnaire.md) → `data/sus.csv`.

## Afterwards
Per task: the success rate, the mean time, and the most frequent problems. These go into the thesis with the SUS score.
