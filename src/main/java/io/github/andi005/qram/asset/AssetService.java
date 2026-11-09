package io.github.andi005.qram.asset;

import io.github.andi005.qram.common.BusinessRuleException;
import io.github.andi005.qram.common.Labels;
import io.github.andi005.qram.common.NotFoundException;
import io.github.andi005.qram.history.ChangeLogService;
import io.github.andi005.qram.history.EntityType;
import io.github.andi005.qram.history.FieldChanges;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The business rules for assets: tag generation, unique serial numbers, warranty after purchase,
 * concurrent-edit detection, and "no retiring or repairing while someone holds it".
 */
@Service
public class AssetService {

  public static final int PAGE_SIZE = 50;

  private final AssetRepository assets;
  private final LocationRepository locations;
  private final ChangeLogService changeLog;
  private final Labels labels;
  private final Clock clock;

  public AssetService(
      AssetRepository assets,
      LocationRepository locations,
      ChangeLogService changeLog,
      Labels labels,
      Clock clock) {
    this.assets = assets;
    this.locations = locations;
    this.changeLog = changeLog;
    this.labels = labels;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public Page<Asset> search(String text, AssetStatus status, Long locationId, int page) {
    String pattern = "%" + (text == null ? "" : text.strip().toLowerCase(Locale.ROOT)) + "%";
    return assets.search(
        pattern, status, locationId, PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by("tag")));
  }

  @Transactional(readOnly = true)
  public Asset get(long id) {
    return assets.findById(id).orElseThrow(() -> new NotFoundException("Asset " + id));
  }

  @Transactional(readOnly = true)
  public Optional<Asset> findByTag(String tag) {
    return assets.findByTag(tag);
  }

  @Transactional(readOnly = true)
  public List<Asset> findByIds(Collection<Long> ids) {
    return ids.isEmpty() ? List.of() : assets.findByIdInOrderByTagAsc(ids);
  }

  @Transactional(readOnly = true)
  public List<Asset> findByTags(Collection<String> tags) {
    return tags.isEmpty() ? List.of() : assets.findByTagIn(tags);
  }

  @Transactional(readOnly = true)
  public List<Asset> findBySerialNumbers(Collection<String> serialNumbers) {
    return serialNumbers.isEmpty() ? List.of() : assets.findBySerialNumberIn(serialNumbers);
  }

  /** The assets that should be found in a location during an inventory (everything not retired). */
  @Transactional(readOnly = true)
  public List<Asset> findInUseAtLocation(long locationId) {
    return assets.findByLocationIdAndStatusNotOrderByTagAsc(locationId, AssetStatus.RETIRED);
  }

  @Transactional(readOnly = true)
  public Map<AssetStatus, Long> countByStatus() {
    Map<AssetStatus, Long> counts = new EnumMap<>(AssetStatus.class);
    for (AssetStatus status : AssetStatus.values()) {
      counts.put(status, assets.countByStatus(status));
    }
    return counts;
  }

  @Transactional(readOnly = true)
  public List<Asset> warrantiesEndingBetween(LocalDate from, LocalDate to) {
    return assets.warrantiesEndingBetween(from, to);
  }

  @Transactional(readOnly = true)
  public List<Asset> warrantiesEndedBefore(LocalDate day) {
    return assets.warrantiesEndedBefore(day);
  }

  @PreAuthorize("hasRole('IT_STAFF')")
  @Transactional
  public Asset create(AssetForm form) {
    return createWithTag(null, form);
  }

  /**
   * Creates an asset. With {@code tag == null} a new tag is generated; the CSV import passes the
   * tag that is already on the device.
   */
  @PreAuthorize("hasRole('IT_STAFF')")
  @Transactional
  public Asset createWithTag(String tag, AssetForm form) {
    String serial = normalizeSerial(form.serialNumber());
    checkDates(form);
    if (serial != null && assets.existsBySerialNumber(serial)) {
      throw BusinessRuleException.onField("serialNumber", "asset.error.serialTaken", serial);
    }
    Asset asset = new Asset(tag == null ? nextFreeTag() : tag, clock.instant());
    apply(asset, form, serial);
    asset = assets.save(asset);
    changeLog.record(EntityType.ASSET, asset.getId(), "CREATED", asset.getDisplayName());
    return asset;
  }

  @PreAuthorize("hasRole('IT_STAFF')")
  @Transactional
  public Asset update(long id, AssetForm form) {
    Asset asset = get(id);
    if (form.version() == null || !form.version().equals(asset.getVersion())) {
      throw new BusinessRuleException("asset.error.concurrentEdit");
    }
    String serial = normalizeSerial(form.serialNumber());
    checkDates(form);
    if (serial != null && assets.existsBySerialNumberAndIdNot(serial, id)) {
      throw BusinessRuleException.onField("serialNumber", "asset.error.serialTaken", serial);
    }
    if (form.status() != AssetStatus.IN_STOCK
        && asset.getStatus() != form.status()
        && assets.hasOpenAssignment(id)) {
      throw BusinessRuleException.onField("status", "asset.error.returnFirst");
    }

    Location newLocation = findLocation(form.locationId());
    FieldChanges changes =
        new FieldChanges()
            .add(
                label("category"),
                categoryLabel(asset.getCategory()),
                categoryLabel(form.category()))
            .add(label("manufacturer"), asset.getManufacturer(), blankToNull(form.manufacturer()))
            .add(label("model"), asset.getModel(), form.model().strip())
            .add(label("serialNumber"), asset.getSerialNumber(), serial)
            .add(label("purchaseDate"), asset.getPurchaseDate(), form.purchaseDate())
            .add(label("warrantyEndDate"), asset.getWarrantyEndDate(), form.warrantyEndDate())
            .add(
                label("location"),
                asset.getLocationName(),
                newLocation == null ? null : newLocation.getName())
            .add(label("status"), statusLabel(asset.getStatus()), statusLabel(form.status()))
            .add(label("notes"), asset.getNotes(), blankToNull(form.notes()));

    apply(asset, form, serial);
    if (!changes.isEmpty()) {
      changeLog.record(EntityType.ASSET, id, "UPDATED", changes.toString());
    }
    return asset;
  }

  /** After an import with explicit tags: generated tags continue after the highest one in use. */
  @PreAuthorize("hasRole('IT_STAFF')")
  @Transactional
  public void syncTagSequence() {
    assets.syncTagSequence();
  }

  private void apply(Asset asset, AssetForm form, String serial) {
    asset.setCategory(form.category());
    asset.setManufacturer(blankToNull(form.manufacturer()));
    asset.setModel(form.model().strip());
    asset.setSerialNumber(serial);
    asset.setPurchaseDate(form.purchaseDate());
    asset.setWarrantyEndDate(form.warrantyEndDate());
    asset.setLocation(findLocation(form.locationId()));
    asset.setStatus(form.status());
    asset.setNotes(blankToNull(form.notes()));
    asset.setUpdatedAt(clock.instant());
  }

  private void checkDates(AssetForm form) {
    LocalDate today = LocalDate.now(clock);
    if (form.purchaseDate() != null && form.purchaseDate().isAfter(today)) {
      throw BusinessRuleException.onField("purchaseDate", "asset.error.purchaseInFuture");
    }
    if (form.purchaseDate() != null
        && form.warrantyEndDate() != null
        && form.warrantyEndDate().isBefore(form.purchaseDate())) {
      throw BusinessRuleException.onField("warrantyEndDate", "asset.error.warrantyBeforePurchase");
    }
  }

  private Location findLocation(Long locationId) {
    if (locationId == null) {
      return null;
    }
    return locations
        .findById(locationId)
        .orElseThrow(() -> BusinessRuleException.onField("locationId", "asset.error.location"));
  }

  /** The next tag from the sequence, skipping any number already used (e.g. by an import). */
  private String nextFreeTag() {
    String tag;
    do {
      tag = formatTag(assets.nextTagNumber());
    } while (assets.existsByTag(tag));
    return tag;
  }

  static String formatTag(long number) {
    if (number < 1 || number > 999_999) {
      throw new IllegalStateException("Tag number out of range: " + number);
    }
    return "IT-%06d".formatted(number);
  }

  static String normalizeSerial(String serial) {
    if (serial == null || serial.isBlank()) {
      return null;
    }
    return serial.strip().toUpperCase(Locale.ROOT);
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text.strip();
  }

  private String label(String field) {
    return labels.get("asset.field." + field);
  }

  private String categoryLabel(AssetCategory category) {
    return category == null ? null : labels.get("asset.category." + category.name());
  }

  private String statusLabel(AssetStatus status) {
    return status == null ? null : labels.get("asset.status." + status.name());
  }
}
