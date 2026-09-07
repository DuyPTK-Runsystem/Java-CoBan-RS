# Dev Note 071 — Tinh chỉnh phần mềm

Ngày: 07/09/2026

## Căn cứ và phạm vi

- User yêu cầu ghi nhận toàn bộ thay đổi của phiên vào Note 071 và không cần Developer Plan. Vì vậy không tạo hoặc cập nhật Developer Plan cho Note 071.
- Note này ghi nhận các tinh chỉnh frontend/backend trong phiên: giảm chữ phụ trên giao diện, giới hạn menu Student và Teacher, Việt hóa workspace học sinh, sửa flow chuyển lớp khi lớp đích chưa có cột điểm và tự tạo cấu hình lớp-môn theo Khối.
- Không bao gồm `BE/BaiTap-RS/src/main/resources/application.properties`: đây là thay đổi có sẵn trong worktree (`java_coban` thành `java_cobann`), không thuộc phiên này.

## Scope đã hoàn thành

### Tinh gọn nội dung giao diện

- Xóa eyebrow và mô tả phụ tại tiêu đề của các workspace học vụ; giữ `h1`, hành động, trạng thái và dữ liệu ngữ cảnh cần thiết. Trang Học kỳ vẫn giữ thông tin năm học/khoảng ngày động.
- Xóa hai mô tả kỹ thuật ở lịch sử chuyên cần của Student.
- Xóa mô tả “Tìm theo mã năm học và lọc trên danh sách hiện tại.” tại danh sách Năm học.

### Menu và route Student

- Student chỉ thấy hai tab: Điểm danh và Bảng điểm.
- Student truy cập URL học vụ khác, kể cả URL legacy hoặc unknown bên trong `/v2`, được chuyển về Điểm danh và giữ phiên đăng nhập.
- Tài khoản có bất kỳ quyền nhân sự `ADMIN`, `ACADEMIC_OFFICE` hoặc `TEACHER` tiếp tục dùng menu hiện có.

### Việt hóa `/v2/students`

- Việt hóa danh sách, bộ lọc, bảng, form thêm/sửa, chi tiết, trạng thái, vai trò, validation, empty state, action labels và accessibility labels.
- Thêm locale PrimeVue tiếng Việt cho ứng dụng và Storybook: lịch, phân trang, danh sách chọn, dialog và nhãn hỗ trợ truy cập.
- Dịch các fallback HTTP mặc định khi hiển thị ở workspace học sinh; giữ nguyên thông điệp nghiệp vụ do backend trả về và dữ liệu người dùng.

### Chuyển lớp khi lớp đích không có cột điểm

- Bỏ guard FE chặn confirm khi snapshot không có target column.
- Dialog emit request với `scores: []`; `EnrollmentListView` gọi `POST /api/v2/enrollments/{enrollmentId}/transfer` thay vì `transfer-with-scores`.
- Thông báo thành công chỉ nói đã chuyển lớp khi không có điểm đích; chỉ nói đã lưu điểm khi thật sự có score entries.
- Thêm regression test và Storybook state `NoTargetColumns`.

### Quyền Teacher

- Teacher không thấy và không thể truy cập trực tiếp các tab Năm học & học kỳ, Khối, Xếp lớp, Hồ sơ học sinh, Phân công giảng dạy, Sổ điểm, Yêu cầu sửa điểm, Kết quả thi lại và Vận hành tính điểm; route bị chuyển về Điểm danh.
- Với Lớp, Môn học và Quản lí môn học các lớp, Teacher chỉ xem dữ liệu. UI ẩn các thao tác tạo, sửa, xóa, đóng/mở trạng thái và cấu hình; hiển thị nhãn “Chỉ xem”.

### Tự tạo cấu hình Môn học – Lớp – Học kỳ theo Khối

- Khi tạo cấu hình áp dụng môn học phạm vi `GRADE`, backend tìm toàn bộ lớp cùng Khối trong năm học của học kỳ và tạo `ClassSubject` hoạt động cho từng lớp chưa có bộ ba môn học–lớp–học kỳ tương ứng.
- Cấu hình đã có không bị tạo trùng; phạm vi `CLASS` không tự tạo thêm lớp khác.

### Chỉnh copy bổ sung

- Trong chi tiết giáo viên, đổi nút “Xem lịch sử phân công” thành “Xem phân công”.
- Trong form giáo viên, bỏ trường và hướng dẫn “User ID liên kết”; liên kết hiện hữu vẫn được giữ nguyên khi cập nhật.
- Trong popup gán môn cho lớp, hai nhãn trạng thái dùng “Trạng thái”; bỏ mô tả về trạng thái mặc định và không xóa lịch sử.

### Cấp tài khoản khi tạo học sinh

- Form tạo học sinh mặc định chọn cấp tài khoản đăng nhập; đổi copy bỏ hậu tố “(V3)” và nêu rõ vai trò Học sinh.
- Khi bỏ trống username, giao diện nói rõ hệ thống tự sinh username và hiển thị sau khi tạo thành công.
- Hiển thị mật khẩu mặc định `12345678`; backend đã dùng giá trị này khi request không có mật khẩu.
- Sau khi API V3 tạo thành công, popup hiển thị đúng `account.username` từ response trước khi trở về danh sách.

### Bảng điểm lớp

- Phóng to hai nút “Bảng điểm theo môn” và “Bảng điểm tổng kết”.
- Bỏ các mô tả tóm tắt dưới tiêu đề bảng điểm theo môn và tổng kết học kỳ.
- Bảng tổng kết cả năm không còn cột Ghi chú và các nội dung kết quả tự sinh như “Lên lớp thẳng” hoặc “Lên lớp sau thi lại”.

### Phân công giảng dạy

- Bỏ mô tả phụ tại phần phân công giáo viên chủ nhiệm và danh sách phân công của giáo viên.

### Điểm danh

- Bỏ các nhãn kỹ thuật về session, backend authorization và bản ghi `PRESENT`.
- Dùng “buổi điểm danh”, “cập nhật điểm danh” và “trạng thái ghi nhận” trên giao diện; không hiển thị thuật ngữ Ngoại lệ/exception.
- Đổi thông báo lịch học thành “Ngày và buổi học hợp lệ để mở buổi điểm danh.”
- Bỏ hai chú thích kỹ thuật ở báo cáo chuyên cần theo lớp.

### Điều hướng sau đăng nhập

- Sau khi đăng nhập thành công, `LoginView` luôn điều hướng tới tab đầu tiên mà role của user được phép mở (`ADMIN`/`ACADEMIC_OFFICE`: Năm học, `TEACHER`: Lớp, `STUDENT`: Điểm danh), không dùng redirect query để bỏ qua thứ tự này.
- Bổ sung regression assertions cho LoginView và router guard.

## File thay đổi

- Menu/route: `FE/src/services/studentNavigation.ts`, `FE/src/router/index.ts`, `FE/src/router/index.spec.ts`, `FE/src/views/AuthenticatedV2ShellView.vue`, `FE/src/views/AuthenticatedV2ShellView.spec.ts`, `FE/src/views/LoginView.vue`, `FE/src/views/LoginView.spec.ts`.
- Workspace học sinh và thành phần dùng chung: `FE/src/views/Student*.vue`, `FE/src/components/Student*.vue`, các test tương ứng, `FE/src/utils/studentUiMessage.ts`, `FE/src/locales/vi.ts`, `FE/src/main.ts`, `FE/.storybook/preview.ts`, `AuthenticatedLayout`, `EmptyState`, `TranscriptStatusCard`, `TranscriptAnnualTable`.
- Nội dung giao diện: `AttendanceHistoryPanel.vue`, các view academic/enrollment/attendance/scorebook/transcript liên quan.
- Chuyển lớp: `TransferScoreAssistDialog.vue`, `.spec.ts`, `.stories.ts`, `EnrollmentListView.vue`, `.spec.ts`.
- Teacher read-only: `AuthenticatedV2ShellView.vue`, router, các view/table Lớp, Môn học, Quản lí môn học các lớp và test liên quan.
- Copy bổ sung: `TeacherDetailDialog.vue`, `ClassSubjectDialog.vue`.
- Cấp tài khoản học sinh: `StudentForm.vue`, `StudentFormView.vue` và unit test tương ứng.
- Bảng điểm lớp: `ClassTranscriptViewerView.vue`, `ClassSummaryTranscriptTable.vue`, `ClassSubjectTranscriptTable.vue` và test liên quan.
- Phân công: `HomeroomAssignmentCard.vue`, `TeacherAssignmentScheduleTable.vue`.
- Điểm danh: `AttendanceWorkspaceView.vue`, `AttendanceContextPanel.vue`, `AttendanceSessionTable.vue`, `AttendanceExceptionDialog.vue`, `AttendanceHistoryPanel.vue` và Storybook context.
- Báo cáo chuyên cần: `ClassAttendanceSummaryPanel.vue`.
- Backend tự tạo cấu hình: `SubjectApplicabilityService.java`, `ClassSubjectApplicabilityProvisioningService.java`, repository `ClassSubjectApplicabilityClassRepository`/`ClassSubjectRepository` và hai unit test dịch vụ.
- Chỉ mục Dev Note: `document/dev-note/fe/FE_DEV_NOTE_SUMMARY.md`, `document/dev-note/summary/DEV_NOTE_SUMMARY.md`.

## Quyết định kỹ thuật

- Backend vẫn là nguồn quyết định quyền và dữ liệu. Route/menu chỉ giới hạn điều hướng UX.
- Không tự map cột điểm giữa hai lớp. Snapshot backend vẫn quyết định `targetColumns`; không có cột đích là transfer thường, không phải lỗi.
- Không đổi API request, enum wire value, dữ liệu lịch sử, quy tắc tính điểm hoặc backend.
- Việc tạo lớp-môn chạy trong transaction tạo cấu hình phạm vi môn học, chỉ thêm tuple đang thiếu để bảo toàn dữ liệu hiện hữu.

## Validation thực tế

| Kiểm tra | Kết quả |
| --- | --- |
| `npm run lint` | PASS |
| `npm run build` | PASS |
| `npm run build-storybook` | PASS; có cảnh báo kích thước bundle |
| Focused Vitest transfer | PASS, 7/7 tests |
| `npm run test` | FAIL; 454/455 tests, 82/83 files PASS |
| `npm run test:coverage` | FAIL; 454/455 tests, 82/83 files PASS |
| `git diff --check` | PASS |
| Teacher focused Vitest | PASS, 79/79 tests |
| Popup gán môn: `npm run lint`, `npm run build` | PASS |
| Focused Vitest `StudentForm` và `StudentFormView`; `npm run lint` | PASS, 13/13 tests |
| Focused Vitest bảng điểm lớp; `npm run lint` | PASS, 7/7 tests |
| Focused Vitest điểm danh; `npm run lint` | PASS, 8/8 tests |
| Focused Vitest `LoginView` + router | PASS, 61/61 tests |
| `npm run build` (FE) | PASS |
| Backend focused Gradle test cho hai dịch vụ cấu hình | PASS |
| `./gradlew test checkstyleMain pmdMain build` | FAIL tại PMD baseline: 75 violations test và sau khi tách dịch vụ còn 5 violations main; focused test backend PASS |
| Browser visual QA và live mutation | NOT RUN |

Hai full-suite failures cùng là test có sẵn `src/services/apiClient.spec.ts`: kỳ vọng giữ session khi HTTP 403 nhưng nhận token `null`. Lỗi này không thuộc các thay đổi trong Note 071.

## Sai khác và vấn đề còn lại

- Không có Developer Plan cho Note 071 theo yêu cầu trực tiếp của user.
- Các Plan/Dev Note tạm được tạo trong phiên đã được hợp nhất vào Note 071; Plan 069 và Dev Note 069 trước phiên vẫn được giữ.
- Không thực hiện browser mutation thật vì chưa có môi trường dữ liệu có thể reset.
- Validation toàn bộ backend chưa đạt vì baseline PMD; cần xử lý riêng các vi phạm trong report PMD trước khi có thể báo PASS toàn bộ build.
