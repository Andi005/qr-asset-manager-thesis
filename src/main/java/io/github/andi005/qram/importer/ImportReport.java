package io.github.andi005.qram.importer;

import java.util.List;

/**
 * The result of an import attempt, shown on the report page.
 *
 * @param rowsRead data rows found in the file
 * @param imported assets created (0 whenever there is any error: all or nothing)
 * @param locationsCreated new locations created by the import
 */
public record ImportReport(
    String fileName,
    int rowsRead,
    List<RowProblem> errors,
    List<RowProblem> warnings,
    int imported,
    int locationsCreated) {

  public boolean success() {
    return errors.isEmpty();
  }
}
