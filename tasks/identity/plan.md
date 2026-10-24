# Implementation Plan: identity (AgendaFácil)

Spec: [../../SPEC-identity.md](../../SPEC-identity.md)
Task list: [todo.md](todo.md)

## Overview

Build register/login/refresh/logout with JWT, backed by Postgres via Flyway migrations `V2`/`V3`/`V4`, replacing `platform-setup`'s placeholder `SecurityConfig` with real JWT validation. Every task lands in a working, testable state (vertical slices: schema+service+endpoint+test together, not layer-by-layer).

## Architecture Decisions

- **HS256 (symmetric secret), not RSA.** This service both issues and validates its own tokens — no other service needs to verify them independently, so there's no reason for asymmetric keys. The secret comes from an env var (`JWT_SECRET`), same pattern as `DB_PASSWORD` in `docker-compose.yml`. Key rotation is out of scope for this module (see Open Questions in the spec — not revisited here, nothing outstanding).
- **Refresh tokens are stored hashed, not raw**, in a `refresh_token` table — lets us revoke on logout and detect reuse (rotation), which a stateless-only JWT refresh design can't do.
- **`GlobalExceptionHandler` ships first** (Task 1), before any endpoint exists, since every later task's error-path tests depend on the `ErrorResponse` shape already existing.
- **`/api/auth/me` is a real endpoint, not a throwaway.** It's the module's proof that JWT validation + role claims work end-to-end, and it's also genuinely useful later for `client-web`'s "who am I" check — no reason to write it twice.
- **Admin seed migration (`V4`) ships with this module**, not `catalog`, since it only needs the `usuario` table `identity` already owns (per the spec's resolved Open Question #2).

## Task List

### Phase 1: Foundation
- [ ] Task 1: Global error handling (`GlobalExceptionHandler`, `ErrorResponse`)
- [ ] Task 2: `usuario` schema (`V2`) + `Usuario` entity + `UsuarioRepository` + `Perfil` enum + `PasswordEncoder` bean

### Checkpoint: Foundation
- [ ] `./mvnw test` passes (Testcontainers Postgres applies `V1`+`V2` cleanly)
- [ ] No endpoint exists yet — this phase is schema + cross-cutting infra only

### Phase 2: Register, login, JWT validation
- [ ] Task 3: Register endpoint (`POST /api/auth/register`) — depends on Task 1, Task 2
- [ ] Task 4: JWT issuing infra + login (`JwtConfig`, rewritten `SecurityConfig`, `POST /api/auth/login` issuing an access token) — depends on Task 1, Task 2
- [ ] Task 5: `GET /api/auth/me` — proves JWT validation + role claim readable — depends on Task 4

### Checkpoint: Core auth
- [ ] Register → login → call `/api/auth/me` with the access token works end-to-end in an integration test
- [ ] Swagger UI shows `/api/auth/register`, `/api/auth/login`, `/api/auth/me` with request/response schemas

### Phase 3: Refresh token lifecycle
- [ ] Task 6: `refresh_token` schema (`V3`) + `RefreshToken` entity + `RefreshTokenRepository` — depends on Task 2
- [ ] Task 7: Refresh with rotation (`POST /api/auth/refresh`); login now also issues + persists a refresh token — depends on Task 4, Task 6
- [ ] Task 8: Logout (`POST /api/auth/logout`, revokes the current refresh token) — depends on Task 7

### Checkpoint: Refresh lifecycle
- [ ] Refresh rotates the token (old refresh token rejected after use)
- [ ] Logout revokes the refresh token (rejected on next use)
- [ ] Swagger UI shows `/api/auth/refresh` and `/api/auth/logout`

### Phase 4: Seed + final checkpoint
- [ ] Task 9: Admin seed migration (`V4`, BCrypt-hashed password) + README demo-credentials note — depends on Task 2 (parallelizable with Phase 2/3)

### Checkpoint: Module complete
- [ ] All 9 tasks checked off
- [ ] Every item in SPEC-identity.md's Success Criteria is satisfied
- [ ] `docker compose up --build` still works end-to-end with the real `SecurityConfig` in place
- [ ] Human review before starting `catalog`

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Refresh token reuse/race (two requests refresh the same token concurrently) | Med | Unique constraint on `token_hash`; mark revoked in the same transaction that issues the replacement, so a second concurrent attempt fails on the revoked check |
| `SecurityConfig` rewrite breaks `platform-setup`'s infra endpoints (`/actuator/health`, `/swagger-ui/**`) | Med | Keep those `permitAll` rules intact in the rewrite (Task 4); re-run `docker compose up` checkpoint after Task 4 to confirm health checks still pass |
| HS256 secret missing/weak in an actual deploy | Low | Out of scope for local dev (env var default is fine here); flag as a `Fase 6 — Entrega` (deploy) concern, not this module's |
| Admin seed password hash computed wrong (e.g. plaintext accidentally committed) | Low | Generate the BCrypt hash via a small one-off `PasswordEncoder` call during Task 9, verify login against the seeded admin in that task's test, never commit the plaintext anywhere but a local note to the user |

## Open Questions

None outstanding — the four questions raised in SPEC-identity.md (token lifetimes, admin seeding ownership, logout scope, JWT library) were resolved during spec review. Token lifetimes (15 min access / 7 day refresh, rotated) are implemented as-proposed in Task 4/Task 7.
