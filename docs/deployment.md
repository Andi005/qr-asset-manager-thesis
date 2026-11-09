# Deployment: Render (app) + Neon (database)

**Why not GitHub Pages?** GitHub Pages only serves static files (HTML, CSS, JS). This system needs a running Java server and a PostgreSQL database, so it needs a host that runs a container. [ADR 004](architecture/adr-004-hosting.md) compares the options and picks:

- **Render**, free web service: builds the `Dockerfile` from GitHub, gives you HTTPS at `https://<name>.onrender.com`, and deploys again on every push to `main` once CI is green.
- **Neon**, free PostgreSQL: unlike Render's free database it doesn't expire after 30 days.

Both are free without a card (re-check the current terms before you rely on them). The catch is that the free Render service **sleeps after ~15 minutes idle**, so the first visit after that takes about a minute.

You create the two accounts yourself. Total time is about 15 minutes.

## 1. Database on Neon
The project is linked to this repository with the Neon CLI (`neon.ts`; the link itself is in the git-ignored `.neon` file). The app reads Neon's own variables directly ([`NeonDatabaseEnvironment`](../src/main/java/io/github/andi005/qram/common/NeonDatabaseEnvironment.java)):

| Variable | What | Used for |
|---|---|---|
| `DATABASE_URL` | pooled connection (host contains `-pooler`) | the app's queries |
| `DATABASE_URL_UNPOOLED` | direct connection | Flyway migrations (they need a direct session) |

This split follows Neon's guidance: pooled connections for web application traffic, direct ones for schema migrations. Both are ordinary `postgresql://user:password@host/db?sslmode=require&channel_binding=require` URLs, which the app converts to JDBC form itself.

**Get the values** (either way):
- With the CLI (Node.js required): `npm i -g neon`, `neon login`, then `neon env pull` in the repository folder. This writes both to `.env.local`, which is git-ignored and must never be committed.
- In the Neon dashboard: **Connect** → copy the string with *Connection pooling* on (→ `DATABASE_URL`) and with it off (→ `DATABASE_URL_UNPOOLED`).

You don't create any tables: the app's Flyway migrations create them on the first start.

**Branches.** `production` is the branch the deployed app uses. For experiments, `neon checkout dev --create` makes a copy that expires after 7 days (policy in `neon.ts`). Run the app against the branch in `.env.local` with the `neon` profile:
```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo,neon"
```
`neon checkout production` switches `.env.local` back.

The older `DB_URL` / `DB_USER` / `DB_PASSWORD` variables still work and take precedence when set, for example for Docker Compose or another host.

## 2. App on Render
1. Sign up at https://render.com with your GitHub account and allow it to access the `qr-asset-manager-thesis` repository.
2. **New → Blueprint**, choose the repository. Render reads [`render.yaml`](../render.yaml) and proposes the web service `qr-asset-manager` (free, Docker, Frankfurt).
   - If you prefer clicking: **New → Web Service** → the repo → Runtime *Docker* → Instance type *Free* → Health check path `/actuator/health`.
3. Fill in the environment variables it asks for:
   | Variable | Value |
   |---|---|
   | `DATABASE_URL` | the `DATABASE_URL` line from `.env.local` (without the quotes) |
   | `DATABASE_URL_UNPOOLED` | the `DATABASE_URL_UNPOOLED` line from `.env.local` (without the quotes) |
   | `APP_BASE_URL` | the service address, e.g. `https://qr-asset-manager.onrender.com`. Render shows it after the service is created. You can set it afterwards; the app restarts when it changes |
   | `ADMIN_INITIAL_PASSWORD` | a long password of your choice for the first `admin` login |
   | `DEMO_PASSWORD` | password for the demo accounts `it` and `viewer` |
   | `SPRING_PROFILES_ACTIVE` | `demo` (already set by the blueprint): fills the empty database with fictional data. Remove it before entering real data |
4. **Apply / Create.** The first build takes about 5-10 minutes. When the log shows `Started QrAssetManagerApplication`, open the address and log in as `admin`.

These values live only in the Render dashboard, never in Git.

## 3. Check it works
- `https://<your-service>.onrender.com/actuator/health` → `{"status":"UP"}`
- Log in and open **Paneli**: the dashboard shows the demo data.
- On a phone, open the address, go to **Skano**, tap **Hap kamerën**, and allow the camera. HTTPS makes it work.
- Print one label (**Asetet** → tick one → **Printo etiketat e zgjedhura**) and scan it with the phone's normal camera app: it opens the login page, then the asset.

## 4. Updates
Push to `main` → GitHub Actions runs the tests → when they pass, Render builds and deploys the new version automatically (`autoDeployTrigger: checksPass` in `render.yaml`). The database and its data stay as they are, and new Flyway migrations run on start.

## 5. Before a demo
The free service sleeps when idle. About 10 minutes before you need it, wake it up in one of two ways:
- GitHub → **Actions → Warm up → Run workflow**, with your Render address; or
- simply open the address in a browser and wait until the login page appears.

## 6. QR labels and the address
Labels contain `APP_BASE_URL/a/<TAG>`. **Print labels only after the address is final.** If the address ever changes, the in-app scanner still reads old labels (it only uses the tag), but the phone's own camera app would open the old address.

## Fallbacks
- **Laptop only (offline):** `docker compose up --build` runs everything locally ([running-locally.md](running-locally.md)). Scanning works with the laptop webcam on `localhost`, or with "scan from a photo" on a phone in the same Wi-Fi.
- **Temporary public HTTPS from the laptop:** a Cloudflare quick tunnel (`cloudflared tunnel --url http://localhost:8080`).
- **Other hosts:** any platform that runs a Docker image and gives HTTPS works (for example Koyeb or Fly.io), with the same environment variables.
