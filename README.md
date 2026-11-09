# QR Asset Manager

A web system for managing IT assets and software licenses, with QR code labels that are scanned from a phone browser.

Bachelor thesis: Andi Suka, Information Systems, UBT College (2026-2027).
> *Dizajnimi dhe implementimi i një sistemi web për menaxhimin e aseteve të TI-së dhe licencave softuerike me integrim të kodeve QR*

The user interface is in Albanian. The code, comments and documentation are in English.

**Live:** https://qr-asset-manager-as5w.onrender.com (fictional demo data; the free instance sleeps when idle, so the first visit can take about a minute)

## Features
Each feature traces to a requirement in [docs/requirements/requirements.md](docs/requirements/requirements.md).

- **Hardware asset register** (FR-01 to FR-03): system-generated tags (`IT-000123`), search and filters, edit with concurrent-edit detection, retire instead of delete.
- **Employees and locations** (FR-04): employees are deactivated, never deleted.
- **Assignments with full history** (FR-05, FR-06): at most one holder per asset, enforced by the database. You can always answer "who had which device, and when".
- **Change history** (FR-07): who changed what, and when, on every asset, employee and license.
- **Software licenses** (FR-08, FR-09): seat counts, seats given to employees or devices, and the seat limit can never be exceeded.
- **Warranties and the 90-day expiry view** (FR-10, FR-11): licenses and warranties that expire from today to today+90 (inclusive), plus the ones already expired. Both also appear on the dashboard.
- **Roles** (FR-12): ADMIN, IT_STAFF and VIEWER, checked on the server for every URL and every service method.
- **CSV import** (FR-13): Excel files saved as CSV. The import is all or nothing, and the report lists every bad row with its line number and the reason.
- **QR labels** (FR-14): a printable A4 sheet. Each label holds only `<base-url>/a/<TAG>`, with no personal data.
- **Phone scanning** (FR-15): open the asset from any camera app, or use the built-in scanner page. It falls back to "scan from a photo" where the browser blocks the camera.
- **Inventory sessions** (FR-16): the expected list comes from a location or a list of tags. Every scan is timestamped, and the session shows found, missing and unexpected assets and the duration. Results export to CSV for the Excel-vs-QR comparison (research question P2).

## Quick start (Docker, no Java needed)
You need [Docker Desktop](https://www.docker.com/products/docker-desktop/).

```bash
git clone https://github.com/Andi005/qr-asset-manager-thesis.git
cd qr-asset-manager-thesis
docker compose up --build
```

Then open **http://localhost:8080**. The system starts with **fictional demo data**:

| User | Password | Role |
|---|---|---|
| `admin` | `admin12345` | ADMIN |
| `it` | `demo12345` | IT_STAFF |
| `viewer` | `demo12345` | VIEWER |

To stop it, press `Ctrl+C`, then run `docker compose down`. Add `-v` to also delete the database.

## Run from source (for development)
You need Java 25 and Docker Desktop (for PostgreSQL and for the tests).

```bash
docker compose up -d db                                     # PostgreSQL only
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo      # Windows: .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Without `ADMIN_INITIAL_PASSWORD`, the first start creates `admin` with a random password and prints it in the log. The full guide, including testing with a phone, is in [docs/running-locally.md](docs/running-locally.md).

## Tests
```bash
./mvnw verify            # Windows: .\mvnw.cmd verify
```

This builds the app, checks formatting, runs all unit and integration tests and writes a coverage report to `target/site/jacoco/index.html`. The integration tests start a real PostgreSQL 18 in Docker (Testcontainers), so Docker must be running. [docs/how-the-app-works.md](docs/how-the-app-works.md#tests) explains which test covers which rule.

## Configuration
All settings come from environment variables. No secrets are stored in the repository.

| Variable | Default | What |
|---|---|---|
| `DATABASE_URL` / `DATABASE_URL_UNPOOLED` | none | Neon's pooled (app queries) and direct (migrations) `postgresql://` URLs, used when `DB_URL` is not set ([deployment](docs/deployment.md#1-database-on-neon)) |
| `DB_URL` | `jdbc:postgresql://localhost:5432/qram` | JDBC URL of the PostgreSQL database (takes precedence) |
| `DB_USER` / `DB_PASSWORD` | `qram` / `qram` | database credentials for `DB_URL` |
| `APP_BASE_URL` | `http://localhost:8080` | public address printed on QR labels (must be final before printing) |
| `ADMIN_INITIAL_PASSWORD` | random, printed to the log | password of the first `admin` account (only used on an empty database) |
| `SPRING_PROFILES_ACTIVE` | none | `demo` loads the fictional demo data into an empty database |
| `DEMO_PASSWORD` | `demo12345` | password of the `it` and `viewer` demo accounts |
| `APP_ZONE` | `Europe/Belgrade` | time zone used to decide what "today" is |
| `PORT` | `8080` | HTTP port (set by the hosting platform) |

## Deployment
GitHub Pages only hosts static files, so it can't run this app, which needs a Java server and a database. The plan in [ADR 004](docs/architecture/adr-004-hosting.md) is a **Render** free web service built from the Dockerfile, with a **Neon** free PostgreSQL database. Both are free, give you HTTPS (phone cameras require it), and deploy from GitHub. Step by step: [docs/deployment.md](docs/deployment.md).

## Stack
Java 25 · Spring Boot 4.1 · Thymeleaf · Spring Security · Spring Data JPA / Hibernate · PostgreSQL 18 · Flyway · Bootstrap 5 · ZXing · html5-qrcode · Apache Commons CSV · JUnit + Mockito + Testcontainers · Docker · GitHub Actions. The reasons for each choice are in [ADR 001](docs/architecture/adr-001-stack-and-versions.md).

## Repository map
| Path | What |
|---|---|
| `src/main/java/io/github/andi005/qram/` | the application, one package per feature (see [ADR 002](docs/architecture/adr-002-package-by-feature.md)) |
| `src/main/resources/db/migration/` | the database schema as versioned Flyway migrations |
| `src/main/resources/templates/`, `static/` | Thymeleaf pages, CSS and the scanner JavaScript |
| `src/main/resources/messages.properties` | every UI text, in Albanian |
| `src/test/` | unit and integration tests |
| `docs/` | how the app works, running and deployment guides, requirements, architecture decisions, proposal |
| `evaluation/` | protocols for the evaluation (inventory Excel vs QR, usability tasks, SUS questionnaire) |
| `tools/SusScore.java` | scores SUS questionnaires: `java tools/SusScore.java evaluation/data/sus.csv` |
| `render.yaml`, `Dockerfile`, `compose.yaml` | deployment and local run |

## Documentation
- [How the app works](docs/how-the-app-works.md): architecture, data model, request, scan and inventory flows, security, tests, limitations.
- [Running locally](docs/running-locally.md): Docker, from source, testing with a phone, troubleshooting.
- [Deployment](docs/deployment.md): Render + Neon step by step.
- [Requirements](docs/requirements/requirements.md) and [traceability](docs/requirements/traceability.md) (requirement → code → test).
- [Architecture decisions](docs/architecture/README.md).
