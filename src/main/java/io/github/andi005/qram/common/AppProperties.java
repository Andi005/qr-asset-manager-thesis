package io.github.andi005.qram.common;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The application's own settings ({@code app.*} in application.yml), all filled from environment
 * variables in production.
 *
 * @param baseUrl the public address printed on QR labels, without a trailing slash
 * @param zone the time zone used to decide what "today" is
 * @param adminInitialPassword password for the first ADMIN account (only used on an empty DB)
 * @param demoPassword password for the demo accounts (demo profile only)
 */
@ConfigurationProperties("app")
public record AppProperties(
    String baseUrl, String zone, String adminInitialPassword, String demoPassword) {

  public AppProperties {
    if (baseUrl == null || baseUrl.isBlank()) {
      baseUrl = "http://localhost:8080";
    }
    baseUrl = baseUrl.strip();
    while (baseUrl.endsWith("/")) {
      baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }
    if (zone == null || zone.isBlank()) {
      zone = "Europe/Belgrade";
    }
  }

  public ZoneId zoneId() {
    return ZoneId.of(zone);
  }
}
