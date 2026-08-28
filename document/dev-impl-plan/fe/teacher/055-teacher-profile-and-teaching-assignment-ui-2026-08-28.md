# Developer Plan 055: Teacher Profile & Teaching Assignment UI

## Trạng thái phê duyệt

- Status: `APPROVED - Implemented` (user approval received via agent message: "tôi approve plan 55").
- Application version: `v2`.
- Parent context: Backend Teacher API (`/api/v2/teachers`) và Teaching Assignment API (`/api/v2/assignments`, `/api/v2/classes/{classId}/homeroom-assignments`, `/api/v2/class-subjects/{classSubjectId}/teaching-assignments`, v.v.) đã hoàn thành ở Plan 027 & 034; FE v2 shell, academic catalog và enrollment đã hoàn thành qua Plan 051–054.
- Phạm vi: Xây dựng 2 module giao diện Frontend:
  1. **Teacher Profile UI**: Danh sách, xem chi tiết, tạo/sửa, xóa và liên kết tài khoản người dùng cho giáo viên.
  2. **Teaching Assignment UI**: Phân công giáo viên chủ nhiệm (GVCN) theo lớp, phân công giáo viên bộ môn (GVBM) theo môn học/lớp/học kỳ, thay thế và kết thúc phân công, xem phân công theo lớp và theo giáo viên.
- Không triển khai mã nguồn production trước khi plan này được người dùng phê duyệt rõ ràng qua agent.

---

## 1. Mục tiêu

Đáp ứng 2 nội dung nghiệp vụ trong phân hệ Quản lý Giáo viên & Phân công Giảng dạy:

1. **Teacher Profile UI (`Hồ sơ giáo viên`)**:
   - Quản lý danh sách giáo viên: xem danh sách, lọc theo trạng thái (`ACTIVE`, `ON_LEAVE`, `INACTIVE`), tìm kiếm client-side theo mã giáo viên, họ tên, email, số điện thoại, tổ chuyên môn.
   - Xem chi tiết hồ sơ giáo viên (thông tin cá nhân, tổ bộ môn, ngày vào trường, tài khoản liên kết).
   - Tạo mới hồ sơ giáo viên với đầy đủ thông tin định danh và tùy chọn liên kết tài khoản `userId`.
   - Cập nhật hồ sơ giáo viên, điều chỉnh trạng thái công tác hoặc cập nhật tài khoản liên kết.
   - Xóa hồ sơ giáo viên an toàn (xác nhận trước khi xóa, hiển thị lỗi rõ ràng nếu giáo viên đã có dữ liệu phân công/chấm điểm).

2. **Teaching Assignment UI (`Phân công giảng dạy`)**:
   - Context phân công theo Năm học, Khối, Lớp và Học kỳ.
   - **Phân công GVCN (Homeroom Assignment)**:
     - Xem GVCN hiện tại đang `ACTIVE` và lịch sử các đợt phân công GVCN của lớp.
     - Phân công GVCN mới cho lớp.
     - Thay thế GVCN (Replace): kết thúc phân công cũ và gán giáo viên mới liền mạch.
     - Kết thúc phân công GVCN (End): nhập ngày kết thúc `validTo`.
   - **Phân công GVBM (Subject Teaching Assignment)**:
     - Tải danh sách môn học của lớp trong học kỳ (`class-subjects`).
     - Xem GVBM đang phụ trách từng môn học.
     - Phân công GVBM cho môn học của lớp.
     - Thay thế GVBM (Replace) hoặc kết thúc phân công (End).
   - **Xem phân công theo Giáo viên (Teacher Teaching Schedule View)**:
     - Chọn giáo viên để xem toàn bộ danh sách lớp/môn học mà giáo viên đó đang được phân công giảng dạy.

---

## 2. Tài liệu và contract áp dụng

- `document/application-doc/v2/ApplicationContext.md` — Kiến trúc, thứ tự ưu tiên tài liệu v2.
- `document/application-doc/v2/RequirementBaseline.md` — Baseline index.
- `document/application-doc/v2/modules/02-EnrollmentAndTeachingModule.md` — Yêu cầu nghiệp vụ `FR-TEACHER-001..006`, `BR-TEACHER-001..006`, `FR-ASSIGN-001..007`, `BR-ASSIGN-001..007`.
- `document/application-doc/v2/data-model/01-IdentityAndAccess.md` — Cấu trúc bảng `teacher`, ràng buộc duy nhất mã giáo viên, quan hệ 1-1 tùy chọn với `app_user`.
- `document/application-doc/v2/data-model/04-TeachingAssignments.md` — Cấu trúc bảng `homeroom_assignment`, `subject_teaching_assignment`, quy tắc trạng thái `ACTIVE` / `ENDED` và ràng buộc không chồng lấn.
- `document/application-doc/v2/frontend-api/00-common-contract.md` — Envelope, authentication, xử lý HTTP error, typed service pattern.
- `document/application-doc/v2/frontend-api/03-teacher-assignment-enrollment.md` — Chi tiết API Teacher & Teaching Assignment.
- `document/application-doc/v2/frontend-api/07-enums-and-known-drift.md` — Canonical wire enums (`TeacherStatus`, `AssignmentStatus`).
- `FE/agent-rules/00-foundation.md` và `FE/agent-rules/02-domain-rules.md` — Quy tắc TypeScript, PrimeVue, component boundary, routing.

---

## 3. Bằng chứng hiện tại và API Contract

### 3.1. Teacher Backend API (`TeacherController`)

Base path: `/api/v2/teachers`

| Method   | Path                           | Authorization              | Request Body / Query     | Response Body       |
| -------- | ------------------------------ | -------------------------- | ------------------------ | ------------------- |
| `GET`    | `/api/v2/teachers`             | `Authenticated`            | `status?: TeacherStatus` | `ResTeacherDTO[]`   |
| `GET`    | `/api/v2/teachers/{teacherId}` | `Authenticated`            | —                        | `ResTeacherDTO`     |
| `POST`   | `/api/v2/teachers`             | `ADMIN`, `ACADEMIC_OFFICE` | `ReqCreateTeacherDTO`    | `201 ResTeacherDTO` |
| `PUT`    | `/api/v2/teachers/{teacherId}` | `ADMIN`, `ACADEMIC_OFFICE` | `ReqUpdateTeacherDTO`    | `ResTeacherDTO`     |
| `DELETE` | `/api/v2/teachers/{teacherId}` | `ADMIN`, `ACADEMIC_OFFICE` | —                        | `204 No Content`    |

**DTO Model**:
- `ResTeacherDTO`: `id`, `userId`, `teacherCode`, `teacherName`, `dateOfBirth`, `gender`, `phone`, `email`, `department`, `joinDate`, `status`.
- `ReqCreateTeacherDTO` / `ReqUpdateTeacherDTO`:
  - `userId` (`Long`, optional, positive)
  - `teacherCode` (`String`, required, max 50)
  - `teacherName` (`String`, required, max 150)
  - `dateOfBirth` (`LocalDate`, optional)
  - `gender` (`String`, optional, max 20)
  - `phone` (`String`, optional, max 30)
  - `email` (`String`, optional, email format, max 150)
  - `department` (`String`, optional, max 100)
  - `joinDate` (`LocalDate`, optional)
  - `status` (`TeacherStatus`: `ACTIVE` | `ON_LEAVE` | `INACTIVE`, required)

### 3.2. Assignment Backend API (`AssignmentController`)

Base path: `/api/v2` (Quyền: `ADMIN`, `ACADEMIC_OFFICE`)

| Method | Path                                                           | Request Body                            | Response Body                         |
| ------ | -------------------------------------------------------------- | --------------------------------------- | ------------------------------------- |
| `GET`  | `/api/v2/assignments/classes/{classId}`                        | —                                       | `ResHomeroomAssignmentDTO[]`          |
| `GET`  | `/api/v2/assignments/teachers/{teacherId}`                     | —                                       | `ResSubjectTeachingAssignmentDTO[]`   |
| `POST` | `/api/v2/classes/{classId}/homeroom-assignments`               | `ReqCreateHomeroomAssignmentDTO`        | `201 ResHomeroomAssignmentDTO`        |
| `POST` | `/api/v2/homeroom-assignments/{assignmentId}/replace`          | `ReqReplaceAssignmentDTO`               | `ResHomeroomAssignmentDTO`            |
| `POST` | `/api/v2/homeroom-assignments/{assignmentId}/end`              | `ReqEndAssignmentDTO`                   | `ResHomeroomAssignmentDTO`            |
| `POST` | `/api/v2/class-subjects/{classSubjectId}/teaching-assignments` | `ReqCreateSubjectTeachingAssignmentDTO` | `201 ResSubjectTeachingAssignmentDTO` |
| `POST` | `/api/v2/subject-teaching-assignments/{assignmentId}/replace`  | `ReqReplaceAssignmentDTO`               | `ResSubjectTeachingAssignmentDTO`     |
| `POST` | `/api/v2/subject-teaching-assignments/{assignmentId}/end`      | `ReqEndAssignmentDTO`                   | `ResSubjectTeachingAssignmentDTO`     |

**DTO Model**:
- `ResHomeroomAssignmentDTO`: `id`, `classId`, `teacherId`, `validFrom`, `validTo`, `status`, `assignedBy`.
- `ResSubjectTeachingAssignmentDTO`: `id`, `classSubjectId`, `teacherId`, `validFrom`, `validTo`, `status`, `assignedBy`.
- `ReqCreateHomeroomAssignmentDTO` / `ReqCreateSubjectTeachingAssignmentDTO`: `teacherId` (required), `validFrom` (required), `validTo` (optional).
- `ReqReplaceAssignmentDTO`: `teacherId` (required), `validFrom` (required), `validTo` (optional).
- `ReqEndAssignmentDTO`: `validTo` (required).
- `AssignmentStatus`: `ACTIVE` | `ENDED` (chuẩn hóa enum).

---

## 4. Phạm vi chức năng chi tiết

### 4.1. Điều hướng và Route

- **Route 1: Teacher Profile UI**
  - Path: `/v2/teachers`, Name: `v2-teachers`.
  - Menu navigation trong `AuthenticatedV2ShellView.vue`: `Hồ sơ giáo viên`, icon `pi pi-id-card`.
- **Route 2: Teaching Assignment UI**
  - Path: `/v2/teaching-assignments`, Name: `v2-teaching-assignments`.
  - Menu navigation trong `AuthenticatedV2ShellView.vue`: `Phân công giảng dạy`, icon `pi pi-briefcase`.

### 4.2. Chi tiết màn hình Teacher Profile UI (`TeacherListView.vue`)

- **Thanh công cụ & Bộ lọc**:
  - Tiêu đề trang: `Hồ sơ giáo viên` / `Quản lý thông tin hồ sơ và liên kết tài khoản giáo viên`.
  - Bộ lọc trạng thái: Tất cả, Đang công tác (`ACTIVE`), Nghỉ phép (`ON_LEAVE`), Ngừng công tác (`INACTIVE`).
  - Ô tìm kiếm nhanh client-side: tìm theo Mã GV, Họ tên, Tổ chuyên môn, Email, Số điện thoại.
  - Nút `Thêm giáo viên` (mở Dialog tạo mới).
- **Bảng danh sách giáo viên (`TeacherTable.vue`)**:
  - Các cột: Mã GV, Họ và tên, Giới tính, Ngày sinh, Tổ chuyên môn, Số điện thoại, Email, Ngày vào trường, Trạng thái (Tag/Badge màu), Tài khoản liên kết (`User ID` hoặc `Chưa liên kết`), Thao tác.
  - Thao tác trên từng dòng: `Xem chi tiết`, `Chỉnh sửa`, `Xóa`.
- **Dialog Tạo / Chỉnh sửa giáo viên (`TeacherDialog.vue`)**:
  - Trường nhập: Mã GV (bắt buộc), Họ tên (bắt buộc), Giới tính, Ngày sinh, Số điện thoại, Email (validate định dạng), Tổ chuyên môn, Ngày vào trường, Trạng thái (dropdown), Liên kết tài khoản (`User ID` - số nguyên dương tùy chọn).
  - Client-side validation: bắt lỗi rỗng, độ dài tối đa, định dạng email, ngày tháng hợp lệ.
- **Dialog Xem chi tiết giáo viên (`TeacherDetailDialog.vue`)**:
  - Hiển thị đầy đủ thông tin cá nhân, tổ bộ môn, ngày vào trường, trạng thái tài khoản liên kết, và cho phép bấm nút chuyển sang xem lịch sử phân công giảng dạy của giáo viên đó.
- **Xác nhận xóa giáo viên (`ConfirmAction.vue`)**:
  - Hộp thoại xác nhận trước khi xóa.
  - Khi backend trả về lỗi do giáo viên đã có phân công hoặc điểm danh (ràng buộc toàn vẹn dữ liệu), hiển thị thông báo lỗi rõ ràng, không làm gián đoạn trạng thái giao diện.

### 4.4. Wireframes giao diện

#### Wireframe 1: Màn hình Hồ sơ giáo viên (`TeacherListView.vue` - `/v2/teachers`)

```text
+--------------------------------------------------------------------------------------------------------------------------+
|  [v2 Shell Header: THCS Demo | User: admin (Logout)]                                                                      |
|  [Sidebar: Năm học | Khối | Lớp | Môn học | Quản lí môn | Xếp lớp | > Hồ sơ giáo viên | Phân công giảng dạy]             |
+--------------------------------------------------------------------------------------------------------------------------+
|                                                                                                                          |
|  HỒ SƠ GIÁO VIÊN                                                                                                          |
|  Quản lý danh sách, thông tin hồ sơ cá nhân và liên kết tài khoản giáo viên                     [ + Thêm giáo viên ]      |
|                                                                                                                          |
|  +--------------------------------------------------------------------------------------------------------------------+  |
|  | Bộ lọc trạng thái: [ (•) Tất cả (24) ]  [ Đang công tác (20) ]  [ Nghỉ phép (2) ]  [ Ngừng công tác (2) ]          |  |
|  |                                                                                                                    |  |
|  | Tìm kiếm: [ 🔍 Nhập mã GV, họ tên, email, SĐT, tổ chuyên môn...                                                 ]  |  |
|  +--------------------------------------------------------------------------------------------------------------------+  |
|                                                                                                                          |
|  +--------------------------------------------------------------------------------------------------------------------+  |
|  | Mã GV   | Họ và tên         | Giới tính | Ngày sinh   | Tổ CM       | Số ĐT      | Email             | TT       | LK TK   | Thao tác   |  |
|  |---------|-------------------|-----------|-------------|-------------|------------|-------------------|----------|---------|------------|  |
|  | GV001   | Nguyễn Văn An     | Nam       | 15/05/1985  | Toán - Tin  | 0912345678 | an.nv@school.edu  | [ACTIVE] | User #2 | 👁️  ✏️  🗑️  |  |
|  | GV002   | Trần Thị Bình     | Nữ        | 20/10/1990  | Ngữ văn     | 0987654321 | binh.tt@school.edu| [ACTIVE] | User #5 | 👁️  ✏️  🗑️  |  |
|  | GV003   | Lê Hoàng Cường    | Nam       | 08/03/1988  | Ngoại ngữ   | 0903112233 | cuong.lh@school...| [ON_LEAV]| Chưa LK | 👁️  ✏️  🗑️  |  |
|  | GV004   | Phạm Minh Đức     | Nam       | 12/12/1979  | KHTN        | 0934556677 | duc.pm@school.edu | [INACTIV]| User #9 | 👁️  ✏️  🗑️  |  |
|  +--------------------------------------------------------------------------------------------------------------------+  |
|  Hiển thị 1-4 trong tổng số 24 giáo viên                                                                                  |
+--------------------------------------------------------------------------------------------------------------------------+
```

#### Wireframe 1.1: Dialog Thêm mới / Chỉnh sửa giáo viên (`TeacherDialog.vue`)

```text
+------------------------------------------------------------------------------------+
|  [Icon] Thêm mới hồ sơ giáo viên (hoặc Cập nhật hồ sơ giáo viên)               [X] |
+------------------------------------------------------------------------------------+
|                                                                                    |
|  Mã giáo viên (*)                           Họ và tên (*)                          |
|  [ GV001                                 ]  [ Nguyễn Văn An                      ] |
|                                                                                    |
|  Giới tính                                  Ngày sinh                              |
|  [ Nam                                 v ]  [ 15/05/1985                      📅 ] |
|                                                                                    |
|  Số điện thoại                              Email                                  |
|  [ 0912345678                            ]  [ an.nv@school.edu.vn                ] |
|                                                                                    |
|  Tổ chuyên môn                              Ngày vào trường                        |
|  [ Toán - Tin                            ]  [ 01/09/2015                      📅 ] |
|                                                                                    |
|  Trạng thái (*)                             Tài khoản liên kết (User ID)           |
|  [ ACTIVE - Đang công tác              v ]  [ 2                                  ] |
|                                             *(Tùy chọn: nhập ID tài khoản hệ thống)|
|                                                                                    |
+------------------------------------------------------------------------------------+
|                                                          [ Hủy ]  [ Lưu thông tin ] |
+------------------------------------------------------------------------------------+
```

#### Wireframe 2: Màn hình Phân công giảng dạy (`TeachingAssignmentView.vue` - `/v2/teaching-assignments`)

```text
+--------------------------------------------------------------------------------------------------------------------------+
|  PHÂN CÔNG GIẢNG DẠY                                                                                                     |
|  Quản lý phân công GVCN, phân công GVBM theo môn học/lớp và xem lịch giảng dạy                                           |
|                                                                                                                          |
|  [ Tab 1: Phân công theo Lớp học (Đang chọn) ]         [ Tab 2: Lịch dạy theo Giáo viên ]                                |
|                                                                                                                          |
|  +--------------------------------------------------------------------------------------------------------------------+  |
|  | Context:  Năm học: [ 2026-2027 (ACTIVE) v ]   Khối: [ Khối 6 v ]   Lớp: [ 6A1 (35 HS) v ]   Học kỳ: [ Học kỳ 1 v ] |  |
|  +--------------------------------------------------------------------------------------------------------------------+  |
|                                                                                                                          |
|  ┌── PHÂN CÔNG GIÁO VIÊN CHỦ NHIỆM (GVCN) ────────────────────────────────────────────────────────────────────────────┐  |
|  │  GVCN Hiện tại: GV001 - Nguyễn Văn An (Toán - Tin)                      Trạng thái: [ ACTIVE ]                     │  |
|  │  Thời gian hiệu lực: Từ 01/09/2026                                                                                 │  |
|  │                                              [ 📋 Xem lịch sử GVCN ]  [ 🔄 Thay đổi GVCN ]  [ ⏹️ Kết thúc phân công ]│  |
|  └────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘  |
|                                                                                                                          |
|  ┌── PHÂN CÔNG GIÁO VIÊN BỘ MÔN (GVBM) THEO MÔN HỌC ──────────────────────────────────────────────────────────────────┐  |
|  │  +---------------------------------------------------------------------------------------------------------------+  │  |
|  │  | STT | Môn học       | Loại môn   | GVBM phụ trách        | Tổ CM       | Hiệu lực từ | Trạng thái | Thao tác  |  │  |
|  │  |-----|---------------|------------|-----------------------|-------------|-------------|------------|-----------|  │  |
|  │  | 1   | Toán học      | Chính khóa | GV001 - Nguyễn Văn An | Toán - Tin  | 05/09/2026  | [ACTIVE]   | [🔄 Thay]  |  │  |
|  │  |     |               |            |                       |             |             |            | [⏹️ Dừng]  |  │  |
|  │  | 2   | Ngữ văn       | Chính khóa | GV002 - Trần Thị Bình | Ngữ văn     | 05/09/2026  | [ACTIVE]   | [🔄 Thay]  |  │  |
|  │  | 3   | Tiếng Anh     | Chính khóa | (Chưa có phân công)   | —           | —           | [TRỐNG]    | [+ Gán GV]|  │  |
|  │  | 4   | GD Thể chất   | Kỹ năng    | GV005 - Lê Văn Cảnh   | Thể chất    | 05/09/2026  | [ACTIVE]   | [🔄 Thay]  |  │  |
|  │  +---------------------------------------------------------------------------------------------------------------+  │  |
|  └────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘  |
+--------------------------------------------------------------------------------------------------------------------------+
```

#### Wireframe 2.1: Tab 2 - Lịch giảng dạy theo Giáo viên

```text
+--------------------------------------------------------------------------------------------------------------------------+
|  [ Tab 1: Phân công theo Lớp học ]         [ Tab 2: Lịch dạy theo Giáo viên (Đang chọn) ]                                |
|                                                                                                                          |
|  +--------------------------------------------------------------------------------------------------------------------+  |
|  | Chọn Giáo viên: [ 🔍 GV001 - Nguyễn Văn An (Tổ: Toán - Tin | ACTIVE)                                             v ] |  |
|  +--------------------------------------------------------------------------------------------------------------------+  |
|                                                                                                                          |
|  Thông tin giáo viên: GV001 - Nguyễn Văn An | Tổ: Toán - Tin | Email: an.nv@school.edu.vn | Tổng số phân công active: 3  |
|                                                                                                                          |
|  +--------------------------------------------------------------------------------------------------------------------+  |
|  | STT | Năm học    | Khối   | Lớp   | Môn học  | Loại môn   | Thời gian bắt đầu | Thời gian kết thúc | Trạng thái      |  |
|  |-----|------------|--------|-------|----------|------------|-------------------|--------------------|-----------------|  |
|  | 1   | 2026-2027  | Khối 6 | 6A1   | Toán học | Chính khóa | 05/09/2026        | — (Hiện tại)       | [ ACTIVE ]      |  |
|  | 2   | 2026-2027  | Khối 6 | 6A2   | Toán học | Chính khóa | 05/09/2026        | — (Hiện tại)       | [ ACTIVE ]      |  |
|  | 3   | 2026-2027  | Khối 7 | 7A1   | Tin học  | Kỹ năng    | 05/09/2026        | — (Hiện tại)       | [ ACTIVE ]      |  |
|  | 4   | 2025-2026  | Khối 6 | 6A1   | Toán học | Chính khóa | 05/09/2025        | 31/05/2026         | [ ENDED ]       |  |
|  +--------------------------------------------------------------------------------------------------------------------+  |
+--------------------------------------------------------------------------------------------------------------------------+
```

#### Wireframe 2.2: Dialog Phân công / Thay thế GVCN & GVBM (`AssignmentDialog.vue`)

```text
+------------------------------------------------------------------------------------+
|  [Icon] Phân công GVBM - Môn Toán học (Lớp 6A1)                                [X] |
|  Chế độ: Thay thế giáo viên giảng dạy                                              |
+------------------------------------------------------------------------------------+
|                                                                                    |
|  ℹ️ Giáo viên đang phụ trách: GV001 - Nguyễn Văn An (Hiệu lực từ: 05/09/2026)      |
|  ⚠️ Việc thay thế sẽ kết thúc phân công cũ và tạo phân công mới cho giáo viên mới. |
|                                                                                    |
|  Chọn giáo viên thay thế (*)                                                       |
|  [ GV006 - Vũ Quốc Hùng (Toán - Tin)                                            v ] |
|                                                                                    |
|  Ngày bắt đầu hiệu lực mới (*)              Ngày kết thúc hiệu lực (tùy chọn)      |
|  [ 15/10/2026                           📅 ] [                                  📅 ] |
|                                                                                    |
+------------------------------------------------------------------------------------+
|                                                    [ Hủy ]  [ Xác nhận thay thế ]   |
+------------------------------------------------------------------------------------+
```

---

## 5. Phương án kỹ thuật

### 5.1. Types và Data Models

Tạo 2 file type:
1. `FE/src/types/teacher.ts`:
   - `TeacherStatus`: `'ACTIVE' | 'ON_LEAVE' | 'INACTIVE'`
   - `Teacher`: Thông tin giáo viên đầy đủ từ backend DTO.
   - `CreateTeacherRequest`, `UpdateTeacherRequest`.
   - `TeacherFormValues`: Form model phục vụ PrimeVue component binding.
2. `FE/src/types/assignment.ts`:
   - `AssignmentStatus`: `'ACTIVE' | 'ENDED'`
   - `HomeroomAssignment`, `SubjectTeachingAssignment`.
   - `CreateHomeroomAssignmentRequest`, `ReplaceHomeroomAssignmentRequest`, `EndHomeroomAssignmentRequest`.
   - `CreateSubjectTeachingAssignmentRequest`, `ReplaceSubjectTeachingAssignmentRequest`, `EndSubjectTeachingAssignmentRequest`.
   - `AssignmentFormValues`.

### 5.2. Typed API Services

Tạo 2 API services:
1. `FE/src/services/teacherApi.ts`:
   - `fetchTeachers(token, status?)`: GET `/api/v2/teachers`
   - `fetchTeacherById(token, teacherId)`: GET `/api/v2/teachers/{teacherId}`
   - `createTeacher(token, request)`: POST `/api/v2/teachers`
   - `updateTeacher(token, teacherId, request)`: PUT `/api/v2/teachers/{teacherId}`
   - `deleteTeacher(token, teacherId)`: DELETE `/api/v2/teachers/{teacherId}`
2. `FE/src/services/assignmentApi.ts`:
   - `fetchHomeroomAssignmentsByClass(token, classId)`: GET `/api/v2/assignments/classes/{classId}`
   - `fetchSubjectAssignmentsByTeacher(token, teacherId)`: GET `/api/v2/assignments/teachers/{teacherId}`
   - `createHomeroomAssignment(token, classId, request)`: POST `/api/v2/classes/{classId}/homeroom-assignments`
   - `replaceHomeroomAssignment(token, assignmentId, request)`: POST `/api/v2/homeroom-assignments/{assignmentId}/replace`
   - `endHomeroomAssignment(token, assignmentId, request)`: POST `/api/v2/homeroom-assignments/{assignmentId}/end`
   - `createSubjectTeachingAssignment(token, classSubjectId, request)`: POST `/api/v2/class-subjects/{classSubjectId}/teaching-assignments`
   - `replaceSubjectTeachingAssignment(token, assignmentId, request)`: POST `/api/v2/subject-teaching-assignments/{assignmentId}/replace`
   - `endSubjectTeachingAssignment(token, assignmentId, request)`: POST `/api/v2/subject-teaching-assignments/{assignmentId}/end`

### 5.3. Components Boundary & Structure

```text
FE/src/
├── types/
│   ├── teacher.ts
│   └── assignment.ts
├── services/
│   ├── teacherApi.ts
│   ├── teacherApi.spec.ts
│   ├── assignmentApi.ts
│   └── assignmentApi.spec.ts
├── views/
│   ├── TeacherListView.vue
│   ├── TeacherListView.spec.ts
│   ├── TeachingAssignmentView.vue
│   └── TeachingAssignmentView.spec.ts
└── components/
    ├── TeacherTable.vue
    ├── TeacherTable.stories.ts
    ├── TeacherDialog.vue
    ├── TeacherDialog.spec.ts
    ├── TeacherDialog.stories.ts
    ├── TeacherDetailDialog.vue
    ├── TeacherDetailDialog.stories.ts
    ├── AssignmentContextPanel.vue
    ├── AssignmentContextPanel.stories.ts
    ├── HomeroomAssignmentCard.vue
    ├── HomeroomAssignmentCard.stories.ts
    ├── HomeroomHistoryDialog.vue
    ├── HomeroomHistoryDialog.stories.ts
    ├── ClassSubjectAssignmentTable.vue
    ├── ClassSubjectAssignmentTable.stories.ts
    ├── HomeroomAssignmentDialog.vue
    ├── HomeroomAssignmentDialog.spec.ts
    ├── HomeroomAssignmentDialog.stories.ts
    ├── SubjectAssignmentDialog.vue
    ├── SubjectAssignmentDialog.spec.ts
    ├── SubjectAssignmentDialog.stories.ts
    ├── TeacherAssignmentScheduleTable.vue
    └── TeacherAssignmentScheduleTable.stories.ts
```

---

## 6. Danh sách file dự kiến thay đổi / tạo mới

| File                                                   | Hành động  | Mục đích                                                     |
| ------------------------------------------------------ | ---------- | ------------------------------------------------------------ |
| `FE/src/types/teacher.ts`                              | **NEW**    | Định nghĩa types, enums, DTOs cho Teacher                    |
| `FE/src/types/assignment.ts`                           | **NEW**    | Định nghĩa types, enums, DTOs cho Teaching Assignment        |
| `FE/src/services/teacherApi.ts`                        | **NEW**    | Typed API service cho Teacher endpoints                      |
| `FE/src/services/teacherApi.spec.ts`                   | **NEW**    | Unit test cho Teacher API service                            |
| `FE/src/services/assignmentApi.ts`                     | **NEW**    | Typed API service cho Assignment endpoints                   |
| `FE/src/services/assignmentApi.spec.ts`                | **NEW**    | Unit test cho Assignment API service                         |
| `FE/src/components/TeacherTable.vue`                   | **NEW**    | Bảng danh sách giáo viên                                     |
| `FE/src/components/TeacherDialog.vue`                  | **NEW**    | Dialog tạo / sửa giáo viên                                   |
| `FE/src/components/TeacherDetailDialog.vue`            | **NEW**    | Dialog xem chi tiết hồ sơ giáo viên                          |
| `FE/src/components/AssignmentContextPanel.vue`         | **NEW**    | Panel chọn Năm học, Khối, Lớp, Học kỳ                        |
| `FE/src/components/HomeroomAssignmentCard.vue`         | **NEW**    | Card hiển thị GVCN active và các nút action                  |
| `FE/src/components/HomeroomHistoryDialog.vue`          | **NEW**    | Dialog xem lịch sử các đời GVCN của lớp                      |
| `FE/src/components/ClassSubjectAssignmentTable.vue`    | **NEW**    | Bảng phân công GVBM theo môn học của lớp                     |
| `FE/src/components/HomeroomAssignmentDialog.vue`       | **NEW**    | Dialog phân công / thay thế / kết thúc GVCN                  |
| `FE/src/components/SubjectAssignmentDialog.vue`        | **NEW**    | Dialog phân công / thay thế / kết thúc GVBM                  |
| `FE/src/components/TeacherAssignmentScheduleTable.vue` | **NEW**    | Bảng phân công theo góc nhìn giáo viên                       |
| `FE/src/views/TeacherListView.vue`                     | **NEW**    | View chính cho Teacher Profile UI                            |
| `FE/src/views/TeacherListView.spec.ts`                 | **NEW**    | Unit test cho TeacherListView                                |
| `FE/src/views/TeachingAssignmentView.vue`              | **NEW**    | View chính cho Teaching Assignment UI                        |
| `FE/src/views/TeachingAssignmentView.spec.ts`          | **NEW**    | Unit test cho TeachingAssignmentView                         |
| `FE/src/router/index.ts`                               | **MODIFY** | Đăng ký route `/v2/teachers` và `/v2/teaching-assignments`   |
| `FE/src/views/AuthenticatedV2ShellView.vue`            | **MODIFY** | Bổ sung menu item `Hồ sơ giáo viên` và `Phân công giảng dạy` |
| Storybook story files (`*.stories.ts`)                 | **NEW**    | Cung cấp deterministic state stories cho các components mới  |
| `document/dev-impl-plan/fe/FE_DEV_PLAN_SUMMARY.md`     | **MODIFY** | Cập nhật mục Plan 055                                        |
| `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md`   | **MODIFY** | Cập nhật mục Plan 055                                        |

---

## 7. Kế hoạch kiểm thử và Validation

### 7.1. Unit & Service Tests
- **Teacher API**: Test đầy đủ GET list (với/không query status), GET by ID, POST create, PUT update, DELETE. Kiểm tra đúng Bearer Token header, URL, query param và payload body.
- **Assignment API**: Test đầy đủ các endpoint homeroom & subject teaching assignment (list theo class/teacher, create, replace, end).
- **Component Tests**: Test validation form Teacher, validation form Assignment (`validFrom`, `validTo`), emit events khi submit, disable nút khi request đang gửi, hiển thị lỗi khi API trả về status mã lỗi.
- **View Orchestration Tests**: Test load danh sách ban đầu, chuyển bộ lọc, chọn context năm học/lớp/học kỳ, mở dialog, reload dữ liệu sau khi mutate thành công, xử lý lỗi `401`, `403`, `404`, `409`.

### 7.2. Storybook Stories
- Cung cấp story cho các trạng thái: Loaded data, Empty state, Loading state, Create/Edit Dialogs, Replace Dialog, End Dialog, History Dialog.
- Đảm bảo story chạy độc lập, deterministic mà không phụ thuộc live backend.

### 7.3. Quality Gates
Chạy toàn bộ bộ kiểm tra chất lượng theo quy định:
```bash
cd FE
npm run lint
npm run test
npm run test:coverage
npm run build
npm run build-storybook
```

---

## 8. Quy tắc nghiệp vụ và Rủi ro

1. **Ràng buộc duy nhất & Quyền hạn**:
   - `teacherCode` là duy nhất trên toàn hệ thống.
   - `userId` liên kết tối đa với một giáo viên (quan hệ 1-1).
   - Quyền hạn giảng dạy/nhập điểm của giáo viên được suy ra từ phân công `subject_teaching_assignment` đang `ACTIVE`, không phụ thuộc vào tổ chuyên môn (`department`).
2. **Quy tắc Phân công**:
   - Một lớp chỉ có tối đa một GVCN `ACTIVE` tại một thời điểm.
   - Một môn học của lớp (`class_subject`) chỉ có tối đa một GVBM `ACTIVE` tại một thời điểm.
   - Khi thay đổi giáo viên (Replace), backend thực hiện đóng phân công cũ và tạo phân công mới; FE cần truyền đúng thông tin thay thế và xử lý reload đồng bộ.
   - Phân công phải nằm trong khoảng thời gian năm học/học kỳ hợp lệ.
3. **Giữ nguyên tính toàn vẹn dữ liệu**:
   - Thay giáo viên không được làm mất điểm hay điểm danh đã phát sinh trước đó.
   - Không cho phép xóa giáo viên nếu giáo viên đó đã có phân công hoặc dữ liệu điểm danh/điểm trong hệ thống (Backend sẽ trả lỗi `400`/`409`).
4. **Không can thiệp Backend**:
   - Mọi endpoint, DTO, authorization đã có sẵn từ backend (Plan 027 & 034). FE tuân thủ đúng contract và không tự ý thay đổi backend hay migration trong plan này.

---

## 9. Tiêu chí Phê duyệt (Approval Gates)

User cần xem xét và phê duyệt các điểm chính sau:

1. **Phạm vi 2 màn hình**:
   - `Teacher Profile UI`: route `/v2/teachers`, menu `Hồ sơ giáo viên`.
   - `Teaching Assignment UI`: route `/v2/teaching-assignments`, menu `Phân công giảng dạy`.
2. **Bố cục Teaching Assignment UI**:
   - Hỗ trợ xem và phân công theo Lớp học (GVCN + GVBM các môn) và xem lịch giảng dạy theo Giáo viên.
   - Hỗ trợ đầy đủ 3 luồng thao tác: Phân công mới, Thay thế giáo viên (Replace), Kết thúc phân công (End).
3. **Xử lý liên kết tài khoản**:
   - Cho phép nhập/chọn `userId` khi tạo hoặc sửa giáo viên.
4. **Cam kết chất lượng**:
   - Đầy đủ unit tests, Storybook stories, coverage cao, không lỗi TypeScript/Lint, và pass 100% build gates.
