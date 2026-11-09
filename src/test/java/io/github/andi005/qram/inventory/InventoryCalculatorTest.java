package io.github.andi005.qram.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Found, missing and unexpected assets of an inventory session (FR-16). */
class InventoryCalculatorTest {

  private static final Set<Long> EXPECTED = Set.of(1L, 2L, 3L);

  @Test
  @DisplayName("Nothing scanned: everything expected is missing")
  void nothingScanned() {
    var outcome = InventoryCalculator.outcome(EXPECTED, List.of());
    assertThat(outcome.found()).isEmpty();
    assertThat(outcome.missing()).containsExactly(1L, 2L, 3L);
    assertThat(outcome.unexpected()).isEmpty();
  }

  @Test
  @DisplayName("All expected scanned: nothing missing, found in scan order")
  void allFound() {
    var outcome = InventoryCalculator.outcome(EXPECTED, List.of(3L, 1L, 2L));
    assertThat(outcome.found()).containsExactly(3L, 1L, 2L);
    assertThat(outcome.missing()).isEmpty();
  }

  @Test
  @DisplayName("A mix: found, missing, unexpected, unknown and duplicate scans")
  void mix() {
    var outcome = InventoryCalculator.outcome(EXPECTED, Arrays.asList(1L, 9L, null, 1L, 9L, 3L));
    assertThat(outcome.found()).containsExactly(1L, 3L);
    assertThat(outcome.missing()).containsExactly(2L);
    assertThat(outcome.unexpected()).containsExactly(9L);
    assertThat(outcome.unknownScans()).isEqualTo(1);
    assertThat(outcome.duplicateScans()).isEqualTo(2);
  }

  @Test
  @DisplayName("Each new scan is classified against the expected list and earlier scans")
  void classify() {
    assertThat(InventoryCalculator.classify(EXPECTED, List.of(), 1L)).isEqualTo(ScanResult.FOUND);
    assertThat(InventoryCalculator.classify(EXPECTED, List.of(1L), 1L))
        .isEqualTo(ScanResult.DUPLICATE);
    assertThat(InventoryCalculator.classify(EXPECTED, List.of(), 9L))
        .isEqualTo(ScanResult.UNEXPECTED);
    assertThat(InventoryCalculator.classify(EXPECTED, List.of(9L), 9L))
        .isEqualTo(ScanResult.DUPLICATE);
    assertThat(InventoryCalculator.classify(EXPECTED, List.of(), null))
        .isEqualTo(ScanResult.UNKNOWN);
  }

  @Test
  @DisplayName("classify and outcome agree on the same scans")
  void classifyMatchesOutcome() {
    List<Long> scans = Arrays.asList(2L, 7L, 2L, null, 3L);
    int found = 0;
    for (int i = 0; i < scans.size(); i++) {
      if (InventoryCalculator.classify(EXPECTED, scans.subList(0, i), scans.get(i))
          == ScanResult.FOUND) {
        found++;
      }
    }
    assertThat(InventoryCalculator.outcome(EXPECTED, scans).found()).hasSize(found);
  }
}
