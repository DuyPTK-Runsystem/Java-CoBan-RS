# Plan 081 — Tách dữ liệu seed

Các bảng dữ liệu chi tiết của Plan 081 được tách theo nhóm để dễ đọc và cập nhật.
Plan chính vẫn giữ nguyên đặc tả workflow, acceptance criteria và ranh giới triển khai;
các file dưới đây là nơi tra cứu row data.

| File | Nội dung | Trạng thái |
| ---- | -------- | ---------- |
| [01-baseline-identity.md](01-baseline-identity.md) | Tài khoản, giáo viên, lớp và 160 học sinh canonical | Seed |
| [02-academic-assignments.md](02-academic-assignments.md) | GVCN, môn giáo viên và phân công lớp–môn–giáo viên | Seed |
| [03-placement.md](03-placement.md) | Placement candidates | Seed |
| [04-functional-rooms.md](04-functional-rooms.md) | Phòng chức năng | Seed |
| [05-notifications.md](05-notifications.md) | Notification records, receipts và delivery | Seed |
| [06-scorebook.md](06-scorebook.md) | Scorebook và score-cell scenarios | Seed |
| [07-unseeded-fixtures.md](07-unseeded-fixtures.md) | G7 timetable slice, teacher load, score import rows và lesson log | G7 đã seed; phần còn lại chưa seed |

Quy ước chung vẫn theo Plan 081: seed opt-in, natural key/idempotent, không xoá dữ
liệu đã tồn tại và không dùng dữ liệu production. Các file có trạng thái “Chưa seed”
chỉ là fixture đặc tả, không phải dữ liệu mà seeder hiện tại tạo ra.

