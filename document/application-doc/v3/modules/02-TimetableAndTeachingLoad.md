# Module 02 — Timetable and Teacher Load

## Mục tiêu

Lập lịch cho lớp và giáo viên theo tuần/học kỳ, kiểm tra conflict trước khi publish và
đối chiếu số tiết/tuần theo policy có nguồn.

## Constraint bắt buộc

- Một lớp không có hai môn/tiết trùng cùng slot.
- Một giáo viên không dạy hai lớp trùng slot.
- Chỉ gắn phòng chức năng; cùng phòng chức năng không được dùng trùng slot. Không gán phòng học thông thường.
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

## Bổ sung được người dùng xác nhận — 2026-09-11, Plan 075

- TKB theo học kỳ, màn hình xem theo tuần; hiện có **2 buổi/ngày, 4 tiết/buổi**. Slot phân biệt buổi và tiết trong buổi; giờ chuông/ngày học được chốt ở calendar.
- TEACHER được đăng ký lịch bận của mình. Lịch bận **cần ADMIN/ACADEMIC_OFFICE duyệt trước**; chỉ lịch bận đã duyệt và còn hiệu lực mới chặn xếp tiết trùng.
- ADMIN và ACADEMIC_OFFICE ngang quyền trong các chức năng Plan 075, kể cả cấu hình, duyệt lịch bận, công bố, phòng chức năng và xác nhận miễn giảm.
- Bổ sung module quản lý phòng chức năng; quản lý môn học hiện hữu bổ sung gán phòng chức năng. Không mở rộng thành quản lý phòng học thông thường.
- Cardinality/điều kiện bắt buộc gán phòng, sửa/rút lịch bận sau duyệt và đăng ký trùng lịch đã công bố còn là quyết định mở trong [Plan 075](../../../dev-impl-plan/be/timetable/075-timetable-teacher-load-2026-09-11.md). Việc bổ sung requirement không phê duyệt toàn bộ API/schema đề xuất.

## Lifecycle

```text
DRAFT -> VALIDATED -> PUBLISHED -> ARCHIVED
```

Draft có thể lưu lỗi để tiếp tục sửa; `PUBLISHED` không được chứa conflict. Sửa lịch đã
publish phải tạo revision/audit và chạy validation lại.

## Contract boundary

Plan 075 phải chốt slot identity, conflict codes, policy evaluation result, permission
matrix và publish semantics. Endpoint/schema cụ thể: `TBD` cho đến contract checkpoint.
