package io.github.andi005.qram.importer;

import io.github.andi005.qram.asset.AssetCategory;
import io.github.andi005.qram.asset.AssetStatus;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Checks every row of an import file and turns valid rows into {@link ImportRow}s. It collects
 * <em>all</em> problems instead of stopping at the first, so one report shows everything to fix.
 * Pure Java: the data it compares against (tags, serials, locations already in the database) is
 * passed in.
 */
final class ImportValidator {

  private static final Pattern TAG = Pattern.compile("IT-[0-9]{6}");
  private static final Pattern SERIAL = Pattern.compile("[A-Za-z0-9._/-]{1,64}");
  private static final DateTimeFormatter ALBANIAN_DATE =
      DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT);
  private static final DateTimeFormatter ISO_DATE =
      DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

  private static final Map<String, AssetCategory> CATEGORIES =
      Map.ofEntries(
          Map.entry("laptop", AssetCategory.LAPTOP),
          Map.entry("desktop", AssetCategory.DESKTOP),
          Map.entry("kompjuter", AssetCategory.DESKTOP),
          Map.entry("monitor", AssetCategory.MONITOR),
          Map.entry("printer", AssetCategory.PRINTER),
          Map.entry("projektor", AssetCategory.PROJECTOR),
          Map.entry("projector", AssetCategory.PROJECTOR),
          Map.entry("rrjet", AssetCategory.NETWORK),
          Map.entry("network", AssetCategory.NETWORK),
          Map.entry("telefon", AssetCategory.PHONE),
          Map.entry("phone", AssetCategory.PHONE),
          Map.entry("tjeter", AssetCategory.OTHER),
          Map.entry("other", AssetCategory.OTHER));

  private static final Map<String, AssetStatus> STATUSES =
      Map.ofEntries(
          Map.entry("ne magazine", AssetStatus.IN_STOCK),
          Map.entry("ne perdorim", AssetStatus.IN_STOCK),
          Map.entry("ne dispozicion", AssetStatus.IN_STOCK),
          Map.entry("in stock", AssetStatus.IN_STOCK),
          Map.entry("in_stock", AssetStatus.IN_STOCK),
          Map.entry("in use", AssetStatus.IN_STOCK),
          Map.entry("ne riparim", AssetStatus.IN_REPAIR),
          Map.entry("in repair", AssetStatus.IN_REPAIR),
          Map.entry("in_repair", AssetStatus.IN_REPAIR),
          Map.entry("jashte perdorimit", AssetStatus.RETIRED),
          Map.entry("i nxjerre jashte perdorimit", AssetStatus.RETIRED),
          Map.entry("retired", AssetStatus.RETIRED));

  private ImportValidator() {}

  /**
   * What is already in the database.
   *
   * @param tags asset tags in use
   * @param serials serial numbers in use (upper case)
   * @param locations names of existing locations
   */
  record Existing(Set<String> tags, Set<String> serials, List<String> locations) {}

  record Result(List<ImportRow> rows, List<RowProblem> errors, List<RowProblem> warnings) {}

  static Result validate(List<CsvAssetParser.Row> rows, Existing existing, LocalDate today) {
    List<ImportRow> valid = new ArrayList<>();
    List<RowProblem> errors = new ArrayList<>();
    List<RowProblem> warnings = new ArrayList<>();
    Map<String, Integer> tagLines = new HashMap<>();
    Map<String, Integer> serialLines = new HashMap<>();
    // simplified name → the spelling seen first (from the database, or earlier in the file)
    Map<String, String> locationSpelling = new HashMap<>();
    for (String name : existing.locations()) {
      locationSpelling.put(Column.simplify(name), name);
    }

    for (CsvAssetParser.Row row : rows) {
      int line = row.line();
      int errorsBefore = errors.size();

      // Tag: optional (generated when empty), strict format, unique in the file and the database.
      String tag = row.get(Column.TAG);
      if (tag.isEmpty()) {
        tag = null;
      } else {
        String original = tag;
        tag = tag.toUpperCase(Locale.ROOT);
        if (!TAG.matcher(tag).matches()) {
          errors.add(RowProblem.of(line, Column.TAG, "import.error.tagFormat", original));
        } else if (tagLines.containsKey(tag)) {
          errors.add(
              RowProblem.of(line, Column.TAG, "import.error.tagDuplicate", tag, tagLines.get(tag)));
        } else if (existing.tags().contains(tag)) {
          errors.add(RowProblem.of(line, Column.TAG, "import.error.tagExists", tag));
        }
        tagLines.putIfAbsent(tag, line);
      }

      AssetCategory category = CATEGORIES.get(Column.simplify(row.get(Column.CATEGORY)));
      if (category == null) {
        errors.add(
            RowProblem.of(
                line, Column.CATEGORY, "import.error.category", row.get(Column.CATEGORY)));
      }

      String manufacturer = row.get(Column.MANUFACTURER);
      if (manufacturer.length() > 80) {
        errors.add(RowProblem.of(line, Column.MANUFACTURER, "import.error.tooLong", 80));
      }

      String model = row.get(Column.MODEL);
      if (model.isEmpty()) {
        errors.add(RowProblem.of(line, Column.MODEL, "import.error.required"));
      } else if (model.length() > 120) {
        errors.add(RowProblem.of(line, Column.MODEL, "import.error.tooLong", 120));
      }

      String serial = row.get(Column.SERIAL).toUpperCase(Locale.ROOT);
      if (serial.isEmpty()) {
        serial = null;
        warnings.add(RowProblem.of(line, Column.SERIAL, "import.warning.noSerial"));
      } else if (!SERIAL.matcher(serial).matches()) {
        errors.add(RowProblem.of(line, Column.SERIAL, "import.error.serialFormat", serial));
      } else if (serialLines.containsKey(serial)) {
        errors.add(
            RowProblem.of(
                line,
                Column.SERIAL,
                "import.error.serialDuplicate",
                serial,
                serialLines.get(serial)));
      } else if (existing.serials().contains(serial)) {
        errors.add(RowProblem.of(line, Column.SERIAL, "import.error.serialExists", serial));
      }
      if (serial != null) {
        serialLines.putIfAbsent(serial, line);
      }

      LocalDate purchase = date(row, Column.PURCHASE_DATE, line, errors);
      LocalDate warranty = date(row, Column.WARRANTY_END, line, errors);
      if (purchase != null && purchase.isAfter(today)) {
        errors.add(RowProblem.of(line, Column.PURCHASE_DATE, "import.error.purchaseInFuture"));
      }
      if (purchase != null && warranty != null && warranty.isBefore(purchase)) {
        errors.add(RowProblem.of(line, Column.WARRANTY_END, "import.error.warrantyBeforePurchase"));
      }

      // Location: created if new, but a spelling that differs only in ë/ç or case is refused.
      String location = row.get(Column.LOCATION).replaceAll("\\s+", " ");
      if (location.isEmpty()) {
        location = null;
      } else if (location.length() > 120) {
        errors.add(RowProblem.of(line, Column.LOCATION, "import.error.tooLong", 120));
      } else {
        String key = Column.simplify(location);
        String known = locationSpelling.putIfAbsent(key, location);
        if (known != null && !known.equals(location)) {
          errors.add(
              RowProblem.of(
                  line, Column.LOCATION, "import.error.locationSpelling", location, known));
        }
      }

      String statusText = row.get(Column.STATUS);
      AssetStatus status =
          statusText.isEmpty()
              ? AssetStatus.IN_STOCK
              : STATUSES.get(Column.simplify(statusText).replaceAll("\\s+", " "));
      if (status == null) {
        errors.add(RowProblem.of(line, Column.STATUS, "import.error.status", statusText));
      }

      String notes = row.get(Column.NOTES);
      if (notes.length() > 2000) {
        errors.add(RowProblem.of(line, Column.NOTES, "import.error.tooLong", 2000));
      }

      if (errors.size() == errorsBefore) {
        valid.add(
            new ImportRow(
                line,
                tag,
                category,
                manufacturer.isEmpty() ? null : manufacturer,
                model,
                serial,
                purchase,
                warranty,
                location,
                status,
                notes.isEmpty() ? null : notes));
      }
    }
    return new Result(valid, errors, warnings);
  }

  private static LocalDate date(
      CsvAssetParser.Row row, Column column, int line, List<RowProblem> errors) {
    String text = row.get(column);
    if (text.isEmpty()) {
      return null;
    }
    Optional<LocalDate> parsed = parseDate(text);
    if (parsed.isEmpty()) {
      errors.add(RowProblem.of(line, column, "import.error.date", text));
      return null;
    }
    return parsed.get();
  }

  /** Accepts dd.MM.yyyy (as Excel writes it in Albanian settings) or ISO yyyy-MM-dd. */
  static Optional<LocalDate> parseDate(String text) {
    for (DateTimeFormatter format : List.of(ALBANIAN_DATE, ISO_DATE)) {
      try {
        return Optional.of(LocalDate.parse(text.strip(), format));
      } catch (DateTimeParseException ignored) {
        // try the next format
      }
    }
    return Optional.empty();
  }
}
