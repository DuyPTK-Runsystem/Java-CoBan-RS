# Requirement Baseline v5 — Library Management

## Trạng thái

- Version: `0.1-draft`
- Ngày: 2026-10-07
- Nền: v3 school-management baseline + platform capabilities hiện có trên target branch; v4 requirement chỉ được kế thừa khi trạng thái tài liệu/code xác nhận áp dụng.
- Source adaptation: standalone training spec “Thẻ Mượn Số”.
- Implementation approval: **PENDING**.

## 1. Catalog — đầu sách và bản sao

- `FR-V5-LIB-CAT-001`: Quản lý đầu sách gồm ISBN, tiêu đề, tác giả, NXB, năm xuất bản, thể loại, giá bìa và cover URL.
- `FR-V5-LIB-CAT-002`: Quản lý bản sao vật lý; mỗi bản sao thuộc đúng một đầu sách và có barcode riêng, shelf location, trạng thái, `referenceOnly`.
- `FR-V5-LIB-CAT-003`: Search/filter/sort/pagination sách theo keyword, thể loại, năm và availability; filter thực hiện ở backend.
- `FR-V5-LIB-CAT-004`: Librarian nhập lô N bản sao cho một đầu sách và hệ thống sinh barcode.
- `FR-V5-LIB-CAT-005`: Hỗ trợ ảnh bìa theo storage mechanism được Developer Plan chốt; không buộc v5 baseline phải tạo file-storage subsystem mới.

- `BR-V5-LIB-CAT-001`: ISBN nếu có phải unique theo policy catalog.
- `BR-V5-LIB-CAT-002`: `book_copy.barcode` unique.
- `BR-V5-LIB-CAT-003`: `REFERENCE`/`referenceOnly=true` không được mượn về.
- `BR-V5-LIB-CAT-004`: Xóa đầu sách phải là safe delete/soft-delete khi có lịch sử circulation; không cascade xóa loan history.

## 2. Patron và thẻ thư viện

- `FR-V5-LIB-PATRON-001`: Người có quyền kích hoạt Library Patron cho một `app_user` đủ điều kiện; không tạo user mới từ Library.
- `FR-V5-LIB-PATRON-002`: Xem patron profile gồm identity reference, card state, active-loan count, unpaid fine total và Library status.
- `FR-V5-LIB-CARD-001`: Phát hành thẻ thư viện có card number, signed QR payload và hạn 12 tháng.
- `FR-V5-LIB-CARD-002`: Render QR PNG; hỗ trợ card printable PDF/PNG nếu implementation plan giữ requirement in thẻ.
- `FR-V5-LIB-CARD-003`: Revoke thẻ báo mất và cho phép phát hành thẻ mới; thẻ cũ không còn hợp lệ.
- `FR-V5-LIB-CARD-004`: Patron tự xem thẻ hiện tại của chính mình.

- `BR-V5-LIB-PATRON-001`: `library_patron` tham chiếu `app_user`; không duplicate application identity.
- `BR-V5-LIB-PATRON-002`: Any user with an eligible patron may borrow except users whose role is ADMIN or LIBRARIAN. Plan 097 approval clarifies one role per user; authorization checks reuse the existing role enum/shared constants, not hardcoded strings.
- `BR-V5-LIB-PATRON-003`: Nợ vượt ngưỡng chuyển patron sang `BORROWING_SUSPENDED`; không lock/disable `app_user`.
- `BR-V5-LIB-CARD-001`: Thẻ hết hạn/revoked không hợp lệ dù signature đúng.
- `BR-V5-LIB-CARD-002`: Mỗi patron có tối đa một thẻ ACTIVE tại một thời điểm.

## 3. Circulation — mượn, trả, gia hạn, đặt giữ

- `FR-V5-LIB-CIRC-001`: Librarian tạo loan bằng patron card/patron lookup và một hoặc nhiều book-copy barcode.
- `FR-V5-LIB-CIRC-002`: Librarian nhận trả theo barcode; backend resolve active loan và tính fine hiện tại.
- `FR-V5-LIB-CIRC-003`: Gia hạn loan hợp lệ.
- `FR-V5-LIB-CIRC-004`: Patron/Librarian xem loan history có filter/pagination.
- `FR-V5-LIB-CIRC-005`: Patron đặt giữ đầu sách khi không có copy khả dụng; queue theo thời điểm đặt.
- `FR-V5-LIB-CIRC-006`: Librarian đánh dấu copy LOST; tạo fine theo policy.

- `BR-V5-LIB-CIRC-001`: Một patron không vượt quá giới hạn active loans do policy cấu hình; mặc định là 5.
- `BR-V5-LIB-CIRC-002`: Hạn trả mặc định 14 ngày từ ngày mượn; duration được policy cấu hình và snapshot theo loan.
- `BR-V5-LIB-CIRC-003`: Giới hạn renewal và số ngày cộng mỗi lần do policy cấu hình; mặc định là tối đa 2 lần, mỗi lần +7 ngày, đồng thời không có reservation đang chờ có priority trên đầu sách.
- `BR-V5-LIB-CIRC-004`: Một copy chỉ có tối đa một active loan tại mọi thời điểm, kể cả concurrent requests.
- `BR-V5-LIB-CIRC-005`: Patron `BORROWING_SUSPENDED`, card không hợp lệ hoặc account không đủ borrower eligibility không được tạo loan mới.
- `BR-V5-LIB-CIRC-006`: LOST fine = `book.listPrice + 50,000 VND`.
- `BR-V5-LIB-CIRC-007`: Mọi mutation circulation phải audit actor và thời điểm.

## 4. Fine và Spring Batch

- `FR-V5-LIB-FINE-001`: `libraryOverdueFineJob` chạy định kỳ cho active loans quá hạn.
- `FR-V5-LIB-FINE-002`: Job idempotent với cùng `runDate`; rerun không nhân đôi fine.
- `FR-V5-LIB-FINE-003`: Job tạo/update notification/email reminder qua notification/mail infrastructure hiện hữu.
- `FR-V5-LIB-FINE-004`: Job đồng bộ `LibraryPatronStatus` theo tổng unpaid fine.
- `FR-V5-LIB-FINE-005`: Librarian/Admin xem lịch sử job và được chạy tay với `runDate`.
- `FR-V5-LIB-FINE-006`: Card-expiry reminder job là SHOULD; có thể tách plan sau core circulation.
- `FR-V5-LIB-FINE-007`: AI enrichment batch là SHOULD và phụ thuộc Library AI feature flag.

- `BR-V5-LIB-FINE-001`: Fine trễ hạn lũy tiến theo các bậc ngày, đơn giá và trần do policy cấu hình; mặc định:
  - ngày 1–7: 5,000 VND/ngày;
  - ngày 8–30: 10,000 VND/ngày;
  - từ ngày 31: 20,000 VND/ngày;
  - trần một loan: 500,000 VND.
- `BR-V5-LIB-FINE-002`: Tiền dùng `BigDecimal`/MySQL `DECIMAL`, không dùng floating point.
- `BR-V5-LIB-FINE-003`: Mỗi loan có tối đa một overdue-fine record; LOST fine nếu cần tách loại phải có explicit fine type.
- `BR-V5-LIB-FINE-004`: Tổng unpaid fine vượt ngưỡng suspension do policy cấu hình (mặc định 500,000 VND) chuyển patron sang `BORROWING_SUSPENDED`.
- `BR-V5-LIB-FINE-005`: Khi unpaid fine trở lại ngưỡng cho phép, patron có thể trở lại ACTIVE nếu không có lý do suspension khác.
- `BR-V5-LIB-FINE-006`: Job không được ghi đè PAID/WAIVED fine bằng một kết quả cũ.

## 5. Barcode, QR và camera

- `FR-V5-LIB-CODE-001`: Sinh Code128 PNG cho từng book copy.
- `FR-V5-LIB-CODE-002`: Sinh signed QR cho library card.
- `FR-V5-LIB-CODE-003`: Export sheet/PDF nhiều barcode là SHOULD.
- `FR-V5-LIB-CODE-004`: FE quét QR/barcode bằng camera.
- `FR-V5-LIB-CODE-005`: Luôn có manual-code fallback.
- `FR-V5-LIB-CODE-006`: Keyboard-wedge handheld scanner là BONUS/optional.

- `BR-V5-LIB-CODE-001`: QR payload có version, card number, patron identifier, expiry và HMAC signature.
- `BR-V5-LIB-CODE-002`: Backend verify signature trước, sau đó kiểm card state trong DB.
- `BR-V5-LIB-CODE-003`: Camera decode tại browser; server nhận decoded text, không cần ảnh camera trong circulation hot path.

## 6. Library AI

- `FR-V5-LIB-AI-001`: Ảnh bìa/trang bản quyền → structured book metadata để prefill form; user phải xác nhận trước save.
- `FR-V5-LIB-AI-002`: Natural-language book search → structured filter → backend query.
- `FR-V5-LIB-AI-003`: Sinh summary/tags cho book là SHOULD.
- `FR-V5-LIB-AI-004`: Recommendation dựa trên borrowing history là SHOULD.
- `FR-V5-LIB-AI-005`: Draft reminder text là SHOULD; số tiền/ngày do Java cung cấp và không được model tự tính.
- `FR-V5-LIB-AI-006`: Library-policy Q&A là BONUS.

- `BR-V5-LIB-AI-001`: Dùng existing Spring AI abstraction; provider/model configurable.
- `BR-V5-LIB-AI-002`: AI không có DB/repository/SQL/direct mutation tool.
- `BR-V5-LIB-AI-003`: Machine-readable output phải structured và backend validate.
- `BR-V5-LIB-AI-004`: AI failure không được chặn catalog/circulation/fine core path.
- `BR-V5-LIB-AI-005`: Không gửi password/token; hạn chế PII. Reminder drafting không gửi name/email/phone/student code.
- `BR-V5-LIB-AI-006`: Search model không được sinh SQL để application chạy.
- `BR-V5-LIB-AI-007`: Test không gọi Internet; mock provider/error/timeout/rate-limit.

## 7. Authorization và compatibility

- `SEC-V5-LIB-001`: Reuse existing JWT/Spring Security; không tạo auth endpoint riêng cho Library.
- `SEC-V5-LIB-002`: Thêm `LIBRARIAN` role vào role model bằng migration, không thêm `MEMBER`.
- `SEC-V5-LIB-003`: Own-data checks dùng authenticated `app_user` → `library_patron` mapping phía backend.
- `SEC-V5-LIB-004`: ADMIN có Library admin capability. ACADEMIC_OFFICE không mặc nhiên có Librarian mutation capability.
- `SEC-V5-LIB-005`: Users other than ADMIN/LIBRARIAN may use borrower/self-service capability when their patron is eligible; Plan 097 approved one-role-per-user and shared role enum/constants.
- `COMP-V5-LIB-001`: Existing `/api/v1`, `/api/v2`, `/api/v3` contracts không thay đổi ngầm.
- `COMP-V5-LIB-002`: API mới ưu tiên `/api/v2`; collision phải namespace `/api/v2/library/*`.

## 8. Non-functional và acceptance chung

- `NFR-V5-LIB-001`: Backend là authority cuối cho quyền, fine, state transition, concurrency và data integrity.
- `NFR-V5-LIB-002`: Library migration phải MySQL-compatible và tiếp nối Flyway head hiện tại.
- `NFR-V5-LIB-003`: Active-loan uniqueness phải được bảo vệ ở database/transaction level, không chỉ bằng service `if`.
- `NFR-V5-LIB-004`: Batch restart/idempotency phải có integration test.
- `NFR-V5-LIB-005`: API errors dùng stable machine code; FE không parse message text.
- `NFR-V5-LIB-006`: FE theo project conventions; không thêm state/network library chỉ để khớp source training.
- `NFR-V5-LIB-007`: Security secrets/config không hard-code.
- `AC-V5-LIB-001`: Core service/fine/batch có coverage target tối thiểu 70% hoặc threshold repo hiện hành nếu cao hơn.
- `AC-V5-LIB-002`: Có concurrency test: 10 requests cùng mượn một copy → đúng 1 success.
- `AC-V5-LIB-003`: Có batch idempotency test: chạy cùng `runDate` 3 lần → fine count/amount không nhân đôi.
- `AC-V5-LIB-004`: Có QR sign/tamper/expiry/revoke tests.
- `AC-V5-LIB-005`: Có role/ownership matrix tests.
- `AC-V5-LIB-006`: Có FE Storybook/component states và browser evidence cho circulation/camera khi Developer Plan yêu cầu.
- `AC-V5-LIB-007`: Gate chưa chạy phải ghi `NOT RUN` hoặc `BLOCKED`, không ghi `PASS`.

## 9. Mục còn mở

- `TBD-V5-LIB-001`: Workflow gán/bỏ role `LIBRARIAN` và ai có quyền thực hiện.
- `TBD-V5-LIB-002`: Reservation pickup window/expiry và cách chuyển WAITING → READY.
- `TBD-V5-LIB-003`: Fine payment là chỉ ghi nhận thu tiền offline hay cần receipt workflow.
- `TBD-V5-LIB-004`: Storage cho cover image/card PDF nếu production cần persistence.
- `TBD-V5-LIB-005`: Chính sách retention đối với AI prompt/output metadata.
