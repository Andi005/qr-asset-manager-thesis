package io.github.andi005.qram.asset;

import io.github.andi005.qram.assignment.AssignmentService;
import io.github.andi005.qram.common.BusinessRuleException;
import io.github.andi005.qram.common.Flash;
import io.github.andi005.qram.history.ChangeLogService;
import io.github.andi005.qram.history.EntityType;
import io.github.andi005.qram.license.LicenseService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
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

/**
 * Asset pages: list and search, details with history, create and edit. Only HTTP in and out; the
 * rules are in {@link AssetService}.
 */
@Controller
@RequestMapping("/assets")
class AssetController {

  private final AssetService assetService;
  private final LocationService locationService;
  private final AssignmentService assignmentService;
  private final LicenseService licenseService;
  private final ChangeLogService changeLog;

  AssetController(
      AssetService assetService,
      LocationService locationService,
      AssignmentService assignmentService,
      LicenseService licenseService,
      ChangeLogService changeLog) {
    this.assetService = assetService;
    this.locationService = locationService;
    this.assignmentService = assignmentService;
    this.licenseService = licenseService;
    this.changeLog = changeLog;
  }

  @GetMapping
  String list(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) AssetStatus status,
      @RequestParam(required = false) Long location,
      @RequestParam(defaultValue = "0") int page,
      Model model) {
    Page<Asset> result = assetService.search(q, status, location, page);
    model.addAttribute("page", result);
    model.addAttribute(
        "holders", assignmentService.currentHolderNames(result.map(Asset::getId).getContent()));
    model.addAttribute("q", q);
    model.addAttribute("status", status);
    model.addAttribute("location", location);
    model.addAttribute("statuses", AssetStatus.values());
    model.addAttribute("locations", locationService.findAll());
    return "assets/list";
  }

  @GetMapping("/{id}")
  String detail(@PathVariable long id, Model model) {
    Asset asset = assetService.get(id);
    model.addAttribute("asset", asset);
    model.addAttribute("current", assignmentService.currentFor(id).orElse(null));
    model.addAttribute("assignments", assignmentService.historyForAsset(id));
    model.addAttribute("licenses", licenseService.openAllocationsForAsset(id));
    model.addAttribute("changes", changeLog.forEntity(EntityType.ASSET, id));
    return "assets/detail";
  }

  @GetMapping("/new")
  String newForm(Model model) {
    return showForm(model, AssetForm.empty(), null);
  }

  @PostMapping
  String create(
      @Valid @ModelAttribute("form") AssetForm form,
      BindingResult binding,
      Model model,
      RedirectAttributes redirect) {
    if (binding.hasErrors()) {
      return showForm(model, form, null);
    }
    try {
      Asset asset = assetService.create(form);
      Flash.success(redirect, "asset.created", asset.getTag());
      return "redirect:/assets/" + asset.getId();
    } catch (BusinessRuleException e) {
      e.addTo(binding);
    } catch (DataIntegrityViolationException e) {
      binding.reject("error.integrity");
    }
    return showForm(model, form, null);
  }

  @GetMapping("/{id}/edit")
  String editForm(@PathVariable long id, Model model) {
    Asset asset = assetService.get(id);
    return showForm(model, AssetForm.from(asset), asset);
  }

  @PostMapping("/{id}")
  String update(
      @PathVariable long id,
      @Valid @ModelAttribute("form") AssetForm form,
      BindingResult binding,
      Model model,
      RedirectAttributes redirect) {
    if (binding.hasErrors()) {
      return showForm(model, form, assetService.get(id));
    }
    try {
      Asset asset = assetService.update(id, form);
      Flash.success(redirect, "asset.updated", asset.getTag());
      return "redirect:/assets/" + id;
    } catch (BusinessRuleException e) {
      e.addTo(binding);
    } catch (OptimisticLockingFailureException e) {
      binding.reject("asset.error.concurrentEdit");
    } catch (DataIntegrityViolationException e) {
      binding.reject("error.integrity");
    }
    return showForm(model, form, assetService.get(id));
  }

  private String showForm(Model model, AssetForm form, Asset existing) {
    model.addAttribute("form", form);
    model.addAttribute("asset", existing);
    model.addAttribute("categories", AssetCategory.values());
    model.addAttribute("statuses", AssetStatus.values());
    model.addAttribute("locations", locationService.findAll());
    return "assets/form";
  }
}
