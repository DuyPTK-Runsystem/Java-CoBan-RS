# Dev Note: BE Operational Closure — Plan 047

- **Kế hoạch liên quan**: [`document/dev-impl-plan/be/scorebook/047-be-operational-closure-2026-08-26.md`](../../../dev-impl-plan/be/scorebook/047-be-operational-closure-2026-08-26.md)
- **Trạng thái phê duyệt**: `Approved` (2026-08-26)
- **Trạng thái triển khai**: `Completed`
- **Application-document version**: `v2`
- **Ngày ghi nhận**: 2026-08-26

## 1. Phạm vi thực tế hoàn thành

1. **Score audit log API**
   - Thêm `GET /api/v2/scorebooks/audit-logs` với filter entity, action, actor, student, khoảng thời gian và phân trang tối đa 50 bản ghi.
   - Parse `beforeData`/`afterData` thành JSON, bổ sung actor username và sort `occurredAt DESC, id DESC`.
   - Giáo vụ/admin được xem toàn bộ; giáo viên được kiểm tra theo assignment scope; học sinh bị chặn ở controller.
2. **Calculation task operations**
   - Thêm alias list task `FAILED` và `POST /retry-all-failed`.
   - Retry hàng loạt tái sử dụng guard/idempotency/audit của retry đơn lẻ, chỉ chuyển task đang `FAILED` về `PENDING`.
   - Bổ sung regex constraint cho hai route recalculate theo `studentCode` và `studentId` để loại bỏ mapping ambiguity của Spring MVC.
3. **Transcript calculation status**
   - Thêm bốn endpoint status cho học kỳ/năm học, cả `/me` và truy vấn theo `studentId`.
   - Response có calculation status, source/calculated version, `isUpToDate`, timestamp và lỗi gần nhất.
4. **Security verification**
   - Thêm `SecurityMatrixVerificationTest` kiểm tra 401 cho anonymous, 403 cho STUDENT/TEACHER tại endpoint vận hành không thuộc quyền, và 200 coarse authorization cho ACADEMIC_OFFICE.
   - Bổ sung/giữ lại unit test cho access guard, retry-all, audit mapping và calculation status.
5. **Postman**
   - Giữ nguyên 11 folder hiện hữu, thêm folder `12. Scorebook & Operational Closure` với các request list/retry/recalculate/status/audit và variables `taskId`, `retakeId`.

## 2. File thay đổi chính

### Production

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/controller/ScoreAuditLogController.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/ScoreAuditLogService.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/AuditLogSpecifications.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/DTOs/requests/ReqFilterScoreAuditLogDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/DTOs/response/ResScoreAuditLogDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/DTOs/response/ResTranscriptCalculationStatusDTO.java`
- `CalculationTaskController`, `CalculationTaskService`, `CalculationTaskRepository`
- `TranscriptQueryController`, `TranscriptQueryService`
- `AuditLogRepository` mở rộng `JpaSpecificationExecutor`

### Tests và collection

- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/security/SecurityMatrixVerificationTest.java`
- `ScoreAuditLogServiceTest`, `TranscriptCalculationStatusServiceTest`
- Các test controller/service liên quan calculation task và transcript query
- `document/postman/Java-CoBan.postman_collection.json`

## 3. Quyết định kỹ thuật

- Không tạo migration hoặc thay đổi schema theo đúng out-of-scope của plan.
- Audit filter student dùng Specification để thu hẹp ứng viên và parse snapshot để xác nhận chính xác `studentId`; cách này tương thích schema JSON hiện tại nhưng có thể cần index/JSON query chuyên dụng nếu dữ liệu audit tăng lớn.
- Teacher scope được suy ra từ snapshot student/year hoặc class subject/scorebook/assessment column; log thiếu context định danh bị loại khỏi kết quả teacher để tránh rò rỉ dữ liệu.
- Retry-all gọi logic retry đơn lẻ trong transaction; lần gọi lặp lại không retry task đã chuyển khỏi `FAILED`.

## 4. Validation Result

| Kiểm tra | Lệnh | Trạng thái | Ghi chú |
| --- | --- | --- | --- |
| Unit/integration/regression test | `./gradlew clean test --no-daemon --console=plain` | **PASS** | Toàn bộ test hiện hữu và test Plan 47 PASS; JaCoCo được sinh qua `jacocoTestReport`. |
| Checkstyle test | `./gradlew checkstyleTest --no-daemon --console=plain` | **PASS** | Không còn violation trong test. |
| PMD test | `./gradlew pmdTest --no-daemon --console=plain` | **PASS** | Không còn PMD violation. |
| Checkstyle/PMD production | `./gradlew checkstyleMain pmdMain --no-daemon --console=plain` | **PASS** | Không có violation; cảnh báo `LoosePackageCoupling` là rule cấu hình bị loại bỏ sẵn. |
| Build | `./gradlew build -x test -x pmdTest -x checkstyleTest --no-daemon --console=plain` | **PASS** | `jar`, `bootJar`, `check`, `build` thành công. |
| Postman JSON | `jq empty document/postman/Java-CoBan.postman_collection.json` | **PASS** | Collection hợp lệ JSON, gồm folder mới 12. |

JaCoCo report: `BE/BaiTap-RS/build/reports/jacoco/test/html/index.html`. Các class mới đều được load trong report; coverage được dùng để kiểm tra test execution, chưa đặt threshold riêng cho plan này.

## 5. Số vòng debug

- **4 vòng chính**: sửa import compile; bổ sung mock cho retry-all unit test; disambiguate route recalculate bằng regex; xử lý Checkstyle/PMD test violations.

## 6. Blockers và rủi ro còn lại

- **Blocker**: Không có.
- **Rủi ro**: Audit snapshot vẫn lưu JSON text; nếu khối lượng log lớn cần tối ưu bằng generated/indexed JSON field ở một plan/schema riêng.
- Không thực hiện live Postman run vì môi trường không có token/session và dữ liệu API vận hành ổn định; collection đã được kiểm tra cú pháp và cấu trúc.
