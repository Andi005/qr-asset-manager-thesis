package io.github.andi005.qram.inventory;

import io.github.andi005.qram.asset.LocationService;
import io.github.andi005.qram.common.AppProperties;
import io.github.andi005.qram.common.BusinessRuleException;
import io.github.andi005.qram.common.Flash;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Inventory session pages: list, start, the live scanning page, finish, CSV exports. */
@Controller
@RequestMapping("/inventory")
class InventoryController {

  private final InventoryService inventoryService;
  private final LocationService locationService;
  private final AppProperties properties;

  InventoryController(
      InventoryService inventoryService,
      LocationService locationService,
      AppProperties properties) {
    this.inventoryService = inventoryService;
    this.locationService = locationService;
    this.properties = properties;
  }

  @GetMapping
  String list(Model model) {
    model.addAttribute("sessions", inventoryService.findAll());
    return "inventory/list";
  }

  @GetMapping("/new")
  String newForm(Model model) {
    model.addAttribute("locations", locationService.findAll());
    return "inventory/new";
  }

  @PostMapping
  String start(
      @RequestParam String name,
      @RequestParam(required = false) Long locationId,
      @RequestParam(required = false) String tagList,
      Model model,
      RedirectAttributes redirect) {
    try {
      InventorySession session = inventoryService.start(name, locationId, tagList);
      return "redirect:/inventory/" + session.getId();
    } catch (BusinessRuleException e) {
      model.addAttribute("error", e);
      model.addAttribute("name", name);
      model.addAttribute("locationId", locationId);
      model.addAttribute("tagList", tagList);
      model.addAttribute("locations", locationService.findAll());
      return "inventory/new";
    }
  }

  @GetMapping("/{id}")
  String session(@PathVariable long id, Model model) {
    model.addAttribute("report", inventoryService.report(id));
    return "inventory/session";
  }

  @PostMapping("/{id}/finish")
  String finish(@PathVariable long id, RedirectAttributes redirect) {
    inventoryService.finish(id);
    Flash.success(redirect, "inventory.finished");
    return "redirect:/inventory/" + id;
  }

  @GetMapping("/{id}/scans.csv")
  ResponseEntity<byte[]> scansCsv(@PathVariable long id) {
    return csv(
        "inventari-" + id + "-skanimet.csv",
        InventoryCsv.scans(inventoryService.report(id), properties.zoneId()));
  }

  @GetMapping("/{id}/summary.csv")
  ResponseEntity<byte[]> summaryCsv(@PathVariable long id) {
    return csv(
        "inventari-" + id + "-permbledhja.csv",
        InventoryCsv.summary(inventoryService.report(id), properties.zoneId()));
  }

  private static ResponseEntity<byte[]> csv(String fileName, byte[] body) {
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(fileName).build().toString())
        .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
        .body(body);
  }
}
