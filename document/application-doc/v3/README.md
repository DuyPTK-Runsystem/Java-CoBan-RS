# Application Documentation v3

v3 là baseline mở rộng kế tiếp v2 cho các năng lực lập kế hoạch học vụ và vận hành
hằng ngày. v3 không sửa ngầm contract v2; mọi thay đổi có ảnh hưởng đến v2 phải đi
qua CR và migration/compatibility plan riêng.

## Source order

1. [`ApplicationContext.md`](ApplicationContext.md)
2. [`RequirementBaseline.md`](RequirementBaseline.md)
3. CR đã được phê duyệt trong [`change-request/`](change-request/)
4. module và data-model v3 liên quan
5. Developer Plan được phê duyệt

## Module map

- [`modules/01-PlacementAndEnrollment.md`](modules/01-PlacementAndEnrollment.md)
- [`modules/02-TimetableAndTeachingLoad.md`](modules/02-TimetableAndTeachingLoad.md)
- [`modules/03-NotificationAndAudience.md`](modules/03-NotificationAndAudience.md)
- [`modules/04-QueryAndScoreImport.md`](modules/04-QueryAndScoreImport.md)
- [`modules/05-LessonLog.md`](modules/05-LessonLog.md)
- [`data-model/README.md`](data-model/README.md)
- [`frontend-api/README.md`](frontend-api/README.md)
- [`specification.html`](specification.html) — tài liệu đặc tả HTML tổng hợp, dùng để review

## Trạng thái

- Version: `v3-draft`
- Ngày lập: `2026-09-09`
- CR nền: [`CR-V3-001-academic-operations-and-targeted-communication.md`](change-request/CR-V3-001-academic-operations-and-targeted-communication.md)
- Approval: `DRAFT - chờ người dùng phê duyệt qua agent message`

Các giá trị pháp lý/chính sách chưa được cung cấp được đánh dấu `TBD`. Nội dung nghiệp vụ
cho `TBD-001` đã được cung cấp: 19 tiết/tuần chuẩn, GVCN giảm 4 tiết và GV nữ nuôi con
dưới 12 tháng giảm thêm 3 tiết, áp dụng cộng dồn. Nguồn chính thức, ngày hiệu lực và
phiên bản policy vẫn cần được bổ sung; không hard-code policy vào engine.

`TBD-005` đã được chốt một phần: import dùng `.xlsx`, có preview bắt buộc, mỗi lần upload
chỉ xử lý một `assessmentColumnId`, và cho phép review rồi cập nhật các ô đã có điểm.
Chi tiết template, row mapping, giới hạn file và các quy tắc còn mở được chốt trong Plan 078.
