package io.github.andi005.qram;

import io.github.andi005.qram.asset.Asset;
import io.github.andi005.qram.asset.AssetCategory;
import io.github.andi005.qram.asset.AssetForm;
import io.github.andi005.qram.asset.AssetService;
import io.github.andi005.qram.asset.AssetStatus;
import io.github.andi005.qram.employee.Employee;
import io.github.andi005.qram.employee.EmployeeForm;
import io.github.andi005.qram.employee.EmployeeService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Creates records for integration tests through the real services (so all rules apply). */
@Component
public class TestData {

  private final AssetService assetService;
  private final EmployeeService employeeService;

  public TestData(AssetService assetService, EmployeeService employeeService) {
    this.assetService = assetService;
    this.employeeService = employeeService;
  }

  /** Runs the following service calls as an ADMIN named "tester". */
  public static void loginAsAdmin() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "tester", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
  }

  public Asset asset() {
    return asset(AssetStatus.IN_STOCK);
  }

  public Asset asset(AssetStatus status) {
    return assetService.create(
        new AssetForm(
            AssetCategory.LAPTOP,
            "Lenovo",
            "ThinkPad T14",
            "SN" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            null,
            null,
            null,
            status,
            null,
            null));
  }

  public Employee employee() {
    return employeeService.create(
        new EmployeeForm("Test " + UUID.randomUUID().toString().substring(0, 6), null, null));
  }
}
