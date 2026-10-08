# Developer Plan 096: Library Catalog & Book Copy

## 1. Trạng thái, mục tiêu và dependency

- Ngày lập: `2026-10-08`.
- Application baseline: **v5**, được người dùng xác nhận trong phiên lập plan.
- Status: **COMPLETED** — Catalog source và Frontend triển khai hoàn tất; tính năng In mã vạch hàng loạt (Bulk Barcode Printing) với bố cục A4, thu nhỏ mã vạch, tự động scale size tiêu đề 2 dòng và in isolated iframe độc lập đã nghiệm thu thành công; toàn bộ FE quality gates PASS (131 test files / 722 tests, lint 0 lỗi, build production & Storybook PASS).
- Scope ban đầu: lập plan. Amendment `2026-10-08`: người dùng yêu cầu làm wireframe bằng subagent và giao diện sau implementation phải đồng nhất với giao diện hiện tại. Approval này cho phép prototype tài liệu; approval ban đầu chỉ cho wireframe; user đã duyệt implementation ở amendment section 12.
- Dependency: [Plan 095](095-v5-foundation-contract-freeze-2026-10-08.md) và [Foundation & Contract Freeze](../../application-doc/v5/FoundationAndContractFreeze.md). Inventory đã hoàn tất; chưa phải `FROZEN_FOR_SLICE`.

Kết quả triển khai mong muốn sau approval: quản lý đầu sách và bản sao vật lý trong Spring Boot/Vue application hiện hữu; người dùng đã đăng nhập tìm kiếm/xem catalog, ADMIN/LIBRARIAN quản lý metadata và bản sao. Barcode duy nhất, dữ liệu lịch sử không bị xóa dây chuyền, FE dùng contract có phân trang và error code rõ ràng.

Các phương án kỹ thuật, endpoint, giới hạn và lifecycle **đề xuất** dưới đây cần review trước code. Xác nhận v5 và yêu cầu lập plan không thay thế approval implementation. Không xin duyệt lại các amendment đã được người dùng chốt ở Plan 095.

## 2. Requirement và nguồn đối chiếu

Source order: [README v5](../../application-doc/v5/README.md) → [ApplicationContext](../../application-doc/v5/ApplicationContext.md) → [RequirementBaseline](../../application-doc/v5/RequirementBaseline.md) → [CR-V5-001](../../application-doc/v5/change-request/CR-V5-001-library-management.md) → module/data-model/contract → Plan 095/amendment → source để kiểm tra compatibility.

Tài liệu trực tiếp: [Library Catalog](../../application-doc/v5/modules/01-LibraryCatalog.md), [Barcode/QR/Scanning](../../application-doc/v5/modules/05-BarcodeQrAndScanning.md), [Data model](../../application-doc/v5/data-model/README.md), [Migration & Concurrency](../../application-doc/v5/data-model/MigrationAndConcurrency.md), [Authorization](../../application-doc/v5/contract/Authorization.md), [ErrorContract](../../application-doc/v5/contract/ErrorContract.md), [IntegrationBoundaries](../../application-doc/v5/contract/IntegrationBoundaries.md), [FE API](../../application-doc/v5/frontend-api/README.md).

| Requirement | Kết quả của slice | Bằng chứng dự kiến sau implementation |
| --- | --- | --- |
| FR-V5-LIB-CAT-001 | Metadata ISBN/title/author/publisher/publishedYear/category/listPrice/cover URL | DTO validation, CRUD/service/API tests, UI form |
| FR-V5-LIB-CAT-002 | Copy thuộc đúng một Book; barcode/shelfLocation/status/referenceOnly riêng | FK/schema tests, copy detail/list tests |
| FR-V5-LIB-CAT-003 | Search/filter/sort/pagination ở backend theo keyword/category/year/availability | Query integration tests và FE query-state tests |
| FR-V5-LIB-CAT-004 | ADMIN/LIBRARIAN thêm N copy, server sinh barcode | Atomic batch tests, uniqueness/concurrency integration tests |
| FR-V5-LIB-CAT-005 | Cover URL theo amendment đã duyệt | URL validation, UI ảnh lỗi/fallback; không tạo storage subsystem |
| BR-V5-LIB-CAT-001 | ISBN nullable, unique theo policy phải chốt | Normalization/duplicate/concurrent insertion tests |
| BR-V5-LIB-CAT-002 | Barcode duy nhất | Unique constraint và concurrent batch tests trên MySQL target |
| BR-V5-LIB-CAT-003 | Reference copy không được cho mượn | Persist/DTO đúng referenceOnly; loan eligibility test thuộc circulation slice |
| BR-V5-LIB-CAT-004 | Safe/soft delete; không cascade-delete loan history | Archive/withdraw guard tests, FK/history regression khi circulation tích hợp |
| NFR-V5-LIB-001/002/003/005/006 | Backend authority, MySQL migration stream, active-loan concurrency boundary, stable error code và FE conventions | Security/schema/error/FE tests; active-loan proof thuộc circulation gate |
| AC-V5-LIB-007 | Kết quả validation phản ánh bằng chứng | PASS/FAIL/NOT RUN/BLOCKED được ghi riêng trong Dev Note triển khai |

Core amendments kế thừa: Catalog/Patron/Circulation/reservation/fine và mandatory integration thuộc core-first; AI MUST/SHOULD/BONUS hoãn sang plan sau. Cover URL only; barcode sinh on-demand, không lưu file barcode. ADMIN grant/revoke LIBRARIAN có audit; không tự cấp role. Một active loan/copy là invariant đã duyệt, nhưng exact DDL và transaction strategy chưa được chọn.

## 3. Phạm vi

### In scope dự kiến

- Book CRUD theo safe-delete policy; copy list/detail, batch create, sửa vị trí/reference flag và withdraw theo transition được duyệt.
- Search/filter/sort/page và availability được backend tính từ copy đủ điều kiện.
- Barcode generation phía server; tra cứu bằng chuỗi barcode; ảnh barcode PNG on-demand cho copy, manual input fallback ở UI.
- Cover URL; frontend render và fallback khi ảnh không tải được, backend không tự fetch URL.
- Authorization ADMIN/LIBRARIAN cho mutation, catalog read cho authenticated platform users; mutation audit; Library error adapter tương thích legacy.
- FE catalog/list/detail/create/edit, quản lý copies, thêm N copy và xem barcode; typed services, route/menu integration và states cần thiết.
- Migration Book/BookCopy, indexes/FK/uniqueness, rollback transaction, focused tests và validation BE/FE sau approval.

### Out of scope

- Patron/library_card, role grant/revoke workflow, borrow/return/renew/lost, reservations/fines/batch/notifications và policy-date configuration: plan riêng. Role grant được kế thừa như dependency, không coi là đã triển khai.
- Gán trực tiếp ON_LOAN/RESERVED/LOST qua generic copy CRUD; các trạng thái này thuộc circulation/reservation.
- AI enrichment/recommendation; CSV import, analytics, live-camera scanner; storage/upload cover/card; PDF/label-printing mở rộng hoãn theo SHOULD/BONUS của baseline.
- Deploy, DB/cloud writes, remote push, thay school-management contract, tạo app/login/database/infrastructure riêng.

## 4. Hiện trạng được xác minh

Inventory source ngày `2026-10-08`: branch `training/duyptk/student-management`, HEAD `dec80afb482881fa537d558034946ce61ed4be91`, worktree sạch trước khi lập plan. Không fetch remote, không kiểm tra runtime.

- Chưa có Library module/controller/migration/FE routes. Source scan chưa thấy collision chính xác với `/api/v2/books` hoặc `/api/v2/book-copies`; phải re-scan target commit trước code.
- Backend feature-first; có pattern `FunctionalRoomController/Service/Repository`, `StudentV2Specifications`, `StudentV2SortResolver` để tham chiếu query, validation và sort allowlist.
- Canonical pagination mới: `common.contract.ResultPaginationDTO<T>` với `{meta:{page,pageSize,totalPages,totalItems},result:[...]}`. Không dùng envelope student v2 khác cho Library.
- `SecurityConfiguration` có authenticated boundary và method security. Role backend là code trong bảng/entity `Role`; FE `UserRole` chưa có `LIBRARIAN`.
- `GlobalExceptionHandler` dùng `RestResponse {statusCode,error,message,data}`, chưa có stable machine `code`. Đây là gap contract phải giải quyết trước API/FE implementation.
- `AcademicCatalogAuditService.writeAudit(...)`, `AuditLogRepository`, `AuditContext` là pattern audit hiện hữu; không gọi trực tiếp service academic từ Library chỉ để tái sử dụng.
- Source migration head `V29__create_timetable_agent.sql`; `V30` chỉ là số kế tiếp **dự kiến**, phải scan lại trước tạo migration.
- Plan 095 ghi DB local `java_coban` theo user; exact MySQL version, applied Flyway head, schema hiện tại và constraint/concurrency behavior **UNKNOWN**. Default connection config và file SQL không chứng minh DB đã migrate.

## 5. Thiết kế và luồng đề xuất

### 5.1 Module và ownership

Tạo bounded module `library/catalog` theo feature-first: controller mỏng → application service có transaction → repository/specifications → entity/DTO. Controller không quyết định availability hoặc role eligibility thay service. Không dùng JPA entity làm API response.

Book là catalog metadata; BookCopy là đơn vị tồn kho. Circulation sau này sở hữu thay đổi trạng thái mượn/giữ chỗ/lost và active-loan integrity; Catalog không tự dựng loan table hoặc mô phỏng loan bằng status.

### 5.2 Metadata và tìm kiếm

- Đề xuất ISBN: optional; trim, blank → null; chuẩn hóa dấu cách/dấu gạch ngang trước unique check; chấp nhận ISBN-10/13 có checksum. Chính sách này là decision C1, không coi baseline đã chọn normalization/checksum.
- Các length/nullability cụ thể, năm xuất bản hợp lệ, listPrice scale/range và category free text hay taxonomy phải chốt trong C2. Đề xuất listPrice không âm, money `BigDecimal`; không tự ép năm <= năm hiện tại khi requirement chưa chọn.
- Query server-side: keyword trên title/author/ISBN; category, publishedYear và availability; sort allowlist. Availability dùng `EXISTS` copy AVAILABLE, không referenceOnly, Book đang active; tránh join làm sai totalItems hoặc duplicate Book.
- Đề xuất query `page` zero-based, `size` mặc định 20/max 100; sort mặc định `title,asc` rồi `id,asc`, allowlist `title,author,publishedYear,listPrice,id`. Null placement và search collation được xác định cùng MySQL target, không suy đoán accent-insensitive behavior.
- Đề xuất DTO detail thêm tổng copy và availableBorrowableCopyCount; counts được query theo page/batch để tránh N+1. FE không tự suy ra tổng availability từ một page copy.

### 5.3 Copies, barcode và concurrency

Batch add: kiểm role → khóa/kiểm Book active → validate quantity và shelfLocation/referenceOnly → sinh barcode server-side → persist toàn bộ trong một transaction → audit → trả danh sách copy vừa tạo. Đề xuất quantity 1..100; limit là C3 cần duyệt. Không hỗ trợ client tự truyền barcode ở core.

Barcode theo baseline module 05: `LIB-{copyId zero-padded 9 digits}`, symbology **Code128**, định danh BookCopy. Dùng ID database cấp, không dùng `COUNT + 1`; unique index là integrity boundary. Phải chốt cách tạo non-null barcode và lấy ID trong cùng transaction (ví dụ insert với temporary unique token nội bộ rồi update sang mã canonical trước commit); không expose token tạm. ID vượt 9 digits là boundary cần contract rõ, không truncate tạo trùng. Constraint failure rollback toàn batch; nếu retry, transaction mới sau rollback với số lần hữu hạn, không tiếp tục trong transaction đã lỗi constraint. Không trả thành công một phần. Retry do network không mặc nhiên idempotent: FE khóa submit khi đang gửi; cơ chế request idempotency cho batch phải được chốt C3 để tránh nhân đôi copies.

Status baseline là AVAILABLE/ON_LOAN/RESERVED/LOST/DAMAGED/WITHDRAWN; `referenceOnly` là flag riêng, không thêm enum REFERENCE nếu dùng flag. C4 chốt transition và mapping referenceOnly; không thay enum baseline. Creation mặc định AVAILABLE; Catalog mutation chỉ xử lý metadata, AVAILABLE ↔ DAMAGED và withdraw khi đủ điều kiện. Ref/shelf edits trên active loan/reservation cần guard sau khi các module đó tồn tại. API không có arbitrary status setter.

Future circulation phải khóa copy/transaction hoặc dùng strategy đã chứng minh với MySQL để bảo đảm một active loan/copy; status check đơn thuần không đủ. Plan 096 chỉ chứng minh barcode uniqueness và catalog batch atomicity; không claim đã đạt active-loan concurrency acceptance.

### 5.4 Lifecycle và audit

Đề xuất soft archive Book và withdraw copy, không hard-delete public API. Không cascade REMOVE tới copy hoặc loan/history. Archive/withdraw có guard active loan/reservation khi module liên quan tồn tại; invariant chưa thể được chứng minh bằng catalog-only tests. Archive visibility, restore và hành vi với copy còn AVAILABLE là C4; không âm thầm biến DELETE thành policy đã duyệt.

Mutation audit trong cùng transaction: actor/time/action/entityType/entityId/before/after và request correlation nếu platform hỗ trợ; batch record chứa IDs và quantity, không ghi secrets. Audit failure rollback mutation. C5 phải xác nhận field mapping và retention theo infrastructure hiện hữu. Không tạo notification cho Catalog nếu không có requirement.

## 6. API / Database / Integration đề xuất để review

### 6.1 API allocation

Document version v5 vẫn dùng `/api/v2`. Nếu re-scan phát hiện collision, đề xuất namespace nhóm bị ảnh hưởng dưới `/api/v2/library`; phải cập nhật contract trước code.

| Method/path đề xuất | Request/query | Response/status | Quyền |
| --- | --- | --- | --- |
| GET `/api/v2/books` | keyword/category/publishedYear/availability/page/size/sort | 200; ResultPaginationDTO<BookSummary> | Authenticated |
| GET `/api/v2/books/{id}` | Book ID | 200 BookDetail; not found 404 | Authenticated |
| POST `/api/v2/books` | BookCreateRequest metadata | 201 BookDetail + Location | ADMIN/LIBRARIAN |
| PUT `/api/v2/books/{id}` | BookUpdateRequest + expectedVersion | 200 BookDetail | ADMIN/LIBRARIAN |
| DELETE `/api/v2/books/{id}` | expectedVersion theo contract C5 | 204 soft archive; conflict 409 | ADMIN/LIBRARIAN |
| GET `/api/v2/books/{id}/copies` | page/size/status/referenceOnly/sort | 200 ResultPaginationDTO<BookCopyResponse> | Authenticated; public-field policy C5 |
| POST `/api/v2/books/{id}/copies` | quantity/shelfLocation/referenceOnly, idempotency contract C3 | 201 batch response `{bookId,createdCount,copies}` | ADMIN/LIBRARIAN |
| GET `/api/v2/book-copies/{barcode}` | URL-encoded barcode string | 200 BookCopyResponse; 404 | Authenticated |
| PATCH `/api/v2/book-copies/{barcode}` | allowed metadata/transition + expectedVersion | 200 BookCopyResponse; 409 | ADMIN/LIBRARIAN |
| DELETE `/api/v2/book-copies/{barcode}` | expectedVersion theo C5 | 204 withdraw; 409 active/history guard | ADMIN/LIBRARIAN |
| GET `/api/v2/book-copies/{barcode}/barcode.png` | barcode, bounded image options nếu duyệt | 200 image/png generated on-demand | Authenticated |

DTOs gồm IDs, metadata/copy fields, lifecycle và version theo contract được duyệt; không expose entity, patron/loan/person data hoặc internal audit blobs. Public copy fields phải chốt C5; không tự thêm borrower info.

**Error proposal C5:** Library exception mang code, scoped handler/advice cho Library, giữ legacy `statusCode/error/message/data` và bổ sung `code` cho Library response. Không đổi handler toàn application nếu chưa được duyệt. Code đề xuất: BOOK_NOT_FOUND/COPY_NOT_FOUND, DUPLICATE_ISBN, DUPLICATE_BARCODE, BOOK_ARCHIVED, COPY_STATE_CONFLICT, VERSION_CONFLICT, INVALID_CATALOG_QUERY, INVALID_COPY_QUANTITY. Phải đối chiếu tên canonical ErrorContract trước freeze; đây chưa là registry đã duyệt. Validation 400, unauthenticated 401, forbidden 403, not found 404, integrity/state/version conflict 409; fieldErrors và auth/filter-level code mapping phải thiết kế rõ, không chỉ xử lý service exception. FE không parse message tiếng Việt để suy ra code.

### 6.2 Schema proposal

- `book`: BIGINT ID; nullable normalized ISBN unique; title/author/publisher/publishedYear/category/listPrice/coverUrl; lifecycle/audit/version.
- `book_copy`: BIGINT ID; non-null book_id FK; barcode non-null unique; shelf_location/status/reference_only; lifecycle/audit/version.
- Money đề xuất DECIMAL(12,2); enum VARCHAR; temporal/audit mapping theo convention platform. C2/C4 chốt lengths/defaults/nullability và soft-delete uniqueness (đề xuất không tái dùng ISBN/barcode từ row archive).
- FK không cascade-delete history; indexes cho book-copy FK/status/reference flag và query catalog sau khi kiểm EXPLAIN. Không tạo full-text/search infrastructure khi LIKE/query chưa được đo.
- Optimistic version cho lost updates; Book row lock khi batch create/archive cạnh tranh, lock order Book → Copy nhất quán. FK chỉ chứng minh parent tồn tại, không chứng minh parent active.
- Migration file `<next>__create_library_catalog.sql` trong Flyway stream; số V30 chỉ tentative. Không sửa migration đã áp dụng, không tạo constraint PostgreSQL-only. Không chạy migration cho tới gate target-version/head đóng.

### 6.3 FE contract và flow

- Routes đề xuất `/library/books`, `/library/books/new`, `/library/books/:id`; route/menu dùng shell authenticated hiện hữu. Kiểm static `new` trước dynamic `:id`.
- List có search/filter/sort/page server-side, reset page khi filter đổi, bỏ stale response, không lọc client-only page hiện tại. Query URL/state được typed và có empty/loading/error/retry.
- Detail có metadata, cover fallback, counts, paginated copy table và lookup barcode. ADMIN/LIBRARIAN có edit/archive/add copies/allowed copy action; backend vẫn là authority.
- Form có validation, pending-submit, duplicate/state/version conflict và forbidden handling; update version conflict yêu cầu reload/review, không ghi đè tự động.
- FE role type và capability chỉ thêm khi backend session/JWT thật sự đưa role code LIBRARIAN. Không mock role như bằng chứng auth integration hoặc tự nâng quyền borrower.
- Barcode view hỗ trợ download PNG on-demand và nhập mã thủ công; không yêu cầu live-camera/ảnh upload scanner trong catalog slice.

## 7. Phạm vi file dự kiến

Package root hiện hữu: `com.JavaTraining.BaiTap_RS`; các tên class/component mới là đề xuất. Không tạo package root mới.

| Path/khu vực | Action | Class/function và mục đích |
| --- | --- | --- |
| `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/library/catalog/controller/BookController.java`, `BookCopyController.java` | New | Endpoint/query/request validation, auth annotations, HTTP statuses |
| `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/library/catalog/service/BookService.java`, `BookCopyService.java` | New | create/update/archive/search; batch create/update/withdraw; transactions/guards |
| `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/library/catalog/service/BookBarcodeGenerator.java`, `LibraryCatalogAuditService.java` | New | Barcode generation/retry orchestration; audited mutations |
| `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/library/catalog/repository/BookRepository.java`, `BookCopyRepository.java` | New | FK/unique persistence, locking and aggregate counts |
| `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/library/catalog/service/BookSpecifications.java`, `BookSortResolver.java` | New | keyword/filter/EXISTS and sort allowlist |
| `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/library/catalog/domain/entity/Book.java`, `BookCopy.java`, `BookCopyStatus.java` | New | Metadata/copy mappings, lifecycle/version; không cascade history |
| `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/library/catalog/domain/DTOs/requests/` và `response/` | New | Create/update/batch/query DTOs, summaries/details/pagination contract |
| `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/library/catalog/exception/` | New | Scoped code adapter/handler theo C5; không thay legacy ngoài scope |
| `BE/BaiTap-RS/src/main/resources/db/migration/<next>__create_library_catalog.sql` | New | Book/copy tables, FK/unique/index sau F7/C2/C4 |
| `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/library/catalog/` | New | Service/controller/MySQL query/migration/concurrency tests |
| `FE/src/services/library/libraryCatalogApi.ts`, `FE/src/types/library/catalog.ts` | New | Typed request/response/query/error contract |
| `FE/src/views/library/LibraryBookListView.vue`, `LibraryBookDetailView.vue`, `LibraryBookFormView.vue` | New | Route-level list/detail/create/edit orchestration |
| `FE/src/components/library/BookForm.vue`, `BookCopyTable.vue`, `AddBookCopiesDialog.vue`, `BookCopyMetadataDialog.vue`, `BookBarcodeDialog.vue` | New | Reusable forms/copy actions/barcode presentation + stories/tests |
| `FE/src/router/index.ts`, `FE/src/views/shell/AuthenticatedV2ShellView.vue`, `FE/src/types/user.ts` | Edit | Routes/menu/role capability theo verified session contract |
| FE tests/stories cạnh file và routing/menu tests hiện hữu | New/Edit | Query state, forms, capability và regressions |
| Library Dev Notes và BE/FE/main indexes | New/Edit sau implementation | Actual files/validation/gate evidence |

Không tạo generic shared refactor chỉ để dùng Library. Nếu audit/error/security yêu cầu sửa shared layer, cập nhật exact paths và compatibility tests trước code. Role migration/assignment là dependency plan riêng, không giấu trong catalog SQL.

## 8. Trình tự triển khai sau approval

1. Đóng catalog decisions C1..C5 và gate F1/F7/F9 theo slice; xác minh MySQL version/applied head, target code/routes và role dependency. F8 loan-specific DDL tiếp tục tracked; không claim đóng bằng BookCopy migration.
2. Freeze DTO/schema/error/auth/audit và review FE wireframe/Storybook states cùng contract. Nếu feature giao cho BE/FE song song, chỉ bắt đầu trên contract đã duyệt; mock UI không là runtime proof.
3. Triển khai schema/entities/repositories và MySQL uniqueness/query tests.
4. Triển khai services/controllers/audit/scoped errors/barcode; focused unit/security tests.
5. Triển khai typed FE service/list/detail/forms/copies và tests/stories; tích hợp role/menu sau dependency role sẵn sàng.
6. Chạy BE/FE gates, integration trên DB target xác minh và browser flow; lập Dev Notes thực tế. Không đánh dấu complete khi gate bắt buộc FAIL/BLOCKED.

## 9. Test plan và validation

### 9.1 Unit / API / FE

| Target | Fixture/mock | Cases và assertions |
| --- | --- | --- |
| BookService create/update/archive | Mock repositories/audit/current actor; active/archived book, versions, null/duplicate ISBN | Success metadata; normalization theo C1; empty/null/invalid fields; not found; duplicate; version conflict; archive guards; audit before/after/actor; failure không publish success |
| BookCopyService add/update/withdraw | Mock ID/barcode renderer deterministic, repository/audit; N copies, ID boundary, constraint failure, inactive parent | Quantity 0/1/max/max+1; rollback/failure; canonical ID-derived barcode và bounded retry; no partial success; allowed/forbidden transitions; active-history guard; audit interaction |
| Controllers + security/filter chain | ADMIN, LIBRARIAN, authenticated other, anonymous; invalid DTO/query | 201/200/204, 400/401/403/404/409; stable code/fieldErrors; envelope/page values; unauthorized mutation never calls service; barcode binary headers |
| Query/sort resolver | Fixed keyword/filter/sort fixtures | Unknown sort/query reject; stable tie-break; correct availability excludes referenceOnly/DAMAGED/archive; literal special chars escape theo contract |
| Barcode renderer | Deterministic payload and approved symbology | Barcode decoder round-trip giữ nguyên chuỗi, content type và size limits; not-found/security; không lưu artifact |
| FE API/views/components | Mock apiClient, deterministic pages/roles/errors; deferred promises | Typed query/envelope; search reset page; stale response ignored; empty/loading/retry; auth capabilities; duplicate/version/field errors; double-submit guard; barcode fallback/download |

Mock tests không chứng minh rollback DB/unique constraint hoặc concurrency. AC-V5-LIB-001 yêu cầu core service/fine/batch coverage **tối thiểu 70% hoặc threshold repo hiện hành nếu cao hơn**. Áp dụng mức này cho core Catalog service trong slice, báo JaCoCo scope/class cùng line/branch metrics; không dùng tổng repo để che phần thay đổi thiếu coverage, không claim fine/batch được cover bởi catalog tests.

### 9.2 MySQL integration và regression

- Trên exact MySQL target version: migrate schema sạch và upgrade từ applied head được xác minh; nullable ISBN nhiều null nhưng duplicate normalized non-null bị chặn; duplicate barcode/FK orphan bị chặn.
- Hai batch cạnh tranh, constraint failure/ID boundary, lỗi giữa batch: transaction atomic, no duplicate, finite retry; batch vs archive tuân lock order và guard. Kiểm idempotency theo quyết định C3, không suy từ HTTP status.
- Pagination/filter/count không duplicate Book; test đa-copy, reference-only, archived/damaged/no-copy; đo query/N+1 và EXPLAIN indexes với dataset cố định.
- Active-loan/return/reservation lịch sử và 10 concurrent borrow attempts/one copy thuộc circulation acceptance AC-V5-LIB-002; giữ dependency và chạy khi tích hợp, **NOT RUN** trong catalog-only slice.
- Regression auth/JWT/role menu, legacy error shape, routes v1/v2/v3 và academic CRUD không đổi. Confirm catalog không expose patron/borrower data.

Commands sau implementation, từ đúng cwd:

```bash
# BE/BaiTap-RS — focused names finalized when test classes exist
./gradlew test --tests '*library.catalog.*' --no-daemon --max-workers=1
./gradlew test checkstyleMain checkstyleTest pmdMain pmdTest build --no-daemon --max-workers=1

# FE
npm test
npm run lint
npm run build
```

`test` hiện được cấu hình finalize bằng `jacocoTestReport`; kiểm report scope sau focused run. Storybook command và DB integration profile chọn theo scripts/config thật ở thời điểm triển khai; không bịa runner. Khi cần dùng scoped writable GRADLE_USER_HOME, ghi rõ trong validation. Bất kỳ gate bị skip phải ghi SKIPPED/NOT RUN, không PASS; full tests, MySQL runtime, browser và remote/deploy là các lớp bằng chứng riêng. Phiên lập plan này **không chạy các gate implementation**.

## 10. Decision gates, rủi ro và output

| Gate | Owner | Hiện trạng / bằng chứng cần có trước implementation bị ảnh hưởng |
| --- | --- | --- |
| F1 | User/PO | OPEN: duyệt Catalog slice và compatibility boundary; approval Plan 096 không tự approve toàn baseline |
| F7 | User/DB owner + DEV | OPEN: exact local MySQL version/applied Flyway head và target commit/routes; DB name user-reported chưa là proof |
| F8 | DEV + QA/DB owner | BLOCKED cho active-loan DDL; cần MySQL strategy/proof riêng. Catalog barcode unique proof không đóng invariant active loan |
| F9 / C5 | User/PO + DEV BE/FE | DEFERRED từ Plan 095; Plan 096 đề xuất catalog API/error/audit/FE để duyệt, không ghi đã frozen. Notification contract không áp dụng Catalog; còn deferred cho circulation |
| C1 | User/PO | OPEN: ISBN normalization/checksum, duplicate/archive reuse policy |
| C2 | User/PO + DEV | OPEN: field limits/required fields/year/category/listPrice precision và schema defaults |
| C3 | User/PO + DEV | OPEN: batch max N, tạo barcode từ ID theo LIB-/Code128 baseline, ID boundary, retry cap và batch request idempotency |
| C4 | User/PO + DEV | OPEN: referenceOnly/status mapping, allowed transitions, archive/withdraw/restore visibility và history guard |
| Role foundation | DEV + User/ADMIN owner | DEPENDENCY: backend LIBRARIAN role/JWT/session + audited assignment đúng Plan 095; không auto-grant trong Catalog |

Rủi ro chính: API/code adapter làm đổi legacy errors; uniqueness chỉ check ở service; archive cạnh tranh batch; counts sai do join; FE dùng role mock hoặc client filtering; schema được chọn khi chưa biết target MySQL. Giảm thiểu bằng scoped adapter + regression, DB unique/transactions, lock order, EXISTS/count queries, server auth, verified target/version/head và gate review.

Output khi plan được triển khai và validate: catalog CRUD/search có phân trang, BookCopy batch/lookup/allowed lifecycle/barcode đúng quyền, lịch sử được bảo vệ, cover URL hoạt động với fallback, FE đầy đủ states, migration/contract/tests/Dev Notes có bằng chứng. Chỉ đánh dấu completion của **Catalog slice**; không báo circulation, active-loan uniqueness, role foundation hoặc toàn Library đã hoàn tất nếu chưa có proof.

Output của phiên hiện tại: Developer Plan 096 để review và index/Dev Note ghi nhận **documentation-only**, giữ tất cả decision/gate chưa chốt tường minh.

## 11. Wireframe amendment — 2026-10-08

- Authorization: user yêu cầu “làm luôn wireframe” và “gọi subculi làm”; giữ baseline v5 đã xác nhận.
- Artifact: [Interactive wireframe](../../wireframes/fe/library/096-library-catalog-and-book-copy/index.html), [README / design mapping](../../wireframes/fe/library/096-library-catalog-and-book-copy/README.md).
- Acceptance: đồng nhất typography/colors/spacing, authenticated shell/sidebar/topbar, button/table/filter/dialog conventions với FE source hiện tại; ghi exact source references và mapping sang components triển khai trong README. Không dùng theme Library riêng.
- Screens: catalog list/filter/sort/pagination, Book detail/copy list/lookup, create/edit form, add-copies dialog, allowed copy actions/archive confirmation/barcode, loading/empty/error/forbidden/version-conflict states, reader versus manager preview.
- Prototype self-contained với dữ liệu mẫu, không gọi API/backend, không đổi FE runtime code hoặc chốt các decision C1..C5. Các role/state switches là công cụ review, không chứng minh authorization runtime.
- Validate artifact structure/JS, local links và browser interaction/responsive screenshots khi có browser runner. Chỉ báo visual QA PASS nếu đã render và kiểm tra thực tế.
- Review wireframe trước production UI implementation; UI được duyệt là reference layout/component mapping, không tự đóng F1/F7/F8/F9.

Wireframe đã tạo và kiểm tra: JS/DOM interactions và actual Chrome screenshots (desktop/mobile list, detail, create-book/add-copies dialogs). Khi implementation, reuse style/components hiện hữu và so sánh Library với màn academic/functional-room trên cùng viewport; không xem prototype static là bằng chứng UI production đã đồng nhất.

## 12. Implementation approval và catalog contract freeze — 2026-10-08

User: “tôi approve plan 96”; yêu cầu dừng agents cũ, tạo `gpt-6-luna` DEV và TEST độc lập, song song khi tách được ownership, cân nhắc worktree. Root đã interrupt ba agent cũ (đã completed); tạo DEV BE, DEV FE và independent QA mới. Không có API xóa agent; agent cũ giữ idle, không giao thêm việc.

Approval áp dụng Catalog slice và các phương án đề xuất trong plan, không mở scope circulation/role assignment/deploy/DB writes. Các dòng OPEN/DRAFT ở sections trước là trạng thái tại thời điểm lập plan; amendment này là trạng thái hiện hành cho subset Catalog. F1 CLOSED cho Catalog; F9/C1..C5 FROZEN_FOR_CATALOG theo contract dưới đây. F7 runtime MySQL và F8 active-loan proof chưa đóng. Role grant/revoke vẫn dependency riêng.

### Contract áp dụng

- ISBN nullable: trim/blank→null, loại khoảng trắng/dấu gạch, uppercase X, ISBN10/13 checksum; normalized non-null unique cả archived rows.
- title/author required trim nonblank ≤200; publisher optional ≤200; category optional free text ≤100; publishedYear optional 1..9999 (không áp giới hạn năm hiện tại); listPrice optional 0..9999999999.99, tối đa 2 decimal; coverUrl optional ≤2048 absolute HTTP(S), không fetch server-side; shelfLocation optional ≤100.
- Query publishedYear exact; keyword/category/availability theo section 5; page zero-based, size default20/max100; sort allowlist và id tie-break như đề xuất. Availability loại referenceOnly, non-AVAILABLE và archived parent.
- Barcode `LIB-` + ID padding tối thiểu 9 chữ số, overflow giữ toàn bộ ID, không truncate; Code128 PNG on demand. IDENTITY persistence dùng opaque temporary unique barcode, flush lấy ID, chuyển canonical trước commit; không expose temporary token.
- Batch quantity1..100; required `Idempotency-Key` ≤128 scoped actor+book. Persist batch request/payload fingerprint/result trong cùng transaction. Same key/payload replay201 cùng copies; different payload409 `IDEMPOTENCY_CONFLICT`. Book row lock serialize batch/archive; kiểm request trước cấp copy mới. Table idempotency là cơ chế của catalog batch, không generic framework.
- Book soft archive block ON_LOAN/RESERVED; giữ mọi copy row/status, kể cả AVAILABLE. List/detail ẩn archived Book (detail404 BOOK_NOT_FOUND); barcode lookup archived parent409 BOOK_ARCHIVED. Không restore endpoint.
- Copy initial AVAILABLE; referenceOnly flag riêng. AVAILABLE↔DAMAGED; withdraw chỉ AVAILABLE/DAMAGED→WITHDRAWN; WITHDRAWN terminal và vẫn hiện inventory. Metadata/actions block ON_LOAN/RESERVED/LOST; không generic setter cho circulation statuses.
- expectedVersion bắt buộc Book update/Copy PATCH và DELETE query. Mutation audit cùng transaction dùng AuditLog/AuditContext hiện hữu, rollback nếu audit fail; không expose audit/patron blobs.
- Scoped Library error DTO giữ legacy statusCode/error/message/data + code/fieldErrors. Catalog codes section 6 + IDEMPOTENCY_CONFLICT; 403 LIBRARY_RESOURCE_FORBIDDEN; 401 AUTHENTICATION_REQUIRED. Narrow Library-path branch ở RestAccessDeniedHandler/RestAuthenticationEntryPoint; legacy endpoints giữ envelope cũ. FE ApiError thêm optional code passthrough, không suy ra từ message.
- Verified source role discovery: UserPrincipal#getRoleCodes, JwtTokenService role claim, ResUserDTO.roles và UserService mapper đã có role codes. FE UserRole thêm LIBRARIAN để nhận role thật; không seed/grant assignment trong Catalog. Stale FE guidance về chưa có roles không thay thế source evidence này.

Ownership: DEV BE owns production BE (kể cả migration source và hai security adapters); DEV FE owns FE production/stories; QA owns tests và independent review/validation; root owns plan/devnotes, integration và browser QA. Worktree đã cân nhắc: không dùng vì path BE/FE/test tách rõ, contract đang tích hợp trên shared workspace và baseline plan/wireframe chưa commit. Không commit/push tự động. Context utilization telemetry UNKNOWN; assignments bounded, không suy percentage từ elapsed time.

Runtime gates chỉ đóng theo evidence thực tế. Viết migration source không đồng nghĩa đã áp dụng DB. MySQL runtime/source/full tests/browser là các lớp bằng chứng riêng; Dev Note sẽ ghi kết quả cuối.

## 13. Implementation checkpoint — validation blocked

Source Catalog và wireframe-backed FE đã triển khai. FE712 tests/lint/build/coverage PASS; latest BE focused130 tests/2 failures,30 new Catalog PMD findings,27 Catalog Checkstyle warnings. Tiny helper correction chưa verify; đã đạt10 debug rounds và dừng theo skill. Full-suite tiếp theo SKIPPED theo user, historical full751/2 pre-existing failures vẫn ghi FAIL. Không báo Plan hoàn thành/backend successful; resume backlog và commands ở [BE Dev Note](../../dev-note/be/library/096-library-catalog-and-book-copy-2026-10-08.md). Target DB/F7 và circulation/F8 vẫn OPEN/deferred; không deploy/commit.
