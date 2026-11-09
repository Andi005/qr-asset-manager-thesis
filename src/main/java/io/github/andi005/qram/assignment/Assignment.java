package io.github.andi005.qram.assignment;

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
 * "Employee X held asset Y from {@code assignedAt} to {@code returnedAt}." While {@code returnedAt}
 * is null the assignment is open and X is the current holder. Rows are closed, never deleted, so
 * they form the full history.
 */
@Entity
@Table(name = "assignment")
public class Assignment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "asset_id", nullable = false)
  private Asset asset;

  @ManyToOne(optional = false)
  @JoinColumn(name = "employee_id", nullable = false)
  private Employee employee;

  @Column(name = "assigned_at", nullable = false)
  private Instant assignedAt;

  @Column(name = "assigned_by", nullable = false, length = 60)
  private String assignedBy;

  @Column(name = "returned_at")
  private Instant returnedAt;

  @Column(name = "returned_by", length = 60)
  private String returnedBy;

  @Column(length = 500)
  private String note;

  protected Assignment() {}

  public Assignment(
      Asset asset, Employee employee, Instant assignedAt, String assignedBy, String note) {
    this.asset = asset;
    this.employee = employee;
    this.assignedAt = assignedAt;
    this.assignedBy = assignedBy;
    this.note = note;
  }

  /** Closes the assignment: the asset is back and has no holder. */
  public void close(Instant at, String by) {
    this.returnedAt = at;
    this.returnedBy = by;
  }

  public boolean isOpen() {
    return returnedAt == null;
  }

  public Long getId() {
    return id;
  }

  public Asset getAsset() {
    return asset;
  }

  public Employee getEmployee() {
    return employee;
  }

  public Instant getAssignedAt() {
    return assignedAt;
  }

  public String getAssignedBy() {
    return assignedBy;
  }

  public Instant getReturnedAt() {
    return returnedAt;
  }

  public String getReturnedBy() {
    return returnedBy;
  }

  public String getNote() {
    return note;
  }
}
