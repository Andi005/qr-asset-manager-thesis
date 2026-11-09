package io.github.andi005.qram.qr;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** What the server accepts from a scanner (FR-15, ADR 003). */
class TagParserTest {

  @Test
  @DisplayName("A full label URL gives its tag")
  void fullUrl() {
    assertThat(TagParser.parse("https://qram.onrender.com/a/IT-000042")).contains("IT-000042");
  }

  @Test
  @DisplayName("A URL from another (old) host still works: only the tag matters")
  void otherHost() {
    assertThat(TagParser.parse("http://192.168.1.20:8080/a/IT-000042")).contains("IT-000042");
  }

  @Test
  @DisplayName("Query, fragment and a trailing slash are ignored")
  void queryAndFragment() {
    assertThat(TagParser.parse("https://x.org/a/IT-000042/?utm=1#top")).contains("IT-000042");
  }

  @Test
  @DisplayName("A bare tag is accepted")
  void bareTag() {
    assertThat(TagParser.parse("IT-000042")).contains("IT-000042");
  }

  @Test
  @DisplayName("Lower case and spaces around are normalised")
  void lowerCase() {
    assertThat(TagParser.parse("  it-000042 ")).contains("IT-000042");
    assertThat(TagParser.parse("https://x.org/a/it-000042")).contains("IT-000042");
  }

  @ParameterizedTest(name = "rejected: \"{0}\"")
  @NullAndEmptySource
  @ValueSource(
      strings = {
        "   ",
        "garbage",
        "IT-42",
        "IT-0000420",
        "XX-000042",
        "IT000042",
        "https://x.org/b/IT-000042",
        "https://x.org/IT-000042",
        "https://x.org/a/",
        "https://x.org/a/IT-000042/extra",
        "it-000042/",
        "javascript:alert(1)",
        "https://x.org/a/IT-000042%00"
      })
  void rejected(String raw) {
    assertThat(TagParser.parse(raw)).isEmpty();
  }

  @Test
  @DisplayName("Input longer than the limit is rejected without parsing")
  void tooLong() {
    String raw = "https://x.org/" + "a/".repeat(100) + "IT-000042";
    assertThat(raw.length()).isGreaterThan(TagParser.MAX_LENGTH);
    assertThat(TagParser.parse(raw)).isEmpty();
  }
}
