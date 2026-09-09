# Application Context v3

## 1. Mục tiêu

v3 mở rộng ứng dụng trường học từ quản lý dữ liệu và điểm sang hỗ trợ vận hành:

- xếp học sinh theo bộ tiêu chí có trọng số và có thể giải thích;
- lập thời khóa biểu cho lớp và giáo viên, có kiểm tra xung đột;
- thông báo theo cá nhân, lớp hoặc toàn trường;
- tìm kiếm/lọc nhất quán trên danh sách và bảng điểm;
- nhập dữ liệu vào đúng một cột điểm được chỉ định;
- ghi sổ đầu bài cho từng tiết học.

## 2. Ranh giới với v2

v2 vẫn là nguồn tương thích cho identity, học sinh, lớp, phân công, điểm, điểm danh
và bảng điểm. v3 bổ sung domain contract; không dùng frontend để suy diễn quyền,
không thay thế backend authorization và không coi JWT role là nguồn quyết định.

## 3. Kiến trúc mục tiêu

```text
Vue 3 / PrimeVue
        |
Typed v3 API services + reviewable Storybook states
        |
Spring Boot REST: placement | timetable | notification | query | score-import | lesson-log
        |
MySQL/JPA + audit/outbox where needed + background jobs only where approved
```

Mỗi capability được phát triển theo lát dọc: contract/schema, BE vertical slice,
FE vertical slice, rồi kiểm thử tích hợp và browser/release evidence. BE không phải
là phase bắt buộc hoàn thành toàn bộ trước khi FE bắt đầu.

## 4. Nguyên tắc bắt buộc

- Conflict thời khóa biểu là lỗi nghiệp vụ backend, không chỉ là cảnh báo UI.
- Xếp lớp phải lưu bộ tiêu chí, phiên bản, kết quả và lý do/vi phạm để có thể xem lại.
- Quy tắc nghiệp vụ `TBD-001` hiện được cung cấp là 19 tiết/tuần chuẩn; GVCN giảm 4 tiết;
  GV nữ nuôi con dưới 12 tháng giảm thêm 3 tiết; các mức giảm được cộng dồn. Policy phải
  được lưu theo tham số, nguồn, ngày hiệu lực và phiên bản; chưa được hard-code vào engine.
- Import điểm phải có `scorebookId`, `assessmentColumnId`, phiên bản/optimistic lock,
  preview, validation từng dòng và audit; không được tự đoán cột.
- Thông báo phải kiểm soát audience, quyền xem, trạng thái đọc và idempotency.
- Sổ đầu bài không thay thế điểm danh hay bảng điểm; nó ghi nhận đánh giá của tiết học.

## 5. Nguồn mở cần chốt

- Nguồn/văn bản hiện hành, ngày hiệu lực và phiên bản cho policy định mức tiết dạy/tuần;
  nội dung nghiệp vụ `TBD-001` đã được cung cấp nhưng metadata chính thức còn mở.
- Danh mục tiêu chí xếp lớp, trọng số, tie-breaker và cách xử lý sĩ số.
- Quyền tạo/sửa/duyệt thời khóa biểu, thông báo và sổ đầu bài theo role/assignment.
- Chính sách giữ dữ liệu, riêng tư và phạm vi người nhận thông báo.

## 6. Bản đồ tài liệu v3

| Domain | Tài liệu |
|---|---|
| Xếp lớp theo tiêu chí | [`modules/01-PlacementAndEnrollment.md`](modules/01-PlacementAndEnrollment.md) |
| Thời khóa biểu và tải giáo viên | [`modules/02-TimetableAndTeachingLoad.md`](modules/02-TimetableAndTeachingLoad.md) |
| Thông báo và audience | [`modules/03-NotificationAndAudience.md`](modules/03-NotificationAndAudience.md) |
| Search/filter và import điểm | [`modules/04-QueryAndScoreImport.md`](modules/04-QueryAndScoreImport.md) |
| Sổ đầu bài | [`modules/05-LessonLog.md`](modules/05-LessonLog.md) |
| Data ownership/constraints | [`data-model/README.md`](data-model/README.md) |
| FE transport boundary | [`frontend-api/README.md`](frontend-api/README.md) |
