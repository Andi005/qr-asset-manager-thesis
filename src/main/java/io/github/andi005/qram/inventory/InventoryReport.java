package io.github.andi005.qram.inventory;

import io.github.andi005.qram.asset.Asset;
import java.time.Duration;
import java.util.List;

/**
 * Everything the session page and the exports show about one inventory session.
 *
 * @param typedCharacters characters typed by hand (manual entries), the "manual typing" measure
 */
public record InventoryReport(
    InventorySession session,
    List<Asset> expected,
    List<Asset> found,
    List<Asset> missing,
    List<Asset> unexpected,
    List<InventoryScan> scans,
    int unknownScans,
    int duplicateScans,
    int manualEntries,
    int typedCharacters,
    Duration duration) {}
