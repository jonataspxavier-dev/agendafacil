# Tasks: identity (AgendaFácil)

Plan: [plan.md](plan.md) · Spec: [../../SPEC-identity.md](../../SPEC-identity.md)

## Task 1: Global error handling ✅

**Description:** Add `GlobalExceptionHandler` (`@RestControllerAdvice`) and the `ErrorResponse` record, so every later task's error-path tests (409 duplicate, 401, 400 validation) have a shape to assert against.

**Acceptance criteria:**
- [x] `ErrorResponse(Instant timestamp, int status, String error, String message, String path)` exists in `dto/`
- [x] `GlobalExceptionHandler` maps: Bean Validation failures → 400, a custom `DuplicateEmailException` → 409, `BadCredentialsException`/auth failures → 401, unhandled → 500 — all producing `ErrorResponse`
- [x] Unit test calling the handler methods directly with fake/mocked exceptions (TDD: written red, then made green)

**Verification:**
- [x] Tests pass: `./mvnw test` (4/4 in `GlobalExceptionHandlerTest`, full `./mvnw clean verify` also green)

**Dependencies:** None

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/dto/ErrorResponse.java`
- `backend/src/main/java/com/agendafacil/backend/exception/GlobalExceptionHandler.java`
- `backend/src/main/java/com/agendafacil/backend/exception/DuplicateEmailException.java`
- `backend/src/test/java/.../exception/GlobalExceptionHandlerTest.java`

**Estimated scope:** Medium: 3-5 files

---

## Task 2: `usuario` schema + entity + repository ✅

**Description:** `V2__create_usuario.sql` Flyway migration, `Usuario` JPA entity, `Perfil` enum (ADMIN/CLIENTE), `UsuarioRepository`, and a `PasswordEncoder` bean (BCrypt).

**Acceptance criteria:**
- [x] `V2__create_usuario.sql`: `usuario(id, nome, email UNIQUE NOT NULL, senha_hash, perfil, criado_em)`
- [x] `Usuario` entity maps 1:1 to the table; `perfil` is the `Perfil` enum stored as a string column (`@Enumerated(EnumType.STRING)`), not a native Postgres enum
- [x] `UsuarioRepository extends JpaRepository<Usuario, Long>` with `findByEmail(String)`
- [x] `PasswordEncoder` bean (`BCryptPasswordEncoder`) registered in `SecurityConfig`

**Verification:**
- [x] Tests pass: `./mvnw test` — Testcontainers Postgres applied `V1` then `V2` cleanly (log: "Migrating schema \"public\" to version \"2 - create usuario\"")
- [x] Manual check: `UsuarioRepositoryTest` (TDD, written red first) saves and reads back a `Usuario` by email; full `./mvnw clean verify` green

**Dependencies:** None (parallelizable with Task 1)

**Files likely touched:**
- `backend/src/main/resources/db/migration/V2__create_usuario.sql`
- `backend/src/main/java/com/agendafacil/backend/entity/Usuario.java`
- `backend/src/main/java/com/agendafacil/backend/entity/Perfil.java`
- `backend/src/main/java/com/agendafacil/backend/repository/UsuarioRepository.java`
- `backend/src/main/java/com/agendafacil/backend/security/SecurityConfig.java` (add `PasswordEncoder` bean)

**Estimated scope:** Medium: 3-5 files

---

## Task 3: Register endpoint ✅

**Description:** `POST /api/auth/register` creates a CLIENTE user. Validates input, hashes the password, rejects duplicate emails.

**Acceptance criteria:**
- [x] `RegisterRequest(String nome, String email, String senha)` with Bean Validation (`@Email`, `@NotBlank`, min length on `senha`)
- [x] `AuthService.register(...)` hashes the password, sets `perfil = CLIENTE`, throws `DuplicateEmailException` on existing email
- [x] `AuthController.register` returns 201 with the created user's id/email/nome (never the hash) — `UsuarioResponse`
- [x] Integration test: register → 201; register same email again → 409 with `ErrorResponse`; invalid email → 400

**Verification:**
- [x] Tests pass: `./mvnw test` (TDD red→green) and full `./mvnw clean verify`
- [x] Manual check: `docker compose up`, curl `/api/auth/register` → 201, repeat → 409 (confirmed against the real container stack)

**Also required (discovered during implementation, not originally listed):**
- `/api/auth/**` added to `SecurityConfig`'s `permitAll` list (register must be publicly reachable before Task 4's full JWT rewrite)
- CSRF disabled + `SessionCreationPolicy.STATELESS` set — Spring Security's default CSRF protection was blocking the POST with 403; this is correct for a stateless JSON API regardless, not a one-off hack
- Test's `ObjectMapper` import fixed to `tools.jackson.databind.ObjectMapper` (Spring Boot 4 default Jackson 3, not classic `com.fasterxml.jackson`) — now documented in `CLAUDE.md`

**Dependencies:** Task 1, Task 2

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/dto/RegisterRequest.java`
- `backend/src/main/java/com/agendafacil/backend/dto/UsuarioResponse.java`
- `backend/src/main/java/com/agendafacil/backend/service/AuthService.java`
- `backend/src/main/java/com/agendafacil/backend/controller/AuthController.java`
- `backend/src/test/java/.../controller/AuthControllerRegisterTest.java`

**Estimated scope:** Medium: 3-5 files

---

## Task 4: JWT issuing infra + login ✅

**Description:** `JwtConfig` (HS256 `JwtEncoder`/`JwtDecoder` beans from a `JWT_SECRET` env var), rewritten `SecurityConfig` (JWT resource-server auth, keeping `platform-setup`'s infra `permitAll` rules), and `POST /api/auth/login` issuing an access token (15 min) with the user's id/role as claims. Refresh token issuance is Task 7 — this task's login returns just the access token for now (`AuthResponse.refreshToken` added in Task 7).

**Acceptance criteria:**
- [x] `JwtConfig` provides `JwtEncoder`/`JwtDecoder` beans using an HMAC-SHA256 key from `JWT_SECRET` (env var, local-dev default in `application.yml`)
- [x] `SecurityConfig` uses `oauth2ResourceServer(oauth2 -> oauth2.jwt(...))` with a `JwtAuthenticationConverter` mapping the `role` claim to a `ROLE_*` authority; `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**`, `/api/auth/**` are `permitAll`; everything else requires authentication
- [x] Access token claims include `sub` (user id), `email`, `role`; expires in 15 minutes (`app.jwt.access-token-ttl-minutes`)
- [x] `AuthController.login` returns 200 with the access token on correct credentials, and the `GlobalExceptionHandler`'s 401 shape on wrong password or unknown email
- [x] Integration test: login with correct/wrong password/unknown email → 200/401/401; token claims asserted directly from the decoded JWT payload

**Verification:**
- [x] Tests pass: `./mvnw test` (TDD red→green, 4/4 in `AuthControllerLoginTest`) and full `./mvnw clean verify`
- [x] Manual check: `docker compose up`, curl register → login → got a real JWT; `/actuator/health` (200) and `/swagger-ui.html` (200) still work unauthenticated — `SecurityConfig` rewrite did not regress `platform-setup`'s checkpoint

**Also required (discovered during implementation):** no third-party JWT library needed — `spring-security-oauth2-jose` + `nimbus-jose-jwt` came transitively via `spring-boot-starter-security-oauth2-resource-server`, confirming the SPEC's Open Question #4 decision was viable without extra dependencies.

**Dependencies:** Task 1, Task 2

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/security/JwtConfig.java`
- `backend/src/main/java/com/agendafacil/backend/security/SecurityConfig.java` (rewrite)
- `backend/src/main/java/com/agendafacil/backend/dto/LoginRequest.java`
- `backend/src/main/java/com/agendafacil/backend/dto/AuthResponse.java`
- `backend/src/main/java/com/agendafacil/backend/service/AuthService.java` (add `login`)
- `backend/src/main/java/com/agendafacil/backend/controller/AuthController.java` (add `login`)
- `backend/src/test/java/.../controller/AuthControllerLoginTest.java`

**Estimated scope:** Large: 5-8 files — acceptable here since the vertical slice (JWT infra + login) can't usefully split smaller without leaving the system non-functional; if it proves unwieldy during implementation, split `JwtConfig`+`SecurityConfig` into its own commit before `AuthService.login`/`AuthController.login`

---

## Task 5: `GET /api/auth/me` ✅

**Description:** Protected endpoint returning the authenticated user's own profile. Proves the JWT filter chain and role claim work end-to-end.

**Acceptance criteria:**
- [x] `GET /api/auth/me` returns 401 with no token, 200 with a valid token
- [x] Response body includes `nome`, `email`, `perfil` (no password hash) — reuses `UsuarioResponse` DTO from Task 3
- [x] Integration test: register → login → call `/api/auth/me` with the access token → 200 with matching data; invalid token → 401

**Verification:**
- [x] Tests pass: `./mvnw test` (TDD red→green, 3/3) and full `./mvnw clean verify`
- [x] Manual check: `docker compose up`, register→login→`/me` without token (401) and with token (200) via curl; `/v3/api-docs` confirms all three `/api/auth/*` operations are documented

**Also required:** narrowed `SecurityConfig`'s `permitAll` from `/api/auth/**` to the two actually-public paths (`/api/auth/register`, `/api/auth/login`), so `/me` (and later `/logout`) fall under `anyRequest().authenticated()`.

**Dependencies:** Task 4

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/controller/AuthController.java` (add `me`)
- `backend/src/test/java/.../controller/AuthControllerMeTest.java`

**Estimated scope:** Small: 1-2 files

---

## Task 6: `refresh_token` schema + entity + repository ✅

**Description:** `V3__create_refresh_token.sql` migration and the `RefreshToken` entity/repository. Stores a hash of the token, not the raw value.

**Acceptance criteria:**
- [x] `V3__create_refresh_token.sql`: `refresh_token(id, usuario_id FK → usuario, token_hash UNIQUE NOT NULL, expira_em, revogado BOOLEAN DEFAULT false, criado_em)`, plus an index on `usuario_id`
- [x] `RefreshToken` entity + `RefreshTokenRepository` with `findByTokenHash(String)`

**Verification:**
- [x] Tests pass: `./mvnw test` (TDD red→green, 2/2) — Testcontainers Postgres applied `V1`→`V3` cleanly; full `./mvnw clean verify` also green

**Dependencies:** Task 2

**Files likely touched:**
- `backend/src/main/resources/db/migration/V3__create_refresh_token.sql`
- `backend/src/main/java/com/agendafacil/backend/entity/RefreshToken.java`
- `backend/src/main/java/com/agendafacil/backend/repository/RefreshTokenRepository.java`

**Estimated scope:** Small: 1-2 files

---

## Task 7: Refresh with rotation ✅

**Description:** `POST /api/auth/refresh` validates a refresh token, issues a new access+refresh pair, revokes the old refresh token (rotation). Login (Task 4) is extended to also issue and persist a refresh token.

**Acceptance criteria:**
- [x] `AuthService.login` now persists a hashed refresh token (7-day expiry, SHA-256 deterministic hash — not BCrypt, which can't be looked up by exact match) alongside issuing the access token; `AuthResponse` includes both
- [x] `AuthService.refresh(request)`: looks up by hash, rejects if not found/expired/revoked (401), otherwise revokes the old row and issues+persists a new refresh token + new access token
- [x] Integration tests: refresh with a valid token → 200 with new tokens; refresh with the now-revoked old token → 401; refresh with an expired token → 401; unknown token → 401

**Verification:**
- [x] Tests pass: `./mvnw test` (TDD red→green, 4/4) and full `./mvnw clean verify`
- [x] Manual check: `docker compose up`, login, refresh (200, new pair), reuse the original refresh token → 401

**Bug found and fixed during manual verification:** `GlobalExceptionHandler.handleBadCredentials` hardcoded the message "E-mail ou senha inválidos" for every `BadCredentialsException`, which was wrong/confusing for refresh failures (showed a login-specific message for a refresh-token error). Fixed to use `ex.getMessage()` so each throw site's message comes through correctly. Re-verified via docker compose — refresh failures now say "Refresh token inválido".

**Dependencies:** Task 4, Task 6

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/dto/RefreshRequest.java`
- `backend/src/main/java/com/agendafacil/backend/service/AuthService.java` (update `login`, add `refresh`)
- `backend/src/main/java/com/agendafacil/backend/controller/AuthController.java` (add `refresh`)
- `backend/src/test/java/.../controller/AuthControllerRefreshTest.java`

**Estimated scope:** Medium: 3-5 files

---

## Task 8: Logout ✅

**Description:** `POST /api/auth/logout` revokes the caller's current refresh token.

**Acceptance criteria:**
- [x] `AuthService.logout(jwt, request)` marks the matching `refresh_token` row revoked — requires a valid access token (not in `SecurityConfig`'s `permitAll`) and checks the token belongs to the authenticated user before revoking (idempotent no-op otherwise, not an error, so it doesn't leak whether a token exists)
- [x] Integration test: login → logout → refresh with that token → 401; logout without access token → 401; logout cannot revoke another user's refresh token

**Verification:**
- [x] Tests pass: `./mvnw test` (TDD red→green, 3/3) and full `./mvnw clean verify`
- [x] Manual check: `docker compose up`, login → logout (204) → refresh with that token (401); `/v3/api-docs` shows all five `/api/auth/*` operations

**Design decision beyond the original todo:** logout requires an authenticated request (access token) and verifies the refresh token being revoked belongs to that same user, rather than accepting any refresh token unauthenticated — closes an otherwise-easy cross-user token revocation path.

**Dependencies:** Task 7

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/service/AuthService.java` (add `logout`)
- `backend/src/main/java/com/agendafacil/backend/controller/AuthController.java` (add `logout`)
- `backend/src/test/java/.../controller/AuthControllerLogoutTest.java`

**Estimated scope:** Small: 1-2 files

---

## Task 9: Admin seed migration ✅

**Description:** `V4__seed_admin.sql` inserts one ADMIN user with a pre-computed BCrypt hash. Document the demo credentials in the README.

**Acceptance criteria:**
- [x] `V4__seed_admin.sql` inserts one `usuario` row with `perfil = 'ADMIN'` and a BCrypt hash — computed via a throwaway `PasswordEncoder.encode(...)` call in a scratch test, printed, copied into the migration, then the scratch test deleted (never hand-written)
- [x] README has a "Demo credentials" section (`admin@agendafacil.com` / `AdminAgenda123!`) — plaintext only in the README, never in the migration or code
- [x] Integration test: login as the seeded admin succeeds, `/api/auth/me` confirms `perfil: "ADMIN"`

**Verification:**
- [x] Tests pass: `./mvnw test` (TDD red→green, `AdminSeedTest`) and full `./mvnw clean verify`
- [x] Manual check: `docker compose up`, logged in with the documented demo credentials → 200, `/api/auth/me` confirmed `ADMIN` role

**Dependencies:** Task 2 (parallelizable with Phase 2/3)

**Files likely touched:**
- `backend/src/main/resources/db/migration/V4__seed_admin.sql`
- `README.md`
- `backend/src/test/java/.../AdminSeedTest.java`

**Estimated scope:** Small: 1-2 files

---

**Bug found and fixed during final module checkpoint review:** an authenticated request to a genuinely unmapped route (e.g. `/api/auth/rota-que-nao-existe`) returned 500 instead of 404 — `GlobalExceptionHandler`'s catch-all `@ExceptionHandler(Exception.class)` was swallowing Spring's `NoResourceFoundException`. Added a specific handler for it (404, same `ErrorResponse` shape). This directly closed SPEC-identity.md's success criterion "every error response (validation failure, 401, 409, 404) uses the same ErrorResponse shape," which hadn't actually been exercised until this check. TDD: test added red first, then the handler made it green. Re-verified via docker compose.

## Checkpoint: Module complete

- [x] All 9 tasks checked off
- [x] Every item in SPEC-identity.md's Success Criteria is satisfied
- [x] `docker compose up --build` works end-to-end with the real `SecurityConfig`
- [ ] Human review before starting `catalog`
