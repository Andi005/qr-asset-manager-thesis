package io.github.andi005.qram.asset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One piece of hardware, identified by its tag (IT-000123). The tag is generated once and never
 * changes; it is what the QR label carries.
 */
@Entity
@Table(name = "asset")
public class Asset {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "asset_tag", nullable = false, length = 16, updatable = false)
  private String tag;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AssetCategory category;

  @Column(length = 80)
  private String manufacturer;

  @Column(nullable = false, length = 120)
  private String model;

  @Column(name = "serial_number", length = 64)
  private String serialNumber;

  @Column(name = "purchase_date")
  private LocalDate purchaseDate;

  @Column(name = "warranty_end_date")
  private LocalDate warrantyEndDate;

  @ManyToOne
  @JoinColumn(name = "location_id")
  private Location location;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AssetStatus status;

  @Column(length = 2000)
  private String notes;

  /** Optimistic locking: two people editing the same asset can't silently overwrite each other. */
  @Version
  @Column(nullable = false)
  private Integer version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Asset() {}

  public Asset(String tag, Instant createdAt) {
    this.tag = tag;
    this.createdAt = createdAt;
    this.updatedAt = createdAt;
    this.status = AssetStatus.IN_STOCK;
  }

  public Long getId() {
    return id;
  }

  public String getTag() {
    return tag;
  }

  public AssetCategory getCategory() {
    return category;
  }

  public void setCategory(AssetCategory category) {
    this.category = category;
  }

  public String getManufacturer() {
    return manufacturer;
  }

  public void setManufacturer(String manufacturer) {
    this.manufacturer = manufacturer;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public String getSerialNumber() {
    return serialNumber;
  }

  public void setSerialNumber(String serialNumber) {
    this.serialNumber = serialNumber;
  }

  public LocalDate getPurchaseDate() {
    return purchaseDate;
  }

  public void setPurchaseDate(LocalDate purchaseDate) {
    this.purchaseDate = purchaseDate;
  }

  public LocalDate getWarrantyEndDate() {
    return warrantyEndDate;
  }

  public void setWarrantyEndDate(LocalDate warrantyEndDate) {
    this.warrantyEndDate = warrantyEndDate;
  }

  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public String getLocationName() {
    return location == null ? null : location.getName();
  }

  public AssetStatus getStatus() {
    return status;
  }

  public void setStatus(AssetStatus status) {
    this.status = status;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public Integer getVersion() {
    return version;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  /** "Lenovo ThinkPad T14", or just the model when there is no manufacturer. */
  public String getDisplayName() {
    return manufacturer == null || manufacturer.isBlank() ? model : manufacturer + " " + model;
  }
}
