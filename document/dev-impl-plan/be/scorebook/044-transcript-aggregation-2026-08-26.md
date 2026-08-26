# Developer Plan 044: Term and Annual Transcript Aggregation

## 1. Trạng thái và thông tin chung

- **Status**: `Approved`.
- **Application-document version**: `v2`.
- **Ngày lập plan**: `2026-08-26`.
- **Phê duyệt**: User phê duyệt qua tin nhắn agent ngày `2026-08-26`.
- **Module**: Backend `scorebook` / Transcript calculation.
- **Tài liệu tham chiếu**:
  - `document/application-doc/v2/modules/06-RetakeAndTranscriptModule.md`.
  - `document/application-doc/v2/modules/05-ScoreChangeAndCalculationModule.md`.
  - `document/application-doc/v2/data-model/07-ResultsAndCalculation.md`.
  - `document/application-doc/v2/data-model/09-MigrationAndJPA.md`.
  - `document/dev-impl-plan/be/scorebook/041-transcript-result-schema-foundation-2026-08-25.md`.
  - `document/dev-impl-plan/be/scorebook/042-subject-calculation-engine-and-worker-lifecycle-2026-08-25.md`.
- **Dependencies**: Plan 041 (transcript result schema), Plan 042 (subject calculation engine, worker lifecycle và version protection), Plan 037/038 (score source và calculation task state).

## 2. Mục tiêu

Hoàn thiện flow tổng hợp bảng điểm theo hai cấp:

1. **Tổng kết học kỳ**: tính và lưu `Đtbmh` từng môn, `Đtbhk` của học sinh trong từng học kỳ.
2. **Tổng kết cả năm**: tính và lưu `ĐtbmhCN` từng môn, `Đtbcn` của học sinh trong năm học.

Kết quả phải được lưu theo đúng `source_version`/`calculated_version`, không coi dữ liệu chưa đủ là điểm `0`, loại trừ môn kỹ năng khỏi công thức trung bình môn học thuật, và chỉ chuyển transcript sang `FINISH` khi toàn bộ kết quả được tính từ đúng version nguồn mới nhất.

## 3. Requirement và business rule

### 3.1. Tổng kết học kỳ

- Với mỗi `{student, academicYear, semester, subject}`, upsert một `StudentSubjectTermResult`.
- Môn thường/học thuật lưu `dtbmh`; môn kỹ năng lưu `skillScore` theo quy ước enum và entity hiện có.
- `Đtbhk` được tính từ các `dtbmh` hợp lệ của môn thường/học thuật trong học kỳ.
- Môn kỹ năng không tham gia công thức `Đtbhk`.
- Điểm `0.0` là điểm hợp lệ và phải tham gia tử số/mẫu số.
- Ô điểm `NULL`, chưa nhập, vắng, miễn hoặc hủy không được tự động chuyển thành `0`.
- Nếu chưa có môn thường/học thuật nào có kết quả hợp lệ, lưu `dtbhk = NULL`.
- Làm tròn kết quả đến một chữ số thập phân bằng `HALF_UP`.

### 3.2. Tổng kết cả năm

- Với mỗi `{student, academicYear, subject}`, upsert một `StudentSubjectAnnualResult`.
- Môn học trong cả hai học kỳ có đủ kết quả:

  ```text
  ĐtbmhCN = ROUND((HK1 + 2 * HK2) / 3, 1)
  ```

- Nếu môn chỉ được giảng dạy trong một học kỳ, lấy kết quả của học kỳ đó theo quy tắc đã thống nhất trong engine.
- Nếu môn cả năm mới có một học kỳ, chưa tính `ĐtbmhCN` chính thức; lưu `NULL` và ghi nhận là dữ liệu chưa đủ.
- `Đtbcn` được tính từ các `regular_dtbmh_cn` hợp lệ của môn thường/học thuật; môn kỹ năng bị loại khỏi mẫu số và tử số.
- Khi chưa có môn thường/học thuật nào có `regular_dtbmh_cn` hợp lệ, lưu `regular_dtbcn = NULL` và `final_dtbcn = NULL`.
- Trước nghiệp vụ thi lại, `official_dtbmh_cn = regular_dtbmh_cn`, `calculation_source = REGULAR`, và `result_source = REGULAR`.

### 3.3. Trạng thái và version

- Khi nguồn điểm thay đổi, transcript liên quan ở `IN_PROGRESS` và `source_version` được tăng bởi flow hiện có.
- Worker nhận `requestedVersion`/version của calculation task và ghi `calculated_version` cho các kết quả được tạo.
- Chỉ chuyển `StudentTermTranscript` và `StudentAnnualTranscript` sang `FINISH` khi:

  ```text
  requestedVersion == sourceVersion
  ```

- Nếu dữ liệu nguồn thay đổi trong lúc tính (`sourceVersion > requestedVersion`), vẫn có thể lưu kết quả trung gian nhưng transcript phải giữ `IN_PROGRESS`.
- Lỗi worker hoặc thiếu dependency bắt buộc không được chuyển transcript sang `FINISH`.
- Flow phải idempotent: chạy lại cùng version không tạo bản ghi trùng theo các unique key hiện có.

## 4. Phạm vi

### 4.1. In-scope

- Hoàn thiện orchestration trong `TranscriptRecalculationService` theo thứ tự:

  ```text
  StudentScore
    -> StudentSubjectTermResult
    -> StudentTermTranscript (dtbhk)
    -> StudentSubjectAnnualResult (regular_dtbmh_cn)
    -> StudentAnnualTranscript (regular_dtbcn/final_dtbcn)
  ```

- Bảo đảm công thức tổng hợp học kỳ và cả năm sử dụng `BigDecimal`, scale 1 và `RoundingMode.HALF_UP`.
- Bảo đảm filter môn kỹ năng được áp dụng nhất quán ở `Đtbhk` và `Đtbcn`.
- Bảo đảm dữ liệu thiếu giữ `NULL`, không làm sai trung bình bằng cách tính thiếu thành `0`.
- Upsert các entity transcript/result hiện có, cập nhật timestamp, source/calculated version và calculation status.
- Bổ sung hoặc điều chỉnh unit test cho engine và orchestration, tập trung vào công thức, dữ liệu thiếu, môn kỹ năng, version match/mismatch và idempotency.
- Đồng bộ DTO/service response nếu API đọc transcript hiện có cần expose `Đtbhk`, `ĐtbmhCN`, `Đtbcn`, trạng thái và version.

### 4.2. Out-of-scope

- Không triển khai nghiệp vụ thi lại, không ghi đè kết quả chính thức bằng `Đtlmh`; phần này thuộc plan Retake riêng.
- Không thay đổi migration/schema đã được Plan 041 thiết lập.
- Không thay đổi công thức điểm thành phần hoặc trọng số môn kỹ năng đã thuộc Plan 042, trừ khi cần gọi đúng API engine hiện có.
- Không tính tổng kết trực tiếp trong HTTP request; calculation phải chạy qua worker/background flow của Plan 042.
- Không triển khai Frontend Vue, Storybook hoặc thay đổi Postman collection nếu không có yêu cầu riêng.
- Không thay đổi các API v1/v2 ngoài contract transcript cần thiết cho kết quả aggregation.

## 5. Thiết kế kỹ thuật và luồng triển khai

### 5.1. Term aggregation

1. Worker load `StudentAnnualTranscript`, enrollment ACTIVE, danh sách semester và `ClassSubject` còn hiệu lực.
2. Với từng môn, gọi `SubjectScoreCalculator`/engine hiện có để tính `dtbmh` hoặc `skillScore` từ score hợp lệ.
3. Upsert `StudentSubjectTermResult` theo `(term_transcript_id, subject_id)`.
4. Gom các `dtbmh` của môn thường/học thuật có dữ liệu và tính `dtbhk`.
5. Set `calculated_version`, `calculated_at` và status của term transcript theo version protection.

### 5.2. Annual aggregation

1. Gom term result theo `subjectId` và xác định kết quả HK1/HK2.
2. Tính `regular_dtbmh_cn` theo công thức cả năm; không tính khi môn hai học kỳ thiếu một kết quả bắt buộc.
3. Upsert `StudentSubjectAnnualResult`, giữ nguyên kết quả từng học kỳ và set nguồn `REGULAR`.
4. Gom `regular_dtbmh_cn` hợp lệ của môn thường/học thuật để tính `regular_dtbcn`.
5. Ở giai đoạn chưa có retake, set `final_dtbcn = regular_dtbcn`.
6. Set `calculated_version`, `calculated_at`, `last_calculation_task_id` và status annual transcript.

### 5.3. Consistency boundary

- Toàn bộ kết quả của một lần recalculate được commit trong transaction của worker.
- Không chuyển `FINISH` riêng lẻ cho một term/annual transcript nếu version đã cũ.
- Nếu có dữ liệu nguồn mới hơn trong hoặc trước lúc commit, giữ `IN_PROGRESS` để task/version mới tính lại.
- Các unique constraint của schema là lớp bảo vệ cuối cùng; service phải dùng query/upsert để tránh duplicate trước khi save.

## 6. Khu vực/file dự kiến thay đổi

### Production

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/TranscriptRecalculationService.java`: hoàn thiện orchestration term/annual aggregation, version protection và mapping result.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/SubjectScoreCalculator.java`: chỉ chỉnh nếu API hiện có chưa đáp ứng công thức/rounding được plan yêu cầu.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/entity/StudentTermTranscript.java`: chỉ chỉnh mapping/accessor nếu thiếu `dtbhk` hoặc status/version field.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/entity/StudentAnnualTranscript.java`: chỉ chỉnh mapping/accessor nếu thiếu `regularDtbcn`, `finalDtbcn`, source/status/version field.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/entity/StudentSubjectTermResult.java`: chỉ chỉnh nếu cần lưu đúng `dtbmh`/`skillScore` và calculated version.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/entity/StudentSubjectAnnualResult.java`: chỉ chỉnh nếu cần lưu đúng `regularDtbmhCn`, `officialDtbmhCn`, source và version.
- Repository transcript/result liên quan: chỉ bổ sung query tối thiểu nếu orchestration chưa có query cần thiết.

### Tests

- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/scorebook/service/SubjectScoreCalculatorTest.java`.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/scorebook/service/TranscriptRecalculationServiceTest.java`.
- Repository/integration test transcript hiện có nếu cần kiểm chứng unique key và persistence contract.

Không chốt cứng việc tạo file mới ngoài các khu vực trên; tên file cuối cùng phải theo pattern hiện tại sau khi kiểm tra implementation.

## 7. Kế hoạch kiểm thử và validation

### 7.1. Unit test bắt buộc

- `Đtbmh`/term result: nhiều cột điểm hợp lệ, gồm cả điểm `0.0`; bỏ qua trạng thái không hợp lệ; không có điểm hợp lệ trả `NULL`; môn kỹ năng có `skillScore` nhưng không đi vào `Đtbhk`.
- `Đtbhk`: trung bình đúng từ các môn thường/học thuật; không có môn hợp lệ trả `NULL`; làm tròn `HALF_UP` đến 0.1.
- `ĐtbmhCN`: đủ HK1/HK2 áp dụng `(HK1 + 2 * HK2) / 3`; thiếu một học kỳ của môn cả năm trả `NULL`; môn chỉ học một học kỳ lấy đúng điểm học kỳ đó.
- `Đtbcn`: chỉ tính `regular_dtbmh_cn` hợp lệ của môn thường/học thuật; loại trừ môn kỹ năng; không có dữ liệu trả `NULL`; `final_dtbcn` bằng `regular_dtbcn` trước retake.
- Version/lifecycle: version khớp -> `FINISH`; version lệch -> `IN_PROGRESS`; calculation thất bại -> không `FINISH`; chạy lại cùng version -> không duplicate.

### 7.2. Backend validation

- `./gradlew test`.
- `./gradlew checkstyleMain checkstyleTest`.
- `./gradlew pmdMain pmdTest`.
- `./gradlew build -x test`.
- Đọc JaCoCo report cho các nhánh công thức, dữ liệu thiếu, loại trừ môn kỹ năng và version protection; không tự đặt threshold mới.

## 8. Rủi ro và assumption

- **Enum subject type**: tài liệu v2 dùng `NORMAL`/`SKILL`, trong khi code hiện tại có thể dùng tên enum khác như `ACADEMIC`/`SKILL`; implementation phải map theo enum thực tế, không đổi enum hoặc schema ngoài scope.
- **Số học kỳ**: plan giả định năm học có HK1 và HK2 theo contract v2; nếu database có thêm semester, phải xác định rõ quy tắc trước khi tính annual score.
- **Dữ liệu thiếu**: kết quả `NULL` là trạng thái chưa đủ dữ liệu, không phải lỗi và không được thay bằng `0.0`.
- **Concurrency**: version protection chỉ an toàn nếu đọc/ghi transcript và source version trong transaction/locking pattern hiện có của Plan 042.
- **Retake**: annual aggregation chỉ tạo regular result; không làm thay đổi `official_dtbmh_cn` theo retake trong plan này.

## 9. Output dự kiến

- Mỗi học sinh/năm học có kết quả tổng hợp học kỳ và cả năm được lưu đúng entity/result tương ứng.
- `Đtbhk` và `Đtbcn` không bao gồm môn kỹ năng.
- `ĐtbmhCN` dùng đúng công thức `(HK1 + 2 * HK2) / 3` cho môn học cả năm.
- Dữ liệu chưa đủ vẫn thể hiện bằng `NULL`/trạng thái đang cập nhật.
- Transcript chỉ ở `FINISH` khi toàn bộ kết quả khớp version nguồn; ngược lại giữ `IN_PROGRESS`.

## 10. Approval gate

- Plan này áp dụng `v2` theo xác nhận của user ngày `2026-08-26`.
- Cần user phê duyệt rõ ràng plan 044 qua agent trước khi chỉnh sửa production code hoặc test.
- Mọi thay đổi sang retake, schema, FE hoặc API ngoài phạm vi aggregation phải được xác nhận lại trước khi triển khai.
