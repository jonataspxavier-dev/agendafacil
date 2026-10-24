# Spec: scheduling (AgendaFácil)

Part of [capability-map.md](capability-map.md). Depends on: `identity` (auth/users), `catalog` (serviços/profissionais/jornada) — both done.

## Objective

This is the product's core: compute real availability from a professional's jornada, their existing bookings, and a service's duration, then let a CLIENTE book a slot with a hard guarantee that two people can never book the same professional for an overlapping time. Admin can mark bookings complete and list them with filters. Dashboard aggregation (today's bookings, monthly revenue, top services) is explicitly **not** this module's job — that's `reporting`, which depends on this one.

Done means: `GET /api/disponibilidade` returns real open slots for a professional+service+date; `POST /api/agendamentos` either books successfully or is rejected — and it is **impossible**, even under concurrent requests, for two `AGENDADO` bookings to overlap for the same professional (the brief's own "critério de pronto"); cancellation is blocked inside the 2-hour window; admin can list/filter bookings and mark them complete.

## Tech Stack

Same as `catalog` (Java 21, Spring Boot 4.0.9, Maven, Postgres via Testcontainers in tests). One new piece: a PostgreSQL **exclusion constraint** (`EXCLUDE USING gist`) on the `agendamento` table, requiring the `btree_gist` extension, to guarantee no two `AGENDADO` rows for the same `profissional_id` ever have overlapping time ranges — enforced by the database itself, not just application code, so it holds even under concurrent requests that no amount of service-layer `if`-checking can fully prevent on its own.

## Commands

Same as `catalog`: `./mvnw clean verify`, `./mvnw test`, `docker compose up --build` from `agendafacil/`.

## Project Structure

New files under `backend/src/main/java/com/agendafacil/backend/`:

```
entity/      Agendamento, StatusAgendamento (enum: AGENDADO, CANCELADO, CONCLUIDO)
dto/         DisponibilidadeResponse, SlotResponse, AgendamentoRequest, AgendamentoResponse
repository/  AgendamentoRepository
service/     DisponibilidadeService (slot calculation), AgendamentoService (book/cancel/concluir/list)
controller/  DisponibilidadeController (GET /api/disponibilidade), AgendamentoController
             (POST /api/agendamentos, GET /api/agendamentos/meus, PATCH .../cancelar,
             PATCH .../concluir, GET /api/admin/agendamentos)
```

New Flyway migration:
```
V9__create_agendamento.sql  -- agendamento(id, cliente_id, profissional_id, servico_id,
                             -- inicio, fim, status, preco_cobrado, criado_em)
                             -- + btree_gist extension + EXCLUDE USING gist constraint
```

## Code Style

```java
public record AgendamentoResponse(
        Long id, Long profissionalId, String profissionalNome,
        Long servicoId, String servicoNome,
        LocalDateTime inicio, LocalDateTime fim,
        StatusAgendamento status, BigDecimal precoCobrado) {}
```

`precoCobrado` is a **snapshot** of `Servico.preco` at booking time, copied into the row on creation — never read live from `servico`, since a later price change must not rewrite history.

## Testing Strategy

This is the module the brief explicitly calls out for correctness testing (`Fase 3`: "Cubra disponibilidade e conflitos com testes unitários e com Testcontainers"; critério de pronto: "Impossível criar dois agendamentos no mesmo horário... (testado)"). Per `CLAUDE.md`, this is squarely a business rule.

- **Unit tests**: slot-generation logic in `DisponibilidadeService` — jornada boundaries, service duration, slot granularity, already-booked slots excluded — isolated from HTTP/DB where the logic allows it.
- **Integration tests** (Testcontainers Postgres): the conflict guarantee itself — attempt two overlapping bookings for the same professional and assert the second is rejected (409); exact-boundary cases (booking ending exactly when another starts should succeed, not conflict); cancellation inside/outside the 2-hour window; full booking lifecycle (book → cancel, book → concluir); admin listing filters; 401/403 on role-restricted endpoints.

## Boundaries

- **Always:** re-validate every booking request server-side against the professional's real jornada and real existing bookings — never trust that a client's `inicio` came from a `disponibilidade` response; snapshot `preco_cobrado` at booking time; rely on the DB exclusion constraint as the actual correctness guarantee, with the service-layer check as a fast-path/better-error-message, not the sole defense.
- **Ask first:** changing the cancellation window away from 2 hours; adding slot granularity other than what's decided in Open Questions below; allowing ADMIN to override the cancellation window.
- **Never:** let `POST /api/agendamentos` succeed without hitting the exclusion constraint's protection (i.e., never bypass the DB-level guarantee with an ORM feature that skips the constraint); allow a CLIENTE to cancel or view another CLIENTE's bookings.

## Success Criteria

- [ ] `GET /api/disponibilidade?profissionalId=&servicoId=&data=` returns only slots that fit inside the professional's jornada for that weekday, account for the service's duration, and exclude already-booked times
- [ ] `POST /api/agendamentos` succeeds for a genuinely open slot, returns 201 with `precoCobrado` snapshotted from the service's current price
- [ ] Two concurrent/sequential requests for the same professional + overlapping time: exactly one succeeds, the other gets 409 — proven by an integration test, not just by inspection
- [ ] `PATCH /api/agendamentos/{id}/cancelar` succeeds more than 2 hours before `inicio`, fails (400/409) within the window; a CLIENTE can only cancel their own booking
- [ ] `PATCH /api/agendamentos/{id}/concluir` is `ROLE_ADMIN`-only and transitions `AGENDADO` → `CONCLUIDO`
- [ ] `GET /api/agendamentos/meus` returns only the authenticated user's own bookings
- [ ] `GET /api/admin/agendamentos` is `ROLE_ADMIN`-only, supports `data`/`status`/paging filters
- [ ] Integration tests cover the conflict guarantee, cancellation window edges, and full lifecycle against a real Testcontainers Postgres; `./mvnw test` passes
- [ ] Swagger UI shows all new operations

## Open Questions

1. **Slot granularity.** The brief doesn't specify how finely `disponibilidade` should offer start times. Proposing a fixed **30-minute** step (e.g. a 45-minute service can start at 9:00, 9:30, 10:00…) rather than stepping by the service's own duration — more realistic for a salon and more flexible for the client. Confirm, or name a different granularity.
2. **Conflict guarantee mechanism.** Proposing a Postgres `EXCLUDE USING gist` constraint (needs `btree_gist`) as the actual source of truth for "no overlapping bookings," with the service layer doing a pre-check for a fast, clean 409 before hitting the DB. This is more robust than an application-only check (which can't fully prevent races under concurrent requests) and is a good showcase piece for the portfolio. Confirm, or say if you'd rather keep it purely application-level (simpler, but weaker under true concurrency) — e.g. if the grader/reviewer environment can't install `btree_gist`.
3. **Who can cancel, and does ADMIN bypass the 2-hour window?** Proposing: CLIENTE can cancel only their own booking; ADMIN can cancel anyone's; the 2-hour rule applies to both with no override. Confirm, or say if ADMIN should be able to cancel inside the window (e.g. for a customer-service phone cancellation).
4. **Is `GET /api/disponibilidade` public (no token), like `servicos`/`profissionais`?** Proposing yes, consistent with `catalog`'s public browsing, since a visitor needs to see open slots before deciding to register. Confirm.
5. **Does `concluir` have any time constraint** (e.g. can't mark a future booking complete), or is it purely an admin judgment call? Proposing no constraint — admin's call, keeps scope simple. Confirm.
