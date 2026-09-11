# Module 02 — Timetable and Teacher Load

## Mục tiêu

Lập lịch cho lớp và giáo viên theo tuần/học kỳ, kiểm tra conflict trước khi publish và
đối chiếu số tiết/tuần theo policy có nguồn.

## Constraint bắt buộc

- Một lớp không có hai môn/tiết trùng cùng slot.
- Một giáo viên không dạy hai lớp trùng slot.
- Phòng không bị dùng trùng slot nếu resource phòng được áp dụng.
- Slot phải thuộc calendar/semester hợp lệ và assignment phải còn hiệu lực.
- Tổng tiết/tuần đối chiếu `TeacherLoadPolicy(version, source, effectiveFrom)`.

Quy tắc nghiệp vụ `TBD-001` đã được cung cấp:

- Định mức chuẩn: `19` tiết/tuần.
- GVCN: giảm `4` tiết/tuần.
- GV nữ nuôi con dưới `12` tháng tuổi: giảm thêm `3` tiết/tuần.
- Các mức giảm được cộng dồn; trường hợp vừa là GVCN vừa thuộc diện nuôi con nhỏ còn
  `12` tiết/tuần.

Các giá trị phải được biểu diễn qua tham số của `TeacherLoadPolicy`, không hard-code vào
engine. `source`, `effectiveFrom` và `version` của policy vẫn cần được bổ sung trước khi
coi policy là quy định active.

## Lifecycle

```text
DRAFT -> VALIDATED -> PUBLISHED -> ARCHIVED
```

Draft có thể lưu lỗi để tiếp tục sửa; `PUBLISHED` không được chứa conflict. Sửa lịch đã
publish phải tạo revision/audit và chạy validation lại.

## Contract boundary

Plan 075 phải chốt slot identity, conflict codes, policy evaluation result, permission
matrix và publish semantics. Endpoint/schema cụ thể: `TBD` cho đến contract checkpoint.
