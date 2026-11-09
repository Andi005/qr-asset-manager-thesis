package io.github.andi005.qram.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** What an ADMIN fills in to create a user. */
public record UserForm(
    @NotBlank @Size(max = 60) @Pattern(regexp = "[A-Za-z0-9._-]+") String username,
    @NotBlank @Size(max = 120) String displayName,
    @NotNull Role role,
    @NotBlank @Size(min = 8, max = 72) String password) {

  public static UserForm empty() {
    return new UserForm("", "", Role.VIEWER, "");
  }
}
