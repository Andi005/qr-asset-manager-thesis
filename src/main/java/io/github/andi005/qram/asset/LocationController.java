package io.github.andi005.qram.asset;

import io.github.andi005.qram.common.BusinessRuleException;
import io.github.andi005.qram.common.Flash;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** The list of locations, with a small form to add one. */
@Controller
class LocationController {

  private final LocationService locationService;

  LocationController(LocationService locationService) {
    this.locationService = locationService;
  }

  @GetMapping("/locations")
  String list(Model model) {
    model.addAttribute("locations", locationService.findAll());
    model.addAttribute("counts", locationService.assetCounts());
    return "locations/list";
  }

  @PostMapping("/locations")
  String create(@RequestParam String name, RedirectAttributes redirect) {
    try {
      Location location = locationService.create(name);
      Flash.success(redirect, "location.created", location.getName());
    } catch (BusinessRuleException e) {
      Flash.error(redirect, e);
    }
    return "redirect:/locations";
  }
}
