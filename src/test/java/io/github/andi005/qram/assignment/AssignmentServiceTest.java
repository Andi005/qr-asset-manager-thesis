package io.github.andi005.qram.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.andi005.qram.asset.Asset;
import io.github.andi005.qram.asset.AssetService;
import io.github.andi005.qram.asset.AssetStatus;
import io.github.andi005.qram.common.BusinessRuleException;
import io.github.andi005.qram.employee.Employee;
import io.github.andi005.qram.employee.EmployeeService;
import io.github.andi005.qram.history.ChangeLogService;
import io.github.andi005.qram.history.EntityType;
import io.github.andi005.qram.security.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** The assignment rules (FR-05, FR-06), without a database: the repository is a mock. */
class AssignmentServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-09T09:00:00Z");

  private final AssignmentRepository assignments = mock(AssignmentRepository.class);
  private final AssetService assetService = mock(AssetService.class);
  private final EmployeeService employeeService = mock(EmployeeService.class);
  private final ChangeLogService changeLog = mock(ChangeLogService.class);
  private final CurrentUser currentUser = mock(CurrentUser.class);
  private final AssignmentService service =
      new AssignmentService(
          assignments,
          assetService,
          employeeService,
          changeLog,
          currentUser,
          Clock.fixed(NOW, ZoneOffset.UTC));

  private Asset asset;
  private Employee employee;

  @BeforeEach
  void setUp() {
    asset = new Asset("IT-000001", NOW);
    ReflectionTestUtils.setField(asset, "id", 1L);
    employee = new Employee("Drita Krasniqi", null, null, NOW);
    ReflectionTestUtils.setField(employee, "id", 2L);
    when(assetService.get(1L)).thenReturn(asset);
    when(employeeService.get(2L)).thenReturn(employee);
    when(currentUser.username()).thenReturn("it");
    when(assignments.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  @DisplayName("An available asset is assigned, and the change log records it")
  void assigns() {
    Assignment a = service.assign(1L, 2L, "  for home office ");

    assertThat(a.getAsset()).isSameAs(asset);
    assertThat(a.getEmployee()).isSameAs(employee);
    assertThat(a.getAssignedAt()).isEqualTo(NOW);
    assertThat(a.getAssignedBy()).isEqualTo("it");
    assertThat(a.getNote()).isEqualTo("for home office");
    assertThat(a.isOpen()).isTrue();
    verify(changeLog).record(eq(EntityType.ASSET), eq(1L), eq("ASSIGNED"), anyString());
  }

  @Test
  @DisplayName("An asset in repair can't be assigned")
  void refusesInRepair() {
    asset.setStatus(AssetStatus.IN_REPAIR);
    assertRefused("assignment.error.status");
  }

  @Test
  @DisplayName("A retired asset can't be assigned")
  void refusesRetired() {
    asset.setStatus(AssetStatus.RETIRED);
    assertRefused("assignment.error.status");
  }

  @Test
  @DisplayName("An inactive employee can't receive an asset")
  void refusesInactiveEmployee() {
    employee.setActive(false);
    assertRefused("assignment.error.inactiveEmployee");
  }

  @Test
  @DisplayName("An asset that already has a holder can't be assigned again")
  void refusesSecondHolder() {
    Employee other = new Employee("Arben Gashi", null, null, NOW);
    when(assignments.findByAssetIdAndReturnedAtIsNull(1L))
        .thenReturn(Optional.of(new Assignment(asset, other, NOW, "it", null)));
    assertRefused("assignment.error.alreadyAssigned");
  }

  @Test
  @DisplayName("Returning closes the open assignment with time and user")
  void returns() {
    Assignment open = new Assignment(asset, employee, NOW.minusSeconds(3600), "it", null);
    when(assignments.findByAssetIdAndReturnedAtIsNull(1L)).thenReturn(Optional.of(open));

    Assignment closed = service.returnAsset(1L);

    assertThat(closed.isOpen()).isFalse();
    assertThat(closed.getReturnedAt()).isEqualTo(NOW);
    assertThat(closed.getReturnedBy()).isEqualTo("it");
    verify(changeLog).record(EntityType.ASSET, 1L, "RETURNED", "Drita Krasniqi");
  }

  @Test
  @DisplayName("Returning an asset nobody holds is refused")
  void returnWithoutHolder() {
    assertThatThrownBy(() -> service.returnAsset(1L))
        .isInstanceOf(BusinessRuleException.class)
        .extracting(e -> ((BusinessRuleException) e).messageKey())
        .isEqualTo("assignment.error.notAssigned");
  }

  private void assertRefused(String key) {
    assertThatThrownBy(() -> service.assign(1L, 2L, null))
        .isInstanceOf(BusinessRuleException.class)
        .extracting(e -> ((BusinessRuleException) e).messageKey())
        .isEqualTo(key);
    verify(assignments, never()).save(any());
    verify(changeLog, never()).record(any(), any(), anyString(), anyString());
  }
}
