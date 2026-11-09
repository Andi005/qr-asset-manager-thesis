package io.github.andi005.qram.security;

import io.github.andi005.qram.common.AppProperties;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * On the very first start (no users yet) creates the account "admin" with the password from
 * ADMIN_INITIAL_PASSWORD. Without that variable a random password is generated and written to the
 * log once. Nothing happens on later starts.
 */
@Component
@Order(1)
class AdminBootstrap implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

  private final AppUserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final AppProperties properties;
  private final Clock clock;

  AdminBootstrap(
      AppUserRepository users,
      PasswordEncoder passwordEncoder,
      AppProperties properties,
      Clock clock) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (users.count() > 0) {
      return;
    }
    String password = properties.adminInitialPassword();
    if (password == null || password.isBlank()) {
      byte[] random = new byte[9];
      new SecureRandom().nextBytes(random);
      password = HexFormat.of().formatHex(random);
      log.warn(
          "ADMIN_INITIAL_PASSWORD is not set. Created user 'admin' with the generated password: {}"
              + " (change it after logging in)",
          password);
    } else {
      log.info("Created the first user 'admin' with the password from ADMIN_INITIAL_PASSWORD.");
    }
    users.save(
        new AppUser(
            "admin",
            passwordEncoder.encode(password),
            "Administrator",
            Role.ADMIN,
            clock.instant()));
  }
}
