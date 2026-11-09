package io.github.andi005.qram.inventory;

import io.github.andi005.qram.asset.Asset;
import io.github.andi005.qram.asset.AssetService;
import io.github.andi005.qram.asset.Location;
import io.github.andi005.qram.asset.LocationService;
import io.github.andi005.qram.common.BusinessRuleException;
import io.github.andi005.qram.common.NotFoundException;
import io.github.andi005.qram.qr.TagParser;
import io.github.andi005.qram.security.CurrentUser;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Starting, scanning into, finishing and reporting inventory sessions. */
@Service
public class InventoryService {

  private final InventorySessionRepository sessions;
  private final InventoryScanRepository scans;
  private final AssetService assetService;
  private final LocationService locationService;
  private final CurrentUser currentUser;
  private final Clock clock;

  public InventoryService(
      InventorySessionRepository sessions,
      InventoryScanRepository scans,
      AssetService assetService,
      LocationService locationService,
      CurrentUser currentUser,
      Clock clock) {
    this.sessions = sessions;
    this.scans = scans;
    this.assetService = assetService;
    this.locationService = locationService;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<InventorySession> findAll() {
    return sessions.findAllByOrderByStartedAtDesc();
  }

  @Transactional(readOnly = true)
  public InventorySession get(long id) {
    return sessions.findById(id).orElseThrow(() -> new NotFoundException("Inventory " + id));
  }

  /**
   * Starts a session. The expected list is either the tags typed or pasted into {@code tagList}
   * (separated by spaces, commas or new lines), or every asset in use at {@code locationId}.
   */
  @PreAuthorize("hasRole('IT_STAFF')")
  @Transactional
  public InventorySession start(String name, Long locationId, String tagList) {
    if (name == null || name.isBlank() || name.strip().length() > 120) {
      throw BusinessRuleException.onField("name", "inventory.error.name");
    }
    Location location = locationId == null ? null : locationService.get(locationId);
    Set<Long> expected;
    if (tagList != null && !tagList.isBlank()) {
      expected = idsForTags(tagList);
    } else if (location != null) {
      expected =
          assetService.findInUseAtLocation(location.getId()).stream()
              .map(Asset::getId)
              .collect(Collectors.toSet());
    } else {
      throw new BusinessRuleException("inventory.error.chooseExpected");
    }
    if (expected.isEmpty()) {
      throw new BusinessRuleException("inventory.error.emptyExpected");
    }
    return sessions.save(
        new InventorySession(
            name.strip(), location, expected, clock.instant(), currentUser.username()));
  }

  /** Records one scan. The raw value is parsed on the server; nothing from the phone is trusted. */
  @PreAuthorize("hasRole('IT_STAFF')")
  @Transactional
  public InventoryScan recordScan(long sessionId, String raw, boolean manual) {
    InventorySession session =
        sessions
            .findForUpdate(sessionId)
            .orElseThrow(() -> new NotFoundException("Inventory " + sessionId));
    if (session.isFinished()) {
      throw new SessionFinishedException(sessionId);
    }
    String value = raw == null ? "" : raw.strip();
    if (value.isEmpty()) {
      throw new BusinessRuleException("inventory.error.emptyScan");
    }
    if (value.length() > TagParser.MAX_LENGTH) {
      value = value.substring(0, TagParser.MAX_LENGTH);
    }
    Asset asset = TagParser.parse(value).flatMap(assetService::findByTag).orElse(null);
    ScanResult result =
        InventoryCalculator.classify(
            session.getExpectedAssetIds(),
            scans.scannedAssetIds(sessionId),
            asset == null ? null : asset.getId());
    return scans.save(new InventoryScan(session, clock.instant(), value, asset, result, manual));
  }

  @PreAuthorize("hasRole('IT_STAFF')")
  @Transactional
  public void finish(long sessionId) {
    InventorySession session =
        sessions
            .findForUpdate(sessionId)
            .orElseThrow(() -> new NotFoundException("Inventory " + sessionId));
    if (!session.isFinished()) {
      session.finish(clock.instant());
    }
  }

  @Transactional(readOnly = true)
  public InventoryReport report(long sessionId) {
    InventorySession session = get(sessionId);
    List<InventoryScan> all = scans.findBySessionIdOrderByScannedAtAscIdAsc(sessionId);
    List<Long> scannedIds = new ArrayList<>();
    Set<Long> involved = new LinkedHashSet<>(session.getExpectedAssetIds());
    int manualEntries = 0;
    int typed = 0;
    for (InventoryScan scan : all) {
      Long id = scan.getAsset() == null ? null : scan.getAsset().getId();
      scannedIds.add(id);
      if (id != null) {
        involved.add(id);
      }
      if (scan.isManual()) {
        manualEntries++;
        typed += scan.getRawValue().length();
      }
    }
    InventoryCalculator.Outcome outcome =
        InventoryCalculator.outcome(session.getExpectedAssetIds(), scannedIds);
    Map<Long, Asset> byId =
        assetService.findByIds(involved).stream()
            .collect(Collectors.toMap(Asset::getId, Function.identity()));
    return new InventoryReport(
        session,
        assets(session.getExpectedAssetIds().stream().sorted().toList(), byId),
        assets(outcome.found(), byId),
        assets(outcome.missing(), byId),
        assets(outcome.unexpected(), byId),
        all,
        outcome.unknownScans(),
        outcome.duplicateScans(),
        manualEntries,
        typed,
        session.duration(clock.instant()));
  }

  private Set<Long> idsForTags(String tagList) {
    Set<String> tags = new LinkedHashSet<>();
    for (String piece : tagList.split("[\\s,;]+")) {
      if (piece.isBlank()) {
        continue;
      }
      Optional<String> tag = TagParser.parse(piece);
      if (tag.isEmpty()) {
        throw BusinessRuleException.onField("tagList", "inventory.error.badTag", piece);
      }
      tags.add(tag.get());
    }
    List<Asset> found = assetService.findByTags(tags);
    if (found.size() != tags.size()) {
      Set<String> known = found.stream().map(Asset::getTag).collect(Collectors.toSet());
      String missing =
          tags.stream().filter(t -> !known.contains(t)).collect(Collectors.joining(", "));
      throw BusinessRuleException.onField("tagList", "inventory.error.unknownTags", missing);
    }
    return found.stream().map(Asset::getId).collect(Collectors.toSet());
  }

  private static List<Asset> assets(Collection<Long> ids, Map<Long, Asset> byId) {
    return ids.stream().map(byId::get).filter(a -> a != null).toList();
  }
}
