# Developer Plan 095: v5 Foundation & Contract Freeze

## 1. Trạng thái và phiên bản áp dụng

- Ngày lập: `2026-10-08`.
- Status: `COMPLETE — inventory/documentation`; contract freeze and implementation approval are **not complete/granted**.
- Application baseline: Library Management v5 (`0.1-draft`).
- Dependency: `CR-V5-001-library-management` và bộ tài liệu `document/application-doc/v5/`.
- Plan 095 chỉ lập inventory, traceability, contract freeze và ghi nhận decision log. Chưa cho phép triển khai feature, migration, API hoặc UI.
- Kết quả hiện tại được phân biệt `INVENTORY_COMPLETE` với `FROZEN_FOR_SLICE`; artifact này hoàn tất inventory tài liệu/source nhưng chưa mở implementation slice khi gate còn OPEN/BLOCKED.
- API Library mới theo baseline `/api/v2`; document version `v5` không đồng nghĩa REST API `/api/v5`.

## 2. Mục tiêu

Tạo một baseline có thể review trước khi bắt đầu implementation Library:

1. Xác minh target branch hiện tại, các API/version, role/security, migration head, batch, audit, notification, AI và FE conventions liên quan.
2. Lập ma trận truy vết requirement v5 tới data ownership/schema, API, authorization, error, migration/concurrency, FE consumer và validation.
3. Đóng băng compatibility boundary: Library tích hợp vào application hiện tại; không làm thay đổi ngầm contract school-management đang chạy.
4. Ghi rõ quyết định đã được tài liệu v5 chọn, các điểm còn TBD, owner, ảnh hưởng và gate cần đóng trước từng implementation slice.
5. Đề xuất thứ tự các plan triển khai sau Foundation mà không gộp toàn bộ Library vào một lần thay đổi.

## 3. Source order và tài liệu đầu vào

Áp dụng source order trong `document/application-doc/v5/README.md`:

1. `ApplicationContext.md`.
2. `RequirementBaseline.md`.
3. `change-request/CR-V5-001-library-management.md`.
4. Module, data-model và contract liên quan.
5. Plan 095.
6. Code/branch hiện tại để xác minh compatibility, không dùng code để tự thay requirement đã chốt.

Các tài liệu cần đối chiếu tối thiểu:

- `document/application-doc/v5/README.md`.
- `ApplicationContext.md`, `RequirementBaseline.md` và `CR-V5-001-library-management.md`.
- `data-model/README.md`, `data-model/MigrationAndConcurrency.md`.
- `contract/Authorization.md`, `contract/ErrorContract.md`, `contract/IntegrationBoundaries.md`.
- `frontend-api/README.md`.
- `modules/01-LibraryCatalog.md` tới `modules/06-LibraryAI.md`.
- `requirement-adaptation/standalone-library-to-school-platform.md`.

## 4. Freeze baseline

### 4.1 Quyết định đã có trong v5

| Area                 | Baseline được giữ                                                                      | Freeze rule                                                                                         |
| -------------------- | -------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| Application boundary | Bounded context trong Spring Boot/Vue app hiện hữu                                     | Không tạo app, database, login hoặc deployment riêng cho Library.                                   |
| Identity             | `app_user` là identity; patron chỉ lưu state Library                                   | Không tạo `MEMBER`, không duplicate credential/profile canonical, không thay JWT.                   |
| Role                 | Thêm `LIBRARIAN`; role platform hiện hữu tiếp tục nguyên nghĩa                         | Không cấp role tự động; không suy rộng quyền `ACADEMIC_OFFICE` sang Librarian.                      |
| Borrower state       | `ACTIVE`, `BORROWING_SUSPENDED`, `CLOSED` thuộc Library                                | Fine không khóa/disable `app_user`; backend kiểm eligibility cho borrower operations.               |
| Persistence          | MySQL + Flyway hiện hữu                                                                | Không mang PostgreSQL-only DDL; phải scan migration head và MySQL version trước khi viết migration. |
| REST                 | Library routes mặc định `/api/v2`; namespace `/api/v2/library` khi collision/ambiguous | Không đổi/xóa `/api/v1`, `/api/v2`, `/api/v3` contract cũ; không bump API chỉ vì document v5.       |
| Security/error/audit | Reuse platform security, global error format và audit conventions                      | Ownership được resolve server-side từ authenticated `userId`; error client dựa stable `code`.       |
| Batch/integration    | Reuse Spring Batch metadata, notification/mail, audit                                  | Không tạo infrastructure thứ hai nếu inventory không chứng minh gap.                                |
| AI                   | Spring AI abstraction, provider configurable, backend validate structured output       | AI không truy cập repository/SQL unrestricted; AI failure không chặn core circulation.              |
| FE                   | Reuse Vue 3, Vite, TypeScript, PrimeVue, router, transport/auth và test conventions    | Không thêm state/network dependency chỉ để khớp source training.                                    |

### 4.2 Không được tự đóng băng bằng suy luận

- Trạng thái `TBD-V5-LIB-001..005` được đối chiếu với decision log của phiên: 001–004 đã được giải quyết cho core direction; 005 vẫn deferred/open cho AI retention. Các quyết định không tự thay đổi source baseline.
- Endpoint/path trong FE/API baseline là proposed allocation; phải scan controller hiện có trước implementation.
- Migration head, MySQL/Azure MySQL major version, role assignment workflow và hiện trạng shared infrastructure là runtime/branch facts cần xác minh lại.
- Không coi tài liệu DRAFT, model answer, code hiện thời hoặc seed demo là bằng chứng người dùng đã duyệt business policy.

### 4.3 Decision log nhận được trong phiên Plan 095

Các quyết định dưới đây là **amendment đã được user duyệt cho core slice**, có precedence so với clause v5 draft mâu thuẫn khi lập slice. Artifact giữ nguyên source baseline và ghi amendment tường minh; đồng bộ baseline/CR là cập nhật tài liệu, không phải xin duyệt lại các quyết định đã chốt. Điều này không đồng nghĩa duyệt toàn bộ RequirementBaseline v5 hoặc cho phép implementation.

- Core-first gồm Catalog, Patron/Card, Circulation, reservation, fine và các batch/notification bắt buộc của các luồng này. AI `FR-V5-LIB-AI-001/002` (MUST), SHOULD và BONUS đều hoãn sang plan/release sau; không xóa baseline hay guardrails AI.
- User reports current local DB target name `java_coban`; work has not been pushed to the existing production DB. This is user-reported target scope, not live DB evidence; exact local MySQL version and applied Flyway head remain `UNKNOWN`.
- ADMIN grant/revoke `LIBRARIAN`, có audit. Borrower là user có patron hợp lệ, ngoại trừ bất kỳ user nào mang role LIBRARIAN; rule từ chối borrower áp dụng dù có thêm role khác. Source schema/entity supports multi-role; actual assigned-role cardinality in the live DB is UNKNOWN. Suspension chặn tạo mượn, gia hạn và reservation mới; vẫn cho đọc, trả và hủy reservation.
- Reservation FIFO, allocate copy đủ điều kiện, READY + notification; pickup due = READY date + configurable duration (default 3 calendar days), expiry at start of following day `Asia/Ho_Chi_Minh`; grant LIBRARIAN blocked if WAITING/READY rows exist until ADMIN resolves; no auto-cancel.
- Fine đang active overdue hiển thị provisional; pay/waive chỉ sau khi loan RETURNED/LOST và amount đã chốt. Offline full payment: ADMIN/LIBRARIAN pay; ADMIN waive; audit actor/time/reference; không partial payment, refund hay receipt PDF. Correction/reversal workflow remains OPEN for a later plan; no refund endpoint is implied. LOST kết thúc active loan, tính overdue tới ngày lost, rồi tạo `LOST_ITEM = listPrice + 50,000 VND`; không accrual sau đó.
- User approved group 2 invariants: canonical table `library_card`; at most one active loan per copy; at most one active card per patron; maximum five active loans per patron; LOST ends the active loan. Generated nullable unique keys and patron-row locking remain candidate design directions only; exact DDL/strategy awaits target-version verification.
- Groups 3–4 contract planning is deferred, not removed: legacy-compatible error-code contract, audit fields, notification retry/idempotency, concrete API method/path/request/response, policy/version and FE contract. This deferral blocks the affected API/audit/notification/FE implementation until a later plan completes them.
- Cover URL only; QR/PNG and barcode generated on-demand; no persisted cover/card PDF. Demo uses browser decoding of uploaded image instead of live camera and preserves manual fallback; server receives decoded text only.
- ADMIN/LIBRARIAN edit audited/versioned date policy (borrow duration, renewal duration, card validity, reservation pickup/expiry); snapshot applies to new transactions. Renew is a new transaction using current version and adds configured duration to current due date; policy edits do not rewrite old due dates/accrual. Defaults: 14 days, +7 days, 12 months, 3 calendar days, timezone `Asia/Ho_Chi_Minh`; due/card dates include deadline day, overdue starts next day, reservation expires at start of following day. Max five loans, max two renewals and fine rates are not configurable here.
- Canonical table/entity name `library_card` and the active-copy/card/patron-max-five/LOST invariants are user-approved. Generated nullable unique keys and patron-row locking remain candidate technical directions, not selected DDL; exact MySQL target/version must be verified and the design proven before migration.

## 5. Inventory và traceability deliverables

Kết quả hoàn thành Plan 095 được lưu trong artifact. Inventory reflects commit/hash below; source and runtime evidence are kept separate.

1. Lập inventory hiện trạng trên target branch cho API path, security role/authority, identity mapping, migration version, MySQL compatibility target, Batch config, audit/error, notification/mail, Spring AI configuration, FE routes/services và package conventions.
2. Đánh dấu từng phát hiện là `SOURCE/DOC`, `CODE`, `TEST`, `RUNTIME/DB` hoặc `UNKNOWN`; không nâng source evidence thành runtime proof.
3. Lập ma trận `FR/BR/SEC/COMP/NFR/AC-V5-LIB-* → module/table/constraint/API/permission/error/consumer/test`.
4. Ghi collision, mismatch hoặc thiếu owner thành decision item; không sửa tài liệu baseline lặng lẽ trong inventory.
5. Lưu kết quả vào artifact `document/application-doc/v5/FoundationAndContractFreeze.md` và liên kết từ README. Artifact phải chứa branch/commit được khảo sát và ngày khảo sát.

## 6. Decision gates trước implementation

| Gate | Quyết định/điều kiện                                                                                                       | Trạng thái từ baseline                                                                 |
| ---- | -------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| F1   | Duyệt RequirementBaseline v5 và phạm vi CR-V5-001                                                                          | Pending; v5 ghi implementation approval pending.                                       |
| F2   | Chốt workflow gán/bỏ `LIBRARIAN`, actor có quyền và audit                                                                  | User đã xác nhận ADMIN-only grant/revoke + audit; ghi nhận amendment, chưa suy rộng thành approve toàn bộ v5. |
| F3   | Chốt reservation lifecycle: pickup window, expiry, WAITING → READY, notification và queue priority                         | `APPROVED`; dates/defaults and LIBRARIAN grant blocker recorded above; notification atomicity remains F9 technical plan. |
| F4   | Chốt fine payment/waive: offline payment, actor, idempotency and correction policy                                           | `APPROVED` for offline full payment, actors, audit, provisional active fine and post-RETURNED/LOST pay/waive/date cutoffs; correction/reversal policy remains OPEN for later plan. |
| F5   | Chốt storage/retention cho cover/card artifact theo nhu cầu môi trường                                                     | `APPROVED` for core: cover URL, generated QR/PNG/barcode on demand, no persisted cover/card PDF; canonical `library_card`. AI retention moves to F6. |
| F6   | Chốt retention và PII policy cho AI prompt/output metadata                                                                 | `DEFERRED`; `TBD-V5-LIB-005` remains open before later AI release.                     |
| F7   | Xác minh route collision, migration head, MySQL target version, role seed/assignment flow và platform integration hiện hữu | User reports local target `java_coban`, not production; source resource-path search and V29 confirmed; applied head/version UNKNOWN. Concrete API contract planning is deferred. |
| F8   | Chọn cơ chế database-level active-copy uniqueness and patron max-five invariant phù hợp MySQL target                       | Invariants and canonical `library_card` are `APPROVED`; exact server/version and DDL choice/proof are `BLOCKED`. Candidate nullable unique keys and patron locking are not approved as final DDL. |
| F9   | Xác định API/error/audit/notification boundary và FE consumer contract                                                     | `DEFERRED` by user for a later planning slice; legacy-compatible error code, audit, retry/idempotency, concrete request/response/routes and policy/version FE contract remain required before affected implementation. |

Mỗi gate phải ghi `APPROVED`, `DEFERRED` (kèm scope bị loại khỏi slice), `OPEN` hoặc `BLOCKED`, decision owner và ngày. `OPEN` không được hiểu là approval mặc định.

## 7. Thứ tự plan triển khai đề xuất

Sau khi Foundation & Contract Freeze được duyệt, tách implementation thành các plan có dependency rõ:

1. **Platform foundation**: role migration/assignment, authenticated identity and patron ownership, shared error/authorization/audit integration; after F1/F2/F7/F9.
2. **Catalog and copies**: book/copy schema, search, barcode, cover URL and copy uniqueness; migration/concurrency proof per F8.
3. **Patron and card**: patron lifecycle, signed QR/PNG, canonical `library_card`, active-card uniqueness and ownership matrix.
4. **Circulation, reservation and core fine**: borrow/return/renew/lost, max-five/copy concurrency, fine model/calculation/offline pay/waive, FIFO allocation/expiry; fine cannot be deferred behind circulation because return/lost create fine state. Concrete API/error/audit/notification/FE contract must be planned before affected implementation per F9.
5. **Mandatory batch and integration**: overdue job idempotency/restart and required notifications; card-expiry reminders and other SHOULD utilities may be a later slice.
6. **Frontend core**: catalog, patron/card, circulation image-upload scanner with manual fallback, reservation and fines; approved demo upload supersedes live-camera clause for core planning.
7. **Library AI later**: AI-001/002 MUST plus SHOULD/BONUS all deferred from core by user decision; retain v5 privacy/validation guardrails for the later implementation plan.

Các thứ tự có thể được điều chỉnh bằng plan riêng nếu dependency hoặc code inventory cho thấy cần thiết; không triển khai scope chưa có contract/owner chỉ để giữ thứ tự danh sách.

## 8. Scope và non-goals của Plan 095

### In scope

- Chỉ đọc để inventory branch/code/config hiện hữu.
- Tạo traceability/contract-freeze artifact cho v5.
- Ghi decision gates, compatibility rules và proposed implementation slices.
- Sửa link/index tài liệu v5 nếu cần để discover artifact.

### Out of scope

- Thêm/chỉnh entity, repository, service, controller, migration, FE, dependency, role seed hoặc runtime configuration.
- Chạy migration, cập nhật database, gọi provider AI, deploy hoặc thay đổi môi trường.
- Chốt thay mặt owner các TBD nghiệp vụ.
- Thực hiện API path renaming hoặc sửa contract legacy.
- Tạo OpenAPI/Postman contract mới nếu chưa được plan API tương ứng yêu cầu.

## 9. Validation và acceptance criteria

Plan 095 hoàn tất khi:

- [x] Foundation inventory artifact dẫn đúng nguồn v5 và gắn commit/branch khảo sát; trạng thái freeze không ghi `FROZEN_FOR_SLICE` khi gate OPEN/BLOCKED.

- [x] Requirement IDs trong scope có trace tới owner/data/API/security/error/consumer/validation hoặc được đánh dấu `OPEN/TBD`.

- [x] Compatibility boundary nêu rõ API cũ, identity, role, migration, batch, AI và FE shell không bị đổi ngầm.

- [x] Mỗi TBD/gate có trạng thái, owner/decision cần thiết, authoritative user decision vs baseline amendment, và slice bị ảnh hưởng.

- [x] Route collision, migration head và MySQL target được ghi theo evidence hoặc `UNKNOWN`; không giả định từ tài liệu cũ.

- [x] Không có implementation hoặc DB/runtime mutation trong Plan 095.

- [x] README/index v5 liên kết artifact; Dev Note and summary index record actual documentation work.

- [x] `git diff --check` sạch; đây chỉ là kiểm tra whitespace, không được báo là test/contract validation.

## 10. Rủi ro và giảm thiểu

| Rủi ro                                                      | Giảm thiểu                                                                                     |
| ----------------------------------------------------------- | ---------------------------------------------------------------------------------------------- |
| v5 draft bị hiểu thành business approval                    | Gắn trạng thái DRAFT/PENDING trên plan và artifact; giữ TBD-005 và các technical gates OPEN, đồng thời tôn trọng các quyết định đã được user duyệt trong decision log. |
| Target branch đã đổi kể từ ngày lập tài liệu                | Gắn commit/branch và rescan route/migration/runtime facts ngay trước implementation.           |
| PostgreSQL assumption lọt vào MySQL migration               | Review migration theo target MySQL major và integration test bằng MySQL-compatible engine.     |
| Role hoặc ownership check chỉ được bảo vệ ở FE              | Contract yêu cầu backend authorization và role/ownership tests.                                |
| Freeze artifact biến thành implementation spec quá mức      | Chỉ freeze baseline/decision; schema/API detail còn thiếu được giao cho plan module tương ứng. |
| AI/notification/batch integration tạo coupling ngoài ý muốn | Inventory shared infrastructure; giữ AI optional và core circulation độc lập.                  |

## 11. Output và approval

Output sau khi thực hiện Plan 095:

- `document/application-doc/v5/FoundationAndContractFreeze.md`.
- Link từ `document/application-doc/v5/README.md`.
- Decision log F1–F9, traceability matrix và danh sách implementation gates.

Approval Plan 095 chỉ cho phép hoàn tất inventory và freeze documentation như scope mục 8. Nó không tự phê duyệt implementation Library, migration, thay đổi DB/runtime hoặc đóng các TBD nghiệp vụ. Mỗi implementation slice cần approval tương ứng theo project workflow.
