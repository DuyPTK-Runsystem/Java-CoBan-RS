# Dev Note: BE Demo Data Bootstrap — Plan 048

- **Kế hoạch liên quan**: [`document/dev-impl-plan/be/bootstrap/048-be-demo-data-bootstrap-2026-08-26.md`](../../../dev-impl-plan/be/bootstrap/048-be-demo-data-bootstrap-2026-08-26.md)
- **Trạng thái phê duyệt**: `Approved` (2026-08-26, tin nhắn `approve`)
- **Trạng thái triển khai**: `Completed`
- **Application-document version**: `v2`
- **Ngày ghi nhận**: 2026-08-26

## 1. Phạm vi thực tế hoàn thành

- Mandatory seed tạo `admin` với password logic `admin`, role `ADMIN`; password lưu BCrypt.
- Demo fixture bật có điều kiện qua `app.seed.demo.enabled`, mặc định `false`.
- Tạo 1 academic office (`academic.office`), 20 giáo viên (`teacher01`–`teacher20`) và 32 học sinh (`student.6a1.01`–`student.9a2.04`).
- Teacher luôn có `teacher.user_id` trỏ tới `app_user` và role `TEACHER`; academic office là `app_user` với role `ACADEMIC_OFFICE`; student có role `STUDENT` và `student_info`.
- Tạo năm học `2026-2027`, HK1/HK2 đúng thời gian đã chốt, khối 6–9 và 8 lớp `6A1` đến `9A2`.
- Tạo 4 enrollment/lớp, 8 GVCN khác nhau; GVCN vẫn nằm trong danh sách giáo viên dạy môn.
- Tạo 11 môn `ACADEMIC`, 2 môn nghề `SKILL`; môn nghề chỉ áp dụng khối 8/9 ở HK2; Công nghệ khối 9 chỉ ở HK1.
- Tạo subject applicability, class subject và subject teaching assignment deterministic, idempotent.
- Scorebook, assessment column, điểm, calculation, transcript và retake vẫn deferred theo quyết định “scorebook không cần quá vội”.

## 2. File thay đổi chính

### Production/configuration

- `BE/BaiTap-RS/src/main/resources/db/migration/V18__seed_default_admin.sql`
- `BE/BaiTap-RS/src/main/resources/application.properties`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/bootstrap/DemoDataSeeder.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/identity/DemoIdentitySeeder.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/service/DemoAcademicCatalogSeeder.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/service/DemoAcademicApplicabilitySeeder.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/assignment/service/DemoAssignmentSeeder.java`

### Tests

- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/config/FlywayMigrationTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/bootstrap/DemoDataSeederIntegrationTest.java`

### Documentation

- `document/dev-impl-plan/be/bootstrap/048-be-demo-data-bootstrap-2026-08-26.md`
- [`048-be-seeded-data-catalog-2026-08-26.md`](048-be-seeded-data-catalog-2026-08-26.md) — catalog chi tiết dữ liệu đã seed.
- `document/dev-impl-plan/be/BE_DEV_PLAN_SUMMARY.md`
- `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md`

## 3. Quyết định kỹ thuật

- `admin` được seed trong V18 để luôn có sau Flyway migration sạch; migration dùng `INSERT ... WHERE NOT EXISTS` và không ghi đè account đã tồn tại.
- Demo fixture chạy sau khi application context khởi tạo, dùng repository và `PasswordEncoder` hiện tại; mỗi bước kiểm tra natural key trước khi tạo.
- Seeder được tách thành identity, academic catalog, academic applicability và assignment để giảm coupling/cognitive complexity và giữ PMD PASS.
- Fixture không tạo scorebook để không kéo theo assessment columns/weights khi scorebook chưa được yêu cầu triển khai.
- Các password demo: `admin/admin`; academic office, teacher và student dùng `12345678`. Chỉ password logic xuất hiện trong tài liệu local test; giá trị lưu DB là BCrypt.

## 4. Validation Result

| Kiểm tra | Lệnh | Trạng thái | Ghi chú |
| --- | --- | --- | --- |
| Integration test trọng tâm | `./gradlew test --tests com.JavaTraining.BaiTap_RS.bootstrap.DemoDataSeederIntegrationTest --tests com.JavaTraining.BaiTap_RS.config.FlywayMigrationTest --no-daemon --console=plain` | **PASS** | Kiểm tra migration sạch, admin BCrypt, fixture counts, role/link và chạy seeder lặp lại không tạo duplicate. |
| Toàn bộ test backend | `./gradlew test --no-daemon --console=plain` | **PASS** | Regression suite và JaCoCo report đều hoàn tất. |
| Checkstyle | `./gradlew checkstyleMain checkstyleTest --no-daemon --console=plain` | **PASS** | Không có violation. |
| PMD | `./gradlew pmdMain pmdTest --no-daemon --console=plain` | **PASS** | Không có violation; cảnh báo `LoosePackageCoupling` là rule cấu hình rỗng có sẵn. |
| Build | `./gradlew build -x test -x pmdTest -x checkstyleTest --no-daemon --console=plain` | **PASS** | `jar`, `bootJar`, `check` và `build` thành công. |
| MySQL container thực tế | — | **NOT RUN** | Docker service không khả dụng trong phiên này; H2 chạy Flyway sạch được dùng làm bằng chứng tự động. |

## 5. Số vòng debug

- **6 vòng debug chính**: xử lý compile/lambda; tương thích migration legacy; điều chỉnh H2 integration fixture và expected counts; sửa import/checkstyle; tách seeder để xử lý PMD coupling/complexity; loại bỏ suppression và xác nhận lại quality gates.

## 6. Deviations, blockers và rủi ro

- So với thiết kế ban đầu, application seeder được tách thành nhiều component nhỏ để đáp ứng PMD; hành vi fixture không thay đổi.
- Chưa seed assessment columns, skill weight config `25/35/40` hoặc điểm mẫu vì scorebook đã được người dùng defer; subject type `SKILL` đã sẵn sàng cho bước scorebook sau.
- Không có blocker code/validation. Rủi ro còn lại là cần chạy một lần trên MySQL container sạch trong môi trường có Docker trước khi dùng cho manual E2E.

## 7. Cách sử dụng local

Sau khi database đã migrate, bật demo fixture bằng:

```bash
cd BE/BaiTap-RS && APP_SEED_DEMO_ENABLED=true ./gradlew bootRun --no-daemon
```

Nếu chỉ cần database nền và `admin`, giữ flag mặc định `false`; migration V18 vẫn tạo `admin/admin`.

## 8. Next steps

- Chạy preflight trên MySQL 8.x sạch.
- Khi bắt đầu Scorebook E2E, tạo scorebook/assessment columns và cấu hình trọng số nghề `25/35/40` theo Plan 036/048.
