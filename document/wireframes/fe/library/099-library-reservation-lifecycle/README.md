# Plan 099 — Wireframe Vòng đời Đặt giữ Sách (Reservation Lifecycle)

Tài liệu này thuyết minh và hướng dẫn sử dụng wireframe tương tác độc lập (self-contained offline prototype) tại `index.html` cho Plan 099 — **Reservation Lifecycle: queue, WAITING/READY/fulfilled/cancelled/expired, pickup-window policy**.

---

## 1. Mục tiêu và phạm vi Wireframe

Wireframe này trực quan hóa toàn bộ vòng đời của yêu cầu đặt giữ sách (reservation) theo tiêu chuẩn thiết kế của phân hệ Thư viện v5 (Library Management), tích hợp trên nền tảng ứng dụng quản lý trường học `Java-CoBan-RS`.

### Các luồng nghiệp vụ chính được mô phỏng:
1. **Kệ giữ sách chờ nhận (Hold-Shelf & Pickup Desk):**
   - Dành cho thủ thư xử lý các đơn đặt giữ đã chuyển sang trạng thái `READY` (bản sao đã được tách riêng tại kệ giữ).
   - Theo dõi thời điểm sẵn sàng, hạn nhận sách (pickup due date), chip đếm ngược thời gian (còn an toàn, hết hạn hôm nay, đã quá hạn).
   - Thao tác tại quầy: Xác nhận bạn đọc nhận sách (`FULFILLED` → tạo loan mượn sách) hoặc Hủy giữ (`CANCELLED` → tự động cấp cho người tiếp theo).
2. **Quản lý hàng đợi FIFO theo đầu sách (Queue Inspector):**
   - Hiển thị thứ tự ưu tiên tuyệt đối theo thời gian đặt (`reserved_at ASC, reservation_id ASC`).
   - Phân biệt rõ đơn vị trí #1 đang giữ sách (`READY`) và các đơn tiếp theo đang xếp hàng (`WAITING`).
   - Thống kê thời gian thực: Tổng bản sao, Đang cho mượn, Đang giữ tại quầy, Khả dụng tại kệ.
3. **Cổng bạn đọc tự phục vụ (Patron Self-Service Portal):**
   - Trải nghiệm của học sinh/giáo viên: theo dõi thứ tự hàng đợi của mình, nhận alert thông báo nổi bật khi sách đã về kho (`READY`).
   - Xem mã bản sao, hạn chót đến thư viện nhận sách và vị trí ngăn kệ.
   - Chủ động hủy yêu cầu đặt giữ khi không còn nhu cầu để nhường cho bạn đọc phía sau.
   - Tra cứu lịch sử đặt giữ cũ (FULFILLED, CANCELLED, EXPIRED).
4. **Quản trị chính sách cửa sổ nhận & Tác vụ tự động (Policy & Batch Expiry):**
   - Cấu hình số ngày cửa sổ nhận sách (`reservationPickupDays`, mặc định 3 ngày).
   - Cấu hình số lượng đơn đặt giữ tối đa đang hoạt động cho mỗi bạn đọc (`maxActiveReservationsPerPatron`, mặc định 3 đơn).
   - Mô phỏng chạy Batch job `libraryReservationExpiryJob` để quét các đơn READY quá hạn, chuyển sang `EXPIRED`, giải phóng bản sao và tự động tái cấp phát (reallocate) cho người chờ kế tiếp.
   - Lịch sử audit thay đổi chính sách có versioning.
5. **Thanh kịch bản mô phỏng tương tác (Lifecycle Simulation Rail):**
   - Các nút bấm kích hoạt trực tiếp các bước chuyển trạng thái (State transitions):
     - `+ 1. Bạn đọc tạo đặt giữ mới` → Trạng thái `WAITING`.
     - `↺ 2. Trả sách vào kho` → Tự động tìm người đầu hàng đợi chuyển thành `READY`.
     - `✓ 3. Đến quầy nhận sách` → Hoàn tất chuyển thành `FULFILLED` và tạo Loan.
     - `✕ 4. Hủy đặt giữ` → Chuyển `CANCELLED` và nhượng ngay bản sao cho người tiếp theo.
     - `⏱ 5. Quá hạn nhận` → Chạy Batch chuyển `EXPIRED` và nhượng bản sao cho người tiếp theo.

---

## 2. Hướng dẫn trải nghiệm Wireframe

1. **Khởi chạy:** Mở trực tiếp tệp `document/wireframes/fe/library/099-library-reservation-lifecycle/index.html` bằng bất kỳ trình duyệt hiện đại nào (Chrome, Firefox, Safari, Edge). Tệp hoàn toàn độc lập, không cần web server, node_modules hay kết nối internet.
2. **Chuyển đổi vai trò (Role Switcher):**
   - `LIBRARIAN` / `ADMIN`: Toàn quyền thao tác quầy, xác nhận nhận sách, hủy đơn, cấu hình chính sách và kích hoạt chạy batch job.
   - `STUDENT` / `TEACHER`: Vai trò bạn đọc; chỉ xem được đơn của chính mình, tạo đơn mới và tự hủy đơn của bản thân; các nút quản trị và xử lý quầy sẽ bị vô hiệu hóa (disabled).
3. **Thử nghiệm chuyển đổi trạng thái bằng Bảng điều khiển mô phỏng:**
   - Nhấn nút **"+ 1. Bạn đọc tạo đặt giữ mới"**: Xem đơn mới xuất hiện trong Hàng đợi với trạng thái `WAITING`.
   - Nhấn nút **"↺ 2. Trả sách vào kho"**: Quan sát hệ thống tự động gán bản sao vật lý cho đơn đầu tiên, chuyển thành `READY` và tính hạn nhận sách (3 ngày).
   - Nhấn nút **"✓ 3. Đến quầy nhận sách"**: Đơn `READY` biến mất khỏi kệ giữ và chuyển sang Lịch sử với trạng thái `FULFILLED`.
   - Nhấn nút **"✕ 4. Hủy đặt giữ"** hoặc **"⏱ 5. Quá hạn nhận"**: Quan sát cơ chế chuyển nhượng tự động — bản sao không bị trả về kho bừa bãi mà lập tức được chuyển cho đơn `WAITING` tiếp theo trong hàng đợi.
4. **Kiểm tra Chính sách & Batch:**
   - Vào tab "Chính sách & Tác vụ Quá hạn".
   - Đổi thời hạn nhận sách từ 3 thành 5 ngày rồi bấm "Lưu & Tăng version mới".
   - Quan sát version tăng từ v12 lên v13 và audit log ghi nhận actor thay đổi.
   - Bấm nút "Chạy batch ngay với ngày hiện tại" để xem mô phỏng quét đơn quá hạn.

---

## 3. Máy trạng thái (State Machine) của Reservation

```
                  ┌────────────────────────────────────────┐
                  │           patron / staff creates       │
                  │   POST /api/v2/reservations (WAITING)  │
                  └───────────────────┬────────────────────┘
                                      │
                                      ▼
                           ┌─────────────────────┐
                           │       WAITING       │◄─────────────────┐
                           │ (In FIFO Queue)     │                  │
                           └──────────┬──────────┘                  │
                                      │                             │
                     Copy available   │                             │
                     via return /     │                             │
                     catalog intake   ▼                             │
                           ┌─────────────────────┐                  │
                           │        READY        │                  │
                           │ (Copy Allocated,    │                  │
                           │  Held on Shelf)     │                  │
                           └──────┬───┬───┬──────┘                  │
                                  │   │   │                         │
     Patron picks up at desk      │   │   │ Patron / Staff cancels  │
     POST /api/v2/loans           │   │   │ POST /api/v2/           │
     (or fulfill endpoint)        │   │   │ reservations/{id}/cancel│
                                  │   │   │                         │
                                  │   │   └───────────────┐         │
                                  │   │                   │         │
                                  │   │ Pickup window     │         │
                                  │   │ expires (>3 days) │         │
                                  │   │ Batch Expiry Job  │         │
                                  │   │                   │         │
                                  │   ▼                   ▼         │
                                  │ ┌───────────┐   ┌───────────┐   │
                                  │ │  EXPIRED  │   │ CANCELLED │   │
                                  │ └─────┬─────┘   └─────┬─────┘   │
                                  │       │               │         │
                                  │       └───────┬───────┘         │
                                  │               │                 │
                                  ▼               ▼                 │
                           ┌─────────────┐   Allocate copy to next  │
                           │  FULFILLED  │   in FIFO queue          │
                           │ (Creates    │   ───────────────────────┘
                           │  Active     │
                           │  Loan)      │
                           └─────────────┘
```

---

## 4. Ánh xạ với Sản phẩm Triển khai (Code Mapping)

| Màn hình / Thành phần Wireframe | Thành phần Frontend dự kiến | Endpoint Backend / Service liên quan |
|---|---|---|
| **Kệ giữ sách chờ nhận (Hold Shelf)** | `FE/src/views/library/LibraryHoldShelfView.vue`<br>`HoldShelfCardList.vue` | `GET /api/v2/reservations?status=READY`<br>`POST /api/v2/loans` (Fulfillment qua Circulation)<br>`LibraryReservationService.java` |
| **Hàng đợi theo đầu sách (Queue View)** | `FE/src/views/library/LibraryReservationQueueView.vue`<br>`ReservationQueueTable.vue` | `GET /api/v2/reservations?bookId={id}`<br>`GET /api/v2/reservations/queue`<br>`LibraryReservationRepository.java` |
| **Bạn đọc tự phục vụ (Self-Service)** | `FE/src/views/library/PatronReservationsView.vue`<br>`MyReservationList.vue` | `GET /api/v2/reservations` (Scope tự thân)<br>`POST /api/v2/reservations`<br>`POST /api/v2/reservations/{id}/cancel` |
| **Chính sách & Tác vụ Batch** | `FE/src/views/library/LibraryPolicyView.vue`<br>`ReservationPolicyCard.vue`<br>`LibraryBatchJobsView.vue` | `GET /api/v2/library/policies/circulation`<br>`PUT /api/v2/library/policies/circulation`<br>`POST /api/v2/library/batch-jobs/reservation-expiry`<br>`libraryReservationExpiryJob` |
| **Modal Đặt giữ sách** | `FE/src/components/library/CreateReservationDialog.vue` | `POST /api/v2/reservations` |
| **Khung Dịch vụ Typed API** | `FE/src/services/library/libraryReservationApi.ts` | `LibraryReservationController.java` |
| **Mô hình DTO & Enums** | `FE/src/types/library/reservation.ts` | `LibraryReservationDTO.java`, `ReservationStatus.java` |

---

## 5. Quy chuẩn Thiết kế Visual (Visual Standards)

- **Bảng màu:**
  - Nền ứng dụng: `#f8fafc` (Slate 50), Card Surface: `#ffffff`, Đường viền: `#e2e8f0` (Slate 200).
  - Màu chủ đạo: `#4f46e5` (Indigo 600), Hover: `#3730a3` (Indigo 800), Nền nhẹ: `#eef2ff` (Indigo 50).
  - Trạng thái `READY`: Xanh lá `#059669` / nền `#ecfdf5` / viền `#a7f3d0`.
  - Trạng thái `WAITING`: Hổ phách `#d97706` / nền `#fffbeb` / viền `#fde68a`.
  - Trạng thái `FULFILLED`: Xanh dương `#2563eb` / nền `#eff6ff` / viền `#bfdbfe`.
  - Trạng thái `EXPIRED`: Đỏ `#dc2626` / nền `#fef2f2` / viền `#fecaca`.
  - Trạng thái `CANCELLED`: Xám `#64748b` / nền `#f1f5f9` / viền `#e2e8f0`.
- **Typography:** Font hệ thống không chân (Roboto / Inter / System UI), kích thước tiêu chuẩn 13px–14px cho dữ liệu bảng và form, 17px cho tiêu đề section, 24px cho tiêu đề trang.
- **Trải nghiệm tương tác:** Đáp ứng tốt trên máy tính để bàn (Desktop layout 2 cột) và màn hình nhỏ/máy tính bảng (1 cột xếp chồng); hiển thị thông báo toast góc phải màn hình sau mỗi thao tác.

