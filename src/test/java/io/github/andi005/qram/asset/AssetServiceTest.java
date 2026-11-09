package io.github.andi005.qram.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.andi005.qram.common.BusinessRuleException;
import io.github.andi005.qram.common.Labels;
import io.github.andi005.qram.history.ChangeLogService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Asset rules (FR-01, FR-03): tags, serials, dates, concurrent edits, retiring. */
class AssetServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-09T09:00:00Z");
  private static final LocalDate TODAY = LocalDate.of(2026, 11, 9);

  private final AssetRepository assets = mock(AssetRepository.class);
  private final LocationRepository locations = mock(LocationRepository.class);
  private final ChangeLogService changeLog = mock(ChangeLogService.class);
  private final Labels labels = mock(Labels.class);
  private final AssetService service =
      new AssetService(assets, locations, changeLog, labels, Clock.fixed(NOW, ZoneOffset.UTC));

  @BeforeEach
  void setUp() {
    when(labels.get(anyString())).thenAnswer(inv -> inv.getArgument(0));
    when(assets.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  private static AssetForm form(String serial, LocalDate purchase, LocalDate warranty) {
    return new AssetForm(
        AssetCategory.LAPTOP,
        "Dell",
        "Latitude",
        serial,
        purchase,
        warranty,
        null,
        AssetStatus.IN_STOCK,
        null,
        null);
  }

  @Test
  @DisplayName("New assets get the next tag from the sequence, formatted IT-000123")
  void generatesTag() {
    when(assets.nextTagNumber()).thenReturn(123L);
    assertThat(service.create(form(null, null, null)).getTag()).isEqualTo("IT-000123");
  }

  @Test
  @DisplayName("A number already used (e.g. by an import) is skipped")
  void skipsUsedTag() {
    when(assets.nextTagNumber()).thenReturn(5L, 6L);
    when(assets.existsByTag("IT-000005")).thenReturn(true);
    assertThat(service.create(form(null, null, null)).getTag()).isEqualTo("IT-000006");
  }

  @Test
  @DisplayName("Serial numbers are stored trimmed and in upper case")
  void normalisesSerial() {
    when(assets.nextTagNumber()).thenReturn(1L);
    assertThat(service.create(form("  ab-12 ", null, null)).getSerialNumber()).isEqualTo("AB-12");
    assertThat(AssetService.normalizeSerial("   ")).isNull();
  }

  @Test
  @DisplayName("A serial number used by another asset is refused")
  void serialTaken() {
    when(assets.existsBySerialNumber("AB-12")).thenReturn(true);
    assertRefused(() -> service.create(form("ab-12", null, null)), "asset.error.serialTaken");
  }

  @Test
  @DisplayName("The purchase date can't be in the future")
  void purchaseInFuture() {
    assertRefused(
        () -> service.create(form(null, TODAY.plusDays(1), null)), "asset.error.purchaseInFuture");
  }

  @Test
  @DisplayName("The warranty can't end before the purchase")
  void warrantyBeforePurchase() {
    assertRefused(
        () -> service.create(form(null, TODAY, TODAY.minusDays(1))),
        "asset.error.warrantyBeforePurchase");
  }

  @Test
  @DisplayName("Saving a form with an old version is refused (someone else edited meanwhile)")
  void concurrentEdit() {
    Asset existing = existing(4);
    assertRefused(
        () -> service.update(existing.getId(), formWithVersion(AssetStatus.IN_STOCK, 3)),
        "asset.error.concurrentEdit");
  }

  @Test
  @DisplayName("An asset with a holder can't be retired: return it first")
  void retireWhileAssigned() {
    Asset existing = existing(0);
    when(assets.hasOpenAssignment(existing.getId())).thenReturn(true);
    assertRefused(
        () -> service.update(existing.getId(), formWithVersion(AssetStatus.RETIRED, 0)),
        "asset.error.returnFirst");
  }

  @Test
  @DisplayName("An unassigned asset can be retired, and stays in the records")
  void retire() {
    Asset existing = existing(0);
    Asset saved = service.update(existing.getId(), formWithVersion(AssetStatus.RETIRED, 0));
    assertThat(saved.getStatus()).isEqualTo(AssetStatus.RETIRED);
    assertThat(saved.getTag()).isEqualTo("IT-000001");
  }

  @Test
  @DisplayName("Tag numbers outside 1..999999 are a programming error")
  void tagRange() {
    assertThat(AssetService.formatTag(1)).isEqualTo("IT-000001");
    assertThat(AssetService.formatTag(999_999)).isEqualTo("IT-999999");
    assertThatThrownBy(() -> AssetService.formatTag(0)).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> AssetService.formatTag(1_000_000))
        .isInstanceOf(IllegalStateException.class);
  }

  private Asset existing(int version) {
    Asset asset = new Asset("IT-000001", NOW);
    asset.setCategory(AssetCategory.LAPTOP);
    asset.setModel("Latitude");
    ReflectionTestUtils.setField(asset, "id", 1L);
    ReflectionTestUtils.setField(asset, "version", version);
    when(assets.findById(1L)).thenReturn(Optional.of(asset));
    return asset;
  }

  private static AssetForm formWithVersion(AssetStatus status, int version) {
    return new AssetForm(
        AssetCategory.LAPTOP, null, "Latitude", null, null, null, null, status, null, version);
  }

  private static void assertRefused(Runnable action, String key) {
    assertThatThrownBy(action::run)
        .isInstanceOf(BusinessRuleException.class)
        .extracting(e -> ((BusinessRuleException) e).messageKey())
        .isEqualTo(key);
  }
}
