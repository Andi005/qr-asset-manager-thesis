package io.github.andi005.qram.security;

import io.github.andi005.qram.common.BusinessRuleException;
import io.github.andi005.qram.common.NotFoundException;
import io.github.andi005.qram.history.ChangeLogService;
import io.github.andi005.qram.history.EntityType;
import java.time.Clock;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** User management (ADMIN only). Users are deactivated, never deleted. */
@Service
public class UserService {

  private final AppUserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final ChangeLogService changeLog;
  private final Clock clock;

  public UserService(
      AppUserRepository users,
      PasswordEncoder passwordEncoder,
      ChangeLogService changeLog,
      Clock clock) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.changeLog = changeLog;
    this.clock = clock;
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Transactional(readOnly = true)
  public List<AppUser> findAll() {
    return users.findAllByOrderByUsernameAsc();
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public AppUser create(UserForm form) {
    if (users.existsByUsernameIgnoreCase(form.username())) {
      throw BusinessRuleException.onField("username", "user.error.usernameTaken");
    }
    AppUser user =
        users.save(
            new AppUser(
                form.username().strip(),
                passwordEncoder.encode(form.password()),
                form.displayName().strip(),
                form.role(),
                clock.instant()));
    changeLog.record(
        EntityType.USER, user.getId(), "CREATED", user.getUsername() + " (" + user.getRole() + ")");
    return user;
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public void changeRole(long id, Role role) {
    AppUser user = get(id);
    if (user.getRole() == Role.ADMIN && role != Role.ADMIN) {
      requireAnotherActiveAdmin(user);
    }
    Role old = user.getRole();
    user.setRole(role);
    changeLog.record(EntityType.USER, id, "ROLE_CHANGED", old + " → " + role);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public void setActive(long id, boolean active) {
    AppUser user = get(id);
    if (!active && user.getRole() == Role.ADMIN) {
      requireAnotherActiveAdmin(user);
    }
    user.setActive(active);
    changeLog.record(EntityType.USER, id, active ? "ACTIVATED" : "DEACTIVATED", user.getUsername());
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public void resetPassword(long id, String newPassword) {
    if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 72) {
      throw new BusinessRuleException("user.error.passwordLength");
    }
    AppUser user = get(id);
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    changeLog.record(EntityType.USER, id, "PASSWORD_RESET", user.getUsername());
  }

  private void requireAnotherActiveAdmin(AppUser user) {
    if (user.isActive() && users.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
      throw new BusinessRuleException("user.error.lastAdmin");
    }
  }

  private AppUser get(long id) {
    return users.findById(id).orElseThrow(() -> new NotFoundException("User " + id));
  }
}
