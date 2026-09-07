# Project: Student Management V2 Migration (/v2/students)

## Architecture
Hệ thống chuyển đổi toàn diện phân hệ quản lý học sinh từ giao diện đơn lập v1 sang phân hệ học vụ tích hợp v2 nằm trong `AuthenticatedV2ShellView`, đồng bộ dữ liệu hồ sơ cá nhân với tài khoản đăng nhập v3 (`app_user`) và các phân hệ học vụ v2 (xếp lớp, điểm danh, sổ điểm, bảng điểm).

### Sơ đồ kiến trúc & luồng dữ liệu
```
[User Login] ──(safeRedirect: /v2)──> [AuthenticatedV2ShellView (/v2)]
                                          │
    ┌─────────────────────────────────────┼──────────────────────────────────┐
    ▼                                     ▼                                  ▼
[Sidebar V2: Hồ sơ học sinh]    [Danh sách học sinh V2]           [Thêm mới học sinh V2/V3]
(icon: pi pi-user, role guard)  (/v2/students)                   (/v2/students/new)
                                ├── Bảng dữ liệu đa chiều         ├── Chế độ V3: POST /api/v3/students
                                ├── Status tag & Lớp học         │   (User + Student + StudentInfo)
                                ├── Phân trang & sắp xếp          │   (Tự sinh username, mật khẩu an toàn)
                                └── Drill-down ──────────┐        └── Chế độ V1: POST /api/v1/students
                                                         ▼
                                          [Chi tiết học sinh 4 Tabs V2]
                                          (/v2/students/:studentId)
                                          ├── Tab 1: Hồ sơ cá nhân & User
                                          ├── Tab 2: Xếp lớp & Lịch sử chuyển lớp
                                          ├── Tab 3: Chuyên cần & Điểm danh
                                          └── Tab 4: Bảng điểm & Tính lại điểm
```

## Feature Inventory
Mọi tính năng được khảo sát từ Phase 0 đều được định danh và phân bổ vào các Milestone cụ thể dưới đây:

| #   | Feature                          | Description                                                                                                 | Milestone | Source              |
| --- | -------------------------------- | ----------------------------------------------------------------------------------------------------------- | --------- | ------------------- |
| 1   | Login Safe Redirect              | Chuyển hướng an toàn sau đăng nhập về `/v2` thay vì `/students`                                             | M1        | Follow-up Request   |
| 2   | GuestOnly Guard                  | Guard router chuyển hướng người dùng đã login về `/v2`                                                      | M1        | Follow-up Request   |
| 3   | Student V2 Route Subtree         | Định tuyến `/v2/students`, `/v2/students/new`, `/v2/students/:id`, `/v2/students/:id/edit` trong Shell v2   | M1        | R1                  |
| 4   | Sidebar V2 Navigation            | Thêm mục "Hồ sơ học sinh" với icon `pi pi-user`, phân quyền `ADMIN, ACADEMIC_OFFICE, TEACHER`, active state | M1        | R1                  |
| 5   | Bảng học sinh v2                 | Hiển thị `studentCode`, họ tên, ngày sinh, `gender`, `status`, lớp hiện tại                                 | M2        | R2                  |
| 6   | Tra cứu & Lọc đa chiều           | Tìm kiếm theo mã, tên, ngày sinh, trạng thái (`ACTIVE/INACTIVE/GRADUATED`), lớp học                         | M2        | R2                  |
| 7   | Server-side Pagination & Sort    | Phân trang và sắp xếp phía server theo chuẩn API                                                            | M2        | R2                  |
| 8   | Drill-down Chi tiết              | Chuyển tiếp tới màn hình chi tiết học sinh khi click mã hoặc dòng                                           | M2        | R2                  |
| 9   | Safe Lifecycle & Inactivation    | Chặn xóa cứng gây lỗi khóa ngoại, hướng dẫn/chuyển trạng thái `INACTIVE`/`GRADUATED`                        | M2        | R5                  |
| 10  | Tạo học sinh V3 có tài khoản     | Tạo đồng thời User, Student, StudentInfo qua `POST /api/v3/students` trong 1 transaction                    | M3        | R3                  |
| 11  | Sinh username & bảo mật mật khẩu | Backend tự sinh username theo chuẩn, mật khẩu an toàn, không để lộ plaintext password/hash                  | M3        | R3                  |
| 12  | Xử lý lỗi 409 Conflict           | Báo lỗi rõ ràng khi trùng mã học sinh hoặc trùng username                                                   | M3        | R3                  |
| 13  | Tạo học sinh V1 tương thích      | Giữ tùy chọn tạo hồ sơ đơn thuần qua `POST /api/v1/students`                                                | M3        | R3                  |
| 14  | Tab 1: Hồ sơ cá nhân & User      | Xem nhân khẩu học (loại bỏ averageScore cũ), thông tin User ID, username, role                              | M4        | R4                  |
| 15  | Tab 2: Xếp lớp & Lịch sử         | Gọi `GET /api/v2/students/{id}/enrollments`, hiển thị lớp hiện tại & lịch sử chuyển lớp                     | M4        | R4                  |
| 16  | Tab 3: Chuyên cần & Điểm danh    | Gọi `GET /api/v2/attendance/students/{id}/history`, thẻ tỷ lệ có mặt/vắng và bảng lịch sử                   | M4        | R4                  |
| 17  | Tab 4: Bảng điểm & Học bạ        | Gọi `GET /api/v2/transcripts/students/{id}/...`, hiển thị điểm kỳ/năm và trạng thái                         | M4        | R4                  |
| 18  | Nút Yêu cầu tính lại điểm        | Nút gọi `POST /api/v2/students/{id}/transcripts/recalculate` cho ADMIN/ACADEMIC_OFFICE                      | M4        | R4                  |
| 19  | Kiểm thử E2E & Hardening         | Kiểm thử 4 Tiers (Feature, Boundary, Combinations, Real-world) & Adversarial hardening                      | M5        | Acceptance Criteria |

## Milestones

| #   | Name                                                    | Scope                                                                                                                                                     | Dependencies               | Status  |
| --- | ------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------- | ------- |
| M1  | Navigation, Routing & Shell V2 Integration              | Chuyển hướng login về `/v2`, cấu hình route `/v2/students` trong Shell v2, thêm Sidebar item `pi pi-user` và active state, cập nhật unit tests liên quan  | None                       | DONE    |
| M2  | Student List, Multi-dimensional Search & Safe Lifecycle | Nâng cấp `StudentTable.vue`, `StudentSearchForm.vue`, `StudentListView.vue`, hiển thị `studentCode`, trạng thái, lớp, drill-down và cơ chế xóa an toàn R5 | M1                         | PLANNED |
| M3  | Student Creation & Account Provisioning V3              | Tích hợp form tạo học sinh V3 (`POST /api/v3/students`) và V1, bảo mật mật khẩu, xử lý 409 Conflict                                                       | M1                         | PLANNED |
| M4  | Student Detail 4-Tab Workspace                          | Xây dựng `StudentDetailView.vue` với 4 tabs: Hồ sơ & User, Phân lớp, Chuyên cần, Bảng điểm & Tính lại điểm                                                | M1, M2                     | PLANNED |
| M5  | Final E2E Test Verification & Adversarial Hardening     | Phase 1: Đạt 100% bộ kiểm thử E2E 4 Tiers từ E2E Testing Track; Phase 2: Adversarial Coverage Hardening (Tier 5)                                          | M1, M2, M3, M4, TEST_READY | PLANNED |

### Parallel Track: E2E Testing Track
- **M-TEST**: Xây dựng kiến trúc kiểm thử E2E 4 Tiers (Feature Coverage >=5/feature, Boundary/Corner >=5/feature, Pairwise Combinations, Real-World Scenarios), tạo `TEST_INFRA.md` và xuất bản `TEST_READY.md`.

## Interface Contracts

### 1. Frontend Route Contract (`FE/src/router/index.ts`)
- Route cha: `/v2` (`AuthenticatedV2ShellView.vue`)
- Route con:
  - `/v2/students` (name: `'v2-students'`) -> `StudentListView.vue`
  - `/v2/students/new` (name: `'v2-student-create'`) -> `StudentFormView.vue`
  - `/v2/students/:studentId` (name: `'v2-student-detail'`) -> `StudentDetailView.vue`
  - `/v2/students/:studentId/edit` (name: `'v2-student-edit'`) -> `StudentFormView.vue`
- Redirect:
  - `/students` -> redirect sang `/v2/students`
  - `LoginView.vue`: `safeRedirect` fallback -> `'/v2'`
  - Guard `guestOnly`: redirect -> `'/v2'`

### 2. Student V3 Creation Contract
- Endpoint: `POST /api/v3/students`
- Headers: `Authorization: Bearer <token>`, `Content-Type: application/json`
- Request:
  ```json
  {
    "studentCode": "STU0000001",
    "studentName": "Nguyen Van A",
    "dateOfBirth": "2008-01-15",
    "address": "123 Hanoi",
    "username": "vana0000001",
    "password": "InitialPassword123"
  }
  ```
- Response (201 Created):
  ```json
  {
    "studentId": 1,
    "studentCode": "STU0000001",
    "studentName": "Nguyen Van A",
    "dateOfBirth": "2008-01-15",
    "address": "123 Hanoi",
    "account": {
      "userId": 10,
      "username": "vana0000001",
      "role": "STUDENT"
    }
  }
  ```
- Errors: `409 Conflict` (Trùng mã sinh viên / Trùng tên đăng nhập), `400 Bad Request`, `403 Forbidden`.

### 3. Student Detail 4-Tab APIs
- Tab 1: `GET /api/v1/students/{id}` -> `ResStudentDTO`
- Tab 2: `GET /api/v2/students/{id}/enrollments` -> `List<ResStudentEnrollmentHistoryDTO>`
- Tab 3: `GET /api/v2/attendance/students/{id}/history` -> `StudentAttendanceHistoryResponse`
- Tab 4:
  - `GET /api/v2/transcripts/students/{id}/semesters/{semesterId}` -> `ResStudentTermTranscriptDTO`
  - `GET /api/v2/transcripts/students/{id}/academic-years/{academicYearId}` -> `ResStudentAnnualTranscriptDTO`
  - `POST /api/v2/students/{id}/transcripts/recalculate?academicYearId={yearId}` -> `ResCalculationTaskDTO` (202 Accepted)

## Code Layout

### Frontend (`FE/src/`)
- `router/index.ts`: Khai báo route `/v2/students*`, redirect fallback `/v2`
- `views/LoginView.vue`: Fallback redirect về `/v2`
- `views/AuthenticatedV2ShellView.vue`: Sidebar menu "Hồ sơ học sinh" (`pi pi-user`)
- `views/StudentListView.vue`: View danh sách v2
- `views/StudentDetailView.vue`: View chi tiết học sinh 4 tabs
- `views/StudentFormView.vue`: View tạo/sửa học sinh chế độ V3 và V1
- `components/StudentTable.vue`: Bảng học sinh v2 với status tag, lớp, drill-down
- `components/StudentSearchForm.vue`: Bộ lọc đa chiều
- `components/StudentForm.vue`: Form nhập liệu có toggle cấp tài khoản
- `components/student-detail/`: Các components con của 4 tabs (Profile, Enrollment, Attendance, Transcript)
- `services/studentApi.ts`: Client API cho student v1 và v3
- `types/student.ts`: Types và interfaces cho Student V2

### Backend (`BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/`)
- `student/controller/StudentV3Controller.java`: `POST /api/v3/students`
- `student/controller/StudentController.java`: API Student v1
- `student/service/StudentAccountService.java`: Atomic creation logic
- `student/domain/`: Entity, DTOs, Enums

