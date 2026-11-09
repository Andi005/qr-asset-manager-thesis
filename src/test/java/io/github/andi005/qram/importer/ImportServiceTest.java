package io.github.andi005.qram.importer;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andi005.qram.IntegrationTest;
import io.github.andi005.qram.TestData;
import io.github.andi005.qram.asset.Asset;
import io.github.andi005.qram.asset.AssetRepository;
import io.github.andi005.qram.asset.AssetStatus;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/** The CSV import against the real database: all or nothing, and tags stay unique afterwards. */
@IntegrationTest
@Transactional
class ImportServiceTest {

  @Autowired private ImportService importService;
  @Autowired private AssetRepository assets;
  @Autowired private TestData testData;

  @BeforeEach
  void login() {
    TestData.loginAsAdmin();
  }

  @AfterEach
  void logout() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("The mock file with deliberate errors imports nothing and reports every error")
  void mockFileIsRefusedCompletely() throws IOException {
    long before = assets.count();
    ImportReport report = importService.importCsv("MOCK-inventory.csv", mockFile());

    assertThat(report.success()).isFalse();
    assertThat(report.imported()).isZero();
    assertThat(assets.count()).isEqualTo(before);
    assertThat(report.errors()).isNotEmpty();
  }

  @Test
  @DisplayName("A valid file is imported in one go, creating new locations")
  void validFileIsImported() {
    String csv =
        """
        Etiketa;Kategoria;Prodhuesi;Modeli;Nr. serik;Data e blerjes;Garancia deri;Lokacioni;Statusi
        IT-900001;Laptop;Dell;Latitude 5440;IMP-0001;01.02.2024;01.02.2027;Import / Salla 1;Në përdorim
        IT-900002;Monitor;LG;24MP400;IMP-0002;2024-03-05;;Import / Salla 1;Në riparim
        ;Printer;HP;LaserJet;IMP-0003;;;Import / Salla 2;
        """;
    ImportReport report = importService.importCsv("ok.csv", csv.getBytes(StandardCharsets.UTF_8));

    assertThat(report.errors()).isEmpty();
    assertThat(report.imported()).isEqualTo(3);
    assertThat(report.locationsCreated()).isEqualTo(2);
    Asset monitor = assets.findByTag("IT-900002").orElseThrow();
    assertThat(monitor.getStatus()).isEqualTo(AssetStatus.IN_REPAIR);
    assertThat(monitor.getLocationName()).isEqualTo("Import / Salla 1");
    // The row without a tag got a generated one, after the highest tag imported.
    String generated =
        assets.findBySerialNumberIn(java.util.List.of("IMP-0003")).getFirst().getTag();
    assertThat(number(generated)).isGreaterThan(900_002);
    // And new assets continue after it, without collisions.
    assertThat(number(testData.asset().getTag())).isEqualTo(number(generated) + 1);
  }

  private static int number(String tag) {
    return Integer.parseInt(tag.substring(3));
  }

  @Test
  @DisplayName("Importing the same file twice is refused: the tags exist now")
  void sameFileTwice() {
    String csv = "Kategoria;Modeli;Etiketa\nLaptop;X1;IT-910001\n";
    assertThat(importService.importCsv("a.csv", csv.getBytes(StandardCharsets.UTF_8)).success())
        .isTrue();
    ImportReport second = importService.importCsv("a.csv", csv.getBytes(StandardCharsets.UTF_8));
    assertThat(second.success()).isFalse();
    assertThat(second.errors())
        .extracting(RowProblem::messageKey)
        .containsExactly("import.error.tagExists");
  }

  private static byte[] mockFile() throws IOException {
    try (InputStream in =
        ImportServiceTest.class.getResourceAsStream("/import/MOCK-inventory.csv")) {
      return in.readAllBytes();
    }
  }
}
