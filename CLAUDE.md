# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BudgetFlow is a SaaS web application for quote/budget generation. It's a Java 21 Spring Boot backend (monolithic modular architecture) with PostgreSQL, using Liquibase for migrations and Spring Security for authentication.

## Common Commands

### Running the Application

```bash
# Run with development profile (default)
./gradlew bootRun

# Run with specific profile
./gradlew bootRun --args='--spring.profiles.active=prd'
```

### Testing

```bash
# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests com.lwv.budgetflow.SomeTest

# Run a specific test method
./gradlew test --tests com.lwv.budgetflow.SomeTest.someMethod
```

### Building

```bash
# Build a JAR (outputs to build/libs/)
./gradlew build

# Build without running tests
./gradlew build -x test

# Clean build
./gradlew clean build
```

### Dependencies

```bash
# Check for dependency vulnerabilities
./gradlew dependencyCheck

# Show dependency tree
./gradlew dependencies
```

## Architecture & Modules

The application uses a modular monolithic architecture where each domain is split into its own package with clear layer separation: `domain`, `repository`, `service`, and `web`.

### Core Modules

**domain/** — Core entities and infrastructure
- `entity/` — JPA entities (User, Organization, RefreshToken)
- `enums/` — Enums (UserStatus, OrganizationStatus, PlanType)
- `converter/` — JPA converters for enums
- `repository/` — Spring Data JPA repositories
- `service/` — Core business logic (AuthService)

**security/** — Authentication & authorization
- `config/` — Spring Security configuration and CORS properties
- `jwt/` — JWT token generation/validation (JwtService, JwtProperties) with access token (15m) and refresh token (7d) TTL
- `userdetails/` — Custom UserDetailsService for Spring Security

**ledger/** — Personal budget/ledger management
- `domain/` — Ledger-specific entities
- `service/` — EntryService (budget entries), EntryScheduleService (scheduled entries)
- `repository/` — Ledger repositories
- `web/` — REST controllers

**taxonomy/** — Budget category taxonomy per user
- `domain/` — Category entities
- `service/` — TaxonomyProvisioningService (auto-provisions default categories on user registration)
- `repository/` — Category repositories
- `web/` — CategoryController

**accounts/** — Payment methods & account lookups
- `domain/` — Account, PaymentMethod entities
- `repository/` — Repositories
- `web/` — LookupController (returns available payment methods, etc.)

**web/** — Global authentication endpoints
- `controller/` — AuthController (register, login, logout), UserController
- `dto/` — Request/response DTOs
- `exception/` — GlobalExceptionHandler, custom exceptions

**shared/** — Cross-cutting utilities
- `web/` — Shared HTTP utilities

### Key Flows

- **User Registration** → User created → TaxonomyProvisioningService auto-provisions default categories
- **Authentication** → JWT access token (15m) + refresh token (7d, httpOnly cookie)
- **Token Refresh** → Refresh endpoint using refresh token from secure cookie

## Database & Migrations

- **Engine:** PostgreSQL (environment: `URL_DB`, `USERNAME_DB`, `PASSWORD_DB`)
- **Migrations:** Liquibase (`src/main/resources/db/changelog/`)
- **Liquibase Master:** `db.changelog-master.yaml` (includes all changes from `changes/` subdirectory)
- **JPA Config:** `ddl-auto: none` — migrations are manual only (via Liquibase)

When modifying the schema:
1. Create a new numbered YAML file in `src/main/resources/db/changelog/changes/`
2. Reference it in `db.changelog-master.yaml`
3. Liquibase applies on next `bootRun` (or `gradle liquibaseUpdate`)

## Configuration & Environment

Profiles in `src/main/resources/`:
- `application.yaml` — Base config (profiles, datasource, JPA, JWT, CORS)
- `application-dev.yaml` — Development overrides
- `application-hml.yaml` — Staging overrides
- `application-prd.yaml` — Production overrides

Key environment variables:
- `SPRING_PROFILES_ACTIVE` — Active profile (default: `dev`)
- `URL_DB`, `USERNAME_DB`, `PASSWORD_DB` — Database credentials
- `JWT_SECRET` — Secret key for signing JWTs (required)
- `CORS_ALLOWED_ORIGINS` — CORS allowed origins (default: `http://localhost:5173`)

JWT configuration:
- Access token TTL: 15 minutes
- Refresh token TTL: 7 days
- Refresh token stored in httpOnly cookie (`refresh_token`)
- Cookie secure flag enabled in non-dev profiles

## Taxonomy Data

Taxonomy categories are defined in `src/main/resources/taxonomy/` (JSON files loaded into memory). When a user registers, TaxonomyProvisioningService creates their personal category hierarchy from these templates.

## Testing

- Test location: `src/test/java/com/lwv/budgetflow/`
- Testing dependencies: Spring Boot Test (data-jpa, security, webmvc, actuator starters)
- Test runner: JUnit Platform

## Key Technologies

- **Framework:** Spring Boot 4.1.0
- **ORM:** Spring Data JPA (Hibernate)
- **Migrations:** Liquibase
- **Auth:** Spring Security + JWT (jjwt 0.12.7)
- **Build:** Gradle (Java 21 toolchain)
- **Database:** PostgreSQL
- **Validation:** Spring Validation
- **Utilities:** Lombok
