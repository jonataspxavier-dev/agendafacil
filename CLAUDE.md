# CLAUDE.md — AgendaFácil

Conventions for this repo. Read [PLANO.md](PLANO.md) for the product brief and [capability-map.md](capability-map.md) for the module build order. Each module has its own `SPEC-<module>.md` and `tasks/<module>/{plan,todo}.md`.

## Backend (Java 21, Spring Boot 4.0.9, Maven)

> Deviation from the original brief: the brief named Spring Boot 3.x, but by the time `platform-setup` was implemented, Spring Initializr no longer offered any 3.x version (3.x had aged out; only 4.0.x/4.1.x/4.2.x-RC remained). Confirmed with the user — target is Spring Boot 4.0.9, which still runs on Java 21.

- **Layering:** `controller/` → `service/` → `repository/`. Controllers depend on services, services depend on repositories. Never skip a layer.
- **DTOs are records, never entities over the wire.** A controller method never returns or accepts a `@Entity` directly — always a `record` DTO in `dto/`.
  ```java
  public record ServicoResponse(Long id, String nome, int duracaoMin, BigDecimal preco) {}
  ```
- **`security/`** holds JWT filters, `SecurityConfig`, and role checks. **`exception/`** holds the global `@RestControllerAdvice` and a standard error-response shape — every error returns the same JSON structure.
- **Tests are mandatory for business rules** (availability calculation, booking conflicts, cancellation window, auth flows). Infrastructure-only code (config classes, DTO records) doesn't need its own test.
- **Flyway migrations:** `src/main/resources/db/migration/V<n>__description.sql`, strictly incrementing, never edited once merged. `V1__baseline.sql` is intentionally empty — the first real schema migration belongs to the `identity`/`catalog` modules.
- **Jackson 3, not classic Jackson 2.** Spring Boot 4's default JSON integration uses `tools.jackson.databind.ObjectMapper` (package `tools.jackson.*`), not `com.fasterxml.jackson.databind.ObjectMapper`. A `com.fasterxml...` `ObjectMapper` is on the classpath transitively (via springdoc) but there's no bean for it — `@Autowired ObjectMapper` in a test must import the `tools.jackson` one or it fails with `NoSuchBeanDefinitionException`.
- **`SecurityConfig` is stateless by default:** CSRF is disabled and session creation policy is `STATELESS`, since this is a token-based JSON API with no server-side sessions. Don't re-enable either without a specific reason.
- **`@SpringBootTest` + `MockMvc` test classes with multiple `@Test` methods that assert a row count need `@Transactional`.** Unlike `@DataJpaTest`, `@SpringBootTest` doesn't roll back between test methods by default — data a `@BeforeEach` inserts in one test is still there for the next, so count-based assertions (`"length()" == 2`) flake once there's more than one test method. Add `@Transactional` to the test class so each method rolls back. Tests that assert by unique value (e.g. a distinct email per test) don't need this, but any test that counts rows does.
- **`@DataJpaTest` on an association (`@ManyToMany`/`@OneToMany`) needs a forced round-trip.** `repository.save(entity); repository.findById(id)` in the same test often returns the same in-memory object graph from the persistence context — the association looks populated even if nothing was ever written to the join table. Call `TestEntityManager.flush()` then `.clear()` between save and findById (autowire `TestEntityManager` from `org.springframework.boot.jpa.test.autoconfigure` — note the package differs from `DataJpaTest`'s own `org.springframework.boot.data.jpa.test.autoconfigure`) to force a real query, and check the Hibernate SQL log actually shows the join-table `INSERT`/`SELECT`.
- **Never assert an exact row count on a table a seed migration writes to** (`usuario`, `servico`, `profissional`, `jornada`, …). `identity`'s `V4` and `catalog`'s `V8` permanently add rows to every test database, so `jsonPath("$.length()").value(2)`-style assertions break the moment a later module seeds more data — as happened when `catalog`'s seed landed and broke three already-passing list tests. Instead: give test fixtures a distinctive name (e.g. `"Corte Teste Lista"`) and assert presence/absence by that name, or capture a baseline count before inserting fixtures and assert `baseline + N`. The one legitimate exception is a seed module's own test (e.g. `CatalogSeedTest`) asserting its own migration's exact output, since that test's Testcontainers Postgres instance is otherwise untouched.

## Frontend (React + Vite + TypeScript)

- **Structure:** `pages/`, `components/`, `services/` (API clients), `hooks/`, `contexts/`.
- Functional components, named exports, Tailwind utility classes, `@/` absolute imports.
- API calls go through `services/`, never a raw `fetch`/`axios` call inside a component.

## Cross-cutting

- **Small commits.** One task from a module's `tasks/<module>/todo.md` per commit where practical; don't bundle unrelated tasks.
- **Docker Compose service names, not `localhost`.** Backend connects to Postgres via the `postgres` service name; frontend's API base URL points at the `backend` service name (or an env var defaulting to it).
- **No business logic in `platform-setup`.** Scaffold, config, and tooling only — entities, endpoints, and UI screens belong to the modules that depend on it.
