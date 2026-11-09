# Approved thesis proposal (text copy, personal data removed)

This is the source of truth for scope. It's a text copy of the approved form
"Kërkesë për aprovimin e temës" (UBT College, form 55-27), with the student's ID
number, phone and e-mail removed. The original .docx is kept outside this repo.

- **Student:** Andi Suka · Information Systems, Bachelor · enrolled 2023
- **Title (EN):** Design and implementation of a web system for managing IT assets and software licenses with QR code integration
- **Methodology:** Design Science Research

## English summary (for reviewers; the Albanian text below is authoritative)
- **Problem:**
  - IT units in Kosovo's higher-education institutions and SMEs keep their asset records in Excel or on paper;
  - there is no reliable history of who held which device and when;
  - records get duplicated when several people edit them;
  - license and warranty expiry goes unnoticed;
  - physical inventory is slow and full of manual transcription errors.
- **Objectives:**
  1. analyse the current process and derive functional and non-functional requirements;
  2. design the data model and architecture, with UML (use case, class) and ER diagrams;
  3. implement:
     - the asset register;
     - assignment to users with history;
     - licenses with seat counts and expiry;
     - warranty tracking;
     - access roles;
     - import from Excel/CSV;
  4. integrate QR label generation and printing, and scanning from a phone browser for instant access to an asset's record;
  5. evaluate empirically.
- **Research questions:**
  - P1: how to model and implement such a system for a real IT unit;
  - P2: how much QR codes reduce inventory time and errors compared with Excel;
  - P3: how users rate usability.
- **Stack (phase 4):** Java, Spring Boot, server-rendered Thymeleaf pages, PostgreSQL, Spring Security, Bootstrap, ZXing (QR generation), html5-qrcode (browser scanning), Git/GitHub.
- **Evaluation (section 5):**
  1. A controlled inventory of 30-50 real assets, done once with Excel and once with QR scanning. It measures time, missed or wrongly recorded assets, and the amount of manual typing, checked against a reference list.
  2. A usability test with 5-8 participants on set tasks: register an asset, assign it to a user, find licenses expiring within 90 days, locate an asset by scanning. It is followed by the System Usability Scale (SUS).
  3. An evaluation interview with IT staff.

  Descriptive statistics only.
- **Out of scope (section 7):** native mobile app (scanning is through the browser), procurement and purchasing, integration with existing university systems, automatic network discovery of devices, financial depreciation.

## Original text (Albanian)

### Titulli i punimit të diplomës

Dizajnimi dhe implementimi i një sistemi web për menaxhimin e aseteve të TI-së dhe licencave softuerike me integrim të kodeve QR

### Kjo temë është e rëndësishme për arsye:

Menaxhimi i aseteve të teknologjisë së informacionit (IT Asset Management) është proces thelbësor për çdo organizatë që posedon një numër të konsiderueshëm pajisjesh kompjuterike dhe licencash softuerike. Megjithatë, në shumicën e institucioneve arsimore dhe të ndërmarrjeve të vogla e të mesme në Kosovë ky proces ende mbahet në fletë pune Excel ose edhe në formë të shtypur. Kjo praktikë sjell probleme konkrete dhe të matshme: mungesën e një historiku të besueshëm se cili punonjës e ka pasur në ngarkim një pajisje dhe në cilën periudhë; mospërputhjen dhe dublikimin e të dhënave kur skedarët përditësohen nga disa persona veç e veç; skadimin e pavërejtur të licencave softuerike dhe të garancioneve; si dhe një inventarizim fizik të ngadalshëm, i cili shoqërohet me gabime gjatë transkriptimit manual të numrave serialë.

Tema është e rëndësishme sepse e adreson këtë problem real organizativ me mjetet e fushës së Sistemeve të Informacionit dhe nuk mbetet vetëm në nivel konceptual: rezultati është një artefakt funksional, vlera e të cilit vërtetohet empirikisht përmes krahasimit të drejtpërdrejtë me gjendjen aktuale. Përveç kontributit praktik për njësinë e TI-së të institucionit, punimi ofron një model të riprodhueshëm digjitalizimi për organizata të tjera në Kosovë që ballafaqohen me të njëjtën problematikë. Njëkohësisht, tema i ndërlidh në një zgjidhje të vetme njohuritë e fituara gjatë studimeve në analizën dhe dizajnimin e sistemeve të informacionit, bazat e të dhënave, programimin dhe menaxhimin e projektit.

### Më poshtë është një deklaratë e problemi/zbrastirës së temës, qëllimin e saj, burim i materialeve dhe metodave të hulumtimit që do të përdoren.

### 1. Deklarata e problemit dhe zbrazëtira

Njësitë e TI-së në institucionet e arsimit të lartë në Kosovë zakonisht nuk disponojnë një sistem të centralizuar për evidentimin e aseteve harduerike, të licencave softuerike dhe të ngarkimit të tyre ndaj përdoruesve. Të dhënat ruhen në skedarë të shpërndarë, pa kontroll qasjeje, pa historik të ndryshimeve dhe pa mekanizëm paralajmërimi për skadimin e licencave e të garancioneve. Si pasojë, inventarizimi fizik është i ngadalshëm, raportimi i pabesueshëm dhe vendimmarrja për zëvendësimin ose riblerjen e pajisjeve mbështetet në të dhëna të vjetruara. Zbrazëtira që e adreson ky punim qëndron në mungesën e një zgjidhjeje të lehtë për t’u implementuar dhe të përshtatur për organizata të vogla, e cila e kombinon evidencën e aseteve me identifikimin fizik përmes kodeve QR dhe e dëshmon përfitimin me të dhëna të matura, jo vetëm me pohime cilësore.

### 2. Qëllimi dhe objektivat

Qëllimi i punimit është dizajnimi, implementimi dhe vlerësimi i një sistemi web për menaxhimin e aseteve të TI-së dhe të licencave softuerike, i cili e zëvendëson evidencën aktuale në Excel dhe e përshpejton inventarizimin fizik përmes kodeve QR. Objektivat specifike janë:

- të analizohet procesi aktual i evidentimit dhe i inventarizimit të aseteve dhe të nxirren kërkesat funksionale e jofunksionale të sistemit;
- të dizajnohet modeli i të dhënave dhe arkitektura e sistemit, e dokumentuar me diagrame UML (rastet e përdorimit, diagrami i klasave) dhe me diagramin ER të bazës së të dhënave;
- të implementohet sistemi web me regjistrimin e aseteve, ngarkimin e tyre ndaj përdoruesve me historik, menaxhimin e licencave me numër vendesh dhe afat skadimi, ndjekjen e garancioneve, rolet e qasjes dhe importimin e të dhënave ekzistuese nga Excel/CSV;
- të integrohet gjenerimi dhe printimi i etiketave me kod QR, si dhe skanimi i tyre nga shfletuesi i telefonit për qasje të menjëhershme në të dhënat e asetit;
- të vlerësohet sistemi empirikisht përmes krahasimit me metodën ekzistuese, testimit të shfrytëzueshmërisë dhe vlerësimit nga personeli i TI-së.

### 3. Pyetjet kërkimore

P1: Si mund të modelohet dhe të implementohet një sistem i centralizuar i menaxhimit të aseteve dhe të licencave që i plotëson kërkesat reale të një njësie të TI-së në një institucion të arsimit të lartë?

P2: Sa e zvogëlon përdorimi i kodeve QR kohën e nevojshme dhe numrin e gabimeve gjatë inventarizimit fizik, krahasuar me metodën aktuale të bazuar në Excel?

P3: Si e vlerësojnë përdoruesit shfrytëzueshmërinë e sistemit të propozuar?

### 4. Metodologjia e hulumtimit

Punimi ndjek metodologjinë e hulumtimit përmes dizajnit (Design Science Research), e cila është e përshtatshme për tema ku kontributi kryesor është ndërtimi dhe vlerësimi i një artefakti softuerik. Puna organizohet në pesë faza:

- Faza 1 – Rishikimi i literaturës: shqyrtimi i praktikave dhe i standardeve të menaxhimit të aseteve (ISO/IEC 19770-1, praktikat ITIL) dhe i zgjidhjeve ekzistuese komerciale e me kod të hapur, me qëllim identifikimin e funksionaliteteve thelbësore dhe të mangësive të tyre për organizatat e vogla.
- Faza 2 – Mbledhja e kërkesave: intervistë gjysmë e strukturuar me personin përgjegjës për asetet e TI-së në institucion dhe analiza e skedarëve ekzistues të evidencës, për të nxjerrë kërkesat dhe rastet e përdorimit.
- Faza 3 – Dizajni: modelimi i bazës së të dhënave (diagrami ER), diagrami i klasave, diagramet e rasteve të përdorimit dhe dizajni i ndërfaqes.
- Faza 4 – Implementimi: zhvillimi i sistemit në Java me kornizën Spring Boot dhe faqe të gjeneruara në server me Thymeleaf, baza e të dhënave PostgreSQL, siguria dhe rolet me Spring Security, ndërfaqja me Bootstrap, gjenerimi i kodeve QR me bibliotekën ZXing dhe skanimi i tyre nga shfletuesi me html5-qrcode. Kodi mbahet nën kontroll versionesh në Git/GitHub.
- Faza 5 – Testimi dhe vlerësimi: testime njësie dhe testim funksional i kërkesave, të pasuara nga vlerësimi empirik i përshkruar më poshtë.

### 5. Vlerësimi i rezultatit

Vlerësimi ka tri komponente. E para, një inventarizim i kontrolluar i një grupi prej 30–50 asetesh reale, i kryer njëherë me metodën aktuale (Excel) dhe njëherë me skanimin e kodeve QR, ku maten koha e nevojshme, numri i aseteve të anashkaluara ose të regjistruara gabimisht dhe sasia e shtypjes manuale, me verifikim ndaj një liste referente. E dyta, testimi i shfrytëzueshmërisë me 5–8 pjesëmarrës mbi detyra të përcaktuara (regjistrimi i një aseti, ngarkimi te një përdorues, gjetja e licencave që skadojnë brenda 90 ditësh, lokalizimi i një aseti përmes skanimit), i ndjekur nga pyetësori standard System Usability Scale (SUS). E treta, një intervistë vlerësuese me personelin e TI-së mbi përputhshmërinë e sistemit me procesin real. Për shkak të madhësisë së kampionit, të dhënat analizohen me statistikë përshkruese (mesatare, përqindje dhe paraqitje grafike), pa testime të domethënies statistikore.

### 6. Burimi i materialeve

Literatura shkencore sigurohet nga bazat IEEE Xplore, ScienceDirect, SpringerLink dhe Google Scholar; standardet dhe praktikat profesionale nga dokumentet e ISO/IEC 19770-1 dhe ITIL; dokumentacioni teknik nga burimet zyrtare të Spring Boot, PostgreSQL dhe ZXing. Të dhënat primare mblidhen drejtpërdrejt nga njësia e TI-së e institucionit përmes intervistave, skedarëve ekzistues të evidencës dhe matjeve gjatë fazës së vlerësimit.

### 7. Kufizimet dhe shtrirja e punimit

Punimi përqendrohet në evidencën, ngarkimin dhe inventarizimin e aseteve e të licencave. Jashtë shtrirjes mbeten: aplikacioni mobil nativ (skanimi realizohet përmes shfletuesit), procesi i prokurimit dhe i blerjes, integrimi me sistemet ekzistuese universitare, zbulimi automatik i pajisjeve në rrjet dhe llogaritja e zhvlerësimit financiar të aseteve. Këto kufizime janë vendosur me qëllim që puna të jetë e realizueshme brenda një semestri dhe të vlerësohet në thellësi.
