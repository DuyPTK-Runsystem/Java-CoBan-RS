# Plan 078 BE — Import một cột điểm từ `.xlsx`

## Trạng thái và approval gate

- Application-document version: `v3`.
- Status: `APPROVED — implementation delivered with v2 bulk-extension amendment`.
- Approval: người dùng approve Plan 078 và wireframe qua agent message ngày `2026-09-10`.
- Ngày lập plan: `2026-09-10`.
- Plan này đi cùng [Plan 078 FE](../../../fe/scorebook/078-one-column-score-import-ui-2026-09-10.md) và [wireframe](../../../../wireframes/fe/scorebook/078-one-column-score-import/README.md).
- Implementation amendment: flow giữ nguyên write contract bulk v2; preview stateless trả payload chuẩn hóa trực tiếp, chưa persist `ScoreImportBatch/Row`.
- Layout amendment (approved qua agent message ngày 2026-09-10): file tải xuống dùng form tiếng Việt gồm tiêu đề/context ở trên và đúng 5 cột `STT | Mã học sinh | Họ tên học sinh | Điểm số | Ghi chú`; không có cột trạng thái hoặc vùng `current*`.

### Unit-test plan cho layout amendment

- `createTemplate`: sinh một sheet `Form nhập điểm`, title/context và header tại dòng 6; roster tạo `STT`, mã, tên, điểm số, ghi chú.
- `preview`: nhận header tiếng Việt tại dòng 6, bỏ qua phần metadata; điểm `8.47` chuẩn hóa `8.5`; dòng trống bị skip; note-only chỉ hợp lệ khi học sinh đã có điểm.
- Regression: header cũ hoặc file không đúng 5 cột bị từ chối; duplicate mã, điểm ngoài 0–10 và file không phải `.xlsx` vẫn bị từ chối.

## 1. Mục tiêu

Mở rộng tính năng `BulkScoreEntryDialog`/bulk add-update hiện có của v2 bằng một nguồn
dữ liệu mới: file `.xlsx`. Người dùng phải tải file mẫu có sẵn toàn bộ danh sách học
sinh của lớp trước, điền dữ liệu, upload lại, xem preview rồi mới xác nhận.

Kết quả mong muốn:

1. File mẫu được tạo từ đầy đủ roster của lớp/scorebook, không chỉ page đang hiển thị.
2. Không để file tự quyết định cột điểm đích.
3. Không ghi dữ liệu khi file còn lỗi hoặc bị trùng học sinh.
4. Giữ nguyên semantics của bulk add/update v2, bao gồm tạo mới, cập nhật và các status hiện có.
5. Commit có transaction, optimistic locking, audit và không tạo partial commit âm thầm.

## 2. Requirement và nguồn đối chiếu

- `FR-V3-IMPORT-001..003`, `BR-V3-IMPORT-001` trong `RequirementBaseline.md`.
- Lifecycle và boundary trong `modules/04-QueryAndScoreImport.md`.
- `TBD-005` trong `CR-V3-001` và Dev Note 073.1.
- `ScoreImportBatch/Row` trong `application-doc/v3/data-model/README.md`.
- Contract/error/audit/optimistic-locking checkpoint của Plan 073.
- Contract hiện có của scorebook: `ScoreEntryService`, `ScoreEntryValidator`,
  `ScoreEntryWriter`, `ScorebookGuard` và `StudentScore.version`.

## 3. Quyết định đề xuất cần user approve

| Chủ đề | Quyết định đề xuất | Lý do / giới hạn |
|---|---|---|
| Định dạng | Chỉ nhận `.xlsx`, một worksheet, dòng header đầu tiên | Hành vi xác định, dễ preview và kiểm tra |
| File mẫu | Sinh từ toàn bộ roster của `classSubject`/scorebook; identity columns được khóa hoặc đánh dấu chỉ đọc | Không dùng danh sách của một page hiện tại |
| Header | `studentCode`, `studentName`, `currentStatus`, `currentScore`, `scoreStatus`, `scoreValue`, `note` | Hai cột `current*` chỉ để đối chiếu; ba cột cuối là vùng nhập |
| Định danh | Chỉ dùng `studentCode` để resolve học sinh | Tái sử dụng `StudentLookupService`, không match bằng tên |
| Giá trị | Reuse rule v2: `0–10`, tối đa một chữ số thập phân; status `SCORED`, `ABSENT`, `EXEMPTED`, `CANCELLED` | Không tạo semantics mới ngoài bulk add/update v2 |
| Ô nhập trống | Nếu `scoreStatus`, `scoreValue`, `note` đều không đổi/để trống thì bỏ qua dòng; nếu đã chọn `SCORED` nhưng thiếu điểm thì báo lỗi | Cho phép template chứa toàn bộ học sinh nhưng chỉ submit dòng cần thay đổi |
| `CANCELLED` | Được hỗ trợ đúng như bulk v2; `scoreValue` phải rỗng | Không tự biến ô trống thành `CANCELLED` |
| Dòng không đổi | Nếu `scoreStatus`, `scoreValue`, `note` đều không có thay đổi thì bỏ qua; nếu đã chọn `SCORED` nhưng thiếu điểm thì lỗi | Template có thể chứa toàn bộ roster |
| Duplicate | Trùng `studentCode` trong file là lỗi batch | Không chọn ngầm dòng đầu/cuối |
| File size | Tối đa 10 MB và 2.000 dòng dữ liệu, cấu hình được | Giới hạn predictable cho preview/commit |
| Partial commit | Reject-all: có lỗi thì không được commit dòng hợp lệ | Đáp ứng wrong-column/partial-commit protection |
| Rounding | Reuse chuẩn v2: chuẩn hóa tối đa một chữ số thập phân trước preview/commit và hiển thị giá trị sau chuẩn hóa | Người dùng review được giá trị cuối cùng |
| Quyền | Reuse `ScorebookGuard` và assignment access hiện có; không suy quyền ở FE | Backend là nguồn quyết định cuối |

Các dòng trên là **đề xuất để review**, không được coi là contract đã approved cho tới
khi người dùng xác nhận Plan 078. Đặc biệt cần xác nhận `TBD-003` về quyền import.

## 4. Phạm vi

### 4.1. In-scope

- Contract checkpoint cho preview/commit/template.
- Tạo file mẫu `.xlsx` bằng thư viện Java được duyệt trong dependency review, có đầy đủ
  roster và vùng nhập liệu rõ ràng.
- Parse `.xlsx` do người dùng điền lại bằng cùng format/template version.
- Preview row snapshot trả ngay trong response; persist `ScoreImportBatch/Row` và batch history được deferred để không tạo một workflow import tách khỏi bulk v2.
- Resolve `studentCode`, kiểm tra học sinh thuộc roster đầy đủ của scorebook và kiểm tra
  `assessmentColumnId` thuộc `scorebookId` đã chọn.
- Preview từng dòng gồm học sinh, giá trị/trạng thái cũ, giá trị/trạng thái mới và lỗi.
- Commit nguyên tử các dòng hợp lệ sau khi batch ở trạng thái `PREVIEWED` không còn lỗi.
- Reuse `ScoreEntryValidator`, `ScoreEntryWriter`, audit và transcript/calculation
  touch flow hiện có khi ghi `StudentScore`.
- Audit batch và các thay đổi điểm before/after.
- Contract/integration/unit tests cho parse, validate, preview, commit, conflict và quyền.

### 4.2. Out-of-scope

- Import nhiều `assessmentColumnId` trong một lần upload.
- Tạo assessment column mới từ file.
- Match học sinh bằng tên, ngày sinh hoặc vị trí dòng.
- Xóa điểm bằng ô trống; status `CANCELLED` chỉ được dùng theo semantics bulk v2.
- Partial commit, retry tự động mù hoặc ghi đè khi version đã thay đổi.
- CSV, Google Sheets, offline import, background worker hoặc lưu file raw lâu dài.
- Thay đổi lifecycle scorebook, calculation formula hoặc transcript contract ngoài touch
  cần thiết sau khi score thay đổi.

## 5. Luồng nghiệp vụ sau thay đổi

```text
Mở BulkScoreEntryDialog v2 cho một assessmentColumnId
        |
Tải file mẫu có toàn bộ roster của lớp
        |
Điền scoreStatus/scoreValue/note -> upload lại .xlsx
        |
Parse -> resolve studentCode -> validate từng dòng
        |
PREVIEWED: hiển thị old/new/status/error
        |
Có lỗi? ---- Có ----> không cho gọi bulk save
   |
  Không
   |
BulkUpsertStudentScoreRequest v2 -> ScoreEntryService.bulkUpsertScores
        |
transaction -> audit -> touch transcript/task
```

Lifecycle batch:

```text
UPLOADED -> PARSED -> VALIDATED -> PREVIEWED -> COMMITTED
                                      \-> REJECTED
```

Preview không persist lifecycle batch trong amendment này. Dòng không thay đổi không được
đưa vào payload; retry sau conflict phải tải lại preview và không được ghi đè dữ liệu mới hơn.

## 6. API contract đề xuất

Endpoint dưới đây là proposal cho contract checkpoint; không tự triển khai nguyên tên
endpoint nếu checkpoint chưa được duyệt.

### Template download

```text
GET /api/v2/assessment-columns/{columnId}/scores/bulk-template
```

Response là file `.xlsx` chứa toàn bộ học sinh thuộc `classSubject` của cột điểm đã
chọn. Template phải có metadata/template version và các cột:

```text
studentCode | studentName | currentStatus | currentScore | scoreStatus | scoreValue | note
```

`studentCode`, `studentName`, `currentStatus`, `currentScore` là cột tham chiếu; vùng
`scoreStatus`, `scoreValue`, `note` là vùng người dùng điền. Template phải lấy toàn bộ
roster, không lấy theo page của `ScoreGrid`.

### Preview

```text
POST /api/v2/assessment-columns/{columnId}/scores/bulk/preview
Content-Type: multipart/form-data

file: .xlsx
```

Response đề xuất giữ cùng shape preview batch, đồng thời trả payload chuẩn hóa có thể
chuyển thành `BulkUpsertStudentScoreRequest` của v2:

```json
{
  "batchId": 101,
  "status": "PREVIEWED",
  "scorebookId": 12,
  "assessmentColumnId": 35,
  "file": {
    "originalName": "diem-giua-ky.xlsx",
    "sizeBytes": 18240,
    "sha256": "...",
    "rowCount": 32
  },
  "summary": {
    "validRows": 29,
    "errorRows": 3,
    "newScores": 20,
    "updatedScores": 9
  },
  "rows": [
    {
      "rowNumber": 2,
      "studentCode": "STU0000001",
      "studentId": 1,
      "studentName": "Nguyễn An",
      "oldStatus": "SCORED",
      "oldValue": 7.0,
      "oldVersion": 3,
      "newStatus": "SCORED",
      "newValue": 8.5,
      "result": "VALID",
      "errorCode": null,
      "message": null
    }
  ]
}
```

### Commit

```text
POST /api/v2/assessment-columns/{columnId}/scores/bulk
```

Đây vẫn là write contract canonical của bulk add/update v2. File mode không tạo một
đường ghi điểm khác: sau khi user confirm preview, FE gửi payload đã chuẩn hóa qua API
này. Backend phải kiểm tra lại actor scope, column identity, semester status và
`StudentScore.version`; nếu có conflict, transaction bulk hiện có phải rollback toàn bộ.

Nếu contract checkpoint yêu cầu batch commit server-side thay vì gửi payload v2, endpoint
commit mới phải vẫn delegate tới `ScoreEntryService.bulkUpsertScores`, không được nhân đôi
semantics ghi điểm.

Template download là endpoint bổ sung cho bulk v2, không phải một màn hình import độc lập.
Không dùng file mẫu tĩnh vì roster và current score/status phải được lấy theo scorebook
đang chọn. Không cho template quyết định cột điểm đích.

## 7. Thiết kế backend dự kiến

### 7.1. Domain và persistence

Tạo aggregate riêng trong package `scorebook`:

- `ScoreImportBatch` — context, metadata, status, counts, actor/time, `@Version`; chỉ
  giữ preview snapshot nếu contract yêu cầu audit/review sau request.
- `ScoreImportRow` — row number, student identity, old/new snapshot, result/error.
- `ScoreImportBatchStatus`, `ScoreImportRowResult` và error-code enum nếu contract
  checkpoint chấp thuận machine-readable errors.
- Flyway migration cho batch/row, FK/index theo `scorebook_id`, `assessment_column_id`,
  `batch_id`, `student_code`.

Không lưu raw file mặc định; lưu hash, tên, content type, size và parsed snapshot để
review/commit/audit. Retention của batch phải được ghi rõ trong data decision.

### 7.2. Service boundary

- `ScoreImportController`: multipart preview và commit, mapping HTTP/validation.
- `ScoreImportService`: tạo template, preview orchestration và snapshot batch; không thay
  thế `ScoreEntryService` cho final write.
- `ScoreImportTemplateService`: tạo workbook từ full roster và metadata cột điểm.
- `ScoreImportParser`: đọc worksheet/header/cell type, không chứa authorization.
- `ScoreImportValidator`: template version, header, size, identity, duplicate, score/status, roster,
  existing version và target-column checks.
- `ScoreImportMapper`: DTO preview/summary/commit result.
- Repository cho batch/row; query existing `StudentScore` theo target column và roster.
- Reuse `ScorebookGuard`, `ScoreEntryContext`, `ScoreEntryValidator`, `ScoreEntryWriter`,
  `ScorebookAuditService`, `TranscriptStateService` và `CalculationTaskService`.

Không gọi controller hiện có từ controller mới; import service có thể dùng shared
writer/service boundary để giữ một cách ghi điểm và audit.

### 7.3. Dependency

Build hiện chưa có thư viện `.xlsx` rõ ràng. Plan đề xuất thêm `org.apache.poi:poi-ooxml`
với version được dependency management duyệt, kèm kiểm tra license, CVE và giới hạn
memory. Nếu dependency review chọn thư viện khác thì cập nhật plan trước implementation.

## 8. Phạm vi mã nguồn dự kiến

| Khu vực | Dự kiến |
|---|---|
| `BE/.../scorebook/controller/` | Tạo `ScoreImportController` |
| `BE/.../scorebook/service/` | Tạo parser/validator/service/mapper; reuse score entry flow |
| `BE/.../scorebook/domain/entity/` | Tạo batch/row/status/result |
| `BE/.../scorebook/domain/DTOs/` | Tạo preview row/summary/batch/commit response |
| `BE/.../scorebook/repository/` | Tạo batch/row repositories và query cần thiết |
| `BE/.../src/main/resources/db/migration/` | Tạo migration mới sau schema approval |
| `BE/BaiTap-RS/build.gradle.kts` | Thêm `.xlsx` parser dependency nếu được duyệt |
| `BE/.../scorebook/**/test/` | Unit/service/controller/integration tests |
| API documentation v2/v3 | Ghi template/preview extension và quan hệ với bulk write contract thực tế sau checkpoint |

## 9. Test và validation dự kiến

### Unit/service

- Template download lấy đủ roster, current score/status đúng cột và không phụ thuộc page hiện tại.
- Header/template version đúng/sai, thiếu header, header duplicate, worksheet thứ hai bị từ chối.
- File rỗng, file quá size, quá số dòng, sai extension/content type.
- `studentCode` hợp lệ, không tồn tại, không thuộc roster, tên hiển thị mismatch.
- Duplicate student code trong file.
- `0`, `10`, `8.5`, `11`, `12`, `CANCELLED` theo bulk v2; âm, >10, `13`, text mơ hồ lỗi.
- Dòng roster có các ô nhập đều trống được bỏ qua; dòng có `SCORED` nhưng thiếu điểm là lỗi.
- Rounding/normalization khớp với `BulkScoreEntryDialog` v2 và preview hiển thị giá trị cuối.
- Ô chưa có điểm tạo mới; ô có điểm cập nhật với old/new/version.
- Commit reject-all khi còn một lỗi; commit rollback khi xảy ra conflict giữa preview và commit.
- Commit gọi lại không tạo duplicate/audit/task lần hai.

### Integration/security

- `401`, `403`, teacher ngoài assignment, academic office/admin theo role matrix.
- Column không thuộc scorebook, scorebook không writable, semester locked/closed.
- Audit batch và audit before/after từng score.
- Reload score grid/transcript sau commit cho thấy dữ liệu đã lưu.

### Quality gates

- Backend test, JaCoCo, Checkstyle, PMD và build theo `backend-validation`.
- FE gates và Storybook theo Plan 078 FE.
- Browser/live API/large-file/performance/SMTP: `NOT RUN` nếu chưa có environment và
  fixture phù hợp; không gọi unit/integration test là browser evidence.

## 10. Rủi ro và giảm thiểu

- **Ghi nhầm cột:** bắt buộc gửi `scorebookId` + `assessmentColumnId`, kiểm tra quan hệ
  server-side và không đọc target column từ file.
- **Sai học sinh:** chỉ resolve bằng `studentCode`, hiển thị tên để review, không match tên.
- **Dữ liệu thay đổi sau preview:** lưu old version từng dòng và reject toàn batch khi conflict.
- **Partial commit:** validate toàn bộ trước, transaction commit reject-all, có integration test rollback.
- **File lớn/POI memory:** giới hạn size/dòng/sheet, kiểm tra dependency và không lưu raw file.
- **Quyền import chưa chốt:** giữ `TBD-003`, reuse guard hiện có, không tự mở rộng role.
- **Template bị sửa identity:** bảo vệ/đánh dấu cột identity và reject khi mã không thuộc roster.

## 11. Acceptance criteria

- Trước khi upload, user có thể tải file mẫu chứa toàn bộ danh sách học sinh của lớp và cột tham chiếu current score/status.
- Preview chỉ hoạt động với `.xlsx`, đúng một scorebook và một assessment column.
- Preview hiển thị đầy đủ dòng hợp lệ/lỗi, học sinh, old/new value/status.
- File có lỗi không thể commit và không làm thay đổi `StudentScore`.
- Commit chỉ ghi đúng target column, không tạo cột mới và dùng lại `ScoreEntryService.bulkUpsertScores` của v2.
- `0`, `ABSENT`, `EXEMPTED`, `CANCELLED` giữ đúng semantics bulk v2; dòng không thay đổi không được gửi.
- Commit tạo audit và kích hoạt flow transcript/calculation hiện có đúng một lần cho dữ liệu thay đổi.
- Unauthorized/out-of-scope/locked/conflict đều trả lỗi chuẩn, không làm mất trạng thái preview ở FE.
- Có contract fixture dùng chung với FE và Dev Note ghi validation thực tế sau implementation.

## 12. Output và bước tiếp theo

Sau khi user approve Plan 078 + wireframe + các quyết định đề xuất ở mục 3:

1. Chốt contract DTO/API/error/role matrix.
2. Chốt schema migration và dependency `.xlsx`.
3. Lập test fixture BE/FE dùng chung.
4. Triển khai BE/FE song song theo vertical slice.
5. Chạy validation và tạo Dev Note thực tế.
