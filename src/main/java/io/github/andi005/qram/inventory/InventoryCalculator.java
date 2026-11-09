package io.github.andi005.qram.inventory;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * The pure logic of an inventory session, without a database: which assets were found, which are
 * missing, and which turned up unexpectedly. Kept free of Spring so it is easy to test.
 */
public final class InventoryCalculator {

  private InventoryCalculator() {}

  /**
   * Classifies one new scan.
   *
   * @param expected ids of the assets that should be found
   * @param alreadyScanned ids of the assets scanned earlier in this session
   * @param assetId the scanned asset's id, or null when the scan did not match a known asset
   */
  public static ScanResult classify(
      Set<Long> expected, Collection<Long> alreadyScanned, Long assetId) {
    if (assetId == null) {
      return ScanResult.UNKNOWN;
    }
    if (alreadyScanned.contains(assetId)) {
      return ScanResult.DUPLICATE;
    }
    return expected.contains(assetId) ? ScanResult.FOUND : ScanResult.UNEXPECTED;
  }

  /**
   * The result of a whole session.
   *
   * @param expected ids of the assets that should be found
   * @param scannedAssetIds the asset id of each scan in the order scanned (null = unknown scan)
   */
  public static Outcome outcome(Set<Long> expected, List<Long> scannedAssetIds) {
    Set<Long> seen = new LinkedHashSet<>();
    int unknown = 0;
    int duplicates = 0;
    for (Long id : scannedAssetIds) {
      if (id == null) {
        unknown++;
      } else if (!seen.add(id)) {
        duplicates++;
      }
    }
    Set<Long> found = new LinkedHashSet<>();
    Set<Long> unexpected = new LinkedHashSet<>();
    for (Long id : seen) {
      (expected.contains(id) ? found : unexpected).add(id);
    }
    Set<Long> missing = new TreeSet<>(expected);
    missing.removeAll(seen);
    return new Outcome(found, missing, unexpected, unknown, duplicates);
  }

  /**
   * @param found expected assets that were scanned, in scan order
   * @param missing expected assets that were not scanned
   * @param unexpected known assets that were scanned but not expected, in scan order
   * @param unknownScans scans that matched no asset
   * @param duplicateScans repeated scans of an already scanned asset
   */
  public record Outcome(
      Set<Long> found,
      Set<Long> missing,
      Set<Long> unexpected,
      int unknownScans,
      int duplicateScans) {}
}
