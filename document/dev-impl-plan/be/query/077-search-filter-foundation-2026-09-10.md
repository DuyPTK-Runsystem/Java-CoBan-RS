# Plan 077 BE — Search/filter foundation cho danh sách và bảng điểm

## 0. Trạng thái và approval gate

- Application-document version: `v3`.
- Status: `TEMPORARILY POSTPONED — chưa triển khai; giữ plan để tiếp tục sau`.
- Ngày lập/cập nhật: `2026-09-10`.
- Thứ tự delivery: Plan 077 được review sau Plan 078; không mở lại contract import điểm.
- Plan liên kết: [Plan 077 FE](../../../fe/query/077-search-filter-foundation-ui-2026-09-10.md).
- Quyết định tạm hoãn: người dùng xác nhận qua agent ngày `2026-09-10`.
- Không triển khai production code/test, không xóa `V3PageResponse` và không migrate
  pagination consumer cho đến khi người dùng yêu cầu tiếp tục Plan 077.

## 1. Mục tiêu

Xây query contract v3 dùng lại cho các màn hình danh sách và bảng điểm, áp dụng theo
vertical slice có kiểm soát thay vì đổi toàn bộ API v2 trong một lần.

Plan phải bảo đảm:

1. Search/filter/sort chạy ở backend trước pagination.
2. Mọi endpoint phân trang trả `ResultPaginationDTO`; không tạo page envelope riêng theo version/domain.
3. Sort chỉ nhận field allow-list và có tie-breaker ổn định.
4. Authorization scope áp dụng trước query/count; metadata không lộ dữ liệu ngoài quyền.
5. Bảng điểm phân biệt điểm `0` với chưa có điểm.
6. API v2 không bị breaking change ngầm.

## 2. Requirement và nguồn đối chiếu

- Application-document v3: `ApplicationContext`, `RequirementBaseline`, `CR-V3-001`.
- `FR-V3-QUERY-001`, `FR-V3-QUERY-002`, `BR-V3-QUERY-001`, `NFR-V3-001`.
- Module `04-QueryAndScoreImport` và `frontend-api/README.md`.
- Quyết định amendment sau Plan 073: dùng `ResultPaginationDTO` làm contract phân trang duy nhất.
- Contract v2 hiện hành của Student, Teacher, Academic Catalog, Enrollment, Attendance,
  Scorebook, Retake, Calculation và Audit.

## 3. Hiện trạng hệ thống

### 3.1. Ma trận backend

| Nhóm | Hiện trạng | Đánh giá |
|---|---|---|
| Học sinh | `GET /api/v1|v2/students`; filter/page/sort tốt nhưng dùng page DTO riêng | Giữ query behavior, migrate response sang `ResultPaginationDTO` |
| Giáo viên | `GET /api/v2/teachers` trả `List`; backend chỉ lọc `status`; text search ở FE | Gap trực tiếp, ưu tiên Plan 077 |
| Năm học | API trả `List`; FE tìm mã/lọc trạng thái trên toàn bộ mảng | Gap, nhưng endpoint còn dùng làm lookup |
| Khối | API trả `List`; chủ yếu catalog/lookup nhỏ | Giữ lookup, không làm pilot |
| Lớp | API trả `List`, chỉ nhận `academicYearId`; FE lọc text/khối/trạng thái | Gap, nhưng nhiều form dùng làm lookup |
| Môn học | API trả `List`, backend có `status`; FE lọc text/type/scope/status | Gap, nhưng dùng làm lookup/config |
| Enrollment | Các API lớp/học sinh trả danh sách theo context | Chưa có shared envelope; defer để tránh ảnh hưởng xếp/chuyển lớp |
| Bảng điểm | `GET /api/v2/scorebooks/{id}/score-entries`; chỉ page/size, sort cố định `studentId` | Gap bắt buộc theo `FR-V3-QUERY-002` |
| Retake | Filter DTO + Specification + `Page` | Giữ query behavior, migrate response contract |
| Calculation task | Filter DTO + Specification + `Page` | Giữ query behavior, migrate response contract |
| Score audit | Nhiều filter + Specification + `Page` | Giữ query behavior, migrate response contract |
| Score change request | Filter DTO + Specification + `Page` | Giữ query behavior, migrate response contract |
| Attendance | History/summary có page query; một số aggregation paginate trong memory | Giữ aggregation, migrate response contract |

### 3.2. Foundation và vấn đề

- `V3PageResponse<T>` đã được Plan 073 tạo nhưng chưa có endpoint production sử dụng; artifact
  này được Plan 077 loại bỏ sau khi các test/fixture tham chiếu được chuyển sang `ResultPaginationDTO`.
- Student v2 có Specification/sort resolver an toàn và là mẫu tham khảo thực tế.
- Teacher và Academic Catalog đang lọc local trên mảng đã tải; cách này sai khi phân trang.
- ScoreGrid phân trang roster nhưng chưa nhận filter nghiệp vụ.
- Naming hiện lẫn `size`/`pageSize`, Spring `Page`, DTO page riêng và list thuần.
- Đổi thẳng list v2 sang page envelope sẽ làm vỡ dropdown/lookup đang mong `[]`.

## 4. Quyết định phạm vi

### 4.1. Direct implementation

1. Khôi phục/đặt `ResultPaginationDTO` trong common response package và dùng làm contract duy nhất.
2. Migrate mọi endpoint đang phân trang: Student v1/v2, ScoreGrid, Retake, Calculation Task,
   Score Audit, Score Change Request và Attendance history/summary.
3. Bổ sung filter ScoreGrid theo `FR-V3-QUERY-002`.
4. Chuyển Teacher management list từ `List` sang query phân trang bằng contract chung.
5. Migrate đồng thời FE consumer/fixture; không để BE và FE lệch contract giữa chừng.

### 4.2. Compatibility alignment

- Giữ URL, query parameter, filter semantics, sort và authorization của endpoint đang
  phân trang; chỉ chuẩn hóa response nếu Plan 077 không bổ sung behavior.
- Đây là breaking change của response body, nên BE DTO/controller và FE service/type/view
  của từng consumer phải đổi atomically trong cùng plan.
- Không duy trì song song nhiều page envelope sau migration.

### 4.3. Audited and deferred

- Academic Year, Grade, Class, Subject, Semester và các Enrollment endpoint hiện trả `List`.
- Các API này dùng cả cho management grid và lookup. Migration sau phải tách query endpoint
  khỏi lookup endpoint hoặc bổ sung opt-in paging; không đổi `List` thành page response ngầm.
- Dev Note Plan 077 phải ghi migration table, không claim các màn hình deferred đã done.

## 5. API contract đề xuất

### 5.1. Common convention

| Field | Rule |
|---|---|
| `page` | Zero-based, default `0`; âm trả validation error |
| `pageSize` | Default `20`; min `1`; max đề xuất `100` |
| `search` | Optional; trim; blank normalize thành absent |
| typed filters | Query param riêng theo endpoint, không arbitrary JSON/map |
| `sortField` | Allow-list theo resource |
| `sortDirection` | `asc`/`desc`; invalid không silently fallback |
| tie-breaker | Luôn thêm ID ổn định sau sort chính |

`pageSize=100` là đề xuất cần approve; nếu chọn giới hạn khác phải cập nhật plan trước code.

Canonical response dùng đúng shape `ResultPaginationDTO`:

```json
{
  "meta": {
    "page": 0,
    "pageSize": 20,
    "totalPages": 3,
    "totalItems": 41
  },
  "result": []
}
```

- `page` thống nhất zero-based với request/Spring `PageRequest`; không dùng cách cộng `1`
  của boilerplate cũ vì sẽ làm lệch các consumer hiện tại.
- `result` chứa list DTO của capability; `meta.totalItems` và `result` dùng cùng predicate
  và authorization scope.
- Không trả `appliedFilters`; request typed đã apply là nguồn state của FE.
- Page vượt phạm vi trả `200`, `result=[]`, giữ metadata thực; không tự nhảy page.
- Không tạo thêm `V3PageResponse`, `ResTeacherPageDTO`, `ResScoreGridPageDTO` hoặc page DTO
  domain-specific mới.

### 5.2. Teacher query

```text
GET /api/v3/teachers
  ?page=0&pageSize=20&search=...
  &status=ACTIVE&department=...
  &sortField=teacherCode&sortDirection=asc
```

- Search trên `teacherCode`, `teacherName`, email, phone, department theo rule normalize
  case-insensitive được test.
- Sort allow-list đề xuất: `teacherCode`, `teacherName`, `department`, `status`; tie-breaker `id`.
- Giữ authorization v2; không mở thêm quyền mutation/profile.

### 5.3. Score-grid query

```text
GET /api/v3/scorebooks/{scorebookId}/score-entries
  ?page=0&pageSize=20&search=...
  &studentStatus=ACTIVE
  &assessmentColumnId=35
  &scoreState=SCORED|MISSING|ABSENT|EXEMPTED|CANCELLED
  &minScore=0&maxScore=10
  &sortField=studentCode&sortDirection=asc
```

- `search`: mã hoặc tên học sinh.
- `assessmentColumnId` phải thuộc scorebook; sai context không silently ignore.
- `MISSING` không bao gồm điểm `0`.
- `minScore/maxScore` chỉ áp dụng cho `SCORED`; nhận `0`; min > max là lỗi validation.
- Sort allow-list đề xuất: `studentCode`, `studentName`; score sort chỉ thêm khi đã chốt
  cột đích qua `assessmentColumnId`.
- Reuse `ScorebookGuard`; actor ngoài scope không được nhận `meta.totalItems`.

## 6. Thiết kế backend dự kiến

### Shared contract

- Tạo/khôi phục `ResultPaginationDTO` trong package common của codebase hiện tại, không dùng
  package mẫu `com.uit...` trong boilerplate.
- Giữ shape `meta + result`; cân nhắc generic nội bộ chỉ khi JSON contract không đổi.
- Xóa `V3PageResponse` và chuyển/xóa `V3ContractDtoTest` tương ứng trong cùng implementation.
- Tạo typed request riêng cho Teacher và ScoreGrid; không dùng generic filter map.
- Shared page bounds/sort direction chỉ tách khi có hai consumer thật.

### Teacher slice

- Controller v3 mỏng nhận validated query.
- Query service/method riêng; Specification và sort resolver allow-list.
- Repository hỗ trợ `JpaSpecificationExecutor<Teacher>` nếu chưa có.
- Mapper trả `ResultPaginationDTO` với `result` là danh sách `ResTeacherDTO`.

### Score-grid slice

- Controller v3 gọi query service mới; giữ endpoint v2 nguyên trạng.
- Roster repository/specification search/filter trước `PageRequest`.
- Query score state/value tránh N+1 và duplicate roster row khi join score columns.
- Load columns/scores cho roster IDs của page, trừ filter score cần join/subquery.
- Reuse `ScoreGridLoader`, `ScorebookGuard` và mapping hiện có; không nhân đôi rule điểm.

## 7. Phạm vi mã nguồn dự kiến

| Khu vực | Thay đổi |
|---|---|
| common response DTO | Tạo/khôi phục `ResultPaginationDTO`; xóa `V3PageResponse` và test liên quan |
| `teacher/controller|service|repository|domain/DTOs` | Endpoint v3, Specification, sort resolver, tests |
| `scorebook/controller|service|repository|domain/DTOs` | Score-grid query v3, filter DB-side, tests |
| `student` | Migrate v1/v2 page response, giữ query behavior |
| retake/calculation/audit/change request | Migrate page response sang contract chung |
| `attendance` | Migrate history/summary page response, giữ aggregation behavior |
| `academic`, `enrollment` list-only | Chưa ép phân trang; khi migrate phải dùng contract chung |

Không tạo Flyway migration nếu chưa có evidence thiếu index. Nếu cần index, phải bổ sung
data-model decision và migration riêng trước implementation.

## 8. Unit-test plan backend

### Shared contract

- `ResultPaginationDTOTest`: map đúng `meta.page/pageSize/totalPages/totalItems` và `result`.
- Empty result, page ngoài phạm vi, total bằng 0 và zero-based page.
- Regression serialization bảo đảm JSON đúng `meta + result`, không xuất hiện
  `items/total/appliedFilters`.
- Test xác nhận không còn reference production/test tới `V3PageResponse` sau migration.
- Query normalization: trim, blank -> absent, defaults/max bound, invalid sort field/direction.

### Teacher query

- Search từng field; case-insensitive; status/department riêng và kết hợp.
- Filter trước pagination; `meta.totalItems` đúng với nhiều page.
- Sort từng allow-list field, asc/desc và ID tie-breaker.
- Empty, page ngoài phạm vi, pageSize 1/max, blank search.
- Invalid enum/sort/page/pageSize; controller không gọi service khi binding/validation fail.
- Authorization regression; mutation role không đổi.
- Unit mock repository/mapper; integration fixture có nhiều status/department và hơn một page.

### Score-grid query

- Search mã/tên; filter student status.
- Filter từng score state; `MISSING` không chứa score `0`.
- `minScore=0`, `maxScore=0`, range 0–10, min > max, ngoài range.
- Assessment column thuộc/không thuộc scorebook.
- Kết hợp search + column + state + range; `meta.totalItems` và `result` chính xác.
- Sort code/name và tie-breaker; không duplicate học sinh khi join.
- Empty roster, page ngoài phạm vi, enrollment inactive theo rule hiện hành.
- 401/403/404/422, guard interaction; unauthorized query không trả count.
- Regression endpoint v2 page/size và DTO cũ.

### Existing paged endpoint migration

- Student v1/v2, Retake, Calculation Task, Score Audit, Score Change Request và Attendance:
  serialization đúng `meta + result`, giữ nguyên item DTO và query behavior.
- Regression page đầu/cuối/ngoài phạm vi, empty, totalPages và zero-based page.
- Test từng FE consumer không còn đọc `content`, `totalElements` hoặc Spring `Page` field cũ.
- Repository/service interaction giữ nguyên nếu Plan 077 không đổi filter semantics.

### Lệnh và coverage

- Chạy focused tests trước, sau đó backend `test`, JaCoCo, Checkstyle, PMD và build theo
  workflow/skill validation tại thời điểm implementation.
- Đọc JaCoCo theo class/method thay đổi; không tự đặt threshold mới.
- Ghi `PASS`, `FAIL`, `BLOCKED`, `NOT RUN`; không suy browser/live từ JUnit.

## 9. Integration, performance và security

- Fixture BE/FE giống nhau; dataset vượt hai page và có `0`, missing, absent, exempted, cancelled.
- Kiểm tra query count/N+1 và execution plan khi cần; không claim performance nếu chưa đo.
- Kiểm tra sort injection, oversized pageSize, wildcard-heavy search và enum invalid.
- Verify role matrix với Admin/Academic Office/Teacher/Student theo scope hiện hành.
- Browser/live API chỉ `PASS` khi chạy backend thật với dữ liệu resettable.

## 10. Acceptance criteria

- Tất cả endpoint phân trang dùng `ResultPaginationDTO`; không còn `V3PageResponse`,
  `ResStudentPageDTO`, `ResStudentV2PageDTO` hoặc Spring `Page` serialization làm HTTP response.
- Teacher search/filter/sort/page chạy database-side.
- ScoreGrid hỗ trợ filter đã duyệt và phân biệt đúng `0`/missing.
- `meta.totalItems`, `meta.totalPages` và `result` thống nhất, không lộ dữ liệu ngoài scope.
- URL/query/item DTO và lookup consumer giữ nguyên; FE được migrate đồng thời với breaking
  change của page envelope.
- Tests đủ success/error/boundary/regression; validation state trung thực.
- Dev Note liệt kê endpoint migrated và các nhóm deferred.

## 11. Out-of-scope

- Import Plan 078, mutation score, calculation/transcript semantics.
- Full-text/fuzzy search, saved filter, export, analytics, distributed cache.
- Ép các API Academic Catalog/Enrollment đang trả `List` phải phân trang ngay; khi được
  chuyển thành paged query, chúng bắt buộc dùng `ResultPaginationDTO`.
- Thay đổi role matrix/navigation/lifecycle.
- Schema/index chưa có evidence và data decision.

## 12. Trình tự sau approval

1. Chốt endpoint path, max pageSize, score filter semantics và error status.
2. Chốt fixture JSON chung Teacher/ScoreGrid.
3. Implement/test shared primitive và Teacher vertical slice.
4. Implement/test ScoreGrid DB-side, giữ regression v2.
5. Nối FE, chạy contract/integration và validation.
6. Chạy browser/live nếu có môi trường.
7. Tạo Dev Note BE/FE, cập nhật summary và ghi rõ deferred scope.

## 13. Ước lượng quy mô và thời gian

### 13.1. Dữ liệu audit dùng để estimate

- Khoảng `22` file production BE có page DTO, `PageRequest`, `Page<T>` hoặc page mapper.
- Khoảng `46` file FE có page type/service/view/component/composable liên quan.
- Ít nhất `30` unit/component/story/integration fixture trực tiếp tham chiếu page contract.
- Hai phần không chỉ là mechanical migration: Teacher cần query mới; ScoreGrid cần filter
  database-side và kiểm soát join/N+1/duplicate.

Đây là plan cỡ `L` đến `XL`, không phải đổi tên DTO đơn thuần.

### 13.2. Agent-effort theo workstream

| Workstream | Nội dung | Estimate Luna High |
|---|---|---:|
| Contract/inventory | Chốt shape, zero-based semantics, endpoint-consumer matrix, shared fixture | 3–5 giờ |
| Shared BE migration | `ResultPaginationDTO`, bỏ page DTO khác, mapper/controller regression | 6–10 giờ |
| Existing BE endpoints | Student, Retake, Calculation, Audit, Change Request, Attendance | 8–14 giờ |
| Teacher vertical slice | Specification, sort, paging, controller/service/tests | 5–8 giờ |
| ScoreGrid vertical slice | Filter/query/join, authorization, performance và tests | 10–16 giờ |
| Existing FE consumers | Types/services/views/stories chuyển sang `meta + result` | 8–14 giờ |
| Teacher FE | Filter bar, URL state, paginator, states/tests/stories | 5–8 giờ |
| ScoreGrid FE | Filter UI, URL/query state, mutation re-query, Plan 078 regression | 7–12 giờ |
| Integration/validation/docs | Contract integration, full gates, browser/live, Dev Notes | 6–10 giờ |

Tổng effort tuần tự dự kiến: `58–97 agent-hours`. Khoảng trên bao gồm debug hợp lý nhưng
không bao gồm xử lý một baseline failure lớn ngoài phạm vi hoặc thiết kế lại schema.

### 13.3. Thời gian theo số subagent Luna High

| Cách tổ chức | Wall-clock ước lượng | Điều kiện |
|---|---:|---|
| 1 agent | 7–12 ngày làm việc | Làm tuần tự, gồm review và validation |
| 2 subagent | 4–7 ngày làm việc | Tách BE và FE; vẫn chờ shared contract/integration |
| 4 subagent | 2.5–4 ngày làm việc | Chia contract+migration, BE query, FE migration, FE UI/test |
| 5–6 subagent | 2–3.5 ngày làm việc | Lợi ích tăng ít vì conflict file và integration bottleneck |

Estimate khuyến nghị: `4 Luna High subagent`, cộng một agent điều phối/review. Không nên
chia nhiều agent cùng sửa shared DTO, `scorebookApi.ts`, scorebook types hoặc summary files.

### 13.4. Phân công khuyến nghị

1. Agent A — owner contract và migration matrix; tạo `ResultPaginationDTO`, fixture và
   chuẩn hóa existing BE page responses.
2. Agent B — Teacher BE vertical slice và backend tests.
3. Agent C — ScoreGrid BE query/filter, repository strategy, performance/security tests.
4. Agent D — FE page-contract migration cho existing consumers.
5. Sau contract checkpoint, Agent B/D xử lý Teacher FE; Agent C/D xử lý ScoreGrid FE theo
   ownership file tuần tự để tránh ghi đè.
6. Agent điều phối chạy integration/full validation, review diff và tạo Dev Notes.

### 13.5. Critical path và buffer

```text
ResultPaginationDTO contract
        ↓
BE response migration ──┬── FE consumer migration
                        ├── Teacher BE/FE
                        └── ScoreGrid BE/FE
                                   ↓
                      integration + full validation + browser
```

- Critical path dự kiến là ScoreGrid query/filter và integration FE sau mutation/import.
- Dành buffer `20–30%` cho response-shape fallout, fixture cũ, PMD baseline và N+1/query issue.
- Nếu browser/live environment không sẵn sàng, code/static gates có thể hoàn thành nhưng
  browser/live vẫn phải ghi `NOT RUN` hoặc `BLOCKED`.
