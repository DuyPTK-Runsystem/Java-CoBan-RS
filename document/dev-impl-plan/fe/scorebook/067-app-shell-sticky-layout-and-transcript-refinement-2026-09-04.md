# Developer Plan 067: App Shell Sticky Layout & Transcript Display Refinements

## 1. Trạng thái

- **Approval gate:** `Approved — Người dùng yêu cầu thực hiện ngày 2026-09-04`.
- **Ngày lập:** `2026-09-04`.
- **Module:** FE `foundation` / `layout` / `scorebook` / `transcript`.
- **Liên quan:** Plan 061, Plan 062, Plan 062.1.

## 2. Mục tiêu

1. **Cố định Top Panel và Side Navigation Bar khi cuộn trang:**
   - Khi người dùng cuộn xem dữ liệu danh sách dài (đặc biệt là bảng điểm theo lớp có 40 học sinh), Top Panel (`.app-header`) và Side Nav Bar (`.sidebar`) phải luôn cố định trên màn hình, không bị trôi đi.
2. **Ẩn hiển thị điểm Regular trong bảng điểm:**
   - Trong bảng điểm cả năm, các môn học có điểm đạt chuẩn thông thường (`REGULAR`, không thi lại) không cần hiển thị badge `REGULAR` và nhãn `Đạt chuẩn` lặp lại dư thừa ở cột Ghi chú; thay vào đó hiển thị ký hiệu `—`.
3. **Đồng bộ hóa giao diện Bảng điểm lớp học:**
   - Chuẩn hóa tiêu đề thành sentence case `Bảng điểm lớp học`.
   - Khôi phục font mặc định thống nhất (Roboto, weight 700).
   - Áp dụng phong cách nền trong suốt (`outlined`), viền và chữ xanh cho 2 mini button ở trên (`BẢNG ĐIỂM THEO MÔN` và `BẢNG ĐIỂM TỔNG KẾT`).

## 3. Kế hoạch Triển khai Chi tiết

### Phase 1: App Shell Layout Sticky Optimization
- Sửa `FE/src/styles.css`:
  - `.app-header`: `position: sticky; top: 0; z-index: 1000; background: white;`
  - `.sidebar`: `position: sticky; top: 64px; height: calc(100vh - 64px); overflow-y: auto; z-index: 999; background: white;`
  - `.page-content`: `flex: 1; min-width: 0;`
  - Mobile breakpoint `<= 680px`: Giữ `position: static; height: auto;`.

### Phase 2: Transcript Regular Note Masking
- Sửa `FE/src/components/TranscriptAnnualTable.vue`:
  - Loại bỏ `<span class="badge regular-badge">REGULAR</span><span class="retake-detail">Đạt chuẩn</span>` khi điểm là regular.
  - Thay bằng `<span class="cell-empty">—</span>`.
- Cập nhật unit test `FE/src/components/TranscriptAnnualTable.spec.ts`.

### Phase 3: Class Transcript UI Harmonization
- Sửa `FE/src/views/ClassTranscriptViewerView.vue`:
  - Tiêu đề `Bảng điểm lớp học`.
  - Font mặc định Roboto, bỏ font-weight 900.
  - 2 mini button scope luôn mang `:outlined="true"`, active có viền chữ xanh.
- Cập nhật unit test `FE/src/views/ClassTranscriptViewerView.spec.ts`.

## 4. Quality Gates

- `npm run lint`: PASS (0 error, 0 warning).
- `npm run test`: PASS 100% test suites (75/75 files, 324/324 tests).
- `npm run build`: PASS Vite production bundle.

