package io.github.andi005.qram.inventory;

import io.github.andi005.qram.asset.Location;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * One timed physical inventory: a list of expected assets, then scans until it is finished. The
 * duration and the found/missing/unexpected lists are the measurements for research question P2.
 */
@Entity
@Table(name = "inventory_session")
public class InventorySession {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String name;

  @ManyToOne
  @JoinColumn(name = "location_id")
  private Location location;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "started_by", nullable = false, length = 60)
  private String startedBy;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "inventory_expected", joinColumns = @JoinColumn(name = "session_id"))
  @Column(name = "asset_id", nullable = false)
  private Set<Long> expectedAssetIds = new HashSet<>();

  protected InventorySession() {}

  public InventorySession(
      String name, Location location, Set<Long> expectedAssetIds, Instant startedAt, String by) {
    this.name = name;
    this.location = location;
    this.expectedAssetIds = new HashSet<>(expectedAssetIds);
    this.startedAt = startedAt;
    this.startedBy = by;
  }

  public boolean isFinished() {
    return finishedAt != null;
  }

  public void finish(Instant at) {
    this.finishedAt = at;
  }

  /** From start to finish, or to {@code now} while still running. */
  public Duration duration(Instant now) {
    return Duration.between(startedAt, finishedAt != null ? finishedAt : now);
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Location getLocation() {
    return location;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public String getStartedBy() {
    return startedBy;
  }

  public Instant getFinishedAt() {
    return finishedAt;
  }

  public Set<Long> getExpectedAssetIds() {
    return Set.copyOf(expectedAssetIds);
  }
}
