# Developer Plan: Student Integration Test

## 1. Mục tiêu

- Bổ sung integration test cho Student API trong `BE/BaiTap-RS`.
- Kiểm tra luồng HTTP request/response, validation, security test context và persistence với H2.
- Giảm khoảng trống coverage hiện tại: `StudentService` đã có unit test, nhưng `StudentController`, request binding, response wrapper và JPA integration chưa có test riêng.

## 2. Requirement liên quan

- Module: `document/application-doc/modules/StudentModule.md`.
- Backend workflow: `.codex/workflows/WORKFLOW-BACKEND.md`.
- Student backend plan đã approved: `document/dev-impl-plan/be/student/005-student-module-backend-2026-08-17.md`.
- Dev Note hiện tại ghi nhận controller coverage còn thấp và có thể bổ sung MVC/controller tests sau.

Business rule cần kiểm tra:

- Student API xem `student` và `student_info` như một aggregate.
- Create student cần tạo cả `Student` và `StudentInfo` trong một operation.
- `studentCode` required, format `STU[0-9]{7}`, unique.
- `studentName` required.
- Update không thay đổi `studentCode`.
- Delete xóa aggregate student và student info.
- Fetch list hỗ trợ pagination, search/filter và response page metadata.

## 3. Phạm vi

### In-scope

- Tạo integration test mới cho endpoint `/api/v1/students`.
- Dùng Spring Boot test context với H2 in-memory:
  - `spring.datasource.url=jdbc:h2:mem:...;MODE=MySQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=USER`
  - `spring.jpa.hibernate.ddl-auto=create-drop`
- Dùng `MockMvc` để gọi API qua HTTP layer.
- Dùng `spring-security-test`, vì Student endpoint đang yêu cầu authenticated request.
- Assert response wrapper `RestResponse` gồm `statusCode`, `message`, `data`, `error` khi phù hợp.
- Assert database state bằng `StudentRepository`.

### Out-of-scope

- Không sửa production code nếu integration test không phát hiện bug bắt buộc.
- Không đổi `SecurityConfiguration.java`; file này đang có thay đổi sẵn trong worktree và không thuộc scope.
- Không thêm dependency/build config trừ khi Gradle dependency hiện tại thiếu bắt buộc.
- Không test endpoint `/api/v1/students/code` trong vòng đầu nếu cần deterministic random setup phức tạp; service unit test hiện đã phủ code generation.
- Không thêm Testcontainers hoặc kết nối MySQL thật.

## 4. Kiến trúc hiện tại

- `StudentController` expose:
  - `GET /api/v1/students`
  - `POST /api/v1/students`
  - `PUT /api/v1/students/{studentId}`
  - `DELETE /api/v1/students/{studentId}`
  - `POST /api/v1/students/code`
- `StudentService` chứa business rules, transaction boundary và DTO mapping.
- `StudentRepository` extends `JpaRepository<Student, Long>` và `JpaSpecificationExecutor<Student>`.
- `Student` cascade/orphan removal tới `StudentInfo`.
- `FormatRestResponse` wrap success response thành `RestResponse`.
- `GlobalExceptionHandler` wrap `AppException` và validation error thành `RestResponse`.
- `SecurityConfiguration` yêu cầu authenticated request cho Student API.

## 5. Phương án triển khai

- Tạo class `StudentControllerIntegrationTest`.
- Annotation dự kiến:
  - `@SpringBootTest(properties = {...H2...})`
  - `@AutoConfigureMockMvc`
  - `@WithMockUser`
- Inject:
  - `MockMvc`
  - `ObjectMapper`
  - `StudentRepository`
- Dùng `@BeforeEach` để `studentRepository.deleteAll()` giúp cô lập dữ liệu.
- Tạo helper fixture:
  - create student payload JSON.
  - persisted student aggregate qua entity/repository.
- Dùng JSONPath để assert API contract.
- Dùng repository để assert state change trong database.

Trade-off:

- `@SpringBootTest` chậm hơn `@WebMvcTest`, nhưng phù hợp hơn cho integration test vì cần controller + service + repository + H2.
- H2 không thay thế hoàn toàn MySQL, nhưng đã đủ để verify JPA mapping và flow backend trong test local.

## 6. Phạm vi mã nguồn dự kiến

- Tạo mới:
  - `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/student/controller/StudentControllerIntegrationTest.java`
  - Mục đích: integration test cho Student REST API.

- Cập nhật nếu cần:
  - `document/dev-note/be/student/007-student-integration-test-2026-08-18.md`
  - Mục đích: ghi lại thay đổi thực tế, validation và coverage sau khi triển khai.

- Không dự kiến sửa:
  - `BE/BaiTap-RS/src/main/java/...`
  - `BE/BaiTap-RS/build.gradle.kts`
  - `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/config/SecurityConfiguration.java`

## 7. API / Database / Integration

Endpoint dự kiến test:

- `POST /api/v1/students`
- `GET /api/v1/students`
- `PUT /api/v1/students/{studentId}`
- `DELETE /api/v1/students/{studentId}`

Database:

- Dùng schema Hibernate tạo từ JPA entity trên H2.
- Test verify relation `student` - `student_info` qua entity aggregate.
- Không thêm migration/schema file.

Security:

- Request Student API được gọi trong authenticated test context bằng `@WithMockUser`.
- Không thay đổi production security rule.

## 8. Test plan

### Class/method cần test

- `StudentController.createStudent(...)`
- `StudentController.fetchStudents(...)`
- `StudentController.updateStudent(...)`
- `StudentController.deleteStudent(...)`
- Integration path qua `StudentService`, `StudentRepository`, `Student`, `StudentInfo`, `FormatRestResponse`, `GlobalExceptionHandler`.

### Case thành công

- Create:
  - Input hợp lệ.
  - Expect HTTP `201`.
  - Assert `data.studentCode`, `data.studentName`, `data.dateOfBirth`, `data.address`, `data.averageScore`.
  - Assert DB có đúng `Student` và linked `StudentInfo`.
- Fetch:
  - Seed nhiều student.
  - Gọi với `page`, `size`, filter/sort cơ bản.
  - Assert `data.content`, `data.page`, `data.size`, `data.totalElements`, `data.totalPages`.
- Update:
  - Seed student.
  - Update name/date/address/score.
  - Expect HTTP `200`.
  - Assert `studentCode` giữ nguyên.
  - Assert DB đã cập nhật `StudentInfo`.
- Delete:
  - Seed student.
  - Expect HTTP `204`.
  - Assert DB không còn student.

### Case lỗi / validation

- Create duplicate `studentCode`:
  - Expect HTTP `409`.
  - Assert `message` là `Mã sinh viên đã tồn tại`.
- Create invalid `studentCode`:
  - Expect HTTP `400`.
  - Assert validation message có field `studentCode`.
- Update missing id:
  - Expect HTTP `404`.
  - Assert message `Không tìm thấy sinh viên`.
- Delete missing id:
  - Expect HTTP `404`.

### Boundary / null / empty

- Empty `studentName` bị reject bởi `@NotBlank`.
- `size=0` khi fetch được service resolve về default page size 10.
- Optional fields `dateOfBirth`, `address`, `averageScore` có thể null nếu request hợp lệ.

### Dependency mock/fake

- Không mock `StudentService` hoặc `StudentRepository`.
- Không mock `StudentCodeGenerator` trong vòng đầu vì không test `/code`.
- Dùng H2 in-memory thay database thật.

### Assertion

- Output:
  - HTTP status.
  - `RestResponse.statusCode`, `message`, `error`, `data`.
- State change:
  - Repository count.
  - Student aggregate fields.
  - Cascade delete behavior.
- Interaction:
  - Không verify mock interaction vì integration test ưu tiên behavior thực.

### Regression

- Bảo vệ contract response wrapper của Student API.
- Bảo vệ validation DTO qua HTTP binding.
- Bảo vệ service transaction + JPA mapping khi create/update/delete.
- Bảo vệ filter/sort/page metadata cơ bản.

## 9. Lệnh validation

Chạy từ thư mục `BE/BaiTap-RS`:

```bash
./gradlew test
./gradlew checkstyleMain checkstyleTest
./gradlew pmdMain pmdTest
./gradlew build
```

JaCoCo:

- HTML: `BE/BaiTap-RS/build/reports/jacoco/test/html/index.html`
- XML: `BE/BaiTap-RS/build/reports/jacoco/test/jacocoTestReport.xml`

## 10. Rủi ro và giảm thiểu

- Security filter có thể chặn request:
  - Giảm thiểu bằng `spring-security-test` và authenticated test context.
- H2 khác MySQL ở một số behavior:
  - Giảm thiểu bằng MySQL mode, `DATABASE_TO_UPPER=false`, và chỉ test behavior JPA/API không phụ thuộc dialect đặc biệt.
- Response `204 No Content` có thể không đi qua body wrapper:
  - Assert status và DB state thay vì body.
- Test integration có thể chậm hơn unit test:
  - Giới hạn số case vào luồng quan trọng nhất, fixture nhỏ và cô lập bằng `deleteAll`.

## 11. Output dự kiến

- Có file integration test mới cho Student API.
- Test pass với H2 và MockMvc.
- Coverage `StudentController`, response wrapper path và JPA integration của Student module tăng so với hiện tại.
- Có Dev Note ghi lại thực tế triển khai và validation sau khi code xong.

## 12. Trạng thái

- Status: Approved.
- Người dùng đã phê duyệt Plan 007 trong phiên làm việc ngày 2026-08-18.
