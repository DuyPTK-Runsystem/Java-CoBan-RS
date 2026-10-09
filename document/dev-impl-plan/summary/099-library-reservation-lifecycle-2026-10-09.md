# Developer Plan 099 — Reservation Lifecycle

## 1. Trạng thái, mục tiêu và dependency

- **Ngày lập:** 2026-10-09.
- **Application baseline:** Library Management v5 (0.1-draft), trên nền tảng ứng dụng quản lý trường học `Java-CoBan-RS`.
- **Trạng thái:** **DRAFT — Kế hoạch chi tiết và wireframe sẵn sàng; chờ người dùng xem xét và phê duyệt; chưa ghi vào file mã nguồn**.
- **Kế hoạch phụ thuộc:**
  - Plan 095: v5 Foundation, Dependency & Contract Freeze.
  - Plan 096: Library Catalog & Book Copy.
  - Plan 097: Library Patron & Library Card.
  - Plan 098: Library Loan / Return / Renewal / Lost (đang được triển khai song song).
- **Kế hoạch sử dụng kết quả:** Hoàn thiện khép kín toàn bộ chu trình lưu thông sách thư viện; làm nền tảng cho batch job mở rộng và phân hệ Library AI (CR-V5-001).
- **Ranh giới thực thi:** Tuân thủ chỉ dẫn của người dùng: *Plan 098 đang được implement, tuyệt đối không ghi hoặc sửa đổi bất kỳ file code backend/frontend nào trong phiên lập plan này*.

---

### 1.1. Mục tiêu của Plan 099

Mục tiêu của Vertical Slice 099 là hoàn thiện toàn diện **Vòng đời đặt giữ sách (Reservation Lifecycle)** với độ tin cậy giao dịch và bảo vệ đồng thời cao:

1. **Hàng đợi FIFO theo đầu sách (Book-level FIFO Queue):**
   - Đặt giữ gắn với đầu sách (`book_id`), không gán cứng vào một bản sao vật lý cụ thể trước khi có bản sao khả dụng.
   - Quản lý hàng đợi công bằng theo thứ tự thời gian đặt (`reserved_at ASC, reservation_id ASC`).
   - Cung cấp cơ chế tính toán thứ tự vị trí trong hàng đợi (Queue Position #1, #2, #3...).
2. **Máy trạng thái hoàn chỉnh (Complete State Machine):**
   - Vận hành đầy đủ 5 trạng thái: `WAITING` → `READY` → `FULFILLED`, `CANCELLED`, `EXPIRED`.
   - Chuyển trạng thái nguyên tử (atomic transitions) đi kèm khóa bản sao (`book_copy`) và ghi nhận thời điểm.
3. **Cơ chế tự động cấp phát bản sao (Auto Allocation Engine):**
   - Tự động gán bản sao vật lý cho đơn đầu tiên trong hàng đợi FIFO khi:
     - Có bản sao được trả về qua luồng nhận trả (`POST /api/v2/returns`).
     - Có bản sao mới được nhập kho hoặc chuyển trạng thái sang `AVAILABLE`.
     - Đơn đặt giữ `READY` trước đó bị hủy (`CANCELLED`) hoặc hết hạn nhận (`EXPIRED`).
4. **Chính sách cửa sổ nhận sách (Pickup-Window Policy):**
   - Cấu hình số ngày giữ sách tại quầy (`reservationPickupDays`, mặc định 3 ngày lịch).
   - Snapshot phiên bản chính sách (`policy_version`) và số ngày tại thời điểm chuyển sang `READY`.
   - Tính toán hạn chót nhận sách chuẩn xác theo múi giờ `Asia/Ho_Chi_Minh` (hiệu lực hết ngày nhận, hết hạn từ đầu ngày tiếp theo).
5. **Tác vụ tự động xử lý quá hạn (Batch Expiry Task):**
   - Thiết kế Batch Job `libraryReservationExpiryJob` định kỳ quét các đơn `READY` quá hạn nhận.
   - Chuyển trạng thái sang `EXPIRED`, giải phóng bản sao và tự động tái cấp phát ngay cho người chờ tiếp theo.
   - Hỗ trợ endpoint kích hoạt thủ công có phân quyền cho `ADMIN` và `LIBRARIAN`.
6. **Bảo vệ ưu tiên hàng đợi trong lưu thông (Circulation Queue Protection):**
   - Ngăn chặn bạn đọc đang mượn sách gia hạn (`RENEW_RESERVATION_PRIORITY_CONFLICT`) nếu đầu sách đang có người chờ trong hàng đợi.
   - Ngăn chặn mượn nhầm tại quầy: bản sao ở trạng thái `RESERVED` chỉ được cấp cho bạn đọc đang sở hữu đơn `READY` tương ứng.
   - Chuyển đơn `READY` thành `FULFILLED` đồng thời kích hoạt tạo loan mượn sách mới tại quầy lưu thông.
7. **Trải nghiệm người dùng đồng bộ (FE Workflows & Wireframe):**
   - Màn hình **Kệ giữ sách chờ nhận (Hold-Shelf & Pickup Desk)** cho thủ thư xử lý tại quầy.
   - Màn hình **Tra cứu hàng đợi (Queue Inspector)** cho thủ thư và quản trị viên.
   - Màn hình **Bạn đọc tự phục vụ (Patron Self-Service Portal)** để học sinh/giáo viên theo dõi vị trí, nhận thông báo và tự hủy đơn.
   - Màn hình **Cấu hình chính sách & Vận hành Batch (Policy & Batch Expiry)**.

---

## 2. Quyết định kế thừa và các điểm đã chốt

Kế hoạch này kế thừa trọn vẹn các quyết định kiến trúc và nghiệp vụ từ Plan 095, 097 và 098:

- **Identity & Role Authority:** `app_user` là nguồn định danh duy nhất. Bạn đọc (`library_patron`) liên kết 1-1 với `app_user`. Không tạo role `MEMBER`.
- **Borrower / Reservation Eligibility:**
  - Mọi người dùng có hồ sơ bạn đọc hợp lệ đều được quyền đặt giữ sách, **ngoại trừ** người dùng mang role `ADMIN` hoặc `LIBRARIAN` (theo quy tắc thống nhất tại Plan 097: tài khoản quản trị/thủ thư không tham gia mượn hoặc đặt giữ sách).
  - Bạn đọc bị tạm đình chỉ (`BORROWING_SUSPENDED`) hoặc có thẻ thư viện hết hạn/bị thu hồi (`REVOKED`/`EXPIRED`) **bị chặn tạo đặt giữ mới**, nhưng vẫn được phép tra cứu lịch sử và tự hủy đơn cũ.
- **Không cấp quyền Thủ thư khi còn đặt giữ:** Hệ thống từ chối cấp role `LIBRARIAN` cho bất kỳ người dùng nào nếu tài khoản đó vẫn còn đơn đặt giữ ở trạng thái `WAITING` hoặc `READY`.
- **Quy tắc đặt giữ đầu sách (Book Reservation Rules):**
  - Không cho phép đặt giữ nếu đầu sách đang có ít nhất 1 bản sao khả dụng mượn về (`AVAILABLE` và `reference_only = false`) nằm trên kệ chưa được phân bổ (`RESERVATION_NOT_NEEDED`).
  - Mỗi bạn đọc chỉ được phép có **tối đa 1 yêu cầu đặt giữ đang hoạt động (`WAITING` hoặc `READY`) trên cùng một đầu sách** (`RESERVATION_ALREADY_EXISTS`).
  - Tổng số yêu cầu đặt giữ đang hoạt động của mỗi bạn đọc không vượt quá hạn mức chính sách `maxActiveReservationsPerPatron` (mặc định 3 đơn).
- **Thứ tự hàng đợi FIFO:** Sắp xếp tuyệt đối theo `reserved_at ASC`, sử dụng `reservation_id ASC` làm tie-breaker ổn định, không thay đổi thứ tự ngầm.
- **Khóa bản sao khi READY:** Khi bản sao được cấp phát cho reservation, bản sao vật lý chuyển trạng thái thành `RESERVED`. Không ai khác được phép mượn bản sao này tại quầy ngoại trừ người sở hữu đơn reservation đó.
- **Quy ước múi giờ và thời hạn:**
  - Áp dụng múi giờ `Asia/Ho_Chi_Minh`.
  - Cửa sổ nhận sách `pickup_due_at`: Tính từ ngày cấp phát + số ngày quy định trong chính sách. Ngày đến hạn vẫn là ngày hợp lệ (hết ngày lúc 23:59:59). Đơn vị đặt giữ chỉ bị coi là quá hạn từ `00:00:00` ngày tiếp theo.
- **Bảo toàn dữ liệu lịch sử:** Tuyệt đối không xóa vật lý (hard-delete) bản ghi reservation. Mọi kết thúc (FULFILLED, CANCELLED, EXPIRED) đều là state transition có ghi nhận audit.
- **Thông báo thời gian thực:** Khi đơn chuyển sang `READY`, hệ thống tạo In-App Notification trong cùng transaction cơ sở dữ liệu và gửi SSE event sau khi commit transaction thành công.

---

## 3. Nguồn đối chiếu và truy vết requirement

| Mã Yêu cầu             | Nội dung yêu cầu                                                                                                       | Hiện thực hóa trong Plan 099                                                         | Bằng chứng nghiệm thu dự kiến                                                                |
| ---------------------- | ---------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------- |
| **FR-V5-LIB-CIRC-005** | Patron đặt giữ đầu sách khi không có copy khả dụng; queue theo thời điểm đặt.                                          | API tạo reservation, kiểm tra điều kiện bản sao, xếp hàng FIFO, tính vị trí.         | Test ca tạo reservation thành công, test chặn khi còn sách khả dụng, test FIFO ordering.     |
| **BR-V5-LIB-CIRC-003** | Không renew khi reservation queue tạo conflict theo policy.                                                            | Kiểm tra hàng đợi WAITING khi patron yêu cầu gia hạn loan tại `LibraryLoanService`.  | Test gia hạn bị từ chối với mã `RENEW_RESERVATION_PRIORITY_CONFLICT`.                        |
| **BR-V5-LIB-CIRC-005** | Patron `BORROWING_SUSPENDED`, card không hợp lệ hoặc account không đủ borrower eligibility không được tạo reservation. | `LibraryEligibilityService` thẩm định trước khi tạo reservation.                     | Test 409 `PATRON_BORROWING_SUSPENDED`, 400 `CARD_EXPIRED`, 403 `LIBRARY_RESOURCE_FORBIDDEN`. |
| **NFR-V5-LIB-001**     | Backend authority cho mọi state transition, concurrency và data integrity.                                             | DB Pessimistic lock (`SELECT ... FOR UPDATE`), transaction boundary chặt chẽ.        | Test đồng thời 10 luồng cùng trả sách/hủy reservation: đúng 1 người nhận được bản sao.       |
| **NFR-V5-LIB-003**     | Stable error codes theo chuẩn Envelope của dự án.                                                                      | Chuẩn hóa mã lỗi `RESERVATION_*`, trả về qua `RestResponse` không làm vỡ API client. | Kiểm thử hợp đồng JSON response lỗi.                                                         |
| **COMP-V5-LIB-001**    | Giữ tương thích ngược với API `/api/v1`, `/api/v2`, `/api/v3` hiện hữu.                                                | Định tuyến `/api/v2/reservations`, không sửa ngầm controller học vụ.                 | Route scan verification.                                                                     |

---

## 4. Phạm vi (Scope)

### 4.1. In Scope

**Backend & Cơ sở dữ liệu:**
1. **Quản lý Hàng đợi & Tạo mới Đặt giữ:**
   - Thẩm định điều kiện: Patron status `ACTIVE`, Thẻ `ACTIVE` & chưa hết hạn, Role không phải `ADMIN`/`LIBRARIAN`, Đầu sách không bị `ARCHIVED`, Không còn bản sao `AVAILABLE` không phải tài liệu tham khảo, Chưa có đơn active trên đầu sách này, Chưa vượt `maxActiveReservationsPerPatron`.
   - Lưu trữ bản ghi reservation với trạng thái `WAITING`, thời điểm `reserved_at` và snapshot cấu hình chính sách hiện hành.
   - API tra cứu hàng đợi: Lấy danh sách đang chờ theo đầu sách kèm thứ tự vị trí (Queue position).
2. **Cơ chế Tự động Cấp phát Bản sao (Auto Allocation):**
   - Khi có bản sao trở về trạng thái khả dụng (sách được trả, sách mới nhập kho): Tìm đơn `WAITING` đầu tiên trong FIFO queue của đầu sách.
   - Chuyển trạng thái đơn từ `WAITING` → `READY`, gán `allocated_copy_id`.
   - Đổi trạng thái bản sao thành `RESERVED`.
   - Tính toán `pickup_due_at = now.toLocalDate().plusDays(pickupDays + 1).atStartOfDay()`.
   - Ghi audit log và phát hành In-App Notification + SSE refresh.
3. **Quy trình Nhận sách tại Quầy (Fulfillment):**
   - Tại quầy lưu thông, khi bạn đọc đến nhận sách: Xác nhận đúng bạn đọc sở hữu đơn `READY`.
   - Chuyển trạng thái reservation từ `READY` → `FULFILLED`, ghi `fulfilled_at`.
   - Chuyển bản sao từ `RESERVED` → tạo `library_loan` mới (trạng thái `ON_LOAN`).
4. **Quy trình Hủy Đặt giữ (Cancellation):**
   - Cho phép bạn đọc tự hủy đơn của chính mình, hoặc thủ thư/quản trị viên hủy thay.
   - Nếu đơn đang `WAITING`: Chuyển sang `CANCELLED`, ghi `cancelled_at`, loại khỏi hàng đợi.
   - Nếu đơn đang `READY`: Chuyển sang `CANCELLED`, giải phóng bản sao đang gán và **kích hoạt ngay lập tức thuật toán cấp phát cho người chờ tiếp theo** trong FIFO queue. Nếu hàng đợi rỗng, trả bản sao về `AVAILABLE`.
5. **Quy trình Quá hạn & Tác vụ Batch Expiry (Pickup Window & Batch Job):**
   - Định nghĩa Spring Batch job / Service task `libraryReservationExpiryJob`.
   - Quét tất cả các đơn `READY` có `pickup_due_at <= now` theo múi giờ `Asia/Ho_Chi_Minh`.
   - Chuyển trạng thái thành `EXPIRED`, ghi `expired_at`.
   - Tự động tái cấp phát bản sao cho đơn `WAITING` tiếp theo trong queue (hoặc chuyển về `AVAILABLE` nếu hàng đợi rỗng).
   - Ghi nhận lịch sử chạy batch vào bảng `library_batch_run_summary`.
   - Endpoint thủ công: `POST /api/v2/library/batch-jobs/reservation-expiry` dành cho `ADMIN` và `LIBRARIAN`.
6. **Bảo vệ Lưu thông (Circulation Interlocking):**
   - Chặn gia hạn mượn sách (`renew`) khi đầu sách có hàng đợi `WAITING`.
   - Chặn mượn tự do (`borrow`) đối với các bản sao đang ở trạng thái `RESERVED`.
7. **Quản trị Chính sách Cửa sổ Nhận (Policy Configuration):**
   - Mở rộng cấu hình chính sách lưu thông: `reservationPickupDays` (mặc định 3 ngày), `maxActiveReservationsPerPatron` (mặc định 3 đơn).
   - Phân quyền cập nhật chính sách cho `ADMIN` và `LIBRARIAN`.
   - Quản lý phiên bản (`policy_version`) và snapshot độc lập cho từng giao dịch.

**Frontend:**
1. Màn hình **Kệ giữ sách chờ nhận (`LibraryHoldShelfView.vue`)**: Quản lý danh sách bản sao đang giữ tại quầy, chip đếm ngược hạn nhận, nút "Xác nhận nhận sách" và "Hủy giữ".
2. Màn hình **Tra cứu hàng đợi (`LibraryReservationQueueView.vue`)**: Chọn đầu sách, xem danh sách FIFO, thông tin người xếp hàng, thời gian chờ.
3. Màn hình **Bạn đọc tự phục vụ (`PatronReservationsView.vue`)**: Banner thông báo sách sẵn sàng, bảng đơn đang hoạt động, lịch sử đơn cũ, nút bấm hủy đơn.
4. Màn hình **Cấu hình & Tác vụ Batch (`LibraryPolicyView.vue` & `LibraryBatchJobsView.vue`)**: Biểu mẫu chỉnh sửa số ngày nhận sách, nút bấm chạy batch quá hạn và bảng nhật ký batch.
5. Modal đặt giữ đầu sách tích hợp tại trang Chi tiết đầu sách (`BookDetailView.vue`).

### 4.2. Out of Scope

- Không gửi email notification có cơ chế retry phức tạp (ngoài phạm vi slice, chỉ dùng In-App notification và SSE đã được kiểm chứng an toàn sau commit).
- Không phạt tiền (Fine) đối với bạn đọc để đơn đặt giữ bị quá hạn (`EXPIRED`) trong giai đoạn này.
- Không hỗ trợ đặt giữ có đặt cọc tiền mặt hoặc cổng thanh toán trực tuyến.
- Không hỗ trợ dịch vụ vận chuyển sách liên chi nhánh (chỉ áp dụng mô hình 1 thư viện trường học tập trung).
- Không xóa cứng (hard-delete) dữ liệu đặt giữ.

---

## 5. Thiết kế kỹ thuật chi tiết

### 5.1. Mô hình Máy trạng thái (State Machine Matrix)

| Trạng thái xuất phát | Sự kiện kích hoạt                                               | Điều kiện chuyển đổi                                                                   | Trạng thái đích | Tác động phụ (Side-effects)                                                                                                                                               |
| -------------------- | --------------------------------------------------------------- | -------------------------------------------------------------------------------------- | --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| *(Khởi tạo)*         | Tạo đặt giữ mới (`POST /api/v2/reservations`)                   | Đủ điều kiện mượn, sách không còn bản sao khả dụng, chưa có đơn active trên sách này.  | `WAITING`       | Xếp vào cuối hàng đợi FIFO. Snapshot `policy_version`. Ghi audit log.                                                                                                     |
| `WAITING`            | Trả sách / Nhập sách mới / Giải phóng sách                      | Bản sao của đầu sách chuyển về khả dụng; đơn này đứng đầu hàng đợi FIFO (`index = 0`). | `READY`         | Gán `allocated_copy_id`. Bản sao chuyển sang `RESERVED`. Tính `pickup_due_at`. Tạo In-App Notification & phát SSE event. Ghi audit log.                                   |
| `WAITING`            | Hủy đặt giữ (`POST /api/v2/reservations/{id}/cancel`)           | Người dùng sở hữu đơn hoặc Thủ thư/Quản trị viên thực hiện.                            | `CANCELLED`     | Đơn bị loại khỏi hàng đợi. Ghi `cancelled_at`. Ghi audit log.                                                                                                             |
| `READY`              | Nhận sách tại quầy (`POST /api/v2/loans` hoặc fulfill endpoint) | Đúng bạn đọc sở hữu đơn xuất trình thẻ tại quầy lưu thông.                             | `FULFILLED`     | Ghi `fulfilled_at`. Tạo bản ghi `library_loan` mới (mượn sách). Bản sao chuyển sang trạng thái `ON_LOAN`. Ghi audit log.                                                  |
| `READY`              | Hủy đơn tại quầy (`cancel`)                                     | Bạn đọc báo không lấy hoặc Thủ thư chủ động hủy.                                       | `CANCELLED`     | Ghi `cancelled_at`. Giải phóng bản sao khỏi đơn. **Kích hoạt ngay `allocateNext()`**: Nếu còn đơn WAITING, chuyển đơn kế sang `READY`; nếu không, bản sao về `AVAILABLE`. |
| `READY`              | Hết hạn nhận sách (Batch Expiry Job hoặc On-demand Check)       | Thời điểm hiện tại `now >= pickup_due_at` (sau 00:00:00 ngày T+1).                     | `EXPIRED`       | Ghi `expired_at`. Giải phóng bản sao khỏi đơn. **Kích hoạt ngay `allocateNext()`**: Cấp cho người chờ kế tiếp hoặc trả về `AVAILABLE`. Ghi audit log.                     |

### 5.2. Thuật toán Cấp phát Bản sao & Khóa Đồng thời (Allocation Engine & Concurrency)

Nhằm bảo đảm tính toàn vẹn dữ liệu trong môi trường nhiều giao dịch đồng thời (10 requests cùng trả sách hoặc hủy đặt giữ), quy trình cấp phát tuân thủ nghiêm ngặt:

```java
// Mã giả thuật toán allocateNext() trong transaction có khóa bi quan
@Transactional
public void allocateNext(BookCopy copy, LocalDateTime now, Long actorUserId) {
    // 1. Khóa bi quan bản sao
    BookCopy lockedCopy = copyRepository.findByIdForUpdate(copy.getId())
            .orElseThrow(...);

    // 2. Tìm danh sách WAITING theo thứ tự FIFO ổn định
    List<LibraryReservation> queue = reservationRepository.findQueue(
            lockedCopy.getBook().getId(), ReservationStatus.WAITING);

    for (LibraryReservation waiting : queue) {
        // Khóa bi quan đơn reservation đang xét
        LibraryReservation lockedRes = reservationRepository.findByIdForUpdate(waiting.getId())
                .orElse(null);
        if (lockedRes == null || lockedRes.getStatus() != ReservationStatus.WAITING) {
            continue; // Bỏ qua nếu giao dịch song song đã xử lý đơn này
        }

        // Kiểm tra tính hợp lệ hiện tại của bạn đọc (chưa bị suspended)
        LibraryPatron patron = patronRepository.findById(lockedRes.getPatronId()).orElse(null);
        if (patron == null || patron.getStatus() != LibraryPatronStatus.ACTIVE) {
            continue; // Bỏ qua nếu bạn đọc đã bị đình chỉ mượn sách
        }

        // 3. Tính toán hạn nhận sách theo snapshot chính sách
        LocalDateTime pickupDue = now.toLocalDate()
                .plusDays(lockedRes.getPickupDaysSnapshot() + 1L)
                .atStartOfDay();

        // 4. Chuyển trạng thái nguyên tử
        lockedRes.ready(lockedCopy.getId(), now, pickupDue);
        lockedCopy.setStatus(BookCopyStatus.RESERVED);

        // 5. Ghi nhận audit và phát hành thông báo
        auditService.record(actorUserId, "RESERVATION_READY", "library_reservation",
                lockedRes.getId(), null, Map.of("copyId", lockedCopy.getId(), "pickupDueAt", pickupDue.toString()));

        notificationService.publish("library-res-ready-" + lockedRes.getId(),
                "Sách đặt giữ đã sẵn sàng",
                "Đầu sách '" + lockedCopy.getBook().getTitle() + "' đã có bản sao. Hạn nhận: " + pickupDue.toLocalDate(),
                patron.getUserId(), actorUserId);

        return; // Đã cấp phát thành công cho 1 bản sao, kết thúc vòng lặp
    }

    // Nếu không còn ai trong hàng đợi, chuyển bản sao về AVAILABLE
    lockedCopy.setStatus(BookCopyStatus.AVAILABLE);
}
```

### 5.3. Tương tác với Luồng Lưu thông Khác (Circulation Interlocking)

1. **Khi Nhận Trả Sách (`POST /api/v2/returns`):**
   - Sau khi đóng active loan của bản sao, thay vì mặc định đưa bản sao về `AVAILABLE`, gọi `LibraryReservationService.allocateNext(copy, now, actorUserId)`.
   - Nếu có người đang đợi, bản sao lập tức chuyển thành `RESERVED` và gán cho đơn đặt giữ.
2. **Khi Bạn Đọc Yêu Cầu Gia Hạn (`POST /api/v2/loans/{loanId}/renew`):**
   - Kiểm tra: `reservationRepository.countWaitingByBookId(bookId) > 0`.
   - Nếu có người đang chờ trong hàng đợi WAITING, từ chối gia hạn ngay lập tức với mã lỗi `RENEW_RESERVATION_PRIORITY_CONFLICT` ("Không thể gia hạn do đầu sách đang có bạn đọc khác xếp hàng chờ mượn").
3. **Khi Thủ Thư Cho Mượn Tại Quầy (`POST /api/v2/loans`):**
   - Nếu bản sao được quét đang ở trạng thái `RESERVED`:
     - Kiểm tra xem bạn đọc đang đứng tại quầy có phải là chủ sở hữu của đơn đặt giữ `READY` tương ứng với bản sao đó hay không.
     - Nếu **đúng**: Cho phép mượn, chuyển đơn reservation thành `FULFILLED` và tạo `library_loan`.
     - Nếu **không đúng**: Từ chối với mã lỗi `COPY_RESERVED_FOR_ANOTHER_PATRON` ("Bản sao này đang được giữ riêng cho bạn đọc khác").

---

## 6. Hợp đồng API đề xuất (Proposed API Contract)

Tất cả các endpoint tuân thủ quy chuẩn RESTful envelope `RestResponse<T>` và tiền tố `/api/v2`.

| Method | Endpoint                                        | Quyền hạn                                   | Mô tả thao tác                                                              |
| ------ | ----------------------------------------------- | ------------------------------------------- | --------------------------------------------------------------------------- |
| `POST` | `/api/v2/reservations`                          | Eligible Patron (Self) / Staff              | Tạo yêu cầu đặt giữ cho một đầu sách.                                       |
| `GET`  | `/api/v2/reservations`                          | Patron (Scope tự thân) / Staff (Toàn quyền) | Lấy danh sách đặt giữ phân trang, có bộ lọc `bookId`, `status`, `patronId`. |
| `GET`  | `/api/v2/reservations/{id}`                     | Owner / Staff                               | Xem chi tiết đơn đặt giữ kèm thứ tự trong hàng đợi.                         |
| `POST` | `/api/v2/reservations/{id}/cancel`              | Owner / Staff                               | Hủy yêu cầu đặt giữ (kèm lý do). Tự động tái cấp phát nếu đang READY.       |
| `GET`  | `/api/v2/reservations/queue`                    | Staff (`ADMIN`, `LIBRARIAN`)                | Tra cứu chi tiết hàng đợi FIFO của một đầu sách (`?bookId=...`).            |
| `GET`  | `/api/v2/reservations/hold-shelf`               | Staff (`ADMIN`, `LIBRARIAN`)                | Lấy danh sách tất cả các bản sao đang ở trạng thái `READY` trên kệ giữ.     |
| `POST` | `/api/v2/library/batch-jobs/reservation-expiry` | Staff (`ADMIN`, `LIBRARIAN`)                | Kích hoạt thủ công tác vụ quét và thu hồi các đơn đặt giữ quá hạn.          |

### Cấu trúc DTO & Payload

#### 1. Yêu cầu tạo đặt giữ (`POST /api/v2/reservations`)
```json
// Request Body
{
  "bookId": 1
}

// Response Body (RestResponse<LibraryReservationDTO>)
{
  "statusCode": 201,
  "message": "Tạo yêu cầu đặt giữ thành công",
  "data": {
    "reservationId": 102,
    "bookId": 1,
    "bookTitle": "Clean Code: A Handbook of Agile Software Craftsmanship",
    "patronId": 12,
    "patronName": "Nguyễn An",
    "status": "WAITING",
    "queuePosition": 2,
    "reservedAt": "2026-10-09T11:20:00+07:00",
    "readyAt": null,
    "pickupDueAt": null,
    "allocatedCopyBarcode": null,
    "fulfilledAt": null,
    "cancelledAt": null,
    "expiredAt": null,
    "policyVersion": "LIB-POLICY-1"
  }
}
```

#### 2. Danh sách hàng đợi FIFO (`GET /api/v2/reservations/queue?bookId=1`)
```json
{
  "statusCode": 200,
  "data": {
    "bookId": 1,
    "bookTitle": "Clean Code",
    "totalCopies": 5,
    "availableCopies": 0,
    "totalWaiting": 3,
    "items": [
      {
        "position": 1,
        "reservationId": 101,
        "patronName": "Trần Minh",
        "patronCode": "STU2600088",
        "status": "READY",
        "reservedAt": "2026-10-07T09:15:00+07:00",
        "allocatedCopyBarcode": "LIB-000000045",
        "pickupDueAt": "2026-10-12T23:59:59+07:00"
      },
      {
        "position": 2,
        "reservationId": 102,
        "patronName": "Nguyễn An",
        "patronCode": "STU2600124",
        "status": "WAITING",
        "reservedAt": "2026-10-08T10:20:00+07:00",
        "allocatedCopyBarcode": null,
        "pickupDueAt": null
      }
    ]
  }
}
```

#### 3. Kích hoạt Batch quét quá hạn (`POST /api/v2/library/batch-jobs/reservation-expiry`)
```json
// Request Body
{
  "runDate": "2026-10-09"
}

// Response Body
{
  "statusCode": 200,
  "message": "Thực thi tác vụ quét đặt giữ quá hạn thành công",
  "data": {
    "jobExecutionId": 88,
    "runDate": "2026-10-09",
    "status": "COMPLETED",
    "expiredCount": 2,
    "reallocatedCount": 2,
    "releasedToAvailableCount": 0
  }
}
```

### Danh mục Mã lỗi Ổn định (Stable Error Codes)

- `RESERVATION_NOT_NEEDED` (409): Đầu sách vẫn còn bản sao khả dụng trên kệ mượn tự do, không cần đặt giữ.
- `RESERVATION_ALREADY_EXISTS` (409): Bạn đọc đã có một yêu cầu đặt giữ đang hoạt động (`WAITING`/`READY`) cho đầu sách này.
- `RESERVATION_LIMIT_EXCEEDED` (409): Vượt quá số lượng đơn đặt giữ tối đa cho phép theo chính sách (`maxActiveReservationsPerPatron`).
- `RESERVATION_NOT_FOUND` (404): Không tìm thấy bản ghi đặt giữ tương ứng.
- `RESERVATION_NOT_ACTIVE` (409): Yêu cầu đặt giữ đã kết thúc (đã hoàn thành, đã hủy hoặc đã hết hạn), không thể thao tác tiếp.
- `COPY_RESERVED_FOR_ANOTHER_PATRON` (409): Bản sao này đang được giữ riêng cho bạn đọc khác, không thể xuất mượn.
- `RENEW_RESERVATION_PRIORITY_CONFLICT` (409): Không thể gia hạn mượn sách vì đầu sách đang có bạn đọc khác xếp hàng chờ.
- `PATRON_BORROWING_SUSPENDED` (409): Bạn đọc đang bị tạm đình chỉ mượn sách, không được đặt giữ mới.
- `CARD_EXPIRED` / `CARD_REVOKED` (400): Thẻ thư viện không hợp lệ.
- `LIBRARY_RESOURCE_FORBIDDEN` (403): Không có quyền thao tác trên đơn đặt giữ của người khác.

---

## 7. Thiết kế Cơ sở dữ liệu & Chỉ mục (Database & Indexes)

Bảng `library_reservation` được định nghĩa trong schema với cấu trúc chi tiết như sau:

```sql
CREATE TABLE library_reservation (
    reservation_id BIGINT NOT NULL AUTO_INCREMENT,
    book_id BIGINT NOT NULL,
    patron_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL, -- WAITING, READY, FULFILLED, CANCELLED, EXPIRED
    reserved_at DATETIME(6) NOT NULL,
    ready_at DATETIME(6) NULL,
    pickup_due_at DATETIME(6) NULL,
    allocated_copy_id BIGINT NULL,
    fulfilled_at DATETIME(6) NULL,
    cancelled_at DATETIME(6) NULL,
    expired_at DATETIME(6) NULL,
    cancel_reason VARCHAR(255) NULL,
    policy_version VARCHAR(64) NOT NULL,
    pickup_days_snapshot INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (reservation_id),
    -- Tối ưu hóa truy vấn hàng đợi FIFO theo đầu sách
    KEY idx_library_reservation_queue (book_id, status, reserved_at, reservation_id),
    -- Tối ưu hóa tra cứu đơn của bạn đọc
    KEY idx_library_reservation_patron (patron_id, status, reserved_at),
    -- Tối ưu hóa quét batch job đơn quá hạn
    KEY idx_library_reservation_expiry (status, pickup_due_at),
    -- Khóa ngoại bảo toàn tính toàn vẹn
    CONSTRAINT fk_library_reservation_book FOREIGN KEY (book_id) REFERENCES book (book_id),
    CONSTRAINT fk_library_reservation_patron FOREIGN KEY (patron_id) REFERENCES library_patron (patron_id),
    CONSTRAINT fk_library_reservation_copy FOREIGN KEY (allocated_copy_id) REFERENCES book_copy (book_copy_id),
    CONSTRAINT fk_library_reservation_policy FOREIGN KEY (policy_version) REFERENCES library_circulation_policy (policy_version)
);
```

### Các quy tắc đảm bảo bất biến (Invariants):
1. **Một bản sao chỉ gán cho tối đa 1 reservation READY tại cùng thời điểm:** Được kiểm soát chặt chẽ bằng khóa bi quan kết hợp cập nhật trạng thái `book_copy.status = 'RESERVED'`.
2. **Không cascade delete:** Khi xóa/sửa Book, Patron hay Card, bảng `library_reservation` không được phép cascade delete để bảo toàn 100% nhật ký lưu thông và kiểm toán.

---

## 8. Danh mục File Dự kiến (Target File Scope)

> **LƯU Ý:** Các file dưới đây chỉ được tạo hoặc chỉnh sửa **sau khi người dùng phê duyệt Plan 099** và Plan 098 đã hoàn tất việc tích hợp.

### Backend (`BE/BaiTap-RS/`)
- `.../library/circulation/domain/entity/LibraryReservation.java` (Entity JPA quản lý vòng đời).
- `.../library/circulation/domain/entity/ReservationStatus.java` (Enum: `WAITING`, `READY`, `FULFILLED`, `CANCELLED`, `EXPIRED`).
- `.../library/circulation/domain/DTOs/requests/ReqReservationDTO.java` (Request tạo đơn).
- `.../library/circulation/domain/DTOs/response/LibraryReservationDTO.java` (Response chi tiết).
- `.../library/circulation/domain/DTOs/response/ReservationQueueDTO.java` (Response hàng đợi FIFO).
- `.../library/circulation/repository/LibraryReservationRepository.java` (Truy vấn hàng đợi, kiểm tra trùng lặp, quét quá hạn).
- `.../library/circulation/service/LibraryReservationService.java` (Mở rộng toàn diện các nghiệp vụ state transitions, allocation, expiry).
- `.../library/circulation/service/LibraryReservationExpiryJobService.java` (Bộ xử lý Batch Job quét quá hạn).
- `.../library/circulation/controller/LibraryReservationController.java` (REST API endpoints).
- `.../src/test/java/.../library/circulation/LibraryReservationServiceTest.java` (Unit & Concurrency Tests).

### Frontend (`FE/`)
- `src/types/library/reservation.ts` (TypeScript interfaces & enums).
- `src/services/library/libraryReservationApi.ts` (API client service).
- `src/views/library/LibraryHoldShelfView.vue` (Màn hình Kệ giữ sách chờ nhận cho Thủ thư).
- `src/views/library/LibraryReservationQueueView.vue` (Màn hình Tra cứu & Quản lý Hàng đợi FIFO).
- `src/views/library/PatronReservationsView.vue` (Màn hình Bạn đọc tự phục vụ).
- `src/components/library/CreateReservationDialog.vue` (Hộp thoại Đặt giữ sách mới).
- `src/components/library/HoldShelfTable.vue` (Bảng danh sách bản sao đang giữ tại quầy).
- `src/router/index.ts` (Cấu hình route phân hệ đặt giữ).

---

## 9. Kế hoạch Kiểm thử & Bằng chứng Nghiệm thu (Validation Plan)

### 9.1. Backend Unit & Concurrency Tests
1. **Kiểm thử Điều kiện Đặt giữ (Reservation Validation):**
   - Đặt giữ thành công khi tất cả bản sao đang mượn → Trạng thái `WAITING`, vị trí #1.
   - Chặn đặt giữ khi vẫn còn bản sao `AVAILABLE` trên kệ → Báo lỗi `RESERVATION_NOT_NEEDED`.
   - Chặn đặt giữ trùng lặp trên cùng đầu sách → Báo lỗi `RESERVATION_ALREADY_EXISTS`.
   - Chặn bạn đọc bị `BORROWING_SUSPENDED` hoặc thẻ hết hạn → Báo lỗi tương ứng.
   - Chặn tài khoản mang role `ADMIN` hoặc `LIBRARIAN` đặt giữ sách → Báo lỗi `LIBRARY_RESOURCE_FORBIDDEN`.
2. **Kiểm thử Cấp phát Tự động (Auto Allocation):**
   - Khi trả sách (`return`), bản sao được tự động gán cho đơn `WAITING` đầu tiên → Đơn chuyển `READY`, bản sao chuyển `RESERVED`, tính đúng `pickup_due_at`.
   - Khi đơn `READY` bị hủy (`cancel`), bản sao lập tức được chuyển cho đơn `WAITING` thứ hai trong hàng đợi.
3. **Kiểm thử Tranh chấp Đồng thời (Concurrency Proof):**
   - Giả lập 10 luồng song song cùng thực hiện trả sách hoặc hủy đơn: Đúng 1 đơn `WAITING` được thăng cấp lên `READY` cho mỗi bản sao vật lý được giải phóng; không bao giờ xảy ra tình trạng 1 bản sao bị gán cho 2 đơn khác nhau.
4. **Kiểm thử Tác vụ Quá hạn (Batch Expiry Test):**
   - Chạy batch quét các đơn `READY` có hạn nhận trong quá khứ → Đơn chuyển sang `EXPIRED`, giải phóng bản sao và tự động tái cấp phát cho người tiếp theo.
   - Batch chạy lại lần 2 trong cùng ngày không bị trùng lặp dữ liệu (Idempotent).

### 9.2. Frontend Tests & Storybook
- Unit tests cho `libraryReservationApi.ts`: Xử lý đúng mapping envelope và mã lỗi.
- Storybook stories cho các trạng thái: Đơn `WAITING` (hiển thị vị trí hàng đợi), Đơn `READY` (hiển thị hạn nhận và cảnh báo hết hạn), Trạng thái rỗng, Trạng thái lỗi phân quyền 403.

---

## 10. Đánh giá Rủi ro và Giải pháp Giảm thiểu

| Rủi ro kỹ thuật / nghiệp vụ                                                                                                         | Mức độ     | Biện pháp giảm thiểu                                                                                                                                                            |
| ----------------------------------------------------------------------------------------------------------------------------------- | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Race condition khi cấp phát bản sao:** Hai bản sao về kho cùng lúc hoặc nhiều đơn hủy đồng thời dẫn đến cấp phát sai thứ tự FIFO. | Cao        | Sử dụng `PESSIMISTIC_WRITE` lock trên Book và Reservation row. Duyệt và cấp phát tuần tự trong transaction độc lập.                                                             |
| **Gia hạn làm nghẽn người chờ:** Người đang mượn liên tục gia hạn khiến người trong hàng đợi không bao giờ nhận được sách.          | Trung bình | Bắt buộc kiểm tra `queue.countWaiting() > 0` trong transaction gia hạn (`LibraryLoanService.renew`), từ chối ngay khi có người chờ.                                             |
| **Bản sao bị mượn nhầm tại quầy:** Bạn đọc khác nhìn thấy sách trên kệ giữ và mang ra quầy mượn.                                    | Trung bình | Kiểm tra `copy.status == RESERVED` tại `LibraryLoanService.create`. Bắt buộc kiểm tra quyền sở hữu đơn đặt giữ, chặn người khác mượn với mã `COPY_RESERVED_FOR_ANOTHER_PATRON`. |
| **Đổi chính sách làm vỡ hạn cũ:** Admin đổi `pickupDays` từ 3 xuống 1 ngày làm các đơn đang chờ nhận bị quá hạn đột ngột.           | Thấp       | Snapshot `pickup_days_snapshot` và `policy_version` ngay tại thời điểm đơn chuyển sang `READY`. Thay đổi chính sách chỉ áp dụng cho các lượt cấp phát mới.                      |

---

## 11. Yêu cầu Phê duyệt (Approval Request)

Kính đề nghị người dùng xem xét và phê duyệt:

1. **Phạm vi và Kiến trúc của Plan 099:** Quản lý toàn bộ vòng đời đặt giữ sách (`WAITING`, `READY`, `FULFILLED`, `CANCELLED`, `EXPIRED`) theo cơ chế hàng đợi FIFO và chính sách cửa sổ nhận sách.
2. **Hợp đồng API & Mã lỗi đề xuất** tại Mục 6.
3. **Mô hình Wireframe tương tác** đã xây dựng tại `document/wireframes/fe/library/099-library-reservation-lifecycle/index.html`.
4. **Cho phép bắt đầu triển khai Plan 099** sau khi Plan 098 hoàn tất và được nghiệm thu chính thức.
