# Plan 081 — Academic assignments

### A.4 GVCN

| Class | Teacher | Start date   | Status   |
| ----- | ------- | ------------ | -------- |
| `6A1` | `GV001` | `2026-09-01` | `ACTIVE` |
| `6A2` | `GV002` | `2026-09-01` | `ACTIVE` |
| `6A3` | `GV003` | `2026-09-01` | `ACTIVE` |
| `6A4` | `GV004` | `2026-09-01` | `ACTIVE` |

### A.5 Phân công môn học

Mỗi giáo viên chỉ xuất hiện ở tối đa 2 môn trong bảng phân công. Các giáo viên
`GV001`–`GV004` đồng thời là GVCN ở bảng A.4; các giáo viên còn lại là GVBM.

| Teacher | Môn 1              | Môn 2              |
| ------- | ------------------ | ------------------ |
| `GV001` | `TOAN`             | `TIN_HOC`          |
| `GV002` | `NGU_VAN`          | `LICH_SU`          |
| `GV003` | `VAT_LY`           | `CONG_NGHE`        |
| `GV004` | `HOA_HOC`          | `SINH_HOC`         |
| `GV005` | `NGOAI_NGU`        | `GDCD`             |
| `GV006` | `DIA_LY`           | `NGOAI_NGU`        |
| `GV007` | `TOAN`             | `NGU_VAN`          |
| `GV008` | `VAT_LY`           | `TIN_HOC`          |
| `GV009` | `HOA_HOC`          | `SINH_HOC`         |
| `GV010` | `CONG_NGHE`        | `NGHE_DIEN`        |
| `GV011` | `NGHE_NONG_NGHIEP` | `GDCD`             |
| `GV012` | `TOAN`             | `DIA_LY`           |
| `GV013` | `NGU_VAN`          | `LICH_SU`          |
| `GV014` | `VAT_LY`           | `HOA_HOC`          |
| `GV015` | `SINH_HOC`         | `TIN_HOC`          |
| `GV016` | `NGOAI_NGU`        | `GDCD`             |
| `GV017` | `DIA_LY`           | `CONG_NGHE`        |
| `GV018` | `TOAN`             | `NGOAI_NGU`        |
| `GV019` | `NGU_VAN`          | `NGHE_DIEN`        |
| `GV020` | `LICH_SU`          | `NGHE_NONG_NGHIEP` |

### A.6 Phân công giáo viên theo lớp

Đây là phân công dạy lớp độc lập với timetable. Seed tạo quan hệ lớp–môn–giáo
viên cho HK1; không tạo ngày, tiết, phòng hoặc thứ tự thời khóa biểu. Mỗi giáo
viên vẫn chỉ dạy các môn đã khai báo ở A.5 và không quá 2 môn.

| Class | Môn         | Giáo viên |
| ----- | ----------- | --------- |
| `6A1` | `TOAN`      | `GV001`   |
| `6A1` | `NGU_VAN`   | `GV002`   |
| `6A1` | `VAT_LY`    | `GV003`   |
| `6A1` | `HOA_HOC`   | `GV004`   |
| `6A1` | `NGOAI_NGU` | `GV005`   |
| `6A2` | `TOAN`      | `GV007`   |
| `6A2` | `NGU_VAN`   | `GV013`   |
| `6A2` | `VAT_LY`    | `GV008`   |
| `6A2` | `HOA_HOC`   | `GV009`   |
| `6A2` | `NGOAI_NGU` | `GV006`   |
| `6A3` | `TOAN`      | `GV012`   |
| `6A3` | `NGU_VAN`   | `GV002`   |
| `6A3` | `TIN_HOC`   | `GV001`   |
| `6A3` | `SINH_HOC`  | `GV004`   |
| `6A3` | `DIA_LY`    | `GV017`   |
| `6A4` | `TOAN`      | `GV018`   |
| `6A4` | `NGU_VAN`   | `GV007`   |
| `6A4` | `TIN_HOC`   | `GV008`   |
| `6A4` | `SINH_HOC`  | `GV009`   |
| `6A4` | `GDCD`      | `GV005`   |


