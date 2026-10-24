# Spec: identity (AgendaFácil)

Part of [capability-map.md](capability-map.md). Depends on: `platform-setup` (done — see [SPEC-platform-setup.md](SPEC-platform-setup.md)).

## Objective

Let a person register as a CLIENTE, and let an ADMIN account exist, so every other module can trust "who is this request from and what role do they have" without touching auth logic themselves. Concretely: register, login, refresh, and a global error shape — the first business-logic module, and the first to actually enforce `SecurityConfig` rules instead of the `platform-setup` placeholder that just opened infra endpoints.

Done means: a client can register, log in, get a short-lived access token + a longer-lived refresh token, use the access token to call a protected endpoint, refresh it when it expires, and every error (validation, auth failure, not found) comes back in one consistent JSON shape that every later module reuses.

## Tech Stack

Same as `platform-setup` (Java 21, Spring Boot 4.0.9, Maven), plus for this module specifically:

- `spring-boot-starter-security-oauth2-resource-server` (validates incoming JWTs) — **confirmed available for Boot 4.0.9** via Spring Initializr
- `spring-security-oauth2-jose` (issues/signs JWTs via `NimbusJwtEncoder`) — standard Spring Security module, not a Boot starter; exact transitive wiring confirmed during Plan/Tasks, not here
- BCrypt via `PasswordEncoder` (already available through `spring-boot-starter-security`)
- No third-party JWT library (`jjwt` etc.) — using Spring Security's own JOSE support instead, since it's better integrated with `SecurityConfig` and more representative of production Spring code for a portfolio piece

## Commands

Same as `platform-setup`: `./mvnw clean verify`, `./mvnw test`, `docker compose up --build` from `agendafacil/`.

## Project Structure

New files under `backend/src/main/java/com/agendafacil/backend/`:

```
entity/      Usuario, RefreshToken (JPA entities)
dto/         RegisterRequest, LoginRequest, RefreshRequest, AuthResponse, ErrorResponse
repository/  UsuarioRepository, RefreshTokenRepository
service/     AuthService (register/login/refresh/logout), UsuarioDetailsService
controller/  AuthController (POST /api/auth/register|login|refresh|logout)
security/    JwtConfig (encoder/decoder beans), JwtAuthenticationFilter wiring via oauth2-resource-server, rewritten SecurityConfig
exception/   GlobalExceptionHandler (@RestControllerAdvice), ErrorResponse-producing exception types
```

New Flyway migrations under `backend/src/main/resources/db/migration/`:
```
V2__create_usuario.sql        -- usuario(id, nome, email UNIQUE, senha_hash, perfil, criado_em)
V3__create_refresh_token.sql  -- refresh_token(id, usuario_id FK, token_hash UNIQUE, expira_em, revogado, criado_em)
```

## Code Style

DTOs as records (per CLAUDE.md), never return `Usuario` from a controller:

```java
public record AuthResponse(String accessToken, String refreshToken, long expiresInSeconds) {}

public record ErrorResponse(Instant timestamp, int status, String error, String message, String path) {}
```

`Usuario.perfil` is a Java enum (`Perfil { ADMIN, CLIENTE }`) mapped to a `varchar` column — not a native Postgres enum type, so adding a third role later is a data migration, not a schema-type migration.

## Testing Strategy

Per `CLAUDE.md`, auth flows are explicitly a business rule requiring tests — and the original brief's Fase 2 prompt calls for integration tests on auth flows specifically. Using `platform-setup`'s already-wired Testcontainers Postgres:

- **Integration tests** (`@SpringBootTest` + `MockMvc`/`WebTestClient`, real Postgres via Testcontainers): register → 201, duplicate email → 409, login with right/wrong password → 200/401, access a protected endpoint with/without a valid token → 200/401, refresh with a valid/expired/revoked token → 200/401, refresh token rotation (old token can't be reused after refresh).
- **Unit tests**: `AuthService` password hashing/verification, JWT claims (role, subject, expiry) via Mockito where a real context isn't needed.
- No test is required for the DTOs themselves (infra, not business logic — same rule as `platform-setup`).

## Boundaries

- **Always:** hash passwords with BCrypt before persisting, never log raw passwords or tokens, return the same `ErrorResponse` shape for every error type, keep refresh tokens single-use (rotate on every refresh, store only a hash of the token server-side — not the raw token).
- **Ask first:** access/refresh token lifetimes if you want something other than the defaults proposed in Open Questions below; adding a third role beyond ADMIN/CLIENTE; any change to the `usuario` table shape after `V2` ships (Flyway migrations are append-only per `CLAUDE.md`).
- **Never:** expose `senha_hash` or the stored refresh-token hash in any DTO or log line; accept a refresh token that's expired or already rotated; store JWT signing secrets in a committed file (env var only, matching `platform-setup`'s `DB_*` pattern in `docker-compose.yml`).

## Success Criteria

- [ ] `POST /api/auth/register` creates a CLIENTE user, returns 201, rejects a duplicate email with 409
- [ ] `POST /api/auth/login` returns an access token + refresh token on correct credentials, 401 on wrong password
- [ ] `POST /api/auth/refresh` returns a new access+refresh token pair for a valid refresh token, rejects an expired or already-used one with 401
- [ ] A protected test endpoint returns 401 without a token, 200 with a valid access token, and the token's role claim is readable by `SecurityConfig` for role checks
- [ ] Every error response (validation failure, 401, 409, 404) uses the same `ErrorResponse` JSON shape
- [ ] One ADMIN user exists after migrations run (seed, not a public registration path — `register` only creates CLIENTE accounts)
- [ ] Integration tests cover every flow above against a real Testcontainers Postgres; `./mvnw test` passes
- [ ] Swagger UI (already wired in `platform-setup`) now shows the `/api/auth/*` operations with request/response schemas

## Open Questions

1. **Token lifetimes.** Proposing access token = 15 minutes, refresh token = 7 days, rotated (single-use) on every refresh. Confirm or adjust.
2. **Admin seeding.** Brief's `Fase 1` mentions a seed migration with "1 admin, 3 profissionais, 5 serviços". Proposing identity owns seeding the one ADMIN user (via a Flyway migration with a BCrypt-hashed password, documented in README for demo login), while `catalog` seeds profissionais/serviços. Confirm this split.
3. **Logout.** Not in the original brief's endpoint list, but since refresh tokens are persisted and revocable, proposing `POST /api/auth/logout` to revoke the current refresh token. Confirm it's in scope for this module (vs. deferred/out of scope).
4. **JWT library.** Proposing Spring Security's own JOSE support (`spring-security-oauth2-jose` + `oauth2-resource-server`) over a third-party library like `jjwt`. Confirm, or say if you'd rather use `jjwt` for something more minimal/widely-copied-from-tutorials.
