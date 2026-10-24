# Tasks: client-web (AgendaFácil)

Plan: [plan.md](plan.md) · Spec: [../../SPEC-client-web.md](../../SPEC-client-web.md)

## Task 1: Shared infra (axios client, auth context, protected routes, reverse proxy) ✅

**Description:** Everything later tasks depend on: the axios instance with token-attach + silent-refresh interceptors, `AuthContext`, `ProtectedRoute`, the router shell replacing `App.tsx`'s placeholder, and same-origin API access via reverse proxy in both dev and Docker.

**Acceptance criteria:**
- [x] `npm install @hookform/resolvers` added to `frontend/package.json`
- [x] `services/api.ts`: axios instance, `baseURL: '/api'`; request interceptor attaches `Authorization: Bearer <accessToken>` from storage when present; response interceptor catches a `401`, attempts `POST /api/auth/refresh` once (guarded against infinite retry via a `_retry` flag), retries the original request on success, clears storage and redirects to `/login` on failure
- [x] `contexts/AuthContext.tsx` + `hooks/useAuth.ts`: exposes `{ user, isAuthenticated, isLoading, login, register, logout }`; on mount, reads tokens from storage and calls `GET /api/auth/me` to hydrate `user` (or clears storage if that call fails)
- [x] `components/ProtectedRoute.tsx`: renders its children when `isAuthenticated`; redirects to `/login` otherwise; shows nothing (or a spinner) while `isLoading`
- [x] `App.tsx` becomes a `react-router-dom` router shell with placeholder routes for `/`, `/login`, `/cadastro`, `/agendar` (wrapped in `ProtectedRoute`), `/meus-agendamentos` (wrapped in `ProtectedRoute`)
- [x] `vite.config.ts`: `server.proxy` forwards `/api` to the backend's host port for `npm run dev`
- [x] `frontend/nginx.conf` (new): reverse-proxies `/api/` to `http://backend:8080/api/`; `frontend/Dockerfile` copies it into the nginx image

**Scope note:** `AuthContext`'s `login`/`register`/`logout` got full working implementations here instead of stubs deferred to Task 3 — they're thin wrappers over `authService` with no UI dependency, so leaving them unimplemented would have been a half-finished commit for no benefit. Task 3 is now purely the `LoginPage`/`RegisterPage` forms (RHF + Zod) calling the already-wired context methods, not also building the methods themselves. `authService.ts` was created now too (`registrar`/`login`/`me`/`logout`, plus `refresh` re-exported from `api.ts` to avoid a circular import between the two files).

**Verification:**
- [x] Tests pass: `npm test` (4/4 — `ProtectedRoute` redirects when unauthenticated, renders children when authenticated via a real `AuthContext.Provider` with controlled props, and renders nothing while loading) and `npm run build` succeeds
- [x] Manual check: `docker compose up --build` — `curl http://localhost:5173/api/servicos` (the frontend's own origin) returned the same JSON as `curl http://localhost:8080/api/servicos` (the backend directly), proving the nginx reverse-proxy actually forwards. Also verified the Vite dev-server proxy separately (`npm run dev`, which picked port 5174 since Docker already held 5173) — same result.

**Bugs found and fixed along the way:**
- `npm install @hookform/resolvers` needed `--legacy-peer-deps` (its v6 has an unrelated `peerOptional` chain on `effect`/`@effect/schema` that strict resolution can't satisfy; irrelevant to the zod-only resolver subpath this project actually uses) — but that install silently pruned `@testing-library/dom` (a transitive peer of `@testing-library/react` that strict mode had been auto-installing). Fixed by adding it explicitly as a devDependency.
- `@testing-library/react`'s auto-cleanup between tests never engaged because `vitest.config.ts` doesn't set `test.globals: true` (test files import `describe`/`it`/`expect` explicitly instead), so DOM from one test in a file leaked into the next. Fixed by adding an explicit `afterEach(cleanup)` in `src/test/setup.ts` — a durable fix for every future test file, not just this one.

**Dependencies:** none (this is the module's foundation)

**Files likely touched:**
- `frontend/package.json` (add `@hookform/resolvers`, `@testing-library/dom`)
- `frontend/src/services/api.ts`
- `frontend/src/services/authService.ts`
- `frontend/src/contexts/AuthContext.tsx`
- `frontend/src/hooks/useAuth.ts`
- `frontend/src/components/ProtectedRoute.tsx`
- `frontend/src/App.tsx`
- `frontend/vite.config.ts`
- `frontend/nginx.conf`
- `frontend/Dockerfile`
- `frontend/src/test/setup.ts`
- `frontend/src/components/ProtectedRoute.test.tsx`

**Estimated scope:** Large: 9-10 files — infra that nothing else can be built on top of until it's right

---

## Task 2: `ServicosPage` ✅

**Description:** The public landing page: list active services, no login required.

**Acceptance criteria:**
- [x] `services/servicosService.ts`: `listar()` calls `GET /api/servicos`
- [x] `pages/ServicosPage.tsx`: fetches on mount, shows a loading state, then the list (nome, descrição, duração, preço formatted via `Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })`), or an error message with a retry action on failure
- [x] Each service has an "Agendar" action that navigates to `/agendar` (carrying the chosen `servicoId` via route state — read by `AgendarPage` in Task 4)
- [x] Route (`/servicos`) is public — not wrapped in `ProtectedRoute`; `/` redirects to `/servicos`

**Verification:**
- [x] Tests pass: `npm test` (7/7 — `ServicosPage` renders a loading state, then the list on a successful mocked fetch, then an error state with a retry button on a failed one) and `npm run build`
- [x] Manual check: `docker compose up --build`, opened the frontend in the browser pane with no login, confirmed `/` redirects to `/servicos` and all 5 seeded services load with correct BRL formatting, no console errors. Checked mobile width (375px) too — no horizontal scroll, cards stack cleanly.

**Bug found and fixed along the way:** the page rendered with a black background and barely-visible dark-on-dark text — `index.css` set no explicit body background/color, so it inherited the browser's dark `prefers-color-scheme` default instead of the app's intended light theme. Fixed with an explicit `background-color`/`color` on `body` in `index.css` (a durable fix, not page-specific).

**Dependencies:** Task 1 (router shell)

**Files likely touched:**
- `frontend/src/services/servicosService.ts`
- `frontend/src/pages/ServicosPage.tsx`
- `frontend/src/pages/ServicosPage.test.tsx`
- `frontend/src/index.css` (explicit light background/text color)
- `frontend/src/App.tsx` (`/` → `/servicos` redirect, route wired)
- `frontend/src/App.test.tsx` (updated for the new redirect, mocking `servicosService`)

**Estimated scope:** Small: 3 files

---

## Task 3: `LoginPage` + `RegisterPage`

**Description:** The two form pages. `authService.ts` and `AuthContext`'s real `login`/`register`/`logout` methods were already built in Task 1 (see its scope note) — this task is the forms and their validation/UX on top of that.

**Acceptance criteria:**
- [ ] `pages/LoginPage.tsx` / `pages/RegisterPage.tsx`: React Hook Form + Zod (`@hookform/resolvers/zod`) validate client-side (required fields, email format, password min length) before any network call; a failed backend call (e.g. 401 on login, 409 on duplicate-email register) surfaces that specific message, not a generic one
- [ ] Successful login redirects to `/servicos` (or back to the originally-requested protected route per `ProtectedRoute`'s redirect state, if present)
- [ ] Successful register auto-logs-in (per `AuthContext.register`'s Task 1 implementation, which chains to `login` internally) and redirects the same way

**Verification:**
- [ ] Tests pass: `npm test` — Zod schema rejects empty/malformed input before calling the mocked service; a mocked 409 register response surfaces that message; a mocked successful login updates `AuthContext` state
- [ ] Manual check: `docker compose up --build`, register a new account, confirm it logs in (or redirects to login) and `GET /api/auth/me` reflects it; log out and confirm storage is cleared and the user lands on `/login`

**Dependencies:** Task 1 (`AuthContext` shape, `api.ts`)

**Files likely touched:**
- `frontend/src/services/authService.ts`
- `frontend/src/contexts/AuthContext.tsx` (fill in real methods)
- `frontend/src/pages/LoginPage.tsx`
- `frontend/src/pages/RegisterPage.tsx`
- `frontend/src/pages/LoginPage.test.tsx`
- `frontend/src/pages/RegisterPage.test.tsx`
- `frontend/src/contexts/AuthContext.test.tsx`

**Estimated scope:** Large: 7 files — forms, validation, and the real auth wiring together

---

## Task 4: `AgendarPage` (4-step booking flow)

**Description:** The product's core customer-facing flow: serviço → profissional → data → horário → confirmar, against real backend data.

**Acceptance criteria:**
- [ ] `services/profissionaisService.ts`: `listar()` → `GET /api/profissionais`; `services/disponibilidadeService.ts`: `buscar(profissionalId, servicoId, data)` → `GET /api/disponibilidade`; `services/agendamentosService.ts` gets `criar(profissionalId, servicoId, inicio)` → `POST /api/agendamentos`
- [ ] Step 1 (serviço): pre-selected if arriving from `ServicosPage` with a `servicoId`, otherwise a picker; Step 2 (profissional): only professionals whose `servicoIds` includes the selected service (filtered client-side); Step 3 (data): a date picker (today or later); Step 4 (horário): real slots from `GET /api/disponibilidade` for the selected profissional+servico+data, showing "no slots" clearly if the list is empty
- [ ] Each step's "next" control is disabled until that step has a real selection — can't advance on an empty step
- [ ] Confirm step shows a summary (serviço, profissional, data/horário, preço) and calls `POST /api/agendamentos` with exactly the selected ids/time on submit; success navigates to `/meus-agendamentos`; a backend error (e.g. 409 conflict, 400 outside jornada) surfaces inline, not as a crash
- [ ] Route wrapped in `ProtectedRoute`

**Verification:**
- [ ] Tests pass: `npm test` — step-guarding logic (can't advance without a selection); professional list is correctly filtered by the selected service's id; the confirm submission payload matches the selections exactly (mocked service)
- [ ] Manual check: `docker compose up --build`, log in, book a real slot end-to-end, confirm it's created with the right price snapshot

**Dependencies:** Task 1 (`ProtectedRoute`), Task 3 (real auth)

**Files likely touched:**
- `frontend/src/services/profissionaisService.ts`
- `frontend/src/services/disponibilidadeService.ts`
- `frontend/src/services/agendamentosService.ts` (new file; also used by Task 5)
- `frontend/src/pages/AgendarPage.tsx`
- `frontend/src/pages/AgendarPage.test.tsx`

**Estimated scope:** Large: 6 files — the module's most involved page

---

## Task 5: `MeusAgendamentosPage`

**Description:** The authenticated user's booking history, with cancellation.

**Acceptance criteria:**
- [ ] `agendamentosService` (from Task 4) gets `listarMeus()` → `GET /api/agendamentos/meus`; `cancelar(id)` → `PATCH /api/agendamentos/{id}/cancelar`
- [ ] `pages/MeusAgendamentosPage.tsx`: fetches on mount, loading/error states; each booking shows service, professional, date/time, status, price; a "Cancelar" action only on `AGENDADO` bookings
- [ ] Cancelling more than 2h out succeeds and the list updates (either by refetching or updating local state) to show `CANCELADO`; cancelling inside the window shows the backend's actual error message (not a generic "something went wrong")
- [ ] Route wrapped in `ProtectedRoute`

**Verification:**
- [ ] Tests pass: `npm test` — list renders bookings with correct status-based cancel-button visibility; a mocked cancel success updates state; a mocked cancel failure (400) surfaces its message
- [ ] Manual check: `docker compose up --build`, book a slot, see it listed, cancel it (success), attempt to cancel a near-future booking inside the 2h window and see the backend's rejection message

**Dependencies:** Task 1 (`ProtectedRoute`), Task 3 (real auth), Task 4 (`agendamentosService` exists)

**Files likely touched:**
- `frontend/src/services/agendamentosService.ts` (add `listarMeus`/`cancelar`)
- `frontend/src/pages/MeusAgendamentosPage.tsx`
- `frontend/src/pages/MeusAgendamentosPage.test.tsx`

**Estimated scope:** Medium: 3 files

---

## Task 6: Polish + end-to-end verification

**Description:** Shared layout/nav, consistent loading/error UI, a responsive pass, and the full manual journey through the real stack.

**Acceptance criteria:**
- [ ] `components/Layout.tsx`/`Nav.tsx`: auth-aware nav links (Login/Cadastro when logged out; Meus Agendamentos/Sair when logged in), wraps all routes
- [ ] `components/LoadingSpinner.tsx` and `components/ErrorMessage.tsx` extracted and reused across every page built in Tasks 2-5 (replacing any ad hoc loading/error markup written along the way)
- [ ] Layout is usable at mobile width (no horizontal scroll, nav doesn't overlap content) — manual check at a narrow viewport, not an automated test
- [ ] Full manual walkthrough against `docker compose up --build`: register → browse `/servicos` with no login → attempt `/agendar` while logged out (redirected to `/login`) → log in → book a real slot → see it in `/meus-agendamentos` → cancel it → log out

**Verification:**
- [ ] Tests pass: `npm test` (full suite) and `npm run build`
- [ ] Manual check: the walkthrough above, performed once start-to-finish against the real Docker Compose stack, no console errors (CORS or otherwise)

**Dependencies:** Tasks 1-5

**Files likely touched:**
- `frontend/src/components/Layout.tsx`
- `frontend/src/components/Nav.tsx`
- `frontend/src/components/LoadingSpinner.tsx`
- `frontend/src/components/ErrorMessage.tsx`
- `frontend/src/App.tsx` (wrap routes in `Layout`)
- touch-ups to Tasks 2-5's pages to use the extracted components

**Estimated scope:** Medium: 5-6 files, mostly extraction/consistency — no new business logic

---

## Checkpoint: Module complete

- [ ] All 6 tasks checked off
- [ ] Every item in SPEC-client-web.md's Success Criteria is satisfied
- [ ] `docker compose up --build` works end-to-end for the full customer journey
- [ ] Human review before starting `admin-web`
