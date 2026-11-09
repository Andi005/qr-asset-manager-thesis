package io.github.andi005.qram.common;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a libpq-style connection string, as Neon and most hosts hand it out ({@code
 * postgresql://user:password@host/db?sslmode=require}), into what the Java driver needs: a JDBC URL
 * without credentials, plus the user name and password separately.
 *
 * @param jdbcUrl e.g. {@code jdbc:postgresql://host:5432/db?sslmode=require}
 * @param username the decoded user name
 * @param password the decoded password
 */
public record PostgresUrl(String jdbcUrl, String username, String password) {

  /** Parses {@code postgres://} or {@code postgresql://} URLs. */
  public static PostgresUrl parse(String url) {
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("Empty database URL");
    }
    URI uri;
    try {
      uri = new URI(url.strip());
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Not a valid database URL", e);
    }
    String scheme = uri.getScheme();
    if (!"postgres".equals(scheme) && !"postgresql".equals(scheme)) {
      throw new IllegalArgumentException("Expected a postgres:// or postgresql:// URL");
    }
    if (uri.getHost() == null) {
      throw new IllegalArgumentException("The database URL has no host");
    }
    String username = null;
    String password = null;
    String userInfo = uri.getRawUserInfo();
    if (userInfo != null) {
      int colon = userInfo.indexOf(':');
      username = decode(colon < 0 ? userInfo : userInfo.substring(0, colon));
      password = colon < 0 ? null : decode(userInfo.substring(colon + 1));
    }
    StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
    if (uri.getPort() > 0) {
      jdbc.append(':').append(uri.getPort());
    }
    jdbc.append(uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath());
    List<String> params = new ArrayList<>();
    if (uri.getRawQuery() != null) {
      for (String param : uri.getRawQuery().split("&")) {
        if (!param.isBlank()) {
          // libpq writes channel_binding; the Java driver calls the same option channelBinding.
          params.add(
              param.startsWith("channel_binding=")
                  ? "channelBinding" + param.substring(15)
                  : param);
        }
      }
    }
    if (!params.isEmpty()) {
      jdbc.append('?').append(String.join("&", params));
    }
    return new PostgresUrl(jdbc.toString(), username, password);
  }

  /** Percent-decoding; a literal "+" stays a "+" (it is not a space in a URL's user part). */
  private static String decode(String text) {
    return URLDecoder.decode(text.replace("+", "%2B"), StandardCharsets.UTF_8);
  }

  /** Never print the password, e.g. in logs. */
  @Override
  public String toString() {
    return "PostgresUrl[" + jdbcUrl + ", user=" + username + "]";
  }
}
