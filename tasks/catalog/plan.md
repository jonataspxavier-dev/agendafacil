# Implementation Plan: catalog (AgendaFácil)

Spec: [../../SPEC-catalog.md](../../SPEC-catalog.md)
Task list: [todo.md](todo.md)

## Overview

Servico and Profissional CRUD, backed by Flyway migrations `V5`-`V8`, with public reads and `ROLE_ADMIN`-only writes. A professional's service associations and weekly jornada are managed atomically through the professional's own request/response, not as separate sub-resources — simpler API surface, matches how an admin would actually edit a professional's profile.

## Architecture Decisions

- **`@EnableMethodSecurity` + `@PreAuthorize("hasRole('ADMIN')")`** on write endpoints, rather than path-based rules in `SecurityConfig`. Method-level annotations keep the auth rule next to the code it protects and read clearly per-endpoint; `identity`'s `JwtAuthenticationConverter` already produces `ROLE_ADMIN`/`ROLE_CLIENTE` authorities, so this is just turning on the switch.
- **No separate `jornada` or `profissional_servico` REST endpoints.** `ProfissionalRequest` carries the full `servicoIds` list and `jornada` list; `PUT` replaces both sets atomically (delete-then-reinsert for jornada rows, set-replace for the `@ManyToMany` service association). Simpler than diffing, and acceptable at this scale (a handful of rows per professional).
- **Soft delete only.** `DELETE` sets `ativo = false`. There is no "list including inactive" endpoint in this module's scope — public `GET` always filters `ativo = true`. If an admin needs to see deactivated rows later, that's a `catalog`-v2 concern, not blocking this module.
- **Pagination is opt-in.** `GET` endpoints accept optional `page`/`size`/`sort`; omitting them returns the full active list. Matches the SPEC's resolved Open Question #1.

## Task List

### Phase 1: Foundation
- [x] Task 1: Enable method security (`@EnableMethodSecurity` in `SecurityConfig`)
- [ ] Task 2: `servico` schema (`V5`) + `Servico` entity + `ServicoRepository`
- [ ] Task 3: `profissional` + `profissional_servico` + `jornada` schema (`V6`, `V7`) + entities + repositories — depends on Task 2 (FK to `servico`)

### Checkpoint: Foundation
- [ ] `./mvnw test` passes (Testcontainers Postgres applies `V1`-`V7` cleanly)
- [ ] No endpoint exists yet — schema + cross-cutting infra only

### Phase 2: Servico CRUD
- [ ] Task 4: `GET /api/servicos` (public, `ativo = true` only, optional paging) — depends on Task 2
- [ ] Task 5: `POST`/`PUT`/`DELETE /api/servicos` (`ROLE_ADMIN`, soft delete) — depends on Task 1, Task 2

### Checkpoint: Servico
- [ ] Full CRUD cycle works via integration test; 401/403/200 verified for write endpoints
- [ ] Swagger shows `/api/servicos/*`

### Phase 3: Profissional CRUD + associations + jornada
- [ ] Task 6: `GET /api/profissionais` (public, response includes `servicoIds` + `jornada`, optional paging) — depends on Task 3
- [ ] Task 7: `POST`/`PUT`/`DELETE /api/profissionais` (`ROLE_ADMIN`, soft delete, atomic `servicoIds`+`jornada` replace on write) — depends on Task 1, Task 3

### Checkpoint: Profissional
- [ ] Full CRUD cycle works via integration test, including linking services and setting jornada
- [ ] Swagger shows `/api/profissionais/*`

### Phase 4: Seed + final checkpoint
- [ ] Task 8: Seed migration (`V8`): 3 profissionais, 5 serviços, links, Mon-Sat jornadas — depends on Task 2, Task 3

### Checkpoint: Module complete
- [ ] All 8 tasks checked off
- [ ] Every item in SPEC-catalog.md's Success Criteria is satisfied
- [ ] `docker compose up --build` still works end-to-end
- [ ] Human review before starting `scheduling`

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| `PUT /api/profissionais/{id}` silently orphans jornada rows if the replace logic is wrong | Med | Delete-then-reinsert inside one `@Transactional` method; cover with an integration test that updates jornada and asserts the old rows are gone, not just that new ones exist |
| `servicoIds` in a request referencing a nonexistent or inactive servico | Med | Validate referenced ids exist via `ServicoRepository.findAllById(...)` size check; 400 if any id is missing (not silently dropped) |
| Soft-delete-only means no path to truly remove bad demo data without a DB console | Low | Acceptable for this module's scope; `scheduling` will need these ids to stay valid anyway |
| Task 7 (Profissional write) grows beyond a clean M/L size once jornada + servico association logic is in | Med | If it gets unwieldy mid-implementation, split into "profissional basic CRUD" and "profissional associations" as two commits within the task rather than redesigning the API |

## Open Questions

None outstanding — the four questions raised in SPEC-catalog.md (pagination, soft delete, seed data content, "especialidades" meaning) were resolved during spec review and are captured as Architecture Decisions above.
