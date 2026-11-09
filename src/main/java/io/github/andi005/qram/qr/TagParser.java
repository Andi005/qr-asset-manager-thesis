package io.github.andi005.qram.qr;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Turns whatever a scanner (or a person) delivers into an asset tag, on the server, so nothing from
 * the browser is trusted.
 *
 * <p>Accepted:
 *
 * <ul>
 *   <li>a full label URL from any host: {@code https://example.org/a/IT-000042} (query and fragment
 *       are ignored), so old labels keep working if the host ever changes;
 *   <li>a bare tag: {@code IT-000042}, in any letter case.
 * </ul>
 *
 * Everything else (garbage, other URLs, too long) gives an empty result.
 */
public final class TagParser {

  /** Longer input is rejected without looking at it. */
  public static final int MAX_LENGTH = 200;

  private static final Pattern TAG = Pattern.compile("IT-[0-9]{6}");

  private TagParser() {}

  public static Optional<String> parse(String raw) {
    if (raw == null) {
      return Optional.empty();
    }
    String text = raw.strip();
    if (text.isEmpty() || text.length() > MAX_LENGTH) {
      return Optional.empty();
    }
    String candidate = text.contains("/") ? tagFromUrl(text) : text;
    if (candidate == null) {
      return Optional.empty();
    }
    candidate = candidate.toUpperCase(Locale.ROOT);
    return TAG.matcher(candidate).matches() ? Optional.of(candidate) : Optional.empty();
  }

  /** The last path segment of {@code .../a/<TAG>}, or null if the URL has another shape. */
  private static String tagFromUrl(String text) {
    String path;
    try {
      path = new URI(text).getPath();
    } catch (URISyntaxException e) {
      return null;
    }
    if (path == null) {
      return null;
    }
    while (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    int slash = path.lastIndexOf('/');
    if (slash < 0) {
      return null;
    }
    String before = path.substring(0, slash).toLowerCase(Locale.ROOT);
    if (!before.endsWith("/a")) {
      return null;
    }
    return path.substring(slash + 1);
  }
}
