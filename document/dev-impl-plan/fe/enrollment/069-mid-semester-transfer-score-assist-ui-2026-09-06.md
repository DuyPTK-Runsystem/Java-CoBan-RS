# Developer Plan 069: Hỗ trợ chuyển điểm khi chuyển lớp giữa học kỳ

## Trạng thái và approval gate

- Status: `APPROVED - implementation in progress (user message 2026-09-07)`.
- Scope owner: Enrollment + Scorebook (FE/BE contract amendment).
- Wireframe bắt buộc: [069 wireframe README](../../../wireframes/fe/enrollment/069-mid-semester-transfer-score-assist-ui/README.md), [static preview](../../../wireframes/fe/enrollment/069-mid-semester-transfer-score-assist-ui/index.html).
- Không được triển khai production code trước khi người dùng phê duyệt plan và wireframe bằng tin nhắn qua agent.

## 1. Mục tiêu

Khi giáo vụ/admin chuyển một học sinh giữa hai lớp trong cùng năm học và học sinh đã có điểm trong học kỳ hiện tại, hệ thống mở một popup hỗ trợ. Popup đặt điểm đã có ở lớp nguồn cạnh các cột điểm của lớp đích để người dùng nhập điểm tương ứng có căn cứ, trước khi hoàn tất transfer.

Kết quả mong muốn:

1. Không làm mất, sửa hoặc “di chuyển” điểm lịch sử của lớp nguồn.
2. Người dùng nhìn được rõ môn/cột/giá trị/trạng thái điểm cũ và cột nhập tương ứng ở lớp mới.
3. Điểm lớp mới chỉ được ghi khi người dùng xác nhận; ô trống, `0.0`, `ABSENT`, `EXEMPTED`, `CANCELLED` phải phân biệt đúng contract.
4. Transfer và việc ghi điểm mới không tạo trạng thái nửa chừng không thể giải thích.

## 2. Requirement và constraint liên quan

- Enrollment v2: `POST /api/v2/enrollments/{enrollmentId}/transfer`; enrollment/history được giữ lại, transfer không phải hard delete.
- Scorebook v2: score entry gắn với `assessmentColumnId` và `studentId`; missing map entry là “Chưa nhập”, không có enum `NOT_ENTERED`; điểm `0.0` hợp lệ.
- FE không tự tính `Đtbmh`, `Đtbhk` hoặc kết quả chính thức.
- Backend authorization/scope là nguồn quyết định cuối; FE không suy diễn quyền từ tên vai trò.
- Cần giữ timezone/format `LocalDateTime` hiện có của enrollment transfer.

## 3. Phạm vi

### In-scope

- Trigger popup từ `TransferEnrollmentDialog` khi đã chọn lớp đích và backend xác định học sinh có điểm trong học kỳ hiện tại.
- Hiển thị thông tin học sinh, năm học/học kỳ, lớp nguồn/lớp đích.
- Hiển thị bảng theo môn: cột điểm nguồn read-only, giá trị/trạng thái bằng chứng, cột lớp đích editable, trạng thái mapping và ghi chú.
- Cho phép người dùng nhập từng cột điểm lớp đích; có thao tác gợi ý/copy từng ô từ bằng chứng cũ nhưng phải xác nhận lại.
- Các state `loading`, `empty/no score`, `validation`, `403`, `409`, lỗi mạng và reload an toàn.
- Storybook deterministic states, component/service tests và cập nhật tài liệu API/Dev Note sau implementation.

### Out-of-scope

- Không tự động chuyển toàn bộ điểm cũ sang lớp mới khi chưa có xác nhận.
- Không xóa, sửa hoặc đổi chủ sở hữu score entry của lớp nguồn.
- Không tính lại điểm trung bình chính thức ở FE.
- Không mở rộng bulk transfer nhiều học sinh trong plan này.
- Không thay đổi quy tắc tính điểm, assessment column, retake hoặc transcript ngoài dữ liệu cần refresh.

## 4. Bằng chứng hiện tại và blocker contract

Đã có:

- `TransferEnrollmentDialog.vue` đang nhận `student`, `currentClassId`, `targetClasses` và emit `TransferEnrollmentFormValues`.
- Enrollment API có transfer mutation và history.
- Scorebook API có grid theo `scorebookId`, columns/scores theo lớp-môn và single/bulk score mutation.
- Transcript API đọc assessment columns theo học kỳ nhưng không cung cấp mapping lớp nguồn → lớp đích cho flow transfer.

Chưa có:

- Endpoint/DTO để lấy một snapshot gồm học sinh, học kỳ, lớp nguồn, lớp đích, source columns/scores, target columns và mapping đề xuất.
- Contract quy định transfer có được commit cùng score entries mới trong một transaction hay cần hai bước/rollback.
- Quy tắc mapping khi môn/cột giữa hai lớp khác nhau.

Đây là **contract gate bắt buộc**, không được FE tự ghép nhiều API rồi giả định semantics. Backend plan/amendment phải chốt ít nhất:

```text
GET  /api/v2/enrollments/{enrollmentId}/transfer-score-assist?targetClassId=...&semesterId=...
POST /api/v2/enrollments/{enrollmentId}/transfer-with-scores
```

Tên endpoint trên là đề xuất để review, không phải contract đã được phê duyệt. Response cần có identity, source/target class, semester, subjects, source evidence, target columns, mapping key, score status/value/version và warnings. Mutation cần nhận `targetClassId`, `effectiveAt`, `reason` và danh sách target score entries với `expectedVersion` nếu áp dụng; backend phải bảo đảm authorization, scorebook lifecycle, duplicate/conflict và transaction semantics.

## 5. Phương án triển khai đề xuất

### Luồng người dùng

1. Người dùng bấm `Chuyển lớp` trong roster.
2. Chọn lớp đích, ngày hiệu lực và lý do trong dialog hiện có.
3. Chọn `Tiếp tục xem hỗ trợ điểm`.
4. FE gọi assist contract. Nếu không có điểm học kỳ hiện tại, hiển thị xác nhận transfer thông thường.
5. Nếu có điểm, mở `TransferScoreAssistDialog` với evidence source read-only và target inputs.
6. Nút `Gợi ý từ điểm cũ` chỉ điền giá trị vào draft target theo mapping backend; người dùng vẫn có thể sửa hoặc để trống.
7. Nút `Xác nhận chuyển lớp và lưu điểm mới` gọi mutation contract. Sau thành công reload roster nguồn/đích và transcript/scorebook liên quan nếu đang hiển thị.

### Component/service boundary

- `TransferEnrollmentDialog.vue`: giữ dữ liệu transfer và chuyển sang bước assist; không chứa mapping business.
- `TransferScoreAssistDialog.vue`: trình bày evidence, editable draft, validation và emit typed submit.
- `TransferScoreEvidenceTable.vue` (chỉ tạo nếu test chứng minh cần tái sử dụng): render hàng môn/cột nguồn/đích.
- `enrollmentApi.ts` hoặc service cross-module được approved: gọi contract transfer-assist/mutation; không raw fetch trong component.
- `types/enrollment.ts` và/hoặc `types/scorebook.ts`: tách DTO read-only, draft form và mutation request.
- View enrollment: orchestration, loading/error/refresh sau mutation; không tính điểm.

### Trade-off

- Snapshot endpoint giảm nguy cơ FE ghép sai scorebook/semester và giúp audit dễ hơn, nhưng cần BE contract mới.
- Mutation nguyên tử bảo vệ integrity tốt hơn hai request rời, nhưng cần xử lý conflict rõ ràng và có thể trả `409` khi scorebook thay đổi.
- Gợi ý mapping giúp thao tác nhanh nhưng không được auto-submit; mapping không tương đồng phải hiển thị `Chưa ghép`.

## 6. Phạm vi mã nguồn dự kiến

### Backend contract trước FE implementation

- `BE/.../enrollment/controller/EnrollmentController.java`: endpoint đã được chốt.
- `BE/.../enrollment/service/...`: snapshot, mapping, authorization và transaction transfer-with-scores.
- `BE/.../enrollment/domain/DTOs/...`: request/response typed DTO.
- `BE/.../scorebook/...`: chỉ mở rộng nếu cần service/repository API để đọc/ghi target score entries.
- `document/application-doc/v2/frontend-api/03-teacher-assignment-enrollment.md` và `05-scorebook-change-audit.md`: ghi contract thực tế.

### Frontend sau khi contract được approve

- `FE/src/components/TransferEnrollmentDialog.vue`: thêm bước assist/hand-off.
- `FE/src/components/TransferScoreAssistDialog.vue`: tạo dialog hai phía.
- `FE/src/components/TransferScoreAssistDialog.stories.ts` và `.spec.ts`: fixture states.
- `FE/src/services/enrollmentApi.ts`: typed assist/mutation methods.
- `FE/src/types/enrollment.ts`/`scorebook.ts`: DTO và form state.
- `FE/src/views/EnrollmentListView.vue`: orchestration và reload.
- `document/dev-note/fe/enrollment/069-...md`: ghi thay đổi thực tế sau implementation.

## 7. Test và validation dự kiến

- Service tests: query đúng `enrollmentId`, `targetClassId`, `semesterId`; map đúng evidence; không nhầm missing với `0.0`.
- Component tests: read-only source; edit target; copy suggestion; required/score-range/one-decimal validation; disabled while saving; `403`/`409` giữ dialog và không mất draft.
- Integration tests BE: unauthorized/out-of-scope, closed/published scorebook rules, duplicate target entry, optimistic conflict, rollback khi score write fail, audit/history preserved.
- Storybook: `DefaultWithEvidence`, `NoExistingScores`, `UnmappedColumns`, `ValidationError`, `Conflict409`, `Forbidden403`, `Saving`.
- FE gates theo `FE/package.json`: lint, test, coverage, build, Storybook build nếu bị ảnh hưởng. Browser visual QA/live backend chỉ báo `PASS` khi thực sự chạy.

## 8. Rủi ro và giảm thiểu

- **Mapping sai môn/cột:** backend trả mapping key/label; FE không suy diễn theo tên lớp hoặc vị trí mảng.
- **Mất dữ liệu khi transfer:** giữ source score read-only, ưu tiên transaction server-side, reload sau success.
- **Stale score:** truyền `expectedVersion` theo contract và xử lý `409` bằng reload snapshot, không overwrite âm thầm.
- **Scorebook khác trạng thái:** backend quyết định writable; FE hiển thị warning/blocked state.
- **Popup quá rộng trên màn hình nhỏ:** content scroll nội bộ, footer sticky/luôn thao tác được, bảng có overflow ngang.

## 9. Acceptance criteria

- Học sinh không có điểm học kỳ: transfer vẫn đi theo flow hiện tại, không mở bảng assist bắt buộc.
- Học sinh có điểm: popup cho thấy source evidence và target columns trong cùng context; source không editable.
- Người dùng có thể nhập/lưu từng target score theo mapping đã chốt; ô trống không biến thành `0`.
- Transfer thành công giữ history/source scores và target scores được backend xác nhận.
- Lỗi validation/403/409/network không làm mất draft hoặc tạo thông báo thành công giả.
- Không có official average/calculation do FE tự tính.

## 10. Output và quyết định cần user approve

Đề nghị approve đồng thời:

1. Wireframe hai bước: transfer form → score assist popup → confirmation.
2. Gợi ý mapping chỉ prefill draft, không auto-submit.
3. Backend contract gate và ưu tiên mutation nguyên tử `transfer-with-scores`.
4. Phạm vi một học sinh/lần; không bao gồm bulk transfer.

Sau khi approved, bước tiếp theo là lập/chốt BE contract amendment, rồi Storybook-first review cho dialog trước production implementation.
