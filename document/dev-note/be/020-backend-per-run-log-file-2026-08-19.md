# Dev Note 020: Backend Per-Run Log File

## Related Developer Plan và approval

- Developer Plan: `document/dev-impl-plan/be/020-backend-per-run-log-file-2026-08-19.md`.
- Status: Approved by user via agent on 2026-08-19.

## Scope đã hoàn thành

- Cấu hình Logback ghi mỗi application process vào một file riêng tại `build/logs/` mặc định.
- Giữ console appender, không thêm dependency hoặc Java source.
- Cho phép đổi thư mục log bằng `APP_LOG_DIRECTORY` qua property `app.logging.directory`.

## Files changed

| Path | Thay đổi thực tế |
|---|---|
| `BE/BaiTap-RS/src/main/resources/logback-spring.xml` | Tạo console/file appenders; file dùng context-birth timestamp, PID và `append=false`. Pattern có timestamp, level, thread, MDC `requestId` nếu có, logger, message và stack trace. |
| `BE/BaiTap-RS/src/main/resources/application.properties` | Thêm `app.logging.directory=${APP_LOG_DIRECTORY:build/logs}`. |
| `document/dev-impl-plan/be/020-backend-per-run-log-file-2026-08-19.md` | Chuyển approval status thành `Approved`. |
| `document/dev-impl-plan/be/BE_DEV_PLAN_SUMMARY.md` | Cập nhật Plan 020 thành `Approved`. |
| `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md` | Cập nhật Plan 020 thành `Approved`. |
| `document/dev-note/be/020-backend-per-run-log-file-2026-08-19.md` | Tạo Dev Note này. |
| `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md` | Đăng ký Dev Note 020. |
| `document/dev-note/summary/DEV_NOTE_SUMMARY.md` | Đăng ký Dev Note 020. |

## Quyết định implementation

- Dùng `logback-spring.xml` để đặt tên file động. `logging.file.name` đơn lẻ không bảo đảm file tách theo mỗi lần chạy.
- Tên file runtime có dạng `backend-<yyyyMMdd-HHmmss-SSS>-<pid>.log`; hai lần `bootRun` tạo hai file khác nhau trong `build/logs/`.
- Khi validation ban đầu, `%wEx` không được Logback phiên bản hiện tại hỗ trợ và khiến application context fail. Đã thay bằng `%ex`; không thay đổi scope hoặc behavior logging khác.
- `build/` đã được ignore bởi `BE/BaiTap-RS/.gitignore`, nên runtime logs không được Git theo dõi.

## Validation thực tế

| Command / kiểm tra | Result | Evidence |
|---|---|---|
| `./gradlew test` | PASS | 41 tests passed; JaCoCo report được tạo. Lần đầu FAIL do `%wEx`, đã sửa thành `%ex` và chạy lại PASS. |
| `./gradlew bootRun` | PASS | Backend khởi động thành công trên port 8081 và ghi `build/logs/backend-20260819-114422-737-186903.log`. |
| Lần `./gradlew bootRun` thứ hai | PASS | Tạo file khác `build/logs/backend-20260819-114515-986-187972.log`, xác nhận per-run filename. |
| `APP_LOG_DIRECTORY=/tmp/backend-log-override-020 ./gradlew test --tests com.JavaTraining.BaiTap_RS.BaiTapRsApplicationTests` | PASS | Test Spring Boot cô lập pass và tạo `/tmp/backend-log-override-020/backend-20260819-114734-202-190612.log`. |
| `./gradlew checkstyleMain checkstyleTest` | PASS | Build successful. |
| `./gradlew pmdMain pmdTest` | PASS | Build successful; console chỉ nêu baseline PMD message `LoosePackageCoupling` bị loại khỏi ruleset do chưa cấu hình package/class. |
| `./gradlew build` | PASS | Build successful; Checkstyle, PMD, test và JaCoCo đều pass/up-to-date. |
| `git diff --check` | PASS | Không có whitespace error. |

## Deviation, blocker và next step

- Không có deviation về chức năng so với Developer Plan. Chi tiết conversion word của stack trace được điều chỉnh từ `%wEx` sang `%ex` để tương thích Logback đang dùng.
- Không có blocker hay validation bị bỏ qua. Validation đã tạo runtime artifact dưới `BE/BaiTap-RS/build/logs/` theo requirement.
- Không có next step bắt buộc. Retention/rolling hoặc log aggregation vẫn ngoài scope Plan 020.

## Debug rounds

- `code -> test -> debug`: 1/10 vòng (sửa `%wEx` sau lỗi configuration, sau đó validation pass).
