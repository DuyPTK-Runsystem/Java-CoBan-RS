# Dev Note 026: Student Enrollment & Class Placement

## 1. Developer Plan liên quan và approval

- Developer Plan: `document/dev-impl-plan/be/enrollment/026-student-enrollment-class-placement-2026-08-21.md`.
- Approval: plan ghi nhận được người dùng phê duyệt ngày 2026-08-21; prompt hiện tại yêu cầu thực hiện plan 26.
- Application-document version: `v2`.

## 2. Phạm vi đã triển khai

- Tạo academic foundation cho `grade_level`, `academic_year` và `school_class` với CRUD/list, lifecycle và guarded delete/close.
- Tạo enrollment đơn, bulk atomic, unassigned list, class students và student enrollment history.
- Tạo transfer transaction cập nhật lớp hiện tại, append `class_transfer_history`, ghi `audit_log` before/after và request context.
- Thêm capacity warning theo trung bình khối; `capacity` chỉ là metadata, không chặn enrollment.
- Giới hạn mutation cho `ADMIN`/`ACADEMIC_OFFICE`; `TEACHER` chỉ đọc các API enrollment/class được phép.
- Giữ nguyên contract legacy `/api/v1/students/**`.
- Refactor sau validation: tách academic controller/service theo `academic-year`, `grade-level`, `school-class`; tách enrollment lookup/query/capacity/audit khỏi mutation service để đạt PMD mà không dùng suppression.

## 3. Files thay đổi theo nhóm

### Backend source

- `BE/BaiTap-RS/src/main/resources/db/migration/V4__create_academic_structure_enrollment_and_audit.sql`: migration V4, status `student`, academic/enrollment/audit tables, FK, index và unique constraints.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/`: academic entities, DTOs, repositories, controller/service/validator theo từng nhóm year/grade/class.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/enrollment/`: enrollment entities, DTOs, repositories, mutation service và query/lookup/capacity/audit collaborators.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/common/audit/`: audit entity/repository và request/security context reader.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/domain/entity/Student.java` và `StudentStatus.java`: map trạng thái student để chặn student không active.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/common/error/GlobalExceptionHandler.java`: map database uniqueness race thành HTTP 409.

### Tests

- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/config/FlywayMigrationTest.java`: kiểm tra 6 bảng V4 và unique constraint; fixture legacy có đủ student/student_info.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/academic/service/AcademicServiceTest.java`: active-year guard, class history delete guard và grade-change guard.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/enrollment/service/EnrollmentServiceTest.java`: create/history, duplicate và bulk duplicate.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/enrollment/service/EnrollmentTransferServiceTest.java`: transfer/audit.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/enrollment/controller/EnrollmentAuthorizationIntegrationTest.java`: teacher read, teacher mutation, student và anonymous authorization.

### Application documentation

- Cập nhật `data-model/02-AcademicCatalog.md`, `03-StudentsAndEnrollment.md` và `08-AuditAndConstraints.md` theo lifecycle `DRAFT/ACTIVE/CLOSED`, `PLANNED/ACTIVE/CLOSED`, `WITHDRAWN` và transfer audit.

## 4. Quyết định triển khai quan trọng

- Migration dùng `V4` vì repository đã có V1–V3; không tạo lại hoặc đổi migration cũ.
- Transfer nội bộ giữ enrollment `ACTIVE`, chỉ đổi `current_class_id` và append history.
- `audit_log` được ghi trong cùng transaction với transfer; lỗi khi ghi history/audit sẽ làm transaction rollback.
- DTO được dùng ở HTTP boundary; entity chỉ dùng trong persistence/service.
- Database unique `(student_id, academic_year_id)` là lớp bảo vệ cuối cho BR-ENROLL-001; service/global handler map conflict rõ ràng.

## 5. Validation thực tế

### 5.1. Validation sau restore từ rescue branch

| Lệnh/kiểm tra | Kết quả | Bằng chứng |
|---|---|---|
| `./gradlew test jacocoTestReport checkstyleMain checkstyleTest pmdMain pmdTest build` | PASS | `BUILD SUCCESSFUL`; 12 task, 3 executed và 9 up-to-date sau khi restore Plan 026 từ `rescue/plan-026-full-63cc286`. |
| `python3 -m json.tool document/postman/Java-CoBan.postman_collection.json` | PASS | Postman collection JSON hợp lệ sau restore. |

Số vòng `restore → validation → debug` trong lần cứu dữ liệu sau revert: `1`.

### 5.2. Validation implementation ban đầu

| Lệnh/kiểm tra | Kết quả | Bằng chứng |
|---|---|---|
| `javac --release 21` cho production source mới và source bị ảnh hưởng | PASS | Compile độc lập hoàn tất với dependency cache và Lombok processor. |
| `javac --release 21` cho test mới và `FlywayMigrationTest` | PASS | Compile độc lập hoàn tất với JUnit/Mockito/Flyway/H2 cache. |
| H2 Shell chạy V1→V4 trên schema clean | PASS | Tạo đủ 6 bảng Plan 026, query trả `6`. |
| H2 Shell chạy fixture legacy + V2→V4 | PASS | Rename `user`, seed role, thêm `student.status` và migration V4 hoàn tất. |
| `git diff --check` | PASS | Không có whitespace error. |
| `./gradlew test` | PASS | `BUILD SUCCESSFUL`; 64 tests chạy qua, `jacocoTestReport` chạy finalize. |
| `./gradlew jacocoTestReport` | PASS | `BUILD SUCCESSFUL`; report XML/HTML up-to-date sau `test`. |
| `./gradlew checkstyleMain checkstyleTest` | PASS | `BUILD SUCCESSFUL`; main/test Checkstyle pass. |
| `./gradlew pmdMain pmdTest` | PASS | `BUILD SUCCESSFUL`; PMD main/test pass sau 3 vòng sửa PMD. |
| `./gradlew build` | PASS | `BUILD SUCCESSFUL`; assemble/check/build pass. |

Số vòng `code → test → debug` trong lần hoàn tất validation: `3`.

1. Vòng 1: `test` fail tại `FlywayMigrationTest` do H2 metadata query dùng `INFORMATION_SCHEMA.CONSTRAINTS`; sửa sang `TABLE_CONSTRAINTS`.
2. Vòng 2: PMD fail nhiều lỗi coupling/method count/assertions; refactor academic/enrollment service/controller và dọn test.
3. Vòng 3: PMD còn lỗi lifecycle callback, helper test và method count; sửa callback package-private có comment, tách validator/test transfer; PMD pass.

## 6. Sai lệch so với Developer Plan

- Không có sai lệch chức năng đáng kể.
- Plan mô tả migration “tiếp theo V3” nhưng danh sách file dự kiến đã xác nhận V4; implementation dùng `V4__create_academic_structure_enrollment_and_audit.sql` theo chuỗi migration thực tế.
- Sau validation, cấu trúc code được tách nhỏ hơn dự kiến ban đầu để đáp ứng PMD mà không suppress rule hoặc chỉnh report/config.

## 7. Blocker và rủi ro còn lại

- Không còn blocker Gradle validation trong môi trường hiện tại.
- Rủi ro còn lại: cần xác nhận schema MySQL thực tế trước khi dùng migration ngoài test/local database.

## 8. Next steps

1. Xác nhận thêm schema MySQL thực tế trước khi dùng migration ngoài test/local database.
