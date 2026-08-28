# Developer Plan 054: Student Enrollment & Class Placement UI

## Trạng thái phê duyệt

- Status: `APPROVED - 2026-08-28`.
- Application version: `v2`.
- Parent context: backend Enrollment & Class Placement đã hoàn thành trong
  Plan 026; FE v2 shell và academic catalog đã hoàn thành qua Plan 051-053.1.
- Phạm vi: xây dựng màn hình FE `Student Enrollment UI` để giáo vụ xem học
  sinh chưa xếp lớp, xem roster theo lớp, xếp đơn/hàng loạt, chuyển lớp và xem
  lịch sử phân lớp bằng các API enrollment hiện có.
- Không triển khai production code trước khi plan này được user phê duyệt rõ
  ràng qua agent.

## 1. Mục tiêu

Màn hình v2 hiện đã có context năm học, khối và lớp nhưng chưa có workspace cho
nghiệp vụ xếp lớp. Người dùng cần một flow tập trung để:

1. Chọn năm học và lớp làm context.
2. Xem danh sách học sinh đang chưa được xếp lớp trong năm học đó.
3. Xếp một học sinh hoặc nhiều học sinh vào lớp đang chọn.
4. Xem danh sách học sinh hiện tại của lớp.
5. Chuyển một học sinh sang lớp khác trong cùng năm học.
6. Mở lịch sử enrollment/transfer của học sinh.
7. Nhìn thấy rõ warning sĩ số nhưng không nhầm warning là lỗi chặn thao tác.

Tiêu đề và mô tả trang phải hiển thị đầy đủ như screenshot tham chiếu:
`Student Enrollment UI` và `Xếp học sinh vào lớp theo năm học, xem học sinh chưa
xếp lớp`; không để text bị cắt ở viewport thấp hoặc mobile.

## 2. Tài liệu và contract áp dụng

- `document/application-doc/v2/ApplicationContext.md` — kiến trúc và thứ tự ưu
  tiên tài liệu v2.
- `document/application-doc/v2/RequirementBaseline.md` — index requirement.
- `document/application-doc/v2/modules/02-EnrollmentAndTeachingModule.md` —
  `FR-ENROLL-001..005`, `BR-ENROLL-001..008` và phạm vi enrollment.
- `document/application-doc/v2/data-model/03-StudentsAndEnrollment.md` —
  enrollment hiện tại, transfer history và quy tắc không xóa lịch sử.
- `document/application-doc/v2/frontend-api/00-common-contract.md` — envelope,
  auth, lỗi HTTP, typed service và warning semantics.
- `document/application-doc/v2/frontend-api/03-teacher-assignment-enrollment.md`
  — API enrollment đang triển khai.
- `document/application-doc/v2/frontend-api/07-enums-and-known-drift.md` —
  wire enum `EnrollmentStatus = ACTIVE | COMPLETED | WITHDRAWN`.
- `FE/agent-rules/00-foundation.md` và `FE/agent-rules/02-domain-rules.md` —
  boundary Vue/PrimeVue/service/type và quy tắc identifier.
- `document/dev-impl-plan/fe/academic/053-grade-class-subject-management-ui-2026-08-27.md`
  — pattern context năm học/lớp hiện có.

## 3. Bằng chứng hiện tại

### Backend contract đã có

`EnrollmentController` hiện cung cấp:

| Method | Path | Authorization | Mục đích |
|---|---|---|---|
| `GET` | `/api/v2/enrollments/unassigned?academicYearId={id}` | `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER` | Học sinh chưa xếp lớp |
| `GET` | `/api/v2/classes/{classId}/students` | `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER` | Roster của lớp |
| `POST` | `/api/v2/enrollments` | `ADMIN`, `ACADEMIC_OFFICE` | Xếp đơn |
| `POST` | `/api/v2/enrollments/bulk` | `ADMIN`, `ACADEMIC_OFFICE` | Xếp hàng loạt |
| `POST` | `/api/v2/enrollments/{enrollmentId}/transfer` | `ADMIN`, `ACADEMIC_OFFICE` | Chuyển lớp |
| `GET` | `/api/v2/students/{studentId}/enrollments` | `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER` | Lịch sử theo technical id |
| `GET` | `/api/v2/students/by-code/{studentCode}/enrollments` | `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER` | Lịch sử theo mã học sinh |

Response hiện tại cần map đúng, không suy diễn thêm field:

- `ResUnassignedStudentDTO`: `studentId`, `studentCode`, `studentName`.
- `ResClassStudentDTO`: `studentId`, `studentCode`, `studentName`,
  `enrollmentId`.
- `ResEnrollmentMutationDTO`: `enrollments`, `warnings`.
- `ResCapacityWarningDTO`: `classId`, `academicYearId`, `gradeLevelId`,
  `activeStudentCount`, `gradeAverage`, `message`.
- History gồm enrollment và danh sách transfer với `transferId`, lớp nguồn,
  lớp đích, `effectiveAt`, `reason`, `approvedBy`.

### Frontend hiện tại

- `AuthenticatedV2ShellView.vue` đã có navigation cho các catalog nhưng chưa có
  enrollment route.
- `SchoolClassListView.vue` và `ClassSubjectListView.vue` đã có pattern tải
  academic year/context, xử lý `401`/`403`, `PageState` và reload sau mutation.
- `academicApi.ts` đang giữ API academic; domain enrollment nên có typed service
  riêng `enrollmentApi.ts` theo common contract.
- Auth role chưa được expose cho FE. Navigation không được tự suy luận role hoặc
  giả vờ ẩn security boundary; backend vẫn quyết định quyền.

## 4. Phạm vi chức năng

### 4.1. Context và điều hướng

- Thêm route authenticated:
  `/v2/enrollments`, name `v2-enrollments`.
- Thêm navigation item `Xếp lớp` trong v2 shell.
- Tải danh sách academic year; chọn năm `ACTIVE` trước, fallback bản ghi đầu
  tiên như pattern catalog hiện có.
- Khi đổi năm học, tải lại danh sách lớp và danh sách học sinh chưa xếp lớp;
  chọn lớp đầu tiên còn phù hợp nếu có.
- Chỉ tải class roster khi đã có `classId`; không gọi API với ID null/không hợp
  lệ.
- Context hiển thị năm học, lớp và trạng thái lớp; lớp `CLOSED` là read-only
  đối với mutation.

### 4.2. Danh sách chưa xếp lớp

- Hiển thị `studentCode`, `studentName` và lựa chọn học sinh.
- Có tìm kiếm cục bộ theo `studentCode`/`studentName` cho danh sách đã tải; không
  gọi endpoint search tưởng tượng.
- Có empty state riêng khi năm học chưa có học sinh chưa xếp lớp.
- Cho phép chọn một hoặc nhiều học sinh để mở flow xếp lớp.
- Ưu tiên `studentCode` cho hiển thị và command human-facing; giữ `studentId`
  cho payload khi row đã có technical id.

### 4.3. Xếp đơn và xếp hàng loạt

- Nút xếp đơn từ từng row và bulk action từ selection.
- Dùng dialog/form dùng chung, nhận class context hiện tại.
- Single request dùng `studentId` và `academicYearId`, `classId`; không gửi
  `studentCode` nếu đã có `studentId` rõ ràng.
- Bulk request dùng `studentIds` từ selection; không trộn `studentIds` và
  `studentCodes`, không tạo positional pairing semantics.
- Sau mutation thành công, reload cả unassigned list và class roster.
- Hiển thị `warnings` từ response như non-blocking status; không rollback UI
  hoặc báo failure nếu mutation đã thành công.
- Nếu backend trả `400`, `404`, `409`, hiển thị message đã normalize bởi
  `apiClient`; `403` giữ session và hiển thị không đủ quyền.

### 4.4. Roster và chuyển lớp

- Bảng roster hiển thị mã, tên học sinh và `enrollmentId`/trạng thái cần thiết
  từ contract hiện có.
- Nút chuyển lớp mở dialog với danh sách lớp khác trong cùng academic year.
- Không cho chọn lớp hiện tại, lớp `CLOSED` hoặc lớp khác năm học.
- Form transfer gồm `targetClassId`, `effectiveAt`, `reason`; date-time không
  được ở tương lai và reason tối đa 500 ký tự theo backend DTO.
- Transfer chỉ dành cho mutation flow; `TEACHER` có thể xem nhưng không được
  tự gọi mutation. UI không tự suy role; backend xử lý `403` là nguồn cuối.
- Sau transfer thành công, reload lớp nguồn/đích và unassigned nếu cần; hiển
  thị warning sĩ số cho các lớp backend trả về.
- Không xóa enrollment cũ, không tự sửa điểm danh/điểm và không hiển thị transfer
  như hard delete.

### 4.5. Lịch sử học sinh

- Từ roster hoặc unassigned row mở history dialog/drawer cho học sinh.
- Dùng endpoint theo `studentId`; chỉ dùng endpoint by-code khi flow không có
  technical id.
- Hiển thị từng enrollment theo năm học, lớp, status, ngày vào/hoàn thành và
  danh sách transfer theo thứ tự API trả về.
- Hiển thị rõ `ACTIVE`, `COMPLETED`, `WITHDRAWN` theo wire enum; không thêm
  `TRANSFERRED` vào TypeScript union vì transfer là history event trong contract
  hiện tại.
- History là read-only và giữ được dù học sinh đã chuyển lớp.

### 4.6. Layout và responsive

- Tạo page heading đúng visual language của v2 catalog: eyebrow, H1, mô tả và
  action area.
- Không dùng negative margin cho title/caption; bảo đảm tiêu đề và mô tả trong
  screenshot không bị clip ở viewport thấp.
- Dùng các content surface hiện có cho context, unassigned và roster; không lồng
  card trong card.
- Với màn hình hẹp, bảng có overflow cục bộ; dialog có content scroll cục bộ
  và footer vẫn thao tác được.
- Nút có icon PrimeIcons hiện có; action icon-only phải có tooltip/accessible
  label, action chính có text rõ ràng.

## 5. Phương án kỹ thuật

### 5.1. Typed service và types

Tạo `FE/src/services/enrollmentApi.ts` và `FE/src/types/enrollment.ts`.

Service dự kiến:

```ts
fetchUnassignedStudents(token, academicYearId)
fetchClassStudents(token, classId)
createEnrollment(token, request)
createBulkEnrollment(token, request)
transferEnrollment(token, enrollmentId, request)
fetchStudentEnrollmentHistory(token, studentId)
fetchStudentEnrollmentHistoryByCode(token, studentCode)
```

TypeScript model phải tách:

- API response DTO (`UnassignedStudent`, `ClassStudent`, `Enrollment`,
  `TransferHistory`, `CapacityWarning`, `EnrollmentMutation`).
- Editable state (`CreateEnrollmentFormValues`, `BulkEnrollmentFormValues`,
  `TransferEnrollmentFormValues`).

Date-time request giữ format mà backend đang nhận cho `LocalDateTime`; không dùng
`toISOString()` một cách mù quáng nếu làm lệch múi giờ `Asia/Ho_Chi_Minh`.

### 5.2. Component boundary

Ưu tiên các component nhỏ, có typed props/emits:

- `EnrollmentContextPanel.vue`: năm học/lớp và trạng thái context.
- `UnassignedStudentTable.vue`: selection, filter cục bộ và actions.
- `ClassStudentTable.vue`: roster và actions transfer/history.
- `EnrollmentMutationDialog.vue`: single/bulk create, state và validation.
- `TransferEnrollmentDialog.vue`: target class, effective time, reason.
- `StudentEnrollmentHistoryDialog.vue`: history read-only.
- `CapacityWarningBanner.vue`: tái sử dụng component hiện có, bổ sung dữ liệu
  thật qua props nếu cần; không thay đổi semantics warning thành error.

Nếu test boundary cho thấy màn hình nhỏ và các bảng không tái sử dụng, có thể
gộp component; không tạo abstraction chung chỉ để giảm vài dòng template.

### 5.3. View orchestration

Tạo `FE/src/views/EnrollmentListView.vue` để:

- giữ state context/list/dialog/loading;
- gọi services và điều phối reload sau mutation;
- xử lý session/`401`, `403`, `404`, `409`, network error;
- không đặt raw `fetch` hoặc business mapping phức tạp trong component table;
- reset selection và dialog state khi đổi academic year/class.

Không thêm Pinia/Vuex hoặc store toàn cục cho flow này.

## 6. File/khu vực dự kiến thay đổi

| File | Thay đổi dự kiến |
|---|---|
| `FE/src/types/enrollment.ts` | Tạo wire DTO, enum, request và form types |
| `FE/src/services/enrollmentApi.ts` | Tạo typed wrapper cho bảy endpoint hiện có |
| `FE/src/services/enrollmentApi.spec.ts` | Kiểm tra URL, query, method, body và bearer token |
| `FE/src/router/index.ts` | Đăng ký route `/v2/enrollments` |
| `FE/src/views/AuthenticatedV2ShellView.vue` | Thêm navigation item enrollment |
| `FE/src/views/EnrollmentListView.vue` | Tạo view orchestration cho context và các flow |
| `FE/src/components/EnrollmentContextPanel.vue` | Tạo context selector/status nếu boundary cần |
| `FE/src/components/UnassignedStudentTable.vue` | Tạo bảng học sinh chưa xếp lớp |
| `FE/src/components/ClassStudentTable.vue` | Tạo roster và action history/transfer |
| `FE/src/components/EnrollmentMutationDialog.vue` | Tạo single/bulk enrollment dialog |
| `FE/src/components/TransferEnrollmentDialog.vue` | Tạo transfer dialog |
| `FE/src/components/StudentEnrollmentHistoryDialog.vue` | Tạo history read-only dialog |
| `FE/src/components/*.spec.ts` | Component tests cho state, emit, validation và action |
| `FE/src/components/*.stories.ts` | Storybook deterministic states, không cần live backend |
| `FE/src/styles.css` | Chỉ thêm CSS cục bộ/responsive cần thiết; không đổi global PrimeVue tùy tiện |
| `document/application-doc/v2/frontend-api/03-teacher-assignment-enrollment.md` | Chỉ cập nhật nếu source contract/type/authorization thực tế phát hiện drift |
| `document/dev-impl-plan/fe/FE_DEV_PLAN_SUMMARY.md` | Thêm Plan 054 sau khi tạo plan |
| `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md` | Thêm Plan 054 sau khi tạo plan |
| `document/dev-note/fe/enrollment/054-...md` | Tạo sau implementation, không thuộc phase lập plan |

Không sửa backend, migration, Postman collection hoặc v1 Student UI trong plan
này. Nếu API thực tế không khớp guide và khiến flow không thể đúng, dừng phần
bị ảnh hưởng và ghi contract gap thay vì tự sửa backend trong cùng plan.

## 7. Test và validation dự kiến

### Service tests

- Unassigned list serialize `academicYearId` đúng.
- Class roster serialize `classId` đúng.
- Single create gửi đúng body với `studentId`.
- Bulk create gửi `studentIds` riêng, không ghép positional với `studentCodes`.
- Transfer gửi đúng `enrollmentId`, `targetClassId`, `effectiveAt`, `reason`.
- History dùng đúng student-id route; by-code route chỉ được kiểm tra như method
  riêng nếu view có sử dụng.
- DELETE không được xuất hiện vì enrollment không có hard-delete endpoint.

### Component/view tests

- Chọn năm học tải lại class, unassigned và reset state phụ thuộc.
- Chọn lớp tải roster; không gọi roster khi chưa có lớp.
- Single/bulk selection validation và emitted request đúng.
- Mutation success reload đúng danh sách, reset selection và hiển thị success.
- Capacity warning được render là non-blocking.
- Transfer chặn lớp hiện tại, lớp đóng, reason quá dài và effective time tương
  lai ở client-side; backend vẫn là validator cuối.
- History mở đúng student và render enrollment/transfer read-only.
- `401` redirect theo foundation; `403` giữ session; `404`/`409` hiển thị lỗi
  có ngữ cảnh.
- Empty/loading/error/forbidden state của từng list không chồng lấn nội dung.

### Storybook và quality gates

Storybook cần các state deterministic tối thiểu:

- Context loaded.
- Unassigned loaded, empty và loading.
- Roster loaded và empty.
- Single/bulk mutation dialog.
- Transfer dialog validation/conflict.
- History loaded.
- Capacity warning và access denied.

Sau implementation chạy đúng scripts hiện có:

```bash
cd FE
npm run lint
npm run test
npm run test:coverage
npm run build
npm run build-storybook
```

Cần kiểm tra thủ công hoặc browser review ở desktop/mobile cho title, bảng có
overflow, dialog thấp và các action không bị che. Validation result phải ghi rõ
`PASS`, `FAIL` hoặc `NOT RUN` trong Dev Note.

## 8. Rủi ro và giới hạn

- **Auth roles chưa có trong FE:** không thể phân quyền bằng navigation; mutation
  phải phản hồi đúng theo `403` từ backend.
- **Danh sách lớp không có student count trong contract hiện tại:** không tự
  tính hoặc gọi N+1 để tạo sĩ số; chỉ hiển thị roster thực tế và warning mutation
  trả về.
- **Bulk request có hai mảng identifier:** chỉ dùng `studentIds` từ row selection;
  không suy diễn mapping giữa ids và codes.
- **LocalDateTime và timezone:** hiển thị/serialize theo contract hiện tại và
  không đổi date-time sang UTC nếu chưa có quyết định riêng.
- **Capacity warning có thể không đi kèm GET:** banner chỉ hiển thị khi mutation
  thực sự trả warning; không tạo warning giả ở FE.
- **Contract drift:** nếu response/envelope/source khác tài liệu, cập nhật plan
  hoặc tạo amendment được duyệt trước khi đổi phạm vi.
- **Nâng cao ngoài scope:** import phân lớp, kết chuyển cuối năm, thôi học,
  hoàn thành năm học, phân trang server-side cho endpoint hiện không hỗ trợ và
  cập nhật điểm danh/điểm không triển khai trong Plan 054.

## 9. Approval gates

Plan 054 cần user phê duyệt trước khi code. Approval xác nhận:

1. Route chính thức là `/v2/enrollments` và navigation label là `Xếp lớp`.
2. Plan sử dụng các endpoint enrollment hiện có, không mở backend contract mới.
3. Bulk flow dùng `studentIds` từ selection; không ghép `studentIds` với
   `studentCodes`.
4. Capacity warning là non-blocking và chỉ render từ response backend.
5. Transfer history và enrollment history là read-only ở FE.
6. Import, kết chuyển cuối năm, lifecycle `WITHDRAWN/COMPLETED` mutation và
   role-aware hiding nằm ngoài scope.

## 10. Output dự kiến

Sau khi plan được phê duyệt và triển khai:

- Có route `/v2/enrollments` hiển thị đúng `Student Enrollment UI`.
- Giáo vụ có thể chọn năm học/lớp, xem học sinh chưa xếp lớp và roster.
- Giáo vụ có thể xếp đơn/hàng loạt và chuyển lớp qua typed service.
- Người dùng có thể xem lịch sử enrollment/transfer mà không làm mất dữ liệu.
- Warning sĩ số, loading, empty, forbidden, conflict và success được phân biệt.
- Layout desktop/mobile không cắt title, bảng hoặc footer dialog.
- Storybook, unit test và quality gates phản ánh các state chính.
- Dev Note 054 được tạo sau implementation với file thực tế, validation và
  limitation; plan không được đánh dấu completed chỉ vì đã viết tài liệu.
