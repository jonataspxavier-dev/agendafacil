# Implementation Plan: scheduling (AgendaFácil)

Spec: [../../SPEC-scheduling.md](../../SPEC-scheduling.md)
Task list: [todo.md](todo.md)

## Overview

Build `disponibilidade` (availability calculation) and the full `agendamento` lifecycle (book/cancel/concluir/list), backed by Flyway migration `V9` with a Postgres exclusion constraint as the actual source of truth for "no overlapping bookings." This is the module the brief calls out explicitly for correctness testing.

## Architecture Decisions

- **`inicio`/`fim` are `TIMESTAMP` (no timezone) + `LocalDateTime`, not `TIMESTAMPTZ`/`Instant`** like `identity`'s `criado_em` columns. This app has no multi-timezone requirement (one local business), so plain wall-clock time avoids any JVM-vs-Postgres-session timezone subtlety and pairs naturally with `tsrange` for the exclusion constraint (not `tstzrange`, which is for `TIMESTAMPTZ`).
- **Exclusion constraint, not just an application check.** `V9` adds `CREATE EXTENSION IF NOT EXISTS btree_gist;` and `EXCLUDE USING gist (profissional_id WITH =, periodo WITH &&) WHERE (status = 'AGENDADO')` on a generated `tsrange` column. The service layer still pre-checks for a fast, clean 409 in the common case, but the constraint is what actually makes concurrent double-booking impossible — the service layer alone cannot guarantee that under real concurrency.
- **30-minute slot granularity**, stepped independently within each of a professional's jornada windows for the requested day (a professional can have more than one jornada row per weekday — a split shift — and the algorithm must generate slots inside each window separately, not assume one window per day).
- **`preco_cobrado` snapshots `Servico.preco` at booking time.** Never read live from `servico` on an existing `agendamento`.
- **Ownership resolution reuses `identity`'s `AuthService.getAuthenticatedUser(Jwt)`** (already a `@Service` bean) rather than duplicating JWT-subject-to-`Usuario` lookup logic in `AgendamentoService`.
- **Slot calculation has a pure, DB-free core.** `DisponibilidadeService` separates "fetch jornadas + existing bookings from the DB" from "given those, compute free slots" — the latter is unit-testable without Testcontainers.

## Task List

### Phase 1: Foundation
- [x] Task 1: `agendamento` schema (`V9`, exclusion constraint) + `Agendamento` entity + `StatusAgendamento` enum + `AgendamentoRepository` — depends on `identity`'s `usuario`, `catalog`'s `profissional`/`servico`

### Checkpoint: Foundation
- [x] `./mvnw test` passes (Testcontainers Postgres applies `V1`-`V9` cleanly, `btree_gist` extension created successfully)
- [x] A repository-level test proves the exclusion constraint itself rejects a raw overlapping insert (before any service-layer code exists to pre-check anything)

### Phase 2: Disponibilidade
- [x] Task 2: `DisponibilidadeService` slot calculation (pure core + DB-fetching orchestration) — depends on Task 1
- [x] Task 3: `GET /api/disponibilidade` (public) — depends on Task 2

### Checkpoint: Disponibilidade
- [x] Unit tests cover jornada boundaries, duration, multiple jornada windows per day, already-booked exclusion
- [x] Swagger shows `/api/disponibilidade`

### Phase 3: Booking creation (the critical path)
- [x] Task 4: `POST /api/agendamentos` — validation, conflict pre-check, exclusion-constraint-violation → 409 translation, `preco_cobrado` snapshot — depends on Task 1, Task 2 (reuses jornada/servico validation)

### Checkpoint: Booking creation
- [x] Integration test proves two overlapping booking attempts for the same professional: exactly one succeeds, the other 409s
- [x] Boundary case (one booking ends exactly when another starts) succeeds, not rejected
- [x] Swagger shows `POST /api/agendamentos`

### Phase 4: Lifecycle + listing
- [x] Task 5: `PATCH /api/agendamentos/{id}/cancelar` (ownership check, 2-hour window) — depends on Task 4
- [x] Task 6: `PATCH /api/agendamentos/{id}/concluir` (`ROLE_ADMIN`) — depends on Task 4
- [x] Task 7: `GET /api/agendamentos/meus` (own bookings only) — depends on Task 4
- [x] Task 8: `GET /api/admin/agendamentos` (`ROLE_ADMIN`, `data`/`status`/paging filters) — depends on Task 4

### Checkpoint: Module complete
- [x] All 8 tasks checked off
- [x] Every item in SPEC-scheduling.md's Success Criteria is satisfied
- [x] `docker compose up --build` still works end-to-end
- [ ] Human review before starting `reporting`

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| `btree_gist` extension unavailable in some Postgres environment (e.g. a restricted managed DB later) | Med | Confirmed available in the official `postgres:17-alpine` image used by both Testcontainers and `docker-compose.yml` (superuser-equivalent role can `CREATE EXTENSION`); flag as a `Fase 6` deploy concern if a managed Postgres provider restricts it, not blocking now |
| Exclusion constraint violation surfaces as a generic 500 if the wrong exception type is caught | High | `org.postgresql.util.PSQLException` (SQLSTATE `23P01`) wrapped in Spring's `DataIntegrityViolationException` — catch that specific type in `AgendamentoService.criar`, translate to a dedicated `ConflitoDeHorarioException` → 409, verified by an integration test that triggers the real DB constraint (not a mocked exception) |
| Slot algorithm assumes one jornada window per day, silently drops split-shift availability | Med | `catalog`'s schema already allows multiple `jornada` rows per `(profissional_id, dia_semana)`; `DisponibilidadeService` iterates all matching windows for the day, covered by a dedicated unit test with two windows on the same day |
| `TIMESTAMP` (no tz) choice diverges from `identity`'s `TIMESTAMPTZ` convention | Low | Deliberate, documented above and in `CLAUDE.md` once implemented — this app has no multi-timezone requirement, and consistency with `tsrange` (not `tstzrange`) simplifies the exclusion constraint |

## Open Questions

None outstanding — the five questions raised in SPEC-scheduling.md (slot granularity, conflict guarantee mechanism, cancellation permissions, disponibilidade's auth requirement, concluir's time constraints) were resolved during spec review and are captured as Architecture Decisions above.
