package io.github.andi005.qram.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CSV exports can't smuggle formulas into Excel. */
class CsvSafeTest {

  @Test
  @DisplayName("Cells starting with = + - @ get a leading apostrophe")
  void neutralisesFormulas() {
    assertThat(CsvSafe.cell("=HYPERLINK(\"x\")")).isEqualTo("'=HYPERLINK(\"x\")");
    assertThat(CsvSafe.cell("+1+1")).isEqualTo("'+1+1");
    assertThat(CsvSafe.cell("-2")).isEqualTo("'-2");
    assertThat(CsvSafe.cell("@SUM(A1)")).isEqualTo("'@SUM(A1)");
  }

  @Test
  @DisplayName("Normal text is left as it is")
  void normalText() {
    assertThat(CsvSafe.cell("IT-000001")).isEqualTo("IT-000001");
    assertThat(CsvSafe.cell("Ndërtesa A")).isEqualTo("Ndërtesa A");
    assertThat(CsvSafe.cell(42)).isEqualTo("42");
  }

  @Test
  @DisplayName("Empty and missing values become empty cells")
  void emptyAndNull() {
    assertThat(CsvSafe.cell("")).isEmpty();
    assertThat(CsvSafe.cell(null)).isEmpty();
  }
}
