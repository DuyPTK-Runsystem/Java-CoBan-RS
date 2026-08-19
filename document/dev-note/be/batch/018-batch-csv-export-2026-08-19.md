# Dev Note 018: Batch CSV Export

## Developer Plan và approval

- Related plan: `document/dev-impl-plan/be/018-batch-csv-export-2026-08-19.md`.
- Approval: user đã phê duyệt Plan 018 ngày 2026-08-19. Trong lúc triển khai, user cho phép lưu metadata execution của Spring Batch vào database.

## Scope đã hoàn thành

- Thêm batch export đọc `student` inner join `student_info`, xuất đúng thứ tự cột: `student_id`, `student_name`, `student_code`, `address`, `average_score`, `date_of_birth`.
- Thêm API có xác thực `GET /api/v1/students/export`, trả raw `byte[]` với `text/csv; charset=UTF-8` và header tải file `students.csv`.
- CSV được tạo trong `ByteArrayOutputStream` theo từng job execution và trả trực tiếp cho request; không ghi/lưu file CSV vào filesystem.
- Dùng Spring Batch JDBC `JobRepository` và bật khởi tạo schema metadata để persist `JobInstance`, `JobExecution` và `StepExecution`. Metadata không chứa CSV bytes.
- Validation theo item: record không hợp lệ bị skip và trace log; record hợp lệ còn lại vẫn được export. Trường hợp không có record hoặc toàn bộ record bị skip trả CSV chỉ có header. Lỗi hạ tầng trả HTTP 500.
- Cập nhật `FormatRestResponse` để không bọc `byte[]` CSV thành JSON.
- Thêm `RequestIdFilter` và đặt trước JWT filter để trace log batch có request id trong MDC.
- Theo yêu cầu tiếp theo của user, tạo Postman Collection riêng để Register -> Login -> tạo 500 student test data bằng Collection Runner.

## Files thay đổi chính

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/batch/studentcsv/`: job, reader projection, processor, writer và in-memory result store.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/controller/StudentController.java`: endpoint export CSV.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/common/filter/RequestIdFilter.java` và `config/SecurityConfiguration.java`: gắn request id cho trace log.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/common/util/FormatRestResponse.java`: bypass response `byte[]`.
- `BE/BaiTap-RS/src/main/resources/application.properties`: tắt auto-run job lúc khởi động và bật tạo schema batch metadata.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/batch/studentcsv/StudentCsvExportItemProcessorTest.java` và `student/controller/StudentCsvExportControllerIntegrationTest.java`: unit/integration tests cho processor và API export.
- `document/application-doc/DataStructure.md`, `document/application-doc/ApplicationContext.md`: đồng bộ contract batch và policy persistence.
- `document/postman/Java-CoBan-Batch-Test-Data.postman_collection.json`: collection v2.1 tạo dữ liệu test cho batch export.

## Quyết định thực thi

- Dùng inner join nên student không có `student_info` không xuất hiện trong CSV.
- Dùng `JpaPagingItemReader` với chunk size 100; writer tự escape dấu phẩy, dấu nháy kép và xuống dòng theo CSV.
- Chỉ item validation exception được skip; lỗi truy vấn, launch job hoặc writer vẫn là lỗi toàn cục.
- Metadata job được persist theo thay đổi requirement sau approval; đây là dữ liệu kỹ thuật Spring Batch, không phải file output.
- Collection test-data tạo username và dải `STUxxxxxxx` theo timestamp, lưu token từ login và tự lặp request tạo student tới `studentTotal=500`; không chứa token hay secret thực tế.

## Validation

| Command/check | Result | Note |
|---|---|---|
| `./gradlew test checkstyleMain checkstyleTest pmdMain pmdTest build` | PASS | Build thành công; test, JaCoCo, Checkstyle và PMD đều pass. |
| JaCoCo report | PASS | Sinh report tại `BE/BaiTap-RS/build/reports/jacoco/test/html/index.html`; có coverage cho batch processor và export controller integration flow. |
| API integration | PASS | Xác nhận raw CSV/header, escaping/null, skip item lỗi, header-only và 401 khi chưa xác thực. |
| Postman collection JSON/schema/secret scan | PASS | Collection v2.1 hợp lệ, dùng `{{baseUrl}}` và collection variables; không gọi API live. |

## Vòng debug đã xử lý

- Sửa import `Job` tương thích Spring Batch 6, cấu hình repository metadata JDBC và scope reader/writer.
- Điều chỉnh thứ tự filter để request id có mặt trước JWT trace.
- Bỏ transaction fixture test để transaction của batch reader nhìn thấy dữ liệu H2.
- Tách helper/constant và bổ sung `serialVersionUID` để Checkstyle/PMD pass.

## Deviation và next step

- Developer Plan 018 chưa bao gồm Postman artifact; collection được thêm theo yêu cầu trực tiếp sau khi batch hoàn thành.
- Import collection vào Postman và chạy folder bằng Collection Runner sau khi backend/database đã sẵn sàng; job tạo dữ liệu không xóa dữ liệu test đã tạo.

## Lưu ý

- Build vẫn hiển thị warning deprecation từ API Spring Batch legacy (`JobLauncher`/chunk API), nhưng không có quality-gate failure.
- Trong môi trường production, schema metadata nên được quản lý qua migration; cấu hình hiện tại khởi tạo schema tự động theo phạm vi requirement hiện tại.
