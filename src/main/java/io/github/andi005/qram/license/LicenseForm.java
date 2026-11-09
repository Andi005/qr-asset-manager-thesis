package io.github.andi005.qram.license;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** What the license form sends. An empty expiry date means a perpetual license. */
public record LicenseForm(
    @NotBlank @Size(max = 120) String name,
    @Size(max = 80) String vendor,
    @NotNull @Min(1) @Max(100_000) Integer seatsTotal,
    LocalDate expiryDate,
    @Size(max = 2000) String notes,
    Integer version) {

  public static LicenseForm empty() {
    return new LicenseForm("", "", 1, null, "", null);
  }

  public static LicenseForm from(SoftwareLicense license) {
    return new LicenseForm(
        license.getName(),
        license.getVendor(),
        license.getSeatsTotal(),
        license.getExpiryDate(),
        license.getNotes(),
        license.getVersion());
  }
}
