# Developer Plan 025: Contract, Migration và Scope Freeze

## 1. Trạng thái và phiên bản áp dụng

- Status: `Approved by user on 2026-08-20`.
- Application-document version: `v2`.
- Số `024` được bỏ qua cho Developer Plan để đồng nhất với Dev Note 024 đã tồn tại.
- Plan này chỉ được triển khai sau khi người dùng phê duyệt Plan 025. M4 vẫn cần preflight xác minh cột/hash thực tế trước khi chạy migration legacy.

## 2. Mục tiêu

Thiết lập một baseline triển khai an toàn cho schema và contract v2 trước khi thay đổi database, entity, API hoặc frontend:

1. Lập ma trận truy vết `requirement v2 → data model → contract hiện có → migration impact`.
2. Đóng băng ranh giới giữa contract legacy đang hoạt động và contract/schema v2 mục tiêu.
3. Chốt chiến lược migration, tương thích dữ liệu và rollback trước khi thêm migration script.
4. Tách các quyết định chưa đủ thông tin thành decision gate rõ ràng; không biến chúng thành implementation ngầm.

## 3. Tài liệu và implementation cần đối chiếu

### Application documentation v2

- `document/application-doc/Java-CoBan-RS-application-doc-modular-v2/document/application-doc/ApplicationContext.md`.
- `document/application-doc/Java-CoBan-RS-application-doc-modular-v2/document/application-doc/RequirementBaseline.md`.
- `document/application-doc/Java-CoBan-RS-application-doc-modular-v2/document/application-doc/modules/00-CommonAndAuthModule.md`.
- `document/application-doc/Java-CoBan-RS-application-doc-modular-v2/document/application-doc/modules/01-AcademicStructureModule.md`.
- `document/application-doc/Java-CoBan-RS-application-doc-modular-v2/document/application-doc/modules/02-EnrollmentAndTeachingModule.md`.
- `document/application-doc/Java-CoBan-RS-application-doc-modular-v2/document/application-doc/modules/04-AssessmentAndScoringModule.md`.
- `document/application-doc/Java-CoBan-RS-application-doc-modular-v2/document/application-doc/modules/05-ScoreChangeAndCalculationModule.md`.
- `document/application-doc/Java-CoBan-RS-application-doc-modular-v2/document/application-doc/modules/06-RetakeAndTranscriptModule.md`.
- `document/application-doc/Java-CoBan-RS-application-doc-modular-v2/document/application-doc/modules/07-AccessQualityAndAcceptanceModule.md`.
- `document/application-doc/Java-CoBan-RS-application-doc-modular-v2/document/application-doc/data-model/00-OverviewAndMigration.md`.
- `document/application-doc/Java-CoBan-RS-application-doc-modular-v2/document/application-doc/data-model/01-IdentityAndAccess.md`.
- `document/application-doc/Java-CoBan-RS-application-doc-modular-v2/document/application-doc/data-model/03-StudentsAndEnrollment.md`.
- `document/application-doc/Java-CoBan-RS-application-doc-modular-v2/document/application-doc/data-model/09-MigrationAndJPA.md`.

### Current implementation and existing contracts

- `BE/BaiTap-RS/src/main/resources/application.properties`.
- `BE/BaiTap-RS/build.gradle.kts`.
- User/Auth, Student and security packages under `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/`.
- `document/dev-impl-plan/be/010-api-contract-and-tbd-resolution-2026-08-18.md`.
- `document/application-doc/Java-CoBan-RS-application-doc-modular-v2/document/application-doc/modules/UserModule.md`.
- `document/application-doc/Java-CoBan-RS-application-doc-modular-v2/document/application-doc/modules/StudentModule.md`.

## 4. Contract và scope cần đóng băng

### Đã xác định từ v2 và implementation hiện tại

- Schema mục tiêu dùng `app_user`, `BIGINT UNSIGNED`, `snake_case`, `password_hash VARCHAR(255)` và không dùng `average_score` làm nguồn điểm chính thức.
- Contract đang chạy tiếp tục là `/api/v1/auth/**` và `/api/v1/students/**`, dùng JWT stateless và `RestResponse`; chưa có quyết định đổi endpoint hay response envelope.
- `student` và `student_info` vẫn là aggregate hiện có; `student_info.student_id` phải giữ unique foreign key.
- Schema hiện tại đang do Hibernate `ddl-auto=update` quản lý; `build.gradle.kts` chưa cấu hình công cụ migration.

### Freeze rule

- Không đổi, xóa, đổi tên endpoint/DTO/response field legacy chỉ vì schema v2 thay đổi.
- Không chạy DDL destructive, rename bảng production, backfill, đổi `ddl-auto`, thêm Flyway/Liquibase, hoặc sửa entity mapping trước khi các gate ở mục 5 được xác nhận.
- Không mở rộng sang điểm, transcript, attendance, assignment hay worker khi foundation migration và contract compatibility chưa được chốt.
- Mọi contract mới phải được version hóa rõ, có consumer, owner và test boundary trước khi triển khai.

## 5. Decision gate bắt buộc

| Gate | Quyết định | Điều kiện/ràng buộc |
|---|---|---|
| M1 | Dùng Flyway | Chỉ thêm dependency/plugin và migration script sau approval Plan 025. |
| M2 | Hỗ trợ cả schema trống và database training legacy | Phải có test riêng cho baseline trống và fixture legacy. Không nhắm production trong plan này. |
| M3 | Rename in-place `user` → `app_user` | Xác minh schema thực tế trước khi đổi `@Table`, repository hoặc security lookup. |
| M4 | Giữ nguyên password hash cũ khi tương thích | Preflight phải xác minh tên cột thực tế và hash là BCrypt hợp lệ; chỉ rename/copy nguyên giá trị, không double-hash. Nếu không tương thích thì dừng migration legacy để xin quyết định reset password. |
| M5 | Giữ `average_score` legacy đến khi hoàn thành luồng tính average score mới | Đánh dấu deprecated; không dùng làm nguồn điểm chính thức, không tạo transcript/backfill chính thức từ dữ liệu này. |
| M6 | Seed `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`, `STUDENT`; authorization bằng `@PreAuthorize` | Bật method security và thống nhất `hasRole(...)` với authority `ROLE_<ROLE>`. Chỉ áp dụng endpoint có policy đã duyệt. |
| M7 | Deferred cho phạm vi project học | Không thực hiện production cutover. Migration chỉ chạy trên local/test database; không tuyên bố có backup, rollback hay production readiness. |

## 6. Phương án triển khai sau approval

1. **Inventory và traceability**
   - Đọc code, schema thực tế và test hiện có ở chế độ chỉ đọc.
   - Lập ma trận cho từng thay đổi v2: nguồn requirement, bảng/constraint, API/consumer ảnh hưởng, compatibility, migration order, owner và test cần có.
2. **Contract freeze artifact**
   - Tạo tài liệu v2 chuyên biệt ghi baseline legacy, contract v2 đã được duyệt, compatibility window, deprecation policy và các decision gate đã chốt.
   - Mọi mục chưa được xác nhận giữ trạng thái `TBD`/`Needs confirmation`; không sửa requirement code.
3. **Migration design**
   - Dùng Flyway và tạo migration theo dependency order v2, bắt đầu từ identity và schema legacy compatibility.
   - Dùng forward-only migration có precondition và dữ liệu fixture; không dựa vào `ddl-auto=update` ở production. Production cutover/runbook được để ngoài scope theo M7.
   - Giữ id và `student_code` khi chuyển dữ liệu; không tạo điểm trung bình chính thức từ `average_score` legacy.
4. **Contract-preserving implementation**
   - Chỉ đổi entity/repository/security khi migration và mapping tương ứng đã được chốt.
   - Bảo toàn `/api/v1` response shape trong compatibility window; thay đổi breaking phải có plan riêng và consumer migration.
5. **Verification và freeze review**
   - Kiểm thử migration trên database trống và fixture legacy đại diện trước khi chạm environment dùng chung.
   - Đối chiếu schema thực tế, JPA mapping, constraint, API contract và documentation; ghi nhận mọi sai lệch trong Dev Note.

## 7. Phạm vi dự kiến

### In-scope

- Artifact contract/migration scope freeze và ma trận traceability v2.
- Cấu hình Flyway, migration foundation và tests chỉ sau approval Plan 025 và preflight M4.
- Cập nhật có kiểm soát application documentation v2, Developer Plan/Dev Note summaries và runbook migration.
- Test contract HTTP, migration, constraint và compatibility có liên quan trực tiếp đến foundation đã duyệt.

### Out-of-scope

- Triển khai toàn bộ module academic, enrollment, attendance, scoring, transcript hoặc batch calculation.
- Thay đổi frontend UX, route hoặc API service ngoài compatibility work đã duyệt.
- Tự tạo role/permission matrix chi tiết, công thức tính điểm, danh sách môn skill, quy tắc thi lại hoặc worker policy còn `TBD`.
- Migration production, backup/rollback production và cutover trên dữ liệu thật.

## 8. Files dự kiến

### Có thể tạo hoặc cập nhật sau approval

- `document/application-doc/Java-CoBan-RS-application-doc-modular-v2/document/application-doc/ContractMigrationScopeFreeze.md`.
- Các `data-model/*.md` v2 liên quan, chỉ để ghi quyết định đã xác nhận hoặc CR.
- `BE/BaiTap-RS/build.gradle.kts`, `BE/BaiTap-RS/src/main/resources/application.properties` và Flyway migration directory.
- Entity/repository/security/test trực tiếp bị ảnh hưởng bởi migration foundation đã được duyệt.
- `document/dev-note/summary/` và area summary phù hợp sau implementation.

### Không thay đổi trong bước lập plan

- Database, backend source, frontend source, Docker runtime và Postman collection.

## 9. Validation dự kiến sau implementation

Chạy từ `BE/BaiTap-RS` sau mỗi thay đổi backend được duyệt:

```bash
./gradlew test
./gradlew checkstyleMain checkstyleTest
./gradlew pmdMain pmdTest
./gradlew build
```

Bổ sung bằng chứng riêng cho migration:

- migration thành công trên schema trống;
- migration fixture legacy bảo toàn `user`/student data theo M2–M5, gồm xác minh BCrypt hash trước khi rename in-place;
- constraint, unique key và foreign key v2 được kiểm thử;
- application khởi động với schema migrated mà không dùng `ddl-auto=update`;
- regression HTTP bảo toàn contract legacy đã freeze.

## 10. Rủi ro và giảm thiểu

| Rủi ro | Giảm thiểu |
|---|---|
| Schema v2 vượt xa feature đang chạy | Chia foundation migration theo dependency order; không mở module nghiệp vụ khi chưa có contract/consumer |
| Rename `user` làm gián đoạn JWT login | Preflight M4, chạy compatibility test trên local/test fixture; production rollout nằm ngoài scope M7 |
| `average_score` bị hiểu nhầm là điểm chính thức | Đánh dấu deprecated, giữ policy rõ và không backfill transcript |
| `ddl-auto=update` làm drift schema | Chỉ tắt/chuyển sang validate sau khi tool migration và baseline schema được duyệt/test |
| Migration dữ liệu không đảo ngược | Chỉ chạy local/test fixture; không thực hiện production cutover khi M7 đang deferred |

## 11. Output dự kiến

- Contract/migration scope freeze v2 đã được phê duyệt.
- Ma trận traceability và decision log M1–M7 có owner/status.
- Chuỗi migration foundation có thể thực thi, kiểm thử và rollback theo policy đã chốt.
- Contract legacy được bảo toàn rõ ràng trong compatibility window.
- Dev Note chứa validation thực tế, scope đã làm và các blocker còn lại.

## 12. Approval cần có

Plan 025 được người dùng phê duyệt ngày 2026-08-20. Trước migration legacy, xác nhận kết quả preflight M4 về tên cột và BCrypt compatibility.
