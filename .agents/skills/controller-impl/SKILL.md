---
name: controller-impl
description: Create, update, or review thin Spring Boot REST controller files in the DoAn1_BhpWebsite style. Use when implementing endpoints, request mappings, validation, pagination, HTTP statuses, authorization annotations, or an EntityController.java template.
---

# Implement a REST controller

## Procedure

1. Inspect adjacent controllers and reuse the base path, response envelope, `ApiMessage`, exception handling, security, pagination, and naming conventions.
2. Define resource-oriented routes and correct HTTP verbs/statuses.
3. Accept `Req...DTO` objects with `@Valid`; return `Res...DTO` objects, never entities. Preserve special qualifiers such as `Bulk`, `List`, or `Candidate` only for distinct contracts.
4. Delegate each endpoint to one service method. Keep business rules, repository access, mapping, and transactions out of the controller.
5. Add focused MVC tests for success, invalid input, authentication/authorization, missing resources, and malformed identifiers.

## Required shape

- Use `@RestController`, class-level `@RequestMapping`, `@RequiredArgsConstructor`, and `private final` dependencies; apply `@lombok-usage` and do not write a manual dependency constructor when Lombok is available.
- Use plural resource paths such as `/api/v1/exams`.
- Use explicit parameter names where project conventions require them.
- Default growing list endpoints to `Page<Res...DTO>` and `Pageable`; bound page size if the global resolver does not.
- Let the global exception handler translate application exceptions.
- Use `ResponseEntity` only when status or headers vary; otherwise return the project-standard type directly.
- Do not log passwords, tokens, uploaded content, or full sensitive DTOs.

Read [controller-template.md](references/controller-template.md) when generating code.
