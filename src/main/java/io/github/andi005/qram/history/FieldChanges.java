package io.github.andi005.qram.history;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Collects "field: old → new" pieces for a readable change-log line, skipping fields that did not
 * change. Example result: {@code Statusi: Në dispozicion → Në riparim; Lokacioni: — → Salla 101}.
 */
public final class FieldChanges {

  private final List<String> parts = new ArrayList<>();

  public FieldChanges add(String label, Object oldValue, Object newValue) {
    if (!Objects.equals(oldValue, newValue)) {
      parts.add(label + ": " + show(oldValue) + " → " + show(newValue));
    }
    return this;
  }

  public boolean isEmpty() {
    return parts.isEmpty();
  }

  @Override
  public String toString() {
    return String.join("; ", parts);
  }

  private static String show(Object value) {
    if (value == null || value.toString().isBlank()) {
      return "—";
    }
    return value.toString();
  }
}
