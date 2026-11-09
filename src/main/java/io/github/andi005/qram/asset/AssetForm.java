package io.github.andi005.qram.asset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * What the asset form sends. The form binds to this record, never to the entity, so nobody can post
 * fields they shouldn't set (the tag, timestamps). {@code version} travels in a hidden field to
 * detect concurrent edits.
 */
public record AssetForm(
    @NotNull AssetCategory category,
    @Size(max = 80) String manufacturer,
    @NotBlank @Size(max = 120) String model,
    @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9._/-]*") String serialNumber,
    LocalDate purchaseDate,
    LocalDate warrantyEndDate,
    Long locationId,
    @NotNull AssetStatus status,
    @Size(max = 2000) String notes,
    Integer version) {

  public static AssetForm empty() {
    return new AssetForm(null, "", "", "", null, null, null, AssetStatus.IN_STOCK, "", null);
  }

  public static AssetForm from(Asset asset) {
    return new AssetForm(
        asset.getCategory(),
        asset.getManufacturer(),
        asset.getModel(),
        asset.getSerialNumber(),
        asset.getPurchaseDate(),
        asset.getWarrantyEndDate(),
        asset.getLocation() == null ? null : asset.getLocation().getId(),
        asset.getStatus(),
        asset.getNotes(),
        asset.getVersion());
  }
}
