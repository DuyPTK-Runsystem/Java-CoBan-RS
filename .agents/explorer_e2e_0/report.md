# BÁO CÁO KHẢO SÁT TOÀN DIỆN TÍCH HỢP END-TO-END VÀ THIẾT KẾ KIẾN TRÚC BỘ KIỂM THỬ E2E 4 TIERS
**Dự án**: Java-CoBan (Phân hệ Quản lý Học sinh & Học vụ v2)  
**Agent**: E2E Integration Explorer (`explorer_e2e_0`)  
**Thời gian hoàn thành**: 2026-09-04  
**Trạng thái**: Hoàn tất khảo sát — Sẵn sàng chuyển giao đội ngũ thực thi  

---

## MỤC LỤC
1. [TỔNG QUAN VÀ MỤC TIÊU KHẢO SÁT](#1-tổng-quan-và-mục-tiêu-khảo-sát)
2. [ĐỐI CHIẾU VÀ ĐÁNH GIÁ CÁC YÊU CẦU CỐT LÕI (R1 – R5 & LOGIN REDIRECT)](#2-đối-chiếu-và-đánh-giá-các-yêu-cầu-cốt-lõi-r1--r5--login-redirect)
   - [2.1. Yêu cầu chuyển hướng Login Redirect '/v2' (Follow-up)](#21-yêu-cầu-chuyển-hướng-login-redirect-v2-follow-up)
   - [2.2. R1: Tích hợp Tuyến đường và Shell v2 cho Học sinh (/v2/students)](#22-r1-tích-hợp-tuyến-đường-và-shell-v2-cho-học-sinh-v2students)
   - [2.3. R2: Nâng cấp Màn hình Danh sách và Tra cứu Học sinh v2](#23-r2-nâng-cấp-màn-hình-danh-sách-và-tra-cứu-học-sinh-v2)
   - [2.4. R3: Tích hợp Tạo học sinh kèm Cấp tài khoản Đăng nhập (Student V3)](#24-r3-tích-hợp-tạo-học-sinh-kèm-cấp-tài-khoản-đăng-nhập-student-v3)
   - [2.5. R4: Màn hình Chi tiết & Chỉnh sửa Học sinh Đa phân hệ 4 Tabs](#25-r4-màn-hình-chi-tiết--chỉnh-sửa-học-sinh-đa-phân-hệ-4-tabs)
   - [2.6. R5: Chuẩn hóa Vòng đời Học sinh và Chính sách Xóa an toàn](#26-r5-chuẩn-hóa-vòng-đời-học-sinh-và-chính-sách-xóa-an-toàn)
3. [MA TRẬN PHÂN QUYỀN TOÀN DIỆN (END-TO-END ROLE MATRIX)](#3-ma-trận-phân-quyền-toàn-diện-end-to-end-role-matrix)
4. [BỘ THIẾT KẾ KIỂM THỬ E2E ĐA TẦNG (4 TIERS ARCHITECTURE)](#4-bộ-thiết-kế-kiểm-thử-e2e-đa-tầng-4-tiers-architecture)
   - [4.1. Tier 1: Feature Coverage Tests (>= 5 cases / feature)](#41-tier-1-feature-coverage-tests--5-cases--feature)
   - [4.2. Tier 2: Boundary & Corner Cases (>= 5 cases / feature)](#42-tier-2-boundary--corner-cases--5-cases--feature)
   - [4.3. Tier 3: Cross-Feature & Pairwise Combinations](#43-tier-3-cross-feature--pairwise-combinations)
   - [4.4. Tier 4: Real-World Application Scenarios (Persona Walkthroughs)](#44-tier-4-real-world-application-scenarios-persona-walkthroughs)
5. [PHƯƠNG THỨC VẬN HÀNH KIỂM THỬ VÀ CÔNG CỤ ĐẢM BẢO CHẤT LƯỢNG](#5-phương-thức-vận-hành-kiểm-thử-và-công-cụ-đảm-bảo-chất-lượng)
6. [KẾ HOẠCH HÀNH ĐỘNG VÀ KHUYẾN NGHỊ CHO CÁC NHÓM TRIỂN KHAI](#6-kế-hoạch-hành-động-và-khuyến-nghị-cho-các-nhóm-triển-khai)

---

## 1. TỔNG QUAN VÀ MỤC TIÊU KHẢO SÁT

Dự án **Java-CoBan** đang trong giai đoạn chuyển đổi kiến trúc quan trọng: chuyển đổi từ giao diện quản lý học sinh đơn lập legacy v1 (`/students`) sang phân hệ học vụ tích hợp v2 (`/v2/students`) nằm trong `AuthenticatedV2ShellView`. Quá trình này kết nối chặt chẽ hồ sơ nhân khẩu học với tài khoản đăng nhập người dùng (Student V3 API - `POST /api/v3/students`) và toàn bộ chuỗi mắt xích học vụ v2 (phân lớp, điểm danh, sổ điểm, bảng điểm).

Nhiệm vụ của **E2E Integration Explorer**:
- Khảo sát mã nguồn thực tế tại Frontend (`FE/`) và Backend (`BE/BaiTap-RS/`).
- Đối chiếu hiện trạng so với toàn bộ yêu cầu **R1 - R5** và yêu cầu **chuyển hướng login redirect sang `/v2`**.
- Xây dựng **Ma trận phân quyền chi tiết (Role Matrix)** cho từng hành động trên giao diện (UI) và API endpoint giữa 4 vai trò: `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`, `STUDENT`.
- Thiết kế **Bộ kiểm thử E2E đa tầng 4 Tiers** đạt chuẩn mực kiểm thử nghiêm ngặt:
  - **Tier 1 (Feature Coverage)**: Đảm bảo tối thiểu 5 test case cho mỗi tính năng cốt lõi.
  - **Tier 2 (Boundary & Corner cases)**: Đảm bảo tối thiểu 5 test case biên/dị thường cho mỗi tính năng.
  - **Tier 3 (Cross-Feature Combinations)**: Kiểm thử tính liên kết nghiệp vụ xuyên suốt giữa Tạo học sinh -> Xếp lớp -> Điểm danh -> Vào điểm -> Bảng điểm.
  - **Tier 4 (Real-World Scenarios)**: Kịch bản người dùng thực tế hoàn chỉnh theo từng vai trò (Persona walkthroughs).

---

## 2. ĐỐI CHIẾU VÀ ĐÁNH GIÁ CÁC YÊU CẦU CỐT LÕI (R1 – R5 & LOGIN REDIRECT)

### 2.1. Yêu cầu chuyển hướng Login Redirect '/v2' (Follow-up)
- **Tài liệu căn cứ**: `ORIGINAL_REQUEST.md#L53-L56`.
- **Yêu cầu**: Cấu hình chuyển hướng sau khi đăng nhập thành công vào thẳng `/v2` thay vì `/students` như trong hệ thống legacy.
- **Hiện trạng mã nguồn**:
  1. `FE/src/views/LoginView.vue#L17, L23`:
     - `const successRedirect = ref('/students')` đang trỏ cứng về `/students`.
     - Hàm `safeRedirect()` có fallback mặc định: `: '/students'`.
  2. `FE/src/router/index.ts#L159-L161`:
     ```ts
     if (to.meta.guestOnly && authenticated) {
       return { name: 'students' }
     }
     ```
     Đang redirect người dùng đã đăng nhập khi vào `/login` hoặc `/register` về route `students`.
  3. `FE/src/views/LoginView.spec.ts#L57, L80`: Unit test đang assert chuyển hướng về `/students/new` hoặc `/students`.
- **Đánh giá tích hợp & Giải pháp chuyển đổi**:
  - `LoginView.vue`: Cập nhật `successRedirect = ref('/v2')` và fallback của `safeRedirect()` thành `'/v2'`.
  - `router/index.ts`: Cập nhật navigation guard `guestOnly` trả về `{ path: '/v2' }` (hoặc route name `v2-shell`).
  - Unit tests: Cập nhật các test case trong `LoginView.spec.ts` và router test để kỳ vọng chuyển hướng về `/v2`.

### 2.2. R1: Tích hợp Tuyến đường và Shell v2 cho Học sinh (/v2/students)
- **Tài liệu căn cứ**: `ORIGINAL_REQUEST.md#L12-L13, L33-L36`.
- **Yêu cầu**: Chuyển đổi tuyến đường FE từ độc lập `/students` sang tuyến đường con `/v2/students` bên trong `AuthenticatedV2ShellView`, hiển thị mục "Hồ sơ học sinh" trên sidebar theo vai trò (`ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`) với icon `pi pi-user`, đồng bộ active state khi điều hướng. Người dùng chưa đăng nhập hoặc không đủ quyền bị chặn an toàn.
- **Hiện trạng mã nguồn**:
  1. `FE/src/router/index.ts#L35-L51`: Các route `/students`, `/students/new`, `/students/:studentId/edit` vẫn nằm ở root level, độc lập với `/v2`.
  2. `FE/src/views/AuthenticatedV2ShellView.vue#L13-L23`: Sidebar navigation list hiện chưa có mục "Hồ sơ học sinh" (`/v2/students`).
  3. `FE/src/views/AuthenticatedV2ShellView.vue#L24-L48`: Logic phân quyền sidebar hiện tại chỉ mới xử lý ẩn/hiện Bảng điểm (`/v2/transcripts`) và Bảng điểm theo lớp (`/v2/class-transcripts`), chưa bổ sung điều kiện ẩn "Hồ sơ học sinh" đối với tài khoản `STUDENT`.
- **Đánh giá tích hợp & Kiến trúc đích**:
  - Di chuyển/bổ sung route con trong `/v2`:
    ```ts
    {
      path: 'students',
      name: 'v2-students',
      component: () => import('@/views/StudentListView.vue'),
    },
    {
      path: 'students/new',
      name: 'v2-student-create',
      component: () => import('@/views/StudentFormView.vue'),
    },
    {
      path: 'students/:studentId',
      name: 'v2-student-detail',
      component: () => import('@/views/StudentDetailView.vue'),
    },
    {
      path: 'students/:studentId/edit',
      name: 'v2-student-edit',
      component: () => import('@/views/StudentFormView.vue'),
    }
    ```
  - Trong `AuthenticatedV2ShellView.vue`:
    - Thêm item:
      ```ts
      if (isNonStudent) {
        items.splice(5, 0, {
          label: 'Hồ sơ học sinh',
          to: '/v2/students',
          icon: 'pi pi-user',
          active: route?.path.startsWith('/v2/students'),
        })
      }
      ```
    - Tuyệt đối không hiển thị menu này cho tài khoản có vai trò `STUDENT`.

### 2.3. R2: Nâng cấp Màn hình Danh sách và Tra cứu Học sinh v2
- **Tài liệu căn cứ**: `ORIGINAL_REQUEST.md#L15-L16, L38-L41`, `CR-STUDENT-001`.
- **Yêu cầu**: Hiển thị thông tin học sinh kết hợp trạng thái học vụ (`Status: ACTIVE/INACTIVE/GRADUATED`), định danh mã học sinh `studentCode` chuẩn `STU` + 7 chữ số (theo `CR-STUDENT-001`), ngày sinh, giới tính và lớp học hiện tại; hỗ trợ bộ lọc đa chiều (mã, tên, ngày sinh, lớp, trạng thái) và drill-down sang chi tiết học vụ. Phân trang, sắp xếp server-side chính xác.
- **Hiện trạng mã nguồn**:
  1. `FE/src/views/StudentListView.vue`: Sử dụng `AuthenticatedLayout` trực tiếp thay vì nằm trong Shell v2 router-outlet. Gọi `fetchStudents` qua `FE/src/services/studentApi.ts`.
  2. `FE/src/types/student.ts#L1-L8`: Interface `Student` hiện tại chỉ có `studentId`, `studentCode`, `studentName`, `dateOfBirth`, `address`, `averageScore`. Chưa có `status`, `gender`, `currentClass`.
  3. `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/domain/entity/Student.java#L45-L48`: Entity `Student` đã có sẵn enum `status` (`ACTIVE`, `INACTIVE`, `GRADUATED`) và `userId`.
  4. `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/domain/DTOs/response/ResStudentDTO.java`: DTO trả về hiện tại của `GET /api/v1/students` chỉ bao gồm `studentId`, `studentCode`, `studentName`, `dateOfBirth`, `address`, `averageScore`.
- **Đánh giá tích hợp & Điểm nghẽn (Gaps)**:
  - **Dữ liệu trạng thái (`status`)**: Backend entity đã có trường này nhưng `ResStudentDTO` chưa map ra ngoài. Cần bổ sung trường `status` vào DTO phản hồi hoặc mở rộng view mapper để FE hiển thị Tag trạng thái.
  - **Lớp hiện tại (`currentClass`)**: Trong kiến trúc v2, quan hệ phân lớp nằm ở bảng `student_year_enrollment` (module Enrollment). FE có thể hiển thị thông tin lớp khi drill-down vào chi tiết qua `GET /api/v2/students/{id}/enrollments`, hoặc API danh sách cần hỗ trợ join nhẹ để trả về lớp học năm hiện tại.
  - **Giới tính (`gender`)**: Trong Data Model v2 (`document/application-doc/v2/data-model/03-StudentsAndEnrollment.md#L30`), bảng `student_info` có cột `gender VARCHAR(20) NULL`.

### 2.4. R3: Tích hợp Tạo học sinh kèm Cấp tài khoản Đăng nhập (Student V3)
- **Tài liệu căn cứ**: `ORIGINAL_REQUEST.md#L18-L19, L43-L47`, `CR-STUDENT-001`, Dev Note 043.
- **Yêu cầu**: Cung cấp tùy chọn cấp tài khoản đăng nhập tự động qua `POST /api/v3/students` (dành cho `ADMIN` / `ACADEMIC_OFFICE`), tự sinh username theo chuẩn và mật khẩu khởi tạo an toàn, đồng thời giữ tùy chọn tạo hồ sơ đơn thuần qua `POST /api/v1/students` khi cần tương thích. Không để lộ plaintext password/hash trên FE. Xử lý lỗi trùng lặp (409 Conflict).
- **Hiện trạng mã nguồn**:
  1. `BE/BaiTap-RS/.../StudentV3Controller.java#L28-L38`: Endpoint `POST /api/v3/students` **đã được triển khai hoàn chỉnh** trong backend!
     - Được bảo vệ bởi `@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")`.
     - Gọi `StudentAccountService.createStudentWithAccount(request)`.
  2. `BE/BaiTap-RS/.../StudentAccountService.java#L43-L70`:
     - Chạy trong 1 Transaction `@Transactional` bảo toàn tính nguyên tử (Atomicity): nếu lỗi bất kỳ bước nào, toàn bộ `User` và `Student` đều rollback.
     - Kiểm tra duplicate `studentCode` -> bắn `409 Conflict` ("Mã sinh viên đã tồn tại").
     - Tự động sinh `username` qua `StudentUsernameGenerator` nếu request truyền `username == null`. Quy tắc: họ tên không dấu + 7 số cuối của studentCode, fallback tối đa 20 ký tự.
     - Kiểm tra duplicate `username` -> bắn `409 Conflict` ("Tên đăng nhập đã tồn tại").
     - Gán mật khẩu mặc định `12345678` nếu `password == null`, hash an toàn bằng BCrypt qua `PasswordEncoder`.
     - Gán role `STUDENT` cho User mới.
     - Trả về `ResStudentWithAccountDTO` với thông tin `account: { userId, username, role: "STUDENT" }`. Tuyệt đối không trả về password hay passwordHash.
  3. `FE/src/views/StudentFormView.vue#L16`: Frontend hiện tại chỉ gọi `createStudent` (`POST /api/v1/students`), chưa có switch chọn cấp tài khoản và chưa tích hợp gọi `POST /api/v3/students`.
- **Đánh giá tích hợp**:
  - Backend API v3 đã hoàn toàn sẵn sàng và có unit/integration test đầy đủ.
  - Phía Frontend cần:
    - Bổ sung hàm `createStudentWithAccount(token, payload)` trong `FE/src/services/studentApi.ts`.
    - Cập nhật Form tạo học sinh với checkbox/toggle: "Cấp tài khoản đăng nhập hệ thống" (mặc định bật cho Admin/Giáo vụ).
    - Hiển thị thông báo kết quả thành công kèm `username` được tạo để bàn giao cho học sinh, với cảnh báo đổi mật khẩu lần đầu.
    - Xử lý lỗi 409 Conflict thân thiện khi trùng mã HS hoặc username.

### 2.5. R4: Màn hình Chi tiết & Chỉnh sửa Học sinh Đa phân hệ 4 Tabs
- **Tài liệu căn cứ**: `ORIGINAL_REQUEST.md#L21-L26, L41`.
- **Yêu cầu**: Tổ chức màn hình chi tiết học sinh v2 dạng Tabbed Workspace gồm 4 Tabs:
  - **Tab 1 - Hồ sơ cá nhân**: Thông tin nhân khẩu học (loại bỏ `averageScore`, thêm `gender`, `status`), thông tin tài khoản đăng nhập (User ID, username, role).
  - **Tab 2 - Phân lớp & Lịch sử chuyển lớp**: Gọi `GET /api/v2/students/{id}/enrollments` hiển thị lớp học hiện tại và lịch sử chuyển lớp.
  - **Tab 3 - Chuyên cần / Điểm danh**: Gọi `GET /api/v2/attendance/students/{id}/history` hiển thị tỷ lệ có mặt, vắng mặt có phép/không phép.
  - **Tab 4 - Bảng điểm & Học bạ**: Tích hợp `TranscriptViewerView` hoặc gọi API bảng điểm v2 `GET /api/v2/transcripts/students/{id}/...`, cung cấp nút yêu cầu tính lại điểm khi có thẩm quyền.
- **Hiện trạng mã nguồn Backend**:
  - Tab 1: `GET /api/v1/students/{studentId}` (hoặc lookup theo code `GET /api/v1/students/code/{studentCode}`).
  - Tab 2: `GET /api/v2/students/{studentId}/enrollments` tại `EnrollmentController.java#L101-L109`. Trả về `List<ResStudentEnrollmentHistoryDTO>` gồm `enrollment` (lớp hiện tại, năm học, trạng thái) và danh sách `transfers` (từ lớp, đến lớp, ngày hiệu lực, lý do).
  - Tab 3: `GET /api/v2/attendance/students/{studentId}/history` tại `AttendanceHistoryController.java#L40-L50`. Cho phép `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER` xem lịch sử chuyên cần, trả về thẻ tổng hợp `Summary` (tỷ lệ có mặt, vắng có phép, vắng không phép, đi muộn, về sớm) và danh sách chi tiết từng buổi học.
  - Tab 4: `GET /api/v2/transcripts/students/{studentId}/semesters/{semesterId}` và `academic-years/{academicYearId}` tại `TranscriptQueryController.java#L78-L100`. Được bảo vệ bởi `TranscriptAccessGuard` (Admin/Giáo vụ xem toàn trường, Giáo viên chỉ xem lớp mình dạy/chủ nhiệm). Tác vụ tính lại điểm: `POST /api/v2/students/{studentCode}/transcripts/recalculate?academicYearId=...` tại `CalculationTaskController.java#L83-L93` (chỉ cho phép `ADMIN`, `ACADEMIC_OFFICE`).
- **Hiện trạng mã nguồn Frontend**:
  - Chưa có màn hình `StudentDetailView.vue` tích hợp 4 Tabs.
  - Đã có sẵn các component bảng điểm mạnh mẽ trong `TranscriptViewerView.vue` (`TranscriptTermTable.vue`, `TranscriptAnnualTable.vue`, `TranscriptStatusCard.vue`) có thể tái sử dụng trực tiếp cho Tab 4.

### 2.6. R5: Chuẩn hóa Vòng đời Học sinh và Chính sách Xóa an toàn
- **Tài liệu căn cứ**: `ORIGINAL_REQUEST.md#L28-L29`.
- **Yêu cầu**: Thay thế cơ chế xóa cứng (`DELETE /api/v1/students/{id}`) gây vỡ dữ liệu lịch sử bằng cảnh báo ràng buộc khóa ngoại chặt chẽ hoặc chuyển đổi trạng thái hồ sơ (`Status: INACTIVE/GRADUATED`) để bảo toàn lịch sử phân lớp, điểm danh và sổ điểm theo quy định v2.
- **Hiện trạng mã nguồn**:
  1. `BE/BaiTap-RS/.../StudentService.java#L77-L80`:
     ```java
     @Transactional
     public void deleteStudent(Long studentId) {
         studentRepository.delete(support.find(studentId));
     }
     ```
     Phương thức xóa hiện tại là hard-delete thẳng vào cơ sở dữ liệu. Nếu học sinh đã được xếp lớp (`student_year_enrollment`), có bản ghi điểm danh hoặc điểm số, lệnh DELETE này sẽ vi phạm Foreign Key Constraint và ném ra lỗi `DataIntegrityViolationException` (HTTP 500 hoặc 409).
  2. `document/application-doc/v2/frontend-api/01-auth-student.md#L398-L405` (Delete/lifecycle gap): Tài liệu quy định rõ ràng rằng vì backend chưa có endpoint riêng cho lifecycle status, FE không được tự ý bịa ra endpoint không có thật mà phải:
     - Chặn xóa cứng trên UI đối với những học sinh đã phát sinh dữ liệu học vụ.
     - Hiển thị thông báo xác nhận và cảnh báo ràng buộc: "Học sinh đã có lịch sử xếp lớp, điểm danh hoặc điểm số trong hệ thống. Không thể xóa để bảo toàn dữ liệu học bạ. Vui lòng cập nhật trạng thái hồ sơ sang Không hoạt động (INACTIVE) hoặc Đã tốt nghiệp (GRADUATED)."
     - Đối với học sinh mới tạo nhầm chưa hề có dữ liệu liên kết, mới cho phép thực hiện thao tác xóa.

---

## 3. MA TRẬN PHÂN QUYỀN TOÀN DIỆN (END-TO-END ROLE MATRIX)

Bảng phân quyền đối chiếu giữa Frontend UI Action và Backend API Endpoints dựa trên `ActualPermissionMatrix.md`, `StudentController`, `StudentV3Controller`, `EnrollmentController`, `AttendanceHistoryController`, và `TranscriptQueryController`:

| Phân hệ / Tính năng | Hành động trên Giao diện (FE Action) | Phương thức & API Endpoint Backend | ADMIN | ACADEMIC_OFFICE | TEACHER | STUDENT | Cơ chế kiểm soát & Ghi chú kỹ thuật |
|---|---|---|:---:|:---:|:---:|:---:|---|
| **Điều hướng Shell v2** | Menu "Hồ sơ học sinh" trên Sidebar | Navigation Link: `/v2/students` | ✅ Cho phép | ✅ Cho phép | ✅ Cho phép | ❌ **Ẩn hoàn toàn** | Sidebar ẩn tab; nếu Student cố truy cập URL trực tiếp -> Router guard điều hướng về `/v2/transcripts`. |
| **Điều hướng Shell v2** | Menu "Bảng điểm cá nhân" trên Sidebar | Navigation Link: `/v2/transcripts` | ❌ Ẩn | ❌ Ẩn | ❌ Ẩn | ✅ **Hiển thị** | Dành riêng cho học sinh tự tra cứu bảng điểm học kỳ/cả năm của mình. |
| **Điều hướng Shell v2** | Menu "Bảng điểm theo lớp" trên Sidebar | Navigation Link: `/v2/class-transcripts` | ✅ Cho phép | ✅ Cho phép | ✅ Cho phép | ❌ **Ẩn hoàn toàn** | Nhân sự tra cứu điểm theo lớp; hỗ trợ drill-down sang chi tiết cá nhân. |
| **Danh sách học sinh** | Xem danh sách, tìm kiếm, phân trang | `GET /api/v1/students` | ✅ Cho phép | ✅ Cho phép | ✅ Cho phép | ❌ **Chặn (403)** | Backend: `@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')")`. Phân trang server-side. |
| **Hồ sơ học sinh** | Xem chi tiết hồ sơ theo ID | `GET /api/v1/students/{studentId}` | ✅ Cho phép | ✅ Cho phép | ✅ Cho phép | ❌ **Chặn (403)** | Backend kiểm soát tại Class-level. Trả về thông tin nhân khẩu học cơ bản. |
| **Hồ sơ học sinh** | Tra cứu hồ sơ theo mã học sinh | `GET /api/v1/students/code/{studentCode}` | ✅ Cho phép | ✅ Cho phép | ✅ Cho phép | ❌ **Chặn (403)** | Tra cứu định danh nghiệp vụ thân thiện `CR-STUDENT-001`. |
| **Tạo mới học sinh (V3)** | Tạo hồ sơ và tự động cấp tài khoản | `POST /api/v3/students` | ✅ **Cho phép** | ✅ **Cho phép** | ❌ **Chặn (403)** | ❌ **Chặn (403)** | Backend: `@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")`. Atomic transaction tạo User + Role STUDENT + Student + StudentInfo. |
| **Tạo mới học sinh (V1)** | Tạo hồ sơ đơn thuần không kèm tài khoản | `POST /api/v1/students` | ✅ Cho phép | ✅ Cho phép | ⚠️ *BE Class-level* | ❌ **Chặn (403)** | Khuyến nghị siết chặt `@PreAuthorize` ở BE để ngăn Teacher tạo học sinh theo đúng baseline BR-AUTH-005. |
| **Chỉnh sửa hồ sơ** | Cập nhật thông tin nhân khẩu học | `PUT /api/v1/students/{studentId}` | ✅ Cho phép | ✅ Cho phép | ⚠️ *BE Class-level* | ❌ **Chặn (403)** | Khuyến nghị siết chặt `@PreAuthorize` ở BE chỉ cho phép Admin và Giáo vụ chỉnh sửa. |
| **Xóa hồ sơ học sinh** | Xóa bản ghi học sinh | `DELETE /api/v1/students/{studentId}` | ✅ Cho phép | ✅ Cho phép | ⚠️ *BE Class-level* | ❌ **Chặn (403)** | Cảnh báo ràng buộc khóa ngoại nếu đã phát sinh dữ liệu học vụ. |
| **Sinh mã học sinh** | Đề xuất mã `STU` tự động khả dụng | `POST /api/v1/students/code` | ✅ Cho phép | ✅ Cho phép | ✅ Cho phép | ❌ **Chặn (403)** | Sinh mã ngẫu nhiên dạng `STUxxxxxxx`, kiểm tra trùng lặp trước khi cấp. |
| **Xuất dữ liệu** | Tải danh sách học sinh file CSV | `GET /api/v1/students/export` | ✅ Cho phép | ✅ Cho phép | ✅ Cho phép | ❌ **Chặn (403)** | Spring Batch streaming CSV với UTF-8 encoding. |
| **Tab 2: Phân lớp** | Xem lịch sử phân lớp của học sinh | `GET /api/v2/students/{id}/enrollments` | ✅ Toàn trường | ✅ Toàn trường | ✅ Toàn trường | ❌ **Chặn (403)** | Trả về lớp hiện tại (`schoolClassCode`) và lịch sử chuyển lớp (`transfers`). |
| **Tab 2: Phân lớp** | Phân lớp đơn lẻ / hàng loạt | `POST /api/v2/enrollments` / `bulk` | ✅ Cho phép | ✅ Cho phép | ❌ **Chặn (403)** | ❌ **Chặn (403)** | Chỉ dành cho Admin và Giáo vụ quản trị học sinh đầu năm. |
| **Tab 2: Phân lớp** | Chuyển lớp học sinh trong năm | `POST /api/v2/enrollments/{id}/transfer` | ✅ Cho phép | ✅ Cho phép | ❌ **Chặn (403)** | ❌ **Chặn (403)** | Ghi nhận lịch sử `class_transfer_history` và cập nhật `current_class_id`. |
| **Tab 3: Chuyên cần** | Xem báo cáo điểm danh của học sinh | `GET /api/v2/attendance/students/{id}/history` | ✅ Toàn trường | ✅ Toàn trường | ✅ Toàn trường | ❌ **Chặn (403)** | Hiển thị thẻ tóm tắt (có mặt, vắng có phép, vắng không phép) và danh sách buổi học. |
| **Tab 3: Chuyên cần** | Học sinh tự xem điểm danh của mình | `GET /api/v2/attendance/students/me/history` | ❌ **Chặn (403)** | ❌ **Chặn (403)** | ❌ **Chặn (403)** | ✅ **Chỉ bản thân** | Lọc dữ liệu theo User ID trong JWT của học sinh. |
| **Tab 4: Bảng điểm** | Xem bảng điểm học kỳ của học sinh | `GET /api/v2/transcripts/students/{id}/semesters/{sId}` | ✅ Toàn trường | ✅ Toàn trường | 🔒 **Chỉ lớp phụ trách** | ❌ **Chặn (403)** | `TranscriptAccessGuard` xác thực giáo viên phải dạy hoặc làm GVCN lớp của học sinh. |
| **Tab 4: Bảng điểm** | Xem bảng điểm cả năm của học sinh | `GET /api/v2/transcripts/students/{id}/academic-years/{yId}` | ✅ Toàn trường | ✅ Toàn trường | 🔒 **Chỉ lớp phụ trách** | ❌ **Chặn (403)** | `TranscriptAccessGuard` xác thực giáo viên phụ trách. |
| **Tab 4: Bảng điểm** | Học sinh tự tra cứu bảng điểm cá nhân | `GET /api/v2/transcripts/students/me/...` | ❌ **Chặn (403)** | ❌ **Chặn (403)** | ❌ **Chặn (403)** | ✅ **Chỉ bản thân** | Lấy dữ liệu bảng điểm chính thức theo token của học sinh đang đăng nhập. |
| **Tab 4: Bảng điểm** | Yêu cầu tính lại bảng điểm | `POST /api/v2/students/{code}/transcripts/recalculate` | ✅ **Cho phép** | ✅ **Cho phép** | ❌ **Chặn (403)** | ❌ **Chặn (403)** | Chỉ Admin và Giáo vụ có thẩm quyền kích hoạt tác vụ tính toán lại điểm số. |

---

## 4. BỘ THIẾT KẾ KIỂM THỬ E2E ĐA TẦNG (4 TIERS ARCHITECTURE)

Bộ kiểm thử được thiết kế theo cấu trúc kim tự tháp 4 tầng đảm bảo bao phủ toàn diện từ chức năng độc lập, ranh giới lỗi, kết hợp chuỗi nghiệp vụ chéo đến các kịch bản người dùng thực tế.

```
                  ┌─────────────────────────────────────┐
                  │ Tier 4: Real-World Scenarios        │  (Persona End-to-End Walkthroughs)
                  ├─────────────────────────────────────┤
                  │ Tier 3: Pairwise & Cross-Feature    │  (Chained Multi-Module Workflows)
                  ├─────────────────────────────────────┤
                  │ Tier 2: Boundary & Corner Cases     │  (Validation, 409 Conflict, Edges)
                  ├─────────────────────────────────────┤
                  │ Tier 1: Feature Coverage (>=5/feat) │  (Core Functional Verification)
                  └─────────────────────────────────────┘
```

---

### 4.1. Tier 1: Feature Coverage Tests (>= 5 cases / feature)

#### Feature 1: Login & Route Redirection sang `/v2` & Shell Navigation (R1 & Follow-up)
- **TC-F1-01 (Đăng nhập thành công chuyển hướng mặc định)**: Người dùng đăng nhập thành công với thông tin hợp lệ -> Hệ thống lưu session token và chuyển hướng thẳng vào `/v2`.
- **TC-F1-02 (Đăng nhập với tham số query redirect an toàn)**: Truy cập `/login?redirect=/v2/academic-years` khi chưa đăng nhập -> Đăng nhập thành công -> Hệ thống chuyển hướng đúng vào `/v2/academic-years`.
- **TC-F1-03 (Chặn open-redirect không an toàn)**: Truy cập `/login?redirect=https://evil.com` -> Đăng nhập thành công -> `safeRedirect()` loại bỏ URL ngoài và fallback về `/v2`.
- **TC-F1-04 (GuestOnly navigation guard)**: Người dùng đã có session truy cập `/login` hoặc `/register` -> Guard chặn lại và chuyển hướng về `/v2`.
- **TC-F1-05 (Hiển thị và kích hoạt Sidebar Hồ sơ học sinh)**: Đăng nhập với vai trò `ADMIN`/`ACADEMIC_OFFICE`/`TEACHER` -> Sidebar v2 hiển thị menu "Hồ sơ học sinh" với icon `pi pi-user`; click vào điều hướng sang `/v2/students` và menu chuyển sang trạng thái active.
- **TC-F1-06 (Ẩn menu Hồ sơ học sinh với vai trò STUDENT)**: Đăng nhập tài khoản `STUDENT` -> Sidebar v2 ẩn hoàn toàn menu "Hồ sơ học sinh"; chỉ hiển thị menu "Bảng điểm".

#### Feature 2: Danh sách và Tra cứu Học sinh v2 Đa chiều (R2)
- **TC-F2-01 (Tải danh sách học sinh phân trang server-side)**: Truy cập `/v2/students` -> Gọi `GET /api/v1/students?page=0&size=10` -> Hiển thị bảng dữ liệu với các cột: Mã HS, Họ tên, Ngày sinh, Trạng thái (ACTIVE Tag).
- **TC-F2-02 (Tìm kiếm theo mã học sinh STU chuẩn)**: Nhập mã học sinh chính xác `STU1234567` vào ô tìm kiếm -> Nhấn Tìm kiếm -> Bảng chỉ hiển thị đúng bản ghi học sinh tương ứng.
- **TC-F2-03 (Bộ lọc theo họ tên không dấu / có dấu)**: Nhập tên "Khánh Duy" vào bộ lọc -> Bảng tải lại và hiển thị các học sinh có tên khớp tương đối.
- **TC-F2-04 (Sắp xếp theo các cột allow-list)**: Click sắp xếp cột Họ và tên (tăng dần/giảm dần) -> Gửi query `sortField=studentName&sortDirection=asc/desc` -> Thứ tự dữ liệu thay đổi chính xác.
- **TC-F2-05 (Drill-down từ danh sách vào chi tiết học sinh)**: Nhấp vào mã học sinh hoặc nút Xem chi tiết trên một hàng -> Điều hướng mượt mà sang `/v2/students/:studentId` trong Shell v2.

#### Feature 3: Thêm mới Học sinh kèm Cấp tài khoản Đăng nhập (R3)
- **TC-F3-01 (Tạo học sinh v3 tự sinh username và mật khẩu mặc định)**: Mở form thêm học sinh, chọn "Cấp tài khoản", để trống username/password -> Gửi `POST /api/v3/students` với `username=null, password=null` -> Backend tự sinh username theo tên + đuôi mã HS, mật khẩu `12345678` đã hash; tạo User role STUDENT, Student, StudentInfo trong 1 transaction -> Trả về 201 Created kèm thông tin account.
- **TC-F3-02 (Tạo học sinh v3 với username và mật khẩu chỉ định)**: Nhập thông tin học sinh kèm username `nguyenvana` và mật khẩu `Secret@123` -> Gửi `POST /api/v3/students` -> Tạo thành công User với username đã chỉ định.
- **TC-F3-03 (Bảo mật thông tin xác thực trên Frontend)**: Kiểm tra network payload và response của `POST /api/v3/students` -> Không có plaintext password hay passwordHash được trả về trong response payload hay lưu trong FE state.
- **TC-F3-04 (Tạo học sinh v1 tương thích không cấp tài khoản)**: Bỏ chọn checkbox "Cấp tài khoản" -> Gửi `POST /api/v1/students` -> Tạo thành công bản ghi Student với `userId = null`.
- **TC-F3-05 (Chặn quyền tạo tài khoản đối với TEACHER)**: Đăng nhập tài khoản giáo viên `TEACHER` truy cập form tạo học sinh -> Ẩn hoặc disable tùy chọn cấp tài khoản v3; nếu cố gọi API v3 bằng curl -> Backend trả về 403 Forbidden.

#### Feature 4: Chi tiết Học sinh Đa phân hệ 4 Tabs Workspace (R4)
- **TC-F4-01 (Tab 1 - Hiển thị hồ sơ cá nhân và thông tin tài khoản)**: Mở `/v2/students/1` -> Tab 1 hiển thị mã HS, họ tên, ngày sinh, giới tính, trạng thái ACTIVE, User ID liên kết, username; không hiển thị trường legacy `averageScore`.
- **TC-F4-02 (Tab 2 - Hiển thị lớp học hiện tại và lịch sử chuyển lớp)**: Chuyển sang Tab 2 -> Gọi `GET /api/v2/students/1/enrollments` -> Hiển thị tên lớp đang theo học, năm học, và bảng lịch sử các lần chuyển lớp (`effectiveAt`, `fromClass`, `toClass`, `reason`).
- **TC-F4-03 (Tab 3 - Hiển thị số liệu chuyên cần và chi tiết điểm danh)**: Chuyển sang Tab 3 -> Gọi `GET /api/v2/attendance/students/1/history` -> Hiển thị thẻ tóm tắt tỷ lệ có mặt, số buổi nghỉ có phép/không phép và bảng chi tiết điểm danh theo từng buổi học.
- **TC-F4-04 (Tab 4 - Xem bảng điểm học kỳ và cả năm)**: Chuyển sang Tab 4 -> Gọi API bảng điểm v2 theo `studentId` -> Hiển thị bảng điểm các môn học kỳ I, kỳ II và tổng kết cả năm thông qua `TranscriptTermTable` và `TranscriptAnnualTable`.
- **TC-F4-05 (Tab 4 - Yêu cầu tính lại bảng điểm với quyền Admin/Giáo vụ)**: Đăng nhập với quyền `ACADEMIC_OFFICE`, vào Tab 4 của học sinh -> Nút "Yêu cầu tính lại bảng điểm" hiển thị khả dụng -> Nhấn nút -> Gọi `POST /api/v2/students/{code}/transcripts/recalculate` -> Hiển thị huy hiệu `CALCULATING`, sau đó cập nhật sang `UP_TO_DATE`.

#### Feature 5: Chuẩn hóa Vòng đời Học sinh & Chính sách Xóa an toàn (R5)
- **TC-F5-01 (Xóa thành công học sinh mồ côi chưa có dữ liệu học vụ)**: Chọn học sinh vừa tạo chưa được xếp lớp, chưa có điểm danh/bảng điểm -> Nhấn Xóa -> Xác nhận -> Gửi `DELETE /api/v1/students/{id}` thành công 204 No Content -> Bản ghi biến mất khỏi danh sách.
- **TC-F5-02 (Cảnh báo ngăn chặn xóa học sinh đã có dữ liệu xếp lớp)**: Chọn học sinh đã được phân vào lớp 6A -> Nhấn Xóa -> Hệ thống kiểm tra hoặc nhận lỗi ràng buộc -> Hiển thị Modal cảnh báo không cho phép xóa cứng để bảo vệ tính toàn vẹn dữ liệu.
- **TC-F5-03 (Hướng dẫn chuyển đổi trạng thái hồ sơ thay vì xóa)**: Khi gặp cảnh báo không thể xóa -> Modal cung cấp hành động điều hướng nhanh hoặc gợi ý: "Chuyển trạng thái học sinh sang INACTIVE hoặc GRADUATED".
- **TC-F5-04 (Cập nhật trạng thái học sinh sang INACTIVE)**: Chỉnh sửa học sinh, chọn trạng thái `INACTIVE` -> Lưu -> Bản ghi hiển thị Tag xám "Ngừng hoạt động", không còn xuất hiện trong danh sách học sinh chưa xếp lớp (`/unassigned`).
- **TC-F5-05 (Cập nhật trạng thái học sinh sang GRADUATED)**: Chỉnh sửa học sinh khối 9 cuối cấp, chuyển sang `GRADUATED` -> Lưu -> Toàn bộ lịch sử điểm danh và bảng điểm được bảo lưu toàn vẹn ở trạng thái lưu trữ vĩnh viễn.

---

### 4.2. Tier 2: Boundary & Corner Cases (>= 5 cases / feature)

#### Biên & Lỗi cho Feature 1: Login & Navigation
- **TC-B1-01 (Lỗi đăng nhập sai thông tin xác thực)**: Nhập sai username hoặc mật khẩu -> Gọi API trả về 401 Unauthorized -> FE hiển thị popup thông báo lỗi "Tên đăng nhập hoặc mật khẩu không đúng", không chuyển hướng, xóa sạch session.
- **TC-B1-02 (Token hết hạn giữa phiên làm việc)**: Đang thao tác trên `/v2/students`, JWT token hết hạn -> Gọi API trả về 401 -> `apiClient.onUnauthorized` kích hoạt -> Xóa session và tự động đưa người dùng về `/login?redirect=/v2/students`.
- **TC-B1-03 (Thử truy cập URL v2 khi chưa đăng nhập)**: Mở tab ẩn danh truy cập thẳng `/v2/students/10` -> Router guard chặn ngay lập tức và đưa về `/login?redirect=%2Fv2%2Fstudents%2F10`.
- **TC-B1-04 (Học sinh cố tình truy cập URL nhân sự `/v2/students`)**: Tài khoản `STUDENT` gõ trực tiếp `/v2/students` trên thanh địa chỉ trình duyệt -> FE chặn chuyển hướng sang `/v2/transcripts`, hoặc Backend trả về 403 Forbidden và hiển thị EmptyState/Forbidden alert.
- **TC-B1-05 (Payload redirect chứa ký tự đặc biệt hoặc javascript:)**: Thử truy cập `/login?redirect=javascript:alert(1)` -> `safeRedirect()` phát hiện chuỗi không an toàn và trả về fallback an toàn `/v2`.

#### Biên & Lỗi cho Feature 2: Danh sách & Tra cứu Học sinh
- **TC-B2-01 (Danh sách học sinh rỗng - Zero State)**: Tìm kiếm với từ khóa không tồn tại `STU9999999` -> API trả về `content: []`, `totalElements: 0` -> Bảng hiển thị giao diện trạng thái rỗng `EmptyState` với thông điệp "Không tìm thấy học sinh phù hợp".
- **TC-B2-02 (Phân trang vượt quá giới hạn trang cuối)**: Gọi query `page=9999&size=10` -> Backend xử lý an toàn trả về trang rỗng không lỗi 500 -> Frontend đưa trang hiện tại về trang hợp lệ.
- **TC-B2-03 (Tham số phân trang âm hoặc không hợp lệ)**: Truyền `page=-1` hoặc `size=0` -> Backend trả về `400 Bad Request` ("Trang không được nhỏ hơn 0") -> Frontend bắt lỗi và hiển thị thông báo hợp lệ.
- **TC-B2-04 (Tìm kiếm học sinh với chuỗi tên tối đa 35 ký tự và chứa ký tự đặc biệt Unicode)**: Nhập họ tên dài đúng 35 ký tự: "Nguyễn Thị Hoàng Trúc Phương Thảo..." -> Tìm kiếm chính xác, không bị lỗi encode URI.
- **TC-B2-05 (Lọc theo ngày sinh định dạng không hợp lệ)**: Nhập chuỗi ngày sai format (vd: `31/02/2026` hoặc chuỗi chữ) -> Form validation phía FE ngăn chặn submit, yêu cầu đúng chuẩn `yyyy-MM-dd`.

#### Biên & Lỗi cho Feature 3: Thêm mới Học sinh & Cấp tài khoản V3
- **TC-B3-01 (Xung đột trùng mã học sinh - 409 Conflict)**: Nhập mã học sinh `STU0000001` đã tồn tại trong database -> Gọi `POST /api/v3/students` -> Backend trả về `409 Conflict` ("Mã sinh viên đã tồn tại") -> Frontend hiển thị lỗi ngay dưới trường Mã học sinh, không tạo User và không rollback dở dang.
- **TC-B3-02 (Xung đột trùng tên đăng nhập - 409 Conflict)**: Nhập username đã tồn tại của một tài khoản khác -> Gọi `POST /api/v3/students` -> Backend trả về `409 Conflict` ("Tên đăng nhập đã tồn tại") -> Transaction rollback hoàn toàn, không lưu bản ghi rác trong bảng `student`.
- **TC-B3-03 (Độ dài username vượt giới hạn 20 ký tự khi sinh tự động)**: Học sinh có tên rất dài: "Công Tằng Tôn Nữ Bích Chiêu Mai" + mã `STU1234567` -> Hàm sinh username tự động kích hoạt cơ chế fallback lấy chữ cái đầu viết tắt (`cttnbcm1234567`), đảm bảo độ dài <= 20 ký tự.
- **TC-B3-04 (Mật khẩu nhập tay không thỏa mãn độ phức tạp)**: Nhập mật khẩu chỉ có 5 ký tự (< 6 ký tự) hoặc 16 ký tự (> 15 ký tự) -> Validation Bean Backend chặn với `400 Bad Request` ("Mật khẩu phải từ 6 đến 15 ký tự").
- **TC-B3-05 (Định dạng mã học sinh không đúng chuẩn regex `STU[0-9]{7}`)**: Nhập mã `STU123` (thiếu số) hoặc `HS1234567` (sai tiền tố) -> Validation chặn với `400 Bad Request` ("Mã sinh viên phải có định dạng STU và 7 chữ số").

#### Biên & Lỗi cho Feature 4: Chi tiết Học sinh 4 Tabs Workspace
- **TC-B4-01 (Học sinh chưa xếp lớp mở Tab 2 Phân lớp)**: Xem chi tiết học sinh mới chưa được xếp lớp -> Tab 2 gọi `GET /api/v2/students/{id}/enrollments` trả về danh sách rỗng -> Hiển thị trạng thái "Học sinh chưa được xếp vào lớp nào trong năm học này" kèm nút dẫn sang màn hình Xếp lớp.
- **TC-B4-02 (Học sinh chưa có dữ liệu chuyên cần mở Tab 3)**: Mở Tab 3 của học sinh mới chuyển đến trường -> API trả về summary các giá trị đếm bằng 0 -> Thẻ thống kê hiển thị 0 buổi, không bị lỗi chia cho 0 (`NaN%`) khi tính tỷ lệ có mặt.
- **TC-B4-03 (Học sinh chưa có điểm mở Tab 4 Bảng điểm)**: Mở Tab 4 khi giáo viên chưa nhập điểm cột nào -> Bảng điểm hiển thị ô điểm dạng ký tự gạch ngang "—", điểm trung bình chưa khả dụng, không bị crash giao diện.
- **TC-B4-04 (Giáo viên xem Tab 4 của học sinh không thuộc lớp mình phụ trách)**: Giáo viên dạy lớp 6A mở chi tiết học sinh lớp 6B và xem Tab 4 -> Backend `TranscriptAccessGuard` trả về `403 Forbidden` -> Tab 4 hiển thị cảnh báo "Bạn không có thẩm quyền xem bảng điểm của học sinh này".
- **TC-B4-05 (Yêu cầu tính lại bảng điểm khi tác vụ đang chạy - Concurrent recalculate)**: Admin bấm nút "Yêu cầu tính lại", sau đó bấm liên tục lần 2 -> Hệ thống disable nút và hiển thị trạng thái `CALCULATING`, ngăn chặn tạo duplicate background task.

#### Biên & Lỗi cho Feature 5: Vòng đời & Xóa an toàn
- **TC-B5-01 (Xóa học sinh không tồn tại - 404 Not Found)**: Gọi lệnh xóa học sinh với ID ngẫu nhiên không tồn tại `studentId=999999` -> Backend trả về `404 Not Found` ("Không tìm thấy sinh viên") -> Frontend thông báo dữ liệu không còn tồn tại và tải lại danh sách.
- **TC-B5-02 (Vi phạm khóa ngoại khi cố xóa học sinh đã có điểm danh)**: Backend nhận yêu cầu xóa học sinh có liên kết bảng điểm danh -> Bắt ngoại lệ `DataIntegrityViolationException` và chuyển đổi thành lỗi nghiệp vụ rõ ràng, không làm lộ stack trace SQL ra response.
- **TC-B5-03 (Chuyển trạng thái học sinh từ INACTIVE trở lại ACTIVE)**: Học sinh tạm nghỉ học quay trở lại trường -> Chỉnh sửa trạng thái sang `ACTIVE` -> Học sinh xuất hiện trở lại trong danh sách chờ xếp lớp (`/api/v2/enrollments/unassigned`).
- **TC-B5-04 (Cố tình xếp lớp cho học sinh có trạng thái GRADUATED)**: Vào module Xếp lớp, cố tình truyền ID của học sinh đã tốt nghiệp -> Backend chặn với lỗi nghiệp vụ ("Chỉ học sinh ở trạng thái ACTIVE mới được phép xếp lớp").
- **TC-B5-05 (Thao tác xóa bởi vai trò không có quyền)**: Tài khoản giáo viên `TEACHER` cố gắng gửi request `DELETE /api/v1/students/{id}` qua API client -> Backend trả về `403 Forbidden`.

---

### 4.3. Tier 3: Cross-Feature & Pairwise Combinations

Kiểm thử chuỗi liên kết chéo giữa các phân hệ học vụ theo luồng nghiệp vụ liên tục (Chained End-to-End Business Flow):

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│   Tạo Học sinh  │ ────> │    Xếp lớp      │ ────> │   Điểm danh     │ ────> │ Sổ điểm & Bảng  │
│   (Student V3)  │       │  (Enrollment)   │       │  (Attendance)   │       │     điểm v2     │
└─────────────────┘       └─────────────────┘       └─────────────────┘       └─────────────────┘
```

#### Chain 1: Chuỗi tạo mới toàn diện -> Xếp lớp -> Điểm danh -> Bảng điểm
- **Bước 1 (Tạo học sinh kèm tài khoản)**: Giáo vụ tạo học sinh mới `STU2026001` - "Lê Hoàng Long" qua form v3 -> Tạo User và tài khoản tự động thành công.
- **Bước 2 (Xếp vào lớp học)**: Chuyển sang phân hệ Xếp lớp (`/v2/enrollments`) -> Tìm thấy học sinh `STU2026001` trong danh sách Chưa xếp lớp (`unassigned`) -> Thực hiện xếp học sinh vào lớp `6A1` năm học 2026-2027.
- **Bước 3 (Điểm danh buổi học)**: Giáo viên chủ nhiệm lớp 6A1 mở phân hệ Điểm danh (`/v2/attendance`) -> Học sinh `STU2026001` xuất hiện trong danh sách học sinh của buổi học -> Ghi nhận điểm danh có mặt.
- **Bước 4 (Vào điểm bộ môn)**: Giáo viên bộ môn Toán vào Sổ điểm (`/v2/scorebooks`) lớp 6A1 -> Nhập điểm miệng 9.0 cho học sinh `STU2026001`.
- **Bước 5 (Kiểm tra Tabbed Workspace)**: Mở chi tiết học sinh `/v2/students/{id}`:
  - Tab 1: Hiển thị đúng thông tin cá nhân và User account.
  - Tab 2: Hiển thị lớp hiện tại là `6A1`.
  - Tab 3: Hiển thị 1 buổi có mặt (tỷ lệ 100%).
  - Tab 4: Hiển thị điểm Toán 9.0 và trạng thái tính điểm sẵn sàng.

#### Chain 2: Chuỗi chuyển lớp -> Kiểm tra tính liên tục của lịch sử học vụ
- **Bước 1**: Học sinh đang học lớp `6A1` phát sinh yêu cầu chuyển sang lớp `6A2`.
- **Bước 2**: Giáo vụ vào `/v2/enrollments`, thực hiện chuyển lớp (Transfer) cho học sinh từ `6A1` sang `6A2` với lý do "Chuyển theo nguyện vọng gia đình", ngày hiệu lực hôm nay.
- **Bước 3**: Mở Chi tiết học sinh Tab 2 (Phân lớp):
  - Lớp hiện tại đã cập nhật thành `6A2`.
  - Bảng lịch sử chuyển lớp ghi nhận 1 bản ghi: Từ `6A1` -> Đến `6A2`, đúng ngày hiệu lực và lý do.
- **Bước 4**: Kiểm tra Tab 3 (Điểm danh): Toàn bộ số buổi điểm danh trước đó tại lớp `6A1` vẫn được lưu giữ nguyên vẹn trong báo cáo chuyên cần của học sinh.

#### Chain 3: Chuỗi nhập điểm danh ngoại lệ -> Phản ánh lên báo cáo chuyên cần Tab 3
- **Bước 1**: Buổi sáng học sinh nghỉ học có đơn xin phép gửi giáo viên chủ nhiệm.
- **Bước 2**: Giáo viên vào module Điểm danh, ghi nhận ngoại lệ vắng có phép cho học sinh bằng mã `by-code/{studentCode}`.
- **Bước 3**: Mở Chi tiết học sinh Tab 3 -> Thẻ tóm tắt ngay lập tức tăng `excusedAbsenceCount` lên 1, `unexcusedAbsenceCount` là 0.
- **Bước 4**: Mở Tab 4 (Bảng điểm) -> Thông tin số buổi nghỉ có phép trên bảng điểm học kỳ đồng bộ khớp đúng với số liệu ở Tab 3.

#### Chain 4: Chuỗi sửa điểm trong sổ điểm -> Yêu cầu tính lại bảng điểm Tab 4
- **Bước 1**: Giáo viên gửi yêu cầu sửa điểm bài kiểm tra 1 tiết của học sinh từ 7.0 lên 8.5.
- **Bước 2**: Ban giám hiệu phê duyệt yêu cầu sửa điểm trong `/v2/score-change-requests`.
- **Bước 3**: Trạng thái bảng điểm học sinh chuyển sang `OUTDATED` (cần tính lại).
- **Bước 4**: Giáo vụ mở Chi tiết học sinh Tab 4 -> Thẻ trạng thái hiển thị cảnh báo cần tính lại -> Nhấn nút "Yêu cầu tính lại bảng điểm" -> Hệ thống gọi API recalculate -> Trạng thái chuyển sang `FINISH` / `UP_TO_DATE` -> Điểm trung bình học kỳ được cập nhật lại chính xác.

#### Chain 5: Chuỗi học sinh đăng nhập tự tra cứu bảng điểm cá nhân
- **Bước 1**: Học sinh lấy tài khoản được cấp từ bước tạo V3 (`username` tự sinh, mật khẩu mặc định `12345678`).
- **Bước 2**: Đăng nhập tại màn hình `/login` -> Hệ thống xác thực thành công và chuyển hướng vào `/v2`.
- **Bước 3**: Trên sidebar v2, học sinh chỉ nhìn thấy menu "Bảng điểm" (`/v2/transcripts`), không thấy menu "Hồ sơ học sinh", "Xếp lớp", "Sổ điểm".
- **Bước 4**: Học sinh xem bảng điểm của chính mình -> Frontend tự động gọi `/api/v2/transcripts/students/me/...` -> Dữ liệu trả về chuẩn xác, không bị lỗi 403 Forbidden.

---

### 4.4. Tier 4: Real-World Application Scenarios (Persona Walkthroughs)

#### Kịch bản 1: Giáo vụ tuyển sinh đầu năm học mới (Academic Office Intake Persona)
- **Bối cảnh**: Đầu năm học 2026-2027, cô Lan (Giáo vụ) cần tiếp nhận 50 học sinh khối 6 mới vào trường.
- **Hành trình**:
  1. Cô Lan đăng nhập vào hệ thống -> Trực tiếp chuyển hướng vào `/v2`.
  2. Truy cập menu "Hồ sơ học sinh" (`/v2/students`) -> Nhấn "Thêm học sinh".
  3. Bật tùy chọn "Cấp tài khoản đăng nhập cho học sinh". Nhấn nút "Tạo mã học sinh" để lấy mã tự động `STU2026001`.
  4. Nhập họ tên, ngày sinh, địa chỉ. Để trống username và password để hệ thống tự cấp.
  5. Nhấn Lưu -> Hệ thống thông báo thành công và hiển thị tên đăng nhập `lannguyen2026001` kèm mật khẩu khởi tạo. Cô Lan in giấy báo tài khoản cho phụ huynh.
  6. Chuyển sang menu "Xếp lớp" (`/v2/enrollments`) -> Chọn Năm học 2026-2027, Khối 6, Lớp 6A1.
  7. Chọn học sinh `STU2026001` từ danh sách chưa xếp lớp và thực hiện xếp vào lớp 6A1.

#### Kịch bản 2: Giáo viên chủ nhiệm quản lý học sinh và theo dõi chuyên cần (Homeroom Teacher Persona)
- **Bối cảnh**: Thầy Hùng là giáo viên chủ nhiệm lớp 7B, cần theo dõi tình hình học sinh cá biệt hay nghỉ học không phép.
- **Hành trình**:
  1. Thầy Hùng đăng nhập -> Vào Shell v2 -> Mở menu "Hồ sơ học sinh".
  2. Tìm kiếm học sinh "Trần Văn Nam" -> Click vào xem Chi tiết học sinh.
  3. Thầy Hùng xem Tab 1 (Hồ sơ cá nhân): Thấy thông tin phụ huynh và địa chỉ liên lạc. Nút chỉnh sửa bị ẩn vì giáo viên không có quyền sửa hồ sơ gốc.
  4. Chuyển sang Tab 2 (Phân lớp): Thấy học sinh được phân lớp 7B từ ngày 05/09/2026, chưa từng chuyển lớp.
  5. Chuyển sang Tab 3 (Chuyên cần): Nhận thấy học sinh đã nghỉ 4 buổi không phép trong tháng 10. Thầy Hùng xuất báo cáo để làm việc với phụ huynh.
  6. Chuyển sang Tab 4 (Bảng điểm): Thầy Hùng được phân quyền chủ nhiệm nên xem trọn vẹn bảng điểm học kỳ của Nam. Nút "Yêu cầu tính lại điểm" bị ẩn/disabled đối với vai trò Giáo viên.

#### Kịch bản 3: Xử lý kỷ luật học sinh buộc thôi học và chính sách xóa an toàn (Safe Offboarding Persona)
- **Bối cảnh**: Học sinh khối 8 chuyển theo gia đình ra nước ngoài định cư giữa năm học.
- **Hành trình**:
  1. Ban giám hiệu yêu cầu rút hồ sơ học sinh nhưng vẫn phải giữ toàn bộ kết quả học tập kỳ 1.
  2. Giáo vụ vào `/v2/students`, tìm học sinh và thử nhấn nút "Xóa học sinh".
  3. Hệ thống hiển thị hộp thoại cảnh báo: *"Học sinh đã có dữ liệu xếp lớp và kết quả học tập học kỳ I. Thao tác xóa cứng bị từ chối để đảm bảo tính toàn vẹn hồ sơ học vụ."*
  4. Giáo vụ chọn hành động "Chuyển trạng thái hồ sơ".
  5. Trong màn hình cập nhật, chuyển trạng thái từ `ACTIVE` sang `INACTIVE` với ghi chú "Chuyển trường ra nước ngoài".
  6. Dữ liệu học sinh vẫn tồn tại trong lịch sử lớp và bảng điểm kỳ I nhưng không còn hiển thị trong danh sách điểm danh hàng ngày của lớp.

#### Kịch bản 4: Học sinh đăng nhập tra cứu kết quả thi và chuyên cần (Student Self-Service Persona)
- **Bối cảnh**: Em Minh (học sinh lớp 8A) sử dụng máy tính cá nhân ở nhà để kiểm tra kết quả điểm thi học kỳ.
- **Hành trình**:
  1. Minh truy cập trang chủ của trường -> Nhập username và mật khẩu được cấp -> Đăng nhập.
  2. Hệ thống chuyển hướng Minh vào `/v2`.
  3. Giao diện của Minh được cá nhân hóa: Không hiển thị các chức năng quản lý, chỉ có menu "Bảng điểm".
  4. Minh xem điểm chi tiết các môn học kỳ I của mình. Minh thử thay đổi đường dẫn URL trên trình duyệt thành `/v2/students` để tò mò xem hồ sơ bạn bè -> Hệ thống tự động chặn lại và đưa Minh trở về an toàn màn hình bảng điểm cá nhân.

---

## 5. PHƯƠNG THỨC VẬN HÀNH KIỂM THỬ VÀ CÔNG CỤ ĐẢM BẢO CHẤT LƯỢNG

Để đảm bảo chất lượng phần mềm khi triển khai thực tế, dự án vận hành hệ thống kiểm thử tự động đa tầng với các lệnh tiêu chuẩn sau:

### 5.1. Kiểm thử Frontend (FE Test Suite)
- **Test Runner**: Vitest (`v3.2.7`) kết hợp `@vue/test-utils` và môi trường DOM giả lập `jsdom`.
- **Lệnh thực thi**:
  ```bash
  # Chạy toàn bộ test suites frontend ở chế độ run một lần
  npm --prefix FE run test -- --run

  # Đo lường độ bao phủ kiểm thử (Coverage)
  npm --prefix FE run test:coverage

  # Kiểm tra lỗi cú pháp và quy chuẩn TypeScript
  npm --prefix FE run lint
  npm --prefix FE run build

  # Kiểm tra Storybook độc lập không cần backend live
  npm --prefix FE run build-storybook
  ```
- **Hiện trạng kiểm thử FE**: 75 test files với 324 tests đều đang PASS tuyệt đối. Khi tích hợp module học sinh v2, cần bổ sung các test suite cho:
  - `LoginView.spec.ts`: Xác minh redirect `/v2`.
  - `AuthenticatedV2ShellView.spec.ts`: Xác minh hiển thị menu "Hồ sơ học sinh" cho Admin/Teacher và ẩn với Student.
  - `StudentListView.spec.ts`: Test danh sách, tìm kiếm, phân trang và Tag trạng thái v2.
  - `StudentFormView.spec.ts`: Test gọi `createStudentWithAccount` (v3) và bắt lỗi 409 Conflict.
  - `StudentDetailView.spec.ts`: Test nạp 4 tabs dữ liệu độc lập.

### 5.2. Kiểm thử Backend (BE Test Suite)
- **Test Runner**: JUnit 5, Spring Boot Test, MockMvc, H2 in-memory database mode MySQL.
- **Lệnh thực thi**:
  ```bash
  # Di chuyển vào thư mục backend
  cd BE/BaiTap-RS

  # Chạy toàn bộ Unit & Integration tests
  ./gradlew test

  # Kiểm tra quy chuẩn chất lượng mã nguồn (Checkstyle & PMD)
  ./gradlew checkstyleMain checkstyleTest
  ./gradlew pmdMain pmdTest

  # Xuất báo cáo độ bao phủ JaCoCo
  ./gradlew jacocoTestReport
  ```
- **Hiện trạng kiểm thử BE**:
  - `StudentAuthorizationIntegrationTest.java`: Kiểm tra phân quyền truy cập endpoint `/api/v1/students`.
  - `StudentServiceAccountTest.java`: Đã kiểm thử đầy đủ logic tạo User kèm role STUDENT, mã hóa mật khẩu và tự sinh username trong `StudentAccountService`.
  - `StudentValidationControllerIntegrationTest.java`: Kiểm thử validation độ dài, định dạng mã `STU`.

### 5.3. Kiểm thử Tích hợp Tự động End-to-End (E2E Integration Test)
- **Postman / Newman Test Collection**:
  Bộ collection `document/postman/Java-CoBan.postman_collection.json` chứa các kịch bản kiểm thử API tuần tự theo chuỗi phân lớp và điểm danh. Có thể chạy tự động trong CI/CD pipeline qua lệnh:
  ```bash
  newman run document/postman/Java-CoBan.postman_collection.json \
    -e document/postman/Local-Environment.json \
    --reporters cli,junit
  ```

---

## 6. KẾ HOẠCH HÀNH ĐỘNG VÀ KHUYẾN NGHỊ CHO CÁC NHÓM TRIỂN KHAI

### 6.1. Đối với nhóm Frontend (FE Implementers)
1. **Login Redirect**: Cập nhật ngay `safeRedirect()` trong `LoginView.vue` và router guard trong `router/index.ts` để đưa người dùng về `/v2`.
2. **Shell v2 Integration**:
   - Thêm route con `/v2/students`, `/v2/students/new`, `/v2/students/:id`, `/v2/students/:id/edit`.
   - Cấu hình Sidebar v2 trong `AuthenticatedV2ShellView.vue` hiển thị menu "Hồ sơ học sinh" (`pi pi-user`) có điều kiện `isNonStudent`.
3. **Màn hình Danh sách v2**: Tích hợp hiển thị Tag trạng thái (ACTIVE/INACTIVE/GRADUATED) và nút drill-down.
4. **Form Thêm mới v3**:
   - Bổ sung toggle "Cấp tài khoản đăng nhập" gọi `POST /api/v3/students`.
   - Xử lý bắt lỗi 409 Conflict khi trùng studentCode hoặc username.
5. **Màn hình Chi tiết 4 Tabs**:
   - Tạo mới `StudentDetailView.vue` dạng Tabbed Workspace.
   - Tab 1: Hồ sơ nhân khẩu & Account info.
   - Tab 2: Phân lớp & Transfer history (`GET /api/v2/students/{id}/enrollments`).
   - Tab 3: Chuyên cần summary & detail (`GET /api/v2/attendance/students/{id}/history`).
   - Tab 4: Tái sử dụng `TranscriptTermTable`, `TranscriptAnnualTable` từ `TranscriptViewerView.vue`.
6. **Xóa an toàn**: Thay thế popup xóa cứng bằng cảnh báo ràng buộc dữ liệu học vụ.

### 6.2. Đối với nhóm Backend (BE Implementers)
1. **Siết chặt phân quyền `@PreAuthorize`**:
   - Thêm `@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")` trên các phương thức `POST`, `PUT`, `DELETE` của `StudentController.java` để ngăn Teacher can thiệp hồ sơ gốc theo đúng `BR-AUTH-005`.
2. **Bổ sung trường `status` và `gender` vào DTO**:
   - Mở rộng `ResStudentDTO` để trả về `status` (enum `StudentStatus`) và `gender` giúp Frontend hiển thị đầy đủ tiêu chí chấp nhận.
3. **Chính sách xóa an toàn**:
   - Bổ sung bước kiểm tra ràng buộc trước khi xóa trong `StudentService.deleteStudent(id)`: nếu tồn tại bản ghi trong `StudentYearEnrollmentRepository`, ném lỗi `AppException(HttpStatus.CONFLICT, "Học sinh đã có lịch sử xếp lớp, không thể xóa")`.

---
*Báo cáo được tổng hợp và lập bởi E2E Integration Explorer — 2026-09-04.*
