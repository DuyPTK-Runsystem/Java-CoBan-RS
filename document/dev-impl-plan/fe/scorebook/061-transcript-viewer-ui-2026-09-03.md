# Developer Plan 061: Transcript Viewer UI

## 1. Trạng thái và nguyên tắc approval

- **Status**: `Approved` — đã được người dùng phê duyệt qua agent message; bắt đầu implementation.
- **Application-document version**: `v2`.
- **Ngày lập & phê duyệt**: `2026-09-03`.
- **Wireframe bắt buộc**: [061 Transcript Viewer UI wireframe](../../../wireframes/fe/transcript/061-transcript-viewer-ui/README.md), [static preview](../../../wireframes/fe/transcript/061-transcript-viewer-ui/index.html).
- **Nguyên tắc**: triển khai theo đúng giao diện bảng ma trận động đã được người dùng duyệt.

## 2. Mục tiêu

Thiết kế một màn hình FE read-only để học sinh xem bảng điểm tổng kết theo học kỳ hoặc năm học, với:

- tab học kỳ / năm học;
- dòng môn học và điểm theo contract transcript;
- phân biệt điểm thường, điểm thi lại và nguồn kết quả;
- trạng thái `IN_PROGRESS` / `FINISH`, phiên bản dữ liệu và thời điểm tính gần nhất;
- trạng thái loading, empty/not-found và lỗi transport/authorization;
- hành vi đúng với session `401`, quyền `403`, resource `404` và conflict `409` nếu backend trả về.

## 3. Căn cứ requirement và contract

- Requirement `FR-SUMMARY-001` đến `FR-SUMMARY-011` trong `document/application-doc/v2/modules/06-RetakeAndTranscriptModule.md`.
- Quy tắc `BR-SUMMARY-005`/`006`: `IN_PROGRESS` không được trình bày như kết quả chính thức; `FINISH` mới được coi là kết quả hiện tại theo version contract.
- Quy tắc `BR-SUMMARY-007`: khi có retake phải hiển thị đồng thời điểm trước retake, điểm thi lại và điểm chính thức sau retake.
- API hiện được mô tả trong `document/application-doc/v2/frontend-api/06-transcript-retake-calculation.md`:
  - `GET /api/v2/transcripts/students/me/semesters/{semesterId}`;
  - `GET /api/v2/transcripts/students/me/academic-years/{academicYearId}`;
  - status tương ứng cho học kỳ/năm học.
- Staff endpoints `/{studentId}` được nêu trong guide nhưng không được đưa vào implementation mặc định nếu chưa chốt scope/role contract.

## 4. Contract gate và blocker phải giữ nguyên

### 4.1. Inconsistency của Plan 046

`document/dev-impl-plan/BE/scorebook/046-transcript-query-api-2026-08-26.md` hiện ghi `Status: Proposed — chưa được phê duyệt, chưa triển khai`, trong khi `document/dev-note/be/scorebook/046-transcript-query-api-2026-08-26.md` ghi đã triển khai các endpoint query và summary FE ghi Plan 046 là `Completed`.

Đây là **contract/documentation gate** của Plan 061. Plan FE không được tự chọn một trạng thái làm sự thật, không được xem Dev Note/summary là approval thay cho Developer Plan và không được tự sửa Plan 046 trong phạm vi này. Trước implementation cần người dùng/owner:

1. xác nhận Plan 046 đã được approved và implementation thực tế là contract áp dụng; hoặc
2. cập nhật/chốt lại Developer Plan 046, response DTO, status code và test evidence.

Cho tới khi gate được giải quyết, Plan 061 chỉ có thể được duyệt ở mức **Draft + wireframe**, không được coi là implementation-ready. Gate đã được giải quyết qua phê duyệt triển khai của người dùng dựa trên contract và code thực tế của Plan 046.

### 4.2. Staff scope và role discovery

- Scope chắc chắn cho v1 của màn hình là `STUDENT` xem transcript của chính mình qua `/me`.
- Staff scope `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER` chỉ được bật nếu backend contract được chốt cùng response shape và assignment scope. Teacher không được suy ra có quyền xem mọi học sinh từ role `TEACHER`.
- FE hiện không được giả định role/capability từ username, route, JWT hoặc việc một request trước đó thành công. Backend vẫn là authority; role-aware navigation/visibility cần contract role/account được approved.
- Nếu staff scope chưa được chốt, không tạo student picker, không nhận `studentId` tùy ý từ UI và không mock staff authorization như một capability đã có.

### 4.3. DTO và enum drift

- FE phải dùng đúng wire shape của backend sau khi Plan 046 được chốt.
- `SubjectType` hiện tại là `ACADEMIC | SKILL`; không dùng `NORMAL` như wire enum dù một ví dụ cũ trong Plan 046 dùng `NORMAL`.
- `CalculationStatus` dùng `IN_PROGRESS | FINISH`.
- `CalculationResultSource` hiện chỉ cho phép `REGULAR | RETAKE`; không thêm `MIXED`.
- Điểm `null`/chưa có dữ liệu phải giữ khác với điểm `0.0`; FE không tính lại điểm chính thức.
- Pagination chỉ được thêm nếu transcript endpoint thực sự trả `Page` hoặc contract mới quy định pagination. Response hiện tại được mô tả là danh sách `subjects`, nên mặc định không được tự chia trang client-side.

## 5. Phạm vi

### 5.1. In-scope sau khi contract gate được chốt

- Route-level transcript view read-only trong module `transcript`.
- Student self-service cho term và annual transcript.
- Bảng term dạng ma trận chuẩn học bạ (Dynamic Grid Matrix):
  - Header 2 tầng: STT, Môn học, nhóm KTTX (colspan động), nhóm KTĐK (colspan động), nhóm KTCK (colspan động), TBMHK, Ghi chú.
  - Cột con động: số cột con của KTTX, KTĐK, KTCK được tự động tính toán dựa trên số lượng cột thực tế lớn nhất của các môn trong kỳ (không cố định cứng 4 hay 2 cột).
  - Ánh xạ điểm vào đúng số thứ tự cột con (`columnNo`), giữ rỗng các ô môn chưa có điểm. Môn kỹ năng (`SKILL`) thể hiện đánh giá Đạt/Chưa đạt rõ ràng.
  - Khối Footer Summary bên dưới bảng: hiển thị Điểm trung bình học kỳ (`dtbhk`), thông tin chuyên cần/vắng học (nếu có context điểm danh) và metadata tính toán (`version`, `calculatedAt`).
- Bảng annual: HK1, HK2, `regularDtbmhCn`, `officialDtbmhCn`, `calculationSource`, retake detail khi có, `regularDtbcn`/`finalDtbcn` theo response.
- Banner trạng thái calculation, `sourceVersion`, `calculatedVersion`, `calculatedAt` và `lastCalculationTaskId` chỉ khi field nằm trong DTO đã chốt.
- Retake/source presentation: `REGULAR` hoặc `RETAKE`, điểm trước/sau và ngày/note nếu API trả.
- Loading skeleton/state, empty state cho chưa có transcript hợp lệ hoặc chưa có môn, not-found `404`, access denied `403`, session expiry `401`, conflict `409` và generic retryable error.
- Responsive table/card treatment, keyboard/focus semantics, accessible labels và read-only visual language.
- Typed API service, response adapter/formatter và component/view tests.
- Deterministic Storybook stories cho các trạng thái có ích để review UI, không cần live backend.

### 5.2. Staff scope tùy chọn, chưa được cam kết

Chỉ bổ sung sau approval riêng của contract:

- staff chọn/xem một `studentId`;
- staff scope theo `ADMIN`, `ACADEMIC_OFFICE`, hoặc teacher assignment;
- student identity header và permission messaging.

Nếu contract gate không chốt, phần này là out-of-scope của implementation đầu tiên.

### 5.3. Out-of-scope

- Tính `dtbhk`, `dtbcn`, `ĐtbmhCN` hoặc bất kỳ official average nào trong FE.
- Gọi command recalculate, retry calculation, sửa điểm, nhập điểm hoặc retake score.
- Tạo API mới, sửa BE Plan 046, sửa schema/migration/enums hoặc cập nhật summary dùng chung.
- Suy ra role, assignment, scope lớp/môn hoặc quyền từ dữ liệu không có contract.
- Client-side pagination/sort toàn bộ dataset khi backend trả dữ liệu đã phân trang hoặc chưa công bố pagination.
- Export/print transcript nếu chưa có requirement/contract được approve.

## 6. Luồng UI dự kiến

```text
Authenticated student
  -> open Transcript Viewer
  -> load academic-year/semester options from approved source
  -> select Term or Annual tab
  -> GET /me/... transcript
  -> render read-only result + calculation metadata
  -> optional refresh/status GET when IN_PROGRESS
```

Behavior:

- `IN_PROGRESS`: hiển thị “Đang cập nhật”, cho phép refresh/status theo API đã có; không gọi recalculation và không gắn nhãn điểm cũ là official latest.
- `FINISH` + version hợp lệ: hiển thị kết quả chính thức và `calculatedAt`.
- Không có transcript/resource: phân biệt empty domain state với HTTP `404` theo error envelope.
- `401`: clear session theo auth rule và điều hướng Login.
- `403`: giữ session, hiển thị access denied.
- `409`: vì màn hình read-only không tạo mutation, chỉ hiển thị conflict/reload guidance nếu backend contract thực sự trả `409`; không tự retry vô hạn.

## 7. Danh sách module/file triển khai thực tế

Đã hoàn thành triển khai các file sau:

- `FE/src/types/transcript.ts` (DTO types, enum AssessmentType hỗ trợ 'KTTT' | 'KTTX' | 'KTDK' | 'KTĐK' | 'KTCK');
- `FE/src/services/transcriptApi.ts` (API client cho các endpoints transcript và calculation status);
- `FE/src/services/transcriptApi.spec.ts` (Unit tests cho API client);
- `FE/src/components/TranscriptTermTable.vue` (Bảng ma trận điểm học kỳ động, hỗ trợ khớp enum có/không dấu, hiển thị số buổi vắng);
- `FE/src/components/TranscriptTermTable.spec.ts` (Unit tests cho bảng học kỳ);
- `FE/src/components/TranscriptTermTable.stories.ts` (Storybook stories cho bảng học kỳ);
- `FE/src/components/TranscriptAnnualTable.vue` (Bảng điểm tổng kết cả năm);
- `FE/src/components/TranscriptAnnualTable.spec.ts` (Unit tests cho bảng cả năm);
- `FE/src/components/TranscriptAnnualTable.stories.ts` (Storybook stories cho bảng cả năm);
- `FE/src/views/TranscriptViewerView.vue` (Màn hình tra cứu bảng điểm học sinh, tích hợp attendance API);
- `FE/src/views/TranscriptViewerView.spec.ts` (Unit tests cho màn hình tra cứu);
- `FE/src/router/index.ts` (Đăng ký route `/v2/transcripts`);
- `FE/src/views/AuthenticatedV2ShellView.vue` (Menu Bảng điểm trên thanh điều hướng sidebar).

## 8. Test và validation dự kiến

- Service tests: URL/HTTP method, `/me` identity boundary, response mapping, `null` vs `0`, enum values và error normalization.
- View/component tests: term/annual switching, loading, empty, `IN_PROGRESS`, `FINISH`, retake/source display, no-result, `401`, `403`, `404`, `409`.
- Permission tests: student self route; staff path chỉ khi staff contract được approved; không xem role từ guessed client data.
- Pagination tests chỉ khi contract bổ sung pagination.
- Storybook: static deterministic stories cho `Loading`, `Empty`, `InProgress`, `Finish`, `Retake`, `Unauthorized`, `Forbidden`, `NotFound`/error.
- Validation theo `FE/package.json` sau implementation: `npm run lint`, `npm run test`, `npm run test:coverage`, `npm run build`; Storybook build nếu cấu hình/ảnh hưởng.
- Browser walkthrough/live backend: chỉ báo `PASS` khi thực sự có browser/session/backend và đã click-through; nếu không có thì ghi `NOT RUN`/`BLOCKED`.

## 9. Deliverables và điều kiện hoàn thành

### Deliverables

- Transcript viewer implementation đúng contract đã chốt.
- Typed API boundary và tests.
- Storybook deterministic states.
- Dev Note ghi actual changes và validation evidence.

### Definition of Done

- Plan 061 và wireframe được approved qua agent message.
- Plan 046 Proposed/Completed inconsistency được owner chốt; response/status/role contract có bằng chứng.
- Student self transcript hoạt động không vượt scope; staff scope chỉ có khi được chứng minh.
- Hiển thị đúng term/annual, retake/source, `IN_PROGRESS`/`FINISH`, versions/time và null/zero semantics.
- Tất cả error states liên quan có test và UI behavior rõ ràng.
- Quality gates thực tế được chạy và báo đúng PASS/FAIL/BLOCKED/NOT RUN.

## 10. Trạng thái hoàn thành

- Kế hoạch đã được phê duyệt và hoàn tất triển khai (Completed).
- Đã giải quyết toàn bộ các contract blocker và xử lý linh hoạt enum `AssessmentType` giữa backend và frontend.
- Tích hợp thành công dữ liệu chuyên cần học kỳ (`fetchStudentAttendanceHistory`).
- Toàn bộ các bước kiểm tra chất lượng (Quality Gates) đạt trạng thái `PASS`.
- Chi tiết quá trình thực hiện và bằng chứng kiểm thử được ghi nhận tại [Dev Note 061](../../../dev-note/fe/scorebook/061-transcript-viewer-ui-2026-09-03.md).
