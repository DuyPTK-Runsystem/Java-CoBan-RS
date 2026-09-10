# Plan 074 FE — Giao diện xếp lớp theo quy tắc

## 1. Trạng thái và mục đích bàn giao

- Application-document version: `v3`.
- Status: `APPROVED — PARTIALLY IMPLEMENTED; production integration handoff ready`.
- Ngày lập: `2026-09-10`.
- Ngày review lại: `2026-09-11`.
- Đi cùng [Plan 074 BE](../../be/enrollment/074-rule-based-class-placement-2026-09-10.md), [Dev Note hợp nhất](../../../dev-note/be/enrollment/074-rule-based-class-placement-2026-09-11.md) và [wireframe review](../../../wireframes/fe/enrollment/074-rule-based-class-placement/README.md).

Plan này là nguồn bàn giao để hoàn thiện **production route/view** cho Plan 074. Phần FE hiện có mới là contract service, types, fixture và review component; chưa có màn hình mà giáo vụ có thể mở để tạo, mô phỏng và xác nhận phiên xếp lớp.

Không lập thêm plan mới cho phần còn thiếu. Agent triển khai phải cập nhật Dev Note 074 hợp nhất sau khi hoàn thành và chỉ báo `Completed` khi toàn bộ acceptance criteria dưới đây có bằng chứng.

## 2. Quyết định nghiệp vụ đã khóa

- Người dùng được thao tác: `ADMIN`, `ACADEMIC_OFFICE`. `TEACHER` và `STUDENT` không có quyền Plan 074.
- Profile được chọn cho từng lớp trong từng phiên:
  - `ADVANCED`: lấy top-N theo điểm trong capacity;
  - `SUPPORT`: lấy nhóm điểm thấp nhất trong capacity;
  - `REGULAR`: cân bằng phần còn lại theo học lực, đồng thời bám tỷ lệ nam/nữ của khối.
- Giới tính chỉ có `MALE | FEMALE`; FE không tạo nhóm giới tính khác.
- Điểm và giới tính dùng cho thuật toán do backend snapshot. FE không tính điểm, không đọc `Student.averageScore`, không gửi giá trị giả để thay nguồn backend.
- Thiếu điểm, thiếu giới tính hoặc bằng điểm tại ngưỡng capacity là `MANUAL_REQUIRED`; không chặn xác nhận phần tự động.
- Vượt capacity của auto-placement là lỗi chặn. Capacity warning của xếp/chuyển thủ công v2 vẫn non-blocking.
- Candidate source: `CONTINUING`, `NEW_ADMISSION`, `REPEAT`. Học sinh mới vào L8 và toàn bộ học sinh của năm học đầu tiên dùng `NEW_ADMISSION` khi có căn cứ/phê duyệt.

## 3. Baseline thực tế phải giữ lại

### Đã có và phải reuse/refactor, không viết lại từ đầu

| File | Trạng thái hiện tại | Yêu cầu continuation |
|---|---|---|
| `FE/src/services/placementApi.ts` | Có create/get/update/simulate/results/confirm/cancel | Giữ HTTP trong service; bổ sung/điều chỉnh test contract |
| `FE/src/types/placement.ts` | Có DTO/request types nhưng còn lệch wire contract | Sửa theo mục 4 trước khi dựng view |
| `FE/src/components/enrollment/PlacementWorkspaceReview.vue` | Presentational review panel, chưa gắn API/route | Refactor để nhận session + page kết quả riêng và emit typed events |
| `FE/src/components/enrollment/PlacementWorkspaceReview.spec.ts` | Có focused tests nhưng một số assertion không kiểm tra đúng hành vi | Viết lại các case nêu tại mục 11 |
| `FE/src/services/placementApi.spec.ts` | Đã kiểm tra pagination request | Mở rộng body/response/action contract |
| `FE/src/fixtures/placementFixture.ts` | Fixture review | Căn lại wire field và bổ sung fixture setup/result page |
| `FE/src/components/enrollment/PlacementWorkspaceReview.stories.ts` | Có review states cơ bản | Giữ deterministic, thêm state còn thiếu khi component thay đổi |

### Chưa có — đây là phần chính Antigravity phải triển khai

- Production route và route tests.
- Entry action từ workspace xếp lớp v2.
- View orchestration gọi API thật.
- Form tạo phiên, chọn lớp/profile và chọn candidate/source.
- Result pagination thật, dialog giải thích, mutation confirmation và error recovery.
- Browser/live walkthrough.

## 4. Contract checkpoint bắt buộc trước khi dựng UI

Agent phải sửa các lệch contract hiện tại, không giữ compatibility giả trong FE:

| Vấn đề hiện tại | Wire contract backend | Sửa bắt buộc |
|---|---|---|
| `PlacementSessionStatus` có `SIMULATING` | Backend trả `SIMULATED` | Đổi union sang `DRAFT | SIMULATED | READY_FOR_CONFIRM | CONFIRMED | CANCELLED` |
| `PlacementResult.classId` | JSON trả `targetClassId` | Đổi type, fixture, component và service test sang `targetClassId` |
| Review component đọc `session.results` làm bảng chính | `GET /{id}/results` trả `ResultPaginationDTO`, zero-based | View tải page riêng; component nhận `results` và `meta` qua props |
| Capacity-block test gắn property `issues` không tồn tại | Blocking nằm ở `PlacementResult.issueSeverity`, session blocking có status `SIMULATED` | Tạo fixture result `BLOCKING`; tìm đúng nút xác nhận thay vì dùng button đầu tiên |
| Session dùng numeric year/grade IDs | FE có academic-year/grade lookup hiện hữu | View resolve label từ `academicApi`; không hiển thị `Năm học #id`/`Khối #id` nếu lookup thành công |

Endpoint placement đã triển khai và được phép dùng:

| Method | Path | FE responsibility |
|---|---|---|
| `POST` | `/api/v3/placement-sessions` | Tạo draft |
| `GET` | `/api/v3/placement-sessions/{id}` | Tải session/lifecycle/snapshot |
| `PUT` | `/api/v3/placement-sessions/{id}` | Sửa target class/profile khi session còn `DRAFT` |
| `POST` | `/api/v3/placement-sessions/{id}/simulate` | Gửi `expectedVersion` |
| `GET` | `/api/v3/placement-sessions/{id}/results?page=&size=` | Tải kết quả phân trang zero-based |
| `POST` | `/api/v3/placement-sessions/{id}/confirm` | Gửi `expectedVersion` + idempotency key cho một confirm intent |
| `POST` | `/api/v3/placement-sessions/{id}/cancel` | Gửi `expectedVersion` |

Không có endpoint list/search placement sessions. Vì vậy continuation này **không dựng màn danh sách phiên và không tự phát minh `GET /placement-sessions`**.

## 5. Route và điểm vào production

Giữ một mục sidebar `Xếp lớp` đang trỏ tới `/v2/enrollments`; không tạo thêm navigation item trùng nghĩa.

Thêm hai child route, đều yêu cầu `ADMIN | ACADEMIC_OFFICE`:

| Route | Name | Mục đích |
|---|---|---|
| `/v2/enrollments/placement/new` | `v2-placement-new` | Cấu hình và tạo phiên mới |
| `/v2/enrollments/placement/:placementSessionId` | `v2-placement-session` | Xem draft/preview, mô phỏng, xác nhận hoặc hủy |

Trong `EnrollmentListView.vue`, thêm action rõ nghĩa `Xếp lớp tự động` dẫn tới route tạo mới. Flow xếp lớp thủ công hiện tại không bị thay đổi.

Route guard là UX boundary; backend vẫn là nguồn authorization. `401` dùng behavior chung của `apiClient`; `403` giữ phiên đăng nhập và hiển thị state không đủ quyền.

## 6. Luồng UX được triển khai

```text
/v2/enrollments
  -> Xếp lớp tự động
  -> Chọn năm học + khối
  -> Chọn lớp đích và profile từng lớp
  -> Chọn học sinh chưa xếp lớp + khai báo nguồn candidate
  -> Tạo phiên DRAFT
  -> /v2/enrollments/placement/{id}
  -> Mô phỏng
  -> Review kết quả phân trang + lý do
  -> READY_FOR_CONFIRM: xác nhận phần tự động
  -> CONFIRMED read-only
  -> MANUAL_REQUIRED: tiếp tục xử lý ở /v2/enrollments
```

### 6.1 Tạo phiên

- Tải song song `fetchAcademicYears`, `fetchGrades`.
- Khi chọn năm học: tải `fetchSchoolClasses(token, academicYearId)` và `fetchUnassignedStudents(token, academicYearId)`.
- Chỉ đưa vào lớp đích các lớp cùng `academicYearId`, `gradeLevelId` đã chọn và không có status `CLOSED`.
- Capacity chỉ hiển thị từ `SchoolClass`; không cho sửa và không dùng làm nguồn quyết định. Khi create/update, bỏ field `capacity` hoặc gửi `null` để backend snapshot lại.
- Chọn ít nhất một lớp đích và profile cho từng lớp.
- Chọn ít nhất một học sinh chưa xếp lớp. Numeric `studentId` chỉ dùng transport; UI hiển thị `studentCode` và `studentName`.
- Mỗi candidate có select `Nguồn học sinh`:
  - `CONTINUING` → `Lên lớp`;
  - `NEW_ADMISSION` → `Nhập học mới`;
  - `REPEAT` → `Học lại khối này`.
- Không tự suy nguồn candidate từ tên lớp, tuổi hoặc lịch sử đang hiển thị.
- `NEW_ADMISSION` và `REPEAT` bắt buộc nhập `Căn cứ xét xếp lớp` (`eligibilityEvidence`) và `Mã/phê duyệt tham chiếu` (`approvalReference`). `CONTINUING` gửi hai field này là `null`.
- `targetGradeId` của mọi candidate lấy từ khối đích đã chọn.
- FE gửi `score`, `scoreSourceReference`, `genderSnapshot` là `null`; backend tự lấy snapshot chính thức.
- `ruleVersion` không hiển thị thành input. Trong continuation hiện tại dùng một constant duy nhất `PLACEMENT_RULE_VERSION = '074-v1'`, cùng giá trị fixture đã duyệt; không cho người dùng sửa và không rải literal qua component.
- Sau create thành công, `router.replace` tới route session bằng `response.id`.

### 6.2 Draft và mô phỏng

- `DRAFT`: cho đổi profile lớp và lưu bằng `PUT` với `expectedVersion` hiện tại.
- Backend hiện chỉ cho update ở `DRAFT`. Sau lần simulate đầu tiên, không hiển thị control sửa profile như còn lưu được.
- `SIMULATED`: có blocking result; hiển thị cảnh báo và khóa confirm.
- `READY_FOR_CONFIRM`: không có lỗi chặn; `MANUAL_REQUIRED` vẫn được hiển thị nhưng không khóa confirm.
- `CONFIRMED`, `CANCELLED`: read-only.
- Mọi mutation có loading riêng và chống double-submit.

### 6.3 Kết quả và giải thích

- View tải kết quả qua `fetchPlacementResults`; mặc định page `0`, size `20`.
- PrimeVue paginator dùng metadata backend; không sort/filter local như thể current page là toàn bộ dữ liệu.
- Khi session hoặc simulation thay đổi, quay về page `0` và reload results.
- Result row hiển thị mã/tên học sinh, lớp đề xuất, điểm snapshot nếu có, trạng thái và lý do.
- Do placement result chỉ trả `studentId`, view hydrate **các học sinh của page hiện tại** bằng `getStudent`, cache theo `studentId`; không tải toàn bộ danh sách và không gọi API trong presentational component.
- `targetClassId` resolve qua `session.targetClasses`; không hiển thị `Lớp #id` khi metadata lớp đã có.
- Dialog `Xem lý do` hiển thị `explanation` backend, trạng thái, score và issue. Không tự viết lại lý do thuật toán.
- Nếu hydrate tên học sinh lỗi nhưng result tải được, vẫn hiển thị `Học sinh #id` và không làm mất kết quả.

### 6.4 Confirm, cancel và quay lại flow thủ công

- Confirm chỉ khả dụng khi `session.status === 'READY_FOR_CONFIRM'` và không có mutation đang chạy. Không suy blocking từ riêng page đang xem.
- Trước confirm, dùng `ConfirmDialog` mô tả rõ chỉ các kết quả `AUTO_ASSIGNED` được ghi; học sinh `MANUAL_REQUIRED` vẫn chưa có lớp.
- Tạo một `crypto.randomUUID()` cho **mỗi confirm intent**, giữ nguyên key khi cùng intent bị submit lại; tạo key mới khi người dùng mở một confirm intent mới. Không đưa key vào URL/log.
- Sau confirm thành công, reload session + page result và chuyển UI read-only.
- Cancel chỉ khả dụng ở `DRAFT | SIMULATED | READY_FOR_CONFIRM`; cần dialog xác nhận.
- Với `MANUAL_REQUIRED`, cung cấp link `Tiếp tục xếp thủ công` về `/v2/enrollments`; không tự tạo enrollment bằng FE.

## 7. State ownership và component boundary

`PlacementWorkspaceView.vue` sở hữu:

- route param và navigation;
- auth token/session;
- setup form state;
- academic year/grade/class/unassigned-student lookups;
- current placement session;
- result page/meta và student display cache;
- loading/empty/forbidden/not-found/conflict/error/mutation states;
- mọi service call và mutation orchestration.

Components chỉ nhận typed props và emit event:

- `PlacementSessionSetup.vue`: context, target classes/profile, candidates/source/evidence;
- `PlacementWorkspaceReview.vue`: lifecycle, profile summary, results page và action events;
- `PlacementResultDetailDialog.vue`: một result và display metadata;
- có thể tách `PlacementCandidateTable.vue` nếu setup component vượt phạm vi đọc hợp lý.

Không thêm Pinia/store, repository layer, domain use-case layer hoặc HTTP call trong component.

## 8. Error và recovery contract

| Trạng thái | Hành vi FE |
|---|---|
| `400`/`422` | Giữ input; hiển thị message backend an toàn. Chỉ map field khi response thật có field identity |
| `401` | Để `apiClient` xóa session và redirect login |
| `403` | Giữ session; render forbidden state, không redirect login |
| `404` | Render `Không tìm thấy phiên xếp lớp` và action quay lại workspace |
| `409` | Giữ input/review; khóa submit hiện tại; hiển thị `Tải lại dữ liệu` — không auto-retry mutation |
| network/5xx | Giữ context có thể phục hồi; cho retry read, không tự retry mutation |

Không hiển thị raw stack trace, enum kỹ thuật hay nội dung `Session/PRESENT/backend/Contract/Calculation/worker/task` trong copy người dùng.

## 9. File scope dự kiến

### Tạo mới

- `FE/src/views/enrollment/PlacementWorkspaceView.vue`
- `FE/src/views/enrollment/PlacementWorkspaceView.spec.ts`
- `FE/src/components/enrollment/PlacementSessionSetup.vue`
- `FE/src/components/enrollment/PlacementSessionSetup.spec.ts`
- `FE/src/components/enrollment/PlacementResultDetailDialog.vue`
- story/spec bổ sung khi component state cần review độc lập

### Chỉnh sửa

- `FE/src/router/index.ts`, `FE/src/router/index.spec.ts`
- `FE/src/views/shell/AuthenticatedV2ShellView.vue` chỉ khi cần active state; không thêm sidebar item mới
- `FE/src/views/enrollment/EnrollmentListView.vue`, `.spec.ts`
- `FE/src/components/enrollment/PlacementWorkspaceReview.vue`, `.spec.ts`, `.stories.ts`
- `FE/src/services/placementApi.ts`, `.spec.ts`
- `FE/src/types/placement.ts`
- `FE/src/fixtures/placementFixture.ts`
- CSS domain hiện hữu hoặc scoped CSS; không tạo design system mới
- Dev Note 074 hợp nhất và các summary sau implementation

Không sửa backend trong task FE này. Nếu wire contract thực tế khác controller/DTO hiện tại, dừng và báo contract gap thay vì tự tạo endpoint/field.

## 10. Các điều không được làm

- Không dựng màn danh sách phiên khi chưa có list endpoint.
- Không dùng fixture làm production data.
- Không tính score/ranking/gender ratio/capacity eligibility ở browser.
- Không tin `Student.averageScore` là điểm chính thức.
- Không cho FE gọi bulk/manual enrollment để giả lập confirm.
- Không coi ẩn route/button là authorization.
- Không hard-code tên học sinh/lớp/năm học từ wireframe.
- Không sửa semantics capacity warning của flow v2.
- Không đánh dấu hoàn tất nếu mới có Storybook/jsdom mà chưa có production route/view.

## 11. Test plan bắt buộc

### Types/service contract

- Wire status dùng `SIMULATED`, không còn `SIMULATING`.
- Result parse `targetClassId` đúng backend.
- Create payload loại bỏ client score/gender snapshot, dùng target grade và source/evidence đúng từng candidate.
- Update/simulate/cancel gửi đúng `expectedVersion`.
- Confirm gửi idempotency key; double-click không tạo intent thứ hai.
- Results serialize `page`/`size` zero-based và đọc `ResultPaginationDTO`.

### Components

- Setup validation: thiếu scope/lớp/profile/candidate; NEW_ADMISSION/REPEAT thiếu evidence hoặc approval.
- `MANUAL_REQUIRED` không khóa confirm khi session `READY_FOR_CONFIRM`.
- Blocking result/session `SIMULATED` khóa confirm.
- Test tìm nút bằng role/accessible name hoặc test id; không assertion vào button đầu tiên.
- `CONFIRMED`/`CANCELLED` read-only; detail dialog giữ nguyên explanation backend.

### View/router

- Entry từ `/v2/enrollments` tới route create.
- Route create tải lookup và tạo payload đúng; thành công chuyển sang route session.
- Route session tải session + result page + student display metadata.
- Pagination gọi lại backend với page zero-based.
- Profile update chỉ ở `DRAFT`.
- Simulate/confirm/cancel cập nhật version/state và reload result.
- `401`, `403`, `404`, `409`, validation và network state đúng mục 8.
- Teacher/Student không được phép vào route theo UX guard; direct API vẫn do backend bảo vệ.

### Regression

- Flow xếp/chuyển thủ công `/v2/enrollments` còn hoạt động và capacity warning vẫn non-blocking.
- Student navigation chỉ còn attendance/transcript.
- Teacher navigation không lộ action placement.

## 12. Validation gate trước khi báo hoàn thành

Chạy đúng scripts trong `FE/package.json`:

```bash
npm run lint
npm run test
npm run test:coverage
npm run build
npm run build-storybook
git diff --check
```

Sau automated gates, chạy browser/live với backend thật:

1. Admin hoặc Giáo vụ mở `/v2/enrollments` và vào `Xếp lớp tự động`.
2. Tạo một phiên có ít nhất hai profile lớp và candidate đủ/thiếu dữ liệu.
3. Mô phỏng; kiểm tra auto/manual result, phân trang và dialog lý do.
4. Xác nhận khi chỉ có warning; kiểm tra `CONFIRMED` read-only.
5. Kiểm tra blocking capacity không confirm được.
6. Kiểm tra `409` bằng stale version và `403` không làm mất phiên đăng nhập.
7. Kiểm tra candidate `MANUAL_REQUIRED` vẫn được xử lý qua flow v2.

Mọi gate không chạy phải ghi `NOT RUN`; failed gate ghi `FAIL/BLOCKED`. Không gọi plan hoàn tất chỉ vì focused tests PASS.

## 13. Acceptance criteria

- Admin/Giáo vụ có thể đi từ workspace Xếp lớp tới tạo phiên, mô phỏng, review, xác nhận/hủy mà không dùng Postman.
- Production route/view dùng API thật; không dùng fixture runtime.
- FE wire types khớp `SIMULATED` và `targetClassId` của backend.
- Người dùng chọn được year/grade/classes/profile/candidates/source; NEW_ADMISSION/REPEAT có evidence/approval bắt buộc.
- FE không gửi score/gender giả và không tự chạy thuật toán placement.
- Result dùng server pagination, hiện tên/mã học sinh và lớp, có explanation dễ đọc.
- `MANUAL_REQUIRED` không chặn confirm; capacity blocking chặn confirm qua lifecycle backend.
- Confirm có expected version, idempotency intent và chống double-submit.
- `401/403/404/409` giữ đúng session/input/recovery semantics.
- Flow manual v2 không bị thay đổi.
- Router, view, component, service và regression tests đều có bằng chứng; full FE gates PASS.
- Browser/live evidence được ghi riêng; nếu chưa chạy, status vẫn là `NOT RUN`, không phải `Completed`.
