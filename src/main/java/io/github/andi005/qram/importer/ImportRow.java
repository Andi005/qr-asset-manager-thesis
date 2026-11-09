package io.github.andi005.qram.importer;

import io.github.andi005.qram.asset.AssetCategory;
import io.github.andi005.qram.asset.AssetStatus;
import java.time.LocalDate;

/** One valid row of an import file, ready to become an asset. A null tag gets generated. */
record ImportRow(
    int line,
    String tag,
    AssetCategory category,
    String manufacturer,
    String model,
    String serialNumber,
    LocalDate purchaseDate,
    LocalDate warrantyEndDate,
    String location,
    AssetStatus status,
    String notes) {}
