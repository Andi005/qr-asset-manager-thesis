package io.github.andi005.qram.inventory;

/** What one scan during an inventory session turned out to be. */
public enum ScanResult {
  /** On the expected list, scanned for the first time. */
  FOUND,
  /** A known asset that is not on the expected list (it's here, but it shouldn't be). */
  UNEXPECTED,
  /** Not a valid tag, or a tag the system doesn't know. */
  UNKNOWN,
  /** An asset already scanned in this session. */
  DUPLICATE
}
