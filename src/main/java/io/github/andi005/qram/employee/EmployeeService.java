package io.github.andi005.qram.employee;

import io.github.andi005.qram.common.BusinessRuleException;
import io.github.andi005.qram.common.Labels;
import io.github.andi005.qram.common.NotFoundException;
import io.github.andi005.qram.history.ChangeLogService;
import io.github.andi005.qram.history.EntityType;
import io.github.andi005.qram.history.FieldChanges;
import java.time.Clock;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Employees: create, edit, deactivate (only when they hold nothing), reactivate. */
@Service
public class EmployeeService {

  private final EmployeeRepository employees;
  private final ChangeLogService changeLog;
  private final Labels labels;
  private final Clock clock;

  public EmployeeService(
      EmployeeRepository employees, ChangeLogService changeLog, Labels labels, Clock clock) {
    this.employees = employees;
    this.changeLog = changeLog;
    this.labels = labels;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<Employee> findAll() {
    return employees.findAllByOrderByActiveDescFullNameAsc();
  }

  @Transactional(readOnly = true)
  public List<Employee> findActive() {
    return employees.findByActiveTrueOrderByFullNameAsc();
  }

  @Transactional(readOnly = true)
  public Employee get(long id) {
    return employees.findById(id).orElseThrow(() -> new NotFoundException("Employee " + id));
  }

  @PreAuthorize("hasRole('IT_STAFF')")
  @Transactional
  public Employee create(EmployeeForm form) {
    String email = blankToNull(form.email());
    if (email != null && employees.existsByEmailIgnoreCase(email)) {
      throw BusinessRuleException.onField("email", "employee.error.emailTaken");
    }
    Employee employee =
        employees.save(
            new Employee(
                form.fullName().strip(), email, blankToNull(form.department()), clock.instant()));
    changeLog.record(EntityType.EMPLOYEE, employee.getId(), "CREATED", employee.getFullName());
    return employee;
  }

  @PreAuthorize("hasRole('IT_STAFF')")
  @Transactional
  public Employee update(long id, EmployeeForm form) {
    Employee employee = get(id);
    String email = blankToNull(form.email());
    if (email != null && employees.existsByEmailIgnoreCaseAndIdNot(email, id)) {
      throw BusinessRuleException.onField("email", "employee.error.emailTaken");
    }
    FieldChanges changes =
        new FieldChanges()
            .add(labels.get("employee.fullName"), employee.getFullName(), form.fullName().strip())
            .add(labels.get("employee.email"), employee.getEmail(), email)
            .add(
                labels.get("employee.department"),
                employee.getDepartment(),
                blankToNull(form.department()));
    employee.setFullName(form.fullName().strip());
    employee.setEmail(email);
    employee.setDepartment(blankToNull(form.department()));
    if (!changes.isEmpty()) {
      changeLog.record(EntityType.EMPLOYEE, id, "UPDATED", changes.toString());
    }
    return employee;
  }

  @PreAuthorize("hasRole('IT_STAFF')")
  @Transactional
  public void setActive(long id, boolean active) {
    Employee employee = get(id);
    if (employee.isActive() == active) {
      return;
    }
    if (!active && employees.holdsAnything(id)) {
      throw new BusinessRuleException("employee.error.returnFirst");
    }
    employee.setActive(active);
    changeLog.record(
        EntityType.EMPLOYEE, id, active ? "ACTIVATED" : "DEACTIVATED", employee.getFullName());
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text.strip();
  }
}
