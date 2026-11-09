package io.github.andi005.qram.license;

import io.github.andi005.qram.common.ExpiryWindow.ExpiryState;

/** A license with its seats in use and its expiry state, for lists. */
public record LicenseSummary(SoftwareLicense license, long seatsUsed, ExpiryState expiry) {

  public long seatsFree() {
    return Math.max(license.getSeatsTotal() - seatsUsed, 0);
  }
}
