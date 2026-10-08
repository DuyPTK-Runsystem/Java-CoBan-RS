# Dev Note 096 — Library Catalog & Book Copy backend

## Plan và approval

[Plan096](../../../dev-impl-plan/summary/096-library-catalog-and-book-copy-2026-10-08.md) APPROVED qua tin nhắn user `2026-10-08`; baseline v5. Section12 là contract Catalog hiện hành; scope circulation/role assignment/deploy/target DB writes không được mở rộng.

## Thực tế triển khai

- Module `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/library/catalog/`: entities Book/BookCopy/BookCopyBatchRequest/status, DTOs, repositories, specifications/sort, controller Book/Copy/Batch/Barcode, services lookup/mapper/policy/batch/audit/barcode và scoped error advice/mapper.
- `V30__create_library_catalog.sql`: Book/copy FK, unique ISBN/barcode, indexes, optimistic version và actor/book/key idempotency result. Key collation binary, phân biệt hoa thường. Source V29→V30 không chứng minh applied head DB mục tiêu.
- Hai security handlers thêm Library-path error envelope, giữ legacy ngoài scope; `build.gradle.kts` thêm ZXing core/javase 3.5.3 cho Code128 + PNG renderer. core tạo BitMatrix; javase dùng MatrixToImageWriter xuất PNG và decoder QA. Không external barcode service.
- Independent tests trong `src/test/java/.../library/catalog/`; DEV không viết tests của QA. Root review tập trung version sau flush, đủ audit metadata, scalar book lookup trước lock tránh stale managed entity, scoped-advice precedence, legacy data:null và shelf field-presence semantics.

## Quyết định

ISBN normalized/checksum unique cả archive. quantity1..100 được service kiểm trước lock; persisted idempotency response cùng transaction, replay giữ copy IDs. Barcode IDENTITY dùng temporary UUID nội bộ→canonical LIB padded tối thiểu9, không truncate ID dài. Book→Copy lock order; không tự implement circulation bằng status. Archive/withdraw soft; giữ history/copies, referenceOnly loại borrowable counts. Audit cùng transaction.

## Validation Result — round 10, BLOCKED

| Mục | Trạng thái | Evidence |
| --- | --- | --- |
| test | FAIL | Latest focused Catalog: 130 tests /2 failures /0 errors. Tiny helper correction after run UNVERIFIED. Further full-suite SKIPPED theo user. |
| checkstyle | PASS | Main/Test tasks pass with warnings; test report378 warnings, gồm27 Catalog warnings (HTTP20, BookService4, JpaTransaction3). Task PASS không có nghĩa0 warnings. |
| PMD | FAIL | pmdMain PASS; pmdTest526 findings, gồm30 trong Catalog và496 trên baseline HEAD. |
| build | FAIL | Lifecycle build không thành công do test/pmdTest. Artifact-only `build -x test -x pmdTest` đã PASS trước cleanup test; không thay thế lifecycle gate. |

Last focused command từ `BE/BaiTap-RS`:

```bash
SPRING_AI_OPENAI_API_KEY=plan096-test-placeholder GRADLE_USER_HOME=/tmp/gradle-plan096 /home/duyptk/.gradle/wrapper/dists/gradle-9.5.1-bin/iq79hdu3mqx29lgffhp8bfmx/gradle-9.5.1/bin/gradle -I /tmp/plan096-test-heap.init.gradle test --tests '*library.catalog.*' checkstyleMain checkstyleTest pmdMain pmdTest --continue --no-daemon --max-workers=1
```

Placeholder là test-only, không credential/provider proof. Init tạm giới hạn test heap1536m, maxParallelForks1 và Spring context cache8; không sửa production config, rule/suppression hay generated reports. Sandbox launcher failures trước runner được tách khỏi kết quả test; authorized escalated runner dùng same command.

Reports: `build/reports/tests/test/index.html`, `build/test-results/test`, `build/reports/checkstyle/{main,test}.html`, `build/reports/pmd/{main,test}.html`, `build/reports/jacoco/test/html/index.html`.

### Lỗi còn lại và resume backlog

- Hai focused failures là `BookCopyBatchServiceTest.differentPayloadDoesNotAuditAgain` và `differentPayloadDoesNotCreateCopiesAgain`: helper để expected IDEMPOTENCY_CONFLICT thoát ra. QA đã sửa helper để bắt đúng exception/code nhưng chưa chạy lại; không gọi test PASS.
- Namespaced PMD còn30 Catalog findings trong HTTP/JpaTransaction/Batch/Lifecycle/CopyService/BookService tests: AvoidDuplicateLiterals5, CommentDefaultAccessModifier14, TooManyMethods2, UnusedLocalVariable1, JUnitUseExpected7, UnnecessaryImport1. Baseline HEAD496 findings được ghi riêng.
- Checkstyle Catalog còn27 warnings; cần sửa và kiểm tra lại sau khi được tiếp tục validation.
- Đã dùng10 vòng code/test/debug. Dừng theo backend-validation skill max10; không chạy vòng11 và không báo backend hoàn thành. Các test cleanup agents đã bàn giao, không có Gradle run còn hoạt động.

### Historical evidence và coverage

Trước test-structure cleanup, Catalog66 tests từng PASS. Full suite751 tests /2 failures /0 errors (Student duplicate-code conflict assertion; Timetable gateway expected3/actual4 calls), tái hiện đúng hai lỗi trên read-only archive HEAD `dec80afb482881fa537d558034946ce61ed4be91`, xác nhận pre-existing. Lượt đầu thiếu global test AI key/OOM đã FAIL/CANCELLED. User sau đó yêu cầu **skip full test**: không chạy thêm full suite; giữ lịch sử FAIL, không đổi thành PASS.

QA lượt đầu đọc thiếu XML namespace và nhầm274 Catalog PMD findings thành baseline. Sau correction, cleanup giảm Catalog274→43→30; vẫn FAIL. Mockito verify được rule tính là assertion, nên test result/interaction được tách; không bỏ behavior checks hoặc suppress rules.

JaCoCo từ **failed** focused run cuối: BookService98.25% lines, BookCopyService89.80%, Batch86.00%, Lookup100%, CopyPolicy87.50%, Specifications100%, Audit77.78%. Core services đều trên70% nhưng coverage này không chứng minh passing test gate. Branch evidence từ suite đã PASS trước cleanup lần lượt81.25/85.71/83.33/75/68.75/85.71/100%; không gắn vào kết quả test cuối như một PASS mới.

### Isolated MySQL fixture

MySQL **8.4.11** fixture network=none/không published ports đã áp dụng V30 với minimal app_user prerequisite. Manual checks gồm NULL/non-null ISBN unique, barcode unique/FK, transaction rollback, archive giữ copies. Harness `src/test/resources/library/catalog/mysql-v30-qa.sh` PASS gồm archived ISBN/barcode/FK, binary key case-sensitivity, JSON_TYPE OBJECT và rollback sau duplicate write. Raw SQL không chứng minh application-service transaction. Fixture không phải target DB, không đóng F7.

V30 SHA256: `0aae23bda1791e7f8903365a09b2109a1514e869e88cf4b3db95ac988d793ff1`. Đã cleanup bằng `docker rm -fv plan096-mysql-qa` và xác nhận container không còn. Server visual QA của root cũng đã dừng. Không commit/push/deploy hoặc áp dụng migration lên target DB.

## Deviation / gates

Helper services và split Barcode/BookCopyBatchController là phân chia trách nhiệm trong bounded catalog để phù hợp PMD, không module/architecture ngoài scope. Error mapper tách khỏi advice để giữ giới hạn số methods; API mapping không đổi. Dedicated idempotency table là cơ chế batch đã chốt ở Plan12. Không migration/deploy target, không role seed/grant, không active-loan invariant proof; F7 target runtime và F8 circulation vẫn chưa đóng.
