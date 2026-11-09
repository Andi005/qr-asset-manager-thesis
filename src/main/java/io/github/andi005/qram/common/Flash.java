package io.github.andi005.qram.common;

import java.io.Serializable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * A one-time message shown at the top of the next page after a redirect (Post/Redirect/Get). The
 * layout renders it with {@code #messages.msgWithParams(key, args)}.
 *
 * @param type a Bootstrap alert type: success, danger, warning
 * @param key a key in messages.properties
 * @param args values for the {0}, {1} placeholders in the message
 */
public record Flash(String type, String key, String... args) implements Serializable {

  public static void success(RedirectAttributes redirect, String key, Object... args) {
    redirect.addFlashAttribute("flash", new Flash("success", key, strings(args)));
  }

  public static void error(RedirectAttributes redirect, String key, Object... args) {
    redirect.addFlashAttribute("flash", new Flash("danger", key, strings(args)));
  }

  public static void error(RedirectAttributes redirect, BusinessRuleException e) {
    redirect.addFlashAttribute("flash", new Flash("danger", e.messageKey(), e.args()));
  }

  private static String[] strings(Object[] args) {
    String[] result = new String[args.length];
    for (int i = 0; i < args.length; i++) {
      result[i] = String.valueOf(args[i]);
    }
    return result;
  }
}
