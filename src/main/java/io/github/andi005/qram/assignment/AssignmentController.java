package io.github.andi005.qram.assignment;

import io.github.andi005.qram.asset.Asset;
import io.github.andi005.qram.asset.AssetService;
import io.github.andi005.qram.common.BusinessRuleException;
import io.github.andi005.qram.common.Flash;
import io.github.andi005.qram.employee.EmployeeService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Assign an asset to an employee, and take it back. */
@Controller
class AssignmentController {

  private final AssignmentService assignmentService;
  private final AssetService assetService;
  private final EmployeeService employeeService;

  AssignmentController(
      AssignmentService assignmentService,
      AssetService assetService,
      EmployeeService employeeService) {
    this.assignmentService = assignmentService;
    this.assetService = assetService;
    this.employeeService = employeeService;
  }

  @GetMapping("/assets/{id}/assign")
  String assignForm(@PathVariable long id, Model model) {
    Asset asset = assetService.get(id);
    model.addAttribute("asset", asset);
    model.addAttribute("current", assignmentService.currentFor(id).orElse(null));
    model.addAttribute("employees", employeeService.findActive());
    return "assets/assign";
  }

  @PostMapping("/assets/{id}/assign")
  String assign(
      @PathVariable long id,
      @RequestParam(required = false) Long employeeId,
      @RequestParam(required = false) String note,
      RedirectAttributes redirect) {
    if (employeeId == null) {
      Flash.error(redirect, "assignment.error.chooseEmployee");
      return "redirect:/assets/" + id + "/assign";
    }
    try {
      Assignment a = assignmentService.assign(id, employeeId, note);
      Flash.success(
          redirect, "assignment.assigned", a.getAsset().getTag(), a.getEmployee().getFullName());
      return "redirect:/assets/" + id;
    } catch (BusinessRuleException e) {
      Flash.error(redirect, e);
    } catch (DataIntegrityViolationException e) {
      // Two people assigned the same asset at the same moment: the database index refused one.
      Flash.error(redirect, "assignment.error.concurrent");
    }
    return "redirect:/assets/" + id + "/assign";
  }

  @PostMapping("/assets/{id}/return")
  String returnAsset(@PathVariable long id, RedirectAttributes redirect) {
    try {
      Assignment a = assignmentService.returnAsset(id);
      Flash.success(
          redirect, "assignment.returned", a.getAsset().getTag(), a.getEmployee().getFullName());
    } catch (BusinessRuleException e) {
      Flash.error(redirect, e);
    }
    return "redirect:/assets/" + id;
  }
}
