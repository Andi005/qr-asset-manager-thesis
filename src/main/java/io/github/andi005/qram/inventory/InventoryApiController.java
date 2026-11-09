package io.github.andi005.qram.inventory;

import io.github.andi005.qram.asset.Asset;
import io.github.andi005.qram.common.BusinessRuleException;
import io.github.andi005.qram.common.Formats;
import io.github.andi005.qram.common.Labels;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JSON endpoints used by the live inventory page: post a scan, read the current state. POSTs need
 * the CSRF token, which the page sends in the X-CSRF-TOKEN header.
 */
@RestController
@RequestMapping("/api/inventory")
class InventoryApiController {

  private final InventoryService inventoryService;
  private final Labels labels;
  private final Formats formats;

  InventoryApiController(InventoryService inventoryService, Labels labels, Formats formats) {
    this.inventoryService = inventoryService;
    this.labels = labels;
    this.formats = formats;
  }

  /** The request body: what was scanned or typed, and whether it was typed by hand. */
  record ScanRequest(String raw, Boolean manual) {}

  record AssetRow(String tag, String name, String location) {}

  record ScanEntry(String time, String raw, String tag, String result, String resultLabel) {}

  record State(
      boolean finished,
      String duration,
      int expected,
      List<AssetRow> found,
      List<AssetRow> missing,
      List<AssetRow> unexpected,
      int unknown,
      int duplicates,
      List<ScanEntry> scans) {}

  record ScanResponse(String result, String resultLabel, String tag, String name, State state) {}

  @PostMapping("/{id}/scans")
  ScanResponse scan(@PathVariable long id, @RequestBody ScanRequest request) {
    InventoryScan scan =
        inventoryService.recordScan(id, request.raw(), Boolean.TRUE.equals(request.manual()));
    Asset asset = scan.getAsset();
    return new ScanResponse(
        scan.getResult().name(),
        labels.get("inventory.result." + scan.getResult().name()),
        asset == null ? null : asset.getTag(),
        asset == null ? null : asset.getDisplayName(),
        state(inventoryService.report(id)));
  }

  @GetMapping("/{id}")
  State current(@PathVariable long id) {
    return state(inventoryService.report(id));
  }

  @ExceptionHandler(SessionFinishedException.class)
  ResponseEntity<Map<String, String>> finished() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("error", "finished", "message", labels.get("inventory.error.finished")));
  }

  @ExceptionHandler(BusinessRuleException.class)
  ResponseEntity<Map<String, String>> refused(BusinessRuleException e) {
    return ResponseEntity.badRequest()
        .body(
            Map.of(
                "error",
                e.messageKey(),
                "message",
                labels.get(e.messageKey(), (Object[]) e.args())));
  }

  private State state(InventoryReport r) {
    List<ScanEntry> scans =
        r.scans().reversed().stream()
            .limit(30)
            .map(s ->
                new ScanEntry(
                    formats.time(s.getScannedAt()),
                    s.getRawValue(),
                    s.getAsset() == null ? null : s.getAsset().getTag(),
                    s.getResult().name(),
                    labels.get("inventory.result." + s.getResult().name())))
            .toList();
    return new State(
        r.session().isFinished(),
        formats.duration(r.duration()),
        r.expected().size(),
        rows(r.found()),
        rows(r.missing()),
        rows(r.unexpected()),
        r.unknownScans(),
        r.duplicateScans(),
        scans);
  }

  private static List<AssetRow> rows(List<Asset> assets) {
    return assets.stream()
        .map(a -> new AssetRow(a.getTag(), a.getDisplayName(), a.getLocationName()))
        .toList();
  }
}
