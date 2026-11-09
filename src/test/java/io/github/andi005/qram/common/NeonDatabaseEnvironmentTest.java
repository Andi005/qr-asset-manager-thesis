package io.github.andi005.qram.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Neon's postgresql:// URLs become the JDBC settings Spring Boot and Flyway need. */
class NeonDatabaseEnvironmentTest {

  private static final String POOLED =
      "postgresql://neondb_owner:s3cr%40t+x@ep-a-1-pooler.eu-central-1.aws.neon.tech/neondb"
          + "?channel_binding=require&sslmode=require";
  private static final String DIRECT =
      "postgresql://neondb_owner:s3cr%40t+x@ep-a-1.eu-central-1.aws.neon.tech/neondb"
          + "?channel_binding=require&sslmode=require";

  @Test
  @DisplayName("A Neon URL becomes a JDBC URL without credentials, plus user and password")
  void parsesNeonUrl() {
    PostgresUrl url = PostgresUrl.parse(DIRECT);
    assertThat(url.jdbcUrl())
        .isEqualTo(
            "jdbc:postgresql://ep-a-1.eu-central-1.aws.neon.tech/neondb"
                + "?channelBinding=require&sslmode=require");
    assertThat(url.username()).isEqualTo("neondb_owner");
    assertThat(url.password()).isEqualTo("s3cr@t+x");
    assertThat(url.toString()).doesNotContain("s3cr");
  }

  @Test
  @DisplayName("Port and the short postgres:// scheme are understood")
  void portAndShortScheme() {
    assertThat(PostgresUrl.parse("postgres://u:p@localhost:5433/qram").jdbcUrl())
        .isEqualTo("jdbc:postgresql://localhost:5433/qram");
  }

  @Test
  @DisplayName("Anything that is not a Postgres URL is refused")
  void refusesOtherUrls() {
    assertThatThrownBy(() -> PostgresUrl.parse("mysql://u:p@h/db"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> PostgresUrl.parse(" ")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Pooled URL for the app's queries, direct URL for Flyway migrations")
  void pooledForAppDirectForMigrations() {
    var props = NeonDatabaseEnvironment.properties(POOLED, DIRECT);
    assertThat((String) props.get("spring.datasource.url")).contains("-pooler.");
    assertThat(props.get("spring.datasource.username")).isEqualTo("neondb_owner");
    assertThat(props.get("spring.datasource.password")).isEqualTo("s3cr@t+x");
    assertThat((String) props.get("spring.flyway.url")).doesNotContain("-pooler");
    assertThat(props.get("spring.flyway.user")).isEqualTo("neondb_owner");
    assertThat(props.get("spring.flyway.password")).isEqualTo("s3cr@t+x");
  }

  @Test
  @DisplayName("With only the direct URL, both use it; with nothing, nothing changes")
  void fallbacks() {
    var onlyDirect = NeonDatabaseEnvironment.properties(null, DIRECT);
    assertThat(onlyDirect.get("spring.datasource.url"))
        .isEqualTo(onlyDirect.get("spring.flyway.url"));
    assertThat(NeonDatabaseEnvironment.properties(null, null)).isEmpty();
  }

  @Test
  @DisplayName("The .env.local file from `neon env pull` is read, quotes removed")
  void readsEnvFile(@TempDir Path dir) throws IOException {
    Path file = dir.resolve(".env.local");
    Files.writeString(
        file, "# comment\nDATABASE_URL=\"" + POOLED + "\"\nexport NEON_BRANCH='production'\n\n");
    var values = NeonDatabaseEnvironment.readEnvFile(file);
    assertThat(values.get("DATABASE_URL")).isEqualTo(POOLED);
    assertThat(values.get("NEON_BRANCH")).isEqualTo("production");
    assertThat(NeonDatabaseEnvironment.readEnvFile(dir.resolve("missing"))).isEmpty();
  }
}
