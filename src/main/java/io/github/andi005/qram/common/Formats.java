package io.github.andi005.qram.common;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * Date and time formatting for the templates, always in the configured zone: {@code
 * ${@fmt.date(asset.purchaseDate)}}, {@code ${@fmt.dateTime(entry.at)}}.
 */
@Component("fmt")
public class Formats {

  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

  private final AppProperties properties;

  public Formats(AppProperties properties) {
    this.properties = properties;
  }

  public String date(LocalDate date) {
    return date == null ? "—" : DATE.format(date);
  }

  public String dateTime(Instant instant) {
    return instant == null ? "—" : DATE_TIME.format(instant.atZone(properties.zoneId()));
  }

  public String time(Instant instant) {
    return instant == null ? "—" : TIME.format(instant.atZone(properties.zoneId()));
  }

  /** A duration as m:ss, or h:mm:ss when it is an hour or longer. */
  public String duration(Duration duration) {
    if (duration == null) {
      return "—";
    }
    long seconds = Math.max(duration.toSeconds(), 0);
    long h = seconds / 3600;
    long m = (seconds % 3600) / 60;
    long s = seconds % 60;
    return h > 0 ? "%d:%02d:%02d".formatted(h, m, s) : "%d:%02d".formatted(m, s);
  }
}
