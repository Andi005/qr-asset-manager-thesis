package io.github.andi005.qram.security;

/**
 * Who may do what. Each role includes the ones below it (see the role hierarchy in {@link
 * SecurityConfig}).
 *
 * <ul>
 *   <li>ADMIN: everything, plus user management and CSV import.
 *   <li>IT_STAFF: manage assets, employees, licenses, assignments, labels and inventories.
 *   <li>VIEWER: read-only: lists, details, dashboard, expiry view, scanning to look up, exports.
 * </ul>
 */
public enum Role {
  ADMIN,
  IT_STAFF,
  VIEWER
}
