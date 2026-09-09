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

Giá trị định mức, ngoại lệ và cách tính tiết quy đổi là `TBD-001`; không hard-code trước
khi có văn bản hiện hành.

## Lifecycle

```text
DRAFT -> VALIDATED -> PUBLISHED -> ARCHIVED
```

Draft có thể lưu lỗi để tiếp tục sửa; `PUBLISHED` không được chứa conflict. Sửa lịch đã
publish phải tạo revision/audit và chạy validation lại.

## Contract boundary

Plan 075 phải chốt slot identity, conflict codes, policy evaluation result, permission
matrix và publish semantics. Endpoint/schema cụ thể: `TBD` cho đến contract checkpoint.
