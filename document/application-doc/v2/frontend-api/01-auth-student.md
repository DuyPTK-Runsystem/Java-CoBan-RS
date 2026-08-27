# Frontend API v2 — Auth and Student

[← Back to `FrontendApiGuide.md`](../FrontendApiGuide.md)

## Auth/User API

### Endpoint

| Method | Path | Auth | Request | Response `data` | Ghi chú FE |
|---|---|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public | `ReqRegisterUserDTO` | `ResUserDTO` | Legacy/self-register nền hiện tại; không phải flow tạo Teacher/Student v2 |
| `POST` | `/api/v1/auth/login` | Public | `ReqLoginUserDTO` | `ResLoginUserDTO` | Trả access token + user summary |
| `GET` | `/api/v1/auth/account` | Authenticated | — | `ResUserDTO` | Account hiện tại |
| `POST` | `/api/v1/auth/logout` | Authenticated | — | `204` | JWT stateless; FE vẫn phải clear local session |

Request login:

```json
{
  "username": "admin",
  "password": "12345678"
}
```

Login response `data`:

```json
{
  "access_token": "<jwt>",
  "user": {
    "id": 1,
    "username": "admin",
    "created_at": "...",
    "updated_at": "...",
    "created_by": "...",
    "updated_by": "..."
  }
}
```

### Blocker: role chưa được expose cho FE

`ResUserDTO` hiện **không có `roles`**.

JWT hiện chứa:

```text
sub
user_id
iat
exp
```

và **không có role claim**.

Vì vậy tại thời điểm tài liệu này:

```text
FE biết authenticated / unauthenticated
FE chưa có contract chính thức để biết ADMIN / ACADEMIC_OFFICE / TEACHER / STUDENT
```

Hệ quả:

- không suy role từ username;
- không suy role từ một API gọi thành công;
- không giả định JWT có role;
- role-aware sidebar/router cần một contract backend/plan riêng.

Backend authorization vẫn là authoritative.

## Student API

### Version map — v1, v2 và v3 cùng tồn tại

Student API hiện **không đi theo mô hình “v3 thay thế hoàn toàn v2, v2 thay thế hoàn toàn v1”**.

Ba generation đang có vai trò khác nhau:

| Generation | Vai trò hiện tại | FE sử dụng khi |
|---|---|---|
| **v1** | Student profile CRUD, lookup, generate code, export | quản lý hồ sơ Student hiện có và compatibility với FE cũ |
| **v2** | Student trong **nghiệp vụ học vụ**: enrollment/history, attendance, scorebook, transcript, recalculation và các flow dùng `studentCode` | Student đã trở thành subject/participant của nghiệp vụ trường học |
| **v3** | Command provisioning mới: tạo `User + role STUDENT + Student + StudentInfo` atomically | giáo vụ/admin tạo học sinh mới kèm tài khoản đăng nhập |

Quan trọng:

```text
v1 != deprecated hoàn toàn
v2 != CRUD replacement của v1
v3 != replacement của toàn bộ v1/v2
```

Một flow FE v2 có thể hợp lệ khi dùng đồng thời cả ba generation.

Ví dụ:

```text
Tạo học sinh + account
POST /api/v3/students
        ↓
Xếp lớp
POST /api/v2/enrollments
        ↓
Tra lịch sử phân lớp
GET /api/v2/students/by-code/{studentCode}/enrollments
        ↓
Cập nhật hồ sơ compatibility hiện tại
PUT /api/v1/students/{studentId}
```

Do đó frontend không được chọn API chỉ dựa trên số version lớn nhất. Phải chọn theo **use case**.

### Student v1 — profile CRUD / compatibility contract

Các endpoint v1 vẫn đang được giữ và vẫn có vai trò thật trong hệ thống hiện tại.

| Method | Path | Auth | Request/Query | Response | Vai trò |
|---|---|---|---|---|---|
| `GET` | `/api/v1/students` | `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER` | `ReqFetchStudentDTO` | `ResStudentPageDTO` | Danh sách/search/page/sort Student profile |
| `GET` | `/api/v1/students/{studentId}` | same | path id | `ResStudentDTO` | Detail profile |
| `GET` | `/api/v1/students/code/{studentCode}` | same | path code | `ResStudentDTO` | Business lookup theo mã |
| `POST` | `/api/v1/students` | same | `ReqCreateStudentDTO` | `201 ResStudentDTO` | Tạo Student profile **không kèm account** |
| `PUT` | `/api/v1/students/{studentId}` | same | `ReqUpdateStudentDTO` | `ResStudentDTO` | Update profile |
| `DELETE` | `/api/v1/students/{studentId}` | same | — | `204` | Legacy hard delete |
| `POST` | `/api/v1/students/code` | same | — | `{ studentCode }` | Sinh mã Student |
| `GET` | `/api/v1/students/export` | same | — | CSV bytes | Export compatibility |

Query list:

```text
studentCode?
studentName?
birthday?     yyyy-MM-dd
page          zero-based
size
sortField?
sortDirection?
```

Response page:

```ts
interface StudentPage {
  content: Student[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
```

#### FE rule cho v1

V1 **không được hiểu là “không được dùng nữa”**.

Trong contract hiện tại, v1 vẫn là API chính cho:

- list/search Student;
- read/update Student profile;
- lookup Student theo code;
- export;
- profile-only create nếu plan cụ thể vẫn yêu cầu flow đó.

Tuy nhiên các nghiệp vụ học vụ mới không được ép quay về v1 chỉ vì Student CRUD nằm ở v1.

### Student v2 — academic/business participation layer

Không có `StudentV2Controller` riêng cho CRUD Student.

Thay vào đó, Student v2 xuất hiện **cross-module**: các controller nghiệp vụ v2 nhận/trả Student identity và liên kết Student với enrollment, attendance, scorebook, transcript và calculation.

Đây là một lớp nghiệp vụ khác với Student profile CRUD.

#### Student-centered v2 endpoints trực tiếp

| Method | Path | Vai trò |
|---|---|---|
| `GET` | `/api/v2/students/{studentId}/enrollments` | Lịch sử phân lớp theo technical id |
| `GET` | `/api/v2/students/by-code/{studentCode}/enrollments` | Lịch sử phân lớp theo business-facing code |
| `POST` | `/api/v2/students/{studentId}/transcripts/recalculate?academicYearId=...` | Yêu cầu tính lại transcript theo id |
| `POST` | `/api/v2/students/{studentCode}/transcripts/recalculate?academicYearId=...` | Yêu cầu tính lại transcript theo code |

#### Các v2 flow khác mà Student là subject chính

Enrollment:

```text
GET  /api/v2/classes/{classId}/students
POST /api/v2/enrollments
POST /api/v2/enrollments/bulk
POST /api/v2/enrollments/{enrollmentId}/transfer
GET  /api/v2/enrollments/unassigned
```

Attendance:

```text
GET /api/v2/attendance/students/me/history
PUT /api/v2/attendance/sessions/{sessionId}/exceptions/by-code/{studentCode}
PUT /api/v2/academic-office/attendance/sessions/{sessionId}/exceptions/by-code/{studentCode}
```

Scorebook/score change:

```text
PUT  /api/v2/assessment-columns/{columnId}/students/{studentId}/score
PUT  /api/v2/assessment-columns/{columnId}/students/by-code/{studentCode}/score
POST /api/v2/assessment-columns/{columnId}/scores/bulk
POST /api/v2/score-change-requests
```

Transcript:

```text
GET /api/v2/transcripts/students/me/...
GET /api/v2/transcripts/students/{studentId}/...
```

Chi tiết request/response của các flow này nằm tại:

- [`03-teacher-assignment-enrollment.md`](03-teacher-assignment-enrollment.md)
- [`04-calendar-attendance.md`](04-calendar-attendance.md)
- [`05-scorebook-change-audit.md`](05-scorebook-change-audit.md)
- [`06-transcript-retake-calculation.md`](06-transcript-retake-calculation.md)

#### Student identifier rule trong v2

Theo `CR-STUDENT-001` / Plan 042.1:

```text
studentId
= technical / canonical FK identifier

studentCode
= business-facing identifier
```

Các contract được mở rộng phải hỗ trợ `studentCode` ở những flow đã được implementation-backed.

Khi API cho phép cả hai:

```text
studentId only       -> valid
studentCode only     -> valid
both matching        -> valid
both mismatching     -> 400
neither supplied     -> 400
not found            -> 404
```

FE nên ưu tiên `studentCode` cho input/display nơi API hỗ trợ, nhưng vẫn giữ `studentId` làm technical identifier trong state/relations khi contract cần.

#### Vai trò của v2 đối với FE

V2 là lớp FE sử dụng để trả lời các câu hỏi kiểu:

```text
Student đang học lớp nào?
Student được chuyển lớp ra sao?
Student có attendance history gì?
Nhập điểm cho Student nào?
Transcript hiện tại của Student là gì?
Có cần recalculate transcript không?
```

Nó **không phải** lớp để thay thế Student profile CRUD.

### Student v3 — create Student + account provisioning

```text
POST /api/v3/students
```

Authorization:

```text
ADMIN
ACADEMIC_OFFICE
```

V3 giải quyết riêng use case:

```text
User
+ role STUDENT
+ Student
+ StudentInfo
+ Student.userId -> User.userId
```

trong **một transaction**.

Request:

```ts
interface CreateStudentV3Request {
  studentCode: string        // STU + exactly 7 digits
  studentName: string        // max 35
  dateOfBirth?: string | null
  address?: string | null
  averageScore?: number | null
  username?: string | null   // max 20 in current implementation
  password?: string | null   // 6..15 if supplied
}
```

Response:

```ts
interface StudentWithAccount {
  studentId: number
  studentCode: string
  studentName: string
  dateOfBirth: string | null
  address: string | null
  averageScore: number | null
  account: {
    userId: number
    username: string
    role: 'STUDENT'
  }
}
```

Important:

- v3 **không phải self-registration**;
- chỉ `ADMIN` / `ACADEMIC_OFFICE` gọi flow này;
- `username == null` kích hoạt username generation ở backend;
- `password == null` hiện dùng default password theo approved CR/Plan 043;
- password phải được encode trước persistence;
- response không trả password/hash;
- duplicate `studentCode` hoặc username trả conflict theo contract;
- failure ở một bước phải rollback toàn operation.

#### FE rule cho create Student

Nếu screen có ý nghĩa:

> “Tạo học sinh mới để học sinh có thể đăng nhập hệ thống”

thì endpoint đúng là:

```text
POST /api/v3/students
```

Nếu use case cụ thể chỉ yêu cầu:

> “Tạo Student profile mà không provisioning account”

thì compatibility flow hiện có vẫn là:

```text
POST /api/v1/students
```

Không tự động thay một flow bằng flow kia.

### Vì sao không có CRUD `/api/v2/students`

Current backend source không có:

```text
StudentV2Controller
@RequestMapping("/api/v2/students") // CRUD controller
```

Điều này **không có nghĩa Student v2 không tồn tại về mặt API surface**.

Nó có nghĩa kiến trúc hiện tại chia:

```text
Student profile/resource management
        -> v1 StudentController

Student in academic business workflows
        -> v2 Enrollment / Attendance / Scorebook /
           Transcript / Calculation controllers

Student + account provisioning command
        -> v3 StudentV3Controller
```

API guide phải giữ distinction này để agent không:

- invent một `/api/v2/students` CRUD chưa tồn tại;
- migrate toàn bộ v1 call sang v3;
- dùng v1 cho nghiệp vụ academic chỉ vì v1 có StudentController.

### `averageScore` compatibility warning

Current v1/v3 Student DTO vẫn có `averageScore`.

Tuy nhiên Data Model v2 xác định `average_score` là legacy/deprecated và **không phải nguồn kết quả học tập chính thức**.

FE v2:

- không dùng `averageScore` để dựng bảng điểm;
- không tính transcript từ field này;
- official result phải lấy từ Transcript API.

### Delete/lifecycle gap

Backend v1 hiện vẫn expose hard delete.

Requirement v2 yêu cầu giữ lịch sử nghiệp vụ và hướng tới lifecycle/status.

Do chưa có Student lifecycle API v2 đầy đủ, FE không được tự tạo UX deactivate/graduate bằng endpoint tưởng tượng.

### Quick decision table cho FE

| FE use case | API generation |
|---|---|
| List/search Student profile | **v1** |
| Read Student profile | **v1** |
| Update Student profile | **v1** |
| Lookup Student by `studentCode` | **v1** |
| Export Student | **v1** |
| Create profile-only Student | **v1** |
| Create Student + login account | **v3** |
| Enroll / bulk enroll / transfer | **v2** |
| Enrollment history | **v2** |
| Attendance / attendance history | **v2** |
| Score entry/change | **v2** |
| Transcript/status | **v2** |
| Recalculate transcript | **v2** |

Rule:

```text
Choose API by use case,
not by highest version number.
```

## Contract blockers trước role-aware FE shell

Trước khi triển khai sidebar/router đúng role cho Req v2, cần backend contract để FE lấy tối thiểu:

```ts
interface CurrentAccount {
  id: number
  username: string
  roles: Array<'ADMIN' | 'ACADEMIC_OFFICE' | 'TEACHER' | 'STUDENT'>
}
```

Có thể mở rộng thêm identity context nếu được phê duyệt:

```text
teacherId?
studentId?
```

nhưng không được tự thêm nếu backend chưa có contract.

Khuyến nghị contract nhỏ nhất:

```text
GET /api/v1/auth/account
```

hoặc login response trả thêm `roles`.

Backend vẫn phải kiểm tra authorization; roles ở FE chỉ phục vụ navigation/visibility/UX.
