# Spec: admin-web (AgendaFácil)

Part of [capability-map.md](capability-map.md). Depends on: `reporting` (dashboard API), `catalog` (serviços/profissionais CRUD API) — both done. Also depends, practically, on `client-web`'s shared frontend infra (`AuthContext`, `ProtectedRoute`, `api.ts`, `Layout`/`Nav`), since both live in the same `frontend/` app per the brief's folder structure.

## Objective

The admin-facing half of the single `frontend/` app: a dashboard (today's bookings, monthly revenue, top services), CRUD for serviços and profissionais, and a filterable/paginated bookings list. No new backend work — `reporting` and `catalog` already expose everything this module needs.

Done means: an authenticated `ADMIN` can see real numbers on `/admin/dashboard`; create, edit, and deactivate serviços and profissionais with real validation and real jornada editing; filter and page through every booking in the system; and a `CLIENTE` (or an unauthenticated visitor) cannot reach any `/admin/*` route or see any admin-only UI.

## Tech Stack

Same as `client-web` (already scaffolded, nothing new to install) — React 19, Vite, TypeScript, React Router, Axios, React Hook Form + Zod, Tailwind CSS. No charting library added (see Architecture Decisions) — this keeps the brief's deliberately small declared stack intact.

## Commands

Same as every other frontend module: `npm run dev`, `npm run build`, `npm test`, `npm run lint` from `agendafacil/frontend/`. Full stack: `docker compose up --build` from `agendafacil/`.

## Project Structure

Extends `client-web`'s existing structure — new files only, nothing in `contexts/`/`hooks/`/`services/api.ts` needs touching:

```
src/
├── components/  AdminRoute (role-gated, wraps ProtectedRoute's pattern), BarChart (top-services bars)
├── services/    servicosAdminService (criar/atualizar/desativar — listar already exists from client-web),
│                profissionaisAdminService (same shape), dashboardService, adminAgendamentosService
└── pages/
    ├── admin/   AdminDashboardPage, AdminServicosPage, AdminProfissionaisPage, AdminAgendamentosPage
```

New routes under `/admin/*`: `/admin/dashboard`, `/admin/servicos`, `/admin/profissionais`, `/admin/agendamentos` — mirroring the backend's own `/api/admin/**` naming.

## Code Style

- **`AdminRoute` wraps the existing `ProtectedRoute`**, adding a role check: unauthenticated → `/login` (same as before); authenticated but not `ADMIN` → redirect to `/servicos`; `ADMIN` → render. No duplicated auth-state logic.
- **`LoginPage` redirects an `ADMIN` user to `/admin/dashboard` instead of `/servicos`** after a successful login (resolved from `SPEC-client-web.md`'s Open Question 4, now that this module exists to redirect to).
- **`Nav` shows admin-specific links when `user.perfil === 'ADMIN'`** (Dashboard, Serviços, Profissionais, Agendamentos, Sair) instead of the CLIENTE-oriented links — this app has no mixed-role usage pattern, so there's no need to show both sets at once.
- **CRUD forms follow the same RHF + Zod pattern as `client-web`'s auth forms**: client-side validation mirrors the backend's own constraints (`ServicoRequest`'s `@NotBlank`/`@Positive`/`@DecimalMin`, `ProfissionalRequest`'s `@NotBlank` nome), and a failed submit surfaces the backend's real error message via the existing `extractErrorMessage` helper.
- **Jornada is edited as a repeatable row list** (dia da semana + hora início + hora fim, add/remove row) inside the profissional form — matches `ProfissionalRequest.jornada`'s shape directly, no intermediate representation.
- **Top services get a hand-built horizontal bar chart** (a `<div>` per bar, width set by percentage of the max count, Tailwind utility classes) — no charting library. The dataset is at most 5 bars with a single number each; a full charting dependency is unjustified weight for that (see Open Questions if this should be reconsidered).

## Testing Strategy

Same approach as `client-web`: Vitest + Testing Library, business logic gets tests, styling doesn't.

- **`AdminRoute`**: redirects to `/login` unauthenticated, to `/servicos` when authenticated-but-not-ADMIN, renders children when ADMIN — mirrors `ProtectedRoute.test.tsx`'s pattern with a real `AuthContext.Provider`.
- **CRUD forms**: Zod validation rejects invalid input before any network call; a successful submit calls the service with the right payload; a failed one (e.g. backend validation error) surfaces inline.
- **Jornada row editor**: adding/removing a row updates what gets submitted; the form's existing jornada pre-populates correctly when editing.
- **`AdminAgendamentosPage`**: filter inputs (data, status) are reflected in the service call's params; pagination controls call the service with the right page; a `concluir`/`cancelar` action calls the right endpoint and updates that row.
- **`AdminDashboardPage`**: renders the three figures from a mocked `dashboardService` response; the bar chart's bar widths are proportional to the mocked counts.

## Boundaries

- **Always:** gate every `/admin/*` route with `AdminRoute`, not just `ProtectedRoute` — a logged-in CLIENTE must never see admin UI, even briefly before a redirect.
- **Ask first:** adding a charting library (the Code Style section proposes not to); any change to `client-web`'s own pages/behavior beyond the two touch points named above (`LoginPage`'s post-login redirect, `Nav`'s admin links).
- **Never:** let a form submit without the client-side validation that mirrors the backend's constraints; silently swallow a backend validation error instead of showing it.

## Success Criteria

- [ ] A `CLIENTE` (or logged-out visitor) hitting any `/admin/*` route is redirected, never shown admin UI
- [ ] `/admin/dashboard` shows real `agendamentosHoje`/`faturamentoMes`/`topServicos` from `GET /api/admin/dashboard`, with a simple bar visualization for top services
- [ ] `/admin/servicos` lists active services, creates/edits one with validation matching the backend's constraints, and deactivates one (with the list updating immediately)
- [ ] `/admin/profissionais` lists active professionals, creates/edits one including `servicoIds` (multi-select) and `jornada` (repeatable day/hora rows), and deactivates one
- [ ] `/admin/agendamentos` lists bookings with working `data`/`status` filters and pagination, matching `GET /api/admin/agendamentos`'s actual filter semantics
- [ ] An ADMIN logging in is redirected to `/admin/dashboard`, not `/servicos`
- [ ] `docker compose up --build` supports a full admin walkthrough (log in as the seeded admin, view dashboard, create/edit/deactivate a serviço and a profissional, filter the bookings list) with no console errors
- [ ] Automated tests (Vitest) pass for `AdminRoute`'s three-way redirect logic and the CRUD forms' validation/submission logic

## Open Questions

1. **No backend endpoint lists inactive/deactivated serviços or profissionais** — `GET /api/servicos`/`GET /api/profissionais` only ever return `ativo = true` rows (confirmed in `ServicoService`/`ProfissionalService`), and there's no reactivation endpoint either. Proposing: accept this as-is — the admin list shows exactly what the public list shows, deactivating an item makes it disappear from the admin's own list too, and there is no "view deactivated" or "undo" in this UI, because the backend (already `Done`, out of scope to reopen) doesn't support it. Confirm, or say if this gap is worth a small `catalog` backend addition (a new admin-only `GET` with an `ativo` filter) before `admin-web` proceeds.
2. **Hand-built bar chart vs. a real charting library.** The brief's Fase 5 prompt says "top serviços **com gráfico**." Proposing a minimal custom horizontal-bar visualization (plain `<div>`s sized by percentage, no new dependency) since the dataset is trivial (≤5 bars, one number each) and `client-web`'s Boundaries already lean toward not adding dependencies casually. Confirm, or say if a proper charting library (e.g. Recharts) is worth the added weight for a more polished look.
3. **Should the admin bookings list also offer `Concluir`/`Cancelar` actions**, or stay read-only with just filters/pagination as the brief's Fase 5 wording literally says ("lista de agendamentos com filtros e paginação")? Both backend endpoints already exist and are already used elsewhere (`concluir` in `scheduling`, `cancelar` already built into `client-web`'s own `MeusAgendamentosPage` logic, reusable here). Proposing: include both actions, since a bookings list an admin can only look at but never act on is a strange half-feature, and the marginal cost is low. Confirm, or say if this should stay strictly read-only for now.
4. **Default landing for a non-ADMIN authenticated user who manually navigates to an `/admin/*` URL.** Proposing: `AdminRoute` redirects them to `/servicos` silently (no error toast, no "access denied" page) — consistent with how this app doesn't yet have a dedicated error-page pattern anywhere. Confirm, or say if a visible "access denied" message matters enough to add that pattern now.
