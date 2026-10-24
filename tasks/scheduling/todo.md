# Tasks: scheduling (AgendaFácil)

Plan: [plan.md](plan.md) · Spec: [../../SPEC-scheduling.md](../../SPEC-scheduling.md)

## Task 1: `agendamento` schema + entity + repository ✅

**Description:** `V9__create_agendamento.sql` with the Postgres exclusion constraint, `Agendamento` entity, `StatusAgendamento` enum, `AgendamentoRepository`. This is the module's actual correctness guarantee — prove the constraint works before any service code exists.

**Acceptance criteria:**
- [x] `V9__create_agendamento.sql`: `CREATE EXTENSION IF NOT EXISTS btree_gist;`, `agendamento(id, cliente_id FK usuario, profissional_id FK profissional, servico_id FK servico, inicio TIMESTAMP, fim TIMESTAMP, status VARCHAR, preco_cobrado NUMERIC, criado_em TIMESTAMP DEFAULT now())`, a generated `tsrange` column, and `EXCLUDE USING gist (profissional_id WITH =, periodo WITH &&) WHERE (status = 'AGENDADO')`
- [x] `StatusAgendamento` enum: `AGENDADO`, `CANCELADO`, `CONCLUIDO`
- [x] `Agendamento` entity maps `cliente`/`profissional`/`servico` as `@ManyToOne`, `status` as `@Enumerated(STRING)`, `inicio`/`fim` as `LocalDateTime`
- [x] `AgendamentoRepository extends JpaRepository<Agendamento, Long>` with `findConflitantes(profissionalId, status, inicio, fim)` (JPQL overlap query: `a.inicio < :fim AND a.fim > :inicio`) for conflict pre-checking

**Verification:**
- [x] Tests pass: `./mvnw test` (TDD red→green, 3/3) — `V1`→`V9` apply cleanly via Testcontainers
- [x] Repository-level tests prove: a genuinely overlapping `AGENDADO` insert is rejected (`DataIntegrityViolationException`, SQLState `23P01`, confirmed in the log); back-to-back non-overlapping bookings succeed; an overlapping booking succeeds once the first is `CANCELADO` (constraint only applies `WHERE status = 'AGENDADO'`)

**Dependencies:** `identity` (`usuario`), `catalog` (`profissional`, `servico`)

**Files likely touched:**
- `backend/src/main/resources/db/migration/V9__create_agendamento.sql`
- `backend/src/main/java/com/agendafacil/backend/entity/Agendamento.java`
- `backend/src/main/java/com/agendafacil/backend/entity/StatusAgendamento.java`
- `backend/src/main/java/com/agendafacil/backend/repository/AgendamentoRepository.java`
- `backend/src/test/java/.../repository/AgendamentoRepositoryTest.java`

**Estimated scope:** Medium: 5 files

---

## Task 2: `DisponibilidadeService` slot calculation ✅

**Description:** Pure slot-calculation core (jornada windows + service duration + existing bookings → free slots) separated from DB-fetching orchestration, so the math is unit-testable without Testcontainers.

**Acceptance criteria:**
- [x] Given a professional's jornada windows for a weekday, a service duration, and existing `AGENDADO` bookings for that date, returns 30-minute-stepped candidate slots that fit inside a window and don't overlap an existing booking
- [x] Handles multiple jornada windows on the same day (split shift) — slots generated independently per window, not just the first
- [x] A slot whose end would exceed the window's `horaFim` is excluded; one ending exactly at `horaFim` is included
- [x] Unit tests (no DB, 0.1s for all 5): empty jornada → no slots; one window, no bookings → full step sequence; exact-boundary slot included; existing booking excludes only the overlapping slot, not neighbors; two windows same day → slots from both

**Verification:**
- [x] Tests pass: `./mvnw test` (TDD red→green, 5/5) and full `./mvnw clean verify`

**Dependencies:** Task 1

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/service/DisponibilidadeService.java`
- `backend/src/test/java/.../service/DisponibilidadeServiceTest.java`

**Estimated scope:** Medium: 2 files, meaningful logic

---

## Task 3: `GET /api/disponibilidade` ✅

**Description:** Public endpoint wiring `DisponibilidadeService`'s DB-fetching orchestration to HTTP: `profissionalId`, `servicoId`, `data` query params → list of available slots.

**Acceptance criteria:**
- [x] `GET /api/disponibilidade?profissionalId=&servicoId=&data=` returns 200 with no auth required
- [x] 404 if `profissionalId` or `servicoId` doesn't exist; **400** (not 404) if the professional doesn't offer that service — `InvalidRequestException`, consistent with `catalog`'s precedent for "ids exist but the combination is invalid" (adjusted from this task's original 404-for-everything wording during implementation, to match what `DisponibilidadeService` already threw from Task 2)
- [x] Integration test: a professional with a Monday 09:00-11:00 jornada and no bookings returns 4 slots for a 30-min service; an existing booking excludes exactly the overlapping slot (4 → 3)

**Verification:**
- [x] Tests pass: `./mvnw test` (TDD red→green, 4/4) and full `./mvnw clean verify`
- [x] Manual check: `docker compose up`, curled `/api/disponibilidade` against the seeded catalog data (Ana Souza, Monday) with no auth header → 200, 18 slots (09:00-18:00, 30-min steps)

**Dependencies:** Task 2

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/dto/SlotResponse.java`
- `backend/src/main/java/com/agendafacil/backend/dto/DisponibilidadeResponse.java`
- `backend/src/main/java/com/agendafacil/backend/controller/DisponibilidadeController.java`
- `backend/src/main/java/com/agendafacil/backend/security/SecurityConfig.java` (open the GET path)
- `backend/src/test/java/.../controller/DisponibilidadeControllerTest.java`

**Estimated scope:** Medium: 4-5 files

---

## Task 4: `POST /api/agendamentos` ✅

**Description:** The module's critical path. Validates the slot against real jornada/duration/existing bookings, snapshots the price, creates the row, and — this is the part that matters — proves the DB exclusion constraint actually prevents a genuine double-booking, translating the constraint violation into a clean 409.

**Acceptance criteria:**
- [x] `AgendamentoRequest(profissionalId, servicoId, inicio)`; validates: professional and service exist and are `ativo`, professional offers the service, `inicio` is in the future, the resulting slot fits the professional's jornada for that weekday and doesn't overlap an existing `AGENDADO` booking (service-layer pre-check via `findConflitantes`)
- [x] On success: 201, `AgendamentoResponse` with `precoCobrado` snapshotted from `Servico.preco`, `status = AGENDADO`
- [x] Any authenticated user can book (no `@PreAuthorize` — the default `anyRequest().authenticated()` is exactly right here)
- [x] `DataIntegrityViolationException` from the exclusion constraint is caught (around `saveAndFlush`, so it surfaces synchronously, not at commit time) and translated to `ConflitoDeHorarioException` → 409
- [x] Integration tests: book a slot → 201; the same slot again → 409; back-to-back non-overlapping slots → both 201; outside jornada → 400

**Verification:**
- [x] Tests pass: `./mvnw test` (TDD red→green, 5/5 + 1 new `GlobalExceptionHandler` case) and full `./mvnw clean verify`
- [x] Manual check: `docker compose up`, booked a real slot (201 with snapshotted price), repeated the exact same request (409 "Horário indisponível"), confirmed `/api/disponibilidade` no longer lists that slot. Swagger shows the operation.

**Note on responsibility split:** this task's own test proves the HTTP-facing behavior (book once, conflict 409s cleanly) in the normal request flow. The DB constraint's independent guarantee — that it rejects an overlapping insert even when application code is bypassed entirely — was already proven at the repository level in Task 1 and isn't re-tested here.

**Dependencies:** Task 1, Task 2

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/dto/AgendamentoRequest.java`
- `backend/src/main/java/com/agendafacil/backend/dto/AgendamentoResponse.java`
- `backend/src/main/java/com/agendafacil/backend/exception/ConflitoDeHorarioException.java`
- `backend/src/main/java/com/agendafacil/backend/exception/GlobalExceptionHandler.java` (new case)
- `backend/src/main/java/com/agendafacil/backend/service/AgendamentoService.java`
- `backend/src/main/java/com/agendafacil/backend/controller/AgendamentoController.java`
- `backend/src/test/java/.../controller/AgendamentoControllerCreateTest.java`

**Estimated scope:** Large: 7 files — the module's core; if the conflict-translation logic gets tangled with the validation logic, split validation into its own method/commit rather than redesigning the exception flow mid-task

---

## Task 5: `PATCH /api/agendamentos/{id}/cancelar` ✅

**Description:** CLIENTE cancels their own booking, ADMIN can cancel any; rejected inside the 2-hour window.

**Acceptance criteria:**
- [x] Succeeds (200, `status → CANCELADO`) when `inicio` is more than 2 hours away
- [x] Fails (400) when `inicio` is within 2 hours
- [x] A CLIENTE attempting to cancel another user's booking → 403 (`AccessDeniedException`, reusing `identity`'s existing handler); ADMIN can cancel any booking regardless of owner
- [x] Already-`CANCELADO` bookings can't be cancelled again (400, with a message naming the current status)
- [x] Integration tests cover: success path, inside-window rejection, cross-user rejection (as CLIENTE), ADMIN-cancels-anyone success, double-cancel rejection

**Verification:**
- [x] Tests pass: `./mvnw test` (TDD red→green, 5/5) and full `./mvnw clean verify`
- [x] Manual check: `docker compose up`, booked a slot, cancelled it (200), cancelled again (400, clear message), confirmed the slot reappears in `/api/disponibilidade`

**Dependencies:** Task 4

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/service/AgendamentoService.java` (add `cancelar`)
- `backend/src/main/java/com/agendafacil/backend/controller/AgendamentoController.java` (add endpoint)
- `backend/src/test/java/.../controller/AgendamentoControllerCancelTest.java`

**Estimated scope:** Medium: 3 files

---

## Task 6: `PATCH /api/agendamentos/{id}/concluir` ✅

**Description:** `ROLE_ADMIN`-only transition `AGENDADO` → `CONCLUIDO`, no time constraint.

**Acceptance criteria:**
- [x] 401 no token, 403 CLIENTE token, 200 + `status = CONCLUIDO` for ADMIN
- [x] Only valid from `AGENDADO` (not from `CANCELADO`) — 400 otherwise

**Verification:**
- [x] Tests pass: `./mvnw test` (TDD red→green, 4/4) and full `./mvnw clean verify`
- [x] Manual check: `docker compose up`, CLIENTE token → 403, ADMIN token → 200 with `status: "CONCLUIDO"`

**Dependencies:** Task 4

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/service/AgendamentoService.java` (add `concluir`)
- `backend/src/main/java/com/agendafacil/backend/controller/AgendamentoController.java` (add endpoint)
- `backend/src/test/java/.../controller/AgendamentoControllerConcluirTest.java`

**Estimated scope:** Small: 3 files

---

## Task 7: `GET /api/agendamentos/meus` ✅

**Description:** Authenticated user's own booking history.

**Acceptance criteria:**
- [x] Returns only the caller's own bookings (resolved via `AuthService.getAuthenticatedUser(jwt)`), 401 with no token
- [x] Another user's bookings never appear, verified by an integration test with two users booking different slots

**Verification:**
- [x] Tests pass: `./mvnw test` (TDD red→green, 2/2) and full `./mvnw clean verify`
- [x] Manual check: `docker compose up`, no token → 401, with token → 200 with exactly the caller's own booking. Swagger shows the operation.

**Dependencies:** Task 4

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/service/AgendamentoService.java` (add `listarMeus`)
- `backend/src/main/java/com/agendafacil/backend/controller/AgendamentoController.java` (add endpoint)
- `backend/src/test/java/.../controller/AgendamentoControllerMeusTest.java`

**Estimated scope:** Small: 3 files

---

## Task 8: `GET /api/admin/agendamentos` ✅

**Description:** `ROLE_ADMIN`-only listing with `data`/`status`/paging filters.

**Acceptance criteria:**
- [x] 401 no token, 403 CLIENTE token, 200 for ADMIN
- [x] `data` filters to bookings on that date, `status` filters by status, both combinable; paging via the existing `PageableFactory` pattern
- [x] Integration test covers each filter independently and combined

**Verification:**
- [x] Tests pass: `./mvnw test` (TDD red→green, 7/7) and full `./mvnw clean verify`
- [x] Manual check: `docker compose up`, booked two slots on different dates as a client, cancelled one, confirmed via curl with the admin token: no filters → both; `data=` → only that date's booking; `status=CANCELADO` → only the cancelled one; combined `data`+`status` → exact match or empty; `page`/`size` → `PageResponse` wrapper with correct `totalElements`. Swagger shows the operation.

**Implementation note:** the straightforward `(:param IS NULL OR ...)` JPQL pattern for optional filters failed against Postgres with `SQLState 42P18 — could not determine data type of parameter` when all filters were null in the same query. Switched `AgendamentoRepository` to `JpaSpecificationExecutor` and built the `data`/`status` predicates conditionally in `AgendamentoService` (no predicate added when a filter is absent) — this sidesteps the untyped-null-parameter issue entirely and reads more clearly than fighting Postgres's parameter type inference. Worth remembering for any future optional-filter query: prefer `Specification` over a `:param IS NULL OR` JPQL query.

**Dependencies:** Task 4

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/repository/AgendamentoRepository.java` (now `JpaSpecificationExecutor`)
- `backend/src/main/java/com/agendafacil/backend/service/AgendamentoService.java` (add `listarAdmin`/`listarAdminTodos`)
- `backend/src/main/java/com/agendafacil/backend/controller/AdminAgendamentoController.java` (new — `/api/admin/agendamentos` doesn't fit under the existing `/api/agendamentos`-mapped controller)
- `backend/src/main/java/com/agendafacil/backend/security/SecurityConfig.java` (no change needed — confirmed `/api/admin/**` falls under `anyRequest().authenticated()`, `@PreAuthorize` handles the role)
- `backend/src/test/java/.../controller/AgendamentoControllerAdminListTest.java`

**Estimated scope:** Medium: 3-4 files

---

## Checkpoint: Module complete

- [x] All 8 tasks checked off
- [x] Every item in SPEC-scheduling.md's Success Criteria is satisfied
- [x] `docker compose up --build` works end-to-end
- [ ] Human review before starting `reporting`
