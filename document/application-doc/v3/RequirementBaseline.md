# Requirement Baseline v3

## Trạng thái

- Version: `0.1-approved`
- Phạm vi: trường THCS, kế thừa v2
- Nguồn thay đổi: `CR-V3-001`
- Các mục phụ thuộc nguồn pháp lý/chính sách chưa cung cấp: `TBD`; `TBD-001` đã có nội dung
  nghiệp vụ nhưng còn thiếu source, effective date và policy version.

## Module và requirement

Chi tiết theo module:

- [`01-PlacementAndEnrollment.md`](modules/01-PlacementAndEnrollment.md)
- [`02-TimetableAndTeachingLoad.md`](modules/02-TimetableAndTeachingLoad.md)
- [`03-NotificationAndAudience.md`](modules/03-NotificationAndAudience.md)
- [`04-QueryAndScoreImport.md`](modules/04-QueryAndScoreImport.md)
- [`05-LessonLog.md`](modules/05-LessonLog.md)

### Placement — Xếp lớp theo tiêu chí

- `FR-V3-PLACE-001`: Tạo phiên xếp lớp cho năm học/khối với capacity và tập học sinh.
- `FR-V3-PLACE-002`: Chọn nhiều tiêu chí như điểm, giới tính, nguyện vọng hoặc tiêu chí
  được cấu hình; mỗi tiêu chí có hướng sắp xếp, trọng số/ưu tiên và nguồn dữ liệu.
- `FR-V3-PLACE-003`: Chạy mô phỏng, xem preview và xác nhận kết quả sau khi kiểm tra
  sĩ số, dữ liệu thiếu và ràng buộc.
- `FR-V3-PLACE-004`: Lưu phiên bản rule, kết quả từng học sinh và lý do phân bổ.
- `BR-V3-PLACE-001`: Không ghi đè enrollment lịch sử; xác nhận tạo assignment mới.
- `BR-V3-PLACE-002`: Kết quả không đủ dữ liệu phải được gắn cờ, không âm thầm suy đoán.

### Timetable — Thời khóa biểu lớp và giáo viên

- `FR-V3-TT-001`: Tạo, sửa, preview và publish thời khóa biểu theo học kỳ.
- `FR-V3-TT-002`: Xem lịch theo lớp, giáo viên, phòng và tiết.
- `FR-V3-TT-003`: Kiểm tra conflict cùng lớp, cùng giáo viên, phòng, tiết nghỉ và
  assignment không hợp lệ trước khi lưu/publish.
- `FR-V3-TT-004`: Kiểm tra tổng số tiết/tuần của giáo viên theo policy version.
- `BR-V3-TT-001`: Conflict là lỗi chặn publish; draft có thể lưu để tiếp tục xử lý.
- `BR-V3-TT-002`: Policy định mức phải có nguồn, effective date và phiên bản. Nội dung
  hiện hành của `TBD-001` là 19 tiết/tuần chuẩn, giảm 4 tiết cho GVCN và giảm thêm 3
  tiết cho GV nữ nuôi con dưới 12 tháng; các mức giảm cộng dồn và phải biểu diễn bằng
  tham số, không hard-code vào timetable engine.

### Notifications — Thông báo theo audience

- `FR-V3-NOTI-001`: Tạo thông báo riêng cho một hoặc nhiều cá nhân hợp lệ.
- `FR-V3-NOTI-002`: Tạo thông báo theo lớp tại thời điểm phát hành và xử lý membership.
- `FR-V3-NOTI-003`: Tạo thông báo chung trong phạm vi trường.
- `FR-V3-NOTI-004`: Người nhận xem, đánh dấu đã đọc và chỉ thấy nội dung trong scope.
- `BR-V3-NOTI-001`: Audience, người tạo, thời gian phát hành và trạng thái gửi phải audit.

### Search/filter — Danh sách và bảng điểm

- `FR-V3-QUERY-001`: Các màn hình danh sách hỗ trợ search, filter, sort và pagination.
- `FR-V3-QUERY-002`: Bảng điểm hỗ trợ lọc theo học sinh, trạng thái, cột điểm và giá trị.
- `BR-V3-QUERY-001`: Filter phải được áp dụng ở backend khi dữ liệu phân trang; UI không
  được lọc giả trên một page đã tải.

### Score import — Import một cột điểm

- `FR-V3-IMPORT-001`: Chọn chính xác một `assessmentColumnId` trước khi import.
- `FR-V3-IMPORT-002`: Upload file `.xlsx`, parse, preview và báo lỗi từng dòng trước khi
  commit.
- `FR-V3-IMPORT-003`: Mỗi lần import chỉ xử lý một `assessmentColumnId`; commit chỉ thay
  đổi cột được chọn, bao gồm trường hợp update ô đã có điểm, có expected version và audit.
- `BR-V3-IMPORT-001`: File thiếu/mismatch student identity, điểm ngoài range hoặc duplicate
  đều không được commit âm thầm. Quy ước giá trị `0–10` là điểm; `11` là `ABSENT` và
  `12` là `EXEMPTED`; các mã khác phải được Plan 078 chốt hoặc từ chối rõ ràng.

### Lesson log — Sổ đầu bài

- `FR-V3-LESSON-001`: Giáo viên ghi nhận nội dung, mức độ hoàn thành, nhận xét, sĩ số/
  tình hình tiết học cho lớp và tiết cụ thể.
- `FR-V3-LESSON-002`: Người có quyền xem lại lịch sử sổ đầu bài theo lớp, giáo viên, ngày.
- `FR-V3-LESSON-003`: Cho phép chỉnh sửa theo lifecycle/policy và lưu audit.
- `BR-V3-LESSON-001`: Bản ghi phải liên kết tới timetable slot/class/teacher; không tạo
  sổ đầu bài mồ côi.

## Non-functional và acceptance chung

- `NFR-V3-001`: Backend là nguồn kiểm tra cuối cùng cho quyền, conflict và tính toàn vẹn.
- `NFR-V3-002`: Các mutation quan trọng có audit, idempotency/optimistic locking phù hợp.
- `AC-V3-001`: Mỗi vertical slice có contract test BE, FE unit/component test, Storybook
  cho state tương tác và browser/live evidence khi scope yêu cầu.
- `AC-V3-002`: Mọi gate chưa chạy phải ghi `NOT RUN` hoặc `BLOCKED`, không ghi `PASS`.
