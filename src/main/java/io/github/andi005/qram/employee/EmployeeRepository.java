package io.github.andi005.qram.employee;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

  List<Employee> findAllByOrderByActiveDescFullNameAsc();

  List<Employee> findByActiveTrueOrderByFullNameAsc();

  boolean existsByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

  /** True while the employee holds an asset or a license seat. */
  @Query(
      value =
          """
          select exists(select 1 from assignment where employee_id = :id and returned_at is null)
              or exists(select 1 from license_allocation where employee_id = :id and released_at is null)
          """,
      nativeQuery = true)
  boolean holdsAnything(@Param("id") long id);
}
