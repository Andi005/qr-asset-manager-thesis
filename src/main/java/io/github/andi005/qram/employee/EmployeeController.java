package io.github.andi005.qram.employee;

import io.github.andi005.qram.assignment.AssignmentService;
import io.github.andi005.qram.common.BusinessRuleException;
import io.github.andi005.qram.common.Flash;
import io.github.andi005.qram.history.ChangeLogService;
import io.github.andi005.qram.history.EntityType;
import io.github.andi005.qram.license.LicenseService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Employee pages: list, details with "who had what", create, edit, (de)activate. */
@Controller
@RequestMapping("/employees")
class EmployeeController {

  private final EmployeeService employeeService;
  private final AssignmentService assignmentService;
  private final LicenseService licenseService;
  private final ChangeLogService changeLog;

  EmployeeController(
      EmployeeService employeeService,
      AssignmentService assignmentService,
      LicenseService licenseService,
      ChangeLogService changeLog) {
    this.employeeService = employeeService;
    this.assignmentService = assignmentService;
    this.licenseService = licenseService;
    this.changeLog = changeLog;
  }

  @GetMapping
  String list(Model model) {
    model.addAttribute("employees", employeeService.findAll());
    model.addAttribute("holdingCounts", assignmentService.openCountsPerEmployee());
    return "employees/list";
  }

  @GetMapping("/{id}")
  String detail(@PathVariable long id, Model model) {
    model.addAttribute("employee", employeeService.get(id));
    model.addAttribute("assignments", assignmentService.historyForEmployee(id));
    model.addAttribute("licenses", licenseService.openAllocationsForEmployee(id));
    model.addAttribute("changes", changeLog.forEntity(EntityType.EMPLOYEE, id));
    return "employees/detail";
  }

  @GetMapping("/new")
  String newForm(Model model) {
    model.addAttribute("form", EmployeeForm.empty());
    return "employees/form";
  }

  @PostMapping
  String create(
      @Valid @ModelAttribute("form") EmployeeForm form,
      BindingResult binding,
      RedirectAttributes redirect) {
    if (!binding.hasErrors()) {
      try {
        Employee employee = employeeService.create(form);
        Flash.success(redirect, "employee.created", employee.getFullName());
        return "redirect:/employees/" + employee.getId();
      } catch (BusinessRuleException e) {
        e.addTo(binding);
      } catch (DataIntegrityViolationException e) {
        binding.reject("error.integrity");
      }
    }
    return "employees/form";
  }

  @GetMapping("/{id}/edit")
  String editForm(@PathVariable long id, Model model) {
    model.addAttribute("employee", employeeService.get(id));
    model.addAttribute("form", EmployeeForm.from(employeeService.get(id)));
    return "employees/form";
  }

  @PostMapping("/{id}")
  String update(
      @PathVariable long id,
      @Valid @ModelAttribute("form") EmployeeForm form,
      BindingResult binding,
      Model model,
      RedirectAttributes redirect) {
    if (!binding.hasErrors()) {
      try {
        employeeService.update(id, form);
        Flash.success(redirect, "employee.updated");
        return "redirect:/employees/" + id;
      } catch (BusinessRuleException e) {
        e.addTo(binding);
      } catch (DataIntegrityViolationException e) {
        binding.reject("error.integrity");
      }
    }
    model.addAttribute("employee", employeeService.get(id));
    return "employees/form";
  }

  @PostMapping("/{id}/active")
  String setActive(
      @PathVariable long id, @RequestParam boolean active, RedirectAttributes redirect) {
    try {
      employeeService.setActive(id, active);
      Flash.success(redirect, active ? "employee.activated" : "employee.deactivated");
    } catch (BusinessRuleException e) {
      Flash.error(redirect, e);
    }
    return "redirect:/employees/" + id;
  }
}
