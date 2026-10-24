# AgendaFácil

Full-stack scheduling system for salons and clinics — admin panel and customer-facing booking. See [PLANO.md](PLANO.md) for the full product brief and [capability-map.md](capability-map.md) for the module build order.

## Stack

- Backend: Java 21, Spring Boot 4.0.9 (Web, Data JPA, Security, Validation, Flyway, PostgreSQL), springdoc-openapi
- Frontend: React 19, Vite, TypeScript, Tailwind CSS, React Router, Axios, React Hook Form, Zod
- Infra: Docker Compose (Postgres + backend + frontend), JUnit 5 + Mockito + Testcontainers, Vitest + React Testing Library

## Running locally

```bash
docker compose up --build
```

- Backend: http://localhost:8080 (health: `/actuator/health`, docs: `/swagger-ui.html`)
- Frontend: http://localhost:5173

## Development conventions

See [CLAUDE.md](CLAUDE.md) for layering, DTO, testing, and Flyway conventions.
