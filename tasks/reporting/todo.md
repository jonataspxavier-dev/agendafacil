# Tasks: reporting (AgendaFácil)

Plan: [plan.md](plan.md) · Spec: [../../SPEC-reporting.md](../../SPEC-reporting.md)

## Task 1: Aggregate queries + DTOs ✅

**Description:** Three new JPQL aggregate queries on `AgendamentoRepository` (today's booking count, this month's revenue, top-5 services by `CONCLUIDO` count), plus the `DashboardResponse`/`TopServicoResponse` DTOs. No controller/service yet — this task proves the numbers are correct in isolation.

**Acceptance criteria:**
- [x] `countByInicioBetweenAndStatusIn(inicio, fim, statuses)`: counts `agendamento` rows whose `inicio` falls within the given `[inicio, fim)` bounds and whose status is in the given list — the caller (Task 2's `DashboardService`) passes today's bounds and `[AGENDADO, CONCLUIDO]`, so `CANCELADO` never counts
- [x] `somarFaturamento(inicio, fim, status)`: sums `preco_cobrado` for rows with the given status within `[inicio, fim)`; returns `BigDecimal.ZERO` (not `null`) when there are no matching rows (via `COALESCE(SUM(...), 0)`) — the caller passes this month's bounds and `CONCLUIDO`
- [x] `buscarTopServicos(status, pageable)`: groups rows by `servico` for the given status, orders by count descending, returns a `TopServicoResponse` per group (via a JPQL constructor expression) capped by the `Pageable`'s page size — services with zero matching bookings never appear; no date bound (all-time)
- [x] `DashboardResponse(long agendamentosHoje, BigDecimal faturamentoMes, List<TopServicoResponse> topServicos)` and `TopServicoResponse(Long servicoId, String nome, long quantidade)` records in `dto/`

**Verification:**
- [x] Tests pass: `./mvnw test` (TDD red→green, 5/5) and full `./mvnw clean verify` — integration tests (Testcontainers Postgres, `AgendamentoRepositoryDashboardTest`) seed rows spanning today/yesterday, this month/last month, `AGENDADO`/`CONCLUIDO`/`CANCELADO`, and three distinct services with different `CONCLUIDO` counts:
  - a `CANCELADO` booking today and an `AGENDADO` booking outside the range are excluded from the count
  - an `AGENDADO` booking this month and a `CONCLUIDO` booking last month are excluded from the sum; a fresh query with nothing matching returns `BigDecimal.ZERO`
  - a service with 3 `CONCLUIDO` bookings outranks one with 1, and one with only `AGENDADO` bookings doesn't appear at all; the `Pageable` page size correctly caps the result list

**Note on query choice:** `countByInicioBetweenAndStatusIn` is a Spring Data **derived** query (no `@Query` needed — `countBy...Between...In` is directly expressible); `somarFaturamento` and `buscarTopServicos` need `@Query` since Spring Data can't derive `SUM`/`GROUP BY`/constructor expressions from a method name.

**Dependencies:** `scheduling` (`Agendamento`, `AgendamentoRepository`, `StatusAgendamento`)

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/repository/AgendamentoRepository.java` (add 3 aggregate queries)
- `backend/src/main/java/com/agendafacil/backend/dto/DashboardResponse.java`
- `backend/src/main/java/com/agendafacil/backend/dto/TopServicoResponse.java`
- `backend/src/test/java/.../repository/AgendamentoRepositoryDashboardTest.java`

**Estimated scope:** Medium: 4 files, three distinct aggregate queries to get exactly right

---

## Task 2: `GET /api/admin/dashboard` ✅

**Description:** Wire the Task 1 queries into a `DashboardService` and expose them as `ROLE_ADMIN`-only `GET /api/admin/dashboard`, returning a single `DashboardResponse`.

**Acceptance criteria:**
- [x] 401 no token, 403 CLIENTE token, 200 for ADMIN
- [x] Response combines all three figures from Task 1 into one `DashboardResponse` JSON body
- [x] Integration test books/completes/cancels real bookings via the existing `POST /api/agendamentos` + `.../concluir` + `.../cancelar` flow (not direct repository saves) and asserts the dashboard reflects them correctly end-to-end

**Verification:**
- [x] Tests pass: `./mvnw test` (TDD red→green, 3/3) and full `./mvnw clean verify`
- [x] Manual check: `docker compose up --build`. Baseline dashboard (`agendamentosHoje:0, faturamentoMes:0, topServicos:[]`), then booked a real slot today as a client (showed up in `agendamentosHoje` only, still `AGENDADO`), marked it `concluir` as admin (then also appeared in `faturamentoMes: 50.00` and `topServicos`). CLIENTE token → 403, no token → 401. Swagger shows the operation.

**Dependencies:** Task 1

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/service/DashboardService.java`
- `backend/src/main/java/com/agendafacil/backend/controller/DashboardController.java`
- `backend/src/main/java/com/agendafacil/backend/security/SecurityConfig.java` (no change needed — confirmed `/api/admin/**` falls under `anyRequest().authenticated()`, `@PreAuthorize` handles the role, same as `scheduling`'s admin listing)
- `backend/src/test/java/.../controller/DashboardControllerTest.java`

**Estimated scope:** Small: 3-4 files — thin wiring over Task 1's already-correct queries

---

## Checkpoint: Module complete

- [x] All 2 tasks checked off
- [x] Every item in SPEC-reporting.md's Success Criteria is satisfied
- [x] `docker compose up --build` works end-to-end
- [ ] Human review before starting `client-web`/`admin-web`
