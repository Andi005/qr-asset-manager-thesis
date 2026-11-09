# ADR 001: Stack and versions

**Context.** The approved proposal fixes the stack: Java, Spring Boot, Thymeleaf, PostgreSQL, Spring Security, Bootstrap, ZXing, html5-qrcode, Git/GitHub. The versions and the supporting libraries were open. The developer is a beginner who already uses Java 25, Maven and JUnit in a course.

**Decision** (versions checked on Maven Central and npm on 2026-11-03):
- **Java 25 LTS** (Temurin): the long-term-support release, the same JDK as the course.
- **Spring Boot 4.1.2**: the latest GA. 4.2.0 is only a release candidate. We take patch updates only.
- **Maven** with the wrapper: one build tool the developer knows; no install needed.
- **Spring Data JPA / Hibernate 7.4, Flyway 12.4, PostgreSQL 18**: the standard Spring persistence stack. Flyway migrations are plain SQL.
- **Thymeleaf 3.1** with fragments for the layout (no extra layout library), `thymeleaf-extras-springsecurity6`.
- **Bootstrap 5.3.8 and html5-qrcode 2.3.8**, served by the app itself (WebJar, vendored), with **no CDN**, so the system also works offline.
- **ZXing 3.5.4** for QR generation; **Apache Commons CSV 1.14.1** for import.
- **JUnit 6, Mockito, AssertJ, Spring test slices, Testcontainers 2.0.5** (real PostgreSQL in tests).
- **Spotless** (google-java-format) to format code automatically. **JaCoCo** for a coverage report. **PlantUML** for diagrams as code.

**Consequences.**
- Every version is managed in `pom.xml` (mostly through the Spring Boot parent). Dependabot only proposes security fixes, and the versions are frozen at tag `v1.0-eval` before the evaluation.
- html5-qrcode has had no release since 2023. It's named in the proposal and works in current browsers, but it has to be tested early on iOS Safari; fallback: the "scan from photo" mode.
- Not chosen:
  - Gradle (a second tool to learn);
  - Liquibase (XML/YAML instead of SQL);
  - H2 for tests (behaves differently from PostgreSQL);
  - Playwright end-to-end tests (setup cost; full-flow tests over HTTP plus a manual camera protocol cover the main flows).
