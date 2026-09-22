# Plan 081 — Scorebook seed scope

## 9. CR v3 — Query và one-column score import

### 9.1 Scorebook

| Dữ liệu            | Giá trị dự kiến                              |
| ------------------ | -------------------------------------------- |
| Scorebook          | Một scorebook `HK1` cho lớp `6A1`            |
| Assessment columns | `Miệng 1`, `15 phút 1`, `Giữa kỳ`, `Cuối kỳ` |
| Expected version   | `0` cho bản mới                              |
| Target import      | Mỗi import chỉ một `assessmentColumnId`      |

### 9.2 Score cells

| Scenario                 | Giá trị                   |
| ------------------------ | ------------------------- |
| Điểm hợp lệ thấp         | `0`                       |
| Điểm hợp lệ thông thường | `5.5`, `7.0`, `9.25`      |
| Absent                   | `11`                      |
| Exempted                 | `12`                      |
| Ô trống                  | Không tự đổi thành `0`    |
| Update existing          | Có old value và new value |
| Invalid high             | `10.1`                    |
| Invalid negative         | `-1`                      |
| Invalid status           | Giá trị ngoài enum        |



