package io.github.andi005.qram.common;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * Looks up Albanian texts from messages.properties in Java code (for example the field names in a
 * change-log entry). Templates use {@code #{key}} instead.
 */
@Component
public class Labels {

  private static final Locale ALBANIAN = Locale.forLanguageTag("sq");

  private final MessageSource messages;

  public Labels(MessageSource messages) {
    this.messages = messages;
  }

  public String get(String key, Object... args) {
    return messages.getMessage(key, args, key, ALBANIAN);
  }
}
