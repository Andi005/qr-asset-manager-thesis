# Evaluation kit (proposal §5)

**Status:** the protocols are written and the tools they need are built into the app (inventory session mode with CSV export, the SUS scorer). **No evaluation sessions have been run yet**, so `data/` contains no results.

There are three components, each answering one research question:

| Component | Research question | Files |
|---|---|---|
| Controlled inventory: Excel vs QR, 30-50 real assets | P2 (time and errors) | [inventory-protocol.md](inventory-protocol.md) |
| Usability test, 5-8 participants, set tasks + SUS | P3 (usability) | [usability-tasks.md](usability-tasks.md), [sus-questionnaire.md](sus-questionnaire.md) |
| Evaluation interview with IT staff | P1/P3 (fit with the real process) | [it-staff-interview.md](it-staff-interview.md) |

Also: [consent-form.md](consent-form.md) for every participant. Raw data goes in `data/` (anonymised: P01, P02, ...).
- **Analysis:** descriptive statistics only (means, percentages, charts), as the proposal says.
- **Scoring SUS:** `java tools/SusScore.java evaluation/data/sus.csv`.

**Schedule:**
- Sessions can run once the system is deployed (docs/deployment.md) and labels are printed with the final URL.
- Before then: recruit participants, get the supervisor's approval, fix the dates.
