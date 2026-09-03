# Developer Plan 062: Retake Result UI

## 1. Trạng thái và thông tin chung

- **Status**: `Completed`.
- **Approval**: Người dùng đã phê duyệt Plan 062.
- **Dev Note**: [`062-retake-result-ui-2026-09-03.md`](../../../dev-note/fe/scorebook/062-retake-result-ui-2026-09-03.md).
- **Application-document version**: `v2`.
- **Ngày lập plan**: `2026-09-03`.
- **Module**: Frontend `scorebook` / Retake Result.
- **Wireframe bắt buộc review trước implementation**: [`062-retake-result-ui`](../../../../wireframes/fe/scorebook/062-retake-result-ui/README.md).
- **Backend dependency**: Plan 045 đã ghi nhận `Completed` và cung cấp retake foundation, calculation integration và authorization.

## 2. Mục tiêu

Xây dựng màn hình để giáo vụ tra cứu và quản lý kết quả thi lại theo học sinh, năm học, môn học và trạng thái; thể hiện rõ điểm trước thi lại, điểm thi lại, kết quả chính thức sau calculation và trạng thái xử lý. FE chỉ lưu nguồn dữ liệu qua API, không tự tính hoặc ghi đè kết quả transcript.

## 3. Contract hiện tại được phép dùng

Base path: `/api/v2/retake-exams`.

| Hành vi | Method/path | FE behavior |
|---|---|---|
| List/filter | `GET /api/v2/retake-exams` | Filter `studentId`, `academicYearId`, `subjectId`, `status`, `page`, `size`; retake default `size=10`. |
| Detail | `GET /api/v2/retake-exams/{retakeId}` | Mở dialog/detail từ một row. |
| Create planned/scored | `POST /api/v2/retake-exams` | Gửi `studentId`, `academicYearId`, `subjectId`, optional `examDate`, optional `retakeScore`, `note`. Chỉ hiển thị khi capability được contract xác nhận. |
| Enter/update score | `PUT /api/v2/retake-exams/{retakeId}/score` | Gửi `retakeScore`, optional `examDate`, `note`; điểm `0.0..10.0`, tối đa 1 chữ số thập phân. |
| Cancel | `POST /api/v2/retake-exams/{retakeId}/cancel` | Xác nhận trước khi gửi; không hard-delete. Chỉ enable khi policy/backend cho phép. |

Response hiện tại `ResRetakeExamDTO` gồm `retakeId`, `studentId`, `academicYearId`, `subjectId`, `preRetakeScore`, `retakeScore`, `examDate`, `status`, `note`, actor IDs và timestamps. Wire enum là `PLANNED | SCORED | CANCELLED`.

## 4. Phạm vi FE

### In-scope

- Route-level view và typed service boundary theo convention FE hiện có.
- List/filter theo student, academic year, subject và status; server-side pagination.
- Empty state khi không có retake record, loading state và refresh sau mutation.
- Create dialog cho record `PLANNED`; cho phép tạo `SCORED` chỉ khi backend contract và capability cho phép.
- Score dialog cho `PLANNED` hoặc `SCORED` theo response/policy; nhập/sửa `retakeScore`, `examDate`, `note`.
- Cancel confirmation và hiển thị `CANCELLED`; không cho sửa score sau khi cancelled nếu backend từ chối lifecycle này.
- Hiển thị before/after: `preRetakeScore`, `retakeScore`, `officialDtbmhCn` và `calculationSource` chỉ khi transcript API cung cấp dữ liệu tương ứng.
- Hiển thị tác động calculation: `IN_PROGRESS`/`FINISH`, task ID hoặc thời điểm tính gần nhất từ Transcript API; sau save phải refresh/read status, không tự tính.
- Status badge rõ ràng cho `PLANNED`, `SCORED`, `CANCELLED` và cảnh báo khi kết quả đang `IN_PROGRESS`.
- Chuẩn hóa lỗi `400`, `401`, `403`, `404`, `409`; giữ message backend phù hợp với UI.
- Storybook deterministic cho normal/list, create dialog, score dialog, before-after, empty, validation, forbidden, not-found/conflict và calculation-in-progress.

### Validation UX

- Required: `studentId`, `academicYearId`, `subjectId` khi create.
- Score update required; range `0.0..10.0`, tối đa một chữ số thập phân; `0.0` là hợp lệ.
- Date-only gửi/nhận theo `yyyy-MM-dd`; không dùng `toISOString()` để tạo ngày.
- Note tối đa 1000 ký tự theo backend DTO.
- Không cho submit khi record đã `CANCELLED`; vẫn hiển thị lịch sử/read-only.

## 5. Luồng chính

1. User có quyền mở Retake Result UI; FE tải list với page 0, size 10.
2. User lọc theo context/status; FE cập nhật query state và gọi list API.
3. User chọn `Tạo kỳ thi lại`; dialog validate client-side rồi POST. Sau thành công đóng dialog, refresh list và hiển thị status mới.
4. User chọn record `PLANNED`/`SCORED`; dialog hiển thị before score và form score. Sau PUT, FE refresh retake detail/list và đọc transcript/status liên quan.
5. Khi status calculation là `IN_PROGRESS`, FE hiển thị processing notice và chỉ đọc kết quả cũ theo đúng contract; khi `FINISH`, refresh official result.
6. User chọn hủy; dialog nêu rõ không xóa audit history. Sau POST, refresh row và disable các mutation không còn hợp lệ.

## 6. Quyền và security

- Backend Plan 045/Dev Note ghi `@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")` cho list/detail/create/update/cancel; `STUDENT` và `TEACHER` nhận `403`.
- FE không dùng route guard hoặc username để thay thế authorization; backend là authoritative.
- Hiện cần đồng bộ nguồn role/capability: FE `AGENTS.override.md` và `FrontendApiGuide.md` vẫn ghi JWT/account chưa expose role, trong khi Plan 058/summary mới ghi JWT có role list. Plan implementation phải dừng role-aware enablement cho tới khi contract source được cập nhật và approved.

## 7. Tác động calculation và transcript

- FE không tính `officialDtbmhCn`, `finalDtbcn`, `regularDtbcn` hay `resultSource`.
- `SCORED` có thể làm transcript chuyển `IN_PROGRESS` và tạo calculation task; cancel một record đã scored có thể khôi phục official result qua worker.
- Chỉ transcript API mới là nguồn hiển thị kết quả official before/after. Retake API hiện không trả `officialDtbmhCn`, `calculationSource`, `calculationStatus` hoặc `lastCalculationTaskId`.
- Nếu chưa có API/context liên kết student + academic year để tải annual transcript, phần after/calculation phải hiển thị `TBD`/blocked, không dựng field từ retake DTO.

## 8. Dự kiến cấu trúc FE

- `FE/src/views/RetakeResultView.vue` — orchestration, query state, loading/error và refresh.
- `FE/src/components/retake/RetakeResultTable.vue` — list/filter result và action affordances.
- `FE/src/components/retake/RetakeResultDialog.vue` — create/update form, before/after và validation.
- `FE/src/services/retakeApi.ts` — typed HTTP boundary; có thể tách transcript read service nếu chưa có.
- `FE/src/types/retake.ts` — wire DTO/status và UI state riêng.
- Tên/route cuối cùng phải bám cấu trúc FE hiện tại sau khi plan được approved.

## 9. Test, Storybook và validation sau approval

- Unit/component tests: filter serialization, page reset, score/date/note validation, `0.0`, status action visibility, mutation refresh và error mapping.
- Storybook: deterministic fixtures, không gọi live backend; phải có list, create dialog, score dialog, before/after, empty, validation, `401`, `403`, `404`, `409`, `IN_PROGRESS` và `FINISH`.
- Chạy đúng scripts có trong `FE/package.json`: lint, test, coverage nếu có, build và build-storybook; báo `NOT RUN/BLOCKED` nếu chưa chạy hoặc bị chặn.
- Kiểm tra `git diff --check` và review wireframe trước production implementation.
- Browser/live-backend walkthrough là release evidence riêng, không được tuyên bố PASS nếu chưa chạy.

## 10. Out-of-scope / TBD

- Không sửa backend, schema, migration, calculation worker, audit log hoặc scorebook regular score.
- Không tự thêm endpoint/field để tìm tên học sinh, tên môn, capability, official after-score hoặc audit history.
- Không cho `TEACHER`/`STUDENT` mutation chỉ vì UI có nút; policy phải đến từ backend contract.
- Không xây audit-log screen trong Plan 062; chỉ hiển thị metadata audit nếu response đã có.
- Điều kiện học sinh phải thi lại, eligibility/lên lớp và policy teacher-specific: `TBD`/ngoài scope.
- Quy tắc cancel ở từng lifecycle và việc create `SCORED` trực tiếp: dùng backend behavior đã approved; nếu chưa có acceptance contract thì block thao tác.

## 11. Contract blockers cần giải quyết trước khi approval/implementation

1. **Role source drift**: Plan 058 nói JWT có role list nhưng FE routing/API docs vẫn nói role chưa expose. Cần chốt một nguồn contract và cập nhật tài liệu trước role-aware UI.
2. **Display identity gap**: list/detail DTO chỉ có numeric IDs, không có student code/name, subject name hoặc academic year label. Cần approved lookup/join contract hoặc chấp nhận UI hiển thị ID.
3. **After-score linkage gap**: Retake API không trả official after score/calculation fields; cần annual transcript endpoint/context rõ ràng cho từng row.
4. **Capability granularity**: backend hiện dùng role-level `ADMIN`/`ACADEMIC_OFFICE`, nhưng FE cần biết create/update/cancel action nào được phép; không suy diễn capability từ role nếu chưa có contract.
5. **Cancel/create lifecycle acceptance**: cần xác nhận UI action matrix cho `PLANNED`, `SCORED`, `CANCELLED`, đặc biệt create trực tiếp `SCORED` và cancel record `SCORED`.
6. **409 refresh contract**: cần xác nhận response/message khi duplicate tuple hoặc stale business state để FE biết refresh list/detail và giữ dữ liệu người dùng.

## 12. Approval gate

Plan 062 chỉ là `Draft — chờ user approval`. Wireframe phải được review/duyệt cùng plan. Chưa được tạo production Vue, service, route, Storybook code hoặc cập nhật summary dùng chung trước khi có approval bằng lời nhắn.
