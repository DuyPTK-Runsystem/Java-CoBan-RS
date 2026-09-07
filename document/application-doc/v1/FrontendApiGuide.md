# Frontend API Catalog

> Tài liệu này là bảng tra cứu API cho FE. Khi tích hợp màn hình, ưu tiên contract
> và tên field trong tài liệu này; không tự suy đoán endpoint hoặc response khác.

## 1. Thông tin chung

| Mục | Giá trị |
|---|---|
| API prefix | `/api/v1` |
| Base URL local | `http://localhost:8081` |
| Cấu hình FE | `VITE_API_BASE_URL` trong `FE/.env` |
| HTTP client | Browser native `fetch` |
| JSON success | `{ statusCode, message?, data }` |
| JSON error | `{ statusCode, error?, message? }` |
| Auth | `Authorization: Bearer <accessToken>` |
| Date API | `yyyy-MM-dd` |
| Date UI | `dd-mm-yyyy` |

FE không đưa secret vào `VITE_*`. API base URL là public configuration, không phải
credential.

## 1.1. Endpoint index

| Method | Endpoint | Auth | Response |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | No | JSON envelope → UserSummary |
| `POST` | `/api/v1/auth/login` | No | JSON envelope → access token + user |
| `GET` | `/api/v1/auth/account` | Bearer | JSON envelope → UserSummary |
| `POST` | `/api/v1/auth/logout` | Bearer | `204 No Content` |
| `GET` | `/api/v1/students` | Bearer | JSON envelope → StudentPage |
| `GET` | `/api/v1/students/{studentId}` | Bearer | JSON envelope → Student |
| `POST` | `/api/v1/students` | Bearer | JSON envelope → Student |
| `PUT` | `/api/v1/students/{studentId}` | Bearer | JSON envelope → Student |
| `DELETE` | `/api/v1/students/{studentId}` | Bearer | `204 No Content` |
| `POST` | `/api/v1/students/code` | Bearer | JSON envelope → `{ studentCode }` |
| `GET` | `/api/v1/students/export` | Bearer | Raw `text/csv` blob |

## 2. Quy tắc response và lỗi

### 2.1 JSON success

Với API JSON, backend trả data bên trong envelope:

```json
{
  "statusCode": 200,
  "message": "Success",
  "data": {}
}
```

Service FE unwrap `data` trước khi trả kết quả cho view. View không cần đọc
`response.data.data`.

### 2.2 Lỗi

Ví dụ:

```json
{
  "statusCode": 400,
  "error": "Bad Request",
  "message": ["Student name is required", "Address is invalid"]
}
```

`ApiError` tại `FE/src/services/userApi.ts` giữ lại:

```ts
error.status  // HTTP status
error.message // message đã chuẩn hóa thành string
```

Policy cho view:

| HTTP status | FE xử lý |
|---:|---|
| `400` / `422` | Hiển thị lỗi validation/business; giữ dữ liệu form nếu phù hợp |
| `401` | Xóa auth session, điều hướng Login |
| `403` | Giữ auth session, hiển thị access denied |
| `404` | Hiển thị resource không tồn tại |
| `409` | Hiển thị conflict, ví dụ username/student code trùng |
| `5xx` | Hiển thị lỗi hệ thống/network thân thiện |

API `204 No Content` không có JSON body. Service phải trả `undefined`, không gọi
`response.json()` cho response này.

## 3. Auth API

### 3.1 Register user

```http
POST /api/v1/auth/register
```

Auth: Không cần Bearer token.

Request body:

```json
{
  "username": "student01",
  "password": "secret1",
  "confirmPassword": "secret1"
}
```

Response `data`:

```json
{
  "id": 3,
  "username": "student01"
}
```

Mapping FE:

| Backend | FE |
|---|---|
| `username` | form field `userName` khi gửi vào service |
| `password` | `password` |
| `confirmPassword` | `confirmPassword` |

`confirmPassword` chỉ dùng cho request/validation, không phải database field.

### 3.2 Login

```http
POST /api/v1/auth/login
```

Auth: Không cần Bearer token.

Request body:

```json
{
  "username": "student01",
  "password": "secret1"
}
```

Response `data`:

```json
{
  "access_token": "jwt-token",
  "user": {
    "id": 3,
    "username": "student01"
  }
}
```

Mapping FE:

```ts
{
  accessToken: data.access_token,
  user: data.user,
}
```

Không lưu hoặc hiển thị password/password hash. FE lưu `accessToken` và user summary
an toàn trong `sessionStorage` qua `authSession.ts`.

### 3.3 Current account

```http
GET /api/v1/auth/account
Authorization: Bearer <accessToken>
```

Response `data` là UI-safe user summary:

```json
{
  "id": 3,
  "username": "student01"
}
```

### 3.4 Logout

```http
POST /api/v1/auth/logout
Authorization: Bearer <accessToken>
```

Request body:

```json
{}
```

Response: `204 No Content`.

Frontend luôn xóa `sessionStorage` sau logout, kể cả khi request logout thất bại.

## 4. Student API

Các endpoint Student cần Bearer token.

### 4.1 List/search/sort/page

```http
GET /api/v1/students
Authorization: Bearer <accessToken>
```

Query parameters:

| Parameter | Required | Example | Meaning |
|---|---|---|---|
| `page` | Có | `0` | Page index zero-based |
| `size` | Có | `10` | Page size; mặc định UI là `10` |
| `studentCode` | Không | `STU001` | Lọc theo code |
| `studentName` | Không | `Nguyen` | Lọc theo name |
| `birthday` | Không | `2012-04-22` | Lọc theo ngày sinh |
| `sortField` | Có | `studentName` | Field sort được backend cho phép |
| `sortDirection` | Có | `asc` / `desc` | Chiều sort |

Ví dụ:

```text
/api/v1/students?page=0&size=10&studentCode=STU001&studentName=Nguyen&birthday=2012-04-22&sortField=studentName&sortDirection=asc
```

FE hiện tại trim các filter dạng text và bỏ parameter nếu giá trị rỗng. Date được
serialize từ local date parts để tránh lệch ngày do timezone; không dùng
`toISOString()` cho birthday-only value.

Response `data`:

```json
{
  "content": [
    {
      "studentId": 1,
      "studentCode": "STU001",
      "studentName": "Nguyen Van B",
      "dateOfBirth": "1989-10-11",
      "address": "Ho Chi Minh City",
      "averageScore": 5.6
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

Sort đang được dùng ở màn hình FE hiện tại:

- `studentCode`
- `studentName`
- `averageScore`

Birthday và Address hiện là cột hiển thị, không gửi sort request theo quyết định của
FE plan 021.

### 4.2 Get student detail

```http
GET /api/v1/students/{studentId}
Authorization: Bearer <accessToken>
```

Response `data` có cùng shape với một item trong list:

```json
{
  "studentId": 1,
  "studentCode": "STU1234567",
  "studentName": "Nguyen Van A",
  "dateOfBirth": "2012-04-22",
  "address": "Ho Chi Minh City",
  "averageScore": 8.5
}
```

`dateOfBirth`, `address` và `averageScore` có thể là `null`.

### 4.3 Create student

```http
POST /api/v1/students
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Request body:

```json
{
  "studentCode": "STU1234567",
  "studentName": "Nguyen Van A",
  "dateOfBirth": "2012-04-22",
  "address": "Ho Chi Minh City",
  "averageScore": 8.5
}
```

`studentCode` có thể lấy từ API Generate code trước đó. Backend vẫn là nơi kiểm tra
format và uniqueness.

Response `data`: Student object.

### 4.4 Update student

```http
PUT /api/v1/students/{studentId}
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Request body FE hiện tại:

```json
{
  "studentName": "Nguyen Van A Updated",
  "dateOfBirth": "2012-04-22",
  "address": "District 1",
  "averageScore": 9.0
}
```

FE không gửi lại `studentCode` khi update. `studentId` nằm ở path và không được đổi
qua request body.

Response `data`: Student object.

### 4.5 Delete student

```http
DELETE /api/v1/students/{studentId}
Authorization: Bearer <accessToken>
```

Response: `204 No Content`.

Sau khi xóa thành công, view refresh list hiện tại và xử lý trường hợp page cuối bị
trống theo state của paginator.

### 4.6 Generate student code

```http
POST /api/v1/students/code
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Request body:

```json
{}
```

Response `data`:

```json
{
  "studentCode": "STU1234567"
}
```

### 4.7 Export CSV

```http
GET /api/v1/students/export
Authorization: Bearer <accessToken>
Accept: text/csv
```

Response: raw CSV body, không có JSON envelope. FE nhận response bằng
`response.blob()` và tạo file download `students.csv`.

## 5. FE model mapping

### User

```ts
interface UserSummary {
  id: number
  username: string
}

interface LoginResponse {
  accessToken: string
  user: UserSummary
}
```

### Student

```ts
interface Student {
  studentId: number
  studentCode: string
  studentName: string
  dateOfBirth: string
  address: string
  averageScore: number | null
}
```

### Page

```ts
interface StudentPage {
  content: Student[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
```

### Field naming gotchas

| Backend/API | FE form/model |
|---|---|
| `username` | `userName` in Login/Register form values |
| `access_token` | `accessToken` |
| `birthday` query | `dateOfBirth` in Student search values |
| `size` query | `pageSize` in Student query state |
| `dateOfBirth` JSON | `Date` in form control, `string` in API model |

## 6. FE service locations

| File | API responsibility |
|---|---|
| `FE/src/services/apiConfig.ts` | Base URL |
| `FE/src/services/authSession.ts` | Read/save/clear session |
| `FE/src/services/userApi.ts` | Auth endpoints, JSON envelope, `ApiError` |
| `FE/src/services/studentApi.ts` | Student CRUD, query, code, CSV |
| `FE/src/types/user.ts` | User/auth/request/response types |
| `FE/src/types/student.ts` | Student/query/page/payload types |

View giữ loading, notification, navigation và UI state. Service chỉ giữ transport
details: URL, headers, serialization, response parsing và transport error.

## 7. Checklist khi thêm API cho FE

1. Xác định endpoint và contract trong backend/application docs.
2. Thêm request/response type rõ ràng trong `FE/src/types/`.
3. Thêm function vào service module tương ứng trong `FE/src/services/`.
4. Dùng `apiBaseUrl`, không hard-code environment khác.
5. Thêm Bearer token cho endpoint protected.
6. Unwrap `data` với JSON success; xử lý riêng `204` và raw CSV/blob.
7. Chuẩn hóa lỗi thành `ApiError` nhưng không đổi business semantics.
8. Không log token, password hoặc request body nhạy cảm.
9. Viết test cho URL, query/body mapping, auth header, response unwrap và lỗi.
10. Chạy validation FE trước khi báo hoàn thành:

```bash
npm run lint
npm run test
npm run test:coverage
npm run build
```

## 8. Tài liệu nguồn

- [Application Context](ApplicationContext.md)
- [User Module](modules/UserModule.md)
- [Student Module](modules/StudentModule.md)
- [FE User/Auth Dev Note](../../dev-note/fe/user-auth/012-user-auth-api-route-guard-2026-08-18.md)
- [FE Student API Dev Note](../../dev-note/fe/student/015-student-api-crud-search-sort-page-delete-2026-08-19.md)
- [FE Student CSV Dev Note](../../dev-note/fe/student/021-student-csv-download-pagination-options-go-to-page-2026-08-19.md)
