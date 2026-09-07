# Developer Plan: API Contract and TBD Resolution

## 1. Mục tiêu

- Chuẩn hóa API contract dùng chung giữa backend, frontend và các artifact kiểm thử.
- Đối chiếu các quyết định đã được implementation áp dụng với `ApplicationContext.md`, `UserModule.md`, `StudentModule.md` và `DataStructure.md`.
- Tách rõ quyết định đã chốt, quyết định cần xác nhận và quyết định vẫn để `TBD`, tránh để các plan frontend hoặc batch tự suy đoán.
- Bổ sung kiểm thử contract ở các boundary backend còn thiếu nếu việc đối chiếu phát hiện khoảng trống cần bảo vệ.

## 2. Tài liệu và implementation liên quan

- `document/application-doc/v1/ApplicationContext.md`.
- `document/application-doc/v1/modules/UserModule.md`.
- `document/application-doc/v1/modules/StudentModule.md`.
- `document/application-doc/v1/DataStructure.md`.
- `document/dev-impl-plan/be/user-auth/001-base-boilerplate-user-auth-2026-08-17.md`.
- `document/dev-impl-plan/be/student/005-student-module-backend-2026-08-17.md`.
- `document/dev-impl-plan/be/student/007-student-integration-test-2026-08-18.md`.
- `document/dev-impl-plan/be/user-auth/008-user-integration-test-2026-08-18.md`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/user/controller/AuthController.java`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/controller/StudentController.java`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/common/dto/RestResponse.java`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/common/error/GlobalExceptionHandler.java`.

## 3. Quyết định cần chuẩn hóa

### 3.1 Đã có bằng chứng từ backend hiện tại

- Auth API dùng các endpoint:
  - `POST /api/v1/auth/register`.
  - `POST /api/v1/auth/login`.
  - `GET /api/v1/auth/account`.
  - `POST /api/v1/auth/logout`.
- Student API dùng các endpoint:
  - `GET /api/v1/students`.
  - `POST /api/v1/students`.
  - `PUT /api/v1/students/{studentId}`.
  - `DELETE /api/v1/students/{studentId}`.
  - `POST /api/v1/students/code`.
- Authentication hiện tại là JWT stateless; logout trả `204 No Content` và không revoke token.
- Success response dùng `RestResponse` qua `FormatRestResponse`, được triển khai bằng `@RestControllerAdvice` và `ResponseBodyAdvice`.
- `FormatRestResponse` đặt `statusCode`, `message` và `data` vào `RestResponse`, đồng thời bỏ qua body đã là `RestResponse`, `String` hoặc `Resource`.
- Student page response dùng `ResStudentPageDTO` trong trường `RestResponse.data`, với `content`, `page`, `size`, `totalElements` và `totalPages`.
- Error response đi qua `GlobalExceptionHandler` và dùng cùng lớp `RestResponse`.
- Student list mặc định page size 10, filter nhiều trường dùng AND semantics và sort chỉ chấp nhận allow-list.
- Student code có format `STU` cộng 7 chữ số và phải unique theo quyết định backend hiện tại.
- User và Student đều dùng Java `Long`; birthday dùng `LocalDate` trong application model.
- UI hiển thị ngày theo format `dd-mm-yyy` theo chỉ dẫn hiện tại.
- Access token được lưu trong `sessionStorage` sau khi login thành công.
- User summary được lưu trong `sessionStorage`, chỉ gồm dữ liệu cần cho UI như `id` và `username`/`name`.
- Frontend không được lưu password hoặc password hash trong auth state.
- Logout xóa toàn bộ auth state khỏi `sessionStorage`.
- `401 Unauthorized` được xử lý như phiên không còn hợp lệ: xóa auth state và redirect về Login.
- `403 Forbidden` giữ nguyên auth state, hiển thị thông báo không có quyền truy cập và không logout.
- Token hết hạn, malformed hoặc signature invalid được backend trả `401` và frontend xử lý như `401` thông thường.

Các quyết định trên phải được ghi nhất quán vào tài liệu source-of-truth. Không đổi behavior production chỉ để làm cho tài liệu khớp nếu chưa có yêu cầu hoặc test chứng minh cần sửa.

### 3.2 Cần xác nhận trước các plan phụ thuộc

- CSV batch là một joined student export hay ba file riêng.
- Tên file, thư mục output và trigger của Spring Batch.
- Có cần migration/schema script chính thức hay tiếp tục dùng JPA `ddl-auto` trong giai đoạn training.

Nếu người dùng/trainer chưa xác nhận các mục này, plan implementation sau phải giữ chúng ở trạng thái `TBD` và không tự đặt business rule thay thế.

## 4. Phạm vi

### In-scope

- Lập bảng API contract cho User/Auth và Student, gồm method, path, request, response, status code, authentication requirement và error behavior.
- Đối chiếu contract với controller, DTO, response wrapper, exception handler và integration test hiện có.
- Cập nhật các tài liệu application và summary liên quan để loại bỏ mâu thuẫn đã xác định, đặc biệt:
  - JWT stateless thay cho auth mechanism chưa xác định.
  - Endpoint `/api/v1/...` thực tế.
  - Student code unique và format 10 ký tự.
  - `LocalDate`/ngày sinh theo application contract.
- Ghi nhận `RestResponse`/`FormatRestResponse` là response envelope chuẩn cho cả response thường và page response.
- Ghi nhận page metadata nằm trong `RestResponse.data` theo `ResStudentPageDTO`.
- Ghi nhận format ngày phía UI là `dd-mm-yyy` theo chỉ dẫn hiện tại.
- Ghi nhận auth state contract: token và user summary trong `sessionStorage`, logout xóa state, `401` redirect Login và `403` không logout.
- Ghi riêng các quyết định chưa được xác nhận vào mục Open Decisions/TBD, không biến chúng thành rule ngầm.
- Bổ sung hoặc điều chỉnh integration test backend chỉ khi cần bảo vệ contract đã được xác nhận, ưu tiên:
  - request validation và response error shape;
  - status code của auth và student endpoint;
  - pagination/filter/sort query contract;
  - invalid/expired bearer token nếu production entry point hiện tại có contract cần kiểm tra.
- Tạo tài liệu input rõ ràng cho Plan 011 về FE test/coverage và các plan FE API tiếp theo.

### Out-of-scope

- Không triển khai frontend API service, auth store, route guard hoặc UI workflow.
- Không triển khai Spring Batch, CSV writer, scheduler hoặc endpoint trigger.
- Không tự quyết định score range, CSV shape/path/trigger, migration strategy hoặc refresh-token/revocation behavior khi chưa được xác nhận.
- Không thêm Role, Permission, OAuth2/OIDC, refresh token hoặc framework mới.
- Không đổi endpoint, request DTO, response DTO, HTTP status hoặc security behavior production nếu chưa có quyết định và test làm căn cứ.
- Không thêm migration tool hoặc thay đổi database schema chỉ để giải quyết các mục còn `TBD`.

## 5. Phương án triển khai

1. Đọc implementation và test hiện tại, lập ma trận `documented contract -> implemented behavior -> test evidence`.
2. Phân loại mỗi quyết định thành:
   - `Confirmed`: có bằng chứng từ user decision hoặc implementation/test đã được chấp nhận.
   - `Needs confirmation`: ảnh hưởng đến consumer nhưng chưa có chỉ dẫn đủ rõ.
   - `TBD`: cố ý chưa quyết định và phải giữ nguyên.
3. Chuẩn hóa API contract theo behavior đã được xác nhận, giữ nguyên response wrapper hiện tại nếu không có yêu cầu thay đổi.
4. Chỉ sửa implementation khi phát hiện lỗi contract rõ ràng hoặc khi người dùng xác nhận thay đổi; mọi thay đổi production phải được tách khỏi phần cập nhật tài liệu.
5. Bổ sung test contract nhỏ, deterministic và đi qua HTTP boundary; không lặp lại các case đã được Plan 007/008 bao phủ nếu không có giá trị regression mới.
6. Cập nhật application docs, plan summaries và Dev Note theo kết quả thực tế.

## 6. Phạm vi mã nguồn và tài liệu dự kiến

### Có thể cập nhật

- `document/application-doc/v1/ApplicationContext.md`.
- `document/application-doc/v1/modules/UserModule.md`.
- `document/application-doc/v1/modules/StudentModule.md`.
- `document/application-doc/v1/DataStructure.md`.
- `document/dev-impl-plan/be/BE_DEV_PLAN_SUMMARY.md`.
- `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md`.
- Backend controller/DTO/error handler chỉ khi đối chiếu contract hoặc test chứng minh cần thiết.

### Có thể tạo mới

- Tài liệu API contract trong `document/application-doc/v1/` nếu contract hiện tại chưa có nơi phù hợp.
- Test backend contract trong package test tương ứng nếu coverage boundary còn thiếu.
- `document/dev-note/be/010-api-contract-and-tbd-resolution-2026-08-18.md` sau implementation.

### Không dự kiến thay đổi

- Frontend production code trong `FE/src/**`.
- `BE/BaiTap-RS/build.gradle.kts`, trừ trường hợp dependency test bắt buộc còn thiếu và được phê duyệt riêng.
- Database migration/schema production.
- Postman collection, trừ khi API contract thay đổi và người dùng yêu cầu đồng bộ artifact này.

## 7. API contract baseline

### User/Auth

| Method | Path | Auth | Expected success |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public | `201 Created`, user summary, không có password |
| `POST` | `/api/v1/auth/login` | Public | `200 OK`, access token và user summary |
| `GET` | `/api/v1/auth/account` | Bearer JWT | `200 OK`, current user summary |
| `POST` | `/api/v1/auth/logout` | Bearer JWT | `204 No Content` |

### Student

| Method | Path | Auth | Expected success |
|---|---|---|---|
| `GET` | `/api/v1/students` | Authenticated | `200 OK`, page data |
| `POST` | `/api/v1/students` | Authenticated | `201 Created`, student aggregate |
| `PUT` | `/api/v1/students/{studentId}` | Authenticated | `200 OK`, updated student aggregate |
| `DELETE` | `/api/v1/students/{studentId}` | Authenticated | `204 No Content` |
| `POST` | `/api/v1/students/code` | Authenticated | `200 OK`, unique generated code |

Baseline này là contract làm việc theo implementation hiện tại; chỉ trở thành contract chính thức sau khi các mục cần xác nhận được chốt.

## 8. Test plan

### Case cần bảo vệ

- Public/authenticated access đúng theo từng endpoint.
- Request DTO validation và error field mapping.
- Auth response không chứa password hoặc password hash.
- Student page metadata, default size, filter AND semantics và sort allow-list.
- Student code format/uniqueness và mutation status code.
- Error response có cùng shape giữa validation error và business error thông qua `RestResponse`.
- Bearer token không hợp lệ hoặc hết hạn có behavior được tài liệu hóa; nếu behavior hiện tại chưa ổn định thì ghi blocker thay vì tự đổi security.

### Nguyên tắc test

- Ưu tiên dùng integration test MockMvc/H2 hiện có.
- Không mock service/repository khi mục tiêu là kiểm tra contract HTTP.
- Không kiểm tra chi tiết implementation nội bộ nếu behavior HTTP đã đủ bảo vệ.
- Không đặt coverage threshold mới trong plan này nếu project chưa có quyết định chung về threshold.

## 9. Validation dự kiến

Chạy từ `BE/BaiTap-RS`:

```bash
./gradlew test
./gradlew checkstyleMain checkstyleTest
./gradlew pmdMain pmdTest
./gradlew build
```

Nếu có thay đổi tài liệu API hoặc Postman theo yêu cầu được duyệt, kiểm tra thêm JSON/schema/consistency tương ứng. Đọc JaCoCo report để xác nhận boundary test đã tăng đúng phần bị tác động; không xem coverage tăng là thay thế cho contract assertion.

## 10. Rủi ro và giảm thiểu

- Tài liệu hiện có mâu thuẫn với implementation ở auth mechanism, endpoint và một số schema decision:
  - Lập ma trận đối chiếu trước khi sửa; không cập nhật im lặng phần chưa được xác nhận.
- Response error hiện phụ thuộc `GlobalExceptionHandler` và security filter:
  - Ghi behavior thực tế bằng integration test; tách production security fix thành scope riêng nếu cần.
- H2 không đại diện hoàn toàn cho MySQL:
  - Không dùng plan này để kết luận migration hoặc dialect production đã đúng.
- Batch requirement còn thiếu output shape/path/trigger:
  - Chặn việc viết batch plan chi tiết cho tới khi các input này được xác nhận.
- Frontend có thể phụ thuộc vào field name chưa ổn định:
  - FE API plan chỉ bắt đầu sau khi baseline contract và error mapping được chốt.

## 11. Output dự kiến

- Một API contract baseline nhất quán cho User/Auth và Student.
- Danh sách confirmed decisions, unresolved TBD và blocker được ghi rõ.
- Tài liệu application không còn mô tả auth/API đã lỗi thời so với behavior được chấp nhận.
- Integration tests hoặc test updates bảo vệ các boundary được xác nhận.
- Input rõ ràng cho Plan 011: FE test/coverage foundation.
- Dev Note ghi nhận validation và mọi quyết định chưa thể hoàn tất sau implementation.

## 12. Trạng thái

- Status: Approved by user on 2026-08-18.
- Người dùng đã phê duyệt Plan 010 qua tin nhắn agent ngày 2026-08-18.
