# Tasks: platform-setup (AgendaFácil)

Plan: [plan.md](plan.md) · Spec: [../../SPEC-platform-setup.md](../../SPEC-platform-setup.md)

## Task 1: Git init + top-level structure + CLAUDE.md ✅

**Description:** Initialize the git repo at `agendafacil/`, create the top-level folders (`backend/`, `frontend/`), copy the original brief in as `PLANO.md`, and write `CLAUDE.md` documenting the conventions every later module must follow.

**Acceptance criteria:**
- [x] `agendafacil/` is a git repo with `backend/` and `frontend/` directories present (even if empty)
- [x] `PLANO.md` is a copy of `../projeto-2-agendamentos-react-spring.md`
- [x] `CLAUDE.md` documents: layered backend architecture (controller/service/repository/dto/entity/security/exception), DTOs-as-records rule, never-expose-entities rule, mandatory tests for business rules, small-commit convention, and the Flyway naming convention (`V<n>__description.sql`, empty baseline pattern)

**Verification:**
- [x] `git log` shows at least one commit (`2f0cd30`)
- [x] Manual check: `CLAUDE.md` read back and each convention above is present

**Dependencies:** None

**Files likely touched:**
- `agendafacil/.git/` (init)
- `agendafacil/CLAUDE.md`
- `agendafacil/PLANO.md`

**Estimated scope:** Small: 1-2 files

---

## Task 2: Backend Spring Boot scaffold + Dockerfile ✅ (build verified; live check pending Docker)

**Description:** Generate the Spring Boot (4.0.9 — see Architecture Decisions in plan.md) / Java 21 Maven project in `backend/` with Web, Data JPA, Security, Validation, Flyway, PostgreSQL driver, Actuator, and springdoc-openapi dependencies. Create the empty package skeleton (`controller/`, `service/`, `repository/`, `dto/`, `entity/`, `security/`, `exception/`) with no business classes. Add a `Dockerfile` that builds and runs the jar.

**Acceptance criteria:**
- [x] `./mvnw clean verify` succeeds with no business logic
- [ ] `GET /actuator/health` returns 200 when run locally — **blocked: needs a reachable Postgres; app can't boot without Docker/Flyway target. Revisit once Docker is installed (Task 7).**
- [ ] `/swagger-ui.html` is reachable — same blocker as above
- [x] All seven package directories exist, empty except for `.gitkeep` (security/ got a real `SecurityConfig.java` instead)
- [x] `backend/Dockerfile` builds a runnable image (image build itself not yet tested — needs Docker)

**Verification:**
- [x] Build succeeds: `./mvnw clean verify` (from `backend/`) — passes, jar produced
- [ ] Manual check: `./mvnw spring-boot:run`, curl `/actuator/health` → 200, open `/swagger-ui.html` → loads — **blocked on Docker/Postgres, see above**

**Dependencies:** Task 1

**Files likely touched:**
- `backend/pom.xml`
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/.../AgendafacilApplication.java`
- `backend/Dockerfile`

**Estimated scope:** Medium: 3-5 files

---

## Task 3: Backend test infra (JUnit 5, Mockito, Testcontainers) ✅ (container smoke test pending Docker)

**Description:** Wire JUnit 5, Mockito, and Testcontainers (Postgres module) onto the Maven classpath with a working Testcontainers base config, so `identity` can add its first `@Test` without touching build config.

**Acceptance criteria:**
- [x] `./mvnw test` runs clean with zero `@Test` methods
- [ ] A Testcontainers Postgres container can start successfully in a throwaway smoke test — **blocked: needs Docker to actually spin up a container**
- [x] Dependencies are scoped `test` only (`spring-boot-testcontainers`, `testcontainers-junit-jupiter`, `testcontainers-postgresql`; JUnit5/Mockito already came in via `spring-boot-starter-*-test` artifacts added in Task 2)

**Verification:**
- [x] Tests pass: `./mvnw test` (0 tests, exit 0)
- [ ] Manual check: logs show the Testcontainers Postgres container starting and stopping — **blocked on Docker**

**Dependencies:** Task 2

**Files likely touched:**
- `backend/pom.xml`
- `backend/src/test/java/.../AgendafacilApplicationTests.java`
- `backend/src/test/resources/application-test.yml`

**Estimated scope:** Small: 1-2 files

---

## Task 4: Frontend Vite+TS scaffold + libs + Dockerfile ✅ (image build pending Docker)

**Description:** Scaffold `frontend/` with Vite + React + TypeScript, install and configure Tailwind CSS, and install (unused for now) React Router, Axios, React Hook Form, Zod. Create the `pages/`, `components/`, `services/`, `hooks/`, `contexts/` directories. Add a `Dockerfile` for the frontend.

**Acceptance criteria:**
- [x] `npm run dev` serves a page with Tailwind classes rendering correctly (verified in-browser: bordered/shadowed card, correct typography)
- [x] `npm run build` succeeds
- [x] `npm run lint` succeeds with no errors (oxlint — Vite's current default linter)
- [x] React Router, Axios, React Hook Form, Zod are installed dependencies (not yet used in any component)
- [x] All five directories exist
- [x] `frontend/Dockerfile` written (multi-stage: `npm run build` then nginx serves `dist/`) — **image build itself not yet tested, needs Docker**

**Verification:**
- [x] Build succeeds: `npm run build`
- [x] Manual check: `npm run dev`, opened in the built-in browser, confirmed Tailwind styling renders

**Dependencies:** Task 1

**Files likely touched:**
- `frontend/package.json`
- `frontend/tailwind.config.ts`
- `frontend/src/main.tsx`, `frontend/src/App.tsx`
- `frontend/Dockerfile`

**Estimated scope:** Medium: 3-5 files

---

## Task 5: Frontend test infra (Vitest + RTL) ✅

**Description:** Configure Vitest and React Testing Library so `client-web`/`admin-web` can add their first component test without touching build config.

**Acceptance criteria:**
- [x] `npm test` runs clean with one trivial smoke test (renders `<App />`, asserts text content)
- [x] Vitest config matches the Vite config (React plugin, Tailwind plugin, `@/` alias)

**Verification:**
- [x] Tests pass: `npm test` (1 passed)

**Dependencies:** Task 4

**Files likely touched:**
- `frontend/vitest.config.ts`
- `frontend/package.json`
- `frontend/src/App.test.tsx` (optional smoke test)

**Estimated scope:** Small: 1-2 files

---

## Task 6: Flyway baseline migration + config ✅ (DB verification pending Docker)

**Description:** Add Flyway configuration pointing at the Postgres service, with a genuinely empty `V1__baseline.sql` (no tables — schema ownership belongs to `identity`/`catalog`). Confirm Flyway runs on backend startup against a real Postgres instance.

**Acceptance criteria:**
- [x] `V1__baseline.sql` exists under `backend/src/main/resources/db/migration/` and contains no table definitions (a comment is fine)
- [ ] Backend startup logs show Flyway successfully applying the baseline migration against Postgres — **blocked: needs Docker/Postgres**
- [ ] `flyway_schema_history` table exists in the database after startup — same blocker

**Verification:**
- [ ] Manual check: run Postgres (container or local), start backend, confirm `flyway_schema_history` row for V1 exists — **blocked on Docker; revisit with Task 2's and Task 7's live checks once Docker is installed**

**Dependencies:** Task 2

**Files likely touched:**
- `backend/src/main/resources/db/migration/V1__baseline.sql`
- `backend/src/main/resources/application.yml`

**Estimated scope:** Small: 1-2 files

---

## Task 7: docker-compose.yml (postgres + backend + frontend)

**Description:** Write `docker-compose.yml` at `agendafacil/` root wiring the three services together via service-name hostnames, with healthchecks for each, so the whole stack starts with one command.

**Acceptance criteria:**
- [ ] `docker compose up --build` starts postgres, backend, frontend with zero manual steps, from a clean checkout
- [ ] Backend's Postgres connection uses the `postgres` service name, not `localhost`
- [ ] Frontend's API base URL uses the `backend` service name (or an env var defaulting to it), not `localhost`
- [ ] Each service has a healthcheck; `docker compose ps` shows all three healthy
- [ ] `docker compose down -v` cleans up cleanly

**Verification:**
- [ ] Manual check: `docker compose up --build` from a clean state, `docker compose ps` shows healthy, curl backend `/actuator/health` → 200 and frontend root → 200, then `docker compose down -v`

**Dependencies:** Task 2, Task 4, Task 6

**Files likely touched:**
- `agendafacil/docker-compose.yml`

**Estimated scope:** Small: 1-2 files

---

## Task 8: README.md stub + initial commit

**Description:** Write a `README.md` stub documenting the stack and the one-command run instruction. Make sure everything from Tasks 1-7 is committed.

**Acceptance criteria:**
- [ ] `README.md` lists the stack (Java 21/Spring Boot 3.x, React+Vite+TS, PostgreSQL, Docker) and the `docker compose up --build` instruction
- [ ] All scaffold files are committed to git

**Verification:**
- [ ] `git status` is clean
- [ ] Manual check: follow the README's own run instructions on a fresh clone-equivalent state and confirm the stack comes up

**Dependencies:** Task 7

**Files likely touched:**
- `agendafacil/README.md`

**Estimated scope:** Small: 1-2 files

---

## Checkpoint: Module complete

- [ ] All 8 tasks checked off
- [ ] Every item in SPEC-platform-setup.md's Success Criteria is satisfied
- [ ] Human review before starting the `identity` module
