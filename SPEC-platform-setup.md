# Spec: platform-setup (AgendaFácil)

Part of [capability-map.md](capability-map.md). Depends on: — (first module, build order position 1).

## Objective

Scaffold the AgendaFácil repository, local dev environment, and conventions so every later module (`identity`, `catalog`, `scheduling`, `reporting`, `client-web`, `admin-web`) has a running backend, running frontend, and a database to build against from day one.

This module has no end-user — the "user" is the developer. Done means: a fresh clone can run the whole stack with one command and knows the rules the rest of the project follows.

## Tech Stack

- Backend: Java 21, Spring Boot 4.0.9 (brief named 3.x, but Initializr had dropped the whole 3.x line by implementation time — confirmed with user, see Open Questions) — Web, Data JPA, Security, Validation, Flyway, PostgreSQL driver
- API docs: springdoc-openapi (Swagger UI)
- Build: Maven (brief doesn't specify Maven vs Gradle — defaulting to Maven; see Open Questions)
- Frontend: React + Vite + TypeScript
- Frontend libs installed but unused until later modules: React Router, Axios, React Hook Form, Zod, Tailwind CSS
- Infra: Docker, Docker Compose (postgres + backend + frontend services)
- Test runners configured, no test cases yet: JUnit 5 + Mockito + Testcontainers (backend), Vitest + React Testing Library (frontend)

## Commands

```
# backend (from backend/)
./mvnw clean verify
./mvnw spring-boot:run

# frontend (from frontend/)
npm install
npm run dev
npm run build
npm run lint

# whole stack (from agendafacil/)
docker compose up --build
docker compose down -v
```

## Project Structure

```
agendafacil/
├── backend/
│   └── src/main/java/.../agendafacil/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── dto/
│       ├── entity/
│       ├── security/
│       └── exception/
├── frontend/
│   └── src/
│       ├── pages/
│       ├── components/
│       ├── services/
│       ├── hooks/
│       └── contexts/
├── capability-map.md
├── SPEC-*.md            # one per module, this file's siblings
├── docker-compose.yml   # postgres + backend + frontend
├── CLAUDE.md
├── README.md
└── PLANO.md             # original brief, copied in verbatim
```

## Code Style

Backend — DTOs are records, never return entities from a controller:

```java
public record ServicoResponse(Long id, String nome, int duracaoMin, BigDecimal preco) {}

@GetMapping("/api/servicos")
public List<ServicoResponse> listar() { ... }
```

Frontend — functional components, named exports, Tailwind utility classes, `@/` absolute imports:

```tsx
export function ServicoCard({ servico }: { servico: Servico }) {
  return <div className="rounded-lg border p-4">{servico.nome}</div>;
}
```

## Testing Strategy

No business logic exists yet in this module, so there are no test *cases* to write here. What this module owns is making sure the test *infrastructure* works before `identity` needs it:

- Backend: `./mvnw test` runs on a project with JUnit 5, Mockito, and Testcontainers on the classpath and a working Testcontainers Postgres config, even with zero `@Test` methods.
- Frontend: `npm test` runs Vitest + RTL with zero spec files, without erroring.
- Verification for this module is operational, not behavioral: containers start and health-check.

## Boundaries

- **Always:** keep backend and frontend decoupled (REST only, no shared code); treat `docker-compose.yml` as the single source of truth for local dev; commit in small scaffolding steps (deps, then config, then health check); never expose a JPA `@Entity` directly from a controller, even as a placeholder.
- **Ask first:** Maven vs Gradle if the user disagrees with the Maven default; any deviation from the Java 21 / Spring Boot 3.x / React+Vite+TS stack named in the brief; adding any dependency not already listed in the brief or this spec.
- **Never:** commit `.env` files or real credentials; use the docker-compose Postgres password for anything beyond local dev; mark this module done without the health-check verification below passing.

## Success Criteria

- [ ] `docker compose up --build` from `agendafacil/` starts postgres, backend, and frontend with zero manual steps
- [ ] Backend health endpoint (Spring Actuator `/actuator/health`) returns 200
- [ ] Frontend dev server responds 200 on its port
- [ ] Swagger UI is reachable (empty — no endpoints yet, that's fine)
- [ ] `CLAUDE.md` exists at repo root and documents: layered architecture, DTOs-as-records rule, never-expose-entities rule, mandatory tests for business rules, small-commit convention
- [ ] `./mvnw test` and `npm test` both run clean with zero test cases
- [ ] `README.md` stub exists with stack list and the one-command run instruction
- [ ] Git repository initialized at `agendafacil/` with an initial commit containing this scaffold

## Open Questions

1. **Maven or Gradle?** Brief doesn't specify. Defaulting to Maven (more common default for Spring Boot tutorials/portfolios) — flag if you want Gradle instead.
2. **Git remote now or later?** This module only requires a local `git init` + initial commit. Creating a GitHub remote is left for the `Fase 6 — Entrega` deploy step from the original brief, not this module.
3. **Swagger wired now with nothing to show?** Yes — the dependency and base config go in now; there's nothing to document until `identity` adds the first real endpoints.
4. **Resolved during Task 2:** Spring Boot 3.x is no longer offered by Spring Initializr (only 4.0.x/4.1.x/4.2.x-RC remain). User chose **Spring Boot 4.0.9** over 4.1.2 or hand-pinning an EOL 3.5.x. Still targets Java 21.
