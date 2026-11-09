package io.github.andi005.qram.license;

import io.github.andi005.qram.asset.Asset;
import io.github.andi005.qram.asset.AssetService;
import io.github.andi005.qram.asset.AssetStatus;
import io.github.andi005.qram.common.BusinessRuleException;
import io.github.andi005.qram.common.ExpiryWindow;
import io.github.andi005.qram.common.Labels;
import io.github.andi005.qram.common.NotFoundException;
import io.github.andi005.qram.employee.Employee;
import io.github.andi005.qram.employee.EmployeeService;
import io.github.andi005.qram.history.ChangeLogService;
import io.github.andi005.qram.history.EntityType;
import io.github.andi005.qram.history.FieldChanges;
import io.github.andi005.qram.security.CurrentUser;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Licenses and their seats. The rules:
 *
 * <ul>
 *   <li>never more seats in use than {@code seatsTotal} (checked under a row lock);
 *   <li>{@code seatsTotal} can't be lowered below the seats in use;
 *   <li>a seat goes to an employee or an asset, and the same holder can't take two seats of one
 *       license;
 *   <li>inactive employees and retired assets can't receive seats.
 * </ul>
 */
@Service
public class LicenseService {

  private final LicenseRepository licenses;
  private final LicenseAllocationRepository allocations;
  private final EmployeeService employeeService;
  private final AssetService assetService;
  private final ChangeLogService changeLog;
  private final CurrentUser currentUser;
  private final Labels labels;
  private final Clock clock;

  public LicenseService(
      LicenseRepository licenses,
      LicenseAllocationRepository allocations,
      EmployeeService employeeService,
      AssetService assetService,
      ChangeLogService changeLog,
      CurrentUser currentUser,
      Labels labels,
      Clock clock) {
    this.licenses = licenses;
    this.allocations = allocations;
    this.employeeService = employeeService;
    this.assetService = assetService;
    this.changeLog = changeLog;
    this.currentUser = currentUser;
    this.labels = labels;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<LicenseSummary> findAll() {
    Map<Long, Long> used = usedSeats();
    LocalDate today = LocalDate.now(clock);
    return licenses.findAllByOrderByNameAsc().stream()
        .map(l ->
            new LicenseSummary(
                l,
                used.getOrDefault(l.getId(), 0L),
                ExpiryWindow.classify(today, l.getExpiryDate())))
        .toList();
  }

  @Transactional(readOnly = true)
  public LicenseSummary summary(long id) {
    SoftwareLicense license = get(id);
    return new LicenseSummary(
        license,
        allocations.countByLicenseIdAndReleasedAtIsNull(id),
        ExpiryWindow.classify(LocalDate.now(clock), license.getExpiryDate()));
  }

  @Transactional(readOnly = true)
  public SoftwareLicense get(long id) {
    return licenses.findById(id).orElseThrow(() -> new NotFoundException("License " + id));
  }

  /** All seats ever given out for this license, newest first. */
  @Transactional(readOnly = true)
  public List<LicenseAllocation> allocations(long licenseId) {
    return allocations.findByLicenseIdOrderByAllocatedAtDescIdDesc(licenseId);
  }

  @Transactional(readOnly = true)
  public List<LicenseAllocation> openAllocationsForEmployee(long employeeId) {
    return allocations.findByEmployeeIdAndReleasedAtIsNullOrderByAllocatedAtDesc(employeeId);
  }

  @Transactional(readOnly = true)
  public List<LicenseAllocation> openAllocationsForAsset(long assetId) {
    return allocations.findByAssetIdAndReleasedAtIsNullOrderByAllocatedAtDesc(assetId);
  }

  @Transactional(readOnly = true)
  public List<SoftwareLicense> expiringBetween(LocalDate from, LocalDate to) {
    return licenses.expiringBetween(from, to);
  }

  @Transactional(readOnly = true)
  public List<SoftwareLicense> expiredBefore(LocalDate day) {
    return licenses.expiredBefore(day);
  }

  @Transactional(readOnly = true)
  public long count() {
    return licenses.count();
  }

  @PreAuthorize("hasRole('IT_STAFF')")
  @Transactional
  public SoftwareLicense create(LicenseForm form) {
    SoftwareLicense license =
        new SoftwareLicense(form.name().strip(), form.seatsTotal(), clock.instant());
    apply(license, form);
    license = licenses.save(license);
    changeLog.record(
        EntityType.LICENSE,
        license.getId(),
        "CREATED",
        license.getName() + " (" + license.getSeatsTotal() + ")");
    return license;
  }

  @PreAuthorize("hasRole('IT_STAFF')")
  @Transactional
  public SoftwareLicense update(long id, LicenseForm form) {
    SoftwareLicense license =
        licenses.findForUpdate(id).orElseThrow(() -> new NotFoundException("License " + id));
    if (form.version() == null || !form.version().equals(license.getVersion())) {
      throw new BusinessRuleException("license.error.concurrentEdit");
    }
    long used = allocations.countByLicenseIdAndReleasedAtIsNull(id);
    if (form.seatsTotal() < used) {
      throw BusinessRuleException.onField("seatsTotal", "license.error.seatsBelowUsed", used);
    }
    FieldChanges changes =
        new FieldChanges()
            .add(labels.get("license.name"), license.getName(), form.name().strip())
            .add(labels.get("license.vendor"), license.getVendor(), blankToNull(form.vendor()))
            .add(labels.get("license.seatsTotal"), license.getSeatsTotal(), form.seatsTotal())
            .add(labels.get("license.expiryDate"), license.getExpiryDate(), form.expiryDate())
            .add(labels.get("license.notes"), license.getNotes(), blankToNull(form.notes()));
    license.setName(form.name().strip());
    license.setSeatsTotal(form.seatsTotal());
    apply(license, form);
    if (!changes.isEmpty()) {
      changeLog.record(EntityType.LICENSE, id, "UPDATED", changes.toString());
    }
    return license;
  }

  @PreAuthorize("hasRole('IT_STAFF')")
  @Transactional
  public LicenseAllocation allocateToEmployee(long licenseId, long employeeId) {
    SoftwareLicense license = lockWithFreeSeat(licenseId);
    Employee employee = employeeService.get(employeeId);
    if (!employee.isActive()) {
      throw new BusinessRuleException("license.error.inactiveEmployee", employee.getFullName());
    }
    if (allocations.existsByLicenseIdAndEmployeeIdAndReleasedAtIsNull(licenseId, employeeId)) {
      throw new BusinessRuleException("license.error.alreadyHolds", employee.getFullName());
    }
    LicenseAllocation allocation =
        allocations.save(
            LicenseAllocation.toEmployee(
                license, employee, clock.instant(), currentUser.username()));
    changeLog.record(EntityType.LICENSE, licenseId, "SEAT_ALLOCATED", employee.getFullName());
    return allocation;
  }

  @PreAuthorize("hasRole('IT_STAFF')")
  @Transactional
  public LicenseAllocation allocateToAsset(long licenseId, long assetId) {
    SoftwareLicense license = lockWithFreeSeat(licenseId);
    Asset asset = assetService.get(assetId);
    if (asset.getStatus() == AssetStatus.RETIRED) {
      throw new BusinessRuleException("license.error.retiredAsset", asset.getTag());
    }
    if (allocations.existsByLicenseIdAndAssetIdAndReleasedAtIsNull(licenseId, assetId)) {
      throw new BusinessRuleException("license.error.alreadyHolds", asset.getTag());
    }
    LicenseAllocation allocation =
        allocations.save(
            LicenseAllocation.toAsset(license, asset, clock.instant(), currentUser.username()));
    changeLog.record(EntityType.LICENSE, licenseId, "SEAT_ALLOCATED", asset.getTag());
    return allocation;
  }

  @PreAuthorize("hasRole('IT_STAFF')")
  @Transactional
  public LicenseAllocation release(long licenseId, long allocationId) {
    LicenseAllocation allocation =
        allocations
            .findById(allocationId)
            .filter(a -> a.getLicense().getId().equals(licenseId))
            .orElseThrow(() -> new NotFoundException("Allocation " + allocationId));
    if (!allocation.isOpen()) {
      throw new BusinessRuleException("license.error.alreadyReleased");
    }
    allocation.release(clock.instant(), currentUser.username());
    changeLog.record(EntityType.LICENSE, licenseId, "SEAT_RELEASED", allocation.getHolderName());
    return allocation;
  }

  private SoftwareLicense lockWithFreeSeat(long licenseId) {
    SoftwareLicense license =
        licenses
            .findForUpdate(licenseId)
            .orElseThrow(() -> new NotFoundException("License " + licenseId));
    long used = allocations.countByLicenseIdAndReleasedAtIsNull(licenseId);
    if (used >= license.getSeatsTotal()) {
      throw new BusinessRuleException("license.error.noFreeSeat", license.getName());
    }
    return license;
  }

  private void apply(SoftwareLicense license, LicenseForm form) {
    license.setVendor(blankToNull(form.vendor()));
    license.setExpiryDate(form.expiryDate());
    license.setNotes(blankToNull(form.notes()));
    license.setUpdatedAt(clock.instant());
  }

  private Map<Long, Long> usedSeats() {
    return allocations.usedSeatsPerLicense().stream()
        .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text.strip();
  }
}
