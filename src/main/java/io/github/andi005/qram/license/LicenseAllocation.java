package io.github.andi005.qram.license;

import io.github.andi005.qram.asset.Asset;
import io.github.andi005.qram.employee.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One seat of a license, held by an employee <b>or</b> an asset (never both; the database checks
 * it). Open while {@code releasedAt} is null.
 */
@Entity
@Table(name = "license_allocation")
public class LicenseAllocation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "license_id", nullable = false)
  private SoftwareLicense license;

  @ManyToOne
  @JoinColumn(name = "employee_id")
  private Employee employee;

  @ManyToOne
  @JoinColumn(name = "asset_id")
  private Asset asset;

  @Column(name = "allocated_at", nullable = false)
  private Instant allocatedAt;

  @Column(name = "allocated_by", nullable = false, length = 60)
  private String allocatedBy;

  @Column(name = "released_at")
  private Instant releasedAt;

  @Column(name = "released_by", length = 60)
  private String releasedBy;

  protected LicenseAllocation() {}

  private LicenseAllocation(
      SoftwareLicense license, Employee employee, Asset asset, Instant at, String by) {
    this.license = license;
    this.employee = employee;
    this.asset = asset;
    this.allocatedAt = at;
    this.allocatedBy = by;
  }

  public static LicenseAllocation toEmployee(
      SoftwareLicense license, Employee employee, Instant at, String by) {
    return new LicenseAllocation(license, employee, null, at, by);
  }

  public static LicenseAllocation toAsset(
      SoftwareLicense license, Asset asset, Instant at, String by) {
    return new LicenseAllocation(license, null, asset, at, by);
  }

  public void release(Instant at, String by) {
    this.releasedAt = at;
    this.releasedBy = by;
  }

  public boolean isOpen() {
    return releasedAt == null;
  }

  /** The employee's name or the asset's tag, for lists. */
  public String getHolderName() {
    return employee != null
        ? employee.getFullName()
        : asset.getTag() + " " + asset.getDisplayName();
  }

  public Long getId() {
    return id;
  }

  public SoftwareLicense getLicense() {
    return license;
  }

  public Employee getEmployee() {
    return employee;
  }

  public Asset getAsset() {
    return asset;
  }

  public Instant getAllocatedAt() {
    return allocatedAt;
  }

  public String getAllocatedBy() {
    return allocatedBy;
  }

  public Instant getReleasedAt() {
    return releasedAt;
  }

  public String getReleasedBy() {
    return releasedBy;
  }
}
