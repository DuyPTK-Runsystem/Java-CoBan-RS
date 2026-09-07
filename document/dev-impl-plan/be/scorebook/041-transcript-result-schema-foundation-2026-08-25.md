# Developer Plan 041: Transcript Result Schema Foundation

## 1. Trạng thái và phiên bản áp dụng

- **Status**: `Approved`.
- **Application-document version**: `v2`.
- **Ngày lập plan**: `2026-08-25`.
- **Phê duyệt**: User approved qua agent ngày `2026-08-25`.
- **Module**: Backend `scorebook` (Transcript result domain).
- **Tài liệu tham chiếu**:
  - `document/application-doc/v2/DataStructure.md`
  - `document/application-doc/v2/data-model/07-ResultsAndCalculation.md` (Mục 12: Bảng điểm và kết quả tính toán).
- **Dependencies**: Plan 012 (`V12__create_transcript_calculation_state.sql`), Plan 036 (Scorebook foundation).

### 1.1. Quyết định đã thống nhất
- **Đơn giản hóa `result_source`**: Ở cả cấp môn học (`student_subject_annual_result`) và cấp bảng điểm cả năm của học sinh (`student_annual_transcript`), chỉ sử dụng **2 giá trị**:
  - `REGULAR`: Điểm thông thường (không có môn nào thi lại).
  - `RETAKE`: Có áp dụng điểm thi lại (học sinh có ít nhất một môn thi lại).
  - Loại bỏ hoàn toàn trạng thái `MIXED` để đơn giản hóa mô hình dữ liệu và nghiệp vụ.

---

## 2. Mục tiêu

Thiết lập phần schema, migration, JPA Entity và Repository còn thiếu cho các bảng kết quả điểm học tập theo đặc tả v2:

1. Tạo bảng và entity `student_subject_term_result` (kết quả môn học theo từng học kỳ).
2. Tạo bảng và entity `student_subject_annual_result` (kết quả môn học cả năm).
3. Bổ sung các cột kết quả còn thiếu vào bảng và entity `student_annual_transcript`:
   - `regular_dtbcn DECIMAL(3,1) NULL`
   - `final_dtbcn DECIMAL(3,1) NULL`
   - `result_source VARCHAR(20) NULL` (REGULAR, RETAKE)
   - `last_calculation_task_id BIGINT NULL` (FK `calculation_task`)
4. Bổ sung cột kết quả còn thiếu vào bảng và entity `student_term_transcript`:
   - `dtbhk DECIMAL(3,1) NULL`
5. Tạo các Repository tương ứng hỗ trợ truy vấn kết quả theo transcript và subject.
6. Thiết lập toàn diện các Unique Constraint, Foreign Key, Check Constraint thang điểm [0.0, 10.0] và Index tra cứu.
7. **Ranh giới rõ ràng**: Tuyệt đối không cài đặt công thức tính điểm hay calculation background worker trong plan này.

---

## 3. Requirement liên quan

### 3.1. Data Model (Section 12 - 07-ResultsAndCalculation.md)
- **12.1 Mô hình kết quả**: Cấu trúc phân cấp:
  ```text
  student_annual_transcript
          |
          +── student_term_transcript
          |       |
          |       └── student_subject_term_result
          |
          └── student_subject_annual_result
                      |
                      └── retake_exam
  ```
- **12.2 `student_annual_transcript`**:
  - Bổ sung `regular_dtbcn`, `final_dtbcn`, `result_source` (REGULAR, RETAKE), `last_calculation_task_id`.
- **12.3 `student_term_transcript`**:
  - Bổ sung `dtbhk`.
- **12.4 `student_subject_term_result`**:
  - `term_result_id`, `term_transcript_id`, `class_subject_id`, `subject_id`, `subject_type`, `dtbmh`, `skill_score`, `calculated_version`, `calculated_at`.
  - UQ: `term_transcript_id + subject_id`.
- **12.5 `student_subject_annual_result`**:
  - `annual_subject_result_id`, `annual_transcript_id`, `subject_id`, `hk1_term_result_id`, `hk2_term_result_id`, `retake_id`, `subject_type`, `regular_dtbmh_cn`, `official_dtbmh_cn`, `calculation_source`, `calculated_version`, `calculated_at`, `note`.
  - UQ: `annual_transcript_id + subject_id`.

---

## 4. Phạm vi

### 4.1. In-scope

- **Database Migration (`V16__create_transcript_subject_result_tables.sql`)**:
  - `ALTER TABLE student_annual_transcript` thêm 4 columns và các constraint tương ứng (`CHECK (result_source IN ('REGULAR', 'RETAKE'))`).
  - `ALTER TABLE student_term_transcript` thêm column `dtbhk` và check constraint.
  - `CREATE TABLE student_subject_term_result` với khóa chính, khóa ngoại, unique constraint, check constraints và index.
  - `CREATE TABLE student_subject_annual_result` với khóa chính, khóa ngoại (kèm liên kết HK1, HK2 kết quả học kỳ), unique constraint, check constraints (`CHECK (calculation_source IN ('REGULAR', 'RETAKE'))`) và index.
- **Java Entities & Enums**:
  - Update `StudentAnnualTranscript.java`:
    - `BigDecimal regularDtbcn`
    - `BigDecimal finalDtbcn`
    - `CalculationResultSource resultSource`
    - `Long lastCalculationTaskId`
  - Update `StudentTermTranscript.java`:
    - `BigDecimal dtbhk`
  - Create `StudentSubjectTermResult.java`:
    - Entity mapping chuẩn JPA, `@Table`, `@UniqueConstraint`, `@PrePersist`, `@PreUpdate`.
  - Create `StudentSubjectAnnualResult.java`:
    - Entity mapping chuẩn JPA, `@Table`, `@UniqueConstraint`, `@PrePersist`, `@PreUpdate`.
  - Create enum `CalculationResultSource` (`REGULAR`, `RETAKE`).
- **Repositories**:
  - Create `StudentSubjectTermResultRepository.java`.
  - Create `StudentSubjectAnnualResultRepository.java`.
- **Unit & Slice Tests**:
  - Test persistence, constraints, foreign keys, lifecycle timestamps với DataJpaTest / RepositoryTest.

### 4.2. Out-of-scope

- Không viết công thức tính điểm (Đtbmh, skill_score, Đtbhk, regular_dtbmh_cn, official_dtbmh_cn, dtbcn).
- Không viết worker/service tính toán background.
- Không tạo bảng `retake_exam` (thuộc plan Retake Exam riêng; cột `retake_id` là nullable field).
- Không làm UI/Frontend, Storybook, Postman.

---

## 5. Thiết kế kỹ thuật

### 5.1. SQL Migration

```sql
-- Migration V16: Create transcript subject result tables and enrich transcript schemas

-- 1. Cập nhật student_annual_transcript
ALTER TABLE student_annual_transcript
    ADD COLUMN regular_dtbcn DECIMAL(3,1) NULL AFTER calculated_version,
    ADD COLUMN final_dtbcn DECIMAL(3,1) NULL AFTER regular_dtbcn,
    ADD COLUMN result_source VARCHAR(20) NULL AFTER final_dtbcn,
    ADD COLUMN last_calculation_task_id BIGINT NULL AFTER calculated_at;

ALTER TABLE student_annual_transcript
    ADD CONSTRAINT ck_annual_transcript_result_source CHECK (result_source IN ('REGULAR', 'RETAKE') OR result_source IS NULL),
    ADD CONSTRAINT ck_annual_transcript_regular_dtbcn CHECK (regular_dtbcn IS NULL OR (regular_dtbcn >= 0.0 AND regular_dtbcn <= 10.0)),
    ADD CONSTRAINT ck_annual_transcript_final_dtbcn CHECK (final_dtbcn IS NULL OR (final_dtbcn >= 0.0 AND final_dtbcn <= 10.0)),
    ADD CONSTRAINT fk_annual_transcript_calc_task FOREIGN KEY (last_calculation_task_id) REFERENCES calculation_task (task_id);

-- 2. Cập nhật student_term_transcript
ALTER TABLE student_term_transcript
    ADD COLUMN dtbhk DECIMAL(3,1) NULL AFTER calculated_version;

ALTER TABLE student_term_transcript
    ADD CONSTRAINT ck_term_transcript_dtbhk CHECK (dtbhk IS NULL OR (dtbhk >= 0.0 AND dtbhk <= 10.0));

-- 3. Tạo bảng student_subject_term_result
CREATE TABLE student_subject_term_result (
    term_result_id BIGINT NOT NULL AUTO_INCREMENT,
    term_transcript_id BIGINT NOT NULL,
    class_subject_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    subject_type VARCHAR(20) NOT NULL,
    dtbmh DECIMAL(3,1) NULL,
    skill_score DECIMAL(3,1) NULL,
    calculated_version BIGINT NULL,
    calculated_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (term_result_id),
    CONSTRAINT uk_subject_term_result UNIQUE (term_transcript_id, subject_id),
    CONSTRAINT ck_subject_term_result_type CHECK (subject_type IN ('NORMAL', 'SKILL', 'ACADEMIC')),
    CONSTRAINT ck_subject_term_result_dtbmh CHECK (dtbmh IS NULL OR (dtbmh >= 0.0 AND dtbmh <= 10.0)),
    CONSTRAINT ck_subject_term_result_skill CHECK (skill_score IS NULL OR (skill_score >= 0.0 AND skill_score <= 10.0)),
    CONSTRAINT fk_subject_term_result_transcript FOREIGN KEY (term_transcript_id) REFERENCES student_term_transcript (term_transcript_id),
    CONSTRAINT fk_subject_term_result_class_subject FOREIGN KEY (class_subject_id) REFERENCES class_subject (class_subject_id),
    CONSTRAINT fk_subject_term_result_subject FOREIGN KEY (subject_id) REFERENCES subject (subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_subject_term_result_transcript ON student_subject_term_result (term_transcript_id);
CREATE INDEX idx_subject_term_result_class_subject ON student_subject_term_result (class_subject_id);

-- 4. Tạo bảng student_subject_annual_result
CREATE TABLE student_subject_annual_result (
    annual_subject_result_id BIGINT NOT NULL AUTO_INCREMENT,
    annual_transcript_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    hk1_term_result_id BIGINT NULL,
    hk2_term_result_id BIGINT NULL,
    retake_id BIGINT NULL,
    subject_type VARCHAR(20) NOT NULL,
    regular_dtbmh_cn DECIMAL(3,1) NULL,
    official_dtbmh_cn DECIMAL(3,1) NULL,
    calculation_source VARCHAR(20) NULL,
    calculated_version BIGINT NULL,
    calculated_at TIMESTAMP NULL,
    note VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (annual_subject_result_id),
    CONSTRAINT uk_subject_annual_result UNIQUE (annual_transcript_id, subject_id),
    CONSTRAINT ck_subject_annual_result_type CHECK (subject_type IN ('NORMAL', 'SKILL', 'ACADEMIC')),
    CONSTRAINT ck_subject_annual_result_source CHECK (calculation_source IN ('REGULAR', 'RETAKE') OR calculation_source IS NULL),
    CONSTRAINT ck_subject_annual_result_regular_dtbmh CHECK (regular_dtbmh_cn IS NULL OR (regular_dtbmh_cn >= 0.0 AND regular_dtbmh_cn <= 10.0)),
    CONSTRAINT ck_subject_annual_result_official_dtbmh CHECK (official_dtbmh_cn IS NULL OR (official_dtbmh_cn >= 0.0 AND official_dtbmh_cn <= 10.0)),
    CONSTRAINT fk_subject_annual_result_transcript FOREIGN KEY (annual_transcript_id) REFERENCES student_annual_transcript (annual_transcript_id),
    CONSTRAINT fk_subject_annual_result_subject FOREIGN KEY (subject_id) REFERENCES subject (subject_id),
    CONSTRAINT fk_subject_annual_result_hk1 FOREIGN KEY (hk1_term_result_id) REFERENCES student_subject_term_result (term_result_id),
    CONSTRAINT fk_subject_annual_result_hk2 FOREIGN KEY (hk2_term_result_id) REFERENCES student_subject_term_result (term_result_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_subject_annual_result_transcript ON student_subject_annual_result (annual_transcript_id);
CREATE INDEX idx_subject_annual_result_subject ON student_subject_annual_result (subject_id);
```

### 5.2. Danh sách file thay đổi & tạo mới

```text
BE/BaiTap-RS/
├── src/main/resources/db/migration/
│   └── [NEW] V16__create_transcript_subject_result_tables.sql
├── src/main/java/com/JavaTraining/BaiTap_RS/scorebook/
│   ├── domain/entity/
│   │   ├── [MODIFY] StudentAnnualTranscript.java
│   │   ├── [MODIFY] StudentTermTranscript.java
│   │   ├── [NEW] StudentSubjectTermResult.java
│   │   ├── [NEW] StudentSubjectAnnualResult.java
│   │   └── [NEW] CalculationResultSource.java (Enum REGULAR, RETAKE)
│   └── repository/
│       ├── [NEW] StudentSubjectTermResultRepository.java
│       └── [NEW] StudentSubjectAnnualResultRepository.java
└── src/test/java/com/JavaTraining/BaiTap_RS/scorebook/
    └── [NEW] repository/StudentSubjectResultRepositoryTest.java
```

---

## 6. Kế hoạch Unit Test & Validation

### 6.1. Unit / Slice Test
- Tạo `StudentSubjectResultRepositoryTest.java` kế thừa cấu hình test JPA hiện có:
  1. Test CRUD & mapping cho `StudentSubjectTermResult` (bao gồm `dtbmh`, `skillScore`, `calculatedVersion`, `calculatedAt`).
  2. Test CRUD & mapping cho `StudentSubjectAnnualResult` (bao gồm `regularDtbmhCn`, `officialDtbmhCn`, `calculationSource`, FK `hk1TermResultId`, `hk2TermResultId`).
  3. Test vi phạm `uk_subject_term_result` khi insert 2 record cùng `(termTranscriptId, subjectId)`.
  4. Test vi phạm `uk_subject_annual_result` khi insert 2 record cùng `(annualTranscriptId, subjectId)`.
  5. Test cập nhật các trường mới trên `StudentAnnualTranscript` (`regularDtbcn`, `finalDtbcn`, `resultSource`, `lastCalculationTaskId`) và `StudentTermTranscript` (`dtbhk`).
  6. Test `@PrePersist` và `@PreUpdate` tự động cập nhật timestamp.

### 6.2. Validation Lệnh
- `./gradlew test --tests "com.JavaTraining.BaiTap_RS.scorebook.repository.*"`
- `./gradlew checkstyleMain checkstyleTest`
- `./gradlew pmdMain pmdTest`
- `./gradlew build -x test`

---

## 7. Dev Note
Dev Note đã được cập nhật sau khi hoàn thành triển khai tại:
`document/dev-note/be/scorebook/041-transcript-result-schema-foundation-2026-08-25.md`
