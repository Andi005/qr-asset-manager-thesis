package io.github.andi005.qram.asset;

/**
 * The physical state of an asset. "Assigned to someone" is deliberately not a status: it is derived
 * from the open assignment row, so the two can never disagree.
 */
public enum AssetStatus {
  /** Working and available (in storage or in use). */
  IN_STOCK,
  /** Out for repair: can't be assigned. */
  IN_REPAIR,
  /** Out of use for good. Kept in the records with its history, never deleted. */
  RETIRED;

  public boolean canBeAssigned() {
    return this == IN_STOCK;
  }
}
