package io.github.andi005.qram.assignment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

  Optional<Assignment> findByAssetIdAndReturnedAtIsNull(Long assetId);

  List<Assignment> findByAssetIdOrderByAssignedAtDescIdDesc(Long assetId);

  List<Assignment> findByEmployeeIdOrderByAssignedAtDescIdDesc(Long employeeId);

  List<Assignment> findByAssetIdInAndReturnedAtIsNull(Collection<Long> assetIds);

  long countByReturnedAtIsNull();

  @Query(
      "select a.employee.id, count(a) from Assignment a where a.returnedAt is null group by a.employee.id")
  List<Object[]> openCountsPerEmployee();

  /**
   * Who held the asset at a given moment: the assignment that started at or before that moment and
   * was not yet returned then.
   */
  @Query(
      """
      select a from Assignment a
      where a.asset.id = :assetId
        and a.assignedAt <= :at
        and (a.returnedAt is null or a.returnedAt > :at)
      """)
  Optional<Assignment> holderAt(@Param("assetId") Long assetId, @Param("at") Instant at);
}
