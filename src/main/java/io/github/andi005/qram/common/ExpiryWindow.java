package io.github.andi005.qram.common;

import java.time.LocalDate;

/**
 * The one place that decides whether a date is "expiring soon" (FR-11).
 *
 * <p>The rule: <b>today ≤ date ≤ today + 90</b>, both ends inclusive. A date before today is
 * already expired; no date at all means it never expires (a perpetual license, or no warranty
 * recorded).
 */
public final class ExpiryWindow {

  public static final int DAYS = 90;

  private ExpiryWindow() {}

  public static ExpiryState classify(LocalDate today, LocalDate date) {
    if (date == null) {
      return ExpiryState.NONE;
    }
    if (date.isBefore(today)) {
      return ExpiryState.EXPIRED;
    }
    if (!date.isAfter(lastDay(today))) {
      return ExpiryState.SOON;
    }
    return ExpiryState.LATER;
  }

  /** The last day that still counts as "expiring soon". */
  public static LocalDate lastDay(LocalDate today) {
    return today.plusDays(DAYS);
  }

  /** Where a date stands relative to today. */
  public enum ExpiryState {
    /** Before today. */
    EXPIRED,
    /** From today to today + 90, inclusive. */
    SOON,
    /** After today + 90. */
    LATER,
    /** No date: never expires. */
    NONE
  }
}
