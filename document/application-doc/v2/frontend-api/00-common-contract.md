# Frontend API v2 — Common Contract

[← Back to `FrontendApiGuide.md`](../FrontendApiGuide.md)

## Mục đích

Tài liệu này là **API contract index dành cho Frontend v2**.

Mục tiêu:

- giúp FE tích hợp API mà không phải đọc toàn bộ controller/DTO Java cho mỗi task;
- phân biệt rõ **Requirement v2** với **wire contract đang được backend triển khai**;
- ghi lại các endpoint legacy vẫn đang được giữ để tương thích;
- chỉ ra các contract gap/blocker mà FE **không được tự suy diễn**;
- cung cấp các enum và response shape quan trọng để TypeScript bám đúng dữ liệu thực tế.

Tài liệu này **không thay thế Requirement Baseline**.

Thứ tự ưu tiên khi làm FE:

1. requirement/CR đã được phê duyệt;
2. module requirement v2;
3. contract API đang triển khai được ghi trong tài liệu này;
4. controller/request/response DTO hiện tại;
5. suy luận của người triển khai.

Nếu Requirement v2 yêu cầu một hành vi nhưng backend chưa có contract tương ứng, FE phải ghi `BLOCKED` hoặc xử lý theo plan/CR được phê duyệt; không tự tạo endpoint/field/enum.

## Nguồn đối chiếu

Application documentation:

- [`ApplicationContext.md`](../ApplicationContext.md)
- [`RequirementBaseline.md`](../RequirementBaseline.md)
- [`modules/`](../modules/)
- [`change-request/`](../change-request/)
- [`data-model/`](../data-model/)

Backend contract:

```text
BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/
```

Các controller chính được đối chiếu:

```text
user/controller/AuthController.java
student/controller/StudentController.java
student/controller/StudentV3Controller.java
academic/controller/*
teacher/controller/TeacherController.java
assignment/controller/AssignmentController.java
enrollment/controller/EnrollmentController.java
calendar/controller/CalendarController.java
attendance/controller/*
scorebook/controller/*
```

## Quy ước transport chung

### Base URL

Frontend lấy base URL từ cấu hình public của Vite và nối với path API.

Không hard-code origin backend trong component.

Ví dụ:

```text
VITE_API_BASE_URL=http://localhost:8080
```

### Authentication

JWT stateless.

Các endpoint public hiện tại:

```text
POST /api/v1/auth/login
POST /api/v1/auth/register
```

Các API còn lại yêu cầu authentication trừ các endpoint infrastructure được Security Configuration permit riêng.

Protected request gửi:

```http
Authorization: Bearer <access-token>
```

### Success envelope

Đối với JSON success thông thường, backend `FormatRestResponse` bọc body:

```json
{
  "statusCode": 200,
  "message": "Lấy danh sách ...",
  "data": {}
}
```

TypeScript nền:

```ts
export interface RestResponse<T> {
  statusCode: number
  error?: string
  message?: string | string[]
  data: T
}
```

Lưu ý:

- response `204 No Content` không có JSON body hữu dụng;
- `byte[]`, `Resource`, `String`, Swagger/OpenAPI path được wrapper bỏ qua;
- Student CSV export trả file CSV, không phải `RestResponse`.

### Error envelope

Validation/application error dùng dạng:

```json
{
  "statusCode": 400,
  "error": "Bad Request",
  "message": [
    "field: thông báo validation"
  ],
  "data": null
}
```

`message` có thể là string hoặc list string.

Các semantics FE phải giữ:

- `401`: clear auth session và điều hướng Login;
- `403`: giữ auth session và hiển thị access denied;
- `404`: resource không tồn tại;
- `409`: conflict/ràng buộc nghiệp vụ;
- `400`: validation hoặc request không hợp lệ.

## FE integration rules

### Typed service boundary

Mỗi domain có service typed.

Ví dụ:

```text
services/
├── userApi.ts
├── studentApi.ts
├── academicApi.ts
├── enrollmentApi.ts
├── teacherApi.ts
├── assignmentApi.ts
├── calendarApi.ts
├── attendanceApi.ts
├── scorebookApi.ts
├── transcriptApi.ts
└── calculationApi.ts
```

Đây là gợi ý tổ chức, không yêu cầu phải tạo toàn bộ file trong một plan.

### Date only

`LocalDate`:

```text
yyyy-MM-dd
```

Không dùng `toISOString()` cho Date-only nếu có nguy cơ timezone shift.

### Pagination

Không giả định mọi endpoint dùng page size 10.

Known defaults:

```text
Student v1 list: existing FE uses 10
Score grid: 10
Student attendance history: 10
Class attendance summary: 20
Score change: 10
Retake: 10
Calculation task: 10
Audit log: 10, max 50
```

Spring `Page<T>` responses vẫn nằm trong `RestResponse.data`.

### Background calculation

FE không tính official averages.

Command thay đổi điểm:

```text
save source
→ backend marks transcript IN_PROGRESS
→ calculation task
→ FE reads/polls status
→ FINISH
→ refresh official result
```

### Student identifier

Khi endpoint hỗ trợ by-code, ưu tiên `studentCode` cho human-facing interaction.

Không thay numeric PK/FK bằng code trong FE model một cách giả tạo.

### Warning vs error

Capacity warning hoặc completeness warning có thể là non-blocking theo requirement.

UI phải phân biệt:

```text
warning
blocking validation error
authorization error
background processing state
```

## Quy tắc cập nhật tài liệu này

Khi controller/DTO/enum mà FE sử dụng thay đổi:

1. cập nhật `FrontendApiGuide.md`;
2. cập nhật FE TypeScript type/service;
3. cập nhật tests;
4. nếu thay đổi business contract, cập nhật requirement/CR liên quan;
5. không dùng v1 document để override v2.

Không thêm endpoint planned/TBD vào bảng current API như thể endpoint đã tồn tại.

Nếu cần ghi endpoint tương lai, đặt riêng dưới nhãn:

```text
PLANNED / BLOCKED / NOT IMPLEMENTED
```
