package io.github.andi005.qram.history;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangeLogRepository extends JpaRepository<ChangeLogEntry, Long> {

  List<ChangeLogEntry> findByEntityTypeAndEntityIdOrderByAtDescIdDesc(
      EntityType entityType, Long entityId);

  Page<ChangeLogEntry> findAllByOrderByAtDescIdDesc(Pageable pageable);
}
