package io.github.andi005.qram.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** What the employee form sends. */
public record EmployeeForm(
    @NotBlank @Size(max = 120) String fullName,
    @Email @Size(max = 160) String email,
    @Size(max = 120) String department) {

  public static EmployeeForm empty() {
    return new EmployeeForm("", "", "");
  }

  public static EmployeeForm from(Employee employee) {
    return new EmployeeForm(employee.getFullName(), employee.getEmail(), employee.getDepartment());
  }
}
