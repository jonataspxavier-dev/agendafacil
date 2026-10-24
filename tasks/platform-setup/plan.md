# Implementation Plan: platform-setup (AgendaFácil)

Spec: [../../SPEC-platform-setup.md](../../SPEC-platform-setup.md)
Task list: [todo.md](todo.md)

> Module-scoped location: each module gets `tasks/<module-id>/plan.md` + `todo.md` instead of a single shared `tasks/plan.md`, since AgendaFácil builds seven modules in sequence and each needs its own plan without colliding with the others.

## Overview

Scaffold the AgendaFácil repo — git, backend (Spring Boot/Maven), frontend (Vite/React/TS), Docker Compose, Flyway baseline, test runners, and CLAUDE.md — so `identity` (the next module) starts from a stack that already runs end to end, with nothing business-specific in it yet.

## Architecture Decisions

- **Spring Boot 4.0.9, not 3.x.** Spring Initializr had dropped the entire 3.x line by the time Task 2 ran (only 4.0.x/4.1.x/4.2.x-RC offered). Confirmed with the user — 4.0.9 (latest stable 4.0.x) over 4.1.2 or hand-pinning an EOL 3.5.x. Still Java 21.
- **Maven over Gradle** for the backend — matches the SPEC's default; more conventional for a Spring Boot portfolio piece.
- **Spring Boot Actuator added** (not explicitly in the original brief) to get a real `/actuator/health` endpoint for the Docker Compose healthcheck and this module's verification step.
- **Flyway baseline is a genuinely empty `V1__baseline.sql`** (no tables) — the `usuario`/`servico`/etc. schema belongs to `identity`/`catalog`, not this module. This module only proves Flyway runs against Postgres on startup.
- **Docker Compose service names are the hostnames** (`postgres`, `backend`, `frontend`) — backend's `application.yml` and frontend's API base URL reference them, not `localhost`, so the same compose file works for every later module without edits.
- **CLAUDE.md written before any code** — later modules' code style (DTOs-as-records, no entities over the wire, tests mandatory for business rules, small commits) needs to exist as a written rule before `identity` is implemented, not discovered after the fact.

## Task List

### Phase 1: Repo foundation
- [x] Task 1: Git init + top-level structure + CLAUDE.md
- [x] Task 2: Backend Spring Boot scaffold + Dockerfile

### Checkpoint: Phase 1
- [x] `./mvnw clean verify` succeeds in `backend/`
- [x] `backend/` has no business entities, only the Spring Boot skeleton
- [x] CLAUDE.md documents layering, DTO, and testing conventions

### Phase 2: Test infra + frontend
- [x] Task 3: Backend test infra (JUnit 5, Mockito, Testcontainers) with zero test cases — Testcontainers smoke test verified against a real Postgres container
- [x] Task 4: Frontend Vite+TS scaffold + libs (Router, Axios, RHF, Zod, Tailwind) + Dockerfile
- [x] Task 5: Frontend test infra (Vitest + RTL) with zero test cases

### Checkpoint: Phase 2
- [x] `./mvnw test` passes clean
- [x] `npm test` passes clean
- [x] `npm run build` and `npm run lint` succeed in `frontend/`

### Phase 3: Wire it together
- [x] Task 6: Flyway baseline migration + config pointed at Postgres — verified applying against real Postgres (Testcontainers and docker compose)
- [x] Task 7: docker-compose.yml (postgres + backend + frontend) with healthchecks — verified, all three services healthy
- [x] Task 8: README.md stub + initial commit

### Checkpoint: Complete (module done)
- [x] `docker compose up --build` starts all three services with zero manual steps
- [x] Backend `/actuator/health` returns 200
- [x] Frontend dev server responds 200
- [x] Swagger UI reachable (empty)
- [x] All SPEC-platform-setup.md success criteria checked off
- [ ] Human review before starting `identity`

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Flyway migration naming convention not fixed now causes rework when `identity` adds its first real migration | Med | Document the `V<n>__description.sql` convention and the empty-baseline pattern in CLAUDE.md in Task 1 |
| Docker networking (backend→postgres, frontend→backend) breaks silently in CI/other machines if `localhost` sneaks in | Med | Use compose service names everywhere; verify by running `docker compose up --build` from a clean state (no locally-cached `localhost` config) in Task 7 |
| Actuator/health dependency wasn't in the original brief — scope creep risk | Low | Called out explicitly as an Architecture Decision above; it's infrastructure, not a feature, so it doesn't need separate approval beyond this plan |
| Maven/Gradle or other stack deviation discovered mid-task | Low | Already resolved as an open question in the SPEC; no re-litigation expected |

## Open Questions

None outstanding — the three questions raised in SPEC-platform-setup.md (Maven vs Gradle, git remote timing, early Swagger wiring) were resolved during spec review and are captured as Architecture Decisions above.
