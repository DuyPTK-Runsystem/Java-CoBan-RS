# Common and Authentication Module

## 0. Document control

| Thuộc tính | Giá trị |
|---|---|
| Tên tài liệu | THCS Student Management System — Requirement Baseline |
| Phiên bản | 1.1 |
| Ngày cập nhật | 20/08/2026 |
| Trạng thái | Baseline |
| Phạm vi tổ chức | Một trường trung học cơ sở |
| Người dùng MVP | Quản trị viên, giáo vụ, giáo viên, học sinh |
| Người dùng giai đoạn sau | Phụ huynh |
| Kiến trúc hiện tại | Spring Boot REST, Vue 3/PrimeVue, MySQL, Spring Data JPA, Spring Batch |

### 0.1. Mục đích tài liệu

Tài liệu này là baseline nghiệp vụ cho việc mở rộng project quản lý học sinh hiện tại thành hệ thống quản lý học tập cho một trường THCS. Baseline xác định phạm vi, tác nhân, dữ liệu nghiệp vụ, functional requirements, business rules, quy tắc tính điểm và các yêu cầu phi chức năng cần tuân thủ khi phân tích chi tiết, thiết kế database, API, giao diện và test.

Các yêu cầu đã được chốt trong tài liệu này không được tự ý thay đổi trong quá trình implementation. Mọi thay đổi phải được ghi nhận bằng Change Request (CR).

### 0.2. Quy ước mã yêu cầu

Mỗi yêu cầu có một mã duy nhất theo cấu trúc:

```text
FR-<MODULE>-<STT>   : Functional Requirement — yêu cầu tính năng
BR-<MODULE>-<STT>   : Business Rule — yêu cầu/quy tắc nghiệp vụ
NFR-<CATEGORY>-<STT>: Non-functional Requirement — yêu cầu phi chức năng
```

Trong đó `STT` là số thứ tự ba chữ số, bắt đầu từ `001` trong từng module hoặc category.

Ví dụ:

```text
FR-[MODULE]-001
BR-[MODULE]-001
NFR-[CATEGORY]-001
```

Các module/category được dùng trong tài liệu:

| Mã | Phạm vi |
|---|---|
| `COMMON` | Quy tắc dùng chung |
| `AUTH` | Xác thực và phân quyền |
| `TIME` | Thời gian hệ thống |
| `GRADE` | Khối |
| `AY` | Năm học |
| `SEM` | Học kỳ và khóa học kỳ |
| `CLASS` | Lớp học |
| `ENROLL` | Xếp và chuyển lớp |
| `TEACHER` | Giáo viên |
| `ASSIGN` | Phân công |
| `CALENDAR` | Lịch và buổi học |
| `ATTENDANCE` | Điểm danh |
| `SUBJECT` | Môn học |
| `SCORE` | Điểm thành phần và điểm trung bình môn |
| `SKILL` | Môn kỹ năng |
| `AVERAGE` | Điểm trung bình học kỳ/năm |
| `SCORECHANGE` | Sửa điểm |
| `CALC` | Tính điểm nền |
| `RETAKE` | Thi lại |
| `SUMMARY` | Bảng điểm tổng kết |
| `PERFORMANCE` | Hiệu năng |
| `RELIABILITY` | Độ tin cậy và nhất quán |
| `SECURITY` | Bảo mật |
| `AUDITABILITY` | Khả năng kiểm toán |
| `USABILITY` | Khả năng sử dụng |
| `MAINTAINABILITY` | Khả năng bảo trì và độc lập thiết kế |

### 0.3. Phạm vi kế thừa từ project hiện tại

Project hiện đã có nền tảng:

- Đăng ký, đăng nhập, lấy thông tin tài khoản và đăng xuất bằng JWT stateless.
- Quản lý học sinh: tạo, xem, sửa, xóa, tìm kiếm, lọc, sắp xếp và phân trang.
- Mã học sinh có tiền tố `STU` và phần số duy nhất.
- Phân trang phía server với kích thước mặc định 10.
- Bộ lọc sử dụng quan hệ AND.
- Chỉ cho phép sắp xếp theo các trường thuộc allow-list.
- Backend là nguồn validation cuối cùng.
- Có khả năng xử lý batch và CSV bằng Spring Batch.

Baseline này mở rộng project bằng các miền nghiệp vụ: khối, năm học, học kỳ, lớp, xếp lớp, giáo viên, phân công, điểm danh, điểm số, tính điểm nền và thi lại.

## 1. Business context

### 1.1. Mục tiêu hệ thống

Hệ thống quản lý xuyên suốt quá trình học tập của học sinh:

1. Cấu hình năm học, học kỳ và khối.
2. Tạo lớp và xếp học sinh vào lớp.
3. Quản lý giáo viên và phân công giảng dạy.
4. Ghi nhận ngoại lệ điểm danh theo buổi.
5. Quản lý điểm thành phần và điểm thi lại.
6. Tính các loại điểm trung bình bằng background process.
7. Cung cấp bảng điểm tổng kết có trạng thái tính toán rõ ràng.
8. Bảo toàn lịch sử khi học sinh chuyển lớp, giáo viên thay đổi hoặc điểm bị sửa.

### 1.2. Tác nhân

| Tác nhân | Trách nhiệm chính |
|---|---|
| Quản trị viên | Quản lý tài khoản, vai trò, quyền và cấu hình kỹ thuật |
| Giáo vụ | Quản lý năm học, học kỳ, khối, lớp, xếp lớp, giáo viên, phân công, khóa dữ liệu và duyệt sửa điểm |
| Giáo viên chủ nhiệm (GVCN) | Xem lớp chủ nhiệm, nhập ngoại lệ điểm danh, theo dõi kết quả tổng hợp của lớp |
| Giáo viên bộ môn (GVBM) | Quản lý cột điểm và nhập điểm cho môn/lớp/học kỳ được phân công |
| Học sinh (HS) | Xem thông tin lớp, chuyên cần, điểm và trạng thái bảng điểm của bản thân |
| Phụ huynh (PH) | Xem dữ liệu của con trong giai đoạn sau, không thuộc MVP |

### 1.3. Thuật ngữ

| Thuật ngữ | Ý nghĩa |
|---|---|
| KTTT | Kiểm tra thường xuyên |
| KTĐK | Kiểm tra định kỳ |
| KTCK | Kiểm tra cuối kỳ |
| Đtbmh | Điểm trung bình môn học trong một học kỳ |
| ĐtbmhCN | Điểm trung bình môn học cả năm |
| Đtbhk | Điểm trung bình học kỳ của học sinh |
| Đtbcn | Điểm trung bình cả năm của học sinh |
| Đtlmh | Điểm thi lại môn học |
| Bảng điểm tổng kết | Dữ liệu tổng hợp điểm theo học sinh, môn, học kỳ hoặc năm học |
| Calculation task | Yêu cầu tính hoặc tính lại điểm được xử lý ở background |
| CR | Change Request — tài liệu thay đổi hoặc bổ sung yêu cầu đã được phê duyệt |

## 2. Scope

### 2.1. Trong phạm vi MVP

- Metadata khối 6, 7, 8 và 9.
- Năm học và học kỳ.
- Lớp học và cảnh báo cân bằng sĩ số.
- Xếp lớp, chuyển lớp và kết chuyển năm học.
- Hồ sơ giáo viên và tài khoản giáo viên.
- Phân công GVCN và GVBM.
- Điểm danh theo buổi sáng/chiều bằng cơ chế ngoại lệ.
- Môn học thông thường và môn kỹ năng.
- Cột điểm, nhập điểm, sửa điểm và request sửa điểm.
- Tính điểm trung bình bằng background process.
- Khóa học kỳ thủ công hoặc tự động.
- Thi lại theo từng môn và tính lại kết quả cả năm.
- Bảng điểm tổng kết và audit log.

### 2.2. Ngoài phạm vi MVP

- Tài khoản và cổng thông tin phụ huynh.
- Thu học phí.
- Thư viện, thiết bị, tài sản và phòng chức năng.
- Tuyển sinh trực tuyến.
- Nhân sự và tiền lương.
- Khen thưởng, kỷ luật và học bạ điện tử hoàn chỉnh.
- Quy tắc xếp loại, xếp hạng và điều kiện lên lớp.

Quy tắc xếp loại, xếp hạng, xác định học sinh phải thi lại và điều kiện lên lớp phải được định nghĩa trong một CR riêng trước khi implementation các chức năng tương ứng.

## 3. Quy tắc dùng chung

### 3.1. Trạng thái và lịch sử

- `BR-COMMON-001`: Không xóa cứng dữ liệu đã phát sinh nghiệp vụ.
- `BR-COMMON-002`: Dữ liệu đã kết thúc được chuyển sang trạng thái không hoạt động hoặc chỉ đọc.
- `BR-COMMON-003`: Mọi thay đổi quan trọng phải lưu người thực hiện, thời gian, giá trị cũ, giá trị mới và lý do nếu có.
- `BR-COMMON-004`: Dữ liệu năm học cũ phải tiếp tục truy cập được ở chế độ xem.

### 3.2. Phân quyền

- `BR-AUTH-001`: Học sinh chỉ được xem dữ liệu của bản thân.
- `BR-AUTH-002`: Học sinh không được tự điểm danh, tự nhập điểm hoặc tự sửa dữ liệu học tập.
- `BR-AUTH-003`: Giáo viên chỉ được thao tác trên lớp, môn và thời gian được phân công.
- `BR-AUTH-004`: GVCN được xem kết quả tổng hợp của lớp nhưng không mặc nhiên được sửa điểm môn khác.
- `BR-AUTH-005`: Giáo vụ có quyền quản lý dữ liệu nền, khóa học kỳ và duyệt request sửa điểm.
- `BR-AUTH-006`: Quyền truy cập phải được kiểm tra tại backend.

### 3.3. Thời gian

- `BR-TIME-001`: Các mốc 10 ngày và 45 ngày trong tài liệu là ngày dương lịch.
- `BR-TIME-002`: Hệ thống lưu thời gian đủ để xác định chính xác thứ tự và thời hạn nghiệp vụ.
- `BR-TIME-003`: Quy ước timezone của API và database phải được cấu hình thống nhất cho trường.

