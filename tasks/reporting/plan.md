# Implementation Plan: reporting (AgendaFácil)

Spec: [../../SPEC-reporting.md](../../SPEC-reporting.md)
Task list: [todo.md](todo.md)

## Overview

A single `ROLE_ADMIN`-only endpoint, `GET /api/admin/dashboard`, returning three figures computed live from `agendamento`/`servico`: today's booking count, this month's revenue, and a top-5 services ranking. No new entities, no new migration, no caching — every number is a fresh aggregate query on each request.

## Architecture Decisions

- **All three figures are JPQL aggregate queries on `AgendamentoRepository`** (`COUNT`, `SUM`, `GROUP BY ... ORDER BY COUNT(...) DESC`), not application-level aggregation over a `findAll()`-style list. Aggregating in the database is both correct (no risk of the service layer forgetting a filter) and the only approach that scales past a trivial row count.
- **`agendamentosHoje` counts `AGENDADO` + `CONCLUIDO`, excluding `CANCELADO`** — a cancelled booking isn't "happening today" anymore (resolved Open Question 1).
- **`faturamentoMes` sums `preco_cobrado` only for `CONCLUIDO` bookings in the current calendar month** — earned revenue, not projected; `AGENDADO` could still be cancelled, `CANCELADO` never happened (resolved Open Question 2).
- **`topServicos` ranks by count of `CONCLUIDO` bookings, all-time, top 5** — a best-sellers signal is more useful long-run than a monthly snapshot (resolved Open Question 3).
- **"Today" and "this month" are computed via `LocalDate.now()` on the server**, consistent with `scheduling`'s no-multi-timezone choice for `agendamento.inicio` (a naive `LocalDateTime`) (resolved Open Question 4).
- **No caching, no scheduled job, no summary table.** Every request recomputes from current data, per the Spec's Boundaries — this app's data volume never justifies the complexity of a materialized view or a cache invalidation story.
- **`DashboardService` composes the three repository calls into one `DashboardResponse`** — a thin orchestration layer, following this project's established `controller → service → repository` layering.

## Task List

### Phase 1: Aggregate queries
- [x] Task 1: `AgendamentoRepository` aggregate queries (`countByInicioBetweenAndStatusIn`, `somarFaturamento`, `buscarTopServicos`) + `DashboardResponse`/`TopServicoResponse` DTOs — depends on `scheduling`'s `Agendamento`/`AgendamentoRepository`

### Checkpoint: Aggregate queries
- [x] Integration tests (Testcontainers Postgres) prove each of the three queries independently against seeded rows spanning multiple days/months/statuses/services — e.g. a `CANCELADO` row today must not count toward the today-count; an `AGENDADO` row this month must not count toward the revenue sum; a service with more `AGENDADO` bookings than `CONCLUIDO` ones must not outrank a service with fewer bookings but more `CONCLUIDO` ones

### Phase 2: Endpoint
- [ ] Task 2: `GET /api/admin/dashboard` — `DashboardService` + `DashboardController`, `@PreAuthorize("hasRole('ADMIN')")` — depends on Task 1

### Checkpoint: Module complete
- [ ] Both tasks checked off
- [ ] Every item in SPEC-reporting.md's Success Criteria is satisfied
- [ ] `docker compose up --build` still works end-to-end
- [ ] Human review before starting `client-web`/`admin-web`

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| "Current month" computed naively (e.g. string-matching) misses timezone/month-boundary edge cases | Med | Use `LocalDate.now()` to derive explicit `[primeiroDiaDoMes, primeiroDiaDoMesSeguinte)` bounds for the JPQL range check, same pattern as `scheduling`'s admin-listing `data` filter — never a `EXTRACT(MONTH FROM ...)`-style query that's harder to reason about and test |
| Top-services query returns fewer than 5 rows (or zero) when there isn't enough seeded/real data yet | Low | Expected and correct — the query and its test must assert "whatever qualifies, ranked correctly, capped at 5," not "exactly 5," matching this project's existing rule (from `CLAUDE.md`) against asserting exact counts on data the test doesn't fully control |
| A service with zero `CONCLUIDO` bookings shows up in `topServicos` with `quantidade: 0`, cluttering the ranking | Low | `GROUP BY` on `agendamento` naturally excludes services with no matching rows — only services with at least one `CONCLUIDO` booking appear at all, which is the correct behavior for a "best sellers" ranking |

## Open Questions

None outstanding — the four questions raised in SPEC-reporting.md (today/month scope definitions, revenue basis, top-services window, timezone handling) were resolved during spec review and are captured as Architecture Decisions above.
