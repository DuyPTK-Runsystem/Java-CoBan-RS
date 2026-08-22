# Developer Plan: Student Module Backend

## 1. Mục tiêu

- Triển khai backend cho Student module trong `BE/BaiTap-RS`.
- Hỗ trợ lấy danh sách sinh viên, filter, sort, pagination, thêm, sửa, xóa và generate student code.
- Quản lý `student` và `student_info` như một aggregate nghiệp vụ, không bắt frontend thao tác hai bảng riêng lẻ.
- Giữ đúng style hiện tại của project: package `com.JavaTraining.BaiTap_RS`, DTO trong `domain/DTOs`, response wrapper qua `FormatRestResponse`, lỗi nghiệp vụ qua `AppException`.

## 2. Requirement liên quan

- Tài liệu:
  - `document/application-doc/v1/ApplicationContext.md`
  - `document/application-doc/v1/modules/StudentModule.md`
  - `document/application-doc/v1/DataStructure.md`
  - `document/dev-impl-plan/be/BE_DEV_PLAN_SUMMARY.md`
- Scope chức năng:
  - Student list.
  - Filter theo student code, student name, birthday.
  - Sort theo các cột nghiệp vụ được hỗ trợ.
  - Pagination mặc định 10 records/page.
  - Add student.
  - Update student.
  - Delete student.
  - Generate student code.

## 3. Quyết định đã chốt

- `student_code` có đúng 10 ký tự:
  - Prefix cố định: `STU`.
  - Phần còn lại: 7 chữ số random.
  - Ví dụ: `STU1234567`.
- `student_code` phải unique ở Entity level bằng `@Column(unique = true)` hoặc `@Table(uniqueConstraints = ...)`.
- DOB dùng `LocalDate` trong Java và định hướng DB type là `DATE`.
- `student_info` dùng `info_id` làm primary key.
- `student_info.student_id` là foreign key unique tới `student.student_id`.
- Delete dùng entity relationship cascade/orphan removal từ `Student` sang `StudentInfo`, kèm transaction boundary trong service.
- Student IDs dùng Java `Long`.
- `StudentRepository` dùng `JpaSpecificationExecutor<Student>` để implement fetch/filter động.

## 4. Phạm vi

### In-scope

- Tạo Student module backend:
  - Entity mapping cho `student` và `student_info`.
  - Repository cho `Student` và `StudentInfo`.
  - DTO request/response cho create, update, fetch/list và generate code nếu cần.
  - Service chứa business rules, transaction boundaries, DTO mapping.
  - Controller REST API mỏng, validate request và delegate service.
- Implement server-side generate code để bảo đảm uniqueness.
- Implement fetch/filter với AND semantics:
  - Empty criteria nghĩa là không filter theo field đó.
  - Nhiều criteria cùng có giá trị thì dùng AND.
- Implement sort allow-list:
  - `studentCode`
  - `studentName`
  - `dateOfBirth`
  - `address`
  - `averageScore`
- Implement pagination page size mặc định 10 cho Student list.
- Viết unit test tập trung cho `StudentService`.
- Chạy validation backend bắt buộc sau implementation.
- Tạo Dev Note sau khi implementation hoàn tất.

### Out-of-scope

- Không implement frontend Vue/PrimeVue trong plan này.
- Không implement CSV export/Spring Batch trong plan này.
- Không tạo Role/Permission hoặc logic authorization theo role.
- Không thêm Flyway/Liquibase hoặc migration framework mới.
- Không tự thêm business rule chưa có trong tài liệu, ví dụ không ép `averageScore` trong khoảng `0..10`.
- Không cập nhật Postman collection trừ khi người dùng yêu cầu riêng.

## 5. Thiết kế module

```text
student/
├── controller/
│   └── StudentController.java
├── service/
│   └── StudentService.java
├── repository/
│   ├── StudentRepository.java
│   └── StudentInfoRepository.java
└── domain/
    ├── entity/
    │   ├── Student.java
    │   └── StudentInfo.java
    └── DTOs/
        ├── requests/
        │   ├── ReqCreateStudentDTO.java
        │   ├── ReqUpdateStudentDTO.java
        │   └── ReqFetchStudentDTO.java
        └── response/
            ├── ResStudentDTO.java
            ├── ResStudentCodeDTO.java
            └── ResStudentPageDTO.java
```

`ReqFetchStudentDTO` được dùng cho query parameters của API lấy danh sách. Không expose JPA entity qua controller.

## 6. Entity / Database

### Student

- Table: `student`
- Fields:
  - `id` -> `student_id`, Java `Long`.
  - `studentName` -> `student_name`, max 20, not null.
  - `studentCode` -> `student_code`, length 10, not null, unique.
  - `studentInfo` -> one-to-one relationship.
- Không cho API update `studentCode` trong edit mode.

### StudentInfo

- Table: `student_info`
- Fields:
  - `id` -> `info_id`, Java `Long`.
  - `student` -> FK unique `student_id`.
  - `dateOfBirth` -> `date_of_birth`, Java `LocalDate`.
  - `address` -> `address`, max 255, nullable.
  - `averageScore` -> `average_score`, `Double`, nullable.
- Owning side là `StudentInfo` vì FK nằm ở `student_info`.

### Delete

- `Student` quản lý `StudentInfo` bằng one-to-one cascade/orphan removal.
- `StudentService.deleteStudent(...)` dùng `@Transactional`.

## 7. API dự kiến

### Fetch/list students

```http
GET /api/v1/students
```

Query params:

- `page`, zero-based.
- `size`, default 10.
- `studentCode`.
- `studentName`.
- `birthday` theo JSON/API format `yyyy-MM-dd`.
- `sort`, format `<field>,<asc|desc>`.

Response data dùng custom page DTO để ổn định contract:

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0
}
```

### Create student

```http
POST /api/v1/students
```

Request:

```json
{
  "studentCode": "STU1234567",
  "studentName": "Nguyen Van A",
  "dateOfBirth": "2012-04-22",
  "address": "Ho Chi Minh City",
  "averageScore": 8.5
}
```

### Update student

```http
PUT /api/v1/students/{studentId}
```

- `studentId` lấy từ path.
- `studentCode` không update từ request.
- Service phải reject/not-found nếu `studentId` không tồn tại.

### Delete student

```http
DELETE /api/v1/students/{studentId}
```

### Generate student code

```http
POST /api/v1/students/code
```

Response:

```json
{
  "studentCode": "STU1234567"
}
```

## 8. Business rules

- `studentCode` required, length exactly 10, pattern `STU[0-9]{7}`.
- `studentCode` unique.
- `studentName` required, max 20.
- `address` max 255, nullable.
- `dateOfBirth` must be a valid date if supplied.
- `averageScore` numeric, nullable; no min/max rule until requirement is confirmed.
- Filtering by multiple populated criteria uses AND.
- Unsupported sort field returns `BAD_REQUEST`.
- Unsupported sort direction returns `BAD_REQUEST`.
- Create/update/delete across `student` and `student_info` must be transactional.

## 9. Unit Test Plan

### Class chính

- `StudentServiceTest`

### Dependencies mock

- `StudentRepository`
  - Extends `JpaRepository<Student, Long>`.
  - Extends `JpaSpecificationExecutor<Student>`.
  - Provides batch lookup for existing student codes from a candidate collection.
- `StudentInfoRepository` nếu service gọi trực tiếp.
- Không dùng database thật trong unit test service.

### Test cases

- `fetchStudentsReturnsPagedResultWithDefaultSizeTen`
  - Verify page size 10 khi request không truyền size.
  - Verify response page DTO map đúng content và metadata.
- `fetchStudentsRejectsUnsupportedSortField`
  - Input sort field ngoài allow-list.
  - Expect `AppException` `BAD_REQUEST`.
- `fetchStudentsRejectsUnsupportedSortDirection`
  - Input sort direction không phải `asc`/`desc`.
  - Expect `AppException` `BAD_REQUEST`.
- `createStudentCreatesStudentAndInfoWhenCodeIsUnique`
  - Mock duplicate check false.
  - Capture saved entity.
  - Assert `studentCode`, `studentName`, `dateOfBirth`, `address`, `averageScore`.
- `createStudentRejectsDuplicateStudentCode`
  - Mock duplicate check true.
  - Expect `AppException` `CONFLICT`.
  - Verify save is not called.
- `updateStudentUpdatesMutableFieldsOnly`
  - Existing student has code `STU0000001`.
  - Request updates name/DOB/address/score.
  - Assert code remains unchanged.
- `updateStudentRejectsMissingStudent`
  - Mock repository returns empty.
  - Expect `AppException` `NOT_FOUND`.
- `deleteStudentDeletesExistingStudent`
  - Mock existing student.
  - Verify repository delete call.
- `deleteStudentRejectsMissingStudent`
  - Mock repository returns empty.
  - Expect `AppException` `NOT_FOUND`.
- `generateStudentCodeReturnsFirstCandidateNotFoundInDbBatch`
  - Generate 20 candidate codes.
  - Query existing codes from DB once for the batch.
  - Return the first candidate not currently in DB.
- `generateStudentCodeRetriesBatchWhenAllCandidatesExist`
  - If all candidates in a batch exist, generate the next batch.
  - Cap retry by batch count, not by per-code DB calls.

### Validation / boundary

- DTO validation hoặc controller test sẽ được thêm nếu implementation có nhiều validation annotation cần kiểm tra ngoài service:
  - blank `studentName`.
  - `studentName` length > 20.
  - invalid `studentCode` format.
  - `address` length > 255.
  - invalid date format at controller binding level.

### Lệnh chạy

```text
./gradlew test
./gradlew build
./gradlew checkstyleMain checkstyleTest
./gradlew pmdMain pmdTest
```

### JaCoCo

- Sau `./gradlew test`, đọc report tại:

```text
BE/BaiTap-RS/build/reports/jacoco/test/html/index.html
BE/BaiTap-RS/build/reports/jacoco/test/jacocoTestReport.xml
```

- Báo coverage thực tế của các class Student mới; không tự đặt threshold nếu project chưa yêu cầu.

## 10. Validation sau implementation

- Chạy unit test tập trung.
- Chạy full backend test.
- Chạy build.
- Chạy Checkstyle.
- Chạy PMD.
- Đọc lỗi report nếu có và sửa source trong scope plan cho tới khi pass hoặc báo blocker rõ ràng.
- Tạo Dev Note trong `document/dev-note/be/student/`.
- Cập nhật summary Dev Note tương ứng.

## 11. Rủi ro / điểm cần chú ý

- `student.student_id` trong docs là `INT`, nhưng user đã chốt dùng Java `Long`; entity mapping dùng `Long` và database auto increment.
- Student code random có rủi ro trùng; service phải retry và có giới hạn retry hợp lý để tránh loop vô hạn.
- Filter/sort trên field thuộc `student_info` cần query join hoặc specification rõ ràng.
- `spring.data.web.pageable.one-indexed-parameters=true` đang bật trong config, nhưng StudentModule nói backend có thể zero-based. Controller/service cần tránh nhầm page index với frontend.

## 12. Output dự kiến

- Student module backend compile được.
- API Student hoạt động theo contract đã mô tả.
- `student_code` đúng format, unique ở entity level và được service kiểm tra duplicate.
- DOB dùng `LocalDate`.
- Delete student xóa/kèm xử lý `StudentInfo` nhất quán.
- Unit test và validation backend pass.
- Dev Note ghi nhận thực tế implementation sau khi code.

## 13. Approval status

- Trạng thái: Approved by user via agent on 2026-08-17.
- Đã chốt qua người dùng ngày 2026-08-17:
  - Student code 10 ký tự gồm `STU` + 7 digits, unique ở Entity level.
  - DOB dùng `LocalDate`/DB `DATE`.
  - `student_info.student_id` là FK unique.
  - Delete dùng cascade/orphan removal từ entity relationship.
  - Student IDs dùng Java `Long`.
  - Repository dùng `JpaSpecificationExecutor<Entity>`.
