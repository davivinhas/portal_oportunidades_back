# Database migrations

Liquibase is the only mechanism that changes the database schema. Hibernate uses
`spring.jpa.hibernate.ddl-auto=validate` and never creates or updates database objects.

## Profiles

- `local`: local PostgreSQL from `compose.yaml`; Liquibase is enabled.
- `integration-test`: disposable PostgreSQL Testcontainer; Liquibase is enabled.
- `cloud`: shared Supabase database; Liquibase is disabled during application startup.

Start the local infrastructure with:

```bash
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Run unit tests independently of infrastructure:

```bash
./mvnw test
```

Run the database integration test in an isolated PostgreSQL 17 container:

```bash
./mvnw verify -Pintegration-tests
```

The integration test applies every changeset to a new database and verifies JPA persistence and
native PostgreSQL enums. The container is discarded after the test, so Supabase data is not read or
changed.

## Shared database procedure

Schema changes in Supabase are deliberate operations. First inspect the SQL generated from the
reviewed changelog, then apply it only after team approval:

```bash
./mvnw liquibase:updateSQL \
  -Dliquibase.url="$SUPABASE_DB_URL" \
  -Dliquibase.username="$SUPABASE_DB_USERNAME" \
  -Dliquibase.password="$SUPABASE_DB_PASSWORD"

./mvnw liquibase:update \
  -Dliquibase.url="$SUPABASE_DB_URL" \
  -Dliquibase.username="$SUPABASE_DB_USERNAME" \
  -Dliquibase.password="$SUPABASE_DB_PASSWORD"
```

Never use `liquibase:clean` against shared or Supabase databases. Never change the identity of an
already applied changeset; add a new versioned changeset instead.
