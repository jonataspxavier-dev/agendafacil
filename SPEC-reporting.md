# Spec: reporting (AgendaFácil)

Part of [capability-map.md](capability-map.md). Depends on: `scheduling` (`agendamento`, `servico`) — done.

## Objective

A single `ROLE_ADMIN`-only aggregation endpoint for the admin dashboard: how many bookings are happening today, how much revenue was actually earned this month, and which services sell the most. This module reads existing `agendamento`/`servico` data — it introduces no new entities, no new tables, and no write operations.

Done means: `GET /api/admin/dashboard` returns today's booking count, this month's revenue, and a top-services ranking, computed directly from real data (not cached/precomputed), and the endpoint is unreachable without `ROLE_ADMIN`.

## Tech Stack

Same as `scheduling` (Java 21, Spring Boot 4.0.9, Maven, Postgres via Testcontainers in tests). No new infrastructure — this is aggregation queries over the existing `agendamento` table (JPQL `COUNT`/`SUM`/`GROUP BY`), not a new subsystem.

## Commands

Same as every other backend module: `./mvnw clean verify`, `./mvnw test`, `docker compose up --build` from `agendafacil/`.

## Project Structure

New files under `backend/src/main/java/com/agendafacil/backend/`:

```
dto/         DashboardResponse, TopServicoResponse
service/     DashboardService (aggregation queries)
controller/  DashboardController (GET /api/admin/dashboard)
```

`AgendamentoRepository` gets new aggregate-query methods (count-today, sum-revenue-this-month, top-services); no new entity, no new migration.

## Code Style

```java
public record DashboardResponse(
        long agendamentosHoje,
        BigDecimal faturamentoMes,
        List<TopServicoResponse> topServicos) {}

public record TopServicoResponse(Long servicoId, String nome, long quantidade) {}
```

Aggregate queries live in `AgendamentoRepository` as JPQL (`COUNT`, `SUM`, `GROUP BY ... ORDER BY COUNT(...) DESC`), following this project's existing convention of JPQL `@Query` methods over Criteria/Specification except where optional filters require it (as in `scheduling`'s admin listing).

## Testing Strategy

- **Integration tests** (Testcontainers Postgres), since this is pure aggregation over real rows: seed `agendamento` rows across today/yesterday, this month/last month, and multiple services/statuses via the real booking flow (or direct repository saves), then assert the dashboard numbers are exactly right for each of the three figures independently, plus one test combining all three.
- **Role check**: 401 no token, 403 CLIENTE token, 200 for ADMIN.
- No unit-test layer needed here (unlike `DisponibilidadeService`) — there's no slot-calculation-style pure logic to isolate; the "logic" is the SQL aggregation itself, which only means something against a real database.

## Boundaries

- **Always:** compute every figure live from `agendamento`/`servico` on each request — no caching, no precomputed summary table, no scheduled job. Dashboard data must reflect the current DB state immediately after any booking/cancellation/conclusion.
- **Ask first:** adding query parameters (e.g. a `mes`/`data` override) beyond what's specified below — the brief describes a fixed "today" + "this month" dashboard, not a report picker.
- **Never:** let a CLIENTE reach this endpoint; let this module write to `agendamento` or any other table — it is read-only by construction.

## Success Criteria

- [ ] `GET /api/admin/dashboard` returns 401 with no token, 403 with a CLIENTE token, 200 with an ADMIN token
- [ ] `agendamentosHoje` is the count of today's bookings, excluding `CANCELADO`
- [ ] `faturamentoMes` is the sum of `preco_cobrado` for `CONCLUIDO` bookings whose `inicio` falls in the current calendar month
- [ ] `topServicos` lists services ranked by number of `CONCLUIDO` bookings (all-time), descending, limited to the top 5
- [ ] Integration tests cover each of the three figures independently against seeded data spanning multiple days/months/statuses/services, plus the role check
- [ ] Swagger UI shows the new operation

## Open Questions

1. **Does "agendamentos do dia" count all of today's bookings, or only `AGENDADO` ones (excluding already-cancelled)?** Proposing: count `AGENDADO` + `CONCLUIDO` (i.e. everything except `CANCELADO`) — a cancelled booking isn't really "happening today" anymore. Confirm, or say if cancelled bookings should still count toward today's total.
2. **Does "faturamento do mês" count money actually earned, or everything scheduled?** Proposing: sum `preco_cobrado` only for `CONCLUIDO` bookings this month — `AGENDADO` hasn't happened yet (and could still be cancelled) and `CANCELADO` never will, so counting either would overstate real revenue. Confirm, or say if `AGENDADO` should count too (projected revenue instead of earned revenue).
3. **What counts toward "serviços mais vendidos," and over what window?** Proposing: top 5 services by count of `CONCLUIDO` bookings, **all-time** (not restricted to the current month) — a best-sellers ranking is more useful as a long-run signal than a monthly snapshot, and the brief doesn't say "do mês" for this one specifically (only for faturamento). Confirm, or say if this should also be scoped to the current month, or ranked by revenue instead of booking count.
4. **"This month" and "today" — server's local date, or the business's timezone?** Same as `scheduling`'s choice: `agendamento.inicio` is a naive `LocalDateTime` (no timezone), so "today"/"this month" are computed via `LocalDate.now()` on the server, consistent with the rest of the app's no-multi-timezone assumption. Confirm.
