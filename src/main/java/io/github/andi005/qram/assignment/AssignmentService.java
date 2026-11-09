package io.github.andi005.qram.assignment;

import io.github.andi005.qram.asset.Asset;
import io.github.andi005.qram.asset.AssetService;
import io.github.andi005.qram.common.BusinessRuleException;
import io.github.andi005.qram.employee.Employee;
import io.github.andi005.qram.employee.EmployeeService;
import io.github.andi005.qram.history.ChangeLogService;
import io.github.andi005.qram.history.EntityType;
import io.github.andi005.qram.security.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Giving an asset to an employee and taking it back. The rules:
 *
 * <ul>
 *   <li>an asset has at most one holder at a time (also enforced by a partial unique index);
 *   <li>retired or in-repair assets can't be assigned;
 *   <li>inactive employees can't receive assets.
 * </ul>
 *
 * Each change and its change-log line are written in one transaction: both or neither.
 */
@Service
public class AssignmentService {

  private final AssignmentRepository assignments;
  private final AssetService assetService;
  private final EmployeeService employeeService;
  private final ChangeLogService changeLog;
  private final CurrentUser currentUser;
  private final Clock clock;

  public AssignmentService(
      AssignmentRepository assignments,
      AssetService assetService,
      EmployeeService employeeService,
      ChangeLogService changeLog,
      CurrentUser currentUser,
      Clock clock) {
    this.assignments = assignments;
    this.assetService = assetService;
    this.employeeService = employeeService;
    this.changeLog = changeLog;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  @PreAuthorize("hasRole('IT_STAFF')")
  @Transactional
  public Assignment assign(long assetId, long employeeId, String note) {
    Asset asset = assetService.get(assetId);
    Employee employee = employeeService.get(employeeId);
    if (!asset.getStatus().canBeAssigned()) {
      throw new BusinessRuleException("assignment.error.status", asset.getTag());
    }
    if (!employee.isActive()) {
      throw new BusinessRuleException("assignment.error.inactiveEmployee", employee.getFullName());
    }
    Optional<Assignment> open = assignments.findByAssetIdAndReturnedAtIsNull(assetId);
    if (open.isPresent()) {
      throw new BusinessRuleException(
          "assignment.error.alreadyAssigned",
          asset.getTag(),
          open.get().getEmployee().getFullName());
    }
    String cleanNote = note == null || note.isBlank() ? null : note.strip();
    Assignment assignment =
        assignments.save(
            new Assignment(asset, employee, clock.instant(), currentUser.username(), cleanNote));
    changeLog.record(
        EntityType.ASSET,
        assetId,
        "ASSIGNED",
        employee.getFullName() + (cleanNote == null ? "" : " (" + cleanNote + ")"));
    return assignment;
  }

  @PreAuthorize("hasRole('IT_STAFF')")
  @Transactional
  public Assignment returnAsset(long assetId) {
    Asset asset = assetService.get(assetId);
    Assignment open =
        assignments
            .findByAssetIdAndReturnedAtIsNull(assetId)
            .orElseThrow(
                () -> new BusinessRuleException("assignment.error.notAssigned", asset.getTag()));
    open.close(clock.instant(), currentUser.username());
    changeLog.record(EntityType.ASSET, assetId, "RETURNED", open.getEmployee().getFullName());
    return open;
  }

  @Transactional(readOnly = true)
  public Optional<Assignment> currentFor(long assetId) {
    return assignments.findByAssetIdAndReturnedAtIsNull(assetId);
  }

  /** Who held the asset at a given moment ("who had X on date D?"). */
  @Transactional(readOnly = true)
  public Optional<Assignment> holderAt(long assetId, Instant at) {
    return assignments.holderAt(assetId, at);
  }

  /** Newest first. */
  @Transactional(readOnly = true)
  public List<Assignment> historyForAsset(long assetId) {
    return assignments.findByAssetIdOrderByAssignedAtDescIdDesc(assetId);
  }

  /** Newest first. */
  @Transactional(readOnly = true)
  public List<Assignment> historyForEmployee(long employeeId) {
    return assignments.findByEmployeeIdOrderByAssignedAtDescIdDesc(employeeId);
  }

  /** Asset id → name of the current holder, for the assets that have one. */
  @Transactional(readOnly = true)
  public Map<Long, String> currentHolderNames(Collection<Long> assetIds) {
    if (assetIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, String> result = new HashMap<>();
    for (Assignment a : assignments.findByAssetIdInAndReturnedAtIsNull(assetIds)) {
      result.put(a.getAsset().getId(), a.getEmployee().getFullName());
    }
    return result;
  }

  /** Employee id → number of assets they hold now. */
  @Transactional(readOnly = true)
  public Map<Long, Long> openCountsPerEmployee() {
    return assignments.openCountsPerEmployee().stream()
        .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
  }

  @Transactional(readOnly = true)
  public long countOpen() {
    return assignments.countByReturnedAtIsNull();
  }
}
