# LinkedUFMA Backend Guide

## Project Overview

LinkedUFMA is the backend for a platform that publishes and manages academic and professional opportunities for UFMA students.

The platform has three user roles:

- Student
- Recruiter: companies, professors, academic leagues, university departments, and other authorized publishers
- Administrator

Core capabilities include authentication, role-based authorization, profiles, opportunity management, applications, application status tracking, moderation, and administration.

The platform tracks the current status of an application. It does not conduct interviews, tests, or assessments.

Out of scope for the MVP:

- Institutional internship management
- University agreement management
- Academic system integrations
- Semantic search
- Advanced recommendation mechanisms

## Technology Stack

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA / Hibernate
- Spring Security with JWT
- Jakarta Validation
- PostgreSQL
- Liquibase
- Redis when caching is needed
- Docker and Docker Compose
- MapStruct
- Lombok, used conservatively
- springdoc-openapi / Swagger
- JUnit 5, Mockito, and Spring Boot Test

## Architecture

Use a modular layered architecture with lightweight DDD concepts.

This project is not Clean Architecture and is not Hexagonal Architecture. Do not introduce ports, adapters, duplicated persistence entities, use-case classes, or unnecessary abstractions unless explicitly requested.

Organize code by business module first, then by technical layer inside each module.

```text
Controller -> Service -> Repository -> Database
```

- Controllers handle HTTP concerns only.
- Services orchestrate use cases, authorization, transactions, and collaboration between repositories.
- Repositories handle persistence only.
- Entities hold state and business rules related to their own lifecycle.

Examples:

- `Opportunity.close()` belongs to the entity because it changes the entity's own state.
- Checking for a duplicate active application belongs to the service because it requires repository access.
- Ownership and authorization checks belong to the service layer.

## Project Organization

Organize the application by business module first and by technical layer inside each module. The main modules are authentication, users, profiles, opportunities, applications, administration, and notifications.

Each module may contain its own controller, service, repository, mapper, DTO, and entity code. Keep code related to one business concept together instead of creating global packages containing every controller, service, or repository in the application.

Shared technical concerns belong in dedicated shared areas:

- Security configuration, JWT support, and authentication filters belong to the security area.
- Framework configuration belongs to the configuration area.
- Global exception handling and reusable application exceptions belong to the exception area.
- Database migrations belong in the standard Liquibase changelog resource location.
- Environment-specific configuration belongs in `application-<profile>.properties` files.
- Tests should mirror the application modules under `src/test/java`.

Within each module, preserve the layered dependency direction:

```text
Controller -> Service -> Repository -> Database
```

Use a specification component only when a module needs dynamic or combinable query filters. Do not create empty layers or folders in advance.

## Coding Conventions

- Use English for package names, classes, methods, variables, enum values, database objects, API paths, migration names, comments, exceptions, and documentation.
- Use constructor injection only. Do not use field injection with `@Autowired`.
- Services are normally concrete classes. Do not create `Service` and `ServiceImpl` pairs by default.
- Spring Data JPA repositories must be interfaces extending `JpaRepository`.
- Create custom repository interfaces only when a dependency is intentionally interchangeable, such as external APIs, file storage, or a persistence strategy likely to change.
- Use MapStruct interfaces for mappers.
- Use `record` for request and response DTOs whenever appropriate.
- Do not expose JPA entities through controllers.
- Do not use Lombok `@Data` on JPA entities.
- For JPA entities, prefer narrowly selected annotations such as `@Getter` and `@NoArgsConstructor`. Use setters and builders only when justified.
- Avoid generic names such as `BaseService`, `GenericRepository`, `DataManager`, and `Utils`.
- Use domain language consistently: `Opportunity`, `Application`, `Recruiter`, `Student`, and `ApplicationStatus`.
- Use UUID identifiers unless an explicit project decision requires another type.
- Use `@Transactional` for service methods that modify persistent state.

## Rich Domain Model

Use a moderate rich domain model. Entities should not be mere data containers, but they must not coordinate repositories or external services.

Keep state and lifecycle behavior in entities:

```text
opportunity.publish();
opportunity.close();
opportunity.canReceiveApplications();

application.updateStatus(ApplicationStatus.APPROVED);
application.cancel();
```

Keep orchestration in services:

- Load entities from repositories
- Validate permissions and ownership
- Check cross-entity rules, such as duplicate active applications
- Manage transactions
- Persist new entities
- Trigger notifications or other integrations

When an entity is loaded through a repository inside a transactional service method, JPA dirty checking persists changes at transaction commit. Use `repository.save()` for new entities.

## API and Validation Rules

- Controllers are responsible for request parsing, validation, status codes, and response mapping.
- Validate request DTOs with Jakarta Validation annotations.
- Use a global exception handler for consistent API errors.
- Use meaningful exceptions such as `ResourceNotFoundException` and `BusinessException`.
- Protect restricted endpoints through Spring Security and validate ownership and authorization in the service layer.
- Keep Swagger/OpenAPI documentation accurate because it is the frontend API contract.

## Domain Rules

- A student cannot have more than one active application for the same opportunity.
- Applications are allowed only while the opportunity is open and inside its registration period.
- Closing an opportunity prevents new applications but preserves its history.
- Only authorized recruiters can edit their opportunities or manage their candidates.
- Administrators can moderate opportunities and manage users.
- Student skills and tools are free-text fields. Do not introduce a fixed skills catalog unless explicitly requested.
- Personal data access must respect role, permission, and purpose.

### Application Status

Applications use a fixed `ApplicationStatus` enum. The MVP does not support recruiter-defined selection stages per opportunity.

Suggested values:

```text
SUBMITTED,
UNDER_REVIEW,
APPROVED,
REJECTED
```

- Recruiters update the current application status.
- Students can view the current status.
- Terminal statuses must not change unless an explicit business rule is added.
- Do not create a `SelectionStage` or `ApplicationStage` entity.
- Do not create status history unless explicitly requested.

## Persistence and Migrations

- PostgreSQL is the relational database.
- Liquibase is the only mechanism allowed to change the schema.
- Configure Hibernate with `spring.jpa.hibernate.ddl-auto=validate`.
- Never rely on Hibernate to create or update schemas.
- Every schema change requires a reviewed, versioned Liquibase changeset.
- Never modify an already applied changeset.
- Generate draft changesets with `liquibase:diffChangeLog`, review them, then version them under `src/main/resources/db/changelog/changes`.
- Add foreign keys, indexes, unique constraints, and timestamps required by the domain.
- The same changesets must work against local PostgreSQL containers and shared Supabase PostgreSQL databases.

## Environment Configuration

Use Spring profiles:

- `local`: local PostgreSQL running through Docker Compose
- `cloud`: shared Supabase PostgreSQL database
- `prod`: future deployed environment

Keep configuration keys in `application-*.properties`. Provide sensitive values through environment variables.

Never commit secrets. Maintain an `.env.example` file with empty placeholders:

```text
DATABASE_URL=
DATABASE_USERNAME=
DATABASE_PASSWORD=
JWT_SECRET=
```

## Testing

- Unit-test services with JUnit 5 and Mockito.
- Mock repositories and external dependencies in unit tests.
- Services do not need interfaces merely to be testable; Mockito can mock concrete classes.
- Add integration tests for critical persistence and security flows when practical.
- Cover authentication, authorization, opportunity creation, duplicate application prevention, registration-period validation, and application status updates.

## Delivery Priorities

The backend is developed before the frontend.

Sprint 1 includes:

- Backend project setup
- Docker Compose, PostgreSQL, Redis, and environment configuration
- Initial entities and Liquibase schema migration
- Authentication, JWT, and role-based authorization
- Backend APIs, business rules, validation, Swagger documentation, and essential tests

The frontend will consume the completed API later. Keep API contracts stable and Swagger documentation current.
