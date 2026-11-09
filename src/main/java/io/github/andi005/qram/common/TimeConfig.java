package io.github.andi005.qram.common;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One shared clock. Every service asks this bean for "now" and "today" instead of calling
 * LocalDate.now(), so tests can use a fixed date and the expiry rules are testable.
 */
@Configuration
class TimeConfig {

  @Bean
  Clock clock(AppProperties properties) {
    return Clock.system(properties.zoneId());
  }
}
