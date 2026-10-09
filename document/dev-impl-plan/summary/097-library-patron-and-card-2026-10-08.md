# Kế hoạch phát triển 097 — Quản lý bạn đọc và thẻ thư viện

## 1. Trạng thái, mục tiêu và các điều kiện phụ thuộc

- **Ngày lập:** `2026-10-08`.

- **Nhánh triển khai:** `training/duyptk/student-management`.

- **Phiên bản tài liệu ứng dụng:** v5 — Quản lý thư viện.

- **Trạng thái:** **ĐÃ PHÊ DUYỆT — triển khai trong phạm vi Plan 097 với các làm rõ tại §1.3**.

- **Kế hoạch phụ thuộc:** Plan 095 và Plan 096.

- **Kế hoạch sử dụng kết quả:** Plan 098 — Nghiệp vụ mượn, trả và gia hạn sách.

### 1.1. Mục tiêu

Triển khai chức năng quản lý bạn đọc và thẻ thư viện trong ứng dụng quản lý trường học hiện hữu, bao gồm:

1. Kích hoạt và quản lý hồ sơ bạn đọc từ tài khoản `app_user` đã tồn tại.

2. Quản lý trạng thái sử dụng thư viện của từng bạn đọc.

3. Phát hành, thu hồi và cấp lại thẻ thư viện.

4. Sinh mã QR cho thẻ bằng chữ ký HMAC-SHA256.

5. Xác thực thẻ theo chữ ký, thời hạn và trạng thái thực tế trong cơ sở dữ liệu.

6. Cung cấp giao diện quản trị bạn đọc cho `ADMIN` và `LIBRARIAN`.

7. Cung cấp giao diện để người dùng tự xem hồ sơ và thẻ thư viện của mình.

8. Chuẩn bị các dịch vụ kiểm tra điều kiện sử dụng thư viện cho nghiệp vụ mượn, trả sách ở Plan 098.

Việc triển khai phải tuân theo kiến trúc Spring Boot và Vue TypeScript của dự án, sử dụng chung hệ thống đăng nhập, phân quyền, ghi nhận lịch sử và cơ sở dữ liệu MySQL hiện hữu.

### 1.2. Các quyết định đã được phê duyệt

Kế hoạch này kế thừa trực tiếp các quyết định tại Plan 095:

- `app_user` tiếp tục là nguồn định danh duy nhất.

- Không tạo vai trò `MEMBER` hoặc hệ thống xác thực riêng.

- Tên bảng quản lý thẻ chính thức là `library_card`, không sử dụng `membership_card`.

- Mỗi tài khoản có tối đa một hồ sơ bạn đọc.

- Mỗi bạn đọc có tối đa một thẻ mang trạng thái `ACTIVE` tại cùng thời điểm.

- Người dùng mang vai trò `LIBRARIAN` không được tạo lượt mượn, gia hạn hoặc đặt giữ mới, kể cả khi có thêm vai trò khác.

- Chỉ `ADMIN` được cấp hoặc thu hồi vai trò `LIBRARIAN`; mọi thay đổi phải được ghi nhận lịch sử.

- Không được cấp vai trò `LIBRARIAN` nếu người dùng còn lượt đặt giữ ở trạng thái `WAITING` hoặc `READY`.

- Việc tạm đình chỉ mượn sách không được khóa tài khoản đăng nhập hoặc ảnh hưởng quyền sử dụng các chức năng học vụ.

- Thời hạn thẻ mặc định là 12 tháng, có thể được cấu hình theo chính sách có phiên bản.

- Mã QR được sinh khi có yêu cầu; không lưu ảnh QR hoặc tệp PDF thẻ lâu dài trong cơ sở dữ liệu.

- Ngày hết hạn vẫn là ngày còn hiệu lực. Thẻ chỉ hết hiệu lực từ đầu ngày tiếp theo theo múi giờ `Asia/Ho_Chi_Minh`.

Những quyết định trên không cần xin phê duyệt lại. Chỉ các phương án kỹ thuật và các vấn đề còn mở trong phạm vi Plan 097 mới cần được xem xét.

### 1.3. Phê duyệt và các làm rõ đã được người dùng duyệt

Plan 097 được phê duyệt. Các quyết định sau có hiệu lực và được ưu tiên khi nội dung cũ trong kế hoạch hoặc tài liệu v5 mâu thuẫn:

- Mọi user đủ điều kiện có thể là bạn đọc/mượn sách, ngoại trừ user có role `ADMIN` hoặc `LIBRARIAN`. Theo role model hiện tại mỗi user có đúng một role; tái sử dụng role enum/shared constants, không hardcode tên role thành string literal.
- Khi phát hành thẻ, người dùng có thể nhập thời hạn theo số tháng hoặc ngày hết hạn. Nếu nhập số tháng, FE tính ngày hết hạn; cả hai chế độ đều gửi `expiresAt` cho BE xử lý.
- Plan 098 sở hữu các luồng Loan, Return, Renewal và Lost, gồm giao dịch lưu thông, khóa bản sao, hạn trả, trả/gia hạn và xử lý mất sách. Plan 097 chỉ cung cấp dữ liệu/thành phần cần tích hợp, không triển khai các luồng này.
- PMD (`pmdMain`, `pmdTest`) và toàn bộ test suite BE/FE được phép bỏ qua và phải ghi `SKIPPED`. Không chạy các gate này. Chỉ báo `PASS` cho lệnh thực sự đã chạy; ghi chính xác gate tập trung, compile, Checkstyle, lint/type/build và test mục tiêu đã chạy.

### 1.4. Test plan trọng điểm

**Backend:** test service/controller/API cho activation candidates (mọi tài khoản chưa có patron, không lọc role), tạo patron, trùng user, cập nhật trạng thái, ownership và authorization; borrower eligibility cho role thường so với `ADMIN`/`LIBRARIAN`; cấp/thu hồi/cấp lại thẻ, `expiresAt` bao gồm ngày hết hạn và hết hiệu lực từ ngày kế tiếp; QR đúng/sai chữ ký, payload sai phiên bản, QR của thẻ hết hạn/thu hồi; đảm bảo không lộ secret. Test cạnh tranh tạo patron/thẻ và ràng buộc một thẻ ACTIVE chỉ được coi là bằng chứng DB/concurrency nếu chạy bằng MySQL hoặc môi trường tương thích; mock/unit không chứng minh được tính chất đó.

**Frontend:** test request/response mapping cho API, danh sách và lựa chọn activation candidate; ngày hết hạn được tính từ duration theo quy tắc hiện có và payload gửi BE luôn là ngày ISO; nhập trực tiếp ngày hết hạn; validation ngày không hợp lệ/quá khứ; trạng thái tải/rỗng/lỗi/quyền và tương tác cấp/thu hồi; QR và ngày hiệu lực trình bày đúng.

**Lệnh và giới hạn:** chạy focused tests theo module sau khi implementation sẵn sàng; chạy compile/type-check, Checkstyle, lint/build phù hợp với thay đổi. Không chạy PMD hoặc full suites theo quyết định đã duyệt. Coverage chỉ báo nếu JaCoCo task/report sẵn có; không tự thêm ngưỡng. MySQL/live DB, runtime/provider và browser evidence chỉ báo khi được chạy thực tế, không suy ra từ mock/source.

## 2. Yêu cầu và nguồn đối chiếu

### 2.1. Thứ tự tài liệu tham chiếu

 1. `document/application-doc/v5/README.md`

 2. `document/application-doc/v5/ApplicationContext.md`

 3. `document/application-doc/v5/RequirementBaseline.md`

 4. `document/application-doc/v5/change-request/CR-V5-001-library-management.md`

 5. `document/application-doc/v5/FoundationAndContractFreeze.md`

 6. `document/application-doc/v5/modules/02-LibraryPatronAndCard.md`

 7. `document/application-doc/v5/modules/05-BarcodeQrAndScanning.md`

 8. `document/application-doc/v5/contract/Authorization.md`

 9. `document/application-doc/v5/contract/ErrorContract.md`

10. `document/application-doc/v5/data-model/MigrationAndConcurrency.md`

11. `document/application-doc/v5/frontend-api/README.md`

12. Plan 095, Plan 096 và mã nguồn trên nhánh triển khai.

Khi tài liệu mô-đun cũ mâu thuẫn với quyết định đã được ghi nhận trong Plan 095, áp dụng quyết định đã được phê duyệt tại Plan 095. Việc đồng bộ tài liệu v5 phải được thực hiện công khai, không tự ý thay đổi lại nghiệp vụ.

### 2.2. Đối chiếu yêu cầu

| Mã yêu cầu               | Nội dung cần đáp ứng                                                       | Bằng chứng nghiệm thu                                      |
| ------------------------ | -------------------------------------------------------------------------- | ---------------------------------------------------------- |
| `FR-V5-LIB-PATRON-001`   | Kích hoạt hồ sơ bạn đọc từ tài khoản hiện hữu                              | Kiểm thử tạo hồ sơ, kiểm tra khóa ngoại và quyền thực hiện |
| `FR-V5-LIB-PATRON-002`   | Xem thông tin bạn đọc, trạng thái thẻ và các thông tin nghiệp vụ liên quan | Kiểm thử API, phân quyền và giao diện                      |
| `FR-V5-LIB-CARD-001`     | Phát hành thẻ có mã, chữ ký QR và thời hạn                                 | Kiểm thử phát hành, thời hạn và chính sách áp dụng         |
| `FR-V5-LIB-CARD-002`     | Sinh ảnh QR để hiển thị và sử dụng                                         | Kiểm thử nội dung, định dạng ảnh và quyền truy cập         |
| `FR-V5-LIB-CARD-003`     | Thu hồi và cấp lại thẻ                                                     | Kiểm thử chuyển trạng thái và vô hiệu hóa thẻ cũ           |
| `FR-V5-LIB-CARD-004`     | Bạn đọc tự xem thẻ của mình                                                | Kiểm thử quyền sở hữu và giao diện                         |
| `BR-V5-LIB-PATRON-001`   | Không tạo định danh trùng lặp với `app_user`                               | Ràng buộc duy nhất và kiểm thử dữ liệu                     |
| `BR-V5-LIB-PATRON-002`   | Kiểm tra điều kiện sử dụng thư viện theo chính sách đã duyệt               | Kiểm thử nhiều vai trò và trạng thái bạn đọc               |
| `BR-V5-LIB-PATRON-003`   | Tạm đình chỉ mượn sách độc lập với tài khoản đăng nhập                     | Kiểm thử chuyển trạng thái và bảo toàn quyền học vụ        |
| `BR-V5-LIB-CARD-001`     | Thẻ bị thu hồi hoặc hết hạn không hợp lệ                                   | Kiểm thử xác thực QR với trạng thái thực tế                |
| `BR-V5-LIB-CARD-002`     | Mỗi bạn đọc có tối đa một thẻ `ACTIVE`                                     | Kiểm thử đồng thời trên MySQL                              |
| `BR-V5-LIB-CODE-001/002` | QR có phiên bản, chữ ký và được xác thực tại máy chủ                       | Kiểm thử chữ ký, dữ liệu bị sửa và tra cứu cơ sở dữ liệu   |
| `SEC-V5-LIB-002..005`    | Tích hợp vai trò, quyền quản lý và quyền sở hữu                            | Kiểm thử bảo mật                                           |
| `NFR-V5-LIB-005`         | Mã lỗi nghiệp vụ ổn định và tương thích hệ thống                           | Kiểm thử cấu trúc lỗi và tương thích API cũ                |

Phần tổng hợp khoản mượn và phí chưa thanh toán phải sử dụng dữ liệu thực từ các mô-đun tương ứng khi những mô-đun đó được triển khai. Plan 097 không được tạo dữ liệu giả để thể hiện rằng nghiệp vụ mượn sách hoặc tính phí đã hoạt động.

## 3. Phạm vi thực hiện

### 3.1. Phạm vi bao gồm

**Phía máy chủ**

- Tạo mô hình dữ liệu `LibraryPatron` và `LibraryCard`.

- Tạo các bảng và ràng buộc bằng Flyway.

- Xây dựng dịch vụ kích hoạt, tìm kiếm, xem chi tiết và quản lý trạng thái bạn đọc.

- Xây dựng dịch vụ phát hành, thu hồi và cấp lại thẻ.

- Sinh mã thẻ duy nhất.

- Sinh và xác thực chữ ký QR bằng HMAC-SHA256.

- Sinh ảnh QR dạng PNG khi có yêu cầu.

- Kiểm tra quyền truy cập và quyền sở hữu tài nguyên.

- Chuẩn bị dịch vụ kiểm tra điều kiện mượn sách để các kế hoạch tiếp theo sử dụng.

- Tích hợp vai trò `LIBRARIAN` với hệ thống phân quyền hiện hữu.

- Ghi nhận lịch sử các thao tác quan trọng.

- Kiểm thử nghiệp vụ, bảo mật, cơ sở dữ liệu và các yêu cầu xử lý đồng thời.

**Phía giao diện**

- Danh sách bạn đọc, tìm kiếm và phân trang.

- Trang chi tiết hồ sơ bạn đọc.

- Chức năng kích hoạt hồ sơ bạn đọc.

- Thay đổi trạng thái bạn đọc theo quyền được cấp.

- Phát hành, thu hồi và cấp lại thẻ.

- Hiển thị và tải ảnh QR.

- Trang tự xem thẻ thư viện.

- Xử lý đầy đủ các trạng thái tải dữ liệu, không có dữ liệu, lỗi, hết hạn và bị thu hồi.

- Kiểm thử giao diện và tích hợp điều hướng.

### 3.2. Phạm vi không bao gồm

- Xử lý mượn, trả, gia hạn hoặc báo mất sách.

- Xử lý hàng đợi đặt giữ.

- Tính phí trễ hạn và ghi nhận thanh toán.

- Tự động đồng bộ trạng thái đình chỉ dựa trên số dư phí.

- Xây dựng tác vụ nhắc hết hạn bằng Spring Batch.

- Tạo chức năng quét bằng camera.

- Tạo hoặc chỉnh sửa tài khoản `Student`, `Teacher`, `User`.

- Tạo ứng dụng hoặc trang đăng nhập thư viện độc lập.

- Lưu trữ tệp PDF thẻ hoặc ảnh QR dưới dạng dữ liệu lâu dài.

- Các tính năng AI của thư viện.

- Triển khai lên môi trường production.

Các nội dung ngoài phạm vi phải được bàn giao cho kế hoạch phù hợp, không được triển khai ngầm trong Plan 097.

## 4. Hiện trạng được kiểm tra trên nhánh

### 4.1. Hiện trạng đã xác nhận

Tại thời điểm lập kế hoạch:

- Nhánh triển khai đã có mô-đun `library/catalog` phía máy chủ.

- Giao diện đã có các trang danh sách, chi tiết và biểu mẫu đầu sách.

- Thư mục Flyway đã có `V30__create_library_catalog.sql`.

- Plan 096 ghi nhận việc triển khai quản lý đầu sách và bản sao, bao gồm chức năng in mã vạch.

- Mô hình người dùng hiện hữu hỗ trợ tập hợp vai trò thông qua `user_role`.

- Hệ thống đã có Spring Security, JWT và cơ chế kiểm tra quyền tại máy chủ.

- Plan 095 xác định cấu trúc phản hồi lỗi hiện hữu gồm `statusCode`, `error`, `message`, `data`.

- Mô hình phân trang được Plan 096 sử dụng là `ResultPaginationDTO<T>`.

Không giả định Plan 096 đã triển khai quản lý bạn đọc, thẻ hoặc quy trình cấp quyền `LIBRARIAN`.

### 4.2. Những vấn đề phải kiểm tra lại trước khi viết mã

 1. Phiên bản và cấu trúc chính xác của MySQL đang sử dụng.

 2. Phiên bản migration thực sự đã được áp dụng trên cơ sở dữ liệu đích.

 3. Migration mới kế tiếp sau `V30` có còn trống hay không.

 4. Các đường dẫn API mới có xung đột với controller hiện hữu hay không.

 5. Vai trò `LIBRARIAN` đã được tạo trong bảng `role` hay chưa.

 6. Cơ chế cấp, thu hồi vai trò hiện tại có thể tái sử dụng đến đâu.

 7. Các thành phần dùng chung của Plan 096 đã hỗ trợ mã lỗi nghiệp vụ như thế nào.

 8. Cách hệ thống lưu thời gian, định danh người thực hiện và lịch sử thao tác.

 9. Các thành phần giao diện PrimeVue và cách tổ chức trang trong `AuthenticatedV2ShellView`.

10. Chính sách quản lý dữ liệu khi tài khoản không gắn với hồ sơ Student hoặc Teacher.

Việc tồn tại migration trong mã nguồn không chứng minh migration đó đã được áp dụng trên cơ sở dữ liệu đích.

## 5. Thiết kế và luồng xử lý đề xuất

### 5.1. Phân chia trách nhiệm

Tổ chức chức năng theo kiến trúc mô-đun hiện hữu:

```
library
├── catalog
├── patron
└── card
```

Mỗi mô-đun mới tuân theo các lớp trách nhiệm của dự án:

- Bộ điều khiển tiếp nhận và xác thực yêu cầu.

- Dịch vụ xử lý nghiệp vụ và giao dịch.

- Repository truy cập dữ liệu.

- Entity biểu diễn dữ liệu lưu trữ.

- DTO biểu diễn hợp đồng trao đổi dữ liệu.

- Cơ chế phân quyền và ghi nhận lịch sử dùng chung.

Không sử dụng JPA entity trực tiếp làm dữ liệu phản hồi API.

Mô-đun `patron` sở hữu trạng thái sử dụng thư viện. Mô-đun `card` sở hữu vòng đời thẻ, mã thẻ và chữ ký QR. Mô-đun `user` tiếp tục sở hữu tài khoản và thông tin xác thực.

### 5.2. Mô hình LibraryPatron

Bảng đề xuất:

```
library_patron
--------------
id
user_id
status
joined_at
borrowing_suspended_at
borrowing_suspended_reason
created_at
updated_at
version
```

Các trường cần bổ sung để phân biệt nguồn đình chỉ phải được chốt trong bước thiết kế chi tiết.

**Ràng buộc**

- `user_id` tham chiếu đến `app_user`.

- `user_id` là duy nhất.

- Không lưu lại mật khẩu, tên đăng nhập hoặc mã học sinh/giáo viên.

- Không xóa dây chuyền dữ liệu lịch sử khi tài khoản thay đổi.

- Hồ sơ bạn đọc được tạo trong một giao dịch có kiểm tra quyền và ràng buộc dữ liệu.

Trạng thái:

```
ACTIVE
BORROWING_SUSPENDED
CLOSED
```

**Quy tắc xử lý**

- `ACTIVE`: hồ sơ đang được phép sử dụng các nghiệp vụ thư viện theo điều kiện cụ thể.

- `BORROWING_SUSPENDED`: chặn tạo khoản mượn, gia hạn và đặt giữ mới; vẫn cho phép xem thông tin, trả sách và hủy đặt giữ.

- `CLOSED`: hồ sơ đã ngừng hoạt động, không được tạo nghiệp vụ mới.

- Thay đổi trạng thái thư viện không tác động đến trạng thái đăng nhập của `app_user`.

- Không cho phép chuyển trạng thái tùy ý nếu vi phạm lịch sử mượn sách hoặc các ràng buộc downstream.

Việc đình chỉ thủ công và đình chỉ do nợ phí phải được phân biệt. Khi một nguyên nhân được giải quyết, hệ thống không được tự mở khóa nếu vẫn còn nguyên nhân đình chỉ khác.

Plan 097 thiết kế khả năng lưu và xử lý nguyên nhân đình chỉ. Việc tự động chuyển trạng thái theo tổng phí chưa thanh toán thuộc kế hoạch triển khai Fine.

### 5.3. Mô hình LibraryCard

Tên bảng chính thức:

```
library_card
------------
id
patron_id
card_no
issued_at
expires_at
status
payload_version
policy_version
revoked_at
revoked_reason
created_at
updated_at
version
```

Trạng thái:

```
ACTIVE
EXPIRED
REVOKED
```

**Ràng buộc**

- Mỗi thẻ thuộc đúng một bạn đọc.

- `card_no` duy nhất và không được tái sử dụng.

- Một bạn đọc có thể có nhiều thẻ trong lịch sử.

- Một bạn đọc không thể có hai thẻ `ACTIVE` đồng thời.

- Thu hồi thẻ không xóa bản ghi lịch sử.

- Thẻ đã bị thu hồi không được kích hoạt lại.

- Thẻ đã hết hạn không được coi là hợp lệ chỉ vì cột trạng thái vẫn mang giá trị `ACTIVE`.

### 5.4. Quy tắc mã thẻ

Theo tài liệu v5, cấu trúc mã được đề xuất:

```
LC-{year}-{6 digit sequence}
```

Ví dụ:

```
LC-2026-000001
```

Đề xuất sử dụng bộ đếm được bảo vệ bằng giao dịch hoặc cơ chế cấp số tương đương.

Không sinh mã bằng cách lấy số lượng bản ghi hiện có cộng một.

Các tình huống cần xử lý:

- Hai yêu cầu cấp thẻ cùng lúc.

- Giao dịch bị hủy.

- Mã thẻ đã tồn tại.

- Bộ đếm vượt giới hạn sáu chữ số.

- Chuyển sang năm mới.

Mã thẻ được cấp không bắt buộc liên tục tuyệt đối, nhưng không được trùng hoặc bị tái sử dụng.

Quy tắc xử lý khi vượt giới hạn phải được chốt trước triển khai.

### 5.5. Phát hành và cấp lại thẻ

Luồng phát hành:

 1. Kiểm tra người thực hiện có quyền quản lý thẻ.

 2. Xác định hồ sơ bạn đọc.

 3. Kiểm tra trạng thái hồ sơ.

 4. Khóa bản ghi bạn đọc hoặc sử dụng cơ chế đồng bộ tương đương.

 5. Kiểm tra các thẻ đang có hiệu lực.

 6. Đối với thẻ đã hết hạn theo ngày thực tế nhưng vẫn lưu trạng thái `ACTIVE`, chuyển về `EXPIRED` trong cùng giao dịch.

 7. Nếu vẫn còn thẻ `ACTIVE` hợp lệ, từ chối phát hành thẻ mới.

 8. Lấy phiên bản chính sách thời hạn thẻ đang có hiệu lực.

 9. Sinh mã thẻ duy nhất.

10. Lưu thẻ và thông tin thời hạn.

11. Ghi nhận lịch sử.

12. Hoàn tất giao dịch.

Thời hạn mặc định là 12 tháng.

Việc tính hạn phải sử dụng múi giờ `Asia/Ho_Chi_Minh`, đồng thời xác định rõ quy tắc đối với ngày cuối tháng và năm nhuận.

Ngày hết hạn được tính là ngày cuối cùng thẻ còn hợp lệ.

**Cấp lại thẻ**

- Nếu thẻ cũ đã bị thu hồi hoặc hết hạn, có thể phát hành thẻ mới khi đáp ứng điều kiện.

- Nếu thẻ cũ đang hoạt động, phải thực hiện quy trình thu hồi hoặc thay thế được phê duyệt trước.

- Không tự động vô hiệu hóa thẻ đang hoạt động mà không có lý do và dấu vết kiểm toán.

- Thẻ mới nhận mã mới và thời hạn theo phiên bản chính sách đang có hiệu lực.

- Không sửa thời hạn và thông tin của thẻ cũ.

### 5.6. Thu hồi thẻ

Người thực hiện phải có quyền `ADMIN` hoặc `LIBRARIAN`.

Thông tin bắt buộc:

- Mã thẻ.

- Lý do thu hồi.

- Người thực hiện.

- Thời điểm thực hiện.

Thu hồi phải cập nhật trạng thái, ghi nhận lịch sử và bảo đảm chữ ký QR cũ không còn được chấp nhận.

Yêu cầu thu hồi lặp lại phải có kết quả xác định rõ, không tạo nhiều thay đổi lịch sử sai lệch.

### 5.7. Mã QR và chữ ký điện tử

Cấu trúc dữ liệu theo tài liệu v5:

```
v1|{cardNo}|{patronId}|{expEpochDay}|{signature}
```

Sử dụng HMAC-SHA256 để tạo chữ ký.

**Quy tắc bảo mật**

- Khóa bí mật chỉ được cung cấp qua cấu hình môi trường hoặc hệ thống quản lý bí mật.

- Không ghi khóa bí mật trong mã nguồn.

- Không truyền khóa bí mật xuống giao diện.

- Không ghi đầy đủ mã QR chứa chữ ký vào nhật ký.

- Chuẩn hóa cách mã hóa chuỗi đầu vào trước khi ký.

- So sánh chữ ký bằng phương pháp có thời gian xử lý không phụ thuộc vào vị trí byte sai.

- Giới hạn độ dài dữ liệu đầu vào và từ chối dữ liệu không đúng định dạng.

- Chữ ký hợp lệ không thay thế việc kiểm tra thẻ trong cơ sở dữ liệu.

**Trình tự xác thực**

1. Phân tích cấu trúc dữ liệu.

2. Kiểm tra phiên bản dữ liệu.

3. Xác thực chữ ký HMAC.

4. Kiểm tra ngày hết hạn.

5. Tìm thẻ tương ứng trong cơ sở dữ liệu.

6. Đối chiếu `cardNo`, `patronId` và ngày hết hạn.

7. Kiểm tra trạng thái `REVOKED` hoặc `EXPIRED`.

8. Kiểm tra trạng thái hồ sơ bạn đọc.

9. Trả kết quả xác thực theo quyền của người gọi.

Cần phân biệt hai kết quả:

- **Tính hợp lệ của thẻ:** chữ ký đúng, thẻ tồn tại, chưa hết hạn và chưa bị thu hồi.

- **Điều kiện thực hiện nghiệp vụ:** ngoài thẻ hợp lệ, bạn đọc còn phải đáp ứng chính sách mượn hoặc đặt giữ.

Một bạn đọc bị đình chỉ mượn có thể vẫn sở hữu thẻ hợp lệ. Hệ thống không được đánh đồng hai trạng thái này.

Ảnh QR được sinh khi có yêu cầu, ưu tiên định dạng PNG và không lưu lâu dài.

### 5.7.1. Mẫu thiết kế thẻ thư viện và Wireframe (Library Card Design & Template)

Mẫu thẻ thư viện chuẩn (tỷ lệ CR-80 85.6mm × 54mm) đã được lưu trữ trong dự án để làm căn cứ thiết kế và nghiệm thu:

- **Ảnh mẫu thẻ gốc:** [`sample-library-card.jpg`](../../wireframes/fe/library/097-library-patron-and-card/sample-library-card.jpg) (và bản sao tại [`document/application-doc/v5/assets/sample-library-card.jpg`](../../application-doc/v5/assets/sample-library-card.jpg)).
- **Wireframe tương tác và đặc tả:** [`document/wireframes/fe/library/097-library-patron-and-card/README.md`](../../wireframes/fe/library/097-library-patron-and-card/README.md) cùng [`index.html`](../../wireframes/fe/library/097-library-patron-and-card/index.html).

Quy cách thẻ theo mẫu:
- **Header:** Dải xanh dương (`#1a56db`), logo biểu trưng giáo dục (sách mở, đuốc, vòng nguyệt quế) và thông tin cơ quan chủ quản / đơn vị trường.
- **Tiêu đề thẻ:** `THẺ THƯ VIỆN` in hoa, màu xanh lá đậm (`#047857`).
- **Khối nhận diện (Trái):** Ô mã QR viền xanh, module mật độ cao chứa payload chữ ký HMAC-SHA256 (`v1|{cardNo}|{patronId}|{expEpochDay}|{signature}`); phía dưới là `MÃ THẺ` (`LIB-0001234` hoặc `LC-YYYY-NNNNNN`).
- **Khối thông tin cá nhân (Phải):** Biểu tượng icon màu xanh dương đi kèm các trường:
  - 👤 Họ và tên (ví dụ: `Nguyễn Văn A`)
  - 🎓 Lớp (ví dụ: `6A1`)
  - 🪪 Mã độc giả (ví dụ: `DG-0001234`)
  - 📅 Ngày sinh (ví dụ: `17/06/2011`)
  - 🕒 Hiệu lực thẻ (ví dụ: `31/05/2029`)
- **Họa tiết:** Hoa sen chìm góc dưới phải và dải màu vát chéo trang trí ở chân thẻ.

Ánh xạ màn hình và thành phần giao diện:
- `FE/src/views/library/LibraryMyCardView.vue`: Màn hình bạn đọc tự xem thẻ điện tử.
- `FE/src/components/library/LibraryCardPreview.vue`: Thành phần hiển thị thẻ trực quan.
- `FE/src/components/library/LibraryCardPrintDialog.vue`: Hộp thoại in thẻ chuẩn CR-80 và lưới A4 cho Thủ thư/Quản trị viên.

### 5.8. Kiểm tra quyền và điều kiện sử dụng thư viện

**Phân quyền quản trị**

| Thao tác                           | ADMIN        | LIBRARIAN    | Người dùng thông thường |
| ---------------------------------- | ------------ | ------------ | ----------------------- |
| Xem danh sách bạn đọc              | Có           | Có           | Không                   |
| Kích hoạt hồ sơ bạn đọc            | Có           | Có           | Không                   |
| Thay đổi trạng thái bạn đọc        | Có           | Có           | Không                   |
| Phát hành, thu hồi thẻ             | Có           | Có           | Không                   |
| Xác thực thẻ để phục vụ nghiệp vụ  | Có           | Có           | Không                   |
| Tự xem hồ sơ và thẻ                | Nếu có hồ sơ | Nếu có hồ sơ | Nếu có hồ sơ            |
| Cấp hoặc thu hồi vai trò LIBRARIAN | Có           | Không        | Không                   |

Việc tự xem thông tin phải dựa vào tài khoản đã xác thực tại máy chủ:

```
authenticated userId
    -> library_patron.user_id
    -> library_card
```

Không sử dụng `patronId` do trình duyệt cung cấp để suy ra quyền sở hữu.

**Điều kiện thực hiện nghiệp vụ mượn**

Dịch vụ kiểm tra điều kiện phải có khả năng từ chối khi:

- Không tồn tại hồ sơ bạn đọc.

- Hồ sơ không ở trạng thái `ACTIVE`.

- Người dùng có vai trò `LIBRARIAN`, kể cả khi còn mang vai trò khác.

- Thẻ không hợp lệ trong nghiệp vụ yêu cầu thẻ.

- Vi phạm các giới hạn mượn sách hoặc phí khi các mô-đun tương ứng được tích hợp.

Plan 097 chỉ triển khai các điều kiện đã có nguồn dữ liệu. Không giả định số khoản mượn hoặc số dư phí bằng không nếu chưa có mô-đun cung cấp dữ liệu.

### 5.9. Tích hợp vai trò LIBRARIAN

Việc cấp và thu hồi vai trò phải tuân theo các quyết định tại Plan 095:

- Chỉ `ADMIN` được thực hiện.

- Phải ghi nhận lịch sử với người thực hiện, đối tượng và thời gian.

- Không có chức năng tự cấp quyền.

- Phải kiểm tra các lượt đặt giữ `WAITING` hoặc `READY` trước khi cấp quyền.

- Nếu còn lượt đặt giữ, yêu cầu `ADMIN` xử lý trước; không tự hủy.

Do mô-đun Reservation chưa thuộc Plan 097, việc đưa chức năng cấp vai trò vào sử dụng phải được bảo vệ bởi điều kiện phụ thuộc rõ ràng.

Không được coi việc chưa có bảng Reservation là bằng chứng người dùng không có lượt đặt giữ.

Nếu chưa thể kiểm tra điều kiện này một cách tin cậy, chức năng cấp vai trò phải tiếp tục bị chặn hoặc chỉ được kích hoạt sau khi tích hợp kiểm tra tương ứng. Phần còn bị chặn phải được ghi rõ trong kết quả nghiệm thu.

## 6. Hợp đồng API, cơ sở dữ liệu và tích hợp đề xuất

### 6.1. Quy tắc đường dẫn API

Sử dụng mặc định `/api/v2`, đồng nhất với Plan 096.

Trước khi triển khai, kiểm tra lại toàn bộ đường dẫn controller trên nhánh hiện tại. Chỉ chuyển nhóm tài nguyên bị xung đột sang `/api/v2/library/**` nếu có bằng chứng xung đột.

Không thay đổi các API v1/v2/v3 hiện hữu chỉ để bổ sung chức năng thư viện.

### 6.2. API quản lý bạn đọc

| Phương thức | Đường dẫn                                   | Chức năng                                      | Quyền            |
| ----------- | ------------------------------------------- | ---------------------------------------------- | ---------------- |
| GET         | `/api/v2/library-patrons`                   | Danh sách, tìm kiếm, lọc và phân trang         | ADMIN, LIBRARIAN |
| POST        | `/api/v2/library-patrons`                   | Kích hoạt hồ sơ bạn đọc cho tài khoản hiện hữu | ADMIN, LIBRARIAN |
| GET         | `/api/v2/library-patrons/{patronId}`        | Xem hồ sơ bạn đọc                              | ADMIN, LIBRARIAN |
| PATCH       | `/api/v2/library-patrons/{patronId}/status` | Thay đổi trạng thái theo quy tắc nghiệp vụ     | ADMIN, LIBRARIAN |
| GET         | `/api/v2/library-patrons/me`                | Tự xem hồ sơ của tài khoản đang đăng nhập      | Chủ sở hữu       |

Yêu cầu kích hoạt:

```
{
  "userId": 123
}
```

API không được nhận thông tin đăng nhập mới hoặc tạo `Student`/`Teacher` cùng lúc.

API danh sách sử dụng `ResultPaginationDTO<T>` giống Plan 096.

Các tham số đề xuất:

- `keyword`: tìm kiếm theo thông tin định danh được cho phép.

- `status`: lọc trạng thái bạn đọc.

- `page`: chỉ số trang bắt đầu từ 0.

- `size`: số phần tử mỗi trang.

- `sort`: trường sắp xếp thuộc danh sách được cho phép.

Giới hạn phân trang và danh sách trường sắp xếp phải đồng nhất với quy ước đã chốt tại Plan 096, trừ khi có lý do nghiệp vụ cụ thể.

### 6.3. API quản lý thẻ

| Phương thức | Đường dẫn                               | Chức năng                     | Quyền                            |
| ----------- | --------------------------------------- | ----------------------------- | -------------------------------- |
| POST        | `/api/v2/library-cards`                 | Phát hành thẻ                 | ADMIN, LIBRARIAN                 |
| GET         | `/api/v2/library-cards/me`              | Xem thẻ hiện tại của bản thân | Chủ sở hữu                       |
| GET         | `/api/v2/library-cards/{cardNo}/qr.png` | Lấy ảnh QR                    | ADMIN, LIBRARIAN hoặc chủ sở hữu |
| POST        | `/api/v2/library-cards/verify`          | Xác thực dữ liệu QR           | ADMIN, LIBRARIAN                 |
| POST        | `/api/v2/library-cards/{cardNo}/revoke` | Thu hồi thẻ                   | ADMIN, LIBRARIAN                 |

Các thao tác cấp lại và lấy chi tiết thẻ theo hồ sơ phải được xác định trong hợp đồng API trước khi triển khai, tránh tạo các endpoint chồng chéo chức năng.

**Các API chưa thực hiện trong Plan 097**

```
GET /api/v2/library-patrons/{patronId}/loans
GET /api/v2/library-patrons/{patronId}/fines
GET /api/v2/library-cards/{cardNo}/card.pdf
```

Hai API đầu phụ thuộc mô-đun mượn sách và phí. API PDF không nằm trong phạm vi cốt lõi đã duyệt.

### 6.4. Dữ liệu phản hồi

Thông tin bạn đọc tối thiểu:

```
patronId
userId
displayName
status
joinedAt
suspensionReasons
currentCard
```

Các trường `activeLoanCount` và `unpaidFineTotal` chỉ được bổ sung khi có nguồn dữ liệu đáng tin cậy.

Thông tin thẻ:

```
cardNo
patronId
status
issuedAt
expiresAt
payloadVersion
policyVersion
```

Không đưa khóa ký, dữ liệu xác thực nội bộ hoặc thông tin nhạy cảm không cần thiết vào phản hồi.

### 6.5. Mã lỗi nghiệp vụ

Ưu tiên sử dụng đúng các mã đã được định nghĩa trong `ErrorContract.md`:

| Mã lỗi                       | Ý nghĩa                                |
| ---------------------------- | -------------------------------------- |
| `PATRON_NOT_FOUND`           | Không tìm thấy bạn đọc                 |
| `PATRON_ALREADY_EXISTS`      | Tài khoản đã có hồ sơ bạn đọc          |
| `PATRON_BORROWING_SUSPENDED` | Bạn đọc đang bị đình chỉ mượn          |
| `LIBRARY_RESOURCE_FORBIDDEN` | Không có quyền thực hiện hoặc truy cập |
| `CARD_ALREADY_ACTIVE`        | Bạn đọc đã có thẻ đang hoạt động       |
| `CARD_PAYLOAD_MALFORMED`     | Dữ liệu QR không đúng cấu trúc         |
| `CARD_SIGNATURE_INVALID`     | Chữ ký QR không hợp lệ                 |
| `CARD_EXPIRED`               | Thẻ đã hết hạn                         |
| `CARD_REVOKED`               | Thẻ đã bị thu hồi                      |

Nếu phát sinh tình huống chưa có mã phù hợp, đề xuất mã mới trong bước chốt hợp đồng. Không đặt lại tên mã lỗi hiện hữu chỉ vì sở thích của mô-đun mới.

Phản hồi lỗi phải giữ tương thích với cấu trúc đã có tại dự án, bao gồm cơ chế mở rộng mã lỗi của Plan 096.

Không đưa thông báo SQL, tên ràng buộc cơ sở dữ liệu hoặc chi tiết xử lý nội bộ ra ngoài API.

### 6.6. Ràng buộc cơ sở dữ liệu

Các ràng buộc bắt buộc:

- Khóa ngoại `library_patron.user_id` đến `app_user`.

- Ràng buộc duy nhất trên `library_patron.user_id`.

- Khóa ngoại `library_card.patron_id` đến `library_patron`.

- Ràng buộc duy nhất trên `library_card.card_no`.

- Ràng buộc bảo đảm tối đa một thẻ `ACTIVE` cho mỗi bạn đọc.

- Chỉ mục hỗ trợ tìm kiếm hồ sơ, thẻ và tra cứu theo mã thẻ.

- Không xóa dây chuyền lịch sử sử dụng thư viện.

**Phương án bảo vệ thẻ ACTIVE đề xuất**

Sử dụng cột phát sinh chỉ nhận `patron_id` khi thẻ ở trạng thái `ACTIVE`, kết hợp ràng buộc duy nhất trên cột đó.

Đây là phương án cần xác minh, chưa phải câu lệnh SQL được phê duyệt.

Phải kiểm tra trên phiên bản MySQL tương thích với môi trường triển khai thực tế.

Đồng thời, dịch vụ phát hành phải đồng bộ các yêu cầu trên cùng một bạn đọc bằng giao dịch và cơ chế khóa phù hợp. Không chỉ dựa vào thao tác kiểm tra thẻ đang tồn tại ở tầng Java.

### 6.7. Chính sách thời hạn thẻ

Chính sách thời hạn được quản lý theo phiên bản.

- Giá trị mặc định: 12 tháng.

- `ADMIN` và `LIBRARIAN` có quyền điều chỉnh theo quyết định Plan 095.

- Mọi thay đổi chính sách phải có lịch sử.

- Thẻ mới sử dụng chính sách có hiệu lực tại thời điểm phát hành.

- Thẻ đã phát hành giữ nguyên ngày hết hạn và phiên bản chính sách cũ.

Plan 097 cần tái sử dụng mô hình chính sách dùng chung nếu đã được tạo. Nếu chưa có, việc chọn bảng và giao diện cấu hình chính sách phải được chốt để tránh tạo hai cơ chế quản lý thời hạn khác nhau giữa Card và Circulation.

## 7. Các tệp dự kiến thay đổi

Danh sách dưới đây là phương án tổ chức ban đầu, không phải cam kết rằng mọi tệp đều đã tồn tại. Đường dẫn cuối cùng phải theo cấu trúc thực tế của nhánh triển khai.

| Khu vực   | Tệp hoặc nhóm tệp dự kiến                          | Mục đích                                               |
| --------- | -------------------------------------------------- | ------------------------------------------------------ |
| Máy chủ   | `library/patron/**`                                | Entity, repository, service, controller và DTO bạn đọc |
| Máy chủ   | `library/card/**`                                  | Entity, repository, service, controller và DTO thẻ     |
| Máy chủ   | `library/**/security` hoặc thành phần tương đương  | Kiểm tra quyền và điều kiện sử dụng thư viện           |
| Máy chủ   | Thành phần xử lý mã lỗi dùng chung                 | Tích hợp mã lỗi ổn định                                |
| Máy chủ   | Thành phần ghi nhận lịch sử hiện hữu               | Lưu dấu vết thay đổi trạng thái và thẻ                 |
| Flyway    | Migration mới sau phiên bản hiện tại               | Tạo bảng, khóa ngoại, chỉ mục và ràng buộc             |
| Giao diện | `FE/src/views/library/LibraryPatronListView.vue`   | Danh sách và tìm kiếm bạn đọc                          |
| Giao diện | `FE/src/views/library/LibraryPatronDetailView.vue` | Chi tiết hồ sơ bạn đọc                                 |
| Giao diện | `FE/src/views/library/LibraryMyCardView.vue`       | Thẻ thư viện của người dùng                            |
| Giao diện | `FE/src/components/library/**`                     | Biểu mẫu, bảng, hộp thoại và thành phần hiển thị thẻ   |
| Giao diện | `FE/src/services/library/**`                       | Gọi API có định kiểu                                   |
| Giao diện | `FE/src/types/library/**`                          | Kiểu dữ liệu cho bạn đọc và thẻ                        |
| Giao diện | Bộ định tuyến và thanh điều hướng hiện hữu         | Tích hợp các trang thư viện                            |
| Tài liệu  | Plan 097, Dev Note 097 và các tệp tổng hợp         | Ghi nhận thiết kế, kết quả triển khai và nghiệm thu    |

Không tạo tầng dùng chung mới nếu có thể sử dụng các thành phần đã được Plan 096 chuẩn hóa.

## 8. Trình tự triển khai sau khi được phê duyệt

### Giai đoạn 1 — Kiểm tra điều kiện triển khai

1. Xác minh mã nguồn và migration hiện tại.

2. Kiểm tra mô hình `User`, `Role`, `UserPrincipal` và phân quyền.

3. Xác minh phiên bản MySQL và cơ chế bảo đảm một thẻ đang hoạt động.

4. Đối chiếu các đường dẫn API.

5. Chốt mô hình đình chỉ, chính sách thời hạn và cơ chế mã thẻ.

6. Chốt hợp đồng request, response và mã lỗi.

7. Xác định các ràng buộc phụ thuộc mô-đun Reservation.

**Điều kiện hoàn thành:** Các điểm còn mở ảnh hưởng đến phạm vi được triển khai đã có quyết định rõ ràng.

### Giai đoạn 2 — Cơ sở dữ liệu và mô hình nghiệp vụ

1. Viết migration.

2. Tạo entity và repository.

3. Tạo các enum và DTO cần thiết.

4. Bổ sung ràng buộc dữ liệu.

5. Kiểm thử migration trên MySQL.

6. Kiểm tra khả năng nâng cấp từ schema hiện hữu.

**Điều kiện hoàn thành:** Các ràng buộc duy nhất và khóa ngoại được chứng minh bằng kiểm thử cơ sở dữ liệu.

### Giai đoạn 3 — Quản lý bạn đọc

1. Kích hoạt hồ sơ bạn đọc.

2. Tìm kiếm và xem chi tiết.

3. Kiểm tra quyền sở hữu.

4. Thay đổi trạng thái và ghi nhận lý do.

5. Cung cấp dịch vụ đánh giá điều kiện mượn sách.

6. Tích hợp những thành phần quản lý vai trò đã đủ điều kiện triển khai.

**Điều kiện hoàn thành:** Quy tắc định danh, phân quyền và trạng thái bạn đọc hoạt động đúng.

### Giai đoạn 4 — Quản lý thẻ và QR

1. Sinh mã thẻ.

2. Phát hành thẻ.

3. Thu hồi và cấp lại thẻ.

4. Tính thời hạn theo phiên bản chính sách.

5. Sinh chữ ký HMAC.

6. Xác thực QR.

7. Sinh ảnh QR PNG.

8. Ghi nhận lịch sử thao tác.

9. Kiểm thử cấp thẻ đồng thời.

**Điều kiện hoàn thành:** Không thể tạo hai thẻ `ACTIVE` cho cùng bạn đọc và không thể sử dụng thẻ hết hạn hoặc đã thu hồi.

### Giai đoạn 5 — Giao diện

1. Thiết kế giao diện theo các thành phần hiện có.

2. Tạo dịch vụ API và kiểu dữ liệu.

3. Tạo danh sách và trang chi tiết bạn đọc.

4. Tạo các hộp thoại phát hành, thu hồi và thay đổi trạng thái.

5. Tạo trang tự xem thẻ.

6. Hiển thị, tải ảnh QR.

7. Tích hợp điều hướng và kiểm tra quyền.

8. Kiểm thử giao diện và các trường hợp lỗi.

**Điều kiện hoàn thành:** Giao diện thống nhất với phần quản lý trường học và hoạt động theo hợp đồng API đã chốt.

### Giai đoạn 6 — Kiểm thử tích hợp và bàn giao

1. Chạy kiểm thử máy chủ.

2. Chạy kiểm thử cơ sở dữ liệu MySQL.

3. Chạy kiểm thử bảo mật.

4. Chạy kiểm thử giao diện, lint và build.

5. Kiểm tra tương thích với Catalog và các API học vụ.

6. Cập nhật tài liệu API và Dev Note.

7. Chuẩn bị hợp đồng tích hợp cho Plan 098.

**Điều kiện hoàn thành:** Các tiêu chí nghiệm thu bắt buộc có bằng chứng và không còn lỗi chặn triển khai trong phạm vi được phê duyệt.

## 9. Kế hoạch kiểm thử và xác minh

### 9.1. Kiểm thử nghiệp vụ

| Nhóm              | Tình huống bắt buộc                                         | Kết quả mong đợi                                  |
| ----------------- | ----------------------------------------------------------- | ------------------------------------------------- |
| Kích hoạt bạn đọc | Tài khoản hợp lệ, chưa có hồ sơ                             | Tạo thành công một hồ sơ                          |
| Kích hoạt bạn đọc | Tài khoản đã có hồ sơ                                       | Từ chối và trả mã lỗi ổn định                     |
| Định danh         | Hai yêu cầu tạo hồ sơ cho cùng người dùng                   | Không xuất hiện hồ sơ trùng                       |
| Phân quyền        | Người dùng thường gọi API quản lý                           | Bị từ chối                                        |
| Quyền sở hữu      | Người dùng truy cập hồ sơ của người khác qua API tự phục vụ | Bị từ chối                                        |
| Trạng thái        | Đình chỉ mượn                                               | Chặn nghiệp vụ tạo mới nhưng không khóa đăng nhập |
| Vai trò           | Người dùng mang nhiều vai trò, trong đó có LIBRARIAN        | Không đủ điều kiện tạo mượn/gia hạn/đặt giữ mới   |
| Phát hành thẻ     | Bạn đọc chưa có thẻ hợp lệ                                  | Phát hành thành công                              |
| Phát hành thẻ     | Đã có thẻ ACTIVE hợp lệ                                     | Từ chối cấp trùng                                 |
| Cấp lại           | Thẻ đã hết hạn hoặc bị thu hồi                              | Cấp thẻ mới và giữ lịch sử                        |
| Thu hồi           | QR của thẻ vừa bị thu hồi                                   | Xác thực thất bại                                 |
| Hết hạn           | Đúng ngày hết hạn                                           | Thẻ vẫn còn hiệu lực                              |
| Hết hạn           | Ngày tiếp theo                                              | Thẻ không còn hiệu lực                            |
| Chính sách        | Thay đổi thời hạn mặc định                                  | Không sửa thẻ đã phát hành                        |
| Chữ ký            | Dữ liệu QR bị thay đổi                                      | Xác thực thất bại                                 |
| QR                | Sinh ảnh cho thẻ hợp lệ                                     | Trả nội dung PNG phù hợp                          |

### 9.2. Kiểm thử xử lý đồng thời trên MySQL

Các trường hợp tối thiểu:

- Hai yêu cầu kích hoạt cùng `userId`.

- Nhiều yêu cầu phát hành thẻ cho cùng `patronId`.

- Phát hành thẻ đồng thời với thu hồi thẻ.

- Cấp lại thẻ khi thẻ cũ đã hết hạn nhưng trạng thái lưu vẫn là `ACTIVE`.

- Hai yêu cầu cấp mã thẻ cạnh tranh cùng bộ đếm.

- Giao dịch bị hủy giữa quá trình cấp thẻ.

- Lỗi ràng buộc duy nhất và cơ chế chuyển thành mã lỗi nghiệp vụ.

**Điều kiện bắt buộc:** Sau mọi giao dịch đồng thời, mỗi bạn đọc có tối đa một bản ghi thẻ `ACTIVE`.

Phải sử dụng MySQL hoặc môi trường kiểm thử tương thích với phiên bản MySQL đích. Kiểm thử H2 không được xem là bằng chứng cho ràng buộc và cơ chế khóa đặc thù MySQL.

### 9.3. Kiểm thử bảo mật

- Truy cập khi chưa đăng nhập.

- Truy cập không đủ quyền.

- Truy cập trái quyền sở hữu.

- Dữ liệu QR sai cấu trúc.

- Chữ ký sai.

- Phiên bản QR không được hỗ trợ.

- Thẻ không tồn tại.

- Thẻ đã bị thu hồi.

- Thẻ đã hết hạn.

- Cố tình sửa `cardNo`, `patronId` hoặc ngày hết hạn.

- Kiểm tra khóa bí mật không xuất hiện trong phản hồi hoặc nhật ký.

- Kiểm tra ảnh QR không bị truy cập bởi người không có quyền.

### 9.4. Kiểm thử giao diện

Giao diện cần có các trạng thái:

- Đang tải.

- Có dữ liệu.

- Không có dữ liệu.

- Không có quyền.

- Lỗi máy chủ.

- Lỗi dữ liệu nhập.

- Xung đột khi lưu.

- Thẻ đang hoạt động.

- Thẻ hết hạn.

- Thẻ bị thu hồi.

- Không có hồ sơ bạn đọc.

- Không có thẻ hiện tại.

Các thao tác lưu phải ngăn gửi trùng ngoài ý muốn và xử lý phản hồi chậm hoặc lỗi mạng.

Đối với ảnh QR, phải sử dụng cơ chế tải phù hợp với phiên xác thực hiện hữu. Nếu ảnh được lấy bằng yêu cầu có `Authorization` header, giao diện không được giả định thẻ `<img>` tự thêm header đó.

### 9.5. Kiểm thử hồi quy

Không được làm hỏng:

- Đăng nhập và quản lý phiên.

- Các API Student v1/v2/v3.

- Vai trò và quyền học vụ.

- Quản lý đầu sách và bản sao của Plan 096.

- Định dạng phản hồi lỗi của các API cũ.

- Giao diện điều hướng hiện hữu.

- Flyway migration và dữ liệu đã tồn tại.

Mức bao phủ kiểm thử phải tuân theo tiêu chí chung của v5: tối thiểu 70% cho phần dịch vụ cốt lõi hoặc ngưỡng cao hơn nếu dự án đang áp dụng. Phải báo cáo cụ thể phạm vi được đo, không sử dụng tỷ lệ toàn dự án để che phần chức năng mới thiếu kiểm thử.

Mọi kết quả kiểm thử phải ghi rõ **ĐẠT**, **KHÔNG ĐẠT**, **CHƯA CHẠY** hoặc **BỊ CHẶN** dựa trên bằng chứng thực tế.

## 10. Các điểm cần chốt, rủi ro và kết quả bàn giao

### 10.1. Các điểm cần chốt trước triển khai

| Mã     | Nội dung                                                           | Trạng thái                         |
| ------ | ------------------------------------------------------------------ | ---------------------------------- |
| P97-01 | Phạm vi và quyền triển khai Plan 097                               | Đã được người dùng phê duyệt       |
| P97-02 | Định dạng mã thẻ, bộ đếm theo năm và xử lý vượt giới hạn           | Lựa chọn kỹ thuật trong implementation; ghi rõ quyết định thực tế |
| P97-03 | Cấp lại thẻ đang hoạt động                                         | Hợp đồng đã chốt: reissue nguyên tử, thu hồi thẻ cũ và tạo thẻ mới cùng transaction |
| P97-04 | Lưu nhiều nguyên nhân đình chỉ                                     | Lựa chọn kỹ thuật trong implementation; không mở khóa khi còn nguyên nhân khác |
| P97-05 | Cơ chế ràng buộc một thẻ ACTIVE trên MySQL                         | Phải xác minh tương thích MySQL; runtime DB đích vẫn chưa có bằng chứng |
| P97-06 | Hợp đồng API, DTO và các mã lỗi bổ sung                            | Chốt theo hợp đồng BE↔FE đã phối hợp; ghi các sai khác thực tế trong Dev Note |
| P97-07 | Mô hình chính sách thời hạn dùng chung                             | Tái sử dụng nếu có; nếu không, dùng lựa chọn kỹ thuật phù hợp và ghi trong Dev Note |
| P97-08 | Quy trình cấp LIBRARIAN khi Reservation chưa triển khai            | Role assignment phụ thuộc khả năng kiểm tra Reservation; deferral nếu chưa có nguồn dữ liệu |
| P97-09 | Patron cho user không có Student/Teacher                           | Đã giải quyết bởi quyết định mọi user trừ ADMIN/LIBRARIAN có thể đủ điều kiện khi patron hợp lệ |

Các quyết định về vai trò `LIBRARIAN`, cách tính ngày hết hạn, tên bảng `library_card` và việc không lưu tệp QR lâu dài đã được phê duyệt trong Plan 095; không đưa vào danh sách cần xin duyệt lại.

### 10.2. Rủi ro chính

**Trùng thẻ khi xử lý đồng thời**

Giảm thiểu bằng giao dịch, khóa phù hợp và ràng buộc duy nhất tại cơ sở dữ liệu.

**Thẻ hết hạn nhưng dữ liệu chưa cập nhật**

Luôn kiểm tra thời hạn thực tế khi xác thực, đồng thời chuẩn hóa trạng thái trước khi cấp thẻ mới.

**Mở khóa sai hồ sơ bị đình chỉ**

Lưu và xử lý nguyên nhân đình chỉ độc lập, không mở khóa toàn bộ chỉ vì một khoản phí được giải quyết.

**Sai quyền do người dùng có nhiều vai trò**

Kiểm tra toàn bộ tập hợp vai trò, đặc biệt quy tắc loại trừ `LIBRARIAN`.

**Cấp vai trò khi chưa thể kiểm tra Reservation**

Chặn việc kích hoạt chức năng liên quan cho đến khi bảo đảm được điều kiện nghiệp vụ, không mặc định cho phép.

**Rò rỉ dữ liệu hoặc khóa ký QR**

Giới hạn dữ liệu phản hồi, quyền tải ảnh, nội dung nhật ký và quyền truy cập cấu hình bí mật.

**Không tương thích với Plan 096**

Tái sử dụng hợp đồng API, cách phân trang, mã lỗi, giao diện và thành phần dùng chung đã được chuẩn hóa.

### 10.3. Kết quả bàn giao

Sau khi Plan 097 được phê duyệt, triển khai và kiểm thử thành công, kết quả phải bao gồm:

 1. Các bảng `library_patron` và `library_card` cùng đầy đủ ràng buộc.

 2. Chức năng kích hoạt, tra cứu và quản lý trạng thái bạn đọc.

 3. Chức năng phát hành, thu hồi và cấp lại thẻ.

 4. Mã thẻ duy nhất và ảnh QR có chữ ký HMAC-SHA256.

 5. Cơ chế xác thực thẻ và kiểm tra điều kiện sử dụng thư viện.

 6. Phân quyền và kiểm tra quyền sở hữu ở máy chủ.

 7. Giao diện quản lý bạn đọc và trang tự xem thẻ.

 8. Kiểm thử đơn vị, tích hợp, bảo mật và xử lý đồng thời có bằng chứng.

 9. Tài liệu API được cập nhật.

10. Dev Note 097 và các tệp tổng hợp kế hoạch được cập nhật.

11. Hợp đồng tích hợp cho Plan 098.

Chỉ đánh dấu **HOÀN THÀNH** khi các nội dung bắt buộc trong phạm vi được phê duyệt có bằng chứng nghiệm thu. Những chức năng còn phụ thuộc Reservation, Fine hoặc quyết định triển khai riêng phải được ghi rõ, không báo hoàn thành thay.

## 11. Nguyên tắc thực hiện

- Không viết mã khi Plan 097 chưa được phê duyệt.

- Không sửa dữ liệu production trong phạm vi lập kế hoạch.

- Không tự động commit, push hoặc viết lại lịch sử Git.

- Không thay đổi các API học vụ không liên quan.

- Không tạo hệ thống đăng nhập hoặc cơ sở dữ liệu độc lập.

- Không tự tạo quy tắc nghiệp vụ mới khi chưa được chốt.

- Không sử dụng kết quả kiểm thử giả hoặc suy diễn rằng kiểm thử đã đạt.

- Không triển khai các nghiệp vụ thuộc Plan 098 trở đi một cách ngầm định.

- Giao diện phải đồng nhất với ứng dụng quản lý trường học hiện tại.

- Tất cả kết quả triển khai phải được đối chiếu với trạng thái thực tế của mã nguồn và cơ sở dữ liệu.

**Trạng thái cuối của tài liệu:** ĐÃ PHÊ DUYỆT — Plan 097 được triển khai theo phạm vi và làm rõ tại §1.3; các lựa chọn kỹ thuật và bằng chứng còn thiếu phải được ghi trong Dev Note, không làm thay đổi nghiệp vụ đã duyệt.

**Thông điệp commit đề xuất sau khi tài liệu được duyệt:**

`docs(plan): add plan 097 for library patron and card`
