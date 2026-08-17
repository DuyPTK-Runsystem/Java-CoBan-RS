---
name: entity-impl
description: Create, update, or review a Spring Data JPA entity and relational table mapping, including identifiers, columns, constraints, indexes, enums, relationships, auditing, and migrations. Use when implementing an Entity.java file, designing a database table, mapping SQL schema to JPA, or checking entity correctness for a DoAn1_BhpWebsite-style Spring Boot service.
---

# Implement a JPA entity/table

## Procedure

1. Inspect the database migration/schema, neighboring entities, Long ID generator, base auditable entity, Hibernate naming strategy, and database version.
2. Define table name, identifier strategy, columns, nullability, lengths, numeric precision/scale, defaults, unique constraints, indexes, foreign keys, and delete behavior before coding.
3. Create the entity mapping and a matching Flyway/Liquibase migration when migrations are used. Do not rely on `ddl-auto=update` for production evolution.
4. Model relationships only when navigation is required. Prefer IDs for cross-service references; never create JPA relationships across databases/services.
5. Verify schema mapping with a focused JPA test or migration validation.

## Mapping rules

- Use `@Entity` and explicit `@Table(name = ...)`.
- Match `@Column` constraints to the database; Java validation alone does not protect the table.
- Use `@Enumerated(EnumType.STRING)` with sufficient length.
- Use `BigDecimal` with explicit precision/scale for scores and money; never `double` for exact values.
- Keep collections lazy and exclude bidirectional relationships from generated `toString`, `equals`, and `hashCode`.
- Avoid Lombok `@Data` on entities; prefer `@Getter`, `@Setter`, and controlled identity equality.
- Do not serialize entities as API responses.
- Add indexes from demonstrated query/filter/join patterns, not every column.
- Use database-enforced unique constraints for true uniqueness; service checks alone race.
- Use `Long id` for entity identifiers by default. Do not introduce UUID identifiers unless the user explicitly requests a migration or the existing table already requires UUID.
- Put auditing in the established auditable base class/listener when available.

Read [entity-template.md](references/entity-template.md) when generating code.
