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

## Task 2: `DisponibilidadeService` slot calculation

**Description:** Pure slot-calculation core (jornada windows + service duration + existing bookings → free slots) separated from DB-fetching orchestration, so the math is unit-testable without Testcontainers.

**Acceptance criteria:**
- [ ] Given a professional's jornada windows for a weekday, a service duration, and existing `AGENDADO` bookings for that date, returns 30-minute-stepped candidate slots that fit inside a window and don't overlap an existing booking
- [ ] Handles multiple jornada windows on the same day (split shift) — slots generated independently per window, not just the first
- [ ] A slot whose end would exceed the window's `horaFim` is excluded
- [ ] Unit tests (no DB): empty jornada → no slots; one window, no bookings → full set of slots; one window with a booking in the middle → that slot excluded, others remain; two windows same day → slots from both

**Verification:**
- [ ] Tests pass: `./mvnw test`

**Dependencies:** Task 1

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/service/DisponibilidadeService.java`
- `backend/src/test/java/.../service/DisponibilidadeServiceTest.java`

**Estimated scope:** Medium: 2 files, meaningful logic

---

## Task 3: `GET /api/disponibilidade`

**Description:** Public endpoint wiring `DisponibilidadeService`'s DB-fetching orchestration to HTTP: `profissionalId`, `servicoId`, `data` query params → list of available slots.

**Acceptance criteria:**
- [ ] `GET /api/disponibilidade?profissionalId=&servicoId=&data=` returns 200 with no auth required
- [ ] 404 if `profissionalId` or `servicoId` doesn't exist, or the professional doesn't offer that service (not in `profissional_servico`)
- [ ] Integration test: a professional with a Monday 09:00-18:00 jornada and no bookings returns the expected slot count for a 30-min service; booking one slot removes it from a subsequent call

**Verification:**
- [ ] Tests pass: `./mvnw test`
- [ ] Manual check: `docker compose up`, curl `/api/disponibilidade` against the seeded catalog data with no auth header → 200

**Dependencies:** Task 2

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/dto/SlotResponse.java`
- `backend/src/main/java/com/agendafacil/backend/dto/DisponibilidadeResponse.java`
- `backend/src/main/java/com/agendafacil/backend/controller/DisponibilidadeController.java`
- `backend/src/main/java/com/agendafacil/backend/security/SecurityConfig.java` (open the GET path)
- `backend/src/test/java/.../controller/DisponibilidadeControllerTest.java`

**Estimated scope:** Medium: 4-5 files

---

## Task 4: `POST /api/agendamentos`

**Description:** The module's critical path. Validates the slot against real jornada/duration/existing bookings, snapshots the price, creates the row, and — this is the part that matters — proves the DB exclusion constraint actually prevents a genuine double-booking, translating the constraint violation into a clean 409.

**Acceptance criteria:**
- [ ] `AgendamentoRequest(profissionalId, servicoId, inicio)`; validates: professional and service exist and are `ativo`, professional offers the service, `inicio` is in the future, the resulting slot fits the professional's jornada for that weekday and doesn't overlap an existing `AGENDADO` booking (service-layer pre-check)
- [ ] On success: 201, `AgendamentoResponse` with `precoCobrado` snapshotted from `Servico.preco`, `status = AGENDADO`
- [ ] Any authenticated user can book (not role-restricted beyond "logged in")
- [ ] `DataIntegrityViolationException` from the exclusion constraint is caught and translated to a 409 with a clear message — not a generic 500
- [ ] Integration test: book a slot → 201; attempt to book the **exact same or overlapping** slot for the same professional via two separate requests → exactly one 201, one 409; a slot that ends exactly when another begins → both succeed (no false conflict)

**Verification:**
- [ ] Tests pass: `./mvnw test`
- [ ] Manual check: `docker compose up`, book a slot via curl with a real user token, attempt the same slot again, confirm 409

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

## Task 5: `PATCH /api/agendamentos/{id}/cancelar`

**Description:** CLIENTE cancels their own booking, ADMIN can cancel any; rejected inside the 2-hour window.

**Acceptance criteria:**
- [ ] Succeeds (200, `status → CANCELADO`) when `inicio` is more than 2 hours away
- [ ] Fails (400/409) when `inicio` is within 2 hours or in the past
- [ ] A CLIENTE attempting to cancel another user's booking → 403 (or 404, not leaking existence — pick one and be consistent); ADMIN can cancel any booking regardless of owner
- [ ] Already-`CANCELADO`/`CONCLUIDO` bookings can't be cancelled again (409 or 400)
- [ ] Integration tests cover: success path, inside-window rejection, cross-user rejection (as CLIENTE), ADMIN-cancels-anyone success, double-cancel rejection

**Verification:**
- [ ] Tests pass: `./mvnw test`
- [ ] Manual check: `docker compose up`, book a slot, cancel it, confirm `status = CANCELADO`; confirm the slot reappears in `/api/disponibilidade`

**Dependencies:** Task 4

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/service/AgendamentoService.java` (add `cancelar`)
- `backend/src/main/java/com/agendafacil/backend/controller/AgendamentoController.java` (add endpoint)
- `backend/src/test/java/.../controller/AgendamentoControllerCancelTest.java`

**Estimated scope:** Medium: 3 files

---

## Task 6: `PATCH /api/agendamentos/{id}/concluir`

**Description:** `ROLE_ADMIN`-only transition `AGENDADO` → `CONCLUIDO`, no time constraint.

**Acceptance criteria:**
- [ ] 401 no token, 403 CLIENTE token, 200 + `status = CONCLUIDO` for ADMIN
- [ ] Only valid from `AGENDADO` (not from `CANCELADO`) — 409/400 otherwise

**Verification:**
- [ ] Tests pass: `./mvnw test`

**Dependencies:** Task 4

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/service/AgendamentoService.java` (add `concluir`)
- `backend/src/main/java/com/agendafacil/backend/controller/AgendamentoController.java` (add endpoint)
- `backend/src/test/java/.../controller/AgendamentoControllerConcluirTest.java`

**Estimated scope:** Small: 3 files

---

## Task 7: `GET /api/agendamentos/meus`

**Description:** Authenticated user's own booking history.

**Acceptance criteria:**
- [ ] Returns only the caller's own bookings (resolved via `AuthService.getAuthenticatedUser(jwt)`), 401 with no token
- [ ] Another user's bookings never appear, verified by an integration test with two users

**Verification:**
- [ ] Tests pass: `./mvnw test`

**Dependencies:** Task 4

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/service/AgendamentoService.java` (add `listarMeus`)
- `backend/src/main/java/com/agendafacil/backend/controller/AgendamentoController.java` (add endpoint)
- `backend/src/test/java/.../controller/AgendamentoControllerMeusTest.java`

**Estimated scope:** Small: 3 files

---

## Task 8: `GET /api/admin/agendamentos`

**Description:** `ROLE_ADMIN`-only listing with `data`/`status`/paging filters.

**Acceptance criteria:**
- [ ] 401 no token, 403 CLIENTE token, 200 for ADMIN
- [ ] `data` filters to bookings on that date, `status` filters by status, both combinable; paging via the existing `PageableFactory` pattern
- [ ] Integration test covers each filter independently and combined

**Verification:**
- [ ] Tests pass: `./mvnw test`
- [ ] Manual check: `docker compose up`, book a couple of slots as different users, confirm admin listing + filters via curl with the admin token

**Dependencies:** Task 4

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/service/AgendamentoService.java` (add `listarAdmin`)
- `backend/src/main/java/com/agendafacil/backend/controller/AgendamentoController.java` (add endpoint)
- `backend/src/main/java/com/agendafacil/backend/security/SecurityConfig.java` (no change expected — `/api/admin/**` already falls under `anyRequest().authenticated()`, `@PreAuthorize` handles the role)
- `backend/src/test/java/.../controller/AgendamentoControllerAdminListTest.java`

**Estimated scope:** Medium: 3-4 files

---

## Checkpoint: Module complete

- [ ] All 8 tasks checked off
- [ ] Every item in SPEC-scheduling.md's Success Criteria is satisfied
- [ ] `docker compose up --build` works end-to-end
- [ ] Human review before starting `reporting`
