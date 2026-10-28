# Personal Engineering Platform

A bilingual (English/Turkish) portfolio website with a custom admin panel, developed as a real,
evolving software-engineering project. Besides presenting projects and technical writing, the
repository demonstrates production-minded backend engineering: explicit architecture decisions,
automated tests, security baselines and operational checks.

> **Status:** early foundation. The backend infrastructure baseline is in place. No business
> features, frontend or public API endpoints exist yet. See
> [Current Status and Roadmap](#current-status-and-roadmap).

## Project Overview

Goals:

- A public portfolio with projects, case studies and an engineering blog in English and Turkish.
- A private, custom-built admin panel for managing content, with no public registration.
- A documented engineering project with architecture decisions, tests and learning notes.
- Technologies are introduced only when a concrete use case justifies them.

## Architecture

### Current decisions (implemented)

- **Modular monolith.** A single deployable Spring Boot application. Business capabilities will
  live in separate packages with explicit boundaries; cross-cutting configuration lives in
  `shared`.
- **PostgreSQL with Flyway.** PostgreSQL is the source of truth and every schema change is a
  versioned Flyway migration. Hibernate only validates the schema (`ddl-auto: validate`); it never
  creates or alters tables.
- **Open Session in View disabled.** Data access stays inside the service and transaction layer.
- **Session-based security baseline.** Spring Security with CSRF protection and server-side
  sessions left enabled. Authentication is not implemented yet.
- **Environment-based configuration.** The base configuration contains no credentials;
  development defaults exist only in an explicit `local` profile.

### Planned (not implemented)

- Next.js and TypeScript frontend with Tailwind CSS.
- Business modules: `identity`, `profile`, `experience`, `skills`, `projects`, `blog`, `contact`.
- Cookie-based admin authentication.
- Containerized deployment behind a reverse proxy.
- Redis, Kafka and microservices are deliberately deferred until a measurable use case exists.

## Technology Stack

| Area | Technology |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.1.2: Spring MVC, Spring Data JPA, Spring Security, Validation, Actuator |
| Database | PostgreSQL 17 |
| Migrations | Flyway |
| Build | Maven 3.10.0 via the Maven Wrapper |
| Testing | JUnit 5, Spring Boot test support, MockMvc, AssertJ |
| Local infrastructure | Docker Compose |

Lombok is declared as a dependency but is not yet used in the code.

## Repository Structure

```text
.
├── docker-compose.yml          # Local PostgreSQL 17
├── .env.example                # Documented environment variables (no real secrets)
├── LICENSE
└── backend/                    # Spring Boot application
    ├── mvnw, mvnw.cmd          # Maven Wrapper
    ├── pom.xml
    └── src/
        ├── main/java/com/my/platform/
        │   ├── PlatformApplication.java
        │   └── shared/config/SecurityConfig.java
        ├── main/resources/
        │   ├── application.yaml          # Base configuration, no credential defaults
        │   ├── application-local.yaml    # Local development defaults
        │   └── db/migration/             # Flyway migrations
        └── test/java/com/my/platform/   # Automated tests
```

Directories for the frontend, infrastructure and documentation will be added with their first
content.

## Prerequisites

- JDK 21
- Docker with Docker Compose v2
- Internet access for the first build; the Maven Wrapper downloads Maven and all dependencies

A local Maven installation is not required.

The commands in this README were run on Windows. On Linux and macOS, use `./mvnw` instead of
`.\mvnw.cmd`. The wrapper script is committed as executable, but these platforms have not been
runtime-tested yet.

## Getting Started

```powershell
git clone https://github.com/Ahmettozcelik/personal-engineering-platform.git
cd personal-engineering-platform

# Start PostgreSQL and wait until the postgres service reports "healthy"
docker compose up -d
docker compose ps

# Run the backend; the "local" profile is activated automatically
cd backend
.\mvnw.cmd spring-boot:run
```

No server port is configured, so Spring Boot uses its default port, 8080, unless it is
overridden. The health endpoint is then available at `http://localhost:8080/actuator/health` and
reports status `UP` when the application and database are available. See
[Running PostgreSQL and Backend](#running-postgresql-and-backend) to use a different port.

## Configuration and Environment Variables

| Variable | Used by | Local default |
| --- | --- | --- |
| `DB_HOST` | Backend | `localhost` |
| `DB_PORT` | Backend; Docker Compose (host port) | `5432` |
| `DB_NAME` | Backend; Docker Compose | `platform` |
| `DB_USERNAME` | Backend; Docker Compose | `platform` |
| `DB_PASSWORD` | Backend; Docker Compose | `platform` |

The local defaults are intended for development only.

How configuration is resolved:

- **Docker Compose** reads an optional `.env` file in the repository root. To override values,
  copy `.env.example` to `.env` and edit it. `.env` is ignored by Git.
- **Spring Boot does not load the root `.env` file.** It reads operating-system environment
  variables and its own configuration files. When you override a value in `.env`, set the same
  variable in the backend's environment as well, for example in your shell or IDE run
  configuration.
- **`local` profile** (`application-local.yaml`) supplies the defaults above. It is activated by
  `spring-boot:run` (configured in `pom.xml`) and by the automated tests. Environment variables
  still take precedence over these defaults.
- **Without the `local` profile**, for example when running the packaged jar, all `DB_*`
  variables are required. If any are missing, startup fails instead of falling back to
  development credentials.

Other settings in `application.yaml`:

- Open Session in View is disabled; Hibernate uses `ddl-auto: validate`.
- The Hikari connection timeout is 5 seconds.
- Error responses never include stack traces, exception names, messages or binding errors.

## Running PostgreSQL and Backend

PostgreSQL runs as the `postgres` service:

- Image `postgres:17-alpine`.
- Data is stored in the named volume `postgres-data`.
- The port is bound to `127.0.0.1` only.
- A `pg_isready` health check runs every 5 seconds.

```powershell
docker compose up -d                  # start
docker compose ps                     # status and health
docker compose stop postgres          # stop; data is kept
docker compose start postgres         # start again
```

On startup, Flyway applies pending migrations from `backend/src/main/resources/db/migration` and
validates the ones already applied. To inspect the migration history with the default
credentials:

```powershell
docker compose exec -T postgres psql -U platform -d platform -c "SELECT version, description, success FROM flyway_schema_history;"
```

To run the backend on a different port, from `backend/`:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

## Running Tests

The automated tests start the full application context and connect to the Docker Compose
PostgreSQL database. Start the database before running them:

```powershell
docker compose up -d
cd backend
.\mvnw.cmd verify
```

| Test class | Verifies |
| --- | --- |
| `PlatformApplicationTests` | The application context starts against PostgreSQL. |
| `SecurityConfigTests` | Health is public; other endpoints return 401. |
| `ErrorResponseTests` | A `POST` request without a CSRF token returns 403, and the error body exposes no internal details. Runs on a real embedded server. |
| `HealthGroupsTests` | Readiness includes the database check; liveness does not. |

Running tests without a database dependency (Testcontainers) and continuous integration (GitHub
Actions) are planned, not implemented.

## Health and Readiness

| Endpoint | Includes | When the database is unavailable |
| --- | --- | --- |
| `GET /actuator/health` | All health indicators, including the database | `503`, status `DOWN` |
| `GET /actuator/health/readiness` | Readiness state and database | `503`, status `DOWN` |
| `GET /actuator/health/liveness` | Liveness state only | `200`, status `UP` |

- Responses contain only the overall status; component details are never shown.
- When the database is unreachable, the database check reports `DOWN` after about 5 seconds,
  which is the Hikari connection timeout.
- The application recovers without a restart when the database becomes available again.
- Use readiness to decide whether an instance should receive traffic, and liveness to decide
  whether it should be restarted.

## Security Baseline

- The health endpoints and `/error` are accessible without authentication.
- Every other path returns `401 Unauthorized`, because no login mechanism exists yet.
- CSRF protection is enabled at Spring Security defaults. Rejection of requests without a valid
  CSRF token (`403 Forbidden`) has been verified for `POST` requests only.
- Server-side sessions are left at Spring Security defaults, in preparation for cookie-based
  admin authentication.
- Actuator is configured to expose only the `health` endpoint over HTTP
  (`management.endpoints.web.exposure.include: health`).
- PostgreSQL is reachable only from the local machine.

## Current Status and Roadmap

### Implemented

- [x] Monorepo layout with the backend in `backend/`
- [x] Local PostgreSQL 17 via Docker Compose: localhost-only, persistent, health-checked
- [x] Environment-based configuration that fails fast without the `local` profile
- [x] Flyway baseline migration, Hibernate schema validation, Open Session in View disabled
- [x] Spring Security baseline with CSRF and sessions enabled, and safe error responses
- [x] Health, readiness and liveness probes
- [x] Automated tests for application startup, security, error responses and health groups

### Planned

- [ ] Testcontainers, so tests do not depend on a running Compose database
- [ ] GitHub Actions for backend build and tests
- [ ] Next.js frontend
- [ ] Architecture Decision Records
- [ ] Admin authentication and business modules
- [ ] Production deployment

## License

This project is licensed under the [MIT License](LICENSE).
