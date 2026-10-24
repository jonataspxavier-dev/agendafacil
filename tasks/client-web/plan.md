# Implementation Plan: client-web (AgendaFácil)

Spec: [../../SPEC-client-web.md](../../SPEC-client-web.md)
Task list: [todo.md](todo.md)

## Overview

Build the customer-facing pages and shared app infrastructure on top of the existing `frontend/` React scaffold: login/register, a public service list, a 4-step booking flow, and "Meus agendamentos" with cancellation. No backend changes. The shared infra built here (axios client, `AuthContext`, `ProtectedRoute`, router shell, reverse-proxy setup) is deliberately reusable by `admin-web` next, since both live in the same codebase.

## Architecture Decisions

- **Same-origin API access via reverse proxy, not CORS.** `vite.config.ts` gets a `server.proxy` entry forwarding `/api` to the backend's host port for `npm run dev`; a new `frontend/nginx.conf` reverse-proxies `/api/` to the `backend` Docker Compose service for the production/Docker build. `api.ts`'s axios instance uses `baseURL: '/api'` with no environment variable. The backend (`identity`'s `SecurityConfig`) is not touched (resolved Open Question 1).
- **Tokens in `localStorage`**, matching what the backend already returns (JSON body, not cookies) — not a new decision so much as accepting `identity`'s existing contract (resolved Open Question 2).
- **The booking flow always starts fresh at step 1** after a forced login detour — no cross-redirect state persistence (resolved Open Question 3).
- **An ADMIN logging in through this screen lands on `/servicos` like any CLIENTE** — no role-aware redirect yet; that's `admin-web`'s job (resolved Open Question 4).
- **No pagination UI on `/servicos`** — the public catalog is 5 seeded services; `GET /api/servicos` without `page`/`size` already returns the full active list (resolved Open Question 5).
- **`AuthContext` is the single source of truth for auth state** (current user, tokens, loading flag), read everywhere via a `useAuth` hook — no prop-drilling, no duplicate token-reading logic in individual pages.
- **The booking flow is one component (`AgendarPage`) with local step state**, not four routes, per the Spec's Code Style section — `useState<Step>` gates which step renders, each step calling back up with its selection.
- **Build order follows the Spec's page list roughly bottom-up**: shared infra first (nothing else can be built or tested without it), then the simplest page (service list, no auth), then auth pages (needed by everything after), then the two protected pages last, since they depend on both infra and auth being real.

## Task List

### Phase 1: Foundation
- [x] Task 1: Shared infra — `api.ts` (axios instance + request/response interceptors for token attach + silent refresh), `AuthContext` + `useAuth` (with full `login`/`register`/`logout`, not stubs), `ProtectedRoute`, router shell in `App.tsx`, Vite dev proxy, `frontend/nginx.conf` + `Dockerfile` update, `@hookform/resolvers` installed

### Checkpoint: Foundation
- [x] `npm run build` and `npm test` pass with the new router shell in place (even with placeholder pages)
- [x] `docker compose up --build`: the frontend container can reach `GET /api/servicos` through its own origin (confirmed via `curl http://localhost:5173/api/servicos` matching the direct backend response) — proves the nginx reverse-proxy actually works, not just compiles. Also confirmed the Vite dev-server proxy independently.

### Phase 2: Public pages
- [x] Task 2: `ServicosPage` + `servicosService` — public list, loading/error states

### Phase 3: Auth
- [x] Task 3: `LoginPage` + `RegisterPage` (Zod validation, backend error surfacing e.g. 409 on duplicate email) — `authService`/`AuthContext` methods were already built in Task 1

### Checkpoint: Auth
- [x] Register → auto-logged-in → redirected to `/servicos` with tokens in storage
- [x] `ProtectedRoute`'s redirect-back-after-login verified end-to-end (visit a protected route logged out → `/login` → correct credentials → lands back on the originally-requested route)

### Phase 4: Booking + history (protected)
- [x] Task 4: `AgendarPage` (5-stage flow: serviço→profissional→data→horário→confirmar) + `profissionaisService` + `disponibilidadeService` + `agendamentosService.criar` — depends on Task 1 (ProtectedRoute), Task 3 (auth)
- [x] Task 5: `MeusAgendamentosPage` + `agendamentosService.listarMeus`/`cancelar` — depends on Task 1, Task 3

### Checkpoint: Booking + history
- [x] A booked slot immediately appears in "Meus agendamentos"
- [x] Cancelling >2h out succeeds; cancelling inside the window shows the backend's actual error message
- [x] An unauthenticated visit to either route redirects to `/login`, not a crash (verified for both routes — `/meus-agendamentos` during Task 3, `/agendar` is the same `ProtectedRoute` component already covered by its own unit tests)

### Phase 5: Polish + end-to-end verification
- [ ] Task 6: Shared `Layout`/`Nav` (auth-aware links), consistent `LoadingSpinner`/`ErrorMessage`, responsive pass, full manual `docker compose up --build` walkthrough: register → browse → book → see it in history → cancel

### Checkpoint: Module complete
- [ ] All 6 tasks checked off
- [ ] Every item in SPEC-client-web.md's Success Criteria is satisfied
- [ ] `docker compose up --build` works end-to-end for the full customer journey
- [ ] Human review before starting `admin-web`

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| nginx reverse-proxy misconfigured (wrong upstream, missing trailing slash) silently serves a 404/502 instead of proxying | High | Task 1's checkpoint explicitly curls/fetches `/api/servicos` through the running Docker Compose frontend container before any page is built on top of it — the proxy must be proven working in isolation, not discovered broken while debugging a page later |
| Axios refresh-interceptor retries infinitely on a persistently-401 request (e.g. refresh token itself expired) | Med | Guard with a "already retried once" flag on the request config; a second 401 after one refresh attempt clears storage and redirects, never loops |
| Booking flow lets a step advance without a real selection (e.g. clicking "next" with no date picked) | Med | Each step's "next" control is disabled until that step's local state is set; Task 4's tests assert this directly rather than relying on manual clicking |
| `ProtectedRoute` built before `AuthContext` has a real `login`, tested only against a stubbed/fake context, then silently breaks against the real one | Low | Task 1's `ProtectedRoute` test uses a minimal real `AuthContext.Provider` with controlled props, not a mock of the whole context shape, so Task 3 plugging in the real implementation can't drift from what Task 1 already verified |

## Open Questions

None outstanding — the five questions raised in SPEC-client-web.md (CORS vs. reverse proxy, token storage, booking-flow resume behavior, ADMIN's interim landing page, service-list pagination) were resolved during spec review and are captured as Architecture Decisions above.
