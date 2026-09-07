# Developer Plan 017: Student Backend Input Validation

## 1. Mục tiêu

- Bảo vệ toàn bộ input của Student API bằng validation backend nhất quán.
- Bổ sung rule `averageScore` chỉ nhận giá trị từ `0` đến `10`, vẫn cho phép `null`
  theo schema hiện có.
- Trả validation error theo `RestResponse` chuẩn thay vì để input HTTP không hợp lệ đi
  vào service/persistence.

## 2. Requirement và hiện trạng

- Yêu cầu người dùng ngày 2026-08-19: thêm validation `averageScore` range `0–10` và
  validation BE cho tất cả input field Student.
- `StudentController` đã dùng `@Valid` cho POST/PUT; Create/Update DTO hiện đã kiểm
  tra Student code, name và address, nhưng chưa validate Birthday/averageScore.
- `StudentService` đã bảo vệ duplicate Student code và page/size âm; `StudentSortResolver`
  đã allow-list sort field/direction.
- Schema/tài liệu đã xác định Birthday, Address và Average score là nullable; không
  được biến các trường này thành bắt buộc trong plan này.

## 3. Phạm vi

### In-scope

- Request body Create/Update:
  - Student code: required, đúng `STU` + 7 chữ số, uniqueness tiếp tục do service
    kiểm tra.
  - Student name: required, tối đa 35 ký tự.
  - Birthday: nếu có phải là ISO `yyyy-MM-dd` hợp lệ và không ở tương lai.
  - Address: optional, tối đa 255 ký tự.
  - Average score: optional; nếu có phải nằm trong `[0, 10]`.
- Query input của `GET /api/v1/students`: giới hạn Student code tối đa 10, Student
  name tối đa 35, page/size không âm; giữ `size=0` là page size mặc định 10 và giữ
  allow-list sort hiện có.
- Path variable `studentId` của detail/update/delete phải là số dương.
- Bổ sung global error handling cần thiết để mọi Bean Validation error trên request
  body, query và path đều dùng envelope lỗi hiện có.
- Regression/integration tests, JaCoCo và quality gates backend.

### Out-of-scope

- Không thay endpoint, response success, schema/migration, `Double` sang `BigDecimal`,
  score decimal scale, Student UI, auth/JWT, CSV hoặc Postman collection.
- Không thêm range tuổi tối thiểu/tối đa, không bắt buộc Birthday/Address/Average score
  non-null, không thay partial-match semantics của Student code/name search.

## 4. Thiết kế đề xuất

```text
HTTP input
  -> Bean Validation at controller boundary
  -> 400 RestResponse with field/message list
  -> StudentService only receives syntactically valid request
  -> existing service rules: duplicate code, sort allow-list, not-found
```

- Dùng Jakarta Bean Validation trên DTO thay vì lặp validation thủ công ở service.
  `@DecimalMin("0.0")` và `@DecimalMax("10.0")` trên `Double averageScore` tự bỏ qua
  `null`, nên khớp schema nullable.
- Dùng `@PastOrPresent` cho Birthday nếu field có giá trị. Jackson/`LocalDate` và
  `@DateTimeFormat` tiếp tục từ chối date format không hợp lệ.
- Đặt `@Valid` cho query DTO và cơ chế validation controller phù hợp cho path variable;
  global handler sẽ map lỗi parameter/query vào `RestResponse` 400 như body validation.
- Không dùng Lombok mới hoặc thay entity/service architecture; service duplicate-code và
  sort validation tiếp tục là business rules riêng.

## 5. Phạm vi file dự kiến

| Path | Thao tác | Mục đích |
|---|---|---|
| `BE/BaiTap-RS/src/main/java/.../student/domain/DTOs/requests/ReqCreateStudentDTO.java` | Sửa | Birthday và score constraints, giữ code/name/address constraints. |
| `BE/BaiTap-RS/src/main/java/.../student/domain/DTOs/requests/ReqUpdateStudentDTO.java` | Sửa | Birthday và score constraints, giữ name/address constraints. |
| `BE/BaiTap-RS/src/main/java/.../student/domain/DTOs/requests/ReqFetchStudentDTO.java` | Sửa | Query length/page/size validation, không đổi search semantics. |
| `BE/BaiTap-RS/src/main/java/.../student/controller/StudentController.java` | Sửa | Kích hoạt validation query/path input, giữ controller thin. |
| `BE/BaiTap-RS/src/main/java/.../common/error/ValidationExceptionHandler.java` | Tạo | Chuẩn hoá validation error body/query/path/date thành envelope 400. |
| `BE/BaiTap-RS/src/test/java/.../student/controller/StudentControllerIntegrationTest.java` | Sửa | Create/Update score boundaries, invalid body, positive path validation. |
| `BE/BaiTap-RS/src/test/java/.../student/controller/StudentFetchValidationIntegrationTest.java` | Tạo nếu cần | Query validation and envelope contract. |
| `document/application-doc/v1/modules/StudentModule.md` | Sửa | Chốt range Average score `0–10` là source of truth. |
| `document/application-doc/v1/ApplicationContext.md` | Sửa | Bỏ range Average score khỏi open decisions đã được xác nhận. |
| `document/dev-note/be/student/017-student-input-validation-2026-08-19.md` | Tạo sau implementation | Ghi thay đổi và evidence validation thực tế. |
| Các Dev Plan/Dev Note summary | Sửa | Đồng bộ approval/completion. |

## 6. Test plan

### Request body

- Create/Update chấp nhận `averageScore` `0`, `10`, `null`.
- Create/Update từ chối `-0.01` và `10.01` với HTTP 400, field `averageScore`, và
  không lưu/thay đổi aggregate.
- Regression: Create từ chối code trống/sai format, name trống/quá 35, address quá
  255; Update từ chối name/address sai; Birthday future bị từ chối, Birthday `null`
  được chấp nhận.
- Date JSON malformed vẫn trả HTTP 400 theo Spring error boundary; test response
  envelope nếu handler hiện có cần mở rộng cho case này.

### Query/path and regression

- Fetch chấp nhận page `0`, size `0`/`10`, search filters rỗng và partial search hiện
  có; từ chối page/size âm và code/name vượt length.
- Detail/Update/Delete từ chối `studentId <= 0` trước service lookup; id dương missing
  vẫn trả 404 như hiện có.
- Duplicate code vẫn trả 409; invalid sort/direction vẫn trả 400 từ service allow-list.

### Isolation và commands

- Dùng MockMvc + H2 integration test hiện có cho controller/validation/envelope;
  Mockito service tests chỉ cập nhật nếu validation thay đổi business service behavior
  (dự kiến không cần).
- Đọc JaCoCo HTML/XML sau test để xác nhận branches validation mới được cover; không
  đặt coverage threshold mới.
- Chạy từ `BE/BaiTap-RS/`:

```text
./gradlew test
./gradlew jacocoTestReport
./gradlew check
./gradlew pmdMain pmdTest
```

## 7. Rủi ro và giảm thiểu

| Rủi ro | Giảm thiểu |
|---|---|
| Client hiện gửi Birthday tương lai. | Trả 400 rõ field/message; FE giữ lỗi API hiện có để hiển thị. |
| Constraint path/query dùng exception khác body validation. | Test từng boundary và thêm handler để giữ RestResponse thống nhất. |
| Double có sai số representation. | Chỉ áp dụng inclusive decimal boundary `0.0`/`10.0`; không suy diễn scale. |
| Search code partial bị vô tình ép exact format. | Chỉ `@Size(max=10)` tại query, giữ partial LIKE behavior. |

## 8. Output dự kiến

- API Student không nhận averageScore ngoài `0–10`.
- Create/Update/List/path input có validation tương ứng với contract/schema, trả error
  400 nhất quán; duplicate/not-found/sort rules cũ không regression.
- Test, JaCoCo, Checkstyle và PMD có evidence thực tế trong Dev Note.

## 9. Approval status

- Trạng thái: Approved by user on 2026-08-19.
- Quyết định đã được phê duyệt: Birthday có giá trị phải là hôm nay hoặc quá khứ
  (`@PastOrPresent`); các field Birthday/Address/Average score vẫn optional.
