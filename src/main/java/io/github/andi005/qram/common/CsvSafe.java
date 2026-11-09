package io.github.andi005.qram.common;

/**
 * Makes text safe to put in a CSV file that will be opened in Excel. A cell that starts with = + -
 * or @ would be run as a formula ("CSV injection"), so it gets a leading apostrophe.
 */
public final class CsvSafe {

  private CsvSafe() {}

  public static String cell(Object value) {
    if (value == null) {
      return "";
    }
    String text = value.toString();
    if (text.isEmpty()) {
      return text;
    }
    char first = text.charAt(0);
    if (first == '='
        || first == '+'
        || first == '-'
        || first == '@'
        || first == '\t'
        || first == '\r') {
      return "'" + text;
    }
    return text;
  }
}
