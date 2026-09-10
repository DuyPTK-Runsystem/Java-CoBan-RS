# Plan 078 FE — UI import một cột điểm

## Trạng thái và approval gate

- Application-document version: `v3`.
- Status: `APPROVED — implementation delivered with v2 bulk-extension amendment`.
- Approval: người dùng approve Plan 078 và wireframe qua agent message ngày `2026-09-10`.
- Ngày lập plan: `2026-09-10`.
- Wireframe bắt buộc review: [README](../../../../wireframes/fe/scorebook/078-one-column-score-import/README.md) · [static preview](../../../../wireframes/fe/scorebook/078-one-column-score-import/index.html).
- Implementation amendment: file mode nằm trong `BulkScoreEntryDialog` và emit cùng payload v2; không tạo route/workspace/batch history riêng.

## 1. Mục tiêu

Mở rộng trực tiếp `BulkScoreEntryDialog` của v2 bằng một chế độ nhập từ file mẫu.
Người dùng tải file mẫu có sẵn toàn bộ danh sách học sinh của lớp, điền điểm, upload
lại, xem preview rồi dùng cùng thao tác bulk add/update hiện có để lưu.

Kết quả mong muốn:

1. Cột điểm đích được chọn từ bulk dialog hiện có và luôn hiển thị trong preview.
2. Người dùng thấy rõ điểm/trạng thái cũ và giá trị mới trước khi commit.
3. Lỗi theo dòng có thể đọc được; nút xác nhận bị khóa khi còn lỗi.
4. Giữ nguyên semantics `SCORED`, `ABSENT`, `EXEMPTED`, `CANCELLED` của bulk v2.
5. `401`, `403`, `404`, `409` và lỗi mạng giữ thông tin cần thiết để người dùng xử lý.

## 2. Requirement và nguồn đối chiếu

- `FR-V3-IMPORT-001..003`, `BR-V3-IMPORT-001`.
- `modules/04-QueryAndScoreImport.md` và contract Plan 078 BE.
- `frontend-api/README.md`: typed service, lỗi `401/403/404/409`, không tự invent DTO.
- `FE/AGENTS.override.md` và agent rules scorebook/API/quality khi chuyển sang implementation.
- Existing scorebook workspace: `ScorebookWorkspaceView`, `ScoreGrid`,
  `AssessmentColumnPanel`, `scorebookApi` và `scorebook.ts`.

## 3. Phạm vi

### 3.1. In-scope

- Mở rộng `BulkScoreEntryDialog.vue`, giữ nguyên chế độ `Nhập trực tiếp` hiện có và
  thêm chế độ `Dùng file mẫu`.
- Trong chế độ file: `Tải file mẫu` → `Upload file đã điền` → `Xem trước` →
  `Lưu hàng loạt` bằng cùng bulk save flow v2.
- File mẫu có đầy đủ roster của lớp/scorebook, không phụ thuộc page hiện tại của grid.
- Hiển thị tên file, dung lượng, số dòng, cột điểm đích và mapping/template version.
- Preview có current status/current score, giá trị mới, status mới, note và lỗi từng dòng.
- Bảng preview có sort/filter trạng thái phía UI cho dữ liệu đã trả về; không lọc thay
  thế validation backend.
- Summary số dòng hợp lệ/lỗi, số điểm mới/cập nhật.
- Confirm dialog trước commit; disable khi có lỗi hoặc đang gửi.
- State `loading`, empty, file invalid, validation error, `403`, `404`, `409`, network error,
  success và retry/reload guidance.
- Typed API service, component tests, Storybook fixtures và responsive layout theo wireframe.

### 3.2. Out-of-scope

- Sửa trực tiếp từng ô trong preview để biến import thành spreadsheet editor.
- Cho file chọn assessment column hoặc import nhiều cột.
- Tạo một route/workspace import riêng bên ngoài scorebook/bulk dialog.
- FE tự match học sinh, tự tính điểm hoặc tự quyết định quyền.
- FE tự thực hiện partial commit khi backend trả lỗi.
- CSV, paste nhiều cột, offline mode, upload background và lịch sử batch riêng.
- Thay đổi navigation role-aware khi contract capability chưa được duyệt.

## 4. Luồng UI

```text
ScoreGrid v2 -> Nhập điểm hàng loạt
  -> Nhập trực tiếp | Dùng file mẫu
  -> Tải file mẫu có toàn bộ roster lớp
  -> Điền scoreStatus/scoreValue/note rồi upload lại
  -> Xem trước: current/new + lỗi từng dòng
  -> Có lỗi? quay lại thay file
  -> Hợp lệ: Lưu hàng loạt bằng bulk add/update v2
  -> Reload score grid
```

### Bố cục chính

- Header: `Nhập điểm hàng loạt`, giữ context/cột đã chọn từ score grid.
- Mode switch: `Nhập trực tiếp` giữ nguyên layout v2; `Dùng file mẫu` là phần bổ sung.
- File mode bước 1: giải thích file mẫu, nút `Tải file mẫu (32 học sinh)`, sau khi tải
  mới mở dropzone upload.
- File mode bước 2: banner kết quả parse; summary; bảng current/new; filter
  `Tất cả/Hợp lệ/Có lỗi`.
- File mode bước 3: xác nhận `Lưu hàng loạt`, dùng cùng loading/error/reload của v2.
- Table: dòng, mã học sinh, học sinh, điểm/trạng thái hiện tại, giá trị nhập,
  trạng thái nhập, ghi chú, kết quả.
- Responsive: dialog cuộn nội bộ, footer actions luôn nhìn thấy.

## 5. API/service boundary dự kiến

Mở rộng `FE/src/services/scorebookApi.ts` và `FE/src/types/scorebook.ts` theo contract
được BE approve. Không tạo service/DTO riêng nếu chỉ là transport bổ sung cho bulk v2.

Service cần cung cấp typed methods tương ứng với:

```text
downloadBulkScoreTemplate(token, columnId)
previewBulkScoreFile(token, columnId, file)
bulkUpsertStudentScores(token, columnId, request) // existing v2 final write
```

`BulkScoreFilePreview`, `BulkScoreFileRow`, `BulkScoreFileSummary` là các type preview;
`BulkUpsertStudentScoreRequest` vẫn là payload commit. Không đưa `File` vào DTO response.
`401` dùng shared auth flow; `403` giữ session; `409` giữ preview và hướng dẫn tải lại.

## 6. Component/view dự kiến

| Khu vực | Dự kiến |
|---|---|
| `ScorebookWorkspaceView.vue` | Giữ orchestration `saveBulk`, reload grid sau bulk save |
| `BulkScoreEntryDialog.vue` | Thêm mode file, download-before-upload, preview và emit cùng `save` payload |
| `BulkScoreImportPreview.vue` (chỉ nếu cần) | Render preview table/summary và row errors |
| `scorebookApi.ts` | Download template/preview transport; giữ `bulkUpsertStudentScores` hiện có |
| `scorebook.ts` | Type template/preview, reuse `BulkScoreItem`/`BulkUpsertStudentScoreRequest` |
| `*.spec.ts`, `*.stories.ts` | Deterministic states theo wireframe |

Tên component có thể gộp nếu test/ownership chứng minh flow nhỏ hơn; không tạo abstraction
chỉ để khớp wireframe.

## 7. State và interaction cần có

| State | Hành vi |
|---|---|
| Chưa tải file mẫu | Upload disabled, hiển thị nút tải mẫu và số học sinh |
| Đã tải mẫu, chưa upload | Cho chọn file `.xlsx` đã điền |
| Đang đọc file | Hiển thị loading, khóa input/đóng dialog có nguy cơ mất state |
| Preview hợp lệ | Cho `Lưu hàng loạt`, hiển thị số dòng tạo mới/cập nhật |
| Preview có lỗi | Hiển thị lỗi từng dòng, save disabled, cho chọn file khác |
| `403` | Giữ session, thông báo không đủ quyền, không xóa context |
| `404` | Báo scorebook/cột không còn tồn tại, yêu cầu quay lại tải context |
| `409` | Giữ summary/preview, yêu cầu tải lại dữ liệu trước khi commit lại |
| Thành công | Hiển thị summary, reload score grid sau khi user chọn xem lại |

## 8. Storybook và test dự kiến

### Storybook

- `ManualBulkMode` (giữ hành vi v2)
- `FileModeBeforeTemplateDownload`
- `TemplateDownloaded`
- `SetupWithFile`
- `PreviewValid`
- `PreviewWithRowErrors`
- `PreviewExistingScoreUpdate`
- `CommitSuccess`
- `Forbidden403`
- `Conflict409`
- `ResourceNotFound404`
- `NetworkError`
- `NarrowViewport`

Không gọi backend thật trong Storybook.

### Component/service tests

- Mode trực tiếp v2 không bị thay đổi.
- Không cho upload trước khi tải file mẫu.
- Gửi đúng `columnId`, `File`; không lấy cột từ file.
- Hiển thị roster/current value và new value; giữ đúng `0`, `ABSENT`, `EXEMPTED`, `CANCELLED`.
- Đếm summary đúng; lọc lỗi chỉ thay đổi hiển thị, không thay đổi commit payload.
- Disable save khi chưa tải mẫu, chưa preview, có lỗi hoặc đang commit.
- Giữ preview khi nhận `409`; không hiển thị success giả.
- Sau success gọi lại `bulkUpsertStudentScores` và reload score grid đúng một lần.
- `401/403/404/409` map đúng copy và hành vi session.

## 9. Validation dự kiến sau approval

- FE lint, unit/component test, coverage, build và Storybook build theo `FE/package.json`.
- `git diff --check` cho tài liệu/wireframe.
- Browser visual QA với static wireframe: review riêng.
- Browser/live API, upload file thật, large-file/performance và permission matrix:
  `NOT RUN` nếu chưa có môi trường resettable/fixture đủ role.

## 10. Acceptance criteria

- Người dùng nhìn thấy và tải được file mẫu có đầy đủ danh sách học sinh trước khi upload.
- Không thể upload trước khi tải file mẫu.
- Flow nằm trong `BulkScoreEntryDialog` và vẫn giữ nguyên chế độ bulk add/update v2.
- Preview luôn hiển thị context và cột điểm đích rõ ràng.
- Mỗi dòng thể hiện học sinh, old/new value/status và kết quả hợp lệ/lỗi.
- Có lỗi thì không thể xác nhận nhập điểm.
- Có conflict/quyền hạn/lỗi mạng thì không mất preview một cách âm thầm.
- Confirm gọi cùng bulk save contract v2; thành công reload bảng điểm để kiểm tra dữ liệu.
- Wireframe và Storybook bao phủ các trạng thái chính trước khi production UI được coi là done.

## 11. Output và bước tiếp theo

Sau khi user approve đồng thời BE plan, FE plan và wireframe:

1. Chốt DTO/error/role/partial-commit contract với BE.
2. Chốt fixture roster/template dùng chung.
3. Bổ sung Storybook state vào `BulkScoreEntryDialog`.
4. Nối template/preview service rồi reuse bulk save event hiện có.
5. Chạy FE validation, integration và browser/live evidence theo khả năng môi trường.
