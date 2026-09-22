# Plan 081 — Functional-room seed rows

| Slot identity | `calendarDay + session + period`       |
| Semester      | Liên kết `HK1`                         |

### 7.2 Phòng chức năng

| Code          | Tên               | Loại            |
| ------------- | ----------------- | --------------- |
| `LAB-PHY-01`  | Phòng Vật lí 01   | Functional room |
| `LAB-CHEM-01` | Phòng Hóa học 01  | Functional room |
| `LAB-BIO-01`  | Phòng Sinh học 01 | Functional room |
| `TIN-1`       | Phòng Tin học 1   | Functional room |
| `TIN-2`       | Phòng Tin học 2   | Functional room |
| `NGHE-1`      | Phòng Nghề        | Functional room |

Mapping fixture: `TIN_HOC` -> `TIN-1`, `TIN-2`; `NGHE_DIEN` và
`NGHE_NONG_NGHIEP` -> `NGHE-1`. Mapping dùng natural key và không tạo duplicate.

