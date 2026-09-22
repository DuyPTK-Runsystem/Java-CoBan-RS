# Dev Note 081: Thiết kế thời khóa biểu đủ bốn khối

- Ngày lập: 2026-09-21
- Trạng thái: IMPLEMENTED — production seed write-set đã cập nhật; focused validation PASS, full backend gate FAIL
- Developer Plan liên quan:
  - [Plan 081 — Seed data v2/v3](../../../dev-impl-plan/summary/081-seed-data-v2-v3-2026-09-18.md)
  - [Plan 075 — Timetable và teacher load](../../../dev-impl-plan/be/timetable/075-timetable-teacher-load-2026-09-11.md)
- Dev Note nguồn phân công: [081 — Seed assignment A6](../bootstrap/081-seed-assignment-a6-2026-09-21.md)
- Phê duyệt/phạm vi: Plan 081 mở rộng được phê duyệt; Luna DEV chỉ sửa production seed và tài liệu, không sửa test.
- Bản chất thay đổi: bảng lịch là nguồn dữ liệu cho production demo seed; persistence vẫn giữ trạng thái `DRAFT`.

## Kết quả

Đã xếp đủ thời khóa biểu HK1 và HK2 cho 16 lớp từ 6A1 đến 9A4, dựa trên toàn bộ phân công hiện tại của 20 giáo viên.

- Cả sáng T2 và sáng T6 của mọi lớp đều học đủ 4 tiết.
- T2: S1–S2 là SHCN, S3–S4 là tiết môn học.
- T6: S1 là SHCN, S2–S4 là tiết môn học.
- Không còn buổi nào chỉ có 1 tiết môn học.
- Không có tiết trống chen giữa một buổi học.
- Không cần dùng phương án fallback 3 tiết.
- Không cần mở T7; toàn bộ lịch nằm trong T2–T6.
- Không có xung đột lớp, giáo viên hoặc phòng chức năng.

## Ràng buộc đã áp dụng

### Khung thời gian và SHCN

- Mỗi ngày có buổi sáng S1–S4 và buổi chiều C1–C4.
- SHCN cố định cho cả 16 lớp: T2 S1–S2 và T6 S1.
- Một buổi có học phải có ít nhất 2 tiết môn học, ngoại trừ SHCN; riêng hai buổi có SHCN bắt buộc đủ cả 4 tiết.
- Ưu tiên gom tiết trống về đầu/cuối buổi và không tạo khoảng trống giữa buổi.

### Phạm vi môn và số tiết/tuần

| Môn | HK1 lớp 6/7/8/9 | HK2 lớp 6/7/8/9 | Ghi chú |
|---|---:|---:|---|
| TOAN | 4/4/4/4 | 4/4/4/4 | |
| VAT_LY | 2/2/2/2 | 2/2/2/2 | |
| HOA_HOC | 0/0/2/2 | 0/0/2/2 | Không xếp lớp 6–7 |
| SINH_HOC | 2/2/2/2 | 2/2/2/2 | |
| NGU_VAN | 4/4/4/5 | 4/4/4/5 | |
| NGOAI_NGU | 3/3/3/2 | 3/3/3/2 | |
| LICH_SU | 1/2/2/1 | 1/2/2/1 | |
| DIA_LY | 1/2/2/1 | 1/2/2/1 | |
| GDCD | 1/1/1/1 | 1/1/1/1 | |
| TIN_HOC | 2/2/2/2 | 2/2/2/2 | |
| CONG_NGHE | 2/2/1/1 | 2/2/1/0 | Không dạy lớp 9 HK2 |
| NGHE_DIEN hoặc NGHE_NONG_NGHIEP | 0/0/0/0 | 0/0/2/2 | Mỗi lớp chỉ học đúng một môn nghề |

Quy ước môn nghề HK2 theo phân công hiện tại:

- Lớp lẻ 8A1, 8A3, 9A1, 9A3 học NGHE_DIEN.
- Lớp chẵn 8A2, 8A4, 9A2, 9A4 học NGHE_NONG_NGHIEP.
- Hai môn nghề dùng chung một phòng NGHE-1.

### Giáo viên và phòng học

- Mỗi giáo viên dạy tối đa 2 môn.
- Tải dạy tối đa 24 tiết/tuần.
- Ngưỡng tối thiểu: 19 tiết với GVBM; 15 tiết với GVCN do được giảm 4 tiết.
- Có đủ 16 GVCN, mỗi lớp đúng một GVCN.
- Tin học dùng hai phòng TIN-1 và TIN-2; số phòng Tin tối thiểu cần đồng thời là 2.
- Hai môn nghề dùng chung NGHE-1; mức sử dụng đồng thời tối đa là 1.

## Tải giáo viên theo phân công

| Giáo viên | Lớp chủ nhiệm | HK1 | HK2 | Môn được phân công |
|---|---|---:|---:|---|
| GV001 | 6A1 | 22 | 22 | TIN_HOC, TOAN |
| GV002 | 6A2 | 19 | 19 | LICH_SU, NGU_VAN |
| GV003 | 6A3 | 20 | 22 | CONG_NGHE, VAT_LY |
| GV004 | 6A4 | 16 | 16 | HOA_HOC, SINH_HOC |
| GV005 | 7A1 | 16 | 19 | GDCD, NGOAI_NGU |
| GV006 | 7A2 | 20 | 20 | DIA_LY, NGOAI_NGU |
| GV007 | 7A3 | 16 | 16 | NGU_VAN, TOAN |
| GV008 | 7A4 | 24 | 24 | TIN_HOC |
| GV009 | 8A1 | 16 | 16 | HOA_HOC, SINH_HOC |
| GV010 | 8A2 | 16 | 17 | CONG_NGHE, NGHE_DIEN |
| GV011 | 8A3 | 16 | 19 | GDCD, NGHE_NONG_NGHIEP |
| GV012 | 8A4 | 20 | 21 | DIA_LY, TOAN |
| GV013 | 9A1 | 19 | 19 | LICH_SU, NGU_VAN |
| GV014 | 9A2 | 18 | 18 | HOA_HOC, VAT_LY |
| GV015 | 9A3 | 20 | 20 | SINH_HOC, TIN_HOC |
| GV016 | 9A4 | 16 | 18 | GDCD, NGOAI_NGU |
| GV017 | — | 20 | 20 | CONG_NGHE, DIA_LY |
| GV018 | — | 20 | 20 | TOAN |
| GV019 | — | 22 | 22 | NGU_VAN |
| GV020 | — | 20 | 20 | LICH_SU |

Tất cả 20 giáo viên đều không vượt 24 tiết. GV001–GV016 là GVCN và đều đạt ít nhất 15 tiết; GV017–GV020 là GVBM và đều đạt ít nhất 19 tiết. Không giáo viên nào vượt quá 2 môn.

## Kết quả kiểm chứng

| Hạng mục | HK1 | HK2 | Trạng thái |
|---|---:|---:|---|
| Tiết môn học theo phân công | 376 | 388 | PASS |
| Dòng phân công lớp–môn | 168 | 172 | PASS |
| Thiếu/thừa tiết so với phân công | 0 | 0 | PASS |
| Xung đột lớp/giáo viên/phòng | 0 | 0 | PASS |
| Buổi SHCN chưa đủ 4 tiết | 0 | 0 | PASS |
| Buổi chỉ có 1 tiết môn học | 0 | 0 | PASS |
| Buổi có tiết trống chen giữa | 0 | 0 | PASS |
| Số phòng Tin dùng đồng thời tối đa | 2 | 2 | PASS |
| Số phòng Nghề dùng đồng thời tối đa | 0 | 1 | PASS |
| Tiết ưu tiên được giữ thành cặp | 222/240 | 236/256 | PASS có ngoại lệ tối thiểu |
| Tiết ưu tiên phải tách cặp | 18 | 20 | Ghi nhận |

Các môn TOAN, VAT_LY, HOA_HOC, SINH_HOC, NGU_VAN, TIN_HOC và hai môn NGHE được ưu tiên xếp cặp 2 tiết. Một số cặp phải tách để 16 lớp cùng có tiết học ở các ô bắt buộc T2 S3–S4 và T6 S2–S4, trong khi chỉ có 15 giáo viên thuộc nhóm môn ưu tiên có thể dạy đồng thời. Việc tách này không tạo buổi 1 tiết và không tạo khoảng trống giữa buổi.

## Chú giải lịch

- Ô có dạng MON/GVxxx; môn Tin và Nghề có thêm tên phòng.
- SHCN/GVxxx là tiết sinh hoạt với GVCN của lớp.
- Dấu — là ô không học.
- T2 đến T6 tương ứng thứ Hai đến thứ Sáu.
- S1–S4 là buổi sáng; C1–C4 là buổi chiều.

## Thời khóa biểu chi tiết

## HK1 — Khối 6

### 6A1 — GVCN GV001

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV001 | SHCN/GV001 | Lý/GV014 | Lý/GV014 | Toán/GV001 | Toán/GV001 | Sinh/GV004 | Sinh/GV004 |
| T3 | — | — | — | — | Văn/GV002 | Văn/GV002 | Tin/GV008/TIN-1 | Tin/GV008/TIN-1 |
| T4 | Địa/GV006 | Công nghệ/GV010 | Ngoại ngữ/GV005 | — | — | — | — | — |
| T5 | Ngoại ngữ/GV005 | Toán/GV001 | Văn/GV002 | Văn/GV002 | — | — | — | — |
| T6 | SHCN/GV001 | Công nghệ/GV010 | Sử/GV020 | Toán/GV001 | — | — | GDCD/GV011 | Ngoại ngữ/GV005 |

### 6A2 — GVCN GV002

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV002 | SHCN/GV002 | Sinh/GV015 | Công nghệ/GV010 | — | — | Địa/GV017 | Ngoại ngữ/GV006 |
| T3 | — | — | — | — | Văn/GV007 | Văn/GV007 | Toán/GV012 | GDCD/GV011 |
| T4 | — | — | — | — | Lý/GV003 | Lý/GV003 | Ngoại ngữ/GV006 | — |
| T5 | Toán/GV012 | Toán/GV012 | Tin/GV008/TIN-1 | Tin/GV008/TIN-1 | — | — | — | — |
| T6 | SHCN/GV002 | Văn/GV007 | Văn/GV007 | Toán/GV012 | Công nghệ/GV010 | Sử/GV002 | Sinh/GV015 | Ngoại ngữ/GV006 |

### 6A3 — GVCN GV003

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV003 | SHCN/GV003 | Công nghệ/GV017 | Sinh/GV015 | — | Sử/GV013 | Ngoại ngữ/GV016 | Toán/GV018 |
| T3 | — | — | — | — | Văn/GV013 | Văn/GV013 | Sinh/GV015 | Ngoại ngữ/GV016 |
| T4 | — | — | — | — | Lý/GV014 | Lý/GV014 | Toán/GV018 | Toán/GV018 |
| T5 | Ngoại ngữ/GV016 | GDCD/GV011 | — | — | Công nghệ/GV017 | Địa/GV006 | Tin/GV008/TIN-1 | Tin/GV008/TIN-1 |
| T6 | SHCN/GV003 | Văn/GV013 | Văn/GV013 | Toán/GV018 | — | — | — | — |

### 6A4 — GVCN GV004

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV004 | SHCN/GV004 | Ngoại ngữ/GV005 | Địa/GV017 | Văn/GV019 | Công nghệ/GV003 | — | — |
| T3 | — | — | — | — | — | — | — | — |
| T4 | Toán/GV001 | Toán/GV001 | Tin/GV008/TIN-1 | Tin/GV008/TIN-1 | — | — | — | — |
| T5 | Lý/GV003 | Lý/GV003 | Văn/GV019 | Văn/GV019 | Sử/GV020 | Công nghệ/GV003 | Ngoại ngữ/GV005 | — |
| T6 | SHCN/GV004 | Toán/GV001 | Toán/GV001 | Văn/GV019 | Sinh/GV004 | Sinh/GV004 | Ngoại ngữ/GV005 | GDCD/GV011 |

## HK1 — Khối 7

### 7A1 — GVCN GV005

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV005 | SHCN/GV005 | Sử/GV020 | GDCD/GV011 | Văn/GV002 | Văn/GV002 | Toán/GV012 | Toán/GV012 |
| T3 | Địa/GV017 | Công nghệ/GV010 | Ngoại ngữ/GV006 | — | — | — | — | — |
| T4 | Tin/GV008/TIN-1 | Tin/GV008/TIN-1 | Toán/GV012 | Toán/GV012 | — | — | Địa/GV017 | Ngoại ngữ/GV006 |
| T5 | — | — | — | — | Văn/GV002 | Văn/GV002 | Sinh/GV009 | Sinh/GV009 |
| T6 | SHCN/GV005 | Lý/GV014 | Lý/GV014 | Công nghệ/GV010 | Ngoại ngữ/GV006 | Sử/GV020 | — | — |

### 7A2 — GVCN GV006

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV006 | SHCN/GV006 | Văn/GV007 | Văn/GV007 | — | — | — | — |
| T3 | — | Ngoại ngữ/GV016 | Địa/GV017 | Công nghệ/GV017 | Sinh/GV015 | Sinh/GV015 | Toán/GV018 | Toán/GV018 |
| T4 | Ngoại ngữ/GV016 | Sử/GV020 | Văn/GV007 | Văn/GV007 | Tin/GV008/TIN-1 | Tin/GV008/TIN-1 | Lý/GV003 | Lý/GV003 |
| T5 | — | — | — | — | — | — | — | — |
| T6 | SHCN/GV006 | Toán/GV018 | Toán/GV018 | GDCD/GV011 | Ngoại ngữ/GV016 | Công nghệ/GV017 | Sử/GV020 | Địa/GV017 |

### 7A3 — GVCN GV007

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV007 | SHCN/GV007 | Toán/GV001 | Toán/GV001 | Công nghệ/GV003 | Ngoại ngữ/GV005 | GDCD/GV011 | — |
| T3 | Văn/GV013 | Văn/GV013 | Lý/GV014 | Lý/GV014 | — | — | Địa/GV017 | Sử/GV020 |
| T4 | Địa/GV017 | Ngoại ngữ/GV005 | — | — | Văn/GV013 | Văn/GV013 | Sinh/GV015 | Sinh/GV015 |
| T5 | — | — | — | — | — | — | — | — |
| T6 | SHCN/GV007 | Ngoại ngữ/GV005 | Công nghệ/GV003 | Sử/GV020 | Toán/GV001 | Toán/GV001 | Tin/GV008/TIN-1 | Tin/GV008/TIN-1 |

### 7A4 — GVCN GV008

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV008 | SHCN/GV008 | Văn/GV019 | Văn/GV019 | — | — | — | — |
| T3 | Ngoại ngữ/GV006 | Địa/GV017 | Công nghệ/GV010 | — | — | — | — | — |
| T4 | — | — | — | — | Sử/GV020 | Ngoại ngữ/GV006 | Toán/GV012 | Toán/GV012 |
| T5 | Ngoại ngữ/GV006 | Sử/GV020 | Công nghệ/GV010 | GDCD/GV011 | Văn/GV019 | Văn/GV019 | Toán/GV012 | Toán/GV012 |
| T6 | SHCN/GV008 | Địa/GV017 | Sinh/GV004 | Sinh/GV004 | Tin/GV008/TIN-1 | Tin/GV008/TIN-1 | Lý/GV003 | Lý/GV003 |

## HK1 — Khối 8

### 8A1 — GVCN GV009

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV009 | SHCN/GV009 | Địa/GV006 | Ngoại ngữ/GV016 | Tin/GV008/TIN-1 | Tin/GV008/TIN-1 | Văn/GV002 | Văn/GV002 |
| T3 | — | — | — | — | Sử/GV020 | Ngoại ngữ/GV016 | Công nghệ/GV010 | Sinh/GV009 |
| T4 | — | — | — | — | Toán/GV018 | Toán/GV018 | Văn/GV002 | Văn/GV002 |
| T5 | Sử/GV020 | Địa/GV006 | Hóa/GV004 | Hóa/GV004 | Toán/GV018 | Toán/GV018 | Lý/GV014 | Lý/GV014 |
| T6 | SHCN/GV009 | Ngoại ngữ/GV016 | GDCD/GV011 | Sinh/GV009 | — | — | — | — |

### 8A2 — GVCN GV010

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV010 | SHCN/GV010 | Lý/GV003 | Lý/GV003 | Sử/GV020 | Địa/GV017 | Ngoại ngữ/GV005 | GDCD/GV011 |
| T3 | — | — | — | — | — | — | — | — |
| T4 | Sử/GV020 | Địa/GV017 | Công nghệ/GV010 | Ngoại ngữ/GV005 | Toán/GV001 | Toán/GV001 | Văn/GV007 | Văn/GV007 |
| T5 | Hóa/GV009 | Hóa/GV009 | Văn/GV007 | Văn/GV007 | — | — | — | — |
| T6 | SHCN/GV010 | Tin/GV015/TIN-1 | Tin/GV015/TIN-1 | Ngoại ngữ/GV005 | Sinh/GV015 | Sinh/GV015 | Toán/GV001 | Toán/GV001 |

### 8A3 — GVCN GV011

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV011 | SHCN/GV011 | Văn/GV013 | Văn/GV013 | Ngoại ngữ/GV006 | Sử/GV020 | — | — |
| T3 | — | — | — | — | — | — | Địa/GV006 | Ngoại ngữ/GV006 |
| T4 | — | — | — | — | Ngoại ngữ/GV006 | Sử/GV020 | Công nghệ/GV010 | GDCD/GV011 |
| T5 | Tin/GV008/TIN-1 | Tin/GV008/TIN-1 | Toán/GV012 | Toán/GV012 | Sinh/GV004 | Sinh/GV004 | Văn/GV013 | Văn/GV013 |
| T6 | SHCN/GV011 | Hóa/GV009 | Hóa/GV009 | Địa/GV006 | Lý/GV014 | Lý/GV014 | Toán/GV012 | Toán/GV012 |

### 8A4 — GVCN GV012

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV012 | SHCN/GV012 | Sinh/GV009 | Sinh/GV009 | GDCD/GV011 | Ngoại ngữ/GV016 | Sử/GV020 | Địa/GV017 |
| T3 | Văn/GV019 | Văn/GV019 | Tin/GV015/TIN-1 | Tin/GV015/TIN-1 | Toán/GV018 | Toán/GV018 | Lý/GV003 | Lý/GV003 |
| T4 | — | — | — | — | — | — | — | — |
| T5 | Hóa/GV004 | Hóa/GV004 | Toán/GV018 | Toán/GV018 | Ngoại ngữ/GV016 | Công nghệ/GV010 | — | — |
| T6 | SHCN/GV012 | Văn/GV019 | Văn/GV019 | Địa/GV017 | — | — | Ngoại ngữ/GV016 | Sử/GV020 |

## HK1 — Khối 9

### 9A1 — GVCN GV013

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV013 | SHCN/GV013 | Công nghệ/GV010 | Văn/GV002 | Ngoại ngữ/GV005 | Địa/GV006 | Hóa/GV009 | Hóa/GV009 |
| T3 | Lý/GV014 | Lý/GV014 | Văn/GV002 | Văn/GV002 | Tin/GV001/TIN-1 | Tin/GV001/TIN-1 | Toán/GV001 | Toán/GV001 |
| T4 | — | — | — | — | — | — | Ngoại ngữ/GV005 | Sử/GV020 |
| T5 | — | — | — | — | Sinh/GV015 | Sinh/GV015 | Toán/GV001 | Toán/GV001 |
| T6 | SHCN/GV013 | GDCD/GV011 | Văn/GV002 | Văn/GV002 | — | — | — | — |

### 9A2 — GVCN GV014

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV014 | SHCN/GV014 | Sinh/GV004 | Sinh/GV004 | — | — | — | — |
| T3 | — | — | — | — | Văn/GV019 | Tin/GV008/TIN-2 | Hóa/GV014 | Hóa/GV014 |
| T4 | Toán/GV012 | Toán/GV012 | Văn/GV019 | Văn/GV019 | GDCD/GV011 | Công nghệ/GV010 | Ngoại ngữ/GV016 | Địa/GV017 |
| T5 | — | — | — | — | — | — | Ngoại ngữ/GV016 | Sử/GV002 |
| T6 | SHCN/GV014 | Toán/GV012 | Toán/GV012 | Tin/GV008/TIN-1 | Lý/GV003 | Lý/GV003 | Văn/GV019 | Văn/GV019 |

### 9A3 — GVCN GV015

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV015 | SHCN/GV015 | Toán/GV018 | Toán/GV018 | Lý/GV014 | Lý/GV014 | Văn/GV013 | Văn/GV013 |
| T3 | Sinh/GV009 | Sinh/GV009 | Sử/GV013 | Công nghệ/GV010 | — | — | GDCD/GV011 | Ngoại ngữ/GV005 |
| T4 | — | — | — | — | — | — | — | — |
| T5 | Toán/GV018 | Toán/GV018 | Tin/GV015/TIN-2 | Tin/GV015/TIN-2 | Văn/GV013 | Văn/GV013 | Hóa/GV004 | Hóa/GV004 |
| T6 | SHCN/GV015 | Địa/GV006 | Ngoại ngữ/GV005 | Văn/GV013 | — | — | — | — |

### 9A4 — GVCN GV016

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV016 | SHCN/GV016 | GDCD/GV011 | Sử/GV020 | Sinh/GV015 | Sinh/GV015 | Văn/GV019 | Văn/GV019 |
| T3 | — | — | Toán/GV007 | Toán/GV007 | — | — | — | — |
| T4 | Văn/GV019 | Văn/GV019 | Hóa/GV009 | Hóa/GV009 | — | — | — | — |
| T5 | — | — | — | — | Công nghệ/GV010 | Ngoại ngữ/GV016 | Lý/GV003 | Văn/GV019 |
| T6 | SHCN/GV016 | Tin/GV008/TIN-2 | Tin/GV008/TIN-2 | Lý/GV003 | Toán/GV007 | Toán/GV007 | Địa/GV017 | Ngoại ngữ/GV016 |

## HK2 — Khối 6

### 6A1 — GVCN GV001

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV001 | SHCN/GV001 | Tin/GV008/TIN-1 | Tin/GV008/TIN-1 | — | — | — | — |
| T3 | Lý/GV014 | Lý/GV014 | Toán/GV001 | Toán/GV001 | — | Ngoại ngữ/GV005 | Văn/GV002 | Văn/GV002 |
| T4 | — | — | — | — | — | — | GDCD/GV011 | Ngoại ngữ/GV005 |
| T5 | Công nghệ/GV003 | Công nghệ/GV003 | Văn/GV002 | Văn/GV002 | — | — | — | — |
| T6 | SHCN/GV001 | Ngoại ngữ/GV005 | Sử/GV020 | Địa/GV006 | Toán/GV001 | Toán/GV001 | Sinh/GV004 | Sinh/GV004 |

### 6A2 — GVCN GV002

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV002 | SHCN/GV002 | GDCD/GV011 | Ngoại ngữ/GV006 | — | — | — | — |
| T3 | Tin/GV008/TIN-1 | Tin/GV008/TIN-1 | Toán/GV012 | Toán/GV012 | — | Ngoại ngữ/GV006 | Văn/GV007 | Văn/GV007 |
| T4 | Văn/GV007 | Văn/GV007 | Sinh/GV015 | Sinh/GV015 | Lý/GV003 | Công nghệ/GV010 | — | — |
| T5 | Công nghệ/GV010 | Sử/GV002 | — | — | — | — | — | — |
| T6 | SHCN/GV002 | Toán/GV012 | Toán/GV012 | Lý/GV003 | Ngoại ngữ/GV006 | Địa/GV006 | — | — |

### 6A3 — GVCN GV003

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV003 | SHCN/GV003 | Ngoại ngữ/GV016 | Lý/GV014 | — | — | — | — |
| T3 | GDCD/GV011 | Ngoại ngữ/GV016 | Văn/GV013 | Văn/GV013 | — | Sử/GV013 | Lý/GV014 | Công nghệ/GV017 |
| T4 | — | — | — | — | Toán/GV018 | Toán/GV018 | Tin/GV008/TIN-1 | Tin/GV008/TIN-1 |
| T5 | Công nghệ/GV017 | Ngoại ngữ/GV016 | Sinh/GV015 | — | — | — | — | — |
| T6 | SHCN/GV003 | Văn/GV013 | Văn/GV013 | Sinh/GV015 | Toán/GV018 | Toán/GV018 | Địa/GV017 | — |

### 6A4 — GVCN GV004

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV004 | SHCN/GV004 | Toán/GV001 | Toán/GV001 | Lý/GV003 | Lý/GV003 | Văn/GV019 | Văn/GV019 |
| T3 | — | — | Ngoại ngữ/GV005 | Công nghệ/GV003 | — | — | — | — |
| T4 | Sinh/GV004 | Sinh/GV004 | Công nghệ/GV003 | — | — | — | — | — |
| T5 | Toán/GV001 | Toán/GV001 | Văn/GV019 | Văn/GV019 | Ngoại ngữ/GV005 | GDCD/GV011 | — | — |
| T6 | SHCN/GV004 | Tin/GV008/TIN-1 | Tin/GV008/TIN-1 | Sử/GV020 | — | — | Địa/GV006 | Ngoại ngữ/GV005 |

## HK2 — Khối 7

### 7A1 — GVCN GV005

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV005 | SHCN/GV005 | Toán/GV012 | Toán/GV012 | Văn/GV002 | Văn/GV002 | Sinh/GV009 | Sinh/GV009 |
| T3 | — | Sử/GV020 | Ngoại ngữ/GV006 | Công nghệ/GV010 | — | — | — | — |
| T4 | — | — | Sử/GV020 | Địa/GV017 | — | — | — | — |
| T5 | Ngoại ngữ/GV006 | Lý/GV014 | — | — | Toán/GV012 | Toán/GV012 | Tin/GV008/TIN-1 | Tin/GV008/TIN-1 |
| T6 | SHCN/GV005 | Ngoại ngữ/GV006 | GDCD/GV011 | Lý/GV014 | Công nghệ/GV010 | Địa/GV017 | Văn/GV002 | Văn/GV002 |

### 7A2 — GVCN GV006

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV006 | SHCN/GV006 | Công nghệ/GV017 | Văn/GV007 | Sinh/GV015 | Sinh/GV015 | Địa/GV017 | Ngoại ngữ/GV016 |
| T3 | Văn/GV007 | Văn/GV007 | Toán/GV018 | Toán/GV018 | — | — | — | — |
| T4 | Ngoại ngữ/GV016 | Sử/GV020 | GDCD/GV011 | — | Tin/GV008/TIN-1 | Tin/GV008/TIN-1 | Lý/GV003 | Lý/GV003 |
| T5 | — | — | — | — | — | — | Công nghệ/GV017 | Ngoại ngữ/GV016 |
| T6 | SHCN/GV006 | Toán/GV018 | Toán/GV018 | Văn/GV007 | — | — | Sử/GV020 | Địa/GV017 |

### 7A3 — GVCN GV007

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV007 | SHCN/GV007 | Sử/GV020 | Địa/GV017 | Tin/GV008/TIN-1 | Tin/GV008/TIN-1 | Toán/GV001 | Toán/GV001 |
| T3 | Toán/GV001 | Toán/GV001 | Sinh/GV015 | Sinh/GV015 | Lý/GV014 | Lý/GV014 | Công nghệ/GV003 | Ngoại ngữ/GV005 |
| T4 | — | — | — | — | Văn/GV013 | Văn/GV013 | Ngoại ngữ/GV005 | — |
| T5 | — | — | — | — | Văn/GV013 | Văn/GV013 | Công nghệ/GV003 | Sử/GV020 |
| T6 | SHCN/GV007 | Địa/GV017 | Ngoại ngữ/GV005 | GDCD/GV011 | — | — | — | — |

### 7A4 — GVCN GV008

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV008 | SHCN/GV008 | Ngoại ngữ/GV006 | Sử/GV020 | — | — | — | — |
| T3 | Văn/GV019 | Văn/GV019 | Tin/GV008/TIN-1 | Tin/GV008/TIN-1 | Sử/GV020 | Địa/GV017 | Toán/GV012 | Toán/GV012 |
| T4 | Lý/GV003 | Lý/GV003 | Toán/GV012 | Toán/GV012 | — | Sinh/GV004 | Ngoại ngữ/GV006 | Công nghệ/GV010 |
| T5 | — | — | GDCD/GV011 | Công nghệ/GV010 | — | — | Ngoại ngữ/GV006 | Địa/GV017 |
| T6 | SHCN/GV008 | Văn/GV019 | Văn/GV019 | Sinh/GV004 | — | — | — | — |

## HK2 — Khối 8

### 8A1 — GVCN GV009

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV009 | SHCN/GV009 | Toán/GV018 | Toán/GV018 | Ngoại ngữ/GV016 | Sử/GV020 | — | — |
| T3 | — | — | — | — | Địa/GV006 | Ngoại ngữ/GV016 | Hóa/GV004 | Hóa/GV004 |
| T4 | Toán/GV018 | Toán/GV018 | Văn/GV002 | Văn/GV002 | — | — | — | — |
| T5 | Ngoại ngữ/GV016 | Địa/GV006 | Sử/GV020 | GDCD/GV005 | Tin/GV008/TIN-1 | Tin/GV008/TIN-1 | Nghề điện/GV010/NGHE-1 | Nghề điện/GV010/NGHE-1 |
| T6 | SHCN/GV009 | Văn/GV002 | Văn/GV002 | Công nghệ/GV010 | Lý/GV014 | Lý/GV014 | Sinh/GV009 | Sinh/GV009 |

### 8A2 — GVCN GV010

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV010 | SHCN/GV010 | Hóa/GV009 | Hóa/GV009 | — | — | — | — |
| T3 | Địa/GV017 | Công nghệ/GV010 | Sử/GV020 | Ngoại ngữ/GV005 | — | — | — | — |
| T4 | Địa/GV017 | GDCD/GV016 | Toán/GV001 | Ngoại ngữ/GV005 | Tin/GV015/TIN-2 | Tin/GV015/TIN-2 | Văn/GV007 | Văn/GV007 |
| T5 | Ngoại ngữ/GV005 | Sử/GV020 | — | — | Lý/GV003 | Lý/GV003 | Toán/GV001 | Toán/GV001 |
| T6 | SHCN/GV010 | Sinh/GV015 | Sinh/GV015 | Toán/GV001 | Nghề nông/GV011/NGHE-1 | Nghề nông/GV011/NGHE-1 | Văn/GV007 | Văn/GV007 |

### 8A3 — GVCN GV011

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV011 | SHCN/GV011 | Văn/GV013 | Văn/GV013 | — | Ngoại ngữ/GV006 | Công nghệ/GV010 | Địa/GV006 |
| T3 | — | — | — | — | — | — | — | — |
| T4 | Toán/GV012 | Toán/GV012 | Nghề điện/GV010/NGHE-1 | Nghề điện/GV010/NGHE-1 | Địa/GV006 | Ngoại ngữ/GV006 | Sử/GV020 | — |
| T5 | Văn/GV013 | Văn/GV013 | Lý/GV014 | Lý/GV014 | Sử/GV020 | Ngoại ngữ/GV006 | Sinh/GV004 | Sinh/GV004 |
| T6 | SHCN/GV011 | GDCD/GV011 | Hóa/GV009 | Hóa/GV009 | Toán/GV012 | Toán/GV012 | Tin/GV008/TIN-1 | Tin/GV008/TIN-1 |

### 8A4 — GVCN GV012

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV012 | SHCN/GV012 | Tin/GV015/TIN-2 | Tin/GV015/TIN-2 | Sinh/GV009 | Sinh/GV009 | Toán/GV018 | Toán/GV018 |
| T3 | — | Toán/GV018 | Công nghệ/GV017 | Ngoại ngữ/GV016 | Lý/GV003 | Lý/GV003 | Văn/GV019 | Văn/GV019 |
| T4 | Sử/GV020 | GDCD/GV005 | Địa/GV017 | Ngoại ngữ/GV016 | — | — | — | — |
| T5 | — | — | — | — | Ngoại ngữ/GV016 | Địa/GV017 | Sử/GV020 | — |
| T6 | SHCN/GV012 | Hóa/GV004 | Hóa/GV004 | Toán/GV018 | Văn/GV019 | Văn/GV019 | Nghề nông/GV011/NGHE-1 | Nghề nông/GV011/NGHE-1 |

## HK2 — Khối 9

### 9A1 — GVCN GV013

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV013 | SHCN/GV013 | Văn/GV002 | Văn/GV002 | — | GDCD/GV011 | Ngoại ngữ/GV005 | Địa/GV017 |
| T3 | Văn/GV002 | Văn/GV002 | Lý/GV014 | Lý/GV014 | Nghề điện/GV010/NGHE-1 | Nghề điện/GV010/NGHE-1 | Toán/GV001 | Toán/GV001 |
| T4 | — | — | — | — | Ngoại ngữ/GV005 | Sử/GV020 | — | — |
| T5 | — | — | — | — | — | — | Hóa/GV009 | Hóa/GV009 |
| T6 | SHCN/GV013 | Toán/GV001 | Toán/GV001 | Văn/GV002 | Sinh/GV015 | Sinh/GV015 | Tin/GV001/TIN-2 | Tin/GV001/TIN-2 |

### 9A2 — GVCN GV014

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV014 | SHCN/GV014 | Văn/GV019 | Văn/GV019 | — | — | — | — |
| T3 | — | — | Ngoại ngữ/GV016 | Văn/GV019 | Toán/GV012 | Toán/GV012 | Nghề nông/GV011/NGHE-1 | Nghề nông/GV011/NGHE-1 |
| T4 | Tin/GV008/TIN-1 | Địa/GV006 | — | — | Văn/GV019 | Văn/GV019 | Toán/GV012 | Toán/GV012 |
| T5 | — | — | — | — | Sinh/GV004 | Sinh/GV004 | Hóa/GV014 | Hóa/GV014 |
| T6 | SHCN/GV014 | Lý/GV003 | Lý/GV003 | Tin/GV008/TIN-1 | — | Sử/GV002 | Ngoại ngữ/GV016 | GDCD/GV016 |

### 9A3 — GVCN GV015

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV015 | SHCN/GV015 | GDCD/GV005 | Hóa/GV004 | Nghề điện/GV010/NGHE-1 | Nghề điện/GV010/NGHE-1 | Tin/GV015/TIN-1 | Tin/GV015/TIN-1 |
| T3 | — | — | — | — | Toán/GV018 | Toán/GV018 | Văn/GV013 | Văn/GV013 |
| T4 | Văn/GV013 | Văn/GV013 | Ngoại ngữ/GV005 | Hóa/GV004 | — | — | — | — |
| T5 | Sinh/GV009 | Sinh/GV009 | Toán/GV018 | Toán/GV018 | Địa/GV017 | Ngoại ngữ/GV005 | Sử/GV013 | — |
| T6 | SHCN/GV015 | Lý/GV014 | Lý/GV014 | Văn/GV013 | — | — | — | — |

### 9A4 — GVCN GV016

| Ngày | S1 | S2 | S3 | S4 | C1 | C2 | C3 | C4 |
|---|---|---|---|---|---|---|---|---|
| T2 | SHCN/GV016 | SHCN/GV016 | Lý/GV003 | Lý/GV003 | Văn/GV019 | Văn/GV019 | Nghề nông/GV011/NGHE-1 | Nghề nông/GV011/NGHE-1 |
| T3 | Ngoại ngữ/GV016 | Địa/GV012 | — | — | Hóa/GV009 | Hóa/GV009 | Sinh/GV015 | Sinh/GV015 |
| T4 | — | GDCD/GV011 | Toán/GV007 | Toán/GV007 | — | — | — | — |
| T5 | Văn/GV019 | Văn/GV019 | Tin/GV008/TIN-1 | Tin/GV008/TIN-1 | — | — | — | — |
| T6 | SHCN/GV016 | Toán/GV007 | Toán/GV007 | Văn/GV019 | Ngoại ngữ/GV016 | Sử/GV020 | — | — |

## Triển khai production seed

- `DemoTimetableSeeder` đã mở rộng target thành đủ 16 lớp `6A1`–`9A4`, HK1/HK2; seed 376/388 tiết môn học theo các bảng trên.
- `day_of_week` được ghi đúng `1..5` tương ứng T2–T6; marker mới là `SEED_PLAN_081_FULL_V2`.
- Mọi ID được resolve theo mã lớp/môn/phòng; `TIN_HOC` dùng `TIN-1`/`TIN-2`, hai môn Nghề dùng `NGHE-1`.
- Revision G7 cũ chỉ được nâng cấp khi toàn bộ các revision có entry đều có audit marker legacy; dữ liệu ngoài fixture sẽ làm seeder no-op.
- `DemoIdentitySeeder` không tạo enrollment cho `STU2600041`–`STU2600080`; không set nullable `current_class_id` và không xóa enrollment đã tồn tại.
- `DemoFunctionalRoomSeeder` tạo thêm đúng các mã phòng fixture; `DemoSubjectFunctionalRoomSeeder` tạo mapping natural-key idempotent trước khi seed timetable.

## File thay đổi

- document/dev-note/be/timetable/081-full-timetable-design-2026-09-21.md
- document/dev-note/be/BE_DEV_NOTE_SUMMARY.md
- document/dev-note/summary/DEV_NOTE_SUMMARY.md
- BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/bootstrap/DemoDataSeeder.java
- BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/bootstrap/DemoFunctionalRoomSeeder.java
- BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/bootstrap/DemoSubjectFunctionalRoomSeeder.java
- BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/bootstrap/DemoTimetableSeeder.java
- BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/identity/DemoIdentitySeeder.java

## Validation Result

- Constraint audit trên mô hình lịch: PASS cho cả HK1 và HK2.
- Kiểm tra ngược 32 bảng lớp–học kỳ/160 dòng ngày học: PASS; không xung đột giáo viên/phòng, không buổi 1 tiết, không khoảng trống giữa buổi và không sai số tiết.
- Kiểm tra định dạng và khoảng trắng bằng git diff --check: PASS.
- Focused test suite: PASS theo Validation Result của culi TEST cuối.
- `checkstyleMain`: PASS với `975` warning baseline; `pmdMain`: PASS; `git diff --check`: PASS.
- Full `test`: FAIL (`554 tests/99 failures`) do `Missing seeded role: ACADEMIC_OFFICE` tại `DemoIdentitySeeder.java:185`.
- `pmdTest`: FAIL với `313` baseline violations; full `build`: FAIL do hai blocker trên.
- Frontend test/build: NOT RUN — thay đổi chỉ là tài liệu thiết kế.

## Sai khác, rủi ro và bước tiếp theo

- Đây là seed write-set trạng thái `DRAFT`, không claim đã publish hoặc full backend xanh.
- DB sạch dùng `LAB-PHY-01`, `LAB-CHEM-01`, `LAB-BIO-01`, `TIN-1`, `TIN-2`, `NGHE-1`; `LAB-IT-01` không còn seed mới. Mapping môn–phòng, marker `SEED_PLAN_081_FULL_V2` và idempotency đã được ghi nhận.
- Chưa đưa vào các ràng buộc chưa được cung cấp như ngày nghỉ riêng của giáo viên, phòng bộ môn khác, tiết chào cờ hoặc hoạt động đột xuất.
- Khi Luna triển khai, cần ánh xạ từng ô sang assignmentId/periodId thực tế, chạy lại validator backend và kiểm thử việc lưu/publish.
- Nếu dữ liệu phân công, GVCN hoặc cấu hình tiết học thay đổi, phải tái sinh lịch thay vì chỉnh tay từng ô.
