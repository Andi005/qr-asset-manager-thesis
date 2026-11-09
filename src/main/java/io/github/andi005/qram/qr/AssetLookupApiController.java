package io.github.andi005.qram.qr;

import io.github.andi005.qram.asset.Asset;
import io.github.andi005.qram.asset.AssetService;
import io.github.andi005.qram.assignment.AssignmentService;
import io.github.andi005.qram.common.Labels;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * JSON lookups for the scanner page. A {@code @RestController} returns data (JSON) instead of a
 * template name. Behind login like everything else; without a session it answers 401.
 */
@RestController
@RequestMapping("/api/assets")
class AssetLookupApiController {

  private final AssetService assetService;
  private final AssignmentService assignmentService;
  private final Labels labels;

  AssetLookupApiController(
      AssetService assetService, AssignmentService assignmentService, Labels labels) {
    this.assetService = assetService;
    this.assignmentService = assignmentService;
    this.labels = labels;
  }

  @GetMapping("/by-tag/{tag}")
  ResponseEntity<?> byTag(@PathVariable String tag) {
    return lookup(tag);
  }

  /** Takes exactly what the camera decoded (a URL or a tag) and lets the server parse it. */
  @GetMapping("/lookup")
  ResponseEntity<?> lookupRaw(@RequestParam("raw") String raw) {
    return lookup(raw);
  }

  private ResponseEntity<?> lookup(String raw) {
    Optional<String> tag = TagParser.parse(raw);
    if (tag.isEmpty()) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "invalidTag", "message", labels.get("scan.invalid")));
    }
    Optional<Asset> asset = assetService.findByTag(tag.get());
    if (asset.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(
              Map.of(
                  "error", "unknownTag",
                  "tag", tag.get(),
                  "message", labels.get("scan.unknown", tag.get())));
    }
    Asset a = asset.get();
    String holder =
        assignmentService
            .currentFor(a.getId())
            .map(x -> x.getEmployee().getFullName())
            .orElse(null);
    return ResponseEntity.ok(
        new AssetInfo(
            a.getId(),
            a.getTag(),
            a.getDisplayName(),
            a.getStatus().name(),
            labels.get("asset.status." + a.getStatus().name()),
            a.getLocationName(),
            holder,
            "/assets/" + a.getId()));
  }
}
