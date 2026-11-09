package io.github.andi005.qram.security;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

  Optional<AppUser> findByUsernameIgnoreCase(String username);

  boolean existsByUsernameIgnoreCase(String username);

  List<AppUser> findAllByOrderByUsernameAsc();

  long countByRoleAndActiveTrue(Role role);
}
