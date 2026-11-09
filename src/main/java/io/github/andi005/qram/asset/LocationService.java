package io.github.andi005.qram.asset;

import io.github.andi005.qram.common.BusinessRuleException;
import io.github.andi005.qram.common.NotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Rooms and places. Names are unique (ignoring upper/lower case). */
@Service
public class LocationService {

  private final LocationRepository locations;
  private final AssetRepository assets;
  private final Clock clock;

  public LocationService(LocationRepository locations, AssetRepository assets, Clock clock) {
    this.locations = locations;
    this.assets = assets;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<Location> findAll() {
    return locations.findAllByOrderByNameAsc();
  }

  @Transactional(readOnly = true)
  public Location get(long id) {
    return locations.findById(id).orElseThrow(() -> new NotFoundException("Location " + id));
  }

  /** How many assets are in each location (location id → count). */
  @Transactional(readOnly = true)
  public Map<Long, Long> assetCounts() {
    return assets.countPerLocation().stream()
        .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
  }

  @PreAuthorize("hasRole('IT_STAFF')")
  @Transactional
  public Location create(String name) {
    String clean = name == null ? "" : name.strip().replaceAll("\\s+", " ");
    if (clean.isEmpty() || clean.length() > 120) {
      throw BusinessRuleException.onField("name", "location.error.name");
    }
    if (locations.existsByNameIgnoreCase(clean)) {
      throw BusinessRuleException.onField("name", "location.error.duplicate", clean);
    }
    return locations.save(new Location(clean, clock.instant()));
  }

  /** For the CSV import: the location with exactly this name, created if it doesn't exist yet. */
  @PreAuthorize("hasRole('IT_STAFF')")
  @Transactional
  public Location getOrCreate(String name) {
    return locations
        .findByName(name)
        .orElseGet(() -> locations.save(new Location(name, clock.instant())));
  }
}
