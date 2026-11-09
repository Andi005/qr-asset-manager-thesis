package io.github.andi005.qram.importer;

import io.github.andi005.qram.asset.Asset;
import io.github.andi005.qram.asset.AssetForm;
import io.github.andi005.qram.asset.AssetService;
import io.github.andi005.qram.asset.Location;
import io.github.andi005.qram.asset.LocationService;
import io.github.andi005.qram.history.ChangeLogService;
import io.github.andi005.qram.history.EntityType;
import io.github.andi005.qram.security.CurrentUser;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Imports existing asset records from a CSV file (FR-13). <b>All or nothing:</b> when any row has
 * an error, nothing is written and the report lists every problem with its line number. When the
 * whole file is valid, every row is written in one transaction.
 */
@Service
public class ImportService {

  private final AssetService assetService;
  private final LocationService locationService;
  private final ImportBatchRepository batches;
  private final ChangeLogService changeLog;
  private final CurrentUser currentUser;
  private final Clock clock;

  public ImportService(
      AssetService assetService,
      LocationService locationService,
      ImportBatchRepository batches,
      ChangeLogService changeLog,
      CurrentUser currentUser,
      Clock clock) {
    this.assetService = assetService;
    this.locationService = locationService;
    this.batches = batches;
    this.changeLog = changeLog;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Transactional(readOnly = true)
  public List<ImportBatch> recentBatches() {
    return batches.findTop10ByOrderByImportedAtDesc();
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public ImportReport importCsv(String fileName, byte[] content) {
    String name = fileName == null || fileName.isBlank() ? "import.csv" : fileName.strip();
    if (name.length() > 200) {
      name = name.substring(0, 200);
    }
    CsvAssetParser.Parsed parsed = CsvAssetParser.parse(content);
    if (!parsed.fileErrors().isEmpty()) {
      return new ImportReport(name, 0, parsed.fileErrors(), parsed.warnings(), 0, 0);
    }
    List<CsvAssetParser.Row> rows = parsed.rows();

    ImportValidator.Result result =
        ImportValidator.validate(rows, existingData(rows), LocalDate.now(clock));
    List<RowProblem> warnings = new java.util.ArrayList<>(parsed.warnings());
    warnings.addAll(result.warnings());
    if (!result.errors().isEmpty()) {
      return new ImportReport(name, rows.size(), result.errors(), warnings, 0, 0);
    }

    Map<String, Location> locations = new HashMap<>();
    Set<String> existingLocationNames =
        locationService.findAll().stream().map(Location::getName).collect(Collectors.toSet());
    // Rows that carry their own tag first; then move the tag sequence past them, so the rows
    // without a tag get new numbers that can't collide with the imported ones.
    for (ImportRow row : result.rows()) {
      if (row.tag() != null) {
        createAsset(row, locations);
      }
    }
    assetService.syncTagSequence();
    for (ImportRow row : result.rows()) {
      if (row.tag() == null) {
        createAsset(row, locations);
      }
    }

    int locationsCreated =
        (int) locations.keySet().stream().filter(n -> !existingLocationNames.contains(n)).count();
    ImportBatch batch =
        batches.save(
            new ImportBatch(
                name,
                clock.instant(),
                currentUser.username(),
                result.rows().size(),
                locationsCreated));
    changeLog.record(
        EntityType.IMPORT,
        batch.getId(),
        "IMPORTED",
        name + ": " + result.rows().size() + " / " + locationsCreated);
    return new ImportReport(
        name, rows.size(), List.of(), warnings, result.rows().size(), locationsCreated);
  }

  private void createAsset(ImportRow row, Map<String, Location> locations) {
    Location location =
        row.location() == null
            ? null
            : locations.computeIfAbsent(row.location(), locationService::getOrCreate);
    AssetForm form =
        new AssetForm(
            row.category(),
            row.manufacturer(),
            row.model(),
            row.serialNumber(),
            row.purchaseDate(),
            row.warrantyEndDate(),
            location == null ? null : location.getId(),
            row.status(),
            row.notes(),
            null);
    assetService.createWithTag(row.tag(), form);
  }

  private ImportValidator.Existing existingData(List<CsvAssetParser.Row> rows) {
    Set<String> fileTags =
        rows.stream()
            .map(r -> r.get(Column.TAG).toUpperCase(Locale.ROOT))
            .filter(t -> !t.isEmpty())
            .collect(Collectors.toSet());
    Set<String> fileSerials =
        rows.stream()
            .map(r -> r.get(Column.SERIAL).toUpperCase(Locale.ROOT))
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
    Set<String> tags =
        assetService.findByTags(fileTags).stream().map(Asset::getTag).collect(Collectors.toSet());
    Set<String> serials =
        assetService.findBySerialNumbers(fileSerials).stream()
            .map(Asset::getSerialNumber)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    List<String> locations = locationService.findAll().stream().map(Location::getName).toList();
    return new ImportValidator.Existing(tags, serials, locations);
  }
}
