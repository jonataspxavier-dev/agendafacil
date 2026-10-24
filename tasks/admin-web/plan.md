# Implementation Plan: admin-web (AgendaFácil)

Spec: [../../SPEC-admin-web.md](../../SPEC-admin-web.md)
Task list: [todo.md](todo.md)

## Overview

The admin-facing half of the shared `frontend/` app: `/admin/dashboard`, `/admin/servicos`, `/admin/profissionais`, `/admin/agendamentos`, all gated by a new role-aware `AdminRoute`. No backend work — `reporting` and `catalog` already expose every endpoint this module needs.

## Architecture Decisions

- **Accept the backend's "active-only" limitation as-is.** `GET /api/servicos`/`GET /api/profissionais` only ever return `ativo = true` rows and there's no reactivation endpoint; the admin lists show exactly what the public lists show, and deactivating something removes it from the admin's own view too, with no undo. `catalog` is `Done` and stays out of scope (resolved Open Question 1).
- **No charting library.** `topServicos` gets a hand-built horizontal bar chart (`<div>`s sized by percentage of the max count) — the dataset is at most 5 bars with one number each, and `client-web`'s existing Boundaries already lean against adding dependencies casually (resolved Open Question 2).
- **The admin bookings list gets `Concluir`/`Cancelar` actions**, not just filters/pagination — both backend endpoints already exist and `cancelar`'s client-side wiring already exists in `client-web`'s `agendamentosService` (resolved Open Question 3).
- **`AdminRoute` redirects a non-ADMIN authenticated user to `/servicos` silently** — no "access denied" page/pattern introduced for this (resolved Open Question 4).
- **`AdminRoute` wraps `ProtectedRoute`'s pattern rather than duplicating it**: unauthenticated → `/login`; authenticated but `perfil !== 'ADMIN'` → `/servicos`; ADMIN → render. One new component, no changes to `ProtectedRoute` itself.
- **Two small, explicitly-scoped touches to existing `client-web` files**: `LoginPage`'s post-login redirect checks `user.perfil` (ADMIN → `/admin/dashboard`, else → `/servicos`/the original protected route); `Nav` renders a different link set when `user.perfil === 'ADMIN'`. Nothing else in `client-web` changes.
- **CRUD forms mirror the backend's own validation constraints client-side** (`ServicoRequest`'s `@NotBlank`/`@Positive`/`@DecimalMin`, `ProfissionalRequest`'s `@NotBlank` nome), via the same RHF + Zod + `extractErrorMessage` pattern `client-web` already established — no new pattern invented.
- **Jornada is a repeatable row list directly shaped like `JornadaRequest[]`** (`diaSemana`/`horaInicio`/`horaFim` per row, add/remove), avoiding any intermediate data model between the form and the request body.
- **Build order**: foundation first (nothing else is reachable or testable without `AdminRoute`), then the simplest page (dashboard, pure read), then the two CRUD pages, then the bookings list (depends on patterns proven in the CRUD pages), then polish + full walkthrough — same bottom-up approach `client-web` used.

## Task List

### Phase 1: Foundation
- [x] Task 1: `AdminRoute`, `LoginPage`'s role-aware redirect, `Nav`'s admin link set, `/admin/*` routes wired (placeholder pages) in `App.tsx`

### Checkpoint: Foundation
- [x] `npm run build` and `npm test` pass with the new routes in place (even with placeholder pages)
- [x] `docker compose up --build`: logging in as the seeded admin (`admin@agendafacil.com`) redirects to `/admin/dashboard`; logging in as a CLIENTE and manually visiting `/admin/dashboard` redirects to `/servicos`; logging out and visiting it redirects to `/login`

### Phase 2: Dashboard
- [ ] Task 2: `AdminDashboardPage` + `dashboardService` + a hand-built `BarChart` component for `topServicos`

### Phase 3: Catalog CRUD
- [ ] Task 3: `AdminServicosPage` + `servicosAdminService` (`criar`/`atualizar`/`desativar`) — create/edit form, deactivate action, list updates immediately
- [ ] Task 4: `AdminProfissionaisPage` + `profissionaisAdminService` — create/edit form with `servicoIds` multi-select and a repeatable jornada row editor, deactivate action

### Checkpoint: Catalog CRUD
- [ ] A serviço created/edited through the UI is immediately reflected in `client-web`'s own public `/servicos` list (same backend data, proving the forms submit real, correct payloads)
- [ ] A profissional's jornada edited through the UI is correctly reflected in `GET /api/disponibilidade` results (proves the jornada row editor produces a correct `JornadaRequest[]`, not just that the form "looks right")

### Phase 4: Bookings list
- [ ] Task 5: `AdminAgendamentosPage` + `adminAgendamentosService` (`listar` with `data`/`status`/paging params, `concluir`, `cancelar`)

### Phase 5: Polish + end-to-end verification
- [ ] Task 6: Full manual admin walkthrough against `docker compose up --build`: log in as admin → dashboard shows real numbers → create and edit a serviço → create a profissional with jornada and servicoIds → deactivate one of each → filter/page the bookings list → conclude and cancel a booking from the admin list

### Checkpoint: Module complete
- [ ] All 6 tasks checked off
- [ ] Every item in SPEC-admin-web.md's Success Criteria is satisfied
- [ ] `docker compose up --build` works end-to-end for the full admin journey
- [ ] Human review — this is the last planned module per capability-map.md

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Jornada row editor produces a payload shape that compiles and looks right in the UI but is subtly wrong (e.g. wrong `DayOfWeek` enum casing, time format mismatch) and the backend silently accepts a nonsensical jornada | High | Task 4's checkpoint doesn't stop at "the form submitted without an error" — it verifies the edited jornada actually changes real `GET /api/disponibilidade` results, the same way `scheduling`'s own tests proved jornada logic by checking real slot output, not by inspecting a request body |
| `AdminRoute`'s role check is bypassed by relying only on client-side hiding (e.g. a CLIENTE could still call an admin API directly from the browser console) | Low — not actually a gap | Not a real risk: every admin-only backend endpoint already enforces `@PreAuthorize("hasRole('ADMIN')")` independently (proven in `catalog`/`scheduling`/`reporting`'s own tests). `AdminRoute` is a UX gate, not a security boundary — the security boundary already exists and doesn't depend on this module at all |
| The bookings list's pagination/filter params don't match `GET /api/admin/agendamentos`'s actual query param names (`data`, `status`, `page`, `size`) exactly, silently returning unfiltered results | Med | `adminAgendamentosService` is written directly against the confirmed query param names from `scheduling`'s `AdminAgendamentoController` (already read during this module's SPEC research), and Task 5's tests assert the exact params passed to the mocked service call, not just that *a* call happened |

## Open Questions

None outstanding — the four questions raised in SPEC-admin-web.md (accepting the active-only catalog limitation, no charting library, bookings-list actions, silent redirect for non-admins) were resolved during spec review and are captured as Architecture Decisions above.
