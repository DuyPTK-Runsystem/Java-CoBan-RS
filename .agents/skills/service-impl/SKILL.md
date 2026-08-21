---
name: service-impl
description: Create, update, or review Spring Boot application service files with business rules, transaction boundaries, repository orchestration, DTO mapping, pagination, and domain errors. Use when implementing an EntityService.java, CRUD use cases, or moving logic out of controllers in a DoAn1_BhpWebsite-style backend.
---

# Implement a Spring service

## Procedure

1. Inspect neighboring services, repositories, exceptions, DTOs, auditing, and security context utilities.
2. State invariants and authorization rules for each use case before implementing it.
3. Inject repositories and collaborating services with `@RequiredArgsConstructor` and `private final` dependencies; apply `@lombok-usage` and do not write a manual dependency constructor when Lombok is available.
4. Mark write use cases `@Transactional`; mark query use cases `@Transactional(readOnly = true)` when appropriate.
5. Load required entities or throw the project's typed not-found exception. Validate business constraints before mutation.
6. Persist the aggregate and map it to a `Res...DTO` inside the transaction when lazy relationships are needed. Accept ordinary inputs as `Req<Action><Entity>DTO`.
7. Test rollback, not-found, validation conflict, authorization, successful mapping, and pagination/filter behavior.

## Boundaries

- Do not import servlet/web controller types into the service.
- Do not return JPA entities to controllers.
- Keep reusable mapping in private methods or a dedicated mapper when mapping becomes large.
- Avoid broad `catch (Exception)` and silent fallbacks.
- Avoid calling `save()` after mutating a managed entity unless repository/project semantics require it.
- Prevent N+1 queries with projections, entity graphs, join fetches, or bulk reads where warranted.
- Keep external/gRPC/file calls outside a long database transaction when safe; design compensation/idempotency when they must participate.
- Resolve the current user from the project security abstraction, not client-supplied ownership fields.

Read [service-template.md](references/service-template.md) when generating code.
