package io.github.andi005.qram.qr;

import io.github.andi005.qram.asset.Asset;
import io.github.andi005.qram.asset.AssetService;
import io.github.andi005.qram.asset.LocationService;
import io.github.andi005.qram.common.NotFoundException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** QR images, the printable label sheet, the scanner page, and the short label address. */
@Controller
class QrController {

  private static final int LABEL_PIXELS = 300;

  private final QrCodeService qrCodeService;
  private final AssetService assetService;
  private final LocationService locationService;

  QrController(
      QrCodeService qrCodeService, AssetService assetService, LocationService locationService) {
    this.qrCodeService = qrCodeService;
    this.assetService = assetService;
    this.locationService = locationService;
  }

  /**
   * The address on every label. Behind login: someone outside who scans a label only sees the login
   * page. After login, Spring Security brings the user back here, and we send them on to the
   * asset's page.
   */
  @GetMapping("/a/{tag}")
  String openLabel(@PathVariable String tag) {
    Asset asset = lookup(tag);
    return "redirect:/assets/" + asset.getId();
  }

  @GetMapping("/qr/{tag}.png")
  ResponseEntity<byte[]> qrImage(@PathVariable String tag) {
    Asset asset = lookup(tag);
    byte[] png = qrCodeService.png(qrCodeService.labelUrl(asset.getTag()), LABEL_PIXELS);
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePrivate())
        .body(png);
  }

  /** The label sheet for chosen assets (ids from the asset list), or for a whole location. */
  @GetMapping("/labels")
  String labels(
      @RequestParam(required = false) List<Long> ids,
      @RequestParam(required = false) Long location,
      Model model) {
    List<Asset> assets = new ArrayList<>();
    if (ids != null && !ids.isEmpty()) {
      assets.addAll(assetService.findByIds(ids));
    } else if (location != null) {
      assets.addAll(assetService.findInUseAtLocation(location));
    }
    model.addAttribute("assets", assets);
    model.addAttribute("selectedLocation", location);
    model.addAttribute("locations", locationService.findAll());
    model.addAttribute("baseUrl", qrCodeService.labelUrl(""));
    return "qr/labels";
  }

  @GetMapping("/scan")
  String scanner() {
    return "qr/scan";
  }

  private Asset lookup(String raw) {
    String tag = TagParser.parse(raw).orElseThrow(() -> new NotFoundException("Tag " + raw));
    return assetService.findByTag(tag).orElseThrow(() -> new NotFoundException("Tag " + tag));
  }
}
