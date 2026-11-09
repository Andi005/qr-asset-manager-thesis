package io.github.andi005.qram.inventory;

import io.github.andi005.qram.asset.Asset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One scan (or one hand-typed entry) during an inventory session, with its exact time. The raw
 * value is kept as received, so the evaluation can check it later.
 */
@Entity
@Table(name = "inventory_scan")
public class InventoryScan {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "session_id", nullable = false)
  private InventorySession session;

  @Column(name = "scanned_at", nullable = false)
  private Instant scannedAt;

  @Column(name = "raw_value", nullable = false, length = 200)
  private String rawValue;

  @ManyToOne
  @JoinColumn(name = "asset_id")
  private Asset asset;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ScanResult result;

  /** True when the value was typed by hand (no readable label): counts as manual typing. */
  @Column(nullable = false)
  private boolean manual;

  protected InventoryScan() {}

  public InventoryScan(
      InventorySession session,
      Instant scannedAt,
      String rawValue,
      Asset asset,
      ScanResult result,
      boolean manual) {
    this.session = session;
    this.scannedAt = scannedAt;
    this.rawValue = rawValue;
    this.asset = asset;
    this.result = result;
    this.manual = manual;
  }

  public Long getId() {
    return id;
  }

  public Instant getScannedAt() {
    return scannedAt;
  }

  public String getRawValue() {
    return rawValue;
  }

  public Asset getAsset() {
    return asset;
  }

  public ScanResult getResult() {
    return result;
  }

  public boolean isManual() {
    return manual;
  }
}
