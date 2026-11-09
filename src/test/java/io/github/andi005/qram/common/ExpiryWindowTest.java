package io.github.andi005.qram.common;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andi005.qram.common.ExpiryWindow.ExpiryState;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The 90-day window boundaries (FR-11): today and today+90 are in, today+91 is out. */
class ExpiryWindowTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 11, 9);

  @Test
  @DisplayName("Yesterday is already expired")
  void yesterday() {
    assertThat(ExpiryWindow.classify(TODAY, TODAY.minusDays(1))).isEqualTo(ExpiryState.EXPIRED);
  }

  @Test
  @DisplayName("Today counts as expiring soon (not expired yet)")
  void today() {
    assertThat(ExpiryWindow.classify(TODAY, TODAY)).isEqualTo(ExpiryState.SOON);
  }

  @Test
  @DisplayName("Day 90 is the last day inside the window")
  void day90() {
    assertThat(ExpiryWindow.classify(TODAY, TODAY.plusDays(90))).isEqualTo(ExpiryState.SOON);
  }

  @Test
  @DisplayName("Day 91 is outside the window")
  void day91() {
    assertThat(ExpiryWindow.classify(TODAY, TODAY.plusDays(91))).isEqualTo(ExpiryState.LATER);
  }

  @Test
  @DisplayName("No date means it never expires")
  void noDate() {
    assertThat(ExpiryWindow.classify(TODAY, null)).isEqualTo(ExpiryState.NONE);
  }

  @Test
  @DisplayName("The window crosses a year and a leap day correctly")
  void acrossYearAndLeapDay() {
    LocalDate dec = LocalDate.of(2027, 12, 31);
    assertThat(ExpiryWindow.lastDay(dec)).isEqualTo(LocalDate.of(2028, 3, 30));
    assertThat(ExpiryWindow.classify(dec, LocalDate.of(2028, 2, 29))).isEqualTo(ExpiryState.SOON);
  }
}
