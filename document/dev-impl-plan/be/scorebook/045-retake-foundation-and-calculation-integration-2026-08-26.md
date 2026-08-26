# Developer Plan 045: Retake Foundation and Calculation Integration

## 1. Trạng thái và thông tin chung

- **Status**: `Approved` — đã được người dùng phê duyệt và hoàn thành triển khai.
- **Application-document version**: `v2`.
- **Ngày lập plan**: `2026-08-26`.
- **Module**: Backend `scorebook` / Retake và transcript calculation.
- **Gộp từ**: Plan 046 — Retake Foundation và Plan 047 — Retake Calculation Integration.
- **Tài liệu tham chiếu**:
  - `document/application-doc/v2/modules/06-RetakeAndTranscriptModule.md`.
  - `document/application-doc/v2/data-model/07-ResultsAndCalculation.md`.
  - `document/application-doc/v2/data-model/08-AuditAndConstraints.md`.
  - `document/dev-impl-plan/be/scorebook/041-transcript-result-schema-foundation-2026-08-25.md`.
  - `document/dev-impl-plan/be/scorebook/042-subject-calculation-engine-and-worker-lifecycle-2026-08-25.md`.
  - `document/dev-impl-plan/be/scorebook/044-transcript-aggregation-2026-08-26.md`.
- **Dependencies**: Plan 041/042/044 phải hoàn tất; `regular_dtbmh_cn`, `official_dtbmh_cn`, annual transcript, calculation task và worker lifecycle phải sẵn có.

## 2. Mục tiêu

Triển khai trọn vẹn nghiệp vụ thi lại cuối năm trong một plan duy nhất:

1. Tạo và quản lý một bản ghi `retake_exam` cho mỗi `{student, academicYear, subject}`.
2. Cho phép người có quyền nhập hoặc sửa `retake_score` (`Đtlmh`) với validation, authorization và audit.
3. Khi điểm thi lại thay đổi, tạo calculation task; worker thay `official_dtbmh_cn` của đúng môn và tính lại `final_dtbcn`.
4. Giữ nguyên mọi điểm thường, kết quả học kỳ và `regular_dtbmh_cn`; chỉ cập nhật kết quả chính thức sau thi lại.
5. Phân biệt nguồn kết quả cấp bảng điểm: `REGULAR` hoặc `RETAKE`.

## 3. Requirement và business rule

### 3.1. Retake record

- Bảng `retake_exam` lưu `student_id`, `academic_year_id`, `subject_id`, `pre_retake_score`, `retake_score`, `exam_date`, `status`, người nhập/sửa, timestamp và `note`.
- Unique constraint `(student_id, academic_year_id, subject_id)` bảo đảm một học sinh chỉ có một lần thi lại hợp lệ cho mỗi môn/năm học; học sinh có thể thi lại nhiều môn.
- Khi tạo retake, snapshot `pre_retake_score` từ `StudentSubjectAnnualResult.regular_dtbmh_cn`; không được lấy từ `official_dtbmh_cn` có thể đã bị thay thế.
- Không cho tạo retake nếu annual subject result chưa tồn tại hoặc `regular_dtbmh_cn` là `NULL`.
- `retake_score` là nullable khi `PLANNED`; khi nhập/sửa phải thuộc `[0.0, 10.0]`, scale một chữ số thập phân, không tự làm tròn input có độ chính xác cao hơn.
- Trạng thái tối thiểu: `PLANNED`, `SCORED`, `CANCELLED`. Chỉ retake `SCORED` có `retake_score` được worker áp dụng.
- Không xóa cứng retake đã tạo; hủy dùng `CANCELLED` và vẫn giữ audit history.

### 3.2. Authorization, validation và audit

- Controller chỉ mỏng: nhận request, validation bean và uỷ quyền cho service.
- Chỉ role/quyền giáo vụ theo convention scorebook hiện có được tạo retake, nhập/sửa điểm hoặc hủy; teacher không có quyền tự ý sửa điểm thi lại ngoài policy được phê duyệt.
- Tất cả create, score update, edit, cancel phải phát audit event với actor, giá trị trước/sau, retake id, student, year và subject.
- Không cập nhật `StudentScore`, điểm thành phần, `StudentSubjectTermResult` hoặc `regular_dtbmh_cn` trong flow thi lại.

### 3.3. Worker integration và công thức

- Nhập hoặc sửa `retake_score` phải tăng/đánh dấu source version các transcript liên quan là `IN_PROGRESS` và tạo `CalculationTask` theo lifecycle Plan 042.
- Worker chỉ xử lý retake `SCORED` thuộc annual transcript/task target:

  ```text
  official_dtbmh_cn = retake_score
  calculation_source = RETAKE
  retake_id = retake_exam.retake_id
  ```

- Môn không có retake `SCORED` giữ:

  ```text
  official_dtbmh_cn = regular_dtbmh_cn
  calculation_source = REGULAR
  retake_id = NULL
  ```

- `final_dtbcn` tính từ toàn bộ `official_dtbmh_cn` hợp lệ của các môn học thuật, bằng `BigDecimal`, scale 1 và `RoundingMode.HALF_UP`.
- Môn kỹ năng có thể được ghi nhận retake nếu CR cho phép, nhưng không tham gia tử số hoặc mẫu số của `final_dtbcn`.
- `regular_dtbcn` luôn được giữ nguyên; không tính lại theo `retake_score`.
- Worker phải tuân thủ version protection/idempotency của Plan 042/044: version khớp mới `FINISH`; version cũ giữ `IN_PROGRESS`; chạy lại không tạo duplicate hay ghi sai nguồn kết quả.

### 3.4. Nguồn kết quả cấp annual transcript

| Điều kiện                            | `result_source` |
| ------------------------------------ | --------------- |
| Không có môn nào dùng điểm thi lại   | `REGULAR`       |
| Có ít nhất một môn dùng điểm thi lại | `RETAKE`        |

Giữ nguyên quyết định đơn giản hóa của Plan 041: chỉ dùng `REGULAR` và `RETAKE`; không bổ sung `MIXED` vào migration, enum, DTO, mapping hoặc test.

## 4. Phạm vi

### 4.1. In-scope

- Migration tạo `retake_exam`, indexes/foreign keys/unique constraint và các enum/constraint cần thiết.
- Giữ nhất quán `result_source` cấp annual transcript với hai giá trị `REGULAR`/`RETAKE` của Plan 041.
- Entity, enum, repository, service, controller, DTO request/response cho retake.
- Guard dữ liệu, validation, authorization và audit cho create/update/cancel retake.
- Tạo calculation task khi `retake_score` được nhập hoặc thay đổi.
- Tích hợp worker/recalculation để cập nhật `official_dtbmh_cn`, `calculation_source`, `retake_id`, `final_dtbcn`, `result_source` và lifecycle transcript.
- Unit/integration tests cho persistence, business rules, task lifecycle và công thức recalculation.

### 4.2. Out-of-scope

- Không xác định điều kiện học sinh phải thi lại hoặc xếp loại/lên lớp; điều này thuộc CR riêng.
- Không triển khai Frontend Vue, Storybook, Postman collection hoặc endpoint đọc bảng điểm mới nếu không có yêu cầu độc lập.
- Không thay đổi công thức `regular_dtbmh_cn`, `regular_dtbcn`, `Đtbmh` hoặc `Đtbhk` của Plan 042/044.
- Không xóa/cập nhật đè dữ liệu điểm thường hoặc term result lịch sử.

## 5. Thiết kế kỹ thuật và luồng triển khai

### 5.1. Retake service flow

1. Resolve student, academic year, subject và annual subject result; kiểm tra scope/quyền.
2. Khi tạo, lock/kiểm tra unique tuple; snapshot `regular_dtbmh_cn` vào `pre_retake_score`; tạo `PLANNED` hoặc `SCORED` theo request được phép.
3. Khi score create/update, validate scale/range và transition status; ghi audit.
4. Đánh dấu annual transcript `IN_PROGRESS`, tăng source version theo state service hiện có, tạo calculation task idempotent cho version mới.
5. Khi cancel, không thay đổi regular result; tạo task để worker khôi phục official result về regular value nếu bản retake trước đó đã `SCORED`.

### 5.2. Worker flow

1. Claim task theo cơ chế hiện có và load annual transcript cùng annual subject results/retake records trong transaction.
2. Với từng subject result, chọn `retake_score` của retake `SCORED` hoặc fallback `regular_dtbmh_cn`.
3. Ghi `official_dtbmh_cn`, `calculation_source`, `retake_id`, calculated version/timestamp.
4. Tính `final_dtbcn` chỉ từ môn học thuật có official score hợp lệ; giữ `NULL` nếu không có môn hợp lệ.
5. Suy ra `result_source = RETAKE` khi có ít nhất một retake `SCORED`, ngược lại là `REGULAR`; hoàn tất/lưu transcript theo version protection.
6. Khi task stale hoặc source version đổi, không ghi `FINISH`; task/lifecycle phải theo convention retry/error hiện hữu.

## 6. Khu vực/file dự kiến thay đổi

### Production

- `BE/BaiTap-RS/src/main/resources/db/migration/`: migration mới cho `retake_exam` và foreign key từ annual subject result nếu schema hiện tại chưa có; không đổi constraint `result_source` hai giá trị của Plan 041.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/entity/`: `RetakeExam`, `RetakeExamStatus`, cập nhật `CalculationResultSource`/annual result/transcript mapping.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/repository/`: repository retake và query tối thiểu cho tuple/worker.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/`: `RetakeExamService`, audit/state/task integration và cập nhật `TranscriptRecalculationService`/worker orchestration.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/controller/`: controller retake và DTO request/response liên quan.

Tên file, URL và annotation authorization cuối cùng phải khớp structure/policy scorebook thực tế tại thời điểm implementation.

### Tests

- Test service retake: unique tuple, snapshot pre-retake, range/scale, state transition, cancel và authorization.
- Test worker/recalculation: regular fallback, retake replacement, cancel restoration, `final_dtbcn`, loại trừ skill, `REGULAR`/`RETAKE`, version match/mismatch và idempotency.
- Test migration/repository nếu project đang có integration-test pattern phù hợp.

## 7. Kế hoạch validation

- `./gradlew test`.
- `./gradlew checkstyleMain checkstyleTest`.
- `./gradlew pmdMain pmdTest`.
- `./gradlew build -x test`.
- Đọc JaCoCo cho nhánh retake scored/cancelled, source classification, score range/scale, skill exclusion, version protection và duplicate protection.

## 8. Rủi ro và quyết định cần giữ nhất quán

- **Schema decision**: giữ nguyên `result_source` hai giá trị `REGULAR`/`RETAKE` của Plan 041. Một retake `SCORED` ở bất kỳ môn nào khiến annual transcript có nguồn `RETAKE`.
- **Authorization policy**: phải tái sử dụng authority/role giáo vụ đang tồn tại; nếu policy code không diễn đạt rõ role nào có quyền, cần user xác nhận trước implementation.
- **Điều kiện đủ tư cách thi lại**: không được tự suy diễn từ điểm thấp; retake record chỉ được tạo theo quyền và dữ liệu CR/operation hợp lệ.
- **Concurrency**: create/update cùng tuple phải dựa vào unique constraint và xử lý duplicate hợp lý; worker phải dùng locking/version flow hiện hữu.

## 9. Output dự kiến

- Một API/service foundation hoàn chỉnh để tạo, nhập/sửa, hủy và truy vết retake.
- Một retake `SCORED` thay thế chính xác `official_dtbmh_cn` của đúng môn, không làm mất điểm thường.
- `final_dtbcn` được tính lại từ official score, loại trừ skill subject.
- Annual transcript thể hiện đúng `REGULAR` hoặc `RETAKE`, cùng trạng thái/version có thể audit.

## 10. Approval gate

Plan 045 hợp nhất Plan 046/047 và giữ nguyên quyết định `result_source` hai giá trị của Plan 041. Cần user phê duyệt Plan 045 trước khi tạo migration, chỉnh code production hoặc test.
