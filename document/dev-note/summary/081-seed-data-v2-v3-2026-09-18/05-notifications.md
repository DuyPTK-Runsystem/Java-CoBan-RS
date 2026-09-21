# Plan 081 — Notification seed rows

### B.4 Notification

| Key                  | Title                   | Audience     | Reference           | State       | Channel  | Publish/expiry          |
| -------------------- | ----------------------- | ------------ | ------------------- | ----------- | -------- | ----------------------- |
| `NOTI-081-IND-01`    | Cập nhật hồ sơ          | `INDIVIDUAL` | `pham.minh.quan`    | `PUBLISHED` | `IN_APP` | 2026-09-18 / null       |
| `NOTI-081-IND-02`    | Kết quả học tập         | `INDIVIDUAL` | `nguyen.minh.khang` | `PUBLISHED` | `IN_APP` | 2026-09-18 / null       |
| `NOTI-081-CLASS-01`  | Lịch học tuần này       | `CLASS`      | `6A1`               | `PUBLISHED` | `IN_APP` | 2026-09-18 / 2026-09-25 |
| `NOTI-081-SCHOOL-01` | Thông báo toàn trường   | `SCHOOL`     | school scope        | `SCHEDULED` | `IN_APP` | 2026-09-20 / 2026-09-30 |
| `NOTI-081-EMAIL-01`  | Nhắc lịch họp           | `INDIVIDUAL` | `tran.thu.ha`       | `PUBLISHED` | `EMAIL`  | 2026-09-18 / null       |
| `NOTI-081-EXPIRED`   | Hạn đăng ký đã kết thúc | `CLASS`      | `6A2`               | `EXPIRED`   | `IN_APP` | 2026-09-10 / 2026-09-17 |
| `NOTI-081-CANCELLED` | Lịch cũ đã huỷ          | `SCHOOL`     | school scope        | `CANCELLED` | `IN_APP` | null / null             |
| `NOTI-081-DRAFT`     | Bản nháp học vụ         | `INDIVIDUAL` | `academic.office`   | `DRAFT`     | `IN_APP` | null / null             |

| Notification        | Recipient           | Read at                     | Email delivery | Idempotency key     |
| ------------------- | ------------------- | --------------------------- | -------------- | ------------------- |
| `NOTI-081-IND-01`   | `pham.minh.quan`    | `2026-09-18T08:30:00+07:00` | null           | `idem-081-ind-01`   |
| `NOTI-081-IND-02`   | `nguyen.minh.khang` | null                        | null           | `idem-081-ind-02`   |
| `NOTI-081-CLASS-01` | `nguyen.minh.khang` | null                        | null           | `idem-081-class-01` |
| `NOTI-081-CLASS-01` | `nguyen.ngoc.anh`   | `2026-09-18T09:00:00+07:00` | null           | `idem-081-class-01` |
| `NOTI-081-EMAIL-01` | `tran.thu.ha`       | null                        | `SENT`         | `idem-081-email-01` |
| `NOTI-081-EMAIL-01` | `le.hoang.nam`      | null                        | `FAILED`       | `idem-081-email-01` |


