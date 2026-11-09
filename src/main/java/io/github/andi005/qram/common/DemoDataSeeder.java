package io.github.andi005.qram.common;

import io.github.andi005.qram.asset.Asset;
import io.github.andi005.qram.asset.AssetCategory;
import io.github.andi005.qram.asset.AssetRepository;
import io.github.andi005.qram.asset.AssetStatus;
import io.github.andi005.qram.asset.Location;
import io.github.andi005.qram.asset.LocationRepository;
import io.github.andi005.qram.assignment.Assignment;
import io.github.andi005.qram.assignment.AssignmentRepository;
import io.github.andi005.qram.employee.Employee;
import io.github.andi005.qram.employee.EmployeeRepository;
import io.github.andi005.qram.history.ChangeLogEntry;
import io.github.andi005.qram.history.ChangeLogRepository;
import io.github.andi005.qram.history.EntityType;
import io.github.andi005.qram.license.LicenseAllocation;
import io.github.andi005.qram.license.LicenseAllocationRepository;
import io.github.andi005.qram.license.LicenseRepository;
import io.github.andi005.qram.license.SoftwareLicense;
import io.github.andi005.qram.security.AppUser;
import io.github.andi005.qram.security.AppUserRepository;
import io.github.andi005.qram.security.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills an empty database with <b>fictional</b> demo data (profile {@code demo} only): the
 * imaginary "Kolegji Shembull" with 45 assets, 12 employees, licenses near expiry and an assignment
 * history. Dates are relative to today, so "expiring soon" always has entries.
 *
 * <p>It also matches the usability task cards in {@code evaluation/usability-tasks.md}: IT-000012
 * is a free laptop and Drita Krasniqi is an active employee.
 *
 * <p>Runs only when there are no assets yet, so it never touches real data.
 */
@Component
@Profile("demo")
@Order(2)
class DemoDataSeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
  private static final String BY = "demo";

  private final AssetRepository assets;
  private final LocationRepository locations;
  private final EmployeeRepository employees;
  private final AssignmentRepository assignments;
  private final LicenseRepository licenses;
  private final LicenseAllocationRepository allocations;
  private final ChangeLogRepository changeLog;
  private final AppUserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final AppProperties properties;
  private final Clock clock;

  DemoDataSeeder(
      AssetRepository assets,
      LocationRepository locations,
      EmployeeRepository employees,
      AssignmentRepository assignments,
      LicenseRepository licenses,
      LicenseAllocationRepository allocations,
      ChangeLogRepository changeLog,
      AppUserRepository users,
      PasswordEncoder passwordEncoder,
      AppProperties properties,
      Clock clock) {
    this.assets = assets;
    this.locations = locations;
    this.employees = employees;
    this.assignments = assignments;
    this.licenses = licenses;
    this.allocations = allocations;
    this.changeLog = changeLog;
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.properties = properties;
    this.clock = clock;
  }

  /** One fictional device model: category, manufacturer, model, serial prefix, warranty years. */
  private record Model(
      AssetCategory category, String manufacturer, String model, String prefix, int years) {}

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (assets.count() > 0) {
      return;
    }
    Instant now = clock.instant();
    LocalDate today = LocalDate.now(clock);
    Random random = new Random(2026);

    seedUsers(now);

    List<Location> rooms = new ArrayList<>();
    for (String name :
        List.of(
            "Ndërtesa A / Salla 101",
            "Ndërtesa A / Salla 102",
            "Ndërtesa A / Zyra e IT-së",
            "Ndërtesa B / Laboratori 1",
            "Ndërtesa B / Laboratori 2",
            "Ndërtesa B / Biblioteka",
            "Ndërtesa C / Administrata")) {
      rooms.add(locations.save(new Location(name, now)));
    }

    List<Employee> people = new ArrayList<>();
    String[][] employeeData = {
      {"Drita Krasniqi", "Administrata"},
      {"Arben Gashi", "Zyra e IT-së"},
      {"Blerta Hoxha", "Biblioteka"},
      {"Fatos Berisha", "Fakulteti i Shkencave Kompjuterike"},
      {"Liridona Shala", "Administrata"},
      {"Valon Morina", "Fakulteti i Shkencave Kompjuterike"},
      {"Era Kelmendi", "Financat"},
      {"Gentrit Rexhepi", "Zyra e IT-së"},
      {"Mimoza Bytyqi", "Fakulteti i Menaxhimit"},
      {"Ilir Dervishi", "Fakulteti i Menaxhimit"},
      {"Teuta Zeqiri", "Biblioteka"},
      {"Besnik Haliti", "Financat"},
    };
    for (String[] e : employeeData) {
      String email =
          e[0].toLowerCase(java.util.Locale.ROOT).replace(' ', '.') + "@shembull.example";
      people.add(employees.save(new Employee(e[0], email, e[1], now)));
    }

    List<Model> models =
        List.of(
            new Model(AssetCategory.LAPTOP, "Lenovo", "ThinkPad T14 Gen 4", "LE", 3),
            new Model(AssetCategory.LAPTOP, "Dell", "Latitude 5440", "DE", 3),
            new Model(AssetCategory.LAPTOP, "HP", "EliteBook 840 G10", "HP", 2),
            new Model(AssetCategory.DESKTOP, "HP", "ProDesk 400 G9", "HP", 3),
            new Model(AssetCategory.DESKTOP, "Dell", "OptiPlex 7010", "DE", 3),
            new Model(AssetCategory.MONITOR, "Dell", "P2423", "DE", 3),
            new Model(AssetCategory.MONITOR, "LG", "24MP400", "LG", 2),
            new Model(AssetCategory.PRINTER, "HP", "LaserJet Pro M404dn", "HP", 1),
            new Model(AssetCategory.PRINTER, "Canon", "i-SENSYS MF445dw", "CA", 2),
            new Model(AssetCategory.PROJECTOR, "Epson", "EB-W51", "EP", 3),
            new Model(AssetCategory.PROJECTOR, "BenQ", "MW560", "BE", 3),
            new Model(AssetCategory.NETWORK, "Cisco", "CBS250-24T", "CI", 3),
            new Model(AssetCategory.NETWORK, "TP-Link", "Archer AX55", "TP", 2),
            new Model(AssetCategory.PHONE, "Samsung", "Galaxy A55", "SA", 2));

    // Warranty end dates chosen so the expiry view has a realistic mix (days from today).
    int[] warrantyOffsets = {12, 40, 75, 90, 91, -10, -200, 25, 60, 150};

    List<Asset> created = new ArrayList<>();
    for (int i = 1; i <= 45; i++) {
      Model m = i == 12 ? models.get(0) : models.get(random.nextInt(models.size()));
      Asset asset = new Asset("IT-%06d".formatted(assets.nextTagNumber()), now);
      asset.setCategory(m.category());
      asset.setManufacturer(m.manufacturer());
      asset.setModel(m.model());
      asset.setSerialNumber(m.prefix() + (10_000_000 + random.nextInt(89_999_999)));
      LocalDate warranty;
      if (i <= warrantyOffsets.length) {
        warranty = today.plusDays(warrantyOffsets[i - 1]);
      } else {
        warranty = today.plusDays(random.nextInt(1400) - 400);
      }
      LocalDate purchase = warranty.minusYears(m.years());
      if (purchase.isAfter(today)) {
        purchase = today.minusDays(30);
      }
      asset.setPurchaseDate(purchase);
      asset.setWarrantyEndDate(warranty);
      asset.setLocation(rooms.get(random.nextInt(rooms.size())));
      asset.setStatus(
          i == 12
              ? AssetStatus.IN_STOCK
              : i % 11 == 0 ? AssetStatus.IN_REPAIR : AssetStatus.IN_STOCK);
      if (i == 44) {
        asset.setStatus(AssetStatus.RETIRED);
        asset.setNotes("Ekrani i thyer, nuk ia vlen riparimi.");
      }
      created.add(assets.save(asset));
      log(EntityType.ASSET, created.getLast().getId(), "CREATED", asset.getDisplayName(), now);
    }
    // Keep generated tags after the seeded ones.
    assets.syncTagSequence();

    // History: some devices changed hands before; the current holders are open assignments.
    int assignmentsMade = 0;
    for (int i = 0; i < created.size(); i++) {
      Asset asset = created.get(i);
      if (asset.getTag().equals("IT-000012") || asset.getStatus() != AssetStatus.IN_STOCK) {
        continue;
      }
      if (i % 3 == 0) {
        Employee previous = people.get((i + 5) % people.size());
        Instant from = now.minus(Duration.ofDays(400 + i));
        Assignment old = new Assignment(asset, previous, from, BY, null);
        old.close(from.plus(Duration.ofDays(200)), BY);
        assignments.save(old);
      }
      if (i % 2 == 0 && assignmentsMade < 18) {
        Employee holder = people.get(i % (people.size() - 1));
        assignments.save(
            new Assignment(asset, holder, now.minus(Duration.ofDays(30 + i * 3L)), BY, null));
        log(EntityType.ASSET, asset.getId(), "ASSIGNED", holder.getFullName(), now);
        assignmentsMade++;
      }
    }
    // Besnik left: he holds nothing and is inactive.
    people.getLast().setActive(false);

    seedLicenses(today, now, people, created);

    log.info(
        "Demo data created: {} assets, {} employees, {} locations (fictional).",
        created.size(),
        people.size(),
        rooms.size());
  }

  private void seedLicenses(
      LocalDate today, Instant now, List<Employee> people, List<Asset> created) {
    Object[][] data = {
      {"Microsoft 365 A3", "Microsoft", 50, today.plusDays(25)},
      {"Adobe Creative Cloud", "Adobe", 10, today.plusDays(60)},
      {"ESET Endpoint Antivirus", "ESET", 120, today.plusDays(88)},
      {"IBM SPSS Statistics", "IBM", 8, today.plusDays(45)},
      {"Windows 11 Pro", "Microsoft", 40, null},
      {"AutoCAD Educational", "Autodesk", 15, today.plusDays(200)},
      {"Zoom Pro", "Zoom", 5, today.minusDays(10)},
      {"JetBrains All Products", "JetBrains", 20, today.plusDays(300)},
    };
    List<SoftwareLicense> saved = new ArrayList<>();
    for (Object[] row : data) {
      SoftwareLicense license = new SoftwareLicense((String) row[0], (Integer) row[2], now);
      license.setVendor((String) row[1]);
      license.setExpiryDate((LocalDate) row[3]);
      saved.add(licenses.save(license));
      log(EntityType.LICENSE, license.getId(), "CREATED", license.getName(), now);
    }
    // Personal seats for most employees on Microsoft 365, a few others; device seats for antivirus.
    for (int i = 0; i < people.size() - 1; i++) {
      allocations.save(
          LicenseAllocation.toEmployee(
              saved.get(0), people.get(i), now.minus(Duration.ofDays(90)), BY));
    }
    for (int i = 0; i < 4; i++) {
      allocations.save(
          LicenseAllocation.toEmployee(
              saved.get(1), people.get(i * 2), now.minus(Duration.ofDays(60)), BY));
    }
    for (int i = 0; i < 5; i++) {
      allocations.save(
          LicenseAllocation.toEmployee(
              saved.get(6), people.get(i + 1), now.minus(Duration.ofDays(300)), BY));
    }
    for (int i = 0; i < 20; i++) {
      Asset asset = created.get(i);
      if (asset.getStatus() != AssetStatus.RETIRED) {
        allocations.save(
            LicenseAllocation.toAsset(saved.get(2), asset, now.minus(Duration.ofDays(120)), BY));
      }
    }
  }

  private void seedUsers(Instant now) {
    String password = passwordEncoder.encode(properties.demoPassword());
    if (!users.existsByUsernameIgnoreCase("it")) {
      users.save(new AppUser("it", password, "Staf i IT-së (demo)", Role.IT_STAFF, now));
    }
    if (!users.existsByUsernameIgnoreCase("viewer")) {
      users.save(new AppUser("viewer", password, "Vëzhgues (demo)", Role.VIEWER, now));
    }
  }

  private void log(EntityType type, Long id, String action, String details, Instant at) {
    changeLog.save(new ChangeLogEntry(type, id, action, BY, at, details));
  }
}
