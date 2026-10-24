# Spec: catalog (AgendaFácil)

Part of [capability-map.md](capability-map.md). Depends on: `identity` (done — see [SPEC-identity.md](SPEC-identity.md)).

## Objective

Let an ADMIN manage the two things every booking is made of — Serviços (what can be booked) and Profissionais (who performs it, and which services they offer, and when they're available). CLIENTE and anonymous visitors can browse both lists to build a booking later (`scheduling` module). This is the second business-logic module and the first to actually use role-based authorization (`ROLE_ADMIN`) on top of `identity`'s JWT claims.

Done means: an ADMIN can create/update/deactivate services and professionals, link professionals to the services they perform, define each professional's weekly working hours (jornada), and anyone (no token required) can browse active services and professionals — all backed by real Postgres, with the seed data the original brief asks for (3 professionals, 5 services, Monday–Saturday jornadas).

## Tech Stack

Same as `identity` (Java 21, Spring Boot 4.0.9, Maven, Postgres, JWT via `identity`'s `SecurityConfig`). Nothing new at the library level — this module's job is to turn on `@EnableMethodSecurity` and use `@PreAuthorize("hasRole('ADMIN')")` on the write endpoints, which `identity`'s `JwtAuthenticationConverter` (role claim → `ROLE_*` authority) already makes possible but never exercised.

## Commands

Same as `identity`: `./mvnw clean verify`, `./mvnw test`, `docker compose up --build` from `agendafacil/`.

## Project Structure

New files under `backend/src/main/java/com/agendafacil/backend/`:

```
entity/      Servico, Profissional, Jornada (Profissional <-> Servico is a @ManyToMany via a join table, no dedicated entity needed)
dto/         ServicoRequest/Response, ProfissionalRequest/Response, JornadaRequest/Response, PageResponse<T>
repository/  ServicoRepository, ProfissionalRepository, JornadaRepository
service/     ServicoService, ProfissionalService (owns jornada + servico-association management)
controller/  ServicoController (GET public, POST/PUT/DELETE @PreAuthorize ADMIN), ProfissionalController (same pattern)
```

New Flyway migrations under `backend/src/main/resources/db/migration/`:
```
V5__create_servico.sql              -- servico(id, nome, descricao, duracao_min, preco, ativo)
V6__create_profissional.sql         -- profissional(id, nome, ativo) + profissional_servico(profissional_id, servico_id)
V7__create_jornada.sql              -- jornada(id, profissional_id, dia_semana, hora_inicio, hora_fim)
V8__seed_catalog.sql                -- 3 profissionais, 5 serviços, profissional_servico links, Mon-Sat jornadas
```

## Code Style

DTOs as records; `ProfissionalResponse` embeds the ids of services the professional performs, not full nested objects (avoids N+1 payload bloat):

```java
public record ServicoResponse(Long id, String nome, String descricao, int duracaoMin, BigDecimal preco, boolean ativo) {}

public record ProfissionalResponse(Long id, String nome, boolean ativo, List<Long> servicoIds, List<JornadaResponse> jornada) {}

public record JornadaResponse(DayOfWeek diaSemana, LocalTime horaInicio, LocalTime horaFim) {}
```

`dia_semana` maps to `java.time.DayOfWeek` (`@Enumerated(EnumType.STRING)`) — no reason to invent a parallel enum.

## Testing Strategy

Per `CLAUDE.md`, business rules need tests; plain CRUD is still tested at the integration level since it's the actual contract of the module (brief explicitly calls catalog's endpoints out in `Fase 3`'s prompt).

- **Integration tests** (`@SpringBootTest` + `MockMvc`, real Postgres via Testcontainers): CRUD happy paths for both Serviço and Profissional; write endpoints return 401 with no token and 403 with a CLIENTE token; public GET endpoints work with no token; linking/unlinking a professional to a service; jornada CRUD; seed data is present and queryable after migrations run.
- **Unit tests**: none planned beyond the integration suite — there's no non-trivial business logic yet (no overlap/conflict rules; that's `scheduling`'s job, not `catalog`'s).

## Boundaries

- **Always:** GET endpoints stay public (no auth) per the brief; every write endpoint (`POST`/`PUT`/`DELETE`) requires `ROLE_ADMIN` via `@PreAuthorize`; DTOs never expose JPA entities.
- **Ask first:** changing seed data content once this module ships (treat it like a Flyway migration — append-only, don't edit `V8` after merge); adding a new top-level entity beyond Servico/Profissional/Jornada.
- **Never:** let a CLIENTE-authenticated or anonymous request reach a write endpoint; hard-delete a Servico/Profissional that has bookings once `scheduling` exists (not a concern yet — no `agendamento` table exists in this module, but the `ativo` flag exists specifically so later modules never need a hard delete).

## Success Criteria

- [ ] `GET /api/servicos` and `GET /api/profissionais` work with no `Authorization` header, return only `ativo = true` by default
- [ ] `POST`/`PUT`/`DELETE` on both resources return 401 with no token, 403 with a valid CLIENTE token, succeed with an ADMIN token
- [ ] `DELETE` on either resource sets `ativo = false` rather than removing the row (see Open Question #2)
- [ ] A professional can be linked to one or more services (`profissional_servico`), and `ProfissionalResponse` reflects the link
- [ ] A professional's weekly jornada (day + start/end time) can be created/read through the API
- [ ] Seed data exists after migrations run: 3 profissionais, 5 serviços, each professional linked to at least one service, Monday–Saturday jornadas
- [ ] Integration tests cover every flow above against a real Testcontainers Postgres; `./mvnw test` passes
- [ ] Swagger UI shows the new `/api/servicos/*` and `/api/profissionais/*` operations

## Open Questions

1. **Pagination.** The brief mentions "paginação, filtros e ordenação nas listagens" generally (most concretely for `scheduling`'s admin bookings list in `Fase 5`). Proposing `catalog`'s list endpoints support optional `page`/`size`/`sort` query params via Spring Data `Pageable`, wrapped in a small `PageResponse<T>` DTO, but default to returning everything if no paging params are given (so the simple case — "show me all active services" — stays a one-liner for `client-web`). Confirm, or say if paging should be out of scope for catalog and deferred entirely to `scheduling`.
2. **Soft delete.** Proposing `DELETE` endpoints set `ativo = false` instead of removing the row, since `scheduling` will later reference these ids from `agendamento` rows and a hard delete would orphan history. Confirm.
3. **Seed data content.** Proposing generic salon-style demo data (e.g. Corte de Cabelo, Manicure, Depilação, Massagem, Limpeza de Pele; 3 professionals each linked to a subset of services; Mon–Sat 09:00–18:00 jornada for all). Confirm this is fine as placeholder/demo data, or provide specific names you'd rather see in the portfolio demo.
4. **"Especialidades".** The brief's `Profissionais` field list says "nome, especialidades, dias e horários de trabalho." Proposing "especialidades" **is** the `profissional_servico` link (the set of services a professional performs) rather than a separate free-text or tag field. Confirm, or say if you want a distinct `especialidade` concept (e.g. "Cabeleireiro", "Esteticista") independent from the services table.
