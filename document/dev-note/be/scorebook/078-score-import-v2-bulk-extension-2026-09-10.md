# Dev Note 078 BE — Mở rộng bulk add/update v2 bằng file mẫu điểm

## Trạng thái và approval

- Plan 078 và wireframe đã được người dùng approve qua agent message ngày 2026-09-10.
- Trạng thái: `IMPLEMENTED; focused validation PASS; repository quality baseline blockers`.

## Phạm vi thực tế đã triển khai

- Thêm dependency Apache POI để tạo/đọc `.xlsx`.
- Thêm `GET /api/v2/assessment-columns/{columnId}/scores/bulk-template`: sinh file mẫu từ roster ACTIVE với ngữ cảnh lớp/môn/học kỳ và đúng 5 cột `STT | Mã học sinh | Họ tên học sinh | Điểm số | Ghi chú`.
- Thêm `POST /api/v2/assessment-columns/{columnId}/scores/bulk/preview`: kiểm tra file một sheet, header, định dạng, giới hạn 10 MB/2.000 dòng, duplicate, roster, điểm/ghi chú, rounding và optimistic-version snapshot.
- Cột `Điểm số` nhận điểm `0–10`, `11` (vắng), `12` (miễn) và `13` (hủy), tương ứng đủ các `ScoreStatus` mà BE hỗ trợ.
- Tiêu đề merged `A1:E1` căn giữa; sau một dòng trống kể từ dữ liệu cuối, file thêm `Ghi chú:` cùng bốn dòng hướng dẫn về điểm và mã `11`/`12`/`13`.
- Preview trả old/new row data, lỗi theo dòng và `items` đã chuẩn hóa.
- File mode không tạo đường ghi điểm riêng; payload preview được gửi qua bulk write v2 hiện có, nên vẫn dùng authorization, lifecycle, optimistic locking, audit và transcript/calculation touch của `ScoreEntryService.bulkUpsertScores`.
- Template/preview kiểm tra `ScorebookGuard`, scorebook writable và semester scoring state trước khi đọc roster.

## Files chính

- `BE/BaiTap-RS/build.gradle.kts`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/controller/BulkScoreFileController.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/BulkScoreFileService.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/DTOs/response/ResBulkScoreFile*.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/scorebook/service/BulkScoreFileServiceTest.java`

## Deviation / giới hạn cần biết

- Không persist `ScoreImportBatch/ScoreImportRow`, không có batch commit server-side và không giữ raw file. Đây là lựa chọn để giữ feature là phần bổ sung trực tiếp của bulk add/update v2; preview dùng trực tiếp payload `items` cho API bulk hiện có.
- Vì vậy audit/review lịch sử batch sau request chưa nằm trong implementation này; nếu cần phải có amendment riêng cho Plan 078.

## Validation Result

- Debug iterations: `3` (contract compile alignment, strict Mockito test setup, PMD/Checkstyle cleanup).
- Backend focused test `BulkScoreFileServiceTest`: `PASS` — 5 tests; JaCoCo report được tạo, gồm template 5 cột và mapping `11`/`12`/`13`.
- Backend focused test được chạy lại sau phần ghi chú template: `PASS` — 5 tests; kiểm tra title căn giữa và các dòng ghi chú.
- `checkstyleMain/checkstyleTest`: `PASS` với warning baseline ngoài scope.
- Backend full `test`: `PASS` trong lệnh quality run; các cảnh báo Spring Batch deprecation là cảnh báo có sẵn.
- `bootJar`: `PASS` trong build lifecycle trước khi quality tasks kết thúc.
- Lệnh đã chạy: `GRADLE_USER_HOME=/tmp/java-coban-rs-gradle ./gradlew test checkstyleMain pmdMain build`, `./gradlew pmdMain pmdTest checkstyleMain checkstyleTest` và focused `test ... jacocoTestReport`.
- `checkstyleMain/checkstyleTest`: `PASS` với warning baseline ở các file cũ; file implementation chính và test mới không còn warning riêng sau chỉnh sửa.
- `pmdMain/pmdTest`: `FAIL` do baseline violations ở `SchoolClassService`, `ClassTranscriptQueryService` và một số test cũ; không còn violation PMD riêng ở `BulkScoreFileService`/test sau suppression hợp lệ.
- `build`: `FAIL` vì build lifecycle kéo theo các PMD baseline violations; `bootJar` độc lập `PASS`.
- Backend live API, upload file thật và permission matrix runtime: `NOT RUN`.
