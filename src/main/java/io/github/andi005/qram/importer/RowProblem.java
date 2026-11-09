package io.github.andi005.qram.importer;

import java.util.List;

/**
 * One problem in the file, shown in the import report.
 *
 * @param line the line number in the file (the header is line 1; 0 = the file as a whole)
 * @param column the column it is about, or null
 * @param messageKey the reason, as a key in messages.properties
 * @param args values for the message placeholders
 */
public record RowProblem(int line, Column column, String messageKey, List<String> args) {

  static RowProblem of(int line, Column column, String messageKey, Object... args) {
    return new RowProblem(
        line, column, messageKey, java.util.Arrays.stream(args).map(String::valueOf).toList());
  }

  public Object[] argsArray() {
    return args.toArray();
  }
}
