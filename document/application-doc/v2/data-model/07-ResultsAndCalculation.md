# Results and Calculation

## 12. Bảng điểm và kết quả tính toán

### 12.1. Mô hình kết quả

~~~text
student_annual_transcript
        |
        +── student_term_transcript
        |       |
        |       └── student_subject_term_result
        |
        └── student_subject_annual_result
                    |
                    └── retake_exam
~~~

### 12.2. student_annual_transcript

~~~text
student_annual_transcript
-------------------------
PK  annual_transcript_id  BIGINT UNSIGNED
FK  student_id            BIGINT UNSIGNED NOT NULL
FK  academic_year_id      BIGINT UNSIGNED NOT NULL
    calculation_status    IN_PROGRESS | FINISH
    source_version        BIGINT UNSIGNED NOT NULL
    calculated_version    BIGINT UNSIGNED NULL
    regular_dtbcn         DECIMAL(3,1) NULL
    final_dtbcn           DECIMAL(3,1) NULL
    result_source          REGULAR | RETAKE | MIXED
    calculated_at          DATETIME NULL
FK  last_calculation_task_id BIGINT UNSIGNED NULL
    last_error             VARCHAR(2000) NULL
    created_at             DATETIME NOT NULL
    updated_at             DATETIME NOT NULL
UQ  student_id + academic_year_id
~~~

Ý nghĩa:

- regular_dtbcn là Đtbcn trước khi áp dụng điểm thi lại.
- final_dtbcn là Đtbcn chính thức sau khi áp dụng điểm thi lại.
- Nếu học sinh không có môn thi lại, final_dtbcn bằng regular_dtbcn.
- result_source là MIXED nếu một số môn dùng điểm thường và một số môn dùng điểm thi lại.

Bảng chỉ được chuyển FINISH khi:

~~~text
calculated_version = source_version
~~~

### 12.3. student_term_transcript

~~~text
student_term_transcript
-----------------------
PK  term_transcript_id   BIGINT UNSIGNED
FK  annual_transcript_id BIGINT UNSIGNED NOT NULL
FK  semester_id          BIGINT UNSIGNED NOT NULL
FK  student_id           BIGINT UNSIGNED NOT NULL
    calculation_status   IN_PROGRESS | FINISH
    source_version       BIGINT UNSIGNED NOT NULL
    calculated_version   BIGINT UNSIGNED NULL
    dtbhk                DECIMAL(3,1) NULL
    calculated_at        DATETIME NULL
    created_at           DATETIME NOT NULL
    updated_at           DATETIME NOT NULL
UQ  annual_transcript_id + semester_id
~~~

Đtbhk chỉ tính từ các môn NORMAL đã phát sinh Đtbmh. Môn SKILL không tham gia công thức Đtbhk theo requirement.

### 12.4. student_subject_term_result

~~~text
student_subject_term_result
---------------------------
PK  term_result_id       BIGINT UNSIGNED
FK  term_transcript_id   BIGINT UNSIGNED NOT NULL
FK  class_subject_id     BIGINT UNSIGNED NOT NULL
FK  subject_id           BIGINT UNSIGNED NOT NULL
    subject_type         NORMAL | SKILL
    dtbmh                DECIMAL(3,1) NULL
    skill_score          DECIMAL(3,1) NULL
    calculated_version   BIGINT UNSIGNED NULL
    calculated_at        DATETIME NULL
UQ  term_transcript_id + subject_id
~~~

Quy tắc lưu:

- Môn NORMAL sử dụng dtbmh.
- Môn SKILL sử dụng skill_score.
- Không dùng đồng thời dtbmh và skill_score cho cùng một loại môn.
- Kết quả chưa có dữ liệu giữ NULL và được hiển thị là Chưa có dữ liệu.
- Các phép tính được worker thực hiện, không thực hiện trong HTTP request.

### 12.5. student_subject_annual_result

~~~text
student_subject_annual_result
-----------------------------
PK  annual_subject_result_id  BIGINT UNSIGNED
FK  annual_transcript_id       BIGINT UNSIGNED NOT NULL
FK  subject_id                 BIGINT UNSIGNED NOT NULL
FK  hk1_term_result_id         BIGINT UNSIGNED NULL
FK  hk2_term_result_id         BIGINT UNSIGNED NULL
FK  retake_id                  BIGINT UNSIGNED NULL
    subject_type               NORMAL | SKILL
    regular_dtbmh_cn           DECIMAL(3,1) NULL
    official_dtbmh_cn          DECIMAL(3,1) NULL
    calculation_source         REGULAR | RETAKE
    calculated_version         BIGINT UNSIGNED NULL
    calculated_at              DATETIME NULL
    note                       VARCHAR(1000) NULL
UQ  annual_transcript_id + subject_id
~~~

Đối với môn không thi lại:

~~~text
regular_dtbmh_cn = official_dtbmh_cn
calculation_source = REGULAR
~~~

Đối với môn có thi lại:

~~~text
regular_dtbmh_cn = điểm trung bình cả năm trước thi lại
official_dtbmh_cn = Đtlmh
calculation_source = RETAKE
~~~

Điểm học kỳ và điểm thường trước thi lại không bị cập nhật đè.

---

## 13. Quy tắc tính điểm được phản ánh trong schema

### 13.1. Điểm trung bình môn học kỳ

Đtbmh của môn NORMAL được worker tính từ các student_score hợp lệ của các assessment_column thuộc scorebook. Cột chưa nhập không được xem là điểm 0. Chi tiết công thức và hệ số phải tuân thủ Requirement Baseline.

Nếu chưa có dữ liệu điểm hợp lệ:

~~~text
dtbmh = NULL
Hiển thị = "Chưa có dữ liệu"
~~~

### 13.2. Điểm môn kỹ năng

Điểm môn SKILL được worker tính theo skill_weight_config và các cột KTTT, KTĐK, KTCK.

Nếu thiếu dữ liệu thành phần, kết quả chưa hoàn chỉnh phải được ghi nhận trong báo cáo ngoại lệ. Worker không tự biến ô trống thành 0.

### 13.3. Điểm trung bình học kỳ

Đtbhk được tính từ các môn NORMAL đã có dtbmh:

~~~text
Đtbhk =
ROUND(
    SUM(dtbmh của các môn NORMAL có dữ liệu)
    / COUNT(các môn NORMAL có dữ liệu),
    1
)
~~~

Nếu không có môn NORMAL nào có dtbmh thì lưu NULL.

### 13.4. Điểm trung bình môn cả năm

regular_dtbmh_cn là kết quả cả năm của một môn NORMAL trước thi lại. Giá trị được lưu trong student_subject_annual_result và được tính từ các kết quả học kỳ theo Requirement Baseline.

### 13.5. Điểm trung bình cả năm

Trước thi lại:

~~~text
regular_dtbcn =
ROUND(
    SUM(regular_dtbmh_cn của các môn NORMAL)
    / COUNT(các môn NORMAL có dữ liệu),
    1
)
~~~

Sau thi lại:

~~~text
final_dtbcn =
ROUND(
    SUM(official_dtbmh_cn của các môn NORMAL)
    / COUNT(các môn NORMAL có dữ liệu),
    1
)
~~~

Môn SKILL không tham gia các công thức trên nếu Requirement Baseline quy định là không tính vào điểm trung bình môn thông thường.

---

## 14. Thi lại cuối năm

### 14.1. retake_exam

~~~text
retake_exam
-----------
PK  retake_id          BIGINT UNSIGNED
FK  student_id         BIGINT UNSIGNED NOT NULL
FK  academic_year_id   BIGINT UNSIGNED NOT NULL
FK  subject_id         BIGINT UNSIGNED NOT NULL
    pre_retake_score   DECIMAL(3,1) NOT NULL
    retake_score       DECIMAL(3,1) NULL
    exam_date          DATE NULL
    status              PLANNED | SCORED | CANCELLED
FK  entered_by         BIGINT UNSIGNED NULL
    entered_at         DATETIME NULL
FK  updated_by         BIGINT UNSIGNED NULL
    updated_at          DATETIME NULL
    note                VARCHAR(1000) NULL
UQ  student_id + academic_year_id + subject_id
~~~

### 14.2. Business rule

Với mỗi bộ:

~~~text
{student_id, academic_year_id, subject_id}
~~~

học sinh chỉ có một lần thi lại duy nhất.

Quy tắc:

1. Một học sinh có thể thi lại nhiều môn khác nhau.
2. Với mỗi môn trong một năm học, học sinh chỉ có một retake_exam hợp lệ.
3. retake_score dùng thang điểm 10 và làm tròn đến 0.1.
4. pre_retake_score là snapshot của regular_dtbmh_cn trước thi lại.
5. Không cập nhật đè các điểm thành phần, dtbmh hoặc regular_dtbmh_cn.
6. official_dtbmh_cn của đúng môn đó bằng retake_score khi retake được SCORED.
7. Worker tính lại final_dtbcn từ toàn bộ official_dtbmh_cn.
8. Nhập hoặc sửa retake_score phải tạo calculation task.
9. Sửa điểm thi lại phải tuân thủ quy trình sửa điểm và audit.

### 14.3. Hiển thị bảng tổng kết

Bảng tổng kết phải thể hiện được:

- regular_dtbmh_cn;
- pre_retake_score;
- retake_score;
- official_dtbmh_cn;
- calculation_source;
- regular_dtbcn;
- final_dtbcn;
- trạng thái IN_PROGRESS hoặc FINISH;
- ghi chú kết quả sau thi lại.

Khi calculation_status là IN_PROGRESS, kết quả cũ không được đánh dấu là kết quả chính thức mới nhất.

---

## 15. Background calculation

### 15.1. calculation_task

~~~text
calculation_task
----------------
PK  task_id                 BIGINT UNSIGNED
FK  student_id             BIGINT UNSIGNED NOT NULL
FK  academic_year_id       BIGINT UNSIGNED NOT NULL
    task_type               STUDENT_YEAR_RECALC
    requested_version       BIGINT UNSIGNED NOT NULL
    status                  PENDING | RUNNING | SUCCEEDED | FAILED
    attempt_count           INT UNSIGNED NOT NULL
    max_attempts            INT UNSIGNED NOT NULL
    available_at            DATETIME NOT NULL
    locked_at               DATETIME NULL
    worker_id               VARCHAR(100) NULL
    last_error              VARCHAR(2000) NULL
    idempotency_key         VARCHAR(255) NOT NULL
    created_at              DATETIME NOT NULL
    started_at              DATETIME NULL
    completed_at            DATETIME NULL
UQ  idempotency_key
~~~

Task có thể mở rộng thêm task_type cho các phạm vi khác, nhưng calculation_status của transcript vẫn chỉ gồm IN_PROGRESS và FINISH.

### 15.2. Transaction khi dữ liệu nguồn thay đổi

Mọi HTTP command làm thay đổi điểm phải thực hiện trong một transaction:

~~~text
1. Validate quyền và dữ liệu đầu vào
2. Lưu hoặc cập nhật dữ liệu điểm gốc
3. Tăng source_version
4. Đặt transcript bị ảnh hưởng thành IN_PROGRESS
5. Tạo hoặc gộp calculation_task
6. Ghi audit_log
7. Commit
8. Trả response
~~~

HTTP request không được chờ worker và không được tính Đtbmh, Đtbhk, ĐtbmhCN hoặc Đtbcn.

### 15.3. Thứ tự worker tính toán

Worker tính theo thứ tự phụ thuộc:

~~~text
student_score
    ↓
Đtbmh hoặc skill_score
    ↓
Đtbhk
    ↓
regular_dtbmh_cn
    ↓
Áp dụng Đtlmh cho môn tương ứng
    ↓
official_dtbmh_cn
    ↓
regular_dtbcn / final_dtbcn
~~~

Worker phải idempotent. Chạy lại cùng một task không được tạo bản ghi kết quả trùng hoặc làm sai kết quả.

### 15.4. Bảo vệ phiên bản

Worker chỉ được ghi FINISH nếu:

~~~text
calculated_version = source_version
~~~

Nếu có thay đổi điểm mới trong lúc worker đang chạy:

1. source_version tăng;
2. task cũ chỉ được ghi kết quả của phiên bản cũ;
3. transcript tiếp tục IN_PROGRESS;
4. worker tạo hoặc giữ lại task cho phiên bản mới;
5. kết quả chỉ được FINISH sau khi phiên bản mới nhất được tính xong.

### 15.5. Retry và lỗi

Khi task thất bại:

- calculation_task chuyển FAILED hoặc được đưa lại PENDING theo chính sách retry;
- attempt_count tăng;
- last_error được lưu;
- transcript vẫn IN_PROGRESS;
- không trả kết quả lỗi thành FINISH;
- giáo vụ có thể yêu cầu chạy lại task;
- task chạy lại phải an toàn và idempotent.

### 15.6. Các sự kiện tạo calculation task

Calculation task được tạo hoặc gộp khi:

- Nhập điểm mới;
- Sửa hoặc hủy điểm;
- Phê duyệt score_change_request;
- Thay đổi trạng thái điểm;
- Thay đổi trọng số môn kỹ năng;
- Thêm, xóa hoặc vô hiệu hóa cột điểm;
- Học sinh chuyển lớp;
- Khóa hoặc mở lại học kỳ;
- Nhập điểm thi lại;
- Sửa điểm thi lại.

---

