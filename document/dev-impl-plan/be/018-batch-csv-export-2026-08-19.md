# Developer Plan 018: Batch CSV Export

## 1. Mục tiêu

- Xây dựng Spring Batch job đọc Student joined với StudentInfo và tạo CSV trong memory để API trả về dạng `byte[]`.
- Tách trách nhiệm đọc, mapping và ghi file thành batch configuration rõ ràng, có thể kiểm thử; không thực hiện vòng lặp export tùy tiện trong controller.
- Không thay đổi dữ liệu nguồn và không tạo/lưu file CSV trên filesystem server.

## 2. Requirement và hiện trạng

- Requirement nhận được: “Tạo batch đọc thông tin từ các table và ghi vào file csv.”
- Source export đã chốt: `student s join student_info si on s.student_id = si.student_id`. `student` và `student_info` là quan hệ 1–1; các trường mô tả của `student_info` có thể `null`.
- Cột CSV, theo đúng thứ tự: `student_id`, `student_name`, `student_code`, `address`, `average_score`, `date_of_birth`.
- Export do API call kích hoạt, raw response là `byte[]`; không persist file hệ thống.
- Object lỗi phải được skip riêng lẻ để các object khác vẫn export; không fail toàn bộ job vì lỗi một object.
- `build.gradle.kts` đã có `spring-boot-starter-batch-jdbc` và test starter tương ứng; source hiện chưa có package/job Spring Batch nào.
- `DataStructure.md` và `ApplicationContext.md` yêu cầu Spring Batch; plan này cập nhật hai tài liệu theo contract mới.

## 3. Contract đã chốt

| Hạng mục | Quyết định |
|---|---|
| Dữ liệu nguồn | Inner join `student` và `student_info`; không export bảng `user`. |
| Cột | `student_id`, `student_name`, `student_code`, `address`, `average_score`, `date_of_birth`. |
| Trigger | API authenticated; endpoint đề xuất: `GET /api/v1/students/export`. |
| Response | Raw `byte[]`, `Content-Type: text/csv; charset=UTF-8`, `Content-Disposition: attachment`; không dùng `RestResponse`. |
| Lưu trữ | Chỉ buffer trong memory cho lifecycle job/request; không có output directory, temp file hoặc retention. |
| Lỗi object | Processor phân loại lỗi object, skip dòng lỗi, ghi log có Student id/code nếu có thể và tiếp tục export. |
| Trace log | Dùng log `>>>BatchCsvExport` theo quy ước project, kèm thread và MDC `requestId`; bổ sung một request-ID filter dùng chung vì ứng dụng chưa populate MDC. |

CSV chỉ có record thành công. Vì contract response là `byte[]`, response body không thể kèm danh sách lỗi; logging là cơ chế truy vết. Nếu toàn bộ record lỗi hoặc join không trả record nào, API vẫn trả CSV header-only. Lỗi hạ tầng không thể quy về một object (database không đọc được, job không khởi tạo được, không tạo được response) vẫn trả lỗi HTTP thay vì giả thành export thành công.

## 4. Phạm vi

### In-scope

- Spring Batch `Job`/`Step` đọc joined projection theo chunk, map sang export record và viết CSV vào in-memory buffer theo từng execution; dùng JDBC repository để persist metadata job.
- `GET /api/v1/students/export` chạy job đồng bộ, nhận bytes kết quả và trả download response; không có thư mục output, file tạm hoặc file đích.
- Header/thứ tự cột đã chốt; UTF-8, comma delimiter, quote escaping chuẩn, `LocalDate` dạng `yyyy-MM-dd`, value `null` thành empty cell.
- Fault-tolerant processing cho lỗi theo object; không mutation Student/StudentInfo.
- Bypass có chủ đích `byte[]` trong `FormatRestResponse`.
- Một `RequestIdFilter` dùng chung để log skip có request ID theo quy ước trace hiện có.
- Unit/integration tests cho job và CSV output; Dev Note sau implementation.

### Out-of-scope

- Bulk add/import CSV, chỉnh sửa dữ liệu database, migration schema nghiệp vụ hoặc thay đổi CRUD/User/Auth hiện có.
- UI download, Postman collection, scheduler, file storage/retention và streaming response.
- Export password, password hash hoặc bí mật cấu hình.
- Thay đổi dependency, trừ khi implementation đã duyệt chứng minh starter hiện có không đáp ứng.

## 5. Thiết kế đề xuất

```text
trigger đã duyệt + JobParameters
    -> Spring Batch Job
    -> Step đọc joined projection theo chunk
    -> mapper sang CSV record
    -> in-memory CSV writer theo JobExecution
    -> byte[]
    -> HTTP CSV attachment response
```

- Dùng Spring Batch đã có sẵn thay vì export trực tiếp trong controller để có execution status, chunk processing và khả năng rerun rõ ràng.
- Reader chỉ select sáu cột contract, không serialize trực tiếp entity và không đọc bảng `user`.
- Job dùng JobParameters khác nhau theo request để tránh Spring Batch coi các lần gọi là cùng job instance.
- Buffer tồn tại theo request/job và được giải phóng sau response. `byte[]` bắt buộc toàn bộ output tồn tại trong memory; nếu data vượt giới hạn vận hành, phải mở scope mới đổi sang streaming thay vì tự thay contract.
- Chỉ lỗi phân loại ở processor mới được skip. Reader/writer/job error mang tính hạ tầng không bị nuốt như lỗi object.
- Dùng Spring Batch JDBC repository để persist `JobInstance`, `JobExecution` và `StepExecution`; cấu hình Spring Boot khởi tạo batch metadata schema. CSV vẫn chỉ tồn tại trong memory cho lifecycle request/response.

## 6. Phạm vi mã nguồn dự kiến

| Path/khu vực | Thao tác | Mục đích |
|---|---|---|
| `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/batch/**` | Tạo | Batch configuration, job/step, reader/mapper/writer và model export nội bộ. |
| `BE/BaiTap-RS/src/main/resources/application.properties` | Sửa | Tắt auto-run lúc app khởi động và bật khởi tạo batch metadata schema. |
| `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/controller/StudentController.java` | Sửa | Thêm endpoint export, headers và `byte[]` response. |
| `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/common/util/FormatRestResponse.java` | Sửa | Skip `byte[]` để giữ raw CSV contract. |
| `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/common/filter/RequestIdFilter.java` | Tạo | Populate/remove MDC `requestId` cho HTTP request và developer trace log. |
| `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/config/SecurityConfiguration.java` | Sửa | Đặt request-ID filter trước authentication filter. |
| Repository/projection dưới `student/**` | Sửa hoặc tạo | Đọc inner join sáu cột theo chunk, không đổi CRUD. |
| `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/batch/**` | Tạo | Job, mapping, skip và CSV bytes tests với H2/memory buffer. |
| `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/student/controller/**` | Sửa hoặc tạo | Endpoint, headers, security và raw response integration test. |
| `document/application-doc/DataStructure.md` | Sửa | Chốt layout CSV, cột và quyết định đã được xác nhận. |
| `document/application-doc/ApplicationContext.md` | Sửa | Chốt batch API/memory/skip contract. |
| `document/dev-note/be/batch/018-batch-csv-export-2026-08-19.md` | Tạo sau implementation | Ghi scope thực tế và evidence validation. |

## 7. API / database / integration

- Endpoint đề xuất: `GET /api/v1/students/export`; security `.anyRequest().authenticated()` hiện có tiếp tục bảo vệ endpoint.
- Success: `200 OK`, `text/csv; charset=UTF-8`, `Content-Disposition` attachment và body raw `byte[]`; không có `RestResponse` envelope.
- Không đổi schema `user`, `student`, `student_info` hay dữ liệu hiện có.
- Spring Batch dùng JDBC repository; metadata execution được lưu trong database. Chỉ CSV output không được lưu/persist file.
- CSV header: `student_id,student_name,student_code,address,average_score,date_of_birth`; UTF-8; comma delimiter; quote escaping chuẩn; `date_of_birth` ISO `yyyy-MM-dd`; trường nullable xuất empty cell.

## 8. Test và validation plan

### Test job/CSV

- Dữ liệu hợp lệ inner join đúng, có header đúng thứ tự và đúng số dòng/sáu cột.
- Nullable address/score/birthday thành empty cell; birthday `yyyy-MM-dd`; text có comma/quote/newline được escape.
- Join rỗng và tất cả item bị skip trả header-only bytes.
- Một object lỗi bị skip/log nhưng object trước/sau vẫn xuất; lỗi hạ tầng vẫn làm job/API error.
- Endpoint trả `200`, `text/csv`, `Content-Disposition` và raw bytes không bị envelope; request chưa authenticated tiếp tục nhận `401` theo security hiện có.

### Cô lập và regression

- Dùng `spring-batch-test`, H2 và memory buffer/job-scoped fixture để cô lập metadata, dữ liệu fixture và output; không tạo filesystem artifact.
- Mock chỉ dùng ở unit test mapper/writer; integration test job phải kiểm tra artifact CSV thực tế.
- Regression: Student/User API, entity mapping và dữ liệu nguồn không bị job mutate.

### Commands sau implementation

Chạy từ `BE/BaiTap-RS/`:

```bash
./gradlew test
./gradlew jacocoTestReport
./gradlew checkstyleMain checkstyleTest
./gradlew pmdMain pmdTest
./gradlew build
```

Đọc JaCoCo report để xác nhận job configuration, mapper và error/rerun branch mới có test thực tế; không đặt threshold mới.

## 9. Rủi ro và giảm thiểu

| Rủi ro | Giảm thiểu |
|---|---|
| Output `byte[]` lớn dùng nhiều memory | Chunk reader/writer, theo dõi giới hạn vận hành; yêu cầu change scope sang streaming nếu cần. |
| Lỗi item làm fail chunk/job | Phân loại exception ở processor, cấu hình skip và test record khác vẫn được export. |
| Raw bytes bị bọc thành JSON | Sửa skip condition của `FormatRestResponse`; kiểm tra headers/body bằng MockMvc. |
| Batch metadata tạo thêm bảng kỹ thuật | Chỉ chứa execution metadata, không chứa CSV bytes; dùng schema chuẩn của Spring Batch và tách khỏi bảng nghiệp vụ. |
| Endpoint bị truy cập trái phép | Kế thừa security authenticated và regression test 401. |

## 10. Output dự kiến

- API export trả CSV Student/StudentInfo dạng `byte[]`, không tạo file server và không sửa dữ liệu nguồn.
- Chỉ các record thành công xuất hiện trong CSV; record lỗi được skip/log, còn lỗi hạ tầng trả HTTP error.
- Tests bảo vệ join, mapping, escaping, nullable data, per-item skip, raw bytes và security; quality gates backend có evidence trong Dev Note.

## 11. Approval status

- Trạng thái: **Approved by user via agent on 2026-08-19.**
- Phạm vi trace-log được làm rõ bằng `RequestIdFilter` tối thiểu để đáp ứng quy ước log đã được phê duyệt; không đổi contract API/data.
- User đã cho phép persist metadata Spring Batch vào database trong tin nhắn agent ngày 2026-08-19; CSV output vẫn không được lưu file.
