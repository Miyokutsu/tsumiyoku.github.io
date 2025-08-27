# Fictional State – Full Stack Pack (Java/Spring + Infra)

Generated: {datetime.datetime.utcnow().isoformat()}Z

This repository bundles the whole setup we designed:

- Spring Boot API with OAuth2 (Discord), admin approvals, audit chain HMAC, canary ForwardAuth, rate-limits, strict
  Origin, CSP/HSTS.
- Flyway DB migrations.
- Traefik configs (security headers, blue/green, multi-services).
- Docker Compose stacks for local/dev/prod (VPS, managed DB), Fly.io, Render, GHCR workflows.
- Minimal portal (static) placeholder.

> Edit `application.yml`, `.env*` files and secrets before running.