package io.github.andi005.qram.importer;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {

  List<ImportBatch> findTop10ByOrderByImportedAtDesc();
}
