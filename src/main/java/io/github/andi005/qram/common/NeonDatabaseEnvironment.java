package io.github.andi005.qram.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Lets the app use Neon's connection variables directly, as `neon env pull` writes them and as they
 * are set on the hosting platform:
 *
 * <ul>
 *   <li>{@code DATABASE_URL} (pooled, host with {@code -pooler}) for the application's queries;
 *   <li>{@code DATABASE_URL_UNPOOLED} (direct) for Flyway migrations, which need a direct session
 *       (Neon's guidance: pooled for app traffic, direct for schema migrations).
 * </ul>
 *
 * Both are {@code postgresql://user:password@host/db?...} URLs and are converted to JDBC form. This
 * only applies when {@code DB_URL} is not set, so the plain {@code DB_URL / DB_USER / DB_PASSWORD}
 * configuration (Docker Compose, other hosts) keeps working unchanged.
 *
 * <p>Locally, with the {@code neon} profile active, the variables are also read from the {@code
 * .env.local} file that {@code neon env pull} creates (real environment variables win).
 */
public class NeonDatabaseEnvironment implements EnvironmentPostProcessor, Ordered {

  static final String ENV_FILE = ".env.local";

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication app) {
    if (has(environment.getProperty("DB_URL"))) {
      return;
    }
    Map<String, String> fromFile =
        Arrays.asList(environment.getActiveProfiles()).contains("neon")
            ? readEnvFile(Path.of(ENV_FILE))
            : Map.of();
    String pooled = first(environment.getProperty("DATABASE_URL"), fromFile.get("DATABASE_URL"));
    String direct =
        first(
            environment.getProperty("DATABASE_URL_UNPOOLED"),
            fromFile.get("DATABASE_URL_UNPOOLED"));
    Map<String, Object> properties = properties(pooled, direct);
    if (!properties.isEmpty()) {
      environment.getPropertySources().addFirst(new MapPropertySource("neonDatabase", properties));
    }
  }

  /** The Spring properties for the given URLs (either may be null). */
  static Map<String, Object> properties(String pooled, String direct) {
    Map<String, Object> properties = new LinkedHashMap<>();
    String app = has(pooled) ? pooled : direct;
    if (has(app)) {
      put(properties, "spring.datasource", PostgresUrl.parse(app));
    }
    if (has(direct)) {
      put(properties, "spring.flyway", PostgresUrl.parse(direct));
    }
    return properties;
  }

  private static void put(Map<String, Object> properties, String prefix, PostgresUrl url) {
    properties.put(prefix + ".url", url.jdbcUrl());
    if (url.username() != null) {
      properties.put(
          prefix + ".user" + (prefix.equals("spring.flyway") ? "" : "name"), url.username());
    }
    if (url.password() != null) {
      properties.put(prefix + ".password", url.password());
    }
  }

  /** Minimal .env reader: KEY=value lines, optional quotes, # comments. */
  static Map<String, String> readEnvFile(Path file) {
    if (!Files.isRegularFile(file)) {
      return Map.of();
    }
    List<String> lines;
    try {
      lines = Files.readAllLines(file, StandardCharsets.UTF_8);
    } catch (IOException e) {
      return Map.of();
    }
    Map<String, String> values = new HashMap<>();
    for (String line : lines) {
      String trimmed = line.strip();
      int eq = trimmed.indexOf('=');
      if (trimmed.isEmpty() || trimmed.startsWith("#") || eq < 1) {
        continue;
      }
      String key = trimmed.substring(0, eq).strip();
      if (key.startsWith("export ")) {
        key = key.substring(7).strip();
      }
      String value = trimmed.substring(eq + 1).strip();
      if (value.length() >= 2
          && (value.startsWith("\"") && value.endsWith("\"")
              || value.startsWith("'") && value.endsWith("'"))) {
        value = value.substring(1, value.length() - 1);
      }
      values.put(key, value);
    }
    return values;
  }

  private static String first(String a, String b) {
    return has(a) ? a : b;
  }

  private static boolean has(String value) {
    return value != null && !value.isBlank();
  }

  /** After Spring Boot has loaded application.yml and the active profiles. */
  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }
}
