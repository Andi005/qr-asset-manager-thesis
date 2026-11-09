package io.github.andi005.qram.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** One line of the audit trail: who did what to which record, and when. Never changed. */
@Entity
@Table(name = "change_log")
public class ChangeLogEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "entity_type", nullable = false, length = 20)
  private EntityType entityType;

  @Column(name = "entity_id", nullable = false)
  private Long entityId;

  @Column(nullable = false, length = 30)
  private String action;

  @Column(nullable = false, length = 60)
  private String username;

  @Column(nullable = false)
  private Instant at;

  @Column(length = 4000)
  private String details;

  protected ChangeLogEntry() {}

  public ChangeLogEntry(
      EntityType entityType,
      Long entityId,
      String action,
      String username,
      Instant at,
      String details) {
    this.entityType = entityType;
    this.entityId = entityId;
    this.action = action;
    this.username = username;
    this.at = at;
    this.details = details;
  }

  public Long getId() {
    return id;
  }

  public EntityType getEntityType() {
    return entityType;
  }

  public Long getEntityId() {
    return entityId;
  }

  public String getAction() {
    return action;
  }

  public String getUsername() {
    return username;
  }

  public Instant getAt() {
    return at;
  }

  public String getDetails() {
    return details;
  }
}
