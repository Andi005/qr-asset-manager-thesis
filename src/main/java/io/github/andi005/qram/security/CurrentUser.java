package io.github.andi005.qram.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** The username of whoever is making the current request, for "who did it" in the history. */
@Component
public class CurrentUser {

  public String username() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth == null ? "system" : auth.getName();
  }
}
