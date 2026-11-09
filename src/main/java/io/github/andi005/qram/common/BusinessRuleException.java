package io.github.andi005.qram.common;

import java.util.Arrays;
import org.springframework.validation.BindingResult;

/**
 * A business rule refused the action (for example "this asset already has a holder"). Carries a
 * message key from messages.properties, so the page can show the reason in Albanian.
 *
 * <p>When {@link #field()} is set, the controller shows the message next to that form field.
 */
public class BusinessRuleException extends RuntimeException {

  private final String messageKey;
  private final String field;
  private final String[] args;

  public BusinessRuleException(String messageKey, Object... args) {
    this(null, messageKey, args);
  }

  private BusinessRuleException(String field, String messageKey, Object... args) {
    super(messageKey + " " + Arrays.toString(args));
    this.messageKey = messageKey;
    this.field = field;
    this.args = Arrays.stream(args).map(String::valueOf).toArray(String[]::new);
  }

  /** The same, but tied to one form field. */
  public static BusinessRuleException onField(String field, String messageKey, Object... args) {
    return new BusinessRuleException(field, messageKey, args);
  }

  public String messageKey() {
    return messageKey;
  }

  public String field() {
    return field;
  }

  public String[] args() {
    return args.clone();
  }

  /** Shows this refusal on a form: next to its field, or at the top of the form. */
  public void addTo(BindingResult binding) {
    if (field != null) {
      binding.rejectValue(field, messageKey, args, messageKey);
    } else {
      binding.reject(messageKey, args, messageKey);
    }
  }
}
