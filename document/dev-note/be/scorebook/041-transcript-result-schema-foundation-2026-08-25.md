# Dev Note: Kế hoạch 041 - Transcript Result Schema Foundation

- **Kế hoạch liên quan**: [`document/dev-impl-plan/be/scorebook/041-transcript-result-schema-foundation-2026-08-25.md`](../../../../dev-impl-plan/be/scorebook/041-transcript-result-schema-foundation-2026-08-25.md)
- **Trạng thái phê duyệt**: `Approved` (2026-08-25)
- **Trạng thái triển khai**: `Completed`
- **Application-document version**: `v2`
- **Ngày ghi nhận**: 2026-08-25

---

## 1. Phạm vi thực tế hoàn thành

- Tạo Flyway migration `V16__create_transcript_subject_result_tables.sql`:
  - Bổ sung các field kết quả vào `student_annual_transcript` và `student_term_transcript`.
  - Tạo `student_subject_term_result` và `student_subject_annual_result`.
  - Thêm unique constraint, check constraint thang điểm, foreign key và index theo plan.
- Cập nhật `StudentAnnualTranscript` với `regularDtbcn`, `finalDtbcn`, `resultSource` và `lastCalculationTaskId`.
- Cập nhật `StudentTermTranscript` với `dtbhk`.
- Tạo entity `StudentSubjectTermResult`, `StudentSubjectAnnualResult`, enum `CalculationResultSource` và hai repository truy vấn theo transcript/subject.
- Không triển khai công thức tính điểm, calculation worker, `retake_exam`, frontend hoặc Postman.

## 2. File thay đổi

### Developer Plan và summaries

- `document/dev-impl-plan/be/scorebook/041-transcript-result-schema-foundation-2026-08-25.md`
- `document/dev-impl-plan/be/BE_DEV_PLAN_SUMMARY.md`
- `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md`

### Backend production và test

- `BE/BaiTap-RS/src/main/resources/db/migration/V16__create_transcript_subject_result_tables.sql`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/entity/StudentAnnualTranscript.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/entity/StudentTermTranscript.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/entity/CalculationResultSource.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/entity/StudentSubjectTermResult.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/entity/StudentSubjectAnnualResult.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/repository/StudentSubjectTermResultRepository.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/repository/StudentSubjectAnnualResultRepository.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/config/ScorebookFlywayMigrationTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/scorebook/repository/StudentSubjectResultRepositoryTest.java`

### Dev Note

- `document/dev-note/be/scorebook/041-transcript-result-schema-foundation-2026-08-25.md`
- `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md`
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`

## 3. Quyết định và phạm vi giữ nguyên

- Áp dụng tài liệu ứng dụng `v2`.
- Giữ nguyên quyết định `result_source`/`calculation_source` chỉ có `REGULAR` và `RETAKE`; không thêm `MIXED`.
- Entity dùng `SubjectType` hiện có của project (`ACADEMIC`, `SKILL`); migration vẫn chấp nhận `NORMAL` cùng các giá trị plan yêu cầu để giữ schema tương thích.
- `retake_id` được giữ nullable và chưa tạo foreign key vì bảng `retake_exam` nằm ngoài scope.
- Không mở rộng sang công thức tính điểm, calculation worker, frontend hoặc Postman.

## 4. Validation

| Công cụ / Lệnh | Trạng thái | Ghi chú |
| --- | --- | --- |
| `./gradlew test --tests "com.JavaTraining.BaiTap_RS.scorebook.repository.*" --tests "com.JavaTraining.BaiTap_RS.config.ScorebookFlywayMigrationTest"` | **PASS** | Focused repository/migration tests pass; 8 test cases của Plan 041. |
| `./gradlew test jacocoTestReport` | **PASS** | Full test suite pass; JaCoCo XML/HTML report được tạo. |
| `./gradlew checkstyleMain checkstyleTest` | **PASS** | Không còn warning từ file Plan 041; còn 1 warning line length baseline ở `academic/domain/entity/SemesterCompletenessNotification.java`. |
| `./gradlew pmdMain pmdTest` | **PASS** | Không còn PMD violation do Plan 041; Gradle vẫn hiển thị warning deprecated của test cũ ngoài scope. |
| `./gradlew build` | **PASS** | Build và `bootJar` hoàn thành. |

JaCoCo report: `BE/BaiTap-RS/build/reports/jacoco/test/jacocoTestReport.xml` và `BE/BaiTap-RS/build/reports/jacoco/test/html/index.html`.

## 5. Deviations, blockers và bước tiếp theo

- **Deviation**:
  - Tách các `ADD COLUMN` và `ADD CONSTRAINT` trong migration thành từng câu lệnh vì H2 2.4 không hỗ trợ dạng multi-add; schema MySQL không thay đổi.
  - Dùng `@SpringBootTest` với H2 `create-drop` cho repository test vì project chưa có dependency cung cấp `@DataJpaTest`; không thêm dependency ngoài plan.
  - Mở rộng `ScorebookFlywayMigrationTest` để kiểm tra trực tiếp bảng/cột/constraint/index/FK của V16.
- **Blocker**: Không có blocker kỹ thuật còn lại. Gradle cache được đặt ở `/tmp/java-coban-gradle` vì cache mặc định ngoài workspace không có quyền ghi.
- **Rủi ro còn lại**: Công thức tính điểm và worker chưa có theo đúng out-of-scope; các FK đến `retake_exam` sẽ thuộc plan riêng.
- **Bước tiếp theo**: Có thể triển khai plan calculation/retake riêng nếu được phê duyệt; Plan 041 không còn hạng mục bắt buộc chưa hoàn thành.
