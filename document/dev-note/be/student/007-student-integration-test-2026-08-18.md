# Dev Note: Student Integration Test

## 1. Developer Plan liên quan

- Plan: `document/dev-impl-plan/be/student/007-student-integration-test-2026-08-18.md`
- Trạng thái phê duyệt: `Approved` bởi người dùng trong phiên làm việc ngày 2026-08-18.

## 2. Phạm vi đã hoàn thành

- Bổ sung integration test cho Student API bằng `MockMvc`.
- Khởi động Spring Boot context với H2 in-memory và schema `create-drop`.
- Kiểm tra security context bằng `@WithMockUser`.
- Kiểm tra các luồng tạo, lấy danh sách có phân trang/sắp xếp, cập nhật và xóa.
- Kiểm tra lỗi trùng mã, validation request và student không tồn tại.
- Kiểm tra response wrapper và trạng thái persistence của aggregate `Student` - `StudentInfo`.

## 3. File thay đổi

### Test

- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/student/controller/StudentControllerIntegrationTest.java`

### Tài liệu

- Cập nhật trạng thái Plan 007 trong `document/dev-impl-plan/be/student/007-student-integration-test-2026-08-18.md`.
- Cập nhật `document/dev-impl-plan/be/BE_DEV_PLAN_SUMMARY.md`.
- Cập nhật các Dev Note summary.

### Không thay đổi

- Không thay đổi production code.
- Không thay đổi `SecurityConfiguration.java`; file này có thay đổi sẵn trong worktree và vẫn còn import `HttpMethod` thừa.

## 4. Quyết định triển khai

- Dùng `@SpringBootTest` vì test cần đi qua controller, service, repository và JPA.
- Dùng `@Transactional` để giữ quan hệ lazy trong lúc kiểm tra database state và cô lập dữ liệu giữa các test.
- Gửi JSON trực tiếp thay vì serialize DTO bằng `ObjectMapper` test bean, vì `ObjectMapper` hiện tại không đăng ký module JSR-310 cho `LocalDate`.
- Không test endpoint `/api/v1/students/code` theo đúng phạm vi đã duyệt; logic sinh mã đã có unit test riêng.

## 5. Validation

| Lệnh | Kết quả thực tế |
|---|---|
| `./gradlew test --tests com.JavaTraining.BaiTap_RS.student.controller.StudentControllerIntegrationTest` | `PASS` - 8 test hoàn thành |
| `./gradlew test checkstyleTest pmdTest build` | `FAIL` ở `pmdMain`; `test`, `checkstyleTest` và `pmdTest` đã chạy `PASS` |
| `checkstyleMain` trong quality gate | `PASS` với cảnh báo import `HttpMethod` thừa trong `SecurityConfiguration.java` |
| `pmdMain` trong quality gate | `FAIL` - import `org.springframework.http.HttpMethod` thừa trong `SecurityConfiguration.java` |
| `build` trong quality gate | `FAIL` do task `check` kéo theo `pmdMain` đang fail ở lỗi baseline trên |
| JaCoCo `build/reports/jacoco/test/jacocoTestReport.xml` | `PASS` - `StudentController`: 5/6 method, 9/10 line được cover |

## 6. Sai lệch và blocker

- Không có sai lệch chức năng so với Plan 007.
- Full quality gate chưa xanh do lỗi PMD/Checkstyle baseline ở `SecurityConfiguration.java`, nằm ngoài phạm vi Plan 007 và không được sửa trong task này.
- Không thêm dependency, Testcontainers hoặc schema migration.

## 7. Bước tiếp theo

- Có thể xử lý import `HttpMethod` thừa trong một task riêng nếu người dùng phê duyệt phạm vi thay đổi `SecurityConfiguration.java`.
- Có thể bổ sung integration test cho endpoint sinh mã khi cần kiểm tra deterministic generator ở tầng HTTP.
