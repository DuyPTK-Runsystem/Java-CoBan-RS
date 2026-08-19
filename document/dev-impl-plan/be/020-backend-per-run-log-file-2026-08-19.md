# Developer Plan 020: Backend Per-Run Log File

## 1. Mục tiêu

- Cấu hình backend Spring Boot ghi log của mỗi lần khởi động ứng dụng vào một file riêng dưới thư mục `BE/BaiTap-RS/build/logs/`.
- Giữ log console hiện có để developer vẫn quan sát được khi chạy local.
- Không cần thêm dependency hoặc viết Java code cho yêu cầu cấu hình này.

## 2. Requirement và hiện trạng

- Requirement trực tiếp: mỗi phiên chạy backend tạo một file log, được lưu trong `build/` của backend.
- Hiện backend chỉ có `src/main/resources/application.properties`; chưa có `logback-spring.xml` hoặc cấu hình file appender.
- Spring Boot đã mang Logback/SLF4J theo starter mặc định. `BE/BaiTap-RS/.gitignore` đã ignore toàn bộ `build/`, nên log runtime sẽ không bị Git theo dõi.

## 3. Phạm vi

### In-scope

- Tạo cấu hình Logback cho console và một file appender theo từng application process.
- Đặt file log mặc định tại `build/logs/`, với tên gồm thời điểm context được tạo và PID để phân biệt các lần chạy/process.
- Cho phép override thư mục log bằng biến môi trường/property mà không sửa source.
- Bổ sung Dev Note sau khi implementation và ghi evidence validation thực tế.

### Out-of-scope

- Không thay đổi format/quy ước nội dung developer trace log đã được chốt ở Plan 019.
- Không thêm audit logging, request filter/MDC, log aggregation, gửi log ra dịch vụ ngoài, dashboard/metrics, retention tự động hoặc nén/xoay vòng log trong phạm vi này.
- Không đổi API, database, security, Gradle dependency hay cấu hình deploy/CI.

## 4. Thiết kế đề xuất

```text
Spring Boot startup
    -> Logback context birth timestamp + process PID
    -> build/logs/backend-<timestamp>-<pid>.log
    -> console appender vẫn nhận cùng log events
```

- Tạo `logback-spring.xml` thay vì chỉ dùng `logging.file.name`: property đơn lẻ đó tạo một tên cố định, không đáp ứng rõ ràng yêu cầu file riêng theo lần chạy.
- Dùng timestamp tại thời điểm Logback context khởi tạo (`contextBirth`) cùng PID trong tên file. Điều này tách log của từng process và tránh ghi đè trong các lần chạy thông thường/cùng thời điểm.
- File appender dùng `append=false`; một process tạo log mới thay vì nối tiếp một file session cũ.
- `application.properties` khai báo `app.logging.directory=${APP_LOG_DIRECTORY:build/logs}`. Giá trị mặc định là đường dẫn tương đối của Gradle project; khi chạy từ `BE/BaiTap-RS/`, output nằm đúng `BE/BaiTap-RS/build/logs/`.
- Giữ mức log theo mặc định Spring Boot hiện có. Không tự tăng mức `DEBUG`/`TRACE`, tránh làm log chứa quá nhiều dữ liệu hoặc làm thay đổi hành vi vận hành.

## 5. Phạm vi mã nguồn dự kiến

| Path/khu vực | Thao tác | Mục đích |
|---|---|---|
| `BE/BaiTap-RS/src/main/resources/logback-spring.xml` | Tạo | Khai báo console appender và per-run file appender; pattern có timestamp, logger, thread, MDC `requestId` (nếu có) và stack trace. |
| `BE/BaiTap-RS/src/main/resources/application.properties` | Sửa | Thêm property `app.logging.directory` với default `build/logs` và override `APP_LOG_DIRECTORY`. |
| `document/dev-note/be/020-backend-per-run-log-file-2026-08-19.md` | Tạo sau implementation | Ghi thay đổi thực tế, đường dẫn log được tạo và kết quả validation. |

## 6. API / database / integration

- Không có endpoint, request/response contract, schema, migration hoặc data change.
- Integration duy nhất là logging runtime của Spring Boot/Logback. Log directory có thể override qua `APP_LOG_DIRECTORY` khi môi trường chạy không cho ghi vào working directory; default vẫn là `build/logs` theo requirement.
- `build/` đang được `.gitignore`, vì vậy log files không được commit. Nếu một môi trường chạy từ working directory khác, relative path sẽ theo working directory đó; tài liệu/run command cần chạy Gradle từ `BE/BaiTap-RS/` để giữ default location như yêu cầu.

## 7. Test và validation plan

Chạy từ `BE/BaiTap-RS/` sau implementation:

```bash
./gradlew clean bootRun
./gradlew test
./gradlew checkstyleMain checkstyleTest
./gradlew pmdMain pmdTest
./gradlew build
```

- Khởi động application tối thiểu hai lần (dừng process an toàn sau khi startup) và xác nhận có hai file khác tên trong `build/logs/`.
- Kiểm tra mỗi file chứa startup log và có thể ghi log ứng dụng; kiểm tra console vẫn hiển thị log.
- Chạy một lần với `APP_LOG_DIRECTORY` để xác nhận override đường dẫn hoạt động, rồi dọn artifact test chỉ trong thư mục `build/` nếu cần.
- Không cần unit test Java mới vì thay đổi chỉ là resource configuration; build/test/quality gates bảo vệ regression.

## 8. Rủi ro và giảm thiểu

| Rủi ro | Giảm thiểu |
|---|---|
| Hai lần chạy tạo cùng tên file | Kết hợp context-birth timestamp độ phân giải milliseconds và PID; không dùng tên cố định. |
| Process không có quyền ghi `build/logs` | Cho phép `APP_LOG_DIRECTORY` override; startup fail rõ ràng thay vì mất log âm thầm. |
| Log tăng dung lượng theo thời gian | Out-of-scope retention/rolling; `build/` là runtime artifact local. Nếu cần vận hành lâu dài, lập plan riêng cho retention/rolling và giới hạn dung lượng. |
| Relative path không trỏ vào backend `build/` | Chạy Gradle từ `BE/BaiTap-RS/`; dùng override ở môi trường khác. |
| Rò rỉ dữ liệu nhạy cảm | Không tăng log level; tiếp tục tuân thủ quy ước Plan 019, không ghi secret/token/password. |

## 9. Output dự kiến

- Sau mỗi lần khởi động backend bằng Gradle trong `BE/BaiTap-RS/`, có một file dạng `build/logs/backend-<timestamp>-<pid>.log`.
- File log mới nhận log trong toàn bộ lifetime của process; log console vẫn hoạt động.
- Log runtime không xuất hiện trong Git status do nằm dưới `build/`.

## 10. Approval status

- Trạng thái: **Approved.**
- Approved by user via agent on 2026-08-19.
