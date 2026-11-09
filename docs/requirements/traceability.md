# Traceability: requirement → implementation → tests

Each requirement from [requirements.md](requirements.md), where it lives in the code, and the tests that prove it. Test classes are in `src/test/java/io/github/andi005/qram/`.

| Requirement | Implementation | Tests |
|---|---|---|
| FR-01 register asset, generated tag | `asset/AssetService.create`, `V1` (`asset_tag_seq`, `uq_asset_tag`, `ck_asset_tag`) | `AssetServiceTest` (generatesTag, skipsUsedTag, serial rules), `FullFlowTest.createAndAssign`, `DatabaseConstraintsTest` (uniqueTag, tagFormat) |
| FR-02 list, search, filter | `AssetRepository.search`, `AssetController.list` | `RoleAccessTest` (GET /assets), manual check in the UI |
| FR-03 edit, retire, concurrent edits | `AssetService.update` (`@Version`), statuses instead of deletion | `AssetServiceTest` (concurrentEdit, retire, retireWhileAssigned), `FullFlowTest.concurrentEditIsDetected` |
| FR-04 employees, locations | `employee/*`, `asset/LocationService` | `DatabaseConstraintsTest.noDeleteWithHistory`, `RoleAccessTest` |
| FR-05 assign / return | `assignment/AssignmentService`, partial unique index `uq_assignment_open_per_asset` | `AssignmentServiceTest`, `DatabaseConstraintsTest` (oneOpenAssignmentPerAsset, closedAssignmentAllowsNewOne), `FullFlowTest.createAndAssign` |
| FR-06 assignment history | `assignment` rows closed, never deleted; asset and employee pages | `AssignmentServiceTest.returns`, `DatabaseConstraintsTest.noDeleteWithHistory` |
| FR-07 change history | `history/ChangeLogService` (propagation MANDATORY), `FieldChanges` | `AssignmentRollbackTest` (log and change are atomic), `AssignmentServiceTest.assigns` |
| FR-08 licenses | `license/LicenseService`, `V3` | `LicenseServiceTest`, `DatabaseConstraintsTest.positiveSeats` |
| FR-09 seat allocation | `LicenseService.allocate*` with `SELECT … FOR UPDATE`, `ck_allocation_holder`, partial unique indexes | `LicenseServiceTest` (refusesWhenFull, releaseThenReallocate, seatsBelowUsed, oneSeatPerHolder), `DatabaseConstraintsTest` (allocationNotBoth, allocationNotNeither, oneOpenSeatPerHolder) |
| FR-10 warranties | `asset.warranty_end_date`, `AssetRepository.warranties*` | `FullFlowTest.expiryViewBoundary`, `ImportValidatorTest.dateRules` |
| FR-11 expiring within 90 days | `common/ExpiryWindow`, `dashboard/ExpiryService`, `/expiring` | `ExpiryWindowTest` (today−1, today, today+90, today+91, no date, leap year), `FullFlowTest.expiryViewBoundary` |
| FR-12 roles, login | `security/SecurityConfig` (URL rules + role hierarchy), `@PreAuthorize` on services | `RoleAccessTest` (role table, POST without CSRF → 403, API → 401), `InventoryApiTest.viewerCannotScan` |
| FR-13 CSV import | `importer/CsvAssetParser`, `ImportValidator`, `ImportService` (all or nothing) | `ImportValidatorTest` (every mistake in the mock file), `ImportServiceTest` |
| FR-14 QR labels | `qr/QrCodeService` (ZXing), `/labels`, `/qr/{tag}.png` | `TagParserTest`; label content checked manually (scan a printed label) |
| FR-15 phone scanning | `/a/{tag}`, `/scan`, `qr/TagParser`, `/api/assets/lookup` | `TagParserTest`, `FullFlowTest.labelUrlLeadsThroughLoginToAsset`, `RoleAccessTest`; camera: manual check on Android Chrome and iOS Safari |
| FR-16 inventory session | `inventory/InventoryCalculator`, `InventoryService`, `InventoryCsv`, `/api/inventory/{id}/scans` | `InventoryCalculatorTest`, `InventoryApiTest` (classification, 409 after finish, CSV) |
| NFR-01 Albanian UI | `messages.properties`, `Labels` | `QrAssetManagerApplicationTest.loginPageIsAlbanian`, `FullFlowTest` (Albanian messages) |
| NFR-02 phone usable | `static/js/qr-scanner.js` (3 s debounce, photo fallback), responsive Bootstrap pages | manual check at phone width and on a phone |
| NFR-03 HTTPS | Render (TLS at the proxy), `server.forward-headers-strategy` | checked after deployment ([deployment.md](../deployment.md)) |
| NFR-04 security basics | BCrypt, CSRF, server-side roles, validation, env-var configuration, `CsvSafe` | `RoleAccessTest`, `CsvSafeTest`, `TagParserTest` |
| NFR-05 DB integrity | constraints and indexes in `V1`–`V5` | `DatabaseConstraintsTest` |
| NFR-06 migrations | Flyway `db/migration/V*.sql`, Hibernate `ddl-auto: validate` | every integration test (the app starts only if the entities match the migrations) |
| NFR-07 response time | indexes on status, location, warranty, expiry; paged asset list (50 per page) | to measure on the deployed app with the demo data |
| NFR-08 Docker, fresh clone | `Dockerfile`, `compose.yaml` | CI `docker` job |
| NFR-09 tests + CI | `.github/workflows/ci.yml` | CI `build` job (129 tests) |
| NFR-10 privacy | labels hold only the URL + tag; employees: name, optional e-mail and department | `TagParserTest`, label content (ADR 003) |
