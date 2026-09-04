# Changes and Attendance

## 10. Request sửa điểm

### 10.1. score_change_request

~~~text
score_change_request
--------------------
PK  request_id              BIGINT UNSIGNED
FK  assessment_column_id    BIGINT UNSIGNED NOT NULL
FK  student_id              BIGINT UNSIGNED NOT NULL
FK  student_score_id        BIGINT UNSIGNED NULL
    before_status           VARCHAR(20) NOT NULL
    before_value            DECIMAL(3,1) NULL
    proposed_status         VARCHAR(20) NOT NULL
    proposed_value          DECIMAL(3,1) NULL
    reason                  VARCHAR(1000) NOT NULL
FK  requested_by            BIGINT UNSIGNED NOT NULL
    requested_at            DATETIME NOT NULL
    status                  PENDING | APPROVED | REJECTED | CANCELLED | APPLIED
FK  reviewed_by             BIGINT UNSIGNED NULL
    reviewed_at             DATETIME NULL
    rejection_reason        VARCHAR(1000) NULL
    applied_at              DATETIME NULL
~~~

Ràng buộc:

- Một học sinh và một cột điểm chỉ có tối đa một request PENDING.
- Snapshot before_* phải được giữ nguyên để phát hiện xung đột.
- Không được tự ý cập nhật before_value sau khi request được tạo.
- Dữ liệu proposed_* phải tuân thủ cùng range và status rule của student_score.

### 10.2. Luồng duyệt

Khi duyệt request:

1. Kiểm tra student_score hiện tại vẫn khớp snapshot before_*;
2. Nếu chưa có row thì tạo row mới, nếu đã có thì cập nhật row;
3. Đánh dấu request là APPLIED;
4. Tăng source_version của phạm vi tổng kết;
5. Đặt bảng tổng kết thành IN_PROGRESS;
6. Tạo hoặc gộp calculation_task;
7. Ghi audit_log.

Việc cập nhật điểm và tạo task phải nằm trong cùng transaction.

---

## 11. Điểm danh

### 11.1. attendance_session

~~~text
attendance_session
------------------
PK  session_id       BIGINT UNSIGNED
FK  class_id         BIGINT UNSIGNED NOT NULL
FK  semester_id      BIGINT UNSIGNED NOT NULL
    attendance_date  DATE NOT NULL
    session_no       INT UNSIGNED NOT NULL
FK  created_by       BIGINT UNSIGNED NOT NULL
    created_at       DATETIME NOT NULL
UQ  class_id + attendance_date + session_no
~~~

### 11.2. attendance_record

~~~text
attendance_record
-----------------
PK  attendance_record_id  BIGINT UNSIGNED
FK  session_id             BIGINT UNSIGNED NOT NULL
FK  student_id             BIGINT UNSIGNED NOT NULL
    status                 PRESENT | ABSENT | LATE | EXCUSED
    note                   VARCHAR(500) NULL
FK  recorded_by            BIGINT UNSIGNED NOT NULL
    recorded_at            DATETIME NOT NULL
FK  updated_by             BIGINT UNSIGNED NULL
    updated_at             DATETIME NULL
UQ  session_id + student_id
~~~

Điều chỉnh điểm danh phải ghi audit. Không xóa record đã dùng trong báo cáo; nếu cần hủy, sử dụng trạng thái hoặc audit correction.

---

