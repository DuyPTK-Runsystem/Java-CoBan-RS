# Seeded Data Catalog — Plan 048

- **Kế hoạch liên quan**: [`document/dev-impl-plan/be/bootstrap/048-be-demo-data-bootstrap-2026-08-26.md`](../../../dev-impl-plan/be/bootstrap/048-be-demo-data-bootstrap-2026-08-26.md)
- **Dev Note liên quan**: [`048-be-demo-data-bootstrap-2026-08-26.md`](048-be-demo-data-bootstrap-2026-08-26.md)
- **Application-document version**: `v2`
- **Ngày ghi nhận**: 2026-08-26
- **Mục đích**: ghi nhận chính xác synthetic data do Plan 048 tạo trên database mới.

## 1. Cơ chế seed

### 1.1. Mandatory admin

Flyway migration `V18__seed_default_admin.sql` luôn chạy sau các migration tạo role và tạo:

| Username | Password logic local | Role | Ghi chú |
| --- | --- | --- | --- |
| `admin` | `admin` | `ADMIN` | Luôn có sau migration sạch, độc lập với demo flag. |

Password được lưu dưới dạng BCrypt. Migration không ghi đè account `admin` đã tồn tại.

### 1.2. Optional demo fixture

Application property:

```properties
app.seed.demo.enabled=${APP_SEED_DEMO_ENABLED:false}
```

Khi giá trị là `true`, `DemoDataSeeder` chạy sau khi application context khởi tạo. Khi là `false`, demo fixture không chạy và dữ liệu đã tồn tại không bị xóa. Các bước seed kiểm tra natural key trước khi tạo nên có thể chạy lại an toàn trong fixture chuẩn.

## 2. Account và role

Tổng cộng có **54 `app_user`** sau khi bật demo fixture:

| Nhóm | Số lượng | Username | Password logic local | Role |
| --- | ---: | --- | --- | --- |
| Administrator | 1 | `admin` | `admin` | `ADMIN` |
| Academic office | 1 | `academic.office` | `12345678` | `ACADEMIC_OFFICE` |
| Teacher | 20 | `teacher01`–`teacher20` | `12345678` | `TEACHER` |
| Student | 32 | `student.<class-code-lowercase>.<01-04>` | `12345678` | `STUDENT` |

### 2.1. Teacher accounts/profile

- Teacher code tương ứng: `teacher01` → `GV001`, …, `teacher20` → `GV020`.
- Mỗi teacher profile có `user_id` trỏ tới đúng account teacher tương ứng.
- Tất cả teacher có trạng thái `ACTIVE`, department `Tổ bộ môn`, email `teacherNN@example.test`.
- Dữ liệu ngày sinh, giới tính và số điện thoại là synthetic deterministic.

### 2.2. Student accounts/profile

- Mỗi student có `user_id`, role `STUDENT`, `student_info` và trạng thái `ACTIVE`.
- Username theo mẫu `student.6a1.01` đến `student.9a2.04`.
- Student code chạy liên tục theo class order:
  - `6A1`: `STU2600001`–`STU2600004`;
  - `6A2`: `STU2600005`–`STU2600008`;
  - `7A1`: `STU2600009`–`STU2600012`;
  - `7A2`: `STU2600013`–`STU2600016`;
  - `8A1`: `STU2600017`–`STU2600020`;
  - `8A2`: `STU2600021`–`STU2600024`;
  - `9A1`: `STU2600025`–`STU2600028`;
  - `9A2`: `STU2600029`–`STU2600032`.
- Tên mẫu: `Học sinh <class-code> <01-04>`; địa chỉ mẫu: `Khu phố <class-code>`.

## 3. Academic structure

### 3.1. Academic year và semesters

| Loại | Code | Tên | Bắt đầu | Kết thúc | Status |
| --- | --- | --- | --- | --- | --- |
| Academic year | `2026-2027` | `2026-2027` | `2026-09-01` | `2027-05-31` | `ACTIVE` |
| Semester | `HK1` | `HK1 2026 - 2027` | `2026-09-01` | `2026-12-31` | `ACTIVE` |
| Semester | `HK2` | `HK2 2026 - 2027` | `2027-01-01` | `2027-05-31` | `DRAFT` |

### 3.2. Grade và class

- Grade levels: `KHOI_6`, `KHOI_7`, `KHOI_8`, `KHOI_9`; level lần lượt 6, 7, 8, 9.
- Next-grade chain: khối 6 → 7 → 8 → 9.
- Classes, tất cả `ACTIVE`, capacity `40`:

| Khối | Classes | Students/class |
| ---: | --- | ---: |
| 6 | `6A1`, `6A2` | 4 |
| 7 | `7A1`, `7A2` | 4 |
| 8 | `8A1`, `8A2` | 4 |
| 9 | `9A1`, `9A2` | 4 |

## 4. Subject catalog

### 4.1. Subjects

| Code | Tên | Type |
| --- | --- | --- |
| `TOAN` | Toán | `ACADEMIC` |
| `VAT_LY` | Vật lí | `ACADEMIC` |
| `HOA_HOC` | Hóa học | `ACADEMIC` |
| `SINH_HOC` | Sinh học | `ACADEMIC` |
| `NGU_VAN` | Ngữ Văn | `ACADEMIC` |
| `NGOAI_NGU` | Ngoại ngữ | `ACADEMIC` |
| `LICH_SU` | Lịch sử | `ACADEMIC` |
| `DIA_LY` | Địa lí | `ACADEMIC` |
| `GDCD` | Giáo dục công dân | `ACADEMIC` |
| `TIN_HOC` | Tin học | `ACADEMIC` |
| `CONG_NGHE` | Công nghệ | `ACADEMIC` |
| `NGHE_DIEN` | Nghề phổ thông - Điện dân dụng | `SKILL` |
| `NGHE_NONG_NGHIEP` | Nghề phổ thông - Nông nghiệp | `SKILL` |

### 4.2. Applicability rules

- 11 môn `ACADEMIC` áp dụng cho các khối 6–9 ở cả HK1 và HK2.
- Ngoại lệ: `CONG_NGHE` không áp dụng cho khối 9 ở HK2; khối 9 chỉ học Công nghệ ở HK1.
- Hai môn `SKILL` chỉ áp dụng cho khối 8 và 9 ở HK2.
- Tổng số `subject_applicability`: **91**.
- Tổng số `class_subject`: **182**.
- Môn phổ thông giữ hệ số KTTX/KTTT 1, KĐK 2, KTCK 3 theo baseline; môn nghề có type `SKILL` để cấu hình trọng số ở bước Scorebook sau.

## 5. Enrollment và assignment

### 5.1. Enrollment

- Tổng số `student_year_enrollment`: **32**.
- Mỗi học sinh có đúng một enrollment trong academic year `2026-2027`.
- Mỗi lớp có đúng 4 học sinh.
- Status: `ACTIVE`.
- `enrolled_at`: `2026-09-01 08:00`.

### 5.2. Homeroom assignment

Academic office là người tạo assignment. Mỗi lớp có một GVCN khác nhau:

| Class | Homeroom teacher | Teacher code |
| --- | --- | --- |
| `6A1` | `teacher01` | `GV001` |
| `6A2` | `teacher02` | `GV002` |
| `7A1` | `teacher03` | `GV003` |
| `7A2` | `teacher04` | `GV004` |
| `8A1` | `teacher05` | `GV005` |
| `8A2` | `teacher06` | `GV006` |
| `9A1` | `teacher07` | `GV007` |
| `9A2` | `teacher08` | `GV008` |

- Tổng số `homeroom_assignment`: **8**.
- Tất cả assignment có status `ACTIVE`, bắt đầu từ `2026-09-01`.
- GVCN vẫn là giáo viên dạy môn; không có mô hình tách riêng GVCN khỏi GVBM.

### 5.3. Subject teaching assignment

- Tổng số `subject_teaching_assignment`: **182**, đúng một assignment cho mỗi `class_subject`.
- Phân công teacher dùng round-robin deterministic trên danh sách `teacher01`–`teacher20`.
- Vì vậy một giáo viên có thể dạy nhiều môn/lớp; dữ liệu bao gồm khả năng một giáo viên dạy cả hai môn nghề hoặc cả Lịch sử và Địa lí.
- Academic office là `assigned_by` cho các assignment.

## 6. Không nằm trong catalog này

Plan 048 chưa tạo các bản ghi:

- `scorebook` và `assessment_column`;
- `skill_weight_config` 25/35/40;
- `student_score`, calculation task, transcript result;
- retake, attendance session/record và notification.

Các dữ liệu này sẽ được bổ sung khi bắt đầu Scorebook E2E.

## 7. Bằng chứng kiểm tra

- Integration test: `DemoDataSeederIntegrationTest` kiểm tra counts, password BCrypt cho toàn bộ account, role, user link, 4 học sinh/lớp và idempotency khi chạy seeder lần hai.
- Migration test: `FlywayMigrationTest` kiểm tra migration sạch tạo đủ role và `admin` có password BCrypt cùng role `ADMIN`.
- Test backend, Checkstyle, PMD và build của Plan 048 đều đã `PASS`.
- MySQL container thực tế chưa được chạy trong phiên ghi nhận này; H2/Flyway clean migration là bằng chứng tự động hiện có.
