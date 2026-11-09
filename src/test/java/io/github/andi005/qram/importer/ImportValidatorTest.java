package io.github.andi005.qram.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.github.andi005.qram.asset.AssetCategory;
import io.github.andi005.qram.asset.AssetStatus;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CSV parsing and row validation (FR-13), first of all against the fictional mock file whose
 * "Shënime" column names the deliberate mistake in each bad row.
 */
class ImportValidatorTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 11, 9);
  private static final ImportValidator.Existing EMPTY =
      new ImportValidator.Existing(Set.of(), Set.of(), List.of());

  private static ImportValidator.Result validate(String csv, ImportValidator.Existing existing) {
    CsvAssetParser.Parsed parsed = CsvAssetParser.parse(csv.getBytes(StandardCharsets.UTF_8));
    assertThat(parsed.fileErrors()).isEmpty();
    return ImportValidator.validate(parsed.rows(), existing, TODAY);
  }

  @Test
  @DisplayName("The mock file: every deliberate mistake is reported on its own line")
  void mockFileErrors() throws IOException {
    CsvAssetParser.Parsed parsed = CsvAssetParser.parse(resource("/import/MOCK-inventory.csv"));
    assertThat(parsed.fileErrors()).isEmpty();
    assertThat(parsed.rows()).hasSize(45); // the trailing ";;;;" row is skipped

    ImportValidator.Result result = ImportValidator.validate(parsed.rows(), EMPTY, TODAY);

    assertThat(result.errors())
        .extracting(RowProblem::line, RowProblem::messageKey)
        .containsExactlyInAnyOrder(
            tuple(11, "import.error.tagDuplicate"), // IT-000003 twice
            tuple(15, "import.error.date"), // 31.02.2024
            tuple(19, "import.error.category"), // Tablet
            tuple(23, "import.error.required"), // empty model
            tuple(27, "import.error.tagFormat"), // it-26
            tuple(32, "import.error.serialDuplicate"), // AP35529407 twice
            tuple(35, "import.error.date"), // 2026-13-01
            tuple(39, "import.error.locationSpelling"), // "Ndertesa" without ë
            tuple(43, "import.error.status")); // "I shkatërruar"
    assertThat(result.warnings())
        .extracting(RowProblem::line, RowProblem::messageKey)
        .containsExactly(tuple(6, "import.warning.noSerial"));
  }

  @Test
  @DisplayName("Albanian Excel format: ; separator, BOM, dd.MM.yyyy, ë in values")
  void albanianExcelFormat() {
    String csv =
        "﻿Etiketa;Kategoria;Modeli;Data e blerjes;Lokacioni;Statusi\r\n"
            + "IT-000101;Projektor;EB-W51;05.03.2024;Ndërtesa B / Biblioteka;Në riparim\r\n";
    var result = validate(csv, EMPTY);
    assertThat(result.errors()).isEmpty();
    ImportRow row = result.rows().getFirst();
    assertThat(row.category()).isEqualTo(AssetCategory.PROJECTOR);
    assertThat(row.purchaseDate()).isEqualTo(LocalDate.of(2024, 3, 5));
    assertThat(row.location()).isEqualTo("Ndërtesa B / Biblioteka");
    assertThat(row.status()).isEqualTo(AssetStatus.IN_REPAIR);
  }

  @Test
  @DisplayName("English headers, comma separator, reordered columns, ISO dates")
  void englishCommaReordered() {
    String csv = "Model,Status,Category,Serial Number\nLatitude,retired,laptop,ab-1\n";
    var result = validate(csv, EMPTY);
    assertThat(result.errors()).isEmpty();
    ImportRow row = result.rows().getFirst();
    assertThat(row.status()).isEqualTo(AssetStatus.RETIRED);
    assertThat(row.serialNumber()).isEqualTo("AB-1");
    assertThat(row.tag()).isNull();
  }

  @Test
  @DisplayName("Tags and serials already in the database are refused")
  void existingInDatabase() {
    String csv =
        "Etiketa;Kategoria;Modeli;Nr. serik\nIT-000001;Laptop;X;S1\nIT-000002;Laptop;Y;S2\n";
    var existing = new ImportValidator.Existing(Set.of("IT-000001"), Set.of("S2"), List.of());
    var result = validate(csv, existing);
    assertThat(result.errors())
        .extracting(RowProblem::line, RowProblem::messageKey)
        .containsExactly(tuple(2, "import.error.tagExists"), tuple(3, "import.error.serialExists"));
  }

  @Test
  @DisplayName("A location typed without ë is refused when the database has it with ë")
  void locationSpellingAgainstDatabase() {
    String csv = "Kategoria;Modeli;Lokacioni\nLaptop;X;ndertesa a / salla 101\n";
    var existing =
        new ImportValidator.Existing(Set.of(), Set.of(), List.of("Ndërtesa A / Salla 101"));
    assertThat(validate(csv, existing).errors())
        .extracting(RowProblem::messageKey)
        .containsExactly("import.error.locationSpelling");
  }

  @Test
  @DisplayName("Date rules: no future purchase, warranty not before purchase")
  void dateRules() {
    String csv =
        "Kategoria;Modeli;Data e blerjes;Garancia deri\n"
            + "Laptop;A;10.11.2026;\n"
            + "Laptop;B;01.06.2025;01.05.2025\n";
    assertThat(validate(csv, EMPTY).errors())
        .extracting(RowProblem::line, RowProblem::messageKey)
        .containsExactly(
            tuple(2, "import.error.purchaseInFuture"),
            tuple(3, "import.error.warrantyBeforePurchase"));
  }

  @Test
  @DisplayName("A file without the required columns is refused as a whole")
  void missingColumns() {
    var parsed = CsvAssetParser.parse("Etiketa;Prodhuesi\nIT-000001;HP\n".getBytes());
    assertThat(parsed.fileErrors())
        .extracting(RowProblem::column)
        .containsExactlyInAnyOrder(Column.CATEGORY, Column.MODEL);
  }

  @Test
  @DisplayName("Empty files and header-only files are refused")
  void emptyFiles() {
    assertThat(CsvAssetParser.parse(new byte[0]).fileErrors())
        .extracting(RowProblem::messageKey)
        .containsExactly("import.error.emptyFile");
    assertThat(CsvAssetParser.parse("Kategoria;Modeli\n;\n".getBytes()).fileErrors())
        .extracting(RowProblem::messageKey)
        .containsExactly("import.error.noRows");
  }

  @Test
  @DisplayName("An old Windows-1252 file is still read, with a warning")
  void windows1252() {
    byte[] bytes =
        "Kategoria;Modeli;Lokacioni\nLaptop;X;Ndërtesa C\n"
            .getBytes(Charset.forName("windows-1252"));
    var parsed = CsvAssetParser.parse(bytes);
    assertThat(parsed.warnings())
        .extracting(RowProblem::messageKey)
        .containsExactly("import.warning.encoding");
    assertThat(parsed.rows().getFirst().get(Column.LOCATION)).isEqualTo("Ndërtesa C");
  }

  @Test
  @DisplayName("Both date formats are accepted, impossible dates are not")
  void dates() {
    assertThat(ImportValidator.parseDate("29.02.2024")).contains(LocalDate.of(2024, 2, 29));
    assertThat(ImportValidator.parseDate("2024-02-29")).contains(LocalDate.of(2024, 2, 29));
    assertThat(ImportValidator.parseDate("29.02.2023")).isEmpty();
    assertThat(ImportValidator.parseDate("31.04.2024")).isEmpty();
    assertThat(ImportValidator.parseDate("1.2.2024")).isEmpty();
  }

  private static byte[] resource(String path) throws IOException {
    try (InputStream in = ImportValidatorTest.class.getResourceAsStream(path)) {
      return in.readAllBytes();
    }
  }
}
