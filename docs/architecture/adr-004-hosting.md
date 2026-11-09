# ADR 004: Hosting on Render (app) + Neon (database), with a local fallback

**Context.** GitHub Pages can't run a Spring Boot app with PostgreSQL. Requirements:
- free;
- HTTPS (phone browsers allow the camera only on secure origins);
- a URL stable enough for printed labels;
- deploys from GitHub;
- survives the December evaluation;
- has an offline plan for presenting.

Free-tier terms checked on 2026-11-03:
- **Render:** a free Docker web service sleeps after 15 min idle (cold start of about a minute) and has 750 instance-hours a month. Its free Postgres **expires after 30 days**.
- **Neon:** free Postgres with no expiry (0.5 GB). It suspends when idle, with a short wake-up.
- **Fly.io:** no free tier for new users.
- **Railway:** trial credit only.
- **Koyeb:** a free instance, but a card is required.

**Decision.**
- **Render** free web service built from our Dockerfile, auto-deploying `main` after CI passes, at a `*.onrender.com` URL. The database is **Neon** free Postgres, using the direct (not pooled) connection URL.
- **Cold starts:** a warm-up (workflow plus script) 10 minutes before any demo or evaluation session.
- **Fallback host:** Koyeb, or a Cloudflare quick tunnel from the laptop.
- **Offline fallback:** `docker compose --profile demo up` on the laptop. Scanning works with the laptop webcam on `localhost` (a secure origin), plus a "scan from photo" mode on the phone that also works over plain HTTP.

**Consequences.**
- No card is needed and nothing costs money.
- The labels' URL is stable as long as the Render service exists.
- The free tiers are re-checked before deploying and before anything is printed.
- The ephemeral disk means no files are stored on the server (QR images are generated on request; imports are processed in memory).
