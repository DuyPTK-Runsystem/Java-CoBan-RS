---
name: module-impl
description: Define, scaffold, implement, or review a feature-first Spring Boot module with controller, service, repository, domain/entity, and domain/DTOs/requests and response packages. Use for requests to create a backend module, add a CRUD feature, standardize package architecture, list required module files, or verify that a module follows the DoAn1_BhpWebsite style.
---

# Implement a Spring Boot module

## Workflow

1. Inspect the repository's package name, Java/Spring versions, response wrapper, exception hierarchy, Long ID/auditing strategy, validation style, and neighboring modules. Preserve established conventions unless the user requests a migration.
2. Define the module boundary and invariants before creating files. Keep one business capability per module.
3. Create the structure below. Treat each leaf as a package; Java package segments should normally be lowercase. If the repository already uses `requestDTO`/`responseDTO`, preserve it instead of creating duplicate conventions.
4. Implement dependency direction: `controller -> service -> repository -> entity/database`. DTOs may cross the HTTP boundary; entities must not.
5. Apply `@lombok-usage` when generating Java classes that use Lombok annotations.
6. Run focused tests, compilation, and formatting available in the repository. Report files created, behavior, and unresolved decisions.

## Required structure

```text
<module-name>/
├── controller/
│   └── <Entity>Controller.java
├── service/
│   └── <Entity>Service.java
├── repository/
│   └── <Entity>Repository.java
└── domain/
    ├── entity/
    │   └── <Entity>.java
    └── DTOs/
        ├── response/
        │   └── Res<Entity>DTO.java
        └── requests/
            ├── ReqCreate<Entity>DTO.java
            └── ReqUpdate<Entity>DTO.java
```

Example: `ExamController.java`, `ExamService.java`, `ExamRepository.java`, `Exam.java`, `ResExamDTO.java`, `ReqCreateExamDTO.java`, and `ReqUpdateExamDTO.java`.

## DTO naming convention

- Name ordinary request DTOs `Req<Action><Entity>DTO`, such as `ReqCreateGradeDTO`, `ReqUpdateCostDTO`, and `ReqDeleteLessonDTO`.
- Name ordinary response DTOs `Res<Entity>DTO`, such as `ResCostDTO`, `ResGradeDTO`, and `ResLessonDTO`.
- Preserve an extra business qualifier only when the DTO has a distinct contract, such as `ReqBulkCreateAttendanceDTO`, `ResGradeListDTO`, `ResLessonAttendanceCandidateDTO`, or `ResLessonEmployeeAssignmentDTO`.
- Do not generate generic names such as `CreateExamRequest`, `UpdateExamRequest`, or `ExamResponse` in this project.
- Keep the `DTO` suffix uppercase and place the request/response direction at the beginning as `Req` or `Res`.

## Layer contracts

- Keep controllers thin: HTTP mapping, input validation, authentication context extraction, service delegation, and status/response construction only.
- Put business rules, orchestration, entity-to-DTO mapping, and transaction boundaries in services.
- Keep repositories as Spring Data interfaces; add explicit queries only when method naming is insufficient or performance requires them.
- Model database structure and persistence invariants in entities. Do not put controller/service behavior in entities.
- Split `Req...DTO` and `Res...DTO` contracts. Never accept or expose a JPA entity as an API contract.
- Add `@Transactional` to writes and `@Transactional(readOnly = true)` to reads that require a persistence context.
- Use pagination for growing collections; use the project default (20 in Exam Service) unless requirements differ.
- Reuse global exception and response formatting. Do not catch broad exceptions in each controller.
- Prefer constructor injection with `final` dependencies.
- Prefer Lombok `@RequiredArgsConstructor` for service/controller dependencies when Lombok is available; avoid `@Data` on JPA entities.

## Composition

- Use `@controller-impl` for controllers.
- Use `@service-impl` for services.
- Use `@entity-impl` for JPA entities/tables.
- Use `@lombok-usage` for Lombok annotation decisions.

Read [module-example.md](references/module-example.md) when file names, package layout, repository, or DTO examples are needed.
