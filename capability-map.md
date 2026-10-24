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
| `platform-setup` | Done — all 8 tasks implemented and verified (Docker installed; `docker compose up --build` confirmed all 3 services healthy). Pending: human review before starting `identity` |
| `identity` | Done — all 9 tasks implemented and verified end-to-end via docker compose; pending human review before `catalog` |
| `catalog` | Spec approved; implementation plan drafted (8 tasks) — pending review |
| `scheduling` | Not started |
| `reporting` | Not started |
| `client-web` | Not started |
| `admin-web` | Not started |
