package io.github.andi005.qram.license;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LicenseRepository extends JpaRepository<SoftwareLicense, Long> {

  List<SoftwareLicense> findAllByOrderByNameAsc();

  /**
   * Loads the license with a row lock (SELECT ... FOR UPDATE). Two people taking the last seat at
   * the same moment are served one after the other, so the seat count can't be exceeded.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select l from SoftwareLicense l where l.id = :id")
  Optional<SoftwareLicense> findForUpdate(@Param("id") Long id);

  @Query(
      "select l from SoftwareLicense l where l.expiryDate between :from and :to order by l.expiryDate, l.name")
  List<SoftwareLicense> expiringBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

  @Query(
      "select l from SoftwareLicense l where l.expiryDate < :day order by l.expiryDate desc, l.name")
  List<SoftwareLicense> expiredBefore(@Param("day") LocalDate day);
}
