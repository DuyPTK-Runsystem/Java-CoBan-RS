# Access, Quality and Acceptance Module

## 23. Ma trận quyền

| Chức năng | Giáo vụ | GVCN | GVBM | Học sinh |
|---|:---:|:---:|:---:|:---:|
| Quản lý năm học/học kỳ | Quản lý | Xem | Xem | Xem hạn chế |
| Quản lý khối/lớp | Quản lý | Xem lớp mình | Xem lớp được dạy | Xem lớp mình |
| Xếp/chuyển lớp | Quản lý | Xem | Xem | Xem bản thân |
| Quản lý giáo viên/phân công | Quản lý | Xem | Xem | Xem hạn chế |
| Điểm danh | Toàn quyền | Nhập lớp mình | Không trong MVP | Xem bản thân |
| Cấu hình cột điểm | Kiểm soát | Không | Môn mình | Không |
| Nhập điểm | Toàn quyền | Theo phân công | Môn mình | Không |
| Duyệt sửa điểm | Có | Không | Không | Không |
| Khóa học kỳ | Có | Không | Không | Không |
| Nhập điểm thi lại | Theo quyền | Không | Theo quyền được cấp | Không |
| Xem bảng điểm | Toàn trường | Lớp mình | Môn/lớp mình | Bản thân |

## 24. Logical data model

`BR-COMMON-005`: Hệ thống phải hỗ trợ tối thiểu các thực thể logic sau:

| Nhóm | Thực thể |
|---|---|
| Tổ chức học tập | `Grade`, `AcademicYear`, `Semester`, `SchoolClass`, `TeachingSession` |
| Con người | `Student`, `Teacher`, `User` |
| Xếp lớp và phân công | `StudentEnrollment`, `TeachingAssignment` |
| Môn và điểm | `Subject`, `SubjectOffering`, `AssessmentColumn`, `ScoreEntry` |
| Điểm danh | `AttendanceException` |
| Tổng kết | `StudentSubjectSemesterSummary`, `StudentSemesterSummary`, `StudentSubjectYearSummary`, `StudentYearSummary` |
| Sửa điểm | `ScoreChangeRequest` |
| Thi lại | `SubjectRetakeResult` |
| Tính nền | `CalculationTask`, `BatchJobHistory` |
| Kiểm toán | `AuditLog` |

`NFR-MAINTAINABILITY-001`: Tên bảng vật lý có thể thay đổi trong technical design nhưng phải bảo toàn các quan hệ và quy tắc nghiệp vụ của baseline.

## 25. Non-functional requirements

### 25.1. Hiệu năng

- `NFR-PERFORMANCE-001`: Tuyệt đối không tính điểm trung bình trong HTTP request.
- `NFR-PERFORMANCE-002`: HTTP request ghi điểm phải hoàn tất sau khi lưu dữ liệu và tạo task, không chờ worker.
- `NFR-PERFORMANCE-003`: Danh sách sử dụng phân trang phía server, mặc định 10.
- `NFR-PERFORMANCE-004`: Filter sử dụng AND; sort dùng allow-list.
- `NFR-PERFORMANCE-005`: Batch import, kết chuyển, khóa tự động và tính hàng loạt sử dụng background processing phù hợp.
- `NFR-PERFORMANCE-006`: Calculation task trùng phải được giảm thiểu để tránh tính lặp không cần thiết.

### 25.2. Reliability

- `NFR-RELIABILITY-001`: Calculation task phải lưu bền vững, retry được và idempotent.
- `NFR-RELIABILITY-002`: Ghi điểm và tạo task phải cùng transaction.
- `NFR-RELIABILITY-003`: Không đánh dấu `FINISH` khi calculation thất bại.
- `NFR-RELIABILITY-004`: Dùng version để ngăn kết quả cũ ghi đè kết quả mới.
- `NFR-RELIABILITY-005`: Các thao tác chuyển lớp, thay phân công, duyệt sửa điểm và áp dụng thi lại phải đảm bảo transaction.

### 25.3. Security

- `NFR-SECURITY-001`: JWT stateless tiếp tục được sử dụng.
- `NFR-SECURITY-002`: Không cho đăng ký tự do tài khoản giáo viên hoặc học sinh.
- `NFR-SECURITY-003`: Quyền giáo viên dựa trên phân công thực tế.
- `NFR-SECURITY-004`: `401` dùng cho chưa hoặc không còn xác thực.
- `NFR-SECURITY-005`: `403` dùng cho đã xác thực nhưng không đủ quyền.
- `NFR-SECURITY-006`: Không lưu password hoặc password hash trong dữ liệu frontend.

### 25.4. Auditability

Bắt buộc audit:

- `NFR-AUDITABILITY-001`: Chuyển lớp.
- `NFR-AUDITABILITY-002`: Thay đổi phân công.
- `NFR-AUDITABILITY-003`: Nhập và sửa điểm.
- `NFR-AUDITABILITY-004`: Duyệt request sửa điểm.
- `NFR-AUDITABILITY-005`: Thay đổi trọng số môn kỹ năng.
- `NFR-AUDITABILITY-006`: Khóa hoặc mở học kỳ.
- `NFR-AUDITABILITY-007`: Điều chỉnh ngoại lệ điểm danh.
- `NFR-AUDITABILITY-008`: Nhập hoặc sửa điểm thi lại.
- `NFR-AUDITABILITY-009`: Chạy lại calculation task.

### 25.5. Usability

- `NFR-USABILITY-001`: UI phải phân biệt rõ `IN_PROGRESS` và `FINISH`.
- `NFR-USABILITY-002`: Không hiển thị ô chưa nhập như điểm 0.
- `NFR-USABILITY-003`: Các cảnh báo không chặn thao tác phải được trình bày rõ là warning.
- `NFR-USABILITY-004`: Lỗi validation phải chỉ ra trường hoặc quy tắc bị vi phạm.
- `NFR-USABILITY-005`: Dữ liệu lịch sử phải có thể truy cập nhưng không gây nhầm với dữ liệu hiện hành.

## 26. Acceptance scenarios trọng yếu

### AC-01 — Điểm danh mặc định

**Given** học sinh đang thuộc lớp và buổi sáng là buổi học hợp lệ  
**And** không có `AttendanceException`  
**When** xem điểm danh buổi sáng  
**Then** học sinh được hiển thị là `PRESENT`  
**And** hệ thống không cần một bản ghi `PRESENT` vật lý.

### AC-02 — Không tính ngày nghỉ là có mặt

**Given** ngày được chọn là ngày nghỉ  
**When** xem báo cáo chuyên cần  
**Then** buổi đó không được tính vào tổng số buổi học  
**And** không suy ra học sinh là `PRESENT`.

### AC-03 — Tính Đtbmh từ dữ liệu hiện có

**Given** học sinh có KTTT = 8 và KTĐK = 7  
**And** KTCK chưa nhập  
**When** background calculation hoàn tất  
**Then** `Đtbmh = 7.3`  
**And** ô KTCK chưa nhập không được xem là 0.

### AC-04 — HTTP request không tính điểm

**Given** giáo viên nhập một điểm mới  
**When** HTTP request thành công  
**Then** điểm nguồn được lưu  
**And** tổng kết chuyển sang `IN_PROGRESS`  
**And** calculation task được tạo  
**And** HTTP request không tính `Đtbmh`, `Đtbhk`, `ĐtbmhCN` hoặc `Đtbcn`.

### AC-05 — Hoàn tất background calculation

**Given** bảng điểm đang `IN_PROGRESS`  
**When** worker tính thành công từ phiên bản dữ liệu mới nhất  
**Then** các kết quả được lưu  
**And** `calculatedVersion = sourceVersion`  
**And** trạng thái chuyển sang `FINISH`.

### AC-06 — Ngăn worker cũ ghi đè

**Given** worker đang tính phiên bản 5  
**And** dữ liệu mới làm `sourceVersion = 6`  
**When** worker phiên bản 5 hoàn tất  
**Then** worker không được chuyển tổng kết sang `FINISH` cho phiên bản 6.

### AC-07 — Sửa điểm trong 10 ngày

**Given** giáo viên đang được phân công  
**And** chưa quá 10 ngày từ lần nhập đầu  
**And** học kỳ chưa khóa  
**When** giáo viên sửa điểm  
**Then** không cần request  
**And** vẫn có audit log và calculation task.

### AC-08 — Sửa điểm sau thời hạn

**Given** đã quá 10 ngày hoặc học kỳ đã khóa  
**When** giáo viên muốn sửa điểm  
**Then** giáo viên phải tạo request  
**And** chỉ sau khi một giáo vụ duyệt thì điểm mới được áp dụng.

### AC-09 — Chuyển lớp

**Given** học sinh đã có điểm ở lớp cũ  
**When** học sinh chuyển sang lớp mới  
**Then** toàn bộ điểm được giữ nguyên  
**And** giáo viên lớp mới được xem điểm tích lũy  
**And** bảng điểm có ghi chú chuyển lớp.

### AC-10 — Cảnh báo sĩ số

**Given** sĩ số lớp sau xếp lớp lệch quá 20% so với trung bình khối  
**When** giáo vụ xác nhận xếp lớp  
**Then** hệ thống hiển thị warning  
**And** vẫn cho phép hoàn thành thao tác.

### AC-11 — Một giáo viên cho một môn/lớp/học kỳ

**Given** đã có một phân công active cho bộ `{môn, lớp, học kỳ}`  
**When** giáo vụ tạo phân công active thứ hai  
**Then** hệ thống từ chối  
**And** yêu cầu kết thúc phân công cũ trước.

### AC-12 — Thi lại một môn

**Given** học sinh có `ĐtbmhCN` môn Toán là 4.0  
**And** học sinh được thi lại môn Toán  
**When** `Đtlmh = 6.0` được nhập  
**Then** tổng kết chuyển sang `IN_PROGRESS`  
**And** HTTP request không tính lại điểm  
**And** background worker đặt `ĐtbmhCN` chính thức môn Toán bằng 6.0  
**And** background worker tính lại `Đtbcn`  
**And** bảng điểm hiển thị điểm trước thi lại, `Đtlmh` và nguồn `RETAKE`.

### AC-13 — Ngăn thi lại lần hai cùng môn

**Given** học sinh đã có một kết quả thi lại hợp lệ cho môn Toán trong năm học  
**When** người dùng tạo kết quả thi lại hợp lệ thứ hai cho cùng môn và năm học  
**Then** hệ thống từ chối.

### AC-14 — Khóa học kỳ tự động

**Given** đã qua 45 ngày từ ngày kết thúc học kỳ  
**When** tác vụ khóa nền chạy  
**Then** học kỳ chuyển sang `LOCKED`  
**And** báo cáo dữ liệu chưa hoàn chỉnh được tạo  
**And** giáo viên không còn được sửa điểm trực tiếp.

## 27. Change Request bắt buộc trước implementation liên quan

Một CR riêng phải định nghĩa:

- Quy tắc xếp loại học sinh.
- Quy tắc xếp hạng học sinh.
- Điều kiện lên lớp, lưu ban và hoàn thành THCS.
- Điều kiện học sinh phải thi lại.
- Môn nào phải thi lại và quy trình phê duyệt danh sách thi lại.
- Ảnh hưởng của kết quả thi lại tới quyết định lên lớp.

Baseline hiện tại chỉ định nghĩa cách lưu và tính điểm thi lại sau khi học sinh/môn học đã được xác định là phải thi lại.

## 28. Definition of Done cho requirement

Một module chỉ sẵn sàng implementation khi:

1. Functional requirements và business rules đã được ánh xạ sang use case hoặc user story.
2. Database design bảo toàn lịch sử và các unique constraints.
3. API contract xác định quyền, validation, error và trạng thái tính nền.
4. UI thể hiện đúng warning, trạng thái `IN_PROGRESS`/`FINISH` và dữ liệu lịch sử.
5. Test case bao phủ các acceptance scenarios liên quan.
6. Không có phép tính điểm trung bình nào nằm trong HTTP request.
7. Calculation task có retry, idempotency và version protection.
8. Audit log được xác định cho mọi thao tác bắt buộc.
9. Các nội dung phụ thuộc CR chưa được tự suy diễn hoặc hard-code.
