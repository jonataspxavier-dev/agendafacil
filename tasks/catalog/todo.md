# Tasks: catalog (AgendaFácil)

Plan: [plan.md](plan.md) · Spec: [../../SPEC-catalog.md](../../SPEC-catalog.md)

## Task 1: Enable method security ✅

**Description:** Turn on `@EnableMethodSecurity` so `@PreAuthorize("hasRole('ADMIN')")` works on controller methods. `identity`'s `JwtAuthenticationConverter` already produces the `ROLE_ADMIN`/`ROLE_CLIENTE` authorities this relies on.

**Acceptance criteria:**
- [x] `@EnableMethodSecurity` added to `SecurityConfig`
- [x] A trivial `@PreAuthorize("hasRole('ADMIN')")`-annotated test-only controller (nested in `MethodSecurityTest`, `@Import`ed, never shipped in production code) proves CLIENTE → 403, ADMIN → 200, no token → 401

**Verification:**
- [x] Tests pass: `./mvnw test` (TDD red→green) and full `./mvnw clean verify`

**Bug found and fixed as part of this task:** `@PreAuthorize` denial (`AccessDeniedException`) had no specific handler in `GlobalExceptionHandler`, so it fell through to the generic catch-all and returned 500 instead of 403. Added a handler mapping it to 403 with the same `ErrorResponse` shape — same category of gap as the 404 fix at the end of `identity`, caught this time by the task's own test instead of manual verification.

**Dependencies:** None

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/security/SecurityConfig.java`

**Estimated scope:** Small: 1 file

---

## Task 2: `servico` schema + entity + repository

**Description:** `V5__create_servico.sql`, `Servico` entity, `ServicoRepository`.

**Acceptance criteria:**
- [ ] `V5__create_servico.sql`: `servico(id, nome, descricao, duracao_min, preco, ativo DEFAULT true)`
- [ ] `Servico` entity maps 1:1; `preco` as `BigDecimal`
- [ ] `ServicoRepository extends JpaRepository<Servico, Long>` with `findAllByAtivoTrue(Pageable)` (or equivalent)

**Verification:**
- [ ] Tests pass: `./mvnw test` — Testcontainers Postgres applies `V1`→`V5` cleanly

**Dependencies:** None

**Files likely touched:**
- `backend/src/main/resources/db/migration/V5__create_servico.sql`
- `backend/src/main/java/com/agendafacil/backend/entity/Servico.java`
- `backend/src/main/java/com/agendafacil/backend/repository/ServicoRepository.java`

**Estimated scope:** Small: 2-3 files

---

## Task 3: `profissional` + `profissional_servico` + `jornada` schema + entities + repositories

**Description:** `V6__create_profissional.sql` (profissional + join table), `V7__create_jornada.sql`. `Profissional` entity owns a `@ManyToMany Set<Servico>` and a `@OneToMany List<Jornada>` (cascade + orphanRemoval, since jornada rows have no existence outside their professional).

**Acceptance criteria:**
- [ ] `V6__create_profissional.sql`: `profissional(id, nome, ativo DEFAULT true)` + `profissional_servico(profissional_id, servico_id)` join table with a composite PK or unique constraint
- [ ] `V7__create_jornada.sql`: `jornada(id, profissional_id FK, dia_semana, hora_inicio, hora_fim)`
- [ ] `Profissional` entity: `@ManyToMany` to `Servico` via the join table, `@OneToMany(cascade = ALL, orphanRemoval = true)` to `Jornada`
- [ ] `Jornada` entity: `diaSemana` as `java.time.DayOfWeek` (`@Enumerated(EnumType.STRING)`), `horaInicio`/`horaFim` as `LocalTime`
- [ ] `ProfissionalRepository`, `JornadaRepository` (the latter mostly used internally by `ProfissionalService`, not exposed as its own REST resource)

**Verification:**
- [ ] Tests pass: `./mvnw test` — Testcontainers Postgres applies `V1`→`V7` cleanly; a repository test saves a Profissional with services and jornada attached and reads it back

**Dependencies:** Task 2 (FK to `servico`)

**Files likely touched:**
- `backend/src/main/resources/db/migration/V6__create_profissional.sql`
- `backend/src/main/resources/db/migration/V7__create_jornada.sql`
- `backend/src/main/java/com/agendafacil/backend/entity/Profissional.java`
- `backend/src/main/java/com/agendafacil/backend/entity/Jornada.java`
- `backend/src/main/java/com/agendafacil/backend/repository/ProfissionalRepository.java`
- `backend/src/main/java/com/agendafacil/backend/repository/JornadaRepository.java`

**Estimated scope:** Medium: 6 files

---

## Task 4: `GET /api/servicos`

**Description:** Public listing of active services, optional paging.

**Acceptance criteria:**
- [ ] `GET /api/servicos` returns only `ativo = true` services, 200, no auth required
- [ ] Optional `page`/`size`/`sort` query params page the result; omitting them returns the full active list
- [ ] `ServicoResponse` DTO — no entity exposed

**Verification:**
- [ ] Tests pass: `./mvnw test`
- [ ] Manual check: `docker compose up`, curl `/api/servicos` with no auth header → 200 with the active list

**Dependencies:** Task 2

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/dto/ServicoResponse.java`
- `backend/src/main/java/com/agendafacil/backend/service/ServicoService.java`
- `backend/src/main/java/com/agendafacil/backend/controller/ServicoController.java`
- `backend/src/test/java/.../controller/ServicoControllerListTest.java`

**Estimated scope:** Small: 3-4 files

---

## Task 5: `POST`/`PUT`/`DELETE /api/servicos`

**Description:** Admin-only write endpoints. `DELETE` sets `ativo = false`.

**Acceptance criteria:**
- [ ] `POST /api/servicos` creates, 201, `ROLE_ADMIN` only (401 no token, 403 CLIENTE token)
- [ ] `PUT /api/servicos/{id}` updates, 200, same auth rule; 404 if id doesn't exist
- [ ] `DELETE /api/servicos/{id}` sets `ativo = false`, 204, same auth rule; the service then disappears from `GET /api/servicos`
- [ ] `ServicoRequest` validated (`nome` not blank, `duracaoMin` positive, `preco` positive)
- [ ] Integration tests: full CRUD cycle as ADMIN; 401/403 as no-token/CLIENTE

**Verification:**
- [ ] Tests pass: `./mvnw test`
- [ ] Manual check: `docker compose up`, full CRUD cycle via curl with an admin token (from `identity`'s seeded admin)

**Dependencies:** Task 1, Task 2

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/dto/ServicoRequest.java`
- `backend/src/main/java/com/agendafacil/backend/service/ServicoService.java` (add create/update/delete)
- `backend/src/main/java/com/agendafacil/backend/controller/ServicoController.java` (add write endpoints)
- `backend/src/test/java/.../controller/ServicoControllerWriteTest.java`

**Estimated scope:** Medium: 3-4 files

---

## Task 6: `GET /api/profissionais`

**Description:** Public listing of active professionals, including their service ids and jornada, optional paging.

**Acceptance criteria:**
- [ ] `GET /api/profissionais` returns only `ativo = true` professionals, 200, no auth required
- [ ] `ProfissionalResponse` includes `servicoIds` (list of linked service ids) and `jornada` (list of day/start/end)
- [ ] Optional `page`/`size`/`sort` query params, same pattern as Task 4

**Verification:**
- [ ] Tests pass: `./mvnw test`
- [ ] Manual check: `docker compose up`, curl `/api/profissionais` with no auth header → 200

**Dependencies:** Task 3

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/dto/ProfissionalResponse.java`
- `backend/src/main/java/com/agendafacil/backend/dto/JornadaResponse.java`
- `backend/src/main/java/com/agendafacil/backend/service/ProfissionalService.java`
- `backend/src/main/java/com/agendafacil/backend/controller/ProfissionalController.java`
- `backend/src/test/java/.../controller/ProfissionalControllerListTest.java`

**Estimated scope:** Medium: 3-5 files

---

## Task 7: `POST`/`PUT`/`DELETE /api/profissionais`

**Description:** Admin-only write endpoints. Create/update accept `servicoIds` + `jornada` and replace both atomically. `DELETE` sets `ativo = false`.

**Acceptance criteria:**
- [ ] `POST /api/profissionais` creates (with optional initial `servicoIds`/`jornada`), 201, `ROLE_ADMIN` only
- [ ] `PUT /api/profissionais/{id}` replaces `servicoIds` (set-replace on the `@ManyToMany`) and `jornada` (delete-then-reinsert) atomically, 200, same auth rule
- [ ] `DELETE /api/profissionais/{id}` sets `ativo = false`, 204, same auth rule
- [ ] Referencing a nonexistent `servicoId` in the request → 400, not a silent drop
- [ ] `ProfissionalRequest` validated (`nome` not blank)
- [ ] Integration tests: full CRUD cycle as ADMIN including linking 2 services and setting 3 jornada entries; updating jornada replaces the old entries (not additive); 401/403 as no-token/CLIENTE

**Verification:**
- [ ] Tests pass: `./mvnw test`
- [ ] Manual check: `docker compose up`, full CRUD cycle via curl with an admin token, including a jornada update that proves old entries are gone (count check)

**Dependencies:** Task 1, Task 3

**Files likely touched:**
- `backend/src/main/java/com/agendafacil/backend/dto/ProfissionalRequest.java`
- `backend/src/main/java/com/agendafacil/backend/dto/JornadaRequest.java`
- `backend/src/main/java/com/agendafacil/backend/service/ProfissionalService.java` (add create/update/delete)
- `backend/src/main/java/com/agendafacil/backend/controller/ProfissionalController.java` (add write endpoints)
- `backend/src/test/java/.../controller/ProfissionalControllerWriteTest.java`

**Estimated scope:** Large: 5-6 files — most complex task in this module; split into a commit for basic CRUD and a second for jornada/service-association handling if it grows unwieldy, rather than redesigning the API mid-task

---

## Task 8: Seed migration

**Description:** `V8__seed_catalog.sql`: 3 profissionais, 5 serviços, `profissional_servico` links, Monday-Saturday jornadas for each professional.

**Acceptance criteria:**
- [ ] `V8__seed_catalog.sql` inserts 5 serviços (generic salon demo data — see SPEC's resolved Open Question #3), 3 profissionais, links each professional to at least one service, and gives each professional a Mon-Sat 09:00-18:00 jornada
- [ ] Integration test: after migrations, `GET /api/servicos` returns 5, `GET /api/profissionais` returns 3 with non-empty `servicoIds` and 6 `jornada` entries each

**Verification:**
- [ ] Tests pass: `./mvnw test`
- [ ] Manual check: `docker compose up`, curl both list endpoints, confirm counts match

**Dependencies:** Task 2, Task 3

**Files likely touched:**
- `backend/src/main/resources/db/migration/V8__seed_catalog.sql`
- `backend/src/test/java/.../CatalogSeedTest.java`

**Estimated scope:** Small: 2 files

---

## Checkpoint: Module complete

- [ ] All 8 tasks checked off
- [ ] Every item in SPEC-catalog.md's Success Criteria is satisfied
- [ ] `docker compose up --build` works end-to-end
- [ ] Human review before starting `scheduling`
