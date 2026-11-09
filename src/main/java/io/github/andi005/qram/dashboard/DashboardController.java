package io.github.andi005.qram.dashboard;

import io.github.andi005.qram.asset.AssetService;
import io.github.andi005.qram.asset.AssetStatus;
import io.github.andi005.qram.assignment.AssignmentService;
import io.github.andi005.qram.history.ChangeLogService;
import io.github.andi005.qram.license.LicenseService;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** The start page after login, and the "expiring within 90 days" view. */
@Controller
class DashboardController {

  private final AssetService assetService;
  private final AssignmentService assignmentService;
  private final LicenseService licenseService;
  private final ExpiryService expiryService;
  private final ChangeLogService changeLog;

  DashboardController(
      AssetService assetService,
      AssignmentService assignmentService,
      LicenseService licenseService,
      ExpiryService expiryService,
      ChangeLogService changeLog) {
    this.assetService = assetService;
    this.assignmentService = assignmentService;
    this.licenseService = licenseService;
    this.expiryService = expiryService;
    this.changeLog = changeLog;
  }

  @GetMapping("/")
  String dashboard(Model model) {
    Map<AssetStatus, Long> counts = assetService.countByStatus();
    model.addAttribute("inRepair", counts.get(AssetStatus.IN_REPAIR));
    model.addAttribute("totalAssets", counts.values().stream().mapToLong(Long::longValue).sum());
    model.addAttribute("assigned", assignmentService.countOpen());
    model.addAttribute("licenseCount", licenseService.count());
    model.addAttribute("expiring", expiryService.expiring());
    model.addAttribute("recent", changeLog.recent(0, 8).getContent());
    return "dashboard";
  }

  @GetMapping("/expiring")
  String expiring(Model model) {
    model.addAttribute("expiring", expiryService.expiring());
    return "expiring";
  }
}
