# Dev Note 075 — Thời khóa biểu & định mức tiết dạy (BE + FE)

## Liên kết và approval

- BE Developer Plan: `document/dev-impl-plan/be/timetable/075-timetable-teacher-load-2026-09-11.md`
- FE Developer Plan: `document/dev-impl-plan/fe/timetable/075-timetable-teacher-load-ui-2026-09-11.md`
- Wireframe: `document/wireframes/fe/timetable/075-timetable-teacher-load/README.md`
- Phê duyệt: Yêu cầu thực thi Plan 75 theo chuẩn v3 từ người dùng.

Đây là Dev Note hợp nhất ghi nhận toàn bộ kết quả triển khai backend và frontend cho tính năng Thời khóa biểu & Định mức tiết dạy theo Plan 075.

---

## Phạm vi thực tế đã hoàn thành

### 1. Backend (`BE/BaiTap-RS`)

- **Database Migration**:
  - Tạo `V22__create_timetable_and_teacher_load.sql` gồm 13 bảng: `functional_room`, `subject_functional_room`, `teacher_load_policy`, `teacher_load_eligibility`, `teacher_unavailability`, `timetable_head`, `timetable_revision`, `timetable_period`, `timetable_calendar`, `timetable_closed_date`, `timetable_entry`, `timetable_publish_intent`, `timetable_audit`.
- **Module Phòng chức năng (`functionalroom`)**:
  - Entity `FunctionalRoom`: mã phòng, tên phòng, sức chứa, trạng thái (`ACTIVE`, `INACTIVE`), mô tả, ghi chú.
  - Repository `FunctionalRoomRepository`: CRUD và truy vấn theo mã, trạng thái.
  - DTOs: `ReqCreateFunctionalRoomDTO`, `ReqUpdateFunctionalRoomDTO`, `ResFunctionalRoomDTO`.
  - Service `FunctionalRoomService`: logic CRUD, validate unique code, cập nhật trạng thái.
  - Controller `FunctionalRoomController`: API REST `/api/v3/functional-rooms` với phân quyền `ADMIN` và `ACADEMIC_OFFICE`.
- **Module Gán môn - Phòng chức năng (`academic`)**:
  - Entity `SubjectFunctionalRoom`: quan hệ N-N giữa môn học và phòng chức năng bắt buộc/cho phép.
  - Repository `SubjectFunctionalRoomRepository`.
  - DTOs: `ReqUpdateSubjectFunctionalRoomsDTO`, `ResSubjectFunctionalRoomsDTO`.
  - Service `SubjectFunctionalRoomService`: quản lý danh sách phòng cho môn học.
  - Controller `SubjectFunctionalRoomController`: API REST `/api/v3/subjects/{subjectId}/functional-rooms`.
- **Module Lịch bận giáo viên (`timetable`)**:
  - Entity `TeacherUnavailability`: đăng ký lịch bận định kỳ (theo thứ trong tuần) hoặc ngày cụ thể, buổi/tiết, trạng thái (`PENDING`, `APPROVED`, `REJECTED`, `WITHDRAWN`).
  - DTOs: `ReqCreateUnavailabilityDTO`, `ReqReviewUnavailabilityDTO`, `ResUnavailabilityDTO`.
  - Service `TeacherUnavailabilityService`: giáo viên tạo/rút lịch bận; Admin/Giáo vụ phê duyệt/từ chối.
  - Controller `TeacherUnavailabilityController`: API REST `/api/v3/teacher-unavailabilities`.
- **Module Thời khóa biểu & Định mức tiết dạy (`timetable`)**:
  - Entities: `TimetableHead`, `TimetableRevision`, `TimetablePeriod`, `TimetableEntry`, `TimetableCalendar`, `TimetableClosedDate`, `TimetablePublishIntent`, `TimetableAudit`, `TeacherLoadPolicy`, `TeacherLoadEligibility`.
  - Cấu trúc tiết học: 2 buổi/ngày × 4 tiết (Sáng: 1–4, Chiều: 5–8), 6 ngày/tuần (Thứ 2 - Thứ 7).
  - DTOs: `ReqCreateTimetableHeadDTO`, `ReqCreateTimetableEntryDTO`, `ReqUpdateTimetableEntryDTO`, `ReqCreateRevisionDTO`, `ReqPublishTimetableDTO`, `ResTimetableHeadDTO`, `ResTimetableRevisionDTO`, `ResTimetableEntryDTO`, `ResTimetablePeriodDTO`, `ResTimetableValidationDTO`, `ResTeacherLoadDTO`, `ResTeacherLoadDetailDTO`.
  - Conflict Engine: Kiểm tra xung đột đa chiều:
    - Xung đột trùng giáo viên trong cùng tiết học.
    - Xung đột trùng phòng học / phòng chức năng trong cùng tiết học.
    - Xung đột trùng lớp học trong cùng tiết học.
    - Xung đột lịch bận đã được phê duyệt của giáo viên (`BLOCKING` vs `WARNING`).
    - Bắt buộc phòng chức năng (`ROOM_REQUIRED`) cho mọi tiết học khi môn học yêu cầu.
  - Định mức tiết dạy (`TeacherLoadService`):
    - Tính toán tổng số tiết phân công trong tuần của giáo viên.
    - So sánh với định mức chuẩn (19 tiết, giảm trừ theo kiêm nhiệm/chức vụ: chủ nhiệm -4, nuôi con nhỏ dưới 12 tháng -3...).
    - Phân loại trạng thái định mức: `TARGET_MET`, `LOAD_BELOW_TARGET`, `LOAD_ABOVE_TARGET`.
  - Quản lý phiên bản (Revision Lifecycle):
    - Các trạng thái: `DRAFT`, `PUBLISHED`, `SUPERSEDED`, `ARCHIVED`.
    - Quy trình Publish: xác thực chặn publish khi còn xung đột `BLOCKING`.
    - Ghi nhận nhật ký kiểm toán `TimetableAudit`.
  - Controllers:
    - `TimetableController`: API `/api/v3/timetables/**` (head, revision, entry, publish, validation).
    - `TeacherLoadController`: API `/api/v3/teacher-loads/**`.

### 2. Frontend (`FE`)

- **Types**:
  - `FE/src/types/functionalRoom.ts`: kiểu dữ liệu phòng chức năng.
  - `FE/src/types/teacherUnavailability.ts`: kiểu dữ liệu lịch bận giáo viên.
  - `FE/src/types/timetable.ts`: kiểu dữ liệu TKB, tiết học, entry, xung đột, định mức tải giáo viên.
- **API Services & Unit Tests**:
  - `FE/src/services/functionalRoomApi.ts` & `functionalRoomApi.spec.ts`
  - `FE/src/services/subjectFunctionalRoomApi.ts` & `subjectFunctionalRoomApi.spec.ts`
  - `FE/src/services/teacherUnavailabilityApi.ts` & `teacherUnavailabilityApi.spec.ts`
  - `FE/src/services/timetableApi.ts` & `timetableApi.spec.ts`
  - `FE/src/services/teacherLoadApi.ts` & `teacherLoadApi.spec.ts`
- **Components**:
  - `FunctionalRoomDialog.vue`: Dialog thêm/sửa thông tin phòng chức năng.
  - `SubjectFunctionalRoomPanel.vue`: Panel gán danh sách phòng chức năng cho môn học.
  - `TeacherUnavailabilityDialog.vue`: Dialog đăng ký lịch bận (lặp theo thứ hoặc ngày cụ thể).
  - `TimetableWeekGrid.vue`: Lưới thời khóa biểu 2 buổi × 4 tiết (Sáng 1-4, Chiều 5-8), hỗ trợ xem theo Lớp, Giáo viên, Phòng.
  - `TimetableEntryDialog.vue`: Dialog xếp tiết học với chọn môn, giáo viên, phòng và hiển thị cảnh báo tức thời.
  - `TimetableConflictPanel.vue`: Panel tổng hợp xung đột và cảnh báo cần xử lý.
  - `TeacherLoadPanel.vue`: Panel theo dõi định mức tiết dạy và trạng thái đủ/thừa/thiếu tiết.
  - `TimetablePublishDialog.vue`: Dialog xác nhận phát hành TKB với kiểm tra blocking issue.
- **Views**:
  - `FunctionalRoomListView.vue`: Quản lý danh mục phòng chức năng.
  - `TeacherUnavailabilityView.vue`: Quản lý lịch bận giáo viên (duyệt/từ chối cho Giáo vụ/Admin; đăng ký/rút cho Giáo viên).
  - `TimetableListView.vue`: Danh sách TKB theo học kỳ.
  - `TimetableWorkspaceView.vue`: Không gian làm việc xếp TKB (lưới tuần, kiểm tra xung đột, định mức giáo viên, tạo revision, xuất bản).
  - `MyTimetableView.vue`: Màn hình xem TKB cá nhân dành riêng cho Giáo viên.
  - `TimetableSettingsView.vue`: Màn hình cấu hình môn - phòng chức năng.
- **Routing & Shell Navigation**:
  - Đăng ký các route mới trong `FE/src/router/index.ts`:
    - `/v2/functional-rooms` (`ADMIN`, `ACADEMIC_OFFICE`)
    - `/v2/timetables` (`ADMIN`, `ACADEMIC_OFFICE`)
    - `/v2/timetables/settings` (`ADMIN`, `ACADEMIC_OFFICE`)
    - `/v2/timetables/unavailability` (`ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`)
    - `/v2/timetables/my-timetable` (`TEACHER`)
    - `/v2/timetables/:timetableId` (`ADMIN`, `ACADEMIC_OFFICE`)
  - Cập nhật menu điều hướng `AuthenticatedV2ShellView.vue` với icon và tab phù hợp cho từng vai trò.

---

## Files Changed / Created

### Backend
- `BE/BaiTap-RS/src/main/resources/db/migration/V22__create_timetable_and_teacher_load.sql`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/functionalroom/**` (Entity, Repository, Service, Controller, DTOs)
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/domain/entity/SubjectFunctionalRoom.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/repository/SubjectFunctionalRoomRepository.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/service/SubjectFunctionalRoomService.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/controller/SubjectFunctionalRoomController.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/domain/DTOs/**`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/timetable/**` (Entities, Repositories, Services, Controllers, DTOs, Enums)
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/functionalroom/service/FunctionalRoomServiceTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/academic/service/SubjectFunctionalRoomServiceTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/timetable/service/TeacherUnavailabilityServiceTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/timetable/service/TimetableServiceTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/timetable/controller/TimetableControllerTest.java`

### Frontend
- `FE/src/types/functionalRoom.ts`
- `FE/src/types/teacherUnavailability.ts`
- `FE/src/types/timetable.ts`
- `FE/src/services/functionalRoomApi.ts` & `.spec.ts`
- `FE/src/services/subjectFunctionalRoomApi.ts` & `.spec.ts`
- `FE/src/services/teacherUnavailabilityApi.ts` & `.spec.ts`
- `FE/src/services/timetableApi.ts` & `.spec.ts`
- `FE/src/services/teacherLoadApi.ts` & `.spec.ts`
- `FE/src/components/functional-room/FunctionalRoomDialog.vue`
- `FE/src/components/academic/SubjectFunctionalRoomPanel.vue`
- `FE/src/components/timetable/TeacherUnavailabilityDialog.vue`
- `FE/src/components/timetable/TimetableWeekGrid.vue`
- `FE/src/components/timetable/TimetableEntryDialog.vue`
- `FE/src/components/timetable/TimetableConflictPanel.vue`
- `FE/src/components/timetable/TeacherLoadPanel.vue`
- `FE/src/components/timetable/TimetablePublishDialog.vue`
- `FE/src/views/functional-room/FunctionalRoomListView.vue`
- `FE/src/views/timetable/TeacherUnavailabilityView.vue`
- `FE/src/views/timetable/TimetableListView.vue`
- `FE/src/views/timetable/TimetableWorkspaceView.vue`
- `FE/src/views/timetable/MyTimetableView.vue`
- `FE/src/views/timetable/TimetableSettingsView.vue`
- `FE/src/router/index.ts`
- `FE/src/views/shell/AuthenticatedV2ShellView.vue`

---

## Validation Result

### PASS
- **Backend Tests (Timetable, Functional Room, Subject Functional Room)**: `./gradlew test --tests "com.JavaTraining.BaiTap_RS.timetable.*" --tests "com.JavaTraining.BaiTap_RS.functionalroom.*" --tests "com.JavaTraining.BaiTap_RS.academic.service.SubjectFunctionalRoomServiceTest"`: **PASS** (100% 39 tests passed).
- **Backend PMD**: `./gradlew compileJava pmdMain`: **PASS** (0 violations trên production code).
- **Backend Build tasks**: `compileJava`, `classes`, `processResources`: **PASS**.
- **Frontend Lint**: `npm run lint`: **PASS** (0 errors, 0 warnings).
- **Frontend Tests**: `npm run test`: **PASS** (99/99 test files passed, 526/526 tests passed).
- **Frontend Production Build**: `npm run build`: **PASS** (`vue-tsc --noEmit && vite build` thành công trong 9.46s).
- **Git Diff Check**: `git diff --check`: **PASS** (không có lỗi newline/trailing whitespace).

### NOT RUN / UNVERIFIED
- **Live backend runtime & Browser E2E**: Chưa kiểm tra tương tác trình duyệt trực tiếp với backend server thực tế.
- **MySQL Database Migration Live Run**: Chưa thực thi Flyway migration trên instance MySQL vật lý bên ngoài (đã kiểm thử JPA mapping và schema với H2 trong unit tests).
- **Toàn bộ 440+ test tích hợp lịch sử trong một lần chạy**: Chạy toàn bộ test tích hợp gặp `OutOfMemoryError` do giới hạn bộ nhớ JVM của worker Gradle mặc định khi chạy 440+ context Spring; bộ test đơn vị và chất lượng code của Plan 75 đã được xác thực độc lập 100% PASS.

---

## Next Steps

1. Khởi động backend và frontend trên môi trường dev/staging có cơ sở dữ liệu MySQL để chạy migration Flyway V22.
2. Thực hiện kiểm thử tích hợp luồng nghiệp vụ trên trình duyệt:
   - Thêm phòng học chức năng và gán môn học vào phòng chức năng.
   - Giáo viên gửi yêu cầu báo bận, Giáo vụ/Admin phê duyệt.
   - Tạo thời khóa biểu, xếp tiết học trên lưới, quan sát phát hiện xung đột và tính toán tải giáo viên.
   - Xuất bản bản sửa đổi TKB (publish revision) và kiểm tra TKB cá nhân của giáo viên.

