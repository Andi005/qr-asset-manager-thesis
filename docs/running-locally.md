# Running the app locally

There are two ways to run it: **Docker only**, which is the easiest and needs no Java, or **from source**, for development. Commands are shown for Windows PowerShell; on macOS or Linux use `./mvnw` instead of `.\mvnw.cmd`.

## What you need
| For | Install |
|---|---|
| Docker only | [Docker Desktop](https://www.docker.com/products/docker-desktop/), started (the whale icon is steady) |
| From source | the above, plus **Java 25** ([Temurin](https://adoptium.net/temurin/releases/?version=25)) and Git |

You don't need to install Maven: the wrapper (`mvnw` / `mvnw.cmd`) downloads the right version itself.

## Option A: Docker only
```powershell
git clone https://github.com/Andi005/qr-asset-manager-thesis.git
cd qr-asset-manager-thesis
docker compose up --build
```
The first build takes a few minutes, because it downloads Java and the libraries. Wait for the log line `Started QrAssetManagerApplication`, then open **http://localhost:8080**.

- Log in as `admin` / `admin12345`. The demo accounts are `it` / `demo12345` (IT staff) and `viewer` / `demo12345` (read-only).
- The data is **fictional demo data** (an invented "Kolegji Shembull"), loaded only into an empty database.
- **Stop:** `Ctrl+C`, then `docker compose down`. The data survives in a Docker volume.
- **Start fresh:** `docker compose down -v` deletes the database volume.
- **Empty system without demo data:** `$env:APP_PROFILE=""; docker compose up --build`.
- **Another admin password:** `$env:ADMIN_INITIAL_PASSWORD="something-long"` before the *first* start. It is only used while there are no users yet.

## Option B: from source
```powershell
docker compose up -d db                                         # PostgreSQL 18 on localhost:5432
$env:ADMIN_INITIAL_PASSWORD="admin12345"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"   # http://localhost:8080
```
- Leave out the `demo` profile to start with an empty database. The admin password is then the one you set, or a random one printed in the log (`ADMIN_INITIAL_PASSWORD is not set...`).
- Add the `local` profile (`"-Dspring-boot.run.profiles=demo,local"`) to see the SQL that Hibernate runs.
- In IntelliJ: open the folder, run `QrAssetManagerApplication`, and set the active profile `demo` and the environment variable `ADMIN_INITIAL_PASSWORD` in the run configuration.

### Build and test
```powershell
.\mvnw.cmd verify
```
This builds the app, formats the code (Spotless), runs all tests, and writes the coverage report to `target\site\jacoco\index.html`. **Docker must be running**, because the integration tests start their own PostgreSQL container. That container is separate from the `db` service, so your local data is never touched.

Run a single test class:
```powershell
.\mvnw.cmd test "-Dtest=ExpiryWindowTest"
```

## Testing with a phone
Phone browsers only open the camera on **HTTPS** (or on `localhost`, which on a phone means the phone itself). On your Wi-Fi network you have three options:

1. **Laptop webcam:** open http://localhost:8080/scan on the laptop and hold a printed label in front of the webcam. `localhost` counts as secure, so the camera works.
2. **"Scan from a photo"** on the phone over plain HTTP:
   - find the laptop's address with `ipconfig` (IPv4, e.g. `192.168.1.20`);
   - start the app with `$env:APP_BASE_URL="http://192.168.1.20:8080"`, so new labels point there;
   - open `http://192.168.1.20:8080` on the phone (same Wi-Fi) and use **Skano nga një foto**.

   Windows may ask to allow Java or Docker through the firewall; allow it for private networks.
3. **A temporary HTTPS address** with a tunnel, for example [Cloudflare quick tunnels](https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/do-more-with-tunnels/trycloudflare/): `cloudflared tunnel --url http://localhost:8080` gives an `https://….trycloudflare.com` address. Set `APP_BASE_URL` to it and restart the app, and the phone camera works. The address changes on every run, so don't print labels for it.

For printed labels that keep working, deploy first ([deployment.md](deployment.md)), then print with the final `APP_BASE_URL`.

## Trying the main features (demo data)
| Try | Where |
|---|---|
| Register an asset | **Asetet → Regjistro aset** (the tag is generated) |
| Assign IT-000012 to Drita Krasniqi, then return it | open IT-000012 → **Ngarko te një punonjës** |
| Licenses and warranties expiring within 90 days | **Skadimet** (also on the dashboard) |
| Print labels | tick assets in **Asetet** → **Printo etiketat e zgjedhura**, or **Më shumë → Lokacionet → Etiketat QR** |
| Look up a device by scanning | **Skano** (or type `IT-000003`) |
| A timed inventory | **Inventari → Fillo inventar të ri**, choose a location, scan or type tags, then **Përfundo**, then export CSV |
| CSV import with an error report | **Më shumë → Importi CSV** (as admin) with `docs/requirements/sample-data/MOCK-inventory.csv`. On an empty system it shows the deliberate mistakes; with demo data loaded, the tags also already exist |
| Who changed what | **Më shumë → Historiku i ndryshimeve** |
| Role limits | log in as `viewer`: lists work, edit buttons are hidden, and edit URLs answer 403 |

## Troubleshooting
| Symptom | Fix |
|---|---|
| `failed to connect to the docker API` / tests fail with "Could not find a valid Docker environment" | start Docker Desktop and wait until it says it is running |
| `port 5432 is already allocated` | another PostgreSQL is running: stop it, or change the left port in `compose.yaml` (`"5433:5432"`) and set `DB_URL=jdbc:postgresql://localhost:5433/qram` |
| `port 8080 is already in use` | stop the other app, or set `$env:PORT="8081"` |
| Can't log in as admin | the admin password is set only on the very first start. Run `docker compose down -v` to start over, or reset it from another ADMIN account |
| `Validate failed: Migrations have failed validation` | a migration file was edited after it ran: never edit `V*.sql`, add a new one. On a throwaway local DB: `docker compose down -v` |
| Camera does not open on the phone | you are on plain HTTP: use "scan from a photo", the laptop webcam, a tunnel, or the deployed HTTPS address |
| The CSV import reports "Kolona … nuk njihet" | that column is ignored. Only the columns listed on the import page are read |
