package io.github.andi005.qram.inventory;

import io.github.andi005.qram.asset.Asset;
import io.github.andi005.qram.common.CsvSafe;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 * CSV exports of an inventory session for the evaluation. Written for Excel with Albanian regional
 * settings: {@code ;} as separator, UTF-8 with a BOM (so ë and ç show correctly), and every text
 * cell neutralised against formulas.
 */
final class InventoryCsv {

  private static final CSVFormat FORMAT =
      CSVFormat.DEFAULT.builder().setDelimiter(';').setRecordSeparator("\r\n").get();
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private InventoryCsv() {}

  /** One row per scan, in order, with seconds since the start. */
  static byte[] scans(InventoryReport report, ZoneId zone) {
    return write(printer -> {
      printer.printRecord(
          "nr", "koha", "sekonda_nga_fillimi", "vlera", "etiketa", "rezultati", "manual");
      int nr = 1;
      for (InventoryScan scan : report.scans()) {
        long seconds =
            Duration.between(report.session().getStartedAt(), scan.getScannedAt()).toSeconds();
        printer.printRecord(
            nr++,
            TIME.format(scan.getScannedAt().atZone(zone)),
            seconds,
            CsvSafe.cell(scan.getRawValue()),
            scan.getAsset() == null ? "" : scan.getAsset().getTag(),
            scan.getResult().name(),
            scan.isManual() ? "po" : "jo");
      }
    });
  }

  /** One summary row with the measurements, then the missing and unexpected assets. */
  static byte[] summary(InventoryReport report, ZoneId zone) {
    InventorySession s = report.session();
    return write(printer -> {
      printer.printRecord(
          "sesioni",
          "lokacioni",
          "fillimi",
          "mbarimi",
          "kohezgjatja_sekonda",
          "te_pritura",
          "te_gjetura",
          "mungojne",
          "te_papritura",
          "te_panjohura",
          "te_perseritura",
          "hyrje_manuale",
          "karaktere_te_shtypura");
      printer.printRecord(
          CsvSafe.cell(s.getName()),
          s.getLocation() == null ? "" : CsvSafe.cell(s.getLocation().getName()),
          TIME.format(s.getStartedAt().atZone(zone)),
          s.getFinishedAt() == null ? "" : TIME.format(s.getFinishedAt().atZone(zone)),
          report.duration().toSeconds(),
          report.expected().size(),
          report.found().size(),
          report.missing().size(),
          report.unexpected().size(),
          report.unknownScans(),
          report.duplicateScans(),
          report.manualEntries(),
          report.typedCharacters());
      printer.println();
      printer.printRecord("lista", "etiketa", "pajisja", "nr_serik", "lokacioni");
      for (Asset a : report.missing()) {
        assetRow(printer, "mungon", a);
      }
      for (Asset a : report.unexpected()) {
        assetRow(printer, "e_papritur", a);
      }
    });
  }

  private static void assetRow(CSVPrinter printer, String list, Asset a) throws IOException {
    printer.printRecord(
        list,
        a.getTag(),
        CsvSafe.cell(a.getDisplayName()),
        CsvSafe.cell(a.getSerialNumber()),
        CsvSafe.cell(a.getLocationName()));
  }

  private static byte[] write(CsvBody body) {
    StringWriter out = new StringWriter();
    out.write('﻿');
    try (CSVPrinter printer = new CSVPrinter(out, FORMAT)) {
      body.print(printer);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return out.toString().getBytes(StandardCharsets.UTF_8);
  }

  @FunctionalInterface
  private interface CsvBody {
    void print(CSVPrinter printer) throws IOException;
  }
}
