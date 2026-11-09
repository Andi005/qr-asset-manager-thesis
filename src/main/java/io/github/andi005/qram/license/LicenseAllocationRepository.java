package io.github.andi005.qram.license;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LicenseAllocationRepository extends JpaRepository<LicenseAllocation, Long> {

  long countByLicenseIdAndReleasedAtIsNull(Long licenseId);

  List<LicenseAllocation> findByLicenseIdOrderByAllocatedAtDescIdDesc(Long licenseId);

  List<LicenseAllocation> findByEmployeeIdAndReleasedAtIsNullOrderByAllocatedAtDesc(
      Long employeeId);

  List<LicenseAllocation> findByAssetIdAndReleasedAtIsNullOrderByAllocatedAtDesc(Long assetId);

  boolean existsByLicenseIdAndEmployeeIdAndReleasedAtIsNull(Long licenseId, Long employeeId);

  boolean existsByLicenseIdAndAssetIdAndReleasedAtIsNull(Long licenseId, Long assetId);

  @Query(
      "select a.license.id, count(a) from LicenseAllocation a where a.releasedAt is null group by a.license.id")
  List<Object[]> usedSeatsPerLicense();
}
