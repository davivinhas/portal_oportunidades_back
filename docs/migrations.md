# Database migrations

Liquibase is the only mechanism that changes the database schema. Hibernate runs with `spring.jpa.hibernate.ddl-auto=validate` and never creates or updates database objects.

## Prerequisites

Export the Supabase connection variables in the shell. Do not commit their values.

```bash
export SUPABASE_DB_URL='jdbc:postgresql://HOST:5432/postgres?sslmode=require'
export SUPABASE_DB_USERNAME='DATABASE_USER'
export SUPABASE_DB_PASSWORD='DATABASE_PASSWORD'
```

## Inspect and validate

```bash
bash ./mvnw liquibase:status
bash ./mvnw liquibase:validate
```

## Generate a draft from JPA entities

After changing entities, compile the project and generate a timestamped YAML changelog. Review the generated file before adding it to Git.

```bash
bash ./mvnw compile liquibase:diffChangeLog \
  -Dliquibase.diffChangeLogFile=src/main/resources/db/changelog/changes/YYYYMMDD_description.yaml
```

The generated file is a draft. Review and manually correct PostgreSQL enums, partial indexes, SQL defaults, schema creation, renames, and destructive changes when needed.

## Preview and apply

```bash
bash ./mvnw liquibase:updateSQL
bash ./mvnw liquibase:update
```

The Spring Boot application also applies reviewed pending changesets on startup. Liquibase records applied changesets in `DATABASECHANGELOG`.

## Rules

- Commit every reviewed changeset with the entity changes that require it.
- Never modify a changeset already applied to a shared database; create a new one instead.
- Never use `liquibase:clean` against shared or Supabase databases.
