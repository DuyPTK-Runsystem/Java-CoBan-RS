# Plan 081 — Fixtures chưa seed

### B.2 Timetable entries (không seed)

Timetable không nằm trong bộ seed này; người dùng tự xếp sau khi các quan hệ
lớp–môn–giáo viên ở A.6 đã tồn tại. Các dòng scenario timetable dưới đây chỉ
là dữ liệu minh họa cho CR, không phải rows sẽ tạo bởi seeder.

| Entry key    | Class | Subject     | Teacher | Day | Session     | Period | Room          | Scenario                   |
| ------------ | ----- | ----------- | ------- | --- | ----------- | -----: | ------------- | -------------------------- |
| `TTE-081-01` | `6A1` | `TOAN`      | `GV001` | MON | `MORNING`   |      1 | null          | valid                      |
| `TTE-081-02` | `6A1` | `NGU_VAN`   | `GV002` | MON | `MORNING`   |      2 | null          | valid                      |
| `TTE-081-03` | `6A1` | `VAT_LY`    | `GV003` | TUE | `MORNING`   |      1 | `LAB-PHY-01`  | valid                      |
| `TTE-081-04` | `6A1` | `HOA_HOC`   | `GV004` | TUE | `MORNING`   |      2 | `LAB-CHEM-01` | valid                      |
| `TTE-081-05` | `6A2` | `TOAN`      | `GV005` | MON | `MORNING`   |      1 | null          | teacher conflict candidate |
| `TTE-081-06` | `7A1` | `TIN_HOC`   | `GV006` | WED | `AFTERNOON` |      1 | `LAB-IT-01`   | valid                      |
| `TTE-081-07` | `7A2` | `SINH_HOC`  | `GV007` | WED | `AFTERNOON` |      1 | `LAB-BIO-01`  | room conflict candidate    |
| `TTE-081-08` | `8A1` | `TOAN`      | `GV008` | THU | `MORNING`   |      1 | null          | class conflict candidate   |
| `TTE-081-09` | `8A1` | `NGOAI_NGU` | `GV009` | THU | `MORNING`   |      1 | null          | class conflict candidate   |
| `TTE-081-10` | `9A1` | `CONG_NGHE` | `GV010` | FRI | `AFTERNOON` |      2 | null          | valid                      |

### B.3 Teacher load and availability

| Key       | Teacher | Is homeroom | Child-care reduction | Standard | Expected load | Availability       |
| --------- | ------- | ----------: | -------------------: | -------: | ------------: | ------------------ |
| `LOAD-01` | `GV001` |         yes |                   no |       19 |            15 | none               |
| `LOAD-02` | `GV002` |         yes |                  yes |       19 |            12 | approved busy slot |
| `LOAD-03` | `GV009` |          no |                  yes |       19 |            16 | pending busy slot  |
| `LOAD-04` | `GV010` |          no |                   no |       19 |            19 | rejected busy slot |

### B.5 Score import rows

| Import key | Student         | Old value | New value | Row state | Error              |
| ---------- | --------------- | --------: | --------: | --------- | ------------------ |
| `ROW-01`   | `STU2600001`    |      null |         0 | valid     | null               |
| `ROW-02`   | `STU2600002`    |       6.5 |       7.0 | update    | null               |
| `ROW-03`   | `STU2600003`    |      null |      9.25 | valid     | null               |
| `ROW-04`   | `STU2600004`    |      null |        11 | valid     | null               |
| `ROW-05`   | `STU2600005`    |      null |        12 | valid     | null               |
| `ROW-06`   | `STU2600006`    |      null |      null | blank     | null               |
| `ROW-07`   | `STU-NOT-FOUND` |      null |       8.0 | invalid   | student not found  |
| `ROW-08`   | `STU2600007`    |      null |      10.1 | invalid   | score out of range |
| `ROW-09`   | `STU2600008`    |      null |        -1 | invalid   | score out of range |
| `ROW-10`   | `STU2600009`    |      null |     `abc` | invalid   | invalid number     |

### B.6 Lesson log rows

| Entry key              | Class | Subject   | Teacher | Date       | State       | Progress      | Completion  |
| ---------------------- | ----- | --------- | ------- | ---------- | ----------- | ------------- | ----------- |
| `LESSON-081-DRAFT`     | `6A1` | `TOAN`    | `GV001` | 2026-09-14 | `DRAFT`     | `ON_SCHEDULE` | `PARTIAL`   |
| `LESSON-081-SUBMITTED` | `6A1` | `NGU_VAN` | `GV002` | 2026-09-14 | `SUBMITTED` | `ON_SCHEDULE` | `COMPLETED` |
| `LESSON-081-REVIEWED`  | `6A1` | `VAT_LY`  | `GV003` | 2026-09-15 | `REVIEWED`  | `AHEAD`       | `COMPLETED` |
| `LESSON-081-AMENDED`   | `6A1` | `HOA_HOC` | `GV004` | 2026-09-15 | `AMENDED`   | `BEHIND`      | `PARTIAL`   |
| `LESSON-081-EXPIRED`   | `6A1` | `TOAN`    | `GV001` | 2026-09-16 | `DRAFT`     | `ON_SCHEDULE` | `PARTIAL`   |
| `LESSON-081-CONFLICT`  | `6A1` | `TIN_HOC` | `GV006` | 2026-09-16 | `DRAFT`     | `ON_SCHEDULE` | `COMPLETED` |

| Entry                  | Content                        | Class situation          | Homework           | Audit reason                 |
| ---------------------- | ------------------------------ | ------------------------ | ------------------ | ---------------------------- |
| `LESSON-081-DRAFT`     | Ôn tập phân số                 | Ổn định                  | Bài tập số 1–3     | null                         |
| `LESSON-081-SUBMITTED` | Đọc hiểu văn bản               | Học sinh tham gia đầy đủ | Đọc trước bài mới  | null                         |
| `LESSON-081-REVIEWED`  | Thí nghiệm lực                 | Tốt                      | Phiếu quan sát     | null                         |
| `LESSON-081-AMENDED`   | Điều chỉnh nội dung thí nghiệm | Có 1 nhóm cần hỗ trợ     | Hoàn thiện báo cáo | Điều chỉnh lỗi nhập nội dung |
| `LESSON-081-EXPIRED`   | Ôn tập chương 1                | Có học sinh vắng         | Không              | null                         |
| `LESSON-081-CONFLICT`  | Thực hành bảng tính            | Chưa đủ thiết bị         | Bài thực hành 1    | null                         |

