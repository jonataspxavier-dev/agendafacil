# Capability Map: AgendaFácil

Source brief: [`../projeto-2-agendamentos-react-spring.md`](../projeto-2-agendamentos-react-spring.md)

| Module id | Responsibility | Depends on |
|---|---|---|
| `platform-setup` | Repo scaffold, Docker Compose (Postgres+backend+frontend), Flyway baseline, CLAUDE.md conventions | — |
| `identity` | Register/login/refresh, JWT access+refresh, BCrypt, ADMIN/CLIENTE roles, global error handling | platform-setup |
| `catalog` | CRUD Serviços + Profissionais, profissional↔serviço association, jornada (work schedule) | identity |
| `scheduling` | Disponibilidade calculation (no conflicts) + Agendamento lifecycle (create/cancel ≤2h/complete) | catalog |
| `reporting` | Admin dashboard: today's bookings, monthly revenue, top services | scheduling |
| `client-web` | Customer frontend: login/signup, service list, step booking flow, "Meus agendamentos" | scheduling |
| `admin-web` | Admin frontend: dashboard, catalog CRUD UI, bookings list with filters/pagination | reporting, catalog |

**Build order:** `platform-setup` → `identity` → `catalog` → `scheduling` → `reporting` & `client-web` (parallel) → `admin-web`

**Status:** Approved 2026-10-23. Module specs are written and implemented one at a time, in build order, each named `SPEC-<module-id>.md` at this directory's root.

| Module | Spec status |
|---|---|
| `platform-setup` | All 8 tasks implemented; 4 success criteria (live health/Swagger, Testcontainers smoke test, `docker compose up`) blocked on Docker Desktop install |
| `identity` | Not started |
| `catalog` | Not started |
| `scheduling` | Not started |
| `reporting` | Not started |
| `client-web` | Not started |
| `admin-web` | Not started |
