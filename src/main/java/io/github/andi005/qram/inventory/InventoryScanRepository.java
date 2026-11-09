package io.github.andi005.qram.inventory;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryScanRepository extends JpaRepository<InventoryScan, Long> {

  List<InventoryScan> findBySessionIdOrderByScannedAtAscIdAsc(Long sessionId);

  @Query(
      "select s.asset.id from InventoryScan s where s.session.id = :sessionId and s.asset is not null")
  List<Long> scannedAssetIds(@Param("sessionId") Long sessionId);
}
