package io.github.andi005.qram.inventory;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventorySessionRepository extends JpaRepository<InventorySession, Long> {

  List<InventorySession> findAllByOrderByStartedAtDesc();

  /** Row lock, so two phones scanning into one session can't both record a "first" scan. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from InventorySession s where s.id = :id")
  Optional<InventorySession> findForUpdate(@Param("id") Long id);
}
