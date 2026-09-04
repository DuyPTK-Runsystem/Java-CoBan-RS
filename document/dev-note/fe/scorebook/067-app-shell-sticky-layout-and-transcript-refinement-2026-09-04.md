# Dev Note 067: App Shell Sticky Layout & Transcript Display Refinements

## 1. Related Developer Plan & Approval Status

- **Developer Plan:** [`document/dev-impl-plan/fe/scorebook/067-app-shell-sticky-layout-and-transcript-refinement-2026-09-04.md`](file:///home/duyptk/Coding/HoiNhapJava/Java-CoBan/document/dev-impl-plan/fe/scorebook/067-app-shell-sticky-layout-and-transcript-refinement-2026-09-04.md).
- **Approval status:** Approved bởi người dùng qua yêu cầu trực tiếp vào ngày 2026-09-04.
- **Liên quan:** Plan/Note 061 (Transcript Viewer UI), 062 (Retake Result UI), 062.1 (Class Transcript Viewer UI).

## 2. Actual Scope Completed

1. **Ẩn điểm Regular trong màn hình bảng điểm:**
   - Trong `FE/src/components/TranscriptAnnualTable.vue`, các môn học có điểm đạt chuẩn thông thường (`REGULAR`, không thi lại `RETAKE` hay `PLANNED`) không còn hiển thị badge `REGULAR` và nhãn `Đạt chuẩn` lặp lại dư thừa ở cột Ghi chú; thay vào đó hiển thị ký hiệu `—` thanh gọn.
   - Cập nhật test case trong `FE/src/components/TranscriptAnnualTable.spec.ts` kiểm tra môn regular không hiển thị `REGULAR` và hiển thị `—`.
2. **Cố định Top Panel và Side Navigation Bar khi cuộn trang:**
   - Trong `FE/src/styles.css`:
     - `.app-header`: `position: sticky; top: 0; z-index: 1000; background: white;`
     - `.sidebar`: `position: sticky; top: 64px; height: calc(100vh - 64px); overflow-y: auto; z-index: 999; background: white;`
     - `.page-content`: `flex: 1; min-width: 0;`
     - Responsive mobile `<= 680px`: Giữ `position: static; height: auto;`.
   - Giúp top panel và side nav bar luôn cố định trên màn hình kể cả khi người dùng cuộn xem danh sách học sinh dài trong bảng điểm.
3. **Căn chỉnh UI Bảng điểm lớp học theo phản hồi người dùng:**
   - Tiêu đề chính chuyển sang sentence case: **`Bảng điểm lớp học`**.
   - Khôi phục font mặc định Roboto thống nhất toàn ứng dụng (`font-weight: 700`, `font-size: 28px`), loại bỏ `font-weight: 900`.
   - 2 mini button ở trên (`BẢNG ĐIỂM THEO MÔN` và `BẢNG ĐIỂM TỔNG KẾT`): Luôn mang nền trong suốt (`outlined`), viền chữ xanh khi active và viền chữ xám khi inactive.

## 3. Files Changed Grouped by Purpose

### App Shell & Layout
- `FE/src/styles.css`: Cố định sticky cho `.app-header` và `.sidebar`, cấu hình responsive mobile và `min-width: 0` cho `.page-content`.

### Transcript Components & Tests
- `FE/src/components/TranscriptAnnualTable.vue`: Ẩn badge `REGULAR` và nhãn `Đạt chuẩn`, thay bằng `—`.
- `FE/src/components/TranscriptAnnualTable.spec.ts`: Cập nhật assertion cho môn regular.
- `FE/src/views/ClassTranscriptViewerView.vue`: Tiêu đề `Bảng điểm lớp học`, font Roboto, nền trong suốt viền chữ xanh cho 2 mini button.
- `FE/src/views/ClassTranscriptViewerView.spec.ts`: Cập nhật assertion tiêu đề.

### Documentation
- `document/dev-impl-plan/fe/scorebook/062.1-class-transcript-viewer-ui-2026-09-04.md`: Bổ sung mục Amendment.
- `document/dev-note/fe/scorebook/062.1-class-transcript-viewer-ui-2026-09-04.md`: Bổ sung mục Amendment 62.1.
- `document/dev-impl-plan/fe/scorebook/067-app-shell-sticky-layout-and-transcript-refinement-2026-09-04.md`: Plan độc lập cho task 67.
- `document/dev-note/fe/scorebook/067-app-shell-sticky-layout-and-transcript-refinement-2026-09-04.md`: Dev Note hoàn thành task 67.

## 4. Validation Results

- **Unit Tests Frontend:** `npm run test` $\rightarrow$ **PASS** (75/75 test files, 324/324 tests passed).
- **ESLint Frontend:** `npm run lint` $\rightarrow$ **PASS** (0 error, 0 warning).
- **Vite Production Build:** `npm run build` $\rightarrow$ **PASS**.

