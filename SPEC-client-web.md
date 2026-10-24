# Spec: client-web (AgendaFácil)

Part of [capability-map.md](capability-map.md). Depends on: `scheduling` (booking/disponibilidade API), `catalog` (serviços/profissionais API) — both done.

## Objective

The customer-facing half of the single `frontend/` React app (per the brief's folder structure, `client-web` and `admin-web` are two phases of work on the *same* codebase, not separate projects — `admin-web` will later add admin routes/pages on top of what this module builds). A visitor can browse services with no login, register/log in, book a service through a 4-step flow (serviço → profissional → data → horário → confirmar), and manage their own bookings ("Meus agendamentos", with cancellation).

Done means: login/register work against the real backend; an unauthenticated user can browse `/servicos` but is redirected to `/login` when attempting to book; a logged-in CLIENTE can complete the full booking flow against real availability and see it immediately in "Meus agendamentos"; cancellation respects the backend's 2-hour rule and surfaces the resulting error clearly; every page handles loading and error states; routes requiring auth are actually protected (not just hidden from the nav).

## Tech Stack

Already scaffolded by `platform-setup` (confirmed in `frontend/package.json`): React 19, Vite, TypeScript, React Router 7, Axios, React Hook Form 7 + Zod 4, Tailwind CSS 4, Vitest + Testing Library + jsdom. **One missing piece**: `@hookform/resolvers` isn't installed yet — needed to wire Zod schemas into React Hook Form's `resolver` prop; add it in Task 1.

No new backend endpoints — `catalog`'s `GET /api/profissionais` has no `servicoId` filter, so "which professionals offer the selected service" is computed client-side from each professional's `servicoIds` array (fine at this seed-data scale: 3 professionals, 5 services).

## Commands

From `agendafacil/frontend/`: `npm run dev` (Vite dev server), `npm run build` (`tsc -b && vite build`), `npm test` (Vitest), `npm run lint` (oxlint). Full stack: `docker compose up --build` from `agendafacil/`.

## Project Structure

Following `CLAUDE.md`'s existing convention (`pages/`, `components/`, `services/`, `hooks/`, `contexts/`, `@/` absolute imports, functional components, named exports):

```
src/
├── contexts/    AuthContext (user, tokens, login/register/logout, loading state)
├── services/    api.ts (axios instance + interceptors), authService, servicosService,
│                profissionaisService, disponibilidadeService, agendamentosService
├── hooks/       useAuth (reads AuthContext)
├── components/  ProtectedRoute, Layout/Nav, LoadingSpinner, ErrorMessage
└── pages/       LoginPage, RegisterPage, ServicosPage, AgendarPage (the 4-step flow),
                 MeusAgendamentosPage
```

`App.tsx` becomes the router shell (`react-router-dom`'s `<Routes>`), replacing the current placeholder.

## Code Style

- **API calls only through `services/`, never raw `axios`/`fetch` inside a component** (existing `CLAUDE.md` rule) — pages call a `services/*Service.ts` function, which calls the shared `api.ts` axios instance.
- **Same-origin API path, no CORS.** `api.ts` uses relative paths (`axios.create({ baseURL: '/api' })`, no absolute URL, no `VITE_API_URL` env var). In dev, `vite.config.ts`'s `server.proxy` forwards `/api` to the backend's host port. In the Docker Compose stack, `frontend/nginx.conf` (new) reverse-proxies `/api/` to the `backend` service by Docker Compose service name — satisfying `CLAUDE.md`'s "Docker Compose service names, not `localhost`" rule, since it's nginx (inside the Docker network) doing that resolution, not the browser. The backend needs **no CORS configuration** and no changes at all — the browser only ever talks to its own origin.
- **Tokens in `localStorage`**, attached via an axios request interceptor (`Authorization: Bearer <accessToken>`). A response interceptor catches a `401`, attempts `POST /api/auth/refresh` once with the stored `refreshToken`, retries the original request on success, or clears storage and redirects to `/login` on failure.
- **Zod schemas colocated with each form** (login, register, booking confirmation if needed), wired via `@hookform/resolvers/zod`.
- **Dates/times formatted with `Intl.DateTimeFormat`/`Intl.NumberFormat` (locale `pt-BR`)** — no new date library; the brief's declared stack doesn't include one and the app's formatting needs (a date, a time, a price) don't justify adding one.
- **The booking flow is one page with local step state** (`useState<1|2|3|4>`), not four separate routes — a linear 4-step wizard doesn't need deep-linkable/bookmarkable intermediate steps, and a single component avoids URL-state-sync complexity for no real benefit here.

## Testing Strategy

Vitest + Testing Library (already configured), consistent with this project's "tests for business rules" principle applied to the frontend: not every component needs a test, but logic that can silently break does.

- **`ProtectedRoute`**: redirects to `/login` when unauthenticated, renders children when authenticated.
- **`AuthContext`**: login success stores tokens and user; login failure surfaces the error; logout clears state.
- **Form validation** (login/register): Zod schema rejects invalid input (empty fields, bad email, short password) with the right message, before any network call.
- **Booking flow**: stepping forward requires a selection at each step (can't skip ahead); the confirm step submits exactly the selected `profissionalId`/`servicoId`/`inicio`.
- **Services layer**: each `services/*Service.ts` function calls the right endpoint with the right shape (mocked axios, not a real backend — backend correctness is `scheduling`/`catalog`'s job, already tested there).
- Explicitly **not** covering: pixel-level styling, full end-to-end browser flows (no Cypress/Playwright in this module's scope) — manual `docker compose up` verification covers that, same pattern as every backend module.

## Boundaries

- **Always:** route every API call through `services/`; protect `/agendar` and `/meus-agendamentos` with `ProtectedRoute`; handle loading and error states on every data-fetching page (per the brief's explicit Fase 4 requirement) — a blank screen or an unhandled rejection is never acceptable.
- **Ask first:** adding any new dependency beyond `@hookform/resolvers` (e.g. a date library, a UI component kit, React Query) — the brief's declared stack is deliberately small.
- **Never:** store a plaintext password anywhere other than the one in-flight request to `/api/auth/login` or `/api/auth/register`; let the booking confirm step submit without re-validating that a slot was actually selected; duplicate business rules already enforced server-side (e.g. don't try to re-implement the 2-hour cancellation window client-side beyond disabling an obviously-expired button as a UX nicety — the backend's 400 is still the real guard, the UI must handle it either way).

## Success Criteria

- [ ] `/servicos` lists active services with no login required; shows a loading state, then the list, or an error message on failure
- [ ] `/cadastro` and `/login` validate input client-side (Zod) and surface backend errors (e.g. duplicate email → 409) clearly; successful login stores tokens and redirects
- [ ] An unauthenticated visitor hitting `/agendar` or `/meus-agendamentos` is redirected to `/login`, not shown a broken page
- [ ] The booking flow's 4 steps (serviço → profissional → data → horário → confirmar) only show professionals that offer the selected service, only show real open slots from `GET /api/disponibilidade`, and the confirm step calls `POST /api/agendamentos` with exactly the selected ids/time
- [ ] A successful booking immediately appears in `/meus-agendamentos`
- [ ] Cancelling a booking more than 2 hours out succeeds and updates the list; cancelling one inside the window shows the backend's error message, not a generic failure
- [ ] An expired/invalid access token triggers a silent refresh-and-retry; a failed refresh logs the user out cleanly (no stuck loading state, no stale token left in storage)
- [ ] `docker compose up --build` serves the frontend and it can complete a full register → book → cancel flow against the real backend with no CORS errors in the browser console
- [ ] Automated tests (Vitest) pass for `ProtectedRoute`, `AuthContext`, form validation, and the booking flow's step-guarding logic

## Open Questions

1. **Same-origin API access via reverse proxy (Vite dev proxy + new `nginx.conf`), instead of enabling CORS on the backend.** This avoids touching `scheduling`/`identity`'s `SecurityConfig` at all and matches `CLAUDE.md`'s service-name convention. Confirm, or say if you'd rather the backend expose CORS headers (simpler infra, but the brief's own Fase 6 flags CORS as something to "revisar," suggesting it's meant to be minimized, not configured open).
2. **Tokens in `localStorage`, not an httpOnly cookie.** The backend already returns both tokens as JSON body fields (not `Set-Cookie`), so this is really "accept what the backend already does" rather than a free choice — switching to cookie-based storage would require backend changes to `identity`, which is done and out of scope to reopen. Confirm proceeding with `localStorage` (standard SPA pattern, minor XSS-exposure trade-off acceptable for a portfolio demo).
3. **An unauthenticated user who clicks "Agendar" is redirected to `/login` and, after logging in, lands back on a *fresh* `/agendar` (step 1), not resumed at whatever step they were on.** Proposing this simpler behavior over preserving in-progress selections across the login detour. Confirm, or say if resuming mid-flow matters enough to justify the extra state-persistence complexity.
4. **What does an ADMIN who logs in through this same `/login` screen see, before `admin-web` exists?** Proposing: nothing special yet — they land on `/servicos` like any CLIENTE, and `admin-web` (next module) adds role-aware routing/redirects on top. Confirm this is an acceptable intermediate state, not a gap to fix now.
5. **No pagination/filtering UI on `/servicos`** even though the backend supports optional paging — the seed catalog is 5 services, and the brief's Fase 4 prompt doesn't mention filters for the customer-facing list (that's Fase 5, admin). Confirm scope, or say if the customer list should paginate too.
