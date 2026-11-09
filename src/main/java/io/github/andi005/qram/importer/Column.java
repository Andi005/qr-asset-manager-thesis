package io.github.andi005.qram.importer;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The columns the import understands. Headers are matched ignoring case, spaces, punctuation and
 * Albanian letters (ë = e, ç = c), in Albanian or English, in any order.
 */
public enum Column {
  TAG(Set.of("etiketa", "tag", "assettag")),
  CATEGORY(Set.of("kategoria", "category")),
  MANUFACTURER(Set.of("prodhuesi", "manufacturer")),
  MODEL(Set.of("modeli", "model")),
  SERIAL(Set.of("nrserik", "numriserik", "serial", "serialnumber")),
  PURCHASE_DATE(Set.of("dataeblerjes", "purchasedate")),
  WARRANTY_END(Set.of("garanciaderi", "garancia", "warrantyend", "warrantyenddate")),
  LOCATION(Set.of("lokacioni", "location")),
  STATUS(Set.of("statusi", "status")),
  NOTES(Set.of("shenime", "notes"));

  private final Set<String> names;

  Column(Set<String> names) {
    this.names = names;
  }

  static Optional<Column> forHeader(String header) {
    String key = simplify(header).replaceAll("[^a-z0-9]", "");
    for (Column column : values()) {
      if (column.names.contains(key)) {
        return Optional.of(column);
      }
    }
    return Optional.empty();
  }

  /** Lower case without diacritics: "Ndërtesa A" → "ndertesa a". Used for loose comparisons. */
  static String simplify(String text) {
    if (text == null) {
      return "";
    }
    String decomposed = Normalizer.normalize(text.strip(), Normalizer.Form.NFD);
    return decomposed.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
  }
}
