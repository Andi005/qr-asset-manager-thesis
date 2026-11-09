package io.github.andi005.qram.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Tells Spring Security how to find a user at login: by username, in the app_user table. */
@Service
class DatabaseUserDetailsService implements UserDetailsService {

  private final AppUserRepository users;

  DatabaseUserDetailsService(AppUserRepository users) {
    this.users = users;
  }

  @Override
  public UserDetails loadUserByUsername(String username) {
    AppUser user =
        users
            .findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new UsernameNotFoundException(username));
    return User.withUsername(user.getUsername())
        .password(user.getPasswordHash())
        .roles(user.getRole().name())
        .disabled(!user.isActive())
        .build();
  }
}
