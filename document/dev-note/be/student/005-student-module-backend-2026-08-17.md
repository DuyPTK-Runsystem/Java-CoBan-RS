# Dev Note: Student Module Backend

## 1. Related Developer Plan

- Plan: `document/dev-impl-plan/be/student/005-student-module-backend-2026-08-17.md`
- Approval: Approved by user via agent on 2026-08-17.

## 2. Actual Scope Completed

- Implemented Student backend module for:
  - fetch list with filter criteria;
  - sort allow-list;
  - pagination default size 10;
  - create student;
  - update mutable student fields;
  - delete student with cascade/orphan relationship;
  - generate unique student code candidate.
- Applied user decisions:
  - ids use Java `Long`;
  - student code format is `STU` + 7 digits;
  - `student_code` is unique at entity/table mapping level;
  - DOB uses `LocalDate`;
  - `student_info.student_id` is a unique FK;
  - repository uses `JpaSpecificationExecutor<Student>`;
  - no `@Autowired` annotation is used.
- Follow-up DTO preference applied after approval:
  - Student request/response DTOs use Java classes instead of records;
  - DTOs use Lombok `@Getter`, `@Setter`, `@NoArgsConstructor`, and `@AllArgsConstructor`;
  - DTOs do not use Lombok `@Data`.
- Follow-up naming/performance preference applied after approval:
  - list method names use `fetch` instead of `search`;
  - list query DTO is `ReqFetchStudentDTO`;
  - student code generation creates 20 candidates per batch and checks existing DB codes in one repository query per batch;
  - max generate-code retry is 5 batches, so worst-case DB lookup count is 5 instead of 100.
- Follow-up Postman collection update applied after explicit user request:
  - added Student folder to `document/postman/Java-CoBan.postman_collection.json`;
  - added fetch, generate code, create, update, and delete student requests;
  - requests use `{{baseUrl}}`, `{{accessToken}}`, and `{{studentId}}` variables.

## 3. Files Changed

### Developer Plan

- `document/dev-impl-plan/be/student/005-student-module-backend-2026-08-17.md`
- `document/dev-impl-plan/be/BE_DEV_PLAN_SUMMARY.md`
- `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md`

### Production Code

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/controller/StudentController.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/domain/DTOs/requests/ReqCreateStudentDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/domain/DTOs/requests/ReqFetchStudentDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/domain/DTOs/requests/ReqUpdateStudentDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/domain/DTOs/response/ResStudentCodeDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/domain/DTOs/response/ResStudentDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/domain/DTOs/response/ResStudentPageDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/domain/entity/Student.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/domain/entity/StudentInfo.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/repository/StudentInfoRepository.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/repository/StudentRepository.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/service/StudentCodeGenerator.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/service/StudentService.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/service/StudentSortResolver.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/service/StudentSpecifications.java`

### Tests

- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/student/service/StudentServiceCodeTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/student/service/StudentServiceMutationTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/student/service/StudentServiceFetchTest.java`

### Dev Note

- `document/dev-note/be/student/005-student-module-backend-2026-08-17.md`
- `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md`
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`

### API Client Artifact

- `document/postman/Java-CoBan.postman_collection.json`

## 4. Important Implementation Decisions

- `StudentRepository` extends `JpaRepository<Student, Long>` and `JpaSpecificationExecutor<Student>`.
- Filter criteria are built in `StudentSpecifications` with AND semantics for populated fields.
- Sort is resolved by `StudentSortResolver` and rejects unsupported fields/directions with `AppException`.
- `StudentService` uses constructor injection only; no `@Autowired` annotation.
- `StudentCodeGenerator` owns random candidate batch creation; `StudentService` owns batch duplicate checking and retry limit.
- `StudentService.fetchStudents(...)` and `StudentController.fetchStudents(...)` use `fetch` naming for the "Lấy danh sách sinh viên" flow.
- PMD issues were fixed by refactoring helper responsibilities instead of adding rule suppressions.
- Student DTOs are class-based API contracts rather than Java records, per user preference.

## 5. Validation

Commands run from `BE/BaiTap-RS`:

| Command | Result | Notes |
|---|---|---|
| `./gradlew test` | PASS | Includes JaCoCo report generation. |
| `./gradlew build` | PASS | Includes compile, test, Checkstyle, PMD, and build lifecycle. |
| `./gradlew checkstyleMain checkstyleTest` | PASS | Final run after code cleanup. |
| `./gradlew pmdMain pmdTest` | PASS | Final run after refactor without PMD suppressions. |

Postman collection validation:

| Check | Result | Notes |
|---|---|---|
| JSON parse | PASS | `document/postman/Java-CoBan.postman_collection.json` parsed successfully. |
| Collection schema | PASS | Uses Postman Collection v2.1 schema. |
| Secret scan | PASS | No config secret copied; only safe sample values and variables are present. |

## 6. JaCoCo Coverage Evidence

- Report:
  - `BE/BaiTap-RS/build/reports/jacoco/test/html/index.html`
  - `BE/BaiTap-RS/build/reports/jacoco/test/jacocoTestReport.xml`
- Student coverage highlights from final report:
  - `StudentService`: line 92.5% (62/67), branch 68.2% (15/22), method 100.0% (12/12).
  - `StudentCodeGenerator`: line 25.0% (2/8), branch 0.0% (0/2), method 50.0% (2/4); service tests mock it to make batch candidates deterministic.
  - `StudentSortResolver`: line 100.0% (11/11), branch 90.0% (9/10), method 100.0% (3/3).
  - `Student` entity: line 100.0% (7/7).
  - `StudentInfo` entity: line 100.0% (5/5).
  - Student DTO classes covered through service tests.

## 7. Deviations From Developer Plan

- Added helper classes `StudentCodeGenerator`, `StudentSortResolver`, and `StudentSpecifications` to keep `StudentService` small enough for PMD without suppressions.
- Split the planned `StudentServiceTest` into three focused service test classes:
  - code generation;
  - mutation CRUD;
  - fetch/filter/sort/page.

## 8. Known Risks / Remaining Work

- `StudentController` has low JaCoCo coverage because this implementation focused on service unit tests per plan; MVC/controller tests can be added in a later task.
- No Postman collection update was done because the user did not request it for this task.
- No frontend integration was implemented in this backend-only plan.
- No migration tool was introduced; schema remains driven by JPA mapping and current `ddl-auto` behavior.

## 9. Debug / Validation Loop Count

- Code -> test/debug loops used: 6.
- Main fixes during validation:
  - removed disallowed `@Autowired`;
  - resolved Spring constructor ambiguity;
  - fixed Checkstyle import/line issues;
  - refactored PMD violations without suppressions.
  - renamed list flow from `searchStudents` to `fetchStudents`;
  - changed student-code generation from per-candidate DB checks to batch lookup.

## 10. Next Steps

- Add controller/MVC tests if API binding and validation coverage becomes required.
- Update Postman collection for Student APIs when explicitly requested.
- Start frontend Student screens after backend API contract is accepted.
