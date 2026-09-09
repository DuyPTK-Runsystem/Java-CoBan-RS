# Module 05 — Lesson Log (Sổ đầu bài)

## Mục tiêu

Ghi nhận đánh giá của giáo viên cho một tiết học cụ thể của lớp: nội dung, tiến độ/mức
độ hoàn thành, nhận xét, tình hình lớp và metadata cần thiết.

## Ownership và liên kết

Mỗi entry phải tham chiếu timetable entry/slot, class, subject và teacher assignment.
Không cho tạo entry mồ côi hoặc entry trùng cho cùng một lesson identity nếu policy không
cho phép.

## Lifecycle

```text
DRAFT -> SUBMITTED -> REVIEWED/AMENDED
```

Giáo viên được sửa theo thời hạn/policy; người duyệt hoặc giáo vụ có thể review/amend khi
được cấp quyền. Mọi thay đổi giữ actor, timestamp, reason và before/after audit.

## Contract boundary

Plan 079 chốt field bắt buộc, rubric đánh giá, edit window, quyền GVCN/GVBM/giáo vụ và
error model. Sổ đầu bài không cập nhật attendance hoặc scorebook một cách ngầm định.
