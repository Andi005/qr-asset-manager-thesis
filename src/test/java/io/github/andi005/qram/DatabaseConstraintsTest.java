package io.github.andi005.qram;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.andi005.qram.asset.Asset;
import io.github.andi005.qram.employee.Employee;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * The rules the database itself enforces (NFR-05), tested with raw SQL that bypasses the services:
 * even a bug in Java code can't break them.
 */
@IntegrationTest
@Transactional
class DatabaseConstraintsTest {

  @Autowired private JdbcTemplate jdbc;
  @Autowired private TestData testData;

  private Asset asset;
  private Employee employee;

  @BeforeEach
  void setUp() {
    TestData.loginAsAdmin();
    asset = testData.asset();
    employee = testData.employee();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Two assets can't have the same tag")
  void uniqueTag() {
    assertThatThrownBy(() ->
            jdbc.update(
                "insert into asset (asset_tag, category, model, status, created_at, updated_at)"
                    + " values (?, 'LAPTOP', 'x', 'IN_STOCK', now(), now())",
                asset.getTag()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("A tag must look like IT-000123")
  void tagFormat() {
    assertThatThrownBy(() ->
            jdbc.update(
                "insert into asset (asset_tag, category, model, status, created_at, updated_at)"
                    + " values ('it-26', 'LAPTOP', 'x', 'IN_STOCK', now(), now())"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("An unknown status is refused")
  void statusCheck() {
    assertThatThrownBy(
            () -> jdbc.update("update asset set status = 'ASSIGNED' where id = ?", asset.getId()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("An asset can't have two open assignments (partial unique index)")
  void oneOpenAssignmentPerAsset() {
    String insert =
        "insert into assignment (asset_id, employee_id, assigned_at, assigned_by)"
            + " values (?, ?, now(), 'test')";
    jdbc.update(insert, asset.getId(), employee.getId());
    assertThatThrownBy(() -> jdbc.update(insert, asset.getId(), employee.getId()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("A closed assignment does not block a new one")
  void closedAssignmentAllowsNewOne() {
    jdbc.update(
        "insert into assignment (asset_id, employee_id, assigned_at, assigned_by, returned_at)"
            + " values (?, ?, now() - interval '2 days', 'test', now() - interval '1 day')",
        asset.getId(),
        employee.getId());
    jdbc.update(
        "insert into assignment (asset_id, employee_id, assigned_at, assigned_by)"
            + " values (?, ?, now(), 'test')",
        asset.getId(),
        employee.getId());
  }

  private static final String INSERT_ALLOCATION =
      "insert into license_allocation (license_id, employee_id, asset_id, allocated_at,"
          + " allocated_by) values (?, ?, ?, now(), 'test')";

  private Long newLicense() {
    return jdbc.queryForObject(
        "insert into software_license (name, seats_total, created_at, updated_at)"
            + " values ('L', 3, now(), now()) returning id",
        Long.class);
  }

  @Test
  @DisplayName("A license seat can't go to an employee and an asset at once")
  void allocationNotBoth() {
    Long licenseId = newLicense();
    assertThatThrownBy(
            () -> jdbc.update(INSERT_ALLOCATION, licenseId, employee.getId(), asset.getId()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("A license seat must go to someone (employee or asset)")
  void allocationNotNeither() {
    Long licenseId = newLicense();
    assertThatThrownBy(() -> jdbc.update(INSERT_ALLOCATION, licenseId, null, null))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("The same employee can't hold two open seats of one license")
  void oneOpenSeatPerHolder() {
    Long licenseId = newLicense();
    jdbc.update(INSERT_ALLOCATION, licenseId, employee.getId(), null);
    assertThatThrownBy(() -> jdbc.update(INSERT_ALLOCATION, licenseId, employee.getId(), null))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("An asset with history can't be deleted (foreign keys RESTRICT)")
  void noDeleteWithHistory() {
    jdbc.update(
        "insert into assignment (asset_id, employee_id, assigned_at, assigned_by)"
            + " values (?, ?, now(), 'test')",
        asset.getId(),
        employee.getId());
    assertThatThrownBy(() -> jdbc.update("delete from employee where id = ?", employee.getId()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("Seats must be a positive number")
  void positiveSeats() {
    assertThatThrownBy(() ->
            jdbc.update(
                "insert into software_license (name, seats_total, created_at, updated_at)"
                    + " values ('L', 0, now(), now())"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
