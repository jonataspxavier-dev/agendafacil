# Tasks: admin-web (AgendaFácil)

Plan: [plan.md](plan.md) · Spec: [../../SPEC-admin-web.md](../../SPEC-admin-web.md)

## Task 1: Foundation (`AdminRoute`, role-aware redirect, admin nav, routes) ✅

**Description:** Everything later tasks depend on: the role-gated route guard, the two small touches to existing `client-web` files, and placeholder `/admin/*` routes.

**Acceptance criteria:**
- [x] `components/AdminRoute.tsx`: unauthenticated → redirect to `/login`; authenticated with `user.perfil !== 'ADMIN'` → redirect to `/servicos`; `ADMIN` → render children; shows nothing while `isLoading` (mirrors `ProtectedRoute`'s shape)
- [x] `LoginPage.tsx`: after a successful login, redirect to `/admin/dashboard` when the resolved user is `ADMIN`, otherwise the existing behavior (originally-requested protected route, or `/servicos`)
- [x] `Nav.tsx`: when `user.perfil === 'ADMIN'`, show Dashboard/Serviços/Profissionais/Agendamentos/Sair links instead of the CLIENTE link set
- [x] `App.tsx`: new routes `/admin/dashboard`, `/admin/servicos`, `/admin/profissionais`, `/admin/agendamentos`, each wrapped in `AdminRoute`, with placeholder content for now

**Scope note:** `AuthContext.login` now returns the logged-in `Usuario` (was `Promise<void>`) so `LoginPage` can read `perfil` immediately after login without relying on a state read that hasn't re-rendered yet. Updated every test that typed a `login` stub (`ProtectedRoute.test.tsx`, the new `AdminRoute.test.tsx`, `LoginPage.test.tsx`) to match the new return type.

**Verification:**
- [x] Tests pass: `npm test` (32/32 — `AdminRoute`'s three-way redirect using a real `AuthContext.Provider` per case, plus a new `LoginPage` test for the ADMIN-redirects-to-`/admin/dashboard` case) and `npm run build`
- [x] Manual check: `docker compose up --build`. Logged in as `admin@agendafacil.com` → redirected to `/admin/dashboard`, Nav showed all 4 admin links. Registered and logged in a fresh CLIENTE, manually navigated to `/admin/dashboard` → redirected to `/servicos` with the CLIENTE nav. Logged out, confirmed `/admin/dashboard` would redirect to `/login` (verified via the logout → `/login` transition). No console errors.

**Dependencies:** `client-web` (`ProtectedRoute`, `AuthContext`, `Nav`, `LoginPage`) — done

**Files likely touched:**
- `frontend/src/components/AdminRoute.tsx`
- `frontend/src/components/AdminRoute.test.tsx`
- `frontend/src/pages/LoginPage.tsx` (role-aware redirect)
- `frontend/src/pages/LoginPage.test.tsx` (new ADMIN-redirect test, fixed `login` mock return type)
- `frontend/src/components/Nav.tsx` (admin link set)
- `frontend/src/components/ProtectedRoute.test.tsx` (fixed `login` mock return type)
- `frontend/src/contexts/AuthContext.tsx` (`login` now returns `Usuario`)
- `frontend/src/App.tsx` (new routes)

**Estimated scope:** Medium: 5 files — small, focused touches to existing files plus one new component

---

## Task 2: `AdminDashboardPage` ✅

**Description:** Real numbers from `GET /api/admin/dashboard`, plus a minimal bar chart for top services.

**Acceptance criteria:**
- [x] `services/dashboardService.ts`: `obter()` → `GET /api/admin/dashboard`
- [x] `components/BarChart.tsx`: takes `{ label: string; value: number }[]`, renders one bar per item with width proportional to the max value in the set (plain `<div>`s + Tailwind, no library) — each bar exposes `role="progressbar"`/`aria-valuenow` for both accessibility and testability
- [x] `pages/admin/AdminDashboardPage.tsx`: fetches on mount, loading/error states (reusing `LoadingSpinner`/`ErrorMessage`); shows `agendamentosHoje` and `faturamentoMes` (formatted via `Intl.NumberFormat`) as simple stat displays, and `topServicos` through `BarChart`

**Verification:**
- [x] Tests pass: `npm test` (37/37 — `AdminDashboardPage` renders the three figures from a mocked `dashboardService.obter()`; `BarChart`'s bar widths are proportional to given values, confirmed exact percentages for max/half-max/zero) and `npm run build`
- [x] Manual check: `docker compose up --build`, logged in as admin, confirmed `/admin/dashboard` shows real figures (`agendamentosHoje: 2`, `faturamentoMes: R$ 50,00`, one bar for Corte de Cabelo) matching a direct `GET /api/admin/dashboard` call exactly. Checked mobile width (375px) — stat cards stack cleanly, no horizontal scroll.

**Dependencies:** Task 1

**Files likely touched:**
- `frontend/src/services/dashboardService.ts`
- `frontend/src/components/BarChart.tsx`
- `frontend/src/components/BarChart.test.tsx`
- `frontend/src/pages/admin/AdminDashboardPage.tsx`
- `frontend/src/pages/admin/AdminDashboardPage.test.tsx`
- `frontend/src/App.tsx` (wire real page in place of the Task 1 placeholder)

**Estimated scope:** Medium: 6 files

---

## Task 3: `AdminServicosPage` ✅

**Description:** CRUD for serviços: list (active only, per the accepted backend limitation), create, edit, deactivate.

**Acceptance criteria:**
- [x] `services/servicosAdminService.ts`: `criar(request)` → `POST /api/servicos`; `atualizar(id, request)` → `PUT /api/servicos/{id}`; `desativar(id)` → `DELETE /api/servicos/{id}` (reuses `servicosService.listar()` from `client-web` for the list — no duplicate `listar`)
- [x] `pages/admin/AdminServicosPage.tsx`: lists active services; a form (RHF + Zod mirroring `ServicoRequest`'s constraints: nome required, duracaoMin positive integer, preco a positive decimal, via `z.coerce.number()`) creates a new one or edits an existing one (same form, different submit target); a "Desativar" action per row, with the list updating immediately after
- [x] A validation or backend error (e.g. malformed input) surfaces inline via `extractErrorMessage`, not a crash

**Build note:** `z.coerce.number()` fields need `useForm`'s 3-generic form (`useForm<InputType, unknown, OutputType>` using `z.input<typeof schema>`/`z.output<typeof schema>`) rather than a single `z.infer` type — otherwise `tsc -b` fails (Vitest's own transform doesn't type-check, so this only surfaced at build time, not test time). Worth remembering for any future numeric-coercing form.

**Verification:**
- [x] Tests pass: `npm test` (43/43 — Zod schema rejects invalid input before any network call; create/edit submit calls the right service function with the right payload; deactivate calls `desativar` and removes that row from the visible list) and `npm run build`
- [x] Manual check: `docker compose up --build`, created "Barba Teste Admin" through the UI, confirmed it appeared in both the admin list and a direct `GET /api/servicos` call; edited its price (25 → 30), confirmed the change in both places; deactivated it, confirmed it disappeared from both. No console errors.

**Dependencies:** Task 1

**Files likely touched:**
- `frontend/src/services/servicosAdminService.ts`
- `frontend/src/pages/admin/AdminServicosPage.tsx`
- `frontend/src/pages/admin/AdminServicosPage.test.tsx`
- `frontend/src/App.tsx` (wire real page)

**Estimated scope:** Medium: 4 files

---

## Task 4: `AdminProfissionaisPage` ✅

**Description:** CRUD for profissionais, including the module's most involved piece of UI: the jornada row editor.

**Acceptance criteria:**
- [x] `services/profissionaisAdminService.ts`: `criar(request)` → `POST /api/profissionais`; `atualizar(id, request)` → `PUT /api/profissionais/{id}`; `desativar(id)` → `DELETE /api/profissionais/{id}` (reuses `profissionaisService.listar()` from `client-web`)
- [x] `pages/admin/AdminProfissionaisPage.tsx`: lists active professionals; a form creates/edits one with: nome (required), a `servicoIds` multi-select (checkboxes against the active services list, RHF's native checkbox-group pattern), and a jornada row editor via `useFieldArray` — each row a `diaSemana` select (7 options) + `horaInicio`/`horaFim` time inputs, with add/remove-row controls; pre-populates every field correctly when editing an existing professional (including stripping the backend's `HH:mm:ss` time strings to `HH:mm` for the `<input type="time">` elements)
- [x] Submitting produces a `ProfissionalRequest`-shaped payload exactly (`servicoIds: number[]`, `jornada: { diaSemana, horaInicio, horaFim }[]`); a "Desativar" action per row updates the list immediately
- [x] A validation or backend error surfaces inline

**Scope note:** `client-web`'s `profissionaisService.ts` `Profissional` type didn't include `jornada` (not needed for the booking flow) — extended it with a `Jornada[]` field and a `DiaSemana` type, since `AdminProfissionaisPage` needs the real jornada to pre-populate edits. Purely additive (an existing API field the type just didn't model yet); updated `AgendarPage.test.tsx`'s fixtures to match the now-required field.

**Verification:**
- [x] Tests pass: `npm test` (49/49 — adding/removing a jornada row updates what gets submitted; editing an existing professional pre-populates `servicoIds` and jornada correctly; the submit payload matches the form state exactly) and `npm run build`
- [x] Manual check: `docker compose up --build`, created "Profissional Jornada Teste" with exactly one jornada row (Tuesday 10:00-14:00) and one service. Confirmed via a direct `GET /api/disponibilidade` call: Tuesday returned exactly 8 half-hour slots from 10:00-14:00, Monday returned zero slots — proving the jornada editor produces a payload the backend actually honors, not just one that "looks right" in the form. Also verified edit pre-population (all fields restored correctly, including the `HH:mm:ss` → `HH:mm` conversion) and deactivation. No console errors.

**Dependencies:** Task 1

**Files likely touched:**
- `frontend/src/services/profissionaisService.ts` (added `jornada`/`Jornada`/`DiaSemana`)
- `frontend/src/services/profissionaisAdminService.ts`
- `frontend/src/pages/admin/AdminProfissionaisPage.tsx`
- `frontend/src/pages/admin/AdminProfissionaisPage.test.tsx`
- `frontend/src/pages/AgendarPage.test.tsx` (fixture fix for the extended `Profissional` type)
- `frontend/src/App.tsx` (wire real page)

**Estimated scope:** Large: 4 files, but the jornada editor is genuinely the most complex form in the app — budget the most time here, not the most files

---

## Task 5: `AdminAgendamentosPage` ✅

**Description:** The system-wide bookings list: filters, pagination, and the `concluir`/`cancelar` actions.

**Acceptance criteria:**
- [x] `services/adminAgendamentosService.ts`: `listar({ data?, status?, page, size })` → `GET /api/admin/agendamentos` with those exact query params; `concluir(id)` → `PATCH /api/agendamentos/{id}/concluir`; `cancelar(id)` → `PATCH /api/agendamentos/{id}/cancelar` (re-exports `client-web`'s `agendamentosService.cancelar` rather than duplicating it — confirmed Vitest's automocking handles the re-export transparently)
- [x] `pages/admin/AdminAgendamentosPage.tsx`: a `data` filter (date input), a `status` filter (select: all/AGENDADO/CANCELADO/CONCLUIDO), and pagination controls (Anterior/Próxima + page indicator, fixed page size of 10), all reflected in the `listar` call; changing a filter resets to page 0; results show service, professional, date/time, status, price (`AgendamentoResponse` exposes no client name, so none shown)
- [x] "Concluir" shown only on `AGENDADO` rows (admin-only action, already enforced server-side too); "Cancelar" shown only on `AGENDADO` rows; both update that row in place on success, surface the backend's error inline on failure (e.g. cancelling inside the 2h window)

**Verification:**
- [x] Tests pass: `npm test` (58/58 — changing a filter or page control calls `listar` with the exact right params; `concluir`/`cancelar` call the right endpoint with the right id and update that row; a failure surfaces inline) and `npm run build`
- [x] Manual check: `docker compose up --build`, booked two fresh slots as a client on 2026-10-28, then from the admin list: `status=CANCELADO` filter matched a direct API call exactly (3 results); `data=2026-10-28` filter narrowed to exactly those 2 fresh bookings; concluded one (status updated to "Concluído" in place, actions disappeared) and cancelled the other (updated to "Cancelado" in place) — both confirmed as real status changes. No unexpected console errors.

**Dependencies:** Task 1

**Files likely touched:**
- `frontend/src/services/adminAgendamentosService.ts`
- `frontend/src/pages/admin/AdminAgendamentosPage.tsx`
- `frontend/src/pages/admin/AdminAgendamentosPage.test.tsx`
- `frontend/src/App.tsx` (wire real page)

**Estimated scope:** Large: 4 files — filters, pagination, and two actions is a lot of interacting state

---

## Task 6: Polish + end-to-end verification ✅

**Description:** The full manual admin journey against the real stack — no new UI components expected, this task is verification (and any small consistency fixes it turns up).

**Acceptance criteria:**
- [x] Every admin page uses `LoadingSpinner`/`ErrorMessage` consistently (confirmed via grep — all four admin pages were built directly on these shared components from Tasks 2-5, no ad hoc markup to clean up)
- [x] Full manual walkthrough against `docker compose up --build`: logged in as `admin@agendafacil.com` (redirected to `/admin/dashboard`, real numbers shown) → created "Servico Task6 Final" → edited its price (40→45, confirmed via a direct `GET /api/servicos` call) → created "Profissional Task6 Final" with a jornada row and that service (confirmed via a direct `GET /api/profissionais` call) → deactivated the serviço (confirmed it vanished from the public `/api/servicos` list too) → went to `/admin/agendamentos`, concluded an `AGENDADO` booking (updated in place) and confirmed the `status=AGENDADO` filter correctly narrowed to exactly the one remaining one → logged out (redirected cleanly to `/login`)
- [x] Mobile width (375px) check on the dashboard and the profissionais page (including the jornada row editor, the most complex layout piece) — no horizontal scroll anywhere, Nav and forms wrap cleanly

**Verification:**
- [x] Tests pass: `npm test` (58/58, full suite) and `npm run build`
- [x] Manual check: the walkthrough above, performed start-to-finish, zero console errors

**Dependencies:** Tasks 1-5

**Files likely touched:** none — the walkthrough surfaced no fixes needed

**Estimated scope:** Small-Medium: primarily verification, not new code

---

## Checkpoint: Module complete

- [x] All 6 tasks checked off
- [x] Every item in SPEC-admin-web.md's Success Criteria is satisfied
- [x] `docker compose up --build` works end-to-end for the full admin journey
- [ ] Human review — this is the last planned module per capability-map.md
