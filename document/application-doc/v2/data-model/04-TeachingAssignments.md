# Teaching Assignments

## 7. Phân công giáo viên

### 7.1. homeroom_assignment

~~~text
homeroom_assignment
-------------------
PK  assignment_id   BIGINT UNSIGNED
FK  class_id        BIGINT UNSIGNED NOT NULL
FK  teacher_id      BIGINT UNSIGNED NOT NULL
    valid_from      DATE NOT NULL
    valid_to        DATE NULL
    status          ACTIVE | ENDED
FK  assigned_by     BIGINT UNSIGNED NULL
    created_at      DATETIME NOT NULL
    updated_at      DATETIME NOT NULL
~~~

Quy tắc:

- Một lớp chỉ có tối đa một GVCN ACTIVE tại một thời điểm.
- Khi đổi GVCN, bản ghi cũ chuyển ENDED trước khi bản ghi mới ACTIVE.
- Một giáo viên có thể là GVCN của một lớp và đồng thời là GVBM của chính lớp đó.

### 7.2. subject_teaching_assignment

~~~text
subject_teaching_assignment
---------------------------
PK  assignment_id      BIGINT UNSIGNED
FK  class_subject_id   BIGINT UNSIGNED NOT NULL
FK  teacher_id         BIGINT UNSIGNED NOT NULL
    valid_from         DATE NOT NULL
    valid_to           DATE NULL
    status             ACTIVE | ENDED
FK  assigned_by        BIGINT UNSIGNED NULL
    created_at          DATETIME NOT NULL
    updated_at          DATETIME NOT NULL
~~~

Quy tắc:

- Một class_subject chỉ có tối đa một GVBM ACTIVE tại một thời điểm.
- Giáo viên chỉ được nhập điểm khi có assignment ACTIVE tương ứng.
- GVCN không tự động có quyền nhập tất cả môn của lớp.
- Một giáo viên có thể có nhiều assignment ACTIVE cho các lớp hoặc môn khác nhau nếu không vi phạm quy tắc nghiệp vụ.

Kiểm tra không chồng lấn khoảng hiệu lực phải được thực hiện trong transaction ở service layer; các index hỗ trợ truy vấn không thay thế business rule.

---

