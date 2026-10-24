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
- **`@DataJpaTest` on an association (`@ManyToMany`/`@OneToMany`) needs a forced round-trip.** `repository.save(entity); repository.findById(id)` in the same test often returns the same in-memory object graph from the persistence context — the association looks populated even if nothing was ever written to the join table. Call `TestEntityManager.flush()` then `.clear()` between save and findById (autowire `TestEntityManager` from `org.springframework.boot.jpa.test.autoconfigure` — note the package differs from `DataJpaTest`'s own `org.springframework.boot.data.jpa.test.autoconfigure`) to force a real query, and check the Hibernate SQL log actually shows the join-table `INSERT`/`SELECT`.

## Frontend (React + Vite + TypeScript)

- **Structure:** `pages/`, `components/`, `services/` (API clients), `hooks/`, `contexts/`.
- Functional components, named exports, Tailwind utility classes, `@/` absolute imports.
- API calls go through `services/`, never a raw `fetch`/`axios` call inside a component.

## Cross-cutting

- **Small commits.** One task from a module's `tasks/<module>/todo.md` per commit where practical; don't bundle unrelated tasks.
- **Docker Compose service names, not `localhost`.** Backend connects to Postgres via the `postgres` service name; frontend's API base URL points at the `backend` service name (or an env var defaulting to it).
- **No business logic in `platform-setup`.** Scaffold, config, and tooling only — entities, endpoints, and UI screens belong to the modules that depend on it.
