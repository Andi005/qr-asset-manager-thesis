# System Usability Scale (SUS), Albanian version

The standard SUS (Brooke, 1996): 10 items, scale 1-5. The Albanian translation below is a draft; Andi and the supervisor check the wording before use. Keep the item order and the alternating positive/negative wording exactly as they are; the scoring depends on them.

**Udhëzim:** Për secilën deklaratë, rrethoni një numër nga 1 (*Nuk pajtohem aspak*) deri në 5 (*Pajtohem plotësisht*). Përgjigjuni menjëherë, pa u menduar gjatë.

| # | Deklarata | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|---|
| 1 | Mendoj se do të doja ta përdorja këtë sistem shpesh. | ☐ | ☐ | ☐ | ☐ | ☐ |
| 2 | Sistemi më dukej i ndërlikuar pa nevojë. | ☐ | ☐ | ☐ | ☐ | ☐ |
| 3 | Mendoj se sistemi ishte i lehtë për t'u përdorur. | ☐ | ☐ | ☐ | ☐ | ☐ |
| 4 | Mendoj se do të më duhej ndihma e një personi teknik për ta përdorur këtë sistem. | ☐ | ☐ | ☐ | ☐ | ☐ |
| 5 | Funksionet e ndryshme të sistemit më dukeshin të ndërlidhura mirë. | ☐ | ☐ | ☐ | ☐ | ☐ |
| 6 | Mendoj se kishte shumë mospërputhje në këtë sistem. | ☐ | ☐ | ☐ | ☐ | ☐ |
| 7 | Mendoj se shumica e njerëzve do të mësonin ta përdornin këtë sistem shumë shpejt. | ☐ | ☐ | ☐ | ☐ | ☐ |
| 8 | Sistemi më dukej shumë i rëndë për t'u përdorur. | ☐ | ☐ | ☐ | ☐ | ☐ |
| 9 | Ndihesha shumë i/e sigurt duke e përdorur sistemin. | ☐ | ☐ | ☐ | ☐ | ☐ |
| 10 | M'u desh të mësoja shumë gjëra para se të filloja ta përdorja këtë sistem. | ☐ | ☐ | ☐ | ☐ | ☐ |

## Scoring (done by `tools/SusScore.java`)
- Odd items (1, 3, 5, 7, 9): the answer − 1.
- Even items (2, 4, 6, 8, 10): 5 − the answer.
- Add up the ten values and multiply by 2.5 → a score from 0 to 100 per participant. Report the mean.
- **Reading it:** about 68 is average. Above 68 is better than average, above 80 is good. It is not a percentage.

Data file `data/sus.csv`: `participant,q1,q2,q3,q4,q5,q6,q7,q8,q9,q10`.
