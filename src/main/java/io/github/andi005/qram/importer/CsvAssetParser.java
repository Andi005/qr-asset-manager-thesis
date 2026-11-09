package io.github.andi005.qram.importer;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Reads a CSV file saved from Excel into rows of named cells. It copes with what Excel produces:
 *
 * <ul>
 *   <li>a byte-order mark at the start;
 *   <li>{@code ;} (Albanian/European settings) or {@code ,} as separator, detected from the header;
 *   <li>UTF-8, or the older Windows-1252 encoding as a fallback (with a warning);
 *   <li>columns in any order, headers in Albanian or English;
 *   <li>empty rows (such as {@code ;;;;;} at the end), which are skipped.
 * </ul>
 */
final class CsvAssetParser {

  static final int MAX_ROWS = 5000;

  private CsvAssetParser() {}

  /** The rows read, or the problems that make the whole file unreadable. */
  record Parsed(List<Row> rows, List<RowProblem> fileErrors, List<RowProblem> warnings) {}

  /** One data row: its line number in the file and its cells by column. */
  record Row(int line, Map<Column, String> cells) {

    String get(Column column) {
      String value = cells.get(column);
      return value == null ? "" : value.strip();
    }
  }

  static Parsed parse(byte[] content) {
    List<RowProblem> warnings = new ArrayList<>();
    if (content == null || content.length == 0) {
      return failed(RowProblem.of(0, null, "import.error.emptyFile"));
    }
    String text = decode(content, warnings);
    if (text.startsWith("﻿")) {
      text = text.substring(1);
    }
    String firstLine = text.lines().findFirst().orElse("");
    char delimiter = count(firstLine, ';') >= count(firstLine, ',') ? ';' : ',';

    CSVFormat format =
        CSVFormat.DEFAULT
            .builder()
            .setDelimiter(delimiter)
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setAllowMissingColumnNames(true)
            .setTrim(true)
            .get();

    try (CSVParser parser = CSVParser.parse(new StringReader(text), format)) {
      Map<Integer, Column> columnAt = new HashMap<>();
      List<String> headers = parser.getHeaderNames();
      for (int i = 0; i < headers.size(); i++) {
        Optional<Column> column = Column.forHeader(headers.get(i));
        if (column.isPresent() && !columnAt.containsValue(column.get())) {
          columnAt.put(i, column.get());
        } else if (!headers.get(i).isBlank()) {
          warnings.add(RowProblem.of(1, null, "import.warning.unknownColumn", headers.get(i)));
        }
      }
      List<RowProblem> missing = new ArrayList<>();
      for (Column required : List.of(Column.CATEGORY, Column.MODEL)) {
        if (!columnAt.containsValue(required)) {
          missing.add(RowProblem.of(1, required, "import.error.missingColumn"));
        }
      }
      if (!missing.isEmpty()) {
        return new Parsed(List.of(), missing, warnings);
      }

      List<Row> rows = new ArrayList<>();
      for (CSVRecord record : parser) {
        Map<Column, String> cells = new EnumMap<>(Column.class);
        boolean empty = true;
        for (Map.Entry<Integer, Column> e : columnAt.entrySet()) {
          String value = e.getKey() < record.size() ? record.get(e.getKey()) : "";
          cells.put(e.getValue(), value);
          if (!value.isBlank()) {
            empty = false;
          }
        }
        if (empty) {
          continue;
        }
        if (rows.size() == MAX_ROWS) {
          return failed(RowProblem.of(0, null, "import.error.tooManyRows", MAX_ROWS));
        }
        // The header is line 1, so data record n sits on line n + 1.
        rows.add(new Row((int) record.getRecordNumber() + 1, cells));
      }
      if (rows.isEmpty()) {
        return failed(RowProblem.of(0, null, "import.error.noRows"));
      }
      return new Parsed(rows, List.of(), warnings);
    } catch (IOException | UncheckedIOException | IllegalArgumentException e) {
      return failed(RowProblem.of(0, null, "import.error.unreadable"));
    }
  }

  private static String decode(byte[] content, List<RowProblem> warnings) {
    try {
      return StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(content))
          .toString();
    } catch (CharacterCodingException e) {
      warnings.add(RowProblem.of(0, null, "import.warning.encoding"));
      return new String(content, Charset.forName("windows-1252"));
    }
  }

  private static Parsed failed(RowProblem problem) {
    return new Parsed(List.of(), List.of(problem), List.of());
  }

  private static long count(String text, char c) {
    return text.chars().filter(ch -> ch == c).count();
  }
}
