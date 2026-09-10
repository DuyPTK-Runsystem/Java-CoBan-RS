# Plan 077 FE — Search/filter foundation cho danh sách và bảng điểm

## 0. Trạng thái và approval gate

- Application-document version: `v3`.
- Status: `TEMPORARILY POSTPONED — chưa triển khai; giữ plan để tiếp tục sau`.
- Ngày lập/cập nhật: `2026-09-10`.
- Plan 077 được review sau Plan 078; không sửa flow file import đã triển khai.
- Plan liên kết: [Plan 077 BE](../../../be/query/077-search-filter-foundation-2026-09-10.md).
- Quyết định tạm hoãn: người dùng xác nhận qua agent ngày `2026-09-10`.
- Không triển khai FE migration/UI/test cho đến khi người dùng yêu cầu tiếp tục Plan 077.

## 1. Mục tiêu

Chuẩn hóa search/filter/sort/pagination cho hai vertical slice trực tiếp:

1. Danh sách giáo viên và bảng điểm/ScoreGrid.
2. Migration mọi FE consumer đang dùng page response khác sang `ResultPaginationDTO<T>`.

FE giữ query state có type, gửi điều kiện tới backend, hiển thị đúng loading/empty/error/page
và không lọc một page đã tải để giả lập toàn bộ dữ liệu.

## 2. Nguồn và dependency boundary

- Application-document v3, `FR-V3-QUERY-001..002`, `BR-V3-QUERY-001`.
- `frontend-api/README.md` và Plan 073 typed v3 contract.
- Plan 077 BE là nguồn endpoint/DTO/error cuối cùng sau approval.
- `apiClient`: `401` xóa session/redirect; `403` giữ session.
- Dependency: `view -> component/composable + service -> backend API`.
- FE không suy quyền, invent filter/endpoint hoặc tự tính `total`.

## 3. Hiện trạng frontend và tác động

| Tính năng | Hiện trạng | Tác động Plan 077 |
|---|---|---|
| Student list | Đã gửi filter/page/sort backend nhưng dùng page DTO riêng | Migrate response/type, giữ UI behavior |
| Teacher list | Tải `List`; `TeacherListView` lọc text/status bằng computed | Chuyển sang v3 paged query, bỏ local authoritative filtering |
| Academic Year | Tải toàn bộ, tìm mã/lọc trạng thái local | Audit/defer; giữ lookup contract |
| School Class | Tải theo năm học, lọc text/khối/trạng thái local | Audit/defer vì nhiều form dùng API làm lookup |
| Subject | Tải list/status, lọc text/type/scope/status local | Audit/defer vì dùng làm lookup/config |
| Grade/Semester | Chủ yếu lookup trả mảng | Không đổi direct slice |
| Enrollment | Lọc class/target trên context đã tải | Không đổi để tránh ảnh hưởng xếp/chuyển lớp |
| ScoreGrid | Service chỉ gửi page/size | Direct slice bắt buộc |
| Retake/Calculation/Audit/Score Change | Đã gửi filter/page backend, response chưa thống nhất | Migrate service/type/view sang `meta + result` |
| Attendance | Query/page riêng, contract chưa đồng nhất | Migrate paged consumer, giữ nghiệp vụ |

Plan 077 đổi behavior query của Teacher/Scorebook và đổi transport/type của mọi màn hình
đang dùng pagination. Các màn hình list-only deferred chưa được claim hoàn tất.

## 4. Contract FE đề xuất

```ts
type SortDirection = 'asc' | 'desc'

interface V3PageQuery<TFilters, TSortField extends string> {
  page: number
  pageSize: number
  search?: string
  filters: TFilters
  sortField: TSortField
  sortDirection: SortDirection
}

interface ResultPaginationDTO<T> {
  meta: {
    page: number
    pageSize: number
    totalPages: number
    totalItems: number
  }
  result: T[]
}
```

- `ResultPaginationDTO<T>` là page response duy nhất; không tạo `V3PageResponse` hoặc page
  response riêng cho Teacher/ScoreGrid.
- Teacher/ScoreGrid vẫn có request filter type riêng.
- URL serializer dùng allow-list và bỏ blank/default không cần thiết.
- Runtime mapping không che schema mismatch bằng cast/default tùy ý; `result` phải là array
  và `meta` phải có đủ bốn field.
- Naming cuối cùng phải khớp BE contract sau approval.

## 5. Phạm vi trực tiếp

### 5.1. Teacher list

- Search mã, tên, email, điện thoại, bộ môn theo BE contract.
- Filter trạng thái/bộ môn; sort; paginator server-side.
- Đổi search/filter/sort reset `page=0`.
- URL giữ allow-listed state để refresh/back-forward tái tạo danh sách.
- Dùng submit hoặc debounce được test, không gọi uncontrolled theo từng ký tự.
- Sau create/update/delete, reload query hiện tại; xóa item cuối page thì re-query page hợp lệ.
- Giữ nguyên mutation capability và dialog hiện hành.

### 5.2. ScoreGrid

- Filter học sinh, trạng thái học sinh, cột điểm, trạng thái điểm và khoảng điểm theo contract.
- `0` hiển thị là điểm; missing có representation riêng.
- Filter cột không được âm thầm đổi target của nhập/import.
- Pagination/sort gửi backend; không lọc rows của page hiện tại.
- Sau single/bulk/import mutation, re-query với filter hiện tại; row không còn match thì
  biến mất theo response server.
- `409` giữ input/preview phù hợp và yêu cầu tải lại; không retry mutation tự động.

## 6. State ownership và luồng

```text
route query/default
        ↓
route-level view sở hữu typed query state
        ↓
typed service serialize request
        ↓
backend response + request identity
        ↓
table/paginator hoặc empty/error state
```

- View sở hữu query, loading/error, request sequence/abort và reload.
- Filter component emit typed draft/applied values; không gọi HTTP.
- Table chỉ render `result`; paginator dùng `meta.totalItems`/`meta.totalPages` backend.
- Chỉ response mới nhất được commit; response cũ bị bỏ qua/abort.
- URL invalid normalize về default và replace, không tạo navigation loop.

## 7. Component và file dự kiến

| Khu vực | Dự kiến |
|---|---|
| shared pagination types | Dùng `ResultPaginationDTO<T>`; loại bỏ FE reference tới v3 page response |
| `services/teacherApi.ts` | Thêm v3 paged query; giữ v2 lookup nếu còn consumer |
| `types/teacher.ts` | `TeacherQuery`, sort/filter, page response |
| `views/teacher/TeacherListView.vue` | Server query, URL state, reload/pagination |
| teacher components | Filter bar/table/paginator props-emits nếu cần |
| `services/scorebookApi.ts` | ScoreGrid v3 query; giữ v2/import Plan 078 |
| `types/scorebook.ts` | ScoreGrid query/filter/page types |
| `views/scorebook/ScorebookWorkspaceView.vue` | Query ownership/delegation vào composable hiện có |
| scorebook components | Filter bar, applied summary, empty state |
| specs/stories | Deterministic service/component/view states |
| Student/Retake/Calculation/Audit/Change/Attendance | Migrate consumer sang `meta + result`, regression UI |

Không tạo global query store. Chỉ tách composable nếu Teacher và ScoreGrid có cùng lifecycle
thật; domain filter mapping vẫn ở service/type tương ứng.

## 8. UI states

| State | Hành vi |
|---|---|
| Initial loading | Loading rõ ràng, không hiện empty giả |
| Query refresh | Giữ context phù hợp, tránh request chồng |
| Empty toàn bộ | Chưa có dữ liệu |
| Empty sau filter | Không có kết quả; có nút xóa bộ lọc |
| Invalid URL | Normalize default; không loop |
| `401` | Shared session clear/redirect |
| `403` | Giữ session, báo không đủ quyền |
| `404` | Context scorebook/cột không còn tồn tại |
| `409` | Giữ input liên quan, yêu cầu tải lại |
| Network/server error | Có retry, không thay bằng empty |
| Stale response | Bỏ qua, không ghi đè query mới |

Copy tiếng Việt tự nhiên; không lộ contract/backend/Session/exception/worker/task.

## 9. Storybook plan

### Shared/query components

- `Default`, `WithAppliedFilters`, `LongLabels`, `NarrowViewport`.

### Teacher list

- `Loading`, `PopulatedFirstPage`, `FilteredResults`, `EmptyAll`, `EmptyAfterFilter`.
- `Forbidden403`, `NetworkError`, `LastPage`, `NarrowViewport`.

### ScoreGrid

- `Loading`, `PopulatedWithZeroAndMissing`, `FilterByStudent`, `FilterByScoreState`.
- `FilterByRangeIncludingZero`, `EmptyAfterFilter`, `Forbidden403`, `NotFound404`.
- `ConflictAfterMutation`, `NetworkError`, `NarrowViewport`.

Storybook dùng fixture deterministic, không gọi backend thật và không là live evidence.

## 10. Test plan frontend

### Service tests

- Serialize defaults, trim search, typed filters, sort, page/pageSize.
- Không gửi blank; giữ số `0` cho min/max.
- Encode Unicode; sort field chỉ từ allow-list type.
- Map đúng `meta + result`; surface schema/error thay vì tạo success giả.
- 401/403/404/409 đi qua `apiClient` behavior.

### Teacher tests

- Initial load và render `meta.totalItems` backend.
- Search/filter/sort reset page, gọi đúng một applied query.
- Paginator không slice/filter local.
- URL refresh/back-forward khôi phục state.
- Stale response không ghi đè request mới.
- Mutation success reload đúng query; xóa item cuối page xử lý page hợp lệ.
- Empty toàn bộ khác empty sau filter; 403 giữ session.

### ScoreGrid tests

- Gửi đúng scorebookId và filters.
- `minScore=0/maxScore=0` không bị bỏ vì falsy.
- `MISSING` và score `0` render khác nhau.
- Filter assessment column không đổi mutation/import target ngầm.
- Sau save/import re-query backend, không patch local trái response.
- 404/409/network giữ context và không hiện success giả.
- Regression Plan 078: template/preview/bulk-save tiếp tục hoạt động.

### Existing paged consumer regression

- Student, Retake, Calculation Task, Score Audit, Score Change Request và Attendance đọc
  đúng `result` và bốn field trong `meta`.
- Không còn type/service/view test dựa vào `content`, `size`, `totalElements` hoặc Spring
  `Page` HTTP shape.
- Query URL, filter behavior, role state và item rendering của từng màn hình không đổi.

## 11. Validation dự kiến

- Chạy scripts thực tế trong `FE/package.json`: lint, unit/component, coverage, build,
  Storybook build.
- `git diff --check` và visual review desktop/narrow viewport.
- Integration backend thật cho Teacher/ScoreGrid và role fixtures.
- Browser walkthrough: filter, page, refresh URL, back/forward, mutation rồi re-query.
- Gate chưa chạy ghi `NOT RUN`/`BLOCKED`; không suy PASS từ Vitest/jsdom/build.

## 12. Acceptance criteria

- TeacherList không còn local filtering làm nguồn kết quả authoritative.
- ScoreGrid filter/search/sort/page do backend xử lý.
- URL chỉ có field hợp lệ; đổi filter reset page; back/forward hoạt động.
- Paginator dùng `meta.totalItems`/`meta.totalPages`; empty/error/loading tách biệt.
- `0` và missing đúng semantics.
- Stale request, 401/403/404/409 và reload sau mutation được test.
- Plan 078 import không regression.
- Academic/Enrollment list-only deferred được ghi rõ; Attendance paged consumer đã migrate.

## 13. Out-of-scope

- Đổi route/navigation/role matrix.
- Global query store hoặc refactor toàn bộ FE/src.
- Ép Academic Year/Class/Subject/Enrollment list-only API phải phân trang ngay.
- Import điểm, calculation, transcript semantics.
- Saved filter, export, full-text/fuzzy search, analytics.

## 14. Trình tự sau approval

1. Chốt contract/fixture với BE.
2. Viết service/type tests cho Teacher và ScoreGrid.
3. Implement Teacher slice và Storybook.
4. Implement ScoreGrid filter/re-query, giữ regression Plan 078.
5. Chạy FE gates và integration.
6. Chạy visual/browser/live nếu môi trường sẵn sàng.
7. Tạo Dev Note FE và cập nhật summary theo từng gate.
