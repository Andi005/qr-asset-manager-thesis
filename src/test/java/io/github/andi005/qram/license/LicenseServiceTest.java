package io.github.andi005.qram.license;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.andi005.qram.asset.Asset;
import io.github.andi005.qram.asset.AssetService;
import io.github.andi005.qram.asset.AssetStatus;
import io.github.andi005.qram.common.BusinessRuleException;
import io.github.andi005.qram.common.Labels;
import io.github.andi005.qram.employee.Employee;
import io.github.andi005.qram.employee.EmployeeService;
import io.github.andi005.qram.history.ChangeLogService;
import io.github.andi005.qram.security.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Seat limits and allocation rules (FR-09). */
class LicenseServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-09T09:00:00Z");

  private final LicenseRepository licenses = mock(LicenseRepository.class);
  private final LicenseAllocationRepository allocations = mock(LicenseAllocationRepository.class);
  private final EmployeeService employeeService = mock(EmployeeService.class);
  private final AssetService assetService = mock(AssetService.class);
  private final ChangeLogService changeLog = mock(ChangeLogService.class);
  private final CurrentUser currentUser = mock(CurrentUser.class);
  private final Labels labels = mock(Labels.class);
  private final LicenseService service =
      new LicenseService(
          licenses,
          allocations,
          employeeService,
          assetService,
          changeLog,
          currentUser,
          labels,
          Clock.fixed(NOW, ZoneOffset.UTC));

  private SoftwareLicense license;
  private Employee employee;

  @BeforeEach
  void setUp() {
    license = new SoftwareLicense("Microsoft 365", 2, NOW);
    ReflectionTestUtils.setField(license, "id", 10L);
    ReflectionTestUtils.setField(license, "version", 0);
    employee = new Employee("Era Kelmendi", null, null, NOW);
    ReflectionTestUtils.setField(employee, "id", 2L);
    when(licenses.findForUpdate(10L)).thenReturn(Optional.of(license));
    when(employeeService.get(2L)).thenReturn(employee);
    when(currentUser.username()).thenReturn("it");
    when(labels.get(anyString())).thenAnswer(inv -> inv.getArgument(0));
    when(allocations.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  private void seatsInUse(long used) {
    when(allocations.countByLicenseIdAndReleasedAtIsNull(10L)).thenReturn(used);
  }

  @Test
  @DisplayName("A free seat is given to an employee (under a row lock)")
  void allocatesFreeSeat() {
    seatsInUse(1);
    LicenseAllocation a = service.allocateToEmployee(10L, 2L);
    assertThat(a.getEmployee()).isSameAs(employee);
    assertThat(a.getAsset()).isNull();
    assertThat(a.isOpen()).isTrue();
    verify(licenses).findForUpdate(10L);
  }

  @Test
  @DisplayName("When all seats are in use, no more are given out")
  void refusesWhenFull() {
    seatsInUse(2);
    assertRefused(() -> service.allocateToEmployee(10L, 2L), "license.error.noFreeSeat");
    verify(allocations, never()).save(any());
  }

  @Test
  @DisplayName("After a release, the freed seat can be given again")
  void releaseThenReallocate() {
    LicenseAllocation held = LicenseAllocation.toEmployee(license, employee, NOW, "it");
    ReflectionTestUtils.setField(held, "id", 5L);
    when(allocations.findById(5L)).thenReturn(Optional.of(held));
    service.release(10L, 5L);
    assertThat(held.isOpen()).isFalse();
    assertThat(held.getReleasedAt()).isEqualTo(NOW);

    seatsInUse(1);
    assertThat(service.allocateToEmployee(10L, 2L).isOpen()).isTrue();
  }

  @Test
  @DisplayName("Releasing twice is refused")
  void releaseTwice() {
    LicenseAllocation held = LicenseAllocation.toEmployee(license, employee, NOW, "it");
    held.release(NOW, "it");
    when(allocations.findById(5L)).thenReturn(Optional.of(held));
    assertRefused(() -> service.release(10L, 5L), "license.error.alreadyReleased");
  }

  @Test
  @DisplayName("The same employee can't take a second seat of the same license")
  void oneSeatPerHolder() {
    seatsInUse(1);
    when(allocations.existsByLicenseIdAndEmployeeIdAndReleasedAtIsNull(10L, 2L)).thenReturn(true);
    assertRefused(() -> service.allocateToEmployee(10L, 2L), "license.error.alreadyHolds");
  }

  @Test
  @DisplayName("Inactive employees can't receive seats")
  void inactiveEmployee() {
    seatsInUse(0);
    employee.setActive(false);
    assertRefused(() -> service.allocateToEmployee(10L, 2L), "license.error.inactiveEmployee");
  }

  @Test
  @DisplayName("Retired assets can't receive seats")
  void retiredAsset() {
    seatsInUse(0);
    Asset asset = new Asset("IT-000009", NOW);
    asset.setStatus(AssetStatus.RETIRED);
    when(assetService.get(9L)).thenReturn(asset);
    assertRefused(() -> service.allocateToAsset(10L, 9L), "license.error.retiredAsset");
  }

  @Test
  @DisplayName("The seat count can't be lowered below the seats in use")
  void seatsBelowUsed() {
    seatsInUse(2);
    assertRefused(
        () -> service.update(10L, new LicenseForm("Microsoft 365", null, 1, null, null, 0)),
        "license.error.seatsBelowUsed");
    assertThat(license.getSeatsTotal()).isEqualTo(2);
  }

  @Test
  @DisplayName("Lowering to exactly the seats in use is allowed")
  void seatsEqualToUsed() {
    seatsInUse(2);
    service.update(10L, new LicenseForm("Microsoft 365", null, 2, null, null, 0));
    assertThat(license.getSeatsTotal()).isEqualTo(2);
  }

  @Test
  @DisplayName("An edit based on an old version is refused")
  void staleVersion() {
    seatsInUse(0);
    assertRefused(
        () -> service.update(10L, new LicenseForm("X", null, 5, null, null, 3)),
        "license.error.concurrentEdit");
  }

  private static void assertRefused(Runnable action, String key) {
    assertThatThrownBy(action::run)
        .isInstanceOf(BusinessRuleException.class)
        .extracting(e -> ((BusinessRuleException) e).messageKey())
        .isEqualTo(key);
  }
}
