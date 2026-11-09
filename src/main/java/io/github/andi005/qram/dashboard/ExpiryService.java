package io.github.andi005.qram.dashboard;

import io.github.andi005.qram.asset.Asset;
import io.github.andi005.qram.asset.AssetService;
import io.github.andi005.qram.common.ExpiryWindow;
import io.github.andi005.qram.license.LicenseService;
import io.github.andi005.qram.license.SoftwareLicense;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Licenses and warranties that expire within 90 days (today to today+90, inclusive), and those
 * already expired (FR-11). "Today" comes from the injected clock, in the configured time zone.
 */
@Service
public class ExpiryService {

  private final LicenseService licenseService;
  private final AssetService assetService;
  private final Clock clock;

  public ExpiryService(LicenseService licenseService, AssetService assetService, Clock clock) {
    this.licenseService = licenseService;
    this.assetService = assetService;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public ExpiringItems expiring() {
    LocalDate today = LocalDate.now(clock);
    LocalDate last = ExpiryWindow.lastDay(today);
    return new ExpiringItems(
        today,
        last,
        licenseService.expiringBetween(today, last),
        licenseService.expiredBefore(today),
        assetService.warrantiesEndingBetween(today, last),
        assetService.warrantiesEndedBefore(today));
  }

  /** What the expiry view shows. Lists are sorted by date, the soonest first. */
  public record ExpiringItems(
      LocalDate today,
      LocalDate lastDay,
      List<SoftwareLicense> licensesSoon,
      List<SoftwareLicense> licensesExpired,
      List<Asset> warrantiesSoon,
      List<Asset> warrantiesExpired) {

    /** Days from today until the date (0 = today). */
    public long daysLeft(LocalDate date) {
      return date == null ? 0 : java.time.temporal.ChronoUnit.DAYS.between(today, date);
    }
  }
}
