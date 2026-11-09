package io.github.andi005.qram.license;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;

/** A software license with a number of seats and an expiry date (null = perpetual). */
@Entity
@Table(name = "software_license")
public class SoftwareLicense {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(length = 80)
  private String vendor;

  @Column(name = "seats_total", nullable = false)
  private int seatsTotal;

  @Column(name = "expiry_date")
  private LocalDate expiryDate;

  @Column(length = 2000)
  private String notes;

  @Version
  @Column(nullable = false)
  private Integer version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected SoftwareLicense() {}

  public SoftwareLicense(String name, int seatsTotal, Instant createdAt) {
    this.name = name;
    this.seatsTotal = seatsTotal;
    this.createdAt = createdAt;
    this.updatedAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVendor() {
    return vendor;
  }

  public void setVendor(String vendor) {
    this.vendor = vendor;
  }

  public int getSeatsTotal() {
    return seatsTotal;
  }

  public void setSeatsTotal(int seatsTotal) {
    this.seatsTotal = seatsTotal;
  }

  public LocalDate getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(LocalDate expiryDate) {
    this.expiryDate = expiryDate;
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
}
