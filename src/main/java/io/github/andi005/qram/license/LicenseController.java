package io.github.andi005.qram.license;

import io.github.andi005.qram.asset.AssetService;
import io.github.andi005.qram.common.BusinessRuleException;
import io.github.andi005.qram.common.Flash;
import io.github.andi005.qram.employee.EmployeeService;
import io.github.andi005.qram.history.ChangeLogService;
import io.github.andi005.qram.history.EntityType;
import io.github.andi005.qram.qr.TagParser;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
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

/** License pages: list, details with seats, create, edit, give and take back seats. */
@Controller
@RequestMapping("/licenses")
class LicenseController {

  private final LicenseService licenseService;
  private final EmployeeService employeeService;
  private final AssetService assetService;
  private final ChangeLogService changeLog;

  LicenseController(
      LicenseService licenseService,
      EmployeeService employeeService,
      AssetService assetService,
      ChangeLogService changeLog) {
    this.licenseService = licenseService;
    this.employeeService = employeeService;
    this.assetService = assetService;
    this.changeLog = changeLog;
  }

  @GetMapping
  String list(Model model) {
    model.addAttribute("licenses", licenseService.findAll());
    return "licenses/list";
  }

  @GetMapping("/{id}")
  String detail(@PathVariable long id, Model model) {
    model.addAttribute("summary", licenseService.summary(id));
    model.addAttribute("allocations", licenseService.allocations(id));
    model.addAttribute("employees", employeeService.findActive());
    model.addAttribute("changes", changeLog.forEntity(EntityType.LICENSE, id));
    return "licenses/detail";
  }

  @GetMapping("/new")
  String newForm(Model model) {
    model.addAttribute("form", LicenseForm.empty());
    return "licenses/form";
  }

  @PostMapping
  String create(
      @Valid @ModelAttribute("form") LicenseForm form,
      BindingResult binding,
      RedirectAttributes redirect) {
    if (binding.hasErrors()) {
      return "licenses/form";
    }
    SoftwareLicense license = licenseService.create(form);
    Flash.success(redirect, "license.created", license.getName());
    return "redirect:/licenses/" + license.getId();
  }

  @GetMapping("/{id}/edit")
  String editForm(@PathVariable long id, Model model) {
    SoftwareLicense license = licenseService.get(id);
    model.addAttribute("license", license);
    model.addAttribute("form", LicenseForm.from(license));
    return "licenses/form";
  }

  @PostMapping("/{id}")
  String update(
      @PathVariable long id,
      @Valid @ModelAttribute("form") LicenseForm form,
      BindingResult binding,
      Model model,
      RedirectAttributes redirect) {
    if (!binding.hasErrors()) {
      try {
        licenseService.update(id, form);
        Flash.success(redirect, "license.updated");
        return "redirect:/licenses/" + id;
      } catch (BusinessRuleException e) {
        e.addTo(binding);
      } catch (OptimisticLockingFailureException e) {
        binding.reject("license.error.concurrentEdit");
      }
    }
    model.addAttribute("license", licenseService.get(id));
    return "licenses/form";
  }

  /** Give a seat to an employee (employeeId) or to an asset (assetTag, typed or scanned). */
  @PostMapping("/{id}/allocations")
  String allocate(
      @PathVariable long id,
      @RequestParam(required = false) Long employeeId,
      @RequestParam(required = false) String assetTag,
      RedirectAttributes redirect) {
    try {
      LicenseAllocation allocation;
      if (employeeId != null) {
        allocation = licenseService.allocateToEmployee(id, employeeId);
      } else if (assetTag != null && !assetTag.isBlank()) {
        String tag =
            TagParser.parse(assetTag)
                .orElseThrow(() -> new BusinessRuleException("license.error.badTag", assetTag));
        long assetId =
            assetService
                .findByTag(tag)
                .orElseThrow(() -> new BusinessRuleException("license.error.unknownTag", tag))
                .getId();
        allocation = licenseService.allocateToAsset(id, assetId);
      } else {
        throw new BusinessRuleException("license.error.chooseHolder");
      }
      Flash.success(redirect, "license.allocated", allocation.getHolderName());
    } catch (BusinessRuleException e) {
      Flash.error(redirect, e);
    } catch (DataIntegrityViolationException e) {
      Flash.error(redirect, "error.integrity");
    }
    return "redirect:/licenses/" + id;
  }

  @PostMapping("/{id}/allocations/{allocationId}/release")
  String release(
      @PathVariable long id, @PathVariable long allocationId, RedirectAttributes redirect) {
    try {
      LicenseAllocation allocation = licenseService.release(id, allocationId);
      Flash.success(redirect, "license.released", allocation.getHolderName());
    } catch (BusinessRuleException e) {
      Flash.error(redirect, e);
    }
    return "redirect:/licenses/" + id;
  }
}
