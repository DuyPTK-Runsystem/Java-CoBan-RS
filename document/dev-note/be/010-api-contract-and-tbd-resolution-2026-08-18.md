# Dev Note: API Contract and TBD Resolution

## 1. Developer Plan liên quan

- Plan: `document/dev-impl-plan/be/010-api-contract-and-tbd-resolution-2026-08-18.md`.
- Approval: Người dùng phê duyệt qua agent ngày 2026-08-18.

## 2. Phạm vi đã hoàn thành

- Chuẩn hóa tài liệu theo API contract hiện tại:
  - Endpoint dùng prefix `/api/v1`.
  - `RestResponse` là response envelope chuẩn.
  - `FormatRestResponse` bọc success body bằng `@RestControllerAdvice`/`ResponseBodyAdvice`.
  - `ResStudentPageDTO` nằm trong `RestResponse.data`.
  - User/Auth dùng JWT stateless.
  - Student code có format `STU` + 7 chữ số và unique.
  - User/Student ids dùng `Long`/`BIGINT` trong model tài liệu.
  - API date dùng `yyyy-MM-dd`, UI date ghi theo chỉ dẫn `dd-mm-yyy`.
- Chuẩn hóa auth state contract cho frontend:
  - Access token và UI-safe user summary lưu trong `sessionStorage`.
  - Không lưu password hoặc password hash.
  - Logout xóa toàn bộ auth state.
  - `401` xóa auth state và redirect Login; `403` giữ auth state.
- Backend trả `401 Unauthorized` nhất quán cho anonymous protected request và bearer token malformed, expired hoặc invalid signature.
- Bổ sung integration tests cho malformed, invalid-signature và expired JWT.
- CSV batch vẫn giữ `TBD` về file layout, output path và trigger.

## 3. Files thay đổi

### Backend security

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/security/RestAuthenticationEntryPoint.java`
  - Trả `RestResponse` JSON với HTTP `401` cho authentication failure.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/security/JwtAuthenticationFilter.java`
  - Bắt lỗi token cụ thể, xóa security context và chuyển lỗi sang authentication entry point.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/config/SecurityConfiguration.java`
  - Đăng ký REST authentication entry point.

### Backend tests

- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/user/controller/AuthControllerIntegrationTest.java`
  - Cập nhật anonymous account/logout contract từ `403` sang `401`.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/user/controller/InvalidJwtIntegrationTest.java`
  - Test malformed và invalid-signature token.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/user/controller/ExpiredJwtIntegrationTest.java`
  - Test token hết hạn bằng JWT validity bằng 0, không dùng sleep.

### Application documentation

- `document/application-doc/v1/ApplicationContext.md`
- `document/application-doc/v1/modules/UserModule.md`
- `document/application-doc/v1/modules/StudentModule.md`
- `document/application-doc/v1/DataStructure.md`
  - Đồng bộ endpoint, response wrapper, auth state, status behavior, student-code uniqueness, id type và date format.

### Plan tracking

- `document/dev-impl-plan/be/010-api-contract-and-tbd-resolution-2026-08-18.md`
- `document/dev-impl-plan/be/BE_DEV_PLAN_SUMMARY.md`
- `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md`

## 4. Quyết định triển khai

- `FormatRestResponse` tiếp tục là nơi bọc success response; không thêm wrapper riêng cho page response.
- Authentication entry point trả cùng `RestResponse` envelope thay vì body lỗi tự do.
- JWT filter chỉ bắt các lỗi token/user lookup liên quan đến authentication; không tắt hoặc thay đổi các quality rule.
- Anonymous request không có authentication và token không hợp lệ đều là `401`; `403` dành cho authenticated user thiếu quyền.
- Không triển khai frontend code trong plan này; frontend sẽ dùng contract đã ghi nhận ở plan sau.

## 5. Validation

Chạy từ `BE/BaiTap-RS`:

| Command | Result | Evidence |
|---|---|---|
| `./gradlew test --tests com.JavaTraining.BaiTap_RS.user.controller.AuthControllerIntegrationTest --tests com.JavaTraining.BaiTap_RS.user.controller.ExpiredJwtIntegrationTest` | PASS | Auth integration và expired JWT test pass |
| `./gradlew test` | PASS | 33 tests pass, không failure/error; JaCoCo report được tạo |
| `./gradlew checkstyleMain checkstyleTest` | PASS | Không còn warning thuộc thay đổi mới |
| `./gradlew pmdMain pmdTest` | PASS | PMD pass; console chỉ ghi nhận rule baseline `LoosePackageCoupling` thiếu package |
| `./gradlew build` | PASS | `BUILD SUCCESSFUL` |
| `./gradlew test checkstyleMain checkstyleTest pmdMain pmdTest build` | PASS | Validation cuối sau sửa lỗi |

JaCoCo report được tạo tại:

- `BE/BaiTap-RS/build/reports/jacoco/test/jacocoTestReport.xml`.
- `BE/BaiTap-RS/build/reports/jacoco/test/html/index.html`.

## 6. Deviations và vấn đề còn lại

- Plan ban đầu dự kiến chỉ bổ sung contract test nếu cần; implementation phát hiện JWT exception chưa được chuyển thành `401`, nên đã bổ sung REST authentication entry point và thay đổi filter trong đúng phạm vi auth contract đã được người dùng chốt.
- CSV batch chưa triển khai vì file layout, output path và trigger vẫn là `TBD`.
- `403` chưa có endpoint business permission cụ thể để test vì project hiện chưa có Role/Permission; behavior frontend đã được ghi nhận.
- PMD vẫn in cảnh báo cấu hình baseline `LoosePackageCoupling` bị thiếu package, nhưng task `pmdMain` và `pmdTest` đều pass.
- Date UI được ghi đúng theo chỉ dẫn hiện tại là `dd-mm-yyy`; cần giữ nguyên hoặc xác nhận lại token trước khi triển khai date formatter frontend.

## 7. Vòng debug

- Tổng số vòng `code -> test -> debug`: 2.
- Vòng 1: test pass nhưng Checkstyle có import/line warning và PMD báo bắt `RuntimeException` cùng test class quá nhiều method.
- Vòng 2: bắt exception cụ thể, tách test JWT invalid, sửa import/line length; toàn bộ validation cuối pass.

## 8. Bước tiếp theo

- Plan FE tiếp theo có thể triển khai API service/auth state theo `sessionStorage` contract.
- Cần chốt CSV layout, output path và trigger trước khi tạo plan Spring Batch.
