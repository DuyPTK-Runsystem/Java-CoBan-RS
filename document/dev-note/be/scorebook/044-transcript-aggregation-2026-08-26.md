# Dev Note: Kế hoạch 044 - Term and Annual Transcript Aggregation

- **Kế hoạch liên quan**: [`document/dev-impl-plan/be/scorebook/044-transcript-aggregation-2026-08-26.md`](../../../../dev-impl-plan/be/scorebook/044-transcript-aggregation-2026-08-26.md)
- **Trạng thái phê duyệt**: `Approved` (user phê duyệt qua agent ngày 2026-08-26)
- **Trạng thái triển khai**: `Implemented`
- **Application-document version**: `v2`
- **Ngày ghi nhận**: 2026-08-26

## 1. Phạm vi thực tế hoàn thành

- Hoàn thiện luồng tổng hợp điểm theo hai cấp trong `TranscriptRecalculationService`:
  1. **Tổng kết học kỳ**:
     - Tính `dtbmh` cho môn học thuật và `skillScore` cho môn kỹ năng thông qua `SubjectScoreCalculator`.
     - Lưu `StudentSubjectTermResult` tương ứng với mỗi `{termTranscript, subject}`, gán `calculatedVersion = requestedVersion` và `calculatedAt`.
     - Tính `Đtbhk` chỉ từ các môn học thuật có điểm hợp lệ; loại trừ môn kỹ năng khỏi công thức `Đtbhk`.
     - Giữ `dtbhk = null` khi chưa có môn học thuật nào có điểm.
     - Cập nhật `StudentTermTranscript`: gán `calculatedVersion`, `calculatedAt`, xóa `lastError` khi trạng thái đạt `FINISH` (khi `currentSourceVersion == requestedVersion`), ngược lại giữ `IN_PROGRESS`.
  2. **Tổng kết cả năm**:
     - Tổng hợp kết quả từng môn qua các học kỳ (sắp xếp theo `endDate ASC`).
     - Với môn học cả năm (có cấu hình môn ở cả 2 học kỳ): tính `ĐtbmhCN = round((HK1 + 2 * HK2) / 3, 1)` khi cả 2 học kỳ đều có điểm; nếu thiếu 1 học kỳ thì giữ `regularDtbmhCn = null`.
     - Với môn học chỉ dạy trong 1 học kỳ: lấy trực tiếp điểm của học kỳ đó.
     - Môn kỹ năng: không tính `regularDtbmhCn` (giữ `null`).
     - Lưu `StudentSubjectAnnualResult` với `calculationSource = REGULAR`, `officialDtbmhCn = regularDtbmhCn`, gán `calculatedVersion` và `calculatedAt`.
     - Tính `Đtbcn` chỉ từ các `regularDtbmhCn` hợp lệ của môn học thuật; loại trừ môn kỹ năng.
     - Gán `finalDtbcn = regularDtbcn` và `resultSource = REGULAR` (trước giai đoạn thi lại).
     - Cập nhật `StudentAnnualTranscript`: `calculatedVersion`, `calculatedAt`, `lastCalculationTaskId`, xóa `lastError` và chuyển sang `FINISH` khi `currentSourceVersion == requestedVersion`, ngược lại giữ `IN_PROGRESS`.
- Đảm bảo tính idempotent: chạy lại cùng version hoặc version mới cập nhật bản ghi hiện có theo các unique key (`uk_subject_term_result`, `uk_subject_annual_result`, `uk_term_transcript_annual_semester`, `uk_annual_transcript_student_year`).
- Thêm unit test chi tiết bao phủ:
  - Tính điểm môn kỹ năng, tách biệt với môn học thuật, loại trừ môn kỹ năng khỏi `Đtbhk` và `Đtbcn`.
  - Xử lý môn học 1 học kỳ và môn học cả năm thiếu điểm học kỳ.
  - Kiểm tra `calculatedVersion` được thiết lập chính xác trên tất cả các entity kết quả.
  - Bảo vệ version (`FINISH` vs `IN_PROGRESS`).

## 2. File thay đổi

### Production

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/TranscriptRecalculationService.java`: Cập nhật `calculateSubjectResult` để nhận và gán `requestedVersion` cho `StudentSubjectTermResult`, xóa `lastError` trên `StudentTermTranscript` khi hoàn tất.

### Tests

- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/scorebook/service/TranscriptRecalculationServiceTest.java`: Bổ sung test case và helper cho môn kỹ năng, môn 1 học kỳ, thiếu điểm và kiểm tra calculated version.

### Summary & Documentation

- `document/dev-impl-plan/be/scorebook/044-transcript-aggregation-2026-08-26.md`
- `document/dev-impl-plan/be/BE_DEV_PLAN_SUMMARY.md`
- `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md`
- `document/dev-note/be/scorebook/044-transcript-aggregation-2026-08-26.md`
- `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md`
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`

## 3. Quyết định kỹ thuật

- Môn kỹ năng không tham gia vào tử số và mẫu số của điểm trung bình học kỳ (`Đtbhk`) và điểm trung bình cả năm (`Đtbcn`).
- Môn học cả năm khi chưa có đủ điểm 2 học kỳ sẽ lưu `regularDtbmhCn = null` để thể hiện trạng thái chưa hoàn tất, không coi là điểm `0.0`.
- Toàn bộ kết quả tính toán (`StudentSubjectTermResult`, `StudentTermTranscript`, `StudentSubjectAnnualResult`, `StudentAnnualTranscript`) đều được gắn `calculatedVersion = requestedVersion` để phục vụ audit và truy vết version.
- Giữ nguyên `finalDtbcn = regularDtbcn` và `officialDtbmhCn = regularDtbmhCn` ở giai đoạn trước khi nghiệp vụ thi lại (Retake) được kích hoạt.

## 4. Validation Result

| Kiểm tra   | Lệnh                                      | Trạng thái | Ghi chú                                                                                    |
| ---------- | ----------------------------------------- | ---------- | ------------------------------------------------------------------------------------------ |
| Test       | `./gradlew test`                          | **PASS**   | 256 tests chạy thành công, 0 fail, 0 skip.                                                 |
| Checkstyle | `./gradlew checkstyleMain checkstyleTest` | **PASS**   | 0 vi phạm style trên cả main và test.                                                      |
| PMD        | `./gradlew pmdMain pmdTest`               | **PASS**   | 0 lỗi PMD (chỉ có log baseline misconfigured rule `LoosePackageCoupling` không ảnh hưởng). |
| Build      | `./gradlew build -x test`                 | **PASS**   | Build thành công toàn bộ artifacts backend.                                                |

### JaCoCo Coverage

- `TranscriptRecalculationService`: 95% instruction coverage, 78% branch coverage.
- `SubjectScoreCalculator`: 95% instruction coverage, 80% branch coverage.
- `CalculationTaskWorker`: 100% instruction coverage, 100% branch coverage.

## 5. Sai lệch so với Developer Plan

- Không có sai lệch so với Developer Plan 044 đã được phê duyệt.

## 6. Known risks và bước tiếp theo

- Nghiệp vụ thi lại (Retake) và tính `official_dtbmh_cn`, `Đtlmh` sẽ được triển khai ở plan Retake tiếp theo (Plan 045+).
- Hiện tại `TranscriptRecalculationService` chạy qua `CalculationTaskWorker` nền và đã có pessimistic locking bảo vệ `sourceVersion`.

