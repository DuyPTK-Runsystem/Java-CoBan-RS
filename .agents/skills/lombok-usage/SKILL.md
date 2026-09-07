---
name: lombok-usage
description: Guide Lombok annotation usage in Spring Boot Java code. Use when creating, updating, or reviewing entities, DTO classes, services, controllers, configuration/components, tests, or backend skills that may use Lombok annotations such as @Data, @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor, @RequiredArgsConstructor, and @Builder.
---

# Lombok Usage

Use Lombok to remove routine Java boilerplate while preserving Spring, JPA, and API contracts.

## Layer Rules

### JPA entities

- Prefer `@Getter`, `@Setter`, and `@NoArgsConstructor`.
- Avoid `@Data` on entities because it generates `toString`, `equals`, and `hashCode` that can be unsafe with JPA identity, lazy relationships, and bidirectional associations.
- Avoid `@AllArgsConstructor` unless there is a clear non-JPA construction use.
- Use `@Builder` only for explicit factory/test-fixture needs and keep a JPA-compatible no-arg constructor.
- Exclude relationships from generated `toString`, `equals`, and `hashCode` if those methods are intentionally added.

### Services, controllers, and components

- Prefer `@RequiredArgsConstructor` with `private final` dependencies.
- Remove manual constructors after adding `@RequiredArgsConstructor`.
- Do not use `@Data` on Spring beans.
- Do not make injected dependencies non-final unless the project pattern requires optional/setter injection.

### DTOs and simple data classes

- Keep Java records as records; do not convert a record to a class just to use Lombok.
- For mutable DTO classes, use `@Data` when getters, setters, `toString`, `equals`, and `hashCode` are all acceptable.
- Use `@NoArgsConstructor` and `@AllArgsConstructor` for framework serialization/deserialization needs.
- Use `@Builder` when it improves construction readability in production or tests.
- Avoid `@Data` on DTOs containing passwords, tokens, large payloads, or values that should not appear in logs; prefer explicit accessors or avoid generated `toString`.

### Tests

- Use `@Builder` or Lombok constructors for test fixtures when it reduces setup noise.
- Do not introduce Lombok in tests when plain record constructors or helper methods are clearer.

## General Rules

- Use the correct annotation name `@RequiredArgsConstructor`, not `@RequireArgsConstructor`.
- Add only the Lombok annotations needed for the class.
- Do not combine constructor annotations that generate duplicate constructors.
- Remove unused imports and manual methods replaced by Lombok.
- Keep explicit methods when they encode business behavior, validation, security, masking, or custom equality.
- Check that Lombok dependencies and annotation processors exist before relying on Lombok in a project.
- Run compile/tests appropriate to the changed Java code; skill-only documentation changes do not require backend validation.
