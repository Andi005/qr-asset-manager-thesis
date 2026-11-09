package io.github.andi005.qram.importer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A record of one successful import: which file, who, when, how many rows. */
@Entity
@Table(name = "import_batch")
public class ImportBatch {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "file_name", nullable = false, length = 200)
  private String fileName;

  @Column(name = "imported_at", nullable = false)
  private Instant importedAt;

  @Column(name = "imported_by", nullable = false, length = 60)
  private String importedBy;

  @Column(name = "rows_imported", nullable = false)
  private int rowsImported;

  @Column(name = "locations_created", nullable = false)
  private int locationsCreated;

  protected ImportBatch() {}

  public ImportBatch(
      String fileName, Instant importedAt, String importedBy, int rowsImported, int locations) {
    this.fileName = fileName;
    this.importedAt = importedAt;
    this.importedBy = importedBy;
    this.rowsImported = rowsImported;
    this.locationsCreated = locations;
  }

  public Long getId() {
    return id;
  }

  public String getFileName() {
    return fileName;
  }

  public Instant getImportedAt() {
    return importedAt;
  }

  public String getImportedBy() {
    return importedBy;
  }

  public int getRowsImported() {
    return rowsImported;
  }

  public int getLocationsCreated() {
    return locationsCreated;
  }
}
